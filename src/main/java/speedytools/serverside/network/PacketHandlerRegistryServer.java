package speedytools.serverside.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayerMP;
import speedytools.common.network.Packet250Base;
import speedytools.common.network.Packet250Types;
import speedytools.common.network.PacketHandlerRegistry;

/**
 * User: The Grey Ghost
 * Date: 7/09/2014
 */
public class PacketHandlerRegistryServer extends PacketHandlerRegistry
{
  public void sendToClientSinglePlayer(IMessage message, EntityPlayerMP entityPlayerMP)
  {
    simpleNetworkWrapper.sendTo(message, entityPlayerMP);
  }
}
