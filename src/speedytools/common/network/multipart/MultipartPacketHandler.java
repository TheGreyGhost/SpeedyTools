package speedytools.common.network.multipart;

import net.minecraft.network.packet.Packet250CustomPayload;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import speedytools.common.network.PacketSender;
import speedytools.common.utilities.ErrorLog;

import java.util.*;

/**
 * User: The Grey Ghost
 * Date: 31/03/14
 */
public class MultipartPacketHandler
{
  public MultipartPacketHandler()
  {
    packetCreatorRegistry = new HashMap<Byte, MultipartPacket.MultipartPacketCreator>();
    packetLinkageFactories = new HashMap<Byte, PacketReceiverLinkageFactory>();
    packetsToIgnoreByID = new HashMap<Integer, Long>();
    packetsToIgnoreByTime = new PriorityQueue<Pair<Long, Integer>>();
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
   * if the same packet is added multiple times, it is ignored
   * @param linkage the linkage that should be used to inform the sender of progress
   * @param packet the packet to be sent.  The uniqueID of the packet must match the unique ID of the linkage!
   * @return true if the packet was successfully added (and hadn't previously been added)
   */
  public boolean sendMultipartPacket(PacketLinkage linkage, MultipartPacket packet)
  {
  //  - start transmission, provide a callback
    if (packetsBeingSent.containsKey(packet.getUniqueID()) || packetsBeingReceived.containsKey(packet.getUniqueID())) return false;
    if (linkage.getPacketID() != packet.getUniqueID()) {
      throw new IllegalArgumentException("linkage packetID " + linkage.getPacketID() + " did not match packet packetID "+ packet.getUniqueID());
    }
    if (packet.hasBeenAborted()) return false;

    PacketTransmissionInfo packetTransmissionInfo = new PacketTransmissionInfo();
    packetTransmissionInfo.packet = packet;
    packetTransmissionInfo.linkage = linkage;
    packetTransmissionInfo.transmissionState = PacketTransmissionInfo.TransmissionState.SENDING_INITIAL_SEGMENTS;
    packetTransmissionInfo.timeOfLastAction = 0;
    packetsBeingSent.put(linkage.getPacketID(), packetTransmissionInfo);
    doTransmission(packetTransmissionInfo);
    linkage.progressUpdate(packet.getPercentComplete());
    return true;
  }

  private final int ACKNOWLEDGEMENT_WAIT_MS = 100;  // minimum ms elapsed between sending a packet and expecting an acknowledgement

  private void doTransmission(PacketTransmissionInfo packetTransmissionInfo)
  {
    // see multipartprotocols.txt for more information on the transmission behaviour
    assert !packetTransmissionInfo.packet.hasBeenAborted();   // packet should have been removed from transmission list

    switch (packetTransmissionInfo.transmissionState) {
      case RECEIVING: {
        assert false: "doTransmission called for a packet in RECEIVING state:";
        break;
      }
      case SENDING_INITIAL_SEGMENTS: {
        if (!packetTransmissionInfo.packet.allSegmentsSent()) {
          Packet250CustomPayload nextSegment = packetTransmissionInfo.packet.getNextUnsentSegment();
          if (nextSegment != null) {
            packetSender.sendPacket(nextSegment);
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
        if (System.nanoTime() - packetTransmissionInfo.timeOfLastAction >= ACKNOWLEDGEMENT_WAIT_MS * 1000000) {  // timeout waiting for ack: send the first unacked segment, then wait
          Packet250CustomPayload nextSegment = packetTransmissionInfo.packet.getNextUnacknowledgedSegment();
          if (nextSegment != null) {
            packetSender.sendPacket(nextSegment);
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
        } else if (System.nanoTime() - packetTransmissionInfo.timeOfLastAction >= ACKNOWLEDGEMENT_WAIT_MS * 1000000) {  // timeout waiting for ack: resend the first unacked segment, then wait again
          packetTransmissionInfo.packet.resetToOldestUnacknowledgedSegment();
          Packet250CustomPayload nextSegment = packetTransmissionInfo.packet.getNextUnacknowledgedSegment();
          if (nextSegment != null) {
            packetSender.sendPacket(nextSegment);
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
          packetSender.sendPacket(nextSegment);
          packetTransmissionInfo.timeOfLastAction = System.nanoTime();
          packetTransmissionInfo.packet.resetToOldestUnacknowledgedSegment();
        }
        break;
      }
      default: {
        assert false: "invalid transmission state: " + packetTransmissionInfo.transmissionState;
      }
    }
  }

  public boolean processIncomingPacket(Packet250CustomPayload packet)
  {
    Integer packetUniqueID = MultipartPacket.readUniqueID(packet);
    if (packetUniqueID == null) return false;

    if (packetsToIgnoreByID.containsKey(packetUniqueID)) return false;
    if (packetsBeingSent.containsKey(packetUniqueID)) {
      PacketTransmissionInfo pti = packetsBeingSent.get(packetUniqueID);
      boolean success = doProcessIncoming(packetsBeingSent, pti,  packet);
      if (success) pti.linkage.progressUpdate(pti.packet.getPercentComplete());
      return success;

    } else if (packetsBeingReceived.containsKey(packetUniqueID)) {
      PacketTransmissionInfo pti = packetsBeingReceived.get(packetUniqueID);
      boolean success = doProcessIncoming(packetsBeingReceived, pti, packet);
      if (success) pti.linkage.progressUpdate(pti.packet.getPercentComplete());
      return success;

    } else {  // new packet for receiving
      if (!packetCreatorRegistry.containsKey(packet.data[0])) {
        ErrorLog.defaultLog().warning("Received packet type " + packet.data[0] + " but no corresponding PacketCreator in registry");
        return false;
      }

      if (!packetLinkageFactories.containsKey(packet.data[0])) {
        ErrorLog.defaultLog().warning("Received packet type " + packet.data[0] + " but no corresponding LinkageFactory in registry");
        return false;
      }

      MultipartPacket newPacket = packetCreatorRegistry.get(packet.data[0]).createNewPacket(packet);
      PacketLinkage newLinkage = packetLinkageFactories.get(packet.data[0]).createNewLinkage();
      if (newPacket == null || newLinkage == null) return false;

      PacketTransmissionInfo packetTransmissionInfo = new PacketTransmissionInfo();
      packetTransmissionInfo.packet = newPacket;
      packetTransmissionInfo.linkage = newLinkage;
      packetTransmissionInfo.transmissionState = PacketTransmissionInfo.TransmissionState.RECEIVING;
      packetTransmissionInfo.timeOfLastAction = System.nanoTime();
      packetsBeingReceived.put(newLinkage.getPacketID(), packetTransmissionInfo);
      boolean success = doProcessIncoming(packetsBeingReceived, packetTransmissionInfo, packet);
      if (success) newLinkage.progressUpdate(newPacket.getPercentComplete());
      return success;
    }

  }

  private boolean doProcessIncoming(HashMap<Integer, PacketTransmissionInfo> packetListing, PacketTransmissionInfo packetTransmissionInfo, Packet250CustomPayload packet)
  {
    boolean success = packetTransmissionInfo.packet.processIncomingPacket(packet);

    if (   packetTransmissionInfo.packet.hasBeenAborted()
        || packetTransmissionInfo.packet.allSegmentsReceived()
        || packetTransmissionInfo.packet.allSegmentsAcknowledged()) {
      int uniqueID = packetTransmissionInfo.packet.getUniqueID();
      packetListing.remove(uniqueID);
      long timenow = System.nanoTime();
      packetsToIgnoreByID.put(uniqueID, timenow);
      packetsToIgnoreByTime.add(new ImmutablePair<Long, Integer>(timenow, uniqueID));
      if (packetTransmissionInfo.packet.hasBeenAborted()) {
        packetTransmissionInfo.linkage.packetAborted();
      } else {
        packetTransmissionInfo.linkage.packetCompleted();
      }
    } else {
      packetTransmissionInfo.linkage.progressUpdate(packetTransmissionInfo.packet.getPercentComplete());
    }
    return success;
  }

  public void onTick()
  {
  //onTick - sends new segments; checks for timeouts, resends segments if necessary

  }

  public void abortPacket(PacketLinkage linkage)
  {

  }

  public void registerMultipartPacketType(byte packet250CustomPayloadID, MultipartPacket.MultipartPacketCreator packetCreator)
  {
    if (packetCreatorRegistry.containsKey(packet250CustomPayloadID)) throw new IllegalArgumentException("Duplicate packet id:" + packet250CustomPayloadID);
    packetCreatorRegistry.put(packet250CustomPayloadID, packetCreator);
  }

  public void registerLinkageFactory(byte packet250CustomPayloadID, PacketReceiverLinkageFactory linkageFactory)
  {
    if (packetLinkageFactories.containsKey(packet250CustomPayloadID)) throw new IllegalArgumentException("Duplicate packet id:" + packet250CustomPayloadID);
    packetLinkageFactories.put(packet250CustomPayloadID, linkageFactory);
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

  /**
   * The Factory creates a new linkage, which will be used to communicate the packet receiving progress to the recipient
   */
  public interface PacketReceiverLinkageFactory
  {
    public PacketLinkage createNewLinkage();
  }

  private static class PacketTransmissionInfo {
    public MultipartPacket packet;
    public PacketLinkage linkage;
    public long timeOfLastAction;
    public TransmissionState transmissionState;
    public enum TransmissionState {RECEIVING, SENDING_INITIAL_SEGMENTS, SENDER_WAITING_FOR_ACK, WAITING_FOR_FIRST_RESEND, RESENDING};
  }

  private HashMap<Byte, MultipartPacket.MultipartPacketCreator> packetCreatorRegistry;
  private HashMap<Byte, PacketReceiverLinkageFactory> packetLinkageFactories;
  private HashMap<Integer, PacketTransmissionInfo> packetsBeingSent;
  private HashMap<Integer, PacketTransmissionInfo> packetsBeingReceived;

  private HashMap<Integer, Long> packetsToIgnoreByID;
  private PriorityQueue<Pair<Long, Integer>> packetsToIgnoreByTime;
  private PacketSender packetSender;

}
