package speedytools.clientside.selections;

import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import speedytools.clientside.network.PacketHandlerRegistryClient;
import speedytools.clientside.network.PacketSenderClient;
import speedytools.clientside.tools.SelectionPacketSender;
import speedytools.common.network.Packet250ServerSelectionGeneration;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.multipart.MultipartOneAtATimeReceiver;
import speedytools.common.network.multipart.MultipartPacket;
import speedytools.common.network.multipart.Packet250MultipartSegment;
import speedytools.common.network.multipart.SelectionPacket;
import speedytools.common.selections.BlockVoxelMultiSelector;
import speedytools.common.selections.VoxelSelectionWithOrigin;
import speedytools.common.utilities.ErrorLog;
import speedytools.common.utilities.QuadOrientation;
import speedytools.common.utilities.ResultWithReason;

/**
 * Created by TheGreyGhost on 26/09/14.
 * Used by the client to track the current tool voxel selection and coordinate any updates with the server
 */
public class ClientVoxelSelection
{
  public ClientVoxelSelection(PacketHandlerRegistryClient packetHandlerRegistryClient,
                              SelectionPacketSender i_selectionPacketSender, PacketSenderClient i_packetSenderClient)
  {
    selectionPacketSender = i_selectionPacketSender;
    packetSenderClient = i_packetSenderClient;
    incomingVoxelSelection = new MultipartOneAtATimeReceiver();
    incomingVoxelSelection.registerPacketCreator(new SelectionPacket.SelectionPacketCreator());
    incomingVoxelSelection.registerLinkageFactory(new IncomingSelectionLinkageFactory());
    incomingVoxelSelection.setPacketSender(packetSenderClient);
    incomingVoxelSelection.registerLinkageFactory(this.new IncomingSelectionLinkageFactory());

    Packet250MultipartSegment.registerHandler(packetHandlerRegistryClient, this.new IncomingSelectionPacketHandler(), Side.CLIENT,
                                              Packet250Types.PACKET250_SELECTION_PACKET);
    Packet250ServerSelectionGeneration.registerHandler(packetHandlerRegistryClient, this.new IncomingPacketHandler(), Side.CLIENT);
  }

  private SelectionPacketSender selectionPacketSender;
  private PacketSenderClient packetSenderClient;

  private enum ClientSelectionState {IDLE, GENERATING, CREATING_RENDERLISTS, COMPLETE}
  private enum OutgoingTransmissionState {IDLE, SENDING, COMPLETE, NOT_REQUIRED}
  private enum ServerSelectionState {IDLE, WAITING_FOR_START, GENERATING, RECEIVING, CREATING_RENDERLISTS, COMPLETE}

  public enum VoxelSelectionState {NO_SELECTION, GENERATING, READY_FOR_DISPLAY}

  private ClientSelectionState clientSelectionState = ClientSelectionState.IDLE;
  private OutgoingTransmissionState outgoingTransmissionState = OutgoingTransmissionState.IDLE;
  private ServerSelectionState serverSelectionState = ServerSelectionState.IDLE;

  private Packet250ServerSelectionGeneration packet250ServerSelectionGeneration; // if needed for server selection generation

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
    // is complete if either:
    // 1) the client selection has been fully sent, or
    // 2) the client selection had missing voxels and the server selection has been generated

