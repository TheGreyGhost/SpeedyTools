package test;

import net.minecraft.network.packet.Packet250CustomPayload;
import org.junit.Test;
import speedytools.common.network.multipart.MultipartPacket;
import speedytools.common.utilities.ErrorLog;

import java.io.IOException;
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
 * (4) repeat sending, generate acknowledgment packets, repeatedly call resetAck, verify it doesn't advance
 * (5) start sending, then abort, verify that the receiver aborts
 *    a) verify that once aborted, the getPackets stop working
 * (6) start sending, generate abort packet from receiver, verify sender aborts
 * (7) start sending, getAbortPacketForLostPacket from the packet, verify receiver aborts
 * (8) start sending, getAbortPacketForLostPacket from a received acknowledge packet, verify sender aborts
 * (9) errors:
 *   a) attempt to use a sender to create Ack packet
 *   b) use receiver to create segment packet
 *   c) generate packets with out of range values / sizes
 *   d) corrupt the packets to verify that errors in each of the fields are detected:
 *      during receipt of the initial packet, receipt of a subsequent packet, receipt of an acknowledgement packet
 */
public class MultipartPacketTest
{
  @Test
  public void testProcessIncomingPacket() throws Exception {
    // test (1)
    final String CHANNEL = "mychannel";
    final byte PACKET_ID = 35;
    final int SEGMENT_SIZE = 4;
    MultipartPacketTester sender = MultipartPacketTester.createSenderPacket(CHANNEL, PACKET_ID, SEGMENT_SIZE);

    final byte[] TEST_DATA = {10, 11, 12, 13, 20, 22, 24, 26, -52, -48, -44, -40, 100, 110, 120, 127};
    sender.setTestData(TEST_DATA);



  }



  public static class MultipartPacketTester extends MultipartPacket
  {
    public static MultipartPacketTester createSenderPacket(String i_channel, byte i_packet250CustomPayloadID, int i_segmentSize)
    {
      return new MultipartPacketTester(i_channel, i_packet250CustomPayloadID, i_segmentSize);
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

    protected MultipartPacketTester(String i_channel, byte i_packet250CustomPayloadID, int i_segmentSize)
    {
      super(i_channel, i_packet250CustomPayloadID, i_segmentSize);
    }
  }

}
