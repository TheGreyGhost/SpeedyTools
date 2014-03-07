package speedytools.common.network;


import cpw.mods.fml.relauncher.Side;
import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.utilities.ErrorLog;

import java.io.*;

/**
 * This class is used to communicate between the client and server for clone use
 * Client to Server (C2S):
 * (1) when user has made a selection using a clone tool (--> will cause a server backup)
 * (3) user has performed an action with the tool (place, or undo) at the current selection position
 *
 * Server to Client (S2C):
 * (2) backup is finished, send the selection
 * (4) percentage completion information
 */
public class Packet250CloneToolUse
{
  public static Packet250CloneToolUse selectionMadeC2S(int x, int y, int z)
  {
    Packet250CloneToolUse retval = new Packet250CloneToolUse(COMMAND_SELECTION_MADE);
    retval.xpos = x;
    retval.ypos = y;
    retval.zpos = z;
    retval.checkInvariants();
    return retval;
  }

  public static Packet250CloneToolUse readyForSelectionS2C()
  {
    return new Packet250CloneToolUse(COMMAND_READY_FOR_SELECTION);
  }

  public static Packet250CloneToolUse toolActionPerformedC2S(int i_toolID, int x, int y, int z, byte i_rotationCount, boolean i_flipped)
  {
    Packet250CloneToolUse retval = new Packet250CloneToolUse(COMMAND_TOOL_ACTION_PERFORMED);
    retval.toolID = i_toolID;
    retval.xpos = x;
    retval.ypos = y;
    retval.zpos = z;
    retval.flipped = i_flipped;
    retval.rotationCount = i_rotationCount;

    retval.checkInvariants();
    return retval;
  }

  public static Packet250CloneToolUse toolUndoPerformedC2S(int i_toolID)
  {
    Packet250CloneToolUse retval = new Packet250CloneToolUse(COMMAND_TOOL_UNDO_PERFORMED);
    retval.toolID = i_toolID;
    retval.checkInvariants();
    return retval;
  }

  /**
   *
   * @param percentComplete must be between 0 and 100 inclusive.  100 indicates completely finished.
   * @return
   */
  public static Packet250CloneToolUse completionStatus(byte percentComplete)
  {
    Packet250CloneToolUse retval = new Packet250CloneToolUse(COMMAND_COMPLETION_STATUS);
    retval.completionPercentage = percentComplete;
    retval.checkInvariants();
    return retval;
  }

  public Packet250CustomPayload getPacket250CustomPayload()
  {
    checkInvariants();
    Packet250CustomPayload retval = null;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(1*1 + 1*4  + 4*4);
      DataOutputStream outputStream = new DataOutputStream(bos);
      outputStream.writeByte(PacketHandler.PACKET250_CLONE_TOOL_USE_ID);
      outputStream.writeByte(command);
      outputStream.writeInt(toolID);
      outputStream.writeInt(xpos);
      outputStream.writeInt(ypos);
      outputStream.writeInt(zpos);
      outputStream.writeByte(completionPercentage);
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
   */
  public Packet250CloneToolUse(Packet250CustomPayload sourcePacket250)
  {
    DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(sourcePacket250.data));

    try {
      byte packetID = inputStream.readByte();
      if (packetID != PacketHandler.PACKET250_CLONE_TOOL_USE_ID) return;

      command = inputStream.readByte();
      toolID = inputStream.readInt();
      xpos = inputStream.readInt();
      ypos = inputStream.readInt();
      zpos = inputStream.readInt();
      completionPercentage = inputStream.readByte();
      rotationCount = inputStream.readByte();
      flipped = inputStream.readBoolean();
    } catch (IOException e) {
      e.printStackTrace();
    }
    checkInvariants();
  }

  private Packet250CloneToolUse(Command command)
  {
    this.command = command;
  }

  public enum Command {
    SELECTION_MADE, TOOL_ACTION_PERFORMED, TOOL_UNDO_PERFORMED
  }


