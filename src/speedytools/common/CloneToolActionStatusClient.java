package speedytools.common;

import cpw.mods.fml.relauncher.Side;
import speedytools.common.network.ClientStatus;
import speedytools.common.network.Packet250CloneToolStatus;

/**
 * User: The Grey Ghost
 * Date: 8/03/14
 */
public class CloneToolActionStatusClient
{
  public static boolean changeClientState(ClientStatus newStatus)
  {
    return sendPacketToServer();
  }

  public void updateStateFromPacket(Packet250CloneToolStatus packet) {
    if (packet.validForSide(Side.CLIENT)) {
      serverStatus =  packet.getServerStatus();
      serverPercentComplete = packet.getCompletionPercentage();
    } else {
      assert (packet.validForSide(Side.SERVER));
      clientStatus = packet.getClientStatus();
      if (clientStatus != ClientStatus.IDLE) {
        sendPacketToClient();
      }
    }
  }

  static CloneToolActionStatus cloneToolActionStatus;
}
