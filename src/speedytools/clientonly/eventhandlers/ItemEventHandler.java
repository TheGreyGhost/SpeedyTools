package speedytools.clientonly.eventhandlers;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.ForgeSubscribe;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.items.ItemCloneTool;
import speedytools.common.items.ItemSpeedyTool;

import java.util.List;

/**
 Contains the custom Forge Event Handlers related to Items
 */
public class ItemEventHandler {

  /**
   * If a SpeedyTools item is selected, draw nothing (drawing of selection box is performed in RenderWorldLastEvent).
   * Otherwise, cancel the event so that the normal selection box is drawn.
   * @param event
   */
  @ForgeSubscribe
  public void blockHighlightDecider(DrawBlockHighlightEvent event)
  {
    EntityPlayer player = event.player;
    ItemStack currentItem = player.inventory.getCurrentItem();
    boolean speedyToolHeld = currentItem != null && ItemSpeedyTool.isAspeedyTool(currentItem.itemID);
    boolean cloneToolHeld = currentItem != null && ItemCloneTool.isAcloneTool(currentItem.itemID);

    if (cloneToolHeld || speedyToolHeld) {
     event.setCanceled(true);
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
      itemSpeedyTool.renderSelection(player, partialTick);
    }

    if (cloneToolHeld) {
      ItemCloneTool itemCloneTool = (ItemCloneTool)currentItem.getItem();

      MovingObjectPosition target = itemCloneTool.rayTraceLineOfSight(player.worldObj, player);
      ChunkCoordinates selection = itemCloneTool.selectBlocks(target, player, currentItem, partialTick);

      ItemCloneTool.setCurrentToolSelection(itemCloneTool, selection);

      if (selection == null) return;
      itemCloneTool.renderSelection(player, partialTick);
      itemCloneTool.renderBoundaryField(player, partialTick);
    }

  }

}
