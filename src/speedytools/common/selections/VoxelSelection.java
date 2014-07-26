package speedytools.common.selections;

import cpw.mods.fml.common.FMLLog;
import speedytools.common.utilities.ErrorLog;
import speedytools.common.utilities.Pair;
import speedytools.common.utilities.QuadOrientation;

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

  public VoxelSelection(int xSize, int ySize, int zSize)
  {
    resize(xSize, ySize, zSize);
  }

  /** deep copy
   *
   * @param source
   */
  public VoxelSelection(VoxelSelection source)
  {
    resize(source.xSize, source.ySize, source.zSize);
    voxels = (BitSet)source.voxels.clone();
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
    voxels.set(0, xSize * ySize * zSize);
  }

  /**
   * set the value of this voxel (or does nothing if x,y,z out of range)
   * @param x
   * @param y
   * @param z
   */
  public void setVoxel(int x, int y, int z)
  {
    if (   x < 0 || x >= xSize
        || y < 0 || y >= ySize
        || z < 0 || z >= zSize) {
      return;
    }
   voxels.set(x + xSize * (y + ySize * z));
  }

  /**
   * set the value of this voxel (or does nothing if x,y,z out of range)
   * @param x
   * @param y
   * @param z
   */
  public void clearVoxel(int x, int y, int z)
  {
    if (   x < 0 || x >= xSize
        || y < 0 || y >= ySize
        || z < 0 || z >= zSize) {
      return;
    }
    voxels.clear(x + xSize * (y + ySize * z));
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
    if (   x < 0 || x >= xSize
        || y < 0 || y >= ySize
        || z < 0 || z >= zSize) {
      return false;
    }

    return voxels.get(x + xSize *(y + ySize * z) );
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
    xSize = x;
    ySize = y;
    zSize = z;
    if (voxels == null) {
      voxels = new BitSet(xSize * ySize * zSize);     // default to all false
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
      outputStream.writeInt(xSize);
      outputStream.writeInt(ySize);
      outputStream.writeInt(zSize);

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
      xSize = newXsize;
      ySize = newYsize;
      zSize = newZsize;
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

  public VoxelSelection makeCopyWithEmptyBorder(int borderWidth)
  {
    VoxelSelection copy = new VoxelSelection(xSize + 2 * borderWidth, ySize + 2 * borderWidth, zSize + 2 * borderWidth);
    for (int x = 0; x < xSize; ++x) {
      for (int y = 0; y < ySize; ++y) {
        for (int z = 0; z < zSize; ++z) {
          if (getVoxel(x,y,z)) {
            copy.setVoxel(x + borderWidth, y + borderWidth, z + borderWidth);
          }
        }
      }
    }
    return copy;
  }

  /** splits this VoxelSelection into two, in accordance with the supplied mask:
   * voxels which are set in the mask are removed from this VoxelSelection and placed into the new VoxelSelection
   * @param mask
   * @param xOffsetOfMask the origin of the mask relative to the origin of this fragment
   * @param yOffsetOfMask
   * @param zOffsetOfMask
   * @return a new WorldFragment containing those voxels that are also present in the mask.  Shallow copy of the original
   */
  public VoxelSelection splitByMask(VoxelSelection mask, int xOffsetOfMask, int yOffsetOfMask, int zOffsetOfMask)
  {
    VoxelSelection overlapped = new VoxelSelection(this);
    int xSize = mask.getxSize();
    int ySize = mask.getySize();
    int zSize = mask.getzSize();
    for (int x = 0; x < xSize; ++x) {
      for (int z = 0; z < zSize; ++z) {
        for (int y = 0; y < ySize; ++y) {
          if (mask.getVoxel(x, y, z)) {
            if (getVoxel(x - xOffsetOfMask, y - yOffsetOfMask, z - zOffsetOfMask)) {
              overlapped.setVoxel(x - xOffsetOfMask, y - yOffsetOfMask, z - zOffsetOfMask);
              this.clearVoxel(x - xOffsetOfMask, y - yOffsetOfMask, z - zOffsetOfMask);
            }
          }
        }
      }
    }
    return overlapped;
  }


  /**
   * Creates a reoriented copy of this VoxelSelection and add a border of blank voxels on all faces.
   * @param borderWidth the number of voxels in the border added to all faces
   * @param orientation the new orientation (flip, rotate)
   * @param wxzOrigin takes the current [wxMin, wzMin] and returns the new [wxMin, wzMin] - if the selection is rotated this may change
   * @return the new VoxelSelection
   */
  public VoxelSelection makeReorientedCopyWithBorder(QuadOrientation orientation, int borderWidth, Pair<Integer, Integer> wxzOrigin)
  {
    int wxMin = wxzOrigin.getFirst();
    int wzMin = wxzOrigin.getSecond();
    Pair<Integer, Integer> xrange = new Pair<Integer, Integer>(wxMin, wxMin + xSize - 1);
    Pair<Integer, Integer> zrange = new Pair<Integer, Integer>(wzMin, wzMin + zSize - 1);
    orientation.getWXZranges(xrange, zrange);
    int wxNewMin = xrange.getFirst();
    int wzNewMin = zrange.getFirst();
    wxzOrigin.setFirst(wxNewMin);
    wxzOrigin.setSecond(wzNewMin);


    int newXsize = (xrange.getSecond() - xrange.getFirst() + 1) + 2 * borderWidth;
    int newYsize = ySize + 2* borderWidth;
    int newZsize = (zrange.getSecond() - zrange.getFirst() + 1) + 2 * borderWidth;
    VoxelSelection copy = new VoxelSelection(newXsize, newYsize, newZsize);
    for (int x = 0; x < xSize; ++x) {
      for (int y = 0; y < ySize; ++y) {
        for (int z = 0; z < zSize; ++z) {
          if (getVoxel(x,y,z)) {
            copy.setVoxel(orientation.calcWXfromXZ(x, z) + borderWidth - wxNewMin,
                          y + borderWidth,
                          orientation.calcWZfromXZ(x, z) + borderWidth - wzNewMin);
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
    VoxelSelection copy = new VoxelSelection(xSize, ySize, zSize);
    for (int x = 0; x < xSize; ++x) {
      for (int y = 0; y < ySize; ++y) {
        for (int z = 0; z < zSize; ++z) {
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

  /**
   * clear all voxels outside of the given ranges (inclusive)
   * @param yMin
   * @param yMax
   */
  public void clipToYrange(int yMin, int yMax)
  {
    for (int y = 0; y < ySize; ++y) {
      if (y < yMin || y > yMax) {
        for (int x = 0; x < xSize; ++x) {
          for (int z = 0; z < zSize; ++z) {
            clearVoxel(x,y,z);
          }
        }
      }
    }
  }

  /** checks whether all of the set voxels in voxelSelection are also set in this VoxelSelection
   * @param voxelSelection the voxels to test against.  must be the same size as 'this'.
   * @return true if all of the set voxels in voxelSelection are also set in this VoxelSelection
   */
  public boolean containsAllOfThisMask(VoxelSelection voxelSelection)
  {
    assert(voxelSelection.xSize == this.xSize && voxelSelection.ySize == this.ySize && voxelSelection.zSize == this.zSize);
    BitSet maskBitsNotInThis = (BitSet)voxelSelection.voxels.clone();
    maskBitsNotInThis.andNot(this.voxels);
    return maskBitsNotInThis.length() == 0;
  }

  /**
   * updates this to include all set Voxels in both this and in voxelSelection
   * @param voxelSelection the voxels to be set.  Must be the same size as 'this'.
   */
  public void union(VoxelSelection voxelSelection)
  {
    assert(voxelSelection.xSize == this.xSize && voxelSelection.ySize == this.ySize && voxelSelection.zSize == this.zSize);
    voxels.or(voxelSelection.voxels);
  }

  private BitSet voxels;

  public int getxSize() {
    return xSize;
  }

  public int getySize() {
    return ySize;
  }

  public int getzSize() {
    return zSize;
  }

  public int getSetVoxelsCount()
  {
    return voxels.cardinality();
  }
  protected int xSize;
  protected int ySize;
  protected int zSize;
}
