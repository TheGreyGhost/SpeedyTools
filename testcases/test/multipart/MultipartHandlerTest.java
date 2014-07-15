package test.multipart;

import cpw.mods.fml.relauncher.Side;
import org.junit.Assert;
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
 *     - verify that receiver correctly responds to an abort packet even if it hasn't received any segments for it yet
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
 *      12a) abort packet sent if it was aborted (verify sender aborts after tick)
 *      12b)full ACK packet sent if it was completed (verify sender completes after tick)
 *
 *  13) Error checks sendMultipartPacket:
 *    a) send a packet to completion, then send it again
 *    b) generate three, start (2), send (1)
 *    c) generate three, send (1) to completion, start (3), send (2)
 *    d) packet and linkage mismatch
 *
 *  14)  Error checks Sender.incomingPacket
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
    senderSender.ready = false;
    mpSender.sendMultipartPacket(senderLinkage, sender);

    for (int i = 0; i < 100; ++i) {
      mpSender.onTick();
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
      while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
      Assert.assertTrue(senderLinkage.completedCount == 0);
      Assert.assertTrue(receiverLinkage == null || receiverLinkage.completedCount == 0);
    }
    mpSender.abortPacket(senderLinkage);
    for (int i = 0; i < 100; ++i) {
      mpSender.onTick();
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
      while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
      Assert.assertTrue(senderLinkage.completedCount == 0);
      Assert.assertTrue(receiverLinkage == null || receiverLinkage.completedCount == 0);
    }

    Assert.assertTrue(senderLinkage.abortedCount == 1);
    Assert.assertTrue(receiverLinkage == null || receiverLinkage.abortedCount == 1);
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
    } while (packetCount < SEGMENT_COUNT);
    long timeTaken = System.nanoTime() - timeNow;
    mpReceiver.processIncomingPacket(lastDroppedPacket);
    while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());

    for (int i = 0; i < SEGMENT_COUNT; ++i ) {
      mpSender.onTick();
    }

    packetCount = senderSender.sentPackets.size();
    Assert.assertTrue(packetCount == SEGMENT_COUNT - 1);
    Assert.assertTrue(senderLinkage.completedCount == 0);
    Assert.assertTrue(receiverLinkage.completedCount == 0);
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    Assert.assertTrue(senderLinkage.completedCount == 1);
    Assert.assertTrue(receiverLinkage.completedCount == 1);
  }

  @Test
  public void testSendMultipartPacket11() throws Exception
  {
    // test (11)
    MultipartPacketTest.MultipartPacketTester sender1 = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    MultipartPacketTest.MultipartPacketTester sender2 = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);

    final byte[] TEST_DATA1 = {10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    final byte[] TEST_DATA2 = {53, 10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    final int SEGMENT_COUNT2 = (TEST_DATA2.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;

    sender1.setTestData(TEST_DATA1);
    sender2.setTestData(TEST_DATA2);

    SenderLinkage senderLinkage1 = new SenderLinkage(sender1);
    SenderLinkage senderLinkage2 = new SenderLinkage(sender2);
    mpSender.sendMultipartPacket(senderLinkage1, sender1);
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());

    ReceiverLinkage receiverLinkageBackup = receiverLinkage;
    mpSender.onTick();
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());

    mpSender.sendMultipartPacket(senderLinkage2, sender2);
    Assert.assertTrue(senderLinkage1.abortedCount == 1);
    Assert.assertTrue(senderLinkage2.abortedCount == 0);
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    Assert.assertTrue(receiverLinkageBackup.abortedCount == 1);
    Assert.assertTrue(receiverLinkage.abortedCount == 0);

    for (int i = 0; i < SEGMENT_COUNT2; ++i ) {
      mpSender.onTick();
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
      while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    }
    Assert.assertTrue(senderLinkage2.completedCount == 1);
    Assert.assertTrue(receiverLinkage.completedCount == 1);
    Assert.assertTrue(receivedPacket.matchesTestData(TEST_DATA2));
  }

  @Test
  public void testSendMultipartPacket12a() throws Exception
  {
    // test (12a)
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
        mpReceiver.abortPacket(receiverLinkage);
      }
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    }
    Assert.assertTrue(receiverLinkage.abortedCount == 1);
    Assert.assertTrue(senderLinkage.abortedCount == 0);

    while (null != receiverSender.sentPackets.poll());

    final long WAIT_TIME_NS = 1500 * 1000 * 1000;
    long startTime = System.nanoTime();
    while (System.nanoTime() - startTime < WAIT_TIME_NS) {
      mpSender.onTick();
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
      while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    }
    Assert.assertTrue(receiverLinkage.abortedCount == 1);
    Assert.assertTrue(senderLinkage.abortedCount == 1);
  }

  @Test
  public void testSendMultipartPacket12b() throws Exception
  {
    // test (12b)
    MultipartPacketTest.MultipartPacketTester sender = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);

    final byte[] TEST_DATA = {10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    sender.setTestData(TEST_DATA);
    final int SEGMENT_COUNT = (TEST_DATA.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;

    senderLinkage = new SenderLinkage(sender);
    mpSender.sendMultipartPacket(senderLinkage, sender);
    Packet250CustomPayload droppedPacket;
    for (int i= 1; i < SEGMENT_COUNT; ++i) {
      mpSender.onTick();
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    }
    Assert.assertTrue(receiverLinkage.completedCount == 1);
    Assert.assertTrue(senderLinkage.completedCount == 0);

    while (null != receiverSender.sentPackets.poll());

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
  public void testSendMultipartPacket13() throws Exception
  {
    // test (13)
    MultipartPacketTest.MultipartPacketTester sender = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);

    final byte[] TEST_DATA = {10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    sender.setTestData(TEST_DATA);
    final int SEGMENT_COUNT = (TEST_DATA.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;

    senderLinkage = new SenderLinkage(sender);
    mpSender.sendMultipartPacket(senderLinkage, sender);
    for (int i= 0; i < SEGMENT_COUNT; ++i) {
      mpSender.onTick();
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
      while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    }
    Assert.assertTrue(receiverLinkage.completedCount == 1);
    Assert.assertTrue(senderLinkage.completedCount == 1);
    senderLinkage = new SenderLinkage(sender);
    boolean threw = false;
    try {
      mpSender.sendMultipartPacket(senderLinkage, sender);
    } catch (IllegalArgumentException iae) {
      threw = true;
    }
    Assert.assertTrue(threw);

    //(13b)
    MultipartPacketTest.MultipartPacketTester sender1 = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    MultipartPacketTest.MultipartPacketTester sender2 = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    MultipartPacketTest.MultipartPacketTester sender3 = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender1.setTestData(TEST_DATA);
    sender2.setTestData(TEST_DATA);
    sender3.setTestData(TEST_DATA);

    SenderLinkage senderLinkage1 = new SenderLinkage(sender1);
    SenderLinkage senderLinkage2 = new SenderLinkage(sender2);
    SenderLinkage senderLinkage3 = new SenderLinkage(sender3);
    mpSender.sendMultipartPacket(senderLinkage2, sender2);
    try {
      threw = false;
      mpSender.sendMultipartPacket(senderLinkage1, sender1);
    } catch (IllegalArgumentException iae) {
      threw = true;
    }
    Assert.assertTrue(threw);

    // (13c)
    sender1 = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender2 = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender3 = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender1.setTestData(TEST_DATA);
    sender2.setTestData(TEST_DATA);
    sender3.setTestData(TEST_DATA);
    senderLinkage1 = new SenderLinkage(sender1);
    senderLinkage2 = new SenderLinkage(sender2);
    senderLinkage3 = new SenderLinkage(sender3);

    while (null != senderSender.sentPackets.poll());
    while (null != receiverSender.sentPackets.poll());

    mpSender.sendMultipartPacket(senderLinkage1, sender1);

    for (int i= 0; i < SEGMENT_COUNT; ++i) {
      mpSender.onTick();
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
      while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    }
    mpSender.sendMultipartPacket(senderLinkage3, sender3);

    try {
      threw = false;
      mpSender.sendMultipartPacket(senderLinkage2, sender2);
    } catch (IllegalArgumentException iae) {
      threw = true;
    }
    Assert.assertTrue(threw);

    sender1 = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender1.setTestData(TEST_DATA);

    try {
      threw = false;
      mpSender.sendMultipartPacket(senderLinkage2, sender1);
    } catch (IllegalArgumentException iae) {
      threw = true;
    }
    Assert.assertTrue(threw);


  }
  @Test
  public void testSendMultipartPacket14() throws Exception
  {
    // test (13)
    MultipartPacketTest.MultipartPacketTester sender = MultipartPacketTest.MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);

    final byte[] TEST_DATA = {10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    sender.setTestData(TEST_DATA);
    final int SEGMENT_COUNT = (TEST_DATA.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;

    senderLinkage = new SenderLinkage(sender);
    mpSender.sendMultipartPacket(senderLinkage, sender);
    while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
    Packet250CustomPayload ackPacket;
    ackPacket = receiverSender.sentPackets.poll();
    Packet250CustomPayload badPacket;

    final byte ARBITRARY_COMMAND = 2;
    final int MUCH_HIGHER_UNIQUE_ID = 1353;
    badPacket = MultipartPacketTest.MultipartPacketTester.corruptPacket(ackPacket, 0, PACKET_ID, MUCH_HIGHER_UNIQUE_ID, ARBITRARY_COMMAND);
    boolean result = mpSender.processIncomingPacket(badPacket);
    Assert.assertTrue(!result);

    for (int i= 0; i < SEGMENT_COUNT; ++i) {
      mpSender.onTick();
      while (!senderSender.sentPackets.isEmpty()) mpReceiver.processIncomingPacket(senderSender.sentPackets.poll());
      while (!receiverSender.sentPackets.isEmpty()) mpSender.processIncomingPacket(receiverSender.sentPackets.poll());
    }
    Assert.assertTrue(receiverLinkage.completedCount == 1);
    Assert.assertTrue(senderLinkage.completedCount == 1);
    result = mpSender.processIncomingPacket(badPacket);
    Assert.assertTrue(!result);
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
