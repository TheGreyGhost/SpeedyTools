package speedytools.common.network.multipart;

import net.minecraft.network.packet.Packet250CustomPayload;

/**
 * User: The Grey Ghost
 * Date: 28/03/14
 */
public class SelectionPacket extends MultipartPacket
{
  @Override
  protected void createMultipartPacket() {

  }

  @Override
  public void initialiseFromPacket(Packet250CustomPayload packet) {
    return;
  }

  public static final byte MY_MULTIPART_PACKET_TYPE_ID = 1;

  /*
  public static class SelectionPacketCreator implements MultipartPacketCreator {
    public MultipartPacket createNew(Packet250CustomPayload packet) {
      return createNewFromPacket(packet);
    }
  }
  */
}
