package speedytools.clientonly.eventhandlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.ForgeSubscribe;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import speedytools.clientonly.SelectionBoxRenderer;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.items.ItemCloneTool;
import speedytools.common.items.ItemSpeedyTool;

import java.util.List;

/**
 Contains the custom Forge Event Handlers related to Input
 */
public class InputEventHandler
{

  @ForgeSubscribe
  public void interceptMouseInput(MouseEvent event)
  {
    if (event.dwheel == 0) return;
    EntityPlayer player = Minecraft.getMinecraft().thePlayer;
    if (player == null) return;
    ItemStack currentItem = player.inventory.getCurrentItem();
    boolean speedyToolHeld = currentItem != null && ItemSpeedyTool.isAspeedyTool(currentItem.itemID);
    boolean cloneToolHeld = currentItem != null && ItemCloneTool.isAcloneTool(currentItem.itemID);

    if (!speedyToolHeld && !cloneToolHeld) return;

    boolean controlKeyDown =  Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    if (!controlKeyDown) return;

    event.setCanceled(true);
    if (speedyToolHeld) {
      ItemSpeedyTool.mouseWheelMoved(event.dwheel);
    } else if (cloneToolHeld) {
      ItemCloneTool.mouseWheelMoved(event.dwheel);
    }
    return;
  }

}
