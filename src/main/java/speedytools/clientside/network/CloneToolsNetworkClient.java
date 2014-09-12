package speedytools.clientside.network;

import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import speedytools.common.network.*;
import speedytools.common.utilities.ErrorLog;
import speedytools.common.utilities.QuadOrientation;
import speedytools.common.utilities.ResultWithReason;

/**
* User: The Grey Ghost
* Date: 8/03/14
* Used to send commands to the server, receive status updates from the server
* When issued with a command, keeps trying to contact the server until it receives acknowledgement
* Usage:
* (2) changeClientStatus when the client is interested in whether the server is busy
* (3) informSelectionMade
*     performToolAction
*     performToolUndo
*     These are called when the client needs to send the command to the server.  Once issued, their
*     progress can be followed by calls to getCurrentActionStatus, getCurrentUndoStatus
*     These will progress from WAITING to REJECTED, or to PROCESSING and then to COMPLETED
* (4) The current busy status of the server can be read using getServerStatus and getPercentComplete
* NB tick() must be called at frequent intervals to check for timeouts - at least once per second
*/
public class CloneToolsNetworkClient
{
  public CloneToolsNetworkClient(PacketHandlerRegistry packetHandlerRegistry, PacketSender i_packetSender)
  {
    clientStatus = ClientStatus.IDLE;
    serverStatus = ServerStatus.IDLE;
    lastActionStatus = ActionStatus.NONE_PENDING;
    lastUndoStatus = ActionStatus.NONE_PENDING;
    lastRejectionReason = "";
    packetSender = i_packetSender;

    packetHandlerCloneToolStatus = this.new PacketHandlerCloneToolStatus();
    Packet250CloneToolStatus.registerHandler(packetHandlerRegistry, packetHandlerCloneToolStatus, Side.CLIENT);
//    packetHandlerRegistry.registerHandlerMethod(Side.CLIENT, Packet250Types.PACKET250_TOOL_STATUS_ID.getPacketTypeID(), packetHandlerCloneToolStatus);
    packetHandlerCloneToolAcknowledge = this.new PacketHandlerCloneToolAcknowledge();
    Packet250CloneToolAcknowledge.registerHandler(packetHandlerRegistry, packetHandlerCloneToolAcknowledge, Side.CLIENT);

//    packetHandlerRegistry.registerHandlerMethod(Side.CLIENT, Packet250Types.PACKET250_TOOL_ACKNOWLEDGE_ID.getPacketTypeID(), packetHandlerCloneToolAcknowledge);
  }

  /*
  public void connectedToServer(EntityClientPlayerMP newPlayer)
  {
    player = newPlayer;
    clientStatus = ClientStatus.IDLE;
    serverStatus = ServerStatus.IDLE;
    lastActionStatus = ActionStatus.NONE_PENDING;
    lastUndoStatus = ActionStatus.NONE_PENDING;
  }

  public void disconnect()
  {
    player = null;
  }
*/
  /**
   * Informs the server of the new client status
   */
  public void changeClientStatus(ClientStatus newClientStatus)
  {
//    assert player != null;
    clientStatus = newClientStatus;
    Packet250CloneToolStatus packet = Packet250CloneToolStatus.clientStatusChange(newClientStatus);
    if (packet != null) {
      packetSender.sendPacket(packet);
      lastServerStatusUpdateTime = System.nanoTime();
    }
  }

  /**
   * sends the "Selection Performed" command to the server
   * @return true for success
   */
  public boolean informSelectionMade()
  {
    Packet250CloneToolUse packet = Packet250CloneToolUse.informSelectionMade();
    if (packet != null) {
      packetSender.sendPacket(packet);
      return true;
    }

    return false;
  }

  /**
   * sends the "Tool Action Performed" command to the server
   * @param toolID
   * @param x
   * @param y
   * @param z
   * @param quadOrientation the flipped and rotation status of the placement
   * @return true for success, false otherwise
   */
  public ResultWithReason performComplexToolAction(int toolID, int x, int y, int z, QuadOrientation quadOrientation)
  {
    ResultWithReason result = isReadyToPerformAction();
    if (!result.succeeded()) return result;

    Packet250CloneToolUse packet = Packet250CloneToolUse.performToolAction(currentActionSequenceNumber, toolID, x, y, z, quadOrientation);
    lastActionPacket = packet;
    if (lastActionPacket != null) {
      packetSender.sendPacket(lastActionPacket);
      lastActionStatus = ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT;
      lastActionSentTime = System.nanoTime();
      return ResultWithReason.success();
    }
    return ResultWithReason.failure("I am confused...");
  }

  /**
   * Check whether an action / undo can be started yet
   * @return
   */
  private ResultWithReason isReadyToPerformAction() {return isReadyToPerform(true);}
  private ResultWithReason isReadyToPerformUndo() {return isReadyToPerform(false);}

