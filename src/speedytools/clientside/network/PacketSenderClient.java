package speedytools.clientside.network;

import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import net.minecraft.network.packet.Packet250CustomPayload;

/**
 * Created by TheGreyGhost on 3/04/14.
 * Allows the caller to just send a packet to a recipient without worrying about which side it is.
 */
public class PacketSenderClient
{
  public void sendPacket(Packet250CustomPayload packet)
  {
    PacketDispatcher.sendPacketToServer(packet);
  }
}
