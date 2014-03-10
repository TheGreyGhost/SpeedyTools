package speedytools.clientside;

import speedytools.serverside.CloneToolServerActions;
import speedytools.serverside.CloneToolsNetworkServer;

/**
 * User: The Grey Ghost
 * Date: 10/03/14
 * Contains the various objects that define the server side
 */
public class ClientSide
{
  public static void initialise()
  {
    cloneToolsNetworkClient = new CloneToolsNetworkClient();
  }

  public static void shutdown()
  {
    cloneToolsNetworkClient = null;
  }


  public static CloneToolsNetworkClient getCloneToolsNetworkClient() {
    return cloneToolsNetworkClient;
  }

  private static CloneToolsNetworkClient cloneToolsNetworkClient;

}
