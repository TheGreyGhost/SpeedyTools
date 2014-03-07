package speedytools.common.network;


import net.minecraft.block.Block;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.util.ChunkCoordinates;
import speedytools.common.blocks.BlockWithMetadata;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to inform the server when the user has used a SpeedyTool, and pass it information about the affected blocks.
 */
public class Packet250SpeedyToolUse
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
   * @param newToolItemID   - the item of the speedy tool used
   * @param newButton       - left mouse button (attack) = 0; right mouse button (use) = 1
   * @param newCurrentlySelectedBlocks - a list of the blocks selected by the tool when the button was clicked
   */
  public Packet250SpeedyToolUse(int newToolItemID, int newButton, BlockWithMetadata newBlockToPlace, List<ChunkCoordinates> newCurrentlySelectedBlocks) throws IOException
  {
    super();

    toolItemID = newToolItemID;
    button = newButton;
    blockToPlace = newBlockToPlace;
    currentlySelectedBlocks = newCurrentlySelectedBlocks;

    int blockID = (blockToPlace == null) ? 0 : blockToPlace.block.blockID;
    int metaData = (blockToPlace == null) ? 0 : blockToPlace.metaData;

    ByteArrayOutputStream bos = new ByteArrayOutputStream(1+ 4*5 + 12 * currentlySelectedBlocks.size());
    DataOutputStream outputStream = new DataOutputStream(bos);
    outputStream.writeByte(PacketHandler.PACKET250_SPEEDY_TOOL_USE_ID);
    outputStream.writeInt(toolItemID);
    outputStream.writeInt(button);
    outputStream.writeInt(blockID);
    outputStream.writeInt(metaData);
    outputStream.writeInt(currentlySelectedBlocks.size());

    for (ChunkCoordinates cc : currentlySelectedBlocks) {
      outputStream.writeInt(cc.posX);
      outputStream.writeInt(cc.posY);
      outputStream.writeInt(cc.posZ);
    }
    packet250 = new Packet250CustomPayload("speedytools",bos.toByteArray());
  }

  public Packet250CustomPayload getPacket250CustomPayload() {
    return packet250;
  }

  /**
   * Creates a Packet250SpeedyToolUse from Packet250CustomPayload
   * @param sourcePacket250
   * @return the converted packet, or null if failure
   */
  public Packet250SpeedyToolUse(Packet250CustomPayload sourcePacket250)
  {
    packet250 = sourcePacket250;
    DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet250.data));

    try {
      byte packetID = inputStream.readByte();
      if (packetID != PacketHandler.PACKET250_SPEEDY_TOOL_USE_ID) return;

      toolItemID = inputStream.readInt();
      button = inputStream.readInt();
      int blockID = inputStream.readInt();

      blockToPlace = new BlockWithMetadata();
      blockToPlace.block = (blockID == 0) ? null : Block.blocksList[blockID];
      blockToPlace.metaData = inputStream.readInt();

      int blockCount = inputStream.readInt();
      for (int i = 0; i < blockCount; ++i) {
        ChunkCoordinates newCC = new ChunkCoordinates();
        newCC.posX = inputStream.readInt();
        newCC.posY = inputStream.readInt();
        newCC.posZ = inputStream.readInt();
        currentlySelectedBlocks.add(newCC);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private int toolItemID;
  private int button;
  private BlockWithMetadata blockToPlace;
  private List<ChunkCoordinates> currentlySelectedBlocks = new ArrayList<ChunkCoordinates>();
  private Packet250CustomPayload packet250 = null;
}
