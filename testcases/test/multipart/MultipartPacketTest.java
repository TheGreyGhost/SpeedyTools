package test.multipart;

import net.minecraftforge.fml.relauncher.Side;
import org.junit.Assert;
import org.junit.Test;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.multipart.MultipartPacket;
import speedytools.common.network.multipart.Packet250MultipartSegment;
import speedytools.common.network.multipart.Packet250MultipartSegmentAcknowledge;
import speedytools.common.utilities.ErrorLog;

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
    final byte PACKET_ID = Packet250Types.PACKET250_SELECTION_PACKET.getPacketTypeID();
    final int SEGMENT_SIZE = 4;
    MultipartPacketTester sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);

    final byte[] TEST_DATA = {10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    sender.setTestData(TEST_DATA);
    final int SEGMENT_COUNT = sender.getSegmentCount(); // (TEST_DATA.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;
    ArrayList<Packet250MultipartSegment> savedPackets = new ArrayList<Packet250MultipartSegment>();
    for (int i = 0; i < SEGMENT_COUNT; ++i) {
      Assert.assertFalse(sender.allSegmentsSent());
      Assert.assertTrue(sender.getPercentComplete() == 100 * i / 2 / SEGMENT_COUNT);
      Packet250MultipartSegment packet = sender.getNextUnsentSegment();
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
      result = receiver.processIncomingSegment(savedPackets.get(i));
      Assert.assertTrue(result);
    }
    Assert.assertTrue(receiver.getSegmentsReceivedFlag());
    Assert.assertTrue(receiver.allSegmentsReceived());
    Assert.assertTrue(receiver.matchesTestData(TEST_DATA));

    final int EXTRA_HEADER_LEN = 1;
    // test (1-a) - different data lengths
    for (int datalen = 1; datalen < TEST_DATA.length; ++datalen) {
      sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
      receiver = null;
      byte[] testDataTrim = Arrays.copyOfRange(TEST_DATA, 0, datalen);
      sender.setTestData(testDataTrim);
      final int segmentCountTrim = sender.getSegmentCount(); //(testDataTrim.length + SEGMENT_SIZE - 1) / SEGMENT_SIZE;
      for (int i = 0; i < segmentCountTrim; ++i) {
        Assert.assertFalse(sender.allSegmentsSent());
        Assert.assertTrue(sender.getPercentComplete() == 100 * i / 2 / segmentCountTrim);
        Packet250MultipartSegment packet = sender.getNextUnsentSegment();
        Assert.assertTrue(packet != null);
        if (receiver == null) {
          receiver = MultipartPacketTester.createReceiverPacket(packet);
        } else {
          result = receiver.processIncomingSegment(packet);
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
      result = receiver.processIncomingSegment(savedPackets.get(i));
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

    Packet250MultipartSegment packet;
    Packet250MultipartSegmentAcknowledge packetAck;
    savedPackets.clear();
    for (int i = 0; i < SEGMENT_COUNT; ++i) {
      Assert.assertFalse(sender.allSegmentsSent());
      Assert.assertFalse(sender.getAcknowledgementsReceivedFlag());
      packet = sender.getNextUnsentSegment();
      savedPackets.add(packet);
      Assert.assertTrue(packet != null);
      if (i == 0) receiver = MultipartPacketTester.createReceiverPacket(packet);
      if (i != DROP_PACKET2) {
        result = receiver.processIncomingSegment(savedPackets.get(i));
        Assert.assertTrue(result);
        packetAck = receiver.getAcknowledgementPacket();
        Assert.assertTrue(packetAck != null);
        result = sender.processIncomingAcknowledgement(packetAck);
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
    result = receiver.processIncomingSegment(packet);
    Assert.assertTrue(result);
    packetAck = receiver.getAcknowledgementPacket();
    Assert.assertTrue(packetAck != null);
    result = sender.processIncomingAcknowledgement(packetAck);
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
        result = receiver.processIncomingSegment(packet);
        Assert.assertTrue(result);
        packetAck = receiver.getAcknowledgementPacket();
        result = sender.processIncomingAcknowledgement(packetAck);
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
        result = receiver.processIncomingSegment(packet);
        Assert.assertTrue(result);
        packetAck = receiver.getAcknowledgementPacket();
        Assert.assertTrue(packet != null);
        result = sender.processIncomingAcknowledgement(packetAck);
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
    result = receiver.processIncomingSegment(packet);
    packet = sender.getSenderAbortPacket();
    Assert.assertTrue(packet != null);
    Assert.assertTrue(sender.hasBeenAborted());
    result = receiver.processIncomingSegment(packet);
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
    result = receiver.processIncomingSegment(packet);
    packetAck = receiver.getReceiverAbortPacket();
    Assert.assertTrue(packetAck != null);
    result = sender.processIncomingAcknowledgement(packetAck);
    Assert.assertTrue(result);
    Assert.assertTrue(sender.hasBeenAborted());
    Assert.assertTrue(receiver.hasBeenAborted());
    packetAck = receiver.getAcknowledgementPacket();
    Assert.assertTrue(packetAck == null);

    // test (7)
    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    packet = sender.getNextUnsentSegment();
    packetAck = MultipartPacket.getAbortPacketForLostPacket(packet, false);
    result = sender.processIncomingAcknowledgement(packetAck);
    Assert.assertTrue(result);
    Assert.assertTrue(sender.hasBeenAborted());
    packet = sender.getSenderAbortPacket();
    Packet250MultipartSegmentAcknowledge abortPacket = MultipartPacket.getAbortPacketForLostPacket(packet, false);
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
    packetAck = receiver.getAcknowledgementPacket();
    packet = MultipartPacket.getAbortPacketForLostPacket(packetAck);
    result = sender.processIncomingSegment(packet);
    Assert.assertTrue(result);
    Assert.assertTrue(sender.hasBeenAborted());
    Assert.assertFalse(sender2.hasBeenAborted());

    // test (8-1)
    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    packet = sender.getNextUnsentSegment();
    packetAck = MultipartPacket.getFullAcknowledgePacketForLostPacket(packet);
    result = sender.processIncomingAcknowledgement(packetAck);
    Assert.assertTrue(result);
    Assert.assertTrue(sender.allSegmentsAcknowledged());

    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    packet = sender.getSenderAbortPacket();
    packetAck = MultipartPacket.getFullAcknowledgePacketForLostPacket(packet);
    Assert.assertTrue(packetAck != null);
    packet = MultipartPacket.getAbortPacketForLostPacket(packetAck);
    Assert.assertTrue(packet == null);

            // test (9)
    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    packet = sender.getNextUnsentSegment();
    receiver = MultipartPacketTester.createReceiverPacket(packet);

    packetAck = sender.getAcknowledgementPacket();
    Assert.assertTrue(packetAck == null);
    packet = receiver.getNextUnsentSegment();
    Assert.assertTrue(packet == null);
    packet = receiver.getNextUnacknowledgedSegment();
    Assert.assertTrue(packet == null);

    sender = MultipartPacketTester.createSenderPacket(CHANNEL, Side.SERVER, PACKET_ID, SEGMENT_SIZE);
    sender.setTestData(TEST_DATA);
    packet = sender.getNextUnsentSegment();
    Packet250MultipartSegment badPacket;

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
    int correctUniqueMultipartID = sender.getUniqueID();
    badPacket = MultipartPacketTester.corruptPacket(packet, 0, PACKET_ID, correctUniqueMultipartID);  // should succeed
    Assert.assertTrue(receiver.processIncomingSegment(badPacket));
    packet = sender.getNextUnsentSegment();
    badPacket = MultipartPacketTester.corruptPacket(packet, 0, (byte)(PACKET_ID+1), correctUniqueMultipartID);
    Assert.assertFalse(receiver.processIncomingSegment(badPacket));
    badPacket = MultipartPacketTester.corruptPacket(packet, 0, PACKET_ID, correctUniqueMultipartID +1);
    Assert.assertFalse(receiver.processIncomingSegment(badPacket));
//    badPacket = MultipartPacketTester.corruptPacket(packet, 0, PACKET_ID, correctUniqueMultipartID);
//    Assert.assertFalse(receiver.processIncomingSegment(badPacket));
    badPacket = MultipartPacketTester.corruptPacket(packet, -1, PACKET_ID, correctUniqueMultipartID);
    Assert.assertFalse(receiver.processIncomingSegment(badPacket));

    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)-1, (short)SEGMENT_SIZE, TEST_DATA.length);
    Assert.assertFalse(receiver.processIncomingSegment(badPacket));
    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)4, (short)SEGMENT_SIZE, TEST_DATA.length);
    Assert.assertFalse(receiver.processIncomingSegment(badPacket));
    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)3, (short)(SEGMENT_SIZE+1), TEST_DATA.length);
    Assert.assertFalse(receiver.processIncomingSegment(badPacket));
    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)3, (short)(SEGMENT_SIZE-1), TEST_DATA.length);
    Assert.assertFalse(receiver.processIncomingSegment(badPacket));
    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)3, (short)SEGMENT_SIZE, TEST_DATA.length+2);
    Assert.assertFalse(receiver.processIncomingSegment(badPacket));
    badPacket = MultipartPacketTester.corruptSegPacket(packet, (short)3, (short)SEGMENT_SIZE, TEST_DATA.length-1);
    Assert.assertFalse(receiver.processIncomingSegment(badPacket));

