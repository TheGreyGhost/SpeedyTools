package speedytools.clientonly;

import net.minecraft.client.Minecraft;
import speedytools.SpeedyToolsMod;

/**
 * Created with IntelliJ IDEA.
 * User: Rick
 * Date: 25/11/13
 * Time: 10:30 PM
 * To change this template use File | Settings | File Templates.
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

  public static void enableInterception(boolean interception)
  {
    useItemButtonInterceptor.setInterceptionActive(interception);
    attackButtonInterceptor.setInterceptionActive(interception);
  }

}
