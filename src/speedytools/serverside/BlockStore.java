package speedytools.serverside;

import net.minecraft.nbt.NBTTagCompound;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * User: The Grey Ghost
 * Date: 27/05/2014
 * stores the block ID, metadata, NBT (TileEntity) data, and Entity data for a cuboid region
 */
public class BlockStore
{
  public static final int MAX_X_SIZE = 256;
  public static final int MAX_Y_SIZE = 256;
  public static final int MAX_Z_SIZE = 256;

  /** the Blockstore is initially filled with air
   *
   * @param i_xcount
   * @param i_ycount
   * @param i_zcount
   */
  public BlockStore(int i_xcount, int i_ycount, int i_zcount)
  {
    assert (i_xcount >= 0 && i_xcount <= MAX_X_SIZE);
    assert (i_ycount >= 0 && i_ycount <= MAX_Y_SIZE);
    assert (i_zcount >= 0 && i_zcount <= MAX_Z_SIZE);
    xcount = i_xcount;
    ycount = i_ycount;
    zcount = i_zcount;

    int numberOfBlocks = xcount * ycount * zcount;
    blockIDbits0to7 = new byte[numberOfBlocks];
    blockIDbits8to11andmetaData = new byte[numberOfBlocks];
    tileEntityData = new HashMap<Integer, NBTTagCompound>();
    entityData = new HashMap<Integer, LinkedList<NBTTagCompound>>();
  }

  /**
   * gets the blockID at a particular location
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   */
  public int getBlockID(int x, int y, int z)
  {
    assert (x >= 0 && x < xcount);
    assert (y >= 0 && y < ycount);
    assert (z >= 0 && z < zcount);
    final int offset = y * xcount * zcount + z * xcount + x;
    return (blockIDbits0to7[offset] & 0xff) | ( (blockIDbits8to11andmetaData[offset] & 0x0f) << 4);
  }

  /**
   * sets the BlockID at a particular location
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   */
  public void setBlockID(int x, int y, int z, int blockID)
  {
    assert (x >= 0 && x < xcount);
    assert (y >= 0 && y < ycount);
    assert (z >= 0 && z < zcount);
    assert (blockID >= 0 && blockID <= 0xfff);
    final int offset = y * xcount * zcount + z * xcount + x;
    blockIDbits0to7[offset] = (byte)(blockID & 0xff);
    blockIDbits8to11andmetaData[offset] = (byte)((blockIDbits8to11andmetaData[offset] & 0xf0) | (blockID >> 8) );
  }

  /**
   * gets the metadata at a particular location
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   */
  public int getMetadata(int x, int y, int z)
  {
    assert (x >= 0 && x < xcount);
    assert (y >= 0 && y < ycount);
    assert (z >= 0 && z < zcount);
    final int offset = y * xcount * zcount + z * xcount + x;
    return (blockIDbits8to11andmetaData[offset] & 0xf0) >> 4;
  }

  /**
   * sets the metadata at a particular location
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   */
  public void setMetadata(int x, int y, int z, int metadata)
  {
    assert (x >= 0 && x < xcount);
    assert (y >= 0 && y < ycount);
    assert (z >= 0 && z < zcount);
    assert (metadata >= 0 && metadata <= 0x0f);
    final int offset = y * xcount * zcount + z * xcount + x;
    blockIDbits8to11andmetaData[offset] = (byte)((blockIDbits8to11andmetaData[offset] & 0x0f) | (metadata << 4) );
  }

  /**
   * Adds an entity to the block store, at the given position
   * @param x  entity position relative to the block origin [0, 0, 0]
   * @param nbtData NBT data of the entity
   */
  public void addEntity(double x, double y, double z, NBTTagCompound nbtData)
  {
    assert (x >= 0 && x < xcount);
    assert (y >= 0 && y < ycount);
    assert (z >= 0 && z < zcount);
    assert (nbtData != null);

    long wx = Math.round(Math.floor(x));
    long wy = Math.round(Math.floor(y));
    long wz = Math.round(Math.floor(z));
    final int offset =   (int)wy * xcount * zcount
                       + (int)wz * xcount
                       + (int)wx;
    LinkedList<NBTTagCompound> entitiesAtThisBlock;
    entitiesAtThisBlock = entityData.get(offset);
    if (entitiesAtThisBlock == null) {
      entitiesAtThisBlock = new LinkedList<NBTTagCompound>();
      entityData.put(offset, entitiesAtThisBlock);
    }
    entitiesAtThisBlock.add(nbtData);
  }

  /**
   * Returns a list of all entities whose [x,y,z] lies within the given block
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   * @return
   */
  public LinkedList<NBTTagCompound> getEntitiesAtBlock(int x, int y, int z)
  {
    final int offset = y * xcount * zcount + z * xcount + x;
    return entityData.get(offset);
  }

  /**
   * returns the NBT data for the TileEntity at the given location, or null if no TileEntity there
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   * @return the TileEntity NBT, or null if no TileEntity here
   */
  public NBTTagCompound getTileEntityData(int x, int y, int z)
  {
    final int offset = y * xcount * zcount + z * xcount + x;
    return tileEntityData.get(offset);
  }

  /**
   * returns the NBT data for the TileEntity at the given location, or null if no TileEntity there
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   * @return the TileEntity NBT, or null if no TileEntity here
   */
  public void setTileEntityData(int x, int y, int z, NBTTagCompound nbtData)
  {
    final int offset = y * xcount * zcount + z * xcount + x;
    if (nbtData == null) {
      tileEntityData.remove(offset);
    } else {
      tileEntityData.put(offset, nbtData);
    }
  }

  public int getXcount() {
    return xcount;
  }

  public int getYcount() {
    return ycount;
  }

  public int getZcount() {
    return zcount;
  }

  private int xcount;
  private int ycount;
  private int zcount;

  private byte blockIDbits0to7[];
  private byte blockIDbits8to11andmetaData[];
  private HashMap<Integer, NBTTagCompound> tileEntityData;
  private HashMap<Integer, LinkedList<NBTTagCompound>> entityData;
}
