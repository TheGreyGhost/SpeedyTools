package speedytools.clientside.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import cpw.mods.fml.common.eventhandler.Event;
import org.lwjgl.opengl.GL11;
import speedytools.clientside.ClientSide;
import speedytools.common.utilities.Colour;

import java.util.ArrayList;
import java.util.Collection;

/**
* User: The Grey Ghost
* Date: 14/04/14
* This class is used to render a wireframe around a collection of cubes.
* Usage:
* (1) Call the constructor, providing a WireframeRenderInfoUpdateLink:
*     This interface is used to fill the supplied WireframeRenderInfo with the requested information for a render.
* (2) When ready to render, call .render.
*/
public class RendererWireframeSelection implements RendererElement
{
  public RendererWireframeSelection(WireframeRenderInfoUpdateLink i_infoProvider)
  {
    infoProvider = i_infoProvider;
    renderInfo = new WireframeRenderInfo();
  }

  public final int SELECTION_BOX_STYLE = 0; //0 = cube, 1 = cube with cross on each side

  @Override
  public Collection<Class<? extends Event>> eventsToReceive() {
    ArrayList<Class<? extends Event>> retval = new ArrayList<Class<? extends Event>>();
    retval.add(RenderWorldLastEvent.class);
    return retval;
  }

  @Override
  public void render(Event event, float partialTick) {
    RenderWorldLastEvent fullEvent = (RenderWorldLastEvent)event;
    RenderGlobal context = fullEvent.context;
    EntityPlayer player = (EntityPlayer)(Minecraft.getMinecraft().renderViewEntity);
    renderWorld(player, ClientSide.getGlobalTickCount(), partialTick);
  }

  public void renderWorld(EntityPlayer player, int animationTickCount, float partialTick)
  {
    boolean shouldIRender = infoProvider.refreshRenderInfo(renderInfo);
    if (!shouldIRender) return;

    try {
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
      GL11.glDepthMask(false);

      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

      // just white
      float cyclePosition = 0.0F;
      float red = Colour.WHITE_40.R + (Colour.BLACK_40.R - Colour.WHITE_40.R) * cyclePosition;
      float green = Colour.WHITE_40.R + (Colour.BLACK_40.G - Colour.WHITE_40.G) * cyclePosition;
      float blue = Colour.WHITE_40.R + (Colour.BLACK_40.B - Colour.WHITE_40.B) * cyclePosition;

      GL11.glColor4f(red, green, blue, Colour.BLACK_40.A);
      GL11.glLineWidth(2.0F);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      double expandDistance = 0.002F;

      Vec3 playerOrigin = player.getPosition(partialTick);

      AxisAlignedBB boundingBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
      for (ChunkCoordinates block : renderInfo.currentlySelectedBlocks) {
        boundingBox.setBounds(block.posX, block.posY, block.posZ, block.posX + 1, block.posY + 1, block.posZ + 1);
        boundingBox = boundingBox.expand(expandDistance, expandDistance, expandDistance).getOffsetBoundingBox(-playerOrigin.xCoord, -playerOrigin.yCoord, -playerOrigin.zCoord);
        switch (SELECTION_BOX_STYLE) {
          case 0: {
            SelectionBoxRenderer.drawCube(boundingBox);
            break;
          }
          case 1: {
            SelectionBoxRenderer.drawFilledCube(boundingBox);
            break;
          }
        }
      }
    } finally {
      GL11.glDepthMask(true);
      GL11.glPopAttrib();
    }
  }

  /**  The WireframeRenderInfoUpdateLink and WireframeRenderInfo are used to retrieve the necessary information for rendering from the current tool
   *  If refreshRenderInfo returns false, no render is performed.
   */
  public interface WireframeRenderInfoUpdateLink {
    public boolean refreshRenderInfo(WireframeRenderInfo infoToUpdate);
  }

  public static class WireframeRenderInfo {
    public Collection<ChunkCoordinates> currentlySelectedBlocks;
  }

  WireframeRenderInfoUpdateLink infoProvider;
  WireframeRenderInfo renderInfo;
}
