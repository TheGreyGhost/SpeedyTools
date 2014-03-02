package speedytools.serveronly;

/*
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

import cpw.mods.fml.common.FMLLog;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import speedytools.common.ErrorLog;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class BackupMinecraftSave
{
  /**
   * Copies a minecraft save folder to a backup folder
   * @param currentSaveFolder
   * @param destinationSaveFolder
   * @return true for success, false for failure
   */
  static public boolean createBackupSave(Path currentSaveFolder, Path destinationSaveFolder, String comment)
  {

    try {
      TreeCopier tc = new TreeCopier(currentSaveFolder, destinationSaveFolder);
      Files.walkFileTree(currentSaveFolder, tc);
      NBTTagCompound backupFolderContentsListing = new NBTTagCompound(ROOT_TAG);
      backupFolderContentsListing.setString(COMMENT_TAG, comment);
      backupFolderContentsListing.setTag(CONTENTS_TAG, tc.getAllFileInfo());

      Path contentsFile = destinationSaveFolder.resolve(CONTENTS_FILENAME);

      CompressedStreamTools.write(backupFolderContentsListing, contentsFile.toFile());

    } catch (IOException e) {
      FMLLog.severe("BackupMinecraftSave::createBackupSave() failed to create backup save: %s", e);
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
      ErrorLog.defaultLog().severe("BackupMinecraftSave::isBackupSaveUnmodified() failed to read: %s", e);
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

      TreeDeleter treeDeleter = new TreeDeleter(nbtFileInfo);
      Files.walkFileTree(backupSaveFolder, treeDeleter);
      if (!treeDeleter.getAllFileInfo().hasNoTags()) return false;
      Files.delete(backupSaveFolder);
    } catch (IOException e) {
      ErrorLog.defaultLog().severe("BackupMinecraftSave::deleteBackupSave() failed to delete backup save: %s", e);
      return false;
    }

    return true;
  }

  /** read the file info listing from the provided file
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
      ErrorLog.defaultLog().warning("Failed to read contents file: " + contentsFile.toString());
      return null;
    }
    if (nbt.getName() != ROOT_TAG || !nbt.hasKey(CONTENTS_TAG)) {
      ErrorLog.defaultLog().warning("tags missing from contents file: " + contentsFile.toString());
      return null;
    }
    try {
      nbt = nbt.getCompoundTag(CONTENTS_TAG);
    } catch (Exception e) {
      ErrorLog.defaultLog().warning("invalid contents tag in contents file: " + contentsFile.toString());
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
      allFileInfo = new NBTTagCompound();
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
      if (path.getFileName().equals(CONTENTS_FILENAME)) {  // ignore the contents file
        return FileVisitResult.CONTINUE;
      }
      if (copyFiles) {
        copyFile(path, destination.resolve(source.relativize(path)));
      }
//      System.out.println("copying file " + path.toString() + " to " + destination.resolve(source.relativize(path)));
      NBTTagCompound thisFileInfo = createFileInfoEntry(path);
      allFileInfo.setCompoundTag(destination.toString(), thisFileInfo);
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
    public NBTTagCompound getAllFileInfo() {
      return allFileInfo;
    }

    /**
     * create a FileVisitor to walk the folder tree, optionally copying each file & folder to a destination directory, and compiling a list
     *   of information about each file in the folder.
     * @param expectedFileList = the Files that are expected to be in this folder
     */
    TreeDeleter(NBTTagCompound expectedFileList) {
      allFileInfo = (NBTTagCompound)expectedFileList.copy();
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

      if (path.getFileName().equals(CONTENTS_FILENAME)) {  // delete the contents file
        okToDelete = true;
      } else {
        okToDelete = isFileExpected(path, allFileInfo.getCompoundTag(path.toString()));
      }

      if (!okToDelete) return FileVisitResult.TERMINATE;

      Files.delete(path);
      allFileInfo.removeTag(path.toString());
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
  }

}

