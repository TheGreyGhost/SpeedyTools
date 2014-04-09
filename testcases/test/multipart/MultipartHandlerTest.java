package test.multipart;

import cpw.mods.fml.relauncher.Side;
import junit.framework.Assert;
import net.minecraft.network.packet.Packet250CustomPayload;
import org.junit.Before;
import org.junit.Test;
import speedytools.common.network.PacketSender;
import speedytools.common.network.multipart.MultipartOneAtATimeReceiver;
import speedytools.common.network.multipart.MultipartOneAtATimeSender;
import speedytools.common.network.multipart.MultipartPacket;
import speedytools.common.utilities.ErrorLog;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * User: The Grey Ghost
 * Date: 8/04/14
 * Test plan:
 *  Set up sender and receiver
 *  1) send packet, make sure it arrives intact; check for correct notifications:
 *     a) progress Update calls
 *     b) packetCompleted
 *  2) send packet, sender abort it, verify correct notifications:
 *     a) sender and receiver both get packetAborted calls
 *  3) send packet, receiver abort it, verify correct notifications:
 *     a) sender and receiver both get packetAborted calls
 *
 */
public class MultipartHandlerTest
{
  public final static String TEST_ERROR_LOG = "MultipartPacketHandlerTestErrorLog.log";
  public static final String TEST_TEMP_ROOT_DIRECTORY = "temp";
  public static final  String CHANNEL = "mychannel";
  public static final  byte PACKET_ID = 35;
  public static final  int SEGMENT_SIZE = 4;

  @Before
  public void setUp() {
    Path testdata = Paths.get(TEST_TEMP_ROOT_DIRECTORY).resolve(TEST_ERROR_LOG);
    ErrorLog.setLogFileAsDefault(testdata.toString());

    senderSender = new DummySender();
    receiverSender = new DummySender();
    receiverLinkage = null;
    senderLinkage = null;
    mpSender = new MultipartOneAtATimeSender();
    mpReceiver = new MultipartOneAtATimeReceiver();
    receivedPacket = null;

    mpSender.setPacketSender(senderSender);
    mpReceiver.setPacketSender(receiverSender);
    mpReceiver.registerLinkageFactory(new DummyLinkageFactory());
    mpReceiver.registerPacketCreator(new MultipartPacketCreator());
  }

  @Test
  public void testSendMultipartPacket1() throws Exception
  {
    // test (1)
    MultipartPacketTest.MultipartPacketTester sender = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);

    final byte[] TEST_DATA = {10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    sender.setTestData(TEST_DATA);
    final int SEGMENT_COUNT = (TEST_DATA.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;

    senderLinkage = new SenderLinkage(sender);
    mpSender.sendMultipartPacket(senderLinkage, sender);
    Assert.assertTrue(senderLinkage.completedCount == 0);
    Assert.assertTrue(senderLinkage.abortedCount == 0);
    int lastPercentSender = senderLinkage.percentComplete;
    int lastPercentReceiver = -1;
    for (int i= 0; i < SEGMENT_COUNT; ++i) {
      mpSender.onTick();
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
      while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
      if (senderLinkage.completedCount == 1) {
        Assert.assertTrue(senderLinkage.percentComplete == 100);
        Assert.assertTrue(receiverLinkage.percentComplete == 100);
        Assert.assertTrue(receiverLinkage.completedCount == 1);
        Assert.assertTrue(senderLinkage.abortedCount == 0);
        Assert.assertTrue(receiverLinkage.abortedCount == 0);
      } else {
        Assert.assertTrue(senderLinkage.percentComplete > lastPercentSender);
        Assert.assertTrue(receiverLinkage.percentComplete > lastPercentReceiver);
        Assert.assertTrue(receiverLinkage.completedCount == 0);
        Assert.assertTrue(senderLinkage.abortedCount == 0);
        Assert.assertTrue(receiverLinkage.abortedCount == 0);
        lastPercentSender = senderLinkage.percentComplete;
        lastPercentReceiver = receiverLinkage.percentComplete;
      }
    }
    Assert.assertTrue(receivedPacket.matchesTestData(TEST_DATA));
  }

  public static class DummySender implements PacketSender
  {
    public boolean sendPacket(Packet250CustomPayload packet) {sentPackets.add(packet); return true;}
    public boolean readyForAnotherPacket() {return ready;}
    public boolean ready = true;
    public Queue<Packet250CustomPayload> sentPackets = new LinkedList<Packet250CustomPayload>();
  }

  public static class SenderLinkage implements MultipartOneAtATimeSender.PacketLinkage
  {
    public void progressUpdate(int newPercentComplete) {percentComplete = newPercentComplete;}
    public void packetCompleted() {++completedCount;}
    public void packetAborted() {++abortedCount;}
    public int getPacketID() {return packetUniqueID;}

    public SenderLinkage(MultipartPacket linkedPacket) {packetUniqueID = linkedPacket.getUniqueID();}
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

    public ReceiverLinkage(MultipartPacket linkedPacket) {packetUniqueID = linkedPacket.getUniqueID();}
    public void resetMessages() {percentComplete = -1; completedCount = 0; abortedCount = 0;}
    public int packetUniqueID;
    public int percentComplete = -1;
    public int completedCount = 0;
    public int abortedCount = 0;
  }

  public static class DummyLinkageFactory implements MultipartOneAtATimeReceiver.PacketReceiverLinkageFactory
  {
    public MultipartOneAtATimeReceiver.PacketLinkage createNewLinkage(MultipartPacket linkedPacket)
    {
      receiverLinkage = new ReceiverLinkage(linkedPacket);
      receiverLinkage.resetMessages();
      receiverLinkage.packetUniqueID = linkedPacket.getUniqueID();
      return MultipartHandlerTest.receiverLinkage;
    }
  }

  // derived classes should implement this interface so that other wishing to create a new MultipartPacket (in response to an incoming packet) can pass this object to the packet handler which will invoke it.
  public static class MultipartPacketCreator implements MultipartPacket.MultipartPacketCreator
  {
    public MultipartPacket createNewPacket(Packet250CustomPayload packet)
    {
      receivedPacket = MultipartPacketTest.MultipartPacketTester.createReceiverPacket(packet);
      return receivedPacket;
    }
  }

  public static DummySender senderSender;
  public static DummySender receiverSender;
  public static ReceiverLinkage receiverLinkage;
  public static SenderLinkage senderLinkage;

  public static MultipartOneAtATimeReceiver mpReceiver;
  public static MultipartOneAtATimeSender mpSender;
  public static MultipartPacketTest.MultipartPacketTester receivedPacket;
}
