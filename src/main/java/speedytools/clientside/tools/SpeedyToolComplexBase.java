package speedytools.clientside.tools;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.network.PacketSenderClient;
import speedytools.clientside.rendering.*;
import speedytools.clientside.sound.SoundController;
import speedytools.common.selections.VoxelSelection;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.items.ItemSpeedyTool;

/**
* User: The Grey Ghost
* Date: 18/04/2014
*/
public abstract class SpeedyToolComplexBase extends SpeedyTool
{
  public SpeedyToolComplexBase(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SoundController i_speedyToolSounds,
                               UndoManagerClient i_undoManagerClient, PacketSenderClient i_packetSenderClient) {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient, i_packetSenderClient);
    // wireframeRendererUpdateLink, boundaryFieldRendererUpdateLink initialised in subclasses
  }

  @Override
  public abstract boolean activateTool(ItemStack newToolItemStack);

  /** The user has unequipped this tool, deactivate it, stop any effects, etc
   * @return
   */
  @Override
  public abstract boolean deactivateTool();

  @Override
  public abstract boolean processUserInput(EntityPlayerSP player, float partialTick, UserInput userInput);

  @Override
  public abstract boolean updateForThisFrame(World world, EntityPlayerSP player, float partialTick);

  @Override
  public void resetTool() {
    boundaryCorner1 = null;
    boundaryCorner2 = null;
  }

  /**
   * Update the item renderer based on whether the player is grabbing the selection or not
   * Call once per tick
   * @param grabbing true if player is grabbing
   */

  protected void updateGrabRenderTick(boolean grabbing)
  {
    final float GRAB_SWING_POSITION = 0.7F;
    if (grabbing) {
      EntityPlayerSP entityClientPlayerMP = Minecraft.getMinecraft().thePlayer;
      entityClientPlayerMP.swingProgress = GRAB_SWING_POSITION;
      entityClientPlayerMP.prevSwingProgress = GRAB_SWING_POSITION;
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
    Vec3 playerPosition = player.getPositionEyes(1.0F);
    Vec3 lookDirection = player.getLook(1.0F);
    Vec3 maxGrabPosition = playerPosition.addVector(lookDirection.xCoord * MAX_GRAB_DISTANCE, lookDirection.yCoord * MAX_GRAB_DISTANCE, lookDirection.zCoord * MAX_GRAB_DISTANCE);
    AxisAlignedBB boundaryField = new AxisAlignedBB(boundaryCorner1.getX(), boundaryCorner1.getY(), boundaryCorner1.getZ(),
            boundaryCorner2.getX() + 1, boundaryCorner2.getY() + 1, boundaryCorner2.getZ() + 1);
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

    int wxmin = Math.min(boundaryCorner1.getX(), boundaryCorner2.getX());
    int wymin = Math.min(boundaryCorner1.getY(), boundaryCorner2.getY());
    int wzmin = Math.min(boundaryCorner1.getZ(), boundaryCorner2.getZ());
    int wxmax = Math.max(boundaryCorner1.getX(), boundaryCorner2.getX());
    int wymax = Math.max(boundaryCorner1.getY(), boundaryCorner2.getY());
    int wzmax = Math.max(boundaryCorner1.getZ(), boundaryCorner2.getZ());

    boundaryCorner1 = new BlockPos(wxmin, wymin, wzmin);
    boundaryCorner2 = new BlockPos(wxmax, wymax, wzmax);
//    boundaryCorner1.posX = wxmin;  todo this might be different? changes object instead of object reference
//    boundaryCorner1.posY = wymin;
//    boundaryCorner1.posZ = wzmin;
//    boundaryCorner2.posX = wxmax;
//    boundaryCorner2.posY = wymax;
//    boundaryCorner2.posZ = wzmax;
  }


  protected BlockPos blockUnderCursor = null;   // todo - removed static - still ok?
  protected EnumFacing blockUnderCursorSideHit;  // which side of the block under cursor is the cursor on?
  protected BlockPos boundaryCorner1 = null;
  protected BlockPos boundaryCorner2 = null;

  protected static final int SELECTION_MAX_XSIZE = VoxelSelection.MAX_X_SIZE;
  protected static final int SELECTION_MAX_YSIZE = VoxelSelection.MAX_Y_SIZE;
  protected static final int SELECTION_MAX_ZSIZE = VoxelSelection.MAX_Z_SIZE;

  protected RendererBoundaryField.BoundaryFieldRenderInfoUpdateLink boundaryFieldRendererUpdateLink = null;

}
