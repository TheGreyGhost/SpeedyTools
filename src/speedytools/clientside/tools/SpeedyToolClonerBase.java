package speedytools.clientside.tools;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.rendering.*;
import speedytools.clientside.selections.VoxelSelection;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.items.ItemSpeedyTool;
import speedytools.common.utilities.UsefulConstants;
import speedytools.common.utilities.UsefulFunctions;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * User: The Grey Ghost
 * Date: 18/04/2014
 */
public abstract class SpeedyToolClonerBase extends SpeedyTool
{
  public SpeedyToolClonerBase(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SpeedyToolSounds i_speedyToolSounds, UndoManagerClient i_undoManagerClient) {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient);
    wireframeRendererUpdateLink = this.new ClonerWireframeRendererLink();
  }

  @Override
  public abstract boolean activateTool();

  /** The user has unequipped this tool, deactivate it, stop any effects, etc
   * @return
   */
  @Override
  public abstract boolean deactivateTool();

  @Override
  public abstract boolean processUserInput(EntityClientPlayerMP player, float partialTick, UserInput userInput);

  @Override
  public abstract boolean update(World world, EntityClientPlayerMP player, float partialTick);
  /**
   * This class is used to provide information to the Boundary Field Renderer when it needs it:
   * The Renderer calls refreshRenderInfo, which copies the relevant information from the tool.
   */
  public class BoundaryFieldRendererUpdateLink implements RendererBoundaryField.BoundaryFieldRenderInfoUpdateLink
  {
    @Override
    public boolean refreshRenderInfo(RendererBoundaryField.BoundaryFieldRenderInfo infoToUpdate, Vec3 playerPosition)
    {
      if (boundaryCorner1 == null && boundaryCorner2 == null) return false;
      infoToUpdate.boundaryCursorSide = boundaryCursorSide;
      infoToUpdate.boundaryGrabActivated = boundaryGrabActivated;
      infoToUpdate.boundaryGrabSide = boundaryGrabSide;
      infoToUpdate.boundaryFieldAABB = getGrabDraggedBoundaryField(playerPosition);
      return true;
    }
  }

  /**
   * This class is used to provide information to the WireFrame Renderer when it needs it:
   * The Renderer calls refreshRenderInfo, which copies the relevant information from the tool.
   * Draws a wireframe selection, provided that not both of the corners have been placed yet
   */
  public class ClonerWireframeRendererLink implements RendererWireframeSelection.WireframeRenderInfoUpdateLink
  {
    @Override
    public boolean refreshRenderInfo(RendererWireframeSelection.WireframeRenderInfo infoToUpdate)
    {
      if (boundaryCorner1 != null && boundaryCorner2 != null) return false;
      infoToUpdate.currentlySelectedBlocks = new ArrayList<ChunkCoordinates>(1);
      infoToUpdate.currentlySelectedBlocks.add(currentlySelectedBlock);
      return true;
    }
  }

  /**
   * Check to see if the player's cursor is on one of the faces of the boundary field.
   * @param player
   * @return null if cursor isn't on any face; .sidehit shows the face if it is
   */
  protected MovingObjectPosition boundaryFieldFaceSelection(EntityLivingBase player)
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
          FMLLog.warning("Invalid boundaryGrabSide (%d", boundaryGrabSide);
        }
      }
    }
    return AxisAlignedBB.getAABBPool().getAABB(wXmin, wYmin, wZmin, wXmax, wYmax, wZmax);
  }

  protected BoundaryFieldRendererUpdateLink boundaryFieldRendererUpdateLink;

  protected static ChunkCoordinates currentlySelectedBlock = null;
  protected ChunkCoordinates boundaryCorner1 = null;
  protected ChunkCoordinates boundaryCorner2 = null;

  protected boolean boundaryGrabActivated = false;
  protected int boundaryGrabSide = -1;
  protected Vec3 boundaryGrabPoint = null;

  protected int boundaryCursorSide = -1;

  protected static final int SELECTION_MAX_XSIZE = VoxelSelection.MAX_X_SIZE;
  protected static final int SELECTION_MAX_YSIZE = VoxelSelection.MAX_Y_SIZE;
  protected static final int SELECTION_MAX_ZSIZE = VoxelSelection.MAX_Z_SIZE;

}
