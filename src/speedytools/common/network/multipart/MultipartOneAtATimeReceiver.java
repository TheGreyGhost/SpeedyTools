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
 * CLASS IS NOT COMPLETED
 */
public class MultipartOneAtATimeReceiver
{
  public MultipartOneAtATimeReceiver()
  {
    packetCreatorRegistry = new HashMap<Byte, MultipartPacket.MultipartPacketCreator>();
    packetLinkageFactories = new HashMap<Byte, PacketReceiverLinkageFactory>();
    packetsBeingReceived = new HashMap<Byte, PacketTransmissionInfo>();

    newestOldPacketIDs = new HashMap<Byte, Integer>();
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

  private void doAbortPacket(byte packetTypeID)
  {
    PacketTransmissionInfo pti = packetsBeingReceived.get(packetTypeID);
    pti.linkage.packetAborted();
    Packet250CustomPayload abortPacket = pti.packet.getAbortPacket();
    packetSender.sendPacket(abortPacket);
    int packetUniqueID = pti.packet.getUniqueID();
    assert newestOldPacketIDs.get(packetTypeID) <= packetUniqueID;
    newestOldPacketIDs.put(packetTypeID, packetUniqueID);
    packetsBeingReceived.remove(packetTypeID);
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
    Byte packetTypeID = MultipartPacket.readPacketTypeID(packet);
    if (packetUniqueID <= newestOldPacketIDs.get(packetTypeID)) {
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

    if (packetsBeingReceived.containsKey(packetTypeID)) {
      if (packetsBeingReceived.get(packetTypeID).packet.getUniqueID() == packetUniqueID) {
        PacketTransmissionInfo pti = packetsBeingReceived.get(packetTypeID);
        boolean success = doProcessIncoming(pti, packet);
        if (success) pti.linkage.progressUpdate(pti.packet.getPercentComplete());
        return success;
      }
      doAbortPacket(packetTypeID);
    }

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
    packetsBeingReceived.put(newPacket.getPacketTypeID(), packetTransmissionInfo);
    boolean success = doProcessIncoming(packetTransmissionInfo, packet);
    if (success) newLinkage.progressUpdate(newPacket.getPercentComplete());
    return success;

  }

  private boolean doProcessIncoming(PacketTransmissionInfo packetTransmissionInfo, Packet250CustomPayload packet)
  {
    boolean success = packetTransmissionInfo.packet.processIncomingPacket(packet);

    if (   packetTransmissionInfo.packet.hasBeenAborted()
        || packetTransmissionInfo.packet.allSegmentsReceived()
        || packetTransmissionInfo.packet.allSegmentsAcknowledged()) {
      int uniqueID = packetTransmissionInfo.packet.getUniqueID();
      packetsBeingReceived.remove(uniqueID);
      if (packetTransmissionInfo.packet.hasBeenAborted()) {
        abortedPacketIDs.add(uniqueID);
        packetTransmissionInfo.linkage.packetAborted();
      } else {
        completedPacketIDs.add(uniqueID);
        packetTransmissionInfo.linkage.packetCompleted();
      }
    } else {
      packetTransmissionInfo.linkage.progressUpdate(packetTransmissionInfo.packet.getPercentComplete());
    }
    return success;
  }

  private static final long TIMEOUT_AGE_S = 120;           // discard "stale" packets older than this - also abort packets and completed receive packets
  private static final long NS_TO_S = 1000 * 1000 * 1000;
  private final int MAX_ABORTED_PACKET_COUNT = 100;  // retain this many aborted packet IDs
  private final int MAX_COMPLETED_PACKET_COUNT = 100;  // retain this many completed packet IDs

  /**
   * should be called frequently to handle packet maintenance tasks, especially timeouts
   */
  public void onTick()
  {

    while (abortedPacketIDs.size() > MAX_ABORTED_PACKET_COUNT) abortedPacketIDs.pollFirst();
    while (completedPacketIDs.size() > MAX_ABORTED_PACKET_COUNT) completedPacketIDs.pollFirst();


    long timeNow =  System.nanoTime();
    long discardTime = timeNow - TIMEOUT_AGE_S * NS_TO_S;

    // discard incoming packets which haven't been updated for a long time
    Iterator<Map.Entry<Byte, PacketTransmissionInfo>> entries = packetsBeingReceived.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry<Byte, PacketTransmissionInfo> thisEntry = entries.next();
      PacketTransmissionInfo pti = thisEntry.getValue();
      if (pti.timeOfLastAction < discardTime) {
        Packet250CustomPayload packet = pti.packet.getAbortPacket();
        packetSender.sendPacket(packet);
        abortedPacketIDs.add(pti.packet.getUniqueID());
        thisEntry.getValue().linkage.packetAborted();
        entries.remove();
      }
    }
  }

  public void abortPacket(PacketLinkage linkage)
  {
    int packetID = linkage.getPacketID();
    Byte packetTypeID = null;
    for (Map.Entry<Byte, PacketTransmissionInfo> entry : packetsBeingReceived.entrySet()) {
      if (entry.getValue().packet.getUniqueID() == packetID) {
        packetTypeID = entry.getKey();
        break;
      }
    }
    if (packetTypeID == null) return;

    doAbortPacket(packetTypeID);
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
  private HashMap<Byte, PacketTransmissionInfo> packetsBeingReceived;

  private HashMap<Byte, Integer> newestOldPacketIDs;
  private TreeSet<Integer>       abortedPacketIDs;
  private TreeSet<Integer>       completedPacketIDs;
  private PacketSender packetSender;

}
