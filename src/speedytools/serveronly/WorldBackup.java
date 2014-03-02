package speedytools.serveronly;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandServerSaveAll;
import net.minecraft.command.CommandServerSaveOff;
import net.minecraft.command.CommandServerSaveOn;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.DimensionManager;
import speedytools.common.ErrorLog;

import java.io.*;
import java.nio.file.*;

/**
 * Created by TheGreyGhost on 24/02/14.
 * WorldBackup is used to maintain a series of backups in case the cloning tools cause severe damage to the world
 * The intended operation is:
 * (1) whenever the clone tool is about to be used (but no more frequently than every 5 minutes), the current save folder
 *     is copied to a new folder
 * (2) as each new backup is created, one of the older backups may be deleted.  The deletion is performed to
 *     keep a series of backups with increasing spacing as they get older
 *     Up to six backups will be kept, the oldest will be at least 14 saves old (up to 21)
 * A backuplisting file is created in the original save folder to record the backup folders created
 * In addition, each backup folder contains a snapshot listing of all the filenames including size and timestamp. The folder is only
 *   deleted if all files still match exactly.
 *
 * Usage:
 * (1) Create a WorldBackup(saveFolderToBeBackedUp) - or WorldBackup() to use the current save folder
 * (2) Each time you wish to create a backup, call .backupWorld();  if it returns true, the backup was successful
 */
public class WorldBackup
{
  /**
   * Creates a WorldBackup for the current save folder
   */
  public WorldBackup()
  {
    this(null);
  }

  /**
   * Creates a WorldBackup for the given save folder (not necessarily the current one)
   * @param currentSaveFolder
   */
  public WorldBackup(Path currentSaveFolder)
  {
    if (currentSaveFolder == null) {
      currentSaveFolder = DimensionManager.getCurrentSaveRootDirectory().toPath();
    }
    sourceSaveFolder = currentSaveFolder;
    if (sourceSaveFolder == null) return;

    Path backupListingPath = sourceSaveFolder.resolve(BACKUP_LISTING_FILENAME);
    storedBackups = new StoredBackups();
    boolean success = storedBackups.retrieveBackupListing(backupListingPath);
    if (!success) storedBackups = null;
  }

  private static final String BACKUP_LISTING_FILENAME = "backuplisting.dat";

  boolean backupWorld()
  {

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
      File rootSavesFolder = new File(Minecraft.getMinecraft().mcDataDir, "saves");
      String saveFolderName = sourceSaveFolder.getFileName().toString();

//      success = backupFilename.mkdir();
      if (success) {
//        success = BackupMinecraftSave.createBackupSave(sourceSaveFolder, backupFilename.toPath(), "Test");
      }

    } catch (Exception e) {
      // todo: think of something to put here...
      success = false;
      ErrorLog.defaultLog().severe("WorldBackup::backupWorld() failed to create backup save: %s", e);
    }

    saveOn.processCommand(minecraftServer, dummy);
    return success;
  }

  private Path sourceSaveFolder;
  private StoredBackups storedBackups;

}
