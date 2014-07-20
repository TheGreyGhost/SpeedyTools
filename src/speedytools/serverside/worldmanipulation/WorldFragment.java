package speedytools.serverside.worldmanipulation;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import speedytools.common.selections.VoxelSelection;
import speedytools.common.utilities.BlockRotateFlipHelper;
import speedytools.common.utilities.Pair;
import speedytools.common.utilities.QuadOrientation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * User: The Grey Ghost
 * Date: 27/05/2014
 * Stores the block ID, metadata, NBT (TileEntity) data, and Entity data for a voxel selection
 * Typical usage:
 * (1) Create an empty world fragment
 * (2a) readFromWorld(VoxelSelection) to read the WorldFragment from the world for all voxels in the given VoxelSelection
 *   or
 * (2b) various set() to manipulate the fragment's contents
 * (3) various get() to retrieve the fragment's contents
 * (4) writeToWorld() to write the fragment into the world.
 */
public class WorldFragment
{
  public static final int MAX_X_SIZE = 256;
  public static final int MAX_Y_SIZE = 256;
  public static final int MAX_Z_SIZE = 256;

  private static final int Y_MIN_VALID = 0;
  private static final int Y_MAX_VALID_PLUS_ONE = 256;

  /**
   * create an empty WorldFragment from a given quadOrientation and y size
   * The x,z size of the region is copied from the quadOrientation
   */
  public WorldFragment(QuadOrientation quadOrientation, int i_ycount)
  {
    initialise(quadOrientation.getWXsize(), i_ycount, quadOrientation.getWZSize());
  }

  /** creates a WorldFragment, initially empty
   *
   * @param i_xcount
   * @param i_ycount
   * @param i_zcount
   */
  public WorldFragment(int i_xcount, int i_ycount, int i_zcount)
  {
    initialise(i_xcount, i_ycount, i_zcount);
  }

  private void initialise(int i_xcount, int i_ycount, int i_zcount)
  {
    assert (i_xcount >= 0 && i_xcount <= MAX_X_SIZE);
    assert (i_ycount >= 0 && i_ycount <= MAX_Y_SIZE);
    assert (i_zcount >= 0 && i_zcount <= MAX_Z_SIZE);
    xCount = i_xcount;
    yCount = i_ycount;
    zCount = i_zcount;

    final int DEFAULT_BLOCK_COUNT_ESTIMATE = 16;
    blockDataStore = new BlockDataStoreSparse(xCount, yCount, zCount, DEFAULT_BLOCK_COUNT_ESTIMATE);
    tileEntityData = new HashMap<Integer, NBTTagCompound>();
    entityData = new HashMap<Integer, LinkedList<NBTTagCompound>>();
    voxelsWithStoredData = new VoxelSelection(i_xcount, i_ycount, i_zcount);
  }

  /**
   * gets the blockID at a particular location.
   * error if the location is not stored in this fragment
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   */
  public int getBlockID(int x, int y, int z)
  {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    assert (voxelsWithStoredData.getVoxel(x, y, z));
    return blockDataStore.getBlockID(x, y, z);
  }

  /**
   * sets the BlockID at a particular location
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   */
  public void setBlockID(int x, int y, int z, int blockID)
  {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    assert (blockID >= 0 && blockID <= 0xfff);
    blockDataStore.setBlockID(x, y, z, blockID);
    voxelsWithStoredData.setVoxel(x, y, z);
  }

  /**
   * gets the metadata at a particular location
   * error if the location is not stored in this fragment
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   */
  public int getMetadata(int x, int y, int z)
  {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    assert (voxelsWithStoredData.getVoxel(x, y, z));
    return blockDataStore.getMetadata(x, y, z);
  }

  /**
   * sets the metadata at a particular location
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   */
  public void setMetadata(int x, int y, int z, int metadata)
  {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    assert (metadata >= 0 && metadata <= 0x0f);
    blockDataStore.setMetadata(x, y, z, metadata);
    voxelsWithStoredData.setVoxel(x, y, z);
  }

  /**
   * Adds an entity to the block store, at the given position.
   * error if the location is not stored in this fragment
   * @param x  entity position relative to the block origin [0, 0, 0].
   * @param nbtData NBT data of the entity
   */
  public void addEntity(int x, int y, int z, NBTTagCompound nbtData)
  {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    assert (voxelsWithStoredData.getVoxel(x, y, z));
    assert (nbtData != null);

    final int offset =   y * xCount * zCount
                       + z * xCount
                       + x;
    LinkedList<NBTTagCompound> entitiesAtThisBlock;
    entitiesAtThisBlock = entityData.get(offset);
    if (entitiesAtThisBlock == null) {
      entitiesAtThisBlock = new LinkedList<NBTTagCompound>();
      entityData.put(offset, entitiesAtThisBlock);
    }
    entitiesAtThisBlock.add(nbtData);
  }

  /**
   * Returns a list of all entities whose [x,y,z] lies within the given block
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   * @return
   */
  public LinkedList<NBTTagCompound> getEntitiesAtBlock(int x, int y, int z)
  {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    assert (voxelsWithStoredData.getVoxel(x, y, z));

    final int offset = y * xCount * zCount + z * xCount + x;
    return entityData.get(offset);
  }

