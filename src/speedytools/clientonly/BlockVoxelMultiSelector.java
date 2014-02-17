package speedytools.clientonly;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.util.*;

/**
 * User: The Grey Ghost
 * Date: 17/02/14
 */
public class BlockVoxelMultiSelector
{


  /**
   * selectFill is used to select a flood fill of blocks which match the starting block, and return a list of their coordinates.
   * Starting from the block identified by mouseTarget, the selection will flood fill out in three directions
   * depending on diagonalOK it will follow diagonals or only the cardinal directions.
   * Keeps going until it reaches maxBlockCount, y goes outside the valid range.  The search algorithm is to look for closest blocks first
   *   ("closest" meaning the shortest distance travelled along the blob being created)
   *
   * @param mouseTarget the block under the player's cursor.  Uses [x,y,z]
   * @param world       the world
   * @param maxBlockCount the maximum number of blocks to select
   * @param diagonalOK    if true, diagonal 45 degree lines are allowed
   * @param matchAnyNonAir
   * @param xMin  the fill will not extend below xMin.  Likewise it will not extend above xMax.  Similar for y, z.
   */
  public static List<ChunkCoordinates> selectFill(MovingObjectPosition mouseTarget, World world,
                                                  int maxBlockCount, boolean diagonalOK, boolean matchAnyNonAir,
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

    if (mouseTarget == null || mouseTarget.typeOfHit != EnumMovingObjectType.TILE) return selection;

    ChunkCoordinates startingBlock = new ChunkCoordinates();
    startingBlock.posX = mouseTarget.blockX;
    startingBlock.posY = mouseTarget.blockY;
    startingBlock.posZ = mouseTarget.blockZ;
    selection.add(startingBlock);

    int blockToReplaceID = world.getBlockId(startingBlock.posX, startingBlock.posY, startingBlock.posZ);
    int blockToReplaceMetadata = world.getBlockMetadata(startingBlock.posX, startingBlock.posY, startingBlock.posZ);

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
        boolean blockIsSuitable = false;

        int blockToCheckID = world.getBlockId(checkPosition.posX, checkPosition.posY, checkPosition.posZ);

        if (matchAnyNonAir && blockToCheckID != 0) {
          blockIsSuitable = true;
        } else if (blockToCheckID == blockToReplaceID) {
          if (world.getBlockMetadata(checkPosition.posX, checkPosition.posY, checkPosition.posZ) == blockToReplaceMetadata) {
            blockIsSuitable = true;
          } else {
            if (Block.blocksList[blockToCheckID].blockMaterial == Material.lava
                    || Block.blocksList[blockToCheckID].blockMaterial == Material.water) {
              blockIsSuitable = true;
            }
          }
        }

        if (blockIsSuitable) {
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
   * @param mouseTarget
   * @param world
   * @param maxBlockCount
   * @param diagonalOK
   * @return
   */
  public static List<ChunkCoordinates> selectFill(MovingObjectPosition mouseTarget, World world,
                                                  int maxBlockCount, boolean diagonalOK)
  {
    return selectFill(mouseTarget, world, maxBlockCount, diagonalOK, false,
            Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 255, Integer.MIN_VALUE, Integer.MAX_VALUE);
  }


}
