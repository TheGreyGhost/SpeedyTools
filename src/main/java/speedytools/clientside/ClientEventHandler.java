package speedytools.clientside;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.WorldEvent;

/**
Contains the custom Forge Event Handlers relevant to the Client
*/
public class ClientEventHandler
{
  @SubscribeEvent
  public void worldLoad(WorldEvent.Load event)
  {
    if (!(event.world instanceof WorldClient)) return;
    ClientSide.activeTool.resetAllTools();
  }
}
