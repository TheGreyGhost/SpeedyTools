package speedytools.common.network;


import cpw.mods.fml.relauncher.Side;
import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.utilities.ErrorLog;

import java.io.*;

/**
 * This class is used to communicate actions from the client to the server for clone use
 * Client to Server:
 * (1) when user has made a selection using a clone tool
 * (2) user has performed an action with the tool (place) at the current selection position
 * (3) user has performed an undo with the tool
 * Server to Client:
 * (1) The packet received by the server, updated with the success status of the action, is returned to the client.
 */
public class Packet250CloneToolUse
{

  public static Packet250CloneToolUse toolSelectionPerformed()
  {
    Packet250CloneToolUse retval = new Packet250CloneToolUse(Command.SELECTION_MADE);
    assert (retval.checkInvariants());
    return retval;
  }

  public static Packet250CloneToolUse toolActionPerformed(int i_sequenceNumber, int i_toolID, int x, int y, int z, byte i_rotationCount, boolean i_flipped)
  {
    Packet250CloneToolUse retval = new Packet250CloneToolUse(Command.TOOL_ACTION_PERFORMED);
    retval.toolID = i_toolID;
    retval.xpos = x;
    retval.ypos = y;
    retval.zpos = z;
    retval.flipped = i_flipped;
    retval.rotationCount = i_rotationCount;
    retval.sequenceNumber = i_sequenceNumber;

    assert (retval.checkInvariants());
    return retval;
  }

  public static Packet250CloneToolUse toolUndoPerformed(int i_undoSequenceNumber, int i_actionSequenceNumber)
  {
    Packet250CloneToolUse retval = new Packet250CloneToolUse(Command.TOOL_UNDO_PERFORMED);
    retval.sequenceNumber = i_undoSequenceNumber;
    retval.actionToBeUndoneSequenceNumber = i_actionSequenceNumber;
    assert (retval.checkInvariants());
    return retval;
  }

  public Packet250CustomPayload getPacket250CustomPayload()
  {
    checkInvariants();
    Packet250CustomPayload retval = null;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      DataOutputStream outputStream = new DataOutputStream(bos);
      outputStream.writeByte(PacketHandler.PACKET250_CLONE_TOOL_USE_ID);
      outputStream.writeByte(commandToByte(command));
      outputStream.writeInt(toolID);
      outputStream.writeInt(sequenceNumber);
      outputStream.writeInt(actionToBeUndoneSequenceNumber);
      outputStream.writeInt(xpos);
      outputStream.writeInt(ypos);
      outputStream.writeInt(zpos);
      outputStream.writeByte(rotationCount);
      outputStream.writeBoolean(flipped);
      retval = new Packet250CustomPayload("speedytools",bos.toByteArray());
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Failed to getPacket250CustomPayload, due to exception " + ioe.toString());
      return null;
    }

    return retval;
  }

  /**
   * Creates a Packet250SpeedyToolUse from Packet250CustomPayload
   * @param sourcePacket250
   * @return the new packet for success, or null for failure
   */
  public static Packet250CloneToolUse createPacket250CloneToolUse(Packet250CustomPayload sourcePacket250)
  {
    try {
      DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(sourcePacket250.data));

      byte packetID = inputStream.readByte();
      if (packetID != PacketHandler.PACKET250_CLONE_TOOL_USE_ID) return null;

      byte commandValue = inputStream.readByte();
      Command command = byteToCommand(commandValue);
      if (command == null) return null;

      Packet250CloneToolUse newPacket = new Packet250CloneToolUse(command);
      newPacket.toolID = inputStream.readInt();
      newPacket.sequenceNumber = inputStream.readInt();
      newPacket.actionToBeUndoneSequenceNumber = inputStream.readInt();
      newPacket.xpos = inputStream.readInt();
      newPacket.ypos = inputStream.readInt();
      newPacket.zpos = inputStream.readInt();
      newPacket.rotationCount = inputStream.readByte();
      newPacket.flipped = inputStream.readBoolean();
      if (newPacket.checkInvariants()) return newPacket;
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Exception while reading Packet250SpeedyToolUse: " + ioe);
    }
    return null;
  }

  public static enum Command {
    SELECTION_MADE, TOOL_ACTION_PERFORMED, TOOL_UNDO_PERFORMED;
    public static final Command[] allValues = {SELECTION_MADE, TOOL_ACTION_PERFORMED, TOOL_UNDO_PERFORMED};
  }

  /**
   * Is this packet valid to be received and acted on by the given side?
   * @param whichSide
   * @return true if yes
   */
  public boolean validForSide(Side whichSide)
  {
    assert(checkInvariants());
    return true;
  }

  public Command getCommand()
  {
    assert (checkInvariants());
    return command;
  }

  public int getToolID() {
    assert (checkInvariants());
    assert(command == Command.TOOL_ACTION_PERFORMED || command == Command.TOOL_UNDO_PERFORMED);
    return toolID;
  }

  public int getXpos() {
    assert (checkInvariants());
    assert(command == Command.TOOL_ACTION_PERFORMED);
    return xpos;
  }

  public int getYpos() {
    assert (checkInvariants());
    assert(command == Command.TOOL_ACTION_PERFORMED);
    return ypos;
  }

  public int getZpos() {
    assert (checkInvariants());
    assert(command == Command.TOOL_ACTION_PERFORMED);
    return zpos;
  }

  public byte getRotationCount() {
    assert (checkInvariants());
    assert(command == Command.TOOL_ACTION_PERFORMED);
    return rotationCount;
  }

  public boolean isFlipped() {
    assert (checkInvariants());
    assert(command == Command.TOOL_ACTION_PERFORMED);
    return flipped;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public int getActionToBeUndoneSequenceNumber() {
    return actionToBeUndoneSequenceNumber;
  }

  private static Command byteToCommand(byte value)
  {
    if (value < 0 || value >= Command.allValues.length) return null;
    return Command.allValues[value];
  }

  private static byte commandToByte(Command value) throws IOException
  {
    byte retval;

    if (value != null) {
      for (retval = 0; retval < Command.allValues.length; ++retval) {
        if (Command.allValues[retval] == value) return retval;
      }
    }
    throw new IOException("Invalid command value");
  }

  private Packet250CloneToolUse(Command command)
  {
    this.command = command;
  }

  /**
   * checks this class to see if it is internally consistent
   * @return true if ok, false if bad.
   */
  private boolean checkInvariants()
  {
    if (command == null) return false;
    switch (command) {
      case SELECTION_MADE: {
        return true;
      }
      case TOOL_ACTION_PERFORMED: {
        return (rotationCount >= 0 && rotationCount <= 3);
      }
      case TOOL_UNDO_PERFORMED: {
        return true;
      }
      default: {
        return false;
      }
    }
  }

  private Command command;
  private int toolID;
  private int sequenceNumber;
  private int actionToBeUndoneSequenceNumber;
  private int xpos;
  private int ypos;
  private int zpos;
  private byte rotationCount;
  private boolean flipped;

}
