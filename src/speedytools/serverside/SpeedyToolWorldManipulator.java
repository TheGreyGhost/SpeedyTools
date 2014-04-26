package speedytools.serverside;

import cpw.mods.fml.common.IPlayerTracker;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.network.ClientStatus;
import speedytools.common.network.Packet250SpeedyToolUse;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.PacketHandlerRegistry;

import java.util.*;

/**
 * User: The Grey Ghost
 * Date: 26/11/13
 * Makes changes to the world in response to player usage of the SpeedyTools, in particular
 *   placing blocks, saving and restoring undo information.
 */
public class SpeedyToolWorldManipulator
{
  private static final int MAXIMUM_UNDO_COUNT = 5;

  public SpeedyToolWorldManipulator(PacketHandlerRegistry packetHandlerRegistry)
  {
    packetHandlerSpeedyToolUse = this.new PacketHandlerSpeedyToolUse();
    packetHandlerRegistry.registerHandlerMethod(Side.SERVER, Packet250Types.PACKET250_SPEEDY_TOOL_USE_ID.getPacketTypeID(), packetHandlerSpeedyToolUse);
    playerTracker = new PlayerTracker();
    GameRegistry.registerPlayerTracker(playerTracker);
  }

  public void addPlayer(EntityPlayerMP newPlayer)
  {
    undoHistories.put(newPlayer, new UndoHistory());
  }

  public void removePlayer(EntityPlayerMP whichPlayer)
  {
    undoHistories.remove(whichPlayer);
  }

  /**
   * Performs a server Speedy Tools action in response to an incoming packet from the client: either place or undo
   * @param entityPlayerMP the user sending the packet
   * @param toolItemID the ID of the tool performing this action
   * @param buttonClicked 0 = left (undo), 1 = right (place)
   * @param blockToPlace the Block and metadata to fill the selection with (buttonClicked = 1 only)
   * @param blockSelection the blocks in the selection to be filled (buttonClicked = 1 only)
   */
  public void performServerAction(EntityPlayerMP entityPlayerMP, int toolItemID, int buttonClicked, BlockWithMetadata blockToPlace, List<ChunkCoordinates> blockSelection)
  {
//    System.out.println("performServerAction: ID, button = " + toolItemID + ", " + buttonClicked);

    switch (buttonClicked) {
      case 0: {
        UndoHistory undoHistory = undoHistories.get(entityPlayerMP.username);
        if (undoHistory == null) return;
        if (undoHistory.undoEntries.isEmpty()) return;
        undoLastFill(entityPlayerMP, undoHistory.undoEntries.removeLast());
        return;
      }
      case 1: {
        UndoEntry undoEntry = fillBlockSelection(entityPlayerMP, blockToPlace, blockSelection);
        if (undoEntry == null) return;
        UndoHistory undoHistory = undoHistories.get(entityPlayerMP.username);
        undoHistory.undoEntries.addLast(undoEntry);
        if (undoHistory.undoEntries.size() > MAXIMUM_UNDO_COUNT) {
          undoHistory.undoEntries.removeFirst();
        }
        return;
      }
      default: {
        return;
      }

    }
  }

  /**
   * Fills the blocks in the selection with the given block & metadata information.  Creates undo information.
   * @param entityPlayerMP the player
   * @param blockToPlace the block and metadata to fill the blockSelection with. must not be null.  (air is blockToPlace.block == null)
   * @param blockSelection the blocks to be filled
   * @return the undo information necessary to undo the placement; null if no blocks placed
   */
  protected UndoEntry fillBlockSelection(EntityPlayerMP entityPlayerMP, BlockWithMetadata blockToPlace, List<ChunkCoordinates> blockSelection)
  {
    if (blockSelection == null || blockSelection.isEmpty()) return null;
    UndoEntry retval = createUndoInformation(entityPlayerMP.theItemInWorldManager.theWorld, blockSelection);
    for (ChunkCoordinates cc : blockSelection) {
      if (blockToPlace.block == null) {
        entityPlayerMP.theItemInWorldManager.theWorld.setBlockToAir(cc.posX, cc.posY, cc.posZ);
      } else {
        entityPlayerMP.theItemInWorldManager.theWorld.setBlock(cc.posX, cc.posY, cc.posZ, blockToPlace.block.blockID, blockToPlace.metaData, 1+2);
      }
    }
    return retval;
  }

