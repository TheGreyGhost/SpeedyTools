package speedytools.common.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/**
 * User: The Grey Ghost
 * Date: 7/09/2014
 *
 * Packets based on Packet250Base are used to communicate between the client and the server.
 * The Packet contains information being transferred and can be supplied to SimpleNetworkWrapper for transmission.
 * Typical usage is:
 * 1) Register the handler for the given packet by calling the static Packet250ClassName.registerHandler() and supplying a
 *    suitable callback class implements PacketHandlerMethod.
 *    A handler should be registered for each side that will receive this packet.
 * 2) Create the packet using the non-default constructor
 * 3) Send the packet (eg using PacketSender.sendPacket())
 *    The readFromBuffer() and writeToBuffer() methods will be called to convert the data to bytes and back again
 * 4) The registered handler on the receiving side will be called
 * If the packet is invalid, or is received on the wrong side, a message is logged and the packet is ignored.
 */
public abstract class Packet250Base implements IMessage
{
  public Packet250Base()
  {
    packetIsValid = false;
  }

  /**
   * Convert from the supplied buffer into your specific message type
   *
   * @param buf
   */
  public void fromBytes(ByteBuf buf)
  {
    int readableBytesBefore = buf.readableBytes();
    readFromBuffer(buf);
    int readableBytesAfter = buf.readableBytes();
    packetSize = readableBytesBefore - readableBytesAfter;
  }

  /**
   * Deconstruct your message into the supplied byte buffer
   * @param buf
   */
  public void toBytes(ByteBuf buf)
  {
    int readableBytesBefore = buf.readableBytes();
    writeToBuffer(buf);
    int readableBytesAfter = buf.readableBytes();
    packetSize = readableBytesAfter - readableBytesBefore;
  }

  public int getPacketSize() { return packetSize;}

  protected abstract void readFromBuffer(ByteBuf buf);

  protected abstract void writeToBuffer(ByteBuf buf);

  private int packetSize;

  public boolean isPacketIsValid() {
    return packetIsValid;
  }

  protected boolean packetIsValid;

}
