package speedytools.common.network;

import net.minecraft.network.packet.Packet250CustomPayload;

/**
 * Created by TheGreyGhost on 3/04/14.
 * wraps NetClientHandler and NetServerHandler so that we can just send a packet to it without worrying about which side
 */
public interface PacketSender
{
  public void sendPacket(Packet250CustomPayload packet);
}
