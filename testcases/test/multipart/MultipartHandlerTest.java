package test.multipart;

import net.minecraft.network.packet.Packet250CustomPayload;
import org.junit.Test;
import speedytools.common.network.PacketSender;
import speedytools.common.network.multipart.MultipartOneAtATimeReceiver;
import speedytools.common.network.multipart.MultipartOneAtATimeSender;

import java.util.ArrayList;

/**
 * User: The Grey Ghost
 * Date: 8/04/14
 * Test plan:
 *
 */
public class MultipartHandlerTest
{
  @Test
  public void testSendMultipartPacket() throws Exception {

  }



  public static class DummySender implements PacketSender
  {
    public boolean sendPacket(Packet250CustomPayload packet) {sentPackets.add(packet); return true;}
    public boolean readyForAnotherPacket() {return ready;}
    public boolean ready = true;
    public ArrayList<Packet250CustomPayload> sentPackets = new ArrayList<Packet250CustomPayload>();
  }

  public static class SenderLinkage implements MultipartOneAtATimeSender.PacketLinkage
  {
    public void progressUpdate(int newPercentComplete) {percentComplete = newPercentComplete;}
    public void packetCompleted() {++completedCount;}
    public void packetAborted() {++abortedCount;}
    public int getPacketID() {return packetUniqueID;}

    public void resetMessages() {percentComplete = -1; completedCount = 0; abortedCount = 0;}
    public int packetUniqueID;
    public int percentComplete = -1;
    public int completedCount = 0;
    public int abortedCount = 0;
  }

  public static class ReceiverLinkage implements MultipartOneAtATimeReceiver.PacketLinkage
  {
    public void progressUpdate(int newPercentComplete) {percentComplete = newPercentComplete;}
    public void packetCompleted() {++completedCount;}
    public void packetAborted() {++abortedCount;}
    public int getPacketID() {return packetUniqueID;}

    public void resetMessages() {percentComplete = -1; completedCount = 0; abortedCount = 0;}
    public int packetUniqueID;
    public int percentComplete = -1;
    public int completedCount = 0;
    public int abortedCount = 0;
  }

  public static class DummaryLinkageFactory implements MultipartOneAtATimeReceiver.PacketReceiverLinkageFactory
  {
    public MultipartOneAtATimeReceiver.PacketLinkage createNewLinkage()
    {
      return MultipartHandlerTest.receiverLinkage;
    }
  }



  public static DummySender senderSender;
  public static DummySender receiverSender;

  public static ReceiverLinkage receiverLinkage;

}
