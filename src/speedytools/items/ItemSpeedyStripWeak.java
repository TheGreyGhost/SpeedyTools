package speedytools.items;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import speedytools.SpeedyToolsMod;

import java.util.List;

public class ItemSpeedyStripWeak extends Item {
  public ItemSpeedyStripWeak(int id) {
    super(id);
    setMaxStackSize(64);
    setUnlocalizedName("SpeedyStripWeak");
    setFull3D();                              // setting this flag causes the wand to render vertically in 3rd person view, like a pickaxe
  }

  @Override
  public void registerIcons(IconRegister iconRegister)
  {
    itemIcon = iconRegister.registerIcon("speedytools:WandWeakIcon");
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
  }


  @Override
  public void onUpdate(ItemStack itemStack, World world, Entity entity, int par4, boolean par5)
  {
    if (world.isRemote && entity instanceof EntityPlayerSP) {
        ItemStack heldItem = ((EntityPlayerSP) entity).getHeldItem();
        if (heldItem != null && heldItem.itemID == SpeedyToolsMod.itemSpeedyStripWeak.itemID) {
          SpeedyToolsMod.useItemButtonInterceptor.setInterceptionActive(true);
          SpeedyToolsMod.attackButtonInterceptor.setInterceptionActive(true);
        } else {
          SpeedyToolsMod.useItemButtonInterceptor.setInterceptionActive(false);
          SpeedyToolsMod.attackButtonInterceptor.setInterceptionActive(false);
        }
    }
  }


}