  /**
   * returns the NBT data for the TileEntity at the given location, or null if no TileEntity there
   * error if this location is not in the fragment
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   * @return the TileEntity NBT, or null if no TileEntity here
   */
  public NBTTagCompound getTileEntityData(int x, int y, int z)
  {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    assert (voxelsWithStoredData.getVoxel(x, y, z));

    final int offset = y * xCount * zCount + z * xCount + x;
    return tileEntityData.get(offset);
  }

  /**
   * sets the NBT data for the TileEntity at the given location
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   */
  public void setTileEntityData(int x, int y, int z, NBTTagCompound nbtData)
  {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    assert (voxelsWithStoredData.getVoxel(x, y, z));

    final int offset = y * xCount * zCount + z * xCount + x;
    if (nbtData == null) {
      tileEntityData.remove(offset);
    } else {
      tileEntityData.put(offset, nbtData);
    }
  }

  /**
   * gets the light value at a particular location.
   * error if the location is not stored in this fragment
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   * @return lightvalue (sky << 4 | block)
   */
  public byte getLightValue(int x, int y, int z)
  {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    assert (voxelsWithStoredData.getVoxel(x, y, z));
    return blockDataStore.getLightValue(x, y, z);
  }

  /**
   * sets the light value at a particular location
   * @param x x position relative to the block origin [0,0,0]
   * @param y y position relative to the block origin [0,0,0]
   * @param z z position relative to the block origin [0,0,0]
   * @param lightValue lightvalue (sky << 4 | block)
   */
  public void setLightValue(int x, int y, int z, byte lightValue)
  {
    assert (x >= 0 && x < xCount);
    assert (y >= 0 && y < yCount);
    assert (z >= 0 && z < zCount);
    blockDataStore.setLightValue(x, y, z, lightValue);
    voxelsWithStoredData.setVoxel(x, y, z);
  }


  public int getxCount() {
    return xCount;
  }

  public int getyCount() {
    return yCount;
  }

  public int getzCount() {
    return zCount;
  }

  public VoxelSelection getVoxelsWithStoredData() {
    return voxelsWithStoredData;
  }

  public boolean getVoxel(int x, int y, int z)
  {
    return voxelsWithStoredData.getVoxel(x, y, z);
  }

  public void readSingleBlockFromWorld(WorldServer worldServer, int wx, int wy, int wz,
                                       int wxOrigin, int wyOrigin, int wzOrigin)
  {
    QuadOrientation noChange = new QuadOrientation(0, 0, 1, 1);
    readSingleBlockFromWorld(worldServer, wx, wy, wz, wxOrigin, wyOrigin, wzOrigin, noChange);
  }

  /**
   * Read a single [wx, wy, wz] from the world into the fragment
   * @param worldServer
   * @param wx  world x coordinate
   * @param wy world y coordinate
   * @param wz world z coordinate
   * @param wxOrigin the world x coordinate of the [0,0,0] corner of the WorldFragment
   * @param wyOrigin the world y coordinate of the [0,0,0] corner of the WorldFragment
   * @param wzOrigin the world z coordinate of the [0,0,0] corner of the WorldFragment
   * @param orientation the orientation of the fragment (flip, rotate)
   */
  public void readSingleBlockFromWorld(WorldServer worldServer, int wx, int wy, int wz,
                                       int wxOrigin, int wyOrigin, int wzOrigin,
                                       QuadOrientation orientation)
  {
    int x = orientation.calcXfromWXZ(wx - wxOrigin, wz - wzOrigin);
    int y = wy - wyOrigin;
    int z = orientation.calcZfromWXZ(wx - wxOrigin, wz - wzOrigin);

    if (x < 0 || x >= xCount || y < 0 || y >= yCount || z < 0 || z >= zCount ) return;

    int id = worldServer.getBlockId(wx, wy, wz);
    int data = worldServer.getBlockMetadata(wx, wy, wz);
    TileEntity tileEntity = worldServer.getBlockTileEntity(wx, wy, wz);
    NBTTagCompound tileEntityTag = null;
    if (tileEntity != null) {
      tileEntityTag = new NBTTagCompound();
      tileEntity.writeToNBT(tileEntityTag);
    }
    setBlockID(x, y, z, id);
    setMetadata(x, y, z, data);
    setTileEntityData(x, y, z, tileEntityTag);

    Chunk chunk = worldServer.getChunkFromChunkCoords(wx >> 4, wz >> 4);
    int lightValue = 0;
    if (wy >= Y_MIN_VALID && wy < Y_MAX_VALID_PLUS_ONE) {
      lightValue = (chunk.getSavedLightValue(EnumSkyBlock.Sky, wx & 0x0f, wy, wz & 0x0f) << 4)
                  | chunk.getSavedLightValue(EnumSkyBlock.Block, wx & 0x0f, wy, wz & 0x0f);
    }
    setLightValue(x, y, z, (byte)lightValue);

    final double EXPAND = 3;
    AxisAlignedBB axisAlignedBB = AxisAlignedBB.getBoundingBox(wxOrigin, wyOrigin, wzOrigin,
                                                                wxOrigin + xCount, wyOrigin + yCount, wzOrigin + zCount)
                                                                .expand(EXPAND, EXPAND, EXPAND);

    List<EntityHanging> allHangingEntities = worldServer.getEntitiesWithinAABB(EntityHanging.class, axisAlignedBB);

    for (EntityHanging entity : allHangingEntities) {
      if (wx == entity.xPosition && wy == entity.yPosition && wz == entity.zPosition) {
        NBTTagCompound tag = new NBTTagCompound();
        entity.writeToNBTOptional(tag);
        addEntity(x, y, z, tag);
      }
    }
  }

