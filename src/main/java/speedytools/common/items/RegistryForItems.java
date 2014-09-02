package speedytools.common.items;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * creates and contains the instances of all of this mod's custom Items
 * the instances are created manually in order to control the creation time and order
 * NB - ItemBlocks (Item corresponding to a Block) are created in the RegistryForBlocks
 */
public class RegistryForItems
{
  // custom items
//  public static ItemSpeedyTool itemSpeedyWandStrong;
//  public static ItemSpeedyTool itemSpeedyWandWeak;
  public static ItemSpeedySceptre itemSpeedySceptre;
  public static ItemSpeedyOrb itemSpeedyOrb;
//  public static ItemSpeedyBoundary itemSpeedyBoundary;
//  public static ItemComplexCopy itemComplexCopy;
//  public static ItemComplexMove itemComplexMove;
//  public static ItemSpeedyTester itemSpeedyTester;

//  public static ItemComplexDelete itemComplexDelete;

  public static void initialise()
  {
    final int START_ITEM = 7235;
//    itemSpeedyWandStrong = new ItemSpeedyWandStrong(START_ITEM);
//    itemSpeedyWandWeak = new ItemSpeedyWandWeak(START_ITEM+1);
//    itemSpeedySceptre = new ItemSpeedySceptre(START_ITEM+2);
    itemSpeedyOrb = new ItemSpeedyOrb();
//    itemSpeedyBoundary = new ItemSpeedyBoundary(START_ITEM+4);
//    itemComplexCopy = new ItemComplexCopy(START_ITEM+5);
//    itemComplexMove = new ItemComplexMove(START_ITEM+7);
//    itemComplexDelete = new ItemComplexDelete(START_ITEM+6);

//    GameRegistry.registerItem(itemSpeedyWandStrong, itemSpeedyWandStrong.getUnlocalizedName());
//    GameRegistry.registerItem(itemSpeedyWandWeak, itemSpeedyWandWeak.getUnlocalizedName());
    GameRegistry.registerItem(itemSpeedySceptre, itemSpeedySceptre.NAME);
    GameRegistry.registerItem(itemSpeedyOrb, itemSpeedyOrb.NAME);
//    GameRegistry.registerItem(itemSpeedyBoundary, itemSpeedyBoundary.getUnlocalizedName());
//    GameRegistry.registerItem(itemComplexCopy, itemComplexCopy.getUnlocalizedName());
//    GameRegistry.registerItem(itemComplexDelete, itemComplexDelete.getUnlocalizedName());
//    GameRegistry.registerItem(itemComplexMove, itemComplexMove.getUnlocalizedName());

//    LanguageRegistry.addName(itemSpeedyWandWeak, "Wand of Non-destructive Linear Conjuration");
//    LanguageRegistry.addName(itemSpeedyWandStrong, "Wand of Destructive Linear Conjuration");
//    LanguageRegistry.addName(itemSpeedyBoundary, "Sorcerous Claw of Boundary Creation");
//    LanguageRegistry.addName(itemComplexCopy, "Staff of Duplication");
//    LanguageRegistry.addName(itemComplexDelete, "Staff of Destruction");
//    LanguageRegistry.addName(itemComplexMove, "Staff of Relocation");

//    if (SpeedyToolsOptions.getTesterToolsEnabled()) {
//      itemSpeedyTester = new ItemSpeedyTester(START_ITEM+8);
//      GameRegistry.registerItem(itemSpeedyTester, itemSpeedyTester.getUnlocalizedName());
//      LanguageRegistry.addName(itemSpeedyTester, "In-game tester");
//    }
  }
}
