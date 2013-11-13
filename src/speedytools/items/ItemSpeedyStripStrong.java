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
import net.minecraft.util.ChunkCoordinates;
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
 /*
  @Override
  public boolean leavesSolidBlocksIntact()
  {
    return false;
  }
*/
  /**
   * Selects the Blocks that will be affected by the tool when the player presses right-click
   * @param target the position of the cursor
   * @param player the player
   * @param currentItem the current item that the player is holding.  MUST be derived from ItemSpeedyTool.
   * @param partialTick partial tick time.
   * @return returns the list of blocks in the selection (may be zero length)
   */

  @Override
  public List<ChunkCoordinates> selectBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, float partialTick)
  {
    return selectLineOfBlocks(target, player, currentItem, false, partialTick);
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