  /**
   * Read a section of the world into the WorldFragment, erasing its existing contents.
   * If the voxel selection is defined, only reads those voxels, otherwise reads the entire block
   * @param worldServer
   * @param wxOrigin the world x coordinate of the [0,0,0] corner of the WorldFragment
   * @param wyOrigin the world y coordinate of the [0,0,0] corner of the WorldFragment
   * @param wzOrigin the world z coordinate of the [0,0,0] corner of the WorldFragment
   * @param voxelSelection the blocks to read, or if null read the entire WorldFragment cuboid
   */
  public void readFromWorld(WorldServer worldServer, int wxOrigin, int wyOrigin, int wzOrigin, VoxelSelection voxelSelection)
  {
    VoxelSelection selection = voxelSelection;
    int setVoxelsCount;
    if (selection == null) {
      selection = new VoxelSelection(xCount, yCount, zCount);
      selection.setAll();
      setVoxelsCount = xCount * yCount * zCount;
    } else {
      setVoxelsCount = selection.getSetVoxelsCount();
    }
    voxelsWithStoredData = new VoxelSelection(xCount, yCount, zCount);   // starts empty, the setBlockID will fill it

    final double THRESHOLD_FILL_FRACTION = 0.25;
    if (setVoxelsCount / (double)(xCount * yCount * zCount) > THRESHOLD_FILL_FRACTION) {
      blockDataStore = new BlockDataStoreArray(xCount, yCount, zCount);
    } else {
      blockDataStore = new BlockDataStoreSparse(xCount, yCount, zCount, setVoxelsCount);
    }

    int yClipMin = Math.max(Y_MIN_VALID, 0 + wyOrigin) - wyOrigin;
    int yClipMaxPlusOne = Math.min(Y_MAX_VALID_PLUS_ONE, yCount + wyOrigin) - wyOrigin;

    for (int y = yClipMin; y < yClipMaxPlusOne; ++y) {
      for (int z = 0; z < zCount; ++z) {
        for (int x = 0; x < xCount; ++x) {
          if (selection.getVoxel(x, y, z)) {
            int wx = x + wxOrigin;
            int wy = y + wyOrigin;
            int wz = z + wzOrigin;
            int id = worldServer.getBlockId(wx, wy, wz);
            int data = worldServer.getBlockMetadata(wx, wy, wz);
            TileEntity tileEntity = worldServer.getBlockTileEntity(wx, wy, wz);
            NBTTagCompound tileEntityTag = null;
            if (tileEntity != null) {
              tileEntityTag = new NBTTagCompound();
              tileEntity.writeToNBT(tileEntityTag);
            }

            Chunk chunk = worldServer.getChunkFromChunkCoords(wx >> 4, wz >> 4);
            int lightValue =   (chunk.getSavedLightValue(EnumSkyBlock.Sky, wx & 0x0f, wy, wz & 0x0f) << 4)
                             | chunk.getSavedLightValue(EnumSkyBlock.Block, wx & 0x0f, wy, wz & 0x0f);
            setBlockID(x, y, z, id);
            setMetadata(x, y, z, data);
            setTileEntityData(x, y, z, tileEntityTag);
            setLightValue(x, y, z, (byte)lightValue);
          }
        }
      }
    }

    final double EXPAND = 3;
    AxisAlignedBB axisAlignedBB = AxisAlignedBB.getBoundingBox(wxOrigin, wyOrigin, wzOrigin,
                                                               wxOrigin + xCount, wyOrigin + yCount, wzOrigin + zCount)
                                               .expand(EXPAND, EXPAND, EXPAND);

    List<EntityHanging> allHangingEntities = worldServer.getEntitiesWithinAABB(EntityHanging.class, axisAlignedBB);

    for (EntityHanging entity : allHangingEntities) {
      int x = entity.xPosition - wxOrigin;
      int y = entity.yPosition - wyOrigin;
      int z = entity.zPosition - wzOrigin;

      if (selection.getVoxel(x, y, z)) {
        NBTTagCompound tag = new NBTTagCompound();
        entity.writeToNBTOptional(tag);
        addEntity(x, y, z, tag);
      }
    }

    return;
  }

