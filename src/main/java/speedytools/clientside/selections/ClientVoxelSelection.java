package speedytools.clientside.selections;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import speedytools.clientside.network.CloneToolsNetworkClient;
import speedytools.clientside.tools.SelectionPacketSender;
import speedytools.common.SpeedyToolsOptions;
import speedytools.common.network.ClientStatus;
import speedytools.common.selections.BlockVoxelMultiSelector;
import speedytools.common.utilities.QuadOrientation;
import speedytools.common.utilities.ResultWithReason;

/**
 * Created by TheGreyGhost on 26/09/14.
 * Used by the client to track the current tool voxel selection and coordinate any updates with the server
 */
public class ClientVoxelSelection
{

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

  public ResultWithReason createFullBoxSelection(EntityClientPlayerMP thePlayer, ChunkCoordinates boundaryCorner1, ChunkCoordinates boundaryCorner2)
  {

  }

  public ResultWithReason createFillSelection(EntityClientPlayerMP thePlayer, ChunkCoordinates fillStartingBlock, ChunkCoordinates boundaryCorner1, ChunkCoordinates boundaryCorner2)
  {

  }

  private void initiateSelectionCreation(EntityClientPlayerMP thePlayer, )
  {       // todo blocks which aren't loaded are empty!
    switch (currentHighlighting) {
      case NONE: {
        if (updateBoundaryCornersFromToolBoundary()) {
          displayNewErrorMessage("First point your cursor at a nearby block, or at the boundary field ...");
        } else {
          displayNewErrorMessage("First point your cursor at a nearby block...");
        }
        return;
      }
      case FULL_BOX: {
        voxelSelectionManager = new BlockVoxelMultiSelector();
        voxelSelectionManager.selectAllInBoxStart(thePlayer.worldObj, boundaryCorner1, boundaryCorner2);
//        selectionOrigin = new ChunkCoordinates(boundaryCorner1);
        currentToolSelectionState = ToolSelectionStates.GENERATING_SELECTION;
        selectionGenerationState = SelectionGenerationState.VOXELS;
        selectionGenerationPercentComplete = 0;
        break;
      }
      case UNBOUND_FILL: {
        voxelSelectionManager = new BlockVoxelMultiSelector();
        voxelSelectionManager.selectUnboundFillStart(thePlayer.worldObj, blockUnderCursor);
//        selectionOrigin = new ChunkCoordinates(blockUnderCursor);
        currentToolSelectionState = ToolSelectionStates.GENERATING_SELECTION;
        selectionGenerationState = SelectionGenerationState.VOXELS;
        selectionGenerationPercentComplete = 0;
        break;
      }
      case BOUND_FILL: {
        voxelSelectionManager = new BlockVoxelMultiSelector();
        voxelSelectionManager.selectBoundFillStart(thePlayer.worldObj, blockUnderCursor, boundaryCorner1, boundaryCorner2);
//        selectionOrigin = new ChunkCoordinates(blockUnderCursor);
        currentToolSelectionState = ToolSelectionStates.GENERATING_SELECTION;
        selectionGenerationState = SelectionGenerationState.VOXELS;
        selectionGenerationPercentComplete = 0;
        break;
      }
    }
  }


