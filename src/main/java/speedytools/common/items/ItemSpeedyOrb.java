package speedytools.common.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ItemSpeedyOrb extends ItemSpeedyTool {
  public static final String NAME = "simpleorb";
  public ItemSpeedyOrb() {
    super(PlacementCountModes.BOTH);
    setMaxStackSize(64);
    setUnlocalizedName(NAME);
    setFull3D();                              // setting this flag causes the trowel to render vertically in 3rd person view, like a pickaxe
  }

  @Override
  public boolean showDurabilityBar(ItemStack stack) {
    return false;
  }

  /**
   * allows items to add custom lines of information to the mouseover description
   */
  @Override
  public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List textList, boolean useAdvancedItemTooltips)
  {
    textList.add("Right click: place blocks");
    textList.add("Left click: undo last place");
    textList.add("Control: hold down to allow diagonal");
    textList.add("Control + mouse wheel: change count");
  }
}