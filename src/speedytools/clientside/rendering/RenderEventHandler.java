package speedytools.clientside.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.ForgeSubscribe;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.items.ItemCloneTool;
import speedytools.common.items.ItemSpeedyTool;

import java.util.List;

/**
 Contains the custom Forge Event Handlers related to Rendering
 */
public class RenderEventHandler
{

  /**
   * Draw the custom crosshairs if reqd
   * Otherwise, cancel the event so that the normal selection box is drawn.
   * @param event
   */
  @ForgeSubscribe
  public void renderOverlayPre(RenderGameOverlayEvent.Pre event)
  {
    if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
    EntityPlayer player = Minecraft.getMinecraft().thePlayer;
    ItemStack currentItem = player.inventory.getCurrentItem();
    boolean speedyToolHeld = currentItem != null && ItemSpeedyTool.isAspeedyTool(currentItem.itemID);
    boolean cloneToolHeld = currentItem != null && ItemCloneTool.isAcloneTool(currentItem.itemID);

    if (cloneToolHeld) {
      ItemCloneTool tool = (ItemCloneTool)currentItem.getItem();
      boolean customRender = tool.renderCrossHairs(event.resolution, event.partialTicks);
      event.setCanceled(customRender);
    }
    return;
  }

  /**
   * If a speedy tool is equipped, selects the appropriate blocks and stores the selection into SpeedyToolsMod.currentlySelectedBlock
   *    along with the substrate used by the tool (the block to be placed) which is the block in the hotbar immediately to the left of the tool
   * Also renders the selection over the top of the existing world
   *
   * @param event
   */
  @ForgeSubscribe
  public void drawSelectionBox(RenderWorldLastEvent event)
  {
    RenderGlobal context = event.context;
    assert(context.mc.renderViewEntity instanceof EntityPlayer);
    EntityPlayer player = (EntityPlayer)context.mc.renderViewEntity;

    ItemStack currentItem = player.inventory.getCurrentItem();
    float partialTick = event.partialTicks;

    boolean speedyToolHeld = currentItem != null && ItemSpeedyTool.isAspeedyTool(currentItem.itemID);
    boolean cloneToolHeld = currentItem != null && ItemCloneTool.isAcloneTool(currentItem.itemID);
    if (!speedyToolHeld && !cloneToolHeld) return;

    if (speedyToolHeld) {
      ItemSpeedyTool itemSpeedyTool = (ItemSpeedyTool)currentItem.getItem();

      // the block to be placed is the one to the left of the tool in the hotbar
      int currentlySelectedHotbarSlot = player.inventory.currentItem;
      ItemStack itemStackToPlace = (currentlySelectedHotbarSlot == 0) ? null : player.inventory.getStackInSlot(currentlySelectedHotbarSlot-1);
      BlockWithMetadata blockToPlace = ItemSpeedyTool.getPlacedBlockFromItemStack(itemStackToPlace);

      MovingObjectPosition target = itemSpeedyTool.rayTraceLineOfSight(player.worldObj, player);
      List<ChunkCoordinates> selection = itemSpeedyTool.selectBlocks(target, player, currentItem, itemStackToPlace, partialTick);

      ItemSpeedyTool.setCurrentToolSelection(itemSpeedyTool, blockToPlace, selection);

      if (selection.isEmpty()) return;
      //  itemSpeedyTool.renderSelection(player, partialTick);
    }

    if (cloneToolHeld) {
      ItemCloneTool itemCloneTool = (ItemCloneTool)currentItem.getItem();

      MovingObjectPosition target = itemCloneTool.rayTraceLineOfSight(player.worldObj, player);
      itemCloneTool.highlightBlocks(target, player, currentItem, partialTick);
      itemCloneTool.renderBlockHighlight(player, partialTick);
      itemCloneTool.renderBoundaryField(player, partialTick);
    }

  }

}
