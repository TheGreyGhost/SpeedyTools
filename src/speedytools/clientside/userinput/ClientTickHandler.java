package speedytools.clientside.userinput;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;
import speedytools.clientside.ClientSide;
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
      boolean speedyToolHeld = ClientSide.activeTool.setHeldItem(heldItem);
      if (speedyToolHeld) {
        ClientSide.userInput.activate();
        SpeedyToolControls.enableClickInterception(speedyToolHeld);
      } else {
        ClientSide.userInput.deactivate();
        boolean speedyOrCloneToolHeld = heldItem != null
                && (   ItemSpeedyTool.isAspeedyTool(heldItem.itemID)
                || ItemCloneTool.isAcloneTool(heldItem.itemID)   );
        SpeedyToolControls.enableClickInterception(speedyOrCloneToolHeld);
      }
    }
  }

  public void tickEnd(EnumSet<TickType> type, Object... tickData)
  {
    EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
    if (player == null) return;

    ItemStack heldItem = player.getHeldItem();
    boolean speedyToolHeld = heldItem != null && ItemSpeedyTool.isAspeedyTool(heldItem.itemID);
    boolean cloneToolHeld = heldItem != null && ItemCloneTool.isAcloneTool(heldItem.itemID);

    if (ClientSide.activeTool.toolIsActive()) {
      ClientSide.userInput.
      }
    }

    if (SpeedyToolControls.attackButtonInterceptor.retrieveClick()) {
      if (speedyToolHeld) {
        ((ItemSpeedyTool)heldItem.getItem()).attackButtonClicked(player);
      } else if (cloneToolHeld) {
        ((ItemCloneTool)heldItem.getItem()).attackButtonClicked(player);
      }
    }

    if (SpeedyToolControls.useItemButtonInterceptor.retrieveClick()) {
      if (speedyToolHeld) {
        ((ItemSpeedyTool)heldItem.getItem()).useButtonClicked(player);
      } else if (cloneToolHeld) {
        ((ItemCloneTool)heldItem.getItem()).useButtonClicked(player);
      }
    }

    if (cloneToolHeld) {
      ((ItemCloneTool)heldItem.getItem()).tick(player.worldObj, SpeedyToolControls.useItemButtonInterceptor.isKeyDown());
    }
  }


  public String getLabel()
  {
    return "ClientTickHandler";
  }

}
