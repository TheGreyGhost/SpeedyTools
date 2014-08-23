package speedytools.clientside;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;
import speedytools.clientside.ClientSide;
import speedytools.clientside.userinput.SpeedyToolControls;

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

  /**
   * Enable user input interception if the user is holding a speedy tool
   * @param type
   * @param tickData
   */
  public void tickStart(EnumSet<TickType> type, Object... tickData)
  {
    if (!type.contains(TickType.CLIENT)) return;
    EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
    if (player != null) {
      ItemStack heldItem = player.getHeldItem();
      boolean speedyToolHeld = ClientSide.activeTool.setHeldItem(heldItem);
      SpeedyToolControls.enableClickInterception(speedyToolHeld);
      if (speedyToolHeld) {
        ClientSide.userInput.activate();
      } else {
        ClientSide.userInput.deactivate();
      }
    }
  }

  /**
   * process any user input
   * tick any other objects which need it
   * @param type
   * @param tickData
   */
  public void tickEnd(EnumSet<TickType> type, Object... tickData)
  {
    ClientSide.tick();
    EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
    if (player == null) return;

    boolean inputUsed = false;
    if (ClientSide.activeTool.toolIsActive()) {
      long timeNow = System.nanoTime();
      ClientSide.userInput.updateButtonStates(SpeedyToolControls.attackButtonInterceptor.isKeyDown(), SpeedyToolControls.useItemButtonInterceptor.isKeyDown(), timeNow);
      inputUsed = ClientSide.activeTool.processUserInput(player, 1.0F, ClientSide.userInput);
    }

    ClientSide.activeTool.performTick(player.getEntityWorld());

    /*
    ItemStack heldItem = player.getHeldItem();

    boolean speedyToolHeld = heldItem != null && ItemSpeedyTool.isAspeedyTool(heldItem.itemID);
    boolean cloneToolHeld = heldItem != null && ItemCloneTool.isAcloneTool(heldItem.itemID);

    if (!inputUsed) {

      if (SpeedyToolControls.attackButtonInterceptor.retrieveClick()) {
        if (speedyToolHeld) {
          ((ItemSpeedyTool) heldItem.getItem()).attackButtonClicked(player);
        } else if (cloneToolHeld) {
          ((ItemCloneTool) heldItem.getItem()).attackButtonClicked(player);
        }
      }

      if (SpeedyToolControls.useItemButtonInterceptor.retrieveClick()) {
        if (speedyToolHeld) {
          ((ItemSpeedyTool) heldItem.getItem()).useButtonClicked(player);
        } else if (cloneToolHeld) {
          ((ItemCloneTool) heldItem.getItem()).useButtonClicked(player);
        }
      }

      if (cloneToolHeld) {
        ((ItemCloneTool) heldItem.getItem()).tick(player.worldObj, SpeedyToolControls.useItemButtonInterceptor.isKeyDown());
      }
    }
    */
  }


  public String getLabel()
  {
    return "ClientTickHandler";
  }

}
