package speedytools.serverside;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import speedytools.clientside.selections.VoxelSelection;
import speedytools.common.network.Packet250CloneToolUse;
import speedytools.common.network.ServerStatus;
import speedytools.common.network.multipart.SelectionPacket;
import speedytools.common.utilities.UsefulFunctions;
import speedytools.serverside.backup.MinecraftSaveFolderBackups;

import java.nio.file.Path;

/**
 * Created by TheGreyGhost on 7/03/14.
 */
public class CloneToolServerActions
{

  public void setCloneToolsNetworkServer(CloneToolsNetworkServer server)
  {
    cloneToolsNetworkServer = server;
  }

  /**
   * performed in response to a "I've made a selection" message from the client
   * @return true for success, false otherwise
   * TODO: make asynchronous later
   */
  public boolean prepareForToolAction(EntityPlayerMP player)
  {
    assert (minecraftSaveFolderBackups != null);
    assert (cloneToolsNetworkServer != null);

    cloneToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_BACKUP, null, (byte)0);
    minecraftSaveFolderBackups.backupWorld();
    cloneToolsNetworkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
    return true;
  }

  /**
   *  Start a tool action for the given player
   * @param player
   * @param toolID
   * @param sequenceNumber the unique sequencenumber for this action
   * @param xpos
   * @param ypos
   * @param zpos
   * @param rotationCount
   * @param flipped
   * @return true if the action has been successfully started
   */

  public static final long ONE_SECOND_AS_NS = 1000 * 1000 * 1000;

  public boolean performToolAction(EntityPlayerMP player, int sequenceNumber, int toolID, int xpos, int ypos, int zpos, byte rotationCount, boolean flipped)
  {
    cloneToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_YOUR_ACTION, player, (byte)0);
    System.out.println("Server: Tool Action received sequence #" + sequenceNumber + ": tool " + toolID + " at [" + xpos + ", " + ypos + ", " + zpos + "], rotated:" + rotationCount + ", flipped:" + flipped);
    getTestDoSomethingStartTime = System.nanoTime();
    testDoSomethingTime = getTestDoSomethingStartTime + 20 * ONE_SECOND_AS_NS;
    testActionSequenceNumber = sequenceNumber;
    testPlayer = player;
    return true;
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

  public boolean performUndoOfCurrentAction(EntityPlayerMP player, int undoSequenceNumber, int actionSequenceNumber)
  {
    cloneToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, player, (byte)0);
    cloneToolsNetworkServer.actionCompleted(player, actionSequenceNumber);
    System.out.println("Server: Tool Undo Current Action received: action sequenceNumber " + actionSequenceNumber + ", undo seq number " + undoSequenceNumber);
    getTestDoSomethingStartTime = System.nanoTime();

    testDoSomethingTime = getTestDoSomethingStartTime + 3 * ONE_SECOND_AS_NS;
    testUndoSequenceNumber = undoSequenceNumber;
    testPlayer = player;
    testActionSequenceNumber = -1;
    return true;
  }

  public boolean performUndoOfLastAction(EntityPlayerMP player, int undoSequenceNumber)
  {
    cloneToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, player, (byte)0);
    System.out.println("Server: Tool Undo Last Completed Action received, undo seq number " + undoSequenceNumber);
    getTestDoSomethingStartTime = System.nanoTime();
    testDoSomethingTime = getTestDoSomethingStartTime + 7 * ONE_SECOND_AS_NS;
    testUndoSequenceNumber = undoSequenceNumber;
    testPlayer = player;
    return true;
  }

  public void tick() {

    if (System.nanoTime() >= testDoSomethingTime) {
      testDoSomethingTime = Long.MAX_VALUE;
      if (testActionSequenceNumber >= 0) {
        cloneToolsNetworkServer.actionCompleted(testPlayer, testActionSequenceNumber);
        System.out.println("Server: actionCompleted # " + testActionSequenceNumber);
        testActionSequenceNumber = -1;
        cloneToolsNetworkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
      }
      if (testUndoSequenceNumber >= 0) {
        cloneToolsNetworkServer.undoCompleted(testPlayer, testUndoSequenceNumber);
        System.out.println("Server: undoCompleted # " + testUndoSequenceNumber);
        testUndoSequenceNumber = -1;
      }
    }

    double progress = (System.nanoTime() - getTestDoSomethingStartTime);
    progress /= (testDoSomethingTime - getTestDoSomethingStartTime);
    progress = UsefulFunctions.clipToRange(progress, 0.0, 100.0);
    if (testActionSequenceNumber >= 0) {
      cloneToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_YOUR_ACTION, testPlayer, (byte)progress);
    } else if (testUndoSequenceNumber >= 0) {
      cloneToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, testPlayer, (byte)progress);
    } else {
      cloneToolsNetworkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
    }

  }

  int testUndoSequenceNumber = -1;
  int testActionSequenceNumber = -1;
  long testDoSomethingTime = Long.MAX_VALUE;
  long getTestDoSomethingStartTime = 0;
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
  private static CloneToolsNetworkServer cloneToolsNetworkServer;
}
