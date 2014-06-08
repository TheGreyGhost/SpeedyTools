package speedytools.serverside.ingametester;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import speedytools.serverside.WorldFragment;
import speedytools.serverside.WorldSelectionUndo;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * User: The Grey Ghost
 * Date: 8/06/2014
 * Holds the undo history for the WorldServer.
 * Every player gets at least one undo.  They will get more if there is enough space.
 * If the history is full, these "extra" undo layers are discarded, oldest first.
 * The layers are grouped according to WorldServer (different dimensions will have different WorldServers)
 * Automatically gets rid of EntityPlayerMP and WorldServer which are no longer valid
 */
public class WorldHistory
{
  public WorldHistory(int maximumHistoryDepth)
  {
    assert (maximumHistoryDepth >= 1);
    maximumDepth = maximumHistoryDepth;
  }

  /** write the given fragment to the World, storing undo information
   * @param player
   * @param worldServer
   * @param fragmentToWrite
   * @param wxOfOrigin
   * @param wyOfOrigin
   * @param wzOfOrigin
   */
  public void writeToWorldWithUndo(EntityPlayerMP player, WorldServer worldServer, WorldFragment fragmentToWrite, int wxOfOrigin, int wyOfOrigin, int wzOfOrigin)
  {
    WorldSelectionUndo worldSelectionUndo = new WorldSelectionUndo();
    worldSelectionUndo.writeToWorld(worldServer, fragmentToWrite, wxOfOrigin, wyOfOrigin, wzOfOrigin);
    UndoLayerInfo undoLayerInfo = new UndoLayerInfo(System.nanoTime(), worldServer, player, worldSelectionUndo);
    undoLayers.add(undoLayerInfo);
    cullUndoLayers(maximumDepth);
  }

  /** perform  undo for the given player - finds the last undo that they did in the current WorldServer
   * @param player
   * @param worldServer
   * @return true for success, or failure if no undo found
   */
  public boolean performUndo(EntityPlayerMP player, WorldServer worldServer)
  {
    Iterator<UndoLayerInfo> undoLayerInfoIterator = undoLayers.descendingIterator();
    while  (undoLayerInfoIterator.hasNext()) {
      UndoLayerInfo undoLayerInfo = undoLayerInfoIterator.next();
      if (undoLayerInfo.worldServer.get() == worldServer
              && undoLayerInfo.entityPlayerMP.get() == player) {
        LinkedList<WorldSelectionUndo> subsequentUndoLayers = collateSubsequentUndoLayers(undoLayerInfo.creationTime, worldServer);
      }
    }
    return false;
  }

  /**
   * Tries to reduce the number of undoLayers to the target size
   * 1) culls all invalid layers (Player or WorldServer no longer exist)
   * 2) for each player with more than one undolayer, delete the extra layers, starting from oldest first
   * @param targetSize
   */
  private void cullUndoLayers(int targetSize)
  {
    // delete all invalid layers
    // iterate backwards to avoid unnecessary work updating Layers which will be deleted anyway
    Iterator<UndoLayerInfo> undoLayerInfoIterator = undoLayers.descendingIterator();
    while  (undoLayerInfoIterator.hasNext()) {
      UndoLayerInfo undoLayerInfo = undoLayerInfoIterator.next();
      if (undoLayerInfo.worldServer == null) {
        undoLayerInfoIterator.remove();
      } else {
        if (undoLayerInfo.entityPlayerMP == null) {
          LinkedList<WorldSelectionUndo> subsequentLayers = collateSubsequentUndoLayers(undoLayerInfo.creationTime, undoLayerInfo.worldServer.get());
          undoLayerInfo.worldSelectionUndo.deleteUndoLayer(subsequentLayers);
          undoLayerInfoIterator.remove();
        }
      }
    }
    if (undoLayers.size() <= targetSize) return;

    HashMap<EntityPlayerMP, Integer> playerUndoCount = new HashMap<EntityPlayerMP, Integer>();
    for (UndoLayerInfo undoLayerInfo : undoLayers) {
      EntityPlayerMP entityPlayerMP = undoLayerInfo.entityPlayerMP.get();
      assert (entityPlayerMP != null);
      if (!playerUndoCount.containsKey(entityPlayerMP)) {
        playerUndoCount.put(entityPlayerMP, 1);
      } else {
        playerUndoCount.put(entityPlayerMP, playerUndoCount.get(entityPlayerMP) + 1);
      }
    }

    int layersToDelete = undoLayers.size() - targetSize;
    assert (layersToDelete > 0);
    Iterator<UndoLayerInfo> excessIterator = undoLayers.iterator();
    while (layersToDelete > 0 && undoLayerInfoIterator.hasNext()) {
      UndoLayerInfo undoLayerInfo = excessIterator.next();
      EntityPlayerMP entityPlayerMP = undoLayerInfo.entityPlayerMP.get();
      assert (entityPlayerMP != null);
      if (playerUndoCount.get(entityPlayerMP) > 1) {
        LinkedList<WorldSelectionUndo> subsequentLayers = collateSubsequentUndoLayers(undoLayerInfo.creationTime, undoLayerInfo.worldServer.get());
        undoLayerInfo.worldSelectionUndo.deleteUndoLayer(subsequentLayers);
        undoLayerInfoIterator.remove();
        playerUndoCount.put(entityPlayerMP, playerUndoCount.get(entityPlayerMP) - 1);
        --layersToDelete;
      }
    }
  }

  /**
   * collates a list of undo layers with a creation time after the given time, for the given worldServer
   * @param creationTime only collate layers with a creation time > this value
   * @param worldServerToMatch the worldServer to match against
   * @return
   */
  private LinkedList<WorldSelectionUndo> collateSubsequentUndoLayers(long creationTime, WorldServer worldServerToMatch)
  {
    LinkedList<WorldSelectionUndo> collatedList = new LinkedList<WorldSelectionUndo>();
    for (UndoLayerInfo undoLayerInfo : undoLayers) {
      if (undoLayerInfo.worldServer.get() == worldServerToMatch
          && undoLayerInfo.creationTime > creationTime) {
        collatedList.add(undoLayerInfo.worldSelectionUndo);
      }
    }
    return collatedList;
  }

  private LinkedList<UndoLayerInfo> undoLayers = new LinkedList<UndoLayerInfo>();
  private int maximumDepth = 0;

  private static class UndoLayerInfo {
    public UndoLayerInfo(long i_creationTime, WorldServer i_worldServer, EntityPlayerMP i_entityPlayerMP, WorldSelectionUndo i_worldSelectionUndo) {
      creationTime = i_creationTime;
      worldServer = new WeakReference<WorldServer>(i_worldServer);
      entityPlayerMP = new WeakReference<EntityPlayerMP>(i_entityPlayerMP);
      worldSelectionUndo = i_worldSelectionUndo;
    }
    public long creationTime;
    WeakReference<WorldServer> worldServer;
    WeakReference<EntityPlayerMP>  entityPlayerMP;
    WorldSelectionUndo worldSelectionUndo;
  }


}
