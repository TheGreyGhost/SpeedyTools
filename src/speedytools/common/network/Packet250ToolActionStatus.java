package speedytools.common.network;


import cpw.mods.fml.relauncher.Side;
import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.utilities.ErrorLog;

import java.io.*;

/**
 * This class is used to inform the client and server of each other's status (primarily for cloning)
 */
public class Packet250ToolActionStatus
{
  public static Packet250ToolActionStatus serverStatusChange(ServerStatus newStatus)
  {
    return new Packet250ToolActionStatus(null, newStatus, (byte)100);
  }

  public static Packet250ToolActionStatus clientStatusChange(ClientStatus newStatus)
  {
    return new Packet250ToolActionStatus(newStatus, null, (byte)100);
  }

  /**
   *
   * @param newPercentage must be between 0 and 100 inclusive.  100 indicates completely finished.
   */
  public static Packet250ToolActionStatus updateCompletionPercentage(ServerStatus newStatus, byte newPercentage)
  {
    return new Packet250ToolActionStatus(null, newStatus, newPercentage);
  }

  /**
   * get the custom packet for this status packet
   * @return null for failure
   */
  public Packet250CustomPayload getPacket250CustomPayload()
  {
    checkInvariants();
    Packet250CustomPayload retval = null;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(1*4);
      DataOutputStream outputStream = new DataOutputStream(bos);
      outputStream.writeByte(PacketHandler.PACKET250_TOOL_ACTION_STATUS_ID);
      outputStream.writeByte(clientStatusToByte(clientStatus));
      outputStream.writeByte(serverStatusToByte(serverStatus));
      outputStream.writeByte(completionPercentage);
      retval = new Packet250CustomPayload("speedytools",bos.toByteArray());
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Failed to getPacket250CustomPayload, due to exception " + ioe.toString());
      return null;
    }
    return retval;
  }

  /**
   * Creates a Packet250SpeedyToolUse from Packet250CustomPayload
   * @param sourcePacket250 the packet to create it from
   * @return the new packet, or null for failure
   */
  public static Packet250ToolActionStatus createPacket250ToolActionStatus(Packet250CustomPayload sourcePacket250)
  {
    DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(sourcePacket250.data));
    Packet250ToolActionStatus newPacket = new Packet250ToolActionStatus();

    try {
      byte packetID = inputStream.readByte();
      if (packetID != PacketHandler.PACKET250_CLONE_TOOL_USE_ID) return null;

      newPacket.clientStatus = byteToClientStatus(inputStream.readByte());
      newPacket.serverStatus = byteToServerStatus(inputStream.readByte());
      newPacket.completionPercentage = inputStream.readByte();
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Exception while reading Packet250SpeedyToolUse: " + ioe);
      return null;
    }
    if (!newPacket.checkInvariants()) return null;
    return newPacket;
  }

  private Packet250ToolActionStatus(ClientStatus newClientStatus,
                                    ServerStatus newServerStatus,
                                    byte newPercentage)
  {
    clientStatus = newClientStatus;
    serverStatus = newServerStatus;
    completionPercentage = newPercentage;
  }

  private static final byte NULL_BYTE_VALUE = Byte.MAX_VALUE;

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

  public ServerStatus getServerStatus() {
    assert (serverStatus != null);
    return serverStatus;
  }

  public ClientStatus getClientStatus() {
    assert (clientStatus != null);
    return clientStatus;
  }

  public byte getCompletionPercentage() {
    assert (checkInvariants());
    assert (serverStatus != null);
    return completionPercentage;
  }

  private boolean checkInvariants()
  {
    boolean valid;
    valid = (clientStatus == null || serverStatus == null);
    valid = valid & (clientStatus != null || serverStatus != null);
    valid = valid & (serverStatus == ServerStatus.IDLE
                     || (completionPercentage >= 0 && completionPercentage <= 100) );
    return valid;
  }

  private static ServerStatus byteToServerStatus(byte value)
  {
    if (value < 0 || value >= ServerStatus.allValues.length) return null;
    return ServerStatus.allValues[value];
  }

  private static byte serverStatusToByte(ServerStatus value) throws IOException
  {
    byte retval;

    if (value == null) return NULL_BYTE_VALUE;

    for (retval = 0; retval < ServerStatus.allValues.length; ++retval) {
      if (ServerStatus.allValues[retval] == value) return retval;
    }
    throw new IOException("Invalid command value");
  }

  private static ClientStatus byteToClientStatus(byte value)
  {
    if (value < 0 || value >= ServerStatus.allValues.length) return null;
    return ClientStatus.allValues[value];
  }

  private static byte clientStatusToByte(ClientStatus value) throws IOException
  {
    byte retval;

    if (value == null) return NULL_BYTE_VALUE;
    for (retval = 0; retval < ServerStatus.allValues.length; ++retval) {
      if (ClientStatus.allValues[retval] == value) return retval;
    }
    throw new IOException("Invalid command value");
  }

  private Packet250ToolActionStatus()
  {
  }

  private ClientStatus clientStatus;
  private ServerStatus serverStatus;
  private byte completionPercentage = 100;
}
