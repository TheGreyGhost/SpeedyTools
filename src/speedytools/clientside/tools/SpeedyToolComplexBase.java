package speedytools.clientside.tools;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.rendering.*;
import speedytools.common.selections.VoxelSelection;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.items.ItemSpeedyTool;

/**
 * User: The Grey Ghost
 * Date: 18/04/2014
 */
public abstract class SpeedyToolComplexBase extends SpeedyTool
{
  public SpeedyToolComplexBase(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SpeedyToolSounds i_speedyToolSounds, UndoManagerClient i_undoManagerClient) {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient);
    // wireframeRendererUpdateLink, boundaryFieldRendererUpdateLink initialised in subclasses
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


  protected static ChunkCoordinates blockUnderCursor = null;      // why is this static?      // todo - try removing static later
  protected ChunkCoordinates boundaryCorner1 = null;
  protected ChunkCoordinates boundaryCorner2 = null;

  protected static final int SELECTION_MAX_XSIZE = VoxelSelection.MAX_X_SIZE;
  protected static final int SELECTION_MAX_YSIZE = VoxelSelection.MAX_Y_SIZE;
  protected static final int SELECTION_MAX_ZSIZE = VoxelSelection.MAX_Z_SIZE;

  protected RendererBoundaryField.BoundaryFieldRenderInfoUpdateLink boundaryFieldRendererUpdateLink = null;

}
