package speedytools.clientside;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.network.ClientStatus;
import speedytools.common.network.Packet250CloneToolUse;
import speedytools.common.network.Packet250ToolActionStatus;
import speedytools.common.network.ServerStatus;

/**
 * User: The Grey Ghost
 * Date: 8/03/14
 * Used to send commands to the server and
 */
public class CloneToolsNetworkClient
{
  public CloneToolsNetworkClient()
  {
    serverStatus = ServerStatus.IDLE;
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
    Packet250ToolActionStatus packet = Packet250ToolActionStatus.clientStatusChange(newClientStatus);
    Packet250CustomPayload packet250 = packet.getPacket250CustomPayload();
    if (packet250 != null) {
      player.sendQueue.addToSendQueue(packet250);
    }
  }

  /**
   * sends the "Selection Performed" command to the server
   * @return a unique sequence number for this command
   */
  public int toolSelectionPerformed()
  {
    Packet250CloneToolUse packet = Packet250CloneToolUse.toolSelectionPerformed();
    Packet250CustomPayload packet250 = packet.getPacket250CustomPayload();
    if (packet250 != null) {
      player.sendQueue.addToSendQueue(packet250);
    }

    return packet.getSequenceNumber();
  }

  /**
   * sends the "Tool Action Performed" command to the server
   * @param toolID
   * @param x
   * @param y
   * @param z
   * @param rotationCount number of quadrants rotated clockwise
   * @param flipped true if flipped left-right
   * @return a unique sequence number for this command
   */
  public int toolActionPerformed(int toolID, int x, int y, int z, byte rotationCount, boolean flipped)
  {
    Packet250CloneToolUse packet = Packet250CloneToolUse.toolActionPerformed(toolID, x, y, z, rotationCount, flipped);
    Packet250CustomPayload packet250 = packet.getPacket250CustomPayload();
    if (packet250 != null) {
      player.sendQueue.addToSendQueue(packet250);
    }

    return packet.getSequenceNumber();
  }

  /**
   * sends the "Tool Undo" command to the server
   * @param sequenceNumber = the sequence number of the action to be undone
   * @return a unique sequence number for this command
   */
  public int toolUndoPerformed(int sequenceNumber)
  {
    Packet250CloneToolUse packet = Packet250CloneToolUse.toolUndoPerformed(sequenceNumber);
    Packet250CustomPayload packet250 = packet.getPacket250CustomPayload();
    if (packet250 != null) {
      player.sendQueue.addToSendQueue(packet250);
    }

    return packet.getSequenceNumber();
  }

  public byte getServerPercentComplete() {
    return serverPercentComplete;
  }

  public ServerStatus getServerStatus() {
    return serverStatus;
  }

  /**
   * respond to an incoming action packet.
   * @param player
   * @param packet
   */
  public void handlePacket(EntityPlayerMP player, Packet250CloneToolUse packet)
  {
    System.out.println("Command #" + packet.getSequenceNumber() + (packet.isCommandSuccessfullyStarted() ? " succeeded" : "failed"));  // todo: remove
  }

  /**
   * respond to an incoming status packet
   * @param player
   * @param packet
   */
  public void handlePacket(EntityPlayerMP player, Packet250ToolActionStatus packet)
  {
    serverStatus = packet.getServerStatus();
    serverPercentComplete = packet.getCompletionPercentage();
  }

  private EntityClientPlayerMP player;

  private ServerStatus serverStatus;
  private byte serverPercentComplete;
}
