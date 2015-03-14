package speedytools.serverside;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
* User: The Grey Ghost
* Date: 2/11/13
*/
public class ServerTickHandler  {

  /**
   *
   * tick any objects which need it
   */
  @SubscribeEvent
  public void tickEnd(TickEvent.ServerTickEvent serverTickEvent)
  {
    if (serverTickEvent.phase == TickEvent.Phase.END) {
      ServerSide.tick();
    }
  }
}
