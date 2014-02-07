package speedytools.clientonly.eventhandlers;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import speedytools.clientonly.SpeedyToolControls;
import speedytools.common.items.ItemCloneTool;
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
      boolean speedyOrCloneToolHeld = heldItem != null
                               && (   ItemSpeedyTool.isAspeedyTool(heldItem.itemID)
                                   || ItemCloneTool.isAcloneTool(heldItem.itemID)   );
      SpeedyToolControls.enableClickInterception(speedyOrCloneToolHeld);
    }
  }

  public void tickEnd(EnumSet<TickType> type, Object... tickData)
  {
    EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
    if (player == null) return;

    ItemStack heldItem = player.getHeldItem();
    boolean speedyToolHeld = heldItem != null && ItemSpeedyTool.isAspeedyTool(heldItem.itemID);
    boolean cloneToolHeld = heldItem != null && ItemCloneTool.isAcloneTool(heldItem.itemID);

    if (SpeedyToolControls.attackButtonInterceptor.retrieveClick()) {
      if (speedyToolHeld) {
        ItemSpeedyTool.attackButtonClicked(player);
      } else if (cloneToolHeld) {
        ItemCloneTool.attackButtonClicked(player);
      }
    }

    if (SpeedyToolControls.useItemButtonInterceptor.retrieveClick()) {
      if (speedyToolHeld) {
        ItemSpeedyTool.useButtonClicked(player);
      } else if (cloneToolHeld) {
        ItemCloneTool.useButtonClicked(player);
      }
    }

  }

  public String getLabel()
  {
    return "ClientTickHandler";
  }

}
