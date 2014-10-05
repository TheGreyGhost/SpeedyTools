package speedytools.serverside.ingametester;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import speedytools.common.selections.BlockVoxelMultiSelector;
import speedytools.common.selections.VoxelSelection;
import speedytools.common.selections.VoxelSelectionWithOrigin;
import speedytools.common.utilities.ErrorLog;
import speedytools.serverside.worldmanipulation.WorldFragment;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;

/**
 * User: The Grey Ghost
 * Date: 5/10/2014
 * Used to test the VoxelChunkwiseFillIterator against the old fill algorithm (should give same results)
 */
public class SelectionFillTester
{

  // for each of the test regions:
  // 1) generates an old-method fill, starting from the [0,0,0] corner of the source region
  // 2) copies that fill selection from the source region to the expected outcome
  // 3) generates a new-method fill, starting from the [0,0,0] corner of the source region
  // 4) compares the old selection to the new selection

  public boolean runFillTests(EntityPlayerMP entityPlayerMP, boolean performTest)
  {
    WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(0);

    final int XORIGIN = 128; final int YORIGIN = 4; final int ZORIGIN = 0;
    final int XSIZE = 48; final int YSIZE = 48; final int ZSIZE = 48;

    final int NUMBER_OF_TEST_REGIONS = 4;
    ArrayList<InGameTester.TestRegions> testRegions = new ArrayList<InGameTester.TestRegions>();
    for (int i = 0; i < NUMBER_OF_TEST_REGIONS; ++i) {
      InGameTester.TestRegions newRegion = new InGameTester.TestRegions(XORIGIN, YORIGIN, ZORIGIN + i * ZSIZE, XSIZE, YSIZE, ZSIZE, true);
      if (!performTest) {
        newRegion.drawAllTestRegionBoundaries();
        WorldFragment worldFragmentBlank = new WorldFragment(newRegion.xSize, newRegion.ySize, newRegion.zSize);
        worldFragmentBlank.readFromWorld(worldServer, newRegion.testRegionInitialiser.posX, newRegion.testRegionInitialiser.posY, newRegion.testRegionInitialiser.posZ, null);
        worldFragmentBlank.writeToWorld(worldServer, newRegion.testOutputRegion.posX, newRegion.testOutputRegion.posY, newRegion.testOutputRegion.posZ, null);
        worldFragmentBlank.writeToWorld(worldServer, newRegion.expectedOutcome.posX, newRegion.expectedOutcome.posY, newRegion.expectedOutcome.posZ, null);
      }
      testRegions.add(newRegion);
    }
    if (!performTest) return true;

    int testRegionNumber = 0;
    for (InGameTester.TestRegions testRegion : testRegions) {
      ChunkCoordinates corner1 = new ChunkCoordinates(testRegion.sourceRegion.posX, testRegion.sourceRegion.posY, testRegion.sourceRegion.posZ);
      ChunkCoordinates corner2 = new ChunkCoordinates(testRegion.sourceRegion.posX + testRegion.xSize,
                                                      testRegion.sourceRegion.posY + testRegion.ySize,
              testRegion.sourceRegion.posZ + testRegion.zSize);
      ChunkCoordinates blockUnderCursor = corner1;
      selectBoundFillStart(worldServer, blockUnderCursor, corner1, corner2);
      selectFillContinue(worldServer, Long.MAX_VALUE);
      ChunkCoordinates origin = getWorldOrigin();
      VoxelSelectionWithOrigin oldSelection = new VoxelSelectionWithOrigin(origin.posX, origin.posY, origin.posZ, getSelection());
      WorldFragment worldFragmentOld = new WorldFragment(oldSelection.getxSize(), oldSelection.getySize(), oldSelection.getzSize());
      worldFragmentOld.readFromWorld(worldServer, testRegion.sourceRegion.posX, testRegion.sourceRegion.posY, testRegion.sourceRegion.posZ,
              oldSelection);
      worldFragmentOld.writeToWorld(worldServer, testRegion.testOutputRegion.posX, testRegion.testOutputRegion.posY, testRegion.testOutputRegion.posZ,
              null);

      BlockVoxelMultiSelector blockVoxelMultiSelector = new BlockVoxelMultiSelector();
      blockVoxelMultiSelector.selectBoundFillStartNew(worldServer, blockUnderCursor, corner1, corner2);
      blockVoxelMultiSelector.continueSelectionGenerationNEW(worldServer, Long.MAX_VALUE);

      VoxelSelection selectionNew = blockVoxelMultiSelector.getSelection();
      VoxelSelection selectionOld = getSelection();

      if (!selectionNew.containsAllOfThisMask(selectionOld)) {
        ErrorLog.defaultLog().debug("Test region " + testRegionNumber + " failed; New didn't contain all of Old");
        return false;
      }
      if (!selectionOld.containsAllOfThisMask(selectionNew)) {
        ErrorLog.defaultLog().debug("Test region " + testRegionNumber + " failed; Old didn't contain all of New");
        return false;
      }

      ++testRegionNumber;
    }
    return true;
  }

