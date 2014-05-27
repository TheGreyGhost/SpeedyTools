package speedytools.serverside.ingametester;

import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import speedytools.common.network.Packet250SpeedyIngameTester;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.PacketHandlerRegistry;
import speedytools.serverside.BlockStore;

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
   */
  public void performTest(int testNumber)
  {
    System.out.println("Test number " + testNumber + "started");
  }

  public boolean performTest1()
  {
    final int XORIGIN = 1;
    final int YORIGIN = 1;
    final int ZORIGIN = 1;
    final int XSIZE = 8;
    final int YSIZE = 8;
    final int ZSIZE = 8;


    WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(0);
    BlockStore blockStore = new BlockStore(XSIZE, YSIZE, ZSIZE);
    for (int y = 0; y < YSIZE; ++y) {
      for (int z = 0; z < ZSIZE; ++z) {
        for (int x = 0; x < XSIZE; ++x) {
          int wx = x + XORIGIN;
          int wy = y + YORIGIN;
          int wz = z + ZORIGIN;
          int id = worldServer.getBlockId(wx, wy, wz);
          int data = worldServer.getBlockMetadata(wx, wy, wz);
          TileEntity tileEntity = worldServer.getBlockTileEntity(wx, wy, wz);
          NBTTagCompound tag = null;
          if (tileEntity != null) {
            tag = new NBTTagCompound();
            tileEntity.writeToNBT(tag);
          }
          blockStore.setBlockID(x, y, z, id);
          blockStore.setMetadata(x, y, z, data);
          blockStore.setTileEntityData(x, y, z, tag);
        }
      }
    }




  }


  public class PacketHandlerSpeedyIngameTester implements PacketHandlerRegistry.PacketHandlerMethod {
    public boolean handlePacket(EntityPlayer player, Packet250CustomPayload packet)
    {
      Packet250SpeedyIngameTester toolIngameTesterPacket = Packet250SpeedyIngameTester.createPacket250SpeedyIngameTester(packet);
      if (toolIngameTesterPacket == null) return false;
      InGameTester.this.performTest(toolIngameTesterPacket.getWhichTest());
      return true;
    }
  }

  private PacketHandlerSpeedyIngameTester packetHandlerSpeedyIngameTester;
}
