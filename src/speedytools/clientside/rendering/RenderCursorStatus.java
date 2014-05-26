package speedytools.clientside.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import speedytools.common.utilities.Colour;
import speedytools.common.utilities.UsefulFunctions;

/**
 * Created by TheGreyGhost on 9/05/14.
 *  This class is used to render the cursor showing the "power up" status for cloning a selection
 * Usage:
 * (1) Call the constructor, providing a CursorRenderInfoUpdateLink:
 *     This interface is used to fill the supplied CursorRenderInfo with the requested information for a render.
 * (2) When ready to render, call .render.
 * See CursorRenderInfo class definition for more information
 */
public class RenderCursorStatus implements RendererElement
{

  public RenderCursorStatus(CursorRenderInfoUpdateLink i_infoProvider)
  {
    infoProvider = i_infoProvider;
    renderInfo = new CursorRenderInfo();
  }

  @Override
  public boolean renderInThisPhase(RenderPhase renderPhase)
  {
    return (renderPhase == RenderPhase.CROSSHAIRS);
  }

  @Override
  public void renderWorld(RenderPhase renderPhase, EntityPlayer player, int animationTickCount, float partialTick)
  {
    assert false : "invalid render phase: " + renderPhase;
  }

  /**
   * renders the 'power up' cursor.  See CursorRenderInfo for the various controls
   * @param scaledResolution
   * @param animationTickCount
   * @param partialTick
   */
  @Override
  public void renderOverlay(RenderPhase renderPhase, ScaledResolution scaledResolution, int animationTickCount, float partialTick)
  {
    if (renderPhase != RenderPhase.CROSSHAIRS) return;
    boolean shouldIRender = infoProvider.refreshRenderInfo(renderInfo);
    if (!shouldIRender) return;

    final int SPIN_DOWN_DURATION_TICKS = 20;
    final float SPIN_DEGREES_PER_TICK = 9.0F;
    final double INITIAL_TASK_COMPLETION_ANGLE = 5.0;  // always show at least this much "task completion" angle

    double animationCounter = animationTickCount + partialTick;
    double lastDegreesOfRotation = degreesOfRotation;
    degreesOfRotation = (animationCounter - spinStartTick) * SPIN_DEGREES_PER_TICK;

    switch (animationState) {
      case IDLE: {
        if (!renderInfo.idle) {
          animationState = AnimationState.SPIN_UP;
          spinStartTick = animationCounter;
        }
        break;
      }
      case SPIN_UP: {
        if (renderInfo.performingTask) {
          animationState = AnimationState.SPINNING;
          taskCompletionRingAngle = INITIAL_TASK_COMPLETION_ANGLE;
        } else if (renderInfo.idle) {
          animationState = AnimationState.SPIN_DOWN;
          spindownCompletionTickCount = animationCounter + SPIN_DOWN_DURATION_TICKS * starSize;
        }
        break;
      }
      case SPINNING: {
        if (!renderInfo.performingTask) {
          animationState = AnimationState.SPIN_DOWN;
          spindownCompletionTickCount = animationCounter + SPIN_DOWN_DURATION_TICKS;
        }
        break;
      }
      case SPIN_DOWN: {
        if (renderInfo.idle) {
          if (animationCounter >= spindownCompletionTickCount) {
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
    final double CROSSHAIR_ICON_WIDTH = 88;
    final double CROSSHAIR_ICON_HEIGHT = 88;
    final double CROSSHAIR_ICON_RADIUS = 40;
    final double CROSSHAIR_X_OFFSET = -CROSSHAIR_ICON_WIDTH / 2.0;
    final double CROSSHAIR_Y_OFFSET = -CROSSHAIR_ICON_HEIGHT / 2.0;

    int width = scaledResolution.getScaledWidth();
    int height = scaledResolution.getScaledHeight();

    final double MIN_RING_SIZE = 0.4;
    final double MAX_RING_SIZE = 1.0;
    final double STAR_COLOUR_MIN_INTENSITY = 0.5;
    final double RING_COLOUR_MIN_INTENSITY = 0.5;
    final double STAR_COLOUR_MAX_INTENSITY = 1.0;
    final double RING_COLOUR_MAX_INTENSITY = 1.0;

    double starColourIntensity, ringColourIntensity;

    switch (animationState) {
      case IDLE: {
        double cursorAngle = 0.0;
        double progressAngle = -1.0;
        if (renderInfo.vanillaCursorSpin) {
          final double CROSSHAIR_SPIN_DEGREES_PER_TICK = 360.0 / 20;
          cursorAngle = (animationCounter - vanillaSpinStartTick) * CROSSHAIR_SPIN_DEGREES_PER_TICK;
          progressAngle = renderInfo.cursorSpinProgress * 360.0 / 100.0;
        } else {
          vanillaSpinStartTick = animationCounter;
        }
        renderCrossHairs(scaledResolution, cursorAngle, progressAngle);
        return;
      }
      case SPIN_UP: {
        starSize = renderInfo.chargePercent / 100.0;
        ringSize = MIN_RING_SIZE + (MAX_RING_SIZE - MIN_RING_SIZE) * renderInfo.readinessPercent / 100.0;
        starColourIntensity = renderInfo.fullyChargedAndReady ? STAR_COLOUR_MAX_INTENSITY : STAR_COLOUR_MIN_INTENSITY;
        ringColourIntensity = RING_COLOUR_MAX_INTENSITY;
        clockwiseRotation = renderInfo.clockwise;
        drawTaskCompletionRing = false;
        break;
      }
      case SPINNING: {
        starSize = 1.0;
        ringSize = MAX_RING_SIZE; //MIN_RING_SIZE + (MAX_RING_SIZE - MIN_RING_SIZE) * renderInfo.readinessPercent / 100.0;
        starColourIntensity = renderInfo.fullyChargedAndReady ? STAR_COLOUR_MAX_INTENSITY : STAR_COLOUR_MIN_INTENSITY;
        ringColourIntensity = RING_COLOUR_MIN_INTENSITY;
        clockwiseRotation = renderInfo.clockwise;
        drawTaskCompletionRing = true;

        // every time the wheel sweeps past the task completion ring angle, extend the ring to the new completion point
        double rotationOffset = lastDegreesOfRotation - (lastDegreesOfRotation % 360.0);
        double lastRotationPosition = lastDegreesOfRotation - rotationOffset;
        double rotationPosition = degreesOfRotation - rotationOffset;
//          System.out.println("rotationOffset=" + rotationOffset + "; lastRotationPosition=" + lastRotationPosition + "; rotationPosition=" + rotationPosition);
        if (lastRotationPosition <= taskCompletionRingAngle &&
            rotationPosition >= taskCompletionRingAngle ) {  // swept over the current ring position
          double newTaskCompletionAngle = renderInfo.taskCompletionPercent * 360.0/100.0;
          taskCompletionRingAngle = Math.min(rotationPosition, newTaskCompletionAngle);
          taskCompletionRingAngle = Math.max(taskCompletionRingAngle, INITIAL_TASK_COMPLETION_ANGLE);
//            System.out.println("newTaskCompletionAngle=" + newTaskCompletionAngle + "; taskCompletionRingAngle=" + taskCompletionRingAngle);
        }

        break;
      }
      case SPIN_DOWN: {
        starSize = (spindownCompletionTickCount - animationCounter) / SPIN_DOWN_DURATION_TICKS;
        starSize = UsefulFunctions.clipToRange(starSize, 0.0, 1.0);
        starColourIntensity = RING_COLOUR_MIN_INTENSITY;
        ringColourIntensity = STAR_COLOUR_MIN_INTENSITY;
        drawTaskCompletionRing = false;
        // uses the saved value of ringSize, clockwiseRotation,
        break;
      }
      default: assert false : "illegal animationState:" + animationState; return;
    }

    Colour lineColour = Colour.BLACK_40;
    switch (renderInfo.cursorType) {
      case COPY: { lineColour = Colour.GREEN_20; break;}
      case MOVE: { lineColour = Colour.YELLOW_20; break;}
      case DELETE: { lineColour = Colour.RED_100; break;}
      default: assert false : "illegal cursorType:" + renderInfo.cursorType;
    }


    try {
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);


      GL11.glColor3d(lineColour.R * ringColourIntensity, lineColour.G * ringColourIntensity, lineColour.B * ringColourIntensity);

      Minecraft.getMinecraft().renderEngine.bindTexture(ringTexture);
      GL11.glPushMatrix();
      GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
      drawTexturedRectangle(CROSSHAIR_X_OFFSET * starSize * ringSize, CROSSHAIR_Y_OFFSET * starSize * ringSize, Z_LEVEL_FROM_GUI_IN_GAME_FORGE,
                            CROSSHAIR_ICON_WIDTH * starSize * ringSize, CROSSHAIR_ICON_HEIGHT * starSize * ringSize);
      GL11.glPopMatrix();

      GL11.glColor3d(lineColour.R * starColourIntensity, lineColour.G * starColourIntensity, lineColour.B * starColourIntensity);
      Minecraft.getMinecraft().renderEngine.bindTexture(octoStarTexture);
      GL11.glPushMatrix();
      GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
      GL11.glRotated( (clockwiseRotation ? degreesOfRotation : - degreesOfRotation),
                     0, 0, 1.0F);
      drawTexturedRectangle(CROSSHAIR_X_OFFSET * starSize, CROSSHAIR_Y_OFFSET * starSize, Z_LEVEL_FROM_GUI_IN_GAME_FORGE,
                            CROSSHAIR_ICON_WIDTH * starSize, CROSSHAIR_ICON_HEIGHT * starSize);
      GL11.glPopMatrix();

      final float ARC_LINE_WIDTH = 4.0F;

      if (drawTaskCompletionRing) {
        double progressBarIntensity = RING_COLOUR_MAX_INTENSITY;

        GL11.glColor3d(lineColour.R * progressBarIntensity, lineColour.G * progressBarIntensity, lineColour.B * progressBarIntensity);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glPushMatrix();
        GL11.glLineWidth(ARC_LINE_WIDTH);
        GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
        drawArc(CROSSHAIR_ICON_RADIUS * ringSize, 0.0,
                clockwiseRotation ? taskCompletionRingAngle : -taskCompletionRingAngle,
                0.0
               );
        GL11.glPopMatrix();
      }
    } finally {
      GL11.glPopAttrib();
    }
  }

  /**  The CursorRenderInfoUpdateLink and CursorRenderInfo are used to retrieve the necessary information for rendering from the current tool
   *  If refreshRenderInfo returns false, no render is performed.
   *  key points
   * 1) if the animation is idle, draw nothing
   * 2) .idle becomes false -> starts spinning up, in the specified direction.  It shows the percent powered up and the busy status of the server
   *    The cursor continues to spin until either performingTask is true (user has started the action) or idle becomes true (action aborted)
   *    An aborted action causes the cursor to spin down.
   *    performingTask becoming true leads to...
   * 3) while performingTask is true, the cursor continues to spin and also updates the display based on taskCompletionStatus
   * 4) when performingTask becomes false and idle becomes true, the cursor spins back down again.
   */
  public interface CursorRenderInfoUpdateLink
  {
    public boolean refreshRenderInfo(CursorRenderInfo infoToUpdate);
  }

  public static class CursorRenderInfo
  {
    public boolean vanillaCursorSpin;    // if true - spin the vanilla cursor
    public float   cursorSpinProgress;   // if vanilla cursor is spinning, add a progress completion bar (<= 0 = no bar, >= 100 = full, otherwise partial)

    public boolean idle;                  // if true - the cursor is either idle or is returning to idle
    public boolean clockwise;            // true if the charging is for an action, false for an undo
    public boolean fullyChargedAndReady;  // if true - fully charged and ready to act as soon as user releases
    public boolean performingTask;        // if true - server is performing an action or an undo for the client
    public boolean taskAborted;           // if true - the last task was aborted.  Only valid if performingTask is false and idle is true

    public float chargePercent;           // degree of charge up; 0.0 (min) - 100.0 (max)
    public float readinessPercent;        // completion percentage if waiting for another task to complete on server; 0.0 (min) - 100.0 (max)
    public float taskCompletionPercent;   // completion percentage if the server is currently performing an action or undo for the client; 0.0 (min) - 100.0 (max)
    public CursorType cursorType;         // the cursor appearance
    public enum CursorType {
      COPY, MOVE, DELETE
    }
  }

  private CursorRenderInfoUpdateLink infoProvider;
  private CursorRenderInfo renderInfo = new CursorRenderInfo();
  private AnimationState animationState = AnimationState.IDLE;
  private double spinStartTick;
  private double spindownCompletionTickCount;
  private double starSize;
  private double ringSize;
  private double taskCompletionRingAngle;
  private boolean clockwiseRotation;
  private double degreesOfRotation;
  private boolean drawTaskCompletionRing;
  private double vanillaSpinStartTick;

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

  /**
   * Render the vanilla crosshair, with optional spinning and optional progress ring.
   * @param scaledResolution
   * @param rotationAngle the angle the crosshair is rotated to (degrees); 0 = vanilla
   * @param progressAngle the angle of the progress ring (-ve = none, >= 360.0 = full, otherwise 0.0 - 360.0 for partial)
   */
  public void renderCrossHairs(ScaledResolution scaledResolution, double rotationAngle, double progressAngle)
  {
    final float Z_LEVEL_FROM_GUI_IN_GAME_FORGE = -90.0F;            // taken from GuiInGameForge.renderCrossHairs
    final int CROSSHAIR_ICON_WIDTH = 16;
    final int CROSSHAIR_ICON_HEIGHT = 16;
    final int CROSSHAIR_X_OFFSET = -7;      // taken from GuiInGameForge.renderCrossHairs
    final int CROSSHAIR_Y_OFFSET = -7;
    final float ARC_LINE_WIDTH = 4.0F;

    Minecraft mc = Minecraft.getMinecraft();
    int width = scaledResolution.getScaledWidth();
    int height = scaledResolution.getScaledHeight();

    GL11.glPushAttrib(GL11.GL_ENABLE_BIT);

    mc.getTextureManager().bindTexture(Gui.icons);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR);

    GL11.glPushMatrix();
    GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
    GL11.glRotated(rotationAngle, 0.0, 0.0, 1.0);
    drawIconFromGUIsheet(CROSSHAIR_X_OFFSET, CROSSHAIR_Y_OFFSET, Z_LEVEL_FROM_GUI_IN_GAME_FORGE,
                         0, 0, CROSSHAIR_ICON_WIDTH, CROSSHAIR_ICON_HEIGHT);
    GL11.glPopMatrix();

    GL11.glPushMatrix();
    GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
    GL11.glColor4d(1.0, 1.0, 1.0, 1.0);
    GL11.glLineWidth(ARC_LINE_WIDTH);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    drawArc(12.0, 0.0, progressAngle, (double) Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
    GL11.glPopMatrix();

    GL11.glPopAttrib();
    return;
  }

  /**
   * Draws a textured rectangle at the given z-value. Args: x, y, u, v, width, height
   * Assumes the texture sheet is 256 x 256 texels
   */
  public void drawIconFromGUIsheet(int x, int y, float z, int u, int v, int width, int height)
  {
    double ICON_SCALE_FACTOR_X = 1/256.0F;
    double ICON_SCALE_FACTOR_Y =  1/256.0F;
    Tessellator tessellator = Tessellator.instance;
    tessellator.startDrawingQuads();
    tessellator.addVertexWithUV(    x + 0, y + height, z,           u * ICON_SCALE_FACTOR_X, (v + height) * ICON_SCALE_FACTOR_Y);
    tessellator.addVertexWithUV(x + width, y + height, z, (u + width) * ICON_SCALE_FACTOR_X, (v + height) * ICON_SCALE_FACTOR_Y);
    tessellator.addVertexWithUV(x + width,      y + 0, z, (u + width) * ICON_SCALE_FACTOR_X,            v * ICON_SCALE_FACTOR_Y);
    tessellator.addVertexWithUV(    x + 0,      y + 0, z,           u * ICON_SCALE_FACTOR_X,            v * ICON_SCALE_FACTOR_Y);
    tessellator.draw();
  }

  private final ResourceLocation octoStarTexture = new ResourceLocation("speedytools", "textures/other/octostar.png");
  private final ResourceLocation ringTexture = new ResourceLocation("speedytools", "textures/other/octoring.png");
}
