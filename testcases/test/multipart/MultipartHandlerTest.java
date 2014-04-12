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
 *  4) Verify that packet transmission stops when sender isn't ready
 *     - abort packets are transmitted even when sender isn't ready
 *  5) Start transmission, drop one of the packets, verify that ticking completes it
 *  6) Start transmission, abort the receiver but drop the abort packet, verify that ticking resolves the problem
 *  7) Start transmission, abort the server but drop the abort packet, verify that ticking resolves the problem
 *  8) Complete a transmission, then send one of the ACK packets to the sender, verify ignored
 *  9) Abort a transmission, drop the abort packet, send one of the ACK packets to the sender, verify an abort packet is sent back.
 *  10) Start transmission, drop the segment packets, verify correct resending behaviour by sender:
 *      a) waits, sends an unack packet, waits, resends, repeats until an ACK received
 *      b) sends all the rest of the unack packets
 *  11) Start a transmission, start a second transmission and verify the first is aborted on both sides.
 *  12) Receiver: Verify that incoming packets for already-completed packets are handled correctly:
 *      abort packet sent if it was aborted
 *      full ACK packet sent if it was completed
 *
 *    Error checks sendMultipartPacket:
 *    a) send a packet to completion, then send it again
 *    a) generate three, start (2), send (1)
 *    b) generate three, send (1) to completion, start (3), send (2)
 *    c) packet and linkage mismatch
 *
 *    Error checks Sender.incomingPacket
 *    a) Incoming packetID > newest, during transmission
 *    b) Incoming packetID > newest, while IDLE
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

  @Test
  public void testSendMultipartPacket2() throws Exception
  {
    // test (2)
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
    mpSender.onTick();
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());

    mpSender.abortPacket(senderLinkage);
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    Assert.assertTrue(senderLinkage.abortedCount == 1);
    Assert.assertTrue(receiverLinkage.abortedCount == 1);
  }

  @Test
  public void testSendMultipartPacket3() throws Exception
  {
    // test (3)
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
    mpSender.onTick();
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    mpReceiver.abortPacket(receiverLinkage);
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());

    Assert.assertTrue(senderLinkage.abortedCount == 1);
    Assert.assertTrue(receiverLinkage.abortedCount == 1);
  }

  @Test
  public void testSendMultipartPacket4() throws Exception
  {
    // test (4)
    MultipartPacketTest.MultipartPacketTester sender = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);

    final byte[] TEST_DATA = {10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    sender.setTestData(TEST_DATA);
    final int SEGMENT_COUNT = (TEST_DATA.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;

    senderLinkage = new SenderLinkage(sender);
    mpSender.sendMultipartPacket(senderLinkage, sender);

    senderSender.ready = false;
    for (int i = 0; i < 100; ++i) {
      mpSender.onTick();
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
      while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
      Assert.assertTrue(senderLinkage.completedCount == 0);
      Assert.assertTrue(receiverLinkage.completedCount == 0);
    }
    mpSender.abortPacket(senderLinkage);
    for (int i = 0; i < 100; ++i) {
      mpSender.onTick();
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
      while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
      Assert.assertTrue(senderLinkage.completedCount == 0);
      Assert.assertTrue(receiverLinkage.completedCount == 0);
    }

    Assert.assertTrue(senderLinkage.abortedCount == 1);
    Assert.assertTrue(receiverLinkage.abortedCount == 1);
  }

  @Test
  public void testSendMultipartPacket5() throws Exception
  {
    // test (5)
    MultipartPacketTest.MultipartPacketTester sender = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);

    final byte[] TEST_DATA = {10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    sender.setTestData(TEST_DATA);
    final int SEGMENT_COUNT = (TEST_DATA.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;

    senderLinkage = new SenderLinkage(sender);
    mpSender.sendMultipartPacket(senderLinkage, sender);
    Packet250CustomPayload droppedPacket;
    for (int i= 1; i < SEGMENT_COUNT; ++i) {
      mpSender.onTick();
      if (i == 2) {
        droppedPacket = senderSender.sentPackets.poll();
      }
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
      while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    }
    Assert.assertTrue(receiverLinkage.completedCount == 0);
    Assert.assertTrue(senderLinkage.completedCount == 0);

    final long WAIT_TIME_NS = 1500 * 1000 * 1000;
    long startTime = System.nanoTime();
    while (System.nanoTime() - startTime < WAIT_TIME_NS) {
      mpSender.onTick();
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
      while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    }
    Assert.assertTrue(receiverLinkage.completedCount == 1);
    Assert.assertTrue(senderLinkage.completedCount == 1);
  }

  @Test
  public void testSendMultipartPacket6() throws Exception
  {
    // test (6)
    MultipartPacketTest.MultipartPacketTester sender = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);

    final byte[] TEST_DATA = {10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    sender.setTestData(TEST_DATA);
    final int SEGMENT_COUNT = (TEST_DATA.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;

    senderLinkage = new SenderLinkage(sender);
    mpSender.sendMultipartPacket(senderLinkage, sender);
    Packet250CustomPayload droppedPacket;
    mpSender.onTick();
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    Assert.assertTrue(receiverLinkage.completedCount == 0);
    Assert.assertTrue(senderLinkage.completedCount == 0);

    mpReceiver.abortPacket(receiverLinkage);
    droppedPacket = receiverSender.sentPackets.poll();
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    Assert.assertTrue(receiverLinkage.abortedCount == 1);
    Assert.assertTrue(senderLinkage.abortedCount == 0);

    final long WAIT_TIME_NS = 1500 * 1000 * 1000;
    long startTime = System.nanoTime();
    while (System.nanoTime() - startTime < WAIT_TIME_NS) {
      mpSender.onTick();
      mpReceiver.onTick();
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
      while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    }
    Assert.assertTrue(receiverLinkage.abortedCount == 1);
    Assert.assertTrue(senderLinkage.abortedCount == 1);
  }

  @Test
  public void testSendMultipartPacket7() throws Exception
  {
    // test (7)
    MultipartPacketTest.MultipartPacketTester sender = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);

    final byte[] TEST_DATA = {10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    sender.setTestData(TEST_DATA);
    final int SEGMENT_COUNT = (TEST_DATA.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;

    senderLinkage = new SenderLinkage(sender);
    mpSender.sendMultipartPacket(senderLinkage, sender);
    Packet250CustomPayload droppedPacket;
    mpSender.onTick();
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    Assert.assertTrue(receiverLinkage.completedCount == 0);
    Assert.assertTrue(senderLinkage.completedCount == 0);

    mpSender.abortPacket(senderLinkage);
    droppedPacket = senderSender.sentPackets.poll();
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    Assert.assertTrue(receiverLinkage.abortedCount == 0);
    Assert.assertTrue(senderLinkage.abortedCount == 1);

    final long WAIT_TIME_NS = 1500 * 1000 * 1000;
    long startTime = System.nanoTime();
    while (System.nanoTime() - startTime < WAIT_TIME_NS) {
      mpSender.onTick();
      mpReceiver.onTick();
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
      while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    }
    Assert.assertTrue(receiverLinkage.abortedCount == 1);
    Assert.assertTrue(senderLinkage.abortedCount == 1);
  }

  @Test
  public void testSendMultipartPacket8() throws Exception
  {
    // test (8)
    MultipartPacketTest.MultipartPacketTester sender = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);

    final byte[] TEST_DATA = {10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    sender.setTestData(TEST_DATA);
    final int SEGMENT_COUNT = (TEST_DATA.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;

    senderLinkage = new SenderLinkage(sender);
    mpSender.sendMultipartPacket(senderLinkage, sender);
    Packet250CustomPayload ackPacket = null;
    for (int i= 0; i < SEGMENT_COUNT; ++i) {
      mpSender.onTick();
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
      if (i == 1) ackPacket = receiverSender.sentPackets.poll();
      while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    }
    Assert.assertTrue(senderLinkage.completedCount == 1);
    boolean retval = mpSender.processIncomingPacket(ackPacket);
    Assert.assertTrue(retval == false);

  }

  @Test
  public void testSendMultipartPacket9() throws Exception
  {
    // test (9)
    MultipartPacketTest.MultipartPacketTester sender = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);

    final byte[] TEST_DATA = {10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    sender.setTestData(TEST_DATA);
    final int SEGMENT_COUNT = (TEST_DATA.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;

    senderLinkage = new SenderLinkage(sender);
    mpSender.sendMultipartPacket(senderLinkage, sender);
    Packet250CustomPayload ackPacket = null;
    mpSender.onTick();
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    ackPacket = receiverSender.sentPackets.poll();
    while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    mpSender.abortPacket(senderLinkage);
    senderSender.sentPackets.poll();
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    Assert.assertTrue(receiverLinkage.abortedCount == 0);
    Assert.assertTrue(senderLinkage.abortedCount == 1);
    boolean retval = mpSender.processIncomingPacket(ackPacket);
    Assert.assertTrue(retval == false);
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    Assert.assertTrue(receiverLinkage.abortedCount == 1);
  }

  @Test
  public void testSendMultipartPacket10() throws Exception
  {
    // test (10)
    MultipartPacketTest.MultipartPacketTester sender = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);

    final byte[] TEST_DATA = {10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    sender.setTestData(TEST_DATA);
    final int SEGMENT_COUNT = (TEST_DATA.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;

    senderLinkage = new SenderLinkage(sender);
    mpSender.sendMultipartPacket(senderLinkage, sender);
    Packet250CustomPayload ackPacket = null;
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());

    do {
      mpSender.onTick();
    } while (null != senderSender.sentPackets.poll());

    long timeNow = System.nanoTime();
    int packetCount = 0;
    Packet250CustomPayload lastDroppedPacket = null;
    do {
      mpSender.onTick();
      while (senderSender.sentPackets.peek() != null) {
        lastDroppedPacket = senderSender.sentPackets.poll();
        ++packetCount;
      }
    } while (packetCount < SEGMENT_COUNT - 1);
    long timeTaken = System.nanoTime() - timeNow;
    mpReceiver.processIncomingPacket(lastDroppedPacket);
    while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());

    mpSender.onTick();
    mpSender.onTick();
    mpSender.onTick();
    mpSender.onTick();
    for (packetCount = 0; senderSender.sentPackets.poll() != null; ++packetCount) { };
    Assert.assertTrue(packetCount == SEGMENT_COUNT - 2);
    Assert.assertTrue(senderLinkage.completedCount == 0);
    Assert.assertTrue(receiverLinkage.completedCount == 0);
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    Assert.assertTrue(senderLinkage.completedCount == 1);
    Assert.assertTrue(receiverLinkage.completedCount == 1);
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
