package speedytools.common.network.multipart;

import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.utilities.ErrorLog;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * User: The Grey Ghost
 * Date: 28/03/14
 */
public class SelectionPacket extends MultipartPacket
{
  @Override
  protected void createMultipartPacket() {

  }

  public static SelectionPacket createFromPacket(Packet250CustomPayload packet)
  {
    try {
      DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet.data));
      CommonHeaderInfo chi = CommonHeaderInfo.readCommonHeader(inputStream);

      MultipartPacket retval;
      switch (chi.multipacketTypeID) {
        case SelectionPacket.MY_MULTIPART_PACKET_TYPE_ID: {
          retval = new SelectionPacket();
          break;
        }
        default: {

        }
      }
      byte commandValue = inputStream.readByte();
      Command command = byteToCommand(commandValue);
      if (command == null) return null;

      Packet250CloneToolUse newPacket = new Packet250CloneToolUse(command);
      newPacket.toolID = inputStream.readInt();
      newPacket.sequenceNumber = inputStream.readInt();
      newPacket.actionToBeUndoneSequenceNumber = inputStream.readInt();
      newPacket.xpos = inputStream.readInt();
      newPacket.ypos = inputStream.readInt();
      newPacket.zpos = inputStream.readInt();
      newPacket.rotationCount = inputStream.readByte();
      newPacket.flipped = inputStream.readBoolean();
      if (newPacket.checkInvariants()) return newPacket;
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Exception while reading processIncomingPacket: " + ioe);
    }
    return false;

  }


  private SelectionPacket() {}

  /*
  public static class SelectionPacketCreator implements MultipartPacketCreator {
    public MultipartPacket createNew(Packet250CustomPayload packet) {
      return createNewFromPacket(packet);
    }
  }
  */
}
