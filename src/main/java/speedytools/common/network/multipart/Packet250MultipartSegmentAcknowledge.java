package speedytools.common.network.multipart;


import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import speedytools.common.network.Packet250Base;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.PacketHandlerRegistry;
import speedytools.common.utilities.ErrorLog;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
* This class is used by the server to acknowledge a multipart segment from the sender
*/
public class Packet250MultipartSegmentAcknowledge extends Packet250Base
{
  /**
   * The packet returns information to the sender about the progress of the multipacket segments
   */
  public Packet250MultipartSegmentAcknowledge(Packet250Types i_packet250Type, Acknowledgement i_acknowledgement,
                                              int i_uniquePacketID, BitSet i_segmentsNotReceivedYet)
  {
    acknowledgement = i_acknowledgement;
    segmentsNotReceivedYet = i_segmentsNotReceivedYet;
    packet250Type = i_packet250Type;
    uniqueMultipartID = i_uniquePacketID;
    packetIsValid = true;
  }
  public Packet250MultipartSegmentAcknowledge()  // used by Netty to create packet; invalid until populated by packet handler
  {
    // default constructor sets packet invalid, does nothing else
  }


  public Packet250Types getPacket250Type() {
    return packet250Type;
  }

  public int getUniqueID() {
    return uniqueMultipartID;
  }

  public Acknowledgement getAcknowledgement() {
    return acknowledgement;
  }

  public BitSet getSegmentsNotReceivedYet() {
    return segmentsNotReceivedYet;
  }

  @Override
  public void readFromBuffer(ByteBuf buf) {
    try {
      packetIsValid = false;
      packet250Type = Packet250Types.byteToPacket250Type(buf.readByte());
      uniqueMultipartID = buf.readInt();
      acknowledgement = Acknowledgement.byteToAcknowledgement(buf.readByte());

      short ackDataLength = buf.readShort();
      if (ackDataLength < 0) {
        ErrorLog.defaultLog().info("Invalid data length on incoming Packet250MultipartSegmentAcknowledge: " + ackDataLength);
        return;
      }
      byte [] rawBuffer = new byte[ackDataLength];
      if (ackDataLength != 0) {
        buf.readBytes(rawBuffer);
        if (buf.readableBytes() != 0) {
          ErrorLog.defaultLog().info("still had  " + buf.readableBytes() + " left after reading " + ackDataLength + " bytes from Packet250MultipartSegmentAcknowledge");
          return;
        }
      }
      segmentsNotReceivedYet = BitSet.valueOf(rawBuffer);

    } catch (IndexOutOfBoundsException ioe) {
      ErrorLog.defaultLog().info("Exception while reading Packet250MultipartSegmentAcknowledge: " + ioe);
      return;
    }
    if (!checkInvariants()) return;
    packetIsValid = true;
  }

  @Override
  public void writeToBuffer(ByteBuf buf) {
    checkInvariants();
    if (!isPacketIsValid()) return;
    buf.writeByte(packet250Type.getPacketTypeID());
    buf.writeInt(uniqueMultipartID);
    buf.writeByte(acknowledgement.getID());
    if (segmentsNotReceivedYet == null) {
      buf.writeShort(0);
    } else {
      byte [] rawBytes = segmentsNotReceivedYet.toByteArray();
      buf.writeShort(rawBytes.length);
      buf.writeBytes(rawBytes);
    }
  }

  /**
   * checks this class to see if it is internally consistent
   * @return true if ok, false if bad.
   */
  private boolean checkInvariants()
  {
    if (acknowledgement == null) return false;
    if (acknowledgement == Acknowledgement.ABORT && segmentsNotReceivedYet != null) return false;
    return true;
  }

  /**
   * Register the handler for this packet
   * @param packetHandlerRegistry
   * @param packetHandlerMethod
   * @param side
   */
  public static void registerHandler(PacketHandlerRegistry packetHandlerRegistry,  PacketHandlerMethod packetHandlerMethod, Side side, Packet250Types packet250Type) {
    if (packetHandlerMethod == null) {
      ErrorLog.defaultLog().severe("tried to register a null PacketHandlerMethod");
      return;
    }
    switch (side) {
      case CLIENT: {
        clientSideHandlers.put(packet250Type, packetHandlerMethod);
        break;
      }
      case SERVER: {
        serverSideHandlers.put(packet250Type, packetHandlerMethod);
        break;
      }
      default: {
        assert false : "Tried to register Packet250MultipartSegmentAcknowledge on side " + side;
      }
    }
    SimpleNetworkWrapper simpleNetworkWrapper =  packetHandlerRegistry.getSimpleNetworkWrapper();
    if (simpleNetworkWrapper != null) { // might be null for testing
      simpleNetworkWrapper.registerMessage(CommonMessageHandler.class, Packet250MultipartSegmentAcknowledge.class,
              packet250Type.getPacketTypeID(), side);
    }
  }

  public interface PacketHandlerMethod
  {
    public boolean handlePacket(Packet250MultipartSegmentAcknowledge packet250CloneToolAcknowledge, MessageContext ctx);
  }

  public static class CommonMessageHandler implements IMessageHandler<Packet250MultipartSegmentAcknowledge, IMessage>
  {
    /**
     * Called when a message is received of the appropriate type. You can optionally return a reply message, or null if no reply
     * is needed.
     *
     * @param message The message
     * @return an optional return message
     */
    public IMessage onMessage(Packet250MultipartSegmentAcknowledge message, MessageContext ctx)
    {
      Packet250Types packet250Type = message.getPacket250Type();
      PacketHandlerMethod handlerMethod = null;
      switch (ctx.side) {
        case CLIENT: {
          handlerMethod = clientSideHandlers.get(packet250Type);
          break;
        }
        case SERVER: {
          handlerMethod = serverSideHandlers.get(packet250Type);
          break;
        }
        default: assert false : "Received message on invalid side: " + ctx.side;
      }

      if (handlerMethod == null) {
        ErrorLog.defaultLog().severe("Packet250MultipartSegmentAcknowledge for packet type " + packet250Type + " received but not registered on side " + ctx.side);
      } else {
        handlerMethod.handlePacket(message, ctx);
      }

      return null;
    }
  }

  public enum Acknowledgement {
    ACKNOWLEDGEMENT(200), ACKNOWLEDGE_ALL(201), ABORT(202);

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

  private static Map<Packet250Types, PacketHandlerMethod> serverSideHandlers = new HashMap<Packet250Types, PacketHandlerMethod>();
  private static Map<Packet250Types, PacketHandlerMethod> clientSideHandlers = new HashMap<Packet250Types, PacketHandlerMethod>();

  private Packet250Types packet250Type;
  private int uniqueMultipartID;
  private Acknowledgement acknowledgement;
  private BitSet segmentsNotReceivedYet;
}
