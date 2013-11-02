package speedytools.client;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import speedytools.SpeedyToolsMod;

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

    /*
    @Override
    public void onUpdate(ItemStack itemStack, World world, Entity entity, int par4, boolean par5)
    {
      if (world.isRemote && entity instanceof EntityPlayerSP) {
        ItemStack heldItem = ((EntityPlayerSP) entity).getHeldItem();
        if (heldItem != null && heldItem.itemID == SpeedyToolsMod.itemSpeedyStripStrong.itemID) {
          SpeedyToolsMod.useItemButtonInterceptor.setInterceptionActive(true);
          SpeedyToolsMod.attackButtonInterceptor.setInterceptionActive(true);
        } else {
          SpeedyToolsMod.useItemButtonInterceptor.setInterceptionActive(false);
          SpeedyToolsMod.attackButtonInterceptor.setInterceptionActive(false);
        }
      }
    }
    */


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
