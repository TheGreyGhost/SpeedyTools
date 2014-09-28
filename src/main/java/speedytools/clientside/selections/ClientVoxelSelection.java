package speedytools.clientside.selections;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import speedytools.clientside.network.PacketSenderClient;
import speedytools.clientside.tools.SelectionPacketSender;
import speedytools.common.network.Packet250ServerSelectionGeneration;
import speedytools.common.selections.BlockVoxelMultiSelector;
import speedytools.common.utilities.ErrorLog;
import speedytools.common.utilities.QuadOrientation;
import speedytools.common.utilities.ResultWithReason;

/**
 * Created by TheGreyGhost on 26/09/14.
 * Used by the client to track the current tool voxel selection and coordinate any updates with the server
 */
public class ClientVoxelSelection
{
  public ClientVoxelSelection(SelectionPacketSender i_selectionPacketSender, PacketSenderClient i_packetSenderClient)
  {
    selectionPacketSender = i_selectionPacketSender;
    packetSenderClient = i_packetSenderClient;
  }

  private SelectionPacketSender selectionPacketSender;
  private PacketSenderClient packetSenderClient;

  private enum ClientSelectionState {IDLE, GENERATING, CREATING_RENDERLISTS, COMPLETE}
  private enum OutgoingTransmissionState {IDLE, SENDING, COMPLETE}
  private enum ServerSelectionState {IDLE, GENERATING, RECEIVING, CREATING_RENDERLISTS, COMPLETE}

  public enum VoxelSelectionState {NO_SELECTION, GENERATING, READY_FOR_DISPLAY}

  private ClientSelectionState clientSelectionState = ClientSelectionState.IDLE;
  private OutgoingTransmissionState outgoingTransmissionState = OutgoingTransmissionState.IDLE;
  private ServerSelectionState serverSelectionState = ServerSelectionState.IDLE;

  /**
   * find out whether the voxelselection is ready for display yet
   * @return
   */
  public VoxelSelectionState getReadinessForDisplaying()
  {
    assert checkInvariants();
    switch (clientSelectionState) {
      case IDLE: return VoxelSelectionState.NO_SELECTION;
      case COMPLETE: {
        return VoxelSelectionState.READY_FOR_DISPLAY;
      }
      case GENERATING:
      case CREATING_RENDERLISTS: {
        return VoxelSelectionState.GENERATING;
      }
      default: {
        ErrorLog.defaultLog().severe("Invalid clientSelectionState " + clientSelectionState + " in " + this.getClass().getName());
        return VoxelSelectionState.NO_SELECTION;
      }
    }
  }

  /**
   * returns true if the server has a complete copy of the selection, ready for a tool action
   * @return
   */
  public boolean isSelectionCompleteOnServer()
  {
    if (clientSelectionState != ClientSelectionState.COMPLETE) return false;
    if (selectionPacketSender.getCurrentPacketProgress() != SelectionPacketSender.PacketProgress.COMPLETED) return false;
    return true;
  }

  /**
   * returns the progress of the selection generation on (or transmission to ) server
   * @return [0 .. 1] for the estimated fractional completion of the voxel selection generation / transmission
   *         returns 0 if not started yet, returns 1.000F if complete
   */
  public float getServerSelectionFractionComplete()
  {
    if (clientSelectionState != ClientSelectionState.COMPLETE) return 0.0F;
    if (selectionPacketSender.getCurrentPacketProgress() == SelectionPacketSender.PacketProgress.COMPLETED) return 1000.F;
    return selectionPacketSender.getCurrentPacketPercentComplete() / 100.0F;
  }

  /**
   * If the voxel selection is currently being generated, return the fraction complete
   * @return [0 .. 1] for the estimated fractional completion of the voxel selection generation
   *         returns 0 if not started yet, returns 1.000F if complete
   */
  public float getGenerationFractionComplete()
  {
    final float SELECTION_GENERATION_FULL = 0.75F;

    assert checkInvariants();
    switch (clientSelectionState) {
      case IDLE: return 0.0F;
      case COMPLETE: return 1.000F;
      case GENERATING: {
        return selectionGenerationFractionComplete * SELECTION_GENERATION_FULL;
      }
      case CREATING_RENDERLISTS: {
        float scaledProgress = SELECTION_GENERATION_FULL * selectionGenerationFractionComplete;
        return scaledProgress + (1.0F - scaledProgress) * renderlistGenerationFractionComplete;
      }
      default: {
        ErrorLog.defaultLog().severe("Invalid clientSelectionState " + clientSelectionState + " in " + this.getClass().getName());
        return 0.0F;
      }
    }
  }

