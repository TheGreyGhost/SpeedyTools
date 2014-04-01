package speedytools.common.network.multipart;

import java.util.HashMap;

/**
 * User: The Grey Ghost
 * Date: 31/03/14
 */
public class MultipartPacketHandler
{
  public MultipartPacketHandler()
  {
    packetCreatorRegistry = new HashMap<Byte, MultipartPacket.MultipartPacketCreator>();
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
      throw new IllegalArgumentException("linkage packetID " + linkage.getPacketID() + " did not match packet packetID "+ packet.getUniqueID())
    }
    PacketTransmissionInfo packetTransmissionInfo = new PacketTransmissionInfo();
    packetTransmissionInfo.packet = packet;
    packetTransmissionInfo.linkage = linkage;
    packetTransmissionInfo.transmissionState = PacketTransmissionInfo.TransmissionState.SENDING_INITIAL_SEGMENTS;
    packetTransmissionInfo.timeOfLastAction = 0;
    packetsBeingSent.put(linkage.getPacketID(), packetTransmissionInfo);
    doTransmission(packetTransmissionInfo);
    return true;
  }

  private void doTransmission(PacketTransmissionInfo packetTransmissionInfo)
  {
    switch (packetTransmissionInfo.transmissionState) {
      case RECEIVING: {
        break;
      }
      case SENDING_INITIAL_SEGMENTS: {
        if (packetTransmissionInfo.timeOfLastAction
        break;
      }
      case SENDER_WAITING_FOR_ACK: {
        break;
      }
      case WAITING_FOR_FIRST_RESEND: {
        break;
      }
      case RESENDING: {
        break;
      }
      default: {
        assert false: "invalid transmission state: " + packetTransmissionInfo.transmissionState;
      }
    }
  }

  public boolean processIncomingPacket()
  {
  //  - called by PacketHandler, which then calls MultipartPacket methods as appropriate
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

  /**
   * This class is used by the MultipartPacketHandler to communicate the packet progress to the sender
   */
  public interface PacketLinkage
  {
    public void progressUpdate();
    public void packetCompleted();
    public void packetAborted();
    public int getPacketID();
  }

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
  private HashMap<Integer, PacketTransmissionInfo> packetsBeingSent;
  private HashMap<Integer, PacketTransmissionInfo> packetsBeingReceived;

}
