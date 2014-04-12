package speedytools.common.network.multipart;

import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.network.PacketSender;
import speedytools.common.utilities.ErrorLog;

import java.util.*;

/**
 * User: The Grey Ghost
 * Date: 31/03/14
 * Multipart Packets are used to send large amounts of data, bigger than will fit into a single Packet250CustomPayload.
 * They are designed to be tolerant of network faults such as dropped packets, out-of-order packets, duplicate copies
 *  of the same packet, loss of connection, etc
 *  See MultipartProtocols.txt and MultipartPacket.png
 * The MultipartOneAtATimeReceiver and MultipartOneAtATimeSender are designed to only handle one of each
 * type of multipartpacket at a time (eg Selection, MapData, etc).  If another packet is sent before the first is
 * finished, the first packet is aborted.
 * Usage:
 * (1) Instantiate MultipartOneAtATimeSender
 * (2) setPacketSender with the appropriate PacketSender (wrapper for either a NetClientHandler or NetServerHandler)
 * (3) sendMultipartPacket to send your MultipartPacket, with an appropriate PacketLinkage used to keep your class
 *     informed of transmission progress.
 * (4) When incoming packets arrive, process them with processIncomingPacket
 * (5) Periodically (eg each tick) call onTick() to send next segment, handle timeouts, etc
 * (6) To abort a packet, call abortPacket
 */
public class MultipartOneAtATimeSender
{
  public MultipartOneAtATimeSender()
  {
    packetBeingSent = null;
    previousPacketID = MultipartPacket.NULL_PACKET_ID;
    abortedPackets = new TreeSet<Integer>();
    unacknowledgedAbortPackets = new TreeMap<Integer, Packet250CustomPayload>();
  }

  /**
   * Changes to a new PacketSender
   * @param newPacketSender
   */
  public void setPacketSender(PacketSender newPacketSender)
  {
    packetSender = newPacketSender;
  }

  /**
   * start sending the given packet.
   * Only one packet of any given type can be sent at any one time.  If a second packet of the same type is added, the first is aborted.
   * the packet uniqueID must greater than all previously sent packets
   * @param linkage the linkage that should be used to inform the sender of progress
   * @param packet the packet to be sent.  The uniqueID of the packet must match the unique ID of the linkage!
   * @return true if the packet was successfully added (and hadn't previously been added)
   */
  public boolean sendMultipartPacket(PacketLinkage linkage, MultipartPacket packet)
  {
  //  - start transmission, provide a callback

    if (packet.getUniqueID() <= previousPacketID) {
      throw new IllegalArgumentException("packetID " + packet.getUniqueID() + " was older than a previous saved packetID "+ previousPacketID);
    }

    if (packetBeingSent != null) {
      if (packet.getUniqueID() <= packetBeingSent.packet.getUniqueID()) {
        throw new IllegalArgumentException("packetID " + packet.getUniqueID() + " was older than existing packetID "+ packetBeingSent.packet.getUniqueID());
      }
      doAbortPacket();
    }

    if (linkage.getPacketID() != packet.getUniqueID()) {
      throw new IllegalArgumentException("linkage packetID " + linkage.getPacketID() + " did not match packet packetID "+ packet.getUniqueID());
    }
    if (packet.hasBeenAborted()) return false;

    PacketTransmissionInfo packetTransmissionInfo = new PacketTransmissionInfo();
    packetTransmissionInfo.packet = packet;
    packetTransmissionInfo.linkage = linkage;
    packetTransmissionInfo.transmissionState = PacketTransmissionInfo.TransmissionState.SENDING_INITIAL_SEGMENTS;
    packetTransmissionInfo.timeOfLastAction = 0;
    assert packetBeingSent == null;
    packetBeingSent = packetTransmissionInfo;
    doTransmission(packetTransmissionInfo);
    linkage.progressUpdate(packet.getPercentComplete());
    return true;
  }

  private void doAbortPacket()
  {
    PacketTransmissionInfo pti = packetBeingSent;
    if (pti == null) return;
    pti.linkage.packetAborted();
    Packet250CustomPayload abortPacket = pti.packet.getAbortPacket();
    packetSender.sendPacket(abortPacket);
    int packetUniqueID = pti.packet.getUniqueID();
    assert previousPacketID <= packetUniqueID;
    previousPacketID = packetUniqueID;
    packetBeingSent = null;
    abortedPackets.add(packetUniqueID);
    unacknowledgedAbortPackets.put(packetUniqueID, abortPacket);
  }

  private static final int ACKNOWLEDGEMENT_WAIT_MS = 100;  // minimum ms elapsed between sending a packet and expecting an acknowledgement
  private static final int MS_TO_NS = 1000000;

