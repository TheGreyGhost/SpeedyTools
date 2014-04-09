package speedytools.common.network.multipart;

import cpw.mods.fml.relauncher.Side;
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
 *   Usage:
 *   (1) Create a class extending MultipartPacket.  Similar to MultipartPacket it should have two constructors
 *    (a) For generating a send packet: the equivalent MultipartPacket constructor should be called, and then
 *        later on once the packet is fully populated with the raw data to be sent, call
 *        setRawData to complete the packet setup
 *    (b) For generating a receive packet: the constructor should accept a packet, call the equivalent MultipartPacket constructor,
 *        complete setup of the class, AND THEN CALL processIncomingPacket after construction is finished, to process the segment rawdata
 *   For senders:
 *   (1) call getNextUnsentSegment to retrieve the next unsent segment.  Send it over the network.  null means there are no more segments.
 *   (2) as incoming packets (acknowledgements) arrive, use MultipartPacket.readUniqueID to find the packet ID, and then send them to processIncomingPacket
 *   (3) once all segments are sent (allSegmentsSent, or getNextUnsentSegment returns null), wait for an appropriate time, then
 *       check for unacknowledged packets (allSegmentsAcknowledged is false).  If necessary:
 *   (4) use getNextUnacknowledgedSegment to iterate through segments which were sent but no acknowledgement yet received.
 *       use resetToOldestUnacknowledgedSegment, getAcknowledgementsReceivedFlag, resetAcknowledgementsReceivedFlag to monitor
 *       incoming acknowledgements, and to control which packets are resent
 *   (5) once allSegmentsAcknowledged() is true, the packet is finished and can be discarded
 *   (6) transmission of the packet can be aborted by calling getAbortPacket() and sending it.
 *       if you receive an Acknowledgment packet for an ID which doesn't exist, call the static getAbortPacketForLostPacket(packet) to
 *       convert it to an abort packet and send it back
 *   For receivers:
 *   (1) Incoming packets are processed by processIncomingPacket.  The uniqueID of an incoming packet can be inspected before processing
 *       using the static readUniqueID
 *   (2) Periodically, check getSegmentsReceivedFlag, and if some have been received, call getAcknowledgementPacket() and send it, also
 *       call resetSegmentsReceivedFlag
 *   (3) When allSegmentsReceived is true, the packet is complete and can be processed
 *   (4) transmission of the packet can be aborted by calling getAbortPacket() and sending it.
 *       if you receive a packet for an ID which you have aborted, call the static getAbortPacketForLostPacket(packet) to
 *       convert it to an abort packet and send it back.  (No abort packets are generated in response to an incoming abort packet)
 *   (5) You should maintain a list of completed and aborted packet IDs for a suitable length of time, to avoid creating a new Multipart
 *       packet in response to delayed packets.
 */
public abstract class MultipartPacket
{
  /**
   * returns the uniqueID for this packet, assuming it is a MultipartPacket
   * @param packet
   * @return the uniqueID, or null if invalid
   */
  public static Integer readUniqueID(Packet250CustomPayload packet)
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

