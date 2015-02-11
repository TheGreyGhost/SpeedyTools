package speedytools.serverside;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import speedytools.common.network.Packet250ServerSelectionGeneration;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.multipart.*;
import speedytools.common.selections.BlockVoxelMultiSelector;
import speedytools.common.selections.VoxelSelectionWithOrigin;
import speedytools.common.utilities.ErrorLog;
import speedytools.serverside.network.PacketHandlerRegistryServer;
import speedytools.serverside.network.PacketSenderServer;

import java.lang.ref.WeakReference;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.WeakHashMap;

/**
* User: The Grey Ghost
* Date: 19/05/2014
* Handles synchronisation of the voxelselection for each client:
* 1) Remembers the current voxel selection of each player, received from the client.
 *2) In response to a Packet250ServerSelectionGeneration command from the client, generates a local selection
 *   and then sends it back to the client
 *
* Automatically registers itself for addition/removal of players, processing of incoming packets
* Usage:
* (1) tick() must be called at frequent intervals to check for timeouts - at least once per second
* (2) getCurrentSelection() to retrieve the current VoxelSelection for a player.  The current VoxelSelection
*     isn't updated until an entire new selection is received / generated.
*/
public class ServerVoxelSelections
{
  public ServerVoxelSelections(PacketHandlerRegistryServer i_packetHandlerRegistryServer, PlayerTrackerRegistry playerTrackerRegistry)
  {
    packetHandlerVoxelsFromClient = this.new PacketHandlerVoxelsFromClient();
    packetHandlerVoxelsToClient = this.new PacketHandlerVoxelsToClient();
    packetHandlerRegistryServer = i_packetHandlerRegistryServer;

    Packet250MultipartSegment.registerHandler(packetHandlerRegistryServer, packetHandlerVoxelsFromClient, Side.SERVER,
          Packet250Types.PACKET250_SELECTION_PACKET);
    Packet250MultipartSegmentAcknowledge.registerHandler(packetHandlerRegistryServer, packetHandlerVoxelsToClient, Side.SERVER,
                                                         Packet250Types.PACKET250_SELECTION_PACKET_ACKNOWLEDGE);
    Packet250ServerSelectionGeneration.registerHandler(packetHandlerRegistryServer, this.new PacketHandlerServerSelectionGeneration(), Side.SERVER);

    playerTracker = this.new PlayerTracker();
    playerTrackerRegistry.registerHandler(playerTracker);
  }

  /** returns the current VoxelSelection for this player, or null if none
   *  The current VoxelSelection isn't updated until an entire new selection is received.
   * @param player
   * @return  current selection for the given player, or null if none
   */
  public VoxelSelectionWithOrigin getVoxelSelection(EntityPlayerMP player)
  {
    return playerSelections.get(player);
  }

