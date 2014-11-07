package speedytools.clientside.selections;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import net.minecraft.world.World;
import speedytools.common.selections.FillMatcher;
import speedytools.common.utilities.ErrorLog;

import java.util.*;

/**
* Created with IntelliJ IDEA.
* User: TheGreyGhost
* Date: 28/10/13
* Time: 9:47 PM
* BlockMultiSelector is a group of methods used to select multiple blocks based on where the mouse is pointing.
*/
public class BlockMultiSelector
{
  public enum BlockTypeToSelect {AIR_ONLY, NON_SOLID_OK, SOLID_OK}

  /**
   * selectStartingBlock is used to select a starting block based on the player's position and look
   * There are three distinct cases for the starting block:
   * (1) the mouse is not on any target: the first block selected will be the one corresponding to the line of sight from the player's head:
   *     a) which doesn't intersect the player's bounding box
   *     b) which is at least 0.5 m from the player's eyes in each of the the x, y, and z directions.
   * (2) the mouse is on a tile target: the first block selected will be according to blockTypeToSelect
   * (3) the mouse is on an entity: no selection.
   * The method also returns the look vector snapped to the midpoint of the face that was hit on the selected Block
   * @param mouseTarget  where the cursor is currently pointed
   * @param blockTypeToSelect the types of blocks that can be selected.  If the cursor is on a block that doesn't meet the criteria, the one adjacent to the tile
   *                          will be used, i.e. on the face in mouseTarget.
   *     AIR = air blocks only
   *     NON_SOLID = if the block is not "solid" (eg flowers, grass, snow, redstone, etc)
   *     SOLID =  if the block is "solid" (can't be walked through)
   * @param player       the player (used for position and look information)
   * @param partialTick  used for calculating player head position
   * @return the coordinates of the starting selection block plus the side hit plus the look vector snapped to the midpoint of
   *         side hit.  null if no selection.
   */
  public static MovingObjectPosition selectStartingBlock(MovingObjectPosition mouseTarget, BlockTypeToSelect blockTypeToSelect, EntityPlayer player, float partialTick)
  {
    final double MINIMUMHITDISTANCE = 0.5; // minimum distance from the player's eyes (axis-aligned not oblique)
    int blockx, blocky, blockz;
    double playerOriginX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTick;
    double playerOriginY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTick;
    double playerOriginZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTick;

    Vec3 playerLook = player.getLook(partialTick);
    Vec3 playerEyesPos = Vec3.createVectorHelper(playerOriginX, playerOriginY, playerOriginZ);

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
      traceResult.hitVec = playerLook;
//      traceResult.hitVec = snapLookToBlockFace(traceResult, playerEyesPos);

      return traceResult;

    } else if (mouseTarget.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
      boolean pullback = false;
      switch (blockTypeToSelect) {
        case AIR_ONLY: {
          pullback = true;
          break;
        }
        case NON_SOLID_OK: {
          pullback = isBlockSolid(player.worldObj, new ChunkCoordinates(mouseTarget.blockX, mouseTarget.blockY, mouseTarget.blockZ));
          break;
        }
        case SOLID_OK: {
          pullback = false;
        }
        default: {
          ErrorLog.defaultLog().debug("Illegal BlockTypeToSelect:" + blockTypeToSelect);
        }
      }
      if (pullback) {
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
   * @return a list of the coordinates of all blocks in the selection, including the startingBlock.  May be zero length if the startingBlock is null
   */
  public static List<ChunkCoordinates> selectLine(ChunkCoordinates startingBlock, World world, Vec3 direction,
                                                  int maxLineLength, boolean diagonalOK, CollisionOptions stopWhenCollide)
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
      if ((stopWhenCollide == CollisionOptions.STOP_WHEN_SOLID_BLOCK_REACHED) && isBlockSolid(world, nextCoordinate)) {
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
   * selectContour is used to select a contoured line of blocks, and return a list of their coordinates.
   * Starting from the block identified by mouseTarget, the selection will attempt to follow any contours in the same plane as the side hit.
   * (for example: if there is a zigzagging wall, it will select the layer of blocks that follows the top of the wall.)
   * Depending on fillMatcher, it will either select the non-solid blocks on top of the contour (to make the wall "taller"), or
   *   select the solid blocks that form the top layer of the contour (to remove the top layer of the wall).
   * depending on diagonalOK it will follow diagonals or only the cardinal directions.
   * Keeps going until it reaches maxBlockCount, y goes outside the valid range, or hits a solid block.  The search algorithm is to look for closest blocks first
   *   ("closest" meaning the shortest distance travelled along the contour being created)
   * @param startingBlockPosition the block to start from
   * @param world       the world
   * @param maxBlockCount the maximum number of blocks to select
   * @param diagonalOK    if true, diagonal 45 degree lines are allowed
   * @param fillMatcher   the matcher used to determine which blocks should be added to the fill
   * @param normalDirection specifies the plane that will be searched in (Facing directions; specifies the normal to the plane)
   * @return a list of the coordinates of all blocks in the selection, including the mouseTarget block.   Will be empty if the mouseTarget is not a tile.
   */

  public static List<ChunkCoordinates> selectContour(ChunkCoordinates startingBlockPosition, World world,
                                                     int maxBlockCount, boolean diagonalOK, FillMatcher fillMatcher, int normalDirection)
  {
    // lookup table to give the possible search directions for any given search plane
    //  row index 0 = xz plane, 1 = xy plane, 2 = yz plane
    //  column index = the eight directions within the plane (even = cardinal, odd = diagonal)
    final int PLANE_XZ = 0;
    final int PLANE_XY = 1;
    final int PLANE_YZ = 2;
    final int searchDirectionsX[][] = {  {+0, -1, -1, -1, +0, +1, +1, +1},
            {+0, -1, -1, -1, +0, +1, +1, +1},
            {+0, +0, +0, +0, +0, +0, +0, +0}
    };
    final int searchDirectionsY[][] = {  {+0, +0, +0, +0, +0, +0, +0, +0},
            {+1, +1, +0, -1, -1, -1, +0, +1},
            {+1, +1, +0, -1, -1, -1, +0, +1}
    };
    final int searchDirectionsZ[][] = {  {+1, +1, +0, -1, -1, -1, +0, +1},
            {+0, +0, +0, +0, +0, +0, +0, +0},
            {+0, -1, -1, -1, +0, +1, +1, +1}
    };

    List<ChunkCoordinates> selection = new ArrayList<ChunkCoordinates>();

//    if (mouseTarget == null || mouseTarget.typeOfHit !=  MovingObjectPosition.MovingObjectType.BLOCK) return selection;

//    ChunkCoordinates startingBlock = new ChunkCoordinates();
    int searchPlane;
    switch (normalDirection) {   //  Bottom = 0, Top = 1, East = 2, West = 3, North = 4, South = 5.
      case 0:
      case 1:
        searchPlane = PLANE_XZ;
        break;
      case 2:
      case 3:
        searchPlane = PLANE_XY;
        break;
      case 4:
      case 5:
        searchPlane = PLANE_YZ;
        break;
      default: return selection;  // illegal value so return nothing
    }

    // first step is to identify the starting block depending on whether this is an additive contour or subtractive contour,

//    EnumFacing blockInFront = EnumFacing.getFront(mouseTarget.sideHit);
//    startingBlock.posX = mouseTarget.blockX;
//    startingBlock.posY = mouseTarget.blockY;
//    startingBlock.posZ = mouseTarget.blockZ;
//    if (selectAdditiveContour && isBlockSolid(world, startingBlock)) {
//      startingBlock.posX += blockInFront.getFrontOffsetX();
//      startingBlock.posY += blockInFront.getFrontOffsetY();
//      startingBlock.posZ += blockInFront.getFrontOffsetZ();
//    }
    selection.add(startingBlockPosition);

    final int INITIAL_CAPACITY = 128;
    Set<ChunkCoordinates> locationsFilled = new HashSet<ChunkCoordinates>(INITIAL_CAPACITY);                                   // locations which have been selected during the fill
    Deque<SearchPosition> currentSearchPositions = new LinkedList<SearchPosition>();
    Deque<SearchPosition> nextDepthSearchPositions = new LinkedList<SearchPosition>();
    ChunkCoordinates checkPosition = new ChunkCoordinates(0,0,0);
//    ChunkCoordinates checkPositionSupport = new ChunkCoordinates(0,0,0);

    locationsFilled.add(startingBlockPosition);
    currentSearchPositions.add(new SearchPosition(startingBlockPosition));

    // algorithm is:
    //   for each block in the list of search positions, iterate through each adjacent block to see whether it meets the criteria for expansion:
    //     a) is not solid (for additive contours) or is solid (for subtractive contours)
    //     b) hasn't been filled already during this contour search
    //     c) for additive contours: if it is "supported" by a solid block (eg in the case of sideHit = top face, then test whether the block at [x,y-1,z] is solid
    //   if the criteria are met, select the block and add it to the list of blocks to be search next round.
    //   if the criteria aren't met, keep trying other directions from the same position until all positions are searched.  Then delete the search position and move onto the next.
    //   This will ensure that the fill spreads evenly out from the starting point.

    while (!currentSearchPositions.isEmpty() && selection.size() < maxBlockCount) {
      SearchPosition currentSearchPosition = currentSearchPositions.getFirst();
      checkPosition.set(currentSearchPosition.chunkCoordinates.posX + searchDirectionsX[searchPlane][currentSearchPosition.nextSearchDirection],
                        currentSearchPosition.chunkCoordinates.posY + searchDirectionsY[searchPlane][currentSearchPosition.nextSearchDirection],
                        currentSearchPosition.chunkCoordinates.posZ + searchDirectionsZ[searchPlane][currentSearchPosition.nextSearchDirection]
                       );
      if (!locationsFilled.contains(checkPosition)) {
        FillMatcher.MatchResult matchResult = fillMatcher.matches(world, checkPosition.posX, checkPosition.posY, checkPosition.posZ);

//        if (selectAdditiveContour) {
//          if (!isBlockSolid(world, checkPosition)) {
//            checkPositionSupport.set(checkPosition.posX - blockInFront.getFrontOffsetX(),      // block behind
//                                     checkPosition.posY - blockInFront.getFrontOffsetY(),
//                                     checkPosition.posZ - blockInFront.getFrontOffsetZ()
//                                    );
//            blockIsSuitable = isBlockSolid(world, checkPositionSupport);
//          }
//        } else { // subtractive contour
//          blockIsSuitable = isBlockSolid(world, checkPosition);
//        }
        if (matchResult == FillMatcher.MatchResult.MATCH) {
          ChunkCoordinates newChunkCoordinate = new ChunkCoordinates(checkPosition);
          SearchPosition nextSearchPosition = new SearchPosition(newChunkCoordinate);
          nextDepthSearchPositions.addLast(nextSearchPosition);
          locationsFilled.add(newChunkCoordinate);
          selection.add(newChunkCoordinate);
        }
      }
      currentSearchPosition.nextSearchDirection += diagonalOK ? 1 : 2;  // no diagonals -> even numbers only
      if (currentSearchPosition.nextSearchDirection >= 8) {
        currentSearchPositions.removeFirst();
        if (currentSearchPositions.isEmpty()) {
          Deque<SearchPosition> temp = currentSearchPositions;
          currentSearchPositions = nextDepthSearchPositions;
          nextDepthSearchPositions = temp;
        }
      }
    }

    return selection;
  }

  /**
   * selectFill is used to select a flood fill of blocks which match the starting block, and return a list of their coordinates.
   * Starting from the block identified by mouseTarget, the selection will flood fill out in three directions
   * depending on diagonalOK it will follow diagonals or only the cardinal directions.
   * Keeps going until it reaches maxBlockCount, y goes outside the valid range.  The search algorithm is to look for closest blocks first
   *   ("closest" meaning the shortest distance travelled along the blob being created)
   *
   * @param world       the world
   * @param maxBlockCount the maximum number of blocks to select
   * @param diagonalOK    if true, diagonal 45 degree lines are allowed
   * @param fillMatcher the matcher used to determine which blocks should be added to the fill
   * @param xMin  the fill will not extend below xMin.  Likewise it will not extend above xMax.  Similar for y, z.
   */
  public static List<ChunkCoordinates> selectFillBounded(ChunkCoordinates fillStartPosition, World world,
                                                         int maxBlockCount, boolean diagonalOK, FillMatcher fillMatcher,
                                                         int xMin, int xMax, int yMin, int yMax, int zMin, int zMax)
  {
    // lookup table to give the possible search directions for non-diagonal and diagonal respectively
    final int NON_DIAGONAL_DIRECTIONS = 6;
    final int ALL_DIRECTIONS = 26;
    final int searchDirectionsX[] = {+0, +0, +0, +0, -1, +1,   // non-diagonal
                                     +1, +0, -1, +0,   +1, +1, -1, -1,   +1, +0, -1, +0,  // top, middle, bottom "edge" blocks
                                     +1, +1, -1, -1,   +1, +1, -1, -1                   // top, bottom "corner" blocks
    };
    final int searchDirectionsY[] = {-1, +1, +0, +0, +0, +0,   // non-diagonal
                                     +1, +1, +1, +1,   +0, +0, +0, +0,   -1, -1, -1, -1,   // top, middle, bottom "edge" blocks
                                     +1, +1, +1, +1,   -1, -1, -1, -1                      // top, bottom "corner" blocks
    };

    final int searchDirectionsZ[] = {+0, +0, -1, +1, +0, +0,   // non-diagonal
                                     +0, -1, +0, +1,   +1, -1, -1, +1,   +0, -1, +0, +1,   // top, middle, bottom "edge" blocks
                                     +1, -1, -1, +1,   +1, -1, -1, +1
    };

    List<ChunkCoordinates> selection = new ArrayList<ChunkCoordinates>();

    ChunkCoordinates startingBlock = new ChunkCoordinates(fillStartPosition);
    if (FillMatcher.MatchResult.MATCH != fillMatcher.matches(world, fillStartPosition.posX, fillStartPosition.posY, fillStartPosition.posZ)) {
      return selection;
    }
    selection.add(startingBlock);

//    Block blockToReplace = world.getBlock(startingBlock.posX, startingBlock.posY, startingBlock.posZ);
//    int blockToReplaceMetadata = world.getBlockMetadata(startingBlock.posX, startingBlock.posY, startingBlock.posZ);

    final int INITIAL_CAPACITY = 128;
    Set<ChunkCoordinates> locationsFilled = new HashSet<ChunkCoordinates>(INITIAL_CAPACITY);                                   // locations which have been selected during the fill
    Deque<SearchPosition> currentSearchPositions = new LinkedList<SearchPosition>();
    Deque<SearchPosition> nextDepthSearchPositions = new LinkedList<SearchPosition>();
    ChunkCoordinates checkPosition = new ChunkCoordinates(0,0,0);
    ChunkCoordinates checkPositionSupport = new ChunkCoordinates(0,0,0);

    locationsFilled.add(startingBlock);
    currentSearchPositions.add(new SearchPosition(startingBlock));

    // algorithm is:
    //   for each block in the list of search positions, iterate through each adjacent block to see whether it meets the criteria for expansion:
    //     a) matches the block-to-be-replaced (if matchAnyNonAir: non-air, otherwise if blockID and metaData match.  For lava or water metadata doesn't need to match).
    //     b) hasn't been filled already during this contour search
    //   if the criteria are met, select the block and add it to the list of blocks to be search next round.
    //   if the criteria aren't met, keep trying other directions from the same position until all positions are searched.  Then delete the search position and move onto the next.
    //   This will ensure that the fill spreads evenly out from the starting point.   Check the boundary to stop fill spreading outside it.

    while (!currentSearchPositions.isEmpty() && selection.size() < maxBlockCount) {
      SearchPosition currentSearchPosition = currentSearchPositions.getFirst();
      checkPosition.set(currentSearchPosition.chunkCoordinates.posX + searchDirectionsX[currentSearchPosition.nextSearchDirection],
                        currentSearchPosition.chunkCoordinates.posY + searchDirectionsY[currentSearchPosition.nextSearchDirection],
                        currentSearchPosition.chunkCoordinates.posZ + searchDirectionsZ[currentSearchPosition.nextSearchDirection]
                        );
      if (    checkPosition.posX >= xMin && checkPosition.posX <= xMax
          &&  checkPosition.posY >= yMin && checkPosition.posY <= yMax
          &&  checkPosition.posZ >= zMin && checkPosition.posZ <= zMax
          && !locationsFilled.contains(checkPosition)) {
        FillMatcher.MatchResult matchResult = fillMatcher.matches(world, checkPosition.posX, checkPosition.posY, checkPosition.posZ);
//        Block blockToCheck = world.getBlock();
//
//        if (matchAnyNonAir && blockToCheck != Blocks.air) {
//          blockIsSuitable = true;
//        } else if (blockToCheck == blockToReplace) {
//          if (world.getBlockMetadata(checkPosition.posX, checkPosition.posY, checkPosition.posZ) == blockToReplaceMetadata) {
//            blockIsSuitable = true;
//          } else {
//            if (blockToCheck.getMaterial() == Material.lava
//                || blockToCheck.getMaterial() == Material.water) {
//              blockIsSuitable = true;
//            }
//          }
//        }

        if (matchResult == FillMatcher.MatchResult.MATCH) {
          ChunkCoordinates newChunkCoordinate = new ChunkCoordinates(checkPosition);
          SearchPosition nextSearchPosition = new SearchPosition(newChunkCoordinate);
          nextDepthSearchPositions.addLast(nextSearchPosition);
          locationsFilled.add(newChunkCoordinate);
          selection.add(newChunkCoordinate);
        }
      }
      ++currentSearchPosition.nextSearchDirection;
      if (currentSearchPosition.nextSearchDirection >= (diagonalOK ? ALL_DIRECTIONS : NON_DIAGONAL_DIRECTIONS)) {
        currentSearchPositions.removeFirst();
        if (currentSearchPositions.isEmpty()) {
          Deque<SearchPosition> temp = currentSearchPositions;
          currentSearchPositions = nextDepthSearchPositions;
          nextDepthSearchPositions = temp;
        }
      }
    }

    return selection;
  }

  /**
   * see selectFill above
   * @param world
   * @param maxBlockCount
   * @param diagonalOK
   * @return
   */
  public static List<ChunkCoordinates> selectFillUnbounded(ChunkCoordinates fillStartPosition, World world,
                                                           int maxBlockCount, boolean diagonalOK, FillMatcher fillMatcher)
  {
    return selectFillBounded(fillStartPosition, world, maxBlockCount, diagonalOK, fillMatcher,
            Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 255, Integer.MIN_VALUE, Integer.MAX_VALUE);
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
   * @param world the world
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
    deflectedDirection = deflectedDirection.normalize();
    deflectedDirection = snapToCardinalDirection(deflectedDirection, false);
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
   *  returns true if the block is "solid" or is water.
   *  Non-solid appears to correlate with "doesn't interact with a piston" i.e. getMobilityFlag == 1
    * @param world  the world
   * @param blockLocation  the [x,y,z] of the block to be checked
   */
  public static boolean isBlockSolid(World world, ChunkCoordinates blockLocation)
  {
    if (blockLocation.posY < 0 || blockLocation.posY >= 256) return false;
    Block block = world.getBlock(blockLocation.posX, blockLocation.posY, blockLocation.posZ);
    if (block == Blocks.air) {
      return false;
    }
    return (block.getMaterial() == Material.water || block.getMobilityFlag() != 1);
  }

  /**
   * SearchPosition contains the coordinates of a block and the current direction in which to search.
   */
  public static class SearchPosition
  {
    public SearchPosition(ChunkCoordinates initChunkCoordinates) {
      chunkCoordinates = initChunkCoordinates;
      nextSearchDirection = 0;
    }
    public ChunkCoordinates chunkCoordinates;
    public int nextSearchDirection;
  }

  public static enum CollisionOptions {
    STOP_WHEN_SOLID_BLOCK_REACHED, CONTINUE_THROUGH_SOLID_BLOCKS
  }

}
