package speedytools.common.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import speedytools.clientonly.BlockMultiSelector;
import speedytools.clientonly.eventhandlers.CustomSoundsHandler;
import speedytools.common.UsefulConstants;
import speedytools.common.UsefulFunctions;

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
   * Updates the block highlight, block selection, and/or boundary fields based on where the cursor is pointing
   * @param target the position of the cursor
   * @param player the player
   * @param currentItem the current item that the player is holding.  MUST be derived from ItemCloneTool.
   * @param partialTick partial tick time.

   */
  @Override
  public void highlightBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, float partialTick)
  {
    if (boundaryCorner1 != null  && boundaryCorner2 != null) {
      MovingObjectPosition highlightedFace = boundaryFieldFaceSelection(player);
      boundaryCursorSide = (highlightedFace != null) ? highlightedFace.sideHit : UsefulConstants.FACE_NONE;
      return;
    }

    currentlySelectedBlock = null;
    MovingObjectPosition airSelectionIgnoringBlocks = BlockMultiSelector.selectStartingBlock(null, player, partialTick);
    if (airSelectionIgnoringBlocks == null) return;
    // we want to make sure that we only select a block at very short range.  So if we have hit a block beyond this range, shorten the target to eliminate it

    if (target == null) {
      target = airSelectionIgnoringBlocks;
    } else if (target.typeOfHit == EnumMovingObjectType.TILE) {
      if (target.hitVec.dotProduct(target.hitVec) > airSelectionIgnoringBlocks.hitVec.dotProduct(airSelectionIgnoringBlocks.hitVec)) {
        target = airSelectionIgnoringBlocks;
      }
    }

    currentlySelectedBlock = new ChunkCoordinates(target.blockX, target.blockY, target.blockZ);
  }

  /** called once per tick while the user is holding an ItemCloneTool
   * @param useKeyHeldDown
   */
  @Override
  public void tick(World world, boolean useKeyHeldDown)
  {
    super.tick(world, useKeyHeldDown);
    // if the user was grabbing a boundary and has now released it, move the boundary blocks

    if (boundaryGrabActivated & !useKeyHeldDown) {
      Vec3 playerPosition = Minecraft.getMinecraft().renderViewEntity.getPosition(1.0F);
      AxisAlignedBB newBoundaryField = getGrabDraggedBoundaryField(playerPosition);
      boundaryCorner1.posX = (int)Math.round(newBoundaryField.minX);
      boundaryCorner1.posY = (int)Math.round(newBoundaryField.minY);
      boundaryCorner1.posZ = (int)Math.round(newBoundaryField.minZ);
      boundaryCorner2.posX = (int)Math.round(newBoundaryField.maxX - 1);
      boundaryCorner2.posY = (int)Math.round(newBoundaryField.maxY - 1);
      boundaryCorner2.posZ = (int)Math.round(newBoundaryField.maxZ - 1);
      boundaryGrabActivated = false;
      playSound(CustomSoundsHandler.BOUNDARY_UNGRAB,
              (float)playerPosition.xCoord, (float)playerPosition.yCoord, (float)playerPosition.zCoord);
    }
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
   * If both are placed, attempt to "grab" one of the boundary sides (cursor / line of sight intersects a side)
   *
   * @param thePlayer
   * @param whichButton 0 = left (undo), 1 = right (use)
   * @return true for success
   */
  @SideOnly(Side.CLIENT)
  @Override
  public void buttonClicked(EntityClientPlayerMP thePlayer, int whichButton)
  {

    switch (whichButton) {
      case 0: {
        boundaryCorner1 = null;
        boundaryCorner2 = null;
        playSound(CustomSoundsHandler.BOUNDARY_UNPLACE, thePlayer);
        break;
      }
      case 1: {
        if (boundaryCorner1 == null) {
          if (currentlySelectedBlock == null) return;
          boundaryCorner1 = new ChunkCoordinates(currentlySelectedBlock);
          playSound(CustomSoundsHandler.BOUNDARY_PLACE_1ST, thePlayer);
        } else if (boundaryCorner2 == null) {
          if (currentlySelectedBlock == null) return;
          addCornerPointWithMaxSize(currentlySelectedBlock);
          playSound(CustomSoundsHandler.BOUNDARY_PLACE_2ND, thePlayer);
        } else {
          MovingObjectPosition highlightedFace = boundaryFieldFaceSelection(Minecraft.getMinecraft().renderViewEntity);
          if (highlightedFace == null) return;

          boundaryGrabActivated = true;
          boundaryGrabSide = highlightedFace.sideHit;
          Vec3 playerPosition = thePlayer.getPosition(1.0F);
          boundaryGrabPoint = Vec3.createVectorHelper(playerPosition.xCoord, playerPosition.yCoord, playerPosition.zCoord);
          playSound(CustomSoundsHandler.BOUNDARY_GRAB, thePlayer);
        }
        break;
      }
      default: {     // should never happen
        break;
      }
    }

    return;
  }

  /**
   * add a new corner to the boundary (replace boundaryCorner2).  If the selection is too big, move boundaryCorner1.
   * @param newCorner
   */
  private void addCornerPointWithMaxSize(ChunkCoordinates newCorner)
  {
    boundaryCorner2 = new ChunkCoordinates(newCorner);
    boundaryCorner1.posX = UsefulFunctions.clipToRange(boundaryCorner1.posX, newCorner.posX - SELECTION_MAX_XSIZE + 1, newCorner.posX + SELECTION_MAX_XSIZE - 1);
    boundaryCorner1.posY = UsefulFunctions.clipToRange(boundaryCorner1.posY, newCorner.posY - SELECTION_MAX_YSIZE + 1, newCorner.posY + SELECTION_MAX_YSIZE - 1);
    boundaryCorner1.posZ = UsefulFunctions.clipToRange(boundaryCorner1.posZ, newCorner.posZ - SELECTION_MAX_ZSIZE + 1, newCorner.posZ + SELECTION_MAX_ZSIZE - 1);
    sortBoundaryFieldCorners();
  }

  private Icon iconTwoPlaced;
  private Icon iconGrabbing;
  private Icon iconNonePlaced;
  private Icon iconOnePlaced;
  private Icon iconBlank;

}