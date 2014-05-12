package speedytools.clientside.rendering;

import net.minecraft.entity.player.EntityPlayer;

import java.util.Collection;
import java.util.LinkedList;

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
  public void render(RendererElement.RenderPhase renderPhase, EntityPlayer player, float partialTick)
  {
    for (RendererElement rendererElement : rendererElements) {
      rendererElement.render(renderPhase, player, , partialTick);
    }
  }

  private Collection<RendererElement> rendererElements;
}
