package speedytools.common.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import speedytools.SpeedyToolsMod;

import java.util.List;

public class ItemSpeedyTester extends ItemSpeedyTool {
  public static final String NAME = "tester";
  public ItemSpeedyTester() {
    super(PlacementCountModes.FINITE_ONLY);
    setMaxStackSize(64);
    setUnlocalizedName(NAME);
    setFull3D();                              // setting this flag causes the sceptre to render vertically in 3rd person view, like a pickaxe
    setTextureName(SpeedyToolsMod.prependModID("testericon"));
  }

  /**
   * allows items to add custom lines of information to the mouseover description
   */
  @Override
  public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List textList, boolean useAdvancedItemTooltips)
  {
    textList.add("Right click: conduct test");
    textList.add("Control + mouse wheel: change test #");
    textList.add("                     : (64 = test all)");
  }
}