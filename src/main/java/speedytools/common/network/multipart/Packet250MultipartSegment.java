package speedytools.common.network.multipart;


import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import speedytools.common.network.Packet250Base;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.PacketHandlerRegistry;
import speedytools.common.utilities.ErrorLog;

import java.util.HashMap;
import java.util.Map;

/**
* This class is used to inform the server when the user has used a SpeedyTool, and pass it information about the affected blocks.
*/
public class Packet250MultipartSegment extends Packet250Base
{
  /** Used to transmit one of the multipacket segments to the receiver (or abort transmission)
   */
  public Packet250MultipartSegment(Packet250Types i_packet250Type, boolean i_abortTransmission,
                                   int i_uniqueMultipartID,
                                   short i_segmentNumber, short i_segmentSize, int i_fullMultipartLength, byte [] i_rawData)
  {
    packet250Type = i_packet250Type;
    abortTransmission = i_abortTransmission;
    uniqueMultipartID = i_uniqueMultipartID;
    segmentNumber = i_segmentNumber;
    segmentSize = i_segmentSize;
    fullMultipartLength = i_fullMultipartLength;
    rawData = i_rawData;
    packetIsValid = true;
  }

  public Packet250Types getPacket250Type() {
    return packet250Type;
  }

  public boolean isAbortTransmission() {
    return abortTransmission;
  }

  public short getSegmentNumber() {
    return segmentNumber;
  }

  public short getSegmentSize() {
    return segmentSize;
  }

  public byte[] getRawData() {
    return rawData;
  }

  public int getUniqueMultipartID() {
    return uniqueMultipartID;
  }

  public int getFullMultipartLength() {
    return fullMultipartLength;
  }

  @Override
  public void readFromBuffer(ByteBuf buf) {
    packetIsValid = false;
    try {
      packet250Type = Packet250Types.byteToPacket250Type(buf.readByte());
      abortTransmission = buf.readBoolean();
      segmentNumber = buf.readShort();
      segmentSize = buf.readShort();
      fullMultipartLength = buf.readInt();
      int rawDataLength = buf.readShort();
      rawData = new byte[rawDataLength];
      if (rawDataLength != 0) {
        buf.readBytes(rawData);
        if (buf.readableBytes() != 0) {
          ErrorLog.defaultLog().info("still had " + buf.readableBytes() + " left after reading " + rawDataLength + " bytes from Packet250MultipartSegment");
          return;
        }
      }
    } catch (ArrayIndexOutOfBoundsException ioe) {
      ErrorLog.defaultLog().info("Exception while reading Packet250MultipartSegment: " + ioe);
      return;
    }
    if (!checkInvariants()) return;
    packetIsValid = true;
  }

  @Override
  public void writeToBuffer(ByteBuf buf) {
    if (!isPacketIsValid() || !checkInvariants()) return;
    buf.writeByte(packet250Type.getPacketTypeID());
    buf.writeBoolean(abortTransmission);
    buf.writeShort(segmentNumber);
    buf.writeShort(segmentSize);
    buf.writeInt(fullMultipartLength);
    buf.writeShort(rawData.length);
    buf.writeBytes(rawData);
  }

  /**
   * Register a handler for this packet
   * @param packetHandlerRegistry
   * @param packetHandlerMethod
   * @param side
   */
  public static void registerHandler(PacketHandlerRegistry packetHandlerRegistry,  PacketHandlerMethod packetHandlerMethod, Side side, Packet250Types packet250Type) {
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
        assert false : "Tried to register Packet250MultipartSegment on side " + side;
      }
    }
    packetHandlerRegistry.getSimpleNetworkWrapper().registerMessage(CommonMessageHandler.class, Packet250MultipartSegment.class,
            packet250Type.getPacketTypeID(), side);
  }

  public interface PacketHandlerMethod
  {
    public boolean handlePacket(Packet250MultipartSegment packet250MultipartSegment, MessageContext ctx);
  }

  public static class CommonMessageHandler implements IMessageHandler<Packet250MultipartSegment, IMessage>
  {
    /**
     * Called when a message is received of the appropriate type. You can optionally return a reply message, or null if no reply
     * is needed.
     *
     * @param message The message
     * @return an optional return message
     */
    public IMessage onMessage(Packet250MultipartSegment message, MessageContext ctx)
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
        ErrorLog.defaultLog().severe("Packet250MultipartSegment for packet type " + packet250Type + " received but not registered on side " + ctx.side);
      } else {
        handlerMethod.handlePacket(message, ctx);
      }

      return null;
    }
  }

  /**
   * Checks if the packet is internally consistent
   * @return true for success, false otherwise
   */
  private boolean checkInvariants()
  {
    if (packet250Type == null) return false;
    if (!abortTransmission) {
      if (segmentSize < 0) return false;
      if (rawData.length > segmentSize) return false;
    }
    return true;
  }

  private Packet250Types packet250Type;
  private int uniqueMultipartID;
  protected boolean abortTransmission;            // these fields are protected to allow for testing by MultipartPacketTest
  protected short segmentNumber;
  protected short segmentSize;
  protected int fullMultipartLength;
  protected byte [] rawData;

  private static Map<Packet250Types, PacketHandlerMethod> serverSideHandlers = new HashMap<Packet250Types, PacketHandlerMethod>();
  private static Map<Packet250Types, PacketHandlerMethod> clientSideHandlers = new HashMap<Packet250Types, PacketHandlerMethod>();
}
