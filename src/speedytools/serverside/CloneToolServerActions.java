package speedytools.serverside;

import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import speedytools.common.network.Packet250CloneToolUse;
import speedytools.serverside.backup.MinecraftSaveFolderBackups;

import java.nio.file.Path;

/**
 * Created by TheGreyGhost on 7/03/14.
 */
public class CloneToolServerActions
{
  public static void handlePacket(Packet250CloneToolUse incomingPacket)
  {
    switch (incomingPacket.getCommand()) {
      case Packet250CloneToolUse.COMMAND_SELECTION_MADE: {
        assert (minecraftSaveFolderBackups != null);
        minecraftSaveFolderBackups.backupWorld();
        break;
      }
      case Packet250CloneToolUse.COMMAND_TOOL_ACTION_PERFORMED: {

        break;
      }
      case Packet250CloneToolUse.COMMAND_TOOL_UNDO_PERFORMED: {
        break;
      }
      default: {
        assert false: "Invalid server side packet";
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

  private static MinecraftSaveFolderBackups minecraftSaveFolderBackups;
}
