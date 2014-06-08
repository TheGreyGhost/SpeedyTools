package speedytools.serverside.ingametester;

import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;
import speedytools.common.network.Packet250SpeedyIngameTester;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.PacketHandlerRegistry;
import speedytools.serverside.WorldFragment;

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

    int firsttest = testNumber;
    int lasttest = testNumber;
    if (testNumber == TEST_ALL) {
      firsttest = 1;
      lasttest = 63;
    }

    for (int i = firsttest; i <= lasttest; ++i) {
      boolean success = false;
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
      }

      if (performTest) {
        System.out.println("; finished with success == " + success);
      } else {
        System.out.println("; completed; ");
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
    return standardCopyAndTest(performTest,  true, XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE);
  }

  public boolean standardCopyAndTest(boolean performTest, boolean expectedMatchesSource,
                                     int xOrigin, int yOrigin, int zOrigin, int xSize, int ySize, int zSize)
  {
    ChunkCoordinates sourceRegion = new ChunkCoordinates(xOrigin, yOrigin, zOrigin);
    ChunkCoordinates expectedOutcome = expectedMatchesSource ? null : new ChunkCoordinates(xOrigin + 1*(xSize + 2), yOrigin, zOrigin);
    ChunkCoordinates testOutputRegion = new ChunkCoordinates(xOrigin + 2*(xSize + 2), yOrigin, zOrigin);
    ChunkCoordinates testRegionInitialiser = new ChunkCoordinates(xOrigin + 3*(xSize + 2), yOrigin, zOrigin);
    return copyAndTestRegion(performTest, testRegionInitialiser, sourceRegion, expectedOutcome, testOutputRegion, xSize, ySize, zSize);
  }

  public boolean copyAndTestRegion(boolean performTest,
                                   ChunkCoordinates testRegionInitialiser,
                                   ChunkCoordinates sourceRegion,
                                   ChunkCoordinates expectedOutcome,
                                   ChunkCoordinates testOutputRegion,
                                   int xSize, int ySize, int zSize)
  {
    WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(0);

    if (!performTest) {
      final int WOOL_BLUE_COLOUR_ID = 2;
      final int WOOL_PURPLE_COLOUR_ID = 3;
      final int WOOL_YELLOW_COLOUR_ID = 4;
      final int WOOL_GREEN_COLOUR_ID = 5;
      drawTestRegionBoundaries(Block.cloth.blockID, WOOL_BLUE_COLOUR_ID, testRegionInitialiser, xSize, ySize, zSize);
      drawTestRegionBoundaries(Block.cloth.blockID, WOOL_PURPLE_COLOUR_ID, sourceRegion, xSize, ySize, zSize);
      if (expectedOutcome != null) drawTestRegionBoundaries(Block.cloth.blockID, WOOL_YELLOW_COLOUR_ID, expectedOutcome, xSize, ySize, zSize);
      drawTestRegionBoundaries(Block.cloth.blockID, WOOL_GREEN_COLOUR_ID, testOutputRegion, xSize, ySize, zSize);
      WorldFragment worldFragmentBlank = new WorldFragment(xSize, ySize, zSize);
      worldFragmentBlank.readFromWorld(worldServer, testRegionInitialiser.posX, testRegionInitialiser.posY, testRegionInitialiser.posZ, null);
      worldFragmentBlank.writeToWorld(worldServer, testOutputRegion.posX, testOutputRegion.posY, testOutputRegion.posZ, null);
      return true;
    } else {
      if (expectedOutcome == null) expectedOutcome = sourceRegion;
      WorldFragment worldFragment = new WorldFragment(xSize, ySize, zSize);
      worldFragment.readFromWorld(worldServer, sourceRegion.posX, sourceRegion.posY, sourceRegion.posZ, null);
      worldFragment.writeToWorld(worldServer,testOutputRegion.posX, testOutputRegion.posY, testOutputRegion.posZ, null);

      WorldFragment worldFragmentExpectedOutcome = new WorldFragment(xSize, ySize, zSize);
      worldFragmentExpectedOutcome.readFromWorld(worldServer, expectedOutcome.posX, expectedOutcome.posY, expectedOutcome.posZ, null);
      WorldFragment worldFragmentActualOutcome = new WorldFragment(xSize, ySize, zSize);
      worldFragmentActualOutcome.readFromWorld(worldServer, testOutputRegion.posX, testOutputRegion.posY, testOutputRegion.posZ, null);
      return WorldFragment.areFragmentsEqual(worldFragmentActualOutcome, worldFragmentExpectedOutcome);
    }

  }

  /**
   * sets up a block boundary for this test.  The parameters give the test region, the boundary will be drawn adjacent to the test region
   * @param origin origin of the test region
   * @param xSize
   * @param ySize
   * @param zSize
   */
  public void drawTestRegionBoundaries(int boundaryBlockID, int boundaryMetadata,
                                       ChunkCoordinates origin,
                                       int xSize, int ySize, int zSize)
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
