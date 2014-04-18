package speedytools.common.items;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import speedytools.clientside.rendering.SelectionBoxRenderer;
import speedytools.clientside.selections.VoxelSelection;
import speedytools.common.utilities.UsefulConstants;
import speedytools.common.utilities.UsefulFunctions;
import speedytools.common.network.Packet250CloneToolUse;

import java.util.*;

/**
 * User: The Grey Ghost
 * Date: 2/11/13
 */
public abstract class ItemCloneTool extends ItemSpeedyTool
{
  public ItemCloneTool(int id) {
    super(id);
    setCreativeTab(CreativeTabs.tabTools);
    setMaxDamage(-1);                         // not damageable
  }

  public static boolean isAcloneTool(int itemID)
  {
    return (   itemID == RegistryForItems.itemCloneBoundary.itemID
            || itemID == RegistryForItems.itemCloneCopy.itemID);
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
  public void highlightBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, float partialTick)
  {
    currentlySelectedBlock = null;
    if (target == null) return;
    currentlySelectedBlock = new ChunkCoordinates(target.blockX, target.blockY, target.blockZ);
  }

  /**
   * called when the user presses the attackButton (Left Mouse)
   * @param thePlayer
   */
  @SideOnly(Side.CLIENT)
  public void attackButtonClicked(EntityClientPlayerMP thePlayer)
  {
    buttonClicked(thePlayer, 0);
  }

  /**
   * called when the user presses the use button (right mouse)
   * @param thePlayer
   */
  @SideOnly(Side.CLIENT)
  public void useButtonClicked(EntityClientPlayerMP thePlayer)
  {
    buttonClicked(thePlayer, 1);
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
  public void buttonClicked(EntityClientPlayerMP thePlayer, int whichButton)
  {
    return;
  }

  /** called once per tick while the user is holding an ItemCloneTool
   * @param useKeyHeldDown
   */
  public void tick(World world, boolean useKeyHeldDown)
  {
    ++tickCount;
  }

  /**
   * Inform the tool whether their action or undo was successfully started by the server or not
   * @param sequenceNumber
   * @param successfullyStarted
   */
  public void updateAction(int sequenceNumber, boolean successfullyStarted)
  {
    // default is do nothing
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
          wYmin = UsefulFunctions.clipToRange(wYmin, wYmax - SELECTION_MAX_YSIZE, wYmax - 1.0);
          break;
        }
        case UsefulConstants.FACE_YPOS: {
          wYmax += playerPosition.yCoord - boundaryGrabPoint.yCoord;
          wYmax = UsefulFunctions.clipToRange(wYmax, wYmin + SELECTION_MAX_YSIZE, wYmin + 1.0);
          break;
        }
        case UsefulConstants.FACE_ZNEG: {
          wZmin += playerPosition.zCoord - boundaryGrabPoint.zCoord;
          wZmin = UsefulFunctions.clipToRange(wZmin, wZmax - SELECTION_MAX_ZSIZE, wZmax - 1.0);
          break;
        }
        case UsefulConstants.FACE_ZPOS: {
          wZmax += playerPosition.zCoord - boundaryGrabPoint.zCoord;
          wZmax = UsefulFunctions.clipToRange(wZmax, wZmin + SELECTION_MAX_ZSIZE, wZmin + 1.0);
          break;
        }
        case UsefulConstants.FACE_XNEG: {
          wXmin += playerPosition.xCoord - boundaryGrabPoint.xCoord;
          wXmin = UsefulFunctions.clipToRange(wXmin, wXmax - SELECTION_MAX_XSIZE, wXmax - 1.0);
          break;
        }
        case UsefulConstants.FACE_XPOS: {
          wXmax += playerPosition.xCoord - boundaryGrabPoint.xCoord;
          wXmax = UsefulFunctions.clipToRange(wXmax, wXmin + SELECTION_MAX_XSIZE, wXmin + 1.0);
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
  public void renderBlockHighlight(EntityPlayer player, float partialTick)
  {
    if (currentlySelectedBlock == null) return;
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
    double EXPAND_BOX_DISTANCE = 0.002F;

    boundingBox = boundingBox.expand(EXPAND_BOX_DISTANCE, EXPAND_BOX_DISTANCE, EXPAND_BOX_DISTANCE)
                             .getOffsetBoundingBox(-playerPosition.xCoord, -playerPosition.yCoord, -playerPosition.zCoord);
    int faceToHighlight = -1;
    if (boundaryGrabActivated) {
      faceToHighlight = boundaryGrabSide;
    } else {
      faceToHighlight = boundaryCursorSide;
    }
    SelectionBoxRenderer.drawFilledCubeWithSelectedSide(boundingBox, faceToHighlight, boundaryGrabActivated);

    GL11.glDepthMask(true);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
  }

  public boolean renderCrossHairs(ScaledResolution scaledResolution, float partialTick) { return false;}

  /**
   * Check to see if the player's cursor is on one of the faces of the boundary field.
   * @param player
   * @return null if cursor isn't on any face; .sidehit shows the face if it is
   */
  protected static MovingObjectPosition boundaryFieldFaceSelection(EntityLivingBase player)
  {
    if (boundaryCorner1 == null || boundaryCorner2 == null) return null;

    final float MAX_GRAB_DISTANCE = 128.0F;
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
//  protected static Item currentlySelectedTool = null;
  protected static Deque<ItemCloneTool> undoSoundsHistory = new LinkedList<ItemCloneTool>();

  protected static ChunkCoordinates boundaryCorner1 = null;
  protected static ChunkCoordinates boundaryCorner2 = null;

  protected static boolean boundaryGrabActivated = false;
  protected static int boundaryGrabSide = -1;
  protected static Vec3 boundaryGrabPoint = null;

  protected static int boundaryCursorSide = -1;

  protected static final int SELECTION_MAX_XSIZE = VoxelSelection.MAX_X_SIZE;
  protected static final int SELECTION_MAX_YSIZE = VoxelSelection.MAX_Y_SIZE;
  protected static final int SELECTION_MAX_ZSIZE = VoxelSelection.MAX_Z_SIZE;

  protected static int tickCount;
}
