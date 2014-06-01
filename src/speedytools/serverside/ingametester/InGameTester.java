package speedytools.serverside.ingametester;

import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.network.packet.Packet51MapChunk;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInstance;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import speedytools.common.network.Packet250SpeedyIngameTester;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.PacketHandlerRegistry;
import speedytools.serverside.BlockStore;

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
      BlockStore blockStoreBlank = new BlockStore(xSize, ySize, zSize);
      setBlocks(worldServer, wDestX, wDestY, wDestZ, blockStoreBlank);
      return true;
    } else {
      BlockStore blockStore = makeBlockStore(worldServer, wOriginX, wOriginY, wOriginZ, xSize, ySize, zSize);
      setBlocks(worldServer, wDestX, wDestY, wDestZ, blockStore);

      BlockStore blockStore2 = makeBlockStore(worldServer, wDestX, wDestY, wDestZ, xSize, ySize, zSize);
      return areBlockStoresEqual(blockStore, blockStore2);
    }

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
          NBTTagCompound tileEntityTag = null;
          if (tileEntity != null) {
            tileEntityTag = new NBTTagCompound();
            tileEntity.writeToNBT(tileEntityTag);
          }
          blockStore.setBlockID(x, y, z, id);
          blockStore.setMetadata(x, y, z, data);
          blockStore.setTileEntityData(x, y, z, tileEntityTag);
        }
      }
    }

    AxisAlignedBB axisAlignedBB = AxisAlignedBB.getBoundingBox(xOrigin, yOrigin, zOrigin,
                                                               xOrigin + xCount, yOrigin + yCount, zOrigin + zCount);
    List<Entity> allHangingEntities = worldServer.getEntitiesWithinAABB(EntityHanging.class, axisAlignedBB);

    for (Entity entity : allHangingEntities) {
      NBTTagCompound tag = new NBTTagCompound();
      entity.writeToNBTOptional(tag);
      blockStore.addEntity(entity.posX - xOrigin, entity.posY - yOrigin, entity.posZ - zOrigin, tag);
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

          Chunk chunk = worldServer.getChunkFromChunkCoords(wx >> 4, wz >> 4);

          chunk.removeChunkBlockTileEntity(wx & 0x0f, wy, wz & 0x0f);
          boolean successful = setBlockIDWithMetadata(chunk, wx, wy, wz, blockID, blockMetadata);
          if (successful && tileEntityNBT != null) {
            setTileEntity(worldServer, wx, wy, wz, tileEntityNBT);
          }
        }
      }
    }

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
          int blockID = worldServer.getBlockId(wx, wy, wz);
          worldServer.func_96440_m(wx, wy, wz, blockID);

          worldServer.notifyBlockOfNeighborChange(wx-1,   wy,   wz, blockID);
          worldServer.notifyBlockOfNeighborChange(wx+1,   wy,   wz, blockID);
          worldServer.notifyBlockOfNeighborChange(  wx, wy-1,   wz, blockID);
          worldServer.notifyBlockOfNeighborChange(  wx, wy+1,   wz, blockID);
          worldServer.notifyBlockOfNeighborChange(  wx,   wy, wz-1, blockID);
          worldServer.notifyBlockOfNeighborChange(  wx,   wy, wz+1, blockID);
        }
      }
    }

    for (int cx = cxMin; cx <= cxMax; ++cx) {
      for (int cz = czMin; cz <= czMax; ++cz) {
        PlayerManager playerManager = worldServer.getPlayerManager();
        PlayerInstance playerinstance = playerManager.getOrCreateChunkWatcher(cx, cz, false);
        if (playerinstance != null) {
          Chunk chunk = worldServer.getChunkFromChunkCoords(cx, cz);
          Packet51MapChunk packet51MapChunk = new Packet51MapChunk(chunk, false, cyFlags);
          playerinstance.sendToAllPlayersWatchingChunk(packet51MapChunk);
        }

        int xmin = Math.max(cx << 4, xOrigin);
        int ymin = yOrigin;
        int zmin = Math.max(cz << 4, zOrigin);
        int xmax = Math.min((cx << 4) | 0x0f, xOrigin + xCount - 1);
        int ymax = yOrigin + yCount - 1;
        int zmax = Math.min((cz << 4) | 0x0f, zOrigin + zCount - 1);

        for (int x = xmin; x <= xmax; ++x) {
          for (int y = ymin; y <= ymax; ++y) {
            for (int z = zmin; z <= zmax; ++z) {
              TileEntity tileEntity = worldServer.getBlockTileEntity(x, y, z);
              if (tileEntity != null) {
                Packet tilePacket = tileEntity.getDescriptionPacket();
                if (tilePacket != null) {
                  playerinstance.sendToAllPlayersWatchingChunk(tilePacket);
                }
              }
              LinkedList<NBTTagCompound> listOfEntitiesAtThisBlock = blockStore.getEntitiesAtBlock(x - xOrigin, y - yOrigin, z - zOrigin);
              if (listOfEntitiesAtThisBlock != null) {
                for (NBTTagCompound nbtTagCompound : listOfEntitiesAtThisBlock) {
                  changeHangingEntityNBTposition(nbtTagCompound, x, y, z);
                  Entity newEntity = EntityList.createEntityFromNBT(nbtTagCompound, worldServer);
                  if (newEntity != null) {
                    worldServer.spawnEntityInWorld(newEntity);
                  }
                }
              }
            }
          }
        }
      }
    }

    //      j = this.chunkLocation.chunkXPos * 16 + (this.locationOfBlockChange[i] >> 12 & 15);
