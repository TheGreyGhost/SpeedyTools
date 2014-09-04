package speedytools.clientside;


import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
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
public class ClientTickHandler  {

  @SubscribeEvent
  public void clientTickEvent(TickEvent.ClientTickEvent event) {
    if (event.phase == TickEvent.Phase.START) {
      tickStart(event);
    } else if (event.phase == TickEvent.Phase.END) {
      tickEnd(event);
    }
  }

  /**
   * Enable user input interception if the user is holding a speedy tool
   */
  public void tickStart(TickEvent.ClientTickEvent event)
  {
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
   */
  public void tickEnd(TickEvent.ClientTickEvent event)
  {
    ClientSide.tick();
    EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
    if (player == null) return;

    boolean inputUsed = false;                  //todo testing only
    if (ClientSide.activeTool.toolIsActive()) {
      long timeNow = System.nanoTime();
      ClientSide.userInput.updateButtonStates(SpeedyToolControls.attackButtonInterceptor.isKeyDown(), SpeedyToolControls.useItemButtonInterceptor.isKeyDown(), timeNow);
      inputUsed = ClientSide.activeTool.processUserInput(player, 1.0F, ClientSide.userInput);
    }

    ClientSide.activeTool.performTick(player.getEntityWorld());

  }

}
