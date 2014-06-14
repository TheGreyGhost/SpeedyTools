package speedytools.serverside;

import net.minecraft.world.WorldServer;
import speedytools.clientside.selections.VoxelSelection;

import java.util.LinkedList;
import java.util.List;

/**
 * User: The Grey Ghost
 * Date: 27/05/2014
 * WorldSelectionUndo is used to record a clone tool change to the World and allow it to be subsequently reversed.
 * Each clone tool change to the World is represented by a WorldSelectionUndo instance, and stores
 * the old block ID, metadata, NBT (TileEntity) data, and Entity data for the parts of the World which changed.
 * WorldHistory is typically used to keep track of a sequence of changes (WorldSelectionUndo instances), for example
 *  first A, then B, then C
 * The philosophy behind the WorldSelectionUndo is that, if action B is undone, the world should be as close as possible
 *   to what it would look like if the action B had never been performed, i.e. the state of the World is rewound to its
 *   initial state, followed by action A, then followed by action C.
 *   Likewise, when the action B is made permanent using makePermanent(), the world and other WorldSelectionUndo
 *   will be changed so that a subsequent undo of A will give the same result as if the World were rewound to the initial state,
 *   and then action B is performed followed by action C.
 * Under some conditions, out-of-order undos will not return the world exactly to the start condition.  This arises when
 * copying the selection causes adjacent blocks to break, and these are then overwritten by an overlapping copy.
 * For example:
 * There is a torch hanging on a wall block.
 * Action A replaces a torch with a new block;
 * Action B replaces the support wall block for that torch with a ladder;
 * undo (A) puts the torch back, but it's now next to a ladder so it breaks immediately; then
 * undo (B) replaces the support wall, but the torch has been broken and will not be replaced in this step
 * This would be relatively complicated to fix and is unlikely to arise much in practice so it won't be fixed at least for now.
 * Typical usage:
 * (1) Create an empty WorldSelectionUndo
 * (2) writeToWorld() to write the supplied WorldFragment and store the undo information
 * (3) undoChanges() to roll back the changes made in writeToWorld.  Should be supplied with a list
 *       of WorldSelectionUndo objects that were performed after this one, so that their undo information
 *       can be updated.
 * (4) makePermanent() to adjust the World and other undo layers to make this undo permanent, typically so that it can
 *     be freed up.
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
       (4) find out which voxels in the border mask were unaffected by the writing into the world, and remove them from the undo mask (changedBlocksMask)
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
   * @param subsequentUndoLayers the list of subsequent undo layers
   */
  public void undoChanges(WorldServer worldServer, List<WorldSelectionUndo> subsequentUndoLayers)
  {
    /* algorithm is:
       1) remove undoLayers which don't overlap the undoWorldFragment at all (quick cull on x,y,z extents)
       2) for each voxel in the undoWorldFragment, check if any subsequent undo layers overwrite it.
          if yes: change that undo layer voxel
          if no: set that voxel in the write mask
       3) write the undo data to the world using the write mask (of voxels not overlapped by any other layers.)
     */
    LinkedList<WorldSelectionUndo> overlappingUndoLayers = new LinkedList<WorldSelectionUndo>();

    for (WorldSelectionUndo undoLayer : subsequentUndoLayers) {
      if (   wxOfOrigin <= undoLayer.wxOfOrigin + undoLayer.undoWorldFragment.getxCount()
          && wyOfOrigin <= undoLayer.wyOfOrigin + undoLayer.undoWorldFragment.getyCount()
          && wzOfOrigin <= undoLayer.wzOfOrigin + undoLayer.undoWorldFragment.getzCount()
          && wxOfOrigin + undoWorldFragment.getxCount() >= undoLayer.wxOfOrigin
          && wyOfOrigin + undoWorldFragment.getyCount() >= undoLayer.wyOfOrigin
          && wzOfOrigin + undoWorldFragment.getzCount() >= undoLayer.wzOfOrigin
         ) {
        overlappingUndoLayers.add(undoLayer);
      }
    }

    VoxelSelection worldWriteMask = new VoxelSelection(undoWorldFragment.getxCount(), undoWorldFragment.getyCount(), undoWorldFragment.getzCount());

    for (int y = 0; y < undoWorldFragment.getyCount(); ++y) {
      for (int x = 0; x < undoWorldFragment.getxCount(); ++x) {
        for (int z = 0; z < undoWorldFragment.getzCount(); ++z) {
          if (changedBlocksMask.getVoxel(x, y, z)) {
            boolean writeVoxelToWorld = true;
            for (WorldSelectionUndo undoLayer : overlappingUndoLayers) {
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
   * (integrates this undoLayer into any preceding Layers that overlap it, so that if any of the
   *  preceding layers are undone, the outcome will not overwrite this permanent change.
   *  For example:
   *  Action A followed by Action B.
   *  If Action B is made permanent, and then Action A is undone, the outcome should be as if the
   *  world were rewound to the initial state and then action B performed.
   * @param precedingUndoLayers the list of undo layers before this one, must be descending order of time, i.e. latest first
   * subsequentUndoLayers the list of undo layers after this one, must be ascending order of time, i.e. earliest first
   */
  public void makePermanent(WorldServer worldServer, List<WorldSelectionUndo> precedingUndoLayers)//, List<WorldSelectionUndo> subsequentUndoLayers)
  {
    /* In order to remove this undoLayer completely, we need to propagate undo information backwards
       For example:
       Initial State then Action A then Action B then Action C stores the following undo information
       A[stores initial], B[stores A], C[stores B].
       After removing action B, this needs to look like
        C[Stores B]
       In other words: The undo voxel for Action A is deleted because it will always be overwritten by B

       algorithm is:
       1) remove undoLayers which don't overlap the undoWorldFragment at all (quick cull on x,y,z extents)
       2) for each voxel in the undoWorldFragment B, remove the undo information for all overlapping voxels in all preceding layers
     */
    LinkedList<WorldSelectionUndo> precedingOverlaps = new LinkedList<WorldSelectionUndo>();
//    LinkedList<WorldSelectionUndo> subsequentOverlaps = new LinkedList<WorldSelectionUndo>();

    for (WorldSelectionUndo undoLayer : precedingUndoLayers) {
      if (wxOfOrigin <= undoLayer.wxOfOrigin + undoLayer.undoWorldFragment.getxCount()
              && wyOfOrigin <= undoLayer.wyOfOrigin + undoLayer.undoWorldFragment.getyCount()
              && wzOfOrigin <= undoLayer.wzOfOrigin + undoLayer.undoWorldFragment.getzCount()
              && wxOfOrigin + undoWorldFragment.getxCount() >= undoLayer.wxOfOrigin
              && wyOfOrigin + undoWorldFragment.getyCount() >= undoLayer.wyOfOrigin
              && wzOfOrigin + undoWorldFragment.getzCount() >= undoLayer.wzOfOrigin
              ) {
        precedingOverlaps.add(undoLayer);
      }
    }


//    for (WorldSelectionUndo undoLayer : subsequentUndoLayers) {
//      if (   wxOfOrigin <= undoLayer.wxOfOrigin + undoLayer.undoWorldFragment.getxCount()
//              && wyOfOrigin <= undoLayer.wyOfOrigin + undoLayer.undoWorldFragment.getyCount()
//              && wzOfOrigin <= undoLayer.wzOfOrigin + undoLayer.undoWorldFragment.getzCount()
//              && wxOfOrigin + undoWorldFragment.getxCount() >= undoLayer.wxOfOrigin
//              && wyOfOrigin + undoWorldFragment.getyCount() >= undoLayer.wyOfOrigin
//              && wzOfOrigin + undoWorldFragment.getzCount() >= undoLayer.wzOfOrigin
//              ) {
//        subsequentOverlaps.add(undoLayer);
//      }
//    }

    for (int y = 0; y < undoWorldFragment.getyCount(); ++y) {
      for (int x = 0; x < undoWorldFragment.getxCount(); ++x) {
//        System.out.print(y + ";" + x + ";");
        for (int z = 0; z < undoWorldFragment.getzCount(); ++z) {
//          char symbol = '.';
          if (this.changedBlocksMask.getVoxel(x, y, z)) {
            for (WorldSelectionUndo precedingUndo : precedingOverlaps) {
              precedingUndo.changedBlocksMask.clearVoxel(x, y, z);
            }
          }
        }  // for z
      } // for x
    } // for y

  }
//              symbol = 'O';
//              if (precedingUndo.changedBlocksMask.getVoxel(x + wxOfOrigin - precedingUndo.wxOfOrigin,
//                                                           y + wyOfOrigin - precedingUndo.wyOfOrigin,
//                                                           z + wzOfOrigin - precedingUndo.wzOfOrigin)){
//
//              }
//                boolean subsequentOverlapFound = false;
//                for (WorldSelectionUndo subsequentUndo: subsequentOverlaps) {
//                  if (subsequentUndo.changedBlocksMask.getVoxel(x + wxOfOrigin - subsequentUndo.wxOfOrigin,
//                                                                y + wyOfOrigin - subsequentUndo.wyOfOrigin,
//                                                                z + wzOfOrigin - subsequentUndo.wzOfOrigin)) {
//                    subsequentOverlapFound = true;
//                    precedingUndo.undoWorldFragment.copyVoxelContents(x + wxOfOrigin - precedingUndo.wxOfOrigin,
//                                                                      y + wyOfOrigin - precedingUndo.wyOfOrigin,
//                                                                      z + wzOfOrigin - precedingUndo.wzOfOrigin,
//                                                                      subsequentUndo.undoWorldFragment,
//                                                                      x + wxOfOrigin - subsequentUndo.wxOfOrigin,
//                                                                      y + wyOfOrigin - subsequentUndo.wyOfOrigin,
//                                                                      z + wzOfOrigin - subsequentUndo.wzOfOrigin);
////                    subsequentUndo.undoWorldFragment.copyVoxelContents(x + wxOfOrigin - subsequentUndo.wxOfOrigin,
////                                                                      y + wyOfOrigin - subsequentUndo.wyOfOrigin,
////                                                                      z + wzOfOrigin - subsequentUndo.wzOfOrigin,
////                                                                     this.undoWorldFragment, x, y, z);
//                    break;
//                  }
//                }
//
//                if (!subsequentOverlapFound) {
//                  precedingUndo.undoWorldFragment.readSingleBlockFromWorld(worldServer, x + wxOfOrigin, y + wyOfOrigin, z + wzOfOrigin,
//                                                                                        wxOfOrigin, wyOfOrigin, wzOfOrigin);
//                }
//                break;
//              }
//            }
//          }
////          System.out.print(symbol);
//        }
////        System.out.println();
//      }
//    }
//  }

  /**
   * returns the undo metadata stored at a particular location (intended for debugging)
   * @param wx  world coordinates
   * @param wy
   * @param wz
   * @return the metadata at this location, or NULL if not stored
   */
  public Integer getStoredMetadata(int wx, int wy, int wz) {
    int x = wx - wxOfOrigin;
    int y = wy - wyOfOrigin;
    int z = wz - wzOfOrigin;
    if (!changedBlocksMask.getVoxel(x, y, z)) return null;
    return undoWorldFragment.getMetadata(x, y, z);
  }

  private WorldFragment undoWorldFragment;
  private VoxelSelection changedBlocksMask;
  private int wxOfOrigin;
  private int wyOfOrigin;
  private int wzOfOrigin;

}