  /** called once per tick on the client side while the user is holding an ItemCloneTool
   * used to:
   * (1) background generation of a selection, if it has been initiated
   * (2) start transmission of the selection, if it has just been completed
   * (3) acknowledge (get) the action and undo statuses
   */
  @Override
  public void performTick(World world) {
    checkInvariants();
    super.performTick(world);
    updateGrabRenderTick(selectionGrabActivated && currentToolSelectionState == ToolSelectionStates.DISPLAYING_SELECTION);

    final long MAX_TIME_IN_NS = SpeedyToolsOptions.getMaxClientBusyTimeMS() * 1000L * 1000L;
    final float VOXEL_MAX_COMPLETION = 75.0F;
    if (currentToolSelectionState == ToolSelectionStates.GENERATING_SELECTION) {
      switch (selectionGenerationState) {
        case VOXELS: {
          float progress = voxelSelectionManager.continueSelectionGeneration(world, MAX_TIME_IN_NS);
          if (progress >= 0) {
            selectionGenerationPercentComplete = VOXEL_MAX_COMPLETION * progress;
          } else {
            voxelCompletionReached = selectionGenerationPercentComplete;
            selectionGenerationState = SelectionGenerationState.RENDERLISTS;
            selectionPacketSender.reset();
            if (voxelSelectionManager.isEmpty()) {
              currentToolSelectionState = ToolSelectionStates.NO_SELECTION;
            } else {
              selectionOrigin = voxelSelectionManager.getWorldOrigin();
              if (voxelSelectionRenderer == null) {
                voxelSelectionRenderer = new BlockVoxelMultiSelectorRenderer();
              }
              ChunkCoordinates wOrigin = voxelSelectionManager.getWorldOrigin();
              voxelSelectionRenderer.createRenderListStart(world, wOrigin.posX, wOrigin.posY, wOrigin.posZ,
                      voxelSelectionManager.getSelection(), voxelSelectionManager.getUnavailableVoxels());
            }
          }
          break;
        }
        case RENDERLISTS: {
          ChunkCoordinates wOrigin = voxelSelectionManager.getWorldOrigin();
          float progress = voxelSelectionRenderer.createRenderListContinue(world, wOrigin.posX, wOrigin.posY, wOrigin.posZ,
                  voxelSelectionManager.getSelection(), voxelSelectionManager.getUnavailableVoxels(), MAX_TIME_IN_NS);

          if (progress >= 0) {
            selectionGenerationPercentComplete = voxelCompletionReached + (100.0F - voxelCompletionReached) * progress;
          } else {
            currentToolSelectionState = ToolSelectionStates.DISPLAYING_SELECTION;
            selectionGenerationState = SelectionGenerationState.IDLE;
            selectionOrientation = new QuadOrientation(0, 0, voxelSelectionManager.getSelection().getxSize(), voxelSelectionManager.getSelection().getzSize());
            hasBeenMoved = false;
            selectionGrabActivated = false;
          }
          break;
        }
        default: assert false : "Invalid selectionGenerationState:" + selectionGenerationState;
      }
    }

    if (currentToolSelectionState == ToolSelectionStates.NO_SELECTION) {
      selectionPacketSender.reset();
    }

    // if the selection has been freshly generated, keep trying to transmit it until we successfully start transmission
    if (currentToolSelectionState == ToolSelectionStates.DISPLAYING_SELECTION
            && selectionPacketSender.getCurrentPacketProgress() == SelectionPacketSender.PacketProgress.IDLE) {
      selectionPacketSender.startSendingSelection(voxelSelectionManager);
    }
    selectionPacketSender.tick();

    CloneToolsNetworkClient.ActionStatus actionStatus = cloneToolsNetworkClient.getCurrentActionStatus();
    CloneToolsNetworkClient.ActionStatus undoStatus = cloneToolsNetworkClient.getCurrentUndoStatus();

    if (undoStatus == CloneToolsNetworkClient.ActionStatus.COMPLETED) {
      lastActionWasRejected = false;
      toolState = ToolState.UNDO_SUCCEEDED;
      cloneToolsNetworkClient.changeClientStatus(ClientStatus.MONITORING_STATUS);
    }
    if (undoStatus == CloneToolsNetworkClient.ActionStatus.REJECTED) {
      lastActionWasRejected = true;
      toolState = ToolState.UNDO_FAILED;
      displayNewErrorMessage(cloneToolsNetworkClient.getLastRejectionReason());
      cloneToolsNetworkClient.changeClientStatus(ClientStatus.MONITORING_STATUS);
    }

    if (undoStatus == CloneToolsNetworkClient.ActionStatus.NONE_PENDING) { // ignore action statuses if undo status is not idle, since we are undoing the current action
      if (actionStatus == CloneToolsNetworkClient.ActionStatus.COMPLETED) {
        lastActionWasRejected = false;
        toolState = ToolState.ACTION_SUCCEEDED;
        cloneToolsNetworkClient.changeClientStatus(ClientStatus.MONITORING_STATUS);
        hasBeenMoved = false;
      }
      if (actionStatus == CloneToolsNetworkClient.ActionStatus.REJECTED) {
        lastActionWasRejected = true;
        toolState = ToolState.ACTION_FAILED;
        displayNewErrorMessage(cloneToolsNetworkClient.getLastRejectionReason());
        cloneToolsNetworkClient.changeClientStatus(ClientStatus.MONITORING_STATUS);
      }
    }

    checkInvariants();
  }


  private enum SelectionGenerationState {IDLE, VOXELS, RENDERLISTS};
  SelectionGenerationState selectionGenerationState = SelectionGenerationState.IDLE;


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

  private SelectionPacketSender selectionPacketSender;

  private BlockVoxelMultiSelector voxelSelectionManager;
  private BlockVoxelMultiSelectorRenderer voxelSelectionRenderer;

}