  /**
   * undoes the last fill, including TileEntities
   * @param undoEntry the undo information from the last fill
   */
  protected void undoLastFill(EntityPlayerMP entityPlayerMP, SpeedyToolWorldManipulator.UndoEntry undoEntry)
  {
    World world = entityPlayerMP.theItemInWorldManager.theWorld;

    for (UndoBlock ub : undoEntry.undoBlocks) {
      world.setBlock(ub.blockCoordinate.posX, ub.blockCoordinate.posY, ub.blockCoordinate.posZ,
              ub.block == null ? 0 : ub.block.blockID,
              ub.metaData,
              1+2);
      if (ub.tileEntityNBTdata != null) {
        TileEntity tileentity = TileEntity.createAndLoadEntity(ub.tileEntityNBTdata);
        if (tileentity != null)
        {
          world.setBlockTileEntity(ub.blockCoordinate.posX, ub.blockCoordinate.posY, ub.blockCoordinate.posZ, tileentity);
        }
      }
    }
  }

  /**
   * used to store sufficient information about the current blocks in blockSelection to allow for them to be
   *   recreated
   * @param world
   * @param blockSelection the blocks to be saved
   * @return a list of information about each block in the selection
   */
  protected UndoEntry createUndoInformation(World world, List<ChunkCoordinates> blockSelection)
  {
    UndoEntry retval = new UndoEntry();
    for (ChunkCoordinates cc : blockSelection) {
      UndoBlock nextBlock = new UndoBlock();
      nextBlock.blockCoordinate = cc;
      int blockID = world.getBlockId(cc.posX, cc.posY, cc.posZ);
      nextBlock.block = (blockID == 0 ? null : Block.blocksList[blockID]);
      nextBlock.metaData = world.getBlockMetadata(cc.posX, cc.posY, cc.posZ);

      TileEntity tileEntity = world.getBlockTileEntity(cc.posX, cc.posY, cc.posZ);
      if (tileEntity != null) {
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        tileEntity.writeToNBT(nbtTagCompound);
        nextBlock.tileEntityNBTdata = nbtTagCompound;
      }
      retval.undoBlocks.add(nextBlock);
    }
    return retval;
  }

  public class PacketHandlerSpeedyToolUse implements PacketHandlerRegistry.PacketHandlerMethod {
    public boolean handlePacket(EntityPlayer player, Packet250CustomPayload packet)
    {
      Packet250SpeedyToolUse toolUsePacket = Packet250SpeedyToolUse.createPacket250SpeedyToolUse(packet);
      if (toolUsePacket == null) return false;
      SpeedyToolWorldManipulator.this.performServerAction((EntityPlayerMP) player, toolUsePacket.getToolItemID(), toolUsePacket.getButton(),
              toolUsePacket.getBlockToPlace(), toolUsePacket.getCurrentlySelectedBlocks());
      return true;
    }
  }

  private PacketHandlerSpeedyToolUse packetHandlerSpeedyToolUse;

  // undo information about a single block
  protected static class UndoBlock
  {
    public ChunkCoordinates blockCoordinate;
    public Block block;
    public int metaData;
    public NBTTagCompound tileEntityNBTdata;
  }

  // a list of blocks corresponding to the single user click
  protected static class UndoEntry
  {
    public List<UndoBlock> undoBlocks = new ArrayList<UndoBlock>();
  }

  // the undo history for a single player
  protected static class UndoHistory
  {
    public Deque<UndoEntry> undoEntries = new LinkedList<UndoEntry>();
  }

  // the undo history for all players
  protected Map<EntityPlayerMP, UndoHistory> undoHistories = new HashMap<EntityPlayerMP, UndoHistory>();

  PlayerTracker playerTracker;
  private class PlayerTracker implements IPlayerTracker
  {
    public void onPlayerLogin(EntityPlayer player)
    {
      EntityPlayerMP entityPlayerMP = (EntityPlayerMP)player;
      SpeedyToolWorldManipulator.this.addPlayer(entityPlayerMP);
    }
    public void onPlayerLogout(EntityPlayer player)
    {
      EntityPlayerMP entityPlayerMP = (EntityPlayerMP)player;
      SpeedyToolWorldManipulator.this.removePlayer(entityPlayerMP);
    }
    public void onPlayerChangedDimension(EntityPlayer player) {}
    public void onPlayerRespawn(EntityPlayer player) {}
  }

}
