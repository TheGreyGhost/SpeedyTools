package speedytools.common.network;


import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ChunkCoordinates;
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

  public static Packet250ServerSelectionGeneration performBoundFill(int whichTaskID, ChunkCoordinates i_cursorPosition, ChunkCoordinates i_corner1, ChunkCoordinates i_corner2)
  {
    Packet250ServerSelectionGeneration retval = new Packet250ServerSelectionGeneration(Command.BOUND_FILL, whichTaskID);
    retval.cursorPosition = i_cursorPosition;
    retval.corner1 = i_corner1;
    retval.corner2 = i_corner2;

    assert (retval.checkInvariants());
    retval.packetIsValid= true;
    return retval;
  }

  public static Packet250ServerSelectionGeneration performUnboundFill(int whichTaskID, ChunkCoordinates i_cursorPosition)
  {
    Packet250ServerSelectionGeneration retval = new Packet250ServerSelectionGeneration(Command.UNBOUND_FILL, whichTaskID);
    retval.cursorPosition = i_cursorPosition;

    assert (retval.checkInvariants());
    retval.packetIsValid= true;
    return retval;
  }

  public static Packet250ServerSelectionGeneration performAllInBox(int whichTaskID, ChunkCoordinates i_corner1, ChunkCoordinates i_corner2)
  {
    Packet250ServerSelectionGeneration retval = new Packet250ServerSelectionGeneration(Command.ALL_IN_BOX, whichTaskID);
    retval.corner1 = i_corner1;
    retval.corner2 = i_corner2;

    assert (retval.checkInvariants());
    retval.packetIsValid= true;
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
        case UNBOUND_FILL: {
          cursorPosition = readChunkCoordinates(buf);
          corner1 = readChunkCoordinates(buf);
          corner2 = readChunkCoordinates(buf);
          break;
        }
        case BOUND_FILL: {
          cursorPosition = readChunkCoordinates(buf);
          break;
        }
        case ALL_IN_BOX: {
          corner1 = readChunkCoordinates(buf);
          corner2 = readChunkCoordinates(buf);
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
      case UNBOUND_FILL: {
        writeChunkCoordinates(buf, cursorPosition);
        writeChunkCoordinates(buf, corner1);
        writeChunkCoordinates(buf, corner2);
        break;
      }
      case BOUND_FILL: {
        writeChunkCoordinates(buf, cursorPosition);
        break;
      }      case ALL_IN_BOX: {
        writeChunkCoordinates(buf, corner1);
        writeChunkCoordinates(buf, corner2);
        break;
      }
      default: {
        ErrorLog.defaultLog().info("Invalid command " + command + " in readFromBuffer in " + this.getClass().getName());
        return;
      }
    }
  }

  private ChunkCoordinates readChunkCoordinates(ByteBuf buf)
  {
    ChunkCoordinates chunkCoordinates = new ChunkCoordinates();
    chunkCoordinates.posX = buf.readInt();
    chunkCoordinates.posY = buf.readInt();
    chunkCoordinates.posZ = buf.readInt();
    return chunkCoordinates;
  }

  private void writeChunkCoordinates(ByteBuf buf, ChunkCoordinates chunkCoordinates)
  {
    buf.writeInt(chunkCoordinates.posX);
    buf.writeInt(chunkCoordinates.posY);
    buf.writeInt(chunkCoordinates.posZ);
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
    public final byte commandID;
  }

  public Command getCommand()
  {
    assert (checkInvariants());
    return command;
  }

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
        return (cursorPosition == null && corner1 == null && corner2 == null);
      }
      case ALL_IN_BOX: {
        return (cursorPosition == null && corner1 != null && corner2 != null);
      }
      case UNBOUND_FILL: {
        return (cursorPosition != null && corner1 != null && corner2 != null);
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

  public ChunkCoordinates getCursorPosition() {
    return new ChunkCoordinates(cursorPosition);
  }

  public ChunkCoordinates getCorner1() {
    return new ChunkCoordinates(corner1);
  }

  public ChunkCoordinates getCorner2() {
    return new ChunkCoordinates(corner2);
  }

  private ChunkCoordinates cursorPosition;
  private ChunkCoordinates corner1;
  private ChunkCoordinates corner2;
  private int uniqueID;

  private static PacketHandlerMethod serverSideHandler;
  private static PacketHandlerMethod clientSideHandler;

}
