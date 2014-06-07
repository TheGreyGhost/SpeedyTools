package speedytools.serverside;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import speedytools.clientside.selections.VoxelSelection;

import java.util.HashMap;
import java.util.LinkedList;

WHAT I STILL NEED TO DO

1) FINISH SPLITTING WORLD FRAGMENT INTO COPY AND UNDO
2) WRITE CONSTRUCTORS
3) REWRITE writeToWorld AND WRITE undo

/**
 * User: The Grey Ghost
 * Date: 27/05/2014
 * Stores the block ID, metadata, NBT (TileEntity) data, and Entity data for a voxel selection
 * Typical usage:
 * (1) Create a WorldSelectionCopy, either cuboid (x,y,z) or from a VoxelSelection (with borderwidth typically 1)
 * (2)  readFromWorld() to read the blockStore from the world
 * (3) various get() and set() to manipulate the blockStore contents
 * (4) writeToWorld() to write the blockStore into the world and return a WorldSelectionUndo
 */
public class WorldSelectionCopy
{
//  public static final int MAX_X_SIZE = 256;
//  public static final int MAX_Y_SIZE = 256;
//  public static final int MAX_Z_SIZE = 256;

  /** the WorldFragment is initially filled with air
   *
   * @param i_xcount
   * @param i_ycount
   * @param i_zcount
   */
  public WorldSelectionCopy(int i_xcount, int i_ycount, int i_zcount)
  {
    worldFragment = new WorldFragment(i_xcount, i_ycount, i_zcount);
//    assert (i_xcount >= 0 && i_xcount <= MAX_X_SIZE);
//    assert (i_ycount >= 0 && i_ycount <= MAX_Y_SIZE);
//    assert (i_zcount >= 0 && i_zcount <= MAX_Z_SIZE);
    xCount = i_xcount;
    yCount = i_ycount;
    zCount = i_zcount;
  }

  /** Create a WorldFragment based on the given VoxelSelection
   *   Automatically creates a border if requested; the VoxelSelection will be centred within the WorldFragment
   * @param i_voxelSelection the voxel selection to base the WorldFragment on
   */
  public WorldSelectionCopy(VoxelSelection i_voxelSelection)
  {
    this(i_voxelSelection.getXsize(), i_voxelSelection.getYsize(), i_voxelSelection.getZsize());
    copyMask = i_voxelSelection;
  }

  /**
   * Read a section of the world into the WorldSelectionCopy
   * @param worldServer
   * @param wxOrigin the world x coordinate of the [0,0,0] corner of the WorldFragment
   * @param wyOrigin the world y coordinate of the [0,0,0] corner of the WorldFragment
   * @param wzOrigin the world z coordinate of the [0,0,0] corner of the WorldFragment
   */
  public void readFromWorld(WorldServer worldServer, int wxOrigin, int wyOrigin, int wzOrigin)
  {
    worldFragment.readFromWorld(worldServer, wxOrigin, wyOrigin, wzOrigin, copyMask);
  }

  /**
   * Write the WorldFragment to the world
   * @param worldServer
   * @param wxOrigin the world x coordinate corresponding to the [0,0,0] corner of the WorldFragment
   * @param wyOrigin the world y coordinate corresponding to the [0,0,0] corner of the WorldFragment
   * @param wzOrigin the world z coordinate corresponding to the [0,0,0] corner of the WorldFragment
   * @return a WorldSelectionUndo which can be used to undo the changes
   */
  public WorldSelectionUndo writeToWorld(WorldServer worldServer, int wxOrigin, int wyOrigin, int wzOrigin)
  {
    return worldFragment.writeToWorld(worldServer, wxOrigin, wyOrigin, wzOrigin, copyMask);
  }

  /**
   * compares the contents of the two WorldSelectionCopies
   * @param worldSelection1
   * @param worldSelection2
   * @return true if the contents are exactly the same, including the copyMask
   */
  public static boolean areTheseEqual(WorldSelectionCopy worldSelection1, WorldSelectionCopy worldSelection2)
  {
    WorldFragment worldFragment1 = worldSelection1.worldFragment;
    WorldFragment worldFragment2 = worldSelection2.worldFragment;
    for (int x = 0; x < worldSelection1.xCount; ++x ) {
      for (int y = 0; y < worldSelection1.yCount; ++y) {
        for (int z = 0; z < worldSelection1.zCount; ++z) {
          if (worldSelection1.copyMask.getVoxel(x, y, z) != worldSelection2.copyMask.getVoxel(x, y, z)) return false;
          if (worldSelection1.copyMask.getVoxel(x, y, z)) {
            if (worldFragment1.getBlockID(x, y, z) != worldFragment2.getBlockID(x, y, z)
                    || worldFragment1.getMetadata(x, y, z) != worldFragment2.getMetadata(x, y, z)) {
              return false;
            }
            if (worldFragment1.getTileEntityData(x, y, z) == null) {
              if (worldFragment2.getTileEntityData(x, y, z) != null) {
                return false;
              }
            } else {
              NBTTagCompound nbt1 = worldFragment1.getTileEntityData(x, y, z);
              WorldFragment.changeTileEntityNBTposition(nbt1, 0, 0, 0);
              NBTTagCompound nbt2 = worldFragment2.getTileEntityData(x, y, z);
              WorldFragment.changeTileEntityNBTposition(nbt2, 0, 0, 0);
              if (0 != nbt1.toString().compareTo(nbt2.toString())) {
                return false;
              }
            }
          }
        }
      }
    }
    return true;
  }

  private WorldFragment worldFragment;
  private VoxelSelection copyMask;                            // each set voxel corresponds to a valid block location in the store
  private int xCount;
  private int yCount;
  private int zCount;

//  private VoxelSelection borderMaskSelection;                       // each set voxel corresponds to a block which is not in the voxelselection but is potentially affected by it.
//  private VoxelSelection affectedNeighbours;                        // each set voxel corresponds to a block in the borderMask which was affected by the placement.
}
