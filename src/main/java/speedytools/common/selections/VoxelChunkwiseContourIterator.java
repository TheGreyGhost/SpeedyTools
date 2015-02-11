package speedytools.common.selections;

import net.minecraft.util.BlockPos;
import speedytools.common.utilities.ErrorLog;

import java.util.*;

/**
 * User: The Grey Ghost
 * Date: 23/09/2014
 * Used to contour floodfill through a Voxel region in a chunkwise fashion:
 * Will fill as far as possible within a chunk before starting to search in the next one, and will prefer to search chunks it
 * has already visited before
 * Usage:
 * 1) Create the iterator with the boundaries of the region that limit the fill
 * 2) setStartPositionAndPlane() to set the fill start point and the searching plane (eg XY, XZ, or YZ)
 * 3) Repeat until iterator.isAtEnd:
 *   a) Check the block at .getWX(), .getWY(), .getWZ().
 *   b) if it belongs to the fill, call .next(true), otherwise .next(false)
 *   c) use hasEnteredNewChunk() to determine when to load a new chunk.  getChunkX() and getChunkZ() give the coordinates
 * 4) estimatedFractionComplete() returns a number that indicates an estimate of how complete the fill process is
 */
public class VoxelChunkwiseContourIterator implements IVoxelIterator
{
  public VoxelChunkwiseContourIterator(int i_wxOrigin, int i_wyOrigin, int i_wzOrigin, int i_xSize, int i_ySize, int i_zSize) {
    if (i_xSize < 0) throw new IllegalArgumentException("xSize < 0: " + i_xSize);
    if (i_ySize < 0) throw new IllegalArgumentException("ySize < 0: " + i_ySize);
    if (i_zSize < 0) throw new IllegalArgumentException("zSize < 0: " + i_zSize);
    wxOrigin = i_wxOrigin;
    wyOrigin = i_wyOrigin;
    wzOrigin = i_wzOrigin;
    xSize = i_xSize;
    ySize = i_ySize;
    zSize = i_zSize;
    cxMin = wxOrigin >> 4;
    czMin = wzOrigin >> 4;
    int cxMax = (wxOrigin + xSize - 1) >> 4;
    int czMax = (wzOrigin + zSize - 1) >> 4;
    cxCount = cxMax - cxMin + 1;
    czCount = czMax - czMin + 1;
    chunkCheckPositions = new ArrayList<LinkedList<BlockPos>>(cxCount * czCount);
    chunksVisited = new BitSet(cxCount * czCount);
    chunksToVisitFirst = new BitSet(cxCount * czCount);
    chunksToVisitLater = new BitSet(cxCount * czCount);
    blocksChecked = new BitSet(xSize * ySize * zSize);
    diagonalAllowed = true;
    reset();
  }

