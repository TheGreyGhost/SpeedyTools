package speedytools.clientside.tools;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import speedytools.clientside.UndoManagerClient;
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
 */
public class SpeedyToolCopy extends SpeedyToolClonerBase
{
  public SpeedyToolCopy(ItemSpeedyCopy i_parentItem, SpeedyToolRenderers i_renderers, SpeedyToolSounds i_speedyToolSounds, UndoManagerClient i_undoManagerClient,
                        SpeedyToolBoundary i_speedyToolBoundary
                        ) {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient);
    itemSpeedyCopy = i_parentItem;
    speedyToolBoundary = i_speedyToolBoundary;
    boundaryFieldRendererUpdateLink = this.new BoundaryFieldRendererUpdateLink();
    wireframeRendererUpdateLink = this.new CopyToolWireframeRendererLink();
    solidSelectionRendererUpdateLink = this.new SolidSelectionRendererLink();
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

  /** The user has unequipped this tool, deactivate it, stop any effects, etc
   * @return
   */
  @Override
  public boolean deactivateTool() {
    speedyToolRenderers.setRenderers(null);
    iAmActive = false;
    return true;
  }

  /**
   * user inputs are:
   * (1) long left hold = undo previous copy (if any)
   * Also -
   * While there is no selection:
   * (1) short right click = create selection
   * While there is a selection:
   * (1) short left click = uncreate selection
   * (2) short right click with CTRL = mirror selection left-right
   * (3) short right click without CTRL = grab / ungrab selection to move it around
   * (4) long right hold = copy the selection to the current position
   * (5) long left hold = undo the previous copy
   * (6) CTRL + mousewheel = rotate selection

   * @param player
   * @param partialTick
   * @param userInput
   * @return
   */

