//package speedytools.common.items;
//
//import net.minecraft.client.renderer.texture.IconRegister;
//import net.minecraft.entity.player.EntityPlayer;
//import net.minecraft.item.ItemStack;
//import net.minecraft.util.Icon;
//
//import java.util.List;
//
//public class ItemComplexCopy extends ItemComplexBase
//{
//  public ItemComplexCopy(int id) {
//    super(id);
//    setMaxStackSize(1);
//    setUnlocalizedName("CloneCopy");
//    setFull3D();                              // setting this flag causes the staff to render vertically in 3rd person view, like a pickaxe
//  }
//
//  @Override
//  public void registerIcons(IconRegister iconRegister)
//  {
//    itemIcon = iconRegister.registerIcon("speedytools:copystafficon");
//  }
//
//  @Override
//  public Icon getIcon(ItemStack stack, int pass)
//  {
//    return itemIcon;
//  }
//
//  /**
//   * allows items to add custom lines of information to the mouseover description
//   */
//  @Override
//  public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List textList, boolean useAdvancedItemTooltips)
//  {
//    textList.add("Right click: select blocks,");
//    textList.add("       then: toggle drag on/off");
//    textList.add("Left click: deselect");
//    textList.add("Right hold: place the copy");
//    textList.add("Left hold: undo copy");
//    textList.add("Mouse wheel: rotate selection");
//    textList.add("CTRL+right: flip selection");
//  }
//
//  private boolean renderHorizontal;
//}
//
