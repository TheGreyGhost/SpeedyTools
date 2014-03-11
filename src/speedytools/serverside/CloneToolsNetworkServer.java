package speedytools.serverside;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.network.ClientStatus;
import speedytools.common.network.Packet250CloneToolStatus;
import speedytools.common.network.Packet250CloneToolUse;
import speedytools.common.network.ServerStatus;
import speedytools.common.utilities.ErrorLog;

import java.util.HashMap;
import java.util.Map;

/**
 * User: The Grey Ghost
 * Date: 8/03/14
 */
public class CloneToolsNetworkServer
{
  public CloneToolsNetworkServer(CloneToolServerActions i_cloneToolServerActions)
  {
    playerStatuses = new HashMap<EntityPlayerMP, ClientStatus>();
    mostRecentAcceptedAction = new HashMap<EntityPlayerMP, Integer>();
    mostRecentRejectedAction = new HashMap<EntityPlayerMP, Integer>();
    mostRecentAcceptedUndo = new HashMap<EntityPlayerMP, Integer>();
    mostRecentRejectedUndo = new HashMap<EntityPlayerMP, Integer>();

    cloneToolServerActions = i_cloneToolServerActions;
    cloneToolServerActions.setCloneToolsNetworkServer(this);
  }

  public void addPlayer(EntityPlayerMP newPlayer)
  {
    if (!playerStatuses.containsKey(newPlayer)) {
      playerStatuses.put(newPlayer, ClientStatus.IDLE);
      mostRecentAcceptedAction.put(newPlayer, Integer.MIN_VALUE);
      mostRecentRejectedAction.put(newPlayer, Integer.MIN_VALUE);
      mostRecentAcceptedUndo.put(newPlayer, Integer.MIN_VALUE);
      mostRecentRejectedUndo.put(newPlayer, Integer.MIN_VALUE);
    }
  }

  public void removePlayer(EntityPlayerMP whichPlayer)
  {
    playerStatuses.remove(whichPlayer);
  }

  /**
   * Changes the server status and informs all clients who are interested in it.
   * @param newServerStatus
   * @param newServerPercentComplete
   */
  public void changeServerStatus(ServerStatus newServerStatus, EntityPlayerMP newPlayerBeingServiced, byte newServerPercentComplete)
  {
    assert (newServerPercentComplete >= 0 && newServerPercentComplete <= 100);
    if (newServerStatus == serverStatus && newPlayerBeingServiced == playerBeingServiced && newServerPercentComplete == serverPercentComplete) {
      return;
    }

    serverStatus = newServerStatus;
    serverPercentComplete = newServerPercentComplete;
    playerBeingServiced = newPlayerBeingServiced;
    for (Map.Entry<EntityPlayerMP,ClientStatus> playerStatus : playerStatuses.entrySet()) {
      if (playerStatus.getValue() != ClientStatus.IDLE) {
        sendUpdateToClient(playerStatus.getKey());
      }
    }
  }

  /**
   * update the most recent action status(es) for the given player
   * @param player
   * @param newSequenceNumber
   * @param accepted true if the action was accepted
   */
  public void updateMostRecentAction(EntityPlayerMP player, int newSequenceNumber, boolean accepted)
  {
    if (accepted) {
      mostRecentAcceptedAction.put(player, newSequenceNumber);
    } else {
      mostRecentRejectedUndo.put(player, newSequenceNumber);
    }
  }

  /**
   * update the most recent undo status(es) for the given player
   * @param player
   * @param newSequenceNumber
   * @param accepted  true if the undo was accepted
   */
  public void updateMostRecentUndo(EntityPlayerMP player, int newSequenceNumber, boolean accepted)
  {
   if (accepted) {
     mostRecentAcceptedUndo.put(player, newSequenceNumber);
   } else {
     mostRecentRejectedUndo.put(player, newSequenceNumber);
   }
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

    if (   !mostRecentAcceptedAction.containsKey(player) || !mostRecentRejectedAction.containsKey(player)
        || !mostRecentAcceptedUndo.containsKey(player) || !mostRecentRejectedUndo.containsKey(player)    ) {
      ErrorLog.defaultLog().warning("mostRecentxxx maps did not contain player");
      return;
    }

    Packet250CloneToolStatus packet = Packet250CloneToolStatus.serverStatusChange(serverStatusForThisPlayer, serverPercentComplete,
            mostRecentAcceptedAction.get(player),
            mostRecentRejectedAction.get(player),
            mostRecentAcceptedUndo.get(player),
            mostRecentRejectedUndo.get(player)
    );
    Packet250CustomPayload packet250 = packet.getPacket250CustomPayload();
    if (packet250 != null) {
      player.playerNetServerHandler.sendPacketToPlayer(packet250);
    }
  }

  /**
   *  send a packet back to the client acknowledging their Action, with success or failure
    * @param packet the action packet sent by the client
   * @param successfulStart true if the action has been started on the server, false if not
   */
  private void acknowledgeAction(EntityPlayerMP player, Packet250CloneToolUse packet, boolean successfulStart)
  {
    packet.setCommandSuccessfullyStarted(successfulStart);
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
      case PERFORM_TOOL_ACTION: {
        boolean successfulStart = false;
        if (serverStatus == ServerStatus.IDLE) {
          successfulStart = cloneToolServerActions.performToolAction(player, packet.getToolID(), packet.getSequenceNumber(),
                                                                      packet.getXpos(), packet.getYpos(), packet.getZpos(),
                                                                      packet.getRotationCount(), packet.isFlipped());
        }
//        acknowledgeAction(player, packet, successfulStart);
        updateMostRecentAction(player, packet.getSequenceNumber(), successfulStart);
        sendUpdateToClient(player);
        break;
      }
      case PERFORM_TOOL_UNDO: {
        boolean successfulUndoStart = false;
        if (serverStatus == ServerStatus.IDLE || playerBeingServiced == player) {
          successfulUndoStart = cloneToolServerActions.performUndoAction(player, packet.getSequenceNumber());
        }
//        acknowledgeAction(player, packet, successfulUndoStart);
        updateMostRecentUndo(player, packet.getSequenceNumber(), successfulUndoStart);
        sendUpdateToClient(player);
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
  public void handlePacket(EntityPlayerMP player, Packet250CloneToolStatus packet)
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

  private Map<EntityPlayerMP, ClientStatus> playerStatuses;
  private Map<EntityPlayerMP, Integer> mostRecentAcceptedAction;
  private Map<EntityPlayerMP, Integer> mostRecentRejectedAction;
  private Map<EntityPlayerMP, Integer> mostRecentAcceptedUndo;
  private Map<EntityPlayerMP, Integer> mostRecentRejectedUndo;

  private ServerStatus serverStatus;
  private byte serverPercentComplete;
  private CloneToolServerActions cloneToolServerActions;
  private EntityPlayerMP playerBeingServiced;
}
