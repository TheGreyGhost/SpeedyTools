package speedytools.common.network;


import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import speedytools.common.utilities.ErrorLog;

/**
* This class is used by the server to acknowledge actions from the client
*/
public class Packet250CloneToolAcknowledge extends Packet250Base
{
  /**
   * The packet will contain acknowledgement information for the action, the undo, or both.
   * If one of them is status rejected, an optional reason can be provided.
   * @param i_actionStatus
   * @param i_actionSequenceNumber
   * @param i_undoStatus
   * @param i_undoSequenceNumber
   * @param i_reason If a status is rejected, this human-readable message will be sent to the client
   *                 If not required - leave blank "", not null.
   */
  public Packet250CloneToolAcknowledge(Acknowledgement i_actionStatus, int i_actionSequenceNumber,
                                       Acknowledgement i_undoStatus, int i_undoSequenceNumber,
                                       String i_reason)
  {
    actionAcknowledgement = i_actionStatus;
    actionSequenceNumber = i_actionSequenceNumber;
    undoAcknowledgement = i_undoStatus;
    undoSequenceNumber = i_undoSequenceNumber;
    reason = i_reason;
    packetIsValid = true;
  }


  /**
   * Is this packet valid to be received and acted on by the given side?
   * @param whichSide
   * @return true if yes
   */
  public boolean validForSide(Side whichSide)
  {
    assert(checkInvariants());
    return (whichSide == Side.CLIENT);
  }

  public Acknowledgement getActionAcknowledgement() {
    return actionAcknowledgement;
  }

  public int getActionSequenceNumber() {
    return actionSequenceNumber;
  }

  public Acknowledgement getUndoAcknowledgement() {
    return undoAcknowledgement;
  }

  public int getUndoSequenceNumber() {
    return undoSequenceNumber;
  }

  public String getReason() {
    return reason;
  }

  public Packet250CloneToolAcknowledge() {}; // used by Netty; invalid until populated by the packet handler

  @Override
  public void readFromBuffer(ByteBuf buf) {
    try {
      packetIsValid = false;
      actionAcknowledgement = Acknowledgement.byteToAcknowledgement(buf.readByte());
      actionSequenceNumber = buf.readInt();
      undoAcknowledgement = Acknowledgement.byteToAcknowledgement(buf.readByte());
      undoSequenceNumber = buf.readInt();
      reason = ByteBufUtils.readUTF8String(buf);
    } catch (IndexOutOfBoundsException ioe) {
      ErrorLog.defaultLog().info("Exception while reading Packet250CloneToolAcknowledge: " + ioe);
      return;
    }
    if (!checkInvariants()) return;
    packetIsValid = true;
  }

  @Override
  public void writeToBuffer(ByteBuf buf) {
    checkInvariants();
    if (!isPacketIsValid()) return;
    buf.writeByte(actionAcknowledgement.getID());
    buf.writeInt(actionSequenceNumber);
    buf.writeByte(undoAcknowledgement.getID());
    buf.writeInt(undoSequenceNumber);
    ByteBufUtils.writeUTF8String(buf, reason);

  }

  /**
   * checks this class to see if it is internally consistent
   * @return true if ok, false if bad.
   */
  private boolean checkInvariants()
  {
    if (actionAcknowledgement == null || undoAcknowledgement == null) return false;
    return true;
  }

  public enum Acknowledgement {
    NOUPDATE(100), REJECTED(101), ACCEPTED(102), COMPLETED(104);

    public byte getID() {return rawID;}

    public static Acknowledgement byteToAcknowledgement(byte value)
    {
      for (Acknowledgement acknowledgement : Acknowledgement.values()) {
        if (value == acknowledgement.getID()) return acknowledgement;
      }
      return null;
    }

    private Acknowledgement(int i_rawID) {
      rawID = (byte)i_rawID;
    }
    private final byte rawID;

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
        assert false : "Tried to register Packet250CloneToolAcknowledge on side " + side;
      }
    }
    packetHandlerRegistry.getSimpleNetworkWrapper().registerMessage(CommonMessageHandler.class, Packet250CloneToolAcknowledge.class,
            Packet250Types.PACKET250_TOOL_ACKNOWLEDGE_ID.getPacketTypeID(), side);
  }

  public interface PacketHandlerMethod
  {
    public boolean handlePacket(Packet250CloneToolAcknowledge packet250CloneToolAcknowledge, MessageContext ctx);
  }

  public static class CommonMessageHandler implements IMessageHandler<Packet250CloneToolAcknowledge, IMessage>
  {
    /**
     * Called when a message is received of the appropriate type. You can optionally return a reply message, or null if no reply
     * is needed.
     *
     * @param message The message
     * @return an optional return message
     */
    public IMessage onMessage(Packet250CloneToolAcknowledge message, MessageContext ctx)
    {
      switch (ctx.side) {
        case CLIENT: {
          if (clientSideHandler == null) {
            ErrorLog.defaultLog().severe("Packet250CloneToolAcknowledge received but not registered on side " + ctx.side);
          } else {
            clientSideHandler.handlePacket(message, ctx);
          }
          break;
        }
        case SERVER: {
          if (serverSideHandler == null) {
            ErrorLog.defaultLog().severe("Packet250CloneToolAcknowledge received but not registered on side " + ctx.side);
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

  private static PacketHandlerMethod serverSideHandler;
  private static PacketHandlerMethod clientSideHandler;

  private Acknowledgement actionAcknowledgement;
  private int actionSequenceNumber;
  private Acknowledgement undoAcknowledgement;
  private int undoSequenceNumber;
  private String reason;
}
