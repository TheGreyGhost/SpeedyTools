package test.backups;

import org.junit.*;
import speedytools.common.utilitiesOld.ErrorLog;
import speedytools.serverside.backup.FolderBackup;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * User: The Grey Ghost
 * Date: 2/03/14
 */
public class FolderBackupTest
{
  public final static String TEST_ERROR_LOG = "BackupMinecraftSaveTestErrorLog.log";
  public final static String PREMADE_FOLDER = "premade/BackupMinecraftSaveTest";
  public final static String PREMADE_TEST_1 = "premade/BackupMinecraftSaveTest/TestFolderRoot";
  public static final String TEST_TEMP_ROOT_DIRECTORY = "temp";

  @BeforeClass
  public static void setUp() throws Exception {
    Path testdata = Paths.get(TEST_TEMP_ROOT_DIRECTORY).resolve(TEST_ERROR_LOG);
    ErrorLog.setLogFileAsDefault(testdata.toString());

    Path tempfolder = Paths.get(TEST_TEMP_ROOT_DIRECTORY);
    tempDirPath = Files.createTempDirectory(tempfolder, "BMSTest");
    tempDirFile = tempDirPath.toFile();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    TreeDeleter treeDeleter = new TreeDeleter();
    Files.walkFileTree(tempDirPath, treeDeleter);

  }

  private static Path tempDirPath;
  private static File tempDirFile;

  @Test
  public void testCreateBackupSave() throws Exception {
    boolean success;

    // -- test for successful creation, check, and deletion
    Path testFolderRootBk = tempDirPath.resolve("TestFolderRoot-bk");
    success = FolderBackup.createBackupSave(Paths.get(PREMADE_TEST_1), testFolderRootBk, "Test");
    Assert.assertTrue("createBackupSave succeeds", success);
    Assert.assertTrue(Files.exists(testFolderRootBk));

    success = FolderBackup.isBackupSaveUnmodified(testFolderRootBk);
    Assert.assertTrue("isBackupSaveUnmodified is true", success);

    success = FolderBackup.deleteBackupSave(testFolderRootBk);
    Assert.assertTrue("deleteBackupSave is true", success);
    Assert.assertFalse("TestFolderRoot-bk no longer exists", Files.exists(testFolderRootBk));

    //---test for detect extra file

    testFolderRootBk = tempDirPath.resolve("TestFolderRoot-bk2");
    success = FolderBackup.createBackupSave(Paths.get(PREMADE_TEST_1), testFolderRootBk, "Test");
    Assert.assertTrue("createBackupSave succeeds", success);
    Assert.assertTrue(Files.exists(testFolderRootBk));

    Files.copy(testFolderRootBk.resolve("TestDocument1.txt"), testFolderRootBk.resolve("TestDocument1-copy.txt"));
    success = FolderBackup.isBackupSaveUnmodified(testFolderRootBk);
    Assert.assertFalse("isBackupSaveUnmodified is false", success);

    success = FolderBackup.deleteBackupSave(testFolderRootBk);
    Assert.assertFalse("deleteBackupSave is false", success);

    // ---test for detect file deleted

    testFolderRootBk = tempDirPath.resolve("TestFolderRoot-bk3");
    success = FolderBackup.createBackupSave(Paths.get(PREMADE_TEST_1), testFolderRootBk, "Test");
    Assert.assertTrue("createBackupSave succeeds", success);
    Assert.assertTrue(Files.exists(testFolderRootBk));

    Files.delete(testFolderRootBk.resolve("TestDocument1.txt"));
    success = FolderBackup.isBackupSaveUnmodified(testFolderRootBk);
    Assert.assertFalse("isBackupSaveUnmodified is false", success);

    success = FolderBackup.deleteBackupSave(testFolderRootBk);
    Assert.assertFalse("deleteBackupSave is false", success);

    // ---test for file size change, in subfolder

    testFolderRootBk = tempDirPath.resolve("TestFolderRoot-bk4");
    success = FolderBackup.createBackupSave(Paths.get(PREMADE_TEST_1), testFolderRootBk, "Test");
    Assert.assertTrue("createBackupSave succeeds", success);
    Assert.assertTrue(Files.exists(testFolderRootBk));

    Path tweakPath = testFolderRootBk.resolve("TestFolder1/TestDocument3b.txt");

    FileTime lmtime = Files.getLastModifiedTime(tweakPath);
    Object ctime = Files.getAttribute(tweakPath, "creationTime");
    FileWriter fw = new FileWriter(tweakPath.toFile(), true); //the true will append the new data
    fw.write("add a line\n");//appends the string to the file
    fw.close();
    Files.setLastModifiedTime(tweakPath, lmtime);
    Files.setAttribute(tweakPath, "creationTime", ctime);

    success = FolderBackup.isBackupSaveUnmodified(testFolderRootBk);
    Assert.assertFalse("isBackupSaveUnmodified is false", success);

    success = FolderBackup.deleteBackupSave(testFolderRootBk);
    Assert.assertFalse("deleteBackupSave is false", success);

    // ---test for file modification time change, in subfolder

    testFolderRootBk = tempDirPath.resolve("TestFolderRoot-bk5");
    success = FolderBackup.createBackupSave(Paths.get(PREMADE_TEST_1), testFolderRootBk, "Test");
    Assert.assertTrue("createBackupSave succeeds", success);
    Assert.assertTrue(Files.exists(testFolderRootBk));

    tweakPath = testFolderRootBk.resolve("TestFolder1/TestDocument3b.txt");
    Files.setLastModifiedTime(tweakPath, FileTime.fromMillis(3513613));

    success = FolderBackup.isBackupSaveUnmodified(testFolderRootBk);
    Assert.assertFalse("isBackupSaveUnmodified is false", success);

    success = FolderBackup.deleteBackupSave(testFolderRootBk);
    Assert.assertFalse("deleteBackupSave is false", success);


  }

  static class TreeDeleter implements FileVisitor<Path>
  {

    /**
     * create a FileVisitor to walk the folder tree, deleting all its contents
     */
    TreeDeleter() {
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
    {
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
      Files.delete(path);
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
  }



}
