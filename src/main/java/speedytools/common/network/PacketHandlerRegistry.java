package speedytools.common.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import speedytools.common.utilities.ErrorLog;

import java.util.HashMap;

/**
 * Used to register class instances for handling incoming packets.
 * This is different to the SimpleNetworkWrapper, which registers classes instead of instances.
 * One PacketHandlerRegistry instance per side, however it shares the same static registry information
 */
public abstract class PacketHandlerRegistry
{
  protected static SimpleNetworkWrapper simpleNetworkWrapper;
  public static final String CHANNEL_NAME = "speedytools";

  // Forge constructs the packet handler using the default constructor;
  //  set up to use the static registries in this case
  public PacketHandlerRegistry() {
//    clientSideHandlers = staticClientSideHandlers;
//    serverSideHandlers = staticServerSideHandlers;
    if (simpleNetworkWrapper == null) {
      simpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);
    }
  }

//  // change this registry to non-static, i.e. to hold its own independent set of handlers
//  //   used primarily for testing
//  public void changeToNonStatic()
//  {
//    clientSideHandlers = new HashMap<Packet250Types, PacketHandlerMethod>();
//    serverSideHandlers = new HashMap<Packet250Types, PacketHandlerMethod>();
//  }

//  public static class ClientSideMessageHandler implements IMessageHandler<IMessage, IMessage>
//  {
//    public IMessage onMessage(IMessage message, MessageContext ctx)
//    {
//      return null;
//    }
//  }

//  @Override
//  public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player playerEntity) {
//
////    System.out.print("PacketHandlerRegistry.onPacketData " + ((playerEntity instanceof EntityPlayerMP) ? "Server " : "Client ") + "[" + packet.data[0] + "]");
////    if (packet.data[0] == Packet250Types.PACKET250_SELECTION_PACKET.packetTypeID) {
////      System.out.println(" cmd:" + packet.data[5]);
////    } else {
////      System.out.println();
////    }
//
//    if (packet.channel.equals(CHANNEL_NAME)) {
//      Side side = (playerEntity instanceof EntityPlayerMP) ? Side.SERVER : Side.CLIENT;
//      byte packetID = packet.data[0];
//      PacketHandlerMethod handlerMethod;
//      if (side == Side.CLIENT) {
//        handlerMethod = clientSideHandlers.get(packetID);
//      } else {
//        handlerMethod = serverSideHandlers.get(packetID);
//      }
//      if (handlerMethod != null) {
//        boolean packetValid;
//        packetValid = handlerMethod.handlePacket((EntityPlayer) playerEntity, packet);
//        if (!packetValid) {
//          malformedPacketError(side, playerEntity, "Packet received but was redundant or invalid (ID == " + packetID + ")");
//        }
//      } else {
//        malformedPacketError(side, playerEntity, "Malformed Packet250SpeedyTools:Invalid packet type ID " + packetID + " on side " + side);
//      }
//      return;
//    }
//  }

//  /**
//   * The class used to handle the incoming packet
//   * handlePacket returns true if this was a valid packet, false otherwise
//   */
//  public static interface PacketHandlerMethod {
//    boolean handlePacket(EntityPlayer player, Packet250Base packet);
//  }
//


  public interface PacketHandlerMethod
  {
    public boolean handlePacket(Packet250Base packet250Base, MessageContext ctx);
  }

  /** registers a handler for the given packet
   *
   * @param packetHandlerMethod
   * @param packet250Base
   */
  protected abstract void registerHandlerMethod(PacketHandlerMethod packetHandlerMethod, Packet250Base packet250Base);

  protected <T extends Packet250Base> void registerHandlerMethod(IMessageHandler<T, IMessage> handler, Class<T> packet,  Packet250Types packet250Type, Side side)
  {
    class MessageHandler implements IMessageHandler<T, IMessage> {
      public IMessage onMessage(T message, MessageContext ctx)
      {
        if (!message.isPacketIsValid()) return null;
        IMessageHandler<IMessage, IMessage> handler = null;
        switch (ctx.side) {
          case CLIENT: {
            handler = staticClientSideHandlers.get(this.getClass());
            break;
          }
          case SERVER: {
            handler = staticServerSideHandlers.get(this.getClass());
            break;
          }
          default:
            throw new IllegalArgumentException("Invalid side:" + ctx.side);
        }
        if (handler == null) {
          ErrorLog.defaultLog().severe("Unregistered Packet " + message + " received on side " + ctx.side);
        } else {
          handler.onMessage(message, ctx);
        }
        return null;
      }
    }

    simpleNetworkWrapper.registerMessage(MessageHandler.class, packet, packet250Type.getPacketTypeID(), side);
  }