  /**
   * Write the WorldFragment to the world
   * If the voxel selection and bordermaskSelection are defined, only writes those voxels, otherwise writes the entire cuboid
   * @param worldServer
   * @param wxOrigin the world x coordinate corresponding to the [0,0,0] corner of the WorldFragment
   * @param wyOrigin the world y coordinate corresponding to the [0,0,0] corner of the WorldFragment
   * @param wzOrigin the world z coordinate corresponding to the [0,0,0] corner of the WorldFragment
   * @param writeMask the blocks to be written; or if null - all valid voxels in the fragment
   */
  public void writeToWorld(WorldServer worldServer, int wxOrigin, int wyOrigin, int wzOrigin,
                           VoxelSelection writeMask)
  {
    QuadOrientation noChange = new QuadOrientation(0, 0, 1, 1);
    writeToWorld(worldServer, wxOrigin, wyOrigin, wzOrigin, writeMask, noChange);
  }


  /**
   * Write the WorldFragment to the world
   * If the voxel selection and bordermaskSelection are defined, only writes those voxels, otherwise writes the entire cuboid
   * @param worldServer
   * @param wxOrigin the world x coordinate corresponding to the [0,0,0] corner of the WorldFragment
   * @param wyOrigin the world y coordinate corresponding to the [0,0,0] corner of the WorldFragment
   * @param wzOrigin the world z coordinate corresponding to the [0,0,0] corner of the WorldFragment
   * @param writeMask the blocks to be written; or if null - all valid voxels in the fragment
   * @param orientation the orientation of the fragment (flip, rotate)
   */
  public void writeToWorld(WorldServer worldServer, int wxOrigin, int wyOrigin, int wzOrigin,
                           VoxelSelection writeMask, QuadOrientation orientation)
  {
    /* the steps are
    10: delete EntityHanging to stop resource leaks / items popping out
    20: copy ID and metadata to chunk directly (chunk setBlockIDwithMetadata without the updating)
    30: create & update TileEntities - setChunkBlockTileEntity, World.addTileEntity
    35: update stored light values
    40: update helper structures - heightmap, lighting
    50: notifyNeighbourChange for all blocks; World.func_96440_m for redstone comparators
    55: queue all changes chunks to resend to client
    57: create any hanging entities
    60: updateTick for all blocks to restart updating (causes Dispensers to dispense, but leave for now)
     */

    VoxelSelection selection = voxelsWithStoredData;

    if (writeMask != null) {
      assert voxelsWithStoredData.containsAllOfThisMask(writeMask);
      selection = writeMask;
    }

    Pair<Integer, Integer> xrange = new Pair<Integer, Integer>(0, xCount - 1);
    Pair<Integer, Integer> zrange = new Pair<Integer, Integer>(0, zCount - 1);
    orientation.getWXZranges(xrange, zrange);
    int wxMin = xrange.getFirst() + wxOrigin;
    int wxMaxPlusOne = xrange.getSecond() + 1 + wxOrigin;
    int wzMin = zrange.getFirst() + wzOrigin;
    int wzMaxPlusOne = zrange.getSecond() + 1 + wzOrigin;

    final double EXPAND = 3;
    AxisAlignedBB axisAlignedBB = AxisAlignedBB.getBoundingBox(wxMin, wyOrigin, wzMin,
            wxMaxPlusOne, wyOrigin + yCount, wzMaxPlusOne)
            .expand(EXPAND, EXPAND, EXPAND);

    List<EntityHanging> allHangingEntities = worldServer.getEntitiesWithinAABB(EntityHanging.class, axisAlignedBB);

    for (EntityHanging entity : allHangingEntities) {
      int x = orientation.calcXfromWXZ(entity.xPosition - wxOrigin, entity.zPosition - wzOrigin);
      int y = entity.yPosition - wyOrigin;
      int z = orientation.calcZfromWXZ(entity.xPosition - wxOrigin, entity.zPosition - wzOrigin);

      if (selection.getVoxel(x, y, z)) {
        entity.setDead();
      }
    }

    //todo: keep track of dirty chunks here
    int yClipMin = Math.max(Y_MIN_VALID, 0 + wyOrigin) - wyOrigin;
    int yClipMaxPlusOne = Math.min(Y_MAX_VALID_PLUS_ONE, yCount + wyOrigin) - wyOrigin;

    for (int y = yClipMin; y < yClipMaxPlusOne; ++y) {
      for (int z = 0; z < zCount; ++z) {
        for (int x = 0; x < xCount; ++x) {
          if (selection.getVoxel(x, y, z)) {
            int wx = orientation.calcWXfromXZ(x, z) + wxOrigin;
            int wy = y + wyOrigin;
            int wz = orientation.calcWZfromXZ(x, z) + wzOrigin;
            int blockID = getBlockID(x, y, z);
            int blockMetadata = getMetadata(x, y, z);
            byte lightValue = getLightValue(x, y, z);
            NBTTagCompound tileEntityNBT = getTileEntityData(x, y, z);

            Chunk chunk = worldServer.getChunkFromChunkCoords(wx >> 4, wz >> 4);

            chunk.removeChunkBlockTileEntity(wx & 0x0f, wy, wz & 0x0f);

            if (orientation.isFlippedX()) {
              blockMetadata = BlockRotateFlipHelper.flip(blockID, blockMetadata, BlockRotateFlipHelper.FlipDirection.WEST_EAST);
            }
            for (int quadrants = orientation.getClockwiseRotationCount(); quadrants > 0; --quadrants) {
              blockMetadata = BlockRotateFlipHelper.rotate90(blockID, blockMetadata);
            }

            boolean successful = setBlockIDWithMetadata(chunk, wx, wy, wz, blockID, blockMetadata);
            if (successful && tileEntityNBT != null) {
              setWorldTileEntity(worldServer, wx, wy, wz, tileEntityNBT);
            }

            setLightValue(chunk, wx, wy, wz, lightValue);
          }
        }
      }
    }

    final int cxMin = wxMin >> 4;
    final int cxMax = (wxMaxPlusOne - 1) >> 4;
    final int cyMin = (yClipMin +  wyOrigin) >> 4;
    final int cyMax = (yClipMaxPlusOne - 1 + wyOrigin) >> 4;
    final int czMin = wzMin >> 4;
    final int czMax = (wzMaxPlusOne - 1) >> 4;

//    int cyFlags = 0;
//    for (int cy = cyMin; cy <= cyMax; ++cy) {
//      cyFlags |= 1 << cy;
//    }

    for (int cx = cxMin; cx <= cxMax; ++cx) {
      for (int cz = czMin; cz <= czMax; ++cz) {
        Chunk chunk = worldServer.getChunkFromChunkCoords(cx, cz);
        chunk.generateHeightMap();
        chunk.generateSkylightMap();
      }
    }

    for (int y = yClipMin; y < yClipMaxPlusOne; ++y) {
      for (int z = 0; z < zCount; ++z) {
        for (int x = 0; x < xCount; ++x) {
          if (selection.getVoxel(x, y, z)) {
            int wx = orientation.calcWXfromXZ(x, z) + wxOrigin;
            int wy = y + wyOrigin;
            int wz = orientation.calcWZfromXZ(x, z) + wzOrigin;
            int blockID = worldServer.getBlockId(wx, wy, wz);
            worldServer.func_96440_m(wx, wy, wz, blockID);

            worldServer.notifyBlockOfNeighborChange(wx - 1, wy, wz, blockID);
            worldServer.notifyBlockOfNeighborChange(wx + 1, wy, wz, blockID);
            worldServer.notifyBlockOfNeighborChange(wx, wy - 1, wz, blockID);
            worldServer.notifyBlockOfNeighborChange(wx, wy + 1, wz, blockID);
            worldServer.notifyBlockOfNeighborChange(wx, wy, wz - 1, blockID);
            worldServer.notifyBlockOfNeighborChange(wx, wy, wz + 1, blockID);
          }
        }
      }
    }

    for (int cx = cxMin; cx <= cxMax; ++cx) {
      for (int cz = czMin; cz <= czMax; ++cz) {
        // mark chunks for "loading" (transfer to any interested player)
        PlayerManager playerManager = worldServer.getPlayerManager();
        if (playerManager != null) {  // may be null during testing
          for (Object playerEntity : worldServer.playerEntities) {
            EntityPlayerMP entityPlayerMP = (EntityPlayerMP)playerEntity;
            if (playerManager.isPlayerWatchingChunk(entityPlayerMP, cx, cz)) {        // todo later: this might be slow because it searches loadedChunks unnecessarily; optimise later
              Chunk chunk = worldServer.getChunkFromChunkCoords(cx, cz);
              ChunkCoordIntPair chunkCoordIntPair = chunk.getChunkCoordIntPair();
              entityPlayerMP.loadedChunks.add(chunkCoordIntPair);                     // a better name for "loadedChunks" would be "dirtyChunksQueuedForSending"
            }
          }
        }

//        int xmin = Math.max(cx << 4, wxOrigin);
//        int ymin = yClipMin + wyOrigin;
//        int zmin = Math.max(cz << 4, wzOrigin);
//        int xmax = Math.min((cx << 4) | 0x0f, wxOrigin + xCount - 1);
//        int ymax = wyOrigin + yClipMaxPlusOne - 1;
//        int zmax = Math.min((cz << 4) | 0x0f, wzOrigin + zCount - 1);

        int xmin = cx << 4;
        int ymin = yClipMin + wyOrigin;
        int zmin = cz << 4;
        int xmax = (cx << 4) | 0x0f;
        int ymax = wyOrigin + yClipMaxPlusOne - 1;
        int zmax = (cz << 4) | 0x0f;

        for (int wx = xmin; wx <= xmax; ++wx) {
          for (int wy = ymin; wy <= ymax; ++wy) {
            for (int wz = zmin; wz <= zmax; ++wz) {
              int x = orientation.calcXfromWXZ(wx - wxOrigin, wz - wzOrigin);
              int y = wy - wyOrigin;
              int z = orientation.calcZfromWXZ(wx - wxOrigin, wz - wzOrigin);
              if (selection.getVoxel(x, y, z)) {
                LinkedList<NBTTagCompound> listOfEntitiesAtThisBlock = getEntitiesAtBlock(x, y, z);
                if (listOfEntitiesAtThisBlock != null) {
                  for (NBTTagCompound nbtTagCompound : listOfEntitiesAtThisBlock) {
                    Entity newEntity = spawnRotatedTranslatedEntity(worldServer, nbtTagCompound, wx, wy, wz, orientation);
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
    }

    for (int y = yClipMin; y < yClipMaxPlusOne; ++y) {
      for (int z = 0; z < zCount; ++z) {
        for (int x = 0; x < xCount; ++x) {
          if (selection.getVoxel(x, y, z)) {
            int wx = orientation.calcWXfromXZ(x, z) + wxOrigin;
            int wy = y + wyOrigin;
            int wz = orientation.calcWZfromXZ(x, z) + wzOrigin;
            int blockID = getBlockID(x, y, z);
            if (blockID > 0) {
              Block.blocksList[blockID].updateTick(worldServer, wx, wy, wz, worldServer.rand);
            }
          }
        }
      }
    }
  }

  /**
   * Update the position information of the given NBT
   * @param nbtTagCompound the NBT
   * @return the updated NBT
   */
  public static NBTTagCompound changeTileEntityNBTposition(NBTTagCompound nbtTagCompound, int wx, int wy, int wz)
  {
    nbtTagCompound.setTag("x", new NBTTagInt("x", wx));
    nbtTagCompound.setTag("y", new NBTTagInt("y", wy));
    nbtTagCompound.setTag("z", new NBTTagInt("z", wz));
    return nbtTagCompound;
  }

  /** spawn the entity after translating, flipping, and rotating it
   *
   * @param nbtTagCompound
   * @param wx
   * @param wy
   * @param wz
   * @param orientation
   * @return
   */
  public Entity spawnRotatedTranslatedEntity(WorldServer worldServer, NBTTagCompound nbtTagCompound, int wx, int wy, int wz, QuadOrientation orientation)
  {
    nbtTagCompound.setInteger("TileX", wx);
    nbtTagCompound.setInteger("TileY", wy);
    nbtTagCompound.setInteger("TileZ", wz);
    Entity newEntity = EntityList.createEntityFromNBT(nbtTagCompound, worldServer);
    if (newEntity instanceof EntityHanging ) {
      EntityHanging newEntityHanging = (EntityHanging)newEntity;
      int direction = newEntityHanging.hangingDirection;

//      System.out.println("Direction:" + direction + "; old [x,y,z]= [" + wx + ", " + wy + ", " + wz + "]");

      if (orientation.isFlippedX()) {
        if (direction == 1 || direction == 3) {   // 0 and 2 are when the painting is parallel to the x axis
          direction ^= 2;
        }
      }
      direction = (direction + orientation.getClockwiseRotationCount()) & 3;

      if (orientation.isFlippedX()) {
        // if a hanging entity is mirrored and is more than 1 block wide, we need to shift the "origin" of the painting to the opposite end of the painting to make sure it stays in the same place
        int originShiftDirection = (newEntityHanging.hangingDirection - orientation.getClockwiseRotationCount()) & 3;
        int paintingOriginShift = newEntityHanging.getWidthPixels() >= 31.9F ? 1 : 0;
        switch (originShiftDirection) {
          case 0: {
            newEntityHanging.xPosition -= paintingOriginShift;
            break;
          }
          case 1: {
            newEntityHanging.zPosition += paintingOriginShift;
            break;
          }
          case 2: {
            newEntityHanging.xPosition += paintingOriginShift;
            break;
          }
          case 3: {
            newEntityHanging.zPosition -= paintingOriginShift;
            break;
          }
        }
//        System.out.println("getWidthPixels = " + newEntityHanging.getWidthPixels() + "; new [x,y,z]= [" + newEntityHanging.xPosition + ", " + newEntityHanging.yPosition + ", " + newEntityHanging.zPosition + "]");
      }
      newEntityHanging.setDirection(direction);
    }
    return newEntity;
  }

  /**
   * Update the position information of the given HangingEntityNBT.  The integer position is changed; the fractional component is unchanged.
   * @param nbtTagCompound the NBT
   * @return the updated NBT
   */
//  public static NBTTagCompound changeHangingEntityNBTposition(NBTTagCompound nbtTagCompound, int wx, int wy, int wz)
//  {
//    int oldPosX = nbtTagCompound.getInteger("TileX");
//    int oldPosY = nbtTagCompound.getInteger("TileY");
//    int oldPosZ = nbtTagCompound.getInteger("TileZ");
//    int deltaX = wx - oldPosX;
//    int deltaY = wy - oldPosY;
//    int deltaZ = wz - oldPosZ;
//
//    NBTTagList nbttaglist = nbtTagCompound.getTagList("Pos");
//    double oldX = ((NBTTagDouble)nbttaglist.tagAt(0)).data;
//    double oldY = ((NBTTagDouble)nbttaglist.tagAt(1)).data;
//    double oldZ = ((NBTTagDouble)nbttaglist.tagAt(2)).data;
//
//    System.out.println("OldPos: ["+ oldX + ", " + oldY + ", " + oldZ + "]"); // todo remove
//
//    double newX = oldX + deltaX;
//    double newY = oldY + deltaY;
//    double newZ = oldZ + deltaZ;
//
//    NBTTagList newPositionNBT = new NBTTagList();
//    newPositionNBT.appendTag(new NBTTagDouble((String) null, newX));
//    newPositionNBT.appendTag(new NBTTagDouble((String) null, newY));
//    newPositionNBT.appendTag(new NBTTagDouble((String) null, newZ));
//    nbtTagCompound.setTag("Pos", newPositionNBT);
//
//    nbtTagCompound.setInteger("TileX", wx);
//    nbtTagCompound.setInteger("TileY", wy);
//    nbtTagCompound.setInteger("TileZ", wz);
//
//    return nbtTagCompound;
//  }

  /**
   * Creates a TileEntity from the given NBT and puts it at the specified world location
   * @param world the world
   * @param nbtTagCompound the NBT for the tile entity (if null - do nothing)
   */
  private void setWorldTileEntity(World world, int wx, int wy, int wz, NBTTagCompound nbtTagCompound) {
    if (nbtTagCompound != null) {
      changeTileEntityNBTposition(nbtTagCompound, wx, wy, wz);
      TileEntity tileEntity = TileEntity.createAndLoadEntity(nbtTagCompound);
      if (tileEntity != null) {
        world.setBlockTileEntity(wx, wy, wz, tileEntity);
      }
    }
  }

  public static boolean setBlockIDWithMetadata(Chunk chunk, int wx, int wy, int wz, int blockID, int metaData)
  {
    int xLSN = wx & 0x0f;
    int yLSN = wy & 0x0f;
    int zLSN = wz & 0x0f;

    ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();
    ExtendedBlockStorage extendedblockstorage = storageArrays[wy >> 4];

    if (extendedblockstorage == null)
    {
      if (blockID == 0) { return false; }
      boolean hasSky = (chunk.worldObj.provider == null) ? true : !chunk.worldObj.provider.hasNoSky;  // testing purposes
      extendedblockstorage =  new ExtendedBlockStorage(wy & ~0x0f, hasSky);
      storageArrays[wy >> 4] = extendedblockstorage;
    }
    extendedblockstorage.setExtBlockID(xLSN, yLSN, zLSN, blockID);
    extendedblockstorage.setExtBlockMetadata(xLSN, yLSN, zLSN, metaData);
    return true;
  }

  public static void setLightValue(Chunk chunk, int wx, int wy, int wz, byte lightValue)
  {
    if (wy < Y_MIN_VALID || wy >= Y_MAX_VALID_PLUS_ONE) return;
    int xLSN = wx & 0x0f;
    int yLSN = wy & 0x0f;
    int zLSN = wz & 0x0f;

    ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();
    ExtendedBlockStorage extendedblockstorage = storageArrays[wy >> 4];
    boolean hasSky = (chunk.worldObj.provider == null) ? true : !chunk.worldObj.provider.hasNoSky;

    if (extendedblockstorage == null)
    {
      extendedblockstorage = new ExtendedBlockStorage(wy & ~0x0f, hasSky);
      storageArrays[wy >> 4] = extendedblockstorage;
    }

    if (hasSky) {
      extendedblockstorage.setExtSkylightValue(xLSN, yLSN, zLSN, (lightValue & 0xf0) >> 4);
    }
    extendedblockstorage.setExtBlocklightValue(xLSN, yLSN, zLSN, lightValue & 0x0f);

  }

  /**
   * Are the contents of this voxel identical in both fragments? (excluding EntityHanging)
   * @param worldFragmentToMatch  the fragment to compare against
   * @param x  x position to compare, for both fragments
   * @param y
   * @param z
   * @return true if they match exactly
   */
  public boolean doesVoxelMatch(WorldFragment worldFragmentToMatch, int x, int y, int z)
  {
    if (this.getBlockID(x, y, z) != worldFragmentToMatch.getBlockID(x, y, z)
            || this.getMetadata(x, y, z) != worldFragmentToMatch.getMetadata(x, y, z) ) {
      return false;
    }
    if (this.getTileEntityData(x, y, z) == null) {
      if (worldFragmentToMatch.getTileEntityData(x, y, z) != null) {
        return false;
      }
    } else {
      NBTTagCompound nbt1 = this.getTileEntityData(x, y, z);
      changeTileEntityNBTposition(nbt1, 0, 0, 0);
      NBTTagCompound nbt2 = worldFragmentToMatch.getTileEntityData(x, y, z);
      changeTileEntityNBTposition(nbt2, 0, 0, 0);
      if (0 != nbt1.toString().compareTo(nbt2.toString()) ) {
        return false;
      }
    }
    return true;
  }

  /**
   * copy the contents of the source voxel into this fragment, at the indicated destination
   * @param xDest
   * @param yDest
   * @param zDest
   * @param sourceFragment
   * @param xSrc
   * @param ySrc
   * @param zSrc
   */
  public void copyVoxelContents(int xDest, int yDest, int zDest,
                                WorldFragment sourceFragment, int xSrc, int ySrc, int zSrc)
  {
    this.setBlockID(xDest, yDest, zDest, sourceFragment.getBlockID(xSrc, ySrc, zSrc));
    this.setMetadata(xDest, yDest, zDest, sourceFragment.getMetadata(xSrc, ySrc, zSrc));
    this.setTileEntityData(xDest, yDest, zDest, sourceFragment.getTileEntityData(xSrc, ySrc, zSrc));
    final int offsetDest =   yDest * xCount * zCount
                            + zDest * xCount
                            + xDest;
    LinkedList<NBTTagCompound> entitiesAtThisBlock;
    entitiesAtThisBlock = sourceFragment.getEntitiesAtBlock(xSrc, ySrc, zSrc);
    entityData.put(offsetDest, entitiesAtThisBlock);
    ;
  }

  /**
   * compares the contents of the two WorldFragments, excluding EntityHanging
   * @param expected
   * @param actual
   * @return true if the contents are exactly the same
   */
  public static boolean areFragmentsEqual(WorldFragment expected, WorldFragment actual)
  {
    if (expected.getxCount() != actual.getxCount()
            || expected.getyCount() != actual.getyCount()
            || expected.getzCount() != actual.getzCount()) {
      lastFailureReason = "Fragment Size Mismatch";
      return false;
    }
    for (int x = 0; x < expected.getxCount(); ++x ) {
      for (int y = 0; y < expected.getyCount(); ++y) {
        for (int z = 0; z < expected.getzCount(); ++z) {
          if (expected.getBlockID(x, y, z) != actual.getBlockID(x, y, z)
                  || expected.getMetadata(x, y, z) != actual.getMetadata(x, y, z) ) {
            if (   (expected.getBlockID(x, y, z) != Block.waterStill.blockID || actual.getBlockID(x, y, z) != Block.waterMoving.blockID) // water changes itself back and forth, ignore
                && (expected.getBlockID(x, y, z) != Block.lavaStill.blockID || actual.getBlockID(x, y, z) != Block.lavaMoving.blockID)) // lava changes itself back and forth, ignore
            {
              lastCompareFailX = x;
              lastCompareFailY = y;
              lastCompareFailZ = z;
              Block temp = Block.blocksList[expected.getBlockID(x, y, z)];
              lastFailureReason = "Name " + ((temp == null) ? "null" : temp.getUnlocalizedName());
              temp = Block.blocksList[actual.getBlockID(x, y, z)];
              lastFailureReason += " vs " + ((temp == null) ? "null" : temp.getUnlocalizedName());
              lastFailureReason += "; ID " + expected.getBlockID(x, y, z) + " vs " + actual.getBlockID(x, y, z);
              lastFailureReason += "; meta " + expected.getMetadata(x, y, z) + " vs " + actual.getMetadata(x, y, z);
              return false;
            }
          }
          if (expected.getTileEntityData(x, y, z) == null) {
            if (actual.getTileEntityData(x, y, z) != null) {
              lastCompareFailX = x; lastCompareFailY = y; lastCompareFailZ = z;
              lastFailureReason = "TileEntity " + expected.getTileEntityData(x, y, z) + " vs " + actual.getTileEntityData(x, y, z);
              return false;
            }
          } else {
            NBTTagCompound nbt1 = expected.getTileEntityData(x, y, z);
            changeTileEntityNBTposition(nbt1, 0, 0, 0);
            NBTTagCompound nbt2 = actual.getTileEntityData(x, y, z);
            changeTileEntityNBTposition(nbt2, 0, 0, 0);
            if (0 != nbt1.toString().compareTo(nbt2.toString()) ) {
              lastCompareFailX = x; lastCompareFailY = y; lastCompareFailZ = z;
              lastFailureReason = "TileEntityNBT " + nbt1.toString() + " vs " + nbt2.toString();
              return false;
            }
          }
        }
      }
    }

    return true;
  }

  public static int lastCompareFailX;    // for testing purposes only
  public static int lastCompareFailY;
  public static int lastCompareFailZ;
  public static String lastFailureReason;

  private int xCount;
  private int yCount;
  private int zCount;

  // data is stored in either the three byte arrays or in sparseData, depending on how much of the fragment is filled with solid blocks
//  private boolean useSparseData;
//  private byte blockIDbits0to7[];                                     // todo make suitable for sparse array
//  private byte blockIDbits8to11andmetaData[];
//  private byte lightValues[];
//  private HashMap<Integer, Integer> sparseData;
  private BlockDataStore blockDataStore;
  private HashMap<Integer, NBTTagCompound> tileEntityData;
  private HashMap<Integer, LinkedList<NBTTagCompound>> entityData;

  private VoxelSelection voxelsWithStoredData;                        // each set voxel corresponds to a block with valid data.
}
