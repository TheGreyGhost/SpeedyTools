package speedytools.common.items;

import cpw.mods.fml.common.registry.LanguageRegistry;

/**
 * creates and contains the instances of all of this mod's custom Items
 * the instances are created manually in order to control the creation time and order
 * NB - ItemBlocks (Item corresponding to a Block) are created in the RegistryForBlocks
 */
public class RegistryForItems
{
  // custom items
  public static ItemSpeedyTool itemSpeedyStripStrong;
  public static ItemSpeedyTool itemSpeedyStripWeak;
  public static ItemSpeedyTool itemSpeedySceptre;
  public static ItemSpeedyTool itemSpeedyOrb;

  public static void initialise()
  {
    final int START_ITEM = 7235;
    itemSpeedyStripStrong = new ItemSpeedyStripStrong(START_ITEM);
    itemSpeedyStripWeak = new ItemSpeedyStripWeak(START_ITEM+1);
    itemSpeedySceptre = new ItemSpeedySceptre(START_ITEM+2);
    itemSpeedyOrb = new ItemSpeedyOrb(START_ITEM+3);

    LanguageRegistry.addName(itemSpeedyStripWeak, "Wand of Benign Linear Conjuration");
    LanguageRegistry.addName(itemSpeedyStripStrong, "Wand of Destructive Linear Conjuration");
    LanguageRegistry.addName(itemSpeedySceptre, "Enchanted Sceptre of Contour Extrusion");
    LanguageRegistry.addName(itemSpeedyOrb, "Orb of Transmutation");
  }
}
