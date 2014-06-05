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
  public WorldSelectionUndo(VoxelSelection i_voxelSelection, int i_wxOfOrigin, int i_wyOfOrigin, int i_wzOfOrigin)
  {
    final int BORDER_WIDTH = 1;
    wxOfOrigin = i_wxOfOrigin - BORDER_WIDTH;
    wyOfOrigin = i_wyOfOrigin - BORDER_WIDTH;
    wzOfOrigin = i_wzOfOrigin - BORDER_WIDTH;
    VoxelSelection expandedSelection = i_voxelSelection.makeCopyWithEmptyBorder(BORDER_WIDTH);
    worldFragment = new WorldFragment(expandedSelection);
    borderMask = expandedSelection.generateBorderMask();
  }

  /**
   * Read a section of the world into the WorldSelectionUndo.
   * If the voxel selection is defined, only reads those voxels, otherwise reads the entire block
   * @param worldServer
   * @param wxOrigin the world x coordinate of the [0,0,0] corner of the WorldFragment
   * @param wyOrigin the world y coordinate of the [0,0,0] corner of the WorldFragment
   * @param wzOrigin the world z coordinate of the [0,0,0] corner of the WorldFragment
   * @param voxelSelection the blocks to read, or if null read the entire WorldFragment cuboid
   */
  public void readFromWorld(WorldServer worldServer, int wxOrigin, int wyOrigin, int wzOrigin, VoxelSelection voxelSelection)
  {

  }


  private WorldFragment worldFragment;
  private VoxelSelection borderMask;
  private int wxOfOrigin;
  private int wyOfOrigin;
  private int wzOfOrigin;

}
