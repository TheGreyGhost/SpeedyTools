package speedytools.common.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.world.World;
import speedytools.common.selections.VoxelSelection;

import java.util.*;

/**
 * User: The Grey Ghost
 * Date: 2/11/13
 */
public abstract class ItemComplexBase extends ItemSpeedyTool
{
  public ItemComplexBase(int id) {
    super(id);
    setCreativeTab(CreativeTabs.tabTools);
    setMaxDamage(-1);                         // not damageable
  }

  public static boolean isAcloneTool(int itemID)
  {
    return (   itemID == RegistryForItems.itemSpeedyBoundary.itemID
            || itemID == RegistryForItems.itemComplexCopy.itemID);
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

  /** called once per tick while the user is holding an ItemCloneTool
   * @param useKeyHeldDown
   */
  public void tick(World world, boolean useKeyHeldDown)
  {
    ++tickCount;
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


    // these keep track of the currently selected block, for when the tool is used
  protected static ChunkCoordinates currentlySelectedBlock = null;
//  protected static Item currentlySelectedTool = null;
  protected static Deque<ItemComplexBase> undoSoundsHistory = new LinkedList<ItemComplexBase>();

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
