package speedytools.common.items;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ItemSpeedyBoundary extends ItemComplexBase
{
  public enum IconNames
  {
    BLANK("blankicon"),
    GRABBING("cloneboundarygrab"),
    NONE_PLACED("cloneboundarynone"),
    ONE_PLACED("cloneboundaryone"),
    TWO_PLACED("cloneboundarytwo");

    private IconNames(String i_filename) {filename = i_filename;}

    public final String filename;
  }

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
  public ModelResourceLocation getModel(ItemStack stack, EntityPlayer player, int useRemaining)
  {
    if (itemBoundaryModels == null) {
      itemBoundaryModels = new ItemBoundaryModels();  // lazy initialisation to prevent crash in dedicated server
    }
    return itemBoundaryModels.getModel(whichIcon);
  }

  public void setCurrentIcon(IconNames newIcon)
  {
    whichIcon = newIcon;
  }

  public void registerVariants()
  {
    if (itemBoundaryModels == null) {
      itemBoundaryModels = new ItemBoundaryModels();  // lazy initialisation to prevent crash in dedicated server
    }
    itemBoundaryModels.registerVariants(this);
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
  ItemBoundaryModels itemBoundaryModels;
}