  /**
   * returns the packetTypeID for this packet, assuming it is a MultipartPacket
   * @param packet
   * @return the packetTypeID, or null if invalid
   */
/*  public static Byte readPacketTypeID(Packet250CustomPayload packet)
  {
    try {
      DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet.data));
      CommonHeaderInfo chi = CommonHeaderInfo.readCommonHeader(inputStream);
      return chi.packet250CustomPayloadID;
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Exception while reading processIncomingPacket: " + ioe);
    }
    return null;
  }
*/
  /**
   * returns the unique ID for this packet
   * @return the packet's unique ID
   */
  public int getUniqueID()
  {
    return commonHeaderInfo.getUniqueID();
  }

//  public byte getPacketTypeID() {return commonHeaderInfo.packet250CustomPayloadID; }

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
      if (chi.uniquePacketID != commonHeaderInfo.uniquePacketID) {
        throw new IOException("incoming unique ID " + chi.uniquePacketID + " doesn't match multipart unique ID " + commonHeaderInfo.uniquePacketID);
      }
      switch (chi.command) {
        case ACKNOWLEDGEMENT: {
          processAcknowledgement(inputStream);
          break;
        }
        case ABORT: {
          processAbort(inputStream);
          break;
        }
        case SEGMENTDATA: {
          processSegmentData(inputStream);
          break;
        }
        case ACKNOWLEDGE_ALL: {
          processAcknowledgeAll(inputStream);
          break;
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
   * @return true if I am a sender and all segments been sent at least once
   */
  public boolean allSegmentsSent() { return iAmASender ? nextUnsentSegment >= segmentCount : false; }

  /**
   * have all segments been acknowledged?
   * @return true if I am a sender and all segments have been acknowledged
   */
  public boolean allSegmentsAcknowledged() { return iAmASender ? segmentsNotAcknowledged.isEmpty() : false; }

  /**
   * have all segments been received?
   * @return true if I am a receiver and all segments have been received,
   */
  public boolean allSegmentsReceived() { return iAmASender ? false : segmentsNotReceived.isEmpty(); }

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

  /** returns the percentage completion of the transmission
   *
   * @return 0 - 100 inclusive
   */
  public int getPercentComplete()
  {
    if (iAmASender) {
      int segmentsComplete = nextUnsentSegment + (segmentCount -  segmentsNotAcknowledged.cardinality());
      return 100 * segmentsComplete / 2 / segmentCount;
    } else {
      return 100 - (100 * segmentsNotReceived.cardinality() / segmentCount);
    }
  }

  /**
   * create a packet to inform of all the segments received to date
   * @return the packet, or null for a problem
   */
  public Packet250CustomPayload getAcknowledgementPacket()
  {
    assert(checkInvariants());
    if (iAmASender) return null;
    if (aborted) return null;

    Packet250CustomPayload retval = null;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      DataOutputStream outputStream = new DataOutputStream(bos);
      commonHeaderInfo.writeCommonHeader(outputStream, Command.ACKNOWLEDGEMENT);
      byte [] buff = segmentsNotReceived.toByteArray();
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
   * aborts this multipartPacket, and creates a packet to inform that this multipartPacket has been aborted
   * @return the packet, or null for a problem
   */
  public Packet250CustomPayload getAbortPacket()
  {
    assert(checkInvariants());
    Packet250CustomPayload retval = null;
    aborted = true;
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
   * create a packet to inform that this multipartPacket lostPacket has been aborted
   * @param acknowledgeAbortPackets - if true, generate an abort packet even if the lostPacket is itself an abort packet
   * @return the abort packet, or null if not possible, or if the lostPacket is itself an abort packet and acknowledgeAbortPackets is false
   */
  public static Packet250CustomPayload getAbortPacketForLostPacket(Packet250CustomPayload lostPacket, boolean acknowledgeAbortPackets)
  {
    Packet250CustomPayload retval = null;
    try {
      DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(lostPacket.data));
      CommonHeaderInfo chi = CommonHeaderInfo.readCommonHeader(inputStream);
      if (chi.command == Command.ABORT && !acknowledgeAbortPackets) return null;

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      DataOutputStream outputStream = new DataOutputStream(bos);
      chi.writeCommonHeader(outputStream, Command.ABORT);
      lostPacket.data = bos.toByteArray();
      retval = lostPacket;
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Failed to getAbortPacketForLostPacket, due to exception " + ioe.toString());
      return null;
    }
    return retval;
  }

  /**
   * create a packet to inform that this multipartPacket lostPacket has been completed (ACKNOWLEDGE ALL)
   * @return the acknowledge packet, or null if not possible, or an abort packet if the lostPacket is an abort packet
   */
  public static Packet250CustomPayload getFullAcknowledgePacketForLostPacket(Packet250CustomPayload lostPacket)
  {
    Packet250CustomPayload retval = null;
    try {
      DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(lostPacket.data));
      CommonHeaderInfo chi = CommonHeaderInfo.readCommonHeader(inputStream);

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      DataOutputStream outputStream = new DataOutputStream(bos);
      chi.writeCommonHeader(outputStream, (chi.command == Command.ABORT) ? Command.ABORT : Command.ACKNOWLEDGE_ALL);
      lostPacket.data = bos.toByteArray();
      retval = lostPacket;
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Failed to getFullAcknowledgePacketForLostPacket, due to exception " + ioe.toString());
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
      outputStream.writeShort(segmentSize);
      outputStream.writeInt(rawData.length);
      outputStream.write(rawData, startBuffPos, segmentLength);
      retval = new Packet250CustomPayload(channel, bos.toByteArray());
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Failed to getAcknowledgementPacket, due to exception " + ioe.toString());
      return null;
    }
    return retval;
  }

  /** Generate a MultipartPacket to be used for sending data
   *  It is initialised to have no data; the sub class must add the rawData later using setRawData
   * @param i_channel  The channel to use when sending packets
   * @param i_whichSideAmIOn is this packet being created on the server side or the client side?
   * @param i_packet250CustomPayloadID The custom payload ID to use when sending packets
   * @param i_segmentSize The segment size to be used
   */
  protected MultipartPacket(String i_channel, Side i_whichSideAmIOn, byte i_packet250CustomPayloadID, int i_segmentSize)
  {
    channel = i_channel;
    iAmASender = true;
    whichSideAmIOn = i_whichSideAmIOn;

    commonHeaderInfo.packet250CustomPayloadID = i_packet250CustomPayloadID;
    if (i_segmentSize <= 0 || i_segmentSize > MAX_SEGMENT_SIZE) throw new IllegalArgumentException("segmentSize " + i_segmentSize + " is out of range [0 -  " + MAX_SEGMENT_SIZE + "]");
    segmentSize = i_segmentSize;
    commonHeaderInfo.uniquePacketID = (nextUniquePacketID << 1) + (whichSideAmIOn == Side.SERVER ? 1 : 0);
    nextUniquePacketID++;

    segmentCount = 0;
    rawData = null;
    segmentsNotAcknowledged = null;
    segmentsNotReceived = null;
    acknowledgementsReceivedFlag = false;
    segmentsReceivedFlag = false;

    nextUnsentSegment = 0;
    nextUnacknowledgedSegment = 0;
    aborted = false;
  }

  /** sets up the MultipartPacket from the incoming packet containing segment data
   * *** DOES NOT PROCESS THE PACKET, the subclass should call processIncomingPacket after construction is finished
   * @param packet
   * @throws IOException if an error occurs or the packet doesn't contain segment data
   */
  protected MultipartPacket(Packet250CustomPayload packet) throws IOException
  {
    DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet.data));
    CommonHeaderInfo chi = CommonHeaderInfo.readCommonHeader(inputStream);
    if (chi.command == null) throw new IOException("Invalid command");
    if (chi.command != Command.SEGMENTDATA) throw new IOException("Tried to create a new Multipart packet from a non-segment-data packet (" + chi.command + ")");

    channel = packet.channel;
    commonHeaderInfo = chi;

    short segmentNumber = inputStream.readShort();
    short pktSegmentSize = inputStream.readShort();
    int   rawdataLength = inputStream.readInt();

    if (pktSegmentSize <= 0 || pktSegmentSize > MAX_SEGMENT_SIZE) throw new IOException("Packet segment size " + pktSegmentSize + " out of allowable range [1 - " + MAX_SEGMENT_SIZE + "]");
    segmentSize = pktSegmentSize;
    int segmentCount = (rawdataLength + segmentSize - 1) / segmentSize;
    if (segmentCount <= 0 || segmentCount > MAX_SEGMENT_COUNT) throw new IOException("Segment count " + segmentCount + " out of allowable range [1 - " + MAX_SEGMENT_COUNT + "]");

    setRawData(new byte[rawdataLength]);
  }

