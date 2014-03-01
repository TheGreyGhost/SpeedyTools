package speedytools.serveronly;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandServerSaveAll;
import net.minecraft.command.CommandServerSaveOff;
import net.minecraft.command.CommandServerSaveOn;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.DimensionManager;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Iterator;
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
 * A backuplisting file is created in the original save folder to record the backup folders created
 * Each backup folder contains a snapshot listing of all the filenames including size and timestamp. The folder is only
 *   deleted if all files still match exactly.
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
  public WorldBackup(File currentSaveFolder)
  {
    if (currentSaveFolder == null) {
      currentSaveFolder = DimensionManager.getCurrentSaveRootDirectory();
    }
    sourceSaveFolder = currentSaveFolder;
    if (sourceSaveFolder == null) return;

    Path backupListingPath = sourceSaveFolder.toPath();
    backupListingPath.resolve(BACKUP_LISTING_FILENAME);
    backupListing = retrieveBackupListing(backupListingPath);
  }

  private static final String BACKUP_LISTING_FILENAME = "backuplisting.dat";

  private static final String BACKUP_LISTING_VERSION_TAG = "VERSION";
  private static final String BACKUP_LISTING_PATHS_TAG = "PATHS";
  private static final String BACKUP_LISTING_NAME_VALUE = "BACKUPLISTING";
  private static final String BACKUP_LISTING_VERSION_VALUE = "V1";

  /**
   * parses the backuplisting file into a map of backup number vs path
   * in case of error, parses as much as possible.
   * does not check if the paths actually exist
   * @param backupListingPath
   * @return a map of backup number vs path; listing will be truncated in case of errors
   */
  public static HashMap<Integer, Path> retrieveBackupListing(Path backupListingPath)
  {
    HashMap<Integer, Path> retval = new HashMap<Integer, Path>();
    if (!Files.isRegularFile(backupListingPath) || !Files.isReadable(backupListingPath)) {
      return retval;
    }
    try {
      NBTTagCompound backupListing = CompressedStreamTools.read(backupListingPath.toFile());
      if (!backupListing.getName().equals(BACKUP_LISTING_NAME_VALUE) ) {
        FMLLog.warning("Invalid backuplisting file (root name wrong): " + backupListingPath.toString());
        return retval;
      }
      if (!backupListing.hasKey(BACKUP_LISTING_VERSION_TAG) || !backupListing.hasKey(BACKUP_LISTING_PATHS_TAG)) {
        FMLLog.warning("Invalid backuplisting file (missing tag): " + backupListingPath.toString());
        return retval;
      }
      if (!backupListing.getString(BACKUP_LISTING_VERSION_TAG).equals(BACKUP_LISTING_VERSION_VALUE)) {
        FMLLog.warning("Invalid backuplisting file (version wrong): "  + backupListingPath.toString());
        return retval;
      }
      backupListing = backupListing.getCompoundTag(BACKUP_LISTING_PATHS_TAG);

      Iterator iterator = backupListing.getTags().iterator();

      while (iterator.hasNext())
      {
        NBTBase nbtbase = (NBTBase)iterator.next();

        if (nbtbase instanceof NBTTagString)
        {
          NBTTagString entry = (NBTTagString)nbtbase;
          String backupNumberString = entry.getName();
          Integer backupNumber;
          try {
            backupNumber = new Integer(Integer.parseInt(backupNumberString));
          } catch (NumberFormatException nfe) {
            FMLLog.warning("Invalid backupNumber tag in backuplisting file: "  + backupListingPath.toString());
            return retval;
          }
          if (retval.containsKey(backupNumber)) {
            FMLLog.warning("Duplicate backupNumber tag in backuplisting file: "  + backupListingPath.toString());
            return retval;
          }
          retval.put(backupNumber, Paths.get(entry.data));
        } else {
          FMLLog.warning("Invalid backupNumber entry in backuplisting file: "  + backupListingPath.toString());
          return retval;
        }
      }
    } catch (IOException ioe) {
      FMLLog.warning("Failure while reading backuplisting file ("  + backupListingPath.toString() + ") :" + ioe.toString());
      return retval;
    }
    return retval;
  }

  /**
   * writes the given backups to the given Path; overwrites automatically if already exists
   * @return true if successful, false otherwise.
   */
  public static boolean saveBackupListing(Path fileToCreate, HashMap<Integer, Path> backups)
  {
    try {
      NBTTagCompound backupListingNBT = new NBTTagCompound(BACKUP_LISTING_NAME_VALUE);
      backupListingNBT.setString(BACKUP_LISTING_VERSION_TAG, BACKUP_LISTING_VERSION_VALUE);

      NBTTagCompound paths = new NBTTagCompound(BACKUP_LISTING_PATHS_TAG);
      for (Map.Entry<Integer, Path> entry : backups.entrySet()) {
        paths.setString(entry.getKey().toString(), entry.getValue().toString());
      }
      backupListingNBT.setTag("WhereIsThis", paths);

      OutputStream out = null;
      try {
        out = Files.newOutputStream(fileToCreate, StandardOpenOption.CREATE_NEW);
        CompressedStreamTools.writeCompressed(backupListingNBT, out);
      } catch (IOException e) {
        FMLLog.severe("BackupMinecraftSave::createBackupSave() failed to create backup save: %s", e.toString());
        return false;
      } finally {
        if (out != null) {
          try {
            out.close();
          } catch (Exception e) {
            // ignore
          }
        }
      }
    } catch (Exception e) {
      FMLLog.severe("BackupMinecraftSave::createBackupSave() failed to create backup save: %s", e.toString());
      return false;
    }

    return true;
  }

  boolean backupWorld()
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
      File rootSavesFolder = new File(Minecraft.getMinecraft().mcDataDir, "saves");
      String saveFolderName = sourceSaveFolder.getName();

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
        success = BackupMinecraftSave.createBackupSave(sourceSaveFolder.toPath(), backupFilename.toPath(), "Test");
      }

    } catch (Exception e) {
      // todo: think of something to put here...
      success = false;
      FMLLog.severe("WorldBackup::backupWorld() failed to create backup save: %s", e);
    }

    saveOn.processCommand(minecraftServer, dummy);
    return success;
  }

  private File sourceSaveFolder;

  private HashMap<Integer, Path> backupListing;

}
