package speedytools.clientside;

import speedytools.clientside.network.CloneToolsNetworkClient;

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
  }

  public static void shutdown()
  {
    cloneToolsNetworkClient = null;
  }


  public static CloneToolsNetworkClient getCloneToolsNetworkClient() {
    return cloneToolsNetworkClient;
  }

  public static CloneToolsNetworkClient cloneToolsNetworkClient;

}
