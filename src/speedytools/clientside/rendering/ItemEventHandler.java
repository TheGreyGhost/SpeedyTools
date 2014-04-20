package speedytools.clientside.rendering;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.ForgeSubscribe;
import speedytools.clientside.ClientSide;

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
    if (ClientSide.activeTool.toolIsActive()) {
      event.setCanceled(true);
    }
    return;
    /*
    EntityPlayer player = event.player;
    ItemStack currentItem = player.inventory.getCurrentItem();
    boolean speedyToolHeld = currentItem != null && ItemSpeedyTool.isAspeedyTool(currentItem.itemID);
    boolean cloneToolHeld = currentItem != null && ItemCloneTool.isAcloneTool(currentItem.itemID);

    if (cloneToolHeld || speedyToolHeld) {
     event.setCanceled(true);
    }
    return;
    */
  }

  /**
   * If a speedy tool is equipped, updates its state from the world (eg the current selection, the block used by the tool, etct)
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

    //ItemStack currentItem = player.inventory.getCurrentItem();         //
    float partialTick = event.partialTicks;

    EntityClientPlayerMP entityClientPlayerMP = (EntityClientPlayerMP)player;
    ClientSide.activeTool.update(player.getEntityWorld(), entityClientPlayerMP, partialTick);
    ClientSide.speedyToolRenderers.render(RendererElement.RenderPhase.WORLD, player, partialTick);

    /*

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

      EntityClientPlayerMP entityClientPlayerMP = (EntityClientPlayerMP)player;
      ClientSide.activeTool.update(player.getEntityWorld(), entityClientPlayerMP, partialTick);
      ClientSide.speedyToolRenderers.render(RendererElement.RenderPhase.WORLD, player, partialTick);
      //itemSpeedyTool.renderSelection(player, partialTick);

    }

    if (cloneToolHeld) {
      ItemCloneTool itemCloneTool = (ItemCloneTool)currentItem.getItem();

      MovingObjectPosition target = itemCloneTool.rayTraceLineOfSight(player.worldObj, player);
      itemCloneTool.highlightBlocks(target, player, currentItem, partialTick);
      itemCloneTool.renderBlockHighlight(player, partialTick);
      itemCloneTool.renderBoundaryField(player, partialTick);
    }
    */
  }

}
