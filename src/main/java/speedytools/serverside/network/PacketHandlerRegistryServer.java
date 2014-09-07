package speedytools.serverside.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.relauncher.Side;
import speedytools.common.network.Packet250Base;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.PacketHandlerRegistry;

/**
 * User: The Grey Ghost
 * Date: 7/09/2014
 */
public class PacketHandlerRegistryServer extends PacketHandlerRegistry
{
  public void registerHandlerMethod(PacketHandlerMethod packetHandlerMethod, Packet250Base packet250Base)
  {
    packet250Base.registerHandler(simpleNetworkWrapper, packetHandlerMethod, Side.SERVER);
  }

  public <T extends Packet250Base> void registerHandlerMethod(IMessageHandler<T, IMessage> handler, Class<T> packet,  Packet250Types packet250Type)
  {
    registerHandlerMethod(handler, packet, packet250Type, Side.SERVER);
  }

}
