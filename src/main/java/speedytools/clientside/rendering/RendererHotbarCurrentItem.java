package speedytools.clientside.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import cpw.mods.fml.common.eventhandler.Event;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import speedytools.SpeedyToolsMod;

import java.util.ArrayList;
import java.util.Collection;

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
public class RendererHotbarCurrentItem implements RendererElement
{
  public RendererHotbarCurrentItem(HotbarRenderInfoUpdateLink i_infoProvider)
  {
    infoProvider = i_infoProvider;
    renderInfo = new HotbarRenderInfo();
  }

  @Override
  public void render(Event event, float partialTick) {
    RenderGameOverlayHotbarEvent fullEvent = (RenderGameOverlayHotbarEvent)event;
    ScaledResolution res = fullEvent.resolution;
    int width = res.getScaledWidth();
    int height = res.getScaledHeight();
    boolean renderedSomething = renderHotbar(width, height, partialTick);
    event.setCanceled(renderedSomething);
  }

  /**  The HotbarRenderInfoUpdateLink and HotbarRenderInfo are used to retrieve the necessary information for rendering from the current tool
   *  If refreshRenderInfo returns false, no render is performed.
   */
  public interface HotbarRenderInfoUpdateLink
  {
    public boolean refreshRenderInfo(HotbarRenderInfo infoToUpdate, ItemStack currentlyHeldItem);
  }

  public static class HotbarRenderInfo
  {
    public boolean dummy; // not currently used for anything!
  }

  HotbarRenderInfoUpdateLink infoProvider;
  HotbarRenderInfo renderInfo;

  public boolean renderHotbar(int width, int height, float partialTicks)
  {
    Minecraft mc = Minecraft.getMinecraft();
    InventoryPlayer inventoryPlayer = mc.thePlayer.inventory;
    boolean shouldIRender = infoProvider.refreshRenderInfo(renderInfo, inventoryPlayer.getCurrentItem());
    if (!shouldIRender) return false;

    GuiIngame guiIngame = Minecraft.getMinecraft().ingameGUI;
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    mc.renderEngine.bindTexture(WIDGETS);

    final int HOTBAR_WIDTH = 182;
    final int HOTBAR_HALF_WIDTH = HOTBAR_WIDTH / 2;
    final int HOTBAR_HEIGHT = 22;
    final int SLOT_WIDTH = 20;
    final int DOUBLE_FRAME_WIDTH = 44;
    final int DOUBLE_FRAME_HEIGHT = 24;
    guiIngame.drawTexturedModalRect(width / 2 - HOTBAR_HALF_WIDTH, height - HOTBAR_HEIGHT, 0, 0, HOTBAR_WIDTH, HOTBAR_HEIGHT);

//    if (renderDoubleSizeSelector) {
    mc.renderEngine.bindTexture(doubleSelectorTexture);
    double zLevel = -90.0;   // copied from vanilla
    drawTexturedRectangle(width / 2 - HOTBAR_HALF_WIDTH - 1 + inventoryPlayer.currentItem * SLOT_WIDTH, height - HOTBAR_HEIGHT - 1, zLevel, DOUBLE_FRAME_WIDTH, DOUBLE_FRAME_HEIGHT);
    mc.renderEngine.bindTexture(WIDGETS);

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

  protected static final RenderItem itemRenderer = new RenderItem();

  @Override
  public Collection<Class<? extends Event>> eventsToReceive() {
    ArrayList<Class<? extends Event>> retval = new ArrayList<Class<? extends Event>>();
    retval.add(RenderGameOverlayHotbarEvent.class);
    return retval;
  }

  private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
  private final ResourceLocation doubleSelectorTexture = new ResourceLocation(SpeedyToolsMod.ID, "textures/other/doubleselector1.png");

}
