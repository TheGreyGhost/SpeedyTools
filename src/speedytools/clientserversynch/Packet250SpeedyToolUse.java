package speedytools.clientserversynch;


import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.util.ChunkCoordinates;

import java.io.*;
import java.util.List;

/**
 * This class is used to inform the server when the user has used a SpeedyTool.
 */
public class Packet250SpeedyToolUse extends Packet250CustomPayload
{
  public int getToolItemID() {
    return toolItemID;
  }

  public int getButton() {
    return button;
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
  public Packet250SpeedyToolUse(int newToolItemID, int newButton, List<ChunkCoordinates> newCurrentlySelectedBlocks) throws IOException
  {
    super();

    toolItemID = newToolItemID;
    button = newButton;
    currentlySelectedBlocks = newCurrentlySelectedBlocks;

    ByteArrayOutputStream bos = new ByteArrayOutputStream(13 + 12 * currentlySelectedBlocks.size());
    DataOutputStream outputStream = new DataOutputStream(bos);
    outputStream.writeByte(PacketHandler.PACKET250_SPEEDY_TOOL_USE_ID);
    outputStream.writeInt(toolItemID);
    outputStream.writeInt(button);
    outputStream.writeInt(currentlySelectedBlocks.size());

    for (ChunkCoordinates cc : currentlySelectedBlocks) {
      outputStream.writeInt(cc.posX);
      outputStream.writeInt(cc.posY);
      outputStream.writeInt(cc.posZ);
    }

    this.channel = "SpeedyTools";
    this.data = bos.toByteArray();
    this.length = bos.size();
    assert(this.length < 32768);  //"Payload may not be larger than 32k"
  }

  /**
   * Converts the Packet250CustomPayload to Packet250SpeedyToolUse
   * @param sourcePacket250
   * @return the converted packet, or null if failure
   */
  static public Packet250SpeedyToolUse convertPacket(Packet250CustomPayload sourcePacket250)
  {
    Packet250SpeedyToolUse newPacket = new Packet250SpeedyToolUse(sourcePacket250);
    DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(newPacket.data));

    try {
      byte packetID = inputStream.readByte();
      if (packetID != PacketHandler.PACKET250_SPEEDY_TOOL_USE_ID) return null;

      newPacket.toolItemID = inputStream.readInt();
      newPacket.button = inputStream.readInt();

      int blockCount = inputStream.readInt();
      for (int i = 0; i < blockCount; ++i) {
        ChunkCoordinates newCC = new ChunkCoordinates();
        newCC.posX = inputStream.readInt();
        newCC.posY = inputStream.readInt();
        newCC.posZ = inputStream.readInt();
      }
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    return newPacket;
  }

  protected Packet250SpeedyToolUse(Packet250CustomPayload sourcePacket250)
  {
    super(sourcePacket250.channel, sourcePacket250.data);
  }

  private int toolItemID;
  private int button;
  private List<ChunkCoordinates> currentlySelectedBlocks;

}
