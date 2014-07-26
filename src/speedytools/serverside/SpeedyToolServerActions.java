package speedytools.serverside;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.network.ServerStatus;
import speedytools.common.selections.VoxelSelectionWithOrigin;
import speedytools.common.utilities.QuadOrientation;
import speedytools.common.utilities.ResultWithReason;
import speedytools.serverside.backup.MinecraftSaveFolderBackups;
import speedytools.serverside.worldmanipulation.WorldFragment;
import speedytools.serverside.worldmanipulation.WorldHistory;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by TheGreyGhost on 7/03/14.
 */
public class SpeedyToolServerActions
{
  public SpeedyToolServerActions(ServerVoxelSelections i_serverVoxelSelections, WorldHistory i_worldHistory)
  {
    worldHistory = i_worldHistory;  // new WorldHistory(SpeedyToolsOptions.getMaxComplexToolUndoCount());
    serverVoxelSelections = i_serverVoxelSelections;
  }

  public void setCloneToolsNetworkServer(SpeedyToolsNetworkServer server)
  {
    speedyToolsNetworkServer = server;
  }

  /**
   * performed in response to a "I've made a selection" message from the client
   * @return true for success, false otherwise
   * TODO: make asynchronous later;
   */
  public ResultWithReason prepareForToolAction(EntityPlayerMP player)
  {
    assert (minecraftSaveFolderBackups != null);
    assert (speedyToolsNetworkServer != null);

    if (ServerSide.getInGameStatusSimulator().isTestModeActivated()) {    // testing only
      ResultWithReason resultWithReason = null;
      resultWithReason = ServerSide.getInGameStatusSimulator().prepareForToolAction(speedyToolsNetworkServer, player);
      if (resultWithReason != null) return resultWithReason;
    }

    speedyToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_BACKUP, null, (byte)0);
    minecraftSaveFolderBackups.backupWorld();
    speedyToolsNetworkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
    return ResultWithReason.success();
  }

  /**
   * Performs a server Simple Speedy Tools action in response to an incoming packet from the client: either place or undo
   * @param entityPlayerMP the user sending the packet
   * @param toolItemID the ID of the tool performing this action
   * @param buttonClicked 0 = left (undo), 1 = right (place)
   * @param blockToPlace the Block and metadata to fill the selection with (buttonClicked = 1 only)
   * @param blockSelection the blocks in the selection to be filled (buttonClicked = 1 only)
   */
  public void performServerSimpleAction(EntityPlayerMP entityPlayerMP, int toolItemID, int buttonClicked, BlockWithMetadata blockToPlace, List<ChunkCoordinates> blockSelection)
  {
    WorldServer worldServer = entityPlayerMP.getServerForPlayer();
    switch (buttonClicked) {
      case 0: {
        worldHistory.performSimpleUndo(entityPlayerMP, worldServer);
        return;
      }
      case 1: {
        worldHistory.writeToWorldWithUndo(worldServer, entityPlayerMP, blockToPlace, blockSelection);
        return;
      }
      default: {
        return;
      }
    }
  }

  /**
   *  Start a tool action for the given player
   * @param player
   * @param toolID
   * @param sequenceNumber the unique sequencenumber for this action
   * @param xpos
   * @param ypos
   * @param zpos
   * @param clockwiseRotationCount
   * @param flipped
   * @return true if the action has been successfully started
   */

  public static final long ONE_SECOND_AS_NS = 1000 * 1000 * 1000;

  public ResultWithReason performToolAction(EntityPlayerMP player, int sequenceNumber, int toolID, int xpos, int ypos, int zpos, QuadOrientation quadOrientation)
  {
    System.out.println("Server: Tool Action received sequence #" + sequenceNumber + ": tool " + toolID + " at [" + xpos + ", " + ypos + ", " + zpos
                       + "], rotated:" + quadOrientation.getClockwiseRotationCount() + ", flippedX:" + quadOrientation.isFlippedX());

    VoxelSelectionWithOrigin voxelSelection = serverVoxelSelections.getVoxelSelection(player);

    if (voxelSelection == null) {
      return ResultWithReason.failure("Must wait for spell preparation to finish ...");
    }

    if (!minecraftSaveFolderBackups.isBackedUpRecently()) {
      ResultWithReason result = prepareForToolAction(player);
      if (!result.succeeded()) {
        return ResultWithReason.failure("Too risky!  World backup failed!");
      }
    }

    if (ServerSide.getInGameStatusSimulator().isTestModeActivated()) {    // testing only
      ResultWithReason resultWithReason = null;
      resultWithReason = ServerSide.getInGameStatusSimulator().performToolAction(speedyToolsNetworkServer, player, sequenceNumber, toolID, xpos, ypos, zpos, quadOrientation);
      if (resultWithReason != null) return resultWithReason;
    }

    speedyToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_YOUR_ACTION, player, (byte)0);

    WorldServer worldServer = (WorldServer)player.theItemInWorldManager.theWorld;

    WorldFragment worldFragment = new WorldFragment(voxelSelection.getxSize(), voxelSelection.getySize(), voxelSelection.getzSize());
    worldFragment.readFromWorld(worldServer, voxelSelection.getWxOrigin(), voxelSelection.getWyOrigin(), voxelSelection.getWzOrigin(),
                                             voxelSelection);
    worldHistory.writeToWorldWithUndo(player, worldServer, worldFragment, xpos, ypos, zpos, quadOrientation);
    speedyToolsNetworkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
