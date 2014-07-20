package speedytools.serverside.worldmanipulation;

/**
 * User: The Grey Ghost
 * Date: 20/07/2014
 * store data about blocks (ID, metadata, lightvalue)
 */
public interface BlockDataStore
{
  /**
   * gets the blockID at a particular location.
   * error if the location is not stored in this fragment
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   */
  public int getBlockID(int x, int y, int z);

  /**
   * sets the BlockID at a particular location
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   */
  public void setBlockID(int x, int y, int z, int blockID);

  /**
   * gets the metadata at a particular location
   * error if the location is not stored in this fragment
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   */
  public int getMetadata(int x, int y, int z);

  /**
   * sets the metadata at a particular location
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   */
  public void setMetadata(int x, int y, int z, int metadata);

  /**
   * gets the light value at a particular location.
   * error if the location is not stored in this fragment
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   * @return lightvalue (sky << 4 | block)
   */
  public byte getLightValue(int x, int y, int z);

  /**
   * sets the light value at a particular location
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   * @param lightValue lightvalue (sky << 4 | block)
   */
  public void setLightValue(int x, int y, int z, byte lightValue);
}
