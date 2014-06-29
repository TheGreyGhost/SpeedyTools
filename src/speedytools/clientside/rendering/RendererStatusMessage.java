package speedytools.clientside.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import speedytools.common.utilities.Colour;
import speedytools.common.utilities.UsefulFunctions;

/**
 * User: The Grey Ghost
 * Date: 22/06/2014
 */
public class RendererStatusMessage implements RendererElement
{
  public RendererStatusMessage(StatusMessageRenderInfoUpdateLink i_infoProvider) {
    infoProvider = i_infoProvider;
    renderInfo = new StatusMessageRenderInfo();
    animationState = AnimationState.NONE;
  }

  @Override
  public boolean renderInThisPhase(RenderPhase renderPhase) {
    return (renderPhase == RenderPhase.CROSSHAIRS);
  }

  @Override
  public void renderWorld(RenderPhase renderPhase, EntityPlayer player, int animationTickCount, float partialTick) {
    assert false : "invalid render phase: " + renderPhase;
  }

  @Override
  public void renderOverlay(RenderPhase renderPhase, ScaledResolution scaledResolution, int animationTickCount, float partialTick) {
    final double FADE_IN_DURATION_TICKS = 20;
    final double FADE_OUT_DURATION_TICKS = 10;
    if (renderPhase != RenderPhase.CROSSHAIRS) return;
    boolean shouldIRender = infoProvider.refreshRenderInfo(renderInfo);
    if (!shouldIRender) return;

    boolean newMessageArrived = false;
    if (!renderInfo.messageToDisplay.isEmpty() &&
        !renderInfo.messageToDisplay.equals(currentlyDisplayedMessage)) {
      newMessageArrived = true;
    }

    double animationCounter = animationTickCount + (double) partialTick;
    if (newMessageArrived) {  // don't start a new message until the old one has faded out
      if (animationState == AnimationState.NONE) {
        startMessageFadeIn(renderInfo.messageToDisplay, animationCounter);
      }
    } else if (renderInfo.messageToDisplay.isEmpty()) {
      if (animationState != AnimationState.NONE && animationState != AnimationState.FADE_OUT) {
        startMessageFadeOut(animationCounter);
      }
    }

    final double OPACITY_MIN = 0.2;
    final double OPACITY_MAX = 1.0;
    double opacity = OPACITY_MIN;
    switch (animationState) {
      case NONE: {
        currentlyDisplayedMessage = "";
        return;
      }
      case FADE_IN: {
        if (animationCounter >= animationFadeInStartCounter + FADE_IN_DURATION_TICKS) {
          animationState = AnimationState.SUSTAIN;
        }
        opacity = UsefulFunctions.interpolate(animationCounter, animationFadeInStartCounter, animationFadeInStartCounter + FADE_IN_DURATION_TICKS,
                OPACITY_MIN, OPACITY_MAX);
        break;
      }
      case FADE_OUT: {
        if (animationCounter >= animationFadeOutStartCounter + FADE_OUT_DURATION_TICKS) {
          animationState = AnimationState.NONE;
        }
        opacity = UsefulFunctions.interpolate(animationCounter, animationFadeOutStartCounter, animationFadeOutStartCounter + FADE_OUT_DURATION_TICKS,
                OPACITY_MAX, OPACITY_MIN);
        break;
      }
      case SUSTAIN: {
        opacity = OPACITY_MAX;
        break;
      }
      default:
        assert false : "Invalid animationState " + animationState + " in RendererStatusMessage";
    }

    int width = scaledResolution.getScaledWidth();
    int height = scaledResolution.getScaledHeight();

//    GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);

    int textColour = Colour.WHITE_40.getColourForFontRenderer(opacity);
//    System.out.println("opacity:" + opacity + "; rgba:" + textColour);
    drawHoveringText(currentlyDisplayedMessage, width/2, height/2, textColour);
  }
//    FontRenderer font = par1ItemStack.getItem().getFontRenderer(par1ItemStack);
//    drawHoveringText(list, par2, par3, (font == null ? fontRenderer : font));
//
//    FontRenderer fontrenderer = this.getFontRendererFromRenderManager();
//    float f = 1.6F;
//    float f1 = 0.016666668F * f;
//    GL11.glPushMatrix();
//    GL11.glTranslatef((float)par3 + 0.0F, (float)par5 + par1EntityLivingBase.height + 0.5F, (float)par7);
//    GL11.glNormal3f(0.0F, 1.0F, 0.0F);
//    GL11.glRotatef(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
//    GL11.glRotatef(this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
//    GL11.glScalef(-f1, -f1, f1);
//    GL11.glDisable(GL11.GL_LIGHTING);
//    GL11.glDepthMask(false);
//    GL11.glDisable(GL11.GL_DEPTH_TEST);
//    GL11.glEnable(GL11.GL_BLEND);
//    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//    Tessellator tessellator = Tessellator.instance;
//    byte b0 = 0;
//
//    if (par2Str.equals("deadmau5"))
//    {
//      b0 = -10;
//    }
//
//    GL11.glDisable(GL11.GL_TEXTURE_2D);
//    tessellator.startDrawingQuads();
//    int j = fontrenderer.getStringWidth(par2Str) / 2;
//    tessellator.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.25F);
//    tessellator.addVertex((double)(-j - 1), (double)(-1 + b0), 0.0D);
//    tessellator.addVertex((double)(-j - 1), (double)(8 + b0), 0.0D);
//    tessellator.addVertex((double)(j + 1), (double)(8 + b0), 0.0D);
//    tessellator.addVertex((double)(j + 1), (double)(-1 + b0), 0.0D);
//    tessellator.draw();
//    GL11.glEnable(GL11.GL_TEXTURE_2D);
//    fontrenderer.drawString(par2Str, -fontrenderer.getStringWidth(par2Str) / 2, b0, 553648127);
//    GL11.glEnable(GL11.GL_DEPTH_TEST);
//    GL11.glDepthMask(true);
//    fontrenderer.drawString(par2Str, -fontrenderer.getStringWidth(par2Str) / 2, b0, -1);
//    GL11.glEnable(GL11.GL_LIGHTING);
//    GL11.glDisable(GL11.GL_BLEND);
//    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
//    GL11.glPopMatrix();

