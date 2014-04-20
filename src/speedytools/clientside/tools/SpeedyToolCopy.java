package speedytools.clientside.tools;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraft.world.World;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.rendering.*;
import speedytools.clientside.selections.BlockVoxelMultiSelector;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.items.ItemSpeedyCopy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
    solidSelectionRenderInfoUpdateLink = this.new SolidSelectionRendererLink();
  }

  @Override
  public boolean activateTool() {
    LinkedList<RendererElement> rendererElements = new LinkedList<RendererElement>();
    rendererElements.add(new RendererWireframeSelection(wireframeRendererUpdateLink));
    rendererElements.add(new RendererBoundaryField(boundaryFieldRendererUpdateLink));
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

  @Override
  public boolean processUserInput(EntityClientPlayerMP player, float partialTick, UserInput userInput) {
    if (!iAmActive) return false;

    controlKeyIsDown = userInput.isControlKeyDown();

    UserInput.InputEvent nextEvent;
    while (null != (nextEvent = userInput.poll())) {
      switch (nextEvent.eventType) {
        case LEFT_CLICK_DOWN: {
          boundaryCorner1 = null;
          boundaryCorner2 = null;
          speedyToolSounds.playSound(SpeedySoundTypes.BOUNDARY_UNPLACE, player.getPosition(partialTick));
          break;
        }
        case RIGHT_CLICK_DOWN: {
          doRightClick(player, partialTick);
          break;
        }
      }
    }
    return true;
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
   * Update the selection or boundary field based on where the player is looking
   * @param world
   * @param player
   * @param partialTick
   * @return
   */
  @Override
  public boolean update(World world, EntityClientPlayerMP player, float partialTick) {
    // update icon renderer


    return true;
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
   * This class is used to provide information to the WireFrame Renderer when it needs it:
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
//      checkInvariants();

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

  // the Tool can be in several states as given by currentToolState:
  // 1) NO_SELECTION
  //    highlightBlocks() is used to update some variables, based on what the player is looking at
  //      a) highlightedBlocks (block wire outline)
  //      b) currentHighlighting (what type of highlighting depending on whether there is a boundary field, whether
  //         the player is looking at a block or at a side of the boundary field
  //      c) currentlySelectedBlock (if looking at a block)
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

  private long lastRightClickTime;                                            // used for double-click detection
  private Long lastLeftClickTime = null;                                      // used for double-click detection

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

  private RendererSolidSelection.SolidSelectionRenderInfoUpdateLink solidSelectionRenderInfoUpdateLink;

}
