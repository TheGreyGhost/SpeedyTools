package speedytools.serverside;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import speedytools.clientside.ClientSide;
import speedytools.clientside.userinput.SpeedyToolControls;

import java.util.EnumSet;

/**
 * User: The Grey Ghost
 * Date: 2/11/13
 */
public class ServerTickHandler implements ITickHandler {

  public EnumSet<TickType> ticks()
  {
    return EnumSet.of(TickType.SERVER);
  }

  public void tickStart(EnumSet<TickType> type, Object... tickData)
  {
  }

  /**
   *
   * tick any objects which need it
   * @param type
   * @param tickData
   */
  public void tickEnd(EnumSet<TickType> type, Object... tickData)
  {
    ServerSide.getCloneToolsNetworkServer().tick();
    ServerSide.getCloneToolServerActions().tick();
  }


  public String getLabel()
  {
    return "ServerTickHandler";
  }

}
