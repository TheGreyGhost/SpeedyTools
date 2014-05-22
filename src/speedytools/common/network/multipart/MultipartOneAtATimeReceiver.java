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
 * (1) Instantiate MultipartOneAtATimeReceiver
 * (2) setPacketSender with the appropriate PacketSender (wrapper for either a NetClientHandler or NetServerHandler)
 * (3) registerPacketCreator to register your PacketCreator.  The PacketCreator takes an incoming Packet250CustomPayload
 *     and turns it into the appropriate MultipartPacket class.
 * (4) registerLinkageFactory to register your LinkageFactory.  The LinkageFactory is used to create an appropriate
 *     PacketLinkage, used to inform the receiving class about the progress of the transmission.
 * (5) When incoming packets arrive, process them with processIncomingPacket
 * (6) Periodically (eg once per second) call onTick() to handle timeouts
 * (7) To abort an incoming packet, call abortPacket
 */
public class MultipartOneAtATimeReceiver
{
  public MultipartOneAtATimeReceiver()
  {
    packetCreatorRegistry = null;
    packetLinkageFactory = null;
    packetBeingReceived = null;

    newestOldPacketID = MultipartPacket.NULL_PACKET_ID;
    abortedPacketIDs = new TreeSet<Integer>();
    completedPacketIDs = new TreeSet<Integer>();
  }

  /**
   * Changes to a new PacketSender
   * @param newPacketSender
   */
  public void setPacketSender(PacketSender newPacketSender)
  {
    packetSender = newPacketSender;
  }

  private void doAbortPacket()
  {
    PacketTransmissionInfo pti = packetBeingReceived;
    pti.linkage.packetAborted();
    Packet250CustomPayload abortPacket = pti.packet.getAbortPacket();
    packetSender.sendPacket(abortPacket);
    int packetUniqueID = pti.packet.getUniqueID();
    assert newestOldPacketID <= packetUniqueID;
    newestOldPacketID = packetUniqueID;
    packetBeingReceived = null;
    abortedPacketIDs.add(packetUniqueID);
  }

  private static final int ACKNOWLEDGEMENT_WAIT_MS = 100;  // minimum ms elapsed between sending a packet and expecting an acknowledgement
  private static final int MS_TO_NS = 1000000;

  /**
   * processes an incoming packet; creates a new MultipartPacket if necessary (calling the LinkageFactory),
   * informs the appropriate linkage of progress
   * @param packet
   * @return true for success, false if packet is invalid or is ignored
   */
  public boolean processIncomingPacket(Packet250CustomPayload packet)
  {
    Integer packetUniqueID = MultipartPacket.readUniqueID(packet);
    if (packetUniqueID == null) return false;

    // If this is an old packet;
    // resend our acknowledgement (either abort, or full acknowledge), or ignore if we don't recognise it
    if (packetUniqueID <= newestOldPacketID) {
      if (abortedPacketIDs.contains(packetUniqueID)) {
        Packet250CustomPayload abortPacket = MultipartPacket.getAbortPacketForLostPacket(packet, true);
        if (abortPacket != null) packetSender.sendPacket(abortPacket);
      } else if (completedPacketIDs.contains(packetUniqueID)) {
        Packet250CustomPayload fullAckPacket = MultipartPacket.getFullAcknowledgePacketForLostPacket(packet);
        if (fullAckPacket != null) packetSender.sendPacket(fullAckPacket);
      }
      return false;
    }

    // if we're already receiving this packet, process it
    // otherwise, create a new one, aborting the packet in progress if there is one

    if (packetBeingReceived != null) {
      if (packetBeingReceived.packet.getUniqueID() == packetUniqueID) {
        PacketTransmissionInfo pti = packetBeingReceived;
        boolean success = doProcessIncoming(pti, packet);
        if (success) pti.linkage.progressUpdate(pti.packet.getPercentComplete());
        return success;
      }
      doAbortPacket();
    }

    if (packetCreatorRegistry == null) {
      ErrorLog.defaultLog().warning("Received packet but no corresponding PacketCreator in registry");
      return false;
    }

    if (packetLinkageFactory == null) {
      ErrorLog.defaultLog().warning("Received packet but no corresponding LinkageFactory in registry");
      return false;
    }

    MultipartPacket newPacket = packetCreatorRegistry.createNewPacket(packet);
    if (newPacket == null) { // something wrong! send abort packet back
      Packet250CustomPayload abortPacket = MultipartPacket.getAbortPacketForLostPacket(packet, true);
      packetSender.sendPacket(abortPacket);
      return false;
    }
    PacketLinkage newLinkage = packetLinkageFactory.createNewLinkage(newPacket);
    if (newLinkage == null) return false;

    PacketTransmissionInfo packetTransmissionInfo = new PacketTransmissionInfo();
    packetTransmissionInfo.packet = newPacket;
    packetTransmissionInfo.linkage = newLinkage;
    packetTransmissionInfo.transmissionState = PacketTransmissionInfo.TransmissionState.RECEIVING;
    packetTransmissionInfo.timeOfLastAction = System.nanoTime();
    assert (packetBeingReceived == null);
    packetBeingReceived = packetTransmissionInfo;
    boolean success = doProcessIncoming(packetTransmissionInfo, packet);
    if (success) newLinkage.progressUpdate(newPacket.getPercentComplete());
    return success;

  }

