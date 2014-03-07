package speedytools.serverside.backup;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandServerSaveAll;
import net.minecraft.command.CommandServerSaveOff;
import net.minecraft.command.CommandServerSaveOn;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.DimensionManager;
import speedytools.common.utilities.ErrorLog;

import java.io.*;
import java.nio.file.*;
import java.util.Calendar;

/**
 * Created by TheGreyGhost on 24/02/14.
 * MinecraftSaveFolderBackups is used to maintain a series of backups in case the cloning tools cause severe damage to the world
 * The intended operation is:
 * (1) whenever the clone tool is about to be used (but no more frequently than every 5 minutes = MINIMUM_TIME_BETWEEN_BACKUPS_MILLIS), the current save folder
 *     is copied to a new folder
 * (2) as each new backup is created, one of the older backups may be deleted.  The deletion is performed to
 *     keep a series of backups with increasing spacing as they get older
 *     Up to six backups will be kept, the oldest will be at least 14 saves old (up to 21)
 * A backuplisting file is created in the original save folder to record the backup folders created
 * In addition, each backup folder contains a snapshot listing of all the filenames including size and timestamp. The folder is only
 *   deleted if all files still match exactly.
 *
 * Usage:
 * (1) Create a MinecraftSaveFolderBackups(saveFolderToBeBackedUp) - or MinecraftSaveFolderBackups() to use the current save folder
 * (2) Each time you wish to create a backup, call .backupWorld();  if it returns true, the backup was successful
 */
public class MinecraftSaveFolderBackups
{
  /**
   * Creates a MinecraftSaveFolderBackups for the current save folder
   */
  public MinecraftSaveFolderBackups()
  {
    this(null);
  }

  /**
   * Creates a MinecraftSaveFolderBackups for the given save folder (not necessarily the current one)
   * @param currentSaveFolder
   */
  public MinecraftSaveFolderBackups(Path currentSaveFolder)
  {
    if (currentSaveFolder == null) {
      currentSaveFolder = DimensionManager.getCurrentSaveRootDirectory().toPath();
    }
    sourceSaveFolder = currentSaveFolder;
    if (sourceSaveFolder == null) return;

    Path backupListingPath = sourceSaveFolder.resolve(BACKUP_LISTING_FILENAME);
    storedBackups = new StoredBackups();
    storedBackups.retrieveBackupListing(backupListingPath);
  }

  private static final String BACKUP_LISTING_FILENAME = "backuplisting.dat";
  private static final long MINIMUM_TIME_BETWEEN_BACKUPS_MILLIS = 5 * 60 * 1000;  // 5 minutes

  public boolean backupWorld()
  {
    if (sourceSaveFolder == null) return false;
    Calendar now = Calendar.getInstance();

    if (now.getTimeInMillis() - lastSaveTimeInMillis < MINIMUM_TIME_BETWEEN_BACKUPS_MILLIS) return false;

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
      Path rootSavesFolder = new File(Minecraft.getMinecraft().mcDataDir, "saves").toPath();
      success = storedBackups.createBackupSave(sourceSaveFolder, rootSavesFolder, now.toString());
      if (success) {
        storedBackups.cullSurplus();     // ignore any failure to cull.
      }
      boolean savedOK;
      savedOK = storedBackups.saveBackupListing(sourceSaveFolder.resolve(BACKUP_LISTING_FILENAME));
      if (!savedOK) {
        ErrorLog.defaultLog().warning("storedBackups.saveBackupListing failed");
      }
    } catch (Exception e) {
      success = false;
      ErrorLog.defaultLog().severe("MinecraftSaveFolderBackups::backupWorld() failed to create backup save: %s", e);
    }

    saveOn.processCommand(minecraftServer, dummy);
    if (success) {
      lastSaveTimeInMillis = now.getTimeInMillis();
    }
    return success;
  }

  public Path getSourceSaveFolder() {
    return sourceSaveFolder;
  }

  private Path sourceSaveFolder;
  private StoredBackups storedBackups;
  private long lastSaveTimeInMillis = 0;

}
