package speedytools.serverside.ingametester;

import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import speedytools.common.selections.VoxelSelection;
import speedytools.common.network.Packet250SpeedyIngameTester;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.PacketHandlerRegistry;
import speedytools.common.utilities.QuadOrientation;
import speedytools.serverside.worldmanipulation.AsynchronousToken;
import speedytools.serverside.worldmanipulation.WorldFragment;
import speedytools.serverside.worldmanipulation.WorldSelectionUndo;

import java.util.LinkedList;
import java.util.List;

/**
 * User: The Grey Ghost
 * Date: 26/05/2014
 */
public class InGameTester
{
  public InGameTester(PacketHandlerRegistry packetHandlerRegistry)
  {
    packetHandlerSpeedyIngameTester = new PacketHandlerSpeedyIngameTester();
    packetHandlerRegistry.registerHandlerMethod(Side.SERVER, Packet250Types.PACKET250_INGAME_TESTER.getPacketTypeID(), packetHandlerSpeedyIngameTester);
  }

  /**
   * Perform an automated in-game test
   * @param testNumber
   * @param performTest if true, perform test.  If false, erase results of last test / prepare for another test
   */
  public void performTest(int testNumber, boolean performTest)
  {
    final int TEST_ALL = 64;
    boolean runAllTests = false;

    int firsttest = testNumber;
    int lasttest = testNumber;
    if (testNumber == TEST_ALL) {
      firsttest = 1;
      lasttest = 63;
      runAllTests = true;
    }

    for (int i = firsttest; i <= lasttest; ++i) {
      boolean success = false;
      boolean blankTest = false;
      if (performTest) {
        System.out.print("Test number " + i + " started");
      } else {
        System.out.print("Preparation for test number " + i);
      }
      switch (i) {
        case 1: success = performTest1(performTest); break;
        case 2: success = performTest2(performTest); break;
        case 3: success = performTest3(performTest); break;
        case 4: success = performTest4(performTest); break;
        case 5: success = performTest5(performTest); break;
        case 6: success = performTest6(performTest); break;
        case 7: success = performTest7(performTest); break;
        case 8: success = performTest8(performTest); break;
        case 9: success = performTest9(performTest, runAllTests); break;
        case 10: success = performTest10(performTest, runAllTests); break;
        case 11: success = performTest11(performTest, runAllTests); break;
        case 12: success = performTest12(performTest); break;
        case 13: success = performTest13(performTest); break;
        default: blankTest = true; break;
      }
      if (blankTest) {
        System.out.println("; unused test");
      } else {
        if (performTest) {
          System.out.println("; finished with success == " + success);
        } else {
          System.out.println("; completed; ");
        }
      }
    }
  }

  public boolean performTest1(boolean performTest)
  {
    // fails comparison due to moving water block
    final int XORIGIN = 1; final int YORIGIN = 4; final int ZORIGIN = 10;
    final int XSIZE = 8; final int YSIZE = 8; final int ZSIZE = 8;
    return standardCopyAndTest(performTest, true, XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE);  }

  public boolean performTest2(boolean performTest)
  {
    final int XORIGIN = 1; final int YORIGIN = 4; final int ZORIGIN = 1;
    final int XSIZE = 8; final int YSIZE = 8; final int ZSIZE = 8;
    return standardCopyAndTest(performTest, true, XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE);  }

  public boolean performTest3(boolean performTest)
  {
    // fails comparison due to dispenser being triggered by the call to update()
    final int XORIGIN = 1; final int YORIGIN = 4; final int ZORIGIN = 19;
    final int XSIZE = 8; final int YSIZE = 8; final int ZSIZE = 200;
    return standardCopyAndTest(performTest, true, XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE);  }

  public boolean performTest4(boolean performTest)
  {
    final int XORIGIN = 1; final int YORIGIN = 4; final int ZORIGIN = -8;
    final int XSIZE = 8; final int YSIZE = 8; final int ZSIZE = 8;
    return standardCopyAndTest(performTest, true, XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE);
  }

  /**
   *  Test5:  use WorldSelectionUndo to copy a cuboid fragment
   */
  public boolean performTest5(boolean performTest)
  {
    WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(0);

    final int XORIGIN = 1; final int YORIGIN = 4; final int ZORIGIN = -17;
    final int XSIZE = 8; final int YSIZE = 8; final int ZSIZE = 8;
    TestRegions testRegions = new TestRegions(XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE, false);
    if (!performTest) {
      testRegions.drawAllTestRegionBoundaries();
      WorldFragment worldFragmentBlank = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
      worldFragmentBlank.readFromWorld(worldServer, testRegions.testRegionInitialiser.posX, testRegions.testRegionInitialiser.posY, testRegions.testRegionInitialiser.posZ, null);
      worldFragmentBlank.writeToWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);
      return true;
    }

    WorldFragment sourceWorldFragment = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    sourceWorldFragment.readFromWorld(worldServer, testRegions.sourceRegion.posX, testRegions.sourceRegion.posY, testRegions.sourceRegion.posZ, null);

    WorldSelectionUndo worldSelectionUndo = new WorldSelectionUndo();
    worldSelectionUndo.writeToWorld(worldServer, sourceWorldFragment, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ);

