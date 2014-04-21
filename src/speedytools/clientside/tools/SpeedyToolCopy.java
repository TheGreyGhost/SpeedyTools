package speedytools.clientside.tools;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraft.world.World;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.network.CloneToolsNetworkClient;
import speedytools.clientside.rendering.*;
import speedytools.clientside.selections.BlockVoxelMultiSelector;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.SpeedyToolsOptions;
import speedytools.common.items.ItemSpeedyCopy;

import java.util.LinkedList;
import java.util.List;

import static speedytools.clientside.selections.BlockMultiSelector.selectFill;

/**
 * User: The Grey Ghost
 * Date: 18/04/2014
 *
 * The various renders for this tool are:
 * 1) Wireframe highlight of the block(s) under the cursor when no selection made yet
 * 2) The boundary field (if any)
 * 3) The solid selection, when made
 * 4) The various status indicators on the cursor:
 *   a) When generating a selection - the "busy" cursor with progress
 *   b) When a selection is made, the tool continually shows the server "ready" status
 *   c) When a selection is made: the power up animation for the action or the undo
 *   d) When an action is in progress: the progress meter
 *   e) When an undo is in progress: the progress meter
 *
 */
public class SpeedyToolCopy extends SpeedyToolClonerBase
{
  public SpeedyToolCopy(ItemSpeedyCopy i_parentItem, SpeedyToolRenderers i_renderers, SpeedyToolSounds i_speedyToolSounds, UndoManagerClient i_undoManagerClient,
                        CloneToolsNetworkClient i_cloneToolsNetworkClient, SpeedyToolBoundary i_speedyToolBoundary) {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient);
    itemSpeedyCopy = i_parentItem;
    speedyToolBoundary = i_speedyToolBoundary;
    boundaryFieldRendererUpdateLink = this.new BoundaryFieldRendererUpdateLink();
    wireframeRendererUpdateLink = this.new CopyToolWireframeRendererLink();
    solidSelectionRendererUpdateLink = this.new SolidSelectionRendererLink();
    cloneToolsNetworkClient = i_cloneToolsNetworkClient;
  }

  @Override
  public boolean activateTool() {
    LinkedList<RendererElement> rendererElements = new LinkedList<RendererElement>();
    rendererElements.add(new RendererWireframeSelection(wireframeRendererUpdateLink));
    rendererElements.add(new RendererBoundaryField(boundaryFieldRendererUpdateLink));
    rendererElements.add(new RendererSolidSelection(solidSelectionRendererUpdateLink));
    speedyToolRenderers.setRenderers(rendererElements);
    iAmActive = true;
    return true;
  }

  /** The user has unequipped this tool:
   * (1) if generating a selection, stop
   * (2) ungrab if dragging a selection
   * (3) if currently performing any actions or communications with the server, terminate them
   * The tool will not deactivate itself until the server has acknowledged abort.
   * @return true if deactivation complete, false if not yet ready to deactivate
   */
  @Override
  public boolean deactivateTool() {
    if (currentToolSelectionState == ToolSelectionStates.GENERATING_SELECTION) {
        currentToolSelectionState = ToolSelectionStates.NO_SELECTION;
    }
    if (cloneToolsNetworkClient.peekCurrentActionStatus() != CloneToolsNetworkClient.ActionStatus.NONE_PENDING
        && cloneToolsNetworkClient.peekCurrentActionStatus() != CloneToolsNetworkClient.ActionStatus.COMPLETED  ) {
      undoAction();
    }
    if (this.iAmBusy()) return false;

    speedyToolRenderers.setRenderers(null);
    iAmActive = false;
    return true;
  }

  /**
   * the various valid user inputs are:
   * While performing an action (eg selection creation, or selection copy):
   * (1) any left click = abort action.
   * Otherwise:
   * If there is no selection:
   * (1) long left hold = undo previous copy (if any)
   * (2) short right click = create selection
   * If there is a selection:
   * (1) long left hold = undo previous copy (if any)
   * (2) short left click = uncreate selection
   * (3) short right click with CTRL = mirror selection left-right
   * (4) short right click without CTRL = grab / ungrab selection to move it around
   * (5) long right hold = copy the selection to the current position
   * (6) long left hold = undo the previous copy
   * (7) CTRL + mousewheel = rotate selection

   * @param player
   * @param partialTick
   * @param userInput
   * @return
   */

  @Override
  public boolean processUserInput(EntityClientPlayerMP player, float partialTick, UserInput userInput) {
    if (!iAmActive) return false;

    final long MIN_UNDO_HOLD_DURATION_NS = SpeedyToolsOptions.getLongClickMinDurationNS(); // length of time to hold for undo
    final long MIN_PLACE_HOLD_DURATION_NS = SpeedyToolsOptions.getLongClickMinDurationNS(); // length of time to hold for action (place)
    final long MAX_SHORT_CLICK_DURATION_NS = SpeedyToolsOptions.getShortClickMaxDurationNS();  // maximum length of time for a "short" click

    checkInvariants();

    UserInput.InputEvent nextEvent;
    while (null != (nextEvent = userInput.poll())) {
      // if we are busy with an action - a short left click will abort current action
      if (cloneToolsNetworkClient.peekCurrentActionStatus() != CloneToolsNetworkClient.ActionStatus.NONE_PENDING) {
        if (nextEvent.eventType == UserInput.InputEventType.LEFT_CLICK_DOWN) {
          if (cloneToolsNetworkClient.peekCurrentActionStatus() != CloneToolsNetworkClient.ActionStatus.NONE_PENDING
                  && cloneToolsNetworkClient.peekCurrentActionStatus() != CloneToolsNetworkClient.ActionStatus.COMPLETED) {
            undoAction();
          }
        }

         // if we are busy with an undo - we can't interrupt
      } else if (cloneToolsNetworkClient.peekCurrentUndoStatus() != CloneToolsNetworkClient.ActionStatus.NONE_PENDING) {
        // do nothing

        // otherwise - is this an undo?
      } else if (nextEvent.eventType == UserInput.InputEventType.LEFT_CLICK_UP &&
              nextEvent.eventDuration >= MIN_UNDO_HOLD_DURATION_NS
          ) {
        undoAction();

        // otherwise - split up according to whether we have a selection or not
      } else {
        switch (currentToolSelectionState) {
          case NO_SELECTION: {
            if (nextEvent.eventType == UserInput.InputEventType.RIGHT_CLICK_UP &&
                    nextEvent.eventDuration <= MAX_SHORT_CLICK_DURATION_NS) {
              initiateSelectionCreation(player);
            }
            break;
          }
          case GENERATING_SELECTION: {
            if (nextEvent.eventType == UserInput.InputEventType.LEFT_CLICK_DOWN) {
              undoSelectionCreation();
            }
            return false;
          }
          case DISPLAYING_SELECTION: {
            processSelectionUserInput(player, partialTick, nextEvent);
            break;
          }
          default:
            assert false : "Illegal currentToolState: " + currentToolSelectionState;
        }
      }
    }

    // If we are in a receptive state for powerup, check if it should be started or status updated.  Otherwise stop it.
    // used for rendering display only; CLICK_UP is used for reacting to the event

    // undo action check
    long timeNow = System.nanoTime();
    if (this.iAmBusy()) {
      if (!leftClickPowerup.isIdle()) {
        leftClickPowerup.abort();
      }
    } else {
      long leftButtonHoldTime = userInput.leftButtonHoldTimeNS(timeNow);

      if (leftButtonHoldTime >= MAX_SHORT_CLICK_DURATION_NS) {
        if (leftClickPowerup.isIdle()) {
          leftClickPowerup.initiate(timeNow, timeNow + MIN_UNDO_HOLD_DURATION_NS, leftButtonHoldTime);
        }
        leftClickPowerup.updateHolddownTime(leftButtonHoldTime);
      } else if (leftButtonHoldTime < 0) { // released button
        if (!leftClickPowerup.isIdle()) {
          long releaseTime = timeNow + leftButtonHoldTime;
          leftClickPowerup.release(releaseTime);
        }
      }
    }

    if (this.iAmBusy() || currentToolSelectionState != ToolSelectionStates.DISPLAYING_SELECTION) {
      if (!rightClickPowerup.isIdle()) {
        rightClickPowerup.abort();
      }
    } else {
      long rightButtonHoldTime = userInput.rightButtonHoldTimeNS(timeNow);
      if (rightButtonHoldTime >= MAX_SHORT_CLICK_DURATION_NS) {
        if (rightClickPowerup.isIdle()) {
          rightClickPowerup.initiate(timeNow, timeNow + MIN_PLACE_HOLD_DURATION_NS, rightButtonHoldTime);
        }
        rightClickPowerup.updateHolddownTime(rightButtonHoldTime);
      } else if (rightButtonHoldTime < 0) { // released button
        if (!rightClickPowerup.isIdle()) {
          long releaseTime = timeNow + rightButtonHoldTime;
          rightClickPowerup.release(releaseTime);
        }
      }
    }

    return true;
  }

  /** Is the tool currently busy with something?
   * @return true if busy, false if not
   */
  private boolean iAmBusy()
  {
    if (currentToolSelectionState == ToolSelectionStates.GENERATING_SELECTION) {
      return true;
    }
    if (cloneToolsNetworkClient.peekCurrentActionStatus() != CloneToolsNetworkClient.ActionStatus.NONE_PENDING
        && cloneToolsNetworkClient.peekCurrentActionStatus() != CloneToolsNetworkClient.ActionStatus.COMPLETED) {
      return true;
    }
    if (cloneToolsNetworkClient.peekCurrentUndoStatus() != CloneToolsNetworkClient.ActionStatus.NONE_PENDING
            && cloneToolsNetworkClient.peekCurrentUndoStatus() != CloneToolsNetworkClient.ActionStatus.COMPLETED) {
      return true;
    }
    return false;
  }

  /** processes user input received while there is a solid selection
   * (1) short left click = uncreate selection
   * (2) short right click with CTRL = mirror selection left-right
   * (3) short right click without CTRL = grab / ungrab selection to move it around
   * (4) long right hold = copy the selection to the current position
   * (5) long left hold = undo the previous copy
   * (6) CTRL + mousewheel = rotate selection
   *
   * @param player
   * @param partialTick
   * @param inputEvent
   * @return
   */
  private boolean processSelectionUserInput(EntityClientPlayerMP player, float partialTick, UserInput.InputEvent inputEvent) {
    final long MIN_UNDO_HOLD_DURATION_NS = SpeedyToolsOptions.getLongClickMinDurationNS(); // length of time to hold for undo
    final long MIN_PLACE_HOLD_DURATION_NS = SpeedyToolsOptions.getLongClickMinDurationNS(); // length of time to hold for action (place)
    final long MAX_SHORT_CLICK_DURATION_NS = SpeedyToolsOptions.getShortClickMaxDurationNS();  // maximum length of time for a "short" click

    switch (inputEvent.eventType) {
      case LEFT_CLICK_UP: {
        if (inputEvent.eventDuration <= MAX_SHORT_CLICK_DURATION_NS) {
          undoSelectionCreation();
        } else if (inputEvent.eventDuration >= MIN_UNDO_HOLD_DURATION_NS) {
          undoAction();
        }
        break;
      }
      case RIGHT_CLICK_UP: {
        if (inputEvent.eventDuration <= MAX_SHORT_CLICK_DURATION_NS) {
          if (inputEvent.controlKeyDown) {
            flipSelection();
          } else { // toggle selection grabbing
            Vec3 playerPosition = player.getPosition(partialTick);  // beware, Vec3 is short-lived
            selectionGrabActivated = !selectionGrabActivated;
            if (selectionGrabActivated) {
              selectionMovedFastYet = false;
              selectionGrabPoint = Vec3.createVectorHelper(playerPosition.xCoord, playerPosition.yCoord, playerPosition.zCoord);
            } else {
              Vec3 distanceMoved = selectionGrabPoint.subtract(playerPosition);
              selectionOrigin.posX += (int)Math.round(distanceMoved.xCoord);
              selectionOrigin.posY += (int)Math.round(distanceMoved.yCoord);
              selectionOrigin.posZ += (int)Math.round(distanceMoved.zCoord);
            }
          }
        } else if (inputEvent.eventDuration >= MIN_PLACE_HOLD_DURATION_NS) {
          placeSelection();
        }
        break;
      }
      case WHEEL_MOVE: {
        rotateSelection();
        break;
      }
    }
    return true;
  }

  /**
   * (1) Selects the first Block that will be affected by the tool when the player presses right-click,
   * (2) Determines which algorithm will be used to select blocks (see below), and
   * (3) creates a wireframe highlight showing up to MAX_NUMBER_OF_HIGHLIGHTED_BLOCKS of the blocks that will be selected
   * 1) no boundary field: cursor pointing at a block - floodfill (all non-air blocks, including diagonal fill) from block clicked up
   * 2) boundary field: standing outside, cursor pointing at one of the boundaries - all solid blocks in the field
   * 3) boundary field: standing inside - cursor pointing at a block - floodfill to boundary field
   * So the selection algorithm is:
   * a) if the player is pointing at a block, specify that, and also check if inside a boundary field; else
   * b) check the player is pointing at a side of the boundary field (from outside)
   *
   * @param player the player
   * @param partialTick partial tick time.
   */
