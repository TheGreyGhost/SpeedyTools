package speedytools.clientside.rendering;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;
import speedytools.clientside.ClientSide;

import java.util.*;

/**
* User: The Grey Ghost
* Date: 14/04/14
*/
public class SpeedyToolRenderers
{
  public SpeedyToolRenderers()
  {
    rendererElements = new LinkedList<RendererElement>();
  }

  /**
   * Set the renderers to be used for displaying the current tool
   * @param newRendererElements the collection of renderers, or null for none.
   */
  public void setRenderers(Collection<RendererElement> newRendererElements)
  {
    rendererElements = newRendererElements;
    if (rendererElements == null) rendererElements = new LinkedList<RendererElement>();

    // construct a map of the renderers to be called for each event
    renderElements1.clear();
    if (newRendererElements != null) {
      for (RendererElement rendererElement : newRendererElements) {
        Collection<Class<? extends Event>> eventsToRegisterFor = rendererElement.eventsToReceive();
        if (eventsToRegisterFor != null) {
          for (Class<? extends Event> event : eventsToRegisterFor) {
            List<RendererElement> currentEntries = renderElements1.get(event);
            if (currentEntries == null) {
              currentEntries = new LinkedList<RendererElement>();
            }
            currentEntries.add(rendererElement);
            renderElements1.put(event, currentEntries);
          }
        }
      }
    }
  }

  public void render(Event event, float partialTick)
  {
    Collection<RendererElement> renderElements = renderElements1.get(event.getClass());
    if (renderElements == null) return;
    for (RendererElement rendererElement : renderElements) {
      rendererElement.render(event, partialTick);
    }
  }

  private Collection<RendererElement> rendererElements;

  private Map<Class <? extends Event>, List<RendererElement>> renderElements1 = new HashMap<Class<? extends Event>, List<RendererElement>>();
}
