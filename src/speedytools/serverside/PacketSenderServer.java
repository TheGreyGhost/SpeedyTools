package speedytools.serverside;

import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.network.PacketSender;

/**
 * Created by TheGreyGhost on 3/04/14.
 * Allows the caller to just send a packet to a recipient without worrying about which side it is.
 */
public class PacketSenderServer implements PacketSender
{
  public PacketSenderServer(Player player)
  {
    thePlayer = player;
  }

  public void sendPacket(Packet250CustomPayload packet)
  {
    PacketDispatcher.sendPacketToPlayer(packet, thePlayer);
  }

  private Player thePlayer;
}
