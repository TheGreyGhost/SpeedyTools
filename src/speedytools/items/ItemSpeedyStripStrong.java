package speedytools.items;

import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import speedytools.SpeedyToolsMod;
import speedytools.client.KeyBindingInterceptor;

import java.util.List;

public class ItemSpeedyStripStrong extends ItemSpeedyTool {
  public ItemSpeedyStripStrong(int id) {
    super(id);
    setMaxStackSize(64);
    setUnlocalizedName("SpeedyStripStrong");
    setFull3D();
  }

  @Override
  public void registerIcons(IconRegister iconRegister)
  {
    itemIcon = iconRegister.registerIcon("speedytools:WandStrongIcon");
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
}