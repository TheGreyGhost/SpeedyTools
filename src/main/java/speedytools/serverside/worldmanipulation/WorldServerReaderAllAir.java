//package speedytools.serverside.worldmanipulation;
//
//import net.minecraft.tileentity.TileEntity;
//import net.minecraft.util.AxisAlignedBB;
//import net.minecraft.world.WorldServer;
//import net.minecraft.world.chunk.Chunk;
//import net.minecraft.world.chunk.EmptyChunk;
//
//import java.util.LinkedList;
//import java.util.List;
//
///**
// * Created by TheGreyGhost on 8/08/14.
// * Wrapper for WorldServer to allow us to transparently read world data from a non-WorldServer
// */
//public class WorldServerReaderAllAir extends WorldServerReader
//{
//  public WorldServerReaderAllAir(WorldServer worldServer)
//  {
//    super(worldServer);
//    emptyChunk = new EmptyChunk(worldServer, 0, 0);
//  }
//
//  @Override
//  public int getBlockId(int wx, int wy, int wz) {
//    return 0;
//  }
//
//  public Chunk getChunkFromChunkCoords(int cx, int cz) {
//    return emptyChunk;
//  }
//
//  public int getBlockMetadata(int wx, int wy, int wz) {
//    return 0;
//  }
//
//  public TileEntity getBlockTileEntity(int wx, int wy, int wz) {
//    return null;
//  }
//
//  public List getEntitiesWithinAABB(Class par1Class, AxisAlignedBB par2AxisAlignedBB) {
//    return emptyList;
//  }
//
//  private EmptyChunk emptyChunk;
//  List<Object> emptyList = new LinkedList<Object>();
//}
