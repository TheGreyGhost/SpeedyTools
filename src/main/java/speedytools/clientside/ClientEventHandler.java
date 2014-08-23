package speedytools.clientside;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.WorldEvent;
import speedytools.serverside.actions.SpeedyToolServerActions;

/**
 Contains the custom Forge Event Handlers relevant to the Client
 */
public class ClientEventHandler
{
  @ForgeSubscribe
  public void worldLoad(WorldEvent.Load event)
  {
    if (!(event.world instanceof WorldClient)) return;
    ClientSide.activeTool.resetAllTools();
  }
}
