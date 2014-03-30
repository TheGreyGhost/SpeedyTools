package test.multipart;

import cpw.mods.fml.relauncher.Side;
import junit.framework.Assert;
import net.minecraft.network.packet.Packet250CustomPayload;
import org.junit.Test;
import speedytools.common.network.multipart.MultipartPacket;
import speedytools.common.utilities.ErrorLog;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * User: The Grey Ghost
 * Date: 29/03/14
 * test plan:
 * (1) create a sender packet, populate it with test data, save the segment packets, use them to generate a receiver, reassemble and compare to original
 *    a) check allSegmentsSent; confirm getNextSegment returns null
 *    b) check allSegmentsReceived;
 *    c) check SegmentsReceivedFlag, reset it
 * (2) repeat sending, in the reverse order
 * (3) repeat sending, generate acknowledgment packets, drop one of the segment packets, complete sending, get the unacknowledged packet, send it
 *    a) confirm this completes the transmission
 *    b) confirm allSegmentsReceived, Sent, Acknowledged
 *    c) check AcknowledgementsReceivedFlag, reset it
 *    d) confirm getNextUnack returns null
 * (3-1) verify getNextUnack returns null if it reaches a segment which hasn't been sent yet
 * (4) repeat sending, drop two packets, generate acknowledgment packets, repeatedly call resetAck, verify it doesn't advance
 * (5) start sending, then abort, verify that the receiver aborts
 *    a) verify that once aborted, the getPackets stop working
 * (6) start sending, generate abort packet from receiver, verify sender aborts
 * (7) start sending, getAbortPacketForLostPacket from the packet, verify sender aborts.  Try to generate another abort packet from the first abort packet, verify it fails
 * (8) start sending, getAbortPacketForLostPacket from a received acknowledge packet, verify receiver aborts
 * (9) errors:
 *   a) attempt to use a sender to create Ack packet
 *   b) use receiver to create segment packet
 *   c) generate packets with out of range values / sizes
 *   d) corrupt the packets to verify that errors in each of the fields are detected:
 *      during receipt of the initial packet, receipt of a subsequent packet, receipt of an acknowledgement packet
 */
public class MultipartPacketTest
{
  public final static String TEST_ERROR_LOG = "MultipartPacketTestErrorLog.log";
  public static final String TEST_TEMP_ROOT_DIRECTORY = "temp";

  @Test
  public void testProcessIncomingPacket() throws Exception {

    Path testdata = Paths.get(TEST_TEMP_ROOT_DIRECTORY).resolve(TEST_ERROR_LOG);
    ErrorLog.setLogFileAsDefault(testdata.toString());

    // test (1)
    final String CHANNEL = "mychannel";
    final byte PACKET_ID = 35;
    final int SEGMENT_SIZE = 4;
    MultipartPacketTester sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);

