package speedytools.serverside;

import net.minecraft.world.WorldServer;
import speedytools.clientside.selections.VoxelSelection;

/**
 * User: The Grey Ghost
 * Date: 27/05/2014
 * Stores the undo data (block ID, metadata, NBT (TileEntity) data, and Entity data) for a voxel selection.
 *
 * Typical usage:
 * (1) Create a WorldSelectionUndo from VoxelSelection
 * (2)  readFromWorld() to read the blockStore from the world
 * (3) various get() and set() to manipulate the blockStore contents
 * (4) writeToWorld() to write the blockStore into the world and return an undo WorldFragment
 * (5) undoWriteToWorld() to undo a previous write
 */
public class WorldSelectionUndo
{
  public WorldSelectionUndo()
  {
  }

  /**
   * writes the given WorldFragment into the world, saving enough information to allow for a subsequent undo
   * @param worldServer
   * @param fragmentToWrite
   * @param i_wxOfOrigin
   * @param i_wyOfOrigin
   * @param i_wzOfOrigin
   */
  public void writeToWorld(WorldServer worldServer, WorldFragment fragmentToWrite, int i_wxOfOrigin, int i_wyOfOrigin, int i_wzOfOrigin)
  {
    /* algorithm is:
       (1) create a border mask for the fragment to be written, i.e. a mask showing all voxels which are adjacent to a set voxel in the fragment.
       (2) save the world data for the fragment voxels and the border mask voxels
       (3) write the fragment data into the world
       (4) find out which voxels in the border mask were unaffected by the writing into the world, and remove them from the undo mask
     */

    final int BORDER_WIDTH = 1;
    wxOfOrigin = i_wxOfOrigin - BORDER_WIDTH;
    wyOfOrigin = i_wyOfOrigin - BORDER_WIDTH;
    wzOfOrigin = i_wzOfOrigin - BORDER_WIDTH;

    VoxelSelection expandedSelection = fragmentToWrite.getVoxelsWithStoredData().makeCopyWithEmptyBorder(BORDER_WIDTH);
    VoxelSelection borderMask = expandedSelection.generateBorderMask();
    expandedSelection.union(borderMask);

    undoWorldFragment = new WorldFragment(expandedSelection.getXsize(), expandedSelection.getYsize(), expandedSelection.getZsize());
    undoWorldFragment.readFromWorld(worldServer, wxOfOrigin, wyOfOrigin, wzOfOrigin, expandedSelection);

    fragmentToWrite.writeToWorld(worldServer, i_wxOfOrigin, i_wyOfOrigin, i_wzOfOrigin, null);

    WorldFragment borderFragmentAfterWrite = new WorldFragment(borderMask.getXsize(), borderMask.getYsize(), borderMask.getZsize());
    borderFragmentAfterWrite.readFromWorld(worldServer, wxOfOrigin, wyOfOrigin, wzOfOrigin, borderMask);

    for (int y = 0; y < borderMask.getYsize(); ++y) {
      for (int x = 0; x < borderMask.getXsize(); ++x) {
        for (int z = 0; z < borderMask.getZsize(); ++z) {
          if (borderMask.getVoxel(x, y, z)
              && borderFragmentAfterWrite.doesVoxelMatch(borderFragmentAfterWrite, x, y, z)) {
            expandedSelection.clearVoxel(x, y, z);
          }
        }
      }
    }
    changedBlocksMask = expandedSelection;

  }


  /**
   * Read a section of the world into the WorldSelectionUndo.
   * If the voxel selection is defined, only reads those voxels, otherwise reads the entire block
   * @param worldServer
   */
  public void readFromWorld(WorldServer worldServer)
  {

    undoWorldFragment.readFromWorld(worldServer, wxOfOrigin, wyOfOrigin, wzOfOrigin, );
  }

  public void checkNeighboursForChanges(int wx, int wy, int wz)
  {

  }


  private WorldFragment undoWorldFragment;
//  private VoxelSelection borderMask;
//  private VoxelSelection
  private VoxelSelection changedBlocksMask;
  private int wxOfOrigin;
  private int wyOfOrigin;
  private int wzOfOrigin;

}
