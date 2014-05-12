package speedytools.clientside.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import speedytools.common.utilities.Colour;

/**
 * Created by TheGreyGhost on 9/05/14.
 */
public class RenderCursorStatus implements RendererElement
{

  public RenderCursorStatus(CursorRenderInfoUpdateLink i_infoProvider)
  {
    infoProvider = i_infoProvider;
    renderInfo = new CursorRenderInfo();
  }

  public boolean renderInThisPhase(RenderPhase renderPhase)
  {
    return (renderPhase == RenderPhase.CROSSHAIRS);
  }

  /**
   * renders the 'power up' cursor.  See CursorRenderInfo for the various controls
   * @param player
   * @param animationTickCount
   * @param partialTick
   */
  @Override
  public void render(RenderPhase renderPhase, EntityPlayer player, int animationTickCount, float partialTick)
  {
    boolean shouldIRender = infoProvider.refreshRenderInfo(renderInfo);
    if (!shouldIRender) return;

    double animationCounter = animationTickCount + partialTick;

    // key points
    // 1) if the animation is idle, draw nothing
    // 2) the animation rotation syncs itself when the caller transitions from idle = true to idle = false
    // 3) when idle returns to false, the animation spins down again.

    final int SPIN_DOWN_DURATION_TICKS = 20;
    final float SPIN_DEGREES_PER_TICK = 9.0F;

    switch (animationState) {
      case IDLE: {
        if (renderInfo.idle) return;
        animationState = AnimationState.SPIN_UP;
        spinOffsetTick = animationCounter;
        break;
      }
      case SPIN_UP: {
        if (renderInfo.idle) {
          animationState = AnimationState.SPIN_DOWN;
          sizeOffsetTick = animationCounter - SPIN_DOWN_DURATION_TICKS * (1 - starSize);
        }
        break;
      }
      case SPINNING: {
        if (renderInfo.idle) {
          animationState = AnimationState.SPIN_DOWN;
          sizeOffsetTick = animationCounter;
        }
        break;
      }
      case SPIN_DOWN: {
        if (renderInfo.idle) {
          if (animationCounter - sizeOffsetTick >= SPIN_DOWN_DURATION_TICKS) {
            animationState = AnimationState.IDLE;
          }
        } else {
          animationState = AnimationState.SPIN_UP;
        }
        break;
      }
      default: assert false : "illegal animationState:" + animationState;
    }

    final float Z_LEVEL_FROM_GUI_IN_GAME_FORGE = -90.0F;            // taken from GuiInGameForge.renderCrossHairs
    final double CROSSHAIR_ICON_WIDTH = 80;
    final double CROSSHAIR_ICON_HEIGHT = 80;
    final double CROSSHAIR_X_OFFSET = -CROSSHAIR_ICON_WIDTH / 2.0;
    final double CROSSHAIR_Y_OFFSET = -CROSSHAIR_ICON_HEIGHT / 2.0;

    Minecraft mc = Minecraft.getMinecraft();
    ScaledResolution scaledResolution = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
    int width = scaledResolution.getScaledWidth();
    int height = scaledResolution.getScaledHeight();

    try {
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

      final double MIN_RING_SIZE = 0.4;
      final double MAX_RING_SIZE = 1.0;
      final double STAR_COLOUR_MIN_INTENSITY = 0.5;
      final double RING_COLOUR_MIN_INTENSITY = 0.5;
      final double STAR_COLOUR_MAX_INTENSITY = 1.0;
      final double RING_COLOUR_MAX_INTENSITY = 1.0;

      double starColourIntensity, ringColourIntensity;

      switch (animationState) {
        case IDLE: {
          return;
        }
        case SPIN_UP: {
          starSize = renderInfo.chargePercent / 100.0;
          ringSize = MIN_RING_SIZE + (MAX_RING_SIZE - MIN_RING_SIZE) * renderInfo.readinessPercent / 100.0;
          starColourIntensity = STAR_COLOUR_MIN_INTENSITY;
          ringColourIntensity = RING_COLOUR_MAX_INTENSITY;
          break;
        }
        case SPINNING: {
          starSize = 1.0;
          ringSize = MIN_RING_SIZE + (MAX_RING_SIZE - MIN_RING_SIZE) * renderInfo.readinessPercent / 100.0;
          starColourIntensity = renderInfo.fullyChargedAndReady ? STAR_COLOUR_MAX_INTENSITY : STAR_COLOUR_MIN_INTENSITY;
          ringColourIntensity = RING_COLOUR_MAX_INTENSITY;
          break;
        }
        case SPIN_DOWN: {
          starSize = (animationCounter - sizeOffsetTick) / SPIN_DOWN_DURATION_TICKS;
          starColourIntensity = RING_COLOUR_MIN_INTENSITY;
          ringColourIntensity = STAR_COLOUR_MIN_INTENSITY;
          break;
          // ringSize saved
        }
        default: assert false : "illegal animationState:" + animationState; return;
      }

      double degreesOfRotation = (animationCounter - spinOffsetTick) * SPIN_DEGREES_PER_TICK;
      if (!renderInfo.isAnAction) degreesOfRotation = - degreesOfRotation;

      Colour lineColour = Colour.BLACK_40;
      switch (renderInfo.cursorType) {
        case COPY: { lineColour = Colour.GREEN_20; break;}
        case MOVE: { lineColour = Colour.YELLOW_20; break;}
        case DELETE: { lineColour = Colour.RED_100; break;}
        default: assert false : "illegal cursorType:" + renderInfo.cursorType;
      }

      GL11.glColor3d(lineColour.R * ringColourIntensity, lineColour.G * ringColourIntensity, lineColour.B * ringColourIntensity);
      mc.renderEngine.bindTexture(ringTexture);
      GL11.glPushMatrix();
      GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
      drawTexturedRectangle(CROSSHAIR_X_OFFSET * starSize * ringSize, CROSSHAIR_Y_OFFSET * starSize * ringSize, Z_LEVEL_FROM_GUI_IN_GAME_FORGE,
                            CROSSHAIR_ICON_WIDTH * starSize * ringSize, CROSSHAIR_ICON_HEIGHT * starSize * ringSize);
      GL11.glPopMatrix();

      GL11.glColor3d(lineColour.R * starColourIntensity, lineColour.G * starColourIntensity, lineColour.B * starColourIntensity);
      mc.renderEngine.bindTexture(octoStarTexture);
      GL11.glPushMatrix();
      GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
      GL11.glRotated(degreesOfRotation, 0, 0, 1.0F);
      drawTexturedRectangle(CROSSHAIR_X_OFFSET * starSize, CROSSHAIR_Y_OFFSET * starSize, Z_LEVEL_FROM_GUI_IN_GAME_FORGE,
                            CROSSHAIR_ICON_WIDTH * starSize, CROSSHAIR_ICON_HEIGHT * starSize);
      GL11.glPopMatrix();
    } finally {
      GL11.glPopAttrib();
    }
  }