  @Override
  public boolean processUserInput(EntityClientPlayerMP player, float partialTick, UserInput userInput) {
    if (!iAmActive) return false;

    final long MIN_UNDO_HOLD_DURATION_NS = SpeedyToolsOptions.getLongClickMinDurationNS(); // length of time to hold for undo
    final long MAX_SHORT_CLICK_DURATION_NS = SpeedyToolsOptions.getShortClickMaxDurationNS();  // ,maximum length of time for a "short" click

    checkInvariants();

    controlKeyIsDown = userInput.isControlKeyDown();

    UserInput.InputEvent nextEvent;

    if (currentToolState.performingAction) {
      userInput.reset();
      return false;
    }

    // update button hold times - used for rendering display only; CLICK_UP is used for reacting to the event
    long timeNow = System.nanoTime();
    leftButtonHoldTime = userInput.leftButtonHoldTimeNS(timeNow);
    rightButtonHoldTime = userInput.rightButtonHoldTimeNS(timeNow);

    if (userInput.leftButtonHoldTimeNS(timeNow) >= MAX_SHORT_CLICK_DURATION_NS) {
    }
    if (!leftButtonBeingHeld && userInput.leftButtonHoldTimeNS(timeNow) >= MAX_SHORT_CLICK_DURATION_NS) { // user has started a new long left hold
      leftButtonBeingHeld = true;

    }
    if (!rightButtonBeingHeld && ) >= MAX_SHORT_CLICK_DURATION_NS) { // user has started a new long right hold


    while (null != (nextEvent = userInput.poll())) {
      if (nextEvent.eventType == UserInput.InputEventType.LEFT_CLICK_UP &&
              nextEvent.eventDuration >= MIN_UNDO_HOLD_DURATION_NS
          ) {
        undoAction(player);
      } else {
        switch (currentToolState) {
          case NO_SELECTION: {
            if (nextEvent.eventType == UserInput.InputEventType.RIGHT_CLICK_UP &&
                    nextEvent.eventDuration <= MAX_SHORT_CLICK_DURATION_NS) {
              initiateSelectionCreation(player);
            }
            break;
          }
          case GENERATING_SELECTION: {
            userInput.reset(); // do nothing while selection generation in place
            return false;
          }
          case DISPLAYING_SELECTION: {
            processSelectionUserInput(player, partialTick, nextEvent);
            break;
          }
          default:
            assert false : "Illegal currentToolState: " + currentToolState;
        }
      }
    }
    return true;

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
  private boolean processSelectionUserInput(EntityClientPlayerMP player, float partialTick, UserInput.InputEvent inputEvent)
  {
    switch (inputEvent.eventType) {
      case LEFT_CLICK_UP: {
        break;
      }
      case RIGHT_CLICK_UP: {
        break;
      }
      case


    }
  }

  public void buttonClicked(EntityClientPlayerMP thePlayer, int whichButton)
  {
    final long DOUBLE_CLICK_SPEED_NS = SpeedyToolsOptions.getDoubleClickSpeedMS() * 1000 * 1000;
    checkInvariants();

    switch (whichButton) {
      case 0: {
        if (lastLeftClickTime == null) {
          lastLeftClickTime = System.nanoTime();  // .tick() will act on this after the double click time has elapsed
        } else {
          long clickElapsedTime = System.nanoTime() - lastLeftClickTime;
          lastLeftClickTime = null;
          if (clickElapsedTime < DOUBLE_CLICK_SPEED_NS) {
            undoAction(thePlayer);
          } else {
            undoSelection(thePlayer);    // the double-click time has elapsed before the second click but the tick() didn't occur yet
          }
        }
        break;
      }
      case 1: {
        if (currentToolState.performingAction) return;
        switch (currentToolState) {
          case NO_SELECTION: {

            break;
          }
          case DISPLAYING_SELECTION: {
            long clickElapsedTime = System.nanoTime() - lastRightClickTime;
            lastRightClickTime = System.nanoTime();

            boolean controlKeyDown =  Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
            if (controlKeyDown) {
              flipSelection();
            } else {
              if (clickElapsedTime < DOUBLE_CLICK_SPEED_NS) {
                placeSelection(thePlayer);
              } else {
                Vec3 playerPosition = thePlayer.getPosition(1.0F);  // beware, Vec3 is short-lived
                selectionGrabActivated = true;
                selectionMovedFastYet = false;
                selectionGrabPoint = Vec3.createVectorHelper(playerPosition.xCoord, playerPosition.yCoord, playerPosition.zCoord);
              }
            }
          }
        }
        break;
      }
      default: {     // should never happen- if it does, ignore it
        break;
      }
    }

    checkInvariants();
    return;
  }

  /**
   * place corner blocks; or if already both placed - grab / ungrab one of the faces.
   * @param player
   * @param partialTick
   */
  protected void doRightClick(EntityClientPlayerMP player, float partialTick)
  {

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
    if (currentToolState != ToolState.NO_SELECTION) return false;
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
   * copies the boundary field coordinates from the boundary tool, if a boundary is defined
   * @return true if a boundary field is defined, false otherwise
   */
  private boolean updateBoundaryCornersFromToolBoundary()
  {
    boundaryCorner1 = null;
    boundaryCorner2 = null;

    if (speedyToolBoundary == null) return false;

    boolean hasABoundaryField = speedyToolBoundary.copyBoundaryCorners(boundaryCorner1, boundaryCorner2);
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
      if (!currentToolState.displayBoundaryField) return false;
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
      if (!currentToolState.displayWireframeHighlight) return false;
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
      if (!currentToolState.displaySolidSelection) return false;
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
    assert (    currentToolState != ToolState.GENERATING_SELECTION || voxelSelectionManager != null);
    assert (   currentToolState != ToolState.DISPLAYING_SELECTION
            || (voxelSelectionManager != null && selectionOrigin != null) );

    assert (    selectionGrabActivated == false || selectionGrabPoint != null);
  }


  // the Tool can be in several states as given by currentToolState:
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

  private SelectionType currentHighlighting = SelectionType.NONE;
  private List<ChunkCoordinates> highlightedBlocks;

  private float actionPercentComplete;
  private ToolState currentToolState = ToolState.NO_SELECTION;

  private BlockVoxelMultiSelector voxelSelectionManager;
  private ChunkCoordinates selectionOrigin;
  private boolean selectionGrabActivated = false;
  private Vec3    selectionGrabPoint = null;
  private boolean selectionMovedFastYet;

  private boolean leftButtonBeingHeld = false;
  private long    leftButtonHoldTime;
  private boolean rightButtonBeingHeld = false;
  private long    rightButtonHoldTime;

  private SpeedyToolBoundary speedyToolBoundary;   // used to retrieve the boundary field coordinates, if selected

  private enum SelectionType {
    NONE, FULL_BOX, BOUND_FILL, UNBOUND_FILL
  }

  private enum ToolState {
            NO_SELECTION( true, false,  true, false),
    GENERATING_SELECTION(false, false,  true,  true),
    DISPLAYING_SELECTION(false,  true, false, false);

    public final boolean displayWireframeHighlight;
    public final boolean        displaySolidSelection;
    public final boolean               displayBoundaryField;
    public final boolean                      performingAction;

    private ToolState(boolean init_displayHighlight, boolean init_displaySelection, boolean init_displayBoundaryField, boolean init_performingAction)
    {
      displayWireframeHighlight = init_displayHighlight;
      displaySolidSelection = init_displaySelection;
      displayBoundaryField = init_displayBoundaryField;
      performingAction = init_performingAction;
    }
  }

  private RendererSolidSelection.SolidSelectionRenderInfoUpdateLink solidSelectionRendererUpdateLink;

}
