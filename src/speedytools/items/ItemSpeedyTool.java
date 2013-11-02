package speedytools.items;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import speedytools.SpeedyToolsMod;

/**
 * User: The Grey Ghost
 * Date: 2/11/13
 */
public abstract class ItemSpeedyTool extends Item
{
  public ItemSpeedyTool(int id) {
    super(id);
    setCreativeTab(CreativeTabs.tabTools);
    setMaxDamage(-1);                         // not damageable
  }

  public static boolean isAspeedyTool(int itemID)
  {
    return (   itemID == SpeedyToolsMod.itemSpeedyStripStrong.itemID
            || itemID == SpeedyToolsMod.itemSpeedyStripWeak.itemID    );
  }

  /**
   * if true, this tool does not destroy "solid" blocks such as stone, only "non-solid" blocks such as air, grass, etc
   * @param itemID
   * @return
   */
  public static boolean leavesSolidBlocksIntact(int itemID)
  {
    return (itemID == SpeedyToolsMod.itemSpeedyStripWeak.itemID);
  }
}