  /**
   * Transmit the next part of this packet as necessary
   * @param packetTransmissionInfo
   * @return true if something was transmitted, false if no action was performed
   */

  private boolean doTransmission(PacketTransmissionInfo packetTransmissionInfo)
  {
    // see multipartprotocols.txt for more information on the transmission behaviour
    assert !packetTransmissionInfo.packet.hasBeenAborted();   // packet should have been removed from transmission list

    if (!packetSender.readyForAnotherPacket()) return false;

    boolean sentSomethingFlag = false;

    switch (packetTransmissionInfo.transmissionState) {
      case RECEIVING: {
        assert false: "doTransmission called for a packet in RECEIVING state:";
        break;
      }
      case SENDING_INITIAL_SEGMENTS: {
        if (!packetTransmissionInfo.packet.allSegmentsSent()) {
          Packet250CustomPayload nextSegment = packetTransmissionInfo.packet.getNextUnsentSegment();
          if (nextSegment != null) {
            sentSomethingFlag = packetSender.sendPacket(nextSegment);
            packetTransmissionInfo.timeOfLastAction = System.nanoTime();
          }
        }
        if (packetTransmissionInfo.packet.allSegmentsSent()) {
          packetTransmissionInfo.transmissionState = PacketTransmissionInfo.TransmissionState.SENDER_WAITING_FOR_ACK;
        }
        break;
      }
      case SENDER_WAITING_FOR_ACK: {
        assert !packetTransmissionInfo.packet.allSegmentsAcknowledged();       // packet should have been removed from transmission list
        if (System.nanoTime() - packetTransmissionInfo.timeOfLastAction >= ACKNOWLEDGEMENT_WAIT_MS * MS_TO_NS) {  // timeout waiting for ack: send the first unacked segment, then wait
          Packet250CustomPayload nextSegment = packetTransmissionInfo.packet.getNextUnacknowledgedSegment();
          if (nextSegment != null) {
            sentSomethingFlag = packetSender.sendPacket(nextSegment);
            packetTransmissionInfo.timeOfLastAction = System.nanoTime();
            packetTransmissionInfo.transmissionState = PacketTransmissionInfo.TransmissionState.WAITING_FOR_FIRST_RESEND;
            packetTransmissionInfo.packet.resetAcknowledgementsReceivedFlag();
          }
        }
        break;
      }
      case WAITING_FOR_FIRST_RESEND: {
        assert !packetTransmissionInfo.packet.allSegmentsAcknowledged();       // packet should have been removed from transmission list
        if (packetTransmissionInfo.packet.getAcknowledgementsReceivedFlag()) {
          packetTransmissionInfo.transmissionState = PacketTransmissionInfo.TransmissionState.RESENDING;
        } else if (System.nanoTime() - packetTransmissionInfo.timeOfLastAction >= ACKNOWLEDGEMENT_WAIT_MS * MS_TO_NS) {  // timeout waiting for ack: resend the first unacked segment, then wait again
          packetTransmissionInfo.packet.resetToOldestUnacknowledgedSegment();
          Packet250CustomPayload nextSegment = packetTransmissionInfo.packet.getNextUnacknowledgedSegment();
          if (nextSegment != null) {
            sentSomethingFlag = packetSender.sendPacket(nextSegment);
            packetTransmissionInfo.timeOfLastAction = System.nanoTime();
          }
        }
        break;
      }
      case RESENDING: {
        assert !packetTransmissionInfo.packet.allSegmentsAcknowledged();       // packet should have been removed from transmission list
        Packet250CustomPayload nextSegment = packetTransmissionInfo.packet.getNextUnacknowledgedSegment();
        if (nextSegment == null) {
          packetTransmissionInfo.transmissionState = PacketTransmissionInfo.TransmissionState.SENDER_WAITING_FOR_ACK;
        } else {
          sentSomethingFlag = packetSender.sendPacket(nextSegment);
          packetTransmissionInfo.timeOfLastAction = System.nanoTime();
        }
        break;
      }
      default: {
        assert false: "invalid transmission state: " + packetTransmissionInfo.transmissionState;
      }
    }
    return sentSomethingFlag;
  }

