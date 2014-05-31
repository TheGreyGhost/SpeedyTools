package speedytools.serverside.ingametester;

import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.network.packet.Packet51MapChunk;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInstance;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
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
    final int TEST_ALL = 64;

    int firsttest = testNumber;
    int lasttest =testNumber;
    if (testNumber == TEST_ALL) {
      firsttest = 1;
      lasttest = 64;
    }

    for (int i = firsttest; i <= lasttest; ++i) {
      boolean success = false;
      System.out.print("Test number " + i + " started");
      switch (i) {
        case 1: success = performTest1(); break;
        case 2: success = performTest2(); break;
      }

      System.out.println("; finished with success == " + success);
    }
  }

  public boolean performTest1()
  {
    final int XORIGIN = 1;
    final int YORIGIN = 4;
    final int ZORIGIN = 11;
    final int XSIZE = 8;
    final int YSIZE = 8;
    final int ZSIZE = 8;

    final int XDEST = 11;
    final int YDEST = 4;
    final int ZDEST = 21;

    WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(0);

    BlockStore blockStoreBlank = new BlockStore(XSIZE, YSIZE, ZSIZE);
    setBlocks(worldServer, XDEST, YDEST, ZDEST, blockStoreBlank);

    BlockStore blockStore = makeBlockStore(worldServer, XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE);
    setBlocks(worldServer, XDEST, YDEST, ZDEST, blockStore);

    BlockStore blockStore2 = makeBlockStore(worldServer, XDEST, YDEST, ZDEST, XSIZE, YSIZE, ZSIZE);
    return areBlockStoresEqual(blockStore, blockStore2);
  }

  public boolean performTest2()
  {
    final int XORIGIN = 1;
    final int YORIGIN = 4;
    final int ZORIGIN = 1;
    final int XSIZE = 8;
    final int YSIZE = 8;
    final int ZSIZE = 8;

    final int XDEST = 1;
    final int YDEST = 4;
    final int ZDEST = 21;

    WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(0);

    BlockStore blockStoreBlank = new BlockStore(XSIZE, YSIZE, ZSIZE);
    setBlocks(worldServer, XDEST, YDEST, ZDEST, blockStoreBlank);

    BlockStore blockStore = makeBlockStore(worldServer, XORIGIN, YORIGIN, ZORIGIN, XSIZE, YSIZE, ZSIZE);
    setBlocks(worldServer, XDEST, YDEST, ZDEST, blockStore);

    BlockStore blockStore2 = makeBlockStore(worldServer, XDEST, YDEST, ZDEST, XSIZE, YSIZE, ZSIZE);
    return areBlockStoresEqual(blockStore, blockStore2);
  }



  public BlockStore makeBlockStore(WorldServer worldServer, int xOrigin, int yOrigin, int zOrigin, int xCount, int yCount, int zCount)
  {
    BlockStore blockStore = new BlockStore(xCount, yCount, zCount);
    for (int y = 0; y < yCount; ++y) {
      for (int z = 0; z < zCount; ++z) {
        for (int x = 0; x < xCount; ++x) {
          int wx = x + xOrigin;
          int wy = y + yOrigin;
          int wz = z + zOrigin;
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
    return blockStore;
  }

  public boolean setBlocks(WorldServer worldServer, int xOrigin, int yOrigin, int zOrigin, BlockStore blockStore)
  {
    /* the steps are

    10: delete TileEntityData to stop resource leak
    20: copy ID and metadata to chunk directly (chunk setBlockIDwithMetadata without the updating)
    30: create & update TileEntities - setChunkBlockTileEntity, World.addTileEntity
    40: update helper structures - precipitation height map, heightmap, lighting
    50: notifyNeighbourChange for all blocks    World.func_96440_m
    55: flagChunkForUpdate to resend to client
    60: updateTick for all blocks

     */

    final int xCount = blockStore.getXcount();
    final int yCount = blockStore.getYcount();
    final int zCount = blockStore.getZcount();

    for (int y = 0; y < yCount; ++y) {
      for (int z = 0; z < zCount; ++z) {
        for (int x = 0; x < xCount; ++x) {
          int wx = x + xOrigin;
          int wy = y + yOrigin;
          int wz = z + zOrigin;
          int blockID = blockStore.getBlockID(x, y, z);
          int blockMetadata = blockStore.getMetadata(x, y, z);
          NBTTagCompound tileEntityNBT = blockStore.getTileEntityData(x, y, z);

          Chunk chunk = worldServer.getChunkFromChunkCoords(wx >> 4, wz  >> 4);

          chunk.removeChunkBlockTileEntity(wx, wy, wz);
          boolean successful = setBlockIDWithMetadata(chunk, wx, wy, wz, blockID, blockMetadata);
        }
      }
    }

    // todo worry about tile entities later

    final int cxMin = xOrigin >> 4;
    final int cxMax = (xOrigin + xCount) >> 4;
    final int cyMin = yOrigin >> 4;
    final int cyMax = (yOrigin + yCount) >> 4;
    final int czMin = zOrigin >> 4;
    final int czMax = (zOrigin + zCount) >> 4;

    int cyFlags = 0;
    for (int cy = cyMin; cy <= cyMax; ++cy) {
      cyFlags = 1 << cy;
    }

    for (int cx = cxMin; cx <= cxMax; ++cx) {
      for (int cz = czMin; cz <= czMax; ++cz) {
        Chunk chunk = worldServer.getChunkFromChunkCoords(cx, cz);
        chunk.generateHeightMap();
        chunk.generateSkylightMap();
      }
    }

    for (int y = 0; y < yCount; ++y) {
      for (int z = 0; z < zCount; ++z) {
        for (int x = 0; x < xCount; ++x) {
          int wx = x + xOrigin;
          int wy = y + yOrigin;
          int wz = z + zOrigin;
          int blockID = blockStore.getBlockID(x, y, z);
          worldServer.func_96440_m(wx, wy, wz, blockID);
        }
      }
    }

    for (int cx = cxMin; cx <= cxMax; ++cx) {
      for (int cz = czMin; cz <= czMax; ++cz) {
        PlayerManager playerManager = worldServer.getPlayerManager();
        PlayerInstance playerinstance = playerManager.getOrCreateChunkWatcher(cx, cz, false);
        Chunk chunk = worldServer.getChunkFromChunkCoords(cx, cz);
        Packet51MapChunk packet51MapChunk = new Packet51MapChunk(chunk, false, cyFlags);
        playerinstance.sendToAllPlayersWatchingChunk(packet51MapChunk);
      }
    }

    for (int y = 0; y < yCount; ++y) {
      for (int z = 0; z < zCount; ++z) {
        for (int x = 0; x < xCount; ++x) {
          int wx = x + xOrigin;
          int wy = y + yOrigin;
          int wz = z + zOrigin;
          int blockID = blockStore.getBlockID(x, y, z);
          if (blockID > 0) {
            Block.blocksList[blockID].updateTick(worldServer, wx, wy, wz, worldServer.rand);
          }
        }
      }
    }

//  { //Forge: Send only the tile entities that are updated, Adding this brace lets us keep the indent and the patch small
//    for (i = 0; i < this.numberOfTilesToUpdate; ++i)
//    {
//      j = this.chunkLocation.chunkXPos * 16 + (this.locationOfBlockChange[i] >> 12 & 15);
//      k = this.locationOfBlockChange[i] & 255;
//      l = this.chunkLocation.chunkZPos * 16 + (this.locationOfBlockChange[i] >> 8 & 15);
//
//      if (PlayerManager.getWorldServer(this.thePlayerManager).blockHasTileEntity(j, k, l))
//      {
//        this.sendTileToAllPlayersWatchingChunk(PlayerManager.getWorldServer(this.thePlayerManager).getBlockTileEntity(j, k, l));
//      }
//    }
//  }

//    // First set the block
//    Chunk chunk = worldServer.getChunkFromChunkCoords(x >> 4, z >> 4);
//    int previousId = 0;
//
//    if (notifyAndLight) {
//      previousId = chunk.getBlockID(x & 15, y, z & 15);
//    }
//
//    boolean successful = chunk.setBlockIDWithMetadata(x & 15, y, z & 15, block.getId(), block.getData());
//
//    // Create the TileEntity
//    if (successful) {
//      CompoundTag tag = block.getNbtData();
//      if (tag != null) {
//        NBTTagCompound nativeTag = NBTConverter.toNative(tag);
//        nativeTag.setString("id", block.getNbtId());
//        TileEntityUtils.setTileEntity(getWorld(), position, nativeTag);
//      }
//    }

//    if (notifyAndLight) {
//      world.updateAllLightTypes(x, y, z);
//      world.markBlockForUpdate(x, y, z);
//      world.notifyBlockChange(x, y, z, previousId);
//
//      Block mcBlock = Block.blocksList[block.getId()];
//      if (mcBlock != null && mcBlock.hasComparatorInputOverride()) {
//        world.func_96440_m(x, y, z, block.getId());
//      }
//    }

    return true;
  }

  public boolean setBlockIDWithMetadata(Chunk chunk, int wx, int wy, int wz, int blockID, int metaData)
  {
    int xLSB = wx & 0x0f;
    int yLSB = wy & 0x0f;
    int zLSB = wz & 0x0f;

    ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();
    ExtendedBlockStorage extendedblockstorage = storageArrays[wy >> 4];

    if (extendedblockstorage == null)
    {
      if (blockID == 0) { return false; }

      extendedblockstorage =  new ExtendedBlockStorage(wy & ~0x0f, !chunk.worldObj.provider.hasNoSky);
      storageArrays[wy >> 4] = extendedblockstorage;
    }
    extendedblockstorage.setExtBlockID(xLSB, yLSB, zLSB, blockID);
    extendedblockstorage.setExtBlockMetadata(xLSB, yLSB, zLSB, metaData);
    return true;
  }

  /**
   * compares the contents of the two blockstores
   * @param blockStore1
   * @param blockStore2
   * @return true if the contents are exactly the same
   */
  public boolean areBlockStoresEqual(BlockStore blockStore1, BlockStore blockStore2)
  {
    if (blockStore1.getXcount() != blockStore2.getXcount()
        || blockStore1.getYcount() != blockStore2.getYcount()
        || blockStore1.getZcount() != blockStore2.getZcount()) {
      return false;
    }
    for (int x = 0; x < blockStore1.getXcount(); ++x ) {
      for (int y = 0; y < blockStore1.getYcount(); ++y) {
        for (int z = 0; z < blockStore1.getZcount(); ++z) {
          if (blockStore1.getBlockID(x, y, z) != blockStore2.getBlockID(x, y, z)
              || blockStore1.getMetadata(x, y, z) != blockStore2.getMetadata(x, y, z) ) {
            return false;
          }
          if (blockStore1.getTileEntityData(x, y, z) == null) {
            if (blockStore2.getTileEntityData(x, y, z) != null) {
              return false;
            }
          } else {
            if (0 != blockStore1.getTileEntityData(x, y, z).toString().compareTo(
                     blockStore2.getTileEntityData(x, y, z).toString()
                      )) {
              return false;
            }
          }
        }
      }
    }
    return true;
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
