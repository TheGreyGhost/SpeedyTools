package speedytools.common.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import speedytools.serverside.ingametester.InGameTester;

import java.util.HashMap;

/**
 * Used to register class instances for handling incoming packets.
 * This is different to the SimpleNetworkWrapper, which registers classes instead of instances.
 */
public class PacketHandlerRegistry
{
  private SimpleNetworkWrapper simpleNetworkWrapper;
  public static final String CHANNEL_NAME = "speedytools";

  // Forge constructs the packet handler using the default constructor;
  //  set up to use the static registries in this case
  public PacketHandlerRegistry() {
    clientSideHandlers = staticClientSideHandlers;
    serverSideHandlers = staticServerSideHandlers;
    simpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);
  }

  // change this registry to non-static, i.e. to hold its own independent set of handlers
  //   used primarily for testing
  public void changeToNonStatic()
  {
    clientSideHandlers = new HashMap<Byte, PacketHandlerMethod>();
    serverSideHandlers = new HashMap<Byte, PacketHandlerMethod>();
  }

  public class ClientSideMessageHandler implements IMessageHandler<IMessage, IMessage>
  {

  }
}

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

  /**
   * The class used to handle the incoming packet
   * handlePacket returns true if this was a valid packet, false otherwise
   */
  public static interface PacketHandlerMethod {
    public boolean handlePacket(EntityPlayer player, Packet250CustomPayload packet);
  }

  public void sendToServer(IMessage message)
  {
    simpleNetworkWrapper.sendToServer(message);
  }

  public void registerHandlerMethod( Packet250Base packet250Base, Packet250Types packetType, Side side)
  {
    simpleNetworkWrapper.registerMessage(InGameTester.PacketHandlerSpeedyIngameTester.class, Packet250SpeedyIngameTester.class, );
    Class<? extends IMessageHandler<IMessage, IMessage>> messageHandlerClass, Class<IMessage> messageClass
    simpleNetworkWrapper.registerMessage(messageHandlerClass, messageClass, packetType.getPacketTypeID(), side);
//    switch (side) {
//      case CLIENT: {
//        if (clientSideHandlers.containsKey(packetID)) {
//          if (handlerMethod != clientSideHandlers.get(packetID)) {
//            throw new IllegalArgumentException("Duplicate Client-side PacketHandler ID:" + packetID);
//          }
//        } else {
//          clientSideHandlers.put(packetID, handlerMethod);
//        }
//        break;
//      }
//
//      case SERVER: {
//        if (serverSideHandlers.containsKey(packetID)) {
//          if (handlerMethod != serverSideHandlers.get(packetID)) {
//            throw new IllegalArgumentException("Duplicate Server-side PacketHandler ID:" + packetID);
//          }
//        } else {
//          serverSideHandlers.put(packetID, handlerMethod);
//        }
//        break;
//      }
//      default:
//        throw new IllegalArgumentException("Invalid side:" + side);
//    }
  }

  public void clearAll()
  {
    clientSideHandlers.clear();
    serverSideHandlers.clear();
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

  // these handlers are usually set to refer to the static handlers
  private HashMap<Byte, PacketHandlerMethod> clientSideHandlers = new HashMap<Byte, PacketHandlerMethod>();
  private HashMap<Byte, PacketHandlerMethod> serverSideHandlers = new HashMap<Byte, PacketHandlerMethod>();

  private static HashMap<Byte, PacketHandlerMethod> staticClientSideHandlers = new HashMap<Byte, PacketHandlerMethod>();
  private static HashMap<Byte, PacketHandlerMethod> staticServerSideHandlers = new HashMap<Byte, PacketHandlerMethod>();
}

