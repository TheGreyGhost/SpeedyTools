package speedytools.common.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import speedytools.SpeedyToolsMod;

import java.util.List;

public class ItemSpeedyWandWeak extends ItemSpeedyTool {
  public static final String NAME = "simplewandweak";
  public ItemSpeedyWandWeak() {
    super(PlacementCountModes.FINITE_ONLY);
    setMaxStackSize(64);
    setUnlocalizedName(NAME);
    setFull3D();                              // setting this flag causes the wand to render vertically in 3rd person view, like a pickaxe
    setTextureName(SpeedyToolsMod.prependModID("wandweakicon"));
  }

  /**
   * allows items to add custom lines of information to the mouseover description
   */
  @Override
  public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List textList, boolean useAdvancedItemTooltips)
  {
    textList.add("Right click: place blocks (won't destroy");
    textList.add("             any blocks in the way)");
    textList.add("Left click: undo last place");
    textList.add("Control: hold down to allow diagonal");
    textList.add("Control + mouse wheel: change count");
  }
}