  /**
   * processes an incoming packet;
   * informs the appropriate linkage of progress
   * sends an abort packet back if this packet has already been completed
   * @param packet
   * @return true for success, false if packet is invalid or is ignored
   */
  public boolean processIncomingPacket(Packet250CustomPayload packet)
  {
    Integer packetUniqueID = MultipartPacket.readUniqueID(packet);
    if (packetUniqueID == null) return false;

    // If this is an old packet:
    // (1) if we have no record of this packet being aborted by us, ignore it.  Otherwise
    // (2) if the receiver has previously replied with an abort, ignore it.  Otherwise,
    // (3) reply with abort - unless the incoming packet is also an abort packet, i.e. is an acknowledgement of our abort.
    if (packetUniqueID <= previousPacketID) {
      if (!abortedPackets.contains(packetUniqueID)) return false;
      if (!unacknowledgedAbortPackets.containsKey(packetUniqueID)) return false;    // if not in map, already received abort ACK
      Packet250CustomPayload abortPacket = MultipartPacket.getAbortPacketForLostPacket(packet, false);
      if (abortPacket == null) {
        unacknowledgedAbortPackets.remove(packetUniqueID);
      } else {
        packetSender.sendPacket(abortPacket);
      }
      return false;
    }
    PacketTransmissionInfo pti = packetBeingSent;
    if (pti == null) {
      ErrorLog.defaultLog().warning("Incoming packetUniqueID " + packetUniqueID + " was newer than the most recent packet sent " + previousPacketID);
      return false;
    }
    if (packetUniqueID != pti.packet.getUniqueID()) {
      ErrorLog.defaultLog().warning("Incoming packetUniqueID " + packetUniqueID + " was newer than the packet currently being sent " + pti.packet.getUniqueID());
      return false;
    }
    boolean success = doProcessIncoming(pti, packet);
    if (success) pti.linkage.progressUpdate(pti.packet.getPercentComplete());
    return success;
  }

  private boolean doProcessIncoming(PacketTransmissionInfo packetTransmissionInfo, Packet250CustomPayload packet)
  {
    boolean success = packetTransmissionInfo.packet.processIncomingPacket(packet);

    if (   packetTransmissionInfo.packet.hasBeenAborted()
        || packetTransmissionInfo.packet.allSegmentsAcknowledged()) {
      assert previousPacketID < packetTransmissionInfo.packet.getUniqueID();
      previousPacketID = packetTransmissionInfo.packet.getUniqueID();
      if (packetTransmissionInfo.packet.hasBeenAborted()) {
        packetBeingSent.linkage.packetAborted();
        abortedPackets.add(packetTransmissionInfo.packet.getUniqueID());
      } else {
        packetBeingSent.linkage.packetCompleted();
      }
      packetBeingSent = null;
    } else {
      packetTransmissionInfo.linkage.progressUpdate(packetTransmissionInfo.packet.getPercentComplete());
    }
    return success;
  }

  private final int MAX_ABORTED_PACKET_COUNT = 100;  // retain this many aborted packet IDs
  private final long ABORT_RESEND_DELAY_NS = 1000 * 1000 * 1000;  // how often to resend abort packets if no response recvd.
  /**
   * should be called frequently to handle sending of segments within packet, etc
   */
  public void onTick()
  {

    if (packetBeingSent != null) {
      boolean retval = doTransmission(packetBeingSent);
      if (retval) packetBeingSent.linkage.progressUpdate(packetBeingSent.packet.getPercentComplete());
    }

      // any aborts which haven't been acknowledged - send again
    long timeNow = System.nanoTime();
    if (timeNow - timeLastAbortPacketSent > ABORT_RESEND_DELAY_NS) {
      timeLastAbortPacketSent = timeNow;
      for (Map.Entry<Integer, Packet250CustomPayload> entry : unacknowledgedAbortPackets.entrySet()) {
        packetSender.sendPacket(entry.getValue());
      }
    }

    while (abortedPackets.size() > MAX_ABORTED_PACKET_COUNT) abortedPackets.pollFirst();
  }

  // abort the packet associated with this linkage
  public void abortPacket(PacketLinkage linkage)
  {
    if (packetBeingSent == null) return;
    if (packetBeingSent.linkage.getPacketID() != linkage.getPacketID()) return;
    doAbortPacket();
  }

  /**
   * This class is used by the MultipartPacketHandler to communicate the packet transmission progress to the sender
   */
  public interface PacketLinkage
  {
    public void progressUpdate(int percentComplete);
    public void packetCompleted();
    public void packetAborted();
    public int getPacketID();
  }

  private static class PacketTransmissionInfo {
    public MultipartPacket packet;
    public PacketLinkage linkage;
    public long timeOfLastAction;
    public TransmissionState transmissionState;
    public enum TransmissionState {RECEIVING, SENDING_INITIAL_SEGMENTS, SENDER_WAITING_FOR_ACK, WAITING_FOR_FIRST_RESEND, RESENDING};
  }

  private PacketTransmissionInfo packetBeingSent;
  private int previousPacketID;
  private TreeSet<Integer> abortedPackets;
  private TreeMap<Integer, Packet250CustomPayload> unacknowledgedAbortPackets;
  private long timeLastAbortPacketSent = -1;

  private PacketSender packetSender;

}
