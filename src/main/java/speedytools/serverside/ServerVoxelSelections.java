package speedytools.serverside;

import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayerMP;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.multipart.MultipartOneAtATimeReceiver;
import speedytools.common.network.multipart.MultipartPacket;
import speedytools.common.network.multipart.Packet250MultipartSegment;
import speedytools.common.network.multipart.SelectionPacket;
import speedytools.common.selections.VoxelSelectionWithOrigin;
import speedytools.common.utilities.ErrorLog;
import speedytools.serverside.network.PacketHandlerRegistryServer;
import speedytools.serverside.network.PacketSenderServer;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
* User: The Grey Ghost
* Date: 19/05/2014
* Remembers the current voxel selection of each player, received from the client.
* Automatically registers itself for addition/removal of players, processing of incoming packets
* Usage:
* (1) tick() must be called at frequent intervals to check for timeouts - at least once per second
* (2) getCurrentSelection() to retrieve the current VoxelSelection for a player.  The current VoxelSelection
*     isn't updated until an entire new selection is received.
*/
public class ServerVoxelSelections
{
  public ServerVoxelSelections(PacketHandlerRegistryServer i_packetHandlerRegistryServer)
  {
//    playerTracker = this.new PlayerTracker();
//    GameRegistry.registerPlayerTracker(playerTracker);   //todo reinstate player tracker
    packetHandlerVoxel = this.new PacketHandlerVoxel();
    packetHandlerRegistryServer = i_packetHandlerRegistryServer;

    Packet250MultipartSegment.registerHandler(packetHandlerRegistryServer, packetHandlerVoxel, Side.SERVER,
                                              Packet250Types.PACKET250_SELECTION_PACKET);
//    packetHandlerRegistry.registerHandlerMethod(Side.SERVER, Packet250Types.PACKET250_SELECTION_PACKET.getPacketTypeID(), packetHandlerVoxel);
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
   */
  public void tick()
  {
    for (MultipartOneAtATimeReceiver receiver : playerMOATreceivers.values()) {
      receiver.onTick();
    }
  }

  private void addPlayer(EntityPlayerMP newPlayer)
  {
    if (!playerSelections.containsKey(newPlayer)) {
//      playerSelections.put(newPlayer, new VoxelSelection(1, 1, 1));
      MultipartOneAtATimeReceiver newReceiver = new MultipartOneAtATimeReceiver();
      newReceiver.registerPacketCreator(new SelectionPacket.SelectionPacketCreator());
      newReceiver.registerLinkageFactory(new VoxelLinkageFactory(newPlayer));
      newReceiver.setPacketSender(new PacketSenderServer(packetHandlerRegistryServer, newPlayer));
      playerMOATreceivers.put(newPlayer, newReceiver);
    }
  }

  private void removePlayer(EntityPlayerMP whichPlayer)
  {
    playerSelections.remove(whichPlayer);
    playerMOATreceivers.remove(whichPlayer);
  }

  private WeakHashMap<EntityPlayerMP, VoxelSelectionWithOrigin> playerSelections = new WeakHashMap<EntityPlayerMP, VoxelSelectionWithOrigin>();

  private WeakHashMap<EntityPlayerMP, MultipartOneAtATimeReceiver> playerMOATreceivers = new WeakHashMap<EntityPlayerMP, MultipartOneAtATimeReceiver>();

  public class PacketHandlerVoxel implements Packet250MultipartSegment.PacketHandlerMethod {
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
  private PacketHandlerVoxel packetHandlerVoxel;

//  private class PlayerTracker implements IPlayerTracker           //todo reinstate player tracker
//  {
//    public void onPlayerLogin(EntityPlayer player)
//    {
//      EntityPlayerMP entityPlayerMP = (EntityPlayerMP)player;
//      ServerVoxelSelections.this.addPlayer(entityPlayerMP);
//    }
//    public void onPlayerLogout(EntityPlayer player)
//    {
//      EntityPlayerMP entityPlayerMP = (EntityPlayerMP)player;
//      ServerVoxelSelections.this.removePlayer(entityPlayerMP);
//    }
//    public void onPlayerChangedDimension(EntityPlayer player) {}
//    public void onPlayerRespawn(EntityPlayer player) {}
//  }
//
//  private PlayerTracker playerTracker;

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
