package speedytools.serverside;

import speedytools.common.network.PacketHandlerRegistry;

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
    cloneToolServerActions = new CloneToolServerActions();
    cloneToolsNetworkServer = new CloneToolsNetworkServer(packetHandlerRegistry, cloneToolServerActions);
    speedyToolWorldManipulator = new SpeedyToolWorldManipulator(packetHandlerRegistry);
  }

  public static void shutdown()
  {
    cloneToolServerActions = null;
    cloneToolsNetworkServer = null;
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
  private static CloneToolServerActions cloneToolServerActions;
  private static SpeedyToolWorldManipulator speedyToolWorldManipulator;
  private static PacketHandlerRegistry packetHandlerRegistry;
}
