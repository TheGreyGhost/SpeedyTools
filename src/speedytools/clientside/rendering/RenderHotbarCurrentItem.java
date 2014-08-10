package speedytools.clientside.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Created by TheGreyGhost on 9/05/14.
 *  This class is used to render the "highlight" square displayed on the hotbar around the slot of the currently held item
 *  Used for the multiblock tools which take the block to be placed from the adjacent slot
 * Usage:
 * (1) Call the constructor, providing a CursorRenderInfoUpdateLink:
 *     This interface is used to fill the supplied CursorRenderInfo with the requested information for a render.
 * (2) When ready to render, call .render.
 * See CursorRenderInfo class definition for more information
 */
public class RenderHotbarCurrentItem
{
  private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");

  private static RenderHotbarCurrentItem renderHotbarCurrentItem = new RenderHotbarCurrentItem();

  static RenderHotbarCurrentItem getInstance() {
     return renderHotbarCurrentItem;
  }

  private boolean renderDoubleSizeSelector = false;

  public boolean renderHotbar(int width, int height, float partialTicks)
  {
    Minecraft mc = Minecraft.getMinecraft();
    GuiIngame guiIngame = Minecraft.getMinecraft().ingameGUI;

    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    mc.renderEngine.bindTexture(WIDGETS);

    InventoryPlayer inventoryPlayer = mc.thePlayer.inventory;
    guiIngame.drawTexturedModalRect(width / 2 - 91, height - 22, 0, 0, 182, 22);

    if (renderDoubleSizeSelector) {
      mc.renderEngine.bindTexture(doubleSelectorTexture);
      guiIngame.drawTexturedModalRect(width / 2 - 91 - 1 + inventoryPlayer.currentItem * 20, height - 22 - 1, 0, 22, 24, 22);
      mc.renderEngine.bindTexture(WIDGETS);
    } else {
      guiIngame.drawTexturedModalRect(width / 2 - 91 - 1 + inventoryPlayer.currentItem * 20, height - 22 - 1, 0, 22, 24, 22);
    }

    GL11.glDisable(GL11.GL_BLEND);
    GL11.glEnable(GL12.GL_RESCALE_NORMAL);
    RenderHelper.enableGUIStandardItemLighting();

    for (int i = 0; i < 9; ++i)
    {
      int x = width / 2 - 90 + i * 20 + 2;
      int y = height - 16 - 3;
      renderInventorySlot(mc, inventoryPlayer, i, x, y, partialTicks);
    }

    RenderHelper.disableStandardItemLighting();
    GL11.glDisable(GL12.GL_RESCALE_NORMAL);
    return true;
  }

  protected void renderInventorySlot(Minecraft minecraft, InventoryPlayer inventoryPlayer, int itemIndex, int x, int y, float partialTick)
  {
    ItemStack itemstack = inventoryPlayer.mainInventory[itemIndex];

    if (itemstack != null)
    {
      float f1 = (float)itemstack.animationsToGo - partialTick;

      if (f1 > 0.0F)
      {
        GL11.glPushMatrix();
        float f2 = 1.0F + f1 / 5.0F;
        GL11.glTranslatef((float)(x + 8), (float)(y + 12), 0.0F);
        GL11.glScalef(1.0F / f2, (f2 + 1.0F) / 2.0F, 1.0F);
        GL11.glTranslatef((float)(-(x + 8)), (float)(-(y + 12)), 0.0F);
      }

      itemRenderer.renderItemAndEffectIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), itemstack, x, y);

      if (f1 > 0.0F)
      {
        GL11.glPopMatrix();
      }

      itemRenderer.renderItemOverlayIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), itemstack, x, y);
    }
  }

  protected static final RenderItem itemRenderer = new RenderItem();

