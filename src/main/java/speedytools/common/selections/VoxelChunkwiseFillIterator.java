package speedytools.common.selections;

import net.minecraft.util.ChunkCoordinates;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.LinkedList;

/**
 * User: The Grey Ghost
 * Date: 23/09/2014
 * Used to floodfill through a Voxel region in a chunkwise fashion:
 * Will fill as far as possible within a chunk before starting to search in the next one, and will prefer to search chunks it
 * has already visited before
 */
public class VoxelChunkwiseFillIterator
{
  public VoxelChunkwiseFillIterator(int i_wxOrigin, int i_wyOrigin, int i_wzOrigin, int i_xSize, int i_ySize, int i_zSize) {
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
    chunkCheckPositions = new ArrayList<LinkedList<ChunkCoordinates>>(cxCount * czCount);
    chunksVisited = new BitSet(cxCount * czCount);
    chunksToVisitFirst = new BitSet(cxCount * czCount);
    chunksToVisitLater = new BitSet(cxCount * czCount);
    blocksChecked = new BitSet(xSize * ySize * zSize);
    reset();
  }

  /**
   * Add a start position for the search
   *
   * @param wx
   * @param wy
   * @param wz
   */
  public void addStartPosition(int wx, int wy, int wz) {
    if (!isWithinBounds(wx, wy, wz)) return;
    int chunkIdx = getChunkIndex(wx, wy, wz);
    LinkedList<ChunkCoordinates> chunkPos = chunkCheckPositions.get(chunkIdx);
    chunkPos.add(new ChunkCoordinates(wx, wy, wz));
    chunksToVisitFirst.set(chunkIdx);
  }

  /**
   * resets the iterator to start at the beginning
   */
  public void reset() {
    atEnd = false;
    enteredNewChunk = true;
    chunksToVisitFirst.clear();
    chunksToVisitLater.clear();
    currentSearchStartPositions.clear();
    chunksVisited.clear();
    blocksChecked.clear();
    blockCount = 0;

    for (int i = 0; i < chunkCheckPositions.size(); ++i) {
      chunkCheckPositions.set(i, new LinkedList<ChunkCoordinates>());
    }
//    cx = cxMin;
//    cz = czMin;
//    setIterationLimits(cx, cz);
//    wxLSB = wxLSBmin;
//    wy = wyMin;
//    wzLSB = wzLSBmin;
    blockCount = 0;
  }

