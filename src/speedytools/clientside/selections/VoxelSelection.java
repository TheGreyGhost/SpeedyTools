package speedytools.clientside.selections;

import cpw.mods.fml.common.FMLLog;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.multipart.MultipartPacket;
import speedytools.common.utilities.ErrorLog;

import java.io.*;
import java.util.BitSet;

/**
 * User: The Grey Ghost
 * Date: 15/02/14
 */
public class VoxelSelection
{
  public static final int MAX_X_SIZE = 256;
  public static final int MAX_Y_SIZE = 256;
  public static final int MAX_Z_SIZE = 256;

  public VoxelSelection(int x, int y, int z)
  {
    resize(x, y, z);
  }

  public void clearAll()
  {
    voxels.clear();
  }

  public void resizeAndClear(int x, int y, int z)
  {
    resize(x, y, z);
  }

  public void setAll()
  {
    voxels.set(0, xsize * ysize * zsize);
  }

  /**
   * set the value of this voxel (or does nothing if x,y,z out of range)
   * @param x
   * @param y
   * @param z
   */
  public void setVoxel(int x, int y, int z)
  {
    if (   x < 0 || x >= xsize
        || y < 0 || y >= ysize
        || z < 0 || z >= zsize) {
      return;
    }
   voxels.set(x + xsize * (y + ysize * z));
  }

  /**
   * set the value of this voxel (or does nothing if x,y,z out of range)
   * @param x
   * @param y
   * @param z
   */
  public void clearVoxel(int x, int y, int z)
  {
    if (   x < 0 || x >= xsize
        || y < 0 || y >= ysize
        || z < 0 || z >= zsize) {
      return;
    }
    voxels.clear(x + xsize * (y + ysize * z));
  }

  /**
   * gets the value of this voxel
   * @param x
   * @param y
   * @param z
   * @return the voxel state, or false if x, y, or z is out of range
   */
  public boolean getVoxel(int x, int y, int z)
  {
    if (   x < 0 || x >= xsize
        || y < 0 || y >= ysize
        || z < 0 || z >= zsize) {
      return false;
    }

    return voxels.get(x + xsize *(y + ysize * z) );
  }

  private void resize(int x, int y, int z)
  {
    if (   x <= 0 || x > MAX_X_SIZE
            || y <= 0 || y > MAX_Y_SIZE
            || z <= 0 || z > MAX_Z_SIZE ) {
      FMLLog.severe("Out-of-range [x,y,z] in VoxelSelection constructor: [%d, %d, %d]", x, y, z);
      x = 1;
      y = 1;
      z = 1;
    }
    xsize = x;
    ysize = y;
    zsize = z;
    if (voxels == null) {
      voxels = new BitSet(xsize * ysize * zsize);     // default to all false
    } else {
      voxels.clear();
    }
  }

  /** serialise the VoxelSelection to a byte array
   * @return the serialised VoxelSelection, or null for failure
   */
  public ByteArrayOutputStream writeToBytes()
  {
    ByteArrayOutputStream bos = null;
    try {
      bos = new ByteArrayOutputStream();
      DataOutputStream outputStream = new DataOutputStream(bos);
      outputStream.writeInt(xsize);
      outputStream.writeInt(ysize);
      outputStream.writeInt(zsize);

      ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
      objectOutputStream.writeObject(voxels);
      objectOutputStream.close();
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Exception while converting VoxelSelection toDataArray:" + ioe);
      bos = null;
    }
    return bos;
  }

  /** fill this VoxelSelection using the serialised VoxelSelection byte array
   * @param byteArrayInputStream the bytearray containing the serialised VoxelSelection
   * @return true for success, false for failure (leaves selection untouched)
   */
  public boolean readFromBytes(ByteArrayInputStream byteArrayInputStream) {
    try {
      DataInputStream inputStream = new DataInputStream(byteArrayInputStream);

      int newXsize = inputStream.readInt();
      int newYsize = inputStream.readInt();
      int newZsize = inputStream.readInt();
      if (newXsize < 1 || newXsize > MAX_X_SIZE || newYsize < 1 || newYsize > MAX_Y_SIZE || newZsize < 1 || newZsize > MAX_Z_SIZE) {
        return false;
      }

      ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
      Object newVoxels = objectInputStream.readObject();
      if (! (newVoxels instanceof BitSet)) return false;
      xsize = newXsize;
      ysize = newYsize;
      zsize = newZsize;
      voxels = (BitSet)newVoxels;
    } catch (ClassNotFoundException cnfe) {
      ErrorLog.defaultLog().warning("Exception while VoxelSelection.readFromDataArray: " + cnfe);
      return false;
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Exception while VoxelSelection.readFromDataArray: " + ioe);
      return false;
    }
    return true;
  }

  /**
   * Creates a copy of this VoxelSelection, adding a border of blank voxels on all faces.
   * For example - if the VoxelSelection has size 5x6x7, and borderWidth is 2, the resulting VoxelSelection is 9x10x11 in size
   * And (eg) if the initial VoxelSelection is all set, the new selection will be all clear except from the box from [2,2,2] to [6,7,8] inclusive which will be all set
   * @param borderWidth the number of voxels in the border added to all faces
   * @return the new VoxelSelection
   */

  public VoxelSelection makeCopyWithBorder(int borderWidth)
  {
    VoxelSelection copy = new VoxelSelection(xsize + 2 * borderWidth, ysize + 2 * borderWidth, zsize + 2 * borderWidth);
    for (int x = 0; x < xsize; ++x) {
      for (int y = 0; y < ysize; ++y) {
        for (int z = 0; z < zsize; ++z) {
          if (getVoxel(x,y,z)) {
            setVoxel(x + borderWidth, y + borderWidth, z + borderWidth);
          }
        }
      }
    }
    return copy;
  }

  /** For the given VoxelSelection, make a "BorderMask" copy where all the empty voxels adjacent to a set voxel are marked as set.
   *  i.e. for a given [x,y,z]:
   *  1) if the voxel is set, the BorderMask voxel is clear
   *  2) if all of the six adjacent voxels are clear, the BorderMask voxel is clear
   *  3) otherwise, the BorderMask voxel is set.
   *
   * @return
   */
  public VoxelSelection generateBorderMask()
  {
    VoxelSelection copy = new VoxelSelection(xsize, ysize, zsize);
    for (int x = 0; x < xsize; ++x) {
      for (int y = 0; y < ysize; ++y) {
        for (int z = 0; z < zsize; ++z) {
          if (!getVoxel(x, y, z)) {
            if (getVoxel(x-1, y, z) || getVoxel(x+1, y, z) || getVoxel(x, y-1, z) || getVoxel(x, y+1, z) || getVoxel(x, y, z-1) || getVoxel(x, y, z+1)) {
              copy.setVoxel(x, y, z);
            }
          }
        }
      }
    }
    return copy;
  }

  private BitSet voxels;

  public int getXsize() {
    return xsize;
  }

  public int getYsize() {
    return ysize;
  }

  public int getZsize() {
    return zsize;
  }

  protected int xsize;
  protected int ysize;
  protected int zsize;
}