  /**
   * test this class to see if all its invariants are still valid
   * @return true if all invariants are fulfilled
   */
  private boolean checkInvariants()
  {
    if (clientSelectionState == ClientSelectionState.IDLE && serverSelectionState != ServerSelectionState.IDLE) return false;
    if (clientSelectionState != ClientSelectionState.IDLE && clientVoxelSelection == null) return false;
    if ((clientSelectionState == ClientSelectionState.CREATING_RENDERLISTS ||
        clientSelectionState == ClientSelectionState.COMPLETE) && voxelSelectionRenderer == null) return false;
    return true;
  }

  /**
   * If there is a current selection, destroy it.  No effect if waiting for the server to do something.
   */
  public void reset()
  {
    clientVoxelSelection = null;
    if (voxelSelectionRenderer != null) {
      voxelSelectionRenderer.release();
      voxelSelectionRenderer = null;
    }
    clientSelectionState = ClientSelectionState.IDLE;
    if (serverSelectionState != ServerSelectionState.IDLE) {
      Packet250ServerSelectionGeneration abortPacket = Packet250ServerSelectionGeneration.abortSelectionGeneration();
      packetSenderClient.sendPacket(abortPacket);
      serverSelectionState = ServerSelectionState.IDLE;
    }

    //todo abort any selection transmission to the server
    outgoingTransmissionState = OutgoingTransmissionState.IDLE;
  }

  /**
   * If selection generation is underway, abort it.  If already complete, do nothing.
   */
  public void abortGenerationIfUnderway()
  {
    if (clientSelectionState == ClientSelectionState.GENERATING || clientSelectionState == ClientSelectionState.CREATING_RENDERLISTS) {
      reset();
    } else {
      if (serverSelectionState != ServerSelectionState.IDLE && serverSelectionState != ServerSelectionState.COMPLETE) {
        Packet250ServerSelectionGeneration abortPacket = Packet250ServerSelectionGeneration.abortSelectionGeneration();
        packetSenderClient.sendPacket(abortPacket);
        serverSelectionState = ServerSelectionState.IDLE;
      }
    }
  }

  private void initialiseForGeneration()
  {
    reset();
    selectionUpdatedFlag = false;
    clientSelectionState = ClientSelectionState.GENERATING;
    selectionGenerationFractionComplete = 0;
    renderlistGenerationFractionComplete = 0;
    clientVoxelSelection = new BlockVoxelMultiSelector();
  }

  /**
   * Create a new selection consisting of all non-air voxels in the given boundary region
   * If a selection is already in place, cancel it.
   * @param thePlayer
   * @param boundaryCorner1
   * @param boundaryCorner2
   * @return
   */
  public ResultWithReason createFullBoxSelection(EntityClientPlayerMP thePlayer, ChunkCoordinates boundaryCorner1, ChunkCoordinates boundaryCorner2)
  {
    initialiseForGeneration();
    clientVoxelSelection.selectAllInBoxStart(thePlayer.worldObj, boundaryCorner1, boundaryCorner2);
    return ResultWithReason.success();
  }

  /**
   * initialise conversion of the selected fill to a VoxelSelection
   * From the starting block, performs a flood fill on all non-air blocks.
   * Will not fill any blocks with y less than the blockUnderCursor.
   * If a selection is already in place, cancel it.
   * @param fillStartingBlock the block being highlighted by the cursor
  */
  public ResultWithReason createUnboundFillSelection(EntityClientPlayerMP thePlayer, ChunkCoordinates fillStartingBlock)
  {
    initialiseForGeneration();
    clientVoxelSelection.selectUnboundFillStart(thePlayer.worldObj, fillStartingBlock);    return ResultWithReason.success();
  }

  /**
   * initialise conversion of the selected fill to a VoxelSelection
   * From the starting block, performs a flood fill on all non-air blocks.
   * Will not fill any blocks outside of the box defined by corner1 and corner2
   * If a selection is already in place, cancel it.
   */
  public ResultWithReason createBoundFillSelection(EntityClientPlayerMP thePlayer, ChunkCoordinates fillStartingBlock, ChunkCoordinates boundaryCorner1, ChunkCoordinates boundaryCorner2)
  {
    initialiseForGeneration();
    clientVoxelSelection.selectBoundFillStart(thePlayer.worldObj, fillStartingBlock, boundaryCorner1, boundaryCorner2);
    return ResultWithReason.success();
  }

  /**
   * Returns true if the selection has been updated since the last call to this function; resets flag after the call.
   * (If true, the selection render information, origin, and QuadOrientation will need to be re-retrieved).
   * @return
   */
  public boolean hasSelectionBeenUpdated() {
    boolean retval = selectionUpdatedFlag;
    selectionUpdatedFlag = false;
    return retval;
  }