  public VoxelSelection getSelection() {
    return selection;
  }

  public VoxelSelectionWithOrigin getUnavailableVoxels() {
    return unavailableVoxels;
  }

  public boolean containsUnavailableVoxels() {
    return containsUnavailableVoxels;
  }

  /**
   * initialise conversion of the selected fill to a VoxelSelection
   * From the starting block, performs a flood fill on all non-air blocks.
   * Will not fill any blocks with y less than the blockUnderCursor.
   *
   * @param world
   * @param blockUnderCursor the block being highlighted by the cursor
   */
  public void selectUnboundFillStart(World world, ChunkCoordinates blockUnderCursor) {
    ChunkCoordinates corner1 = new ChunkCoordinates();
    ChunkCoordinates corner2 = new ChunkCoordinates();
    final int BORDER_ALLOWANCE = 2;
    corner1.posX = blockUnderCursor.posX - VoxelSelection.MAX_X_SIZE / 2 + BORDER_ALLOWANCE;
    corner2.posX = blockUnderCursor.posX + VoxelSelection.MAX_X_SIZE / 2 - BORDER_ALLOWANCE;
    corner1.posY = blockUnderCursor.posY;
    corner2.posY = Math.min(255, blockUnderCursor.posY + VoxelSelection.MAX_Y_SIZE - 2 * BORDER_ALLOWANCE);
    corner1.posZ = blockUnderCursor.posZ - VoxelSelection.MAX_Z_SIZE / 2 + BORDER_ALLOWANCE;
    corner2.posZ = blockUnderCursor.posZ + VoxelSelection.MAX_Z_SIZE / 2 - BORDER_ALLOWANCE;

    selectBoundFillStart(world, blockUnderCursor, corner1, corner2);
  }

  /**
   * initialise conversion of the selected fill to a VoxelSelection
   * From the starting block, performs a flood fill on all non-air blocks.
   * Will not fill any blocks outside of the box defined by corner1 and corner2
   *
   * @param world
   * @param blockUnderCursor the block being highlighted by the cursor
   */
  public void selectBoundFillStart(World world, ChunkCoordinates blockUnderCursor, ChunkCoordinates corner1, ChunkCoordinates corner2) {
    initialiseSelectionSizeFromBoundary(corner1, corner2);
    assert (blockUnderCursor.posX >= wxOrigin && blockUnderCursor.posY >= wyOrigin && blockUnderCursor.posZ >= wzOrigin);
    assert (blockUnderCursor.posX < wxOrigin + xSize && blockUnderCursor.posY < wyOrigin + ySize && blockUnderCursor.posZ < wzOrigin + zSize);
    mode = OperationInProgress.FILL;
    initialiseVoxelRange();
    ChunkCoordinates startingBlockCopy = new ChunkCoordinates(blockUnderCursor.posX - wxOrigin, blockUnderCursor.posY - wyOrigin, blockUnderCursor.posZ - wzOrigin);
    currentSearchPositions.clear();
    nextDepthSearchPositions.clear();
    currentSearchPositions.add(new SearchPosition(startingBlockCopy));
    selection.setVoxel(startingBlockCopy.posX, startingBlockCopy.posY, startingBlockCopy.posZ);
    expandVoxelRange(startingBlockCopy.posX, startingBlockCopy.posY, startingBlockCopy.posZ);
    blocksAddedCount = 0;
  }

