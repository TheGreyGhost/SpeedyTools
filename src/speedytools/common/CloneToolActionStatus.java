package speedytools.common;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.network.Packet250SpeedyToolUse;
import speedytools.common.network.Packet250ToolActionStatus;

import java.io.IOException;

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
    whichPlayer = player;
  }

  public CloneToolActionStatus(EntityClientPlayerMP player)
  {
    whichSide = Side.CLIENT;
    whichPlayer = player;
  }

  public boolean changeServerState(ServerStatus newStatus, byte percentCompletion)
  {
    assert (whichSide == Side.SERVER);
    assert (percentCompletion >= 0 && percentCompletion <= 100);
    serverStatus = newStatus;
    return sendPacketToClient();
  }

  public boolean changeClientState(ClientStatus newStatus)
  {


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
    PacketDispatcher.sendPacketToPlayer(packet250, whichPlayer);
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

  public enum ClientStatus {
    IDLE, WAITING_FOR_ACTION_COMPLETE;

    public static final ClientStatus[] allValues = {IDLE, WAITING_FOR_ACTION_COMPLETE};
  }

  public enum ServerStatus {
    IDLE, PERFORMING_BACKUP, PERFORMING_YOUR_ACTION, UNDOING_YOUR_ACTION, BUSY_WITH_OTHER_PLAYER;

    public static final ServerStatus[] allValues = {IDLE, PERFORMING_BACKUP, PERFORMING_YOUR_ACTION, UNDOING_YOUR_ACTION, BUSY_WITH_OTHER_PLAYER};
  }

  private ClientStatus clientStatus = ClientStatus.IDLE;
  private ServerStatus serverStatus = ServerStatus.IDLE;
  private byte serverPercentComplete = 100;

  EntityPlayer whichPlayer;
  Side whichSide;
}
