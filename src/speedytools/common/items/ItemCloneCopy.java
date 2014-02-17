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
import org.lwjgl.opengl.GL11;
import speedytools.clientonly.BlockMultiSelector;
import speedytools.clientonly.SelectionBoxRenderer;
import speedytools.clientonly.eventhandlers.CustomSoundsHandler;
import speedytools.common.UsefulConstants;

import java.util.List;

import static speedytools.clientonly.BlockMultiSelector.selectFill;

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
   * 1) no selection field - floodfill (all non-air blocks, including diagonal fill) from block clicked up
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
  public void highlightBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, float partialTick)
  {
    final int MAX_NUMBER_OF_HIGHLIGHTED_BLOCKS = 64;
    currentlySelectedBlock = null;
    highlightedBlocks = null;
    boundaryGrabActivated = false;
    boundaryCursorSide = UsefulConstants.FACE_NONE;

    if (selectionMade) return;

    if (target != null && target.typeOfHit == EnumMovingObjectType.TILE) {
      currentlySelectedBlock = new ChunkCoordinates(target.blockX, target.blockY, target.blockZ);
      boolean playerIsInsideBoundaryField = false;

      if (boundaryCorner1 != null && boundaryCorner2 != null) {
        sortBoundaryFieldCorners();
        if (   currentlySelectedBlock.posX >= boundaryCorner1.posX && currentlySelectedBlock.posX <= boundaryCorner2.posX
            && currentlySelectedBlock.posY >= boundaryCorner1.posY && currentlySelectedBlock.posY <= boundaryCorner2.posY
            && currentlySelectedBlock.posZ >= boundaryCorner1.posZ && currentlySelectedBlock.posZ <= boundaryCorner2.posZ ) {
          playerIsInsideBoundaryField = true;
        }
      }

      if (playerIsInsideBoundaryField) {
        highlightedBlocks = selectFill(target, player.worldObj, MAX_NUMBER_OF_HIGHLIGHTED_BLOCKS, true, true,
                                       boundaryCorner1.posX, boundaryCorner2.posX,
                                       boundaryCorner1.posY, boundaryCorner2.posY,
                                       boundaryCorner1.posZ, boundaryCorner2.posZ);
      } else {
        highlightedBlocks = selectFill(target, player.worldObj, MAX_NUMBER_OF_HIGHLIGHTED_BLOCKS, true, true,
                                       Integer.MIN_VALUE, Integer.MAX_VALUE,
                                       currentlySelectedBlock.posY, 255,
                                       Integer.MIN_VALUE, Integer.MAX_VALUE);
      }
      return;
    }

    if (boundaryCorner1 == null || boundaryCorner2 == null) return;
    Vec3 playerPosition = player.getPosition(1.0F);
    if (   playerPosition.xCoord >= boundaryCorner1.posX && playerPosition.xCoord <= boundaryCorner2.posX +1
        && playerPosition.yCoord >= boundaryCorner1.posY && playerPosition.yCoord <= boundaryCorner2.posY +1
        && playerPosition.zCoord >= boundaryCorner1.posZ && playerPosition.zCoord <= boundaryCorner2.posZ +1) {
      return;
    }
    MovingObjectPosition highlightedFace = boundaryFieldFaceSelection(Minecraft.getMinecraft().renderViewEntity);
    boundaryCursorSide = (highlightedFace != null) ? UsefulConstants.FACE_ALL : UsefulConstants.FACE_NONE;
 }

  @Override
  public void renderBoundaryField(EntityPlayer player, float partialTick)
  {
    if (!selectionMade) {
      super.renderBoundaryField(player, partialTick);
    }
  }

    @Override
  public void renderBlockHighlight(EntityPlayer player, float partialTick)
  {
    if (highlightedBlocks == null) return;
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
    GL11.glLineWidth(2.0F);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDepthMask(false);
    double expandDistance = 0.002F;

    double playerOriginX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTick;
    double playerOriginY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTick;
    double playerOriginZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTick;

    AxisAlignedBB boundingBox = AxisAlignedBB.getAABBPool().getAABB(0, 0, 0, 0, 0, 0);
    for (ChunkCoordinates block : highlightedBlocks) {
      boundingBox.setBounds(block.posX, block.posY, block.posZ, block.posX+1, block.posY+1, block.posZ+1);
      boundingBox = boundingBox.expand(expandDistance, expandDistance, expandDistance).getOffsetBoundingBox(-playerOriginX, -playerOriginY, -playerOriginZ);
      SelectionBoxRenderer.drawCube(boundingBox);
    }

    GL11.glDepthMask(true);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
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


  private boolean selectionMade;
  private List<ChunkCoordinates> highlightedBlocks;

}