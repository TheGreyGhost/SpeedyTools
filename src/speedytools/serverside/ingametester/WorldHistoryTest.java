package speedytools.serverside.ingametester;

import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import speedytools.clientside.selections.VoxelSelection;
import speedytools.serverside.WorldFragment;
import speedytools.serverside.WorldSelectionUndo;

import java.util.ArrayList;
import java.util.List;

/**
 * User: The Grey Ghost
 * Date: 13/06/2014
 * This class tests for correct operation of WorldHistory, WorldSelectionUndo, and WorldFragment
 *
 *
 * This class tests for correct communication between CloneToolsNetworkClient and CloneToolsNetworkServer.  The classes tested are
 * CloneToolsNetworkClient, CloneToolsNetworkServer, Packet250CloneToolAcknowledge, Packet250CloneToolStatus, Packet250CloneToolUse,
 *   , ClientStatus, ServerStatus
 * The tests are based around the networkprotocols.txt specification.  It uses dummy objects to simulate network communications:
 * EntityClientPlayerMP, EntityPlayerMP, NetClientHandler, NetServerHandler.  PacketHandler is bypassed (not used)
 *
 * Test plan:
 *  Test of WorldSelectionUndo: Tests all permutations of makePermanent() and undoChanges() for five blocks:
 //  1) place A, B, C, D, E (in a binary pattern 0..31 so that every combination of A, B, C, D, E voxels is present)
 //  2) remove each one by either makePermanent() or undoChanges(), and check that the result at each step matches
 //     a straight-forward placement.  eg
 //     after makePermanent(C) and undoChanges(B), the result matches the world after placing A, D, then E.
 //     performs every permutation:
 //     a) order of actions = 5! = 120
 //     b) removal method = 2^5 = 32
 //     So a total of 120 * 32 = 40,000 or so.
 *
 */
public class WorldHistoryTest
{
  public void runTest() throws Exception {
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
    worldFragmentBlank.readFromWorld(worldServer, allRegions.testRegionInitialiser.posX, allRegions.testRegionInitialiser.posY, allRegions.testRegionInitialiser.posZ, null);
    ArrayList<WorldFragment> sourceFragments = new ArrayList<WorldFragment>();
    for (int i = 0; i < ACTION_COUNT; ++i) {
      InGameTester.TestRegions testRegion = testRegions.get(i);
      WorldFragment worldFragment = new WorldFragment(testRegion.xSize, testRegion.ySize, testRegion.zSize);
      for (int j = 0; j < (2 << ACTION_COUNT); ++j) {                                                          // make binary pattern for all combinations of actions
        int wx = testRegion.sourceRegion.posX + j / testRegion.zSize;
        int wy = testRegion.sourceRegion.posY;
        int wz = testRegion.sourceRegion.posZ + j % testRegion.zSize;

        Chunk chunk = worldServer.getChunkFromChunkCoords(wx >> 4, wz >> 4);
        boolean successful = WorldFragment.setBlockIDWithMetadata(chunk, wx, wy, wz, (0 == (j & (1 << i))) ? 0 : Block.cloth.blockID, i);
      }
      VoxelSelection voxelSelection = InGameTester.selectAllNonAir(worldServer, testRegion.sourceRegion, testRegion.xSize, testRegion.ySize, testRegion.zSize);
      worldFragment.readFromWorld(worldServer, testRegion.sourceRegion.posX, testRegion.sourceRegion.posY, testRegion.sourceRegion.posZ, voxelSelection);
      sourceFragments.add(worldFragment);
    }

    for (int perm = 0; perm < permutations; ++perm) {
      System.out.println(perm + "; ");
      for (int placeOrUndo = 0; placeOrUndo < (1 << ACTION_COUNT); ++placeOrUndo) {
        // create expected outcome
        worldFragmentBlank.writeToWorld(worldServer, allRegions.expectedOutcome.posX, allRegions.expectedOutcome.posY, allRegions.expectedOutcome.posZ, null);
        for (int i = 0; i < ACTION_COUNT; ++i) {
          if (0 == (placeOrUndo & (1 << i))) {
            sourceFragments.get(permutationOrder[perm][i]).writeToWorld(worldServer, allRegions.expectedOutcome.posX, allRegions.expectedOutcome.posY, allRegions.expectedOutcome.posZ, null);
          }
        }
      }
      worldFragmentBlank.writeToWorld(worldServer, allRegions.testOutputRegion.posX, allRegions.testOutputRegion.posY, allRegions.testOutputRegion.posZ, null);
    }

    WorldSelectionUndo worldSelectionUndo1_10;
    WorldSelectionUndo worldSelectionUndo2_10;
    WorldSelectionUndo worldSelectionUndo3_10;
    WorldSelectionUndo worldSelectionUndo4_10;
    WorldSelectionUndo worldSelectionUndo5_10;
  }

  public static class WorldServerTest extends WorldServer {
    public static WorldServerTest createDummyInstance(int xBlockCount, int zBlockCount) {
      Objenesis objenesis = new ObjenesisStd();
      WorldServerTest worldServerTest = (WorldServerTest) objenesis.newInstance(WorldServerTest.class);
      worldServerTest.initialise(((xBlockCount-1) >> 4) + 1, ((zBlockCount-1) >> 4) + 1);
      return worldServerTest;
    }

    public WorldServerTest() {
      super(null, null, null, 0, null, null, null);
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

    public TileEntity getBlockTileEntity(int par1, int par2, int par3) {return null;}

    public List selectEntitiesWithinAABB(Class par1Class, AxisAlignedBB par2AxisAlignedBB, IEntitySelector par3IEntitySelector) {return new ArrayList();}

    public Chunk getChunkFromChunkCoords(int cx, int cz)
    {
      assert(cx >= 0 && cx < xChunkCount);
      assert(cz >= 0 && cz < zChunkCount);
      return chunks[cx][cz];
    }

    ChunkTest chunks[][];
    int xChunkCount;
    int zChunkCount;
  }

  public static class ChunkTest extends Chunk
  {
    public ChunkTest(WorldServerTest worldServerTest) {
      super(worldServerTest, 0, 0);
    }
    public void generateSkylightMap() {return;}
    public void generateHeightMap() {return;}
  }

}
