package speedytools.serverside.worldmanipulation;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.WorldServer;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.selections.VoxelSelectionWithOrigin;
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
    AsynchronousToken token = writeToWorldWithUndoAsynchronous(player, worldServer, fragmentToWrite, wxOfOrigin, wyOfOrigin, wzOfOrigin, quadOrientation, null);
    if (token == null) return;
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
                                                            QuadOrientation quadOrientation, UniqueTokenID transactionID)
  {
    if (currentAsynchronousTask != null && !currentAsynchronousTask.isTaskComplete()) return null; // only one complex task at once!

    WorldSelectionUndo worldSelectionUndo = new WorldSelectionUndo();
    AsynchronousToken subToken = worldSelectionUndo.writeToWorldAsynchronous(worldServer, fragmentToWrite, wxOfOrigin, wyOfOrigin, wzOfOrigin, quadOrientation, transactionID);
    UndoLayerInfo undoLayerInfo = new UndoLayerInfo(System.nanoTime(), worldServer, player, worldSelectionUndo);
    currentAsynchronousTask = new AsynchronousWriteOrUndo(AsynchronousActionType.WRITE, subToken, undoLayerInfo, transactionID);

    return currentAsynchronousTask;
    // once the operation is complete, the token will add the undoLayerInfo to the complex list
  }

  /** write the given fragment to the World, storing undo information in the "simple tools" history
   * @param worldServer

   */
  public void writeToWorldWithUndo(WorldServer worldServer, EntityPlayerMP entityPlayerMP, BlockWithMetadata blockToPlace, EnumFacing sideToPlace, List<BlockPos> blockSelection)
  {
    if (currentAsynchronousTask != null && !currentAsynchronousTask.isTaskComplete()) {
      blockSelection = currentAsynchronousTask.cullLockedVoxels(worldServer, blockSelection);
    }
    if (blockSelection.isEmpty()) return;
    WorldSelectionUndo worldSelectionUndo = new WorldSelectionUndo();
    worldSelectionUndo.writeToWorld(worldServer, entityPlayerMP, blockToPlace, sideToPlace, blockSelection);
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
    AsynchronousToken token = performComplexUndoAsynchronous(player, worldServer, null);
    if (token == null) return false;
    token.setTimeOfInterrupt(token.INFINITE_TIMEOUT);
    token.continueProcessing();
    return true;
  }

  /** perform complex undo action for the given player - finds the most recent complex action that they did in the current WorldServer
   *    * Runs asynchronously: after the initial call, use the returned token to monitor and advance the task
   *    -> repeatedly call token.setTimeToInterrupt() and token.continueProcessing()
   * @param player
   * @param worldServer
   * @param transactionID if not null, perform the undo with the given transactionID, if it still exists.
   * @return the asynchronous token for further processing, or null if no undo found or a complex operation is already in progress
   */
  public AsynchronousToken performComplexUndoAsynchronous(EntityPlayerMP player, WorldServer worldServer, UniqueTokenID transactionID) {
    if (currentAsynchronousTask != null && !currentAsynchronousTask.isTaskComplete()) return null;

    UndoLayerInfo undoLayerFound = null;
    if (transactionID != null) {
      undoLayerFound = getSpecificUndo(undoLayersComplex, player, worldServer, transactionID);
    } else {
      undoLayerFound = getMostRecentUndo(undoLayersComplex, player, worldServer);
    }
    if (undoLayerFound == null) return null;

    LinkedList<WorldSelectionUndo> subsequentUndoLayers = collateSubsequentUndoLayersAllHistories(undoLayerFound.creationTime, worldServer);
    AsynchronousToken subToken = undoLayerFound.worldSelectionUndo.undoChangesAsynchronous(worldServer, subsequentUndoLayers);
    currentAsynchronousTask = new AsynchronousWriteOrUndo(AsynchronousActionType.UNDO, subToken, undoLayerFound, null);

    return currentAsynchronousTask;
  }

  /** get the unique transaction ID for the undo that will be performed next
   *
   * @param player
   * @param worldServer
   * @return null if no undo found
   */
  public UniqueTokenID getTransactionIDForNextComplexUndo(EntityPlayerMP player, WorldServer worldServer)
  {
    UndoLayerInfo undoLayerFound = getMostRecentUndo(undoLayersComplex, player, worldServer);
    if (undoLayerFound == null) return null;
    return undoLayerFound.transactionID;
  }

  /** perform simple undo action for the given player - finds the most recent simple action that they did in the given WorldServer
   * @param player
   * @param worldServer
   * @return true for success, or failure if no undo found
   */
  public boolean performSimpleUndo(EntityPlayerMP player, WorldServer worldServer) {
    UndoLayerInfo undoLayerFound = getMostRecentUndo(undoLayersSimple, player, worldServer);
    if (undoLayerFound == null) return false;
    undoLayerFound.undoHasCommenced = true;  // prevent future performUndo from finding this undo (in case of deferred removal)

    boolean deferLayerRemoval = false;
    if (currentAsynchronousTask != null && !currentAsynchronousTask.isTaskComplete()) {
      // if an asynch task is happening, strip out any voxels locked by the task and only undo these.
      //   the task will queue the remaining (locked) voxels for later undo
      UndoLayerInfo unlockedOnly = currentAsynchronousTask.removeLockedVoxelsAndScheduleForLaterExecution(undoLayerFound);
      if (unlockedOnly != null) {  // null means no locked voxels so just perform undo as normal
        undoLayerFound = unlockedOnly;
        deferLayerRemoval = true;
      }
    }

    LinkedList<WorldSelectionUndo> subsequentUndoLayers = collateSubsequentUndoLayersAllHistories(undoLayerFound.creationTime, worldServer);
    undoLayerFound.worldSelectionUndo.undoChanges(worldServer, subsequentUndoLayers);
    if (!deferLayerRemoval) {
      undoLayersSimple.remove(undoLayerFound);
    }
    return true;
  }

  /** find the most recent undo entry, in the given undoHistory, for the given player and worldServerReader
   * @param undoHistory
   * @param player
   * @param worldServer
   * @return the undo entry, or null if none found
   */
  private UndoLayerInfo getMostRecentUndo(LinkedList<UndoLayerInfo> undoHistory, EntityPlayerMP player, WorldServer worldServer)
  {
    UndoLayerInfo undoLayerFound = null;
    Iterator<UndoLayerInfo> undoLayerInfoIterator = undoHistory.descendingIterator();
    while (undoLayerFound == null && undoLayerInfoIterator.hasNext()) {
      UndoLayerInfo undoLayerInfo = undoLayerInfoIterator.next();
      if (undoLayerInfo.worldServer.get() == worldServer
              && undoLayerInfo.entityPlayerMP.get() == player
              && !undoLayerInfo.undoHasCommenced    ) {
        undoLayerFound = undoLayerInfo;
      }
    }
    return undoLayerFound;
  }

  /** find the most recent undo entry, in the given undoHistory, for the given player and worldServerReader
   * @param undoHistory
   * @param player
   * @param worldServer
   * @return the undo entry, or null if none found
   */
  private UndoLayerInfo getSpecificUndo(LinkedList<UndoLayerInfo> undoHistory, EntityPlayerMP player, WorldServer worldServer, UniqueTokenID transactionID)
  {
    UndoLayerInfo undoLayerFound = null;
    Iterator<UndoLayerInfo> undoLayerInfoIterator = undoHistory.descendingIterator();
    while (undoLayerFound == null && undoLayerInfoIterator.hasNext()) {
      UndoLayerInfo undoLayerInfo = undoLayerInfoIterator.next();
      if (undoLayerInfo.transactionID.equals(transactionID)
          && undoLayerInfo.worldServer.get() == worldServer
          && undoLayerInfo.entityPlayerMP.get() == player
          && !undoLayerInfo.undoHasCommenced    ) {
        undoLayerFound = undoLayerInfo;
      }
    }
    return undoLayerFound;
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
   * removes the specified worldServerReader from the history.
   * Optional, since any worldServerReader entries in the history which become invalid will eventually be removed automatically.
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
  public void printUndoStackYSlice(WorldServer worldServer, BlockPos origin, int xSize, int y, int zSize)
  {
    for (int x = 0; x < xSize; ++x) {
      for (UndoLayerInfo undoLayerInfo : undoLayersComplex) {
        if (undoLayerInfo.worldServer.get() == worldServer) {
          for (int z = 0; z < zSize; ++z) {
            Integer metadata = undoLayerInfo.worldSelectionUndo.getStoredMetadata(x + origin.getX(), y + origin.getY(), z + origin.getZ());
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
    HashSet<UniqueTokenID> uniqueTransactions = new HashSet<UniqueTokenID>();
    for (UndoLayerInfo undoLayerInfo : historyToCull) {
      if (!uniqueTransactions.contains(undoLayerInfo.transactionID)) {
        uniqueTransactions.add(undoLayerInfo.transactionID);
        EntityPlayerMP entityPlayerMP = undoLayerInfo.entityPlayerMP.get();
        assert (entityPlayerMP != null);
        if (!playerUndoCount.containsKey(entityPlayerMP)) {
          playerUndoCount.put(entityPlayerMP, 1);
        } else {
          playerUndoCount.put(entityPlayerMP, playerUndoCount.get(entityPlayerMP) + 1);
        }
      }
    }

    int transactionCount = uniqueTransactions.size();
    int layersToDelete = transactionCount - targetTotalSize;

    for (Integer layerCount : playerUndoCount.values()) {      // account for layers which will be deleted due to per-player limits
      if (layerCount > maxUndoPerPlayer) {
        layersToDelete -= (layerCount - maxUndoPerPlayer);
      }
    }

    HashSet<UniqueTokenID> deletedTransactions = new HashSet<UniqueTokenID>();
    Iterator<UndoLayerInfo> excessIterator = historyToCull.iterator();
    while (excessIterator.hasNext()) {
      UndoLayerInfo undoLayerInfo = excessIterator.next();
      EntityPlayerMP entityPlayerMP = undoLayerInfo.entityPlayerMP.get();
      assert (entityPlayerMP != null);
      if (playerUndoCount.get(entityPlayerMP) > 1 && (layersToDelete > 0 || playerUndoCount.get(entityPlayerMP) > maxUndoPerPlayer)) {
        deletedTransactions.add(undoLayerInfo.transactionID);
        if (playerUndoCount.get(entityPlayerMP) <= maxUndoPerPlayer) {
          --layersToDelete;
        }
        playerUndoCount.put(entityPlayerMP, playerUndoCount.get(entityPlayerMP) - 1);
      }
      if (deletedTransactions.contains(undoLayerInfo.transactionID)) {
        LinkedList<WorldSelectionUndo> precedingUndoLayers = collatePrecedingUndoLayersAllHistories(undoLayerInfo.creationTime, undoLayerInfo.worldServer.get());
        undoLayerInfo.worldSelectionUndo.makePermanent(undoLayerInfo.worldServer.get(), precedingUndoLayers);
        excessIterator.remove();
      }
    }
  }

  /**
   * collates a list of undo layers with a creation time after the given time, for the given worldServerReader
   * @param creationTime only collate layers with a creation time > this value
   * @param worldServerToMatch the worldServerReader to match against
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
   * collates a list of undo layers with a creation time before the given time, for the given worldServerReader
   * @param creationTime only collate layers with a creation time < this value
   * @param worldServerToMatch the worldServerReader to match against
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

  enum AsynchronousActionType {WRITE, UNDO};

  private class AsynchronousWriteOrUndo implements AsynchronousToken
  {
    @Override
    public boolean isTaskComplete() {
      return completed;
    }

    @Override
    public boolean isTaskAborted() {
      return aborting && isTaskComplete();
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
      // first, complete the subTask (placement or undo of the fragment)
      // then perform all of the deferred undos (i.e. the Voxels locked by the subTask) and remove them from the history
      // beware - further undo may be added every time we interrupt

      if (completed) return;
      if (!executeSubTask()) return;
      if (undoLayerInfo != null) {
        switch (asynchronousActionType) {
          case WRITE: {
            undoLayerInfo.transactionID = transactionID;
            undoLayersComplex.add(undoLayerInfo);
            break;
          }
          case UNDO: {
            if (!aborting) {
              undoLayersComplex.remove(undoLayerInfo);
            }
            break;
          }
          default: assert false : "Invalid asynchronousActionType: " + asynchronousActionType;
        }
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

      completed = true;
      cullUndoLayers(undoLayersComplex, maximumSimpleDepthPerPlayer, maximumComplexDepth);
    }

    @Override
    public void abortProcessing() {
      aborting = true;
    }

    @Override
    public double getFractionComplete()
    {
      return fractionComplete;
    }

    public AsynchronousWriteOrUndo(AsynchronousActionType i_actionType, AsynchronousToken i_subTask, UndoLayerInfo i_undoLayerInfo, UniqueTokenID i_transactionID)
    {
      subTask = i_subTask;
      undoLayerInfo = i_undoLayerInfo;
      interruptTimeNS = INFINITE_TIMEOUT;
      fractionComplete = 0;
      completed = false;
      asynchronousActionType = i_actionType;
      transactionID = (i_transactionID == null) ? new UniqueTokenID() : i_transactionID;
    }

    // returns true if the sub-task is finished
    public boolean executeSubTask() {
      if (subTask == null || subTask.isTaskComplete()) {
        return true;
      }
      if (aborting) {
        subTask.abortProcessing();
      }
      subTask.setTimeOfInterrupt(interruptTimeNS);
      subTask.continueProcessing();
      fractionComplete = subTask.getFractionComplete();
//      System.out.println("AsynchronousWriteOrUndo fractionComplete:" + fractionComplete);
      return subTask.isTaskComplete();
    }

    public VoxelSelectionWithOrigin getLockedRegion()
    {
      if (subTask == null || subTask.isTaskComplete()) return null;
      return subTask.getLockedRegion();
    }

    @Override
    public UniqueTokenID getUniqueTokenID() {
      return uniqueTokenID;
    }

    /**
     * remove all blocks from the placement list which are locked by the current task
     * @param worldServer
     * @param blocksToCheck
     * @return
     */
    public List<BlockPos> cullLockedVoxels(WorldServer worldServer, List<BlockPos> blocksToCheck)
    {
      if (undoLayerInfo == null) return blocksToCheck;
      VoxelSelectionWithOrigin lockedRegion = getLockedRegion();
      if (lockedRegion == null) return blocksToCheck;

      WorldServer taskWorldServer = undoLayerInfo.worldServer.get();
      if (taskWorldServer == null || taskWorldServer != worldServer) return blocksToCheck;

      LinkedList<BlockPos> culledList = new LinkedList<BlockPos>();
      for (BlockPos coordinate : blocksToCheck) {
        if (!lockedRegion.getVoxelWXYZ(coordinate.getX(), coordinate.getY(), coordinate.getZ())) {
          culledList.add(coordinate);
        }
      }
      return culledList;
    }

    /**
     * Used when performing a simple undo at the same time as a complex task is taking place.
     * The simple undo is split into two parts:
     * 1) a portion that can be performed immediately (not locked by the complex task)
     * 2) a portion that will be performed after the complex task is finished
     * After the call, layerToBeSplit will contain only the voxels locked by the current task
     * These locked voxels are scheduled to be undone at the end of the current task.
     * The unlocked voxels, which can be executed immediately, are placed in the return value
     * @param layerToBeSplit
     * @return a new undo layer which contains only unlocked voxels.  Shallow copy of original!  Might have no voxels at all.  If null, there is no locked
     *         region and the undo hasn't been scheduled for later
     */
    public UndoLayerInfo removeLockedVoxelsAndScheduleForLaterExecution(UndoLayerInfo layerToBeSplit)
    {
      if (undoLayerInfo == null) return null;
      UndoLayerInfo unlockedVoxelsOnly = new UndoLayerInfo(layerToBeSplit);

      VoxelSelectionWithOrigin lockedRegion = getLockedRegion();
      if (lockedRegion == null) return null;

      unlockedVoxelsOnly.worldSelectionUndo = layerToBeSplit.worldSelectionUndo.splitByLockedVoxels(lockedRegion);
      deferredSimpleUndoToPerform.add(layerToBeSplit);    // only the locked voxels remain
      return unlockedVoxelsOnly;
    }

    private List<UndoLayerInfo> deferredSimpleUndoToPerform = new ArrayList<UndoLayerInfo>();

    private boolean completed;
    private long interruptTimeNS;
    private double fractionComplete;
    private AsynchronousToken subTask;
    private UndoLayerInfo undoLayerInfo;
    private AsynchronousActionType asynchronousActionType;
    private boolean aborting = false;
    private final UniqueTokenID uniqueTokenID = new UniqueTokenID();
    private UniqueTokenID transactionID = null;
  }

  private AsynchronousWriteOrUndo currentAsynchronousTask;

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
      transactionID = new UniqueTokenID();  // default; might be replaced later
//      creatingTaskID = null;
    }

//    public UndoLayerInfo(long i_creationTime, WorldServer i_worldServer, EntityPlayerMP i_entityPlayerMP, WorldSelectionUndo i_worldSelectionUndo, UniqueTokenID i_creatingTaskID)
//    {
//     this(i_creationTime, i_worldServer, i_entityPlayerMP, i_worldSelectionUndo);
////     creatingTaskID = i_creatingTaskID;
//    }

//    public void setCreatingTaskID(UniqueTokenID newCreatingTaskID) {creatingTaskID = newCreatingTaskID;}

    // shallow copy!
    public UndoLayerInfo(UndoLayerInfo source)
    {
      creationTime = source.creationTime;
      worldServer = source.worldServer;
      entityPlayerMP = source.entityPlayerMP;
      worldSelectionUndo = source.worldSelectionUndo;
//      creatingTaskID = source.creatingTaskID;
      undoHasCommenced = false;
    }

    public long creationTime;
    public WeakReference<WorldServer> worldServer;
    public WeakReference<EntityPlayerMP>  entityPlayerMP;
    public WorldSelectionUndo worldSelectionUndo;
    public UniqueTokenID transactionID;
    boolean undoHasCommenced;  // set to true once the player has commenced this undo
//    public UniqueTokenID creatingTaskID;

    @Override
    public int compareTo(UndoLayerInfo objectToCompareAgainst)
    {
      if (creationTime > objectToCompareAgainst.creationTime) return 1;
      if (creationTime < objectToCompareAgainst.creationTime) return -1;
      return 0;
    }
  }
}
