package speedytools.clientonly;

import net.minecraft.entity.player.InventoryPlayer;

/**
 * The purpose of InventoryPlayerInterceptor is to intercept mouse wheel movements in game (i.e. which normally scroll the currently-selected item in the hotbar)
 * It does this by replacing InventoryPlayer in Minecraft.thePlayer.inventory to override changeCurrentItem
 * When interception is on, the mouse wheel delta is saved and is not passed to the vanilla code.  The mouse wheel delta can later be retrieved using retrieveLastMouseWheelDelta
 * When interception is off, the vanilla changeCurrentItem is called.
 *
 *   Usage:
 *    (1) replace Minecraft.thePlayer.inventory with a newly generated interceptor
 *        eg
 *        InventoryPlayer inventoryPlayer = entityClientPlayerMP.inventory;
 *        if (!(inventoryPlayer instanceof InventoryPlayerInterceptor)) {
 *          mouseWheelInterceptor = new InventoryPlayerInterceptor(inventoryPlayer);
 *          Minecraft.getMinecraft().thePlayer.inventory = mouseWheelInterceptor;
 *        }
 *    (2) Set the interception mode (eg true = on)
 *        eg   mouseWheelInterceptor.setInterceptionActive(interception);
 *    (3) read the underlying delta using retrieveLastMouseWheelDelta
 *
 */
public class InventoryPlayerInterceptor extends InventoryPlayer
{
  public InventoryPlayerInterceptor(InventoryPlayer inventoryPlayer)
  {
    super(inventoryPlayer.player);

    mainInventory = inventoryPlayer.mainInventory;
    armorInventory = inventoryPlayer.armorInventory;
    currentItem = inventoryPlayer.currentItem;
    inventoryChanged = inventoryPlayer.inventoryChanged;
    this.setItemStack(inventoryPlayer.getItemStack());

    // don't need to worry about currentItemStack because it's only used within setCurrentItem, after it has been set at the start of setCurrentItem
  }

  @Override
  public void changeCurrentItem(int delta) {
    if (!interceptionActive) {
      super.changeCurrentItem(delta);
    } else {
      lastMouseWheelDelta += delta;
    }
  }

  public void setInterceptionActive(boolean newMode)
  {
    interceptionActive = newMode;
  }

  /**
   * retrieve the cumulative captured mouse wheel delta since the last call to retrieveLastMouseWheelDelta
   * @return cumulative delta - see Mouse.getDWheel()
   */
  public int retrieveLastMouseWheelDelta() {
    int retval = lastMouseWheelDelta;
    lastMouseWheelDelta = 0;
    return retval;
  }

  private boolean interceptionActive = false;
  private int lastMouseWheelDelta = 0;

}
