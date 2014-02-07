package speedytools.common.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import speedytools.clientonly.BlockMultiSelector;

import java.util.List;

public class ItemCloneBoundary extends ItemCloneTool {
  public ItemCloneBoundary(int id) {
    super(id);
    setMaxStackSize(1);
    setUnlocalizedName("CloneBoundary");
    setFull3D();                              // setting this flag causes the staff to render vertically in 3rd person view, like a pickaxe
  }

  @Override
  public void registerIcons(IconRegister iconRegister)
  {
    itemIcon = iconRegister.registerIcon("speedytools:cloneboundaryicon");
  }

  /**
   * Selects the Block that will be affected by the tool when the player presses right-click
   *
   * @param target the position of the cursor
   * @param player the player
   * @param currentItem the current item that the player is holding.  MUST be derived from ItemCloneTool.
   * @param itemStackToPlace the item that would be placed in the selection
   * @param partialTick partial tick time.
   * @return returns the coordinates of the block selected, or null if none
   */
  @Override
  public ChunkCoordinates selectBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, ItemStack itemStackToPlace, float partialTick)
  {
    final double MAX_TILE_DISTANCE = 1;
    // we want to make sure that we only select a block at very short range.  So if we have hit a block beyond this range, shorten the target to eliminate it
    if (target.typeOfHit == EnumMovingObjectType.TILE) {
      MovingObjectPosition ignoreBlock = BlockMultiSelector.selectStartingBlock(null, player, partialTick);
      if (ignoreBlock != null ) { // should always be true
        if (target.hitVec.dotProduct(target.hitVec) > ignoreBlock.hitVec.dotProduct(ignoreBlock.hitVec)) {
          target = ignoreBlock;
        }
      }
    }

    if (target == null) return null; // should never be true
    ChunkCoordinates startBlockCoordinates = new ChunkCoordinates(target.blockX, target.blockY, target.blockZ);
    return startBlockCoordinates;
  }

  /**
   * allows items to add custom lines of information to the mouseover description
   */
  @Override
  public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List textList, boolean useAdvancedItemTooltips)
  {
    textList.add("Right click: place/move boundary marker");
    textList.add("Left click: remove all boundary markers");
  }

  /**
   * Place or remove a boundary marker.
   * If one of the two boundary markers is unplaced, set that.
   * If both are placed, move the nearest to the new position
   * @param whichButton 0 = left (undo), 1 = right (use)
   * @return true for success
   */
  @SideOnly(Side.CLIENT)
  @Override
  public boolean actOnButtonClicked(int whichButton)
  {
    if (currentlySelectedBlock == null) return false;

    switch (whichButton) {
      case 0: {
        boundaryCorner1 = null;
        boundaryCorner2 = null;
        break;
      }
      case 1: {
        if (boundaryCorner1 == null) {
          boundaryCorner1 = currentlySelectedBlock;
        } else if (boundaryCorner2 == null) {
          boundaryCorner2 = currentlySelectedBlock;
        } else {
          if (  boundaryCorner1.getDistanceSquaredToChunkCoordinates(currentlySelectedBlock)
              < boundaryCorner2.getDistanceSquaredToChunkCoordinates(currentlySelectedBlock)) {
            boundaryCorner1 = currentlySelectedBlock;
          } else {
            boundaryCorner2 = currentlySelectedBlock;
          }
        }
        break;
      }
      default: {     // should never happen
        return false;
      }
    }

    return true;
  }

  @Override
  protected String getPlaceSound() {return "speedytools:boundaryplace";}

  @Override
  protected String getUnPlaceSound() {return "speedytools:boundary";}

}