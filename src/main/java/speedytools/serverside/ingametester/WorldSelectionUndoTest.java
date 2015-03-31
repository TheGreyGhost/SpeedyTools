package speedytools.serverside.ingametester;

import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.storage.WorldInfo;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import speedytools.common.selections.VoxelSelection;
import speedytools.serverside.worldmanipulation.WorldFragment;
import speedytools.serverside.worldmanipulation.WorldHistory;
import speedytools.serverside.worldmanipulation.WorldSelectionUndo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
* User: The Grey Ghost
* Date: 13/06/2014 WorldSelectionUndo, and WorldFragment
*
*
* Test plan:
*  Test of WorldSelectionUndo: Tests all permutations of makePermanent() and undoChanges() for five blocks:
//  1) place A, B, C, D, E (the voxels in A, B, C, D, E have a binary pattern 0..31 so that every combination of overlapping A, B, C, D, E voxels is present)
//  2) remove each one, one step at a time, by either makePermanent() or undoChanges(), and check that the result at each step matches a
//     straight-forward placement.  eg
//     after makePermanent(C) and undoChanges(B), the result matches the world after placing A, D, then E.
//     performs every permutation:
//     a) order of action removal = 5! = 120
//     b) removal method = 2^5 = 32
//     So a total of 120 * 32 = 40,000 or so.
*   Test of WorldHistory: see method comments
*/
public class WorldSelectionUndoTest
{
  public void runWorldHistoryTests() {
    final int TEST_WORLD_X_SIZE = 64;
    final int TEST_WORLD_Z_SIZE = 32;
    WorldServerTest worldServer1 = WorldServerTest.createDummyInstance(TEST_WORLD_X_SIZE, TEST_WORLD_Z_SIZE);
    WorldServerTest worldServer2 = WorldServerTest.createDummyInstance(TEST_WORLD_X_SIZE, TEST_WORLD_Z_SIZE);
    EntityPlayerMPTest entityPlayerMPTest1 = EntityPlayerMPTest.createDummyInstance();
    EntityPlayerMPTest entityPlayerMPTest2 = EntityPlayerMPTest.createDummyInstance();

    final int ACTION_COUNT = 5;
    ArrayList<InGameTester.TestRegions> testRegions = new ArrayList<InGameTester.TestRegions>();

    final int XORIGIN = 0;
    final int YORIGIN = 0;
    final int ZORIGIN = 0;
    final int XSIZE = 8;
    final int YSIZE = 1;
    final int ZSIZE = 4;
    for (int i = 0; i < ACTION_COUNT; ++i) {
      testRegions.add(new InGameTester.TestRegions(XORIGIN, YORIGIN, ZORIGIN + i * ZSIZE, XSIZE, YSIZE, ZSIZE, true));
    }
    InGameTester.TestRegions allRegions = new InGameTester.TestRegions(XORIGIN, YORIGIN, ZORIGIN + ACTION_COUNT * ZSIZE, XSIZE, YSIZE, ZSIZE, true);

    WorldFragment worldFragmentBlank = new WorldFragment(allRegions.xSize, allRegions.ySize, allRegions.zSize);
    worldFragmentBlank.readFromWorld(worldServer1, allRegions.testRegionInitialiser.getX(), allRegions.testRegionInitialiser.getY(), allRegions.testRegionInitialiser.getZ(), null);
    ArrayList<WorldFragment> sourceFragments = new ArrayList<WorldFragment>();
    for (int i = 0; i < ACTION_COUNT; ++i) {
      InGameTester.TestRegions testRegion = testRegions.get(i);
      WorldFragment worldFragment = new WorldFragment(testRegion.xSize, testRegion.ySize, testRegion.zSize);
      for (int j = 0; j < (2 << ACTION_COUNT); ++j) {                                                          // make binary pattern for all combinations of actions
        int wx = testRegion.sourceRegion.getX() + j / testRegion.zSize;
        int wy = testRegion.sourceRegion.getY();
        int wz = testRegion.sourceRegion.getZ() + j % testRegion.zSize;

        Chunk chunk = worldServer1.getChunkFromChunkCoords(wx >> 4, wz >> 4);
        boolean successful = WorldFragment.setBlockIDWithMetadata(chunk, wx, wy, wz, (0 == (j & (1 << i))) ? 0 : Block.getIdFromBlock(Blocks.wool), i+1);
      }
      VoxelSelection voxelSelection = InGameTester.selectAllNonAir(worldServer1, testRegion.sourceRegion, testRegion.xSize, testRegion.ySize, testRegion.zSize);
      worldFragment.readFromWorld(worldServer1, testRegion.sourceRegion.getX(), testRegion.sourceRegion.getY(), testRegion.sourceRegion.getZ(), voxelSelection);
      sourceFragments.add(worldFragment);
    }

    // first test:
//    1) Do four writeToWorldWithUndo for the player, check that performUndo does them in correct order with the correct undolist.
//            Check that the last performUndo does nothing (returns false)
    final int MAX_HISTORY_DEPTH_TEST_1 = 8;
    final int MAX_HISTORY_PER_PLAYER = 100;
    WorldHistory worldHistory = new WorldHistory(MAX_HISTORY_DEPTH_TEST_1, MAX_HISTORY_PER_PLAYER);
    ArrayList<WorldSelectionUndo> worldSelectionUndos = new ArrayList<WorldSelectionUndo>();
    for (int i = 0; i < ACTION_COUNT; ++i) {
      worldHistory.writeToWorldWithUndo(entityPlayerMPTest1, worldServer1, sourceFragments.get(i), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
      worldSelectionUndos.add(new WorldSelectionUndo());
      worldSelectionUndos.get(i).writeToWorld(worldServer2, sourceFragments.get(i), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
//      printTestRegionSlice("Assemble" + i, worldServer1, worldServer2, allRegions, 0);
    }

    for (int i = ACTION_COUNT - 1; i >= 0; --i) {
      boolean retval = worldHistory.performComplexUndo(entityPlayerMPTest1, worldServer1);
      assert (retval);
      List<WorldSelectionUndo> empty = new LinkedList<WorldSelectionUndo>();
      worldSelectionUndos.get(i).undoChanges(worldServer2, empty);
//      printTestRegionSlice("Disassemble" + i, worldServer1, worldServer2, allRegions, 0);
      retval = compareTestWorldServers(worldServer1, worldServer2, allRegions, true);
      assert (retval);
    }
    boolean retval = worldHistory.performComplexUndo(entityPlayerMPTest1, worldServer1);
    assert (!retval);

    // second test:
    // 2) Intersperse two different WorldServers and two different players.  Verify that the performUndo calls in correct order
    worldFragmentBlank.writeToWorld(worldServer1, allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ(), null);
    worldFragmentBlank.writeToWorld(worldServer2, allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ(), null);

    worldHistory.writeToWorldWithUndo(entityPlayerMPTest1, worldServer1, sourceFragments.get(0), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldHistory.writeToWorldWithUndo(entityPlayerMPTest1, worldServer2, sourceFragments.get(1), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldHistory.writeToWorldWithUndo(entityPlayerMPTest2, worldServer1, sourceFragments.get(2), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldHistory.writeToWorldWithUndo(entityPlayerMPTest2, worldServer2, sourceFragments.get(3), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());

    WorldServerTest worldServer1b = WorldServerTest.createDummyInstance(TEST_WORLD_X_SIZE, TEST_WORLD_Z_SIZE);
    WorldServerTest worldServer2b = WorldServerTest.createDummyInstance(TEST_WORLD_X_SIZE, TEST_WORLD_Z_SIZE);
    worldSelectionUndos.get(0).writeToWorld(worldServer1b, sourceFragments.get(0), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldSelectionUndos.get(1).writeToWorld(worldServer2b, sourceFragments.get(1), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldSelectionUndos.get(2).writeToWorld(worldServer1b, sourceFragments.get(2), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldSelectionUndos.get(3).writeToWorld(worldServer2b, sourceFragments.get(3), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    assert compareTestWorldServers(worldServer1, worldServer1b, allRegions, true) && compareTestWorldServers(worldServer2, worldServer2b, allRegions, true);

    List<WorldSelectionUndo> laterLayers = new LinkedList<WorldSelectionUndo>();
    worldHistory.performComplexUndo(entityPlayerMPTest1, worldServer2);
    laterLayers.add(worldSelectionUndos.get(3));
    worldSelectionUndos.get(1).undoChanges(worldServer2b, laterLayers);
    assert compareTestWorldServers(worldServer1, worldServer1b, allRegions, true) && compareTestWorldServers(worldServer2, worldServer2b, allRegions, true);

    worldHistory.performComplexUndo(entityPlayerMPTest2, worldServer1);
    laterLayers.clear(); // laterLayers.add(worldSelectionUndos.get(3));
    worldSelectionUndos.get(2).undoChanges(worldServer1b, laterLayers);
    assert compareTestWorldServers(worldServer1, worldServer1b, allRegions, true) && compareTestWorldServers(worldServer2, worldServer2b, allRegions, true);

    worldHistory.performComplexUndo(entityPlayerMPTest1, worldServer1);
    laterLayers.clear(); // laterLayers.add(worldSelectionUndos.get(3));
    worldSelectionUndos.get(0).undoChanges(worldServer1b, laterLayers);
    assert compareTestWorldServers(worldServer1, worldServer1b, allRegions, true) && compareTestWorldServers(worldServer2, worldServer2b, allRegions, true);

    worldHistory.performComplexUndo(entityPlayerMPTest2, worldServer2);
    laterLayers.clear(); // laterLayers.add(worldSelectionUndos.get(3));
    worldSelectionUndos.get(3).undoChanges(worldServer2b, laterLayers);
    assert compareTestWorldServers(worldServer1, worldServer1b, allRegions, true) && compareTestWorldServers(worldServer2, worldServer2b, allRegions, true);

    // third test: 3) Delete one of the WorldServers and verify that the performUndo doesn't crash, correctly undoes the remaining change, and further undoes return false
    worldFragmentBlank.writeToWorld(worldServer1, allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ(), null);
    worldFragmentBlank.writeToWorld(worldServer1b, allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ(), null);

    WorldServerTest worldServer3 = WorldServerTest.createDummyInstance(TEST_WORLD_X_SIZE, TEST_WORLD_Z_SIZE);
    WorldServerTest worldServer4 = WorldServerTest.createDummyInstance(TEST_WORLD_X_SIZE, TEST_WORLD_Z_SIZE);
    worldHistory.writeToWorldWithUndo(entityPlayerMPTest1, worldServer3, sourceFragments.get(0), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldHistory.writeToWorldWithUndo(entityPlayerMPTest1, worldServer1, sourceFragments.get(1), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldHistory.writeToWorldWithUndo(entityPlayerMPTest1, worldServer4, sourceFragments.get(2), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldSelectionUndos.get(0).writeToWorld(worldServer1b, sourceFragments.get(1), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    assert compareTestWorldServers(worldServer1, worldServer1b, allRegions, true);

    worldServer3 = null;
    worldServer4 = null;
    assert worldHistory.performComplexUndo(entityPlayerMPTest1, worldServer1);
    List<WorldSelectionUndo> empty = new LinkedList<WorldSelectionUndo>();
    worldSelectionUndos.get(0).undoChanges(worldServer1b, empty);
    assert compareTestWorldServers(worldServer1, worldServer1b, allRegions, true);
    assert !worldHistory.performComplexUndo(entityPlayerMPTest1, worldServer1);

    // 4th test: 4) Delete one of the EntityPlayerMP and verify that it doesn't affect the other player's undoes
    worldFragmentBlank.writeToWorld(worldServer1, allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ(), null);
    worldFragmentBlank.writeToWorld(worldServer1b, allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ(), null);

    worldHistory.writeToWorldWithUndo(entityPlayerMPTest2, worldServer1, sourceFragments.get(0), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldHistory.writeToWorldWithUndo(entityPlayerMPTest1, worldServer1, sourceFragments.get(1), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldHistory.writeToWorldWithUndo(entityPlayerMPTest1, worldServer1, sourceFragments.get(2), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldHistory.writeToWorldWithUndo(entityPlayerMPTest2, worldServer1, sourceFragments.get(3), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldHistory.writeToWorldWithUndo(entityPlayerMPTest1, worldServer1, sourceFragments.get(4), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());

    worldSelectionUndos.get(0).writeToWorld(worldServer1b, sourceFragments.get(0), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldSelectionUndos.get(1).writeToWorld(worldServer1b, sourceFragments.get(1), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldSelectionUndos.get(2).writeToWorld(worldServer1b, sourceFragments.get(2), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldSelectionUndos.get(3).writeToWorld(worldServer1b, sourceFragments.get(3), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldSelectionUndos.get(4).writeToWorld(worldServer1b, sourceFragments.get(4), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    assert compareTestWorldServers(worldServer1b, worldServer1, allRegions, true);
//    printTestRegionSlice("init", worldServer1b, worldServer1, allRegions, 0);
    entityPlayerMPTest2 = null;

    assert worldHistory.performComplexUndo(entityPlayerMPTest1, worldServer1);
//    printTestRegionSlice("WorldHistory.performUndo", worldServer1b, worldServer1, allRegions, 0);
    worldSelectionUndos.get(4).undoChanges(worldServer1b, empty);
//    printTestRegionSlice("worldSelectionUndos.get(4).undoChanges",worldServer1b, worldServer1, allRegions, 0);
    assert compareTestWorldServers(worldServer1b, worldServer1, allRegions, true);

    assert worldHistory.performComplexUndo(entityPlayerMPTest1, worldServer1);
//    printTestRegionSlice("WorldHistory.performUndo", worldServer1b, worldServer1, allRegions, 0);
    laterLayers.clear(); laterLayers.add(worldSelectionUndos.get(3));
    worldSelectionUndos.get(2).undoChanges(worldServer1b, laterLayers);
//    printTestRegionSlice("worldSelectionUndos.get(2).undoChanges", worldServer1b, worldServer1, allRegions, 0);
    assert compareTestWorldServers(worldServer1b, worldServer1, allRegions, true);

    assert worldHistory.performComplexUndo(entityPlayerMPTest1, worldServer1);
    worldSelectionUndos.get(1).undoChanges(worldServer1b, laterLayers);
    assert compareTestWorldServers(worldServer1b, worldServer1, allRegions, true);

    assert !worldHistory.performComplexUndo(entityPlayerMPTest1, worldServer1);

    // 5th test:
    // 5) Initialise with a small history depth, add multiple from different players, and verify that they are deleted oldest first, leaving each player with at least one.
    //    add a WorldServer and delete it and verify that it doesn't occupy space.
    worldFragmentBlank.writeToWorld(worldServer1, allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ(), null);
    worldFragmentBlank.writeToWorld(worldServer1b, allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ(), null);
    entityPlayerMPTest2 = EntityPlayerMPTest.createDummyInstance();
    EntityPlayerMPTest entityPlayerMPTest3 = EntityPlayerMPTest.createDummyInstance();

    worldHistory = new WorldHistory(3, MAX_HISTORY_PER_PLAYER);
    worldServer2 = WorldServerTest.createDummyInstance(TEST_WORLD_X_SIZE, TEST_WORLD_Z_SIZE);
    worldHistory.writeToWorldWithUndo(entityPlayerMPTest1, worldServer1, sourceFragments.get(0), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
//    printTestRegionSlice("writeToWorldWithUndo:1", worldServer1b, worldServer1, allRegions, 0);
//    worldHistory.printUndoStackYSlice(worldServer1, allRegions.testOutputRegion, allRegions.xSize, 0, allRegions.zSize);

    worldHistory.writeToWorldWithUndo(entityPlayerMPTest2, worldServer1, sourceFragments.get(1), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldHistory.writeToWorldWithUndo(entityPlayerMPTest1, worldServer2, sourceFragments.get(1), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldHistory.removeWorldServer(worldServer2);  worldServer2 = null;
//    printTestRegionSlice("writeToWorldWithUndo:2", worldServer1b, worldServer1, allRegions, 0);
//    worldHistory.printUndoStackYSlice(worldServer1, allRegions.testOutputRegion, allRegions.xSize, 0, allRegions.zSize);

    // test that 0 is not pushed because server2 was removed
    worldHistory.writeToWorldWithUndo(entityPlayerMPTest2, worldServer1, sourceFragments.get(2), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
//    printTestRegionSlice("writeToWorldWithUndo:3", worldServer1b, worldServer1, allRegions, 0);
//    worldHistory.printUndoStackYSlice(worldServer1, allRegions.testOutputRegion, allRegions.xSize, 0, allRegions.zSize);

    // test that oldest player 2 is pushed not 1
    worldHistory.writeToWorldWithUndo(entityPlayerMPTest3, worldServer1, sourceFragments.get(3), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    worldHistory.removePlayer(entityPlayerMPTest3);  entityPlayerMPTest3 = null;
//    printTestRegionSlice("writeToWorldWithUndo:4", worldServer1b, worldServer1, allRegions, 0);
//    worldHistory.printUndoStackYSlice(worldServer1, allRegions.testOutputRegion, allRegions.xSize, 0, allRegions.zSize);

    // test that 0 is not pushed because player3 was removed
    worldHistory.writeToWorldWithUndo(entityPlayerMPTest2, worldServer1, sourceFragments.get(4), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
    // in summary: should see 0U, 1P, 2U, 3P, 4U

//    printTestRegionSlice("writeToWorldWithUndo:5", worldServer1b, worldServer1, allRegions, 0);
//    worldHistory.printUndoStackYSlice(worldServer1, allRegions.testOutputRegion, allRegions.xSize, 0, allRegions.zSize);

//    printTestRegionSlice("worldHistory.writeToWorldWithUndo)", worldServer1b, worldServer1, allRegions, 0);
    worldSelectionUndos.get(0).writeToWorld(worldServer1b, sourceFragments.get(0), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
//    printTestRegionSlice("worldSelectionUndos.get(0)", worldServer1b, worldServer1, allRegions, 0);
    worldSelectionUndos.get(1).writeToWorld(worldServer1b, sourceFragments.get(1), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
//    printTestRegionSlice("worldSelectionUndos.get(1)", worldServer1b, worldServer1, allRegions, 0);
    worldSelectionUndos.get(2).writeToWorld(worldServer1b, sourceFragments.get(2), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
//    printTestRegionSlice("worldSelectionUndos.get(2)", worldServer1b, worldServer1, allRegions, 0);
    worldSelectionUndos.get(3).writeToWorld(worldServer1b, sourceFragments.get(3), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
//    printTestRegionSlice("worldSelectionUndos.get(3)", worldServer1b, worldServer1, allRegions, 0);
    worldSelectionUndos.get(4).writeToWorld(worldServer1b, sourceFragments.get(4), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
//    printTestRegionSlice("worldSelectionUndos.get(4)", worldServer1b, worldServer1, allRegions, 0);
    assert compareTestWorldServers(worldServer1b, worldServer1, allRegions, true);

    assert worldHistory.performComplexUndo(entityPlayerMPTest2, worldServer1);
//    printTestRegionSlice("performComplexUndo", worldServer1b, worldServer1, allRegions, 0);
    laterLayers.clear();
    worldSelectionUndos.get(4).undoChanges(worldServer1b, laterLayers);
    assert compareTestWorldServers(worldServer1b, worldServer1, allRegions, true);

    assert worldHistory.performComplexUndo(entityPlayerMPTest2, worldServer1);
    laterLayers.add(worldSelectionUndos.get(3));
    worldSelectionUndos.get(2).undoChanges(worldServer1b, laterLayers);
    assert compareTestWorldServers(worldServer1b, worldServer1, allRegions, true);

    assert worldHistory.performComplexUndo(entityPlayerMPTest1, worldServer1);
    laterLayers.add(worldSelectionUndos.get(1));
    worldSelectionUndos.get(0).undoChanges(worldServer1b, laterLayers);
    assert compareTestWorldServers(worldServer1b, worldServer1, allRegions, true);

    assert !worldHistory.performComplexUndo(entityPlayerMPTest1, worldServer1);
    assert !worldHistory.performComplexUndo(entityPlayerMPTest2, worldServer1);
  }

  // returns true if the testOutputRegion in both worldServers is identical
  public boolean compareTestWorldServers(WorldServerTest worldServerExpected, WorldServerTest worldServerActual, InGameTester.TestRegions testRegions, boolean printOnCompareFail) {
    WorldFragment worldFragmentExpectedOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentExpectedOutcome.readFromWorld(worldServerExpected, testRegions.testOutputRegion.getX(), testRegions.testOutputRegion.getY(), testRegions.testOutputRegion.getZ(), null);
    WorldFragment worldFragmentActualOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentActualOutcome.readFromWorld(worldServerActual, testRegions.testOutputRegion.getX(), testRegions.testOutputRegion.getY(), testRegions.testOutputRegion.getZ(), null);
    boolean retval = WorldFragment.areFragmentsEqual(worldFragmentExpectedOutcome, worldFragmentActualOutcome);
    if (!retval && printOnCompareFail) {
      printFragments(worldFragmentExpectedOutcome, worldFragmentActualOutcome, WorldFragment.lastCompareFailY);
    }
    return retval;
  }

  // print a side-by-side comparison of a single y slice of the two world Fragments
  public void printFragments(WorldFragment worldFragmentExpected, WorldFragment worldFragmentActual, int y)
  {
    System.out.println("Expected  : Actual for y-slice " + y);
    for (int x = 0; x < worldFragmentExpected.getxCount(); ++x) {
      for (int z = 0; z < worldFragmentExpected.getzCount(); ++z) {
        System.out.print(worldFragmentExpected.getMetadata(x, y, z) + " ");
      }
      System.out.print(": ");
      for (int z = 0; z < worldFragmentActual.getzCount(); ++z) {
        System.out.print(worldFragmentActual.getMetadata(x, y, z) + " ");
      }
      System.out.println();
    }

  }

  // prints a slice from two world Server test regions
  public void printTestRegionSlice(String title, WorldServerTest worldServerExpected, WorldServerTest worldServerActual, InGameTester.TestRegions testRegions, int y) {
    WorldFragment worldFragmentExpectedOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentExpectedOutcome.readFromWorld(worldServerExpected, testRegions.testOutputRegion.getX(), testRegions.testOutputRegion.getY(), testRegions.testOutputRegion.getZ(), null);
    WorldFragment worldFragmentActualOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentActualOutcome.readFromWorld(worldServerActual, testRegions.testOutputRegion.getX(), testRegions.testOutputRegion.getY(), testRegions.testOutputRegion.getZ(), null);
    System.out.println(title);
    printFragments(worldFragmentExpectedOutcome, worldFragmentActualOutcome, y);
  }

  public void runWorldSelectionUndoTest() {
    final int TEST_WORLD_X_SIZE = 64;
    final int TEST_WORLD_Z_SIZE = 32;
    WorldServerTest worldServer = WorldServerTest.createDummyInstance(TEST_WORLD_X_SIZE, TEST_WORLD_Z_SIZE);

    final int ACTION_COUNT = 5;
    ArrayList<InGameTester.TestRegions> testRegions = new ArrayList<InGameTester.TestRegions>();

    final int XORIGIN = 0;
    final int YORIGIN = 0;
    final int ZORIGIN = 0;
    final int XSIZE = 8;
    final int YSIZE = 1;
    final int ZSIZE = 4;
    for (int i = 0; i < ACTION_COUNT; ++i) {
      testRegions.add(new InGameTester.TestRegions(XORIGIN, YORIGIN, ZORIGIN + i * ZSIZE, XSIZE, YSIZE, ZSIZE, true));
    }
    InGameTester.TestRegions allRegions = new InGameTester.TestRegions(XORIGIN, YORIGIN, ZORIGIN + ACTION_COUNT * ZSIZE, XSIZE, YSIZE, ZSIZE, true);

    int permutations = 1;
    for (int i = ACTION_COUNT; i > 1; --i) {
      permutations *= i;
    }

    int permutationOrder[][] = new int[permutations][ACTION_COUNT];  // the order to perform the actions in.  second []: [][0] = 1st action, [][1] = 2nd action, etc

    for (int perm = 0; perm < permutations; ++perm) {
      int temp = perm;
      boolean taken[] = new boolean[ACTION_COUNT];
      for (int i = 0; i < ACTION_COUNT; ++i) {
        int skip = temp % (ACTION_COUNT - i);
        temp /= (ACTION_COUNT - i);
        int idx = 0;
        while (skip > 0 || taken[idx]) {
          if (!taken[idx]) {
            --skip;
          }
          ++idx;
        }
        taken[idx] = true;
        permutationOrder[perm][i] = idx;
      }
    }

    WorldFragment worldFragmentBlank = new WorldFragment(allRegions.xSize, allRegions.ySize, allRegions.zSize);
    worldFragmentBlank.readFromWorld(worldServer, allRegions.testRegionInitialiser.getX(), allRegions.testRegionInitialiser.getY(), allRegions.testRegionInitialiser.getZ(), null);
    ArrayList<WorldFragment> sourceFragments = new ArrayList<WorldFragment>();
    for (int i = 0; i < ACTION_COUNT; ++i) {
      InGameTester.TestRegions testRegion = testRegions.get(i);
      WorldFragment worldFragment = new WorldFragment(testRegion.xSize, testRegion.ySize, testRegion.zSize);
      for (int j = 0; j < (2 << ACTION_COUNT); ++j) {                                                          // make binary pattern for all combinations of actions
        int wx = testRegion.sourceRegion.getX() + j / testRegion.zSize;
        int wy = testRegion.sourceRegion.getY();
        int wz = testRegion.sourceRegion.getZ() + j % testRegion.zSize;

        Chunk chunk = worldServer.getChunkFromChunkCoords(wx >> 4, wz >> 4);
        boolean successful = WorldFragment.setBlockIDWithMetadata(chunk, wx, wy, wz, (0 == (j & (1 << i))) ? 0 : Block.getIdFromBlock(Blocks.wool), i+1);
      }
      VoxelSelection voxelSelection = InGameTester.selectAllNonAir(worldServer, testRegion.sourceRegion, testRegion.xSize, testRegion.ySize, testRegion.zSize);
      worldFragment.readFromWorld(worldServer, testRegion.sourceRegion.getX(), testRegion.sourceRegion.getY(), testRegion.sourceRegion.getZ(), voxelSelection);
      sourceFragments.add(worldFragment);
    }

    ArrayList<WorldSelectionUndo> worldSelectionUndos = new ArrayList<WorldSelectionUndo>();
    for (int i = 0; i < ACTION_COUNT; ++i) {
      worldSelectionUndos.add(new WorldSelectionUndo());
    }
    for (int perm = 0; perm < permutations; ++perm) {
      System.out.print(perm + "; ");
      System.out.flush();
      for (int placeOrUndo = 0; placeOrUndo < (1 << ACTION_COUNT); ++placeOrUndo) {  // mask used for placing or undoing each action, one bit per action
        boolean debugPrint = false;
        final int DEBUG_PERM = -1;
        final int DEBUG_PLACEORUNDO = 6;
        final int DEBUG_XPOS = 1;
        final int DEBUG_ZPOS = 3;
        if (perm == DEBUG_PERM && placeOrUndo == DEBUG_PLACEORUNDO) {
          debugPrint = true; // breakpoint here
        }
        // start by placing all actions, then undoing or permanenting them one by one, checking the match after each step
        worldFragmentBlank.writeToWorld(worldServer, allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ(), null);
        boolean actionIncluded[] = new boolean[ACTION_COUNT];
        ArrayList<WorldSelectionUndo> layersLeft = new ArrayList<WorldSelectionUndo>();
        for (int i = 0; i < ACTION_COUNT; ++i) {
          worldSelectionUndos.get(i).writeToWorld(worldServer, sourceFragments.get(i), allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ());
          layersLeft.add(worldSelectionUndos.get(i));
          actionIncluded[i] = true;
        }
        if (debugPrint) {
          String states = "worldSelectionUndos[" + DEBUG_XPOS + ", 0, " + DEBUG_ZPOS + "]:";
          for (int k = 0; k < ACTION_COUNT; ++k) {
            Integer savedValue = worldSelectionUndos.get(k).getStoredMetadata(DEBUG_XPOS + allRegions.testOutputRegion.getX(),
                                                                             allRegions.testOutputRegion.getY(),
                                                                             DEBUG_ZPOS + allRegions.testOutputRegion.getZ());
            states += (savedValue == null) ? "-" : savedValue;
            states += " ";
          }
          System.out.println(states);
        }

        // undo or permanent the changes one by one
        for (int j = 0; j < ACTION_COUNT; ++j) {
          int whichAction = permutationOrder[perm][j];
          boolean makeThisActionPermanent = (0 == (placeOrUndo & (1 << j)));
          ArrayList<WorldSelectionUndo> subsequentLayers = new ArrayList<WorldSelectionUndo>();
          LinkedList<WorldSelectionUndo> precedingLayers = new LinkedList<WorldSelectionUndo>();
          WorldSelectionUndo thisLayer = worldSelectionUndos.get(whichAction);
          boolean preceding = true;
          for (WorldSelectionUndo eachLayer : layersLeft) {
            if (eachLayer == thisLayer) {
              preceding = false;
            } else {
              if (preceding) {
                precedingLayers.addFirst(eachLayer);
              } else {
                subsequentLayers.add(eachLayer);
              }
            }
          }

          if (makeThisActionPermanent) {
            thisLayer.makePermanent(worldServer, precedingLayers);//, subsequentLayers);
          } else {
            thisLayer.undoChanges(worldServer, subsequentLayers);
            actionIncluded[whichAction] = false;
          }
          layersLeft.remove(thisLayer);
          if (debugPrint) {
            String states = "after undo/place   [" + DEBUG_XPOS + ", 0, " + DEBUG_ZPOS + "]:";
            for (int k = 0; k < ACTION_COUNT; ++k) {
              Integer savedValue = worldSelectionUndos.get(k).getStoredMetadata(DEBUG_XPOS + allRegions.testOutputRegion.getX(),
                      allRegions.testOutputRegion.getY(),
                      DEBUG_ZPOS + allRegions.testOutputRegion.getZ());
              states += (savedValue == null) ? "-" : savedValue;
              states += " ";
            }
            System.out.println(states);
          }
          // create expected outcome at this step = placing all fragments which are still included
          worldFragmentBlank.writeToWorld(worldServer, allRegions.expectedOutcome.getX(), allRegions.expectedOutcome.getY(), allRegions.expectedOutcome.getZ(), null);
          for (int i = 0; i < ACTION_COUNT; ++i) {
            if (actionIncluded[i]) {
              sourceFragments.get(i).writeToWorld(worldServer, allRegions.expectedOutcome.getX(), allRegions.expectedOutcome.getY(), allRegions.expectedOutcome.getZ(), null);
            }
          }
          WorldFragment worldFragmentExpectedOutcome = new WorldFragment(allRegions.xSize, allRegions.ySize, allRegions.zSize);
          worldFragmentExpectedOutcome.readFromWorld(worldServer, allRegions.expectedOutcome.getX(), allRegions.expectedOutcome.getY(), allRegions.expectedOutcome.getZ(), null);
          WorldFragment worldFragmentActualOutcome = new WorldFragment(allRegions.xSize, allRegions.ySize, allRegions.zSize);
          worldFragmentActualOutcome.readFromWorld(worldServer, allRegions.testOutputRegion.getX(), allRegions.testOutputRegion.getY(), allRegions.testOutputRegion.getZ(), null);
          boolean retval = WorldFragment.areFragmentsEqual(worldFragmentActualOutcome, worldFragmentExpectedOutcome);
          if (!retval) {
            String errorString = "comparison failed for perm=" + perm + " [";
            for (int k = 0; k < ACTION_COUNT; ++k) {
              errorString += permutationOrder[perm][k] + " ";
            }
            errorString += "]; placeOrUndo=" + placeOrUndo + "; steps performed=" + j;
            System.out.println(errorString);
            System.out.print("Operations: ");
            for (int k = 0; k <= j; ++k) {
              System.out.print(permutationOrder[perm][k] + 1 + "-"
                               + ((0 == (placeOrUndo & (1 << k))) ? "P " : "U "));
            }
            System.out.println();
            System.out.print("Actions Included:");
            for (int k = 0; k < ACTION_COUNT; ++k) {
              if (actionIncluded[k]) System.out.print(k+1 + ", ");
            }
            System.out.println();
            System.out.println("Expected  : Actual");
            for (int x = 0; x < worldFragmentActualOutcome.getxCount(); ++x) {
              for (int z = 0; z < worldFragmentExpectedOutcome.getzCount(); ++z) {
                System.out.print(worldFragmentExpectedOutcome.getMetadata(x, 0, z) + " ");
              }
              System.out.print(": ");
              for (int z = 0; z < worldFragmentActualOutcome.getzCount(); ++z) {
                System.out.print(worldFragmentActualOutcome.getMetadata(x, 0, z) + " ");
              }
              System.out.println();
            }
            assert false: errorString;
          }
        }
      }
    }
    System.out.println();
}

  public static class WorldServerTest extends WorldServer {
    public static WorldServerTest createDummyInstance(int xBlockCount, int zBlockCount) {
      Objenesis objenesis = new ObjenesisStd();
      WorldServerTest worldServerTest = (WorldServerTest) objenesis.newInstance(WorldServerTest.class);
      worldServerTest.initialise(((xBlockCount-1) >> 4) + 1, ((zBlockCount-1) >> 4) + 1);
      return worldServerTest;
    }

    public WorldServerTest() {
      super(null, null, null, 0, null);
    }

    public void initialise(int i_xChunkCount, int i_zChunkCount) {
      xChunkCount = i_xChunkCount;
      zChunkCount = i_zChunkCount;
      chunks = new ChunkTest[xChunkCount][zChunkCount];
      for (int cx = 0; cx < xChunkCount; ++cx) {
        for (int cz = 0; cz < zChunkCount; ++cz) {
          chunks[cx][cz] = new ChunkTest(this);
        }
      }
    }

    @Override
    public TileEntity getTileEntity(BlockPos blockPos) {return null;}

    @Override
    public List getEntitiesWithinAABB(Class par1Class, AxisAlignedBB par2AxisAlignedBB) {return new ArrayList();}

    @Override
    public Chunk getChunkFromChunkCoords(int cx, int cz)
    {
      assert(cx >= 0 && cx < xChunkCount);
      assert(cz >= 0 && cz < zChunkCount);
      return chunks[cx][cz];
    }

    @Override
    public WorldType getWorldType() {return WorldType.DEFAULT;}

    @Override
    public WorldInfo getWorldInfo()
    {
      return MinecraftServer.getServer().getEntityWorld().getWorldInfo();
    }

    @Override
    public List func_175712_a(StructureBoundingBox p_175712_1_, boolean p_175712_2_) {return null;}  // get ticking blocks

    @Override
    public void func_180497_b(BlockPos pos, Block p_180497_2_, int p_180497_3_, int p_180497_4_) {} // schedule block tick

    @Override
    public void notifyNeighborsRespectDebug(BlockPos pos, Block blockType) {  }

    ChunkTest chunks[][];
    int xChunkCount;
    int zChunkCount;
  }

  public static class ChunkTest extends Chunk
  {
    public ChunkTest(WorldServerTest worldServerTest) {
      super(worldServerTest, 0, 0);
    }
    @Override
    public void generateSkylightMap() {return;}
    @Override
    public void generateHeightMap() {return;}

    @Override
    public int getLightFor(EnumSkyBlock enumSkyBlock, BlockPos pos)
    {
      int i = pos.getX() & 15;
      int j = pos.getY();
      int k = pos.getZ() & 15;
      ExtendedBlockStorage extendedblockstorage = this.getBlockStorageArray()[j >> 4];
      if (extendedblockstorage == null) {
        return (this.canSeeSky(pos) ? enumSkyBlock.defaultLightValue : 0);
      } else {
        if (enumSkyBlock == EnumSkyBlock.SKY) {
          return (extendedblockstorage.getExtSkylightValue(i, j & 15, k));
        } else {
          return (enumSkyBlock == EnumSkyBlock.BLOCK ? extendedblockstorage.getExtBlocklightValue(i, j & 15, k) :
                  enumSkyBlock.defaultLightValue);
        }
      }
    }
  }

  public static class EntityPlayerMPTest extends EntityPlayerMP {
    public static EntityPlayerMPTest createDummyInstance() {
      Objenesis objenesis = new ObjenesisStd();
      EntityPlayerMPTest entityPlayerMPTest = (EntityPlayerMPTest) objenesis.newInstance(EntityPlayerMPTest.class);
      entityPlayerMPTest.setEntityId(++nextID); // needed for hashcode unique
//      entityPlayerMPTest.initialise(((xBlockCount-1) >> 4) + 1, ((zBlockCount-1) >> 4) + 1);
      return entityPlayerMPTest;
    }

    public EntityPlayerMPTest() {
      super(null, null, null, null);
    }
    private static int nextID = 0;

  }
}
