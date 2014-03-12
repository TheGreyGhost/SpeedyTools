package speedytools.serverside;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import speedytools.common.network.Packet250CloneToolUse;
import speedytools.common.network.ServerStatus;
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
  public boolean performToolAction(EntityPlayerMP player, int sequenceNumber, int toolID, int xpos, int ypos, int zpos, byte rotationCount, boolean flipped)
  {
    cloneToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_YOUR_ACTION, player, (byte)0);
    System.out.println("Server: Tool Action received sequence #" + sequenceNumber + ": tool " + toolID + " at [" + xpos + ", " + ypos + ", " + zpos + "], rotated:" + rotationCount + ", flipped:" + flipped);
    return true;
  }

  public boolean performUndoOfCurrentAction(EntityPlayerMP player, int undoSequenceNumber, int actionSequenceNumber)
  {
    cloneToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, player, (byte)0);
    System.out.println("Server: Tool Undo Current Action received: sequenceNumber " + actionSequenceNumber);
    return true;
  }

  public boolean performUndoOfLastAction(EntityPlayerMP player, int undoSequenceNumber)
  {
    cloneToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, player, (byte)0);
    System.out.println("Server: Tool Undo Last Completed Action received ");
    return true;
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

  private static MinecraftSaveFolderBackups minecraftSaveFolderBackups;
  private static CloneToolsNetworkServer cloneToolsNetworkServer;
}
