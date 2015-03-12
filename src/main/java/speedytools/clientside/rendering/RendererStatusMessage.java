package speedytools.clientside.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import speedytools.clientside.ClientSide;
import speedytools.common.utilities.Colour;
import speedytools.common.utilities.UsefulFunctions;

import java.util.ArrayList;
import java.util.Collection;

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

//  @Override
//  public boolean renderInThisPhase(RenderPhase renderPhase) {
//    return (renderPhase == RenderPhase.CROSSHAIRS);
//  }

  @Override
  public Collection<Class<? extends Event>> eventsToReceive() {
    ArrayList<Class<? extends Event>> retval = new ArrayList<Class<? extends Event>>();
    retval.add(RenderGameOverlayCrosshairsEvent.class);
    return retval;
  }

  @Override
  public void render(Event event, float partialTick) {
    RenderGameOverlayCrosshairsEvent fullEvent = (RenderGameOverlayCrosshairsEvent)event;
    renderOverlay(fullEvent.resolution, ClientSide.getGlobalTickCount(), partialTick);
  }

//  @Override
//  public void renderWorld(RenderPhase renderPhase, EntityPlayer player, int animationTickCount, float partialTick) {
//    assert false : "invalid render phase: " + renderPhase;
//  }

  public void renderOverlay(ScaledResolution scaledResolution, int animationTickCount, float partialTick) {
    final double FADE_IN_DURATION_TICKS = 20;
    final double FADE_OUT_DURATION_TICKS = 10;
//    if (renderPhase != RenderPhase.CROSSHAIRS) return;
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


    int textColour = Colour.WHITE_40.getColourForFontRenderer(opacity);
    final int MESSAGE_HEIGHT_OFFSET = 8;
    drawHoveringText(currentlyDisplayedMessage, width/2, height/2 + MESSAGE_HEIGHT_OFFSET, textColour);
  }

    /**
     * draw hovering text centred at x, y
     */
  private void drawHoveringText(String message, int x, int y, int rgba)
  {
    if (message.isEmpty()) return;
    FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
    try {
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
      GL11.glPushMatrix();
      GL11.glDisable(GL12.GL_RESCALE_NORMAL);
      RenderHelper.disableStandardItemLighting();
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_DEPTH_TEST);

      int stringWidth = font.getStringWidth(message);
      font.drawStringWithShadow(message, x - stringWidth / 2, y, rgba);

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
