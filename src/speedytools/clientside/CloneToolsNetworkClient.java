package speedytools.clientside;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.network.ClientStatus;
import speedytools.common.network.Packet250CloneToolUse;
import speedytools.common.network.Packet250ToolActionStatus;
import speedytools.common.network.ServerStatus;
import speedytools.common.utilities.ErrorLog;
import speedytools.serverside.CloneToolServerActions;

import java.util.HashMap;
import java.util.Map;

/**
 * User: The Grey Ghost
 * Date: 8/03/14
 * Used to send commands to the server and
 */
public class CloneToolsNetworkClient
{
  public CloneToolsNetworkClient()
  {
  }

  public void connectedToServer(EntityClientPlayerMP newPlayer)
  {
    player = newPlayer;
  }

  public void disconnect()
  {
    player = null;
  }

  /**
   * Informs the server of the new client status
   */
  public void changeClientStatus(ClientStatus newClientStatus)
  {
    assert player != null;

  }

  /**
   * Send the appropriate update status packet to this player
   * @param player
   */
  private void sendUpdateToClient(EntityPlayerMP player)
  {
    ServerStatus serverStatusForThisPlayer = serverStatus;
    if (player != playerBeingServiced) {
      switch (serverStatus) {
        case IDLE:
        case PERFORMING_BACKUP: {
          break;
        }
        case PERFORMING_YOUR_ACTION:
        case UNDOING_YOUR_ACTION: {
          serverStatusForThisPlayer = ServerStatus.BUSY_WITH_OTHER_PLAYER;
          break;
        }
        default:
          assert false: "Invalid serverStatus";
      }
    }
    Packet250ToolActionStatus packet = Packet250ToolActionStatus.updateCompletionPercentage(serverStatusForThisPlayer, serverPercentComplete);
    Packet250CustomPayload packet250 = packet.getPacket250CustomPayload();
    if (packet250 != null) {
      player.playerNetServerHandler.sendPacketToPlayer(packet250);
    }
  }

  /**
   * respond to an incoming action packet.
   * If the server is not busy with something else, perform the action, otherwise send the appropriate non-idle packet back to the caller
   * @param player
   * @param packet
   */
  public void handlePacket(EntityPlayerMP player, Packet250CloneToolUse packet)
  {
    switch (packet.getCommand()) {
      case SELECTION_MADE: {
        cloneToolServerActions.prepareForToolAction();
        break;
      }
      case TOOL_ACTION_PERFORMED: {
        if (serverStatus == ServerStatus.IDLE) {
          cloneToolServerActions.performToolAction(player, packet.getToolID(),
                                                    packet.getXpos(), packet.getYpos(), packet.getZpos(),
                                                    packet.getRotationCount(), packet.isFlipped());
        } else {
          sendUpdateToClient(player);
        }
        break;
      }
      case TOOL_UNDO_PERFORMED: {
        if (serverStatus == ServerStatus.IDLE || playerBeingServiced == player) {
          cloneToolServerActions.performUndoAction(player, packet.getToolID());
        } else {
          sendUpdateToClient(player);
        }
        break;
      }
      default: {
        assert false: "Invalid server side packet";
      }
    }

  }

  /**
   * update the status of the appropriate client; replies with the server status if the client is interested
   * @param player
   * @param packet
   */
  public void handlePacket(EntityPlayerMP player, Packet250ToolActionStatus packet)
  {
    ClientStatus newStatus = packet.getClientStatus();

    if (!playerStatuses.containsKey(player)) {
      ErrorLog.defaultLog().warning("CloneToolsNetworkServer:: Packet received from player not in playerStatuses");
      return;
    }
    playerStatuses.put(player, newStatus);

    if (newStatus != ClientStatus.IDLE) {
      sendUpdateToClient(player);
    }

  }

  private EntityClientPlayerMP player;

  private Map<EntityPlayerMP, ClientStatus> playerStatuses;
  private ServerStatus serverStatus;
  private byte serverPercentComplete;
  private CloneToolServerActions cloneToolServerActions;
  private EntityPlayerMP playerBeingServiced;
}
