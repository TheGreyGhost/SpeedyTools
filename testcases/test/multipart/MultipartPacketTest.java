package test.multipart;

import cpw.mods.fml.relauncher.Side;
import org.junit.Assert;
import net.minecraft.network.packet.Packet250CustomPayload;
import org.junit.Test;
import speedytools.common.network.multipart.MultipartPacket;
import speedytools.common.utilities.ErrorLog;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

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
 *       Try to generate another abort packet from the first abort packet, this time with the "acknowledgeAbortPackets" true, and verify it succeeds
 * (8) start sending, getAbortPacketForLostPacket from a received acknowledge packet, verify receiver aborts
 * (8-1) start sending, getFullAcknowledgePacketForLostPacket and verify that sender goes complete
 *       start sending, generate an abort packet, call getFullAcknowledgePacketForLostPacket with it, and verify that the sender aborts
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
      Assert.assertTrue(sender.getPercentComplete() == 100 * i / 2 / SEGMENT_COUNT);
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
      Assert.assertTrue(receiver.getPercentComplete() == 100 * i / SEGMENT_COUNT);
      Assert.assertFalse(receiver.allSegmentsReceived());
      result = receiver.processIncomingPacket(savedPackets.get(i));
      Assert.assertTrue(result);
    }
    Assert.assertTrue(receiver.getSegmentsReceivedFlag());
    Assert.assertTrue(receiver.allSegmentsReceived());
    Assert.assertTrue(receiver.matchesTestData(TEST_DATA));

    // test (1-a) - different data lengths
    for (int datalen = 1; datalen < TEST_DATA.length; ++datalen) {
      sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
      receiver = null;
      byte[] testDataTrim = Arrays.copyOfRange(TEST_DATA, 0, datalen);
      sender.setTestData(testDataTrim);
      final int segmentCountTrim = (testDataTrim.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;
      for (int i = 0; i < segmentCountTrim; ++i) {
        Assert.assertFalse(sender.allSegmentsSent());
        Assert.assertTrue(sender.getPercentComplete() == 100 * i / 2 / segmentCountTrim);
        Packet250CustomPayload packet = sender.getNextUnsentSegment();
        Assert.assertTrue(packet != null);
        if (receiver == null) {
          receiver = MultipartPacketTester.createReceiverPacket(packet);
        } else {
          result = receiver.processIncomingPacket(packet);
          Assert.assertTrue(result);
        }
      }
      Assert.assertTrue(receiver.matchesTestData(testDataTrim));
    }

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
    Assert.assertTrue(sender.getPercentComplete() == 100);
    Assert.assertTrue(receiver.getPercentComplete() == 100);
    packet = receiver.getNextUnacknowledgedSegment();
    Assert.assertTrue(packet == null);

    // test (3-1)
    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    packet = sender.getNextUnsentSegment();
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
    packet = MultipartPacket.getAbortPacketForLostPacket(packet, false);
    result = sender.processIncomingPacket(packet);
    Assert.assertTrue(result);
    Assert.assertTrue(sender.hasBeenAborted());
    Packet250CustomPayload abortPacket = MultipartPacket.getAbortPacketForLostPacket(packet, false);
    Assert.assertTrue(abortPacket == null);
    abortPacket = MultipartPacket.getAbortPacketForLostPacket(packet, true);
    Assert.assertTrue(abortPacket != null);

    // test (8)
    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    packet = sender.getNextUnsentSegment();
    receiver = MultipartPacketTester.createReceiverPacket(packet);

    MultipartPacketTester sender2 = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender2.setTestData(TEST_DATA);
    packet = sender2.getNextUnsentSegment();
    packet = receiver.getAcknowledgementPacket();
    packet = MultipartPacket.getAbortPacketForLostPacket(packet, false);
    result = sender.processIncomingPacket(packet);
    Assert.assertTrue(result);
    Assert.assertTrue(sender.hasBeenAborted());
    Assert.assertFalse(sender2.hasBeenAborted());

    // test (8-1)
    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    packet = sender.getNextUnsentSegment();
    packet = MultipartPacket.getFullAcknowledgePacketForLostPacket(packet);
    result = sender.processIncomingPacket(packet);
    Assert.assertTrue(result);
    Assert.assertTrue(sender.allSegmentsAcknowledged());

    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    packet = sender.getAbortPacket();
    packet = MultipartPacket.getFullAcknowledgePacketForLostPacket(packet);
    Assert.assertTrue(packet != null);
    packet = MultipartPacket.getAbortPacketForLostPacket(packet, false);
    Assert.assertTrue(packet == null);

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
    sender.setTestData(TEST_DATA);
    packet = sender.getNextUnsentSegment();
    Packet250CustomPayload badPacket;

    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)0, (short)SEGMENT_SIZE, TEST_DATA.length);    // should succeed
    Assert.assertTrue(null != MultipartPacketTester.createReceiverPacket(badPacket));

    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)-1, (short)SEGMENT_SIZE, TEST_DATA.length);
    Assert.assertTrue(null == MultipartPacketTester.createReceiverPacket(badPacket));
    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)SEGMENT_COUNT, (short)SEGMENT_SIZE, TEST_DATA.length);
    Assert.assertTrue(null == MultipartPacketTester.createReceiverPacket(badPacket));
    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)0, (short)0, TEST_DATA.length);
    Assert.assertTrue(null == MultipartPacketTester.createReceiverPacket(badPacket));
    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)0, Short.MAX_VALUE, TEST_DATA.length);
    Assert.assertTrue(null == MultipartPacketTester.createReceiverPacket(badPacket));
    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)0, (short)SEGMENT_SIZE, -1);
    Assert.assertTrue(null == MultipartPacketTester.createReceiverPacket(badPacket));

    receiver = MultipartPacketTester.createReceiverPacket(packet);
    packet = sender.getNextUnsentSegment();
    final byte SEG_DATA_COMMAND = 0;
    final byte BAD_COMMAND = 6;
    int correctPacketID = MultipartPacket.readUniqueID(packet);
    badPacket = MultipartPacketTester.corruptPacket(packet, 0, PACKET_ID, correctPacketID, SEG_DATA_COMMAND);  // should succeed
    Assert.assertTrue(receiver.processIncomingPacket(badPacket));
    packet = sender.getNextUnsentSegment();
    badPacket = MultipartPacketTester.corruptPacket(packet, 0, (byte)(PACKET_ID+1), correctPacketID, SEG_DATA_COMMAND);
    Assert.assertFalse(receiver.processIncomingPacket(badPacket));
    badPacket = MultipartPacketTester.corruptPacket(packet, 0, PACKET_ID, correctPacketID+1, SEG_DATA_COMMAND);
    Assert.assertFalse(receiver.processIncomingPacket(badPacket));
    badPacket = MultipartPacketTester.corruptPacket(packet, 0, PACKET_ID, correctPacketID, BAD_COMMAND);
    Assert.assertFalse(receiver.processIncomingPacket(badPacket));
    badPacket = MultipartPacketTester.corruptPacket(packet, -1, PACKET_ID, correctPacketID, SEG_DATA_COMMAND);
    Assert.assertFalse(receiver.processIncomingPacket(badPacket));

    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)-1, (short)SEGMENT_SIZE, TEST_DATA.length);
    Assert.assertFalse(receiver.processIncomingPacket(badPacket));
    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)4, (short)SEGMENT_SIZE, TEST_DATA.length);
    Assert.assertFalse(receiver.processIncomingPacket(badPacket));
    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)3, (short)(SEGMENT_SIZE+1), TEST_DATA.length);
    Assert.assertFalse(receiver.processIncomingPacket(badPacket));
    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)3, (short)(SEGMENT_SIZE-1), TEST_DATA.length);
    Assert.assertFalse(receiver.processIncomingPacket(badPacket));
    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)3, (short)SEGMENT_SIZE, TEST_DATA.length+1);
    Assert.assertFalse(receiver.processIncomingPacket(badPacket));
    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)3, (short)SEGMENT_SIZE, TEST_DATA.length-1);
    Assert.assertFalse(receiver.processIncomingPacket(badPacket));

    packet = receiver.getAcknowledgementPacket();
    badPacket = MultipartPacketTester.corruptAckPacket(packet, (short)-1);
    Assert.assertFalse(sender.processIncomingPacket(badPacket));
    badPacket = MultipartPacketTester.corruptAckPacket(packet, (short) 2);
    Assert.assertFalse(sender.processIncomingPacket(badPacket));
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
        boolean result = newPacket.processIncomingPacket(packet);

        return result ? newPacket : null;
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

    public static Packet250CustomPayload corruptPacket(Packet250CustomPayload packet, int deltaPacketLength,
                                                       byte newCustomPayloadID, int newUniquePacketID, byte newCommand)
    {
      try {
        Packet250CustomPayload newPacket = new Packet250CustomPayload();
        newPacket.channel = packet.channel;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream outputStream = new DataOutputStream(bos);
        outputStream.writeByte(newCustomPayloadID);
        outputStream.writeInt(newUniquePacketID);
        outputStream.writeByte(newCommand);
        byte [] newdata = new byte[packet.data.length + deltaPacketLength];
        System.arraycopy(packet.data, 0, newdata, 0, newdata.length);
        System.arraycopy(bos.toByteArray(), 0, newdata, 0, bos.size());
        newPacket.data = newdata;
        newPacket.length = packet.length + deltaPacketLength;
        return newPacket;
      } catch (IOException ioe) {
        Assert.fail("exception " + ioe);
        return null;
      }
    }

    public static Packet250CustomPayload corruptAckPacket(Packet250CustomPayload packet, short newAckDataLength)
    {
      try {
        Packet250CustomPayload newPacket = new Packet250CustomPayload();
        newPacket.channel = packet.channel;
        newPacket.data = packet.data.clone();
        newPacket.length = packet.length;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream outputStream = new DataOutputStream(bos);
        outputStream.writeShort(newAckDataLength);

        final int ACK_DATA_LEN_POSITION = 6;
        System.arraycopy(bos.toByteArray(), 0, newPacket.data, ACK_DATA_LEN_POSITION, bos.size());
        return newPacket;
      } catch (IOException ioe) {
        Assert.fail("exception " + ioe);
        return null;
      }
    }

    public static Packet250CustomPayload corruptSegPacket(Packet250CustomPayload packet,  short newSegmentNumber, short newSegmentSize, int newRawDataLength)
    {
      try {
      Packet250CustomPayload newPacket = new Packet250CustomPayload();
      newPacket.channel = packet.channel;
      newPacket.data = packet.data.clone();
      newPacket.length = packet.length;
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      DataOutputStream outputStream = new DataOutputStream(bos);
      outputStream.writeShort(newSegmentNumber);
      outputStream.writeShort(newSegmentSize);
      outputStream.writeInt(newRawDataLength);

      final int SEGMENT_NUMBER_POSITION = 6;
      System.arraycopy(bos.toByteArray(), 0, newPacket.data, SEGMENT_NUMBER_POSITION, bos.size());
      return newPacket;
      } catch (IOException ioe) {
        Assert.fail("exception " + ioe);
        return null;
      }
    }

  }

}
