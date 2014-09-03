package speedytools.common.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import speedytools.SpeedyToolsMod;

import java.util.List;

public class ItemSpeedyWandStrong extends ItemSpeedyTool {
  public static final String NAME = "simplewandstrong";
  public ItemSpeedyWandStrong() {
    super();
    setMaxStackSize(64);
    setUnlocalizedName(NAME);
    setFull3D();
    setTextureName(SpeedyToolsMod.prependModID("wandstrongicon"));
  }
  /**
   * allows items to add custom lines of information to the mouseover description
   */
  @Override
  public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List textList, boolean useAdvancedItemTooltips)
  {
    textList.add("Right click: place blocks (destroys");
    textList.add("              any blocks in the way)");
    textList.add("Left click: undo last place");
    textList.add("Control: hold down to allow diagonal");
    textList.add("Control + mouse wheel: change count");
  }
}