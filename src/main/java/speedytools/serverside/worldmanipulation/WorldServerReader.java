package speedytools.serverside.worldmanipulation;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.List;

/**
* Created by TheGreyGhost on 8/08/14.
* Wrapper for WorldServer to allow us to transparently read world data from a non-WorldServer
*/
public class WorldServerReader
{
  public WorldServerReader(WorldServer i_worldServer) {
    worldServer = i_worldServer;
  }

  public int getBlockId(int wx, int wy, int wz) {
    Block block = worldServer.getBlockState(new BlockPos(wx, wy, wz)).getBlock();
    return Block.getIdFromBlock(block);
  }

  public Chunk getChunkFromChunkCoords(int cx, int cz) {
    return worldServer.getChunkFromChunkCoords(cx, cz);
  }

  public int getBlockMetadata(int wx, int wy, int wz) {
    IBlockState iBlockState = worldServer.getBlockState(new BlockPos(wx, wy, wz));
    return iBlockState.getBlock().getMetaFromState(iBlockState);
  }

  public TileEntity getBlockTileEntity(int wx, int wy, int wz) {
    return worldServer.getTileEntity(new BlockPos(wx, wy, wz));
  }

  public List getEntitiesWithinAABB(Class par1Class, AxisAlignedBB par2AxisAlignedBB) {
    return worldServer.getEntitiesWithinAABB(par1Class, par2AxisAlignedBB);
  }

  /**
   * Get a list of all the blocks within the given area that are scheduled for ticking
   * @param structureBoundingBox cuboid region - NB all corner points are inclusive!
   * @return list of ticking information, empty list for none
   */

  public List<NextTickListEntry> getTickingBlocks(StructureBoundingBox structureBoundingBox)
  {
    return worldServer.func_175712_a(structureBoundingBox, false);
  }

  private WorldServer worldServer;
}
