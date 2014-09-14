package speedytools.common.network.multipart;

import cpw.mods.fml.relauncher.Side;
import speedytools.common.network.Packet250Types;
import speedytools.common.utilities.ErrorLog;

import java.io.*;
import java.util.Arrays;
import java.util.BitSet;

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
   * returns the unique ID for this packet
   * @return the packet's unique ID
   */
  public int getUniqueID()
  {
    return uniquePacketID;
  }

//  public byte getPacketTypeID() {return commonHeaderInfo.packet250CustomPayloadID; }

  /**
   * attempt to process the incoming segment
   * @param packet
   * @return true for success
   */
  public boolean processIncomingSegment(Packet250MultipartSegment packet)
  {
    if (!packet.isPacketIsValid()) {
      ErrorLog.defaultLog().info("Invalid incoming packet in processIncomingPacket");
      return false;
    }
    if (packet.getUniqueMultipartID() != uniquePacketID) {
      ErrorLog.defaultLog().info("incoming unique ID " + packet.getUniqueMultipartID()+ " doesn't match multipart unique ID " + uniquePacketID);
      return false;
    }
    if (packet.getPacket250Type() != packet250Type) {
      ErrorLog.defaultLog().info("incoming packet250Type " + packet.getPacket250Type() + " doesn't match multipart packet250Type" + packet250Type);
      return false;
    }

    if (packet.isAbortTransmission()) {
      processAbort();
    } else {
      try {
        processSegmentData(packet);
      } catch (IOException ioe) {
        ErrorLog.defaultLog().info(ioe.getMessage());
        return false;
      }
    }

    return true;
  }

  /**
   * attempt to process the incoming segment
   * @param packet
   * @return true for success
   */
  public boolean processIncomingAcknowledgement(Packet250MultipartSegmentAcknowledge packet)
  {
    if (!packet.isPacketIsValid()) {
      ErrorLog.defaultLog().info("Invalid incoming packet in processIncomingAcknowledgement");
      return false;
    }
    if (packet.getUniqueID() != uniquePacketID) {
      ErrorLog.defaultLog().info("incoming unique ID " + packet.getUniqueID()+ " doesn't match multipart unique ID " + uniquePacketID);
      return false;
    }
    if (packet.getPacket250Type() != packet250Type) {
      ErrorLog.defaultLog().info("incoming packet250Type " + packet.getPacket250Type() + " doesn't match multipart packet250Type" + packet250Type);
      return false;
    }
    switch (packet.getAcknowledgement()) {
      case ABORT: {
        processAbort();
        break;
      }
      case ACKNOWLEDGE_ALL: {
        processAcknowledgeAll(packet);
        break;
      }
      case ACKNOWLEDGEMENT: {
        processAcknowledgement(packet);
        break;
      }
      default: {
        ErrorLog.defaultLog().info("Invalid acknowledgement " + packet.getAcknowledgement() + " in incoming Packet250MultipartSegmentAcknowledge");
        return false;
      }
    }

    return true;
  }


  /** returns the next segment that has never been sent
   * @return the packet for the segment; null for failure or "no more unsent"
   */
  public Packet250MultipartSegment getNextUnsentSegment()
  {
    assert(checkInvariants());
    if (!iAmASender) return null;
    if (aborted) return null;

    assert (nextUnsentSegment >= 0);
    if (nextUnsentSegment >= segmentCount) return null;
    Packet250MultipartSegment retval = getSegmentByNumber(nextUnsentSegment);
    ++nextUnsentSegment;
    return retval;
  }

  /** returns the next in the sequence of unacknowledged segments
   *  Note - will not return the same segment, even if unacknowledged, until resetToOldestUnacknowledgedSegment is called.
   * @return the packet for the segment; null for failure or for "no more sent and unacknowledged"
   */
  public Packet250MultipartSegment getNextUnacknowledgedSegment()
  {
    assert(checkInvariants());
    if (!iAmASender) return null;
    if (aborted) return null;

    if (nextUnacknowledgedSegment >= segmentCount || nextUnacknowledgedSegment >= nextUnsentSegment) return null;
    if (!segmentsNotAcknowledged.get(nextUnacknowledgedSegment)) {  // i.e. this segment has been acknowledged
      nextUnacknowledgedSegment = segmentsNotAcknowledged.nextSetBit(nextUnacknowledgedSegment);
      if (nextUnacknowledgedSegment >= segmentCount || nextUnacknowledgedSegment >= nextUnsentSegment) return null;
    }

    Packet250MultipartSegment retval = getSegmentByNumber(nextUnacknowledgedSegment);
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
  public boolean allSegmentsReceived() { return iAmASender ? false : segmentsNotReceivedYet.isEmpty(); }

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
      return 100 - (100 * segmentsNotReceivedYet.cardinality() / segmentCount);
    }
  }

  /**
   * create a packet to inform of all the segments received to date
   * @return the packet, or null for a problem
   */
  public Packet250MultipartSegmentAcknowledge getAcknowledgementPacket()
  {
    assert(checkInvariants());
    if (iAmASender) return null;
    if (aborted) return null;

    Packet250MultipartSegmentAcknowledge retval = null;
    byte [] rawBitsetBytes = segmentsNotReceivedYet.toByteArray();
    if (rawBitsetBytes.length > MAX_SEGMENT_SIZE) {
      ErrorLog.defaultLog().info("Failed to getAcknowledgementPacket, because acknowledgement bitset length " + rawBitsetBytes.length + " is > MAXSEGMENTSIZE" + MAX_SEGMENT_SIZE);
      return null;
    }

    retval = new Packet250MultipartSegmentAcknowledge(packet250Type, Packet250MultipartSegmentAcknowledge.Acknowledgement.ACKNOWLEDGEMENT,
                                                      uniquePacketID, segmentsNotReceivedYet);
    return retval;
  }

  /**
   * aborts this multipartPacket, and creates a packet to inform that this multipartPacket has been aborted
   * @return the packet, or null for a problem
   */
  public Packet250MultipartSegment getSenderAbortPacket()
  {
    assert(checkInvariants());
    Packet250MultipartSegment retval = new Packet250MultipartSegment(packet250Type, true, uniquePacketID,
            (short)0, (short)segmentSize, rawData.length, null);
    aborted = true;
    return retval;
  }

  /**
   * aborts this multipartPacket, and creates a packet to inform that this multipartPacket has been aborted
   * @return the packet, or null for a problem
   */
  public Packet250MultipartSegmentAcknowledge getReceiverAbortPacket()
  {
    assert(checkInvariants());
    Packet250MultipartSegmentAcknowledge retval = new Packet250MultipartSegmentAcknowledge(packet250Type,
            Packet250MultipartSegmentAcknowledge.Acknowledgement.ABORT, uniquePacketID, null);
    aborted = true;
    return retval;
  }

  /**
   * create a packet to inform that this multipartPacket lostPacket has been aborted
   * @param acknowledgeAbortPackets - if true, generate an abort packet even if the lostPacket is itself an abort packet
   * @return the abort packet, or null if not possible, or if the lostPacket is itself an abort packet and acknowledgeAbortPackets is false
   */
  public static Packet250MultipartSegmentAcknowledge getAbortPacketForLostPacket(Packet250MultipartSegment lostPacket, boolean acknowledgeAbortPackets)
  {
    if (lostPacket.isAbortTransmission() && !acknowledgeAbortPackets) return null;
    Packet250MultipartSegmentAcknowledge retval = new Packet250MultipartSegmentAcknowledge(lostPacket.getPacket250Type(),
            Packet250MultipartSegmentAcknowledge.Acknowledgement.ABORT, lostPacket.getUniqueMultipartID(), null);
    return retval;
  }

  /**
   * create a packet to inform that this multipartPacket lostPacket has been aborted
   * @return the abort packet, or null if not possible, or if the lostPacket is itself an abort packet
   */
  public static Packet250MultipartSegment getAbortPacketForLostPacket(Packet250MultipartSegmentAcknowledge lostPacket)
  {
    if (lostPacket.getAcknowledgement() == Packet250MultipartSegmentAcknowledge.Acknowledgement.ABORT) return null;
    Packet250MultipartSegment retval = new Packet250MultipartSegment(lostPacket.getPacket250Type(), true, lostPacket.getUniqueID(), (short)0, (short)0, 0, null);
    return retval;
  }

  /**
   * create a packet to inform that this multipartPacket lostPacket has been completed (ACKNOWLEDGE ALL)
   * @return the acknowledge packet, or null if not possible, or an abort packet if the lostPacket is an abort packet
   */
  public static Packet250MultipartSegmentAcknowledge getFullAcknowledgePacketForLostPacket(Packet250MultipartSegment lostPacket)
  {
    if (lostPacket.isAbortTransmission()) return getAbortPacketForLostPacket(lostPacket, true);
    Packet250MultipartSegmentAcknowledge retval = new Packet250MultipartSegmentAcknowledge(lostPacket.getPacket250Type(),
            Packet250MultipartSegmentAcknowledge.Acknowledgement.ACKNOWLEDGE_ALL, lostPacket.getUniqueMultipartID(), null);
    return retval;
  }
  /**
   * get the packet for the given segment number
   * @param segmentNumber from 0 up to segmentCount - 1 inclusive
   * @return the packet, or null for failure
   */
  protected Packet250MultipartSegment getSegmentByNumber(int segmentNumber)
  {
    Packet250MultipartSegment retval = null;
    try {
      if (segmentNumber < 0 || segmentNumber >= segmentCount) throw new IOException("Invalid segment number:" + segmentNumber);
      int startBuffPos = segmentNumber * segmentSize;
      int segmentLength = Math.min(segmentSize, rawData.length - startBuffPos);
      assert (segmentLength <= Short.MAX_VALUE);
      assert (segmentNumber <= Short.MAX_VALUE);
      assert (segmentSize <= Short.MAX_VALUE);

      byte [] segmentToSend = Arrays.copyOfRange(rawData, startBuffPos, startBuffPos + segmentLength);
      retval = new Packet250MultipartSegment(packet250Type, false, uniquePacketID, (short)segmentNumber, (short)segmentSize, rawData.length, segmentToSend);
    } catch (IOException ioe) {
      ErrorLog.defaultLog().info("Failed to getAcknowledgementPacket, due to exception " + ioe.toString());
      return null;
    } catch (IllegalArgumentException iae) {
      ErrorLog.defaultLog().info("Failed to getAcknowledgementPacket, due to exception " + iae.toString());
      return null;
    }
    return retval;
  }

  /** Generate a MultipartPacket to be used for sending data
   *  It is initialised to have no data; the sub class must add the rawData later using setRawData
   * @param i_whichSideAmIOn is this packet being created on the server side or the client side?
   * @param i_packet250Type The custom payload ID to use when sending packets
   * @param i_segmentSize The segment size to be used
   */
  protected MultipartPacket(Packet250Types i_packet250Type, Side i_whichSideAmIOn, int i_segmentSize)
  {
    iAmASender = true;
    whichSideAmIOn = i_whichSideAmIOn;

    packet250Type = i_packet250Type;
    if (i_segmentSize <= 0 || i_segmentSize > MAX_SEGMENT_SIZE) throw new IllegalArgumentException("segmentSize " + i_segmentSize + " is out of range [0 -  " + MAX_SEGMENT_SIZE + "]");
    segmentSize = i_segmentSize;
    uniquePacketID = (nextUniquePacketID << 1) + (whichSideAmIOn == Side.SERVER ? 1 : 0);
    nextUniquePacketID++;

    segmentCount = 0;
    rawData = null;
    segmentsNotAcknowledged = null;
    segmentsNotReceivedYet = null;
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
  protected MultipartPacket(Packet250MultipartSegment packet) throws IOException
  {
    if (!packet.isPacketIsValid()) throw new IOException("Invalid Packet250MultipartSegment");
    if (packet.isAbortTransmission()) throw new IOException("Tried to create a new Multipart packet from an abort packet");

    packet250Type = packet.getPacket250Type();
    uniquePacketID = packet.getUniqueMultipartID();
    segmentSize = packet.getSegmentSize();
    int fullMultipartLength = packet.getFullMultipartLength();

    if (segmentSize <= 0 || segmentSize > MAX_SEGMENT_SIZE) throw new IOException("Packet segment size " + segmentSize + " out of allowable range [1 - " + MAX_SEGMENT_SIZE + "]");

    int checkSegmentCount = (fullMultipartLength + segmentSize - 1) / segmentSize;
    if (checkSegmentCount <= 0 || checkSegmentCount > MAX_SEGMENT_COUNT) throw new IOException("Segment count " + checkSegmentCount + " out of allowable range [1 - " + MAX_SEGMENT_COUNT + "]");

    setRawData(new byte[fullMultipartLength]);
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
      segmentsNotReceivedYet = new BitSet(segmentCount);
      segmentsNotReceivedYet.set(0, segmentCount);
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

  /** abort this transmission
   */
  protected void processAbort()
  {
    aborted = true;
    assert checkInvariants();
  }

  /** fully acknowledge this packet
   * @throws IOException
   */
  protected void processAcknowledgeAll(Packet250MultipartSegmentAcknowledge packet)
  {
    if (!iAmASender) {
      ErrorLog.defaultLog().info("received acknowledgement packet on receiver side");
    }
    if (segmentsNotAcknowledged.isEmpty()) return;
    segmentsNotAcknowledged.clear();
    nextUnsentSegment = segmentCount;
    acknowledgementsReceivedFlag = true;
    nextUnacknowledgedSegment = segmentCount;
    assert checkInvariants();
  }

  /** incorporate the data for this segment into the packet
   * @throws IOException
   */
  protected void processSegmentData(Packet250MultipartSegment segmentPacket) throws IOException
  {
    short segmentNumber = segmentPacket.getSegmentNumber();
    short pktSegmentSize = segmentPacket.getSegmentSize();
    int   rawdataLength = segmentPacket.getFullMultipartLength();
    if (segmentNumber < 0 || segmentNumber >= segmentCount) throw new IOException("Packet segment number " + segmentNumber + " outside valid range [0 - " + (segmentCount-1) + "] inclusive");
    if (!segmentsNotReceivedYet.get(segmentNumber)) return;  // duplicate of a segment already received, just ignore it

    if (pktSegmentSize != segmentSize) throw new IOException("Packet segment size " + pktSegmentSize + " does not match expected (" + segmentSize + ")");
    if (rawdataLength != rawData.length) throw new IOException("Packet rawdataLength " + rawdataLength + " does not match expected (" + rawData.length + ")");
    int startBuffPos = segmentNumber * segmentSize;
    int expectedSegmentLength = Math.min(segmentSize, rawData.length - startBuffPos);
    assert (expectedSegmentLength < Short.MAX_VALUE);
    try {
      System.arraycopy(segmentPacket.getRawData(), 0, rawData, startBuffPos, expectedSegmentLength);
    } catch (IndexOutOfBoundsException iobe) {
      ErrorLog.defaultLog().info("Size of segment data read " + expectedSegmentLength + " was too big for the target buffer ");
    }

    segmentsNotReceivedYet.clear(segmentNumber);
    segmentsReceivedFlag = true;
    assert checkInvariants();
  }

  /** integrate the acknowledgement bits into this packet
   * @throws IOException
   */
  protected void processAcknowledgement(Packet250MultipartSegmentAcknowledge acknowledgementPacket)
  {
    if (!iAmASender) {
      ErrorLog.defaultLog().info("received Packet250MultipartSegmentAcknowledge on receiver side");
      return;
    }

    BitSet notAcknowledgedNew = acknowledgementPacket.getSegmentsNotReceivedYet();

    // check to see if we have received acknowledgement for any segments we haven't sent yet!
    //  i.e. if the last zero is greater than the sent segment count
    int lastAcknowledgedPacket = notAcknowledgedNew.previousClearBit(segmentCount - 1);
    if (lastAcknowledgedPacket >= nextUnsentSegment) {
      ErrorLog.defaultLog().info("acknowledged segment was never sent in incoming Packet250MultipartSegmentAcknowledge");
      return;
    }
    BitSet savedNack = (BitSet)segmentsNotAcknowledged.clone();
    segmentsNotAcknowledged.and(notAcknowledgedNew);
    if (!savedNack.equals(segmentsNotAcknowledged)) acknowledgementsReceivedFlag = true;
    assert checkInvariants();
  }


  // derived classes should implement this interface so that others wishing to create a new MultipartPacket (in response to an incoming packet) can pass this object to the packet handler which will invoke it.
  public interface MultipartPacketCreator
  {
    public MultipartPacket createNewPacket(Packet250MultipartSegment packet);
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
      if ((uniquePacketID & 1) != (whichSideAmIOn == Side.SERVER ? 1 : 0 )) return false;
    } else {
      if (segmentsNotReceivedYet == null || segmentsNotReceivedYet.length() > segmentCount) return false;
    }

    return true;
  }

  public static final int MAX_SEGMENT_COUNT = Short.MAX_VALUE;
  public static final int MAX_SEGMENT_SIZE = 30000;
  public static final int NULL_PACKET_ID = -1;

  private Side whichSideAmIOn;

  private Packet250Types packet250Type;
  private int uniquePacketID;
  private int segmentSize;
  private int segmentCount;
  private byte [] rawData;

  private boolean iAmASender;                    // true if this packet is being sent; false if it's being received.
  private BitSet segmentsNotAcknowledged;
  private boolean acknowledgementsReceivedFlag;  // set to true once one or more acknowledgements have been received
  private int nextUnsentSegment = -1;         // the next segment we have never sent.  -1 is dummy value to trigger error if we forget to initialise
  private int nextUnacknowledgedSegment = -1; // the next unacknowledged segment.

  private BitSet segmentsNotReceivedYet;
  private boolean segmentsReceivedFlag;          // set to true once one or more segments have been received

  private boolean aborted = false;

  private static int nextUniquePacketID = 0;      // unique across all packets sent from this server and derived from MultipartPacket
}

