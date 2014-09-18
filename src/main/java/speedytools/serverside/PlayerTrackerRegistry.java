package speedytools.serverside;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * User: The Grey Ghost
 * Date: 18/09/2014
 * Used to register handlers for player events, similar to how the IPlayerTracker and GameRegistry used to work.
 */
public class PlayerTrackerRegistry
{
  public void registerHandler(IPlayerTracker handler)
  {
    handlers.add(handler);
  }

  public Set<IPlayerTracker> getHandlers() {return handlers;}

  public interface IPlayerTracker
  {
    public void onPlayerLogin(EntityPlayer player);
    public void onPlayerLogout(EntityPlayer player);
    public void onPlayerChangedDimension(EntityPlayer player);
    public void onPlayerRespawn(EntityPlayer player);
  }

  @SubscribeEvent
  public  void playerLogon(PlayerEvent.PlayerLoggedInEvent playerLoggedInEvent)
  {
    for (IPlayerTracker tracker : handlers) {
      tracker.onPlayerLogin(playerLoggedInEvent.player);
    }
  }

  @SubscribeEvent
  public  void playerLogoff(PlayerEvent.PlayerLoggedOutEvent playerLoggedOutEvent)
  {
    for (IPlayerTracker tracker : handlers) {
      tracker.onPlayerLogout(playerLoggedOutEvent.player);
    }
  }

   Set<IPlayerTracker> handlers = new HashSet<IPlayerTracker>();
}
