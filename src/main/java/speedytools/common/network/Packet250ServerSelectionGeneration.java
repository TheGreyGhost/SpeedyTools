package speedytools.common.network;


import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.BlockPos;
import speedytools.common.selections.FillAlgorithmSettings;
import speedytools.common.utilities.ErrorLog;

/**
* This class is used to communicate from the client to the server when it's necessary to generate a selection on the server
* (if the selection is large, the client may have empty chunks in it.  See notes/SelectionGeneration.txt)
* Typical usage:
* Client to Server:
* (1) FILL command to perform a flood fill from the cursor
* (2) ALL_IN_BOX command to select all in the given box region
* (3) ABORT to stop the selection generation
* (4) STATUS_REQUEST to ask the server to return an estimate of the fraction completed [0..1].
*
* Server to Client:
* (1) STATUS command in response to the STATUS_REQUEST message or a command.
*
* The commands contain a uniqueID, which is returned in the status messages.
*
*/
public class Packet250ServerSelectionGeneration extends Packet250Base
{

  public static Packet250ServerSelectionGeneration abortSelectionGeneration(int whichTaskID)
  {
    Packet250ServerSelectionGeneration retval = new Packet250ServerSelectionGeneration(Command.ABORT, whichTaskID);
    assert (retval.checkInvariants());
    return retval;
  }

  public static Packet250ServerSelectionGeneration requestStatus(int whichTaskID)
  {
    Packet250ServerSelectionGeneration retval = new Packet250ServerSelectionGeneration(Command.STATUS_REQUEST, whichTaskID);
    assert (retval.checkInvariants());
    return retval;
  }

  public static Packet250ServerSelectionGeneration replyFractionCompleted(int whichTaskID, float i_completedFraction)
  {
    Packet250ServerSelectionGeneration retval = new Packet250ServerSelectionGeneration(Command.STATUS_REPLY, whichTaskID);
    retval.completedFraction = i_completedFraction;
    assert (retval.checkInvariants());
    return retval;
  }

  public static Packet250ServerSelectionGeneration performBoundFill(FillAlgorithmSettings i_fillAlgorithmSettings,
                                                                    int whichTaskID, BlockPos i_corner1, BlockPos i_corner2)
  {
    Packet250ServerSelectionGeneration retval = new Packet250ServerSelectionGeneration(Command.BOUND_FILL, whichTaskID);
    retval.fillAlgorithmSettings = i_fillAlgorithmSettings;
//    retval.cursorPosition = i_cursorPosition;
    retval.corner1 = i_corner1;
    retval.corner2 = i_corner2;

    assert (retval.checkInvariants());
    retval.packetIsValid = true;
    return retval;
  }

  public static Packet250ServerSelectionGeneration performUnboundFill(FillAlgorithmSettings i_fillAlgorithmSettings, int whichTaskID)
  {
    Packet250ServerSelectionGeneration retval = new Packet250ServerSelectionGeneration(Command.UNBOUND_FILL, whichTaskID);
    retval.fillAlgorithmSettings = i_fillAlgorithmSettings;
//    retval.cursorPosition = i_cursorPosition;

    assert (retval.checkInvariants());
    retval.packetIsValid = true;
    return retval;
  }

  public static Packet250ServerSelectionGeneration performAllInBox(int whichTaskID, BlockPos i_corner1, BlockPos i_corner2)
  {
    Packet250ServerSelectionGeneration retval = new Packet250ServerSelectionGeneration(Command.ALL_IN_BOX, whichTaskID);
    retval.corner1 = i_corner1;
    retval.corner2 = i_corner2;

    assert (retval.checkInvariants());
    retval.packetIsValid = true;
    return retval;
  }

