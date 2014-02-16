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
    itemIcon = iconRegister.registerIcon("speedytools:cloneboundarynone");
    iconTwoPlaced = iconRegister.registerIcon("speedytools:cloneboundarytwo");
    iconGrabbing = iconRegister.registerIcon("speedytools:cloneboundarygrab");
    iconNonePlaced = iconRegister.registerIcon("speedytools:cloneboundarynone");
    iconOnePlaced = iconRegister.registerIcon("speedytools:cloneboundaryone");
    iconBlank = iconRegister.registerIcon("speedytools:blankicon");
  }

  @Override
  public Icon getIcon(ItemStack stack, int pass)
  {
    if (boundaryGrabActivated) return iconGrabbing;

    if (boundaryCorner1 == null && boundaryCorner2 == null) {
      return iconNonePlaced;
    } else if (boundaryCorner1 != null && boundaryCorner2 != null) {
      return iconTwoPlaced;
    } else {
      return iconOnePlaced;
    }
  }

  /**
   * Selects the Block that will be affected by the tool when the player presses right-click
   *
   *
   * @param target the position of the cursor
   * @param player the player
   * @param currentItem the current item that the player is holding.  MUST be derived from ItemCloneTool.
   * @param partialTick partial tick time.
   * @return returns the coordinates of the block selected, or null if none
   */
  @Override
  public ChunkCoordinates getHighlightedBlock(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, float partialTick)
  {
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

  /**
   * renders the selection box if both corners haven't been placed yet.
   * @param player
   * @param partialTick
   */
  @Override
  public void renderBlockHighlight(EntityPlayer player, float partialTick)
  {
    if (boundaryCorner1 != null && boundaryCorner2 != null) return;
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

  private Icon iconTwoPlaced;
  private Icon iconGrabbing;
  private Icon iconNonePlaced;
  private Icon iconOnePlaced;
  private Icon iconBlank;

}