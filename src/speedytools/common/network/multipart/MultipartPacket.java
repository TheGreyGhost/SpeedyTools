package speedytools.common.network.multipart;

import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.network.PacketHandler;
import speedytools.common.utilities.ErrorLog;

import java.io.*;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;

/**
 * User: The Grey Ghost
 * Date: 28/03/14
 * Multipart Packets are used to send large amounts of data, bigger than will fit into a single Packet250CustomPayload.
 *   They are designed to be tolerant of network faults such as dropped packets, out-of-order packets, duplicate copies
 *   of the same packet, loss of connection, etc
 *   See MultipartPacketHandler or multipartprotocols.txt for more information
 */
public abstract class MultipartPacket
{
  /**
   * returns the uniqueID for this packet, assuming it is a MultipartPacket
   * @param packet
   * @return the uniqueID, or null if invalid
   */
  public static Long readUniqueID(Packet250CustomPayload packet)
  {
    try {
      DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet.data));
      CommonHeaderInfo chi = CommonHeaderInfo.readCommonHeader(inputStream);
      return chi.getUniqueID();
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Exception while reading processIncomingPacket: " + ioe);
    }
    return null;
  }

  protected static class CommonHeaderInfo
  {
    public byte packet250CustomPayloadID;
    public int uniquePacketID;
    public Command command;

    public long getUniqueID() {
      long retval = 0;
      retval |= packet250CustomPayloadID;
      retval <<= 32;
      retval |= uniquePacketID;
      return retval;
    }

    /**
     * reads the header common to all packet types
     * @param inputStream
     * @return true if the packet is valid, false otherwise
     * @throws IOException
     */
    protected static CommonHeaderInfo readCommonHeader(DataInputStream inputStream) throws IOException
    {
      CommonHeaderInfo chi = new CommonHeaderInfo();
      chi.packet250CustomPayloadID = inputStream.readByte();
      chi.uniquePacketID = inputStream.readInt();
      chi.command = byteToCommand(inputStream.readByte());
      return chi;
    }

    /**
     *   Writes the header common to all packet types
     *    - byte packet250CustomPayloadID (as per normal Packet250CustomPayload data[0])
     *    - byte multipartPacket type ID (unique for each class derived from MultipartPacket eg MapdataPacket extends MultipartPacket, SelectiondataPacket extends MultipartPacket, etc)
     *    - int uniqueMultipartPacketID (in ascending order) - unique ID for the whole packet
     *    - byte command type (segment data, acknowledgement of segments, abort)
     * @param outputStream
     */
    protected void writeCommonHeader(DataOutputStream outputStream, Command command) throws IOException
    {
      outputStream.writeByte(packet250CustomPayloadID);
      outputStream.writeInt(uniquePacketID);
      outputStream.writeByte(commandToByte(command));
    }
  }

  /**
   * attempt to process the incoming packet
   * @param packet
   * @return true for success
   */
  public boolean processIncomingPacket(Packet250CustomPayload packet)
  {
    try {
      DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet.data));
      CommonHeaderInfo chi = CommonHeaderInfo.readCommonHeader(inputStream);
      if (chi.packet250CustomPayloadID != commonHeaderInfo.packet250CustomPayloadID) throw new IOException("Invalid payloadID");
      if (chi.command == null) throw new IOException("Invalid command");

      switch (chi.command) {
        case ACKNOWLEDGEMENT: {
          processAcknowledgement(inputStream);
          break;
        }
        case ABORT: {
          processAbort(inputStream);
        }
        case SEGMENTDATA: {
          processSegmentData(inputStream);
        }
        default: {
          assert false : "Invalid command";
        }
      }
      return true;
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Exception while reading processIncomingPacket: " + ioe);
    }
    return false;
  }

  /** returns the next segment that has never been sent
   * @return the packet for the segment; null for failure or "no more unsent"
   */
  public Packet250CustomPayload getNextUnsentSegment()
  {
    assert(checkInvariants());
    if (!iAmASender) return null;
    if (aborted) return null;

    assert (nextUnsentSegment >= 0);
    if (nextUnsentSegment >= segmentCount) return null;
    Packet250CustomPayload retval = getSegmentByNumber(nextUnsentSegment);
    ++nextUnsentSegment;
    return retval;
  }

  /** returns the next in the sequence of unacknowledged segments
   *  Note - will not return the same segment, even if unacknowledged, until resetToOldestUnacknowledgedSegment is called.
   * @return the packet for the segment; null for failure or for "no more sent and unacknowledged"
   */
  public Packet250CustomPayload getNextUnacknowledgedSegment()
  {
    assert(checkInvariants());
    if (!iAmASender) return null;
    if (aborted) return null;

    if (nextUnacknowledgedSegment >= segmentCount || nextUnacknowledgedSegment >= nextUnsentSegment) return null;
    if (!segmentsNotAcknowledged.get(nextUnacknowledgedSegment)) {  // i.e. this segment has been acknowledged
      nextUnacknowledgedSegment = segmentsNotAcknowledged.nextSetBit(nextUnacknowledgedSegment);
      if (nextUnacknowledgedSegment >= segmentCount || nextUnacknowledgedSegment >= nextUnsentSegment) return null;
    }

    Packet250CustomPayload retval = getSegmentByNumber(nextUnacknowledgedSegment);
    ++nextUnacknowledgedSegment;
    return retval;
  }

  /** resets getNextUnacknowledgedSegment to the oldest unacknowledged segment
   */
  public void resetToOldestUnacknowledgedSegment() { nextUnacknowledgedSegment = 0; }

  /**
   * have all segments been sent at least once?
   * @return true if all segments been sent at least once
   */
  public boolean allSegmentsSent() { return nextUnsentSegment >= segmentCount; }

  /**
   * have all segments been acknowledged?
   * @return true if all segments have been acknowledged
   */
  public boolean allSegmentsAcknowledged() { return segmentsNotAcknowledged.isEmpty(); }

  /**
   * have all segments been received?
   * @return true if all segments have been received
   */
  public boolean allSegmentsReceived() { return segmentsNotReceived.isEmpty(); }

  /**
   * has this packet has been aborted?
   * @return true if the packet has been aborted
   */
  public boolean hasBeenAborted() {return aborted; }

  /**
   * returns true if at least one new acknowledgement has been received since the last reset
   * (in order to be counted, the acknowledgement must have included at least one
   *   segment not previously acknowledged)
   * @return
   */
  public boolean getAcknowledgementsReceivedFlag()
  {
    return acknowledgementsReceivedFlag;
  }

  /**
   * reset the acknowledgementsReceived flag
   */
  public void resetAcknowledgementsReceivedFlag()  { acknowledgementsReceivedFlag = false; }

  /**
   * returns true if at least one new segment has been received since the last reset
   * (segments which have been received previously don't count)
   * @return
   */
  public boolean getSegmentsReceivedFlag()
  {
    return segmentsReceivedFlag;
  }

  /**
   * reset the segmentsReceivedFlag flag
   */
  public void resetSegmentsReceivedFlag()  { segmentsReceivedFlag = false; }

  /**
   * create a packet to inform of all the segments received to date
   * @return
   */
  public Packet250CustomPayload getAcknowledgementPacket()
  {
    assert(checkInvariants());
    if (iAmASender) return null;

    Packet250CustomPayload retval = null;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      DataOutputStream outputStream = new DataOutputStream(bos);
      commonHeaderInfo.writeCommonHeader(outputStream, Command.ACKNOWLEDGEMENT);
      byte [] buff = segmentsNotAcknowledged.toByteArray();
      if (buff.length > MAX_SEGMENT_SIZE) throw new IOException("bitset too big");
      outputStream.writeShort(buff.length);
      outputStream.write(buff);
      retval = new Packet250CustomPayload(channel, bos.toByteArray());
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Failed to getAcknowledgementPacket, due to exception " + ioe.toString());
      return null;
    }
    return retval;
  }

  /**
   * create a packet to inform that this multipartPacket has been aborted
   * @return
   */
  public Packet250CustomPayload getAbortPacket()
  {
    assert(checkInvariants());
    Packet250CustomPayload retval = null;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      DataOutputStream outputStream = new DataOutputStream(bos);
      commonHeaderInfo.writeCommonHeader(outputStream, Command.ABORT);
      retval = new Packet250CustomPayload(channel, bos.toByteArray());
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Failed to getAbortPacket, due to exception " + ioe.toString());
      return null;
    }
    return retval;
  }

  /**
   * get the packet for the given segment number
   * @param segmentNumber from 0 up to segmentCount - 1 inclusive
   * @return the packet, or null for failure
   */
  protected Packet250CustomPayload getSegmentByNumber(int segmentNumber)
  {
    Packet250CustomPayload retval = null;
    try {
      if (segmentNumber < 0 || segmentNumber >= segmentCount) throw new IOException("Invalid segment number:" + segmentNumber);
      int startBuffPos = segmentNumber * segmentSize;
      int segmentLength = Math.min(segmentSize, rawData.length - startBuffPos);
      assert (segmentLength < Short.MAX_VALUE);

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      DataOutputStream outputStream = new DataOutputStream(bos);
      commonHeaderInfo.writeCommonHeader(outputStream, Command.SEGMENTDATA);
      outputStream.writeShort(segmentNumber);
      outputStream.writeShort(segmentLength);
      outputStream.writeInt(rawData.length);
      outputStream.write(rawData, startBuffPos, segmentLength);
      retval = new Packet250CustomPayload(channel, bos.toByteArray());
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Failed to getAcknowledgementPacket, due to exception " + ioe.toString());
      return null;
    }
    return retval;
  }

  protected MultipartPacket(String i_channel, byte i_packet250CustomPayloadID, int i_uniquePacketID,
                            int i_segmentSize, boolean i_thisPacketIsASender)
  {
    channel = i_channel;
    commonHeaderInfo.packet250CustomPayloadID = i_packet250CustomPayloadID;
    commonHeaderInfo.uniquePacketID = i_uniquePacketID;
    if (i_segmentSize <= 0 || i_segmentSize > MAX_SEGMENT_SIZE) throw new IllegalArgumentException("segmentSize " + i_segmentSize + " is out of range [0 -  " + MAX_SEGMENT_SIZE + "]");
    segmentSize = i_segmentSize;

    segmentCount = 0;
    rawData = null;
    iAmASender = i_thisPacketIsASender;
    segmentsNotAcknowledged = null;
    segmentsNotReceived = null;
    acknowledgementsReceivedFlag = false;
    segmentsReceivedFlag = false;

    nextUnsentSegment = 0;
    nextUnacknowledgedSegment = 0;
    aborted = false;

  }

  /** set the raw data for this packet and initialise the associated segment data
   *
   * @param newRawData
   */
  protected void setRawData(byte [] newRawData)
  {
    int totalLength = newRawData.length;
    if (totalLength > MAX_SEGMENT_COUNT * segmentSize) {
      throw new IllegalArgumentException("RawData size " + totalLength + " is greater than the maximum " + segmentSize * MAX_SEGMENT_COUNT + "for segment size " + segmentSize);
    }
    segmentCount = (totalLength + segmentSize - 1) / segmentSize;

    rawData = newRawData;
    segmentsNotReceived = new BitSet(segmentCount);
    segmentsNotReceived.set(0, segmentCount);
    segmentsNotAcknowledged = new BitSet(segmentCount);
    segmentsNotAcknowledged.set(0, segmentCount);
  }

  /** abort this packet
   * @param inputStream
   * @throws IOException
   */
  protected void processAbort(DataInputStream inputStream) throws IOException
  {
    aborted = true;
  }

  /** incorporate the data for this segment into the packet
   * @param inputStream
   * @throws IOException
   */
  protected void processSegmentData(DataInputStream inputStream) throws IOException
  {
    short segmentNumber = inputStream.readShort();
    short segmentLength = inputStream.readShort();
    int   rawdataLength = inputStream.readInt();
    if (segmentNumber < 0 || segmentNumber >= segmentCount) throw new IOException("Packet segment number " + segmentNumber + " outside valid range [0 - " + (segmentCount-1) + "] inclusive");
    if (!segmentsNotReceived.get(segmentNumber)) return;  // duplicate of a segment already received, just ignore it

    if (rawdataLength != rawData.length) throw new IOException("Packet rawdataLength " + rawdataLength + " does not match expected (" + rawData.length + ")");
    int startBuffPos = segmentNumber * segmentSize;
    int expectedSegmentLength = Math.min(segmentSize, rawData.length - startBuffPos);
    if (expectedSegmentLength != segmentLength) throw new IOException("Packet segment length " + segmentLength + " does not match expected (" + expectedSegmentLength + ")");
    assert (segmentLength < Short.MAX_VALUE);
    int bytesread = inputStream.read(rawData, startBuffPos, segmentLength);
    if (bytesread != segmentLength) throw new IOException("Size of segment data read " + bytesread + " did not match expected " + segmentLength);

    segmentsNotReceived.clear(segmentNumber);
  }

  /** integrate the acknowledgement bits into this packet
   * @param inputStream
   * @throws IOException
   */
  protected void processAcknowledgement(DataInputStream inputStream) throws IOException
  {
    if (!iAmASender) throw new IOException("received acknowledgement packet on receiver side");
    short ackDataLength = inputStream.readShort();
    if (ackDataLength <= 0) throw new IOException("Invalid data length" + ackDataLength);
    byte [] buff = new byte[ackDataLength];
    int readcount = inputStream.read(buff);
    if (readcount != ackDataLength) throw new IOException("readCount " + readcount + " didn't match expected " + ackDataLength);
    BitSet notAcknowledged = BitSet.valueOf(buff);

    // check to see if we have received acknowledgement for any segments we haven't sent yet!
    //  i.e. if the last zero is greater than the sent segment count
    int lastAcknowledgedPacket = notAcknowledged.previousClearBit(segmentCount - 1);
    if (lastAcknowledgedPacket >= nextUnsentSegment) throw new IOException("acknowledged segment was never sent");
    BitSet savedNack = (BitSet)segmentsNotAcknowledged.clone();
    segmentsNotAcknowledged.and(notAcknowledged);
    if (!savedNack.equals(segmentsNotAcknowledged)) acknowledgementsReceivedFlag = true;
  }

  protected enum Command {
    SEGMENTDATA, ACKNOWLEDGEMENT, ABORT;
    public static final Command[] allValues = {SEGMENTDATA, ACKNOWLEDGEMENT, ABORT};
  }

  private static Command byteToCommand(byte value)
  {
    if (value < 0 || value >= Command.allValues.length) return null;
    return Command.allValues[value];
  }

  private static byte commandToByte(Command value) throws IOException
  {
    byte retval;

    if (value != null) {
      for (retval = 0; retval < Command.allValues.length; ++retval) {
        if (Command.allValues[retval] == value) return retval;
      }
    }
    throw new IOException("Invalid Command value");
  }

  /**
   * checks this class to see if it is internally consistent
   * @return true if ok, false if bad.
   */
  protected boolean checkInvariants()
  {
    if (segmentSize <= 0 || segmentSize > MAX_SEGMENT_SIZE) return false;
    return true;
  }

  private final int MAX_SEGMENT_COUNT = Short.MAX_VALUE;
  private final int MAX_SEGMENT_SIZE = 30000;

  private String channel;
  private CommonHeaderInfo commonHeaderInfo;

  private int segmentSize;
  private int segmentCount;
  private byte [] rawData;

  private boolean iAmASender;                    // true if this packet is being sent; false if it's being received.
  private BitSet segmentsNotReceived;
  private BitSet segmentsNotAcknowledged;
  private boolean acknowledgementsReceivedFlag;  // set to true once one or more acknowledgements have been received
  private boolean segmentsReceivedFlag;          // set to true once one or more segments have been received

  private int nextUnsentSegment = -1;         // the next segment we have never sent.  -1 is dummy value to trigger error if we forget to initialise
  private int nextUnacknowledgedSegment = 0; // the next unacknowledged segment.
  private boolean aborted = false;

}

