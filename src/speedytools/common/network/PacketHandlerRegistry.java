package speedytools.common.network;

import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import speedytools.clientside.ClientSide;
import speedytools.serverside.ServerSide;
import speedytools.serverside.SpeedyToolWorldManipulator;

import java.util.HashMap;

public class PacketHandlerRegistry implements IPacketHandler
{
  public static final String CHANNEL_NAME = "speedytools";

  // Forge constructs the packet handler using the default constructor;
  //  set up to use the static registries in this case
  public PacketHandlerRegistry() {
    clientSideHandlers = staticClientSideHandlers;
    serverSideHandlers = staticServerSideHandlers;
  }

  // change this registry to non-static, i.e. to hold its own independent set of handlers
  //   used primarily for testing
  public void changeToNonStatic()
  {
    clientSideHandlers = new HashMap<Byte, PacketHandlerMethod>();
    serverSideHandlers = new HashMap<Byte, PacketHandlerMethod>();
  }

  @Override
  public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player playerEntity) {
    if (packet.channel.equals(CHANNEL_NAME)) {
      Side side = (playerEntity instanceof EntityPlayerMP) ? Side.SERVER : Side.CLIENT;
      byte packetID = packet.data[0];
      PacketHandlerMethod handlerMethod;
      if (side == Side.CLIENT) {
        handlerMethod = clientSideHandlers.get(packetID);
      } else {
        handlerMethod = serverSideHandlers.get(packetID);
      }
      if (handlerMethod != null) {
        boolean packetValid;
        packetValid = handlerMethod.handlePacket((EntityPlayer) playerEntity, packet);
        if (!packetValid) {
          malformedPacketError(side, playerEntity, "Invalid packet received (ID == " + packetID + ")");
        }
      } else {
        malformedPacketError(side, playerEntity, "Malformed Packet250SpeedyTools:Invalid packet type ID " + packetID + " on side " + side);
      }
      return;
    }
  }

//      switch (packetID) {
//        case PACKET250_SPEEDY_TOOL_USE_ID: {
//          if (side != Side.SERVER) {
//            malformedPacketError(side, playerEntity, "PACKET250_SPEEDY_TOOL_USE_ID received on wrong side");
//            return;
//          }
//          Packet250SpeedyToolUse toolUsePacket = Packet250SpeedyToolUse.createPacket250SpeedyToolUse(packet);
//          if (toolUsePacket == null) return;
//          SpeedyToolWorldManipulator manipulator = ServerSide.getSpeedyToolWorldManipulator();
//          manipulator.performServerAction(playerEntity, toolUsePacket.getToolItemID(), toolUsePacket.getButton(),
//                                          toolUsePacket.getBlockToPlace(), toolUsePacket.getCurrentlySelectedBlocks());
//          break;
//        }
//        case PACKET250_CLONE_TOOL_USE_ID: {
//          Packet250CloneToolUse toolUsePacket = Packet250CloneToolUse.createPacket250CloneToolUse(packet);
//          if (toolUsePacket != null && toolUsePacket != null && toolUsePacket.validForSide(side)) {
//            if (side == Side.SERVER) {
//              ServerSide.getCloneToolsNetworkServer().handlePacket((EntityPlayerMP)playerEntity, toolUsePacket);
//            }
//          } else {
//            malformedPacketError(side, playerEntity, "PACKET250_CLONE_TOOL_USE_ID received on wrong side");
//            return;
//          }
//          break;
//        }
//        case PACKET250_TOOL_STATUS_ID: {
//          Packet250CloneToolStatus toolStatusPacket = Packet250CloneToolStatus.createPacket250ToolStatus(packet);
//          if (toolStatusPacket != null && toolStatusPacket.validForSide(side)) {
//            if (side == Side.SERVER) {
//              ServerSide.getCloneToolsNetworkServer().handlePacket((EntityPlayerMP)playerEntity, toolStatusPacket);
//            } else {
////              ClientSide.getCloneToolsNetworkClient().handlePacket((EntityClientPlayerMP)playerEntity, toolStatusPacket);
//            }
//          } else {
//            malformedPacketError(side, playerEntity, "PACKET250_TOOL_STATUS_ID received on wrong side");
//            return;
//          }
//          break;
//        }
//        default: {


  /**
   * The class used to handle the incoming packet
   * handlePacket returns true if this was a valid packet, false otherwise
   */
  public static interface PacketHandlerMethod {
      public boolean handlePacket(EntityPlayer player, Packet250CustomPayload packet);
  }

  public void registerHandlerMethod(Side side, byte packetID, PacketHandlerMethod handlerMethod)
  {
    switch (side) {
      case CLIENT: {
        if (clientSideHandlers.containsKey(packetID)) {
          if (handlerMethod != clientSideHandlers.get(packetID)) {
            throw new IllegalArgumentException("Duplicate Client-side PacketHandler ID:" + packetID);
          }
        } else {
          clientSideHandlers.put(packetID, handlerMethod);
        }
        break;
      }

      case SERVER: {
        if (serverSideHandlers.containsKey(packetID)) {
          if (handlerMethod != serverSideHandlers.get(packetID)) {
            throw new IllegalArgumentException("Duplicate Server-side PacketHandler ID:" + packetID);
          }
        } else {
          serverSideHandlers.put(packetID, handlerMethod);
        }
        break;
      }
      default:
        throw new IllegalArgumentException("Invalid side:" + side);
    }
  }

  public void clearAll()
  {
    clientSideHandlers.clear();
    serverSideHandlers.clear();
  }

  private void malformedPacketError(Side side, Player player, String message) {
    switch (side) {
      case CLIENT: {
        Minecraft.getMinecraft().getLogAgent().logWarning(message);
        break;
      }
      case SERVER: {
        MinecraftServer.getServer().getLogAgent().logWarning(message);
        break;
      }
      default:
        assert false: "invalid Side";
    }
  }

  // these handlers are usually set to refer to the static handlers
  private HashMap<Byte, PacketHandlerMethod> clientSideHandlers = new HashMap<Byte, PacketHandlerMethod>();
  private HashMap<Byte, PacketHandlerMethod> serverSideHandlers = new HashMap<Byte, PacketHandlerMethod>();

  private static HashMap<Byte, PacketHandlerMethod> staticClientSideHandlers = new HashMap<Byte, PacketHandlerMethod>();
  private static HashMap<Byte, PacketHandlerMethod> staticServerSideHandlers = new HashMap<Byte, PacketHandlerMethod>();
}

