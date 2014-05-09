package speedytools.clientside.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.ForgeSubscribe;
import org.lwjgl.opengl.GL11;

/**
 * Created by TheGreyGhost on 9/05/14.
 */
public class RenderCursorClonerStatus
{

  package testitemrendering;

  import net.minecraft.client.Minecraft;
  import net.minecraft.client.gui.Gui;
  import net.minecraft.client.gui.ScaledResolution;
  import net.minecraft.client.renderer.Tessellator;
  import net.minecraft.entity.player.EntityPlayer;
  import net.minecraft.item.ItemStack;
  import net.minecraft.util.ResourceLocation;
  import net.minecraftforge.client.event.RenderGameOverlayEvent;
  import net.minecraftforge.event.ForgeSubscribe;
  import org.lwjgl.opengl.GL11;

  /**
   * Created by TheGreyGhost on 8/05/14.
   */
  public class TestOverlayRendering
  {
    ResourceLocation octoStarTexture1 = new ResourceLocation("testitemrendering", "textures/other/starbase1.png");
    ResourceLocation ringTexture1 = new ResourceLocation("testitemrendering", "textures/other/ring1.png");

    ResourceLocation octoStarTexture2 = new ResourceLocation("testitemrendering", "textures/other/starbase2.png");
    ResourceLocation ringTexture2 = new ResourceLocation("testitemrendering", "textures/other/ring2.png");

    /**
     * Draw the custom crosshairs if reqd
     * Otherwise, cancel the event so that the normal selection box is drawn.
     *
     * @param event
     */
    @ForgeSubscribe
    public void renderOverlayPre(RenderGameOverlayEvent.Pre event) {
      if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
      EntityPlayer player = Minecraft.getMinecraft().thePlayer;
      boolean customRender = renderCrossHairs(event.resolution, event.partialTicks);
      event.setCanceled(customRender);
      return;
    }

