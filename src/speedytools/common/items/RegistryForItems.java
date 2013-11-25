package speedytools.common.items;

import cpw.mods.fml.common.registry.LanguageRegistry;
import net.minecraft.item.Item;

/**
 * creates and contains the instances of all of this mod's custom Items
 * the instances are created manually in order to control the creation time and order
 * NB - ItemBlocks (Item corresponding to a Block) are created in the RegistryForBlocks
 */
public class RegistryForItems
{
  // custom items
  public static Item itemSpeedyStripStrong;
  public static Item itemSpeedyStripWeak;

  public static void initialise()
  {
    final int START_ITEM = 7235;
    itemSpeedyStripStrong = new ItemSpeedyStripStrong(START_ITEM);
    itemSpeedyStripWeak = new ItemSpeedyStripWeak(START_ITEM+1);

    LanguageRegistry.addName(itemSpeedyStripWeak, "Wand of Benign Conjuration");
    LanguageRegistry.addName(itemSpeedyStripStrong, "Wand of Destructive Conjuration");
  }
}
