package speedytools.clientside;

import speedytools.clientside.network.CloneToolsNetworkClient;
import speedytools.clientside.rendering.SpeedyToolRenderers;
import speedytools.clientside.rendering.SpeedyToolSounds;
import speedytools.clientside.tools.ActiveTool;
import speedytools.clientside.userinput.UserInput;

/**
 * User: The Grey Ghost
 * Date: 10/03/14
 * Contains the various objects that define the client side
 */
public class ClientSide
{
  public static void initialise()
  {
    cloneToolsNetworkClient = new CloneToolsNetworkClient();
    speedyToolRenderers = new SpeedyToolRenderers();
    activeTool = new ActiveTool();
    userInput = new UserInput();
    speedyToolSounds = new SpeedyToolSounds();
    undoManagerClient = new UndoManagerClient();
  }

  public static void shutdown()
  {
    cloneToolsNetworkClient = null;
  }


  public static CloneToolsNetworkClient getCloneToolsNetworkClient() {
    return cloneToolsNetworkClient;
  }

  public static CloneToolsNetworkClient cloneToolsNetworkClient;

  public static SpeedyToolRenderers speedyToolRenderers;
  public static ActiveTool activeTool;
  public static UserInput userInput;
  public static UndoManagerClient undoManagerClient;
  public static SpeedyToolSounds speedyToolSounds;

}
