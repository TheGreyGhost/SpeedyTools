package speedytools.common.network;


import cpw.mods.fml.relauncher.Side;
import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.utilities.ErrorLog;

import java.io.*;

/**
 * This class is used by the server to acknowledge actions from the client
 */
public class Packet250CloneToolAcknowledge
{
  public Packet250CloneToolAcknowledge(Acknowledgement i_actionStatus, int i_actionSequenceNumber,
                                       Acknowledgement i_undoStatus, int i_undoSequenceNumber)
  {
    actionAcknowledgement = i_actionStatus;
    actionSequenceNumber = i_actionSequenceNumber;
    undoAcknowledgement = i_undoStatus;
    undoSequenceNumber = i_undoSequenceNumber;
  }

  public Packet250CustomPayload getPacket250CustomPayload()
  {
    checkInvariants();
    Packet250CustomPayload retval = null;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      DataOutputStream outputStream = new DataOutputStream(bos);
      outputStream.writeByte(Packet250Types.PACKET250_TOOL_ACKNOWLEDGE_ID.getPacketTypeID());
      outputStream.writeByte(acknowledgementToByte(actionAcknowledgement));
      outputStream.writeInt(actionSequenceNumber);
      outputStream.writeByte(acknowledgementToByte(undoAcknowledgement));
      outputStream.writeInt(undoSequenceNumber);
      retval = new Packet250CustomPayload("speedytools",bos.toByteArray());
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Failed to getPacket250CustomPayload, due to exception " + ioe.toString());
      return null;
    }

    return retval;
  }

  /**
   * Creates a Packet250CloneToolAcknowledge from Packet250CustomPayload
   * @param sourcePacket250
   * @return the new packet for success, or null for failure
   */
  public static Packet250CloneToolAcknowledge createPacket250CloneToolAcknowledge(Packet250CustomPayload sourcePacket250)
  {
    try {
      DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(sourcePacket250.data));

      byte packetID = inputStream.readByte();
      if (packetID != Packet250Types.PACKET250_TOOL_ACKNOWLEDGE_ID.getPacketTypeID()) return null;
      Packet250CloneToolAcknowledge newPacket = new Packet250CloneToolAcknowledge();
      newPacket.actionAcknowledgement = byteToAcknowledgement(inputStream.readByte());
      newPacket.actionSequenceNumber = inputStream.readInt();
      newPacket.undoAcknowledgement = byteToAcknowledgement(inputStream.readByte());
      newPacket.undoSequenceNumber = inputStream.readInt();
      if (newPacket.checkInvariants()) return newPacket;
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Exception while reading Packet250SpeedyToolUse: " + ioe);
    }
    return null;
  }

  /**
   * Is this packet valid to be received and acted on by the given side?
   * @param whichSide
   * @return true if yes
   */
  public boolean validForSide(Side whichSide)
  {
    assert(checkInvariants());
    return (whichSide == Side.CLIENT);
  }

  public Acknowledgement getActionAcknowledgement() {
    return actionAcknowledgement;
  }

  public int getActionSequenceNumber() {
    return actionSequenceNumber;
  }

  public Acknowledgement getUndoAcknowledgement() {
    return undoAcknowledgement;
  }

  public int getUndoSequenceNumber() {
    return undoSequenceNumber;
  }

  private Packet250CloneToolAcknowledge() {}

  private static Acknowledgement byteToAcknowledgement(byte value)
  {
    if (value < 0 || value >= Acknowledgement.allValues.length) return null;
    return Acknowledgement.allValues[value];
  }

  private static byte acknowledgementToByte(Acknowledgement value) throws IOException
  {
    byte retval;

    if (value != null) {
      for (retval = 0; retval < Acknowledgement.allValues.length; ++retval) {
        if (Acknowledgement.allValues[retval] == value) return retval;
      }
    }
    throw new IOException("Invalid Acknowledgement value");
  }

  /**
   * checks this class to see if it is internally consistent
   * @return true if ok, false if bad.
   */
  private boolean checkInvariants()
  {
    if (actionAcknowledgement == null || undoAcknowledgement == null) return false;
    return true;
  }

  public enum Acknowledgement {
    NOUPDATE, REJECTED, ACCEPTED, COMPLETED;
    public static final Acknowledgement[] allValues = {NOUPDATE, REJECTED, ACCEPTED, COMPLETED};
  }

  private Acknowledgement actionAcknowledgement;
  private int actionSequenceNumber;
  private Acknowledgement undoAcknowledgement;
  private int undoSequenceNumber;


}
