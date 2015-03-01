package speedytools.clientside;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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