  private ResultWithReason isReadyToPerform(boolean isAction)
  {
    switch (serverStatus) {
      case IDLE: {
        break;
      }
      case PERFORMING_BACKUP: {
        return ResultWithReason.failure("Must wait for world backup to finish!");
      }
      case PERFORMING_YOUR_ACTION: {
        return ResultWithReason.failure("Must wait for your earlier spell to finish!");
      }
      case UNDOING_YOUR_ACTION: {
        return ResultWithReason.failure("Must wait for your earlier spell to undo!");
      }
      case BUSY_WITH_OTHER_PLAYER: {
        return ResultWithReason.failure("Must wait for " + nameOfPlayerBeingServiced + " to finish!");
      }
      default: assert false : "invalid serverStatus " + serverStatus;
    }

    if (lastUndoStatus != ActionStatus.NONE_PENDING) {
      return ResultWithReason.failure();
    }
    if (isAction && lastActionStatus != ActionStatus.NONE_PENDING) {
      return ResultWithReason.failure();
    }
    return ResultWithReason.success();
  }

/**
* sends the "Tool Undo" command to the server
* undoes the last action (or the action currently in progress)
* @return true for success, false otherwise
*/
  public ResultWithReason performComplexToolUndo()
  {
    Packet250CloneToolUse packet;

    if (lastActionStatus == ActionStatus.PROCESSING || lastActionStatus == ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT) {
      packet = Packet250CloneToolUse.cancelCurrentAction(currentUndoSequenceNumber, currentActionSequenceNumber);
    } else {
      ResultWithReason result = isReadyToPerformUndo();
      if (!result.succeeded()) return result;
      packet = Packet250CloneToolUse.performToolUndo(currentUndoSequenceNumber);
  }
  lastUndoPacket = packet;
  if (lastUndoPacket != null) {
    packetSender.sendPacket(lastUndoPacket);
    lastUndoStatus = ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT;
    lastUndoSentTime = System.nanoTime();
    return ResultWithReason.success();
  }
  return ResultWithReason.failure("I am confused...");
  }

  public byte getServerPercentComplete() {
    return serverPercentComplete;
  }

  public ServerStatus getServerStatus() {
    return serverStatus;
  }

  public String getNameOfPlayerBeingServiced() { return nameOfPlayerBeingServiced;}

  /**
   * respond to an incoming status packet
   * @param player
   * @param packet
   */
  public void handlePacket(EntityClientPlayerMP player, Packet250CloneToolStatus packet)
  {
    serverStatus = packet.getServerStatus();
    serverPercentComplete = packet.getCompletionPercentage();
    nameOfPlayerBeingServiced = packet.getNameOfPlayerBeingServiced();
    lastServerStatusUpdateTime = System.nanoTime();
  }

  /**
   * act on an incoming acknowledgement packet
   * reject any packets which don't match the current sequencenumber
   * reject any packets we're not waiting for
   * @param player
   * @param packet
   */
  public void handlePacket(EntityClientPlayerMP player, Packet250CloneToolAcknowledge packet)
  {
    if (lastActionStatus == ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT || lastActionStatus == ActionStatus.PROCESSING) {
      if (packet.getActionSequenceNumber() == currentActionSequenceNumber) {
        switch (packet.getActionAcknowledgement()) {
          case NOUPDATE: {
            break;
          }
          case REJECTED: {
            lastActionStatus = ActionStatus.REJECTED;
            lastRejectionReason = packet.getReason();
            ++currentActionSequenceNumber;
            break;
          }
          case ACCEPTED: {
            lastActionStatus = ActionStatus.PROCESSING;
            lastActionSentTime = System.nanoTime();
            break;
          }
          case COMPLETED: {
            lastActionStatus = ActionStatus.COMPLETED;
            ++currentActionSequenceNumber;
            break;
          }
          default: {
            ErrorLog.defaultLog().info("Illegal action Acknowledgement in Packet250CloneToolAcknowledgement");
            return;
          }
        }
      }
    }
    if (lastUndoStatus == ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT || lastUndoStatus == ActionStatus.PROCESSING) {
      if (packet.getUndoSequenceNumber() == currentUndoSequenceNumber) {
        switch (packet.getUndoAcknowledgement()) {
          case NOUPDATE: {
            break;
          }
          case REJECTED: {
            lastUndoStatus = ActionStatus.REJECTED;
            lastRejectionReason = packet.getReason();
            ++currentUndoSequenceNumber;
            break;
          }
          case ACCEPTED: {
            lastUndoStatus = ActionStatus.PROCESSING;
            lastUndoSentTime = System.nanoTime();
            break;
          }
          case COMPLETED: {
            lastUndoStatus = ActionStatus.COMPLETED;
            ++currentUndoSequenceNumber;
            break;
          }
          default: {
            ErrorLog.defaultLog().info("Illegal undo Acknowledgement in Packet250CloneToolAcknowledgement");
            return;
          }
        }
      }
    }
  }

