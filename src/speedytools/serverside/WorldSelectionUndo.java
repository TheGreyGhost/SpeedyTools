package speedytools.serverside;

import net.minecraft.world.WorldServer;
import speedytools.clientside.selections.VoxelSelection;

import java.util.LinkedList;
import java.util.List;

/**
 * User: The Grey Ghost
 * Date: 27/05/2014
 * Stores the undo data (block ID, metadata, NBT (TileEntity) data, and Entity data) for a voxel selection.
 *
 * Typical usage:
 * (1) Create an empty WorldSelectionUndo
 * (2) writeToWorld() to write the supplied WorldFragment and store the undo information
 * (3) undoChanges() to roll back the changes made in writeToWorld.  Should be supplied with a list
 *       of WorldSelectionUndo objects that were performed after this one, so that their undo information
 *       can be updated.
 * (4) deleteUndoLayer to remove an undoLayer from the middle of a list of undoLayers
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
              && borderFragmentAfterWrite.doesVoxelMatch(undoWorldFragment, x, y, z)) {
            expandedSelection.clearVoxel(x, y, z);
          }
        }
      }
    }
    changedBlocksMask = expandedSelection;
  }

  /**
   * un-does the changes previously made by this WorldSelectionUndo, taking into account any subsequent
   *   undo layers which overlap this one
   * @param worldServer
   * @param undoLayers the list of subsequent undo layers
   */
  public void undoChanges(WorldServer worldServer, List<WorldSelectionUndo> undoLayers)
  {
    /* algorithm is:
       1) remove undoLayers which don't overlap the undoWorldFragment at all (quick cull on x,y,z extents)
       2) for each voxel in the undoWorldFragment, check if any subsequent undo layers overwrite it.
          if yes: change that undo layer voxel
          if no: set that voxel in the write mask
       3) write the undo data to the world using the write mask (of voxels not overlapped by any other layers.)
     */
    LinkedList<WorldSelectionUndo> culledUndoLayers = new LinkedList<WorldSelectionUndo>();

    for (WorldSelectionUndo undoLayer : undoLayers) {
      if (   wxOfOrigin <= undoLayer.wxOfOrigin + undoLayer.undoWorldFragment.getxCount()
          && wyOfOrigin <= undoLayer.wyOfOrigin + undoLayer.undoWorldFragment.getyCount()
          && wzOfOrigin <= undoLayer.wzOfOrigin + undoLayer.undoWorldFragment.getzCount()
          && wxOfOrigin + undoWorldFragment.getxCount() >= undoLayer.wxOfOrigin
          && wyOfOrigin + undoWorldFragment.getyCount() >= undoLayer.wyOfOrigin
          && wzOfOrigin + undoWorldFragment.getzCount() >= undoLayer.wzOfOrigin
         ) {
        culledUndoLayers.add(undoLayer);
      }
    }

    VoxelSelection worldWriteMask = new VoxelSelection(undoWorldFragment.getxCount(), undoWorldFragment.getyCount(), undoWorldFragment.getzCount());

    for (int y = 0; y < undoWorldFragment.getyCount(); ++y) {
      for (int x = 0; x < undoWorldFragment.getxCount(); ++x) {
        for (int z = 0; z < undoWorldFragment.getzCount(); ++z) {
          if (changedBlocksMask.getVoxel(x, y, z)) {
            boolean writeVoxelToWorld = true;
            for (WorldSelectionUndo undoLayer : culledUndoLayers) {
              if (undoLayer.changedBlocksMask.getVoxel(x + wxOfOrigin - undoLayer.wxOfOrigin,
                      y + wyOfOrigin - undoLayer.wyOfOrigin,
                      z + wzOfOrigin - undoLayer.wzOfOrigin)) {
                writeVoxelToWorld = false;
                undoLayer.undoWorldFragment.copyVoxelContents(x + wxOfOrigin - undoLayer.wxOfOrigin,
                        y + wyOfOrigin - undoLayer.wyOfOrigin,
                        z + wzOfOrigin - undoLayer.wzOfOrigin,
                        this.undoWorldFragment, x, y, z);
                break;
              }
            }
            if (writeVoxelToWorld) {
              worldWriteMask.setVoxel(x, y, z);
            }
          }
        }
      }
    }
    undoWorldFragment.writeToWorld(worldServer, wxOfOrigin, wyOfOrigin, wzOfOrigin, worldWriteMask);
  }

  /**
   * deletes this UndoLayer from the list;
   * (integrates this undoLayer into any Layers that overlap it)
   * @param undoLayers the list of subsequent undo layers
   */
  public void deleteUndoLayer(List<WorldSelectionUndo> undoLayers)
  {
    /* algorithm is:
       1) remove undoLayers which don't overlap the undoWorldFragment at all (quick cull on x,y,z extents)
       2) for each voxel in the undoWorldFragment, check if any subsequent undo layers overwrite it.
          if yes: change that undo layer voxel
          if no: do nothing
     */
    LinkedList<WorldSelectionUndo> culledUndoLayers = new LinkedList<WorldSelectionUndo>();

    for (WorldSelectionUndo undoLayer : undoLayers) {
      if (   wxOfOrigin <= undoLayer.wxOfOrigin + undoLayer.undoWorldFragment.getxCount()
              && wyOfOrigin <= undoLayer.wyOfOrigin + undoLayer.undoWorldFragment.getyCount()
              && wzOfOrigin <= undoLayer.wzOfOrigin + undoLayer.undoWorldFragment.getzCount()
              && wxOfOrigin + undoWorldFragment.getxCount() >= undoLayer.wxOfOrigin
              && wyOfOrigin + undoWorldFragment.getyCount() >= undoLayer.wyOfOrigin
              && wzOfOrigin + undoWorldFragment.getzCount() >= undoLayer.wzOfOrigin
              ) {
        culledUndoLayers.add(undoLayer);
      }
    }

    for (int y = 0; y < undoWorldFragment.getyCount(); ++y) {
      for (int x = 0; x < undoWorldFragment.getxCount(); ++x) {
        for (int z = 0; z < undoWorldFragment.getzCount(); ++z) {
          for (WorldSelectionUndo undoLayer : culledUndoLayers) {
            if (undoLayer.changedBlocksMask.getVoxel(x + wxOfOrigin - undoLayer.wxOfOrigin,
                    y + wyOfOrigin - undoLayer.wyOfOrigin,
                    z + wzOfOrigin - undoLayer.wzOfOrigin)) {
              undoLayer.undoWorldFragment.copyVoxelContents(x + wxOfOrigin - undoLayer.wxOfOrigin,
                      y + wyOfOrigin - undoLayer.wyOfOrigin,
                      z + wzOfOrigin - undoLayer.wzOfOrigin,
                      this.undoWorldFragment, x, y, z);
              break;
            }
          }
        }
      }
    }
  }

  private WorldFragment undoWorldFragment;
  private VoxelSelection changedBlocksMask;
  private int wxOfOrigin;
  private int wyOfOrigin;
  private int wzOfOrigin;

}