    WorldFragment worldFragmentExpectedOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentExpectedOutcome.readFromWorld(worldServer, testRegions.sourceRegion.posX, testRegions.sourceRegion.posY, testRegions.sourceRegion.posZ, null);
    WorldFragment worldFragmentActualOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentActualOutcome.readFromWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);
    return WorldFragment.areFragmentsEqual(worldFragmentActualOutcome, worldFragmentExpectedOutcome);
  }

  // Test7: simple undo to troubleshoot a problem with masks
  public boolean performTest7(boolean performTest)
  {
    WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(0);

    final int XORIGIN = 1; final int YORIGIN = 4; final int ZORIGIN = -35;
    final int XSIZE = 3; final int YSIZE = 3; final int ZSIZE = 3;
    TestRegions testRegions = new TestRegions(XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE, true);
    if (!performTest) {
      testRegions.drawAllTestRegionBoundaries();
      WorldFragment worldFragmentInitial = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
      worldFragmentInitial.readFromWorld(worldServer, testRegions.testRegionInitialiser.posX, testRegions.testRegionInitialiser.posY, testRegions.testRegionInitialiser.posZ, null);
      worldFragmentInitial.writeToWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);
      return true;
    }

    WorldFragment sourceWorldFragment = new WorldFragment(testRegions.xSize-2, testRegions.ySize, testRegions.zSize-2);
    sourceWorldFragment.readFromWorld(worldServer, testRegions.sourceRegion.posX+1, testRegions.sourceRegion.posY, testRegions.sourceRegion.posZ+1, null);

    WorldSelectionUndo worldSelectionUndo = new WorldSelectionUndo();
    worldSelectionUndo.writeToWorld(worldServer, sourceWorldFragment, testRegions.testOutputRegion.posX+1, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ+1);
    List<WorldSelectionUndo> undoLayers = new LinkedList<WorldSelectionUndo>();
    worldSelectionUndo.undoChanges(worldServer, undoLayers);

    WorldFragment worldFragmentExpectedOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentExpectedOutcome.readFromWorld(worldServer, testRegions.expectedOutcome.posX, testRegions.expectedOutcome.posY, testRegions.expectedOutcome.posZ, null);
    WorldFragment worldFragmentActualOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentActualOutcome.readFromWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);
    return WorldFragment.areFragmentsEqual(worldFragmentActualOutcome, worldFragmentExpectedOutcome);
  }

  // Test6: more complicated undo
  public boolean performTest6(boolean performTest)
  {
    WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(0);

    final int XORIGIN = 1; final int YORIGIN = 4; final int ZORIGIN = -26;
    final int XSIZE = 8; final int YSIZE = 8; final int ZSIZE = 8;
    TestRegions testRegions = new TestRegions(XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE, true);
    if (!performTest) {
      testRegions.drawAllTestRegionBoundaries();
      WorldFragment worldFragmentInitial = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
      worldFragmentInitial.readFromWorld(worldServer, testRegions.testRegionInitialiser.posX, testRegions.testRegionInitialiser.posY, testRegions.testRegionInitialiser.posZ, null);
      worldFragmentInitial.writeToWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);
//      worldFragmentInitial.readFromWorld(worldServer, testRegions.sourceRegion.posX, testRegions.sourceRegion.posY, testRegions.sourceRegion.posZ, null);
//      worldFragmentInitial.writeToWorld(worldServer, testRegions.testRegionInitialiser.posX, testRegions.testRegionInitialiser.posY, testRegions.testRegionInitialiser.posZ, null);
//      worldFragmentInitial.writeToWorld(worldServer, testRegions.expectedOutcome.posX, testRegions.expectedOutcome.posY, testRegions.expectedOutcome.posZ, null);
      return true;
    }

    WorldFragment sourceWorldFragment = new WorldFragment(testRegions.xSize-2, testRegions.ySize, testRegions.zSize-2);
    sourceWorldFragment.readFromWorld(worldServer, testRegions.sourceRegion.posX+1, testRegions.sourceRegion.posY, testRegions.sourceRegion.posZ+1, null);

    WorldSelectionUndo worldSelectionUndo = new WorldSelectionUndo();
    worldSelectionUndo.writeToWorld(worldServer, sourceWorldFragment, testRegions.testOutputRegion.posX+1, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ+1);
    List<WorldSelectionUndo> undoLayers = new LinkedList<WorldSelectionUndo>();
    worldSelectionUndo.undoChanges(worldServer, undoLayers);

    WorldFragment worldFragmentExpectedOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentExpectedOutcome.readFromWorld(worldServer, testRegions.expectedOutcome.posX, testRegions.expectedOutcome.posY, testRegions.expectedOutcome.posZ, null);
    WorldFragment worldFragmentActualOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentActualOutcome.readFromWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);
    return WorldFragment.areFragmentsEqual(worldFragmentActualOutcome, worldFragmentExpectedOutcome);
  }

  /**
   *  Test8:  use WorldSelectionUndo with a selection mask to copy a fragment
   */
  public boolean performTest8(boolean performTest)
  {
    WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(0);

    final int XORIGIN = 1; final int YORIGIN = 4; final int ZORIGIN = -44;
    final int XSIZE = 8; final int YSIZE = 8; final int ZSIZE = 8;
    TestRegions testRegions = new TestRegions(XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE, true);
    if (!performTest) {
      testRegions.drawAllTestRegionBoundaries();
      WorldFragment worldFragmentBlank = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
      worldFragmentBlank.readFromWorld(worldServer, testRegions.testRegionInitialiser.posX, testRegions.testRegionInitialiser.posY, testRegions.testRegionInitialiser.posZ, null);
      worldFragmentBlank.writeToWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);
      return true;
    }

    VoxelSelection voxelSelection = selectAllNonAir(worldServer, testRegions.sourceRegion, testRegions.xSize, testRegions.ySize, testRegions.zSize);

    WorldFragment sourceWorldFragment = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    sourceWorldFragment.readFromWorld(worldServer, testRegions.sourceRegion.posX, testRegions.sourceRegion.posY, testRegions.sourceRegion.posZ, voxelSelection);

    WorldSelectionUndo worldSelectionUndo = new WorldSelectionUndo();
    worldSelectionUndo.writeToWorld(worldServer, sourceWorldFragment, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ);

    WorldFragment worldFragmentExpectedOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentExpectedOutcome.readFromWorld(worldServer, testRegions.expectedOutcome.posX, testRegions.expectedOutcome.posY, testRegions.expectedOutcome.posZ, null);
    WorldFragment worldFragmentActualOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentActualOutcome.readFromWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);
    return WorldFragment.areFragmentsEqual(worldFragmentActualOutcome, worldFragmentExpectedOutcome);
  }

  // Test9: two placements, undo the first placement, then undo the second placement.  Also test deleteUndoLayer
  //  takes up three test bays:
  //  the initialiser is the same in all three
  //  the source Region is different, region1 is applied first then region 2
  //  the output Regions are 1: undo first only, 2: undo first then undo second, 3: delete first then undo second
  //  one step per click
  // NOTE - this out-of-order undo will not return the world exactly to the start condition:
  // (1) erases a torch; (2) replaces the support wall for that torch with a ladder;
  //  undo (1) puts the torch back, but it's now next to a ladder so it breaks; then
  //  undo (2) replaces the support wall, but not the torch
  static int whichStep9 = 0;
  static WorldSelectionUndo worldSelectionUndo1a_9;
  static WorldSelectionUndo worldSelectionUndo1b_9;
  static WorldSelectionUndo worldSelectionUndo2a_9;
  static WorldSelectionUndo worldSelectionUndo2b_9;
  static WorldSelectionUndo worldSelectionUndo3a_9;
  static WorldSelectionUndo worldSelectionUndo3b_9;
  public boolean performTest9(boolean performTest, boolean runAllSteps)
  {
    WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(0);

    final int XORIGIN = 1; final int YORIGIN = 4; final int ZORIGIN = -53;
    final int XSIZE = 8; final int YSIZE = 8; final int ZSIZE = 8;
    TestRegions testRegions1 = new TestRegions(XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE, true);
    TestRegions testRegions2 = new TestRegions(XORIGIN, YORIGIN, ZORIGIN - 9, XSIZE, YSIZE, ZSIZE, true);
    TestRegions testRegions3 = new TestRegions(XORIGIN, YORIGIN, ZORIGIN - 18, XSIZE, YSIZE, ZSIZE, true);
    if (!performTest) {
      testRegions1.drawAllTestRegionBoundaries();
      testRegions2.drawAllTestRegionBoundaries();
      testRegions3.drawAllTestRegionBoundaries();
      WorldFragment worldFragmentInitial = new WorldFragment(testRegions1.xSize, testRegions1.ySize, testRegions1.zSize);
      worldFragmentInitial.readFromWorld(worldServer, testRegions1.testRegionInitialiser.posX, testRegions1.testRegionInitialiser.posY, testRegions1.testRegionInitialiser.posZ, null);
      worldFragmentInitial.writeToWorld(worldServer, testRegions1.testOutputRegion.posX, testRegions1.testOutputRegion.posY, testRegions1.testOutputRegion.posZ, null);
      worldFragmentInitial.writeToWorld(worldServer, testRegions2.testOutputRegion.posX, testRegions2.testOutputRegion.posY, testRegions2.testOutputRegion.posZ, null);
      worldFragmentInitial.writeToWorld(worldServer, testRegions3.testOutputRegion.posX, testRegions3.testOutputRegion.posY, testRegions3.testOutputRegion.posZ, null);
      worldFragmentInitial.writeToWorld(worldServer, testRegions3.expectedOutcome.posX, testRegions3.expectedOutcome.posY, testRegions3.expectedOutcome.posZ, null);

//      worldFragmentInitial.writeToWorld(worldServer, testRegions1.expectedOutcome.posX, testRegions1.expectedOutcome.posY, testRegions1.expectedOutcome.posZ, null);
//      worldFragmentInitial.writeToWorld(worldServer, testRegions2.expectedOutcome.posX, testRegions2.expectedOutcome.posY, testRegions2.expectedOutcome.posZ, null);
//      worldFragmentInitial.writeToWorld(worldServer, testRegions3.expectedOutcome.posX, testRegions3.expectedOutcome.posY, testRegions3.expectedOutcome.posZ, null);

      ChunkCoordinates sourceFragOrigin = new ChunkCoordinates(testRegions2.sourceRegion);
      sourceFragOrigin.posX++; sourceFragOrigin.posZ++;
      VoxelSelection voxelSelection = selectAllNonAir(worldServer, sourceFragOrigin, testRegions2.xSize-2, testRegions2.ySize, testRegions2.zSize-2);
      WorldFragment sourceWorldFragment2 = new WorldFragment(testRegions2.xSize-2, testRegions2.ySize, testRegions2.zSize-2);
      sourceWorldFragment2.readFromWorld(worldServer, testRegions2.sourceRegion.posX + 1, testRegions2.sourceRegion.posY, testRegions2.sourceRegion.posZ + 1, voxelSelection);
      sourceWorldFragment2.writeToWorld(worldServer, testRegions1.expectedOutcome.posX+1,  testRegions1.expectedOutcome.posY,  testRegions1.expectedOutcome.posZ+1, null);
      sourceWorldFragment2.writeToWorld(worldServer, testRegions3.expectedOutcome.posX + 1, testRegions3.expectedOutcome.posY, testRegions3.expectedOutcome.posZ + 1, null);

//      WorldFragment sourceWorldFragment3 = new WorldFragment(testRegions2.xSize, testRegions2.ySize, testRegions2.zSize);
//      sourceWorldFragment3.readFromWorld(worldServer, testRegions2.sourceRegion.posX+1, testRegions2.sourceRegion.posY, testRegions2.sourceRegion.posZ+1, voxelSelection);
//      sourceWorldFragment3.writeToWorld(worldServer, testRegions3.expectedOutcome.posX+1,  testRegions3.expectedOutcome.posY,  testRegions3.expectedOutcome.posZ+1, null);
//      worldFragmentInitial.readFromWorld(worldServer, testRegions.sourceRegion.posX, testRegions.sourceRegion.posY, testRegions.sourceRegion.posZ, null);
//      worldFragmentInitial.writeToWorld(worldServer, testRegions.testRegionInitialiser.posX, testRegions.testRegionInitialiser.posY, testRegions.testRegionInitialiser.posZ, null);
//      worldFragmentInitial.writeToWorld(worldServer, testRegions.expectedOutcome.posX, testRegions.expectedOutcome.posY, testRegions.expectedOutcome.posZ, null);
      whichStep9 = 0;
      return true;
    }

    ++whichStep9;
    ChunkCoordinates sourceFragOrigin = new ChunkCoordinates(testRegions1.sourceRegion);
    sourceFragOrigin.posX++; sourceFragOrigin.posZ++;
    VoxelSelection voxelSelection = selectAllNonAir(worldServer, sourceFragOrigin, testRegions1.xSize-2, testRegions1.ySize, testRegions1.zSize-2);
    WorldFragment sourceWorldFragment1 = new WorldFragment(testRegions1.xSize-2, testRegions1.ySize, testRegions1.zSize-2);
    sourceWorldFragment1.readFromWorld(worldServer, testRegions1.sourceRegion.posX+1, testRegions1.sourceRegion.posY, testRegions1.sourceRegion.posZ+1, voxelSelection);

    sourceFragOrigin = new ChunkCoordinates(testRegions2.sourceRegion);
    sourceFragOrigin.posX++; sourceFragOrigin.posZ++;
    voxelSelection = selectAllNonAir(worldServer, sourceFragOrigin, testRegions2.xSize-2, testRegions2.ySize, testRegions2.zSize-2);
    WorldFragment sourceWorldFragment2 = new WorldFragment(testRegions2.xSize-2, testRegions2.ySize, testRegions2.zSize-2);
    sourceWorldFragment2.readFromWorld(worldServer, testRegions2.sourceRegion.posX+1, testRegions2.sourceRegion.posY, testRegions2.sourceRegion.posZ+1, voxelSelection);

    if (runAllSteps || (whichStep9 == 1)) {
      worldSelectionUndo1a_9 = new WorldSelectionUndo();
      worldSelectionUndo1a_9.writeToWorld(worldServer, sourceWorldFragment1, testRegions1.testOutputRegion.posX + 1, testRegions1.testOutputRegion.posY, testRegions1.testOutputRegion.posZ + 1);
    }
    if (runAllSteps || (whichStep9 == 2)) {
      worldSelectionUndo1b_9 = new WorldSelectionUndo();
      worldSelectionUndo1b_9.writeToWorld(worldServer, sourceWorldFragment2, testRegions1.testOutputRegion.posX + 1, testRegions1.testOutputRegion.posY, testRegions1.testOutputRegion.posZ + 1);
    }

    if (runAllSteps || (whichStep9 == 3)) {
      List<WorldSelectionUndo> undoLayers = new LinkedList<WorldSelectionUndo>();
      undoLayers.add(worldSelectionUndo1b_9);
      worldSelectionUndo1a_9.undoChanges(worldServer, undoLayers);
    }

    if (runAllSteps || (whichStep9 == 1)) {
      worldSelectionUndo2a_9 = new WorldSelectionUndo();
      worldSelectionUndo2a_9.writeToWorld(worldServer, sourceWorldFragment1, testRegions2.testOutputRegion.posX + 1, testRegions2.testOutputRegion.posY, testRegions2.testOutputRegion.posZ + 1);
    }
    if (runAllSteps || (whichStep9 == 2)) {
      worldSelectionUndo2b_9 = new WorldSelectionUndo();
      worldSelectionUndo2b_9.writeToWorld(worldServer, sourceWorldFragment2, testRegions2.testOutputRegion.posX + 1, testRegions2.testOutputRegion.posY, testRegions2.testOutputRegion.posZ + 1);
    }
    if (runAllSteps || (whichStep9 == 3)) {
      LinkedList<WorldSelectionUndo> undoLayers = new LinkedList<WorldSelectionUndo>();
      undoLayers.add(worldSelectionUndo2b_9);
      worldSelectionUndo2a_9.undoChanges(worldServer, undoLayers);
    }
    if (runAllSteps || (whichStep9 == 4)) {
      LinkedList<WorldSelectionUndo> undoLayers = new LinkedList<WorldSelectionUndo>();
      worldSelectionUndo2b_9.undoChanges(worldServer, undoLayers);
    }

    if (runAllSteps || (whichStep9 == 1)) {
      worldSelectionUndo3a_9 = new WorldSelectionUndo();
      worldSelectionUndo3a_9.writeToWorld(worldServer, sourceWorldFragment1, testRegions3.testOutputRegion.posX + 1, testRegions3.testOutputRegion.posY, testRegions3.testOutputRegion.posZ + 1);
    }
    if (runAllSteps || (whichStep9 == 2)) {
      worldSelectionUndo3b_9 = new WorldSelectionUndo();
      worldSelectionUndo3b_9.writeToWorld(worldServer, sourceWorldFragment2, testRegions3.testOutputRegion.posX + 1, testRegions3.testOutputRegion.posY, testRegions3.testOutputRegion.posZ + 1);
    }
    if (runAllSteps || (whichStep9 == 3)) {
      LinkedList<WorldSelectionUndo> precedingUndoLayers = new LinkedList<WorldSelectionUndo>();
      LinkedList<WorldSelectionUndo> subsequentUndoLayers = new LinkedList<WorldSelectionUndo>();
      precedingUndoLayers.add(worldSelectionUndo3a_9);
      worldSelectionUndo3b_9.makePermanent(worldServer, precedingUndoLayers); //, subsequentUndoLayers);
    }
    if (runAllSteps || (whichStep9 == 4)) {
      LinkedList<WorldSelectionUndo> undoLayers = new LinkedList<WorldSelectionUndo>();
      worldSelectionUndo3a_9.undoChanges(worldServer, undoLayers);
    }

    if (runAllSteps || (whichStep9 >= 5)) {
      WorldFragment worldFragmentExpectedOutcome = new WorldFragment(testRegions1.xSize, testRegions1.ySize, testRegions1.zSize);
      worldFragmentExpectedOutcome.readFromWorld(worldServer, testRegions1.expectedOutcome.posX, testRegions1.expectedOutcome.posY, testRegions1.expectedOutcome.posZ, null);
      WorldFragment worldFragmentActualOutcome = new WorldFragment(testRegions1.xSize, testRegions1.ySize, testRegions1.zSize);
      worldFragmentActualOutcome.readFromWorld(worldServer, testRegions1.testOutputRegion.posX, testRegions1.testOutputRegion.posY, testRegions1.testOutputRegion.posZ, null);
      boolean retval = WorldFragment.areFragmentsEqual(worldFragmentActualOutcome, worldFragmentExpectedOutcome);

      worldFragmentExpectedOutcome = new WorldFragment(testRegions2.xSize, testRegions2.ySize, testRegions2.zSize);
      worldFragmentExpectedOutcome.readFromWorld(worldServer, testRegions2.expectedOutcome.posX, testRegions2.expectedOutcome.posY, testRegions2.expectedOutcome.posZ, null);
      worldFragmentActualOutcome = new WorldFragment(testRegions2.xSize, testRegions2.ySize, testRegions2.zSize);
      worldFragmentActualOutcome.readFromWorld(worldServer, testRegions2.testOutputRegion.posX, testRegions2.testOutputRegion.posY, testRegions2.testOutputRegion.posZ, null);
      retval = retval && WorldFragment.areFragmentsEqual(worldFragmentActualOutcome, worldFragmentExpectedOutcome);

      worldFragmentExpectedOutcome = new WorldFragment(testRegions3.xSize, testRegions3.ySize, testRegions3.zSize);
      worldFragmentExpectedOutcome.readFromWorld(worldServer, testRegions3.expectedOutcome.posX, testRegions3.expectedOutcome.posY, testRegions3.expectedOutcome.posZ, null);
      worldFragmentActualOutcome = new WorldFragment(testRegions3.xSize, testRegions3.ySize, testRegions3.zSize);
      worldFragmentActualOutcome.readFromWorld(worldServer, testRegions3.testOutputRegion.posX, testRegions3.testOutputRegion.posY, testRegions3.testOutputRegion.posZ, null);
      retval = retval && WorldFragment.areFragmentsEqual(worldFragmentActualOutcome, worldFragmentExpectedOutcome);

      return retval;
    }
    return false;
  }


  // Test10:
  // Test of WorldSelectionUndo: Tests all permutations of makePermanent() and undoChanges() for five blocks:
  //  1) place A, B, C, D, E (in a binary pattern 0..31 so that every combination of A, B, C, D, E voxels is present)
  //  2) remove each one by either makePermanent() or undoChanges(), and check that the result at each step matches
  //     a straight-forward placement.  eg
  //     after makePermanent(C) and undoChanges(B), the result matches the world after placing A, D, then E.
  //     performs every permutation:
  //     a) order of actions = 5! = 120
  //     b) removal method = 2^5 = 32
  //     So a total of 120 * 32 = 50,000 or so.
  public boolean performTest10(boolean performTest, boolean runAllSteps)
  {
    if (!performTest) return true;
    WorldSelectionUndoTest worldSelectionUndoTest = new WorldSelectionUndoTest();
    try {
      worldSelectionUndoTest.runWorldSelectionUndoTest();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return true;
  }

  public boolean performTest11(boolean performTest, boolean runAllSteps)
  {
    if (!performTest) return true;
    WorldSelectionUndoTest worldSelectionUndoTest = new WorldSelectionUndoTest();
    try {
      worldSelectionUndoTest.runWorldHistoryTests();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return true;
  }

  /**
   *  Test12:  test of asynchronous WorldFragment: use WorldFragment with a selection mask to copy a fragment asynchronously and verify it matches the synchronous copy
   */
  public boolean performTest12(boolean performTest)
  {
    WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(0);

    final int XORIGIN = 1; final int YORIGIN = 4; final int ZORIGIN = -80 - (35 - 8);
    final int XSIZE = 35; final int YSIZE = 54; final int ZSIZE = 17;
    TestRegions testRegions = new TestRegions(XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE, true);
    if (!performTest) {
      testRegions.drawAllTestRegionBoundaries();
      WorldFragment worldFragmentBlank = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
      worldFragmentBlank.readFromWorld(worldServer, testRegions.testRegionInitialiser.posX, testRegions.testRegionInitialiser.posY, testRegions.testRegionInitialiser.posZ, null);
      worldFragmentBlank.writeToWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);
      worldFragmentBlank.writeToWorld(worldServer, testRegions.expectedOutcome.posX, testRegions.expectedOutcome.posY, testRegions.expectedOutcome.posZ, null);
      return true;
    }

    VoxelSelection voxelSelection = selectAllNonAir(worldServer, testRegions.sourceRegion, testRegions.xSize, testRegions.ySize, testRegions.zSize);

    // do a synchronous copy
    WorldFragment sourceWorldFragment = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    sourceWorldFragment.readFromWorld(worldServer, testRegions.sourceRegion.posX, testRegions.sourceRegion.posY, testRegions.sourceRegion.posZ, voxelSelection);
    sourceWorldFragment.writeToWorld(worldServer, testRegions.expectedOutcome.posX, testRegions.expectedOutcome.posY, testRegions.expectedOutcome.posZ, voxelSelection);

    // do the asynchronous copy
    WorldFragment asyncWorldFragment = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    AsynchronousToken token = asyncWorldFragment.readFromWorldAsynchronous(worldServer, testRegions.sourceRegion.posX,
                                                                           testRegions.sourceRegion.posY, testRegions.sourceRegion.posZ, voxelSelection);
    while (!token.isTaskComplete()) {
      token.setTimeToInterrupt(token.IMMEDIATE_TIMEOUT);
      token.continueProcessing();
    }

    QuadOrientation orientation = new QuadOrientation(0,0,1,1);
    AsynchronousToken writeToken = asyncWorldFragment.writeToWorldAsynchronous(worldServer, testRegions.testOutputRegion.posX,
                                                                               testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, voxelSelection, orientation);
    while (!writeToken.isTaskComplete()) {
      writeToken.setTimeToInterrupt(writeToken.IMMEDIATE_TIMEOUT);
      writeToken.continueProcessing();
    }

    // compare the two
    WorldFragment worldFragmentExpectedOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentExpectedOutcome.readFromWorld(worldServer, testRegions.expectedOutcome.posX, testRegions.expectedOutcome.posY, testRegions.expectedOutcome.posZ, null);
    WorldFragment worldFragmentActualOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentActualOutcome.readFromWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);
    boolean retval = WorldFragment.areFragmentsEqual(worldFragmentActualOutcome, worldFragmentExpectedOutcome);
    if (!retval) {
      System.out.println();
      System.out.println("Mismatch at [" + WorldFragment.lastCompareFailX + ", " + WorldFragment.lastCompareFailY + ", " + WorldFragment.lastCompareFailZ + "]");
      System.out.println("Expected vs Actual:");
      System.out.println(WorldFragment.lastFailureReason);
    }
    return retval;
  }

  /**
   *  Test13:  test WorldSelectionUndo with a selection mask to copy a fragment asynchronously and verify it matches the synchronous copy, likewise the subsequent undo
   */
  public boolean performTest13(boolean performTest)
  {
    WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(0);

    final int XORIGIN = 1; final int YORIGIN = 4; final int ZORIGIN = -120;
    final int XSIZE = 8; final int YSIZE = 8; final int ZSIZE = 8;
    TestRegions testRegions = new TestRegions(XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE, true);
    if (!performTest) {
      testRegions.drawAllTestRegionBoundaries();
      WorldFragment worldFragmentBlank = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
      worldFragmentBlank.readFromWorld(worldServer, testRegions.testRegionInitialiser.posX, testRegions.testRegionInitialiser.posY, testRegions.testRegionInitialiser.posZ, null);
      worldFragmentBlank.writeToWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);
      worldFragmentBlank.writeToWorld(worldServer, testRegions.expectedOutcome.posX, testRegions.expectedOutcome.posY, testRegions.expectedOutcome.posZ, null);
      return true;
    }

    VoxelSelection voxelSelection = selectAllNonAir(worldServer, testRegions.sourceRegion, testRegions.xSize, testRegions.ySize, testRegions.zSize);

    // do a synchronous copy
    WorldFragment sourceWorldFragment = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    sourceWorldFragment.readFromWorld(worldServer, testRegions.sourceRegion.posX, testRegions.sourceRegion.posY, testRegions.sourceRegion.posZ, voxelSelection);
    WorldSelectionUndo worldSelectionUndo = new WorldSelectionUndo();
    worldSelectionUndo.writeToWorld(worldServer, sourceWorldFragment, testRegions.expectedOutcome.posX, testRegions.expectedOutcome.posY, testRegions.expectedOutcome.posZ);

    // do the asynchronous copy
    WorldSelectionUndo asyncWorldSelectionUndo = new WorldSelectionUndo();
    QuadOrientation identity = new QuadOrientation(0,0,1,1);
    AsynchronousToken token = asyncWorldSelectionUndo.writeToWorldAsynchronous(worldServer, sourceWorldFragment,
                                                                                  testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, identity);
    while (!token.isTaskComplete()) {
      token.setTimeToInterrupt(token.IMMEDIATE_TIMEOUT);
      token.continueProcessing();
    }

    // compare the two
    WorldFragment worldFragmentExpectedOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentExpectedOutcome.readFromWorld(worldServer, testRegions.expectedOutcome.posX, testRegions.expectedOutcome.posY, testRegions.expectedOutcome.posZ, null);
    WorldFragment worldFragmentActualOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentActualOutcome.readFromWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);
    boolean retval = WorldFragment.areFragmentsEqual(worldFragmentActualOutcome, worldFragmentExpectedOutcome);
    if (!retval) {
      System.out.println();
      System.out.println("Mismatch at [" + WorldFragment.lastCompareFailX + ", " + WorldFragment.lastCompareFailY + ", " + WorldFragment.lastCompareFailZ + "]");
      System.out.println("Expected vs Actual:");
      System.out.println(WorldFragment.lastFailureReason);
    }

    // synchronous undo
    List<WorldSelectionUndo> dummyList = new LinkedList<WorldSelectionUndo>();
    worldSelectionUndo.undoChanges(worldServer, dummyList);

    // asynchronous undo
    token = asyncWorldSelectionUndo.undoChangesAsynchronous(worldServer, dummyList);
    while (!token.isTaskComplete()) {
      token.setTimeToInterrupt(token.IMMEDIATE_TIMEOUT);
      token.continueProcessing();
    }

    // compare the two
     worldFragmentExpectedOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentExpectedOutcome.readFromWorld(worldServer, testRegions.expectedOutcome.posX, testRegions.expectedOutcome.posY, testRegions.expectedOutcome.posZ, null);
     worldFragmentActualOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
    worldFragmentActualOutcome.readFromWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);
     retval = WorldFragment.areFragmentsEqual(worldFragmentActualOutcome, worldFragmentExpectedOutcome);
    if (!retval) {
      System.out.println();
      System.out.println("Mismatch at [" + WorldFragment.lastCompareFailX + ", " + WorldFragment.lastCompareFailY + ", " + WorldFragment.lastCompareFailZ + "]");
      System.out.println("Expected vs Actual:");
      System.out.println(WorldFragment.lastFailureReason);
    }
    return retval;
  }


  public boolean standardCopyAndTest(boolean performTest, boolean expectedMatchesSource,
                                     int xOrigin, int yOrigin, int zOrigin, int xSize, int ySize, int zSize)
  {
    TestRegions testRegions = new TestRegions(xOrigin, yOrigin, zOrigin, xSize, ySize, zSize, !expectedMatchesSource);
    return copyAndTestRegion(performTest, testRegions);
  }

  public boolean copyAndTestRegion(boolean performTest, TestRegions testRegions)
  {
    WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(0);

    if (!performTest) {
      testRegions.drawAllTestRegionBoundaries();
      WorldFragment worldFragmentBlank = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
      worldFragmentBlank.readFromWorld(worldServer, testRegions.testRegionInitialiser.posX, testRegions.testRegionInitialiser.posY, testRegions.testRegionInitialiser.posZ, null);
      worldFragmentBlank.writeToWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);
      return true;
    } else {
      ChunkCoordinates expectedOutcome = (testRegions.expectedOutcome == null) ? testRegions.sourceRegion : testRegions.expectedOutcome;
      WorldFragment worldFragment = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
      worldFragment.readFromWorld(worldServer, testRegions.sourceRegion.posX, testRegions.sourceRegion.posY, testRegions.sourceRegion.posZ, null);
      worldFragment.writeToWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);

      WorldFragment worldFragmentExpectedOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
      worldFragmentExpectedOutcome.readFromWorld(worldServer, expectedOutcome.posX, expectedOutcome.posY, expectedOutcome.posZ, null);
      WorldFragment worldFragmentActualOutcome = new WorldFragment(testRegions.xSize, testRegions.ySize, testRegions.zSize);
      worldFragmentActualOutcome.readFromWorld(worldServer, testRegions.testOutputRegion.posX, testRegions.testOutputRegion.posY, testRegions.testOutputRegion.posZ, null);
      boolean retval = WorldFragment.areFragmentsEqual(worldFragmentExpectedOutcome, worldFragmentActualOutcome);
      if (!retval) {
        System.out.println();
        System.out.println("Mismatch at [" + WorldFragment.lastCompareFailX + ", " + WorldFragment.lastCompareFailY + ", " + WorldFragment.lastCompareFailZ + "]");
        System.out.println("Expected vs Actual:");
        System.out.println(WorldFragment.lastFailureReason);
      }
      return retval;
    }
  }

  public static VoxelSelection selectAllNonAir(World world, ChunkCoordinates origin, int xSize, int ySize, int zSize)
  {
    VoxelSelection retval = new VoxelSelection(xSize, ySize, zSize);
    for (int zpos = 0; zpos < zSize; ++zpos) {
      for (int xpos = 0; xpos < xSize; ++xpos) {
        for (int ypos = 0; ypos < ySize; ++ypos) {
          if (world.getBlockId(xpos + origin.posX, ypos + origin.posY, zpos + origin.posZ) != 0) {
            retval.setVoxel(xpos, ypos, zpos);
          }
        }
      }
    }
    return retval;
  }


  public static class TestRegions
  {
    /**
     * Generate a new set of four test regions from the given origin and size information
     * @param xOrigin
     * @param yOrigin
     * @param zOrigin
     * @param i_xSize
     * @param i_ySize
     * @param i_zSize
     * @param hasExpectedOutcomeRegion
     */
    public TestRegions(int xOrigin, int yOrigin, int zOrigin, int i_xSize, int i_ySize, int i_zSize, boolean hasExpectedOutcomeRegion)
    {
      sourceRegion = new ChunkCoordinates(xOrigin, yOrigin, zOrigin);
      expectedOutcome = !hasExpectedOutcomeRegion ? null : new ChunkCoordinates(xOrigin + 1*(i_xSize + 2), yOrigin, zOrigin);
      testOutputRegion = new ChunkCoordinates(xOrigin + 2*(i_xSize + 2), yOrigin, zOrigin);
      testRegionInitialiser = new ChunkCoordinates(xOrigin + 3*(i_xSize + 2), yOrigin, zOrigin);
      xSize = i_xSize;
      ySize = i_ySize;
      zSize = i_zSize;
    }

    /**
     * draw all of the test region boundaries for this test
     */
    public void drawAllTestRegionBoundaries()
    {
      final int WOOL_BLUE_COLOUR_ID = 2;
      final int WOOL_PURPLE_COLOUR_ID = 3;
      final int WOOL_YELLOW_COLOUR_ID = 4;
      final int WOOL_GREEN_COLOUR_ID = 5;
      drawSingleTestRegionBoundaries(Block.cloth.blockID, WOOL_BLUE_COLOUR_ID, testRegionInitialiser);
      drawSingleTestRegionBoundaries(Block.cloth.blockID, WOOL_PURPLE_COLOUR_ID, sourceRegion);
      if (expectedOutcome != null) drawSingleTestRegionBoundaries(Block.cloth.blockID, WOOL_YELLOW_COLOUR_ID, expectedOutcome);
      drawSingleTestRegionBoundaries(Block.cloth.blockID, WOOL_GREEN_COLOUR_ID, testOutputRegion);
    }

    /**
     * sets up a block boundary for this test.  The parameters give the test region, the boundary will be drawn adjacent to the test region
     * @param origin origin of the test region
     */
    public void drawSingleTestRegionBoundaries(int boundaryBlockID, int boundaryMetadata,
                                               ChunkCoordinates origin)
    {
      int wOriginX = origin.posX;
      int wOriginY = origin.posY;
      int wOriginZ = origin.posZ;
      WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(0);
      int wy = wOriginY - 1;
      for (int x = -1; x <= xSize; ++x) {
        worldServer.setBlock(x + wOriginX, wy, wOriginZ - 1, boundaryBlockID, boundaryMetadata, 1 + 2);
        worldServer.setBlock(x + wOriginX, wy, wOriginZ + zSize, boundaryBlockID, boundaryMetadata, 1 + 2);
      }

      for (int z = -1; z <= zSize; ++z) {
        worldServer.setBlock(wOriginX - 1, wy, z + wOriginZ, boundaryBlockID, boundaryMetadata, 1 + 2);
        worldServer.setBlock(wOriginX + xSize, wy, z + wOriginZ, boundaryBlockID, boundaryMetadata, 1 + 2);
      }

      for (int y = 0; y < ySize; ++y) {
        worldServer.setBlock(    wOriginX - 1, y + wOriginY,     wOriginZ - 1, boundaryBlockID, boundaryMetadata, 1 + 2);
        worldServer.setBlock(wOriginX + xSize, y + wOriginY,     wOriginZ - 1, boundaryBlockID, boundaryMetadata, 1 + 2);
        worldServer.setBlock(    wOriginX - 1, y + wOriginY, wOriginZ + zSize, boundaryBlockID, boundaryMetadata, 1 + 2);
        worldServer.setBlock(wOriginX + xSize, y + wOriginY, wOriginZ + zSize, boundaryBlockID, boundaryMetadata, 1 + 2);
      }
    }

    public ChunkCoordinates testRegionInitialiser;
    public ChunkCoordinates sourceRegion;
    public ChunkCoordinates expectedOutcome;
    public ChunkCoordinates testOutputRegion;
    public int xSize;
    public int ySize;
    public int zSize;
  }


  public class PacketHandlerSpeedyIngameTester implements PacketHandlerRegistry.PacketHandlerMethod {
    public boolean handlePacket(EntityPlayer player, Packet250CustomPayload packet)
    {
      Packet250SpeedyIngameTester toolIngameTesterPacket = Packet250SpeedyIngameTester.createPacket250SpeedyIngameTester(packet);
      if (toolIngameTesterPacket == null) return false;
      InGameTester.this.performTest(toolIngameTesterPacket.getWhichTest(), toolIngameTesterPacket.isPerformTest());
      return true;
    }
  }

  private PacketHandlerSpeedyIngameTester packetHandlerSpeedyIngameTester;
}
