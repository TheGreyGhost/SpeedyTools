package speedytools.common.items;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import speedytools.clientonly.SelectionBoxRenderer;
import speedytools.clientonly.SpeedyToolControls;
import speedytools.clientonly.eventhandlers.CustomSoundsHandler;
import speedytools.common.UsefulConstants;

import java.util.*;

/**
 * User: The Grey Ghost
 * Date: 2/11/13
 */
public abstract class ItemCloneTool extends Item
{
  public ItemCloneTool(int id) {
    super(id);
    setCreativeTab(CreativeTabs.tabTools);
    setMaxDamage(-1);                         // not damageable
  }

  public static boolean isAcloneTool(int itemID)
  {
    return (itemID == RegistryForItems.itemCloneBoundary.itemID);
  }

  /**
   * Finds the first block in the player's line of sight, including liquids
   * @param world
   * @param entityPlayer
   * @return the corresponding MovingObjectPosition, null if none
   */
  @SideOnly(Side.CLIENT)
  public MovingObjectPosition rayTraceLineOfSight(World world, EntityPlayer entityPlayer)
  {
    return this.getMovingObjectPositionFromPlayer(world, entityPlayer, true);
  }

  /**
   * Selects the Block that will be affected by the tool when the player presses right-click
   *   default method just selects the first block as per vanilla
   *
   *
   * @param target the position of the cursor
   * @param player the player
   * @param currentItem the current item that the player is holding.  MUST be derived from ItemSpeedyTool.
   * @param partialTick partial tick time.
   * @return returns the coordinates of the block selected, or null if none
   */
  @SideOnly(Side.CLIENT)
  public ChunkCoordinates selectBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, float partialTick)
  {
    if (target == null) return null;
    ChunkCoordinates startBlockCoordinates = new ChunkCoordinates(target.blockX, target.blockY, target.blockZ);
    return startBlockCoordinates;
  }

  /**
   * Sets the current selection for the currently-held clone tool
   * @param currentTool the currently-held clone tool
   * @param currentSelection the currently selected block, or null if none
   */
  @SideOnly(Side.CLIENT)
  public static void setCurrentToolSelection(Item currentTool, ChunkCoordinates currentSelection)
  {
    currentlySelectedTool = currentTool;
    currentlySelectedBlock = currentSelection;
  }

  /**
   * called when the user presses the attackButton (Left Mouse)
   * @param thePlayer
   */
  @SideOnly(Side.CLIENT)
  public void attackButtonClicked(EntityClientPlayerMP thePlayer)
  {
    boolean success = buttonClicked(thePlayer, 0);
  }

  /**
   * called when the user presses the use button (right mouse)
   * @param thePlayer
   */
  @SideOnly(Side.CLIENT)
  public void useButtonClicked(EntityClientPlayerMP thePlayer)
  {
    boolean success = buttonClicked(thePlayer, 1);
  }

  /**
   * called when the user scrolls the mouse wheel.
   * @param delta the delta (see Mouse.getDWheel() )
   */
  @SideOnly(Side.CLIENT)
  public static void mouseWheelMoved(int delta)
  {
    int MOUSE_DELTA_PER_SLOT = 120;

    // by default - do nothing
  }

  /**
   * Inform the server of the player's action with the SpeedyTool.  Checks to make sure that currentlySelectedTool is valid, and if right click then currentlySelectedBlock has at least one entry.
   *
   *
   * @param thePlayer
   * @param whichButton 0 = left (undo), 1 = right (use)
   * @return true for success
   */

  @SideOnly(Side.CLIENT)
  public boolean buttonClicked(EntityClientPlayerMP thePlayer, int whichButton)
  {
    return false;
  }

  /** called once per tick while the user is holding an ItemCloneTool
   * @param useKeyHeldDown
   */
  public void tickKeyStates(boolean useKeyHeldDown)
  {
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
   * Calculate the new boundary field after being dragged to the current player position
   *   Drags one side depending on which one the player grabbed, and how far they have moved
   *   Won't drag the boundary field smaller than one block
   * @param playerPosition
   * @return the new boundary field, null if a problem occurred
   */
  protected AxisAlignedBB getGrabDraggedBoundaryField(Vec3 playerPosition)
  {
    sortBoundaryFieldCorners();

    if (boundaryCorner1 == null) return null;
    ChunkCoordinates cnrMin = boundaryCorner1;
    ChunkCoordinates cnrMax = (boundaryCorner2 != null) ? boundaryCorner2 : boundaryCorner1;
    double wXmin = cnrMin.posX;
    double wYmin = cnrMin.posY;
    double wZmin = cnrMin.posZ;
    double wXmax = cnrMax.posX + 1;
    double wYmax = cnrMax.posY + 1;
    double wZmax = cnrMax.posZ + 1;

    if (boundaryGrabActivated) {
      switch (boundaryGrabSide) {
        case UsefulConstants.FACE_YNEG: {
          wYmin += playerPosition.yCoord - boundaryGrabPoint.yCoord;
          wYmin = Math.min(wYmin, wYmax - 1.0);
          break;
        }
        case UsefulConstants.FACE_YPOS: {
          wYmax += playerPosition.yCoord - boundaryGrabPoint.yCoord;
          wYmax = Math.max(wYmax, wYmin + 1.0);
          break;
        }
        case UsefulConstants.FACE_ZNEG: {
          wZmin += playerPosition.zCoord - boundaryGrabPoint.zCoord;
          wZmin = Math.min(wZmin, wZmax - 1.0);
          break;
        }
        case UsefulConstants.FACE_ZPOS: {
          wZmax += playerPosition.zCoord - boundaryGrabPoint.zCoord;
          wZmax = Math.max(wZmax, wZmin + 1.0);
          break;
        }
        case UsefulConstants.FACE_XNEG: {
          wXmin += playerPosition.xCoord - boundaryGrabPoint.xCoord;
          wXmin = Math.min(wXmin, wXmax - 1.0);
          break;
        }
        case UsefulConstants.FACE_XPOS: {
          wXmax += playerPosition.xCoord - boundaryGrabPoint.xCoord;
          wXmax = Math.max(wXmax, wXmin + 1.0);
          break;
        }
        default: {
          FMLLog.warning("Invalid boundaryGrabSide (%d) in %s", boundaryGrabSide, ItemCloneTool.class.getCanonicalName());
        }
      }
    }
    return AxisAlignedBB.getAABBPool().getAABB(wXmin, wYmin, wZmin, wXmax, wYmax, wZmax);
  }

  public final int SELECTION_BOX_STYLE = 0; //0 = cube, 1 = cube with cross on each side

  /**
   * renders the selection box if both corners haven't been placed yet.
   * @param player
   * @param partialTick
   */
  public void renderSelection(EntityPlayer player, float partialTick)
  {
    if (boundaryCorner1 != null && boundaryCorner2 != null) return;

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

    AxisAlignedBB boundingBox = AxisAlignedBB.getAABBPool().getAABB(currentlySelectedBlock.posX, currentlySelectedBlock.posY, currentlySelectedBlock.posZ,
                                                                    currentlySelectedBlock.posX+1, currentlySelectedBlock.posY+1, currentlySelectedBlock.posZ+1);
    boundingBox = boundingBox.expand(expandDistance, expandDistance, expandDistance).getOffsetBoundingBox(-playerOriginX, -playerOriginY, -playerOriginZ);
    switch (SELECTION_BOX_STYLE) {
      case 0: {
        SelectionBoxRenderer.drawCube(boundingBox);
        break;
      }
      case 1: {
        SelectionBoxRenderer.drawFilledCube(boundingBox);
        break;
      }
    }

    GL11.glDepthMask(true);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
  }

  /**
   * render the boundary field if there is one selected
   * @param player
   * @param partialTick
   */
  public void renderBoundaryField(EntityPlayer player, float partialTick) {
    if (boundaryCorner1 == null && boundaryCorner2 == null) return;

    Vec3 playerPosition = player.getPosition(partialTick);
    AxisAlignedBB boundingBox = getGrabDraggedBoundaryField(playerPosition);

    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
    GL11.glLineWidth(2.0F);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDepthMask(false);
    double expandDistance = 0.002F;

    boundingBox = boundingBox.expand(expandDistance, expandDistance, expandDistance)
                             .getOffsetBoundingBox(-playerPosition.xCoord, -playerPosition.yCoord, -playerPosition.zCoord);
    int faceToHighlight = -1;
    if (boundaryGrabActivated) {
      faceToHighlight = boundaryGrabSide;
    } else {
      MovingObjectPosition highlightedFace = boundaryFieldFaceSelection(player);
      faceToHighlight = (highlightedFace != null) ? highlightedFace.sideHit : -1;
    }
    SelectionBoxRenderer.drawFilledCubeWithSelectedSide(boundingBox, faceToHighlight, boundaryGrabActivated);

    GL11.glDepthMask(true);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
  }

  protected static MovingObjectPosition boundaryFieldFaceSelection(EntityLivingBase player)
  {
    if (boundaryCorner1 == null || boundaryCorner2 == null) return null;

    final float MAX_GRAB_DISTANCE = 32.0F;
    Vec3 playerPosition = player.getPosition(1.0F);
    Vec3 lookDirection = player.getLook(1.0F);
    Vec3 maxGrabPosition = playerPosition.addVector(lookDirection.xCoord * MAX_GRAB_DISTANCE, lookDirection.yCoord * MAX_GRAB_DISTANCE, lookDirection.zCoord * MAX_GRAB_DISTANCE);
    AxisAlignedBB boundaryField = AxisAlignedBB.getAABBPool().getAABB(boundaryCorner1.posX, boundaryCorner1.posY, boundaryCorner1.posZ,
                                                                      boundaryCorner2.posX + 1, boundaryCorner2.posY + 1, boundaryCorner2.posZ + 1);
    MovingObjectPosition fieldIntersection = boundaryField.calculateIntercept(playerPosition, maxGrabPosition);
    return fieldIntersection;
  }

  /**
   * Sort the corner coordinates so that boundaryCorner1 is [xmin, ymin, zmin] and boundaryCorner2 is [xmax, ymax, zmax]
   */
  protected void sortBoundaryFieldCorners()
  {
    if (boundaryCorner1 == null) {
      boundaryCorner1 = boundaryCorner2;
      boundaryCorner2 = null;
    }
    if (boundaryCorner2 == null) return;

    int wxmin = Math.min(boundaryCorner1.posX, boundaryCorner2.posX);
    int wymin = Math.min(boundaryCorner1.posY, boundaryCorner2.posY);
    int wzmin = Math.min(boundaryCorner1.posZ, boundaryCorner2.posZ);
    int wxmax = Math.max(boundaryCorner1.posX, boundaryCorner2.posX);
    int wymax = Math.max(boundaryCorner1.posY, boundaryCorner2.posY);
    int wzmax = Math.max(boundaryCorner1.posZ, boundaryCorner2.posZ);

    boundaryCorner1.posX = wxmin;
    boundaryCorner1.posY = wymin;
    boundaryCorner1.posZ = wzmin;
    boundaryCorner2.posX = wxmax;
    boundaryCorner2.posY = wymax;
    boundaryCorner2.posZ = wzmax;
  }

  protected void playSound(String soundname, EntityClientPlayerMP thePlayer)
  {
    Minecraft.getMinecraft().sndManager.playSound(soundname,
            (float) (thePlayer.posX),
            (float) (thePlayer.posY),
            (float) (thePlayer.posZ),
            1.0F, 1.0F);
  }

  protected void playSound(String soundname, float x, float y, float z)
  {
    Minecraft.getMinecraft().sndManager.playSound(soundname, x, y, z, 1.0F, 1.0F);
  }

    // these keep track of the currently selected block, for when the tool is used
  protected static ChunkCoordinates currentlySelectedBlock = null;
  protected static Item currentlySelectedTool = null;
  protected static Deque<ItemCloneTool> undoSoundsHistory = new LinkedList<ItemCloneTool>();

  protected static ChunkCoordinates boundaryCorner1 = null;
  protected static ChunkCoordinates boundaryCorner2 = null;

  protected static boolean boundaryGrabActivated = false;
  protected static int boundaryGrabSide = 0;
  protected static Vec3 boundaryGrabPoint = null;


}
