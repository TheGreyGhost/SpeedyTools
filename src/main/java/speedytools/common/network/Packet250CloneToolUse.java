package speedytools.common.network;


import cpw.mods.fml.relauncher.Side;
import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.utilities.ErrorLog;
import speedytools.common.utilities.QuadOrientation;

import java.io.*;

/**
 * This class is used to communicate actions from the client to the server for clone use
 * Client to Server:
 * (1) when user has made a selection using a clone tool
 * (2) user has performed an action with the tool (place) at the current selection position
 * (3) user has performed an undo with the tool
 */
public class Packet250CloneToolUse
{

  public static Packet250CloneToolUse informSelectionMade()
  {
    Packet250CloneToolUse retval = new Packet250CloneToolUse(Command.SELECTION_MADE);
    assert (retval.checkInvariants());
    return retval;
  }

  public static Packet250CloneToolUse performToolAction(int i_sequenceNumber, int i_toolID, int x, int y, int z, QuadOrientation i_quadOrientation)
  {
    Packet250CloneToolUse retval = new Packet250CloneToolUse(Command.PERFORM_TOOL_ACTION);
    retval.toolID = i_toolID;
    retval.xpos = x;
    retval.ypos = y;
    retval.zpos = z;
    retval.quadOrientation = i_quadOrientation;
    retval.sequenceNumber = i_sequenceNumber;

    assert (retval.checkInvariants());
    return retval;
  }

  public static Packet250CloneToolUse cancelCurrentAction(int i_undoSequenceNumber, int i_actionSequenceNumber)
  {
    Packet250CloneToolUse retval = new Packet250CloneToolUse(Command.PERFORM_TOOL_UNDO);
    retval.sequenceNumber = i_undoSequenceNumber;
    retval.actionToBeUndoneSequenceNumber = i_actionSequenceNumber;
    assert (retval.checkInvariants());
    return retval;
  }

  public static Packet250CloneToolUse performToolUndo(int i_undoSequenceNumber)
  {
    Packet250CloneToolUse retval = new Packet250CloneToolUse(Command.PERFORM_TOOL_UNDO);
    retval.sequenceNumber = i_undoSequenceNumber;
    retval.actionToBeUndoneSequenceNumber = NULL_SEQUENCE_NUMBER;
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
      outputStream.writeByte(Packet250Types.PACKET250_CLONE_TOOL_USE_ID.getPacketTypeID());
      outputStream.writeByte(commandToByte(command));
      outputStream.writeInt(toolID);
      outputStream.writeInt(sequenceNumber);
      outputStream.writeInt(actionToBeUndoneSequenceNumber);
      outputStream.writeInt(xpos);
      outputStream.writeInt(ypos);
      outputStream.writeInt(zpos);
      if (quadOrientation == null) {
        quadOrientation = new QuadOrientation(0, 0, 1, 1); // just a dummy
      }
      quadOrientation.writeToStream(outputStream);
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
      if (packetID != Packet250Types.PACKET250_CLONE_TOOL_USE_ID.getPacketTypeID()) return null;

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
      newPacket.quadOrientation = new QuadOrientation(inputStream);
      if (newPacket.checkInvariants()) return newPacket;
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Exception while reading Packet250SpeedyToolUse: " + ioe);
    }
    return null;
  }

  public static enum Command {
    SELECTION_MADE, PERFORM_TOOL_ACTION, PERFORM_TOOL_UNDO;
    public static final Command[] allValues = {SELECTION_MADE, PERFORM_TOOL_ACTION, PERFORM_TOOL_UNDO};
  }

  /**
   * Is this packet valid to be received and acted on by the given side?
   * @param whichSide
   * @return true if yes
   */
  public boolean validForSide(Side whichSide)
  {
    assert(checkInvariants());
    return (whichSide == Side.SERVER);
  }

  public Command getCommand()
  {
    assert (checkInvariants());
    return command;
  }

  public int getToolID() {
    assert (checkInvariants());
    assert(command == Command.PERFORM_TOOL_ACTION || command == Command.PERFORM_TOOL_UNDO);
    return toolID;
  }

  public int getXpos() {
    assert (checkInvariants());
    assert(command == Command.PERFORM_TOOL_ACTION);
    return xpos;
  }

  public int getYpos() {
    assert (checkInvariants());
    assert(command == Command.PERFORM_TOOL_ACTION);
    return ypos;
  }

  public int getZpos() {
    assert (checkInvariants());
    assert(command == Command.PERFORM_TOOL_ACTION);
    return zpos;
  }

  public QuadOrientation getQuadOrientation() {
    assert (checkInvariants());
    assert(command == Command.PERFORM_TOOL_ACTION);
    return quadOrientation;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  // null means no specific action nominated for undo
  public Integer getActionToBeUndoneSequenceNumber()
  {
    return (actionToBeUndoneSequenceNumber == NULL_SEQUENCE_NUMBER) ? null : actionToBeUndoneSequenceNumber;
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
      case PERFORM_TOOL_ACTION: {
        return true;
      }
      case PERFORM_TOOL_UNDO: {
        return true;
      }
      default: {
        return false;
      }
    }
  }

  private static final int NULL_SEQUENCE_NUMBER = Integer.MIN_VALUE;
  private Command command;
  private int toolID;
  private int sequenceNumber;
  private int actionToBeUndoneSequenceNumber;
  private int xpos;
  private int ypos;
  private int zpos;
  private QuadOrientation quadOrientation;
}
