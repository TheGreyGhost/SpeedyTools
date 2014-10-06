package speedytools.common.selections;

/**
 * Created by TheGreyGhost on 6/10/14.
 */
public interface IVoxelIterator
{
  /**
   * resets the iterator to start at the beginning
   */
  public void reset();

  /**
   * advances to the next voxel coordinate
   * @param currentPositionWasFilled true if the current iterator position was incorporated, i.e. met the
   *                                 criteria to be added to the floodfill selection
   * @return true if the coordinate position is valid, false if not (there are no more positions)
   */
  public boolean next(boolean currentPositionWasFilled);

  /**
   * returns true on the first call after the iterator has moved into a new chunk
   * @return returns true the first time this method is called after the iterator has moved into a new chunk
   */
  public boolean hasEnteredNewChunk();

  /**
   * true if the iterator has reached the end
   * @return  true if the iterator has reached the end
   */
  public boolean isAtEnd();

  /**return the chunk x, z coordinate the iterator is currently in
   * @return return the chunk x, z coordinate the iterator is currently in
   */
  public int getChunkX();
  public int getChunkZ();

  /**return the world x, y, z of the current iterator position
   * @return return the world x, y, z of the current iterator position
   */
  public int getWX();
  public int getWY();
  public int getWZ();

  /** get the [x,y,z] index of the current iterator position, i.e. relative to the origin
   * @return [x,y,z] index of the current iterator position, i.e. relative to the origin
   */
  public int getXpos();
  public int getYpos();
  public int getZpos();

  /** estimate the fraction of the range that has been iterated through (should increase monotonically)
   * @return [0 .. 1]
   */
  public float estimatedFractionComplete();
}