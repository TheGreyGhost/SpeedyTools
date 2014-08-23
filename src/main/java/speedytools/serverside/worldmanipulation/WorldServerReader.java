package speedytools.serverside.worldmanipulation;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

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
    return worldServer.getBlockId(wx, wy, wz);
  }

  public Chunk getChunkFromChunkCoords(int cx, int cz) {
    return worldServer.getChunkFromChunkCoords(cx, cz);
  }

  public int getBlockMetadata(int wx, int wy, int wz) {
    return worldServer.getBlockMetadata(wx, wy, wz);
  }

  public TileEntity getBlockTileEntity(int wx, int wy, int wz) {
    return worldServer.getBlockTileEntity(wx, wy, wz);
  }

  public List getEntitiesWithinAABB(Class par1Class, AxisAlignedBB par2AxisAlignedBB) {
    return worldServer.getEntitiesWithinAABB(par1Class, par2AxisAlignedBB);
  }

  private WorldServer worldServer;
}
