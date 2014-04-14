package speedytools.clientside.rendering;

import net.minecraft.entity.player.EntityPlayer;

/**
 * User: The Grey Ghost
 * Date: 14/04/14
 */
public interface RendererElement
{
  public void render(RenderPhase renderPhase, EntityPlayer player, float partialTick);
  public boolean renderInThisPhase(RenderPhase renderPhase);

  public enum RenderPhase {
    CROSSHAIR, WORLD,
  }

}
