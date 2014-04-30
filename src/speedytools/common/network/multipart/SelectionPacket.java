package speedytools.common.network.multipart;

import cpw.mods.fml.relauncher.Side;
import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.clientside.selections.BlockVoxelMultiSelector;
import speedytools.clientside.selections.VoxelSelection;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.PacketHandlerRegistry;
import speedytools.common.utilities.ErrorLog;

import java.io.*;

/**
 * User: The Grey Ghost
 * Date: 28/03/14
 *
 *
 * Used to transmit a VoxelSelection
 * Basic usage:
 * On the client side (the sender)
 * (1) createSenderPacket to create a SelectionPacket from the voxel selection
 * (2) Transmit the SelectionPacket as per MultipartPacket
 * On the server side (the receiver)
 * (1) give SelectionPacketCreator to the MultipartPacket factory
 * (1) createReceiverPacket to create the packet from the incoming Packet250
 * (2) continue receiving the SelectionPacket as per MultipartPacket
 * (3) once complete, retrieveVoxelSelection to extract the VoxelSelection
 */
public class SelectionPacket extends MultipartPacket
{
  public static final int SEGMENT_SIZE = 32000;

  /**
   * Creates a new SelectionPacket for the supplied selection
   * @param selection
   * @return the new SelectionPacket, or null for failure
   */
  public static SelectionPacket createSenderPacket(BlockVoxelMultiSelector selection)
  {
    SelectionPacket newPacket = new SelectionPacket(PacketHandlerRegistry.CHANNEL_NAME, Side.CLIENT, Packet250Types.PACKET250_SELECTION_PACKET.getPacketTypeID(), SEGMENT_SIZE);
    ByteArrayOutputStream bos = selection.writeToBytes();
    if (bos == null) return null;
    newPacket.setRawData(bos.toByteArray());
    return newPacket;
  }

  public static SelectionPacket createReceiverPacket(Packet250CustomPayload packet)
  {
    SelectionPacket newPacket;
    try {
      newPacket = new SelectionPacket(packet);
      newPacket.processIncomingPacket(packet);

      return newPacket;
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Failed to createReceiverPacket, due to exception " + ioe.toString());
      return null;
    }
  }

  /**
   * Once the packet is completely received, can be used to extract the VoxelSelection from it
   * @return the received VoxelSelection, or null if a problem
   */
  public VoxelSelection retrieveVoxelSelection()
  {
    byte [] rawDataCopy = getRawDataCopy();
    if (rawDataCopy == null) return null;
    VoxelSelection voxelSelection = new VoxelSelection(1,1,1);
    ByteArrayInputStream inputStream = new ByteArrayInputStream(rawDataCopy);
    boolean success = voxelSelection.readFromBytes(inputStream);
    return success ? voxelSelection : null;
  }

  protected SelectionPacket(Packet250CustomPayload packet) throws IOException
  {
    super(packet);
  }

  protected SelectionPacket(String i_channel, Side whichSideAmIOn, byte i_packet250CustomPayloadID, int i_segmentSize)
  {
    super(i_channel, whichSideAmIOn, i_packet250CustomPayloadID, i_segmentSize);
  }

  // derived classes should implement this interface so that callers wishing to create a new MultipartPacket (in response to an incoming packet)
  //    can pass this object to the packet handler, which will invoke it to create the SelectionPacket
  public static class SelectionPacketCreator implements MultipartPacketCreator
  {
    public MultipartPacket createNewPacket(Packet250CustomPayload packet)
    {
      return createReceiverPacket(packet);
    }
  }
}
