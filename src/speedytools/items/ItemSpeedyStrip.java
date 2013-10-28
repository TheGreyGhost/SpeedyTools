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

public class ItemSpeedyStrip extends Item {
  public ItemSpeedyStrip(int id) {
    super(id);
    setMaxStackSize(1);
    setCreativeTab(CreativeTabs.tabTools);
    setUnlocalizedName("SpeedyStrip");
    setFull3D();                              // setting this flag causes the wand to render vertically in 3rd person view, like a pickaxe
    setMaxDamage(-1);                         // not damageable

  }

  @Override
  public void registerIcons(IconRegister iconRegister)
  {
    itemIcon = iconRegister.registerIcon("speedytools:WandIcon");
  }

  /**
   * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
   * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
   */
  @Override
  public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
  {
    return false;
  }

  /**
   * Called whenever this item is equipped and the right mouse button is pressed. Args: itemStack, world, entityPlayer
   */
  @Override
  public ItemStack onItemRightClick(ItemStack itemStack, World par2World, EntityPlayer par3EntityPlayer)
  {
    return itemStack;
  }

  /**
   * called when the player releases the use item button. Args: itemstack, world, entityplayer, itemInUseCount
   */
  @Override
  public void onPlayerStoppedUsing(ItemStack par1ItemStack, World par2World, EntityPlayer par3EntityPlayer, int par4)
  {
  }

  /**
   * allows items to add custom lines of information to the mouseover description
   */
  @Override
  public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List textList, boolean useAdvancedItemTooltips)
  {
    textList.add("Right click:place strip");
    textList.add("Left click:undo last strip");
  }

  /**
   * This is called when the item is used, before the block is activated.
   * @param stack The Item Stack
   * @param player The Player that used the item
   * @param world The Current World
   * @param x Target X Position
   * @param y Target Y Position
   * @param z Target Z Position
   * @param side The side of the target hit
   * @return Return true to prevent any further processing.
   */
  @Override
  public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ)
  {
    return false;
  }

  /**
   * Called before a block is broken.  Return true to prevent default block harvesting.
   *
   * Note: In SMP, this is called on both client and server sides!
   *
   * @param itemstack The current ItemStack
   * @param X The X Position
   * @param Y The X Position
   * @param Z The X Position
   * @param player The Player that is wielding the item
   * @return True to prevent harvesting, false to continue as normal
   */
  @Override
  public boolean onBlockStartBreak(ItemStack itemstack, int X, int Y, int Z, EntityPlayer player)
  {
    return true;
  }

  /**
   * Called when a entity tries to play the 'swing' animation.
   *
   * @param entityLiving The entity swinging the item.
   * @param stack The Item stack
   * @return True to cancel any further processing by EntityLiving
   */
  @Override
  public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack)
  {
    return false;
  }

  @Override
  public void onUpdate(ItemStack itemStack, World world, Entity entity, int par4, boolean par5)
  {
    if (world.isRemote && entity instanceof EntityPlayerSP) {
        ItemStack heldItem = ((EntityPlayerSP) entity).getHeldItem();
        if (heldItem != null && heldItem.itemID == SpeedyToolsMod.itemSpeedyStrip.itemID) {
         SpeedyToolsMod.attackButtonInterceptor.setInterceptionActive(true);
        } else {
          SpeedyToolsMod.attackButtonInterceptor.setInterceptionActive(false);
        }
    }
  }

  public void drawSelectionBox(EntityPlayer par1EntityPlayer, MovingObjectPosition par2MovingObjectPosition, int par3, float par4)
  {
    /*
    if (par3 == 0 && par2MovingObjectPosition.typeOfHit == EnumMovingObjectType.TILE)
    {
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
      GL11.glLineWidth(2.0F);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glDepthMask(false);
      float f1 = 0.002F;
      int j = this.theWorld.getBlockId(par2MovingObjectPosition.blockX, par2MovingObjectPosition.blockY, par2MovingObjectPosition.blockZ);

      if (j > 0)
      {
        Block.blocksList[j].setBlockBoundsBasedOnState(this.theWorld, par2MovingObjectPosition.blockX, par2MovingObjectPosition.blockY, par2MovingObjectPosition.blockZ);
        double d0 = par1EntityPlayer.lastTickPosX + (par1EntityPlayer.posX - par1EntityPlayer.lastTickPosX) * (double)par4;
        double d1 = par1EntityPlayer.lastTickPosY + (par1EntityPlayer.posY - par1EntityPlayer.lastTickPosY) * (double)par4;
        double d2 = par1EntityPlayer.lastTickPosZ + (par1EntityPlayer.posZ - par1EntityPlayer.lastTickPosZ) * (double)par4;
        this.drawOutlinedBoundingBox(Block.blocksList[j].getSelectedBoundingBoxFromPool(this.theWorld, par2MovingObjectPosition.blockX, par2MovingObjectPosition.blockY, par2MovingObjectPosition.blockZ).expand((double)f1, (double)f1, (double)f1).getOffsetBoundingBox(-d0, -d1, -d2));
      }

      GL11.glDepthMask(true);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glDisable(GL11.GL_BLEND);
    }
    */
  }


}