  private MultipartPacket() {};

  /** set the raw data for this packet and initialise the associated segment data
   * @param newRawData the rawdata to be copied into the packet
   */
  protected void setRawData(byte [] newRawData)
  {
    int totalLength = newRawData.length;
    if (totalLength > MAX_SEGMENT_COUNT * segmentSize) {
      throw new IllegalArgumentException("RawData size " + totalLength + " is greater than the maximum " + segmentSize * MAX_SEGMENT_COUNT + "for segment size " + segmentSize);
    }
    segmentCount = (totalLength + segmentSize - 1) / segmentSize;

    rawData = Arrays.copyOf(newRawData, newRawData.length);
    if (iAmASender) {
      segmentsNotAcknowledged = new BitSet(segmentCount);
      segmentsNotAcknowledged.set(0, segmentCount);
    } else {
      segmentsNotReceived = new BitSet(segmentCount);
      segmentsNotReceived.set(0, segmentCount);
    }
    assert checkInvariants();
  }

  /** retrieve a copy of the packet's raw data
   * @return the raw data, or null if the packet isn't finished yet (error)
   */
  protected byte [] getRawDataCopy()
  {
    if (!iAmASender && !allSegmentsReceived()) return null;
    return Arrays.copyOf(rawData, rawData.length);
  }