    /**
     * draw hovering text centred at x, y
     */
  private void drawHoveringText(String message, int x, int y, int rgba)
  {
    if (message.isEmpty()) return;
    FontRenderer font = Minecraft.getMinecraft().fontRenderer;
    try {
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
      GL11.glPushMatrix();
      GL11.glDisable(GL12.GL_RESCALE_NORMAL);
      RenderHelper.disableStandardItemLighting();
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_DEPTH_TEST);

      int stringWidth = font.getStringWidth(message);
      font.drawStringWithShadow(message, x - stringWidth / 2, y, rgba);

//        this.drawGradientRect(i1 - 3, j1 - 4, i1 + k + 3, j1 - 3, l1, l1);
//        this.drawGradientRect(i1 - 3, j1 + k1 + 3, i1 + k + 3, j1 + k1 + 4, l1, l1);
//        this.drawGradientRect(i1 - 3, j1 - 3, i1 + k + 3, j1 + k1 + 3, l1, l1);
//        this.drawGradientRect(i1 - 4, j1 - 3, i1 - 3, j1 + k1 + 3, l1, l1);
//        this.drawGradientRect(i1 + k + 3, j1 - 3, i1 + k + 4, j1 + k1 + 3, l1, l1);
//        int i2 = 1347420415;
//        int j2 = (i2 & 16711422) >> 1 | i2 & -16777216;
//        this.drawGradientRect(i1 - 3, j1 - 3 + 1, i1 - 3 + 1, j1 + k1 + 3 - 1, i2, j2);
//        this.drawGradientRect(i1 + k + 2, j1 - 3 + 1, i1 + k + 3, j1 + k1 + 3 - 1, i2, j2);
//        this.drawGradientRect(i1 - 3, j1 - 3, i1 + k + 3, j1 - 3 + 1, i2, i2);
//        this.drawGradientRect(i1 - 3, j1 + k1 + 2, i1 + k + 3, j1 + k1 + 3, j2, j2);


    } finally {
      RenderHelper.enableStandardItemLighting();
      GL11.glPopMatrix();
      GL11.glPopAttrib();
    }
  }

  private void startMessageFadeOut(double animationCounter)
  {
    animationFadeOutStartCounter = animationCounter;
    animationState = AnimationState.FADE_OUT;
  }

  private void startMessageFadeIn(String newMessage, double animationCounter)
  {
    currentlyDisplayedMessage = newMessage;
    animationFadeInStartCounter = animationCounter;
    animationState = AnimationState.FADE_IN;
  }

  /**
   * The StatusMessageRenderInfoUpdateLink and StatusMessageRenderInfo are used to retrieve the necessary information for rendering from the current tool
   * If refreshRenderInfo returns false, no render is performed.
   */
  public interface StatusMessageRenderInfoUpdateLink
  {
    public boolean refreshRenderInfo(StatusMessageRenderInfo infoToUpdate);
  }

  public static class StatusMessageRenderInfo
  {
    public String messageToDisplay;   // empty for none
  }

  private StatusMessageRenderInfoUpdateLink infoProvider;
  private StatusMessageRenderInfo renderInfo;
  private AnimationState animationState;
  private String currentlyDisplayedMessage;
  private double animationFadeInStartCounter;
  private double animationFadeOutStartCounter;

  private enum AnimationState
  {
    NONE, FADE_IN, SUSTAIN, FADE_OUT;
  }

}
