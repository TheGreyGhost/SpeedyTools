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
  public static final boolean ENABLE_MOUSE_WHEEL = true;
  public static KeyBindingInterceptor attackButtonInterceptor;
  public static KeyBindingInterceptor useItemButtonInterceptor;
  public static InventoryPlayerInterceptor mouseWheelInterceptor;

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

  public static void enableMouseWheelInterception(boolean interception)
  {
    if (!ENABLE_MOUSE_WHEEL) return;
    EntityClientPlayerMP entityClientPlayerMP = Minecraft.getMinecraft().thePlayer;
    if (entityClientPlayerMP != null) {
      InventoryPlayer inventoryPlayer = entityClientPlayerMP.inventory;
      if (!(inventoryPlayer instanceof InventoryPlayerInterceptor)) {
        mouseWheelInterceptor = new InventoryPlayerInterceptor(inventoryPlayer);
        Minecraft.getMinecraft().thePlayer.inventory = mouseWheelInterceptor;
      }

      mouseWheelInterceptor.setInterceptionActive(interception);
    }
  }

}
