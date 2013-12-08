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
import speedytools.common.items.ItemSpeedyTool;

import java.util.List;

/**
 Contains the custom Forge Event Handlers related to Input
 */
public class InputEventHandler
{

  /**
   * If a SpeedyTools item is selected, draw nothing (drawing of selection box is performed in RenderWorldLastEvent).
   * Otherwise, cancel the event so that the normal selection box is drawn.
   * @param event
   */
  @ForgeSubscribe
  public void interceptMouseInput(MouseEvent event)
  {
    if (event.dwheel == 0) return;
    EntityPlayer player = Minecraft.getMinecraft().thePlayer;
    if (player == null) return;
    ItemStack currentItem = player.inventory.getCurrentItem();

    if (currentItem == null || !ItemSpeedyTool.isAspeedyTool(currentItem.getItem().itemID)) {
      return;
    }

    boolean controlKeyDown =  Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    if (!controlKeyDown) return;

    event.setCanceled(true);
    ItemSpeedyTool.mouseWheelMoved(event.dwheel);
    return;
  }

}
