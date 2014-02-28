package speedytools.serveronly;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandServerSaveAll;
import net.minecraft.command.CommandServerSaveOff;
import net.minecraft.command.CommandServerSaveOn;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.DimensionManager;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by TheGreyGhost on 24/02/14.
 * WorldBackup is used to maintain a series of backups in case the cloning tools cause severe damage to the world
 * The operation is:
 * (1) whenever the clone tool is about to be used (but no more frequently than every 5 minutes), the current save folder
 *     is copied to a new folder
 * (2) as each new backup is created, one of the older backups may be deleted.  The deletion is performed to
 *     keep a series of backups with increasing spacing as they get older
 *     Up to six backups will be kept, the oldest will be at least 14 saves old (up to 21)
 */
public class WorldBackup
{
  static boolean backupWorld()
  {
    // If the saves are numbered from 0, 1, 2, etc and savenumber is the number of the
    //   current save,
    //   then savenumber - deletionSchedule[savenumber%16] is to be deleted
    // This sequence leads to a fairly evenly increasing gap between saves, up to 6 saves deep
    int[] deletionSchedule = {1, 13, 1, 5, 1, 21, 1, 5, 1, 13, 1, 5, 1, 21, 1, 5};

    boolean success = false;
    MinecraftServer minecraftServer = MinecraftServer.getServer();

    CommandServerSaveOff saveOff = new CommandServerSaveOff();
    CommandServerSaveAll saveAll = new CommandServerSaveAll();
    CommandServerSaveOn saveOn = new CommandServerSaveOn();

    String[] dummyFlush = {"flush"};
    String[] dummy = {""};

    saveOff.processCommand(minecraftServer, dummy);
    saveAll.processCommand(minecraftServer, dummy);

    try {

      File currentSaveFolder = DimensionManager.getCurrentSaveRootDirectory();
      File rootSavesFolder = new File(Minecraft.getMinecraft().mcDataDir, "saves");

      String saveFolderName = currentSaveFolder.getName();

      int backupNumber = 1;
      boolean backupFolderExists;
      File backupFilename;
      do {

        String backupFolderName = saveFolderName + "-bk" + backupNumber;
        backupFilename = new File(rootSavesFolder, backupFolderName);
        backupFolderExists = backupFilename.exists();
      } while (backupFolderExists);

      success = backupFilename.mkdir();
      if (success) {
        success = BackupMinecraftSave.createBackupSave(currentSaveFolder.toPath(), backupFilename.toPath(), "Test");
      }

    } catch (Exception e) {
      // todo: think of something to put here...
      success = false;
      FMLLog.severe("WorldBackup::backupWorld() failed to create backup save: %s", e);
    }

    saveOn.processCommand(minecraftServer, dummy);
    return success;
  }
}