  /**
   * advances to the next voxel coordinate
   * @param currentPositionWasFilled true if the current iterator position was incorporated into the fill, i.e. met the
   *          criteria to be added to the floodfill selection
   * @return true if the coordinate position is valid, false if not (there are no more positions)
   */
  public boolean next(boolean currentPositionWasFilled) {
    if (atEnd) return false;
    ++blockCount;

    // algorithm is:
    // first, for the current chunk, check if there are any currentSearchPositions.
    //   If so, use one of them and iterate through its search directions until it finds a valid one:
    //   a) is within the boundary region
    //   b) hasn't already been checked
    //   c) is within the same chunk (if outside the current chunk, add it to that chunk's chunkStartSearchPositions)
    // If there are no currentSearchPositions remaining, check for any chunkStartSearchPositions in the current chunk:
    //   if there is a valid one (hasn't already been checked), use that.
    // If there are no more blocks in this chunk, choose the next chunk from chunksToVisitFirst.
    // If chunksToVisitFirst is empty, choose the next chunk from chunksToVisitLater

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

//    LinkedList<ChunkCoordinates> currentChunkStartSearchPositions
//        = chunkCheckPositions.get(getChunkIndex(currentSearchPosition.chunkCoordinates.posX,
//                                                currentSearchPosition.chunkCoordinates.posY,
//                                                currentSearchPosition.chunkCoordinates.posZ));
    if (currentPositionWasFilled) {
      currentSearchStartPositions.add(new SearchPosition(currentCheckPosition));
    }

    while (!currentSearchStartPositions.isEmpty()) {
      SearchPosition currentPosition = currentSearchStartPositions.peekFirst();
      int wx = currentPosition.chunkCoordinates.posX + searchDirectionsX[currentPosition.nextSearchDirection];
      int wy = currentPosition.chunkCoordinates.posY + searchDirectionsY[currentPosition.nextSearchDirection];
      int wz = currentPosition.chunkCoordinates.posZ + searchDirectionsZ[currentPosition.nextSearchDirection];
      ++currentPosition.nextSearchDirection;
      if (currentPosition.nextSearchDirection >= ALL_DIRECTIONS) {
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
        LinkedList<ChunkCoordinates> chunkStartSearchPositions = chunkCheckPositions.get(chunkIdx);
        chunkStartSearchPositions.add(new ChunkCoordinates(wx, wy, wz));
        if (chunksVisited.get(chunkIdx)) {
          chunksToVisitFirst.set(chunkIdx);
        } else {
          chunksToVisitLater.set(chunkIdx);
        }
      }
    }

    int chunkIdx = getChunkIndex(currentCheckPosition.posX, currentCheckPosition.posY, currentCheckPosition.posZ);
    LinkedList<ChunkCoordinates> currentChunkStartSearchPositions = chunkCheckPositions.get(chunkIdx);

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
  public float estimatedFractionComplete() {
    if (blocksAddedCount == 0) return 0;
    double fillFraction = blocksAddedCount / (double)(xSize * ySize * zSize);

    final double FULL_SCALE = Math.log(1.0 / (xSize * (double)ySize * zSize)) - 1;
    double fractionComplete = (1 - Math.log(fillFraction) / FULL_SCALE);
    return (float)fractionComplete;
  }

  private static class SearchPosition
  {
    public SearchPosition(ChunkCoordinates initChunkCoordinates) {
      chunkCoordinates = initChunkCoordinates;
      nextSearchDirection = 0;
    }

    public ChunkCoordinates chunkCoordinates;
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

//  /**
//   * for a given chunk coordinates, set the LSB iteration limits within that chunk
//   * eg if cx = 1 and wxOrigin = 21 ( = 16 * cx + 5), then the LSBmin is set to 5.
//   *
//   * @param cx
//   * @param cz
//   */
//  private void setIterationLimits(int cx, int cz) {
//    int wxChunk = cx << 4;
//    int wxChunkPlus15 = wxChunk + 15;
//    int wxMin = Math.max(wxOrigin, wxChunk);
//    int wxMax = Math.min(wxOrigin + xSize - 1, wxChunkPlus15);
//    assert (wxMin <= wxMax);
//    wxLSBmin = wxMin & 0x0f;
//    wxLSBmax = (wxMax & 0x0f);
//
//    int wzChunk = cz << 4;
//    int wzChunkPlus15 = wzChunk + 15;
//    int wzMin = Math.max(wzOrigin, wzChunk);
//    int wzMax = Math.min(wzOrigin + zSize - 1, wzChunkPlus15);
//    assert (wzMin <= wzMax);
//    wzLSBmin = wzMin & 0x0f;
//    wzLSBmax = (wzMax & 0x0f);
//
//    final int MINIMUM_Y_COORDINATE = 0;
//    final int MAXIMUM_Y_COORDINATE = 255;
//    wyMin = Math.max(MINIMUM_Y_COORDINATE, wyOrigin);
//    wyMax = Math.min(MAXIMUM_Y_COORDINATE, wyOrigin + ySize - 1);
//  }

  private Deque<SearchPosition> currentSearchStartPositions = new LinkedList<SearchPosition>();   // search positions within the current chunk
//  private Deque<SearchPosition> nextDepthSearchPositions = new LinkedList<SearchPosition>();
  private ChunkCoordinates currentCheckPosition;

  // for each chunk in the boundary, a list of block positions to be checked.  Chunks arranged in idx = cx + cz * cxCount order
  private ArrayList<LinkedList<ChunkCoordinates>> chunkCheckPositions;
  private BitSet chunksVisited;  // true for each chunk which we have already visited
  private BitSet chunksToVisitFirst;
  private BitSet chunksToVisitLater;
  private BitSet blocksChecked;  // true for each block which has been checked already; or which is queued for checking in chunkCheckPositions
  private int blocksAddedCount;

  private int cxMin;
//  private int cxMax;
  private int czMin;
//  private int czMax;
  private int cxCount; // number of x chunks in the fill region (xwide * zlong)
  private int czCount; // number of z chunks in the fill region (xwide * zlong)
//  private int wxLSBmin;
//  private int wxLSBmax;
//  private int wzLSBmin;
//  private int wzLSBmax;
//  private int wyMin;
//  private int wyMax;
//  private int wxLSB;
//  private int wzLSB;
//  private int wy;
//  private int cx;
//  private int cz;
  private boolean atEnd;
  private boolean enteredNewChunk;
  private int wxOrigin;
  private int wyOrigin;
  private int wzOrigin;
  private int xSize;
  private int ySize;
  private int zSize;
  private int blockCount;
}
//{
//        ++wy;
//        }
//        {
//        wy=wyMin;
//        if(wzLSB<wzLSBmax){
//        ++wzLSB;
//        }else{
//        wzLSB=wzLSBmin;
//        if(wxLSB<wxLSBmax){
//        ++wxLSB;
//        }else{
//        wxLSB=wxLSBmin;
//        enteredNewChunk=true;
//        if(cz<czMax){
//        ++cz;
//        }else{
//        cz=czMin;
//        if(cx<cxMax){
//        ++cx;
//        }else{
//        atEnd=true;
//        }
//        }
//        if(!atEnd){
//        setIterationLimits(cx,cz);
//        wxLSB=wxLSBmin;
//        wzLSB=wzLSBmin;
//        }
//        }
//        }
//        }
//        }
