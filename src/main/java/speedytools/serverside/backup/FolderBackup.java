package speedytools.serverside.backup;

/*
* Contains sample code from Oracle:
*
* Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
*   - Redistributions of source code must retain the above copyright
*     notice, this list of conditions and the following disclaimer.
*
*   - Redistributions in binary form must reproduce the above copyright
*     notice, this list of conditions and the following disclaimer in the
*     documentation and/or other materials provided with the distribution.
*
*   - Neither the name of Oracle nor the names of its
*     contributors may be used to endorse or promote products derived
*     from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
* IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
* THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
* PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
* CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
* PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
* PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
* NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import speedytools.common.utilities.ErrorLog;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FolderBackup
{
  /**
   * Copies an entire save folder to a backup folder.
   * destinationSaveFolder is created (it should not exist already)
   * @param currentSaveFolder
   * @param destinationSaveFolder
   * @return true for success, false for failure
   */
  static public boolean createBackupSave(Path currentSaveFolder, Path destinationSaveFolder, String comment)
  {

    try {
//      if (!Files.exists(destinationSaveFolder)) {
//        Files.createDirectory(destinationSaveFolder);
//      }

      TreeCopier tc = new TreeCopier(currentSaveFolder, destinationSaveFolder);
      Files.walkFileTree(currentSaveFolder, tc);
      NBTTagCompound backupFolderContentsListing = new NBTTagCompound();
      backupFolderContentsListing.setString(COMMENT_TAG, comment);
      backupFolderContentsListing.setTag(CONTENTS_TAG, tc.getAllFileInfo());

      Path contentsFile = destinationSaveFolder.resolve(CONTENTS_FILENAME);

      CompressedStreamTools.write(backupFolderContentsListing, contentsFile.toFile());

    } catch (IOException e) {
      ErrorLog.defaultLog().severe("FolderBackup::createBackupSave() failed to create backup save: %s", e);
      return false;
    }
    return true;
  }

  /**
   * Checks the specified backup folder to see if the size and timestamps of all the files still match
   *   the fileinfo file CONTENTS_FILENAME
   * @param backupFolder
   * @return true if all files match, false if any don't
   */
  static public boolean isBackupSaveUnmodified(Path backupFolder)
  {
    NBTTagCompound nbt = readFileInfo(backupFolder);
    if (nbt == null) return false;

    try {
      TreeCopier tc = new TreeCopier(backupFolder, null);
      Files.walkFileTree(backupFolder, tc);
      if (nbt.equals(tc.getAllFileInfo()) ) return true;
    } catch (IOException e) {
      ErrorLog.defaultLog().severe("FolderBackup::isBackupSaveUnmodified() failed to read: %s", e);
      return false;
    }
    return false;
  }

  /**
   * deletes the entire backup folder, checking each file against the list of expected files before deleting it
   * @param backupSaveFolder
   * @return true if there were no unexpected files and all the expected files were present and deleted
   */
  static public boolean deleteBackupSave(Path backupSaveFolder)
  {
    try {
      NBTTagCompound nbtFileInfo = readFileInfo(backupSaveFolder);
      if (nbtFileInfo == null) return false;
      if (!isBackupSaveUnmodified(backupSaveFolder)) return false;

      TreeDeleter treeDeleter = new TreeDeleter(backupSaveFolder, nbtFileInfo);
      Files.walkFileTree(backupSaveFolder, treeDeleter);
      if (treeDeleter.lastWalkWasTerminated()) return false;
      if (!treeDeleter.getAllFileInfo().hasNoTags()) return false;
    } catch (IOException e) {
      ErrorLog.defaultLog().severe("FolderBackup::deleteBackupSave() failed to delete backup save: %s", e);
      return false;
    }

    return true;
  }

  /** read the file info listing from contents listing file in the specified folder
   *
   * @param backupFolder
   * @return the NBT with the list of file information, or null if error
   */
  static private NBTTagCompound readFileInfo(Path backupFolder)
  {
    Path contentsFile = backupFolder.resolve(CONTENTS_FILENAME);
    if (!Files.isRegularFile(contentsFile) || !Files.isReadable(contentsFile)) return null;
    NBTTagCompound nbt;
    try {
      nbt = CompressedStreamTools.read(contentsFile.toFile());
    } catch (IOException ioe) {
      ErrorLog.defaultLog().info("Failed to read contents file: " + contentsFile.toString());
      return null;
    }
    if (!nbt.hasKey(CONTENTS_TAG)) {                    // !nbt.getName().equals(ROOT_TAG) || !nbt.hasKey(CONTENTS_TAG)) {
      ErrorLog.defaultLog().info("tags missing from contents file: " + contentsFile.toString());
      return null;
    }
    try {
      nbt = nbt.getCompoundTag(CONTENTS_TAG);
    } catch (Exception e) {
      ErrorLog.defaultLog().info("invalid contents tag in contents file: " + contentsFile.toString());
      return null;
    }
    return nbt;
  }

  /**
   * Copy source file to destination location.
   */
  static private void copyFile(Path source, Path target) throws IOException {
    if (Files.notExists(target)) {
      Files.copy(source, target);
    }
  }

  private final static String ROOT_TAG = "BACKUP_FOLDER_CONTENTS";
  private final static String COMMENT_TAG = "PATH";
  private final static String CONTENTS_TAG = "LIST_OF_FILES";
  private final static String CONTENTS_FILENAME = "fileinfo.dat";

  static class TreeCopier implements FileVisitor<Path>
  {
    private final Path source;
    private final Path destination;   // destination folder, set to = source if copyFiles is false
    private boolean copyFiles;
    private boolean success;

    public NBTTagCompound getAllFileInfo() {
      return allFileInfo;
    }

    private NBTTagCompound allFileInfo;

    /**
     * create a FileVisitor to walk the folder tree, optionally copying each file & folder to a destination directory, and compiling a list
     *   of information about each file in the folder.
     * @param i_source the source folder
     * @param i_destination the destination folder, or if null - don't copy.
     */
    TreeCopier(Path i_source, Path i_destination) {
      this.source = i_source;
      if (i_destination == null) {
        copyFiles = false;
        this.destination = this.source;
      } else {
        copyFiles = true;
        this.destination = i_destination;
      }
      this.success = false;
      allFileInfo = new NBTTagCompound(); //new NBTTagCompound(CONTENTS_TAG);
    }

    private final String PATH_TAG = "PATH";
    private final String FILE_SIZE_TAG = "SIZE";
    private final String FILE_CREATED_TAG = "CREATED";
    private final String FILE_MODIFIED_TAG = "MODIFIED";

    private NBTTagCompound createFileInfoEntry(Path path) throws IOException
    {
      BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
//      fileRecord.setString(PATH_TAG, path.toString());
      NBTTagCompound nbt = new NBTTagCompound();
      nbt.setLong(FILE_SIZE_TAG, attributes.size());
      nbt.setLong(FILE_CREATED_TAG, attributes.lastModifiedTime().toMillis());
      nbt.setLong(FILE_MODIFIED_TAG, attributes.lastModifiedTime().toMillis());
      return nbt;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
    {
      // before visiting entries in a directory we copy the directory
      // (okay if directory already exists).

      if (!copyFiles) return FileVisitResult.CONTINUE;
      Path newdir = destination.resolve(source.relativize(dir));
//      System.out.println("creating new directory: " + newdir.toString());
      Files.copy(dir, newdir);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
      if (path.getFileName().equals(Paths.get(CONTENTS_FILENAME))) {  // ignore the contents file
        return FileVisitResult.CONTINUE;
      }
      if (copyFiles) {
        copyFile(path, destination.resolve(source.relativize(path)));
      }
//      System.out.println("copying file " + path.toString() + " to " + destination.resolve(source.relativize(path)));
      NBTTagCompound thisFileInfo = createFileInfoEntry(path);
      allFileInfo.setTag(source.relativize(path).toString(), thisFileInfo);    //.setCompoundTag(source.relativize(path).toString(), thisFileInfo);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
    {
      throw exc;
    }
  }

  static class TreeDeleter implements FileVisitor<Path>
  {
    /**
     * create a FileVisitor to walk the folder tree, optionally copying each file & folder to a destination directory, and compiling a list
     *   of information about each file in the folder.
     * @param expectedFileList = the Files that are expected to be in this folder
     */
    TreeDeleter(Path i_backupFolderRoot, NBTTagCompound expectedFileList) {
      allFileInfo = (NBTTagCompound)expectedFileList.copy();
      backupFolderRoot = i_backupFolderRoot;
      terminated = false;
    }

    public boolean lastWalkWasTerminated() {
      return terminated;
    }

    public NBTTagCompound getAllFileInfo() {
      return allFileInfo;
    }

    private final String FILE_SIZE_TAG = "SIZE";
    private final String FILE_CREATED_TAG = "CREATED";
    private final String FILE_MODIFIED_TAG = "MODIFIED";

    private boolean isFileExpected(Path path, NBTTagCompound fileRecord) throws IOException
    {
      BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
      if (   fileRecord.getLong(FILE_SIZE_TAG) != attributes.size()
          || fileRecord.getLong(FILE_CREATED_TAG) != attributes.lastModifiedTime().toMillis()
          || fileRecord.getLong(FILE_MODIFIED_TAG) != attributes.lastModifiedTime().toMillis() ) {
        return false;
      } else {
        return true;
      }

    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
    {
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
      boolean okToDelete = false;
      Path relativePath = backupFolderRoot.relativize(path);

      if (path.getFileName().equals(Paths.get(CONTENTS_FILENAME))) {  // delete the contents file
        okToDelete = true;
      } else {
        okToDelete = isFileExpected(path, allFileInfo.getCompoundTag(relativePath.toString()));
      }

      if (!okToDelete) {
        terminated = true;
        return FileVisitResult.TERMINATE;
      }

      Files.delete(path);
      allFileInfo.removeTag(relativePath.toString());
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      Files.delete(dir);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
    {
      throw exc;
    }
    private NBTTagCompound allFileInfo;
    private Path backupFolderRoot;
    private boolean terminated;
  }

}