//    speedyToolsNetworkServer.actionCompleted(player, sequenceNumber);  // todo - later this will be required

    return ResultWithReason.success();
  }

//  /**
//   * sets the current selection for the given player
//   * @param player
//   * @return true if accepted
//   */
//
//  public boolean setCurrentSelection(EntityPlayerMP player, VoxelSelection newSelection)
//  {
//    return true;
//  }

  public ResultWithReason performUndoOfCurrentAction(EntityPlayerMP player, int undoSequenceNumber, int actionSequenceNumber)
  {
    System.out.println("Server: Tool Undo Current Action received: action sequenceNumber " + actionSequenceNumber + ", undo seq number " + undoSequenceNumber);
    if (ServerSide.getInGameStatusSimulator().isTestModeActivated()) {    // testing only
      ResultWithReason resultWithReason = null;
      resultWithReason = ServerSide.getInGameStatusSimulator().performUndoOfCurrentAction(speedyToolsNetworkServer, player, undoSequenceNumber, actionSequenceNumber);
      if (resultWithReason != null) return resultWithReason;
    }

    // we're currently still synchronous undo so this is not relevant yet; just call performUndoOfLastAction instead

    return performUndoOfLastAction(player, undoSequenceNumber);

//    speedyToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, player, (byte)0);
//    speedyToolsNetworkServer.actionCompleted(player, actionSequenceNumber);
//    speedyToolsNetworkServer.undoCompleted(player, undoSequenceNumber);
//    getTestDoSomethingStartTime = System.nanoTime();
//
//    testDoSomethingTime = getTestDoSomethingStartTime + 3 * ONE_SECOND_AS_NS;
//    testUndoSequenceNumber = undoSequenceNumber;
//    testPlayer = player;
//    testActionSequenceNumber = -1;
//    return ResultWithReason.success();
  }

  public ResultWithReason performUndoOfLastAction(EntityPlayerMP player, int undoSequenceNumber)
  {
    System.out.println("Server: Tool Undo Last Completed Action received, undo seq number " + undoSequenceNumber);

    if (ServerSide.getInGameStatusSimulator().isTestModeActivated()) {    // testing only
      ResultWithReason resultWithReason = null;
      resultWithReason = ServerSide.getInGameStatusSimulator().performUndoOfLastAction(speedyToolsNetworkServer, player, undoSequenceNumber);
      if (resultWithReason != null) return resultWithReason;
    }

    speedyToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, player, (byte)0);
    WorldServer worldServer = (WorldServer)player.theItemInWorldManager.theWorld;

    boolean result = worldHistory.performComplexUndo(player, worldServer);

//    speedyToolsNetworkServer.undoCompleted(player, undoSequenceNumber);      // todo later this will be required
    speedyToolsNetworkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
    if (result) {
      return ResultWithReason.success();
    } else {
      return ResultWithReason.failure("There are no more spells to undo...");
    }
//    getTestDoSomethingStartTime = System.nanoTime();
//    testDoSomethingTime = getTestDoSomethingStartTime + 7 * ONE_SECOND_AS_NS;
//    testUndoSequenceNumber = undoSequenceNumber;
//    testPlayer = player;
  }

  public void tick() {
    if (ServerSide.getInGameStatusSimulator().isTestModeActivated()) {
      ServerSide.getInGameStatusSimulator().updateServerStatus(speedyToolsNetworkServer);
    }
  }

  int testUndoSequenceNumber = -1;
  int testActionSequenceNumber = -1;
  long testDoSomethingTime = Long.MAX_VALUE;
  long getTestDoSomethingStartTime = 0;
  boolean iAmBusy = false;

  EntityPlayerMP testPlayer;

  /**
   * ensure that the save folder backups are initialised
   * @param world
   */
  public static void worldLoadEvent(World world)
  {
    if (minecraftSaveFolderBackups == null) {
      minecraftSaveFolderBackups = new MinecraftSaveFolderBackups();
    } else {
      Path savePath = DimensionManager.getCurrentSaveRootDirectory().toPath();
      if (!savePath.equals(minecraftSaveFolderBackups.getSourceSaveFolder())) {
        minecraftSaveFolderBackups = new MinecraftSaveFolderBackups();
      }
    }
  }

  public static void worldUnloadEvent(World world)
  {
    // for now - don't need to do anything
  }

  private static MinecraftSaveFolderBackups minecraftSaveFolderBackups;
  private static SpeedyToolsNetworkServer speedyToolsNetworkServer;
  private WorldHistory worldHistory;
  private ServerVoxelSelections serverVoxelSelections;
}
