package speedytools.common;

import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.network.ClientStatus;
import speedytools.common.network.Packet250ToolActionStatus;
import speedytools.common.network.ServerStatus;

/**
 * Created by TheGreyGhost on 7/03/14.
 *
 * Used to keep the Client and Server informed of each others' status
 * Not used to trigger actions.
 * The Client sends a changeClientState call, which triggers a response from the server if the status is WAITING.
 */
public class CloneToolActionStatus
{
  public CloneToolActionStatus(EntityPlayerMP player)
  {
    whichSide = Side.SERVER;
    entityPlayerMP = player;
  }

  public CloneToolActionStatus(EntityClientPlayerMP player)
  {
    whichSide = Side.CLIENT;
    entityClientPlayerMP = player;
  }

  public boolean changeServerState(ServerStatus newStatus, byte percentCompletion)
  {
    assert (whichSide == Side.SERVER);
    assert (percentCompletion >= 0 && percentCompletion <= 100);
    serverStatus = newStatus;
    serverPercentComplete = percentCompletion;
    boolean success;
    if (clientStatus == ClientStatus.IDLE) return true;
    success = sendPacketToClient();
    return success;
  }

  public boolean changeClientState(ClientStatus newStatus)
  {
    assert (whichSide == Side.CLIENT);
    clientStatus = newStatus;
    boolean success;
    success = sendPacketToServer();


  }

  public void updateStateFromPacket(Packet250ToolActionStatus packet) {
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

  private boolean sendPacketToServer()
  {
    Packet250ToolActionStatus packet = Packet250ToolActionStatus.updateCompletionPercentage(serverStatus, serverPercentComplete);
    Packet250CustomPayload packet250 = packet.getPacket250CustomPayload();
    if (packet250 == null) return false;
    entityPlayerMP.playerNetServerHandler.sendPacketToPlayer(packet250);
    return true;
  }

  private boolean sendPacketToClient()
  {
    Packet250ToolActionStatus packet = Packet250ToolActionStatus.clientStatusChange(clientStatus);
    Packet250CustomPayload packet250 = packet.getPacket250CustomPayload();
    if (packet250 == null) return false;
    entityClientPlayerMP.sendQueue.addToSendQueue(packet250);
    return true;
  }

  public ClientStatus getClientStatus() {
    return clientStatus;
  }

  public ServerStatus getServerStatus() {
    return serverStatus;
  }

  public byte getServerPercentComplete() {
    return serverPercentComplete;
  }

  private ClientStatus clientStatus = ClientStatus.IDLE;
  private ServerStatus serverStatus = ServerStatus.IDLE;
  private byte serverPercentComplete = 100;

  EntityPlayerMP entityPlayerMP;
  EntityClientPlayerMP entityClientPlayerMP;
  Side whichSide;
}