  /**
   * handle timeouts etc
   * @param maximumDurationInNS - the maximum amount of time to spend generating selections for clients. 0 = don't generate any.
   */
  public void tick(long maximumDurationInNS)
  {
    for (MultipartOneAtATimeReceiver receiver : playerMOATreceivers.values()) {
      receiver.onTick();
    }
    for (MultipartOneAtATimeSender sender : playerMOATsenders.values()) {
      sender.onTick();
    }

    if (maximumDurationInNS == 0) return;

    boolean foundSuitable = false;
    CommandQueueEntry currentCommand;
    do {
      currentCommand = commandQueue.peekFirst();
      if (currentCommand == null) return;
      if (currentCommand.entityPlayerMP.get() == null) {
        commandQueue.removeFirst();
      } else {
        foundSuitable = true;
      }
    } while (!foundSuitable);
    EntityPlayerMP entityPlayerMP = currentCommand.entityPlayerMP.get();
    World playerWorld = entityPlayerMP.getEntityWorld();
    Packet250ServerSelectionGeneration commandPacket = currentCommand.commandPacket;

    if (!currentCommand.hasStarted) {
      BlockVoxelMultiSelector blockVoxelMultiSelector = new BlockVoxelMultiSelector();
      playerBlockVoxelMultiSelectors.put(entityPlayerMP, blockVoxelMultiSelector);
      playerCommandStatus.put(entityPlayerMP, CommandStatus.EXECUTING);
      currentCommand.blockVoxelMultiSelector = blockVoxelMultiSelector;
      switch (commandPacket.getCommand()) {
        case ALL_IN_BOX: {
          blockVoxelMultiSelector.selectAllInBoxStart(playerWorld, commandPacket.getCorner1(), commandPacket.getCorner2());
          break;
        }
        case UNBOUND_FILL: {
          blockVoxelMultiSelector.selectUnboundFillStart(playerWorld, commandPacket.getFillAlgorithmSettings());
          break;
        }
        case BOUND_FILL: {
          blockVoxelMultiSelector.selectBoundFillStart(playerWorld, commandPacket.getFillAlgorithmSettings(),
                                                        commandPacket.getCorner1(), commandPacket.getCorner2());
          break;
        }
        default: {
          ErrorLog.defaultLog().severe("Invalid command in ServerVoxelSelections: " + commandPacket.getCommand());
          break;
        }
      }
      currentCommand.hasStarted = true;
    } else {
      BlockVoxelMultiSelector blockVoxelMultiSelector = currentCommand.blockVoxelMultiSelector;
      float progress = blockVoxelMultiSelector.continueSelectionGeneration(playerWorld, maximumDurationInNS);
      if (progress < 0) { // finished
        ChunkCoordinates origin = blockVoxelMultiSelector.getWorldOrigin();
        VoxelSelectionWithOrigin newSelection = new VoxelSelectionWithOrigin(origin.posX, origin.posY, origin.posZ,
                                                                             blockVoxelMultiSelector.getSelection());
        playerSelections.put(entityPlayerMP, newSelection);
        playerBlockVoxelMultiSelectors.remove(entityPlayerMP);
        playerCommandStatus.put(entityPlayerMP, CommandStatus.COMPLETED);


        MultipartOneAtATimeSender sender = playerMOATsenders.get(entityPlayerMP);
        if (sender != null) {
          SelectionPacket selectionPacket = SelectionPacket.createSenderPacket(blockVoxelMultiSelector);
          SenderLinkage newLinkage = new SenderLinkage(entityPlayerMP, selectionPacket.getUniqueID());
          playerSenderLinkages.put(entityPlayerMP, newLinkage);
          sender.sendMultipartPacket(newLinkage, selectionPacket);
        }
        assert (commandQueue.peekFirst() == currentCommand);
        commandQueue.removeFirst();
      }
    }
  }

//  private BlockVoxelMultiSelector.Matcher getMatcherTranslation(Packet250ServerSelectionGeneration.MatcherType matcherType)
//  {
//    switch (matcherType) {
//      case ANY_NON_AIR: {
//        return BlockVoxelMultiSelector.Matcher.ALL_NON_AIR;
//      }
//      case STARTING_BLOCK_ONLY: {
//        return BlockVoxelMultiSelector.Matcher.STARTING_BLOCK_ONLY;
//      }
//      default: {
//        ErrorLog.defaultLog().severe("Illegal matcherType:" + matcherType);
//        return null;
//      }
//    }
//  }


// the linkage doesn't actually need to do anything
  public class SenderLinkage implements MultipartOneAtATimeSender.PacketLinkage
  {
    public SenderLinkage(EntityPlayerMP entityPlayerMP, int uniqueID) {
      myEntityPlayerMP = new WeakReference<EntityPlayerMP>(entityPlayerMP);
      myUniqueID = uniqueID;
    }

    public void progressUpdate(int percentComplete) {}
    public void packetCompleted() {}
    public void packetAborted() {}
    public int getPacketID() {return myUniqueID;}

    private WeakReference<EntityPlayerMP> myEntityPlayerMP;
    private int myUniqueID;
  }

 // ------------

