package speedytools.serverside.worldmanipulation;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.utilities.QuadOrientation;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * User: The Grey Ghost
 * Date: 8/06/2014
 * Holds the undo history for the WorldServers.
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
    QuadOrientation noChange = new QuadOrientation(0, 0, 1, 1);
    writeToWorldWithUndo(player, worldServer, fragmentToWrite, wxOfOrigin, wyOfOrigin, wzOfOrigin, noChange);
  }

  /** write the given fragment to the World, storing undo information
   * @param player
   * @param worldServer
   * @param fragmentToWrite
   * @param wxOfOrigin
   * @param wyOfOrigin
   * @param wzOfOrigin
   */
  public void writeToWorldWithUndo(EntityPlayerMP player, WorldServer worldServer, WorldFragment fragmentToWrite, int wxOfOrigin, int wyOfOrigin, int wzOfOrigin,
                                   QuadOrientation quadOrientation)
  {
    WorldSelectionUndo worldSelectionUndo = new WorldSelectionUndo();
    worldSelectionUndo.writeToWorld(worldServer, fragmentToWrite, wxOfOrigin, wyOfOrigin, wzOfOrigin, quadOrientation);
    UndoLayerInfo undoLayerInfo = new UndoLayerInfo(System.nanoTime(), worldServer, player, worldSelectionUndo);
    undoLayers.add(undoLayerInfo);
    cullUndoLayers(maximumDepth);
  }

  /** write the given fragment to the World, storing undo information
   * @param worldServer

   */
  public void writeToWorldWithUndo(WorldServer worldServer, EntityPlayerMP entityPlayerMP, BlockWithMetadata blockToPlace, List<ChunkCoordinates> blockSelection)
  {
    WorldSelectionUndo worldSelectionUndo = new WorldSelectionUndo();
    worldSelectionUndo.writeToWorld(worldServer,entityPlayerMP, blockToPlace, blockSelection);
    UndoLayerInfo undoLayerInfo = new UndoLayerInfo(System.nanoTime(), worldServer, entityPlayerMP, worldSelectionUndo);
    undoLayers.add(undoLayerInfo);
    cullUndoLayers(maximumDepth);
  }

  /** perform undo action for the given player - finds the most recent action that they did in the current WorldServer
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
        undoLayerInfo.worldSelectionUndo.undoChanges(worldServer, subsequentUndoLayers);
        undoLayerInfoIterator.remove();
        return true;
      }
    }
    return false;
  }

  /**
   * removes the specified player from the history.
   * Optional, since any entityPlayerMP entries in the history which become invalid will eventually be removed automatically.
   * @param entityPlayerMP
   */
  public void removePlayer(EntityPlayerMP entityPlayerMP)
  {
    for (UndoLayerInfo undoLayerInfo : undoLayers) {
      if (undoLayerInfo.entityPlayerMP.get() == entityPlayerMP) {
        undoLayerInfo.entityPlayerMP.clear();
      }
    }
  }

  /**
   * removes the specified worldServer from the history.
   * Optional, since any worldServer entries in the history which become invalid will eventually be removed automatically.
   * @param worldServer
   */
  public void removeWorldServer(WorldServer worldServer)
  {
    for (UndoLayerInfo undoLayerInfo : undoLayers) {
      if (undoLayerInfo.worldServer.get() == worldServer) {
        undoLayerInfo.worldServer.clear();
      }
    }
  }

  /** for debugging purposes
   */
  public void printUndoStackYSlice(WorldServer worldServer, ChunkCoordinates origin, int xSize, int y, int zSize)
  {
    for (int x = 0; x < xSize; ++x) {
      for (UndoLayerInfo undoLayerInfo : undoLayers) {
        if (undoLayerInfo.worldServer.get() == worldServer) {
          for (int z = 0; z < zSize; ++z) {
            Integer metadata = undoLayerInfo.worldSelectionUndo.getStoredMetadata(x + origin.posX, y + origin.posY, z + origin.posZ);
            System.out.print((metadata == null) ? "-" : metadata);
            System.out.print(" ");
          }
        }
        System.out.print(": ");
      }
      System.out.println();
    }
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
    Iterator<UndoLayerInfo> undoLayerInfoIterator = undoLayers.iterator();
    while  (undoLayerInfoIterator.hasNext()) {
      UndoLayerInfo undoLayerInfo = undoLayerInfoIterator.next();
      if (undoLayerInfo.worldServer.get() == null) {
        undoLayerInfoIterator.remove();
      } else {
        if (undoLayerInfo.entityPlayerMP.get() == null) {
          LinkedList<WorldSelectionUndo> precedingUndoLayers = collatePrecedingUndoLayers(undoLayerInfo.creationTime, undoLayerInfo.worldServer.get());
          undoLayerInfo.worldSelectionUndo.makePermanent(undoLayerInfo.worldServer.get(), precedingUndoLayers);
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
    while (layersToDelete > 0 && excessIterator.hasNext()) {
      UndoLayerInfo undoLayerInfo = excessIterator.next();
      EntityPlayerMP entityPlayerMP = undoLayerInfo.entityPlayerMP.get();
      assert (entityPlayerMP != null);
      if (playerUndoCount.get(entityPlayerMP) > 1) {
        LinkedList<WorldSelectionUndo> precedingUndoLayers = collatePrecedingUndoLayers(undoLayerInfo.creationTime, undoLayerInfo.worldServer.get());
        undoLayerInfo.worldSelectionUndo.makePermanent(undoLayerInfo.worldServer.get(), precedingUndoLayers);
        excessIterator.remove();
        playerUndoCount.put(entityPlayerMP, playerUndoCount.get(entityPlayerMP) - 1);
        --layersToDelete;
      }
    }
  }

  /**
   * collates a list of undo layers with a creation time after the given time, for the given worldServer
   * @param creationTime only collate layers with a creation time > this value
   * @param worldServerToMatch the worldServer to match against
   * @return a list of matching WorldSelectionUndo in ascending order of time.
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

  /**
   * collates a list of undo layers with a creation time before the given time, for the given worldServer
   * @param creationTime only collate layers with a creation time < this value
   * @param worldServerToMatch the worldServer to match against
   * @return a list of matching WorldSelectionUndo in descending order of time.
   */
  private LinkedList<WorldSelectionUndo> collatePrecedingUndoLayers(long creationTime, WorldServer worldServerToMatch)
  {
    LinkedList<WorldSelectionUndo> collatedList = new LinkedList<WorldSelectionUndo>();
    Iterator<UndoLayerInfo> undoLayerInfoIterator = undoLayers.descendingIterator();
    while  (undoLayerInfoIterator.hasNext()) {
      UndoLayerInfo undoLayerInfo = undoLayerInfoIterator.next();
      if (undoLayerInfo.worldServer.get() == worldServerToMatch
              && undoLayerInfo.creationTime < creationTime) {
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
