package speedytools.serveronly;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandServerSaveAll;
import net.minecraft.command.CommandServerSaveOff;
import net.minecraft.command.CommandServerSaveOn;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.DimensionManager;

import java.io.File;

/**
 * Created by TheGreyGhost on 24/02/14.
 */
public class WorldBackup
{
  static boolean backupWorld()
  {
    MinecraftServer minecraftServer = MinecraftServer.getServer();

    CommandServerSaveOff saveOff = new CommandServerSaveOff();
    CommandServerSaveAll saveAll = new CommandServerSaveAll();
    CommandServerSaveOn saveOn = new CommandServerSaveOn();

    String[] dummy = {"flush"};

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

      boolean success = backupFilename.mkdir();

    } catch (Exception e) {
      // todo: think of something to put here...

    }

    saveOn.processCommand(minecraftServer, dummy);
    return true;
  }
}
