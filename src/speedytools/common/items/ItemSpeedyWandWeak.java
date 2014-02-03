package speedytools.common.items;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;

import java.util.List;

public class ItemSpeedyWandWeak extends ItemSpeedyTool {
  public ItemSpeedyWandWeak(int id) {
    super(id);
    setMaxStackSize(64);
    setUnlocalizedName("SpeedyWandWeak");
    setFull3D();                              // setting this flag causes the wand to render vertically in 3rd person view, like a pickaxe
  }

  @Override
  public void registerIcons(IconRegister iconRegister)
  {
    itemIcon = iconRegister.registerIcon("speedytools:wandweakicon");
  }

  /*
  @Override
  public boolean leavesSolidBlocksIntact()
  {
    return true;
  }
*/
  /**
   * Selects the Blocks that will be affected by the tool when the player presses right-click
   * @param target the position of the cursor
   * @param player the player
   * @param currentItem the current item that the player is holding.  MUST be derived from ItemSpeedyTool.
   * @param itemStackToPlace the item that would be placed in the selection
   * @param partialTick partial tick time.
   * @return returns the list of blocks in the selection (may be zero length)
   */
  @Override
  public List<ChunkCoordinates> selectBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, ItemStack itemStackToPlace, float partialTick)
  {
    return selectLineOfBlocks(target, player, currentItem, true, partialTick);
  }

  /**
   * allows items to add custom lines of information to the mouseover description
   */
  @Override
  public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List textList, boolean useAdvancedItemTooltips)
  {
    textList.add("Right click: place blocks (stop at first solid block) ");
    textList.add("Left click: undo last place");
    textList.add("Control: hold down to allow diagonal");
    textList.add("Control + mouse wheel: change count");
  }

  @Override
  protected String getPlaceSound() {return "speedytools:wandplace";}

  @Override
  protected String getUnPlaceSound() {return "speedytools:wandunplace";}



}