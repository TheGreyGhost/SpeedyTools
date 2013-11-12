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
 /*
      if (player.movementInput instanceof ConfusedMovementInput) {

      } else {
        SpeedyToolsMod.confusedMovementInput = new ConfusedMovementInput(player.movementInput);
        player.movementInput = SpeedyToolsMod.confusedMovementInput;
      }
 */
      ItemStack heldItem = player.getHeldItem();
      if (heldItem != null && ItemSpeedyTool.isAspeedyTool(heldItem.itemID)) {
        SpeedyToolsMod.useItemButtonInterceptor.setInterceptionActive(true);
        SpeedyToolsMod.attackButtonInterceptor.setInterceptionActive(true);
 //       SpeedyToolsMod.confusedMovementInput.setConfusion(true);
      } else {
        SpeedyToolsMod.useItemButtonInterceptor.setInterceptionActive(false);
        SpeedyToolsMod.attackButtonInterceptor.setInterceptionActive(false);
 //       SpeedyToolsMod.confusedMovementInput.setConfusion(false);
      }
    }



  }

  public void tickEnd(EnumSet<TickType> type, Object... tickData)
  {
    if (SpeedyToolsMod.attackButtonInterceptor.retrieveClick()) {
//      System.out.println("SpeedyToolsMod.attackButtonInterceptor.retrieveClick()");
      ItemSpeedyTool.attackButtonClicked();
    }

    if (SpeedyToolsMod.useItemButtonInterceptor.retrieveClick()) {
//      System.out.println("SpeedyToolsMod.useItemButtonInterceptor.retrieveClick()");
      ItemSpeedyTool.useButtonClicked();
    }

  }

  public String getLabel()
  {
    return "ClientTickHandler";
  }

}