//  @Override
//  public void highlightBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, float partialTick)

  @Override
  public boolean update(World world, EntityClientPlayerMP player, float partialTick)
  {
    checkInvariants();
    if (currentToolSelectionState != ToolSelectionStates.NO_SELECTION) return false;
    updateBoundaryCornersFromToolBoundary();

    MovingObjectPosition target = itemSpeedyCopy.rayTraceLineOfSight(player.worldObj, player);

    final int MAX_NUMBER_OF_HIGHLIGHTED_BLOCKS = 64;
    blockUnderCursor = null;
    highlightedBlocks = null;
    currentHighlighting = SelectionType.NONE;

    if (target != null && target.typeOfHit == EnumMovingObjectType.TILE) {
      blockUnderCursor = new ChunkCoordinates(target.blockX, target.blockY, target.blockZ);
      boolean selectedBlockIsInsideBoundaryField = false;

      if (boundaryCorner1 != null && boundaryCorner2 != null) {
        if (   blockUnderCursor.posX >= boundaryCorner1.posX && blockUnderCursor.posX <= boundaryCorner2.posX
                && blockUnderCursor.posY >= boundaryCorner1.posY && blockUnderCursor.posY <= boundaryCorner2.posY
                && blockUnderCursor.posZ >= boundaryCorner1.posZ && blockUnderCursor.posZ <= boundaryCorner2.posZ ) {
          selectedBlockIsInsideBoundaryField = true;
        }
      }

      if (selectedBlockIsInsideBoundaryField) {
        currentHighlighting = SelectionType.BOUND_FILL;
        highlightedBlocks = selectFill(target, player.worldObj, MAX_NUMBER_OF_HIGHLIGHTED_BLOCKS, true, true,
                boundaryCorner1.posX, boundaryCorner2.posX,
                boundaryCorner1.posY, boundaryCorner2.posY,
                boundaryCorner1.posZ, boundaryCorner2.posZ);
      } else {
        currentHighlighting = SelectionType.UNBOUND_FILL;
        highlightedBlocks = selectFill(target, player.worldObj, MAX_NUMBER_OF_HIGHLIGHTED_BLOCKS, true, true,
                Integer.MIN_VALUE, Integer.MAX_VALUE,
                blockUnderCursor.posY, 255,
                Integer.MIN_VALUE, Integer.MAX_VALUE);
      }
      checkInvariants();
      return true;
    }

    // if there is no block selected, check whether the player is pointing at the boundary field from outside.
    if (boundaryCorner1 == null || boundaryCorner2 == null) return false;
    Vec3 playerPosition = player.getPosition(1.0F);
    if (   playerPosition.xCoord >= boundaryCorner1.posX && playerPosition.xCoord <= boundaryCorner2.posX +1
            && playerPosition.yCoord >= boundaryCorner1.posY && playerPosition.yCoord <= boundaryCorner2.posY +1
            && playerPosition.zCoord >= boundaryCorner1.posZ && playerPosition.zCoord <= boundaryCorner2.posZ +1) {
      return false;
    }
    MovingObjectPosition highlightedFace = boundaryFieldFaceSelection(player);
    if (highlightedFace == null) return false;
    currentHighlighting = SelectionType.FULL_BOX;
    checkInvariants();
    return true;
  }

  /**
   * If there is a current selection, destroy it.  No effect if waiting for the server to do something.
   */
  private void undoSelectionCreation()
  {
    if (cloneToolsNetworkClient.peekCurrentActionStatus() != CloneToolsNetworkClient.ActionStatus.NONE_PENDING
        || cloneToolsNetworkClient.peekCurrentUndoStatus() != CloneToolsNetworkClient.ActionStatus.NONE_PENDING    ) {
      return;
    }
    currentToolSelectionState = ToolSelectionStates.NO_SELECTION;
  }

  /**
   * if the player has previously performed an action, undo it.
   */
  private void undoAction()
  {
    checkInvariants();
    boolean success = cloneToolsNetworkClient.performToolUndo();

//        playSound(CustomSoundsHandler.BOUNDARY_UNPLACE, thePlayer);
  }

  /**
   * don't call if an action is already pending
   */
  private void placeSelection()
  {
    checkInvariants();
    boolean success = cloneToolsNetworkClient.performToolAction(itemSpeedyCopy.itemID,
            selectionOrigin.posX, selectionOrigin.posY, selectionOrigin.posZ,
            (byte) 0, false); // todo: implement rotation and flipped
  }

  private void flipSelection()
  {
    // todo: do something here!
  }

  private void rotateSelection()
  {
    // todo: do something here!
  }

  private void initiateSelectionCreation(EntityClientPlayerMP thePlayer)
  {
    switch (currentHighlighting) {
      case NONE: {
        return;
      }
      case FULL_BOX: {
        voxelSelectionManager = new BlockVoxelMultiSelector();
        voxelSelectionManager.selectAllInBoxStart(thePlayer.worldObj, boundaryCorner1, boundaryCorner2);
//        sortBoundaryFieldCorners();
        selectionOrigin = new ChunkCoordinates(boundaryCorner1);
        currentToolSelectionState = ToolSelectionStates.GENERATING_SELECTION;
//            playSound(CustomSoundsHandler.BOUNDARY_PLACE_1ST, thePlayer);
        break;
      }
      case UNBOUND_FILL: {
        break;
      }
      case BOUND_FILL: {
        break;
      }
    }
    cloneToolsNetworkClient.informSelectionMade();
  }

  /** called once per tick on the client side while the user is holding an ItemCloneTool
   * used to:
   * (1) background generation of a selection, if it has been initiated
   * (2) update the client status to the server
   */
  @Override
  public void performTick(World world)
  {
    checkInvariants();
    final long MAX_TIME_IN_NS = 20 * 1000 * 1000;
    if (currentToolSelectionState == ToolSelectionStates.GENERATING_SELECTION) {
      System.out.print("Vox start nano(ms) : " + System.nanoTime()/ 1000000);                                     //todo: remove
      actionPercentComplete = 100.0F * voxelSelectionManager.selectAllInBoxContinue(world, MAX_TIME_IN_NS);
      System.out.println(": end (ms) : " + System.nanoTime()/ 1000000);                                           //todo: remove

      if (actionPercentComplete < 0.0F) {
        if (voxelSelectionManager.isEmpty()) {
          currentToolSelectionState = ToolSelectionStates.NO_SELECTION;
        } else {
          voxelSelectionManager.createRenderList(world);
          currentToolSelectionState = ToolSelectionStates.DISPLAYING_SELECTION;
        }
      }
    }

    checkInvariants();
  }


  /**
   * copies the boundary field coordinates from the boundary tool, if a boundary is defined
   * @return true if a boundary field is defined, false otherwise
   */
  private boolean updateBoundaryCornersFromToolBoundary()
  {
    boundaryCorner1 = null;
    boundaryCorner2 = null;

    if (speedyToolBoundary == null) return false;
    ChunkCoordinates cnrMin = new ChunkCoordinates();
    ChunkCoordinates cnrMax = new ChunkCoordinates();

    boolean hasABoundaryField = speedyToolBoundary.copyBoundaryCorners(cnrMin, cnrMax);
    if (hasABoundaryField) {
      boundaryCorner1 = cnrMin;
      boundaryCorner2 = cnrMax;
    }
    return hasABoundaryField;
  }

  /**
   * This class is used to provide information to the Boundary Field Renderer when it needs it.
   * The information is taken from the reference to the SpeedyToolBoundary.
   */
  public class BoundaryFieldRendererUpdateLink implements RendererBoundaryField.BoundaryFieldRenderInfoUpdateLink
  {
    @Override
    public boolean refreshRenderInfo(RendererBoundaryField.BoundaryFieldRenderInfo infoToUpdate, Vec3 playerPosition)
    {
      if (!currentToolSelectionState.displayBoundaryField) return false;
      if (speedyToolBoundary == null) return false;
      AxisAlignedBB boundaryFieldAABB = speedyToolBoundary.getBoundaryField();
      if (boundaryFieldAABB == null) return false;
      infoToUpdate.boundaryCursorSide = -1;
      infoToUpdate.boundaryGrabActivated = false;
      infoToUpdate.boundaryGrabSide = -1;
      infoToUpdate.boundaryFieldAABB = boundaryFieldAABB;
      return true;
    }
  }

  /**
   * This class is used to provide information to the WireFrame Renderer for rendering the wireframe
   *   of highlighted blocks.
   * The Renderer calls refreshRenderInfo, which copies the relevant information from the tool.
   * Draws a wireframe selection, provided that not both of the corners have been placed yet
   */
  public class CopyToolWireframeRendererLink implements RendererWireframeSelection.WireframeRenderInfoUpdateLink
  {
    @Override
    public boolean refreshRenderInfo(RendererWireframeSelection.WireframeRenderInfo infoToUpdate)
    {
      if (   !currentToolSelectionState.displayWireframeHighlight
          || highlightedBlocks == null) {
        return false;
      }
      infoToUpdate.currentlySelectedBlocks = highlightedBlocks;
      return true;
    }
  }

  /**
   * This class passes the needed information for the rendering of the solid selection:
   *  - the voxelManager
   *  - the coordinates of the selectionOrigin after it has been dragged from its starting point
   */
  public class SolidSelectionRendererLink implements RendererSolidSelection.SolidSelectionRenderInfoUpdateLink
  {
    @Override
    public boolean refreshRenderInfo(RendererSolidSelection.SolidSelectionRenderInfo infoToUpdate, EntityPlayer player, float partialTick) {
      if (!currentToolSelectionState.displaySolidSelection) return false;
      final double THRESHOLD_SPEED_SQUARED_FOR_SNAP_GRID = 0.01;
      checkInvariants();

      Vec3 playerOrigin = player.getPosition(partialTick);

      double draggedSelectionOriginX = selectionOrigin.posX;
      double draggedSelectionOriginY = selectionOrigin.posY;
      double draggedSelectionOriginZ = selectionOrigin.posZ;
      if (selectionGrabActivated) {
        double currentSpeedSquared = player.motionX * player.motionX + player.motionY * player.motionY + player.motionZ * player.motionZ;
        if (currentSpeedSquared >= THRESHOLD_SPEED_SQUARED_FOR_SNAP_GRID) {
          selectionMovedFastYet = true;
        }
        final boolean snapToGridWhileMoving = selectionMovedFastYet && currentSpeedSquared <= THRESHOLD_SPEED_SQUARED_FOR_SNAP_GRID;

        Vec3 distanceMoved = selectionGrabPoint.subtract(playerOrigin);
        draggedSelectionOriginX += distanceMoved.xCoord;
        draggedSelectionOriginY += distanceMoved.yCoord;
        draggedSelectionOriginZ += distanceMoved.zCoord;
        if (snapToGridWhileMoving) {
          draggedSelectionOriginX = Math.round(draggedSelectionOriginX);
          draggedSelectionOriginY = Math.round(draggedSelectionOriginY);
          draggedSelectionOriginZ = Math.round(draggedSelectionOriginZ);
        }
      }
      infoToUpdate.blockVoxelMultiSelector = voxelSelectionManager;
      infoToUpdate.draggedSelectionOriginX = draggedSelectionOriginX;
      infoToUpdate.draggedSelectionOriginY = draggedSelectionOriginY;
      infoToUpdate.draggedSelectionOriginZ = draggedSelectionOriginZ;

      return true;
    }
  }

  private ItemSpeedyCopy itemSpeedyCopy;

  private void checkInvariants()
  {
    assert (    currentToolSelectionState != ToolSelectionStates.GENERATING_SELECTION || voxelSelectionManager != null);
    assert (   currentToolSelectionState != ToolSelectionStates.DISPLAYING_SELECTION
            || (voxelSelectionManager != null && selectionOrigin != null) );

    assert (    selectionGrabActivated == false || selectionGrabPoint != null);
  }


  // the Tool Selection can be in several states as given by currentToolState:
  // 1) NO_SELECTION
  //    highlightBlocks() is used to update some variables, based on what the player is looking at
  //      a) highlightedBlocks (block wire outline)
  //      b) currentHighlighting (what type of highlighting depending on whether there is a boundary field, whether
  //         the player is looking at a block or at a side of the boundary field
  //      c) blockUnderCursor (if looking at a block)
  //      d) boundaryCursorSide (if looking at the boundary field)
  //    voxelSelectionManager is not valid
  // 2) GENERATING_SELECTION - user has clicked to generate a selection
  //    a) actionInProgress gives the action being performed
  //    b) voxelSelectionManager has been created and initialised
  //    c) every tick, the voxelSelectionManager is updated further until complete
  // 3) DISPLAYING_SELECTION - selection is being displayed and/or moved
  //    voxelSelectionManager is valid and has a renderlist
  // combined with this are the various other actions that the tool can perform:
  //  1) transmitting selection to server
  //  2) performing an action / waiting for server
  //  3) performing an undo / waiting for server

  private SelectionType currentHighlighting = SelectionType.NONE;
  private List<ChunkCoordinates> highlightedBlocks;

  private float actionPercentComplete;
  private ToolSelectionStates currentToolSelectionState = ToolSelectionStates.NO_SELECTION;

  private BlockVoxelMultiSelector voxelSelectionManager;
  private ChunkCoordinates selectionOrigin;
  private boolean selectionGrabActivated = false;
  private Vec3    selectionGrabPoint = null;
  private boolean selectionMovedFastYet;

  private CloneToolsNetworkClient cloneToolsNetworkClient;

  private SpeedyToolBoundary speedyToolBoundary;   // used to retrieve the boundary field coordinates, if selected

  private enum SelectionType {
    NONE, FULL_BOX, BOUND_FILL, UNBOUND_FILL
  }

  private enum ToolSelectionStates  {
            NO_SELECTION( true, false,  true, false),
    GENERATING_SELECTION(false, false,  true,  true),
    DISPLAYING_SELECTION(false,  true, false, false),
    ;

    public final boolean displayWireframeHighlight;
    public final boolean        displaySolidSelection;
    public final boolean               displayBoundaryField;
    public final boolean                      performingAction;

    private ToolSelectionStates(boolean init_displayHighlight, boolean init_displaySelection, boolean init_displayBoundaryField, boolean init_performingAction)
    {
      displayWireframeHighlight = init_displayHighlight;
      displaySolidSelection = init_displaySelection;
      displayBoundaryField = init_displayBoundaryField;
      performingAction = init_performingAction;
    }
  }

  private RendererSolidSelection.SolidSelectionRenderInfoUpdateLink solidSelectionRendererUpdateLink;

  private PowerUpEffect leftClickPowerup = new PowerUpEffect();
  private PowerUpEffect rightClickPowerup = new PowerUpEffect();

  /**
   * Manages the state of a "Power up" object (eg for animation purposes)
   * - start of charging up, progress of charge, release of charge
   * Usage:
   * (1) initiate() to begin the powerup; supply the current time and the duration of the powerup
   *     The first call to pollState() will return INITIATING, subsequent calls return POWERINGUP
   * (2) updateHolddownTime periodically, to inform as the charge builds up
   *     The first call to pollState() will return RELEASING, subsequent calls return IDLE
   * (3) release() to stop the powerup
   * The state of the powerup can be read:
   * (1) The current state is available using peekState or pollState.
   * (2) The current time in a state from getTimeSpentInThisState
   * (3) the percentage completion (0 - 100) from getPercentComplete
   */

  private static class PowerUpEffect {
    public enum State {
        IDLE, INITIATING, POWERINGUP, RELEASING     // INITIATING and RELEASING are transient, to inform the caller that the state has changed.
     }

    /** start the powerup
     * @param new_initiationTime the time (in ns) when the powerup started
     * @param new_completionTime the time (in ns) when the powerup will be complete
     * @param new_initialHoldDuration the length of time (in ns) that the button had been held down, when the powerup started
     */
    public void initiate(long new_initiationTime, long new_completionTime, long new_initialHoldDuration) {
      initiationTime = new_initiationTime;
      expectedCompletionTime = new_completionTime;
      assert (expectedCompletionTime > initiationTime);
      assert (initialHoldDuration >= 0);
      initialHoldDuration = new_initialHoldDuration;
      currentHoldDuration = 0;
      state = State.INITIATING;
    }

    public void release(long new_releaseTime) {
      releaseTime = new_releaseTime;
      state = State.RELEASING;
    }
    public void abort() {
      releaseTime = initiationTime;
      state = State.RELEASING;
    }

    public void updateHolddownTime(long lengthOfHold) {
      if (state != State.INITIATING && state != State.POWERINGUP) return;
      if (lengthOfHold < initialHoldDuration) {
        initialHoldDuration = currentHoldDuration;  // just in case we miss a fast click
      }
      currentHoldDuration = lengthOfHold - initialHoldDuration;
    }

    /** returns the current state without affecting it
     * @return
     */
    public State peekState() {return state;}

    /** returns the current state and advances to the next state as appropriate
     * @return
     */
    public State pollState() {
      State retval = state;
      if (state == State.INITIATING) state = State.POWERINGUP;
      if (state == State.RELEASING) state = State.IDLE;
      return retval;
    }

    public boolean isIdle(){return (state == State.IDLE || state == State.RELEASING);}

    /** returns the time spent in this state
      * @param timeNow the current time in ns
     * @return duration in ns, or 0 if not available.
     */
    public long getTimeSpentInThisState(long timeNow) {
      switch (state) {
        case RELEASING:
        case IDLE: {
          if (releaseTime == 0) return 0;
          assert (timeNow > releaseTime);
          return (timeNow - releaseTime);
        }
        case INITIATING:
        case POWERINGUP: {
          assert (initiationTime != 0 && timeNow > initiationTime);

          return (timeNow - initiationTime);
        }
        default: assert false: "Invalid state:" + state;
      }
      return 0;
    }

    /**
     * returns the percentage of the powerup completed; either the current one (if currently powering up) or the most-recently-completed one.
     * @return
     */
    public double getPercentCompleted() {
      switch (state) {
        case RELEASING:
        case IDLE: {
          if (releaseTime == 0) return 0;
          double fraction = (releaseTime - initiationTime);
          fraction /= (expectedCompletionTime - initiationTime);
          fraction *= 100.0;
          assert (fraction >= 0.0 && fraction <= 100.0);
          return fraction;
        }
        case INITIATING:
        case POWERINGUP: {
          if (currentHoldDuration == 0) return 0;
          double fraction = currentHoldDuration - initialHoldDuration;
          fraction /= (expectedCompletionTime - initiationTime);
          fraction *= 100.0;
          assert (fraction >= 0.0 && fraction <= 100.0);
          return fraction;
        }
        default: assert false: "Invalid state:" + state;
      }
      return 0.0;
    }

    private State state;
    private long initiationTime = 0;
    private long initialHoldDuration = 0;
    private long currentHoldDuration = 0;
    private long expectedCompletionTime = 0;
    private long releaseTime = 0;
  }


}
