package speedytools.serverside;

import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.WorldEvent;

/**
 Contains the custom Forge Event Handlers relevant to the Server
 */
public class ServerEventHandler
{
  @ForgeSubscribe
  public void worldLoad(WorldEvent.Load event)
  {
    if (!(event.world instanceof WorldServer)) return;
    SpeedyToolServerActions.worldLoadEvent(event.world);
  }

  @ForgeSubscribe
  public void worldUnload(WorldEvent.Unload event)
  {
    if (!(event.world instanceof WorldServer)) return;
    SpeedyToolServerActions.worldUnloadEvent(event.world);
  }
}