/*
    private void junk()
    {
      try
      {
        par1NBTTagCompound.setTag("Pos", this.newDoubleNBTList(new double[] {this.posX, this.posY + (double)this.ySize, this.posZ}));
        par1NBTTagCompound.setTag("Motion", this.newDoubleNBTList(new double[] {this.motionX, this.motionY, this.motionZ}));
        par1NBTTagCompound.setTag("Rotation", this.newFloatNBTList(new float[] {this.rotationYaw, this.rotationPitch}));
        par1NBTTagCompound.setFloat("FallDistance", this.fallDistance);
        par1NBTTagCompound.setShort("Fire", (short)this.fire);
        par1NBTTagCompound.setShort("Air", (short)this.getAir());
        par1NBTTagCompound.setBoolean("OnGround", this.onGround);
        par1NBTTagCompound.setInteger("Dimension", this.dimension);
        par1NBTTagCompound.setBoolean("Invulnerable", this.invulnerable);
        par1NBTTagCompound.setInteger("PortalCooldown", this.timeUntilPortal);
        par1NBTTagCompound.setLong("UUIDMost", this.entityUniqueID.getMostSignificantBits());
        par1NBTTagCompound.setLong("UUIDLeast", this.entityUniqueID.getLeastSignificantBits());
        if (customEntityData != null)
        {
          par1NBTTagCompound.setCompoundTag("ForgeData", customEntityData);
        }

        for (String identifier : this.extendedProperties.keySet()){
          try{
            IExtendedEntityProperties props = this.extendedProperties.get(identifier);
            props.saveNBTData(par1NBTTagCompound);
          }catch (Throwable t){
            FMLLog.severe("Failed to save extended properties for %s.  This is a mod issue.", identifier);
            t.printStackTrace();
          }
        }

        this.writeEntityToNBT(par1NBTTagCompound);

        if (this.ridingEntity != null)
        {
          NBTTagCompound nbttagcompound1 = new NBTTagCompound("Riding");

          if (this.ridingEntity.writeMountToNBT(nbttagcompound1))
          {
            par1NBTTagCompound.setTag("Riding", nbttagcompound1);
          }
        }
      }
      catch (Throwable throwable)
      {
        CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Saving entity NBT");
        CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being saved");
        this.addEntityCrashInfo(crashreportcategory);
        throw new ReportedException(crashreport);
      }

      try
      {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        par1EntityPlayer.writeToNBT(nbttagcompound);
        File file1 = new File(this.playersDirectory, par1EntityPlayer.getCommandSenderName() + ".dat.tmp");
        File file2 = new File(this.playersDirectory, par1EntityPlayer.getCommandSenderName() + ".dat");
        CompressedStreamTools.writeCompressed(nbttagcompound, new FileOutputStream(file1));

        if (file2.exists())
        {
          file2.delete();
        }

        file1.renameTo(file2);
      }
      catch (Exception exception)
      {
        MinecraftServer.getServer().getLogAgent().logWarning("Failed to save player data for " + par1EntityPlayer.getCommandSenderName());
      }
      DataOutputStream dataoutputstream = RegionFileCache.getChunkOutputStream(this.chunkSaveLocation, par1AnvilChunkLoaderPending.chunkCoordinate.chunkXPos, par1AnvilChunkLoaderPending.chunkCoordinate.chunkZPos);
      CompressedStreamTools.write(par1AnvilChunkLoaderPending.nbtTags, dataoutputstream);
      dataoutputstream.close();

    }


    public NBTTagCompound getPlayerData(String par1Str)
    {
      try
      {
        File file1 = new File(this.playersDirectory, par1Str + ".dat");

        if (file1.exists())
        {
          return CompressedStreamTools.readCompressed(new FileInputStream(file1));
        }
      }
      catch (Exception exception)
      {
        MinecraftServer.getServer().getLogAgent().logWarning("Failed to load player data for " + par1Str);
      }

      return null;
    }
  */

/*
  public static void main(String[] args) throws IOException {

    // remaining arguments are the source files(s) and the destination location
    Path destination = Paths.get(args[argi]);

    // check if destination is a directory
    boolean isDir = Files.isDirectory(destination);

    // copy each source file/directory to destination
    for (i=0; i<source.length; i++) {
      Path dest = (isDir) ? destination.resolve(source[i].getFileName()) : destination;

      if (recursive) {
        // follow links when copying files
        EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        TreeCopier tc = new TreeCopier(source[i], dest, prompt, preserve);
        Files.walkFileTree(source[i], opts, Integer.MAX_VALUE, tc);
      } else {
        // not recursive so source must not be a directory
        if (Files.isDirectory(source[i])) {
          System.err.format("%s: is a directory%n", source[i]);
          continue;
        }
        copyFile(source[i], dest, prompt, preserve);
      }
    }
  }
}
*/