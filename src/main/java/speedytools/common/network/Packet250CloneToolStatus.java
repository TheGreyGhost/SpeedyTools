package speedytools.common.network;


import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import speedytools.common.utilities.ErrorLog;

import java.io.*;

/**
* This class is used to inform the client and server of each other's status (primarily for cloning)
*/
public class Packet250CloneToolStatus  extends Packet250Base
{
  public static Packet250CloneToolStatus serverStatusChange(ServerStatus newStatus, byte newPercentage, String newNameOfPlayerBeingServiced)
  {
    return new Packet250CloneToolStatus(null, newStatus, newPercentage, newNameOfPlayerBeingServiced);
  }

  public static Packet250CloneToolStatus clientStatusChange(ClientStatus newStatus)
  {
    return new Packet250CloneToolStatus(newStatus, null, (byte)100, "");
  }

  private Packet250CloneToolStatus(ClientStatus newClientStatus, ServerStatus newServerStatus,
                                   byte newPercentage, String newNameOfPlayerBeingServiced
  )
  {
    super();
    clientStatus = newClientStatus;
    serverStatus = newServerStatus;
    completionPercentage = newPercentage;
    nameOfPlayerBeingServiced = newNameOfPlayerBeingServiced;
    packetIsValid = true;
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
  public String getNameOfPlayerBeingServiced() {
    assert (serverStatus != null);
    return nameOfPlayerBeingServiced;
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

  private Packet250CloneToolStatus()
  {
    super();
    packetIsValid = false;
  }

  @Override
  protected void readFromBuffer(ByteBuf buf) {
    packetIsValid = false;
    try {
      clientStatus = ClientStatus.byteToCommand(buf.readByte());
      serverStatus = ServerStatus.byteToCommand(buf.readByte());
      completionPercentage = buf.readByte();
      nameOfPlayerBeingServiced = ByteBufUtils.readUTF8String(buf);
    } catch (IndexOutOfBoundsException ioe) {
      ErrorLog.defaultLog().info("Exception while reading Packet250CloneToolStatus: " + ioe);
      return;
    }
    if (!checkInvariants()) return;
    packetIsValid = true;
  }

  @Override
  protected void writeToBuffer(ByteBuf buf) {
    if (!packetIsValid) return;
    buf.writeByte(clientStatus.getStatusID());
    buf.writeByte(serverStatus.getStatusID());
    buf.writeByte(completionPercentage);
    ByteBufUtils.writeUTF8String(buf, nameOfPlayerBeingServiced == null ? "" : nameOfPlayerBeingServiced);
  }

  /**
   * Register the handler for this packet
   * @param packetHandlerRegistry
   * @param packetHandlerMethod
   * @param side
   */
  public static void registerHandler(PacketHandlerRegistry packetHandlerRegistry,  PacketHandlerMethod packetHandlerMethod, Side side) {
    switch (side) {
      case CLIENT: {
        clientSideHandler = packetHandlerMethod;
        break;
      }
      case SERVER: {
        serverSideHandler = packetHandlerMethod;
        break;
      }
      default: {
        assert false : "Tried to register Packet250CloneToolStatus on side " + side;
      }
    }
    packetHandlerRegistry.getSimpleNetworkWrapper().registerMessage(CommonMessageHandler.class, Packet250CloneToolStatus.class,
            Packet250Types.PACKET250_TOOL_STATUS_ID.getPacketTypeID(), side);
  }

  public interface PacketHandlerMethod
  {
    public boolean handlePacket(Packet250CloneToolStatus packet250CloneToolStatus, MessageContext ctx);
  }

  public static class CommonMessageHandler implements IMessageHandler<Packet250CloneToolStatus, IMessage>
  {
    /**
     * Called when a message is received of the appropriate type. You can optionally return a reply message, or null if no reply
     * is needed.
     *
     * @param message The message
     * @return an optional return message
     */
    public IMessage onMessage(Packet250CloneToolStatus message, MessageContext ctx)
    {
      switch (ctx.side) {
        case CLIENT: {
          if (clientSideHandler == null) {
            ErrorLog.defaultLog().severe("Packet250CloneToolStatus received but not registered on side " + ctx.side);
          } else {
            clientSideHandler.handlePacket(message, ctx);
          }
          break;
        }
        case SERVER: {
          if (serverSideHandler == null) {
            ErrorLog.defaultLog().severe("Packet250CloneToolStatus received but not registered on side " + ctx.side);
          } else {
            serverSideHandler.handlePacket(message, ctx);
          }
          break;
        }
        default: assert false : "Received message on invalid side: " + ctx.side;
      }
      return null;
    }
  }

  private ClientStatus clientStatus;
  private ServerStatus serverStatus;
  private byte completionPercentage = 100;
  private String nameOfPlayerBeingServiced;

  private static PacketHandlerMethod serverSideHandler;
  private static PacketHandlerMethod clientSideHandler;
}
