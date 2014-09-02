//package speedytools.common.items;
//
//import net.minecraft.client.renderer.texture.IconRegister;
//import net.minecraft.entity.player.EntityPlayer;
//import net.minecraft.item.ItemStack;
//
//import java.util.List;
//
//public class ItemSpeedyTester extends ItemSpeedyTool {
//  public ItemSpeedyTester(int id) {
//    super(id);
//    setMaxStackSize(64);
//    setUnlocalizedName("SpeedyTester");
//    setFull3D();                              // setting this flag causes the sceptre to render vertically in 3rd person view, like a pickaxe
//  }
//
//  @Override
//  public void registerIcons(IconRegister iconRegister)
//  {
//    itemIcon = iconRegister.registerIcon("speedytools:testericon");
//  }
//
//  /**
//   * allows items to add custom lines of information to the mouseover description
//   */
//  @Override
//  public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List textList, boolean useAdvancedItemTooltips)
//  {
//    textList.add("Right click: conduct test");
//    textList.add("Control + mouse wheel: change test #");
//    textList.add("                     : (64 = test all)");
//  }
//}