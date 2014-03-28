package speedytools.common.network.multipart;

import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.network.PacketHandler;
import speedytools.common.utilities.ErrorLog;

import java.io.*;
import java.util.BitSet;

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



  public boolean processIncomingPacket(Packet250CustomPayload packet)
  {
    try {
      DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet.data));

      byte packetID = inputStream.readByte();
      if (packetID != packet250CustomPayloadID) return false;

      byte commandValue = inputStream.readByte();
      Command command = byteToCommand(commandValue);
      if (command == null) return null;

      Packet250CloneToolUse newPacket = new Packet250CloneToolUse(command);
      newPacket.toolID = inputStream.readInt();
      newPacket.sequenceNumber = inputStream.readInt();
      newPacket.actionToBeUndoneSequenceNumber = inputStream.readInt();
      newPacket.xpos = inputStream.readInt();
      newPacket.ypos = inputStream.readInt();
      newPacket.zpos = inputStream.readInt();
      newPacket.rotationCount = inputStream.readByte();
      newPacket.flipped = inputStream.readBoolean();
      if (newPacket.checkInvariants()) return newPacket;
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
   * returns true if at least one acknowledgement has been received since the last reset
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
      writeCommonHeader(outputStream, Command.ACKNOWLEDGEMENT);
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
      writeCommonHeader(outputStream, Command.ABORT);
      retval = new Packet250CustomPayload(channel, bos.toByteArray());
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Failed to getAbortPacket, due to exception " + ioe.toString());
      return null;
    }
    return retval;
  }

  protected abstract void createMultipartPacket();

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
      writeCommonHeader(outputStream, Command.SEGMENTDATA);
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

  /**
   * reads the header common to all packet types
   * @param inputStream
   * @return true if the packet is valid, false otherwise
   * @throws IOException
   */
  protected boolean readCommonHeader(DataInputStream inputStream) throws IOException
  {
    if (inputStream.readByte();
    byte multi
    outputStream.writeByte(multipacketTypeID);
    outputStream.writeInt(uniquePacketID);
    outputStream.writeByte(commandToByte(command));

    byte commandValue = inputStream.readByte();
    Command command = byteToCommand(commandValue);
    if (command == null) return null;

    Packet250CloneToolUse newPacket = new Packet250CloneToolUse(command);
    newPacket.toolID = inputStream.readInt();
    newPacket.sequenceNumber = inputStream.readInt();


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
    outputStream.writeByte(multipacketTypeID);
    outputStream.writeInt(uniquePacketID);
    outputStream.writeByte(commandToByte(command));
  }

  protected MultipartPacket(String i_channel, byte i_packet250CustomPayloadID, byte i_multipacketTypeID, int i_uniquePacketID, int i_segmentSize)
  {
    channel = i_channel;
    packet250CustomPayloadID = i_packet250CustomPayloadID;
    multipacketTypeID = i_multipacketTypeID;
    uniquePacketID = i_uniquePacketID;
    segmentSize = i_segmentSize;
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
  private byte packet250CustomPayloadID;
  private byte multipacketTypeID;
  private int uniquePacketID;
  private int segmentSize;
  private int segmentCount;
  private byte [] rawData;

  private boolean iAmASender;        // true if this packet is being sent; false if it's being received.
  private BitSet segmentsNotReceived;
  private BitSet segmentsNotAcknowledged;
  private boolean acknowledgementsReceivedFlag;  // set to true once one or more acknowledgements have been received

  private int nextUnsentSegment = -1;         // the next segment we have never sent.  -1 is dummy value to trigger error if we forget to initialise
  private int nextUnacknowledgedSegment = 0; // the next unacknowledged segment.
  private boolean aborted = false;
}


Packet250CustomPayload:
        Comprised of a header with
        channel, packetID, MultipacketTypeID, MultipartPacket unique ID number, BitSet header for all segments, in addition to the actual packet data
        abort command
        Max size 32 k per payload?

        MultipartPacket:
        specify packet size
        base class; overridden by the specific packet of interest
        processIncomingPacket(Packet250CustomPayload)
        getMultipartSegment - returns a Packet250CustomPayload
        complete and acknowledged
        isComplete
        resetAcknowledged, isAcknowledged
        various methods to get & set the data in the packet as appropriate