    final byte[] TEST_DATA = {10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    sender.setTestData(TEST_DATA);
    final int SEGMENT_COUNT = (TEST_DATA.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;
    ArrayList<Packet250CustomPayload> savedPackets = new ArrayList<Packet250CustomPayload>();
    for (int i = 0; i < SEGMENT_COUNT; ++i) {
      Assert.assertFalse(sender.allSegmentsSent());
      Packet250CustomPayload packet = sender.getNextUnsentSegment();
      Assert.assertTrue(packet != null);
      savedPackets.add(packet);
    }
    Assert.assertTrue(sender.allSegmentsSent());
    Assert.assertTrue(null == sender.getNextUnsentSegment());

    MultipartPacketTester receiver = MultipartPacketTester.createReceiverPacket(savedPackets.get(0));
    Assert.assertTrue(receiver != null);
    boolean result;
    Assert.assertTrue(receiver.getSegmentsReceivedFlag());
    receiver.resetSegmentsReceivedFlag();
    Assert.assertFalse(receiver.getSegmentsReceivedFlag());

    for (int i = 1; i < SEGMENT_COUNT; ++i) {
      Assert.assertFalse(receiver.allSegmentsReceived());
      result = receiver.processIncomingPacket(savedPackets.get(i));
      Assert.assertTrue(result);
    }
    Assert.assertTrue(receiver.getSegmentsReceivedFlag());
    Assert.assertTrue(receiver.allSegmentsReceived());
    Assert.assertTrue(receiver.matchesTestData(TEST_DATA));

    // test (2)
    final int START_PACKET2 = 2;
    receiver = MultipartPacketTester.createReceiverPacket(savedPackets.get(START_PACKET2));
    Assert.assertTrue(receiver != null);
    Assert.assertTrue(receiver.getSegmentsReceivedFlag());

    for (int i = SEGMENT_COUNT - 1; i >= 0; --i) {
      Assert.assertFalse(receiver.allSegmentsReceived());
      receiver.resetSegmentsReceivedFlag();
      result = receiver.processIncomingPacket(savedPackets.get(i));
      Assert.assertTrue(result);
      Assert.assertTrue(    (i == START_PACKET2 && !receiver.getSegmentsReceivedFlag())
                         || (i != START_PACKET2 && receiver.getSegmentsReceivedFlag())  );
    }
    Assert.assertTrue(receiver.getSegmentsReceivedFlag());
    Assert.assertTrue(receiver.allSegmentsReceived());
    Assert.assertTrue(receiver.matchesTestData(TEST_DATA));

    // test (3)
    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    final int DROP_PACKET2 = 2;

    Packet250CustomPayload packet;
    savedPackets.clear();
    for (int i = 0; i < SEGMENT_COUNT; ++i) {
      Assert.assertFalse(sender.allSegmentsSent());
      Assert.assertFalse(sender.getAcknowledgementsReceivedFlag());
      packet = sender.getNextUnsentSegment();
      savedPackets.add(packet);
      Assert.assertTrue(packet != null);
      if (i == 0) receiver = MultipartPacketTester.createReceiverPacket(packet);
      if (i != DROP_PACKET2) {
        result = receiver.processIncomingPacket(savedPackets.get(i));
        Assert.assertTrue(result);
        packet = receiver.getAcknowledgementPacket();
        Assert.assertTrue(packet != null);
        result = sender.processIncomingPacket(packet);
        Assert.assertTrue(result);
        Assert.assertTrue(sender.getAcknowledgementsReceivedFlag());
        sender.resetAcknowledgementsReceivedFlag();
        Assert.assertFalse(sender.getAcknowledgementsReceivedFlag());
      }
    }
    Assert.assertTrue(sender.allSegmentsSent());
    Assert.assertFalse(receiver.allSegmentsReceived());
    Assert.assertFalse(sender.allSegmentsAcknowledged());
    packet = sender.getNextUnacknowledgedSegment();
    Assert.assertTrue(packet != null);
    result = receiver.processIncomingPacket(packet);
    Assert.assertTrue(result);
    packet = receiver.getAcknowledgementPacket();
    Assert.assertTrue(packet != null);
    result = sender.processIncomingPacket(packet);
    Assert.assertTrue(result);

    Assert.assertTrue(sender.allSegmentsSent());
    Assert.assertTrue(receiver.allSegmentsReceived());
    Assert.assertTrue(sender.allSegmentsAcknowledged());
    packet = receiver.getNextUnacknowledgedSegment();
    Assert.assertTrue(packet == null);

    // test (3-1)
    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    packet = sender.getNextUnsentSegment();
    packet = sender.getNextUnacknowledgedSegment();
    Assert.assertTrue(packet != null);
    packet = sender.getNextUnacknowledgedSegment();
    Assert.assertTrue(packet != null);
    packet = sender.getNextUnacknowledgedSegment();
    Assert.assertTrue(packet == null);

    // test (4)
    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);

    final int DROP_PACKET3 = 3;
    for (int i = 0; i < SEGMENT_COUNT; ++i) {
      packet = sender.getNextUnsentSegment();
      Assert.assertTrue(packet != null);
      if (i == 0) {
        receiver = MultipartPacketTester.createReceiverPacket(packet);
      }

      if (i != DROP_PACKET2 && i != DROP_PACKET3) {
        result = receiver.processIncomingPacket(packet);
        Assert.assertTrue(result);
        packet = receiver.getAcknowledgementPacket();
        result = sender.processIncomingPacket(packet);
        Assert.assertTrue(result);
      }
    }
    packet = sender.getNextUnacknowledgedSegment();
    Assert.assertTrue(packet != null);
    packet = sender.getNextUnacknowledgedSegment();
    Assert.assertTrue(packet != null);
    packet = sender.getNextUnacknowledgedSegment();
    Assert.assertTrue(packet == null);

    sender.resetToOldestUnacknowledgedSegment();
    for (int i = 10; i > 0; --i) {
      packet = sender.getNextUnacknowledgedSegment();
      Assert.assertTrue(packet != null);
      Assert.assertFalse(sender.allSegmentsAcknowledged());
      if (i <= 2) {
        result = receiver.processIncomingPacket(packet);
        Assert.assertTrue(result);
        packet = receiver.getAcknowledgementPacket();
        Assert.assertTrue(packet != null);
        result = sender.processIncomingPacket(packet);
        Assert.assertTrue(result);
      } else {
        sender.resetToOldestUnacknowledgedSegment();
      }
    }
    Assert.assertTrue(sender.allSegmentsSent());
    Assert.assertTrue(receiver.allSegmentsReceived());
    Assert.assertTrue(sender.allSegmentsAcknowledged());

    // test (5)
    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    packet = sender.getNextUnsentSegment();
    receiver = MultipartPacketTester.createReceiverPacket(packet);
    packet = sender.getNextUnsentSegment();
    result = receiver.processIncomingPacket(packet);
    packet = sender.getAbortPacket();
    Assert.assertTrue(packet != null);
    Assert.assertTrue(sender.hasBeenAborted());
    result = receiver.processIncomingPacket(packet);
    Assert.assertTrue(result);
    Assert.assertTrue(receiver.hasBeenAborted());
    packet = sender.getNextUnsentSegment();
    Assert.assertTrue(packet == null);
    Assert.assertFalse(sender.allSegmentsAcknowledged());
    Assert.assertFalse(sender.allSegmentsSent());
    Assert.assertFalse(receiver.allSegmentsReceived());

    // test (6)
    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    packet = sender.getNextUnsentSegment();
    receiver = MultipartPacketTester.createReceiverPacket(packet);
    packet = sender.getNextUnsentSegment();
    result = receiver.processIncomingPacket(packet);
    packet = receiver.getAbortPacket();
    Assert.assertTrue(packet != null);
    result = sender.processIncomingPacket(packet);
    Assert.assertTrue(result);
    Assert.assertTrue(sender.hasBeenAborted());
    Assert.assertTrue(receiver.hasBeenAborted());
    packet = receiver.getAcknowledgementPacket();
    Assert.assertTrue(packet == null);

    // test (7)
    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    packet = sender.getNextUnsentSegment();
    packet = MultipartPacket.getAbortPacketForLostPacket(packet);
    result = sender.processIncomingPacket(packet);
    Assert.assertTrue(result);
    Assert.assertTrue(sender.hasBeenAborted());
    packet = MultipartPacket.getAbortPacketForLostPacket(packet);
    Assert.assertTrue(packet == null);

    // test (8)
    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    packet = sender.getNextUnsentSegment();
    receiver = MultipartPacketTester.createReceiverPacket(packet);

    MultipartPacketTester sender2 = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender2.setTestData(TEST_DATA);
    packet = sender2.getNextUnsentSegment();
    packet = receiver.getAcknowledgementPacket();
    packet = MultipartPacket.getAbortPacketForLostPacket(packet);
    result = sender.processIncomingPacket(packet);
    Assert.assertTrue(result);
    Assert.assertTrue(sender.hasBeenAborted());
    Assert.assertFalse(sender2.hasBeenAborted());

    // test (9)
    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    packet = sender.getNextUnsentSegment();
    receiver = MultipartPacketTester.createReceiverPacket(packet);

    packet = sender.getAcknowledgementPacket();
    Assert.assertTrue(packet == null);
    packet = receiver.getNextUnsentSegment();
    Assert.assertTrue(packet == null);
    packet = receiver.getNextUnacknowledgedSegment();
    Assert.assertTrue(packet == null);

    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);


  }

  public static class MultipartPacketTester extends MultipartPacket
  {
    public static MultipartPacketTester createSenderPacket(String i_channel, Side whichSide, byte i_packet250CustomPayloadID, int i_segmentSize)
    {
      return new MultipartPacketTester(i_channel, whichSide, i_packet250CustomPayloadID, i_segmentSize);
    }

    public static MultipartPacketTester createReceiverPacket(Packet250CustomPayload packet)
    {
      MultipartPacketTester newPacket;
      try {
        newPacket = new MultipartPacketTester(packet);
        newPacket.processIncomingPacket(packet);

        return newPacket;
      } catch (IOException ioe) {
        ErrorLog.defaultLog().warning("Failed to createReceiverPacket, due to exception " + ioe.toString());
        return null;
      }
    }

    public void setTestData(byte [] testData)
    {
      setRawData(testData);
    }

    public boolean matchesTestData(byte [] dataToCompare)
    {
      return Arrays.equals(dataToCompare, getRawDataCopy());
    }

    protected MultipartPacketTester(Packet250CustomPayload packet) throws IOException
    {
      super(packet);
    }

    protected MultipartPacketTester(String i_channel, Side whichSide, byte i_packet250CustomPayloadID, int i_segmentSize)
    {
      super(i_channel, whichSide, i_packet250CustomPayloadID, i_segmentSize);
    }
  }

}