  /**  The CursorRenderInfoUpdateLink and CursorRenderInfo are used to retrieve the necessary information for rendering from the current tool
   *  If refreshRenderInfo returns false, no render is performed.
   */
  public interface CursorRenderInfoUpdateLink
  {
    public boolean refreshRenderInfo(CursorRenderInfo infoToUpdate);
  }

  public static class CursorRenderInfo
  {
    public boolean idle;                  // if true - the cursor is either idle or is returning to idle
    public boolean isAnAction;            // true if the charging is for an action, false for an undo
    public boolean fullyChargedAndReady;  // if true - fully charged and ready to act as soon as user releases
    public float chargePercent;           // degree of charge up; 0.0 (min) - 100.0 (max)
    public float readinessPercent;        // completion percentage if waiting for another task to complete on server; 0.0 (min) - 100.0 (max)
    public CursorType cursorType;         // the cursor appearance
    public enum CursorType {
      COPY, MOVE, DELETE
    }
  }

  private CursorRenderInfoUpdateLink infoProvider;
  private CursorRenderInfo renderInfo = new CursorRenderInfo();
  private AnimationState animationState;
  private double spinOffsetTick;
  private double sizeOffsetTick;
  private double starSize;
  private double ringSize;

  private enum AnimationState {
    IDLE, SPIN_UP, SPINNING, SPIN_DOWN
  }

//    /**
//     * Draw the custom crosshairs if reqd
//     * Otherwise, cancel the event so that the normal selection box is drawn.
//     *
//     * @param event
//     */
//    @ForgeSubscribe
//    public void renderOverlayPre(RenderGameOverlayEvent.Pre event) {
//      if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
//      EntityPlayer player = Minecraft.getMinecraft().thePlayer;
//      boolean customRender = renderCrossHairs(event.resolution, event.partialTicks);
//      event.setCanceled(customRender);
//      return;
//    }

  /**
   * Draws a textured rectangle at the given z-value, using the entire texture. Args: x, y, z, width, height
   */
  private void drawTexturedRectangle(double x, double y, double z, double width, double height)
  {
    double ICON_MIN_U = 0.0;
    double ICON_MAX_U = 1.0;
    double ICON_MIN_V = 0.0;
    double ICON_MAX_V = 1.0;
    Tessellator tessellator = Tessellator.instance;
    tessellator.startDrawingQuads();
    tessellator.addVertexWithUV(    x + 0, y + height, z,  ICON_MIN_U, ICON_MAX_V);
    tessellator.addVertexWithUV(x + width, y + height, z,  ICON_MAX_U, ICON_MAX_V);
    tessellator.addVertexWithUV(x + width,      y + 0, z,  ICON_MAX_U, ICON_MIN_V);
    tessellator.addVertexWithUV(    x + 0,      y + 0, z,  ICON_MIN_U, ICON_MIN_V);
    tessellator.draw();
  }


  /**
   * Draw an arc centred around the zero point.  Setup translatef, colour and line width etc before calling.
   * @param radius
   * @param startAngle clockwise starting from 12 O'clock
   * @param endAngle
   */
  void drawArc(double radius, double startAngle, double endAngle, double zLevel)
  {
    final double angleIncrement = Math.toRadians(10.0);
    float direction = (endAngle >= startAngle) ? 1.0F : -1.0F;
    double deltaAngle = Math.abs(endAngle - startAngle);
    deltaAngle %= 360.0;

    startAngle -= Math.floor(startAngle/360.0);
    startAngle = Math.toRadians(startAngle);
    deltaAngle = Math.toRadians(deltaAngle);

    GL11.glBegin(GL11.GL_LINE_STRIP);

    double x, y;
    double arcPos = 0;
    boolean arcFinished = false;
    do {
      double truncAngle = Math.min(arcPos, deltaAngle);
      x = radius * Math.sin(startAngle + direction * truncAngle);
      y = -radius * Math.cos(startAngle + direction * truncAngle);
      GL11.glVertex3d(x, y, zLevel);

      arcFinished = (arcPos >= deltaAngle);
      arcPos += angleIncrement;
    } while (!arcFinished);
    GL11.glEnd();
  }


  private final ResourceLocation octoStarTexture = new ResourceLocation("testitemrendering", "textures/other/octostar.png");
  private final ResourceLocation ringTexture = new ResourceLocation("testitemrendering", "textures/other/octoring.png");


}
