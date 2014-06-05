package speedytools.serverside.ingametester;

import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
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
      System.out.print("Test number " + i + " started");
      switch (i) {
        case 1: success = performTest1(performTest); break;
        case 2: success = performTest2(performTest); break;
        case 3: success = performTest3(performTest); break;
        case 4: success = performTest4(performTest); break;
      }

      System.out.println("; finished with success == " + success);
    }
  }

  public boolean performTest1(boolean performTest)
  {
    final int XORIGIN = 1; final int YORIGIN = 4; final int ZORIGIN = 10;
    final int XSIZE = 8; final int YSIZE = 8; final int ZSIZE = 8;
    final int XDEST = 11; final int YDEST = 4; final int ZDEST = 10;
    return copyAndTestRegion(performTest,  XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE, XDEST, YDEST, ZDEST);
  }

  public boolean performTest2(boolean performTest)
  {
    final int XORIGIN = 1; final int YORIGIN = 4; final int ZORIGIN = 1;
    final int XSIZE = 8; final int YSIZE = 8; final int ZSIZE = 8;
    final int XDEST = 11; final int YDEST = 4; final int ZDEST = 1;
    return copyAndTestRegion(performTest,  XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE, XDEST, YDEST, ZDEST);
  }

  public boolean performTest3(boolean performTest)
  {
    final int XORIGIN = 1; final int YORIGIN = 4; final int ZORIGIN = 19;
    final int XSIZE = 8; final int YSIZE = 8; final int ZSIZE = 200;
    final int XDEST = 11; final int YDEST = 4; final int ZDEST = 19;
    return copyAndTestRegion(performTest,  XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE, XDEST, YDEST, ZDEST);
  }

  public boolean performTest4(boolean performTest)
  {
    final int XORIGIN = 1; final int YORIGIN = 4; final int ZORIGIN = -8;
    final int XSIZE = 8; final int YSIZE = 8; final int ZSIZE = 8;
    final int XDEST = 11; final int YDEST = 4; final int ZDEST = -8;
    return copyAndTestRegion(performTest,  XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE, XDEST, YDEST, ZDEST);
  }

  public boolean copyAndTestRegion(boolean performTest,
                                    int wOriginX, int wOriginY, int wOriginZ,
                                    int xSize, int ySize, int zSize,
                                    int wDestX, int wDestY, int wDestZ)
  {
    WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(0);

    if (!performTest) {
      WorldFragment worldFragmentBlank = new WorldFragment(xSize, ySize, zSize);
      worldFragmentBlank.writeToWorld(worldServer, wDestX, wDestY, wDestZ);
      return true;
    } else {
      WorldFragment worldFragment = new WorldFragment(xSize, ySize, zSize);
      worldFragment.readFromWorld(worldServer, wOriginX, wOriginY, wOriginZ);
      worldFragment.writeToWorld(worldServer, wDestX, wDestY, wDestZ);

      WorldFragment worldFragment2 = new WorldFragment(xSize, ySize, zSize);
      worldFragment2.readFromWorld(worldServer, wDestX, wDestY, wDestZ);
      return WorldFragment.areBlockStoresEqual(worldFragment, worldFragment2);
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
