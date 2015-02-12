//package speedytools.serverside;
//
//import net.minecraftforge.fml.relauncher.Side;
//import speedytools.common.SpeedyToolsOptions;
//import speedytools.common.network.NetworkTrafficMonitor;
//import speedytools.common.utilities.ErrorLog;
//import speedytools.serverside.actions.SpeedyToolServerActions;
//import speedytools.serverside.ingametester.InGameStatusSimulator;
//import speedytools.serverside.ingametester.InGameTester;
//import speedytools.serverside.network.PacketHandlerRegistryServer;
//import speedytools.serverside.network.SpeedyToolsNetworkServer;
//import speedytools.serverside.worldmanipulation.WorldHistory;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Path;
//
///**
//* User: The Grey Ghost
//* Date: 10/03/14
//* Contains the various objects that define the server side
//*/
//public class ServerSide
//{
//  public static void load()
//  {
//    packetHandlerRegistryServer = new PacketHandlerRegistryServer() ;
//    serverVoxelSelections = new ServerVoxelSelections(packetHandlerRegistryServer, playerTrackerRegistry);
//    worldHistory = new WorldHistory(SpeedyToolsOptions.getMaxComplexToolUndoCount(), SpeedyToolsOptions.getMaxSimpleToolUndoCount());
//    speedyToolServerActions = new SpeedyToolServerActions(serverVoxelSelections, worldHistory);
//    speedyToolsNetworkServer = new SpeedyToolsNetworkServer(packetHandlerRegistryServer, speedyToolServerActions, playerTrackerRegistry);
//    inGameTester = new InGameTester(packetHandlerRegistryServer);
//    inGameStatusSimulator = new InGameStatusSimulator();
//
//    String NETWORK_LOG_FILENAME_STEM = "NetworkMonitor";
//    if (SpeedyToolsOptions.getNetworkLoggingActive()) {
//      try {
//        File loggingDirectory = SpeedyToolsOptions.getNetworkLoggingDirectory();
//        Path loggingPath = loggingDirectory == null ? null : loggingDirectory.toPath();
//        networkTrafficMonitor = new NetworkTrafficMonitor(Side.SERVER, loggingPath, NETWORK_LOG_FILENAME_STEM);
//      } catch (IOException ioe) {
//        ErrorLog.defaultLog().info("Couldn't create a NetworkTrafficMonitor because:" + ioe);
//        networkTrafficMonitor = new NetworkTrafficMonitor.NetworkTrafficMonitorNULL();
//      }
//    } else {
//      networkTrafficMonitor = new NetworkTrafficMonitor.NetworkTrafficMonitorNULL();
//    }
//  }
//
//  public static void initialiseForJTest()
//  {
//    inGameStatusSimulator = new InGameStatusSimulator();
//  }
//
//  public static void shutdown()
//  {
//    packetHandlerRegistryServer = null;
//    speedyToolServerActions = null;
//    speedyToolsNetworkServer = null;
//    serverVoxelSelections = null;
//    try {
//      networkTrafficMonitor.closeAll();
//    } catch (IOException ioe) {
//      // do nothing
//    }
//  }
//
//  public static int getGlobalTickCount() {
//    return globalTickCount;
//  }
//
//  public static void tick()
//  {
//    ++globalTickCount;
//
//    getSpeedyToolServerActions().tick();
//    getSpeedyToolsNetworkServer().tick();
//    long maxTimeForSelectionGeneration = SpeedyToolsOptions.getMaxServerSelGenTimeMS();
//    if (getSpeedyToolServerActions().isAsynchronousActionInProgress()) {  // no selection generation if asynch task underway
//      maxTimeForSelectionGeneration = 0;
//    }
//    final long NS_PER_MS = 1000 * 1000;
//    getServerVoxelSelections().tick(maxTimeForSelectionGeneration * NS_PER_MS);
//
//    if (globalTickCount % SpeedyToolsOptions.getNetworkLoggingPeriodInTicks() == 0) {
//      try {
//        ServerSide.getNetworkTrafficMonitor().log();
//      } catch (IOException ioe) {
//        ErrorLog.defaultLog().info("Failed to log network traffic due to:" + ioe);
//      }
//    }
//  }
//
//  private static int globalTickCount = 0;
//
//  public static SpeedyToolsNetworkServer getSpeedyToolsNetworkServer() {
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
//  public static PlayerTrackerRegistry getPlayerTrackerRegistry() {
//    return playerTrackerRegistry;
//  }
//
//  private static ServerVoxelSelections serverVoxelSelections;
//  private static SpeedyToolServerActions speedyToolServerActions;
////  private static SpeedyToolWorldManipulator speedyToolWorldManipulator;
//  private static PacketHandlerRegistryServer packetHandlerRegistryServer;
//  private static InGameTester inGameTester;
//  private static InGameStatusSimulator inGameStatusSimulator;
//  private static WorldHistory worldHistory;
//
//  private static PlayerTrackerRegistry playerTrackerRegistry = new PlayerTrackerRegistry();
//
//  public static NetworkTrafficMonitor getNetworkTrafficMonitor() {
//    return networkTrafficMonitor;
//  }
//
//  private static NetworkTrafficMonitor networkTrafficMonitor;
//}