  /**
   * returns true if there are no solid pixels at all in this selection.
   *
   * @return
   */
  public boolean isEmpty() {
    return empty;
  }

  /**
   * write the current selection in serialised form to a ByteArray
   *
   * @return the byte array, or null for failure
   */
  public ByteArrayOutputStream writeToBytes() {
    return selection.writeToBytes();
  }

  /**
   * gets the origin for the selection in world coordinates
   *
   * @return the origin for the selection in world coordinates
   */
  public ChunkCoordinates getWorldOrigin() {
    return new ChunkCoordinates(selection.getWxOrigin(), selection.getWyOrigin(), selection.getWzOrigin());
  }

  public static class SearchPosition
  {
    public SearchPosition(ChunkCoordinates initChunkCoordinates) {
      chunkCoordinates = initChunkCoordinates;
      nextSearchDirection = 0;
    }

    public ChunkCoordinates chunkCoordinates;
    public int nextSearchDirection;
  }

  /**
   * continue conversion of the selected box to a VoxelSelection.  Call repeatedly until conversion complete.
   *
   * @param world
   * @param maxTimeInNS maximum elapsed duration before processing stops & function returns
   * @return fraction complete (0 - 1), -ve number for finished
   */
  private float selectFillContinue(World world, long maxTimeInNS) {
    if (mode != OperationInProgress.FILL) {
      FMLLog.severe("Mode should be FILL in BlockVoxelMultiSelector::selectFillContinue");
      return -1;
    }

    long startTime = System.nanoTime();

    // lookup table to give the possible search directions for non-diagonal and diagonal respectively
    final int NON_DIAGONAL_DIRECTIONS = 6;
    final int ALL_DIRECTIONS = 26;
    final int searchDirectionsX[] = {+0, +0, +0, +0, -1, +1,   // non-diagonal
            +1, +0, -1, +0, +1, +1, -1, -1, +1, +0, -1, +0,  // top, middle, bottom "edge" blocks
            +1, +1, -1, -1, +1, +1, -1, -1                   // top, bottom "corner" blocks
    };
    final int searchDirectionsY[] = {-1, +1, +0, +0, +0, +0,   // non-diagonal
            +1, +1, +1, +1, +0, +0, +0, +0, -1, -1, -1, -1,   // top, middle, bottom "edge" blocks
            +1, +1, +1, +1, -1, -1, -1, -1                      // top, bottom "corner" blocks
    };

    final int searchDirectionsZ[] = {+0, +0, -1, +1, +0, +0,   // non-diagonal
            +0, -1, +0, +1, +1, -1, -1, +1, +0, -1, +0, +1,   // top, middle, bottom "edge" blocks
            +1, -1, -1, +1, +1, -1, -1, +1
    };

    ChunkCoordinates checkPosition = new ChunkCoordinates(0, 0, 0);
    ChunkCoordinates checkPositionSupport = new ChunkCoordinates(0, 0, 0);

    // algorithm is:
    //   for each block in the list of search positions, iterate through each adjacent block to see whether it meets the criteria for expansion:
    //     a) matches the block-to-be-replaced (if matchAnyNonAir: non-air, otherwise if blockID and metaData match.  For lava or water metadata doesn't need to match).
    //     b) hasn't been filled already during this contour search
    //   if the criteria are met, select the block and add it to the list of blocks to be search next round.
    //   if the criteria aren't met, keep trying other directions from the same position until all positions are searched.  Then delete the search position and move onto the next.
    //   This will ensure that the fill spreads evenly out from the starting point.   Check the boundary to stop fill spreading outside it.
    //todo containsUnavailableVoxels = true; if any unavailable found
    while (!currentSearchPositions.isEmpty()) {
      SearchPosition currentSearchPosition = currentSearchPositions.getFirst();
      checkPosition.set(currentSearchPosition.chunkCoordinates.posX + searchDirectionsX[currentSearchPosition.nextSearchDirection],
              currentSearchPosition.chunkCoordinates.posY + searchDirectionsY[currentSearchPosition.nextSearchDirection],
              currentSearchPosition.chunkCoordinates.posZ + searchDirectionsZ[currentSearchPosition.nextSearchDirection]);
      if (checkPosition.posX >= 0 && checkPosition.posX < xSize
              && checkPosition.posY >= 0 && checkPosition.posY < ySize
              && checkPosition.posZ >= 0 && checkPosition.posZ < zSize
              && !selection.getVoxel(checkPosition.posX, checkPosition.posY, checkPosition.posZ)) {
        boolean blockIsAir = world.isAirBlock(checkPosition.posX + wxOrigin, checkPosition.posY + wyOrigin, checkPosition.posZ + wzOrigin);
        if (!blockIsAir) {
          ChunkCoordinates newChunkCoordinate = new ChunkCoordinates(checkPosition);
          SearchPosition nextSearchPosition = new SearchPosition(newChunkCoordinate);
          nextDepthSearchPositions.addLast(nextSearchPosition);
          selection.setVoxel(checkPosition.posX, checkPosition.posY, checkPosition.posZ);
          expandVoxelRange(checkPosition.posX, checkPosition.posY, checkPosition.posZ);
          ++blocksAddedCount;
        }
      }
      ++currentSearchPosition.nextSearchDirection;
      if (currentSearchPosition.nextSearchDirection >= ALL_DIRECTIONS) {
        currentSearchPositions.removeFirst();
        if (currentSearchPositions.isEmpty()) {
          Deque<SearchPosition> temp = currentSearchPositions;
          currentSearchPositions = nextDepthSearchPositions;
          nextDepthSearchPositions = temp;
        }
      }
      if (System.nanoTime() - startTime >= maxTimeInNS) {   // completion fraction is hard to predict, so use a logarithmic function instead to provide some visual movement regardless of size
        if (blocksAddedCount == 0) return 0;
        double fillFraction = blocksAddedCount / (double) (xSize * ySize * zSize);

        final double FULL_SCALE = Math.log(1.0 / (xSize * ySize * zSize)) - 1;
        double fractionComplete = (1 - Math.log(fillFraction) / FULL_SCALE);
        return (float) fractionComplete;
      }
    }

    mode = OperationInProgress.COMPLETE;
    shrinkToSmallestEnclosingCuboid();
    return -1;
  }

