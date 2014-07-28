package speedytools.serverside.worldmanipulation;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.utilities.QuadOrientation;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * User: The Grey Ghost
 * Date: 8/06/2014
 * Holds the undo history for the WorldServers.
 * Every player gets:
 * a) at least one "complex" undo eg for clone & copy tools.  They will get more if there is enough space.
 *    If the history is full, these "extra" undo layers are discarded, oldest first.
 * b) a fixed maximum number of "simple" undos with instant placement eg for wand and orb
 * The layers are grouped according to WorldServer (different dimensions will have different WorldServers)
 * Automatically gets rid of EntityPlayerMP and WorldServer which are no longer valid
 */
public class WorldHistory
{
  public WorldHistory(int maximumComplexHistoryDepth, int maximumSimpleUndosPerPlayer)
  {
    assert (maximumComplexHistoryDepth >= 1);
    maximumComplexDepth = maximumComplexHistoryDepth;
    maximumSimpleDepthPerPlayer = maximumSimpleUndosPerPlayer;
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

  /** write the given fragment to the World, storing undo information in the "complex tools" history
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
    AsynchronousToken token = writeToWorldWithUndoAsynchronous(player, worldServer, fragmentToWrite, wxOfOrigin, wyOfOrigin, wzOfOrigin, quadOrientation);
    token.setTimeOfInterrupt(token.INFINITE_TIMEOUT);
    token.continueProcessing();
  }

  /** write the given fragment to the World, storing undo information in the "complex tools" history
   * Runs asynchronously: after the initial call, use the returned token to monitor and advance the task
   *    -> repeatedly call token.setTimeToInterrupt() and token.continueProcessing()
   * @param player
   * @param worldServer
   * @param fragmentToWrite
   * @param wxOfOrigin
   * @param wyOfOrigin
   * @param wzOfOrigin
   * @param quadOrientation
   * @return the asynchronous token for further processing, or null if a complex operation is already in progress
   */
  public AsynchronousToken writeToWorldWithUndoAsynchronous(EntityPlayerMP player, WorldServer worldServer, WorldFragment fragmentToWrite, int wxOfOrigin, int wyOfOrigin, int wzOfOrigin,
                                                                QuadOrientation quadOrientation)
  {
    if (currentAsynchronousTask != null && !currentAsynchronousTask.isTaskComplete()) return null; // only one complex task at once!

    WorldSelectionUndo worldSelectionUndo = new WorldSelectionUndo();
    AsynchronousToken subToken = worldSelectionUndo.writeToWorldAsynchronous(worldServer, fragmentToWrite, wxOfOrigin, wyOfOrigin, wzOfOrigin, quadOrientation);
    UndoLayerInfo undoLayerInfo = new UndoLayerInfo(System.nanoTime(), worldServer, player, worldSelectionUndo);
    currentAsynchronousTask = new AsynchronousOperation(subToken, undoLayerInfo);

    return currentAsynchronousTask;

    // once the operation is complete, the token will add the undoLayerInfo to the complex list

//    worldSelectionUndo.writeToWorld(worldServer, fragmentToWrite, wxOfOrigin, wyOfOrigin, wzOfOrigin, quadOrientation);
  }

  /** write the given fragment to the World, storing undo information in the "simple tools" history
   * @param worldServer

   */
  public void writeToWorldWithUndo(WorldServer worldServer, EntityPlayerMP entityPlayerMP, BlockWithMetadata blockToPlace, List<ChunkCoordinates> blockSelection)
  {
    if (currentAsynchronousTask != null && !currentAsynchronousTask.isTaskComplete()) {
      blockSelection = currentAsynchronousTask.getUnlockedVoxels(worldServer, blockSelection);
    }

    WorldSelectionUndo worldSelectionUndo = new WorldSelectionUndo();
    worldSelectionUndo.writeToWorld(worldServer, blockToPlace, blockSelection);
    UndoLayerInfo undoLayerInfo = new UndoLayerInfo(System.nanoTime(), worldServer, entityPlayerMP, worldSelectionUndo);
    undoLayersSimple.add(undoLayerInfo);

    final int ARBITRARY_LARGE_VALUE = 1000000;
    cullUndoLayers(undoLayersSimple, maximumSimpleDepthPerPlayer, ARBITRARY_LARGE_VALUE);
  }

  /** perform complex undo action for the given player - finds the most recent complex action that they did in the current WorldServer
   * @param player
   * @param worldServer
   * @return true for success, or failure if either no undo found or if an asynchronous task is currently running
   */
  public boolean performComplexUndo(EntityPlayerMP player, WorldServer worldServer) {
    if (currentAsynchronousTask != null && !currentAsynchronousTask.isTaskComplete()) return false;

    return performUndo(undoLayersComplex, player, worldServer);
  }

  /** perform simple undo action for the given player - finds the most recent simple action that they did in the current WorldServer
   * @param player
   * @param worldServer
   * @return true for success, or failure if no undo found
   */
  public boolean performSimpleUndo(EntityPlayerMP player, WorldServer worldServer) {
    return performUndo(undoLayersSimple, player, worldServer);
  }

    /** perform undo action for the given player - finds the most recent action that they did in the current WorldServer
     * @param player
     * @param worldServer
     * @return true for success, or failure if no undo found
     */
  private boolean performUndo(LinkedList<UndoLayerInfo> undoHistory, EntityPlayerMP player, WorldServer worldServer)
  {
    UndoLayerInfo undoLayerFound = null;
    Iterator<UndoLayerInfo> undoLayerInfoIterator = undoHistory.descendingIterator();
    while (undoLayerFound == null &&  undoLayerInfoIterator.hasNext()) {
      UndoLayerInfo undoLayerInfo = undoLayerInfoIterator.next();
      if (undoLayerInfo.worldServer.get() == worldServer
              && undoLayerInfo.entityPlayerMP.get() == player) {
        undoLayerFound = undoLayerInfo;
      }
    }
    if (undoLayerFound == null) return false;

    boolean deferLayerRemoval = false;
    if (currentAsynchronousTask != null && !currentAsynchronousTask.isTaskComplete()) {
      // if an asynch task is happening, strip out any voxels locked by the task and only undo these.
      //   the task will queue the remaining (locked) voxels for later undo
      UndoLayerInfo unlockedOnly = currentAsynchronousTask.getUnlockedVoxelsAndScheduleLocked(undoLayerFound);
      if (unlockedOnly != null) {  // null means no locked voxels so just perform undo as normal
        undoLayerFound = unlockedOnly;
        deferLayerRemoval = true;
      }
    }

    LinkedList<WorldSelectionUndo> subsequentUndoLayers = collateSubsequentUndoLayersAllHistories(undoLayerFound.creationTime, worldServer);
    undoLayerFound.worldSelectionUndo.undoChanges(worldServer, subsequentUndoLayers);
    if (!deferLayerRemoval) {
      undoHistory.remove(undoLayerFound);
    }
    return true;
  }

    /**
     * removes the specified player from the history.
     * Optional, since any entityPlayerMP entries in the history which become invalid will eventually be removed automatically.
     * @param entityPlayerMP
     */
  public void removePlayer(EntityPlayerMP entityPlayerMP)
  {
    for (UndoLayerInfo undoLayerInfo : undoLayersComplex) {
      if (undoLayerInfo.entityPlayerMP.get() == entityPlayerMP) {
        undoLayerInfo.entityPlayerMP.clear();
      }
    }
    for (UndoLayerInfo undoLayerInfo : undoLayersSimple) {
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
    for (UndoLayerInfo undoLayerInfo : undoLayersComplex) {
      if (undoLayerInfo.worldServer.get() == worldServer) {
        undoLayerInfo.worldServer.clear();
      }
    }
    for (UndoLayerInfo undoLayerInfo : undoLayersSimple) {
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
      for (UndoLayerInfo undoLayerInfo : undoLayersComplex) {
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
   * 2) limit each player to the given maximum per player
   * 3) If the total layers is still above target - for each player with more than one undolayer, delete the extra layers, starting from oldest first
   * NB does nothing if there is an asynchronous task currently in progress
   */
  private void cullUndoLayers(LinkedList<UndoLayerInfo> historyToCull, int maxUndoPerPlayer, int targetTotalSize)
  {
    if (currentAsynchronousTask != null && !currentAsynchronousTask.isTaskComplete()) return;

    // delete all invalid layers
    Iterator<UndoLayerInfo> undoLayerInfoIterator = historyToCull.iterator();
    while  (undoLayerInfoIterator.hasNext()) {
      UndoLayerInfo undoLayerInfo = undoLayerInfoIterator.next();
      if (undoLayerInfo.worldServer.get() == null) {
        undoLayerInfoIterator.remove();
      } else {
        if (undoLayerInfo.entityPlayerMP.get() == null) {
          LinkedList<WorldSelectionUndo> precedingUndoLayers = collatePrecedingUndoLayersAllHistories(undoLayerInfo.creationTime, undoLayerInfo.worldServer.get());
          undoLayerInfo.worldSelectionUndo.makePermanent(undoLayerInfo.worldServer.get(), precedingUndoLayers);
          undoLayerInfoIterator.remove();
        }
      }
    }

    HashMap<EntityPlayerMP, Integer> playerUndoCount = new HashMap<EntityPlayerMP, Integer>();
    for (UndoLayerInfo undoLayerInfo : historyToCull) {
      EntityPlayerMP entityPlayerMP = undoLayerInfo.entityPlayerMP.get();
      assert (entityPlayerMP != null);
      if (!playerUndoCount.containsKey(entityPlayerMP)) {
        playerUndoCount.put(entityPlayerMP, 1);
      } else {
        playerUndoCount.put(entityPlayerMP, playerUndoCount.get(entityPlayerMP) + 1);
      }
    }

    int layersToDelete = historyToCull.size() - targetTotalSize;

    for (Integer layerCount : playerUndoCount.values()) {      // account for layers which will be deleted due to per-player limits
      if (layerCount > maxUndoPerPlayer) {
        layersToDelete -= (layerCount - maxUndoPerPlayer);
      }
    }

    Iterator<UndoLayerInfo> excessIterator = historyToCull.iterator();
    while (excessIterator.hasNext()) {
      UndoLayerInfo undoLayerInfo = excessIterator.next();
      EntityPlayerMP entityPlayerMP = undoLayerInfo.entityPlayerMP.get();
      assert (entityPlayerMP != null);
      if (playerUndoCount.get(entityPlayerMP) > 1 && (layersToDelete > 0 || playerUndoCount.get(entityPlayerMP) > maxUndoPerPlayer)) {
        LinkedList<WorldSelectionUndo> precedingUndoLayers = collatePrecedingUndoLayersAllHistories(undoLayerInfo.creationTime, undoLayerInfo.worldServer.get());
        undoLayerInfo.worldSelectionUndo.makePermanent(undoLayerInfo.worldServer.get(), precedingUndoLayers);
        excessIterator.remove();
        if (playerUndoCount.get(entityPlayerMP) <= maxUndoPerPlayer) {
          --layersToDelete;
        }
        playerUndoCount.put(entityPlayerMP, playerUndoCount.get(entityPlayerMP) - 1);
      }
    }
  }

  /**
   * collates a list of undo layers with a creation time after the given time, for the given worldServer
   * @param creationTime only collate layers with a creation time > this value
   * @param worldServerToMatch the worldServer to match against
   * @return a list of matching WorldSelectionUndo in ascending order of time.
   */
  private LinkedList<WorldSelectionUndo> collateSubsequentUndoLayersAllHistories(long creationTime, WorldServer worldServerToMatch)
  {
    LinkedList<UndoLayerInfo> combinedList = collateSubsequentUndoLayers(undoLayersSimple, creationTime, worldServerToMatch);
    combinedList.addAll(collateSubsequentUndoLayers(undoLayersComplex, creationTime, worldServerToMatch));
    Collections.sort(combinedList);
    LinkedList<WorldSelectionUndo> collatedList = new LinkedList<WorldSelectionUndo>();
    for (UndoLayerInfo layerInfo : combinedList) {
      collatedList.add(layerInfo.worldSelectionUndo);
    }
    return collatedList;
  }

  private LinkedList<UndoLayerInfo> collateSubsequentUndoLayers(LinkedList<UndoLayerInfo> whichHistory, long creationTime, WorldServer worldServerToMatch)
  {
    LinkedList<UndoLayerInfo> collatedList = new LinkedList<UndoLayerInfo>();
    for (UndoLayerInfo undoLayerInfo : whichHistory) {
      if (undoLayerInfo.worldServer.get() == worldServerToMatch
              && undoLayerInfo.creationTime > creationTime) {
        collatedList.add(undoLayerInfo);
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
  private LinkedList<WorldSelectionUndo> collatePrecedingUndoLayersAllHistories(long creationTime, WorldServer worldServerToMatch) {
    LinkedList<UndoLayerInfo> combinedList = collatePrecedingUndoLayers(undoLayersSimple, creationTime, worldServerToMatch);
    combinedList.addAll(collatePrecedingUndoLayers(undoLayersComplex, creationTime, worldServerToMatch));
    Collections.sort(combinedList);
    LinkedList<WorldSelectionUndo> collatedList = new LinkedList<WorldSelectionUndo>();
    for (UndoLayerInfo layerInfo : combinedList) {
      collatedList.addFirst(layerInfo.worldSelectionUndo);               // reverse the order
    }
    return collatedList;
  }

  private LinkedList<UndoLayerInfo> collatePrecedingUndoLayers(LinkedList<UndoLayerInfo> whichHistory, long creationTime, WorldServer worldServerToMatch)
  {
    LinkedList<UndoLayerInfo> collatedList = new LinkedList<UndoLayerInfo>();
    for (UndoLayerInfo undoLayerInfo : whichHistory) {
      if (undoLayerInfo.worldServer.get() == worldServerToMatch
              && undoLayerInfo.creationTime < creationTime) {
        collatedList.add(undoLayerInfo);
      }
    }
    return collatedList;
  }

  private class AsynchronousOperation implements AsynchronousToken
  {
    @Override
    public boolean isTaskComplete() {
      return completed;
    }

    @Override
    public boolean isTimeToInterrupt() {
      return (interruptTimeNS == IMMEDIATE_TIMEOUT || (interruptTimeNS != INFINITE_TIMEOUT && System.nanoTime() >= interruptTimeNS));
    }

    @Override
    public void setTimeOfInterrupt(long timeToStopNS) {
      interruptTimeNS = timeToStopNS;
    }

    @Override
    public void continueProcessing() {
      // first, complete the subTask (placement of the fragment)
      // then perform all of the deferred undos (i.e. the Voxels locked by the subTask) and remove them from the history
      // beware - further undo may be added every time we interrupt

      if (completed) return;
      if (!executeSubTask()) return;
      if (undoLayerInfo != null) {
        undoLayersComplex.add(undoLayerInfo);
        undoLayerInfo = null;
      }
      while (!deferredSimpleUndoToPerform.isEmpty()) {
        UndoLayerInfo queuedUndoLayerInfo = deferredSimpleUndoToPerform.remove(0);
        LinkedList<WorldSelectionUndo> subsequentUndoLayers = collateSubsequentUndoLayersAllHistories(queuedUndoLayerInfo.creationTime, queuedUndoLayerInfo.worldServer.get());
        queuedUndoLayerInfo.worldSelectionUndo.undoChanges(queuedUndoLayerInfo.worldServer.get(), subsequentUndoLayers);
        boolean foundAndRemoved = undoLayersSimple.remove(queuedUndoLayerInfo);
        assert (foundAndRemoved);
        if (isTimeToInterrupt()) return;
      }

      cullUndoLayers(undoLayersComplex, maximumSimpleDepthPerPlayer, maximumComplexDepth);
      completed = true;
    }

    @Override
    public double getFractionComplete()
    {
      return fractionComplete;
    }

    public AsynchronousOperation(AsynchronousToken i_subTask, UndoLayerInfo i_undoLayerInfo)
    {
      subTask = i_subTask;
      undoLayerInfo = i_undoLayerInfo;
      interruptTimeNS = INFINITE_TIMEOUT;
      fractionComplete = 0;
      completed = false;
    }

    // returns true if the sub-task is finished
    public boolean executeSubTask() {
      if (subTask == null || subTask.isTaskComplete()) {
        return true;
      }
      subTask.setTimeOfInterrupt(interruptTimeNS);
      subTask.continueProcessing();
      fractionComplete = subTask.getFractionComplete();
      return subTask.isTaskComplete();
    }

    public List<ChunkCoordinates> getUnlockedVoxels(WorldServer worldServer, List<ChunkCoordinates> blocksToCheck)
    {
      if (undoLayerInfo == null) return blocksToCheck;
      WorldServer taskWorldServer = undoLayerInfo.worldServer.get();
      if (taskWorldServer == null || taskWorldServer != worldServer) return blocksToCheck;
      return undoLayerInfo.worldSelectionUndo.excludeBlocksInSelection(blocksToCheck);
    }

    /** returns an UndoLayerInfo containing only the voxels which aren't locked by the current task.
     * Also schedules the locked voxels to be undone at the end of the current task.
     * @param layerToBeSplit
     * @return An UndoLayerInfo with only those Voxels not locked by the current task; null if the task hasn't locked any voxels at all
     */
    public UndoLayerInfo getUnlockedVoxelsAndScheduleLocked(UndoLayerInfo layerToBeSplit)
    {
      if (undoLayerInfo == null) return null;
      UndoLayerInfo unlocked = new UndoLayerInfo(layerToBeSplit);
      WorldSelectionUndo taskInProgress = undoLayerInfo.worldSelectionUndo;
      unlocked.worldSelectionUndo = layerToBeSplit.worldSelectionUndo.splitByLockedVoxels(taskInProgress);
      deferredSimpleUndoToPerform.add(layerToBeSplit);
      return unlocked;
    }

    private List<UndoLayerInfo> deferredSimpleUndoToPerform = new ArrayList<UndoLayerInfo>();

    private boolean completed;
    private long interruptTimeNS;
    private double fractionComplete;
    private AsynchronousToken subTask;
    private UndoLayerInfo undoLayerInfo;
  }

  private AsynchronousOperation currentAsynchronousTask;

  private LinkedList<UndoLayerInfo> undoLayersComplex = new LinkedList<UndoLayerInfo>();    // cloning tools
  private LinkedList<UndoLayerInfo> undoLayersSimple = new LinkedList<UndoLayerInfo>();     // instant tools

  private int maximumComplexDepth = 0;
  private int maximumSimpleDepthPerPlayer = 0;

  private static class UndoLayerInfo implements Comparable<UndoLayerInfo> {
    public UndoLayerInfo(long i_creationTime, WorldServer i_worldServer, EntityPlayerMP i_entityPlayerMP, WorldSelectionUndo i_worldSelectionUndo) {
      creationTime = i_creationTime;
      worldServer = new WeakReference<WorldServer>(i_worldServer);
      entityPlayerMP = new WeakReference<EntityPlayerMP>(i_entityPlayerMP);
      worldSelectionUndo = i_worldSelectionUndo;
    }
    // shallow copy!
    public UndoLayerInfo(UndoLayerInfo source)
    {
      creationTime = source.creationTime;
      worldServer = source.worldServer;
      entityPlayerMP = source.entityPlayerMP;
      worldSelectionUndo = source.worldSelectionUndo;
    }

    public long creationTime;
    public WeakReference<WorldServer> worldServer;
    public WeakReference<EntityPlayerMP>  entityPlayerMP;
    public WorldSelectionUndo worldSelectionUndo;

    @Override
    public int compareTo(UndoLayerInfo objectToCompareAgainst)
    {
      if (creationTime > objectToCompareAgainst.creationTime) return 1;
      if (creationTime < objectToCompareAgainst.creationTime) return -1;
      return 0;
    }
  }
}
