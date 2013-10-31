package speedytools.items;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraft.world.World;
import speedytools.SpeedyToolsMod;

import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

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
   *     head or feet.
   * (2) the mouse is on a tile target: the first block selected will be:
   *     if the block is not "solid" (eg flowers, grass, snow, redstone, etc): the selected block
   *     if the block is "solid": the one adjacent to the tile in mouseTarget, on the face in mouseTarget.
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
      //     block and side it hits first.
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
      if (isBlockSolid(player.worldObj, new ChunkCoordinates(mouseTarget.blockX, mouseTarget.blockY, mouseTarget.blockZ))) {
        EnumFacing blockInFront = EnumFacing.getFront(mouseTarget.sideHit);
        blockx = mouseTarget.blockX + blockInFront.getFrontOffsetX();
        blocky = mouseTarget.blockY + blockInFront.getFrontOffsetY();
        blockz = mouseTarget.blockZ + blockInFront.getFrontOffsetZ();
      } else {
        blockx = mouseTarget.blockX;
        blocky = mouseTarget.blockY;
        blockz = mouseTarget.blockZ;
      }

    } else {  // currently only ENTITY
      return null;
    }
    return new ChunkCoordinates(blockx, blocky, blockz);
  }

  /**
   * selectLine is used to select a straight line of blocks, and return a list of their coordinates.
   * Starting from the startingBlock, the selection will continue in a line parallel to the player's look direction, snapped to the six cardinal directions or
   *   alternatively to one of the twenty 45 degree directions (if diagonalOK == true)
   *   Keeps going until it reaches maxLineLength, y goes outside the valid range, or hits a solid block (and stopWhenCollide is true)
   * @param startingBlock the first block in the straight line
   * @param player       the player (used for position and look information)
   * @param partialTick   partial tick time
   * @param maxLineLength the maximum number of blocks to select
   * @param diagonalOK    if true, diagonal 45 degree lines are allowed
   * @param stopWhenCollide if true, stops when a solid block is encountered (canCollide == true).  Otherwise, continues for maxLineLength
   * @return a list of the coordinates of all blocks in the selection, including the startingBlock.
   */
  public static List<ChunkCoordinates> selectLine(ChunkCoordinates startingBlock, EntityPlayer player, float partialTick,
                                                  int maxLineLength, boolean diagonalOK, boolean stopWhenCollide)
  {
    List<ChunkCoordinates> selection = new ArrayList<ChunkCoordinates>();

    if (startingBlock == null) return selection;

    Vec3 snappedCardinalDirection = snapToCardinalDirection(player.getLook(partialTick), diagonalOK);
    if (snappedCardinalDirection == null) return selection;

    final float EPSILON = 0.1F;
    ChunkCoordinates deltaPosition = new ChunkCoordinates(0, 0, 0);
    if (snappedCardinalDirection.xCoord > EPSILON) deltaPosition.posX = 1;
    if (snappedCardinalDirection.xCoord < -EPSILON) deltaPosition.posX = -1;
    if (snappedCardinalDirection.yCoord > EPSILON) deltaPosition.posY = 1;
    if (snappedCardinalDirection.yCoord < -EPSILON) deltaPosition.posY = -1;
    if (snappedCardinalDirection.zCoord > EPSILON) deltaPosition.posZ = 1;
    if (snappedCardinalDirection.zCoord < -EPSILON) deltaPosition.posZ = -1;

    ChunkCoordinates nextCoordinate = new ChunkCoordinates(startingBlock);
    selection.add(startingBlock);
    int blocksLeft = maxLineLength - 1;
    while (blocksLeft > 0) {
      nextCoordinate.set(nextCoordinate.posX + deltaPosition.posX,
                         nextCoordinate.posY + deltaPosition.posY,
                         nextCoordinate.posZ + deltaPosition.posZ
                        );
      if (nextCoordinate.posY < 0 || nextCoordinate.posY >= 256) break;
      if (stopWhenCollide) {
        if (isBlockSolid(player.worldObj, nextCoordinate)) break;
      }
      selection.add(new ChunkCoordinates(nextCoordinate));
      --blocksLeft;
    }

    return selection;
  }

  /**
   * Snaps the given vector to the closest of the six cardinal directions, or alternatively to one of the twenty 45 degree directions (if diagonalOK == true)
   * @param vectorToSnap the vector to be snapped to a cardinal direction
   * @param diagonalOK if true, diagonal 45 degree directions are allowed
   * @return the cardinal direction snapped to (unit length vector), or null if input vector is null or zero.
   */
  public static Vec3 snapToCardinalDirection(Vec3 vectorToSnap, boolean diagonalOK)
  {
    final float R2 = 0.707107F;  // 1 / sqrt(2)
    final float R3 = 0.577350F;  // 1 / sqrt(3)
    final float cardinal[][] =   {   {1, 0, 0},      {0, 1, 0},      {0,0,1} };
    final float cardinal45[][] = { {R2, R2, 0},   {-R2, R2, 0},   {R2, 0, R2},  {R2, 0 -R2}, {0, R2, R2}, {0, R2, -R2},
                                   {R3, R3, R3}, {R3, -R3, R3}, {R3, R3, -R3}, {R3, -R3, -R3}
                                 };
    Vec3 cardinalVector;
    Vec3 closestVector = null;
    double highestDotProduct = 0.0;

    // use the dot product to find the closest match (highest projection of vectorToSnap onto the cardinaldirection).
    // if the best match has negative dot product, it points the opposite way so reverse it

    int i;
    for (i=0; i < 3; ++i) {
      cardinalVector= Vec3.createVectorHelper(cardinal[i][0], cardinal[i][1], cardinal[i][2]);
      double dotProduct = cardinalVector.dotProduct(vectorToSnap);
      if (Math.abs(dotProduct) > Math.abs(highestDotProduct)) {
        highestDotProduct = dotProduct;
        closestVector = cardinalVector;
      }
    }

    if (diagonalOK) {
      for (i=0; i < 10; ++i) {
        cardinalVector= Vec3.createVectorHelper(cardinal45[i][0], cardinal45[i][1], cardinal45[i][2]);
        double dotProduct = cardinalVector.dotProduct(vectorToSnap);
        if (Math.abs(dotProduct) > Math.abs(highestDotProduct)) {
          highestDotProduct = dotProduct;
          closestVector = cardinalVector;
        }
      }
    }

    if (closestVector == null) return null;

    if (highestDotProduct < 0) {
      Vec3 nullVector = Vec3.createVectorHelper(0, 0, 0);
      closestVector = closestVector.subtract(nullVector);
    }

    return closestVector;
  }

  /**
   *  returns true if the block is "solid".
   *  Non-solid appears to correlate with "doesn't interact with a piston" i.e. getMobilityFlag == 1
    * @param world  the world
   * @param blockLocation  the [x]y,z] of the block to be checked
   */
  public static boolean isBlockSolid(World world, ChunkCoordinates blockLocation)
  {
    int blockId = world.getBlockId(blockLocation.posX, blockLocation.posY, blockLocation.posZ);
    return (Block.blocksList[blockId].getMobilityFlag() != 1);
  }

}