  /**
   * Add a start position for the search
   *
   * @param wx
   * @param wy
   * @param wz
   * @param normalDirection specifies the plane that will be searched in (Facing directions; specifies the normal to the plane)
   */
  public void setStartPositionAndPlane(int wx, int wy, int wz, int normalDirection) {
    if (!isWithinBounds(wx, wy, wz)) return;
    currentCheckPosition = new BlockPos(wx, wy, wz);
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
      default: {
        ErrorLog.defaultLog().debug("Illegal normalDirection:" + normalDirection);
        searchPlane = PLANE_XY;
        break;
      }
    }
  }

  /**
   * true if diagonal filling is allowed; false if cardinal directions only
   * @param i_diagonalAllowed
   */
  public void setDiagonalAllowed(boolean i_diagonalAllowed)
  {
    diagonalAllowed = i_diagonalAllowed;
  }

  /**
   * resets the iterator to start at the beginning
   */
  @Override
  public void reset() {
    atEnd = false;
    enteredNewChunk = true;
    chunksToVisitFirst.clear();
    chunksToVisitLater.clear();
    currentSearchStartPositions.clear();
    chunksVisited.clear();
    blocksChecked.clear();
    blocksAddedCount = 0;

    for (int i = 0; i < cxCount * czCount; ++i) {
      chunkCheckPositions.add(i, new LinkedList<BlockPos>());
    }
  }

  /**
   * advances to the next voxel coordinate
   * @param currentPositionWasFilled true if the current iterator position was incorporated into the fill, i.e. met the
   *          criteria to be added to the floodfill selection
   * @return true if the coordinate position is valid, false if not (there are no more positions)
   */
  @Override
  public boolean next(boolean currentPositionWasFilled) {
    if (atEnd) return false;
    ++blocksAddedCount;

    // lookup table to give the possible search directions for any given search plane
    //  row index 0 = xz plane, 1 = xy plane, 2 = yz plane
    //  column index = the eight directions within the plane (even = cardinal, odd = diagonal)
    final int NUMBER_OF_SEARCH_DIRECTIONS = 8;
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

    if (currentPositionWasFilled) {
      currentSearchStartPositions.add(new SearchPosition(currentCheckPosition));
    }

    while (!currentSearchStartPositions.isEmpty()) {
      SearchPosition currentPosition = currentSearchStartPositions.peekFirst();
      int wx = currentPosition.chunkCoordinates.posX + searchDirectionsX[searchPlane][currentPosition.nextSearchDirection];
      int wy = currentPosition.chunkCoordinates.posY + searchDirectionsY[searchPlane][currentPosition.nextSearchDirection];
      int wz = currentPosition.chunkCoordinates.posZ + searchDirectionsZ[searchPlane][currentPosition.nextSearchDirection];

      currentPosition.nextSearchDirection += diagonalAllowed ? 1 : 2;  // no diagonals -> even numbers only

      if (currentPosition.nextSearchDirection >= NUMBER_OF_SEARCH_DIRECTIONS) {
        currentSearchStartPositions.removeFirst();
      }
      if (isWithinBounds(wx, wy, wz) && !blocksChecked.get(getBlockIndex(wx, wy, wz))) {
        blocksChecked.set(getBlockIndex(wx, wy, wz));
        if (getChunkIndex(wx, wy, wz) == getChunkIndex(currentPosition.chunkCoordinates.posX,
                currentPosition.chunkCoordinates.posY,
                currentPosition.chunkCoordinates.posZ)) {
          currentCheckPosition.set(wx, wy, wz);
          return true;
        }
        // different chunk, so queue it up
        int chunkIdx = getChunkIndex(wx, wy, wz);
        LinkedList<BlockPos> chunkStartSearchPositions = chunkCheckPositions.get(chunkIdx);
        chunkStartSearchPositions.add(new BlockPos(wx, wy, wz));
        if (chunksVisited.get(chunkIdx)) {
          chunksToVisitFirst.set(chunkIdx);
        } else {
          chunksToVisitLater.set(chunkIdx);
        }
      }
    }

    int chunkIdx = getChunkIndex(currentCheckPosition.posX, currentCheckPosition.posY, currentCheckPosition.posZ);
    LinkedList<BlockPos> currentChunkStartSearchPositions = chunkCheckPositions.get(chunkIdx);

    if (!currentChunkStartSearchPositions.isEmpty()) {
      currentCheckPosition = currentChunkStartSearchPositions.removeFirst();
      return true;
    }

    while (!chunksToVisitFirst.isEmpty()) {
      int chunkToVisitIdx = chunksToVisitFirst.previousSetBit(Integer.MAX_VALUE);
      chunksToVisitFirst.clear(chunkToVisitIdx);
      currentCheckPosition = chunkCheckPositions.get(chunkToVisitIdx).removeFirst();
      enteredNewChunk = true;
      return true;
    }

    while (!chunksToVisitLater.isEmpty()) {
      int chunkToVisitIdx = chunksToVisitLater.previousSetBit(Integer.MAX_VALUE);
      chunksToVisitLater.clear(chunkToVisitIdx);
      currentCheckPosition = chunkCheckPositions.get(chunkToVisitIdx).removeFirst();
      chunksVisited.set(chunkToVisitIdx);
      enteredNewChunk = true;
      return true;
    }
    atEnd = true;
    return false;  // nothing left to do!
  }

  /**
   * returns true on the first call after the iterator has moved into a new chunk
   *
   * @return
   */
  public boolean hasEnteredNewChunk() {
    boolean retval = enteredNewChunk;
    enteredNewChunk = false;
    return retval;
  }

  /**
   * has the iterator reached the end of the region?
   *
   * @return
   */
  @Override
  public boolean isAtEnd() {
    return atEnd;
  }

  /**
   * return the chunk x, z coordinate the iterator is currently in
   *
   * @return
   */
  public int getChunkX() {
    return currentCheckPosition.posX >> 4;
  }

  public int getChunkZ() {
    return currentCheckPosition.posZ >> 4;
  }

  /**
   * return the world x, y, z of the current iterator position
   *
   * @return
   */
  public int getWX() {
    return currentCheckPosition.posX;
  }

  public int getWY() {
    return currentCheckPosition.posY;
  }

  public int getWZ() {
    return currentCheckPosition.posZ;
  }

  /**
   * get the [x,y,z] index of the current iterator position, i.e. relative to the origin
   *
   * @return
   */
  public int getXpos() {
    return getWX() - wxOrigin;
  }

  public int getYpos() {
    return getWY() - wyOrigin;
  }

  public int getZpos() {
    return getWZ() - wzOrigin;
  }

  /**
   * estimate the fraction of the range that has been iterated through
   * (logarithmic transformation to show progress over a much wider range)
   * @return [0 .. 1]
   */
  @Override
  public float estimatedFractionComplete() {
    if (blocksAddedCount == 0) return 0;
    double fillFraction = blocksAddedCount / (double)(xSize * ySize * zSize);

    final double FULL_SCALE = Math.log(1.0 / (xSize * (double)ySize * zSize)) - 1;
    double fractionComplete = (1 - Math.log(fillFraction) / FULL_SCALE);
    return (float)fractionComplete;
  }

  private static class SearchPosition
  {
    public SearchPosition(BlockPos initBlockPos) {
      chunkCoordinates = new BlockPos(initBlockPos);
      nextSearchDirection = 0;
    }

    public BlockPos chunkCoordinates;
    public int nextSearchDirection;
  }

  /**
   * gets the index into the chunk arrays for a given set of world coordinates
   *
   * @param wx world [x,y,z]
   * @param wy
   * @param wz
   */
  private int getChunkIndex(int wx, int wy, int wz) {
    return ((wx >> 4) - cxMin) + cxCount * ((wz >> 4) - czMin);
  }

  /**
   * gets the index into the block arrays for a given set of world coordinates
   *
   * @param wx world [x,y,z]
   * @param wy
   * @param wz
   */
  private int getBlockIndex(int wx, int wy, int wz) {
    return (wx - wxOrigin) + xSize * (wy - wyOrigin) + xSize * ySize * (wz - wzOrigin);
  }

  /**
   * checks whether the given point is within the boundary region
   *
   * @param wx world [x,y,z]
   * @param wy
   * @param wz
   * @return true if within, false otherwise
   */
  private boolean isWithinBounds(int wx, int wy, int wz) {
    return (wx >= wxOrigin && wx < wxOrigin + xSize
            && wy >= wyOrigin && wy < wyOrigin + ySize
            && wz >= wzOrigin && wz < wzOrigin + zSize);
  }

  private Deque<SearchPosition> currentSearchStartPositions = new LinkedList<SearchPosition>();   // search positions within the current chunk
  private BlockPos currentCheckPosition;

  // for each chunk in the boundary, a list of block positions to be checked.  Chunks arranged in idx = cx + cz * cxCount order
  private ArrayList<LinkedList<BlockPos>> chunkCheckPositions;
  private BitSet chunksVisited;  // true for each chunk which we have already visited
  private BitSet chunksToVisitFirst;
  private BitSet chunksToVisitLater;
  private BitSet blocksChecked;  // true for each block which has been checked already; or which is queued for checking in chunkCheckPositions
  private int blocksAddedCount;

  private int cxMin;
  private int czMin;
  private int cxCount; // number of x chunks in the fill region (xwide * zlong)
  private int czCount; // number of z chunks in the fill region (xwide * zlong)

  private boolean atEnd;
  private boolean enteredNewChunk;
  private int wxOrigin;
  private int wyOrigin;
  private int wzOrigin;
  private int xSize;
  private int ySize;
  private int zSize;
  private boolean diagonalAllowed;

  private int searchPlane;
  private final int PLANE_XZ = 0;
  private final int PLANE_XY = 1;
  private final int PLANE_YZ = 2;

}

