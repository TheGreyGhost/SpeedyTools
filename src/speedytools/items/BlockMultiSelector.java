package speedytools.items;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraft.world.World;

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
   * (1) the mouse is not on any target: the first block selected will be the one corresponding to the line of sight from the player's head:
   *     a) which doesn't intersect the player's bounding box
   *     b) which is at least 0.5 m from the player's eyes in each of the the x, y, and z directions.
   * (2) the mouse is on a tile target: the first block selected will be:
   *     if the block is not "solid" (eg flowers, grass, snow, redstone, etc): the mouseTarget block itself
   *     if the block is "solid": the one adjacent to the tile in mouseTarget, on the face in mouseTarget.
   * (3) the mouse is on an entity: no selection.
   * The method also returns the look vector snapped to the midpoint of the face that was hit on the selected Block
   * @param mouseTarget  where the cursor is currently pointed
   * @param player       the player (used for position and look information)
   * @param partialTick  used for calculating player head position
   * @return the coordinates of the starting selection block plus the side hit plus the look vector snapped to the midpoint of
   *         side hit.  null if no selection.
   */
  public static MovingObjectPosition selectStartingBlock(MovingObjectPosition mouseTarget, EntityPlayer player, float partialTick)
  {
    final double MINIMUMHITDISTANCE = 0.5; // minimum distance from the player's eyes (axis-aligned not oblique)
    int blockx, blocky, blockz;
    double playerOriginX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTick;
    double playerOriginY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTick;
    double playerOriginZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTick;

    Vec3 playerLook = player.getLook(partialTick);
    Vec3 playerEyesPos = player.worldObj.getWorldVec3Pool().getVecFromPool(playerOriginX, playerOriginY, playerOriginZ);

    if (mouseTarget == null) {   // no hit
      // we need to find the closest [x,y,z] in the direction the player is looking in, that the player is not occupying.
      // This will depend on the yaw but also the elevation.
      // The algorithm is:
      // (1) calculated an expanded AABB around the player (all sides at least 0.5m from the eyes) and snap it to the next largest enclosing blocks.
      // (2) find the intersection of the look vector with this AABB
      // (3) the selected block is the one just beyond the intersection point

      double AABBminX = Math.floor(Math.min(player.boundingBox.minX, playerOriginX - MINIMUMHITDISTANCE));
      double AABBminY = Math.floor(Math.min(player.boundingBox.minY, playerOriginY - MINIMUMHITDISTANCE));
      double AABBminZ = Math.floor(Math.min(player.boundingBox.minZ, playerOriginZ - MINIMUMHITDISTANCE));
      double AABBmaxX = Math.ceil(Math.max(player.boundingBox.maxX, playerOriginX + MINIMUMHITDISTANCE));
      double AABBmaxY = Math.ceil(Math.max(player.boundingBox.maxY, playerOriginY + MINIMUMHITDISTANCE));
      double AABBmaxZ = Math.ceil(Math.max(player.boundingBox.maxZ, playerOriginZ + MINIMUMHITDISTANCE));

      AxisAlignedBB expandedAABB = AxisAlignedBB.getBoundingBox(AABBminX, AABBminY, AABBminZ,   AABBmaxX, AABBmaxY, AABBmaxZ);

      Vec3 startVec = playerEyesPos.addVector(0, 0, 0);
      Vec3 endVec = playerEyesPos.addVector(playerLook.xCoord * 8.0, playerLook.yCoord * 8.0, playerLook.zCoord * 8.0);

      MovingObjectPosition traceResult = expandedAABB.calculateIntercept(startVec, endVec);
      if (traceResult == null) {  // shouldn't be possible
        return null;
      }

      blockx = MathHelper.floor_double(traceResult.hitVec.xCoord + playerLook.xCoord * 0.001);
      blocky = MathHelper.floor_double(traceResult.hitVec.yCoord + playerLook.yCoord * 0.001);
      blockz = MathHelper.floor_double(traceResult.hitVec.zCoord + playerLook.zCoord * 0.001);
      traceResult = new MovingObjectPosition(blockx, blocky, blockz, Facing.oppositeSide[traceResult.sideHit], traceResult.hitVec);
      traceResult.hitVec = snapLookToBlockFace(traceResult, playerEyesPos);

      return traceResult;

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

      mouseTarget = new MovingObjectPosition(blockx, blocky, blockz, mouseTarget.sideHit, mouseTarget.hitVec);
      mouseTarget.hitVec = snapLookToBlockFace(mouseTarget, playerEyesPos);
      return mouseTarget;

    } else {  // currently only ENTITY
      return null;
    }

  }

  /**
   * Used to create vector from the starting point to the midpoint of the specified side of the block.
   * @param blockPos the [x,y,z] of the target block, and the side.  hitVec is ignored.
   * @param startPos the origin of the vector to be created
   * @return the direction vector, or null for failure
   */
  public static Vec3 snapLookToBlockFace(MovingObjectPosition blockPos, Vec3 startPos)
  {
    // midpoint of each face based on side
    final double[] XfaceOffset = {0.5, 0.5, 0.5, 0.5, 0.0, 1.0};
    final double[] YfaceOffset = {0.0, 1.0, 0.5, 0.5, 0.5, 0.5};
    final double[] ZfaceOffset = {0.5, 0.5, 0.0, 1.0, 0.5, 0.5};

    Vec3 endPos = Vec3.createVectorHelper(blockPos.blockX + XfaceOffset[blockPos.sideHit],
                                          blockPos.blockY + YfaceOffset[blockPos.sideHit],
                                          blockPos.blockZ + ZfaceOffset[blockPos.sideHit]);

    return startPos.subtract(endPos);
  }


  /**
   * selectLine is used to select a straight line of blocks, and return a list of their coordinates.
   * Starting from the startingBlock, the selection will continue in a line parallel to the direction vector, snapped to the six cardinal directions or
   *   alternatively to one of the twenty 45 degree directions (if diagonalOK == true).
   *   If stopWhenCollide == true and the snapped direction points directly into a solid block, the direction will be deflected up to lie flat along the surface
   *   Keeps going until it reaches maxLineLength, y goes outside the valid range, or hits a solid block (and stopWhenCollide is true)
   * @param startingBlock the first block in the straight line
   * @param world       the world
   * @param direction    the direction to extend the selection
   * @param maxLineLength the maximum number of blocks to select
   * @param diagonalOK    if true, diagonal 45 degree lines are allowed
   * @param stopWhenCollide if true, stops when a solid block is encountered (canCollide == true).  Otherwise, continues for maxLineLength
   * @return a list of the coordinates of all blocks in the selection, including the startingBlock.
   */
  public static List<ChunkCoordinates> selectLine(ChunkCoordinates startingBlock, World world, Vec3 direction,
                                                  int maxLineLength, boolean diagonalOK, boolean stopWhenCollide)
  {
    List<ChunkCoordinates> selection = new ArrayList<ChunkCoordinates>();

    if (startingBlock == null) return selection;

    Vec3 snappedCardinalDirection = snapToCardinalDirection(direction, diagonalOK);
    if (snappedCardinalDirection == null) return selection;

    ChunkCoordinates deltaPosition = convertToDelta(snappedCardinalDirection);

    ChunkCoordinates nextCoordinate = new ChunkCoordinates(startingBlock);
    selection.add(startingBlock);
    int blocksCount = 1;
    while (blocksCount < maxLineLength) {
      nextCoordinate.set(nextCoordinate.posX + deltaPosition.posX,
                         nextCoordinate.posY + deltaPosition.posY,
                         nextCoordinate.posZ + deltaPosition.posZ
                        );
      if (nextCoordinate.posY < 0 || nextCoordinate.posY >= 256) break;
      if (stopWhenCollide && isBlockSolid(world, nextCoordinate)) {
        if (blocksCount > 1) break;
        deltaPosition = deflectDirectionVector(world, startingBlock, direction, deltaPosition);
        nextCoordinate.set(startingBlock.posX + deltaPosition.posX, startingBlock.posY + deltaPosition.posY, startingBlock.posZ + deltaPosition.posZ);
        if (isBlockSolid(world, nextCoordinate)) break;
      }
      selection.add(new ChunkCoordinates(nextCoordinate));
      ++blocksCount;
    }

    return selection;
  }

  /**
   * Snaps the given vector to the closest of the six cardinal directions, or alternatively to one of the twenty "45 degree" directions (if diagonalOK == true)
   * @param vectorToSnap the vector to be snapped to a cardinal direction
   * @param diagonalOK if true, diagonal "45 degree" directions are allowed
   * @return the cardinal direction snapped to (unit length vector), or null if input vector is null or zero.
   */
  public static Vec3 snapToCardinalDirection(Vec3 vectorToSnap, boolean diagonalOK)
  {
    final float R2 = 0.707107F;  // 1 / sqrt(2)
    final float R3 = 0.577350F;  // 1 / sqrt(3)
    final float cardinal[][] =   {   {1, 0, 0},      {0, 1, 0},      {0,0,1} };
    final float cardinal45[][] = { {R2, R2, 0},   {-R2, R2, 0},   {R2, 0, R2},  {R2, 0, -R2}, {0, R2, R2}, {0, R2, -R2},
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
   * "deflects" the direction vector so that it doesn't try to penetrate a solid block.
   * for example: if the vector is [0.707, -0.707, 0] and the starting block is sitting on a flat plane:
   *    the direction vector will be "deflected" up to [0.707, 0, 0], converted to [+1, 0, 0] return value, so
   *    that the direction runs along the surface of the plane
   * @param world
   * @param startingBlock - the starting block, should be non-solid (isBlockSolid == false)
   * @param direction - the direction vector to be deflected.
   * @param deltaPosition - the current [deltax, deltay, deltaz] where each delta is -1, 0, or 1
   * @return a [deltax,deltay,deltaz] where each delta is -1, 0, or 1
   */
  public static ChunkCoordinates deflectDirectionVector(World world, ChunkCoordinates startingBlock, Vec3 direction, ChunkCoordinates deltaPosition)
  {
    // algorithm is:
    // if deltaPosition has two or three non-zero components:
    //     re-snap the vector to the six cardinal axes only.  If it still fails, perform further deflection as for deltaPosition with one non-zero component below
    // if deltaPosition has one non-zero component (is parallel to one of the six coordinate axes):
    //     normalise the direction vector to unit length, eliminate the deltaPosition's non-zero axis from the direction vector, verify that at least one of the other two
    //     components is at least 0.1, renormalise and snap the vector to the cardinal axes again.

    int nonZeroCount = Math.abs(deltaPosition.posX) + Math.abs(deltaPosition.posY) + Math.abs(deltaPosition.posZ);
    Vec3 deflectedDirection;
    ChunkCoordinates deflectedDeltaPosition;

    if (nonZeroCount >= 2) {
      deflectedDirection = snapToCardinalDirection(direction, false);
      if (deflectedDirection == null) return new ChunkCoordinates(deltaPosition);

      deflectedDeltaPosition = convertToDelta(deflectedDirection);
      ChunkCoordinates nextCoordinate = new ChunkCoordinates(startingBlock);
      nextCoordinate.set(nextCoordinate.posX + deflectedDeltaPosition.posX, nextCoordinate.posY + deflectedDeltaPosition.posY, nextCoordinate.posZ + deflectedDeltaPosition.posZ);
      if (!isBlockSolid(world, nextCoordinate)) return deflectedDeltaPosition;
    } else {
      deflectedDeltaPosition = new ChunkCoordinates(deltaPosition);
    }

    deflectedDirection = direction.normalize();
    if (deflectedDeltaPosition.posX != 0) {
      deflectedDirection.xCoord = 0.0;
    } else if (deflectedDeltaPosition.posY != 0) {
      deflectedDirection.yCoord = 0.0;
    } else {
      deflectedDirection.zCoord = 0.0;
    }
    deflectedDirection = direction.normalize();
    deflectedDirection = snapToCardinalDirection(direction, false);
    if (deflectedDirection == null) return new ChunkCoordinates(deltaPosition);

    deflectedDeltaPosition = convertToDelta(deflectedDirection);
    return deflectedDeltaPosition;
  }

  /**
   * Converts the unit vector to a [deltax,deltay,deltaz] where each delta is -1, 0, or 1
   * @param vector - valid inputs are unit length vectors parallel to [dx, dy, dz] where d{} is -1, 0, or +1
   * @return a [deltax,deltay,deltaz] where each delta is -1, 0, or 1
   */
  public static ChunkCoordinates convertToDelta(Vec3 vector)
  {
    final float EPSILON = 0.1F;
    ChunkCoordinates deltaPosition = new ChunkCoordinates(0, 0, 0);
    if (vector.xCoord > EPSILON) deltaPosition.posX = 1;
    if (vector.xCoord < -EPSILON) deltaPosition.posX = -1;
    if (vector.yCoord > EPSILON) deltaPosition.posY = 1;
    if (vector.yCoord < -EPSILON) deltaPosition.posY = -1;
    if (vector.zCoord > EPSILON) deltaPosition.posZ = 1;
    if (vector.zCoord < -EPSILON) deltaPosition.posZ = -1;
    return deltaPosition;
  }

  /**
   *  returns true if the block is "solid".
   *  Non-solid appears to correlate with "doesn't interact with a piston" i.e. getMobilityFlag == 1
    * @param world  the world
   * @param blockLocation  the [x,y,z] of the block to be checked
   */
  public static boolean isBlockSolid(World world, ChunkCoordinates blockLocation)
  {
    if (blockLocation.posY < 0 || blockLocation.posY >= 256) return false;
    int blockId = world.getBlockId(blockLocation.posX, blockLocation.posY, blockLocation.posZ);
    if (blockId == 0) {
      return false;
    }
    return (Block.blocksList[blockId].getMobilityFlag() != 1);
  }

}
