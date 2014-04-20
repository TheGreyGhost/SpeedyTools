package speedytools.common.items;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;

/**
 * creates and contains the instances of all of this mod's custom Items
 * the instances are created manually in order to control the creation time and order
 * NB - ItemBlocks (Item corresponding to a Block) are created in the RegistryForBlocks
 */
public class RegistryForItems
{
  // custom items
  public static ItemSpeedyTool itemSpeedyWandStrong;
  public static ItemSpeedyTool itemSpeedyWandWeak;
  public static ItemSpeedyTool itemSpeedySceptre;
  public static ItemSpeedyTool itemSpeedyOrb;
  public static ItemCloneBoundary itemCloneBoundary;
  public static ItemCloneTool itemCloneCopy;

  public static void initialise()
  {
    final int START_ITEM = 7235;
    itemSpeedyWandStrong = new ItemSpeedyWandStrong(START_ITEM);
    itemSpeedyWandWeak = new ItemSpeedyWandWeak(START_ITEM+1);
    itemSpeedySceptre = new ItemSpeedySceptre(START_ITEM+2);
    itemSpeedyOrb = new ItemSpeedyOrb(START_ITEM+3);
    itemCloneBoundary = new ItemCloneBoundary(START_ITEM+4);
    itemCloneCopy = new ItemCloneCopy(START_ITEM+5);

    GameRegistry.registerItem(itemSpeedyWandStrong, itemSpeedyWandStrong.getUnlocalizedName());
    GameRegistry.registerItem(itemSpeedyWandWeak, itemSpeedyWandWeak.getUnlocalizedName());
    GameRegistry.registerItem(itemSpeedySceptre, itemSpeedySceptre.getUnlocalizedName());
    GameRegistry.registerItem(itemSpeedyOrb, itemSpeedyOrb.getUnlocalizedName());
    GameRegistry.registerItem(itemCloneBoundary, itemCloneBoundary.getUnlocalizedName());
    GameRegistry.registerItem(itemCloneCopy, itemCloneCopy.getUnlocalizedName());

    LanguageRegistry.addName(itemSpeedyWandWeak, "Wand of Non-destructive Linear Conjuration");
    LanguageRegistry.addName(itemSpeedyWandStrong, "Wand of Destructive Linear Conjuration");
    LanguageRegistry.addName(itemSpeedySceptre, "Enchanted Sceptre of Contour Extrusion");
    LanguageRegistry.addName(itemSpeedyOrb, "Orb of Transmutation");
    LanguageRegistry.addName(itemCloneBoundary, "Sorcerous Claw of Boundary Creation");
    LanguageRegistry.addName(itemCloneCopy, "Staff of Duplication");
  }
}