  private void addPlayer(EntityPlayerMP newPlayer)
  {
    if (!players.containsKey(newPlayer)) {
      players.put(newPlayer, 1);
      // playerSelections starts off null = no selection
      MultipartOneAtATimeReceiver newReceiver = new MultipartOneAtATimeReceiver();
      newReceiver.registerPacketCreator(new SelectionPacket.SelectionPacketCreator());
      newReceiver.registerLinkageFactory(new VoxelLinkageFactory(newPlayer));
      newReceiver.setPacketSender(new PacketSenderServer(packetHandlerRegistryServer, newPlayer));
      playerMOATreceivers.put(newPlayer, newReceiver);

      MultipartOneAtATimeSender newSender = new MultipartOneAtATimeSender(packetHandlerRegistryServer, null,
                                                          Packet250Types.PACKET250_SELECTION_PACKET_ACKNOWLEDGE, Side.SERVER);
      newSender.setPacketSender(new PacketSenderServer(packetHandlerRegistryServer, newPlayer));
      playerMOATsenders.put(newPlayer, newSender);
//      playerBlockVoxelMultiSelectors.put(newPlayer, new BlockVoxelMultiSelector());
    }
  }

  private void removePlayer(EntityPlayerMP whichPlayer)
  {
    players.remove(whichPlayer);
    playerSelections.remove(whichPlayer);
    playerMOATreceivers.remove(whichPlayer);
    playerMOATsenders.remove(whichPlayer);
    playerBlockVoxelMultiSelectors.remove(whichPlayer);
    playerSenderLinkages.remove(whichPlayer);
  }

  private WeakHashMap<EntityPlayerMP, VoxelSelectionWithOrigin> playerSelections = new WeakHashMap<EntityPlayerMP, VoxelSelectionWithOrigin>();
  private WeakHashMap<EntityPlayerMP, Integer> players = new WeakHashMap<EntityPlayerMP, Integer>();    // Integer is a dummy vble

  private WeakHashMap<EntityPlayerMP, MultipartOneAtATimeReceiver> playerMOATreceivers = new WeakHashMap<EntityPlayerMP, MultipartOneAtATimeReceiver>();
  private WeakHashMap<EntityPlayerMP, MultipartOneAtATimeSender> playerMOATsenders = new WeakHashMap<EntityPlayerMP, MultipartOneAtATimeSender>();
  private WeakHashMap<EntityPlayerMP, SenderLinkage> playerSenderLinkages = new WeakHashMap<EntityPlayerMP, SenderLinkage>();

  private class PlayerTracker implements PlayerTrackerRegistry.IPlayerTracker
  {
    public void onPlayerLogin(EntityPlayer player)
    {
      EntityPlayerMP entityPlayerMP = (EntityPlayerMP)player;
      ServerVoxelSelections.this.addPlayer(entityPlayerMP);
    }
    public void onPlayerLogout(EntityPlayer player)
    {
      EntityPlayerMP entityPlayerMP = (EntityPlayerMP)player;
      ServerVoxelSelections.this.removePlayer(entityPlayerMP);
    }
    public void onPlayerChangedDimension(EntityPlayer player) {}
    public void onPlayerRespawn(EntityPlayer player) {}
  }

  private PlayerTracker playerTracker;

