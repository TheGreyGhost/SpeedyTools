package speedytools.serverside;

import speedytools.common.SpeedyToolsOptions;
import speedytools.common.network.PacketHandlerRegistry;
import speedytools.serverside.ingametester.InGameStatusSimulator;
import speedytools.serverside.ingametester.InGameTester;
import speedytools.serverside.worldmanipulation.WorldHistory;

/**
 * User: The Grey Ghost
 * Date: 10/03/14
 * Contains the various objects that define the server side
 */
public class ServerSide
{
  public static void initialise()
  {
    packetHandlerRegistry = new PacketHandlerRegistry() ;
    serverVoxelSelections = new ServerVoxelSelections(packetHandlerRegistry);
    worldHistory = new WorldHistory(SpeedyToolsOptions.getMaxComplexToolUndoCount());
    cloneToolServerActions = new CloneToolServerActions(serverVoxelSelections, worldHistory);
    cloneToolsNetworkServer = new CloneToolsNetworkServer(packetHandlerRegistry, cloneToolServerActions);
    speedyToolWorldManipulator = new SpeedyToolWorldManipulator(packetHandlerRegistry, worldHistory);
    inGameTester = new InGameTester(packetHandlerRegistry);
    inGameStatusSimulator = new InGameStatusSimulator();
  }

  public static void initialiseForJTest()
  {
    inGameStatusSimulator = new InGameStatusSimulator();
  }

  public static void shutdown()
  {
    packetHandlerRegistry = null;
    cloneToolServerActions = null;
    cloneToolsNetworkServer = null;
    speedyToolWorldManipulator = null;
    serverVoxelSelections = null;
  }

  public static CloneToolsNetworkServer getCloneToolsNetworkServer() {
    return cloneToolsNetworkServer;
  }
  public static CloneToolServerActions getCloneToolServerActions() {
    return cloneToolServerActions;
  }
  public static SpeedyToolWorldManipulator getSpeedyToolWorldManipulator() {
    return speedyToolWorldManipulator;
  }


  private static CloneToolsNetworkServer cloneToolsNetworkServer;

  public static ServerVoxelSelections getServerVoxelSelections() {
    return serverVoxelSelections;
  }
  public static InGameStatusSimulator getInGameStatusSimulator() {
    return inGameStatusSimulator;
  }

  private static ServerVoxelSelections serverVoxelSelections;
  private static CloneToolServerActions cloneToolServerActions;
  private static SpeedyToolWorldManipulator speedyToolWorldManipulator;
  private static PacketHandlerRegistry packetHandlerRegistry;
  private static InGameTester inGameTester;
  private static InGameStatusSimulator inGameStatusSimulator;
  private static WorldHistory worldHistory;

}
