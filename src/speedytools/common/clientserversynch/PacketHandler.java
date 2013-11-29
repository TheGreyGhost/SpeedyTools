package speedytools.common.clientserversynch;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import speedytools.common.items.ItemSpeedyTool;
import speedytools.serveronly.SpeedyToolWorldManipulator;

public class PacketHandler implements IPacketHandler
{
  public static final byte PACKET250_SPEEDY_TOOL_USE_ID = 0;

  @Override
  public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player playerEntity)
  {
    if (packet.channel.equals("SpeedyTools")) {
      Side side = FMLCommonHandler.instance().getEffectiveSide();

      switch (packet.data[0]) {
        case PACKET250_SPEEDY_TOOL_USE_ID: {
          Packet250SpeedyToolUse toolUsePacket = new Packet250SpeedyToolUse(packet);
          SpeedyToolWorldManipulator.performServerAction(playerEntity, toolUsePacket.getToolItemID(), toolUsePacket.getButton(),
                                                         toolUsePacket.getBlockToPlace(), toolUsePacket.getCurrentlySelectedBlocks());

          break;
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

