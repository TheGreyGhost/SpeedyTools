package speedytools.clientside.tools;

import cpw.mods.fml.relauncher.Side;
import speedytools.clientside.selections.BlockVoxelMultiSelector;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.PacketHandlerRegistry;
import speedytools.common.network.PacketSender;
import speedytools.common.network.multipart.MultipartOneAtATimeSender;
import speedytools.common.network.multipart.SelectionPacket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * User: The Grey Ghost
 * Date: 28/04/2014
 * Sends the SelectionPacket to the server.
 * Usage:
 * (1) Create with the PacketSender
 * (2) To send a new Selection: startSendingSelection()
 *     Monitor its progress using getCurrentPacketPercentComplete and getCurrentPacketProgress:
 *     IDLE means there is no valid selection sent, and currently not sending anything
 *     SENDING means a transmission is in progress
 *     COMPLETED means that the transmission is complete and the server has a valid copy
 *     ABORTED means that the transmission was aborted and the server has no valid copy
 * (3) To abort the current transmission, call abortSending()
 * (4) reset() is used to invalidate the current selection (return to IDLE)
 * (5) call tick() frequently to handle packet sending and timeouts
 */
public class SelectionPacketSender
{
  public SelectionPacketSender(PacketSender packetSender)
  {
    multipartOneAtATimeSender = new MultipartOneAtATimeSender();
    multipartOneAtATimeSender.setPacketSender(packetSender);
    currentPacketProgress = PacketProgress.IDLE;
    packetLinkage = null;
  }

  public void reset()
  {
    if (currentPacketProgress == PacketProgress.SENDING) {
      abortSending();
    }
    currentPacketProgress = PacketProgress.IDLE;
  }

  /** start sending the selection to the server
   *
   * @param selection
   * @return true for successful start
   */
  public boolean startSendingSelection(BlockVoxelMultiSelector selection)
  {
    currentPacketProgress = PacketProgress.IDLE;
    SelectionPacket newSelectionPacket =  SelectionPacket.createSenderPacket(selection);
    if (newSelectionPacket == null) {
      packetLinkage = null;
      return false;
    }

    packetLinkage = this.new PacketLinkage(newSelectionPacket);
    boolean success = multipartOneAtATimeSender.sendMultipartPacket(packetLinkage, newSelectionPacket);
    currentPacketProgress = success ? PacketProgress.SENDING : PacketProgress.IDLE;
    return success;
  }

  /** abort the current selection
   */
  public void abortSending()
  {
    if (packetLinkage != null) {
      multipartOneAtATimeSender.abortPacket(packetLinkage);
      packetLinkage = null;
    }
  }

  /** call every tick to progress the sending of the selection
   *
   */
  public void tick()
  {
    multipartOneAtATimeSender.onTick();
  }

  private class PacketLinkage implements MultipartOneAtATimeSender.PacketLinkage
  {
    public PacketLinkage(SelectionPacket selectionPacket) {
      packetID = selectionPacket.getUniqueID();
    }
    public void progressUpdate(int percentComplete) {currentPacketPercentComplete = percentComplete;}
    public void packetCompleted() {if (currentPacketProgress == PacketProgress.SENDING) currentPacketProgress = PacketProgress.COMPLETED;}
    public void packetAborted() {if (currentPacketProgress == PacketProgress.SENDING) currentPacketProgress = PacketProgress.ABORTED;}
    public int getPacketID() {return packetID;}
    private int packetID;
  }

  public int getCurrentPacketPercentComplete() {
    return currentPacketPercentComplete;
  }

  public PacketProgress getCurrentPacketProgress() {
    return currentPacketProgress;
  }

  private int currentPacketPercentComplete;
  public enum PacketProgress {IDLE, SENDING, COMPLETED, ABORTED};
  private PacketProgress currentPacketProgress;

  MultipartOneAtATimeSender multipartOneAtATimeSender;
  PacketLinkage packetLinkage;
}