  /** abort this packet
   * @param inputStream
   * @throws IOException
   */
  protected void processAbort(DataInputStream inputStream) throws IOException
  {
    aborted = true;
    assert checkInvariants();
  }

  /** fully acknowledge this packet
   * @param inputStream
   * @throws IOException
   */
  protected void processAcknowledgeAll(DataInputStream inputStream) throws IOException
  {
    if (!iAmASender) throw new IOException("received acknowledgement packet on receiver side");
    if (segmentsNotAcknowledged.isEmpty()) return;
    segmentsNotAcknowledged.clear();
    nextUnsentSegment = segmentCount;
    acknowledgementsReceivedFlag = true;
    nextUnacknowledgedSegment = segmentCount;
    assert checkInvariants();
  }

  /** incorporate the data for this segment into the packet
   * @param inputStream
   * @throws IOException
   */
  protected void processSegmentData(DataInputStream inputStream) throws IOException
  {
    short segmentNumber = inputStream.readShort();
    short pktSegmentSize = inputStream.readShort();
    int   rawdataLength = inputStream.readInt();
    if (segmentNumber < 0 || segmentNumber >= segmentCount) throw new IOException("Packet segment number " + segmentNumber + " outside valid range [0 - " + (segmentCount-1) + "] inclusive");
    if (!segmentsNotReceived.get(segmentNumber)) return;  // duplicate of a segment already received, just ignore it

    if (pktSegmentSize != segmentSize) throw new IOException("Packet segment size " + pktSegmentSize + " does not match expected (" + segmentSize + ")");
    if (rawdataLength != rawData.length) throw new IOException("Packet rawdataLength " + rawdataLength + " does not match expected (" + rawData.length + ")");
    int startBuffPos = segmentNumber * segmentSize;
    int expectedSegmentLength = Math.min(segmentSize, rawData.length - startBuffPos);
    assert (expectedSegmentLength < Short.MAX_VALUE);
    int bytesread = inputStream.read(rawData, startBuffPos, expectedSegmentLength);
    if (bytesread != expectedSegmentLength) throw new IOException("Size of segment data read " + bytesread + " did not match expected " + expectedSegmentLength);

    segmentsNotReceived.clear(segmentNumber);
    segmentsReceivedFlag = true;
    assert checkInvariants();
  }

  /** integrate the acknowledgement bits into this packet
   * @param inputStream
   * @throws IOException
   */
  protected void processAcknowledgement(DataInputStream inputStream) throws IOException
  {
    if (!iAmASender) throw new IOException("received acknowledgement packet on receiver side");
    short ackDataLength = inputStream.readShort();
    if (ackDataLength < 0) throw new IOException("Invalid data length" + ackDataLength);
    byte [] buff = new byte[ackDataLength];
    if (ackDataLength != 0) {
      int readcount = inputStream.read(buff);
      if (readcount != ackDataLength) throw new IOException("readCount " + readcount + " didn't match expected " + ackDataLength);
    }
    BitSet notAcknowledged = BitSet.valueOf(buff);

    // check to see if we have received acknowledgement for any segments we haven't sent yet!
    //  i.e. if the last zero is greater than the sent segment count
    int lastAcknowledgedPacket = notAcknowledged.previousClearBit(segmentCount - 1);
    if (lastAcknowledgedPacket >= nextUnsentSegment) throw new IOException("acknowledged segment was never sent");
    BitSet savedNack = (BitSet)segmentsNotAcknowledged.clone();
    segmentsNotAcknowledged.and(notAcknowledged);
    if (!savedNack.equals(segmentsNotAcknowledged)) acknowledgementsReceivedFlag = true;
    assert checkInvariants();
  }

