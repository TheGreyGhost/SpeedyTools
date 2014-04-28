package speedytools.clientside.tools;

import speedytools.clientside.selections.BlockVoxelMultiSelector;
import speedytools.common.network.PacketSender;
import speedytools.common.network.multipart.MultipartOneAtATimeSender;
import speedytools.common.network.multipart.SelectionPacket;

/**
 * User: The Grey Ghost
 * Date: 28/04/2014
 */
public class SelectionPacketSender
{
  public SelectionPacketSender(PacketSender packetSender)
  {
    multipartOneAtATimeSender = new MultipartOneAtATimeSender();
    multipartOneAtATimeSender.setPacketSender(packetSender);
    currentPacketProgress = PacketProgress.IDLE;
  }

  public boolean sendSelection(BlockVoxelMultiSelector selection)
  {
    SelectionPacket newSelectionPacket = new SelectionPacket(selection);

  }

  public void tick()
  {
    multipartOneAtATimeSender.onTick();
  }

  private class PacketLinkage implements MultipartOneAtATimeSender.PacketLinkage
  {
    public PacketLinkage(SelectionPacket selectionPacket) {
      packetID = selectionPacket.getUniqueID();
    }
    public void progressUpdate(int percentComplete);
    public void packetCompleted();
    public void packetAborted();
    public int getPacketID() {return packetID;}
    private int packetID;
  }

  private int currentPacketPercentComplete;
  public enum PacketProgress {IDLE, SENDING, COMPLETED, ABORTED};
  private PacketProgress currentPacketProgress;

  MultipartOneAtATimeSender multipartOneAtATimeSender;
}
