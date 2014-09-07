package speedytools.serverside;

import speedytools.serverside.ingametester.InGameTester;
import speedytools.serverside.network.PacketHandlerRegistryServer;

/**
* User: The Grey Ghost
* Date: 10/03/14
* Contains the various objects that define the server side
*/
public class ServerSide
{
  public static void load()
  {
    packetHandlerRegistryServer = new PacketHandlerRegistryServer() ;
//    serverVoxelSelections = new ServerVoxelSelections(packetHandlerRegistry);
//    worldHistory = new WorldHistory(SpeedyToolsOptions.getMaxComplexToolUndoCount(), SpeedyToolsOptions.getMaxSimpleToolUndoCount());
//    speedyToolServerActions = new SpeedyToolServerActions(serverVoxelSelections, worldHistory);
//    speedyToolsNetworkServer = new SpeedyToolsNetworkServer(packetHandlerRegistry, speedyToolServerActions);
//    speedyToolWorldManipulator = new SpeedyToolWorldManipulator(packetHandlerRegistry, worldHistory);
    inGameTester = new InGameTester(packetHandlerRegistryServer);
//    inGameStatusSimulator = new InGameStatusSimulator();

    String NETWORK_LOG_FILENAME_STEM = "NetworkMonitor";
//    if (SpeedyToolsOptions.getNetworkLoggingActive()) {
//      try {
//        File loggingDirectory = SpeedyToolsOptions.getNetworkLoggingDirectory();
//        Path loggingPath = loggingDirectory == null ? null : loggingDirectory.toPath();
//        networkTrafficMonitor = new NetworkTrafficMonitor(Side.SERVER, loggingPath, NETWORK_LOG_FILENAME_STEM);
//      } catch (IOException ioe) {
//        ErrorLog.defaultLog().warning("Couldn't create a NetworkTrafficMonitor because:" + ioe);
//        networkTrafficMonitor = new NetworkTrafficMonitor.NetworkTrafficMonitorNULL();
//      }
//    } else {
//      networkTrafficMonitor = new NetworkTrafficMonitor.NetworkTrafficMonitorNULL();
//    }
  }

  public static void initialiseForJTest()
  {
//    inGameStatusSimulator = new InGameStatusSimulator();       //todo testing only
  }

  public static void shutdown()
  {
    packetHandlerRegistryServer = null;
//    speedyToolServerActions = null;
//    speedyToolsNetworkServer = null;
//    speedyToolWorldManipulator = null;
//    serverVoxelSelections = null;
//    try {                                                        //todo testing only
//      networkTrafficMonitor.closeAll();
//    } catch (IOException ioe) {
//      // do nothing
//    }
  }

  public static int getGlobalTickCount() {
    return globalTickCount;
  }

  public static void tick()
  {
    ++globalTickCount;

//    getSpeedyToolsNetworkServer().tick();                         //todo testing only
//    getSpeedyToolServerActions().tick();
//    getServerVoxelSelections().tick();
//
//    if (globalTickCount % SpeedyToolsOptions.getNetworkLoggingPeriodInTicks() == 0) {
//      try {
//        ServerSide.getNetworkTrafficMonitor().log();
//      } catch (IOException ioe) {
//        ErrorLog.defaultLog().warning("Failed to log network traffic due to:" + ioe);
//      }
//    }
  }

  private static int globalTickCount = 0;

//  public static SpeedyToolsNetworkServer getSpeedyToolsNetworkServer() {          //todo testing only
//    return speedyToolsNetworkServer;
//  }
//  public static SpeedyToolServerActions getSpeedyToolServerActions() {
//    return speedyToolServerActions;
//  }
////  public static SpeedyToolWorldManipulator getSpeedyToolWorldManipulator() {
////    return speedyToolWorldManipulator;
////  }
//
//  private static SpeedyToolsNetworkServer speedyToolsNetworkServer;
//
//  public static ServerVoxelSelections getServerVoxelSelections() {
//    return serverVoxelSelections;
//  }
//  public static InGameStatusSimulator getInGameStatusSimulator() {
//    return inGameStatusSimulator;
//  }
//
//  private static ServerVoxelSelections serverVoxelSelections;
//  private static SpeedyToolServerActions speedyToolServerActions;
//  private static SpeedyToolWorldManipulator speedyToolWorldManipulator;
  private static PacketHandlerRegistryServer packetHandlerRegistryServer;
  private static InGameTester inGameTester;
//  private static InGameStatusSimulator inGameStatusSimulator;              //todo testing only
//  private static WorldHistory worldHistory;
//
//  public static NetworkTrafficMonitor getNetworkTrafficMonitor() {
//    return networkTrafficMonitor;
//  }
//
//  private static NetworkTrafficMonitor networkTrafficMonitor;
}
