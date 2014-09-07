package speedytools.clientside.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import speedytools.common.network.PacketHandlerRegistry;

/**
 * User: The Grey Ghost
 * Date: 7/09/2014
 */
public class PacketHandlerRegistryClient extends PacketHandlerRegistry
{
   public void sendToServer(IMessage message)
  {
    simpleNetworkWrapper.sendToServer(message);
  }
}
