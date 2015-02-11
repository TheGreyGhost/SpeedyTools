package speedytools.clientside.rendering;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.Collection;

/**
* User: The Grey Ghost
* Date: 14/04/14
*/
public interface RendererElement
{
  /**
   * Which events is this RendererElement interested in?
   * @return a collection of events that the Renderer wants to receive.
   */
  public Collection<Class<? extends Event>> eventsToReceive();

  /**
   * render this element in response to the given event
   * @param partialTick
   */
  public void render(Event event, float partialTick);
}
