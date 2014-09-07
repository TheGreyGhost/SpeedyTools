package speedytools.common.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/**
 * User: The Grey Ghost
 * Date: 7/09/2014
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