    switch (outgoingTransmissionState) {
      case COMPLETE: {
        return true;
      }
      case NOT_REQUIRED: {
        if (serverSelectionState == ServerSelectionState.RECEIVING
                || serverSelectionState == ServerSelectionState.CREATING_RENDERLISTS
                || serverSelectionState == ServerSelectionState.COMPLETE) {
          return true;
        }
        return false;
      }
      case SENDING:
      case IDLE: {
        return false;
      }
      default: {
        ErrorLog.defaultLog().severe("Invalid outgoingTransmissionState " + outgoingTransmissionState + " in " + this.getClass().getName());
        return false;
      }
    }
  }

  /** return true if the selection is currently being generated, either locally or on the server.
   * i.e. - returns false if IDLE or COMPLETE, true otherwise.
   * @return
   */
  public boolean isGenerationInProgress()
  {
    if (  (clientSelectionState != ClientSelectionState.IDLE && clientSelectionState != ClientSelectionState.COMPLETE)
        || (serverSelectionState != ServerSelectionState.IDLE && serverSelectionState != ServerSelectionState.COMPLETE
            && serverSelectionState != ServerSelectionState.CREATING_RENDERLISTS )
       ) {
      return true;
    } else {
      return false;
    }
  }

  public final float FULLY_COMPLETE = 1.000F;
  private final float FULLY_COMPLETE_PLUS_DELTA = FULLY_COMPLETE + 0.001F;

  /**
   * returns the progress of the selection generation on (or transmission to ) server
   * @return [0 .. 1] for the estimated fractional completion of the voxel selection generation / transmission
   *         returns 0 if not started yet, returns > FULLY_COMPLETE if complete
   */
  public float getServerSelectionFractionComplete()
  {
    if (clientSelectionState != ClientSelectionState.COMPLETE) return 0.0F;
    switch (outgoingTransmissionState) {
      case IDLE: {
        return 0.0F;
      }
      case COMPLETE: {
        return FULLY_COMPLETE_PLUS_DELTA;
      }
      case NOT_REQUIRED: {
        if (serverSelectionState == ServerSelectionState.RECEIVING
                || serverSelectionState == ServerSelectionState.CREATING_RENDERLISTS
                || serverSelectionState == ServerSelectionState.COMPLETE) {
          return FULLY_COMPLETE_PLUS_DELTA;
        }
        return serverGenerationFractionComplete;
      }
      case SENDING: {
        return selectionPacketSender.getCurrentPacketPercentComplete() / 100.0F;
      }
      default: {
        ErrorLog.defaultLog().severe("Invalid outgoingTransmissionState " + outgoingTransmissionState + " in " + this.getClass().getName());
        return 0.0F;
      }
    }
  }

  /**
   * If the voxel selection is currently being generated, return the fraction complete  (local selection, not server generation)
   * @return [0 .. 1] for the estimated fractional completion of the voxel selection generation
   *         returns 0 if not started yet, returns 1.000F if complete
   */
  public float getLocalGenerationFractionComplete()
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
    if (clientSelectionState != ClientSelectionState.IDLE && clientVoxelMultiSelector == null) return false;
    if ((clientSelectionState == ClientSelectionState.CREATING_RENDERLISTS ||
        clientSelectionState == ClientSelectionState.COMPLETE) && voxelSelectionRenderer == null) return false;
    if (serverSelectionState == ServerSelectionState.COMPLETE && serverVoxelSelection == null) return false;
    return true;
  }

  /**
   * If there is a current selection, destroy it.  No effect if waiting for the server to do something.
   */
  public void reset()
  {
    clientVoxelMultiSelector = null;
    if (voxelSelectionRenderer != null) {
      voxelSelectionRenderer.release();
      voxelSelectionRenderer = null;
      selectionBeingDisplayed = null;
    }
    clientSelectionState = ClientSelectionState.IDLE;
    if (serverSelectionState != ServerSelectionState.IDLE) {
      Packet250ServerSelectionGeneration abortPacket = Packet250ServerSelectionGeneration.abortSelectionGeneration(currentSelectionUniqueID);
      packetSenderClient.sendPacket(abortPacket);
      serverSelectionState = ServerSelectionState.IDLE;
    }

    outgoingTransmissionState = OutgoingTransmissionState.IDLE;
    selectionPacketSender.reset();
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
        Packet250ServerSelectionGeneration abortPacket = Packet250ServerSelectionGeneration.abortSelectionGeneration(currentSelectionUniqueID);
        packetSenderClient.sendPacket(abortPacket);
        serverSelectionState = ServerSelectionState.IDLE;
      }
    }
  }

  private void initialiseForGeneration()
  {
    reset();
    selectionUpdatedFlag = false;
    selectionBeingDisplayed = null;
    clientSelectionState = ClientSelectionState.GENERATING;
    outgoingTransmissionState = OutgoingTransmissionState.IDLE;
    selectionGenerationFractionComplete = 0;
    renderlistGenerationFractionComplete = 0;
    clientVoxelMultiSelector = new BlockVoxelMultiSelector();
    currentSelectionUniqueID = nextUniqueID++;
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
    clientVoxelMultiSelector.selectAllInBoxStart(thePlayer.worldObj, boundaryCorner1, boundaryCorner2);
    packet250ServerSelectionGeneration = Packet250ServerSelectionGeneration.performAllInBox(currentSelectionUniqueID, boundaryCorner1, boundaryCorner2);
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
    clientVoxelMultiSelector.selectUnboundFillStart(thePlayer.worldObj, fillStartingBlock);
    packet250ServerSelectionGeneration = Packet250ServerSelectionGeneration.performUnboundFill(currentSelectionUniqueID, fillStartingBlock);
    return ResultWithReason.success();
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
    clientVoxelMultiSelector.selectBoundFillStart(thePlayer.worldObj, fillStartingBlock, boundaryCorner1, boundaryCorner2);
    packet250ServerSelectionGeneration = Packet250ServerSelectionGeneration.performBoundFill(currentSelectionUniqueID, fillStartingBlock, boundaryCorner1, boundaryCorner2);
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

  /** return a copy of the selection's origin at the time of its creation from the world
   *  If not available (IDLE or GENERATING), causes an error
   * @return
   */
  public ChunkCoordinates getSourceWorldOrigin() {
    if (clientSelectionState != ClientSelectionState.COMPLETE && clientSelectionState != ClientSelectionState.CREATING_RENDERLISTS) {
      ErrorLog.defaultLog().severe("called getInitialOrigin for invalid state: " + clientSelectionState);
      return new ChunkCoordinates(0, 0, 0);
    }

    return new ChunkCoordinates(selectionBeingDisplayed.getWxOrigin(), selectionBeingDisplayed.getWyOrigin(), selectionBeingDisplayed.getWzOrigin());
  }

  /** return a copy of the selection's QuadOrientation at the time of its creation from the world.
   *  If not available (IDLE or GENERATING), causes an error
   */
  public QuadOrientation getSourceQuadOrientation() {
    if (clientSelectionState != ClientSelectionState.COMPLETE && clientSelectionState != ClientSelectionState.CREATING_RENDERLISTS) {
      ErrorLog.defaultLog().severe("called getInitialQuadOrientation for invalid state: " + clientSelectionState);
      return new QuadOrientation(0, 0, 1, 1);
    }
    return new QuadOrientation(0, 0, selectionBeingDisplayed.getxSize(), selectionBeingDisplayed.getzSize());
  }

  /** called once per tick on the client side
   * used to advance through the various states of the selection, eg generation, transmission, etc
   * @param maxDurationInNS = the maximum time to spend on selection generation, in NS
   */
  public void performTick(World world, long maxDurationInNS) {
    ++tickCount;
    assert checkInvariants();

    switch (clientSelectionState) {
      case IDLE:
      case COMPLETE: {
        break;
      }
      case GENERATING: {   // keep generating; if complete, switch to creation of rendering lists
        float progress = clientVoxelMultiSelector.continueSelectionGeneration(world, maxDurationInNS);
        if (progress >= 0) {
          selectionGenerationFractionComplete = progress;
        } else {   // -1 means complete

//          selectionPacketSender.reset();
          if (clientVoxelMultiSelector.isEmpty()) {
            clientSelectionState = ClientSelectionState.IDLE;
          } else {
            clientSelectionState = ClientSelectionState.CREATING_RENDERLISTS;
            ChunkCoordinates selectionInitialOrigin = clientVoxelMultiSelector.getWorldOrigin();
            if (voxelSelectionRenderer == null) {
              voxelSelectionRenderer = new BlockVoxelMultiSelectorRenderer();
            }
            voxelSelectionRenderer.createRenderListStart(world, selectionInitialOrigin.posX, selectionInitialOrigin.posY, selectionInitialOrigin.posZ,
                    clientVoxelMultiSelector.getSelection(), clientVoxelMultiSelector.getUnavailableVoxels());
          }
        }
        break;
      }
      case CREATING_RENDERLISTS: {
        ChunkCoordinates wOrigin = clientVoxelMultiSelector.getWorldOrigin();
        float progress = voxelSelectionRenderer.createRenderListContinue(world, wOrigin.posX, wOrigin.posY, wOrigin.posZ,
                clientVoxelMultiSelector.getSelection(), clientVoxelMultiSelector.getUnavailableVoxels(), maxDurationInNS);

        if (progress >= 0) {
          renderlistGenerationFractionComplete = progress;
        } else {  // -1 means finished
          clientSelectionState = ClientSelectionState.COMPLETE;
          selectionBeingDisplayed = clientVoxelMultiSelector.getSelection();
          selectionUpdatedFlag = true;
        }
        break;
      }
      default: {
        ErrorLog.defaultLog().severe("Invalid clientSelectionState " + clientSelectionState + " in " + this.getClass().getName());
        break;
      }
    }

    final int TICKS_BETWEEN_STATUS_REQUEST = 20;
    switch (serverSelectionState) {
      case IDLE: {
        if (clientSelectionState != ClientSelectionState.IDLE && clientVoxelMultiSelector.containsUnavailableVoxels()) {     // need the server to generate the selection.
          serverSelectionState = ServerSelectionState.WAITING_FOR_START;
          packetSenderClient.sendPacket(packet250ServerSelectionGeneration);
          lastStatusRequestTick = tickCount;
          serverGenerationFractionComplete = 0;
          incomingSelectionFractionComplete = 0;
          incomingSelectionUniqueID = null;
          serverVoxelSelection = null;
        }
        break;
      }
      case WAITING_FOR_START: {
        sendStatusRequestIfDue(TICKS_BETWEEN_STATUS_REQUEST);
        if (serverGenerationFractionComplete > 0) {
          serverSelectionState = ServerSelectionState.GENERATING;
        }
        break;
      }
      // for GENERATING and RECEIVING, the server 'pushes' the packet across - see code in IncomingSelectionLinkage
      case GENERATING: {
        if (incomingSelectionUniqueID != null) {  // the packet linkage has started receiving a new selection
          serverSelectionState = ServerSelectionState.RECEIVING;
        }
        sendStatusRequestIfDue(TICKS_BETWEEN_STATUS_REQUEST);
        break;
      }
      case RECEIVING: {
        if (serverVoxelSelection != null) {  // incoming packet transmission has finished
          serverSelectionState = ServerSelectionState.CREATING_RENDERLISTS;
          voxelSelectionRenderer.resize(serverVoxelSelection.getWxOrigin(), serverVoxelSelection.getWyOrigin(), serverVoxelSelection.getWzOrigin(),
                                        serverVoxelSelection.getxSize(), serverVoxelSelection.getySize(), serverVoxelSelection.getzSize());
          voxelSelectionRenderer.refreshRenderListStart();
          selectionBeingDisplayed = serverVoxelSelection;
          selectionUpdatedFlag = true;
        }
        break;
      }
      case CREATING_RENDERLISTS: {
        VoxelSelectionWithOrigin nullSelection = new VoxelSelectionWithOrigin(0, 0, 0, 1, 1, 1);
        float progress = voxelSelectionRenderer.refreshRenderListContinue(world, serverVoxelSelection, nullSelection, maxDurationInNS);
        if (progress < 0) {
          boolean noFogLeft = voxelSelectionRenderer.updateWithLoadedChunks(world, serverVoxelSelection, nullSelection, maxDurationInNS);
          if (noFogLeft) {
            serverSelectionState = ServerSelectionState.COMPLETE;
          }
        }
        break;
      }
      case COMPLETE: {
        break;
      }
      default: {
        ErrorLog.defaultLog().severe("Invalid serverSelectionState " + serverSelectionState + " in " + this.getClass().getName());
        break;
      }
    }

    switch (outgoingTransmissionState) {
      case IDLE: {
        if (clientSelectionState == ClientSelectionState.COMPLETE) {
          if (clientVoxelMultiSelector.containsUnavailableVoxels()) {
            outgoingTransmissionState = OutgoingTransmissionState.NOT_REQUIRED;
          } else {
            boolean success = selectionPacketSender.startSendingSelection(clientVoxelMultiSelector);
            if (success) {
              outgoingTransmissionState = OutgoingTransmissionState.SENDING;
            }
          }
        }
        break;
      }
      case NOT_REQUIRED:
      case COMPLETE: {
        break;
      }

      case SENDING: {
        SelectionPacketSender.PacketProgress packetProgress = selectionPacketSender.getCurrentPacketProgress();
        if (packetProgress != SelectionPacketSender.PacketProgress.SENDING) {
          if (packetProgress == SelectionPacketSender.PacketProgress.COMPLETED) {
            outgoingTransmissionState = OutgoingTransmissionState.COMPLETE;
          } else {
            outgoingTransmissionState = OutgoingTransmissionState.IDLE;  // Abort or return to Idle
          }
        }
        break;
      }

      default: {
        ErrorLog.defaultLog().severe("Invalid outgoingTransmissionState " + outgoingTransmissionState + " in " + this.getClass().getName());
        break;
      }
    }

    if (clientSelectionState == ClientSelectionState.IDLE) {
      selectionPacketSender.reset();
    }

//    // if the selection has been freshly generated, keep trying to transmit it until we successfully start transmission
//    if (clientSelectionState == ClientSelectionState.COMPLETE
//        && selectionPacketSender.getCurrentPacketProgress() == SelectionPacketSender.PacketProgress.IDLE) {
//      selectionPacketSender.startSendingSelection(clientVoxelMultiSelector);
//    }
    selectionPacketSender.tick();
  }

  private void sendStatusRequestIfDue(int ticksBetweenStatusRequests)
  {
    if ((tickCount - lastStatusRequestTick) >= ticksBetweenStatusRequests) {
      Packet250ServerSelectionGeneration packet = Packet250ServerSelectionGeneration.requestStatus(currentSelectionUniqueID);
      packetSenderClient.sendPacket(packet);
      lastStatusRequestTick = tickCount;
    }
  }

  private BlockVoxelMultiSelector clientVoxelMultiSelector;
  private int currentSelectionUniqueID;
  private static int nextUniqueID = -2366236; // arbitrary
  private int tickCount;
  private int lastStatusRequestTick;

  public BlockVoxelMultiSelectorRenderer getVoxelSelectionRenderer() {
    return voxelSelectionRenderer;
  }

  private BlockVoxelMultiSelectorRenderer voxelSelectionRenderer;
  private float renderlistGenerationFractionComplete;
  private float selectionGenerationFractionComplete;
  private boolean selectionUpdatedFlag;

  private VoxelSelectionWithOrigin selectionBeingDisplayed;

  private float serverGenerationFractionComplete;
  private float incomingSelectionFractionComplete;
  private Integer incomingSelectionUniqueID;
  private VoxelSelectionWithOrigin serverVoxelSelection;

  public class IncomingSelectionPacketHandler implements Packet250MultipartSegment.PacketHandlerMethod {
    @Override
    public boolean handlePacket(Packet250MultipartSegment packet250MultipartSegment, MessageContext ctx) {
      boolean success = incomingVoxelSelection.processIncomingPacket(packet250MultipartSegment);
      return success;
    }
  }
  private IncomingSelectionPacketHandler incomingSelectionPacketHandler;
  private MultipartOneAtATimeReceiver incomingVoxelSelection;

  // processes incoming status information about the server selection generation commands
  public class IncomingPacketHandler implements Packet250ServerSelectionGeneration.PacketHandlerMethod
  {
    public Packet250ServerSelectionGeneration handlePacket(Packet250ServerSelectionGeneration packet, MessageContext ctx)
    {
      switch (packet.getCommand()) {
        case STATUS_REPLY: {
          if (packet.getUniqueID() == currentSelectionUniqueID) {   // ignore any messages not about the current selection
            serverGenerationFractionComplete = packet.getCompletedFraction();
          }
          break;
        }
        default: {
          ErrorLog.defaultLog().severe("Invalid command received by ClientVoxelSelection: " + packet.getCommand());
          return null;
        }
      }
      return null;
    }
  }

  /**
   * The linkage is used to convey information about the process of the incoming selection
   */
  public class IncomingSelectionLinkage implements MultipartOneAtATimeReceiver.PacketLinkage
  {
    public IncomingSelectionLinkage(SelectionPacket linkedPacket) {
//      System.out.println("VoxelPacketLinkage constructed for Selection Packet ID " + linkedPacket.getUniqueID());
      myLinkedPacket = linkedPacket;
      incomingSelectionUniqueID = linkedPacket.getUniqueID();
      incomingSelectionFractionComplete = 0;
//      myPlayer = new WeakReference<EntityPlayerMP>(player);
    }
    @Override
    public void progressUpdate(int percentComplete) {
      incomingSelectionFractionComplete = percentComplete;
    }
    @Override
    public void packetCompleted() {
//      System.out.println("VoxelPacketLinkage - completed packet ID " + myLinkedPacket.getUniqueID());
      if (myLinkedPacket == null) return;
      serverVoxelSelection = myLinkedPacket.retrieveVoxelSelection();
    }
    @Override
    public void packetAborted() {}
    @Override
    public int getPacketID() {return myLinkedPacket.getUniqueID();}
//    private WeakReference<EntityPlayerMP> myPlayer;
    private SelectionPacket myLinkedPacket;
  }

  /**
   * The Factory creates a new linkage, which will be used to communicate the packet receiving progress to the recipient
   */
  public class IncomingSelectionLinkageFactory implements MultipartOneAtATimeReceiver.PacketReceiverLinkageFactory
  {
//    public IncomingSelectionLinkageFactory() {
//      myPlayer = new WeakReference<EntityPlayerMP>(playerMP);
//    }
    @Override
    public IncomingSelectionLinkage createNewLinkage(MultipartPacket linkedPacket) {
      assert linkedPacket instanceof SelectionPacket;
      return ClientVoxelSelection.this.new IncomingSelectionLinkage((SelectionPacket)linkedPacket);
    }
//    private WeakReference<EntityPlayerMP> myPlayer;
  }

}