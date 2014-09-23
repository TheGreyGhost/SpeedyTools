package speedytools.common.network.multipart;

import cpw.mods.fml.relauncher.Side;
import speedytools.common.selections.BlockVoxelMultiSelector;
import speedytools.common.SpeedyToolsOptions;
import speedytools.common.network.Packet250Types;
import speedytools.common.selections.VoxelSelectionWithOrigin;
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
  /**
   * Creates a new SelectionPacket for the supplied selection
   * @param selection
   * @return the new SelectionPacket, or null for failure
   */
  public static SelectionPacket createSenderPacket(BlockVoxelMultiSelector selection)
  {
    final int SEGMENT_SIZE = SpeedyToolsOptions.getSelectionPacketFragmentSize();

    SelectionPacket newPacket = new SelectionPacket(Packet250Types.PACKET250_SELECTION_PACKET, Side.CLIENT, SEGMENT_SIZE);
    ByteArrayOutputStream bos = selection.writeToBytes();
    if (bos == null) return null;
    newPacket.setRawData(bos.toByteArray());
    return newPacket;
  }

  public static SelectionPacket createReceiverPacket(Packet250MultipartSegment packet)
  {
    SelectionPacket newPacket;
    try {
      newPacket = new SelectionPacket(packet);
      newPacket.processIncomingSegment(packet);

      return newPacket;
    } catch (IOException ioe) {
      ErrorLog.defaultLog().info("Failed to createReceiverPacket, due to exception " + ioe.toString());
      return null;
    }
  }

  /**
   * Once the packet is completely received, can be used to extract the VoxelSelection from it
   * @return the received VoxelSelection, or null if a problem
   */
  public VoxelSelectionWithOrigin retrieveVoxelSelection()
  {
    byte [] rawDataCopy = getRawDataCopy();
    if (rawDataCopy == null) return null;
    VoxelSelectionWithOrigin voxelSelection = new VoxelSelectionWithOrigin(0, 0, 0, 1, 1, 1);
    ByteArrayInputStream inputStream = new ByteArrayInputStream(rawDataCopy);
    boolean success = voxelSelection.readFromBytes(inputStream);
    return success ? voxelSelection : null;
  }

  /**
   * Create a selection packet from in incoming segment
   * @param packet
   * @throws IOException
   */
  protected SelectionPacket(Packet250MultipartSegment packet) throws IOException
  {
    super(packet);
  }

  /**
   * Create a selection packet to be sent to a receiver
   * @param whichSideAmIOn
   * @param i_packet250Type
   * @param i_segmentSize
   */
  protected SelectionPacket(Packet250Types i_packet250Type, Side whichSideAmIOn, int i_segmentSize)
  {
    super(i_packet250Type, whichSideAmIOn, i_segmentSize);
  }

  // derived classes should implement this interface so that callers wishing to create a new MultipartPacket (in response to an incoming packet)
  //    can pass this object to the packet handler, which will invoke it to create the SelectionPacket
  public static class SelectionPacketCreator implements MultipartPacketCreator
  {
    public MultipartPacket createNewPacket(Packet250MultipartSegment packet)
    {
      return createReceiverPacket(packet);
    }
  }
}
