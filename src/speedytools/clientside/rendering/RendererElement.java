package speedytools.clientside.rendering;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;

/**
 * User: The Grey Ghost
 * Date: 14/04/14
 */
public interface RendererElement
{
  /** renders an element in the world
   * @param renderPhase
   * @param player
   * @param animationTickCount
   * @param partialTick
   */
  public void renderWorld(RenderPhase renderPhase, EntityPlayer player, int animationTickCount, float partialTick);

  /** renders an element on the overlay
   * @param renderPhase
   * @param scaledResolution
   * @param animationTickCount
   * @param partialTick
   */
  public void renderOverlay(RenderPhase renderPhase, ScaledResolution scaledResolution, int animationTickCount, float partialTick);
  public boolean renderInThisPhase(RenderPhase renderPhase);

  public enum RenderPhase {
    CROSSHAIRS, WORLD,
  }

}
