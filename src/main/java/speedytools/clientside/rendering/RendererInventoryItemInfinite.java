package speedytools.clientside.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;
import speedytools.common.items.ItemSpeedyTool;
import speedytools.common.utilities.ErrorLog;

/**
 * Created by TheGreyGhost on 28/10/14.
 */
public class RendererInventoryItemInfinite implements IItemRenderer
{
  public RendererInventoryItemInfinite(ItemSpeedyTool i_itemSpeedyTool)
  {
    itemSpeedyTool = i_itemSpeedyTool;
  }

  @Override
  public boolean handleRenderType(ItemStack item, ItemRenderType type) {
    return (type == ItemRenderType.INVENTORY);
  }

  @Override
  public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
//    if (type != ItemRenderType.INVENTORY) return false;
    return false;
  }

  @Override
  public void renderItem(ItemRenderType type, ItemStack itemStack, Object... data) {
    int xPos = 0;
    int yPos = 0;
    RenderItem renderItem = RenderItem.getInstance();
    FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
    TextureManager textureManger = Minecraft.getMinecraft().getTextureManager();
    renderItem.renderItemIntoGUI(fontRenderer, textureManger, itemStack, xPos, yPos);

    if (!itemSpeedyTool.isInfiniteMode(itemStack)) {
      return;
    }

    String  overlayText = "##";
    try {
      renderItem.zLevel -= 50.0F;        // undo the renderItemAndEffectIntoGUI
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_BLEND);
      int textWidth = fontRenderer.getStringWidth(overlayText);
      fontRenderer.drawStringWithShadow(overlayText, xPos + 19 - 2 - textWidth, yPos + 6 + 3, 0xffffff);
    } finally {
      GL11.glPopAttrib();
      renderItem.zLevel += 50.0F;
    }
  }

  private ItemSpeedyTool itemSpeedyTool;
}
