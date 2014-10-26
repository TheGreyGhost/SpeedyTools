package speedytools.common.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import speedytools.SpeedyToolsMod;

import java.util.List;

public class ItemComplexOrb extends ItemComplexBase
{
  public static final String NAME = "complexorb";
  public ItemComplexOrb() {
    super();
    setMaxStackSize(1);
    setUnlocalizedName(NAME);
    setFull3D();                              // setting this flag causes the staff to render vertically in 3rd person view, like a pickaxe
    setTextureName(SpeedyToolsMod.prependModID("complexorbicon"));
  }

  /**
   * allows items to add custom lines of information to the mouseover description
   */
  @Override
  public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List textList, boolean useAdvancedItemTooltips)
  {
    textList.add("Right click: select blocks,");
    textList.add("       then: toggle drag on/off");
    textList.add("Left click: deselect");
    textList.add("Right hold: place the copy");
    textList.add("Left hold: undo copy");
    textList.add("Mouse wheel: rotate selection");
    textList.add("CTRL+right: flip selection");
  }

  private boolean renderHorizontal;
}

