package speedytools.serverside.worldmanipulation;

import java.util.HashMap;

/**
 * User: The Grey Ghost
 * Date: 20/07/2014
 * Stores the Block Data (ID, metadata, lightvalue) as arrays
 */
public class BlockDataStoreSparse implements BlockDataStore
{
  public BlockDataStoreSparse(int i_xcount, int i_ycount, int i_zcount, int estimatedElementsUsed)
  {
    xCount = i_xcount;
    yCount = i_ycount;
    zCount = i_zcount;
    sparseData = new HashMap<Integer, Integer>(estimatedElementsUsed);
  }
  /**
   * gets the blockID at a particular location.
   * error if the location is not stored in this fragment
   *
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   */
  @Override
  public int getBlockID(int x, int y, int z) {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    final int offset = y * xCount * zCount + z * xCount + x;
    Integer data = sparseData.get(offset);
    return data == null ? 0 : (data & 0xfff);
  }

  /**
   * sets the BlockID at a particular location
   *
   * @param x       x position relative to the block origin [0,0,0]
   * @param y       y position relative to the block origin [0,0,0]
   * @param z       z position relative to the block origin [0,0,0]
   * @param blockID
   */
  @Override
  public void setBlockID(int x, int y, int z, int blockID) {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    final int offset = y * xCount * zCount + z * xCount + x;
    Integer data = sparseData.get(offset);
    sparseData.put(offset, (blockID | (data == null ? 0 : (data & ~0xfff))) );
  }

  /**
   * gets the metadata at a particular location
   * error if the location is not stored in this fragment
   *
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   */
  @Override
  public int getMetadata(int x, int y, int z) {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    final int offset = y * xCount * zCount + z * xCount + x;
    Integer data = sparseData.get(offset);
    return data == null ? 0 : ((data >> 12) & 0x0f);
  }

  /**
   * sets the metadata at a particular location
   *
   * @param x        x position relative to the block origin [0,0,0]
   * @param y        y position relative to the block origin [0,0,0]
   * @param z        z position relative to the block origin [0,0,0]
   * @param metadata
   */
  @Override
  public void setMetadata(int x, int y, int z, int metadata) {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    final int offset = y * xCount * zCount + z * xCount + x;
    Integer data = sparseData.get(offset);
    sparseData.put(offset, (metadata << 12) | (data == null ? 0 : (data & ~0xf000)) );
  }

  /**
   * gets the light value at a particular location.
   * error if the location is not stored in this fragment
   *
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   * @return lightvalue (sky << 4 | block)
   */
  @Override
  public byte getLightValue(int x, int y, int z) {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    final int offset = y * xCount * zCount + z * xCount + x;
    Integer data = sparseData.get(offset);
    return data == null ? 0 : (byte)(data >> 16);
  }

  /**
   * sets the light value at a particular location
   *
   * @param x          x position relative to the block origin [0,0,0]
   * @param y          y position relative to the block origin [0,0,0]
   * @param z          z position relative to the block origin [0,0,0]
   * @param lightValue lightvalue (sky << 4 | block)
   */
  @Override
  public void setLightValue(int x, int y, int z, byte lightValue) {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    final int offset = y * xCount * zCount + z * xCount + x;
    Integer data = sparseData.get(offset);
    sparseData.put(offset, (lightValue << 16) | (data == null ? 0 : data & ~0xff0000));
  }

  private HashMap<Integer, Integer> sparseData;

  private int xCount;
  private int yCount;
  private int zCount;
}