  @Override
  public void readFromBuffer(ByteBuf buf) {
    packetIsValid = false;
    try {
      byte commandValue = buf.readByte();
      command = Command.byteToCommand(commandValue);
      if (command == null) return;
      uniqueID = buf.readInt();

      switch (command) {
        case STATUS_REQUEST:
        case ABORT: {
          break;
        }
        case STATUS_REPLY: {
          completedFraction = buf.readFloat();
          break;
        }
        case BOUND_FILL: {
          fillAlgorithmSettings = FillAlgorithmSettings.createFromBuffer(buf);// = MatcherType.byteToMatcherType(buf.readByte());
//          cursorPosition = readBlockPos(buf);
          corner1 = readBlockPos(buf);
          corner2 = readBlockPos(buf);
          break;
        }
        case UNBOUND_FILL: {
          fillAlgorithmSettings = FillAlgorithmSettings.createFromBuffer(buf); //MatcherType.byteToMatcherType(buf.readByte());
//          cursorPosition = readBlockPos(buf);
          break;
        }
        case ALL_IN_BOX: {
          corner1 = readBlockPos(buf);
          corner2 = readBlockPos(buf);
          break;
        }
        default: {
          ErrorLog.defaultLog().info("Invalid command " + command + " in readFromBuffer in " + this.getClass().getName());
          return;
        }
      }
    } catch (IndexOutOfBoundsException ioe) {
      ErrorLog.defaultLog().info("Exception while reading " + this.getClass().getName() + ": " + ioe);
      return;
    }
    if (!checkInvariants()) return;
    packetIsValid = true;
  }

  @Override
  public void writeToBuffer(ByteBuf buf) {
    if (!isPacketIsValid()) {
      ErrorLog.defaultLog().info("Tried to send invalid packet in writeToBuffer in " + this.getClass().getName());
      return;
    }
    if (!checkInvariants()) {
      ErrorLog.defaultLog().info("Invariants failed on packet in writeToBuffer in " + this.getClass().getName());
      return;
    }

    buf.writeByte(command.getCommandID());
    buf.writeInt(uniqueID);
    switch (command) {
      case STATUS_REQUEST:
      case ABORT: {
        break;
      }
      case STATUS_REPLY: {
        buf.writeFloat(completedFraction);
        break;
      }
      case BOUND_FILL: {
        fillAlgorithmSettings.writeToBuffer(buf);
//        writeBlockPos(buf, cursorPosition);
        writeBlockPos(buf, corner1);
        writeBlockPos(buf, corner2);
        break;
      }
      case UNBOUND_FILL: {
        fillAlgorithmSettings.writeToBuffer(buf);
//        writeBlockPos(buf, cursorPosition);
        break;
      }
      case ALL_IN_BOX: {
        writeBlockPos(buf, corner1);
        writeBlockPos(buf, corner2);
        break;
      }
      default: {
        ErrorLog.defaultLog().info("Invalid command " + command + " in readFromBuffer in " + this.getClass().getName());
        return;
      }
    }
  }

  private BlockPos readBlockPos(ByteBuf buf)
  {
    int x = buf.readInt();
    int y = buf.readInt();
    int z = buf.readInt();
    BlockPos chunkCoordinates = new BlockPos(x, y, z);
    return chunkCoordinates;
  }

  private void writeBlockPos(ByteBuf buf, BlockPos chunkCoordinates)
  {
    buf.writeInt(chunkCoordinates.getX());
    buf.writeInt(chunkCoordinates.getY());
    buf.writeInt(chunkCoordinates.getZ());
  }

  public static enum Command {
    ABORT(150), UNBOUND_FILL(151), BOUND_FILL(152), ALL_IN_BOX(153), STATUS_REQUEST(154), STATUS_REPLY(155);

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
    private final byte commandID;
  }

//  public static enum MatcherType {
//    ANY_NON_AIR(163), STARTING_BLOCK_ONLY(164);
//
//    public byte getMatcherTypeID() {return matcherTypeID;}
//
//    private static MatcherType byteToMatcherType(byte value)
//    {
//      for (MatcherType matcherType : MatcherType.values()) {
//        if (value == matcherType.getMatcherTypeID()) return matcherType;
//      }
//      return null;
//    }
//
//    private MatcherType(int i_matcherTypeID) {matcherTypeID = (byte)i_matcherTypeID;}
//    private final byte matcherTypeID;
//  }


  public Command getCommand()
  {
    assert (checkInvariants());
    return command;
  }

//  public MatcherType getMatcherType()
//  {
//    assert (checkInvariants());
//    return matcherType;
//  }

  public int getUniqueID()
  {
    return uniqueID;
  }