  private void initialiseVoxelRange() {
    smallestVoxelX = xSize;
    largestVoxelX = -1;
    smallestVoxelY = ySize;
    largestVoxelY = -1;
    smallestVoxelZ = zSize;
    largestVoxelZ = -1;
    empty = true;
    containsUnavailableVoxels = false;
  }

  private void expandVoxelRange(int x, int y, int z) {
    smallestVoxelX = Math.min(smallestVoxelX, x);
    smallestVoxelY = Math.min(smallestVoxelY, y);
    smallestVoxelZ = Math.min(smallestVoxelZ, z);
    largestVoxelX = Math.max(largestVoxelX, x);
    largestVoxelY = Math.max(largestVoxelY, y);
    largestVoxelZ = Math.max(largestVoxelZ, z);
    empty = false;
  }

  /**
   * shrinks the voxel selection to the minimum size needed to contain the set voxels
   */
  private void shrinkToSmallestEnclosingCuboid() {
    if (smallestVoxelX == 0 && smallestVoxelY == 0 && smallestVoxelZ == 0
            && largestVoxelX == xSize - 1 && largestVoxelY == ySize - 1 && largestVoxelZ == zSize - 1) {
      return;
    }

    int newXsize = largestVoxelX - smallestVoxelX + 1;
    int newYsize = largestVoxelY - smallestVoxelY + 1;
    int newZsize = largestVoxelZ - smallestVoxelZ + 1;
    VoxelSelectionWithOrigin smallerSelection = new VoxelSelectionWithOrigin(
            wxOrigin + smallestVoxelX, wyOrigin + smallestVoxelY, wzOrigin + smallestVoxelZ,
            newXsize, newYsize, newZsize);
    for (int y = 0; y < newYsize; ++y) {
      for (int z = 0; z < newZsize; ++z) {
        for (int x = 0; x < newXsize; ++x) {
          if (selection.getVoxel(x + smallestVoxelX, y + smallestVoxelY, z + smallestVoxelZ)) {
            smallerSelection.setVoxel(x, y, z);
          }
        }
      }
    }
    selection = smallerSelection;

    VoxelSelectionWithOrigin smallerUnavailableVoxels = new VoxelSelectionWithOrigin(
            wxOrigin + smallestVoxelX, wyOrigin + smallestVoxelY, wzOrigin + smallestVoxelZ,
            newXsize, newYsize, newZsize);
    for (int y = 0; y < newYsize; ++y) {
      for (int z = 0; z < newZsize; ++z) {
        for (int x = 0; x < newXsize; ++x) {
          if (unavailableVoxels.getVoxel(x + smallestVoxelX, y + smallestVoxelY, z + smallestVoxelZ)) {
            smallerUnavailableVoxels.setVoxel(x, y, z);
          }
        }
      }
    }
    unavailableVoxels = smallerUnavailableVoxels;

    wxOrigin += smallestVoxelX;
    wyOrigin += smallestVoxelY;
    wzOrigin += smallestVoxelZ;
    smallestVoxelX = 0;
    smallestVoxelY = 0;
    smallestVoxelZ = 0;
    largestVoxelX = newXsize - 1;
    largestVoxelY = newYsize - 1;
    largestVoxelZ = newZsize - 1;
    xSize = newXsize;
    ySize = newYsize;
    zSize = newZsize;
  }

