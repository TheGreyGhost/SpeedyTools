package speedytools.clientonly;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;
import speedytools.SpeedyToolsMod;
import speedytools.common.items.ItemSpeedyTool;

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
      SpeedyToolControls.enableInterception(heldItem != null && ItemSpeedyTool.isAspeedyTool(heldItem.itemID));
    }
  }

  public void tickEnd(EnumSet<TickType> type, Object... tickData)
  {
    if (SpeedyToolControls.attackButtonInterceptor.retrieveClick()) {
      ItemSpeedyTool.attackButtonClicked();
    }

    if (SpeedyToolControls.useItemButtonInterceptor.retrieveClick()) {
      ItemSpeedyTool.useButtonClicked();
    }

  }

  public String getLabel()
  {
    return "ClientTickHandler";
  }

}
