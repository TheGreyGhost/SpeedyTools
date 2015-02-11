package speedytools.common.items;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;

import java.util.HashMap;
import java.util.List;

public class ItemSpeedyBoundary extends ItemComplexBase
{
  public static final String NAME = "complexboundary";

  public ItemSpeedyBoundary() {
    super();
    setMaxStackSize(1);
    setUnlocalizedName(NAME);
    setFull3D();                              // setting this flag causes the staff to render vertically in 3rd person view, like a pickaxe
    whichIcon = IconNames.NONE_PLACED;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void registerIcons(IIconRegister iconRegister)
  {
    icons.clear();
    for (IconNames entry : IconNames.values()) {
      IIcon newIcon = iconRegister.registerIcon(entry.filename);
      icons.put(entry, newIcon);
    }
    itemIcon = icons.get(IconNames.NONE_PLACED);
  }

  @Override
  public IIcon getIcon(ItemStack stack, int pass)
  {
    return icons.get(whichIcon);
  }

  public enum IconNames
  {
    BLANK("speedytoolsmod:blankicon"),
    GRABBING("speedytoolsmod:cloneboundarygrab"),
    NONE_PLACED("speedytoolsmod:cloneboundarynone"),
    ONE_PLACED("speedytoolsmod:cloneboundaryone"),
    TWO_PLACED("speedytoolsmod:cloneboundarytwo");

    private IconNames(String i_filename) {filename = i_filename;}

    private final String filename;
  }

  public void setCurrentIcon(IconNames newIcon)
  {
    whichIcon = newIcon;
  }

  /**
   * allows items to add custom lines of information to the mouseover description
   */
  @Override
  public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List textList, boolean useAdvancedItemTooltips)
  {
    textList.add("Right click: place boundary");
    textList.add("             markers (x2),");
    textList.add("       then: toggle drag on/off");
    textList.add("Left click: remove all markers");
  }

  private IconNames whichIcon;
  private HashMap<IconNames, IIcon> icons = new HashMap<IconNames, IIcon>();
}