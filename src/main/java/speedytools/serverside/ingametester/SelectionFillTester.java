package speedytools.serverside.ingametester;

import net.minecraftforge.fml.common.FMLLog;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import speedytools.common.selections.*;
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
    final int XSIZE = 48; final int YSIZE = 48; final int ZSIZE = 44;

    final int NUMBER_OF_TEST_REGIONS = 4;
    ArrayList<InGameTester.TestRegions> testRegions = new ArrayList<InGameTester.TestRegions>();
    for (int i = 0; i < NUMBER_OF_TEST_REGIONS; ++i) {
      InGameTester.TestRegions newRegion = new InGameTester.TestRegions(XORIGIN, YORIGIN, ZORIGIN + i * (ZSIZE+1), XSIZE, YSIZE, ZSIZE, true);
      if (!performTest) {
        newRegion.drawAllTestRegionBoundaries();
        WorldFragment worldFragmentBlank = new WorldFragment(newRegion.xSize, newRegion.ySize, newRegion.zSize);
        worldFragmentBlank.readFromWorld(worldServer, newRegion.testRegionInitialiser.getX(), newRegion.testRegionInitialiser.getY(), newRegion.testRegionInitialiser.getZ(), null);
        worldFragmentBlank.writeToWorld(worldServer, newRegion.testOutputRegion.getX(), newRegion.testOutputRegion.getY(), newRegion.testOutputRegion.getZ(), null);
        worldFragmentBlank.writeToWorld(worldServer, newRegion.expectedOutcome.getX(), newRegion.expectedOutcome.getY(), newRegion.expectedOutcome.getZ(), null);
      }
      testRegions.add(newRegion);
    }
    if (!performTest) return true;

    int testRegionNumber = 0;
    for (InGameTester.TestRegions testRegion : testRegions) {
      BlockPos corner1 = new BlockPos(testRegion.sourceRegion.getX(), testRegion.sourceRegion.getY(), testRegion.sourceRegion.getZ());
      BlockPos corner2 = new BlockPos(testRegion.sourceRegion.getX() + testRegion.xSize,
                                                      testRegion.sourceRegion.getY() + testRegion.ySize,
              testRegion.sourceRegion.getZ() + testRegion.zSize);
      BlockPos blockUnderCursor = corner1;
      selectBoundFillStart(worldServer, blockUnderCursor, corner1, corner2);
      selectFillContinue(worldServer, Long.MAX_VALUE);
      BlockPos origin = getWorldOrigin();
      VoxelSelectionWithOrigin oldSelection = new VoxelSelectionWithOrigin(origin.getX(), origin.getY(), origin.getZ(), getSelection());
      WorldFragment worldFragmentOld = new WorldFragment(oldSelection.getxSize(), oldSelection.getySize(), oldSelection.getzSize());
      worldFragmentOld.readFromWorld(worldServer, testRegion.sourceRegion.getX(), testRegion.sourceRegion.getY(), testRegion.sourceRegion.getZ(),
              oldSelection);
      worldFragmentOld.writeToWorld(worldServer, testRegion.testOutputRegion.getX(), testRegion.testOutputRegion.getY(), testRegion.testOutputRegion.getZ(),
              null);

      FillAlgorithmSettings fillAlgorithmSettings = new FillAlgorithmSettings();
      fillAlgorithmSettings.setDiagonalPropagationAllowed(true);
      fillAlgorithmSettings.setFillMatcher(new FillMatcher.AnyNonAir());
      fillAlgorithmSettings.setStartPosition(blockUnderCursor);
      BlockVoxelMultiSelector blockVoxelMultiSelector = new BlockVoxelMultiSelector();

      blockVoxelMultiSelector.selectBoundFillStart(worldServer, fillAlgorithmSettings, corner1, corner2);
      blockVoxelMultiSelector.continueSelectionGeneration(worldServer, Long.MAX_VALUE);

      VoxelSelection selectionNew = blockVoxelMultiSelector.getSelection();
      VoxelSelection selectionOld = getSelection();

      if (!selectionNew.containsAllOfThisMask(selectionOld)) {
        System.out.println("Test region " + testRegionNumber + " failed; New didn't contain all of Old");
        return false;
      }
      if (!selectionOld.containsAllOfThisMask(selectionNew)) {
        System.out.println("Test region " + testRegionNumber + " failed; Old didn't contain all of New");
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
  public void selectUnboundFillStart(World world, BlockPos blockUnderCursor) {
    final int BORDER_ALLOWANCE = 2;
    BlockPos corner1 = new BlockPos(
      blockUnderCursor.getX() - VoxelSelection.MAX_X_SIZE / 2 + BORDER_ALLOWANCE,
      blockUnderCursor.getY(),
      blockUnderCursor.getZ() - VoxelSelection.MAX_Z_SIZE / 2 + BORDER_ALLOWANCE);

    BlockPos corner2 = new BlockPos(
      blockUnderCursor.getX() + VoxelSelection.MAX_X_SIZE / 2 - BORDER_ALLOWANCE,
      Math.min(255, blockUnderCursor.getY() + VoxelSelection.MAX_Y_SIZE - 2 * BORDER_ALLOWANCE),
      blockUnderCursor.getZ() + VoxelSelection.MAX_Z_SIZE / 2 - BORDER_ALLOWANCE);


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
  public void selectBoundFillStart(World world, BlockPos blockUnderCursor, BlockPos corner1, BlockPos corner2) {
    initialiseSelectionSizeFromBoundary(corner1, corner2);
    assert (blockUnderCursor.getX() >= wxOrigin && blockUnderCursor.getY() >= wyOrigin && blockUnderCursor.getZ() >= wzOrigin);
    assert (blockUnderCursor.getX() < wxOrigin + xSize && blockUnderCursor.getY() < wyOrigin + ySize && blockUnderCursor.getZ() < wzOrigin + zSize);
    mode = OperationInProgress.FILL;
    initialiseVoxelRange();
    BlockPos startingBlockCopy = new BlockPos(blockUnderCursor.getX() - wxOrigin, blockUnderCursor.getY() - wyOrigin, blockUnderCursor.getZ() - wzOrigin);
    currentSearchPositions.clear();
    nextDepthSearchPositions.clear();
    currentSearchPositions.add(new SearchPosition(startingBlockCopy));
    selection.setVoxel(startingBlockCopy.getX(), startingBlockCopy.getY(), startingBlockCopy.getZ());
    expandVoxelRange(startingBlockCopy.getX(), startingBlockCopy.getY(), startingBlockCopy.getZ());
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
  public BlockPos getWorldOrigin() {
    return new BlockPos(selection.getWxOrigin(), selection.getWyOrigin(), selection.getWzOrigin());
  }

  public static class SearchPosition
  {
    public SearchPosition(BlockPos initBlockPos) {
      chunkCoordinates = initBlockPos;
      nextSearchDirection = 0;
    }

    public BlockPos chunkCoordinates;
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

    BlockPos checkPosition = new BlockPos(0, 0, 0);
    BlockPos checkPositionSupport = new BlockPos(0, 0, 0);

    while (!currentSearchPositions.isEmpty()) {
      SearchPosition currentSearchPosition = currentSearchPositions.getFirst();
      checkPosition = new BlockPos(currentSearchPosition.chunkCoordinates.getX() + searchDirectionsX[currentSearchPosition.nextSearchDirection],
              currentSearchPosition.chunkCoordinates.getY() + searchDirectionsY[currentSearchPosition.nextSearchDirection],
              currentSearchPosition.chunkCoordinates.getZ() + searchDirectionsZ[currentSearchPosition.nextSearchDirection]);
      if (checkPosition.getX() >= 0 && checkPosition.getX() < xSize
              && checkPosition.getY() >= 0 && checkPosition.getY() < ySize
              && checkPosition.getZ() >= 0 && checkPosition.getZ() < zSize
              && !selection.getVoxel(checkPosition.getX(), checkPosition.getY(), checkPosition.getZ())) {
        boolean blockIsAir = world.isAirBlock(checkPosition.add(wxOrigin, wyOrigin, wzOrigin));
        if (!blockIsAir) {
          BlockPos newChunkCoordinate = new BlockPos(checkPosition);
          SearchPosition nextSearchPosition = new SearchPosition(newChunkCoordinate);
          nextDepthSearchPositions.addLast(nextSearchPosition);
          selection.setVoxel(checkPosition.getX(), checkPosition.getY(), checkPosition.getZ());
          expandVoxelRange(checkPosition.getX(), checkPosition.getY(), checkPosition.getZ());
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

  private void initialiseSelectionSizeFromBoundary(BlockPos corner1, BlockPos corner2) {
    wxOrigin = Math.min(corner1.getX(), corner2.getX());
    wyOrigin = Math.min(corner1.getY(), corner2.getY());
    wzOrigin = Math.min(corner1.getZ(), corner2.getZ());
    xSize = 1 + Math.max(corner1.getX(), corner2.getX()) - wxOrigin;
    ySize = 1 + Math.max(corner1.getY(), corner2.getY()) - wyOrigin;
    zSize = 1 + Math.max(corner1.getZ(), corner2.getZ()) - wzOrigin;
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
