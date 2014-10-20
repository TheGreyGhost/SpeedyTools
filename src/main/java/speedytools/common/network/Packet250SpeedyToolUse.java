package speedytools.common.network;


import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChunkCoordinates;
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

  public List<ChunkCoordinates> getCurrentlySelectedBlocks() {
    return currentlySelectedBlocks;
  }

  /**
   * Packet sent from client to server, to indicate when the user has used a SpeedyTool
   * @param newButton       - left mouse button (attack) = 0; right mouse button (use) = 1
   * @param newCurrentlySelectedBlocks - a list of the blocks selected by the tool when the button was clicked
   */
  public Packet250SpeedyToolUse(int newButton, BlockWithMetadata newBlockToPlace, List<ChunkCoordinates> newCurrentlySelectedBlocks)
  {
    super();

//    toolItemID = newToolItemID;
    button = newButton;
    blockToPlace = newBlockToPlace;
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

      int blockCount = buf.readInt();
      for (int i = 0; i < blockCount; ++i) {
        ChunkCoordinates newCC = new ChunkCoordinates();
        newCC.posX = buf.readInt();
        newCC.posY = buf.readInt();
        newCC.posZ = buf.readInt();
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
    buf.writeInt(currentlySelectedBlocks.size());

    for (ChunkCoordinates cc : currentlySelectedBlocks) {
      buf.writeInt(cc.posX);
      buf.writeInt(cc.posY);
      buf.writeInt(cc.posZ);
    }
  }

  /**
   * Checks if the packet is internally consistent
   * @return true for success, false otherwise
   */
  private boolean checkInvariants()
  {
    if (button != 0 && button != 1) return false;
    return true;
  }

  private int toolItemID;
  private int button;
  private BlockWithMetadata blockToPlace;
  private List<ChunkCoordinates> currentlySelectedBlocks = new ArrayList<ChunkCoordinates>();

  private static PacketHandlerMethod serverSideHandler;

//  private boolean packetIsValid;
}