 // ------ handler for responding to Selection generation commands from the client
 //   Response to commands is a STATUS_REPLY - with 0 for success, -ve number for failure
 public class PacketHandlerServerSelectionGeneration implements Packet250ServerSelectionGeneration.PacketHandlerMethod
 {
   public Packet250ServerSelectionGeneration handlePacket(Packet250ServerSelectionGeneration packet, MessageContext ctx)
   {
     final float ERROR_STATUS = -10.0F;
     EntityPlayerMP entityPlayerMP = ctx.getServerHandler().playerEntity;
     int uniqueID = packet.getUniqueID();
     if (!players.containsKey(entityPlayerMP)) {
       ErrorLog.defaultLog().info("ServerVoxelSelections:: Packet received from player not in players");
       return Packet250ServerSelectionGeneration.replyFractionCompleted(uniqueID, ERROR_STATUS);
     }

     Integer lastCommandID = playerLastCommandID.get(entityPlayerMP);
     if (lastCommandID == null) lastCommandID = Integer.MIN_VALUE;
     if (uniqueID < lastCommandID) return null;  // discard old commands

     switch(packet.getCommand()) {
       case STATUS_REQUEST: {
         if (uniqueID != lastCommandID) return null;  // discard old or too-new commands
         CommandStatus commandStatus = playerCommandStatus.get(entityPlayerMP);
         if (commandStatus == null) return Packet250ServerSelectionGeneration.replyFractionCompleted(uniqueID, ERROR_STATUS);
         final float JUST_STARTED_VALUE = 0.01F;
         switch (commandStatus) {
           case QUEUED: return Packet250ServerSelectionGeneration.replyFractionCompleted(uniqueID, JUST_STARTED_VALUE);
           case COMPLETED: return Packet250ServerSelectionGeneration.replyFractionCompleted(uniqueID, 1.0F);
           case EXECUTING: {
             BlockVoxelMultiSelector blockVoxelMultiSelector = playerBlockVoxelMultiSelectors.get(entityPlayerMP);
             if (blockVoxelMultiSelector == null) return Packet250ServerSelectionGeneration.replyFractionCompleted(uniqueID, ERROR_STATUS);
             return Packet250ServerSelectionGeneration.replyFractionCompleted(uniqueID, blockVoxelMultiSelector.getEstimatedFractionComplete());
           }
           default: assert false : "Invalid commandStatus in ServerVoxelSelections:" + commandStatus;
         }

       }
       case ABORT: {
         if (uniqueID == lastCommandID) {
           abortCurrentCommand(entityPlayerMP);
         }
         return null;
       }
       case ALL_IN_BOX:
       case UNBOUND_FILL:
       case BOUND_FILL: {
         if (uniqueID <= lastCommandID) return null;  // discard old commands

         boolean success = enqueueSelectionCommand(entityPlayerMP, packet);
         if (success) {
           playerLastCommandID.put(entityPlayerMP, uniqueID);
         }
         return Packet250ServerSelectionGeneration.replyFractionCompleted(uniqueID, success ? 0.0F : ERROR_STATUS);
       }
       default: {
         ErrorLog.defaultLog().severe("Invalid command received in ServerVoxelSelections:" + packet.getCommand());
         return null;
       }
     }
   }
 }

  /**
   * a client has requested the server to create a selection and send it back.  Queue it up for later processing.
   * @param commandPacket
   * @return
   */
  private boolean enqueueSelectionCommand(EntityPlayerMP entityPlayerMP, Packet250ServerSelectionGeneration commandPacket)
  {
    removeCommandsForPlayer(entityPlayerMP);
    commandQueue.addLast(new CommandQueueEntry(entityPlayerMP, commandPacket));
    playerCommandStatus.put(entityPlayerMP, CommandStatus.QUEUED);
    return true;
  }

  private void abortCurrentCommand(EntityPlayerMP entityPlayerMP)
  {
    MultipartOneAtATimeSender sender = playerMOATsenders.get(entityPlayerMP);
    SenderLinkage senderLinkage = playerSenderLinkages.get(entityPlayerMP);
    if (sender != null && senderLinkage != null) {
      sender.abortPacket(senderLinkage);
    }
    removeCommandsForPlayer(entityPlayerMP);
  }

  private void removeCommandsForPlayer(EntityPlayerMP entityPlayerMP)
  {
    Iterator<CommandQueueEntry> iterator = commandQueue.iterator();
    while (iterator.hasNext()) {
      CommandQueueEntry commandQueueEntry = iterator.next();
      if (commandQueueEntry.entityPlayerMP.get() == entityPlayerMP) {
        iterator.remove();
      }
    }
    playerBlockVoxelMultiSelectors.remove(entityPlayerMP);
    playerSenderLinkages.remove(entityPlayerMP);
  }

  private enum CommandStatus {QUEUED, EXECUTING, COMPLETED}
  private WeakHashMap<EntityPlayerMP, Integer> playerLastCommandID = new WeakHashMap<EntityPlayerMP, Integer>();
  private WeakHashMap<EntityPlayerMP, BlockVoxelMultiSelector> playerBlockVoxelMultiSelectors = new WeakHashMap<EntityPlayerMP, BlockVoxelMultiSelector>();
  private WeakHashMap<EntityPlayerMP, CommandStatus> playerCommandStatus = new WeakHashMap<EntityPlayerMP, CommandStatus>();

  private class CommandQueueEntry {
    public WeakReference<EntityPlayerMP> entityPlayerMP;
    public Packet250ServerSelectionGeneration commandPacket;
    public boolean hasStarted;
    public BlockVoxelMultiSelector blockVoxelMultiSelector;

