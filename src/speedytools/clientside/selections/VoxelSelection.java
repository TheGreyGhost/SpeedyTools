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

  public void clearAll(int x, int y, int z)
  {
    resize(x, y, z);
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

  private BitSet voxels;
  private int xsize;
  private int ysize;
  private int zsize;
}