  private boolean doProcessIncoming(PacketTransmissionInfo packetTransmissionInfo, Packet250CustomPayload packet)
  {
    boolean success = packetTransmissionInfo.packet.processIncomingPacket(packet);

    if (   packetTransmissionInfo.packet.hasBeenAborted()
        || packetTransmissionInfo.packet.allSegmentsReceived()) {
      int uniqueID = packetTransmissionInfo.packet.getUniqueID();
      if (packetTransmissionInfo.packet.hasBeenAborted()) {
        abortedPacketIDs.add(uniqueID);
        packetTransmissionInfo.linkage.packetAborted();
      } else {
        completedPacketIDs.add(uniqueID);
        packetTransmissionInfo.linkage.packetCompleted();
        Packet250CustomPayload ackPacket = packetBeingReceived.packet.getAcknowledgementPacket();
        packetSender.sendPacket(ackPacket);
      }
      packetBeingReceived = null;
    } else {
      packetTransmissionInfo.linkage.progressUpdate(packetTransmissionInfo.packet.getPercentComplete());
      Packet250CustomPayload ackPacket = packetBeingReceived.packet.getAcknowledgementPacket();
      packetSender.sendPacket(ackPacket);
    }
    return success;
  }

  private static final long TIMEOUT_AGE_S = 120;           // discard "stale" packets older than this - also abort packets and completed receive packets
  private static final long NS_TO_S = 1000 * 1000 * 1000;
  private static final int MAX_ABORTED_PACKET_COUNT = 100;  // retain this many aborted packet IDs
  private static final int MAX_COMPLETED_PACKET_COUNT = 100;  // retain this many completed packet IDs
  /**
   * should be called frequently to handle packet maintenance tasks, especially timeouts
   */
  public void onTick()
  {
    while (abortedPacketIDs.size() > MAX_ABORTED_PACKET_COUNT) abortedPacketIDs.pollFirst();
    while (completedPacketIDs.size() > MAX_COMPLETED_PACKET_COUNT) completedPacketIDs.pollFirst();

    long timeNow =  System.nanoTime();
    long discardTime = timeNow - TIMEOUT_AGE_S * NS_TO_S;

    // discard incoming packet if it hasn't been updated for a long time
    PacketTransmissionInfo pti = packetBeingReceived;
    if (pti != null && pti.timeOfLastAction < discardTime) {
      Packet250CustomPayload packet = pti.packet.getAbortPacket();
      packetSender.sendPacket(packet);
      abortedPacketIDs.add(pti.packet.getUniqueID());
      pti.linkage.packetAborted();
      packetBeingReceived = null;
    }
  }

  public void abortPacket(PacketLinkage linkage)
  {
    if (packetBeingReceived == null) return;
    if (packetBeingReceived.linkage.getPacketID() != linkage.getPacketID()) return;
    doAbortPacket();
  }

  public void registerPacketCreator(MultipartPacket.MultipartPacketCreator packetCreator)
  {
    packetCreatorRegistry = packetCreator;
  }

  public void registerLinkageFactory(PacketReceiverLinkageFactory linkageFactory)
  {
    packetLinkageFactory = linkageFactory;
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
    public PacketLinkage createNewLinkage(MultipartPacket linkedPacket);
  }

  private static class PacketTransmissionInfo {
    public MultipartPacket packet;
    public PacketLinkage linkage;
    public long timeOfLastAction;
    public TransmissionState transmissionState;
    public enum TransmissionState {RECEIVING, SENDING_INITIAL_SEGMENTS, SENDER_WAITING_FOR_ACK, WAITING_FOR_FIRST_RESEND, RESENDING};
  }

  private MultipartPacket.MultipartPacketCreator packetCreatorRegistry;
  private PacketReceiverLinkageFactory packetLinkageFactory;
  private PacketTransmissionInfo packetBeingReceived;

  private Integer newestOldPacketID;
  private TreeSet<Integer>       abortedPacketIDs;
  private TreeSet<Integer>       completedPacketIDs;
  private PacketSender packetSender;

}
