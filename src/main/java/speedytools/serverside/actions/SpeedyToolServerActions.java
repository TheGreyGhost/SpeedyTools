package speedytools.serverside.actions;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import speedytools.common.SpeedyToolsOptions;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.items.RegistryForItems;
import speedytools.common.network.ServerStatus;
import speedytools.common.selections.VoxelSelectionWithOrigin;
import speedytools.common.utilities.ErrorLog;
import speedytools.common.utilities.QuadOrientation;
import speedytools.common.utilities.ResultWithReason;
import speedytools.serverside.ServerSide;
import speedytools.serverside.ServerVoxelSelections;
import speedytools.serverside.backup.MinecraftSaveFolderBackups;
import speedytools.serverside.network.SpeedyToolsNetworkServer;
import speedytools.serverside.worldmanipulation.AsynchronousToken;
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
    worldHistory = i_worldHistory;
    serverVoxelSelections = i_serverVoxelSelections;
  }

  public void setCloneToolsNetworkServer(SpeedyToolsNetworkServer server)
  {
    speedyToolsNetworkServer = server;
  }

  /**
   * performed in response to a "I've made a selection" message from the client
   * @return true for success, false otherwise
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
   * @param buttonClicked 0 = left (undo), 1 = right (place)
   * @param blockToPlace the Block and metadata to fill the selection with (buttonClicked = 1 only)
   * @param sideToPlace
   * @param blockSelection the blocks in the selection to be filled (buttonClicked = 1 only)
   */
  public void performSimpleAction(EntityPlayerMP entityPlayerMP, int buttonClicked, BlockWithMetadata blockToPlace, EnumFacing sideToPlace, List<BlockPos> blockSelection)
  {
    WorldServer worldServer = entityPlayerMP.getServerForPlayer();
    switch (buttonClicked) {
      case 0: {
        worldHistory.performSimpleUndo(entityPlayerMP, worldServer);
        return;
      }
      case 1: {
        worldHistory.writeToWorldWithUndo(worldServer, entityPlayerMP, blockToPlace, sideToPlace, blockSelection);
        return;
      }
      default: {
        return;
      }
    }
  }

  /**
   * Starts a complex (asynchronous) action.  It will be progressed automatically whenever tick() is called.
   * @param player
   * @param sequenceNumber
   * @param toolID
   * @param fillBlock for fill tools, the block that will be used to fill
   *@param xpos
   * @param ypos
   * @param zpos
   * @param quadOrientation     @return
   */
  public ResultWithReason performComplexAction(EntityPlayerMP player, int sequenceNumber, int toolID,
                                               BlockWithMetadata fillBlock,
                                               int xpos, int ypos, int zpos, QuadOrientation quadOrientation,
                                               BlockPos initialSelectionOrigin)
  {
    assert (!isAsynchronousActionInProgress());
//    System.out.println("Server: Tool Action received sequence #" + sequenceNumber + ": tool " + toolID + " at [" + xpos + ", " + ypos + ", " + zpos
//                       + "], rotated:" + quadOrientation.getClockwiseRotationCount() + ", flippedX:" + quadOrientation.isFlippedX());

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
      resultWithReason = ServerSide.getInGameStatusSimulator().performToolAction(speedyToolsNetworkServer, player,
                                                                                 sequenceNumber, toolID,
                                                                                 xpos, ypos, zpos, quadOrientation,
                                                                                 initialSelectionOrigin);
      if (resultWithReason != null) return resultWithReason;
    }

    WorldServer worldServer = (WorldServer)player.theItemInWorldManager.theWorld;

    System.out.println("initialSelectionOrigin:" + initialSelectionOrigin);
    System.out.println("voxelSelection: [" + voxelSelection.getWxOrigin() + "," + voxelSelection.getWyOrigin()
                       + ", " + voxelSelection.getWzOrigin() + "]");
    System.out.println("placement pos before: [" + xpos + "," + ypos + ", " + zpos + "]");
    // In some cases, the client will still be using the client-side-generated selection while the server has calculated
    //  a new, expanded selection to include the chunks that the client couldn't see.  In this case, the selection
    //  origins may not match; and if not, the placement position must be corrected to account for it.
    int dx = voxelSelection.getWxOrigin() - initialSelectionOrigin.getX();
    int dy = voxelSelection.getWyOrigin() - initialSelectionOrigin.getY();
    int dz = voxelSelection.getWzOrigin() - initialSelectionOrigin.getZ();
    if (dx != 0 || dy != 0 || dz != 0) {
      xpos += dx;
      ypos += dy;
      zpos += dz;
      System.out.println("placement pos after: [" + xpos + "," + ypos + ", " + zpos + "]");
    }

    STILL DOESnt appear to be right when flipping.  Also - not ready error on second placement after first placement without undo in between

    AsynchronousActionBase token;
    if (toolID == Item.getIdFromItem(RegistryForItems.itemComplexCopy)) {
      token = new AsynchronousActionCopy(worldServer, player, worldHistory, voxelSelection, sequenceNumber, toolID, xpos, ypos, zpos, quadOrientation);
    } else if (toolID == Item.getIdFromItem(RegistryForItems.itemComplexDelete)) {
      BlockWithMetadata airBWM = new BlockWithMetadata(Blocks.air, 0);
      token = new AsynchronousActionFill(worldServer, player, worldHistory, voxelSelection, airBWM, sequenceNumber, toolID, xpos, ypos, zpos, quadOrientation);
    } else if (toolID == Item.getIdFromItem(RegistryForItems.itemComplexMove)) {
      token = new AsynchronousActionMove(worldServer, player, worldHistory, voxelSelection, sequenceNumber, toolID, xpos, ypos, zpos, quadOrientation);
    } else if (toolID == Item.getIdFromItem(RegistryForItems.itemSpeedyOrb)) {
      token = new AsynchronousActionFill(worldServer, player, worldHistory, voxelSelection, fillBlock, sequenceNumber, toolID, xpos, ypos, zpos, quadOrientation);
    } else if (toolID == Item.getIdFromItem(RegistryForItems.itemSpeedySceptre)) {
      token = new AsynchronousActionFill(worldServer, player, worldHistory, voxelSelection, fillBlock, sequenceNumber, toolID, xpos, ypos, zpos, quadOrientation);
    } else {
      ErrorLog.defaultLog().info("Invalid toolID received in performComplexAction:" + toolID);
      return ResultWithReason.failure();
    }

    token.setTimeOfInterrupt(AsynchronousToken.IMMEDIATE_TIMEOUT);
    speedyToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_YOUR_ACTION, player, (byte) 0);
    token.continueProcessing();
    asynchronousTaskInProgress = token;
    asynchronousTaskActionType = ActionType.ACTION;
    asynchronousTaskSequenceNumber = sequenceNumber;
    asynchronousTaskEntityPlayerMP = player;

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

  public ResultWithReason performUndoOfCurrentComplexAction(EntityPlayerMP player, int undoSequenceNumber, int actionSequenceNumber)
  {
//    System.out.println("Server: Tool Undo Current Action received: action sequenceNumber " + actionSequenceNumber + ", undo seq number " + undoSequenceNumber);
    if (ServerSide.getInGameStatusSimulator().isTestModeActivated()) {    // testing only
      ResultWithReason resultWithReason = null;
      resultWithReason = ServerSide.getInGameStatusSimulator().performUndoOfCurrentAction(speedyToolsNetworkServer, player, undoSequenceNumber, actionSequenceNumber);
      if (resultWithReason != null) return resultWithReason;
    }

    // we're currently still synchronous undo so this is not relevant yet; just call performUndoOfLastAction instead
    if (asynchronousTaskInProgress != null && !asynchronousTaskInProgress.isTaskComplete()) {
      asynchronousTaskInProgress.rollback(undoSequenceNumber);
      asynchronousTaskActionType = ActionType.UNDO;
      asynchronousTaskSequenceNumber = undoSequenceNumber;
      asynchronousTaskEntityPlayerMP = player;
      return ResultWithReason.success();
    }

    return performUndoOfLastComplexAction(player, undoSequenceNumber);
  }

  public ResultWithReason performUndoOfLastComplexAction(EntityPlayerMP player, int undoSequenceNumber)
  {
//    System.out.println("Server: Tool Undo Last Completed Action received, undo seq number " + undoSequenceNumber);

    if (ServerSide.getInGameStatusSimulator().isTestModeActivated()) {    // testing only
      ResultWithReason resultWithReason = null;
      resultWithReason = ServerSide.getInGameStatusSimulator().performUndoOfLastAction(speedyToolsNetworkServer, player, undoSequenceNumber);
      if (resultWithReason != null) return resultWithReason;
    }

    WorldServer worldServer = (WorldServer)player.theItemInWorldManager.theWorld;
    AsynchronousActionBase token = new AsynchronousActionUndo(speedyToolsNetworkServer, worldServer, player, worldHistory, undoSequenceNumber);

    asynchronousTaskActionType = ActionType.UNDO;
    asynchronousTaskSequenceNumber = undoSequenceNumber;
    asynchronousTaskEntityPlayerMP = player;
    token.setTimeOfInterrupt(AsynchronousToken.IMMEDIATE_TIMEOUT);
    token.continueProcessing();
    if (token.isTaskAborted()) {
      return ResultWithReason.failure("There are no more spells to undo...");
    }
    asynchronousTaskInProgress = token;
    return ResultWithReason.success();
  }

  public void tick() {
    if (ServerSide.getInGameStatusSimulator().isTestModeActivated()) {
      ServerSide.getInGameStatusSimulator().updateServerStatus(speedyToolsNetworkServer);
    }

    long stopTimeNS = System.nanoTime() + SpeedyToolsOptions.getMaxServerBusyTimeMS() * 1000L * 1000L;
    final int STATUS_UPDATE_PERIOD_TICKS = 10;

    if (asynchronousTaskInProgress != null && !asynchronousTaskInProgress.isTaskComplete()) {
      asynchronousTaskInProgress.setTimeOfInterrupt(stopTimeNS);
      asynchronousTaskInProgress.continueProcessing();

      if (asynchronousTaskInProgress.isTaskComplete()) {
        if (asynchronousTaskActionType == ActionType.ACTION) {
          speedyToolsNetworkServer.actionCompleted(asynchronousTaskEntityPlayerMP, asynchronousTaskSequenceNumber);
        } else {
          speedyToolsNetworkServer.undoCompleted(asynchronousTaskEntityPlayerMP, asynchronousTaskSequenceNumber);
        }
        asynchronousTaskInProgress = null;
      } else if (0 == (ServerSide.getGlobalTickCount() % STATUS_UPDATE_PERIOD_TICKS)) {  // task not complete
        speedyToolsNetworkServer.changeServerStatus((asynchronousTaskActionType == ActionType.ACTION) ? ServerStatus.PERFORMING_YOUR_ACTION : ServerStatus.UNDOING_YOUR_ACTION,
                asynchronousTaskEntityPlayerMP,
                (byte) (100 * asynchronousTaskInProgress.getFractionComplete()));
      }
    }

    if (!ServerSide.getInGameStatusSimulator().isTestModeActivated()) {
      if (asynchronousTaskInProgress == null || asynchronousTaskInProgress.isTaskComplete()) {
        speedyToolsNetworkServer.changeServerStatus(ServerStatus.IDLE, null, (byte) 0);
      }
    }
  }

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

  // return true if an asynchronous action is in progress - backup, complex placement, complex undo
  public boolean isAsynchronousActionInProgress()
  {
    if (asynchronousTaskInProgress != null && !asynchronousTaskInProgress.isTaskComplete()) return true;
    return false;
  }

  private static MinecraftSaveFolderBackups minecraftSaveFolderBackups;
  private static SpeedyToolsNetworkServer speedyToolsNetworkServer;
  private WorldHistory worldHistory;
  protected ServerVoxelSelections serverVoxelSelections;  // protected for test stub

  private AsynchronousActionBase asynchronousTaskInProgress;
  private int asynchronousTaskSequenceNumber;
  enum ActionType {ACTION, UNDO};
  private ActionType asynchronousTaskActionType;
  private EntityPlayerMP asynchronousTaskEntityPlayerMP;

}