  protected enum Command {
    SEGMENTDATA, ACKNOWLEDGEMENT, ABORT, ACKNOWLEDGE_ALL;
    public static final Command[] allValues = {SEGMENTDATA, ACKNOWLEDGEMENT, ABORT, ACKNOWLEDGE_ALL};
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

  public static class CommonHeaderInfo
  {
    public byte packet250CustomPayloadID;
    public int uniquePacketID;
    public Command command;

    public int getUniqueID() {
      return uniquePacketID;
    }

    /**
     * reads the header common to all packet types
     * @param inputStream
     * @return true if the packet is valid, false otherwise
     * @throws IOException
     */
    public static CommonHeaderInfo readCommonHeader(DataInputStream inputStream) throws IOException
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
    public void writeCommonHeader(DataOutputStream outputStream, Command command) throws IOException
    {
      outputStream.writeByte(packet250CustomPayloadID);
      outputStream.writeInt(uniquePacketID);
      outputStream.writeByte(commandToByte(command));
    }
  }

  // derived classes should implement this interface so that other wishing to create a new MultipartPacket (in response to an incoming packet) can pass this object to the packet handler which will invoke it.
  public interface MultipartPacketCreator
  {
    public MultipartPacket createNewPacket(Packet250CustomPayload packet);
  }

  /**
   * checks this class to see if it is internally consistent
   * @return true if ok, false if bad.
   */
  protected boolean checkInvariants()
  {
    if (aborted) return true;
    if (segmentSize <= 0 || segmentSize > MAX_SEGMENT_SIZE) return false;
    if (segmentCount <= 0 || segmentCount > MAX_SEGMENT_COUNT) return false;
    if (iAmASender) {
      if (nextUnsentSegment < 0 || nextUnsentSegment > segmentCount) return false;
      if (nextUnacknowledgedSegment < 0 || nextUnacknowledgedSegment > segmentCount) return false;
      if (segmentsNotAcknowledged.length() > segmentCount) return false;
      if (segmentsNotAcknowledged == null || segmentsNotAcknowledged.length() > segmentCount) return false;
      if (segmentsNotAcknowledged.previousClearBit(segmentCount-1) > nextUnsentSegment) return false;
      if ((commonHeaderInfo.uniquePacketID & 1) != (whichSideAmIOn == Side.SERVER ? 1 : 0 )) return false;
    } else {
      if (segmentsNotReceived == null || segmentsNotReceived.length() > segmentCount) return false;
    }

    return true;
  }

  public static final int MAX_SEGMENT_COUNT = Short.MAX_VALUE;
  public static final int MAX_SEGMENT_SIZE = 30000;
  public static final int NULL_PACKET_ID = -1;

  private String channel;
  private Side whichSideAmIOn;
  private CommonHeaderInfo commonHeaderInfo = new CommonHeaderInfo();

  private int segmentSize;
  private int segmentCount;
  private byte [] rawData;

  private boolean iAmASender;                    // true if this packet is being sent; false if it's being received.
  private BitSet segmentsNotReceived;
  private BitSet segmentsNotAcknowledged;
  private boolean acknowledgementsReceivedFlag;  // set to true once one or more acknowledgements have been received
  private boolean segmentsReceivedFlag;          // set to true once one or more segments have been received

  private int nextUnsentSegment = -1;         // the next segment we have never sent.  -1 is dummy value to trigger error if we forget to initialise
  private int nextUnacknowledgedSegment = -1; // the next unacknowledged segment.
  private boolean aborted = false;

  private static int nextUniquePacketID = 0;      // unique across all packets sent from this server and derived from MultipartPacket
}