  /**
   * Called once per tick to handle timeouts (if no response obtained, send packet again)
   */
  public void tick()
  {
//    if (player == null) return;
    long timenow = System.nanoTime();
    if (lastActionStatus == ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT || lastActionStatus == ActionStatus.PROCESSING) {
      if (timenow - lastActionSentTime > RESPONSE_TIMEOUT_MS * 1000 * 1000) {
        packetSender.sendPacket(lastActionPacket);
        lastActionSentTime = timenow;
      }
    }
    if (lastUndoStatus == ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT || lastUndoStatus == ActionStatus.PROCESSING) {
      if (timenow - lastUndoSentTime > RESPONSE_TIMEOUT_MS * 1000 * 1000) {
        packetSender.sendPacket(lastUndoPacket);
        lastUndoSentTime = timenow;
      }
    }
    if (clientStatus != ClientStatus.IDLE && (timenow - lastServerStatusUpdateTime > RESPONSE_TIMEOUT_MS * 1000 * 1000) ) {
      Packet250CloneToolStatus packet = Packet250CloneToolStatus.clientStatusChange(clientStatus);
      if (packet != null) {
        packetSender.sendPacket(packet);
        lastServerStatusUpdateTime = timenow;
      }
    }

  }

  /**
   * retrieves the status of the action currently being peformed
   * If the status is REJECTED or COMPLETED, it will revert to NONE_PENDING after the call
   * @return
   */
  public ActionStatus getCurrentActionStatus()
  {
    ActionStatus retval = lastActionStatus;
    if (lastActionStatus == ActionStatus.COMPLETED || lastActionStatus == ActionStatus.REJECTED) {
      lastActionStatus = ActionStatus.NONE_PENDING;
    }
    return retval;
  }

  /**
   * retrieves the status of the undo currently being performed
   * If the status is REJECTED or COMPLETED, it will revert to NONE_PENDING after the call
   * @return
   */
  public ActionStatus getCurrentUndoStatus()
  {
    ActionStatus retval = lastUndoStatus;
    if (lastUndoStatus == ActionStatus.COMPLETED || lastUndoStatus == ActionStatus.REJECTED) {
      lastUndoStatus = ActionStatus.NONE_PENDING;
    }
    return retval;
  }

  /** if an action or an undo has been rejected, this may hold a human-readable message
   *    from the server explaining why.
   * @return empty string if no reason given.
   */
  public String getLastRejectionReason() {return lastRejectionReason;}

  /** retrieves the status of the action currently being performed, without
   *   acknowledging a REJECTED or COMPLETED, i.e. unlike getCurrentActionStatus
   *   it won't revert to NONE_PENDING after the call if the status is REJECTED or COMPLETED
   * @return
   */
  public ActionStatus peekCurrentActionStatus()
  {
    return lastActionStatus;
  }

  /** retrieves the status of the undo currently being performed, without
   *   acknowledging a REJECTED or COMPLETED, i.e. unlike getCurrentUndoStatus
   *   it won't revert to NONE_PENDING after the call if the status is REJECTED or COMPLETED
   * @return
   */
  public ActionStatus peekCurrentUndoStatus()
  {
    return lastUndoStatus;
  }

  public class PacketHandlerCloneToolStatus implements Packet250CloneToolStatus.PacketHandlerMethod {
    @Override
    public boolean handlePacket(Packet250CloneToolStatus toolStatusPacket, MessageContext ctx)
    {
      if (toolStatusPacket == null || !toolStatusPacket.validForSide(Side.CLIENT)) return false;
      CloneToolsNetworkClient.this.handlePacket(Minecraft.getMinecraft().thePlayer, toolStatusPacket);
      return true;
    }
  }
  public class PacketHandlerCloneToolAcknowledge implements Packet250CloneToolAcknowledge.PacketHandlerMethod {
    @Override
    public boolean handlePacket(Packet250CloneToolAcknowledge toolAcknowledgePacket, MessageContext ctx)
    {
      if (toolAcknowledgePacket == null || !toolAcknowledgePacket.validForSide(Side.CLIENT)) return false;
      CloneToolsNetworkClient.this.handlePacket(Minecraft.getMinecraft().thePlayer, toolAcknowledgePacket);
      return true;
    }
  }

  private PacketHandlerCloneToolStatus packetHandlerCloneToolStatus;
  private PacketHandlerCloneToolAcknowledge packetHandlerCloneToolAcknowledge;

  //  private EntityClientPlayerMP player;
  private PacketSender packetSender;

  private ClientStatus clientStatus;
  private ServerStatus serverStatus;
  private byte serverPercentComplete;
  private String nameOfPlayerBeingServiced = "";

  private ActionStatus lastActionStatus;
  private ActionStatus lastUndoStatus;
  private String lastRejectionReason;

  private long lastServerStatusUpdateTime;  //time in ns.
  private long lastActionSentTime;          //time in ns.
  private long lastUndoSentTime;            //time in ns.

  private Packet250CloneToolUse lastActionPacket = null;
  private Packet250CloneToolUse lastUndoPacket = null;

  static int currentActionSequenceNumber = 0;
  static int currentUndoSequenceNumber = 0;

  private static final int RESPONSE_TIMEOUT_MS = 2000;  // how long to wait for a response before sending another query

  public static enum ActionStatus
  {
    NONE_PENDING, WAITING_FOR_ACKNOWLEDGEMENT, REJECTED, PROCESSING, COMPLETED;
    public static final ActionStatus[] allValues = {NONE_PENDING, WAITING_FOR_ACKNOWLEDGEMENT, REJECTED, PROCESSING, COMPLETED};
  }

}
