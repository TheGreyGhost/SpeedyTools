package speedytools.common.network;


import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.utilities.ErrorLog;

import java.util.ArrayList;
import java.util.List;

/**
* This class is used to inform the server when the user has used a SpeedyTool, and pass it information about the affected blocks.
*/
public class Packet250SpeedyToolUse extends Packet250Base
{
  public int getToolItemID() {
    return toolItemID;
  }

  public int getButton() {
    return button;
  }

  public BlockWithMetadata getBlockToPlace() {
    return blockToPlace;
  }

  public EnumFacing getSideToPlace() {return sideToPlace;}

  public List<BlockPos> getCurrentlySelectedBlocks() {
    return currentlySelectedBlocks;
  }

  /**
   * Packet sent from client to server, to indicate when the user has used a SpeedyTool
   * @param newButton       - left mouse button (attack) = 0; right mouse button (use) = 1
   * @param i_sideToPlace - the side on which the block is being placed (top, east, etc)
   * @param newCurrentlySelectedBlocks - a list of the blocks selected by the tool when the button was clicked
   */
  public Packet250SpeedyToolUse(int newButton, BlockWithMetadata newBlockToPlace, EnumFacing i_sideToPlace, List<BlockPos> newCurrentlySelectedBlocks)
  {
    super();

//    toolItemID = newToolItemID;
    button = newButton;
    blockToPlace = newBlockToPlace;
    sideToPlace = i_sideToPlace;
    currentlySelectedBlocks = newCurrentlySelectedBlocks;
    packetIsValid = true;
  }

  public Packet250SpeedyToolUse()
  {
    packetIsValid = false;
  }

  /**
   * Register the handler for this packet
   * @param packetHandlerRegistry
   * @param packetHandlerMethod
   * @param side
   */
  public static void registerHandler(PacketHandlerRegistry packetHandlerRegistry,  PacketHandlerMethod packetHandlerMethod, Side side) {
    if (side != Side.SERVER) {
      assert false : "Tried to register Packet250SpeedyToolUse on side " + side;
    }
    serverSideHandler = packetHandlerMethod;
    packetHandlerRegistry.getSimpleNetworkWrapper().registerMessage(ServerMessageHandler.class, Packet250SpeedyToolUse.class,
            Packet250Types.PACKET250_SPEEDY_TOOL_USE_ID.getPacketTypeID(), side);
  }

  public interface PacketHandlerMethod
  {
    public boolean handlePacket(Packet250SpeedyToolUse packet250SpeedyToolUse, MessageContext ctx);
  }

  public static class ServerMessageHandler implements IMessageHandler<Packet250SpeedyToolUse, IMessage>
  {
    /**
     * Called when a message is received of the appropriate type. You can optionally return a reply message, or null if no reply
     * is needed.
     *
     * @param message The message
     * @return an optional return message
     */
    public IMessage onMessage(Packet250SpeedyToolUse message, MessageContext ctx)
    {
      if (serverSideHandler == null) {
        ErrorLog.defaultLog().severe("Packet250SpeedyToolUse received but not registered.");
      } else if (ctx.side != Side.SERVER) {
        ErrorLog.defaultLog().severe("Packet250SpeedyToolUse received on wrong side");
      } else {
        serverSideHandler.handlePacket(message, ctx);
      }
      return null;
    }
  }

  @Override
  public void readFromBuffer(ByteBuf buf) {
    packetIsValid = false;
    try {
      toolItemID = buf.readInt();
      button = buf.readInt();
      int blockID = buf.readInt();

      blockToPlace = new BlockWithMetadata();
      blockToPlace.block = Block.getBlockById(blockID);
      blockToPlace.metaData = buf.readInt();
      int sideToPlaceIndex = buf.readInt();
      sideToPlace = EnumFacing.getFront(sideToPlaceIndex);

      int blockCount = buf.readInt();
      for (int i = 0; i < blockCount; ++i) {
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        BlockPos newCC = new BlockPos(x, y, z);
        currentlySelectedBlocks.add(newCC);
      }
    } catch (IndexOutOfBoundsException ioe) {
      ErrorLog.defaultLog().info("Exception while reading Packet250SpeedyToolUse: " + ioe);
      return;
    }
    if (!checkInvariants()) return;
    packetIsValid = true;
  }

  @Override
  public void writeToBuffer(ByteBuf buf) {
    if (!isPacketIsValid()) return;
    int blockID = Block.getIdFromBlock(blockToPlace == null ? Blocks.air : blockToPlace.block);
    int metaData = (blockToPlace == null) ? 0 : blockToPlace.metaData;

    buf.writeInt(toolItemID);
    buf.writeInt(button);
    buf.writeInt(blockID);
    buf.writeInt(metaData);
    buf.writeInt(sideToPlace.getIndex());
    buf.writeInt(currentlySelectedBlocks.size());

    for (BlockPos cc : currentlySelectedBlocks) {
      buf.writeInt(cc.getX());
      buf.writeInt(cc.getY());
      buf.writeInt(cc.getZ());
    }
  }

  /**
   * Checks if the packet is internally consistent
   * @return true for success, false otherwise
   */
  private boolean checkInvariants()
  {
    if (button != 0 && button != 1) return false;
    if (sideToPlace == null) return false;
    return true;
  }

  private int toolItemID;
  private int button;
  private BlockWithMetadata blockToPlace;
  private EnumFacing sideToPlace;
  private List<BlockPos> currentlySelectedBlocks = new ArrayList<BlockPos>();

  private static PacketHandlerMethod serverSideHandler;

//  private boolean packetIsValid;
}
