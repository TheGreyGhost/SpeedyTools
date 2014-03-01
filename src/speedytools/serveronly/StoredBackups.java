package speedytools.serveronly;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import speedytools.common.ErrorLog;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: The Grey Ghost
 * Date: 1/03/14
 * Maintains a record of the backed up save folders
 */
public class StoredBackups
{
  public StoredBackups() {
    this(null);
  }

  public StoredBackups(HashMap<Integer, Path> i_backupListing) {
    backupListing = i_backupListing;
  }

  private static final String BACKUP_LISTING_VERSION_TAG = "VERSION";
  private static final String BACKUP_LISTING_PATHS_TAG = "PATHS";
  private static final String BACKUP_LISTING_NAME_VALUE = "BACKUPLISTING";
  private static final String BACKUP_LISTING_VERSION_VALUE = "V1";

  /**
   * parses the backuplisting file into a map of backup number vs path
   * in case of error, parses as much as possible.
   * does not check if the paths actually exist
   * @param backupListingPath
   * @return true for success, false otherwise
   */
  public boolean retrieveBackupListing(Path backupListingPath)
  {
    backupListing = new HashMap<Integer, Path>();

    if (!Files.isRegularFile(backupListingPath) || !Files.isReadable(backupListingPath)) {
      return false;
    }
    try {
      NBTTagCompound backupListingNBT = CompressedStreamTools.read(backupListingPath.toFile());
      if (!backupListingNBT.getName().equals(BACKUP_LISTING_NAME_VALUE) ) {
        ErrorLog.defaultLog().warning("Invalid backuplisting file (root name wrong): " + backupListingPath.toString());
        return false;
      }
      if (!backupListingNBT.hasKey(BACKUP_LISTING_VERSION_TAG) || !backupListingNBT.hasKey(BACKUP_LISTING_PATHS_TAG)) {
        ErrorLog.defaultLog().warning("Invalid backuplisting file (missing tag): " + backupListingPath.toString());
        return false;
      }
      if (!backupListingNBT.getString(BACKUP_LISTING_VERSION_TAG).equals(BACKUP_LISTING_VERSION_VALUE)) {
        ErrorLog.defaultLog().warning("Invalid backuplisting file (version wrong): "  + backupListingPath.toString());
        return false;
      }
      backupListingNBT = backupListingNBT.getCompoundTag(BACKUP_LISTING_PATHS_TAG);

      Iterator iterator = backupListingNBT.getTags().iterator();

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
            ErrorLog.defaultLog().warning("Invalid backupNumber tag in backuplisting file: "  + backupListingPath.toString());
            return false;
          }
          if (backupListing.containsKey(backupNumber)) {
            ErrorLog.defaultLog().warning("Duplicate backupNumber tag in backuplisting file: "  + backupListingPath.toString());
            return false;
          }
          backupListing.put(backupNumber, Paths.get(entry.data));
        } else {
          ErrorLog.defaultLog().warning("Invalid backupNumber entry in backuplisting file: "  + backupListingPath.toString());
          return false;
        }
      }
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Failure while reading backuplisting file ("  + backupListingPath.toString() + ") :" + ioe.toString());
      return false;
    }
    return true;
  }

  /**
   * writes the given backups to the given Path; overwrites automatically if already exists
   * @return true if successful, false otherwise.
   */
  public boolean saveBackupListing(Path fileToCreate)
  {
    try {
      NBTTagCompound backupListingNBT = new NBTTagCompound(BACKUP_LISTING_NAME_VALUE);
      backupListingNBT.setString(BACKUP_LISTING_VERSION_TAG, BACKUP_LISTING_VERSION_VALUE);

      NBTTagCompound paths = new NBTTagCompound("dummy");
      for (Map.Entry<Integer, Path> entry : backupListing.entrySet()) {
        paths.setString(entry.getKey().toString(), entry.getValue().toString());
      }
      backupListingNBT.setTag(BACKUP_LISTING_PATHS_TAG, paths);

      OutputStream out = null;
      try {
        CompressedStreamTools.write(backupListingNBT, fileToCreate.toFile());
      } catch (IOException e) {
        ErrorLog.defaultLog().severe("StoredBackups::saveBackupListing() failed to create backup save: %s", e.toString());
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
      ErrorLog.defaultLog().severe("StoredBackups::saveBackupListing() failed to create backup save: %s", e.toString());
      return false;
    }

    return true;
  }

  public HashMap<Integer, Path> getBackupListing() {
    return backupListing;
  }

  /**
   * Looks through the StoredBackups and culls any which are surplus
   * The deletion is performed to keep a series of backups with increasing spacing as they get older
   *     Up to six backups will be kept, the oldest will be at least 14 saves old (up to 21)
   * @return
   */
  public boolean cullSurplus() {
    // If the saves are numbered from 0, 1, 2, etc and savenumber is the number of the
    //   current save,
    //   then savenumber - deletionSchedule[savenumber%16] is to be deleted
    // This sequence leads to a fairly evenly increasing gap between saves, up to 6 saves deep
    int[] deletionSchedule = {1, 13, 1, 5, 1, 21, 1, 5, 1, 13, 1, 5, 1, 21, 1, 5};

    CHECK VALIDITY OF EXISTING
  }

  /**
   * provides a suitable folder name for the next save backup
   * @param basePath and baseStem the base path and filename stem eg /saves and GreysWorld
   * @return the folder name to be used for the next save backup, or null for failure
   */
  public Path getNextSaveFolder(Path basePath, String baseStem) {
    int nextBackupNumber = getNextBackupNumber();
    char backupLetter = 'a';
    boolean backupFolderExistsAlready;
    Path folderToTry;
    do {
      folderToTry = basePath.resolve((baseStem + "-bk" + nextBackupNumber) + backupLetter);
      backupFolderExistsAlready = Files.exists(folderToTry);
      ++backupLetter;
    } while (backupFolderExistsAlready && backupLetter <= 'z');
    if (backupFolderExistsAlready) {
      ErrorLog.defaultLog().warning("StoredBackups::getNextSaveFolder couldn't find a suitable name");
      return null;
    }
    return folderToTry;
  }

  /**
   * Adds the given path to the listing of stored backups
   * @param newBackup
   * @return true for success
   */
  public boolean addStoredBackup(Path newBackup) {
    backupListing.put(new Integer(getNextBackupNumber()), newBackup);
    return true;
  }

  private int getNextBackupNumber()
  {
    int nextBackupNumber = Integer.MIN_VALUE;

    for (Integer entry : backupListing.keySet()) {
      nextBackupNumber = Math.max(nextBackupNumber, entry+1);
    }
    if (nextBackupNumber == Integer.MIN_VALUE) nextBackupNumber = 1;
    return nextBackupNumber;
  }

  private HashMap<Integer, Path> backupListing;

}

