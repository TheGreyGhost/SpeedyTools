package speedytools.clientside;

import speedytools.clientside.network.CloneToolsNetworkClient;
import speedytools.clientside.network.PacketSenderClient;
import speedytools.clientside.rendering.SpeedyToolRenderers;
import speedytools.clientside.rendering.SpeedyToolSounds;
import speedytools.clientside.tools.ActiveTool;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.SpeedyToolsOptions;
import speedytools.common.network.PacketHandlerRegistry;

/**
 * User: The Grey Ghost
 * Date: 10/03/14
 * Contains the various objects that define the client side
 */
public class ClientSide
{
  public static void initialise()
  {
    packetHandlerRegistry = new PacketHandlerRegistry();
    packetSenderClient = new  PacketSenderClient();
    cloneToolsNetworkClient = new CloneToolsNetworkClient(packetHandlerRegistry, packetSenderClient);
    speedyToolRenderers = new SpeedyToolRenderers();
    activeTool = new ActiveTool();
    userInput = new UserInput();
    speedyToolSounds = new SpeedyToolSounds();
    undoManagerSimple = new UndoManagerClient(SpeedyToolsOptions.getMaxSimpleToolUndoCount());
    undoManagerComplex = new UndoManagerClient(SpeedyToolsOptions.getMaxComplexToolUndoCount());
  }

/*
  public static void shutdown()
  {
    cloneToolsNetworkClient = null;
  }
*/

  public static CloneToolsNetworkClient getCloneToolsNetworkClient() {
    return cloneToolsNetworkClient;
  }

  public static CloneToolsNetworkClient cloneToolsNetworkClient;
  public static SpeedyToolRenderers speedyToolRenderers;
  public static ActiveTool activeTool;
  public static UserInput userInput;
  public static UndoManagerClient undoManagerSimple;
  public static UndoManagerClient undoManagerComplex;
  public static SpeedyToolSounds speedyToolSounds;
  public static PacketSenderClient packetSenderClient;
  public static PacketHandlerRegistry packetHandlerRegistry;

  public static int getGlobalTickCount() {
    return globalTickCount;
  }

  public static void tick() { ++globalTickCount;}

  private static int globalTickCount = 0;

}