//  public RenderHotbarCurrentItem(CursorRenderInfoUpdateLink i_infoProvider)
//  {
//    infoProvider = i_infoProvider;
//    renderInfo = new CursorRenderInfo();
//  }
//
//  @Override
//  public boolean renderInThisPhase(RenderPhase renderPhase)
//  {
//    return (renderPhase == RenderPhase.CROSSHAIRS);
//  }
//
//  @Override
//  public void renderWorld(RenderPhase renderPhase, EntityPlayer player, int animationTickCount, float partialTick)
//  {
//    assert false : "invalid render phase: " + renderPhase;
//  }
//
//  /**
//   * renders the 'power up' cursor.  See CursorRenderInfo for the various controls
//   * @param scaledResolution
//   * @param animationTickCount
//   * @param partialTick
//   */
//  @Override
//  public void renderOverlay(RenderPhase renderPhase, ScaledResolution scaledResolution, int animationTickCount, float partialTick)
//  {
//    if (renderPhase != RenderPhase.CROSSHAIRS) return;
//    boolean shouldIRender = infoProvider.refreshRenderInfo(renderInfo);
//    if (!shouldIRender) return;
//
//
//
////    final int SPIN_DOWN_RING_COMPLETION_TICKS = 0;
//    final int SPIN_DOWN_STAR_SHRINK_TICKS = 20;
//    final int SPIN_DOWN_TOTAL_DURATION_TICKS =  SPIN_DOWN_STAR_SHRINK_TICKS; // + SPIN_DOWN_RING_COMPLETION_TICKS
//    final float SPIN_DEGREES_PER_TICK = 9.0F;
//    final double INITIAL_TASK_COMPLETION_ANGLE = 5.0;  // always show at least this much "task completion" angle
//
//    double animationCounter = animationTickCount + partialTick;
//    double lastDegreesOfRotation = degreesOfRotation;
//    degreesOfRotation = (animationCounter - spinStartTick) * SPIN_DEGREES_PER_TICK;
//
//    // look for transitions
//    if (renderInfo.animationState != animationState) {
//      boolean performTransition = true;
//      // check for special cases
//      switch (renderInfo.animationState) {
//        case IDLE: {  // should never get to here - abrupt cut
//          break;
//        }
//        case SPIN_UP_CW:
//        case SPIN_UP_CCW: { // delay transition if we are spinning down
//          if (   animationState == CursorRenderInfo.AnimationState.SPIN_DOWN_CW_CANCELLED
//              || animationState == CursorRenderInfo.AnimationState.SPIN_DOWN_CCW_CANCELLED
//              || animationState == CursorRenderInfo.AnimationState.SPIN_DOWN_CW_SUCCESS
//              || animationState == CursorRenderInfo.AnimationState.SPIN_DOWN_CCW_SUCCESS  ) {
//            performTransition = false;
//          } else {
//            spinStartTick = animationCounter;
//          }
//          break;
//        }
//        case SPIN_DOWN_CW_SUCCESS: {
//          if (animationState == CursorRenderInfo.AnimationState.IDLE) {
//            performTransition = false;
//          } else if (animationState == CursorRenderInfo.AnimationState.SPINNING_CW && taskCompletionRingAngle < 359.0) { // wait until ring is drawn full
//            performTransition = false;
//            renderInfo.taskCompletionPercent = 100.0F;
//          } else {
//            spindownStartTick = animationCounter;
//          }
//          break;
//        }
//        case SPIN_DOWN_CCW_SUCCESS: {
//          if (animationState == CursorRenderInfo.AnimationState.IDLE) {
//            performTransition = false;
//          } else if ( (animationState == CursorRenderInfo.AnimationState.SPINNING_CCW_FROM_FULL
//                 || animationState == CursorRenderInfo.AnimationState.SPINNING_CCW_FROM_PARTIAL )
//               && taskCompletionRingAngle < 359.0) { // wait until ring is drawn full
//            performTransition = false;
//            renderInfo.taskCompletionPercent = 100.0F;
//          } else {
//            spindownStartTick = animationCounter;
//          }
//          break;
//        }
//        case SPIN_DOWN_CW_CANCELLED:
//        case SPIN_DOWN_CCW_CANCELLED: {
//          if (animationState == CursorRenderInfo.AnimationState.IDLE) {
//            performTransition = false;
//          } else {
//            spindownStartTick = animationCounter;
//            cancelledStarSize = starSize;
//          }
//          break;
//        }
//        case SPINNING_CW: {
//          taskCompletionRingAngle = INITIAL_TASK_COMPLETION_ANGLE;
//          break;
//        }
//        case SPINNING_CCW_FROM_FULL: {
//          taskCompletionRingAngle = INITIAL_TASK_COMPLETION_ANGLE;
//          break;
//        }
//        case SPINNING_CCW_FROM_PARTIAL: {
//          // reverse spin direction, adjust spinStartTick to start from the current ring rotation
//          renderInfo.taskCompletionPercent = 0;
//          partialSpinStartingCompletionRingAngle = 360.0 - taskCompletionRingAngle;
//          taskCompletionRingAngle = 360.0 - taskCompletionRingAngle;
//          degreesOfRotation = 360.0 - (degreesOfRotation % 360);
//          spinStartTick = animationCounter - degreesOfRotation / SPIN_DEGREES_PER_TICK;
//          break;
//        }
//
//      }
//      if (performTransition) animationState = renderInfo.animationState;
//    }
//
//    final float Z_LEVEL_FROM_GUI_IN_GAME_FORGE = -90.0F;            // taken from GuiInGameForge.renderCrossHairs
//    final double CROSSHAIR_ICON_WIDTH = 88;
//    final double CROSSHAIR_ICON_HEIGHT = 88;
//    final double CROSSHAIR_ICON_RADIUS = 40;
//    final double CROSSHAIR_X_OFFSET = -CROSSHAIR_ICON_WIDTH / 2.0;
//    final double CROSSHAIR_Y_OFFSET = -CROSSHAIR_ICON_HEIGHT / 2.0;
//
//    int width = scaledResolution.getScaledWidth();
//    int height = scaledResolution.getScaledHeight();
//
//    final double MIN_RING_SIZE = 0.4;
//    final double MAX_RING_SIZE = 1.0;
//    final double MIN_STAR_SIZE = 0.1;
//    final double MAX_STAR_SIZE = 1.0;
//    final double STAR_COLOUR_MIN_INTENSITY = 0.5;
//    final double RING_COLOUR_MIN_INTENSITY = 0.5;
//    final double STAR_COLOUR_MAX_INTENSITY = 1.0;
//    final double RING_COLOUR_MAX_INTENSITY = 1.0;
//
//    double starColourIntensity = 0;
//    double ringColourIntensity = 0;
//    Colour lineColour = Colour.BLACK_40;
//    switch (renderInfo.cursorType) {
//      case COPY: { lineColour = Colour.GREEN_100; break;}
//      case MOVE: { lineColour = Colour.BLUE_100; break;}
//      case DELETE: { lineColour = Colour.RED_100; break;}
//      default: assert false : "illegal cursorType:" + renderInfo.cursorType;
//    }
//
//    switch (animationState) {
//      case IDLE: {
//        if (renderInfo.aSelectionIsDefined) {
//          starSize = MIN_STAR_SIZE;
//          drawTaskCompletionRing = false;
//          starColourIntensity = STAR_COLOUR_MIN_INTENSITY;
//          ringColourIntensity = RING_COLOUR_MIN_INTENSITY;
//          lineColour = Colour.WHITE_40;
//          degreesOfRotation = 0;
//          renderCrossHairs(scaledResolution, 45, 360.0);
//        } else {
//          double cursorAngle = 0.0;
//          double progressAngle = -1.0;
//          if (renderInfo.vanillaCursorSpin) {
//            final double CROSSHAIR_SPIN_DEGREES_PER_TICK = 360.0 / 20;
//            cursorAngle = (animationCounter - vanillaSpinStartTick) * CROSSHAIR_SPIN_DEGREES_PER_TICK;
//            progressAngle = renderInfo.cursorSpinProgress * 360.0 / 100.0;
//          } else {
//            vanillaSpinStartTick = animationCounter;
//          }
//          renderCrossHairs(scaledResolution, cursorAngle, progressAngle);
//          return;
//        }
//        break;
//      }
//      case SPIN_UP_CCW:
//      case SPIN_UP_CW: {
//        starSize = MIN_STAR_SIZE + (MAX_STAR_SIZE - MIN_STAR_SIZE) * renderInfo.chargePercent / 100.0;
//        ringSize = MIN_RING_SIZE + (MAX_RING_SIZE - MIN_RING_SIZE) * renderInfo.readinessPercent / 100.0;
//        starColourIntensity = renderInfo.fullyChargedAndReady ? STAR_COLOUR_MAX_INTENSITY : STAR_COLOUR_MIN_INTENSITY;
//        ringColourIntensity = RING_COLOUR_MAX_INTENSITY;
//        clockwiseRotation = (animationState == CursorRenderInfo.AnimationState.SPIN_UP_CW);
//        drawTaskCompletionRing = false;
// //       taskCompletionRingAngle = INITIAL_TASK_COMPLETION_ANGLE;
//        break;
//      }
//      case SPINNING_CW: {
//        starSize = MAX_STAR_SIZE;
//        ringSize = MAX_RING_SIZE;
//        starColourIntensity =  STAR_COLOUR_MAX_INTENSITY;
//        ringColourIntensity = RING_COLOUR_MIN_INTENSITY;
//        clockwiseRotation = true;
//        drawTaskCompletionRing = true;
//        targetTaskCompletionRingAngle = renderInfo.taskCompletionPercent * 360.0/100.0;
//        break;
//      }
//      case SPINNING_CCW_FROM_FULL: {
//        starSize = MAX_STAR_SIZE;
//        ringSize = MAX_RING_SIZE;
//        starColourIntensity =  STAR_COLOUR_MAX_INTENSITY;
//        ringColourIntensity = RING_COLOUR_MIN_INTENSITY;
//        clockwiseRotation = false;
//        drawTaskCompletionRing = true;
//        targetTaskCompletionRingAngle = renderInfo.taskCompletionPercent * 360.0/100.0;
//        break;
//      }
//      case SPINNING_CCW_FROM_PARTIAL: {
//        starSize = MAX_STAR_SIZE;
//        ringSize = MAX_RING_SIZE;
//        starColourIntensity = STAR_COLOUR_MAX_INTENSITY;
//        ringColourIntensity = RING_COLOUR_MIN_INTENSITY;
//        clockwiseRotation = false;
//        drawTaskCompletionRing = true;
//        targetTaskCompletionRingAngle = partialSpinStartingCompletionRingAngle + (360.0 - partialSpinStartingCompletionRingAngle) * renderInfo.taskCompletionPercent / 100.0;
////        System.out.println("target " + targetTaskCompletionRingAngle); //todo remove
//        break;
//      }
//      case SPIN_DOWN_CCW_SUCCESS:
//      case SPIN_DOWN_CW_SUCCESS: {
//        double spinDownTicksElapsed = animationCounter - spindownStartTick;
//        starSize = MAX_STAR_SIZE - spinDownTicksElapsed / SPIN_DOWN_STAR_SHRINK_TICKS;
//        starSize = UsefulFunctions.clipToRange(starSize, MIN_STAR_SIZE, MAX_STAR_SIZE);
//        ringSize = MAX_RING_SIZE;
//        drawTaskCompletionRing = false;
//        starColourIntensity = STAR_COLOUR_MAX_INTENSITY;
//        ringColourIntensity = RING_COLOUR_MIN_INTENSITY;
//        clockwiseRotation = (animationState == CursorRenderInfo.AnimationState.SPIN_DOWN_CW_SUCCESS);
//        if (spinDownTicksElapsed > SPIN_DOWN_STAR_SHRINK_TICKS) animationState = CursorRenderInfo.AnimationState.IDLE;
//        break;
//      }
//      case SPIN_DOWN_CW_CANCELLED:
//      case SPIN_DOWN_CCW_CANCELLED: {
//        double spinDownTicksElapsed = animationCounter - spindownStartTick;
//        starSize = cancelledStarSize - spinDownTicksElapsed / SPIN_DOWN_STAR_SHRINK_TICKS;
//        if (starSize < MIN_STAR_SIZE) animationState = CursorRenderInfo.AnimationState.IDLE;
//        starSize = UsefulFunctions.clipToRange(starSize, MIN_STAR_SIZE, MAX_STAR_SIZE);
//        drawTaskCompletionRing = false;
//        starColourIntensity = STAR_COLOUR_MIN_INTENSITY;
//        ringColourIntensity = RING_COLOUR_MIN_INTENSITY;
//        clockwiseRotation = (animationState == CursorRenderInfo.AnimationState.SPIN_DOWN_CW_CANCELLED);
//        // uses the saved value of ringSize
//        break;
//      }
//      default: assert false : "illegal animationState:" + animationState;
//    }
//
//    // every time the wheel sweeps past the target ring angle, extend the ring to the new completion point
//    // there are two sweep points on the wheel, at 0 and at 180
//    if (drawTaskCompletionRing) {
//      double rotationOffset = lastDegreesOfRotation - (lastDegreesOfRotation % 360.0);
//      double lastRotationPosition = lastDegreesOfRotation - rotationOffset;
//      double rotationPosition = degreesOfRotation - rotationOffset;
////      System.out.println("rotationOffset=" + rotationOffset + "; lastRotationPosition=" + lastRotationPosition + "; rotationPosition=" + rotationPosition);
////      System.out.println("taskCompletionRingAngle:"+ taskCompletionRingAngle + "; targetTaskCompletionRingAngle:" + targetTaskCompletionRingAngle);
////      System.out.println("rotationPosition:" + rotationPosition);
//
//      // swept over the current ring position - check near 0 and also near 360; repeat check for the 180 degree point on the wheel too
//
//      if ((lastRotationPosition <= taskCompletionRingAngle && rotationPosition >= taskCompletionRingAngle)
//              || (lastRotationPosition <= taskCompletionRingAngle + 360 && rotationPosition >= taskCompletionRingAngle + 360)) {
//        taskCompletionRingAngle = Math.min(rotationPosition, targetTaskCompletionRingAngle);
//        taskCompletionRingAngle = Math.max(taskCompletionRingAngle, INITIAL_TASK_COMPLETION_ANGLE);
////        System.out.println("taskCompletionRingAngle:" + taskCompletionRingAngle + "; targetTaskCompletionRingAngle:" + targetTaskCompletionRingAngle);
//      } else {
//        rotationOffset =  (lastDegreesOfRotation + 180) - (  (lastDegreesOfRotation + 180) % 360.0);
//        lastRotationPosition = lastDegreesOfRotation + 180 - rotationOffset;
//        rotationPosition = degreesOfRotation + 180 - rotationOffset;
//        if ((lastRotationPosition <= taskCompletionRingAngle && rotationPosition >= taskCompletionRingAngle)
//                || (lastRotationPosition <= taskCompletionRingAngle + 360 && rotationPosition >= taskCompletionRingAngle + 360)) {
//          taskCompletionRingAngle = Math.min(rotationPosition, targetTaskCompletionRingAngle);
//          taskCompletionRingAngle = Math.max(taskCompletionRingAngle, INITIAL_TASK_COMPLETION_ANGLE);
//        }
//      }
//    }
//
//    try {
//      GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
//      GL11.glEnable(GL11.GL_BLEND);
//      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//
//      GL11.glColor3d(lineColour.R * ringColourIntensity, lineColour.G * ringColourIntensity, lineColour.B * ringColourIntensity);
//
//      Minecraft.getMinecraft().renderEngine.bindTexture(ringTexture);
//      GL11.glPushMatrix();
//      GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
//      drawTexturedRectangle(CROSSHAIR_X_OFFSET * starSize * ringSize, CROSSHAIR_Y_OFFSET * starSize * ringSize, Z_LEVEL_FROM_GUI_IN_GAME_FORGE,
//                            CROSSHAIR_ICON_WIDTH * starSize * ringSize, CROSSHAIR_ICON_HEIGHT * starSize * ringSize);
//      GL11.glPopMatrix();
//
//      GL11.glColor3d(lineColour.R * starColourIntensity, lineColour.G * starColourIntensity, lineColour.B * starColourIntensity);
//      Minecraft.getMinecraft().renderEngine.bindTexture(octoStarTexture);
//      GL11.glPushMatrix();
//      GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
//      GL11.glRotated( (clockwiseRotation ? degreesOfRotation : - degreesOfRotation),
//                     0, 0, 1.0F);
//      drawTexturedRectangle(CROSSHAIR_X_OFFSET * starSize, CROSSHAIR_Y_OFFSET * starSize, Z_LEVEL_FROM_GUI_IN_GAME_FORGE,
//                            CROSSHAIR_ICON_WIDTH * starSize, CROSSHAIR_ICON_HEIGHT * starSize);
//      GL11.glPopMatrix();
//
//      final float ARC_LINE_WIDTH = 4.0F;
//
//      if (drawTaskCompletionRing) {
//        double progressBarIntensity = RING_COLOUR_MAX_INTENSITY;
////        System.out.println("ring from 0.0 to " + (clockwiseRotation ? taskCompletionRingAngle : 360.0 - taskCompletionRingAngle));
//
//        GL11.glColor3d(lineColour.R * progressBarIntensity, lineColour.G * progressBarIntensity, lineColour.B * progressBarIntensity);
//        GL11.glDisable(GL11.GL_TEXTURE_2D);
//        GL11.glPushMatrix();
//        GL11.glLineWidth(ARC_LINE_WIDTH);
//        GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
//        drawArc(CROSSHAIR_ICON_RADIUS * ringSize, 0.0,
//                clockwiseRotation ? taskCompletionRingAngle : 360.0 - taskCompletionRingAngle,
//                0.0
//               );
//        GL11.glPopMatrix();
//      }
//    } finally {
//      GL11.glPopAttrib();
//    }
//  }
//
//  /**  The CursorRenderInfoUpdateLink and CursorRenderInfo are used to retrieve the necessary information for rendering from the current tool
//   *  If refreshRenderInfo returns false, no render is performed.
//   *  key points
//   * 1) if the animation is idle, draw nothing
//   * 2) .idle becomes false -> starts spinning up, in the specified direction.  It shows the percent powered up and the busy status of the server
//   *    The cursor continues to spin until either performingTask is true (user has started the action) or idle becomes true (action aborted)
//   *    An aborted action causes the cursor to spin down.
//   *    performingTask becoming true leads to...
//   * 3) while performingTask is true, the cursor continues to spin and also updates the display based on taskCompletionStatus
//   * 4) when performingTask becomes false and idle becomes true, the cursor spins back down again.
//   */
//  public interface CursorRenderInfoUpdateLink
//  {
//    public boolean refreshRenderInfo(CursorRenderInfo infoToUpdate);
//  }
//
//  public static class CursorRenderInfo
//  {
//    public boolean vanillaCursorSpin;    // if true - spin the vanilla cursor
//    public float   cursorSpinProgress;   // if vanilla cursor is spinning, add a progress completion bar (<= 0 = no bar, >= 100 = full, otherwise partial)
//    public boolean aSelectionIsDefined;     // true if a selection is defined, false if not
//
//    public boolean fullyChargedAndReady;  // if true - fully charged and ready to act as soon as user releases
//    public float chargePercent;           // degree of charge up; 0.0 (min) - 100.0 (max)
//    public float readinessPercent;        // completion percentage if waiting for another task to complete on server; 0.0 (min) - 100.0 (max)
//    public float taskCompletionPercent;   // completion percentage if the server is currently performing an action or undo for the client; 0.0 (min) - 100.0 (max)
//    public CursorType cursorType;         // the cursor appearance
//    public AnimationState animationState; // the target animation state of the cursor
//
//    public enum AnimationState
//    {
//      IDLE, SPIN_UP_CW, SPINNING_CW, SPIN_DOWN_CW_SUCCESS, SPIN_DOWN_CW_CANCELLED,
//      SPIN_UP_CCW, SPINNING_CCW_FROM_FULL, SPINNING_CCW_FROM_PARTIAL, SPIN_DOWN_CCW_CANCELLED, SPIN_DOWN_CCW_SUCCESS
//    }
//
//    public enum CursorType {
//      COPY, MOVE, DELETE
//    }
//  }
//
//  private CursorRenderInfoUpdateLink infoProvider;
//  private CursorRenderInfo renderInfo = new CursorRenderInfo();
//  private CursorRenderInfo.AnimationState animationState = CursorRenderInfo.AnimationState.IDLE;
//  private double spinStartTick;
//  private double degreesOfRotation;               // the number of degrees of rotation performed relative to spinStartTick. Always increases regardless of rotation direction
//  private double partialSpinStartingCompletionRingAngle;
//  private double spindownStartTick;
//  private double cancelledStarSize;
//  private double starSize;
//  private double ringSize;
//  private double taskCompletionRingAngle;
//  private boolean clockwiseRotation;
//  private boolean drawTaskCompletionRing;
//  private double targetTaskCompletionRingAngle;
//  private double vanillaSpinStartTick;
//
//  /**
//   * Draws a textured rectangle at the given z-value, using the entire texture. Args: x, y, z, width, height
//   */
//  private void drawTexturedRectangle(double x, double y, double z, double width, double height)
//  {
//    double ICON_MIN_U = 0.0;
//    double ICON_MAX_U = 1.0;
//    double ICON_MIN_V = 0.0;
//    double ICON_MAX_V = 1.0;
//    Tessellator tessellator = Tessellator.instance;
//    tessellator.startDrawingQuads();
//    tessellator.addVertexWithUV(    x + 0, y + height, z,  ICON_MIN_U, ICON_MAX_V);
//    tessellator.addVertexWithUV(x + width, y + height, z,  ICON_MAX_U, ICON_MAX_V);
//    tessellator.addVertexWithUV(x + width,      y + 0, z,  ICON_MAX_U, ICON_MIN_V);
//    tessellator.addVertexWithUV(    x + 0,      y + 0, z,  ICON_MIN_U, ICON_MIN_V);
//    tessellator.draw();
//  }
//
//
//  /**
//   * Draw an arc centred around the zero point.  Setup translatef, colour and line width etc before calling.
//   * @param radius
//   * @param startAngle clockwise starting from 12 O'clock
//   * @param endAngle
//   */
//  void drawArc(double radius, double startAngle, double endAngle, double zLevel)
//  {
//    final double angleIncrement = Math.toRadians(10.0);
//    float direction = (endAngle >= startAngle) ? 1.0F : -1.0F;
//    double deltaAngle = Math.abs(endAngle - startAngle);
//    deltaAngle %= 360.0;
//
//    startAngle -= Math.floor(startAngle/360.0);
//    startAngle = Math.toRadians(startAngle);
//    deltaAngle = Math.toRadians(deltaAngle);
//
//    GL11.glBegin(GL11.GL_LINE_STRIP);
//
//    double x, y;
//    double arcPos = 0;
//    boolean arcFinished = false;
//
//    do {
//      double truncAngle = Math.min(arcPos, deltaAngle);
//      x = radius * Math.sin(startAngle + direction * truncAngle);
//      y = -radius * Math.cos(startAngle + direction * truncAngle);
//      GL11.glVertex3d(x, y, zLevel);
//
//      arcFinished = (arcPos >= deltaAngle);
//      arcPos += angleIncrement;
//    } while (!arcFinished && arcPos <= Math.toRadians(360.0));      // arcPos test is a fail safe to prevent infinite loop in case of problem with angle arguments
//    GL11.glEnd();
//  }
//
//  /**
//   * Render the vanilla crosshair, with optional spinning and optional progress ring.
//   * @param scaledResolution
//   * @param rotationAngle the angle the crosshair is rotated to (degrees); 0 = vanilla
//   * @param progressAngle the angle of the progress ring (-ve = none, >= 360.0 = full, otherwise 0.0 - 360.0 for partial)
//   */
//  public void renderCrossHairs(ScaledResolution scaledResolution, double rotationAngle, double progressAngle)
//  {
//    final float Z_LEVEL_FROM_GUI_IN_GAME_FORGE = -90.0F;            // taken from GuiInGameForge.renderCrossHairs
//    final int CROSSHAIR_ICON_WIDTH = 16;
//    final int CROSSHAIR_ICON_HEIGHT = 16;
//    final int CROSSHAIR_X_OFFSET = -7;      // taken from GuiInGameForge.renderCrossHairs
//    final int CROSSHAIR_Y_OFFSET = -7;
//    final float ARC_LINE_WIDTH = 4.0F;
//
//    Minecraft mc = Minecraft.getMinecraft();
//    int width = scaledResolution.getScaledWidth();
//    int height = scaledResolution.getScaledHeight();
//
//    GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
//
//    mc.getTextureManager().bindTexture(Gui.icons);
//    GL11.glEnable(GL11.GL_BLEND);
//    GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR);
//
//    GL11.glPushMatrix();
//    GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
//    GL11.glRotated(rotationAngle, 0.0, 0.0, 1.0);
//    drawIconFromGUIsheet(CROSSHAIR_X_OFFSET, CROSSHAIR_Y_OFFSET, Z_LEVEL_FROM_GUI_IN_GAME_FORGE,
//                         0, 0, CROSSHAIR_ICON_WIDTH, CROSSHAIR_ICON_HEIGHT);
//    GL11.glPopMatrix();
//
//    GL11.glPushMatrix();
//    GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
//    GL11.glColor4d(1.0, 1.0, 1.0, 1.0);
//    GL11.glLineWidth(ARC_LINE_WIDTH);
//    GL11.glDisable(GL11.GL_TEXTURE_2D);
//    drawArc(12.0, 0.0, progressAngle, (double) Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
//    GL11.glPopMatrix();
//
//    GL11.glPopAttrib();
//    return;
//  }
//
//  /**
//   * Draws a textured rectangle at the given z-value. Args: x, y, u, v, width, height
//   * Assumes the texture sheet is 256 x 256 texels
//   */
//  public void drawIconFromGUIsheet(int x, int y, float z, int u, int v, int width, int height)
//  {
//    double ICON_SCALE_FACTOR_X = 1/256.0F;
//    double ICON_SCALE_FACTOR_Y =  1/256.0F;
//    Tessellator tessellator = Tessellator.instance;
//    tessellator.startDrawingQuads();
//    tessellator.addVertexWithUV(    x + 0, y + height, z,           u * ICON_SCALE_FACTOR_X, (v + height) * ICON_SCALE_FACTOR_Y);
//    tessellator.addVertexWithUV(x + width, y + height, z, (u + width) * ICON_SCALE_FACTOR_X, (v + height) * ICON_SCALE_FACTOR_Y);
//    tessellator.addVertexWithUV(x + width,      y + 0, z, (u + width) * ICON_SCALE_FACTOR_X,            v * ICON_SCALE_FACTOR_Y);
//    tessellator.addVertexWithUV(    x + 0,      y + 0, z,           u * ICON_SCALE_FACTOR_X,            v * ICON_SCALE_FACTOR_Y);
//    tessellator.draw();
//  }

  private final ResourceLocation doubleSelectorTexture = new ResourceLocation("speedytools", "textures/other/doubleselector1.png");
}