//    packetAck = receiver.getAcknowledgementPacket();
//    badPacket = MultipartPacketTester.corruptAckPacket(packetAck, (short)-1);
//    Assert.assertFalse(sender.processIncomingSegment(badPacket));
//    badPacket = MultipartPacketTester.corruptAckPacket(packetAck, (short) 2);
//    Assert.assertFalse(sender.processIncomingSegment(badPacket));
  }

  public static class MultipartPacketTester extends MultipartPacket
  {
    public static MultipartPacketTester createSenderPacket(String i_channel, Side whichSide, byte i_packet250CustomPayloadID, int i_segmentSize)
    {
      Packet250Types packet250Type = Packet250Types.byteToPacket250Type(i_packet250CustomPayloadID);
      return new MultipartPacketTester(packet250Type, whichSide, i_segmentSize);
    }

    public static MultipartPacketTester createReceiverPacket(Packet250MultipartSegment packet)
    {
      MultipartPacketTester newPacket;
      try {
        newPacket = new MultipartPacketTester(packet);
        boolean result = newPacket.processIncomingSegment(packet);

        return result ? newPacket : null;
      } catch (IOException ioe) {
        ErrorLog.defaultLog().debug("Failed to createReceiverPacket, due to exception " + ioe.toString());
        return null;
      }
    }

    public void setTestData(byte [] testData)
    {
      setRawDataForSending(testData);
    }

    public boolean matchesTestData(byte [] dataToCompare)
    {
      return Arrays.equals(dataToCompare, getRawDataCopy());
    }

    protected MultipartPacketTester(Packet250MultipartSegment packet) throws IOException
    {
      super(packet);
    }

    protected MultipartPacketTester(Packet250Types i_packet250Type, Side i_whichSideAmIOn, int i_segmentSize)
    {
      super(i_packet250Type, i_whichSideAmIOn, i_segmentSize);
    }

    public static Packet250MultipartSegment corruptPacket(Packet250MultipartSegment packet, int deltaPacketLength,
                                                          int newPacketTypeID, int newUniqueID)
    {
        Packet250Types newPacketType = Packet250Types.byteToPacket250Type((byte)newPacketTypeID);
        Packet250MultipartSegment newPacket = new Packet250MultipartSegment(newPacketType, packet.isAbortTransmission(),
                 newUniqueID, packet.getSegmentNumber(),
                (short)(packet.getSegmentSize() + deltaPacketLength), packet.getFullMultipartLength(), packet.getRawData());
        return newPacket;
    }

    public static Packet250MultipartSegmentAcknowledge corruptAckPacket(Packet250MultipartSegmentAcknowledge packet,
                                                                        int newPacketTypeID, int newAckID, int newUniqueID)
    {
      Packet250Types newPacketType = Packet250Types.byteToPacket250Type((byte)newPacketTypeID);
      Packet250MultipartSegmentAcknowledge.Acknowledgement newAcknowledgement =
              Packet250MultipartSegmentAcknowledge.Acknowledgement.byteToAcknowledgement((byte)newAckID);
      Packet250MultipartSegmentAcknowledge newPacket = new Packet250MultipartSegmentAcknowledge(
              newPacketType, newAcknowledgement,
              newUniqueID, packet.getSegmentsNotReceivedYet());
      return newPacket;
    }


//    public static Packet250MultipartSegmentAcknowledge corruptAckPacket(Packet250MultipartSegmentAcknowledge packet, short newAckDataLength)
//    {
//      try {
//        Packet250MultipartSegmentAcknowledge newPacket = new Packet250MultipartSegmentAcknowledge(packet.getPacket250Type(), packet.getAcknowledgement(),
//                packet.getSegmentsNotReceivedYet());
//        packet.
//        return newPacket;
//      } catch (IOException ioe) {
//        Assert.fail("exception " + ioe);
//        return null;
//      }
//    }

    public static Packet250MultipartSegment corruptSegPacket(Packet250MultipartSegment packet,  short newSegmentNumber, short newSegmentSize, int newRawDataLength)
    {
      Packet250MultipartSegment newPacket = new Packet250MultipartSegment(packet.getPacket250Type(), packet.isAbortTransmission(),
              packet.getUniqueMultipartID(), newSegmentNumber, newSegmentSize, newRawDataLength, packet.getRawData());
      return newPacket;
    }

  }

}
