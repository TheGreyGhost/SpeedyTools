package speedytools.clientonly.eventhandlers;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import speedytools.clientonly.SpeedyToolControls;
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
      boolean speedyToolHeld = heldItem != null && ItemSpeedyTool.isAspeedyTool(heldItem.itemID);
      SpeedyToolControls.enableClickInterception(speedyToolHeld);

      boolean controlKeyDown =  Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
      SpeedyToolControls.enableMouseWheelInterception(speedyToolHeld && controlKeyDown);
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

    if (SpeedyToolControls.mouseWheelInterceptor != null) {
      ItemSpeedyTool.mouseWheelMoved(SpeedyToolControls.mouseWheelInterceptor.retrieveLastMouseWheelDelta());
    }

  }

  public String getLabel()
  {
    return "ClientTickHandler";
  }

}
