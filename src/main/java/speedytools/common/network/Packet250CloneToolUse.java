package speedytools.common.network;


import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.utilities.ErrorLog;
import speedytools.common.utilities.QuadOrientation;

/**
* This class is used to communicate actions from the client to the server for clone use
* Client to Server:
* (1) when user has made a selection using a clone tool
* (2) user has performed an action with the tool (place) at the current selection position
* (3) user has performed an undo with the tool
*/
public class Packet250CloneToolUse extends Packet250Base
{

  public static Packet250CloneToolUse prepareForLaterAction()
  {
    Packet250CloneToolUse retval = new Packet250CloneToolUse(Command.PREPARE_FOR_LATER_ACTION);
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
    retval.packetIsValid= true;
    return retval;
  }

  public static Packet250CloneToolUse performToolFillAction(int i_sequenceNumber, int i_toolID, BlockWithMetadata i_fillBlock,
                                                            int x, int y, int z, QuadOrientation i_quadOrientation)
  {
    Packet250CloneToolUse retval = new Packet250CloneToolUse(Command.PERFORM_TOOL_ACTION);
    retval.toolID = i_toolID;
    retval.blockWithMetadata = i_fillBlock;
    retval.xpos = x;
    retval.ypos = y;
    retval.zpos = z;
    retval.quadOrientation = i_quadOrientation;
    retval.sequenceNumber = i_sequenceNumber;

    assert (retval.checkInvariants());
    retval.packetIsValid= true;
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

  @Override
  protected void readFromBuffer(ByteBuf buf) {
    packetIsValid = false;
    try {
      byte commandValue = buf.readByte();
      command = Command.byteToCommand(commandValue);
      if (command == null) return;

      toolID = buf.readInt();
      sequenceNumber = buf.readInt();
      actionToBeUndoneSequenceNumber = buf.readInt();
      int blockID = buf.readInt();
      blockWithMetadata = new BlockWithMetadata();
      blockWithMetadata.block = Block.getBlockById(blockID);
      blockWithMetadata.metaData = buf.readInt();

      xpos = buf.readInt();
      ypos = buf.readInt();
      zpos = buf.readInt();
      quadOrientation = new QuadOrientation(buf);
    } catch (IndexOutOfBoundsException ioe) {
      ErrorLog.defaultLog().info("Exception while reading Packet250CloneToolUse: " + ioe);
      return;
    }
    if (!checkInvariants()) return;
    packetIsValid = true;
  }

  @Override
  protected void writeToBuffer(ByteBuf buf) {
    if (!isPacketIsValid()) return;
//    buf.writeByte(Packet250Types.PACKET250_CLONE_TOOL_USE_ID.getPacketTypeID());
    buf.writeByte(command.getCommandID());
    buf.writeInt(toolID);
    buf.writeInt(sequenceNumber);
    buf.writeInt(actionToBeUndoneSequenceNumber);
    if (blockWithMetadata == null) {
      buf.writeInt(Block.getIdFromBlock(Blocks.air));
      buf.writeInt(0);
    } else {
      buf.writeInt(Block.getIdFromBlock(blockWithMetadata.block));
      buf.writeInt(blockWithMetadata.metaData);
    }
    buf.writeInt(xpos);
    buf.writeInt(ypos);
    buf.writeInt(zpos);
    if (quadOrientation == null) {
      quadOrientation = new QuadOrientation(0, 0, 1, 1); // just a dummy
    }
    quadOrientation.writeToStream(buf);
  }

  public static enum Command {
    PREPARE_FOR_LATER_ACTION(0), PERFORM_TOOL_ACTION(1), PERFORM_TOOL_UNDO(2);

    public byte getCommandID() {return commandID;}

    private static Command byteToCommand(byte value)
    {
      for (Command command : Command.values()) {
        if (value == command.getCommandID()) return command;
      }
      return null;
    }

    private Command(int i_commandID) {
      commandID = (byte)i_commandID;
    }
    public final byte commandID;
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

  public BlockWithMetadata getBlockWithMetadata() {
    return blockWithMetadata;
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

  /**
   * Register the handler for this packet
   * @param packetHandlerRegistry
   * @param packetHandlerMethod
   * @param side
   */
  public static void registerHandler(PacketHandlerRegistry packetHandlerRegistry,  PacketHandlerMethod packetHandlerMethod, Side side) {
    if (side != Side.SERVER) {
      assert false : "Tried to register Packet250CloneToolUse on side " + side;
    }
    serverSideHandler = packetHandlerMethod;
    packetHandlerRegistry.getSimpleNetworkWrapper().registerMessage(ServerMessageHandler.class, Packet250CloneToolUse.class,
            Packet250Types.PACKET250_CLONE_TOOL_USE_ID.getPacketTypeID(), side);
  }

  public interface PacketHandlerMethod
  {
    public boolean handlePacket(Packet250CloneToolUse packet250CloneToolUse, MessageContext ctx);
  }

  public static class ServerMessageHandler implements IMessageHandler<Packet250CloneToolUse, IMessage>
  {
    /**
     * Called when a message is received of the appropriate type. You can optionally return a reply message, or null if no reply
     * is needed.
     *
     * @param message The message
     * @return an optional return message
     */
    public IMessage onMessage(Packet250CloneToolUse message, MessageContext ctx)
    {
      if (serverSideHandler == null) {
        ErrorLog.defaultLog().severe("Packet250CloneToolUse received but not registered.");
      } else if (ctx.side != Side.SERVER) {
        ErrorLog.defaultLog().severe("Packet250CloneToolUse received on wrong side");
      } else {
        boolean success = serverSideHandler.handlePacket(message, ctx);
        if (!success) {
          ErrorLog.defaultLog().severe("Packet250CloneToolUse failed to handle Packet");
        }
      }
      return null;
    }
  }

  private Packet250CloneToolUse(Command command)
  {
    this.command = command;
    this.packetIsValid = true;
  }

  public Packet250CloneToolUse()  // used by Netty; invalid until populated by the packet handler
  {
  }

  /**
   * checks this class to see if it is internally consistent
   * @return true if ok, false if bad.
   */
  private boolean checkInvariants()
  {
    if (command == null) return false;
    switch (command) {
      case PREPARE_FOR_LATER_ACTION: {
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

  private BlockWithMetadata blockWithMetadata;
  private int sequenceNumber;
  private int actionToBeUndoneSequenceNumber;
  private int xpos;
  private int ypos;
  private int zpos;
  private QuadOrientation quadOrientation;

  private static PacketHandlerMethod serverSideHandler;

}
