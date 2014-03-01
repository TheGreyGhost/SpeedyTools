package test;

import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import speedytools.common.ErrorLog;
import speedytools.serveronly.WorldBackup;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * User: The Grey Ghost
 * Date: 1/03/14
 */
public class WorldBackupTest
{
  public final static String TEST_ERROR_LOG = "WorldBackupTestErrorLog.log";
  public final static String PREMADE_FOLDER = "premade/WorldBackupTest";

  @Test
  public void testRetrieveBackupListing() throws Exception {
    Path testdata = tempDirPath;

    HashMap<Integer, Path> testInput = new HashMap<Integer, Path>();

    testInput.put(new Integer(5), testdata.resolve("testpath5"));
    testInput.put(new Integer(8), testdata.resolve("testpath8"));
    testInput.put(new Integer(10), testdata.resolve("testpath10"));
    testInput.put(new Integer(20), testdata.resolve("testpath20"));

//    WorldBackup wb = new WorldBackup(testdata.resolve("test2"));

    boolean success;
    HashMap<Integer, Path> testResult;
    Path test1file = testdata.resolve("backuplisting1.dat");
    success = WorldBackup.saveBackupListing(test1file, testInput);
    testResult = WorldBackup.retrieveBackupListing(test1file);
    Assert.assertTrue("saveBackupListing succeeded", success);
    Assert.assertTrue("Test1: retrieved listing exactly matches saved listing", testResult.equals(testInput));

    testInput.put(new Integer(30), testdata.resolve("testpath30"));
    success = WorldBackup.saveBackupListing(test1file, testInput);
    testResult = WorldBackup.retrieveBackupListing(test1file);
    Assert.assertTrue("saveBackupListing over the top succeeded", success);
    Assert.assertTrue("Test2: retrieved listing exactly matches saved listing", testResult.equals(testInput));

    Path premade = Paths.get(PREMADE_FOLDER);

    String[] testFiles = {"badRoot.dat", "duplicateBackupNumber.dat", "invalidBackupNumber.dat", "missingVersion.dat",
                          "wrongPathType.dat", "wrongVersion.dat"};

    for (String testfile : testFiles) {
      testResult = WorldBackup.retrieveBackupListing(premade.resolve(testfile));
      Assert.assertNull("testfile " + testfile + "expected to fail", testResult);
    }
  }

  @Test
  public void testSaveBackupListing() throws Exception {

    final String ILLEGAL_FILENAME = "fail/g.dat";

    HashMap<Integer, Path> testInput = new HashMap<Integer, Path>();
    boolean success;
    success = WorldBackup.saveBackupListing(tempDirPath.resolve(ILLEGAL_FILENAME), testInput);
    Assert.assertFalse("Illegal filename causes failed save", success);
  }

  public static final String TEST_TEMP_ROOT_DIRECTORY = "temp";

  @BeforeClass
  public static void setUp() throws Exception {
    Path testdata = Paths.get(TEST_TEMP_ROOT_DIRECTORY).resolve(TEST_ERROR_LOG);
    ErrorLog.setLogFileAsDefault(testdata.toString());

    Path tempfolder = Paths.get(TEST_TEMP_ROOT_DIRECTORY);
    tempDirPath = Files.createTempDirectory(tempfolder, "WorldBackupTest");
    tempDirFile = tempDirPath.toFile();
  } 
  @AfterClass
  public static void tearDown() throws Exception {
    File[] tempFiles = tempDirFile.listFiles();

    for (File file : tempFiles) {
      file.delete();
    }
    tempDirFile.delete();
  }

  private static Path tempDirPath;
  private static File tempDirFile;
}