  private void initialiseSelectionSizeFromBoundary(ChunkCoordinates corner1, ChunkCoordinates corner2) {
    wxOrigin = Math.min(corner1.posX, corner2.posX);
    wyOrigin = Math.min(corner1.posY, corner2.posY);
    wzOrigin = Math.min(corner1.posZ, corner2.posZ);
    xSize = 1 + Math.max(corner1.posX, corner2.posX) - wxOrigin;
    ySize = 1 + Math.max(corner1.posY, corner2.posY) - wyOrigin;
    zSize = 1 + Math.max(corner1.posZ, corner2.posZ) - wzOrigin;
    if (selection == null) {
      selection = new VoxelSelectionWithOrigin(wxOrigin, wyOrigin, wzOrigin, xSize, ySize, zSize);
      unavailableVoxels = new VoxelSelectionWithOrigin(wxOrigin, wyOrigin, wzOrigin, xSize, ySize, zSize);
    } else {
      selection.resizeAndClear(xSize, ySize, zSize);
      unavailableVoxels.resizeAndClear(xSize, ySize, zSize);
    }
  }

  private enum OperationInProgress
  {
    IDLE, ALL_IN_BOX, FILL, COMPLETE
  }

  Deque<SearchPosition> currentSearchPositions = new LinkedList<SearchPosition>();
  Deque<SearchPosition> nextDepthSearchPositions = new LinkedList<SearchPosition>();
  int blocksAddedCount;
  private VoxelSelectionWithOrigin selection;
  private VoxelSelectionWithOrigin unavailableVoxels;
  private boolean containsUnavailableVoxels;
  private int smallestVoxelX;
  private int largestVoxelX;
  private int smallestVoxelY;
  private int largestVoxelY;
  private int smallestVoxelZ;
  private int largestVoxelZ;
  private int xSize;
  private int ySize;
  private int zSize;
  private int wxOrigin;
  private int wyOrigin;
  private int wzOrigin;
  private boolean empty = true;
  private OperationInProgress mode;

}
