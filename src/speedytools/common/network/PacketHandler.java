package speedytools.common.network;

import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import speedytools.clientside.ClientSide;
import speedytools.serverside.ServerSide;
import speedytools.serverside.SpeedyToolWorldManipulator;

import java.util.HashMap;

public class PacketHandler implements IPacketHandler
{
  public static final String CHANNEL_NAME = "speedytools";
  public static final byte PACKET250_SPEEDY_TOOL_USE_ID = 0;
  public static final byte PACKET250_CLONE_TOOL_USE_ID = 1;
  public static final byte PACKET250_TOOL_STATUS_ID = 2;
  public static final byte PACKET250_TOOL_ACKNOWLEDGE_ID = 3;


  @Override
  public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player playerEntity)
  {
    if (packet.channel.equals(CHANNEL_NAME)) {
      Side side = (playerEntity instanceof EntityPlayerMP) ? Side.SERVER : Side.CLIENT;
      byte packetID = packet.data[0];
      switch (packetID) {
        case PACKET250_SPEEDY_TOOL_USE_ID: {
          if (side != Side.SERVER) {
            malformedPacketError(side, playerEntity, "PACKET250_SPEEDY_TOOL_USE_ID received on wrong side");
            return;
          }
          Packet250SpeedyToolUse toolUsePacket = Packet250SpeedyToolUse.createPacket250SpeedyToolUse(packet);
          if (toolUsePacket == null) return;
          SpeedyToolWorldManipulator manipulator = ServerSide.getSpeedyToolWorldManipulator();
          manipulator.performServerAction(playerEntity, toolUsePacket.getToolItemID(), toolUsePacket.getButton(),
                                          toolUsePacket.getBlockToPlace(), toolUsePacket.getCurrentlySelectedBlocks());
          break;
        }
        case PACKET250_CLONE_TOOL_USE_ID: {
          Packet250CloneToolUse toolUsePacket = Packet250CloneToolUse.createPacket250CloneToolUse(packet);
          if (toolUsePacket != null && toolUsePacket != null && toolUsePacket.validForSide(side)) {
            if (side == Side.SERVER) {
              ServerSide.getCloneToolsNetworkServer().handlePacket((EntityPlayerMP)playerEntity, toolUsePacket);
            }
          } else {
            malformedPacketError(side, playerEntity, "PACKET250_CLONE_TOOL_USE_ID received on wrong side");
            return;
          }
          break;
        }
        case PACKET250_TOOL_STATUS_ID: {
          Packet250CloneToolStatus toolStatusPacket = Packet250CloneToolStatus.createPacket250ToolStatus(packet);
          if (toolStatusPacket != null && toolStatusPacket.validForSide(side)) {
            if (side == Side.SERVER) {
              ServerSide.getCloneToolsNetworkServer().handlePacket((EntityPlayerMP)playerEntity, toolStatusPacket);
            } else {
              ClientSide.getCloneToolsNetworkClient().handlePacket((EntityClientPlayerMP)playerEntity, toolStatusPacket);
            }
          } else {
            malformedPacketError(side, playerEntity, "PACKET250_TOOL_STATUS_ID received on wrong side");
            return;
          }
          break;
        }
        case PACKET250_TOOL_ACKNOWLEDGE_ID: {
          Packet250CloneToolAcknowledge toolAcknowledgePacket = Packet250CloneToolAcknowledge.createPacket250CloneToolAcknowledge(packet);
          if (toolAcknowledgePacket != null && toolAcknowledgePacket.validForSide(side)) {
            if (side == Side.CLIENT) {
              ClientSide.getCloneToolsNetworkClient().handlePacket((EntityClientPlayerMP)playerEntity, toolAcknowledgePacket);
            }
          } else {
            malformedPacketError(side, playerEntity, "PACKET250_TOOL_ACKNOWLEDGE_ID received on wrong side");
            return;
          }
        }
        default: {
          PacketHandlerMethod handlerMethod;
          if (side == Side.CLIENT) {
            handlerMethod = clientSideHandlers.get(packetID);
          } else {
            handlerMethod = serverSideHandlers.get(packetID);
          }
          if (handlerMethod != null) {
           handlerMethod.handlePacket(packetID, packet);
          } else {
            malformedPacketError(side, playerEntity, "Malformed Packet250SpeedyTools:Invalid packet type ID");
          }
          return;
        }

      }
    }
  }

  public static interface PacketHandlerMethod {
    public void handlePacket(byte packetID, Packet250CustomPayload packet);
  }

  public static void registerHandlerMethod(Side side, byte packetID, PacketHandlerMethod handlerMethod)
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

  private static HashMap<Byte, PacketHandlerMethod> clientSideHandlers = new HashMap<Byte, PacketHandlerMethod>();
  private static HashMap<Byte, PacketHandlerMethod> serverSideHandlers = new HashMap<Byte, PacketHandlerMethod>();
}