  /** return a copy of the selection's initial origin, i.e. at the time of its creation.
   *  If not available (IDLE or GENERATING), causes an error
   * @return
   */
  public ChunkCoordinates getInitialOrigin() {
    if (clientSelectionState != ClientSelectionState.COMPLETE && clientSelectionState != ClientSelectionState.CREATING_RENDERLISTS) {
      ErrorLog.defaultLog().severe("called getInitialOrigin for invalid state: " + clientSelectionState);
      return new ChunkCoordinates(0, 0, 0);
    }
    return clientVoxelSelection.getWorldOrigin();  // getWorldOrigin returns a new copy
  }

  /** return a copy of the selection's initial QuadOrientation, i.e. at the time of its creation.
   *  If not available (IDLE or GENERATING), causes an error
   */
  public QuadOrientation getInitialQuadOrientation() {
    if (clientSelectionState != ClientSelectionState.COMPLETE && clientSelectionState != ClientSelectionState.CREATING_RENDERLISTS) {
      ErrorLog.defaultLog().severe("called getInitialQuadOrientation for invalid state: " + clientSelectionState);
      return new QuadOrientation(0, 0, 1, 1);
    }
    return new QuadOrientation(0, 0, clientVoxelSelection.getSelection().getxSize(), clientVoxelSelection.getSelection().getzSize());
  }

  /** called once per tick on the client side
   * used to:
   * (1) background generation of a selection, if it has been initiated
   * (2) start transmission of the selection, if it has just been completed
   * (3) acknowledge (get) the action and undo statuses
   * @param maxDurationInNS = the maximum time to spend on selection generation, in NS
   */
  public void performTick(World world, long maxDurationInNS) {
    assert checkInvariants();

    switch (clientSelectionState) {
      case IDLE:
      case COMPLETE: {
        break;
      }
      case GENERATING: {   // keep generating; if complete, switch to creation of rendering lists
        float progress = clientVoxelSelection.continueSelectionGeneration(world, maxDurationInNS);
        if (progress >= 0) {
          selectionGenerationFractionComplete = progress;
        } else {
          clientSelectionState = ClientSelectionState.CREATING_RENDERLISTS;
          selectionPacketSender.reset();
          if (clientVoxelSelection.isEmpty()) {
            clientSelectionState = ClientSelectionState.IDLE;
          } else {
            ChunkCoordinates selectionInitialOrigin = clientVoxelSelection.getWorldOrigin();
            if (voxelSelectionRenderer == null) {
              voxelSelectionRenderer = new BlockVoxelMultiSelectorRenderer();
            }
            voxelSelectionRenderer.createRenderListStart(world, selectionInitialOrigin.posX, selectionInitialOrigin.posY, selectionInitialOrigin.posZ,
                    clientVoxelSelection.getSelection(), clientVoxelSelection.getUnavailableVoxels());
          }
        }
        break;
      }
      case CREATING_RENDERLISTS: {
        ChunkCoordinates wOrigin = clientVoxelSelection.getWorldOrigin();
        float progress = voxelSelectionRenderer.createRenderListContinue(world, wOrigin.posX, wOrigin.posY, wOrigin.posZ,
                clientVoxelSelection.getSelection(), clientVoxelSelection.getUnavailableVoxels(), maxDurationInNS);

        if (progress >= 0) {
          renderlistGenerationFractionComplete = progress;
        } else {
          clientSelectionState = ClientSelectionState.COMPLETE;
          selectionUpdatedFlag = true;
        }
        break;
      }
      default: {
        ErrorLog.defaultLog().severe("Invalid clientSelectionState " + clientSelectionState + " in " + this.getClass().getName());
        break;
      }
    }

    if (clientSelectionState == ClientSelectionState.IDLE) {
      selectionPacketSender.reset();
    }

    // if the selection has been freshly generated, keep trying to transmit it until we successfully start transmission
    if (clientSelectionState == ClientSelectionState.COMPLETE
        && selectionPacketSender.getCurrentPacketProgress() == SelectionPacketSender.PacketProgress.IDLE) {
      selectionPacketSender.startSendingSelection(clientVoxelSelection);
    }
    selectionPacketSender.tick();
  }

  private BlockVoxelMultiSelector clientVoxelSelection;

  public BlockVoxelMultiSelectorRenderer getVoxelSelectionRenderer() {
    return voxelSelectionRenderer;
  }

  private BlockVoxelMultiSelectorRenderer voxelSelectionRenderer;
  private float renderlistGenerationFractionComplete;
  private float selectionGenerationFractionComplete;
  private boolean selectionUpdatedFlag;
}