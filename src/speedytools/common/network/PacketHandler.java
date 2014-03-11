package speedytools.common.network;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import speedytools.clientside.ClientSide;
import speedytools.serverside.ServerSide;
import speedytools.serverside.SpeedyToolWorldManipulator;

public class PacketHandler implements IPacketHandler
{
  public static final byte PACKET250_SPEEDY_TOOL_USE_ID = 0;
  public static final byte PACKET250_CLONE_TOOL_USE_ID = 1;
  public static final byte PACKET250_TOOL_STATUS_ID = 2;
  public static final byte PACKET250_TOOL_ACKNOWLEDGE_ID = 3;

  @Override
  public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player playerEntity)
  {
    if (packet.channel.equals("speedytools")) {
      Side side = FMLCommonHandler.instance().getEffectiveSide();

      switch (packet.data[0]) {
        case PACKET250_SPEEDY_TOOL_USE_ID: {
          if (side != Side.SERVER) {
            malformedPacketError(playerEntity, "PACKET250_SPEEDY_TOOL_USE_ID received on wrong side");
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
          if (toolUsePacket != null && toolUsePacket.validForSide(side)) {
            if (side == Side.SERVER) {
              ServerSide.getCloneToolsNetworkServer().handlePacket((EntityPlayerMP)playerEntity, toolUsePacket);
            }
          } else {
            malformedPacketError(playerEntity, "PACKET250_CLONE_TOOL_USE_ID received on wrong side");
            return;
          }
          break;
        }
        case PACKET250_TOOL_STATUS_ID: {
          Packet250CloneToolStatus toolStatusPacket = Packet250CloneToolStatus.createPacket250ToolStatus(packet);
          if (toolStatusPacket.validForSide(side)) {
            if (side == Side.SERVER) {
              ServerSide.getCloneToolsNetworkServer().handlePacket((EntityPlayerMP)playerEntity, toolStatusPacket);
            } else {
              ClientSide.getCloneToolsNetworkClient().handlePacket((EntityPlayerMP)playerEntity, toolStatusPacket);
            }
          } else {
            malformedPacketError(playerEntity, "PACKET250_TOOL_STATUS_ID received on wrong side");
            return;
          }
          break;
        }
        case PACKET250_TOOL_ACKNOWLEDGE_ID: {
          Packet250CloneToolAcknowledge toolAcknowledgePacket = Packet250CloneToolAcknowledge.createPacket250CloneToolUse(packet);
          if (toolAcknowledgePacket.validForSide(side)) {
            if (side == Side.CLIENT) {
              ClientSide.getCloneToolsNetworkClient().handlePacket((EntityPlayerMP)playerEntity, toolAcknowledgePacket);
            }
          } else {
            malformedPacketError(playerEntity, "PACKET250_TOOL_ACKNOWLEDGE_ID received on wrong side");
            return;
          }
        }
        default: {
          malformedPacketError(playerEntity, "Malformed Packet250SpeedyTools:Invalid packet type ID");
          return;
        }

      }
    }
  }

  private void malformedPacketError(Player player, String message) {
    Side side = FMLCommonHandler.instance().getEffectiveSide();
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
}

