package speedytools.clientside.network;

import speedytools.common.network.PacketHandlerRegistry;
import speedytools.common.network.PacketSender;
import speedytools.common.network.Packet250Base;

/**
* Created by TheGreyGhost on 3/04/14.
* Allows the caller to just send a packet to a recipient without worrying about which side it is.
*/
public class PacketSenderClient implements PacketSender
{
  public PacketSenderClient(PacketHandlerRegistry i_packetHandlerRegistry)
  {
    packetHandlerRegistry = i_packetHandlerRegistry;
    bytesSentBacklog = 0;
    lastTime = null;
  }

  @Override
  public boolean sendPacket(Packet250Base packet)
  {
//    System.out.print("PacketSenderClient sendPacket [" + packet.data[0] + "]");
//    if (packet.data[0] == Packet250Types.PACKET250_SELECTION_PACKET.getPacketTypeID()) {
//      System.out.println(" cmd:" + packet.data[5]);
//    } else {
//      System.out.println();
//    }
    packetHandlerRegistry.sendToServer(packet);
    bytesSentBacklog += packet.getPacketSize();
    return true;
  }

  private final int MAXIMUM_KB_PER_SECOND = 50;  // won't queue more than this many packet kb per second.  Later on, might implement better congestion control, but not yet

  @Override
  public boolean readyForAnotherPacket()
  {
    final float NS_PER_S = 1e9F;
    // Later on, might implement better congestion control, but not yet
    long now = System.nanoTime();
    if (lastTime != null) {
      assert now >= lastTime;
      float bytesElapsed = (now - lastTime) / NS_PER_S * MAXIMUM_KB_PER_SECOND * 1000;
      bytesSentBacklog = (Math.round(bytesElapsed) > bytesSentBacklog) ? 0 : (bytesSentBacklog - Math.round(bytesElapsed));
    }
    assert bytesSentBacklog >= 0;
    lastTime = now;
    return (bytesSentBacklog <= MAXIMUM_KB_PER_SECOND);
  }

  private Long lastTime;
  private int bytesSentBacklog;

  PacketHandlerRegistry packetHandlerRegistry;
}
