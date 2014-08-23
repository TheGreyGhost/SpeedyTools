package speedytools.serverside.worldmanipulation;

/**
 * User: The Grey Ghost
 * Date: 20/07/2014
 * Stores the Block Data (ID, metadata, lightvalue) as arrays
 */
public class BlockDataStoreArray implements BlockDataStore
{
  public BlockDataStoreArray(int i_xcount, int i_ycount, int i_zcount)
  {
    xCount = i_xcount;
    yCount = i_ycount;
    zCount = i_zcount;
    int numberOfBlocks = xCount * yCount * zCount;
    blockIDbits0to7 = new byte[numberOfBlocks];
    blockIDbits8to11andmetaData = new byte[numberOfBlocks];
    lightValues = new byte[numberOfBlocks];
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
    return (blockIDbits0to7[offset] & 0xff) | ((blockIDbits8to11andmetaData[offset] & 0x0f) << 4);
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
    blockIDbits0to7[offset] = (byte) (blockID & 0xff);
    blockIDbits8to11andmetaData[offset] = (byte) ((blockIDbits8to11andmetaData[offset] & 0xf0) | (blockID >> 8));
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
    return (blockIDbits8to11andmetaData[offset] & 0xf0) >> 4;
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
    blockIDbits8to11andmetaData[offset] = (byte) ((blockIDbits8to11andmetaData[offset] & 0x0f) | (metadata << 4));
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
    return lightValues[offset];
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
    lightValues[offset] = lightValue;
  }

  private byte blockIDbits0to7[];
  private byte blockIDbits8to11andmetaData[];
  private byte lightValues[];

  private int xCount;
  private int yCount;
  private int zCount;
}
