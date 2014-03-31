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

  public void abortPacket(PacketSenderLinkage packet)
  {

  }

  public void abortPacket(PacketReceiverLinkage packet)
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
  public interface PacketSenderLinkage
  {
    public void progressUpdate();
    public void packetCompleted();
    public void packetAborted();
  }

  public interface PacketReceiverLinkage
  {
    public void progressUpdate();
    public void packetCompleted();
    public void packetAborted();
  }

  public interface PacketReceiverLinkageFactory
  {
    public PacketReceiverLinkage createNewLinkage();
  }

  private HashMap<Byte, MultipartPacket.MultipartPacketCreator> packetCreatorRegistry;
}
