package speedytools.common.selections;

/**
 * User: The Grey Ghost
 * Date: 23/09/2014
 * Used to iterate through a Voxel region in a chunkwise fashion:
 * Iterates through the chunks as
 * cz=0: cx=0, cx=1, cx=2 etc then cz=1: cx=0, cx=1, cx=2 etc
 * Within each chunk: y fastest, z next, x slowest eg
 * [0,0,0] [0,1,0] ..  [0,15,0]
 * [0,0,1] [0,1,1] ..  [0,15,1]
 * to
 * [0,0,15] .. [0,15,15]
 * then
 * [1,0,0] [1,1,0] .. [1,15,0]
 * etc
 */
public class VoxelChunkwiseIterator implements IVoxelIterator
{
  public VoxelChunkwiseIterator(int i_wxOrigin, int i_wyOrigin, int i_wzOrigin, int i_xSize, int i_ySize, int i_zSize)
  {
    if (i_xSize < 0) throw new IllegalArgumentException("xSize < 0: " + i_xSize);
    if (i_ySize < 0) throw new IllegalArgumentException("ySize < 0: " + i_ySize);
    if (i_zSize < 0) throw new IllegalArgumentException("zSize < 0: " + i_zSize);
    wxOrigin = i_wxOrigin;
    wyOrigin = i_wyOrigin;
    wzOrigin = i_wzOrigin;
    xSize = i_xSize;
    ySize = i_ySize;
    zSize = i_zSize;
    reset();
  }

  /**
   * resets the iterator to start at the beginning
   */
  public void reset()
  {
    atEnd = false;
    cxMin = wxOrigin >> 4;
    cxMax = (wxOrigin + xSize - 1) >> 4;
    czMin = wzOrigin >> 4;
    czMax = (wzOrigin + zSize - 1) >> 4;
    enteredNewChunk = true;
    cx = cxMin;
    cz = czMin;
    setIterationLimits(cx, cz);
    wxLSB = wxLSBmin;
    wy = wyMin;
    wzLSB = wzLSBmin;
    blockCount = 0;
  }

  /**
   * advances to the next voxel coordinate
   * @return true if the coordinate position is valid, false otherwise
   */
  public boolean next(boolean ignored)
  {
    if (atEnd) return false;
    ++blockCount;
    if (wy < wyMax) {
      ++wy;
    } else {
      wy = wyMin;
      if (wzLSB < wzLSBmax) {
        ++wzLSB;
      } else {
        wzLSB = wzLSBmin;
        if (wxLSB < wxLSBmax) {
          ++wxLSB;
        } else {
          wxLSB = wxLSBmin;
          enteredNewChunk = true;
          if (cz < czMax) {
            ++cz;
          } else {
            cz = czMin;
            if (cx < cxMax) {
              ++cx;
            } else {
              atEnd = true;
            }
          }
          if (!atEnd) {
            setIterationLimits(cx, cz);
            wxLSB = wxLSBmin;
            wzLSB = wzLSBmin;
          }
        }
      }
    }
    return !atEnd;
  }

  /** returns true on the first call after the iterator has moved into a new chunk
   * @return
   */
  public boolean hasEnteredNewChunk() {
    boolean retval = enteredNewChunk;
    enteredNewChunk = false;
    return retval;
  }

  /** has the iterator reached the end of the region?
   * @return
   */
  public boolean isAtEnd() {return atEnd;}

  /**
   * return the chunk x, z coordinate the iterator is currently in
   * @return
   */
  public int getChunkX() {return cx;}
  public int getChunkZ() {return cz;}

  /** return the world x, y, z of the current iterator position
   * @return
   */
  public int getWX() {return (cx << 4) +  wxLSB;}
  public int getWY() {return wy;}
  public int getWZ() {return (cz << 4) +  wzLSB;}

  /** get the [x,y,z] index of the current iterator position, i.e. relative to the origin
    * @return
   */
  public int getXpos() {return getWX() - wxOrigin;}
  public int getYpos() {return getWY() - wyOrigin;}
  public int getZpos() {return getWZ() - wzOrigin;}

  /** estimate the fraction of the range that has been iterated through
   * @return [0 .. 1]
   */
  public float estimatedFractionComplete()
  {
    return blockCount / (xSize * (float)ySize * zSize);
  }


  /**
   * for a given chunk coordinates, set the LSB iteration limits within that chunk
   * eg if cx = 1 and wxOrigin = 21 ( = 16 * cx + 5), then the LSBmin is set to 5.
   * @param cx
   * @param cz
   */
  private void setIterationLimits(int cx, int cz)
  {
    int wxChunk = cx << 4;
    int wxChunkPlus15 = wxChunk + 15;
    int wxMin = Math.max(wxOrigin, wxChunk);
    int wxMax = Math.min(wxOrigin + xSize - 1, wxChunkPlus15);
    assert (wxMin <= wxMax);
    wxLSBmin = wxMin & 0x0f;
    wxLSBmax = (wxMax & 0x0f);

    int wzChunk = cz << 4;
    int wzChunkPlus15 = wzChunk + 15;
    int wzMin = Math.max(wzOrigin, wzChunk);
    int wzMax = Math.min(wzOrigin + zSize - 1, wzChunkPlus15);
    assert (wzMin <= wzMax);
    wzLSBmin = wzMin & 0x0f;
    wzLSBmax = (wzMax & 0x0f);

    final int MINIMUM_Y_COORDINATE = 0;
    final int MAXIMUM_Y_COORDINATE = 255;
    wyMin = Math.max(MINIMUM_Y_COORDINATE, wyOrigin);
    wyMax = Math.min(MAXIMUM_Y_COORDINATE, wyOrigin + ySize - 1);
  }

  private int cxMin;
  private int cxMax;
  private int czMin;
  private int czMax;

  private int wxLSBmin;
  private int wxLSBmax;
  private int wzLSBmin;
  private int wzLSBmax;
  private int wyMin;
  private int wyMax;

  private int wxLSB;
  private int wzLSB;
  private int wy;
  private int cx;
  private int cz;

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
