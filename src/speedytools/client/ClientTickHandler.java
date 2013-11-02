package speedytools.client;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import speedytools.SpeedyToolsMod;
import speedytools.items.ItemSpeedyTool;

import java.util.EnumSet;

/**
 * User: The Grey Ghost
 * Date: 2/11/13
 */
public class ClientTickHandler implements ITickHandler {

  public EnumSet<TickType> ticks()
  {
    return EnumSet.of(TickType.CLIENT);
  }

  public void tickStart(EnumSet<TickType> type, Object... tickData)
  {
    if (!type.contains(TickType.CLIENT)) return;

    EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
    if (player != null) {
      ItemStack heldItem = player.getHeldItem();
      if (heldItem != null && ItemSpeedyTool.isAspeedyTool(heldItem.itemID)) {
        SpeedyToolsMod.useItemButtonInterceptor.setInterceptionActive(true);
        SpeedyToolsMod.attackButtonInterceptor.setInterceptionActive(true);
      } else {
        SpeedyToolsMod.useItemButtonInterceptor.setInterceptionActive(false);
        SpeedyToolsMod.attackButtonInterceptor.setInterceptionActive(false);
      }
    }
  }

  public void tickEnd(EnumSet<TickType> type, Object... tickData)
  {
    // don't need it yet
  }

  public String getLabel()
  {
    return "ClientTickHandler";
  }

}
