//package speedytools.common.items;
//
//import net.minecraft.client.renderer.texture.IconRegister;
//import net.minecraft.entity.player.EntityPlayer;
//import net.minecraft.item.ItemStack;
//import net.minecraft.util.ChunkCoordinates;
//import net.minecraft.util.MovingObjectPosition;
//
//import java.util.List;
//
//public class ItemSpeedyWandStrong extends ItemSpeedyTool {
//  public ItemSpeedyWandStrong(int id) {
//    super(id);
//    setMaxStackSize(64);
//    setUnlocalizedName("SpeedyWandStrong");
//    setFull3D();
//  }
//
//  @Override
//  public void registerIcons(IconRegister iconRegister)
//  {
//    itemIcon = iconRegister.registerIcon("speedytools:wandstrongicon");
//  }
//
//  /**
//   * allows items to add custom lines of information to the mouseover description
//   */
//  @Override
//  public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List textList, boolean useAdvancedItemTooltips)
//  {
//    textList.add("Right click: place blocks (destroys");
//    textList.add("              any blocks in the way)");
//    textList.add("Left click: undo last place");
//    textList.add("Control: hold down to allow diagonal");
//    textList.add("Control + mouse wheel: change count");
//  }
//}