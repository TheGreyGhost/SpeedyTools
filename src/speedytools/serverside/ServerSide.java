package speedytools.serverside;

import cpw.mods.fml.relauncher.Side;
import speedytools.common.SpeedyToolsOptions;
import speedytools.common.network.NetworkTrafficMonitor;
import speedytools.common.network.PacketHandlerRegistry;
import speedytools.common.utilities.ErrorLog;
import speedytools.serverside.ingametester.InGameStatusSimulator;
import speedytools.serverside.ingametester.InGameTester;
import speedytools.serverside.worldmanipulation.WorldHistory;

import java.io.IOException;

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
    worldHistory = new WorldHistory(SpeedyToolsOptions.getMaxComplexToolUndoCount(), SpeedyToolsOptions.getMaxSimpleToolUndoCount());
    speedyToolServerActions = new SpeedyToolServerActions(serverVoxelSelections, worldHistory);
    speedyToolsNetworkServer = new SpeedyToolsNetworkServer(packetHandlerRegistry, speedyToolServerActions);
//    speedyToolWorldManipulator = new SpeedyToolWorldManipulator(packetHandlerRegistry, worldHistory);
    inGameTester = new InGameTester(packetHandlerRegistry);
    inGameStatusSimulator = new InGameStatusSimulator();

    String NETWORK_LOG_FILENAME_STEM = "NetworkMonitor";
    if (SpeedyToolsOptions.getNetworkLoggingActive()) {
      try {
        networkTrafficMonitor = new NetworkTrafficMonitor(Side.SERVER, SpeedyToolsOptions.getNetworkLoggingDirectory().toPath(), NETWORK_LOG_FILENAME_STEM);
      } catch (IOException ioe) {
        ErrorLog.defaultLog().warning("Couldn't create a NetworkTrafficMonitor because:" + ioe);
        networkTrafficMonitor = new NetworkTrafficMonitor.NetworkTrafficMonitorNULL();
      }
    } else {
      networkTrafficMonitor = new NetworkTrafficMonitor.NetworkTrafficMonitorNULL();
    }
  }

  public static void initialiseForJTest()
  {
    inGameStatusSimulator = new InGameStatusSimulator();
  }

  public static void shutdown()
  {
    packetHandlerRegistry = null;
    speedyToolServerActions = null;
    speedyToolsNetworkServer = null;
//    speedyToolWorldManipulator = null;
    serverVoxelSelections = null;
    try {
      networkTrafficMonitor.closeAll();
    } catch (IOException ioe) {
      // do nothing
    }
  }

  public static int getGlobalTickCount() {
    return globalTickCount;
  }

  public static void tick()
  {
    ++globalTickCount;

    getSpeedyToolsNetworkServer().tick();
    getSpeedyToolServerActions().tick();
    getServerVoxelSelections().tick();

    if (globalTickCount % SpeedyToolsOptions.getNetworkLoggingPeriodInTicks() == 0) {
      try {
        ServerSide.getNetworkTrafficMonitor().log();
      } catch (IOException ioe) {
        ErrorLog.defaultLog().warning("Failed to log network traffic due to:" + ioe);
      }
    }
  }

  private static int globalTickCount = 0;

  public static SpeedyToolsNetworkServer getSpeedyToolsNetworkServer() {
    return speedyToolsNetworkServer;
  }
  public static SpeedyToolServerActions getSpeedyToolServerActions() {
    return speedyToolServerActions;
  }
//  public static SpeedyToolWorldManipulator getSpeedyToolWorldManipulator() {
//    return speedyToolWorldManipulator;
//  }

  private static SpeedyToolsNetworkServer speedyToolsNetworkServer;

  public static ServerVoxelSelections getServerVoxelSelections() {
    return serverVoxelSelections;
  }
  public static InGameStatusSimulator getInGameStatusSimulator() {
    return inGameStatusSimulator;
  }

  private static ServerVoxelSelections serverVoxelSelections;
  private static SpeedyToolServerActions speedyToolServerActions;
//  private static SpeedyToolWorldManipulator speedyToolWorldManipulator;
  private static PacketHandlerRegistry packetHandlerRegistry;
  private static InGameTester inGameTester;
  private static InGameStatusSimulator inGameStatusSimulator;
  private static WorldHistory worldHistory;

  public static NetworkTrafficMonitor getNetworkTrafficMonitor() {
    return networkTrafficMonitor;
  }

  private static NetworkTrafficMonitor networkTrafficMonitor;
}
