package speedytools.common.network.multipart;

import cpw.mods.fml.relauncher.Side;
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
  public static SelectionPacket createSenderPacket(String i_channel, Side whichSideAmIOn, byte i_packet250CustomPayloadID, int i_segmentSize)
  {
    return new SelectionPacket(i_channel, whichSideAmIOn, i_packet250CustomPayloadID, i_segmentSize);
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

  protected SelectionPacket(String i_channel, Side whichSideAmIOn, byte i_packet250CustomPayloadID, int i_segmentSize)
  {
    super(i_channel, whichSideAmIOn, i_packet250CustomPayloadID, i_segmentSize);
  }

  // derived classes should implement this interface so that other wishing to create a new MultipartPacket (in response to an incoming packet) can pass this object to the packet handler which will invoke it.
  public static class SelectionPacketCreator implements MultipartPacketCreator
  {
    public MultipartPacket createNewPacket(Packet250CustomPayload packet)
    {
      return createReceiverPacket(packet);
    }
  }
}
