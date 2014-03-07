package speedytools.common.network;


import cpw.mods.fml.relauncher.Side;
import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.CloneToolActionStatus;
import speedytools.common.utilities.ErrorLog;

import java.io.*;

/**
 * This class is used to inform the client and server of each other's status (primarily for cloning)
 */
public class Packet250ToolActionStatus
{
  public static Packet250ToolActionStatus serverStatusChange(CloneToolActionStatus.ServerStatus newStatus)
  {
    return new Packet250ToolActionStatus(null, newStatus, 100);
  }

  public static Packet250ToolActionStatus clientStatusChange(CloneToolActionStatus.ClientStatus newStatus)
  {
    return new Packet250ToolActionStatus(newStatus, null, 100);
  }

  /**
   *
   * @param newPercentage must be between 0 and 100 inclusive.  100 indicates completely finished.
   */
  public static Packet250ToolActionStatus updateCompletionPercentage(CloneToolActionStatus.ServerStatus newStatus, byte newPercentage)
  {
    return new Packet250ToolActionStatus(null, newStatus, newPercentage);
  }


  public Packet250CustomPayload getPacket250CustomPayload()
  {
    checkInvariants();
    Packet250CustomPayload retval = null;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(1*1 + 1*4  + 4*4);
      DataOutputStream outputStream = new DataOutputStream(bos);
      outputStream.writeByte(PacketHandler.PACKET250_CLONE_TOOL_USE_ID);
      outputStream.writeByte(commandID);
      outputStream.writeInt(toolID);
      outputStream.writeInt(xpos);
      outputStream.writeInt(ypos);
      outputStream.writeInt(zpos);
      outputStream.writeByte(completionPercentage);
      outputStream.writeByte(rotationCount);
      outputStream.writeBoolean(flipped);
      retval = new Packet250CustomPayload("speedytools",bos.toByteArray());

    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Failed to getPacket250CustomPayload, due to exception " + ioe.toString());
      return null;
    }

    return retval;
  }

  /**
   * Creates a Packet250SpeedyToolUse from Packet250CustomPayload
   * @param sourcePacket250
   */
  public Packet250ToolActionStatus(Packet250CustomPayload sourcePacket250)
  {
    DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(sourcePacket250.data));

    try {
      byte packetID = inputStream.readByte();
      if (packetID != PacketHandler.PACKET250_CLONE_TOOL_USE_ID) return;

      commandID = inputStream.readByte();
      toolID = inputStream.readInt();
      xpos = inputStream.readInt();
      ypos = inputStream.readInt();
      zpos = inputStream.readInt();
      completionPercentage = inputStream.readByte();
      rotationCount = inputStream.readByte();
      flipped = inputStream.readBoolean();
    } catch (IOException e) {
      e.printStackTrace();
    }
    checkInvariants();
  }

  private Packet250ToolActionStatus(CloneToolActionStatus.ClientStatus newClientStatus,
                                    CloneToolActionStatus.ServerStatus newServerStatus,
                                    byte newPercentage)
  {
    clientStatus = newClientStatus;
    serverStatus = newServerStatus;
    completionPercentage = newPercentage;
  }

  public enum

  private static final byte COMMAND_MINIMUM_VALID = 0;
  public static final byte COMMAND_SELECTION_MADE = 0;
  public static final byte COMMAND_READY_FOR_SELECTION = 1;
  public static final byte COMMAND_TOOL_ACTION_PERFORMED = 2;
  public static final byte COMMAND_TOOL_UNDO_PERFORMED = 3;
  public static final byte COMMAND_COMPLETION_STATUS = 4;
  private static final byte COMMAND_MAXIMUM_VALID = 4;

  private static final int INVALID_INT_VALUE = Integer.MAX_VALUE;
  private static final byte INVALID_BYTE_VALUE = Byte.MAX_VALUE;

  /**
   * Is this packet valid to be received and acted on by the given side?
   * @param whichSide
   * @return true if yes
   */
  public boolean validForSide(Side whichSide)
  {
    checkInvariants();
    return (   (clientStatus == null && whichSide == Side.CLIENT)
            || (serverStatus == null & whichSide == Side.SERVER)  );
  }


  public byte getCompletionPercentage() {
    checkInvariants();
    assert(completionPercentage != INVALID_BYTE_VALUE);
    return completionPercentage;
  }


  private void checkInvariants()
  {
    assert (clientStatus == null || serverStatus == null);
    assert (clientStatus != null || serverStatus != null);
  }

  private CloneToolActionStatus.ClientStatus clientStatus;
  private CloneToolActionStatus.ServerStatus serverStatus;
  private byte completionPercentage = 100;
}
