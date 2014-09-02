//package speedytools.clientside.selections;
//
//import cpw.mods.fml.common.FMLLog;
//import net.minecraft.util.ChunkCoordinates;
//import net.minecraft.world.World;
//import speedytools.common.selections.VoxelSelection;
//import speedytools.common.selections.VoxelSelectionWithOrigin;
//
//import java.io.ByteArrayOutputStream;
//import java.util.Deque;
//import java.util.LinkedList;
//
///**
// * User: The Grey Ghost
// * Date: 17/02/14
// */
//public class BlockVoxelMultiSelector
//{
//  private VoxelSelectionWithOrigin selection;
////  private VoxelSelection shadow;
//
//  private int smallestVoxelX;
//  private int largestVoxelX;
//  private int smallestVoxelY;
//  private int largestVoxelY;
//  private int smallestVoxelZ;
//  private int largestVoxelZ;
//
///*
//  // the coordinates of the blocks that form the
//  private ArrayList<ChunkCoordinates> wireFrameXnegY;
//  private ArrayList<ChunkCoordinates> wireFrameXposY;
//  private ArrayList<ChunkCoordinates> wireFrameZnegY;
//  private ArrayList<ChunkCoordinates> wireFrameZposY;
//*/
//  private int xSize;
//  private int ySize;
//  private int zSize;
//  private int wxOrigin;
//  private int wyOrigin;
//  private int wzOrigin;
//
//  private int xpos;
//  private int ypos;
//  private int zpos;
//
//  private boolean empty = true;
//
//  private enum OperationInProgress {
//    IDLE, ALL_IN_BOX, FILL, COMPLETE
//  }
//  private OperationInProgress mode;
//
////  private int displayListSelection = 0;
////  private int displayListWireFrameXY = 0;
////  private int displayListWireFrameYZ = 0;
////  private int displayListWireFrameXZ = 0;
////
////  private int [] displayList
//
//  /**
//   * initialise conversion of the selected box to a VoxelSelection
//   * @param world
//   * @param corner1 one corner of the box
//   * @param corner2 opposite corner of the box
//   */
//  public void selectAllInBoxStart(World world, ChunkCoordinates corner1, ChunkCoordinates corner2)
//  {
//    initialiseSelectionSizeFromBoundary(corner1, corner2);
//    xpos = 0;
//    ypos = 0;
//    zpos = 0;
//    mode = OperationInProgress.ALL_IN_BOX;
//    initialiseVoxelRange();
//  }
//
//  public float continueSelectionGeneration(World world, long maxTimeInNS)
//  {
//    switch (mode) {
//      case ALL_IN_BOX: {
//        return selectAllInBoxContinue(world, maxTimeInNS);
//      }
//      case FILL: {
//        return selectFillContinue(world, maxTimeInNS);
//      }
//      case COMPLETE: {
//        return -1;
//      }
//      default: assert false : "invalid mode " + mode + " in continueSelectionGeneration";
//    }
//    return 0;
//  }
//
//  public VoxelSelection getSelection() {
//    return selection;
//  }
//
//  /**
//   * continue conversion of the selected box to a VoxelSelection.  Call repeatedly until conversion complete.
//   * @param world
//   * @param maxTimeInNS maximum elapsed duration before processing stops & function returns
//   * @return fraction complete (0 - 1), -ve number for finished
//   */
//  private float selectAllInBoxContinue(World world, long maxTimeInNS)
//  {
//    if (mode != OperationInProgress.ALL_IN_BOX) {
//      FMLLog.severe("Mode should be ALL_IN_BOX in BlockVoxelMultiSelector::selectAllInBoxContinue");
//      return -1;
//    }
//
//    long startTime = System.nanoTime();
//
//    for ( ; zpos < zSize; ++zpos, xpos = 0) {
//      for ( ; xpos < xSize; ++xpos, ypos = 0) {
//        for ( ; ypos < ySize; ++ypos) {
//          if (System.nanoTime() - startTime >= maxTimeInNS) return (zpos / (float)zSize);
//          if (world.getBlockId(xpos + wxOrigin, ypos + wyOrigin, zpos + wzOrigin) != 0) {
//            selection.setVoxel(xpos, ypos, zpos);
//            expandVoxelRange(xpos, ypos, zpos);
//          }
//        }
//      }
//    }
//    mode = OperationInProgress.COMPLETE;
//    return -1;
//  }
//
//  /**
//   * initialise conversion of the selected fill to a VoxelSelection
//   * From the starting block, performs a flood fill on all non-air blocks.
//   * Will not fill any blocks with y less than the blockUnderCursor.
//   * @param world
//   * @param blockUnderCursor the block being highlighted by the cursor
//   */
//  public void selectUnboundFillStart(World world, ChunkCoordinates blockUnderCursor)
//  {
//    ChunkCoordinates corner1 = new ChunkCoordinates();
//    ChunkCoordinates corner2 = new ChunkCoordinates();
//    final int BORDER_ALLOWANCE = 2;
//    corner1.posX = blockUnderCursor.posX - VoxelSelection.MAX_X_SIZE / 2 + BORDER_ALLOWANCE;
//    corner2.posX = blockUnderCursor.posX + VoxelSelection.MAX_X_SIZE / 2 - BORDER_ALLOWANCE;
//    corner1.posY = blockUnderCursor.posY;
//    corner2.posY = Math.min(255, blockUnderCursor.posY + VoxelSelection.MAX_Y_SIZE - 2 * BORDER_ALLOWANCE);
//    corner1.posZ = blockUnderCursor.posZ - VoxelSelection.MAX_Z_SIZE / 2 + BORDER_ALLOWANCE;
//    corner2.posZ = blockUnderCursor.posZ + VoxelSelection.MAX_Z_SIZE / 2 - BORDER_ALLOWANCE;
//
//    selectBoundFillStart(world, blockUnderCursor, corner1, corner2);
//  }
//
//  /**
//   * initialise conversion of the selected fill to a VoxelSelection
//   * From the starting block, performs a flood fill on all non-air blocks.
//   * Will not fill any blocks outside of the box defined by corner1 and corner2
//   * @param world
//   * @param blockUnderCursor the block being highlighted by the cursor
//   */
//  public void selectBoundFillStart(World world, ChunkCoordinates blockUnderCursor, ChunkCoordinates corner1, ChunkCoordinates corner2)
//  {
//    initialiseSelectionSizeFromBoundary(corner1, corner2);
//    assert (blockUnderCursor.posX >= wxOrigin && blockUnderCursor.posY >= wyOrigin && blockUnderCursor.posZ >= wzOrigin);
//    assert (blockUnderCursor.posX < wxOrigin + xSize && blockUnderCursor.posY < wyOrigin + ySize && blockUnderCursor.posZ < wzOrigin + zSize);
//    mode = OperationInProgress.FILL;
//    initialiseVoxelRange();
//    ChunkCoordinates startingBlockCopy = new ChunkCoordinates(blockUnderCursor.posX - wxOrigin, blockUnderCursor.posY - wyOrigin, blockUnderCursor.posZ - wzOrigin);
//    currentSearchPositions.clear();
//    nextDepthSearchPositions.clear();
//    currentSearchPositions.add(new SearchPosition(startingBlockCopy));
//    selection.setVoxel(startingBlockCopy.posX, startingBlockCopy.posY, startingBlockCopy.posZ);
//    expandVoxelRange(startingBlockCopy.posX, startingBlockCopy.posY, startingBlockCopy.posZ);
//    blocksAddedCount = 0;
////    checkedLocations = new VoxelSelection(xSize, ySize, zSize);
//  }
//
//  /**
//   * continue conversion of the selected box to a VoxelSelection.  Call repeatedly until conversion complete.
//   * @param world
//   * @param maxTimeInNS maximum elapsed duration before processing stops & function returns
//   * @return fraction complete (0 - 1), -ve number for finished
//   */
//  private float selectFillContinue(World world, long maxTimeInNS)
//  {
//    if (mode != OperationInProgress.FILL) {
//      FMLLog.severe("Mode should be FILL in BlockVoxelMultiSelector::selectFillContinue");
//      return -1;
//    }
//
//    long startTime = System.nanoTime();
//
//    // lookup table to give the possible search directions for non-diagonal and diagonal respectively
//    final int NON_DIAGONAL_DIRECTIONS = 6;
//    final int ALL_DIRECTIONS = 26;
//    final int searchDirectionsX[] = {+0, +0, +0, +0, -1, +1,   // non-diagonal
//            +1, +0, -1, +0,   +1, +1, -1, -1,   +1, +0, -1, +0,  // top, middle, bottom "edge" blocks
//            +1, +1, -1, -1,   +1, +1, -1, -1                   // top, bottom "corner" blocks
//    };
//    final int searchDirectionsY[] = {-1, +1, +0, +0, +0, +0,   // non-diagonal
//            +1, +1, +1, +1,   +0, +0, +0, +0,   -1, -1, -1, -1,   // top, middle, bottom "edge" blocks
//            +1, +1, +1, +1,   -1, -1, -1, -1                      // top, bottom "corner" blocks
//    };
//
//    final int searchDirectionsZ[] = {+0, +0, -1, +1, +0, +0,   // non-diagonal
//            +0, -1, +0, +1,   +1, -1, -1, +1,   +0, -1, +0, +1,   // top, middle, bottom "edge" blocks
//            +1, -1, -1, +1,   +1, -1, -1, +1
//    };
//
////    final int INITIAL_CAPACITY = 128;
////    Set<ChunkCoordinates> locationsFilled = new HashSet<ChunkCoordinates>(INITIAL_CAPACITY);                                   // locations which have been selected during the fill
//    ChunkCoordinates checkPosition = new ChunkCoordinates(0,0,0);
//    ChunkCoordinates checkPositionSupport = new ChunkCoordinates(0,0,0);
//
//    // algorithm is:
//    //   for each block in the list of search positions, iterate through each adjacent block to see whether it meets the criteria for expansion:
//    //     a) matches the block-to-be-replaced (if matchAnyNonAir: non-air, otherwise if blockID and metaData match.  For lava or water metadata doesn't need to match).
//    //     b) hasn't been filled already during this contour search
//    //   if the criteria are met, select the block and add it to the list of blocks to be search next round.
//    //   if the criteria aren't met, keep trying other directions from the same position until all positions are searched.  Then delete the search position and move onto the next.
//    //   This will ensure that the fill spreads evenly out from the starting point.   Check the boundary to stop fill spreading outside it.
//
//    while (!currentSearchPositions.isEmpty()) {
//      SearchPosition currentSearchPosition = currentSearchPositions.getFirst();
//      checkPosition.set(currentSearchPosition.chunkCoordinates.posX + searchDirectionsX[currentSearchPosition.nextSearchDirection],
//              currentSearchPosition.chunkCoordinates.posY + searchDirectionsY[currentSearchPosition.nextSearchDirection],
//              currentSearchPosition.chunkCoordinates.posZ + searchDirectionsZ[currentSearchPosition.nextSearchDirection]);
//      if (    checkPosition.posX >= 0 && checkPosition.posX < xSize
//              &&  checkPosition.posY >= 0 && checkPosition.posY < ySize
//              &&  checkPosition.posZ >= 0 && checkPosition.posZ < zSize
//              && !selection.getVoxel(checkPosition.posX, checkPosition.posY, checkPosition.posZ)) {
//        int blockToCheckID = world.getBlockId(checkPosition.posX + wxOrigin, checkPosition.posY + wyOrigin, checkPosition.posZ + wzOrigin);
//        if (blockToCheckID != 0) {
//          ChunkCoordinates newChunkCoordinate = new ChunkCoordinates(checkPosition);
//          SearchPosition nextSearchPosition = new SearchPosition(newChunkCoordinate);
//          nextDepthSearchPositions.addLast(nextSearchPosition);
//          selection.setVoxel(checkPosition.posX, checkPosition.posY, checkPosition.posZ);
//          expandVoxelRange(checkPosition.posX, checkPosition.posY, checkPosition.posZ);
//          ++blocksAddedCount;
////          checkedLocations.setVoxel(checkPosition.posX, checkPosition.posY, checkPosition.posZ);
//        }
//      }
//      ++currentSearchPosition.nextSearchDirection;
//      if (currentSearchPosition.nextSearchDirection >= ALL_DIRECTIONS) {
//        currentSearchPositions.removeFirst();
//        if (currentSearchPositions.isEmpty()) {
//          Deque<SearchPosition> temp = currentSearchPositions;
//          currentSearchPositions = nextDepthSearchPositions;
//          nextDepthSearchPositions = temp;
//        }
//      }
//      if (System.nanoTime() - startTime >= maxTimeInNS) {   // completion fraction is hard to predict, so use a logarithmic function instead to provide some visual movement regardless of size
//        if (blocksAddedCount == 0) return 0;
//        double fillFraction = blocksAddedCount / (double)(xSize * ySize * zSize);
//
//        final double FULL_SCALE = Math.log(1.0 / (xSize * ySize * zSize)) - 1;
//        double fractionComplete = (1 - Math.log(fillFraction) / FULL_SCALE);
//        return (float)fractionComplete;
//      }
//    }
//
//    mode = OperationInProgress.COMPLETE;
//    shrinkToSmallestEnclosingCuboid();
////    checkedLocations = null;
//    return -1;
//  }
//
//  Deque<SearchPosition> currentSearchPositions = new LinkedList<SearchPosition>();
//  Deque<SearchPosition> nextDepthSearchPositions = new LinkedList<SearchPosition>();
//  int blocksAddedCount;
////  VoxelSelection checkedLocations;
//
//  public static class SearchPosition
//  {
//    public SearchPosition(ChunkCoordinates initChunkCoordinates) {
//      chunkCoordinates = initChunkCoordinates;
//      nextSearchDirection = 0;
//    }
//    public ChunkCoordinates chunkCoordinates;
//    public int nextSearchDirection;
//  }
//
//  /**
//   * returns true if there are no solid pixels at all in this selection.
//   * @return
//   */
//  public boolean isEmpty()
//  {
//    return empty;
//  }
//
//  /**
//   * write the current selection in serialised form to a ByteArray
//   * @return the byte array, or null for failure
//   */
//  public ByteArrayOutputStream writeToBytes()
//  {
//    return selection.writeToBytes();
//  }
//
//  private void initialiseVoxelRange()
//  {
//    smallestVoxelX = xSize;
//    largestVoxelX = -1;
//    smallestVoxelY = ySize;
//    largestVoxelY = -1;
//    smallestVoxelZ = zSize;
//    largestVoxelZ = -1;
//    empty = true;
//  }
//
//  private void expandVoxelRange(int x, int y, int z)
//  {
//    smallestVoxelX = Math.min(smallestVoxelX, x);
//    smallestVoxelY = Math.min(smallestVoxelY, y);
//    smallestVoxelZ = Math.min(smallestVoxelZ, z);
//    largestVoxelX = Math.max(largestVoxelX, x);
//    largestVoxelY = Math.max(largestVoxelY, y);
//    largestVoxelZ = Math.max(largestVoxelZ, z);
//    empty = false;
//  }
//
//  /**
//   * shrinks the voxel selection to the minimum size needed to contain the set voxels
//   */
//  private void shrinkToSmallestEnclosingCuboid()
//  {
//    if (smallestVoxelX == 0 && smallestVoxelY == 0 && smallestVoxelZ == 0
//        && largestVoxelX == xSize-1 && largestVoxelY == ySize-1 && largestVoxelZ == zSize-1) {
//      return;
//    }
//
//    int newXsize = largestVoxelX - smallestVoxelX + 1;
//    int newYsize = largestVoxelY - smallestVoxelY + 1;
//    int newZsize = largestVoxelZ - smallestVoxelZ + 1;
//    VoxelSelectionWithOrigin smallerSelection = new VoxelSelectionWithOrigin(
//            wxOrigin + smallestVoxelX, wyOrigin + smallestVoxelY, wzOrigin + smallestVoxelZ,
//            newXsize, newYsize, newZsize);
//    for (int y = 0; y < newYsize; ++y) {
//      for (int z = 0; z < newZsize; ++z) {
//        for (int x = 0; x < newXsize; ++x) {
//          if (selection.getVoxel(x + smallestVoxelX, y + smallestVoxelY, z + smallestVoxelZ)) {
//            smallerSelection.setVoxel(x, y, z);
//          }
//        }
//      }
//    }
//    selection = smallerSelection;
//    wxOrigin += smallestVoxelX;
//    wyOrigin += smallestVoxelY;
//    wzOrigin += smallestVoxelZ;
//    smallestVoxelX = 0;
//    smallestVoxelY = 0;
//    smallestVoxelZ = 0;
//    largestVoxelX = newXsize - 1;
//    largestVoxelY = newYsize - 1;
//    largestVoxelZ = newZsize - 1;
//    xSize = newXsize;
//    ySize = newYsize;
//    zSize = newZsize;
//  }
//
//  private void initialiseSelectionSizeFromBoundary(ChunkCoordinates corner1, ChunkCoordinates corner2)
//  {
//    wxOrigin = Math.min(corner1.posX, corner2.posX);
//    wyOrigin = Math.min(corner1.posY, corner2.posY);
//    wzOrigin = Math.min(corner1.posZ, corner2.posZ);
//    xSize = 1 + Math.max(corner1.posX, corner2.posX) - wxOrigin;
//    ySize = 1 + Math.max(corner1.posY, corner2.posY) - wyOrigin;
//    zSize = 1 + Math.max(corner1.posZ, corner2.posZ) - wzOrigin;
//    if (selection == null) {
//      selection = new VoxelSelectionWithOrigin(wxOrigin, wyOrigin, wzOrigin, xSize, ySize, zSize);
////      shadow = new VoxelSelection(xSize, 1, zSize);
//    } else {
//      selection.resizeAndClear(xSize, ySize, zSize);
////      shadow.clearAll(xSize, 1, zSize);
//    }
//  }
//
//  /** gets the origin for the selection in world coordinates
//   * @return the origin for the selection in world coordinates
//   */
//  public ChunkCoordinates getWorldOrigin()
//  {
//    return new ChunkCoordinates(selection.getWxOrigin(), selection.getWyOrigin(), selection.getWzOrigin());
//  }
//
//}
