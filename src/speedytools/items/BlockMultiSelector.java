package speedytools.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import speedytools.SpeedyToolsMod;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: TheGreyGhost
 * Date: 28/10/13
 * Time: 9:47 PM
 * BlockMultiSelector is a group of methods used to select multiple blocks based on where the mouse is pointing.
 */
public class BlockMultiSelector
{
  /**
   * selectStartingBlock is used to select a starting block based on the player's position and look
   * There are three distinct cases for the starting block:
   * (1) the mouse is not on any target: the first block selected will be the one corresponding to the line of sight from the player's head, not including the block enclosing the player's
   *     head or feet
   * (2) the mouse is on a tile target: the first block selected will be the one adjacent to the tile in mouseTarget, on the face in mouseTarget.
   * (3) the mouse is on an entity: no selection.
   * After selecting the inital block, the selection will continue in a line parallel to the player's look direction, snapped to the six cardinal directions or
   *   alternatively to one of the twenty 45 degree directions (if diagonalOK == true)
   * @param mouseTarget  where the cursor is currently pointed
   * @param player       the player (used for position and look information)
   * @param partialTick  used for calculating player head position
   * @return the coordinates of the starting selection block, or null if none
   */
  public static ChunkCoordinates selectStartingBlock(MovingObjectPosition mouseTarget, EntityPlayer player, float partialTick)
  {
    int blockx, blocky, blockz;
    double playerOriginX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTick;
    double playerOriginY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTick;
    double playerOriginZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTick;

    Vec3 playerLook = player.getLook(partialTick);

    if (mouseTarget == null) {   // no hit
      // we need to find the closest [x,y,z] in the direction the player is looking in, that the player is not occupying.
      // This will depend on the yaw but also the elevation.
      // The algorithm is
      // (1) using a dummy block located around the player's eye position, perform a ray trace on the look and find the
      //     block and side it hits first
      // (2) If this block is up or to the side, select that block.
      // (3) If the block is down, it interferes with the player's legs, so continue the ray trace through this block as well.

      blockx = MathHelper.floor_double(playerOriginX);
      blocky = MathHelper.floor_double(playerOriginY);
      blockz = MathHelper.floor_double(playerOriginZ);

      Vec3 startVec = player.worldObj.getWorldVec3Pool().getVecFromPool(playerOriginX, playerOriginY, playerOriginZ);
      Vec3 endVec = startVec.addVector(playerLook.xCoord * 4.0, playerLook.yCoord * 4.0, playerLook.zCoord * 4.0);

      MovingObjectPosition startingBlockPosition;
      startingBlockPosition = SpeedyToolsMod.blockCollisionCheck.collisionRayTrace(player.worldObj, blockx, blocky, blockz, startVec, endVec);
      if (startingBlockPosition == null || startingBlockPosition.sideHit < 0) {  // shouldn't be possible
        return null;
      }
      blockx += Facing.offsetsXForSide[startingBlockPosition.sideHit];
      blocky += Facing.offsetsYForSide[startingBlockPosition.sideHit];
      blockz += Facing.offsetsZForSide[startingBlockPosition.sideHit];
      if (startingBlockPosition.sideHit == 0) {
        startVec = startingBlockPosition.hitVec.addVector(0.0, -0.01, 0.0);  // prevent collision with same point again
        endVec = startVec.addVector(playerLook.xCoord * 4.0, playerLook.yCoord * 4.0, playerLook.zCoord * 4.0);
        startingBlockPosition = SpeedyToolsMod.blockCollisionCheck.collisionRayTrace(player.worldObj, blockx, blocky, blockz, startVec, endVec);
        blockx += Facing.offsetsXForSide[startingBlockPosition.sideHit];
        blocky += Facing.offsetsYForSide[startingBlockPosition.sideHit];
        blockz += Facing.offsetsZForSide[startingBlockPosition.sideHit];
      }

    } else if (mouseTarget.typeOfHit == EnumMovingObjectType.TILE) {
      EnumFacing blockInFront = EnumFacing.getFront(mouseTarget.sideHit);
      blockx = mouseTarget.blockX + blockInFront.getFrontOffsetX();
      blocky = mouseTarget.blockY + blockInFront.getFrontOffsetY();
      blockz = mouseTarget.blockZ + blockInFront.getFrontOffsetZ();
    } else {  // currently only ENTITY
      return null;
    }
    return new ChunkCoordinates(blockx, blocky, blockz);
  }



  /**
   * selectLine is used to select a straight line of blocks, and return a list of their coordinates.
   * There are three distinct cases for the starting block:
   * (1) the mouse is not on any target: the first block selected will be the one corresponding to the position of the player's hand
   * (2) the mouse is on a tile target: the first block selected will be the one adjacent to the tile in mouseTarget, on the face in mouseTarget.
   * (3) the mouse is on an entity: no selection.
   * After selecting the inital block, the selection will continue in a line parallel to the player's look direction, snapped to the six cardinal directions or
   *   alternatively to one of the twenty 45 degree directions (if diagonalOK == true)
   * @param mouseTarget  where the cursor is currently pointed
   * @param player       the player (used for position and look information)
   * @param partialTick
   * @param maxLineLength the maximum number of blocks to select
   * @param diagonalOK    if true, diagonal 45 degree lines are allowed
   * @param stopWhenCollide if true, stops when a solid block is encountered (canCollide == true).  Otherwise, continues for maxLineLength
   * @return a list of the coordinates of all blocks in the selection
   */
  public static List<ChunkCoordinates> selectLine(MovingObjectPosition mouseTarget, EntityPlayer player, float partialTick,
                                      int maxLineLength, boolean diagonalOK, boolean stopWhenCollide)
  {
    List<ChunkCoordinates> selection = new ArrayList<ChunkCoordinates>();

    switch (mouseTarget.typeOfHit) {
 //     case null:

      case TILE: {
        break;
      }
      case ENTITY:{
        return selection;
      }
    }


    return selection;
  }

}
