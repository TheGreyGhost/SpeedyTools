package speedytools.clientside.rendering;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.Event;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: The Grey Ghost
 * Date: 18/04/2014
 *  * This class is used to render the boundary field (translucent cuboid)
 * Usage:
 * (1) Call the constructor, providing a BoundaryFieldRenderInfoUpdateLink:
 *     This interface is used to fill the supplied BoundaryFieldRenderInfo with the requested information for a render.
 * (2) When ready to render, call .render.

 */
public class RendererBoundaryField implements RendererElement
{
  public RendererBoundaryField(BoundaryFieldRenderInfoUpdateLink i_infoProvider)
  {
    infoProvider = i_infoProvider;
    renderInfo = new BoundaryFieldRenderInfo();
  }

  @Override
  public boolean renderInThisPhase(RenderPhase renderPhase)
  {
    return (renderPhase == RenderPhase.WORLD);
  }

  /**
   * Which events is this RendererElement interested in?
   *
   * @return a collection of events that the Renderer wants to receive.
   */
  @Override
  public Collection<Class<? extends Event>> eventsToReceive() {
    ArrayList<Class<? extends Event>> retval = new ArrayList<Class<? extends Event>>();
    retval.add(RenderWorldLastEvent.class);
    return retval;
  }

  /**
   * render this element in response to the given event
   *
   * @param event
   * @param world
   * @param player
   * @param animationTickCount
   * @param partialTick
   */
  @Override
  public void render(Event event, World world, EntityPlayer player, int animationTickCount, float partialTick) {

  }

  @Override
  public void renderOverlay(RenderPhase renderPhase, ScaledResolution scaledResolution, int animationTickCount, float partialTick)
  {
    assert false : "invalid render phase: " + renderPhase;
  }

  /**
   * render the boundary field if there is one selected
   * @param player
   * @param animationTickCount
   * @param partialTick
   */
  @Override
  public void renderWorld(RenderPhase renderPhase, EntityPlayer player, int animationTickCount, float partialTick)
  {
    Vec3 playerPosition = player.getPosition(partialTick);
    boolean shouldIRender = infoProvider.refreshRenderInfo(renderInfo, playerPosition);
    if (!shouldIRender) return;

    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
    GL11.glLineWidth(2.0F);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDepthMask(false);
    double EXPAND_BOX_DISTANCE = 0.002F;

    AxisAlignedBB boundingBox = renderInfo.boundaryFieldAABB;
    boundingBox = boundingBox.expand(EXPAND_BOX_DISTANCE, EXPAND_BOX_DISTANCE, EXPAND_BOX_DISTANCE)
            .getOffsetBoundingBox(-playerPosition.xCoord, -playerPosition.yCoord, -playerPosition.zCoord);
    int faceToHighlight = -1;
    if (renderInfo.boundaryGrabActivated) {
      faceToHighlight = renderInfo.boundaryGrabSide;
    } else {
      faceToHighlight = renderInfo.boundaryCursorSide;
    }
    SelectionBoxRenderer.drawFilledCubeWithSelectedSide(boundingBox, faceToHighlight, renderInfo.boundaryGrabActivated);

    GL11.glDepthMask(true);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
  }

  /**  The BoundaryFieldRenderInfoUpdateLink and BoundaryFieldRenderInfo are used to retrieve the necessary information for rendering from the current tool
   *  If refreshRenderInfo returns false, no render is performed.
   */
  public interface BoundaryFieldRenderInfoUpdateLink
  {
    public boolean refreshRenderInfo(BoundaryFieldRenderInfo infoToUpdate, Vec3 playerPosition);
  }

  public static class BoundaryFieldRenderInfo
  {
    public AxisAlignedBB boundaryFieldAABB;
    public boolean boundaryGrabActivated;
    public int boundaryGrabSide;
    public int boundaryCursorSide;
  }

  BoundaryFieldRenderInfoUpdateLink infoProvider;
  BoundaryFieldRenderInfo renderInfo;
}
