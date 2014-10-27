package speedytools.clientside.rendering;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;
import speedytools.common.utilities.ErrorLog;

/**
 * Created by TheGreyGhost on 28/10/14.
 */
public class RendererInventoryItemInfinite implements IItemRenderer
{
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
  public void renderItem(ItemRenderType type, ItemStack item, Object... data) {

  render the icon, then draw font over the top

    this.zLevel -= 50.0F;        // undo the renderItemAndEffectIntoGUI

    render overlay

    this.zLevel += 50.0F;

    renderItem.renderItemIntoGUI

    RenderItem.getInstance()

    this.mc.fontRenderer

    render over the top

    if (p_94148_3_.stackSize > 1 || p_94148_6_ != null)
    {
      String s1 = p_94148_6_ == null ? String.valueOf(p_94148_3_.stackSize) : p_94148_6_;
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_BLEND);
      p_94148_1_.drawStringWithShadow(s1, p_94148_4_ + 19 - 2 - p_94148_1_.getStringWidth(s1), p_94148_5_ + 6 + 3, 16777215);
      GL11.glEnable(GL11.GL_LIGHTING);
      GL11.glEnable(GL11.GL_DEPTH_TEST);
    }



  }
}
