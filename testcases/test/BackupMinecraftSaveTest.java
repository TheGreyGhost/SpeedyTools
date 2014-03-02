package test;

import org.junit.*;
import speedytools.common.ErrorLog;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * User: The Grey Ghost
 * Date: 2/03/14
 */
public class BackupMinecraftSaveTest
{
  public final static String TEST_ERROR_LOG = "BackupMinecraftSaveTestErrorLog.log";
  public final static String PREMADE_FOLDER = "premade/BackupMinecraftSaveTest";
  public static final String TEST_TEMP_ROOT_DIRECTORY = "temp";

  @BeforeClass
  public void setUp() throws Exception {
    Path testdata = Paths.get(TEST_TEMP_ROOT_DIRECTORY).resolve(TEST_ERROR_LOG);
    ErrorLog.setLogFileAsDefault(testdata.toString());

    Path tempfolder = Paths.get(TEST_TEMP_ROOT_DIRECTORY);
    tempDirPath = Files.createTempDirectory(tempfolder, "StoredBackupsTest");
    tempDirFile = tempDirPath.toFile();
  }

  @AfterClass
  public void tearDown() throws Exception {
    File[] tempFiles = tempDirFile.listFiles();

    for (File file : tempFiles) {
      file.delete();
    }
    tempDirFile.delete();

  }

  private static Path tempDirPath;
  private static File tempDirFile;


  @Test
  public void testCreateBackupSave() throws Exception {

  }

  @Test
  public void testIsBackupSaveUnmodified() throws Exception {

  }

  @Test
  public void testDeleteBackupSave() throws Exception {

  }
}
