package speedytools.clientonly;

import net.minecraft.entity.player.InventoryPlayer;

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

    // don't need to worry about currentItemStack because it's only used within setCurrentItem, after it has been set already
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

  public int retrieveLastMouseWheelDelta() {
    int retval = lastMouseWheelDelta;
    lastMouseWheelDelta = 0;
    return retval;
  }

  private boolean interceptionActive = false;
  private int lastMouseWheelDelta = 0;

}
