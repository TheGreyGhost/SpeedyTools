package speedytools.common.network.multipart;

import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.utilities.ErrorLog;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * User: The Grey Ghost
 * Date: 28/03/14
 *
 * For usage, see MultipartPacket
 */
public class SelectionPacket extends MultipartPacket
{
  public static SelectionPacket createSenderPacket(String i_channel, byte i_packet250CustomPayloadID, int i_segmentSize)
  {
    return new SelectionPacket(i_channel, i_packet250CustomPayloadID, i_segmentSize);
  }

  public static SelectionPacket createReceiverPacket(Packet250CustomPayload packet)
  {
    SelectionPacket newPacket;
    try {
      newPacket = new SelectionPacket(packet);
      newPacket.processIncomingPacket(packet);

      return newPacket;
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Failed to createReceiverPacket, due to exception " + ioe.toString());
      return null;
    }
  }

  protected SelectionPacket(Packet250CustomPayload packet) throws IOException
  {
    super(packet);
  }

  protected SelectionPacket(String i_channel, byte i_packet250CustomPayloadID, int i_segmentSize)
  {
    super(i_channel, i_packet250CustomPayloadID, i_segmentSize);
  }
}