//      k = this.locationOfBlockChange[i] & 255;
//      l = this.chunkLocation.chunkZPos * 16 + (this.locationOfBlockChange[i] >> 8 & 15);
//
//      if (PlayerManager.getWorldServer(this.thePlayerManager).blockHasTileEntity(j, k, l))
//      {
//        this.sendTileToAllPlayersWatchingChunk(PlayerManager.getWorldServer(this.thePlayerManager).getBlockTileEntity(j, k, l));
//      }


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

  /**
   * Update the position information of the given NBT
   * @param nbtTagCompound the NBT
   * @return the updated NBT
   */
  public NBTTagCompound changeTileEntityNBTposition(NBTTagCompound nbtTagCompound, int wx, int wy, int wz)
  {
    nbtTagCompound.setTag("x", new NBTTagInt("x", wx));
    nbtTagCompound.setTag("y", new NBTTagInt("y", wy));
    nbtTagCompound.setTag("z", new NBTTagInt("z", wz));
    return nbtTagCompound;
  }

  /**
   * Update the position information of the given HangingEntityNBT.  The integer position is changed; the fractional component is unchanged.
   * @param nbtTagCompound the NBT
   * @return the updated NBT
   */
  public NBTTagCompound changeHangingEntityNBTposition(NBTTagCompound nbtTagCompound, int wx, int wy, int wz)
  {
    NBTTagList nbttaglist = nbtTagCompound.getTagList("Pos");
    double oldX = ((NBTTagDouble)nbttaglist.tagAt(0)).data;
    double oldY = ((NBTTagDouble)nbttaglist.tagAt(1)).data;
    double oldZ = ((NBTTagDouble)nbttaglist.tagAt(2)).data;

    double newX = wx + (oldX - Math.floor(oldX));
    double newY = wy + (oldY - Math.floor(oldY));
    double newZ = wz + (oldZ - Math.floor(oldZ));

    NBTTagList newPositionNBT = new NBTTagList();
    newPositionNBT.appendTag(new NBTTagDouble((String) null, newX));
    newPositionNBT.appendTag(new NBTTagDouble((String) null, newY));
    newPositionNBT.appendTag(new NBTTagDouble((String) null, newZ));
    nbtTagCompound.setTag("Pos", newPositionNBT);

    int oldPosX = nbtTagCompound.getInteger("TileX");
    int oldPosY = nbtTagCompound.getInteger("TileY");
    int oldPosZ = nbtTagCompound.getInteger("TileZ");
    long newPosX = oldPosX + Math.round(newX - oldX);
    long newPosY = oldPosY + Math.round(newY - oldY);
    long newPosZ = oldPosZ + Math.round(newZ - oldZ);

    nbtTagCompound.setInteger("TileX", (int)newPosX);
    nbtTagCompound.setInteger("TileY", (int)newPosY);
    nbtTagCompound.setInteger("TileZ", (int)newPosZ);

    return nbtTagCompound;
  }

  /**
   * Creates a TileEntity from the given NBT and puts it at the specified world location
   * @param world the world
   * @param nbtTagCompound the NBT for the tile entity (if null - do nothing)
   */
  public void setTileEntity(World world, int wx, int wy, int wz, NBTTagCompound nbtTagCompound) {
    if (nbtTagCompound != null) {
      changeTileEntityNBTposition(nbtTagCompound, wx, wy, wz);
      TileEntity tileEntity = TileEntity.createAndLoadEntity(nbtTagCompound);
      if (tileEntity != null) {
        world.setBlockTileEntity(wx, wy, wz, tileEntity);
      }
    }
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
            NBTTagCompound nbt1 = blockStore1.getTileEntityData(x, y, z);
            changeTileEntityNBTposition(nbt1, 0, 0, 0);
            NBTTagCompound nbt2 = blockStore2.getTileEntityData(x, y, z);
            changeTileEntityNBTposition(nbt2, 0, 0, 0);
            if (0 != nbt1.toString().compareTo(nbt2.toString()) ) {
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
      InGameTester.this.performTest(toolIngameTesterPacket.getWhichTest(), toolIngameTesterPacket.isPerformTest());
      return true;
    }
  }

  private PacketHandlerSpeedyIngameTester packetHandlerSpeedyIngameTester;
}
