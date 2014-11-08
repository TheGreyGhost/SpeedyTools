package speedytools.common.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import speedytools.clientside.ClientSide;
import speedytools.common.utilities.ErrorLog;

/**
* User: The Grey Ghost
* Date: 2/11/13
 * Some items use a combination of stack size and damage to determine the mode
*/
public abstract class ItemSpeedyTool extends Item
{
  public ItemSpeedyTool(PlacementCountModes i_validPlacementCountModes) {
    super();
    setCreativeTab(ClientSide.tabSpeedyTools);
    validPlacementCountModes = i_validPlacementCountModes;
    setMaxDamage(-1);                         // not damageable
  }

  public enum PlacementCountModes {FINITE_ONLY, INFINITE_ONLY, BOTH}

  /**
   * Finds the first block in the player's line of sight, including liquids
   * Has to be in here because getMovingObjectPositionFromPlayer is protected.
   * @param world
   * @param entityPlayer
   * @return the corresponding MovingObjectPosition
   */
  public MovingObjectPosition rayTraceLineOfSight(World world, EntityPlayer entityPlayer)
  {
    return this.getMovingObjectPositionFromPlayer(world, entityPlayer, true);
  }

  /**
   * does this tool use the adjacent block in the hotbar?
   * @return
   */
  public boolean usesAdjacentBlockInHotbar() {return true;}

  private final int INFINITE_MODE_DAMAGE = 1;
  private final int FINITE_MODE_DAMAGE = 2;
  private final int INFINITE_MODE_STACKSIZE_CHANGE_TRIGGER = 64;

  /**
   * sets the number of blocks to be placed
   * if invalid, wraps around to a valid number
   * @param itemStack
   * @param newCount the number of blocks; 0 = infinite; if invalid, wraps around to a valid number
   */
  public void setPlacementCount(ItemStack itemStack, int newCount)
  {
    if (itemStack == null) return;
    boolean makeInfinite = false;

    if (validPlacementCountModes == PlacementCountModes.INFINITE_ONLY) {
      makeInfinite = true;
    } else {
      if (newCount == 0 && validPlacementCountModes != PlacementCountModes.FINITE_ONLY) {
        makeInfinite = true;
      } else {
        newCount = ((newCount - 1) % maxStackSize);
        newCount = ((newCount + maxStackSize) % maxStackSize) + 1;    // take care of negative
        makeInfinite = (newCount == INFINITE_MODE_STACKSIZE_CHANGE_TRIGGER) && validPlacementCountModes != PlacementCountModes.FINITE_ONLY;
      }
    }

    if (makeInfinite) {
      assert (validPlacementCountModes != PlacementCountModes.FINITE_ONLY);
      setDamage(itemStack, INFINITE_MODE_DAMAGE);
      itemStack.stackSize = 1;
      return;
    }

    itemStack.stackSize = newCount;
    setDamage(itemStack, FINITE_MODE_DAMAGE);
  }

  /**
   * retrieves the number of blocks to be placed
   * @param itemStack
   * @return 1 - max for finite, or 0 for infinite (see also isInfiniteMode)
   */
  public int getPlacementCount(ItemStack itemStack)
  {
    if (itemStack == null) return 1;
    if (isInfiniteMode(itemStack)) return 0;
    return itemStack.stackSize;
  }

  // revalidate the placement count, in case the user has changed it (eg merged a stack of items)

  public void revalidatePlacementCount(ItemStack itemStack)
  {
    setPlacementCount(itemStack, getPlacementCount(itemStack));
  }

  /**
   * returns true if this tool is in "infinite placement" mode.
   * @param itemStack
   * @return
   */
  public boolean isInfiniteMode(ItemStack itemStack)
  {
    if (itemStack == null) return false;
    switch (validPlacementCountModes) {
      case FINITE_ONLY: {
        return false;
      }
      case INFINITE_ONLY: {
        return true;
      }
      case BOTH: {
        return (itemStack.getItemDamage() == INFINITE_MODE_DAMAGE);
      }
      default: {
        ErrorLog.defaultLog().severe("ItemSpeedyTool invalid validPlacementCountModes:" + validPlacementCountModes);
        return false;
      }
    }
  }

  private final PlacementCountModes validPlacementCountModes;

}
