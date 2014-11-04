package speedytools.serverside.worldmanipulation;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import speedytools.common.blocks.BlockWithMetadata;

import java.util.LinkedList;
import java.util.List;

/**
* Created by TheGreyGhost on 8/08/14.
* Wrapper for WorldServer to allow us to transparently read world data from a non-WorldServer
*/
public class WorldServerReaderFill extends WorldServerReader
{
  public WorldServerReaderFill(WorldServer worldServer, BlockWithMetadata i_fillBlock)
  {
    super(worldServer);
    emptyChunk = new EmptyChunk(worldServer, 0, 0);
    if (i_fillBlock == null) {
      fillBlock = new BlockWithMetadata();
      fillBlock.block = Blocks.air;
    } else {
      fillBlock = i_fillBlock;
    }
    blockID = Block.getIdFromBlock(fillBlock.block);
  }

  @Override
  public int getBlockId(int wx, int wy, int wz) {
    return blockID;
  }

  public Chunk getChunkFromChunkCoords(int cx, int cz) {
    return emptyChunk;
  }

  public int getBlockMetadata(int wx, int wy, int wz) {
    return fillBlock.metaData;
  }

  public TileEntity getBlockTileEntity(int wx, int wy, int wz) {
    return null;
  }

  public List getEntitiesWithinAABB(Class par1Class, AxisAlignedBB par2AxisAlignedBB) {
    return emptyList;
  }

  private EmptyChunk emptyChunk;
  private List<Object> emptyList = new LinkedList<Object>();
  private BlockWithMetadata fillBlock;
  private int blockID;
}
