package speedytools.common.network;


import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import speedytools.common.utilities.ErrorLog;

/**
* This class is used to inform the server to perform an in-game automated test
*/
public class Packet250SpeedyIngameTester extends Packet250Base implements IMessage
{
  /**
   * Packet sent from client to server, to indicate when the user has used the ingame tester tool
   * @param i_whichTest the number of the test to be performed
   * @param i_performTest false for erase results of test/ prepare for next test; true for perform
   */
  public Packet250SpeedyIngameTester(int i_whichTest, boolean i_performTest)
  {
    super();

    whichTest = i_whichTest;
    performTest = i_performTest;
    packetIsValid = true;
  }

//  /**
//   * Creates a Packet250SpeedyToolUse from Packet250CustomPayload
//   * @param sourcePacket250
//   * @return the converted packet, or null if failure
//   */
//  public static Packet250SpeedyIngameTester createPacket250SpeedyIngameTester(Packet250CustomPayload sourcePacket250)
//  {
//    Packet250SpeedyIngameTester newPacket = new Packet250SpeedyIngameTester();
//    newPacket.packet250 = sourcePacket250;
//    DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(sourcePacket250.data));
//
//    try {
//      byte packetID = inputStream.readByte();
//      if (packetID != Packet250Types.PACKET250_INGAME_TESTER.getPacketTypeID()) return null;
//
//      newPacket.whichTest = inputStream.readInt();
//      newPacket.performTest = inputStream.readBoolean();
//    } catch (IOException ioe) {
//      ErrorLog.defaultLog().warning("Exception while reading Packet250SpeedyIngameTester: " + ioe);
//      return null;
//    }
//    if (!newPacket.checkInvariants()) return null;
//    return newPacket;
//  }

   public Packet250SpeedyIngameTester()
  {
    packetIsValid = false;
  }

  /**
   * The Packet handler will register itself
   *
   * @param simpleNetworkWrapper
   * @param side
   */
  @Override
  public void registerHandler(SimpleNetworkWrapper simpleNetworkWrapper, PacketHandlerRegistry.PacketHandlerMethod packetHandlerMethod, Side side) {
    serverSideHandler = packetHandlerMethod;
    simpleNetworkWrapper.registerMessage(ServerMessageHandler.class, Packet250SpeedyIngameTester.class,
                                         Packet250Types.PACKET250_INGAME_TESTER.getPacketTypeID(), side);
  }

  public static class ServerMessageHandler implements IMessageHandler<Packet250SpeedyIngameTester, IMessage> {
    /**
     * Called when a message is received of the appropriate type. You can optionally return a reply message, or null if no reply
     * is needed.
     *
     * @param message The message
     * @return an optional return message
     */
    public IMessage onMessage(Packet250SpeedyIngameTester message, MessageContext ctx)
    {
      if (serverSideHandler == null) {
        ErrorLog.defaultLog().severe("Packet250SpeedyIngameTester received but not registered.");
      } else if (ctx.side != Side.SERVER) {
        ErrorLog.defaultLog().severe("Packet250SpeedyIngameTester received on wrong side");
      } else {
        serverSideHandler.handlePacket(message, ctx);
      }
      return null;
    }
  }

  @Override
  protected void readFromBuffer(ByteBuf buf)
  {
//    DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(buf));

    try {
//      byte packetID = inputStream.readByte();
//      if (packetID != Packet250Types.PACKET250_INGAME_TESTER.getPacketTypeID()) return null;

      whichTest = buf.readInt();
      performTest = buf.readBoolean();
    } catch (IndexOutOfBoundsException ioe) {
      ErrorLog.defaultLog().info("Exception while reading Packet250SpeedyIngameTester: " + ioe);
      return;
    }
    if (!checkInvariants()) return;
    packetIsValid = true;
  }

  @Override
  protected void writeToBuffer(ByteBuf buf)
  {
    if (!isPacketIsValid()) return;
    buf.writeInt(whichTest);
    buf.writeBoolean(performTest);
  }

  /**
   * Checks if the packet is internally consistent
   * @return true for success, false otherwise
   */
  private boolean checkInvariants()
  {
    return true;
  }

  public int getWhichTest() {
    return whichTest;
  }

  private int whichTest;

  public boolean isPerformTest() {
    return performTest;
  }

  private boolean performTest;

//  // Handler and registerHandler are used to convert from the static incoming message to the registered non-static handler
//  public static class Handler implements IMessageHandler<Packet250SpeedyIngameTester, IMessage>
//  {
//    @Override
//    public IMessage onMessage(Packet250SpeedyIngameTester message, MessageContext ctx) {
//      if (!message.isPacketIsValid()) return null;
//      IMessageHandler<Packet250SpeedyIngameTester, IMessage> handler = null;
//      switch (ctx.side) {
//        case CLIENT: {
//          handler = clientSideHandler;
//          break;
//        }
//        case SERVER: {
//          handler = serverSideHandler;
//          break;
//        }
//        default:
//          throw new IllegalArgumentException("Invalid side:" + ctx.side);
//      }
//      if (handler == null) {
//        ErrorLog.defaultLog().severe("Unregistered Packet " + message + " received on side " + ctx.side);
//      } else {
//        handler.onMessage(message, ctx);
//      }
//      return null;
//    }
//  }
//
//  public static void registerHandler(IMessageHandler<Packet250SpeedyIngameTester, IMessage> handler, Side side)
//  {
//    boolean alreadyRegisteredThisSide = false;
//    switch (side) {
//      case CLIENT: {
//        alreadyRegisteredThisSide = (clientSideHandler != null);
//        clientSideHandler = handler;
//        break;
//      }
//      case SERVER: {
//        alreadyRegisteredThisSide = (serverSideHandler != null);
//        serverSideHandler = handler;
//        break;
//      }
//      default:
//        throw new IllegalArgumentException("Invalid side:" + side);
//    }
//    if (!alreadyRegisteredThisSide) {
//
//    }
//  }
//
//  private static IMessageHandler<Packet250SpeedyIngameTester, IMessage> clientSideHandler;
  private static PacketHandlerRegistry.PacketHandlerMethod serverSideHandler;

}
