package speedytools.clientside.rendering;

import net.minecraft.client.gui.ScaledResolution;
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
}