    public boolean renderCrossHairs(ScaledResolution scaledResolution, float partialTick)
    {

      final float Z_LEVEL_FROM_GUI_IN_GAME_FORGE = -90.0F;            // taken from GuiInGameForge.renderCrossHairs
      final double CROSSHAIR_SPIN_DEGREES_PER_TICK = 360.0 / 20;
      final int CROSSHAIR_ICON_WIDTH = 80;
      final int CROSSHAIR_ICON_HEIGHT = 80;
      final int CROSSHAIR_X_OFFSET = -40;      // taken from GuiInGameForge.renderCrossHairs
      final int CROSSHAIR_Y_OFFSET = -40;
      final float ARC_LINE_WIDTH = 4.0F;

      Minecraft mc = Minecraft.getMinecraft();
      int width = scaledResolution.getScaledWidth();
      int height = scaledResolution.getScaledHeight();

      GL11.glPushAttrib(GL11.GL_ENABLE_BIT);

//    mc.getTextureManager().bindTexture(Gui.icons);
      mc.renderEngine.bindTexture(octoStarTexture1);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

      long ms = System.nanoTime() / 1000 / 1000;
      int animationCounter = (int)(ms % 20000);
      final float DEGREES_PER_MS = 0.180F;
      float degreesOfRotation, scaleFactor, offsetRotation;
      final float ACCELERATION_DEG_PER_MS2 = DEGREES_PER_MS / 2000.0F;

      boolean forward = (animationCounter <= 10000);
      animationCounter %= 10000;

/*
    if (animationCounter<= 2000) {
      degreesOfRotation = 0.5F * animationCounter * animationCounter * ACCELERATION_DEG_PER_MS2;
      scaleFactor = animationCounter / 2000.0F;
    } else if (animationCounter <= 8000) {
      offsetRotation =  0.5F * 2000 * 2000 * ACCELERATION_DEG_PER_MS2;
      degreesOfRotation = (animationCounter - 2000) * DEGREES_PER_MS + offsetRotation;
      scaleFactor = 1.0F;
    } else {
      offsetRotation =  0.5F * 2000 * 2000 * ACCELERATION_DEG_PER_MS2 + (8000 - 2000) * DEGREES_PER_MS;
      degreesOfRotation = 2000 * DEGREES_PER_MS - 0.5F * (10000 - animationCounter) * (10000 - animationCounter)  * ACCELERATION_DEG_PER_MS2;
      scaleFactor = (10000 - animationCounter) / 2000.0F;
    }
*/

      final float MIN_RING_SIZE = 0.4F;
      float ringSize, starColourIntensity, ringColourIntensity;

      if (animationCounter<= 2000) {
        scaleFactor = animationCounter / 2000.0F;
        ringSize = MIN_RING_SIZE;
        starColourIntensity = 0.5F;
        ringColourIntensity = 1.0F;
      } else if (animationCounter <= 7000) {
        scaleFactor = 1.0F;
        ringSize = MIN_RING_SIZE + (1.0F - MIN_RING_SIZE) * (animationCounter - 2000) / (7000 - 2000);
        starColourIntensity = 0.5F;
        ringColourIntensity = 1.0F;
      } else if (animationCounter <= 9000) {
        scaleFactor = 1.0F;
        ringSize = 1.0F;
        starColourIntensity = 1.0F;
        ringColourIntensity = 1.0F;
      } else {
        scaleFactor = (10000 - animationCounter) / 1000.0F;
        ringSize = 1.0F;
        starColourIntensity = 0.5F;
        ringColourIntensity = 0.5F;
      }
      degreesOfRotation = forward ? animationCounter * DEGREES_PER_MS : (20000-animationCounter) * DEGREES_PER_MS;

/*
    GL11.glPushMatrix();
    GL11.glTranslatef(width / 4, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
    GL11.glRotatef(degreesOfRotation, 0, 0, 1.0F);
//    GL11.glRotated(((tickCount + (double) partialTick) * CROSSHAIR_SPIN_DEGREES_PER_TICK) % 360.0, 0.0, 0.0, 1.0);
    drawTexturedModalRect((int)(CROSSHAIR_X_OFFSET* scaleFactor), (int)(CROSSHAIR_Y_OFFSET* scaleFactor), Z_LEVEL_FROM_GUI_IN_GAME_FORGE,
                          (int)(CROSSHAIR_ICON_WIDTH * scaleFactor), (int)(CROSSHAIR_ICON_HEIGHT * scaleFactor));
    GL11.glPopMatrix();
*/

      GL11.glColor3f(0.0F * ringColourIntensity, 1.0F * ringColourIntensity, 0.0F * ringColourIntensity);

      mc.renderEngine.bindTexture(ringTexture2);
      GL11.glPushMatrix();
      GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
//    GL11.glRotatef(degreesOfRotation, 0, 0, 1.0F);
//    GL11.glRotated(((tickCount + (double) partialTick) * CROSSHAIR_SPIN_DEGREES_PER_TICK) % 360.0, 0.0, 0.0, 1.0);
      drawTexturedModalRect(CROSSHAIR_X_OFFSET* scaleFactor * ringSize, CROSSHAIR_Y_OFFSET* scaleFactor * ringSize, Z_LEVEL_FROM_GUI_IN_GAME_FORGE,
              CROSSHAIR_ICON_WIDTH * scaleFactor * ringSize, CROSSHAIR_ICON_HEIGHT * scaleFactor * ringSize);
      GL11.glPopMatrix();

      GL11.glColor3f(0.0F * starColourIntensity, 1.0F * starColourIntensity, 0.0F * starColourIntensity);

      mc.renderEngine.bindTexture(octoStarTexture2);
      GL11.glPushMatrix();
      GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
      GL11.glRotatef(degreesOfRotation, 0, 0, 1.0F);
//    GL11.glRotated(((tickCount + (double) partialTick) * CROSSHAIR_SPIN_DEGREES_PER_TICK) % 360.0, 0.0, 0.0, 1.0);
      drawTexturedModalRect(CROSSHAIR_X_OFFSET* scaleFactor, CROSSHAIR_Y_OFFSET* scaleFactor, Z_LEVEL_FROM_GUI_IN_GAME_FORGE,
              CROSSHAIR_ICON_WIDTH * scaleFactor, CROSSHAIR_ICON_HEIGHT * scaleFactor);
      GL11.glPopMatrix();


//    GL11.glPushMatrix();
//    GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
//    GL11.glColor4d(1.0, 1.0, 1.0, 1.0);
//    GL11.glLineWidth(ARC_LINE_WIDTH);
//    GL11.glDisable(GL11.GL_TEXTURE_2D);
////    drawArc(12.0, 0.0, actionPercentComplete * 360.0 / 100.0, (double) Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
//    GL11.glPopMatrix();

      GL11.glPopAttrib();
      return true;
    }

    /**
     * Draws a textured rectangle at the given z-value. Args: x, y, u, v, width, height
     */
    public void drawTexturedModalRect(double x, double y, double z, double width, double height)
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


  }

}