    public CommandQueueEntry(EntityPlayerMP i_entityPlayerMP, Packet250ServerSelectionGeneration i_commandPacket) {
      entityPlayerMP = new WeakReference<EntityPlayerMP>(i_entityPlayerMP);
      commandPacket = i_commandPacket;
      hasStarted = false;
    }
  }

  Deque<CommandQueueEntry> commandQueue = new LinkedList<CommandQueueEntry>();

  // ------ handlers for sending selection to clients

  public class PacketHandlerVoxelsToClient implements Packet250MultipartSegmentAcknowledge.PacketHandlerMethod {
    @Override
    public boolean handlePacket(Packet250MultipartSegmentAcknowledge packetAck, MessageContext ctx) {
      {
        EntityPlayerMP entityPlayerMP = ctx.getServerHandler().playerEntity;
        if (!playerMOATsenders.containsKey(entityPlayerMP)) {
          ErrorLog.defaultLog().info("ServerVoxelSelections:: Packet received from player not in playerMOATsenders");
          return false;
        }
//      System.out.println("ServerVoxelSelections packet received");
        return playerMOATsenders.get(entityPlayerMP).handleIncomingPacket(packetAck);
      }
    }
  }
  private PacketHandlerVoxelsToClient packetHandlerVoxelsToClient;

   // ------ handlers for incoming selections from client

  public class PacketHandlerVoxelsFromClient implements Packet250MultipartSegment.PacketHandlerMethod {
    @Override
    public boolean handlePacket(Packet250MultipartSegment packet250MultipartSegment, MessageContext ctx) {
      {
        EntityPlayerMP entityPlayerMP = ctx.getServerHandler().playerEntity;
        if (!playerMOATreceivers.containsKey(entityPlayerMP)) {
          ErrorLog.defaultLog().info("ServerVoxelSelections:: Packet received from player not in playerMOATreceivers");
          return false;
        }
//      System.out.println("ServerVoxelSelections packet received");
        return playerMOATreceivers.get(entityPlayerMP).processIncomingPacket(packet250MultipartSegment);
      }
    }
  }
  private PacketHandlerVoxelsFromClient packetHandlerVoxelsFromClient;

  /**
       * This class is used by the MultipartOneAtATimeReceiver to communicate the packet transmission progress to the receiver
       * When the incoming packet is completed, it overwrites the player's current VoxelSelection with the new one.
       */
  public class VoxelPacketLinkage implements MultipartOneAtATimeReceiver.PacketLinkage
  {
    public VoxelPacketLinkage(EntityPlayerMP player, SelectionPacket linkedPacket) {
//      System.out.println("VoxelPacketLinkage constructed for Selection Packet ID " + linkedPacket.getUniqueID());
      myLinkedPacket = linkedPacket;
      myPlayer = new WeakReference<EntityPlayerMP>(player);
    }
    @Override
    public void progressUpdate(int percentComplete) {}
    @Override
    public void packetCompleted() {
//      System.out.println("VoxelPacketLinkage - completed packet ID " + myLinkedPacket.getUniqueID());
      if (myPlayer == null || myPlayer.get() == null ||  myLinkedPacket == null) return;
      playerSelections.put(myPlayer.get(), myLinkedPacket.retrieveVoxelSelection());
    }
    @Override
    public void packetAborted() {}
    @Override
    public int getPacketID() {return myLinkedPacket.getUniqueID();}
    private WeakReference<EntityPlayerMP> myPlayer;
    private SelectionPacket myLinkedPacket;
  }

  /**
   * The Factory creates a new linkage, which will be used to communicate the packet receiving progress to the recipient
   */
  public class VoxelLinkageFactory implements MultipartOneAtATimeReceiver.PacketReceiverLinkageFactory
  {
    public VoxelLinkageFactory(EntityPlayerMP playerMP) {
      myPlayer = new WeakReference<EntityPlayerMP>(playerMP);
    }
    @Override
    public VoxelPacketLinkage createNewLinkage(MultipartPacket linkedPacket) {
      assert linkedPacket instanceof SelectionPacket;
      return ServerVoxelSelections.this.new VoxelPacketLinkage(myPlayer.get(), (SelectionPacket)linkedPacket);
    }
    private WeakReference<EntityPlayerMP> myPlayer;
  }

  private PacketHandlerRegistryServer packetHandlerRegistryServer;
}
