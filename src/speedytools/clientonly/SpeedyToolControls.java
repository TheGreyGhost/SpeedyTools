package speedytools.clientonly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import speedytools.SpeedyToolsMod;


/**
 * registry for the various user input interceptors
 */
public class SpeedyToolControls
{
  public static KeyBindingInterceptor attackButtonInterceptor;
  public static KeyBindingInterceptor useItemButtonInterceptor;

  public static void initialiseInterceptors()
  {
    attackButtonInterceptor = new KeyBindingInterceptor(Minecraft.getMinecraft().gameSettings.keyBindAttack);
    Minecraft.getMinecraft().gameSettings.keyBindAttack = attackButtonInterceptor;
    attackButtonInterceptor.setInterceptionActive(false);

    useItemButtonInterceptor = new KeyBindingInterceptor(Minecraft.getMinecraft().gameSettings.keyBindUseItem);
    Minecraft.getMinecraft().gameSettings.keyBindUseItem = useItemButtonInterceptor;
    useItemButtonInterceptor.setInterceptionActive(false);
  }

  public static void enableClickInterception(boolean interception)
  {
    useItemButtonInterceptor.setInterceptionActive(interception);
    attackButtonInterceptor.setInterceptionActive(interception);
  }


}
