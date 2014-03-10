package speedytools.serverside;

/**
 * User: The Grey Ghost
 * Date: 10/03/14
 * Contains the various objects that define the server side
 */
public class ServerSide
{
  public static void initialise()
  {
    cloneToolServerActions = new CloneToolServerActions();
    cloneToolsNetworkServer = new CloneToolsNetworkServer(cloneToolServerActions);
    speedyToolWorldManipulator = new SpeedyToolWorldManipulator();
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

}