  /**
   * Register the handler for this packet
   * @param packetHandlerRegistry
   * @param packetHandlerMethod
   * @param side
   */
  public static void registerHandler(PacketHandlerRegistry packetHandlerRegistry,  PacketHandlerMethod packetHandlerMethod, Side side)
  {
    switch (side) {
      case SERVER: {
        serverSideHandler = packetHandlerMethod;
        break;
      }
      case CLIENT: {
        clientSideHandler = packetHandlerMethod;
        break;
      }
      default: {
        assert false : "Tried to register Packet250ServerSelectionGeneration on side " + side;
      }
    }
    packetHandlerRegistry.getSimpleNetworkWrapper().registerMessage(CommonMessageHandler.class, Packet250ServerSelectionGeneration.class,
            Packet250Types.PACKET250_SERVER_SELECTION_GENERATION.getPacketTypeID(), side);
  }

  public interface PacketHandlerMethod
  {
    public Packet250ServerSelectionGeneration handlePacket(Packet250ServerSelectionGeneration packet250CloneToolUse, MessageContext ctx);
  }

  private static Packet250ServerSelectionGeneration handleMessage(Packet250ServerSelectionGeneration message, MessageContext ctx)
  {
    switch (ctx.side) {
      case CLIENT: {
        if (clientSideHandler == null) {
          ErrorLog.defaultLog().severe("Packet250ServerSelectionGeneration received but not registered on client.");
        } else {
          clientSideHandler.handlePacket(message, ctx);
        }
        break;
      }
      case SERVER: {
        if (serverSideHandler == null) {
          ErrorLog.defaultLog().severe("Packet250ServerSelectionGeneration received but not registered.");
        } else {
          Packet250ServerSelectionGeneration reply = serverSideHandler.handlePacket(message, ctx);
          return reply;
        }
        break;
      }
      default: {
        ErrorLog.defaultLog().severe("Packet250ServerSelectionGeneration received on wrong side " + ctx.side);
      }
    }
    return null;
  }

  public static class CommonMessageHandler implements IMessageHandler<Packet250ServerSelectionGeneration, Packet250ServerSelectionGeneration>
  {
    public Packet250ServerSelectionGeneration onMessage(Packet250ServerSelectionGeneration message, MessageContext ctx) {
      return handleMessage(message, ctx);
    }
  }

//  public static class ServerMessageHandler implements IMessageHandler<Packet250ServerSelectionGeneration, Packet250ServerSelectionGeneration>
//  {
//    public Packet250ServerSelectionGeneration onMessage(Packet250ServerSelectionGeneration message, MessageContext ctx)
//    {
//      return handleMessage(message, ctx);
//    }
//  }

  private Packet250ServerSelectionGeneration(Command command, int whichTaskID)
  {
    this.command = command;
    this.uniqueID = whichTaskID;
    this.packetIsValid = true;
  }

  public Packet250ServerSelectionGeneration()  // used by Netty; invalid until populated by the packet handler
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
      case STATUS_REPLY:
      case STATUS_REQUEST:
      case ABORT: {
        return (fillAlgorithmSettings == null && corner1 == null && corner2 == null);
      }
      case ALL_IN_BOX: {
        return (fillAlgorithmSettings == null && corner1 != null && corner2 != null);
      }
      case UNBOUND_FILL: {
        return (fillAlgorithmSettings != null  && corner1 == null && corner2 == null);
      }
      case BOUND_FILL: {
        return (fillAlgorithmSettings != null  && corner1 != null && corner2 != null);
      }
      default: {
        return false;
      }
    }
  }

  private Command command;

  public float getCompletedFraction() {
    return completedFraction;
  }

  private float completedFraction;

//  public BlockPos getCursorPosition() {
//    return new BlockPos(cursorPosition);
//  }


  public BlockPos getCorner1() {
    return new BlockPos(corner1);
  }

  public BlockPos getCorner2() {
    return new BlockPos(corner2);
  }

  public FillAlgorithmSettings getFillAlgorithmSettings() {
    return fillAlgorithmSettings;
  }

  private FillAlgorithmSettings fillAlgorithmSettings;
//  private BlockPos cursorPosition;
  private BlockPos corner1;
  private BlockPos corner2;
  private int uniqueID;

  private static PacketHandlerMethod serverSideHandler;
  private static PacketHandlerMethod clientSideHandler;

}