  private static final byte COMMAND_MINIMUM_VALID = 0;
  public static final byte COMMAND_SELECTION_MADE = 0;
  public static final byte COMMAND_READY_FOR_SELECTION = 1;
  public static final byte COMMAND_TOOL_ACTION_PERFORMED = 2;
  public static final byte COMMAND_TOOL_UNDO_PERFORMED = 3;
  public static final byte COMMAND_COMPLETION_STATUS = 4;
  private static final byte COMMAND_MAXIMUM_VALID = 4;

  private static final int INVALID_INT_VALUE = Integer.MAX_VALUE;
  private static final byte INVALID_BYTE_VALUE = Byte.MAX_VALUE;

  /**
   * Is this packet valid to be received and acted on by the given side?
   * @param whichSide
   * @return true if yes
   */
  public boolean validForSide(Side whichSide)
  {
    checkInvariants();
    switch (command) {
      case COMMAND_SELECTION_MADE: return (whichSide == Side.SERVER);
      case COMMAND_READY_FOR_SELECTION: return (whichSide == Side.CLIENT);
      case COMMAND_TOOL_ACTION_PERFORMED: return (whichSide == Side.SERVER);
      case COMMAND_TOOL_UNDO_PERFORMED: return (whichSide == Side.SERVER);
      case COMMAND_COMPLETION_STATUS: return (whichSide == Side.CLIENT);
      default: return false;
    }
  }

  public byte getCommand()
  {
    checkInvariants();
    return command;
  }

  public int getToolID() {
    checkInvariants();
    assert(toolID != INVALID_INT_VALUE);
    return toolID;
  }

  public int getXpos() {
    checkInvariants();
    assert(xpos != INVALID_INT_VALUE);
    return xpos;
  }

  public int getYpos() {
    checkInvariants();
    assert(ypos != INVALID_INT_VALUE);
    return ypos;
  }

  public int getZpos() {
    checkInvariants();
    assert(zpos != INVALID_INT_VALUE);
    return zpos;
  }

  public byte getCompletionPercentage() {
    checkInvariants();
    assert(completionPercentage != INVALID_BYTE_VALUE);
    return completionPercentage;
  }

  public byte getRotationCount() {
    checkInvariants();
    assert(rotationCount != INVALID_BYTE_VALUE);
    return rotationCount;
  }

  public boolean isFlipped() {
    checkInvariants();
    assert(command == COMMAND_TOOL_ACTION_PERFORMED);
    return flipped;
  }

  /**
   * checks this class to see if it is internally consistent
   * @return true if ok, false if bad.
   */
  private boolean checkInvariants()
  {
    boolean valid;
    valid = (command >= COMMAND_MINIMUM_VALID && command <= COMMAND_MAXIMUM_VALID);
    valid = valid && (command != COMMAND_SELECTION_MADE || (xpos != INVALID_INT_VALUE && ypos != INVALID_INT_VALUE && zpos != INVALID_INT_VALUE));
    valid = valid && (command != COMMAND_TOOL_ACTION_PERFORMED
                      || (xpos != INVALID_INT_VALUE && ypos != INVALID_INT_VALUE && zpos != INVALID_INT_VALUE
                          && rotationCount!= INVALID_BYTE_VALUE && toolID != INVALID_INT_VALUE ));
    valid = valid && (command != COMMAND_COMPLETION_STATUS || (completionPercentage >= 0 && completionPercentage <= 100));
    return valid;
  }

  private Command command = INVALID_BYTE_VALUE;
  private int toolID = INVALID_INT_VALUE;
  private int xpos = INVALID_INT_VALUE;
  private int ypos = INVALID_INT_VALUE;
  private int zpos = INVALID_INT_VALUE;
  private byte completionPercentage = INVALID_BYTE_VALUE;
  private byte rotationCount = INVALID_BYTE_VALUE;
  private boolean flipped = false;
}