//
//  class NonstaticMessageHandler<REQ extends IMessage, REPLY extends IMessage> {
//    public REPLY onMessage(REQ message, MessageContext ctx) {
//
//    }
//    private static
//  }
//}

//  public void registerHandlerMethod(PacketHandlerMethod, )
//  private class messageHandler implements IMessageHandler<Packet250Base, IMessage>
//  {
//    @Override
//    public IMessage onMessage(Packet250Base message, MessageContext ctx) {
//      if (!message.isPacketIsValid()) {
//        ErrorLog.defaultLog().info("Invalid message received:" + message);
//        return null;
//      }
//      PacketHandlerMethod handlerMethod;
//      switch (ctx.side) {
//        case CLIENT: {
//          handlerMethod = clientSideHandlers.get(message.);
//          break;
//        }
//        case SERVER: {
//          break;
//        }
//        default:
//          throw new IllegalArgumentException("Invalid side:" + ctx.side);
//      }
//    }
//  }
//
//  public void registerHandlerMethod(PacketHandlerMethod handlerMethod, Class<? extends Packet250Base> packetClass, Packet250Types packetType, Side side)
//  {
//    switch (side) {
//      case CLIENT: {
//        if (clientSideHandlers.containsKey(packetClass)) {
//          if (handlerMethod != clientSideHandlers.get(packetClass)) {
//            throw new IllegalArgumentException("Duplicate Client-side PacketHandler ID:" + packetType);
//          }
//        } else {
//          clientSideHandlers.put(packetClass, handlerMethod);
//        }
//        break;
//      }
//
//      case SERVER: {
//        if (serverSideHandlers.containsKey(packetClass)) {
//          if (handlerMethod != serverSideHandlers.get(packetClass)) {
//            throw new IllegalArgumentException("Duplicate Server-side PacketHandler ID:" + packetType);
//          }
//        } else {
//          serverSideHandlers.put(packetClass, handlerMethod);
//        }
//        break;
//      }
//      default:
//        throw new IllegalArgumentException("Invalid side:" + side);
//    }
//    simpleNetworkWrapper.registerMessage(messageHandler.class, Packet250Base.class, packetType.getPacketTypeID(), side);
//  }

  public void clearAll()
  {
//    clientSideHandlers.clear();
//    serverSideHandlers.clear();
  }

//  private void malformedPacketError(Side side, Player player, String message) {
//    switch (side) {
//      case CLIENT: {
//        Minecraft.getMinecraft().getLogAgent().logWarning(message);
//        break;
//      }
//      case SERVER: {
//        MinecraftServer.getServer().getLogAgent().logWarning(message);
//        break;
//      }
//      default:
//        assert false: "invalid Side";
//    }
//  }

//  // these handlers are usually set to refer to the static handlers
//  private HashMap<Class<? extends Packet250Base>, PacketHandlerMethod> clientSideHandlers = new HashMap<Class<? extends Packet250Base>, PacketHandlerMethod>();
//  private HashMap<Class<? extends Packet250Base>, PacketHandlerMethod> serverSideHandlers = new HashMap<Class<? extends Packet250Base>, PacketHandlerMethod>();
//
  private static HashMap<Class<? extends Packet250Base>, IMessageHandler<IMessage, IMessage>> staticClientSideHandlers = new HashMap<Class<? extends Packet250Base>, IMessageHandler<IMessage, IMessage>>();
  private static HashMap<Class<? extends Packet250Base>, IMessageHandler<IMessage, IMessage>> staticServerSideHandlers = new HashMap<Class<? extends Packet250Base>, IMessageHandler<IMessage, IMessage>>();
}

