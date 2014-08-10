package speedytools.clientside.rendering;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.Event;

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
    for (RendererElement rendererElement : newRendererElements) {
      Collection<Class<? extends Event>> eventsToRegisterFor = rendererElement.eventsToReceive();
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

  public void render(Event event, World world, EntityPlayer player, int animationTickCount, float partialTick)
  {
    Collection<RendererElement> renderElements = renderElements1.get(event.getClass());
    for (RendererElement rendererElement : renderElements) {
      rendererElement.render(event, world, player, animationTickCount, partialTick);
    }
  }

  /**
   * Do any of the renderers draw something in this phase?
   * @param renderPhase
   * @return
   */
  public boolean areAnyRendersInThisPhase(RendererElement.RenderPhase renderPhase)
  {
    for (RendererElement rendererElement : rendererElements) {
      if (rendererElement.renderInThisPhase(renderPhase)) return true;
    }
    return false;
  }

  /**
   * Render all elements which draw something in this phase
   * @param renderPhase
   * @param player
   * @param partialTick
   */
  public void renderWorld(RendererElement.RenderPhase renderPhase, EntityPlayer player, int animationTickCount, float partialTick)
  {
    for (RendererElement rendererElement : rendererElements) {
      if (rendererElement.renderInThisPhase(renderPhase)) {
        rendererElement.renderWorld(renderPhase, player, animationTickCount, partialTick);
      }
    }
  }

  /**
   * Render all elements which draw something in this phase
   * @param renderPhase
   * @param scaledResolution
   * @param partialTick
   */
  public void renderOverlay(RendererElement.RenderPhase renderPhase, ScaledResolution scaledResolution, int animationTickCount, float partialTick)
  {
    for (RendererElement rendererElement : rendererElements) {
      if (rendererElement.renderInThisPhase(renderPhase)) {
        rendererElement.renderOverlay(renderPhase, scaledResolution, animationTickCount, partialTick);
      }
    }
  }

  private Collection<RendererElement> rendererElements;

  private Map<Class <? extends Event>, List<RendererElement>> renderElements1 = new HashMap<Class<? extends Event>, List<RendererElement>>();
}
