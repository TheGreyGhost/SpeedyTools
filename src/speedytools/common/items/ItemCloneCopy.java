package speedytools.common.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import speedytools.clientonly.BlockMultiSelector;
import speedytools.clientonly.eventhandlers.CustomSoundsHandler;

import java.util.List;

/*
three selection modes:
1) no selection field - floodfill from block clicked up
2) selection field: standing outside - all solid blocks in the field
3) selection field: standing inside - floodfill to boundary

1st rightclick = create selection
2nd right click & hold = drag selection
double right click = place
left click = undo place / undo selection
ctrl + Rclick = flip selection
ctrl + mousewheel = rotate

 */


public class ItemCloneCopy extends ItemCloneTool {
  public ItemCloneCopy(int id) {
    super(id);
    setMaxStackSize(1);
    setUnlocalizedName("CloneCopy");
    setFull3D();                              // setting this flag causes the staff to render vertically in 3rd person view, like a pickaxe
  }

  @Override
  public void registerIcons(IconRegister iconRegister)
  {
    itemIcon = iconRegister.registerIcon("speedytools:copystafficon");
  }

  @Override
  public Icon getIcon(ItemStack stack, int pass)
  {
    return itemIcon;
  }

  /**
   * Selects the first Block that will be affected by the tool when the player presses right-click
   * 1) no selection field - floodfill from block clicked up
   * 2) selection field: standing outside - all solid blocks in the field
   * 3) selection field: standing inside - floodfill to boundary field
   * So the selection algorithm is:
   * a) if the player is pointing at a block, return it; else
   * b) check the player is pointing at a side of the boundary field (from outside)
   *
   * @param target the position of the cursor
   * @param player the player
   * @param currentItem the current item that the player is holding.  MUST be derived from ItemCloneTool.
   * @param partialTick partial tick time.
   * @return
   */
  @Override
  public ChunkCoordinates getHighlightedBlock(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, float partialTick)
  {
    if (target != null && target.typeOfHit == EnumMovingObjectType.TILE) {
        ChunkCoordinates startBlockCoordinates = new ChunkCoordinates(target.blockX, target.blockY, target.blockZ);
        return startBlockCoordinates;
    }

    Vec3 playerPosition = player.getPosition(1.0F);
    if (   playerPosition.xCoord >= boundaryCorner1.posX && playerPosition.xCoord <= boundaryCorner2.posX
        && playerPosition.yCoord >= boundaryCorner1.posY && playerPosition.yCoord <= boundaryCorner2.posY
            && playerPosition.zCoord >= boundaryCorner1.posZ && playerPosition.zCoord <= boundaryCorner2.posZ) {

    }
    MovingObjectPosition highlightedFace = boundaryFieldFaceSelection(Minecraft.getMinecraft().renderViewEntity);
    if (highlightedFace == null) return null;



      MovingObjectPosition startBlock = BlockMultiSelector.selectStartingBlock(target, player, partialTick);
      if (startBlock == null)

        int faceToHighlight = -1;
      if (boundaryGrabActivated) {
        faceToHighlight = boundaryGrabSide;
      } else {
        MovingObjectPosition highlightedFace = boundaryFieldFaceSelection(player);
        faceToHighlight = (highlightedFace != null) ? highlightedFace.sideHit : -1;
      }


    }


    MovingObjectPosition airSelectionIgnoringBlocks = BlockMultiSelector.selectStartingBlock(null, player, partialTick);
    if (airSelectionIgnoringBlocks == null) return null;
    // we want to make sure that we only select a block at very short range.  So if we have hit a block beyond this range, shorten the target to eliminate it

    if (target == null) {
      target = airSelectionIgnoringBlocks;
    } else if (target.typeOfHit == EnumMovingObjectType.TILE) {
      if (target.hitVec.dotProduct(target.hitVec) > airSelectionIgnoringBlocks.hitVec.dotProduct(airSelectionIgnoringBlocks.hitVec)) {
        target = airSelectionIgnoringBlocks;
      }
    }

    ChunkCoordinates startBlockCoordinates = new ChunkCoordinates(target.blockX, target.blockY, target.blockZ);
    return startBlockCoordinates;
  }

  @Override
  public void renderBlockHighlight(EntityPlayer player, float partialTick)
  {
    super.renderBlockHighlight(player, partialTick);
  }

  /**
   * allows items to add custom lines of information to the mouseover description
   */
  @Override
  public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List textList, boolean useAdvancedItemTooltips)
  {
    textList.add("Right click: place boundary");
    textList.add("             markers (x2), then");
    textList.add(" Right button hold: move around");
    textList.add("             to drag boundary");
    textList.add("Left click: remove all markers");
  }

  /**
   * Place or remove a boundary marker.
   * If one of the two boundary markers is unplaced, set that.
   * If both are placed, attempt to "grab" one of the boundary sides (cursor / line of sight intersects one of them)
   *
   * @param thePlayer
   * @param whichButton 0 = left (undo), 1 = right (use)
   * @return true for success
   */
  @SideOnly(Side.CLIENT)
  @Override
  public boolean buttonClicked(EntityClientPlayerMP thePlayer, int whichButton)
  {
    if (currentlySelectedBlock == null) return false;

    switch (whichButton) {
      case 0: {
        boundaryCorner1 = null;
        boundaryCorner2 = null;
        playSound(CustomSoundsHandler.BOUNDARY_UNPLACE, thePlayer);
        break;
      }
      case 1: {
        if (boundaryCorner1 == null) {
          boundaryCorner1 = new ChunkCoordinates(currentlySelectedBlock);
          playSound(CustomSoundsHandler.BOUNDARY_PLACE_1ST, thePlayer);
        } else if (boundaryCorner2 == null) {
          boundaryCorner2 = new ChunkCoordinates(currentlySelectedBlock);
          sortBoundaryFieldCorners();
          playSound(CustomSoundsHandler.BOUNDARY_PLACE_2ND, thePlayer);
        } else {
          MovingObjectPosition highlightedFace = boundaryFieldFaceSelection(Minecraft.getMinecraft().renderViewEntity);
          if (highlightedFace == null) return false;

          boundaryGrabActivated = true;
          boundaryGrabSide = highlightedFace.sideHit;
          Vec3 playerPosition = thePlayer.getPosition(1.0F);
          boundaryGrabPoint = Vec3.createVectorHelper(playerPosition.xCoord, playerPosition.yCoord, playerPosition.zCoord);
          playSound(CustomSoundsHandler.BOUNDARY_GRAB, thePlayer);
        }
        break;
      }
      default: {     // should never happen
        return false;
      }
    }

    return true;
  }

}