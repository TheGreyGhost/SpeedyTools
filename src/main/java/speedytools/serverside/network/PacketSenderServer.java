//package speedytools.serverside.network;
//
//import cpw.mods.fml.common.network.PacketDispatcher;
//import cpw.mods.fml.common.network.Player;
//import net.minecraft.entity.player.EntityPlayerMP;
//import net.minecraft.network.packet.Packet250CustomPayload;
//import speedytools.common.network.Packet250Types;
//import speedytools.common.network.PacketSender;
//
///**
// * Created by TheGreyGhost on 3/04/14.
// * Allows the caller to just send a packet to a recipient without worrying about which side it is.
// */
//public class PacketSenderServer implements PacketSender
//{
//  public PacketSenderServer(EntityPlayerMP player)
//  {
//    thePlayer = (Player)player;
//    bytesSentBacklog = 0;
//    lastTime = null;
//  }
//
//  @Override
//  public boolean sendPacket(Packet250CustomPayload packet)
//  {
////    System.out.print("PacketSenderServer sendPacket [" + packet.data[0] + "]");
////    if (packet.data[0] == Packet250Types.PACKET250_SELECTION_PACKET.getPacketTypeID()) {
////      System.out.println(" cmd:" + packet.data[5]);
////    } else {
////      System.out.println();
////    }
//
//    PacketDispatcher.sendPacketToPlayer(packet, thePlayer);
//    bytesSentBacklog += packet.length;
//    return true;
//  }
//
//  private final int MAXIMUM_KB_PER_SECOND = 50;  // won't queue more than this many kb of packets per second.  Later on, might implement better congestion control, but not yet
//
//  @Override
//  public boolean readyForAnotherPacket()
//  {
//    final float NS_PER_S = 1e9F;
//    // Later on, might implement better congestion control, but not yet
//    long now = System.nanoTime();
//    if (lastTime != null) {
//      assert now >= lastTime;
//      float kilobytesElapsed = (now - lastTime) / NS_PER_S * MAXIMUM_KB_PER_SECOND;
//      bytesSentBacklog = (kilobytesElapsed < bytesSentBacklog) ? 0 : bytesSentBacklog - Math.round(kilobytesElapsed);
//    }
//    assert bytesSentBacklog >= 0;
//    lastTime = now;
//    return (bytesSentBacklog <= MAXIMUM_KB_PER_SECOND);
//  }
//
//  private Player thePlayer;
//  private Long lastTime;
//  private int bytesSentBacklog;
//}
