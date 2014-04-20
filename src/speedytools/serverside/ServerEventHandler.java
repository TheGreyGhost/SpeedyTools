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
    assert (event.world instanceof WorldServer);
    CloneToolServerActions.worldLoadEvent(event.world);
  }

  @ForgeSubscribe
  public void worldUnload(WorldEvent.Unload event)
  {
    assert (event.world instanceof WorldServer);
    CloneToolServerActions.worldUnloadEvent(event.world);
  }
}
