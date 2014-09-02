//package speedytools.clientside.rendering;
//
//import com.sun.deploy.util.SessionState;
//import net.minecraft.client.entity.EntityClientPlayerMP;
//import net.minecraft.client.gui.ScaledResolution;
//import net.minecraft.client.renderer.RenderGlobal;
//import net.minecraft.entity.player.EntityPlayer;
//import net.minecraftforge.client.event.DrawBlockHighlightEvent;
//import net.minecraftforge.client.event.RenderGameOverlayEvent;
//import net.minecraftforge.client.event.RenderWorldLastEvent;
//import net.minecraftforge.event.ForgeSubscribe;
//import speedytools.clientside.ClientSide;
//
///**
// Contains the custom Forge Event Handlers related to Rendering
// */
//public class RenderEventHandlers
//{
//
//  /**
//   * Draw the custom crosshairs if reqd
//   * Otherwise, cancel the event so that the normal selection box is drawn.
//   * @param event
//   */
//  @ForgeSubscribe
//  public void renderOverlayPre(RenderGameOverlayEvent.Pre event) {
//    if (!ClientSide.activeTool.toolIsActive()) {
//      return;
//    }
//
//    if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
//      RenderGameOverlayCrosshairsEvent renderGameOverlayCrosshairsEvent = new RenderGameOverlayCrosshairsEvent(event);
//      ClientSide.speedyToolRenderers.render(renderGameOverlayCrosshairsEvent, event.partialTicks);
//      event.setCanceled(renderGameOverlayCrosshairsEvent.isCanceled());
////      if (ClientSide.speedyToolRenderers.areAnyRendersInThisPhase(RendererElement.RenderPhase.CROSSHAIRS)) {
////        float partialTick = event.partialTicks;
////        ClientSide.speedyToolRenderers.renderOverlay(RendererElement.RenderPhase.CROSSHAIRS, event.resolution, ClientSide.getGlobalTickCount(), partialTick);
////        event.setCanceled(true);
////      }
//    } else if (event.type == RenderGameOverlayEvent.ElementType.HOTBAR) {
//      RenderGameOverlayHotbarEvent renderGameOverlayHotbarEvent = new RenderGameOverlayHotbarEvent(event);
//      ClientSide.speedyToolRenderers.render(renderGameOverlayHotbarEvent, event.partialTicks);
//      event.setCanceled(renderGameOverlayHotbarEvent.isCanceled());
//    }
//
////    if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
////      if (!ClientSide.activeTool.toolIsActive()) {
////        return;
////      }
////      if (ClientSide.speedyToolRenderers.areAnyRendersInThisPhase(RendererElement.RenderPhase.CROSSHAIRS)) {
////        float partialTick = event.partialTicks;
////        ClientSide.speedyToolRenderers.renderOverlay(RendererElement.RenderPhase.CROSSHAIRS, event.resolution, ClientSide.getGlobalTickCount(), partialTick);
////        event.setCanceled(true);
////      }
////    } else if (event.type == RenderGameOverlayEvent.ElementType.HOTBAR) {
////      ClientSide.speedyToolRenderers.render(event, event.partialTicks);
////      ScaledResolution res = event.resolution;
////      int width = res.getScaledWidth();
////      int height = res.getScaledHeight();
////      RendererHotbarCurrentItem.getInstance().renderHotbar(width, height, event.partialTicks);
////      event.setCanceled(true);
//
//    return;
//  }
//
//
//
////    EntityPlayer player = Minecraft.getMinecraft().thePlayer;
////    ItemStack currentItem = player.inventory.getCurrentItem();
////    boolean cloneToolHeld = currentItem != null && ItemSpeedyClonerBase.isAcloneTool(currentItem.itemID);
////
////    if (cloneToolHeld) {
////      ItemSpeedyClonerBase tool = (ItemSpeedyClonerBase)currentItem.getItem();
////      boolean customRender = tool.renderCrossHairs(event.resolution, event.partialTicks);
////      event.setCanceled(customRender);
////    }
////    return;
////  }
//
//  @ForgeSubscribe
//  public void blockHighlightDecider(DrawBlockHighlightEvent event)
//  {
//    if (ClientSide.activeTool.toolIsActive()) {
//      event.setCanceled(true);
//    }
//    return;
//  }
//
//
//  /**
//   * If a speedy tool is equipped, selects the appropriate blocks and stores the selection into SpeedyToolsMod.blockUnderCursor
//   *    along with the substrate used by the tool (the block to be placed) which is the block in the hotbar immediately to the left of the tool
//   * Also renders the selection over the top of the existing world
//   *
//   * @param event
//   */
//  @ForgeSubscribe
//  public void worldRender(RenderWorldLastEvent event)
//  {
//    RenderGlobal context = event.context;
//    assert(context.mc.renderViewEntity instanceof EntityPlayer);
//    EntityPlayer player = (EntityPlayer)context.mc.renderViewEntity;
//    EntityClientPlayerMP entityClientPlayerMP = (EntityClientPlayerMP)player;
//
//    //ItemStack currentItem = player.inventory.getCurrentItem();         //
//    float partialTick = event.partialTicks;
//
//    ClientSide.activeTool.updateForThisFrame(player.getEntityWorld(), entityClientPlayerMP, partialTick);
////    ClientSide.speedyToolRenderers.renderWorld(RendererElement.RenderPhase.WORLD, player, ClientSide.getGlobalTickCount(), partialTick);
//    ClientSide.speedyToolRenderers.render(event, partialTick);
//
///*
//    if (speedyToolHeld) {
//      ItemSpeedyTool itemSpeedyTool = (ItemSpeedyTool)currentItem.getItem();
//
//      // the block to be placed is the one to the left of the tool in the hotbar
//      int currentlySelectedHotbarSlot = player.inventory.currentItem;
//      ItemStack itemStackToPlace = (currentlySelectedHotbarSlot == 0) ? null : player.inventory.getStackInSlot(currentlySelectedHotbarSlot-1);
//      BlockWithMetadata blockToPlace = ItemSpeedyTool.getPlacedBlockFromItemStack(itemStackToPlace);
//
//      MovingObjectPosition target = itemSpeedyTool.rayTraceLineOfSight(player.worldObj, player);
//      List<ChunkCoordinates> selection = itemSpeedyTool.selectBlocks(target, player, currentItem, itemStackToPlace, partialTick);
//
//      ItemSpeedyTool.setCurrentToolSelection(itemSpeedyTool, blockToPlace, selection);
//
//      if (selection.isEmpty()) return;
//      //  itemSpeedyTool.renderSelection(player, partialTick);
//    }
//*/
///*
//    if (cloneToolHeld) {
//      ItemCloneTool itemCloneTool = (ItemCloneTool)currentItem.getItem();
//
//      MovingObjectPosition target = itemCloneTool.rayTraceLineOfSight(player.worldObj, player);
//      itemCloneTool.highlightBlocks(target, player, currentItem, partialTick);
//      itemCloneTool.renderBlockHighlight(player, partialTick);
//      itemCloneTool.renderBoundaryField(player, partialTick);
//    }
//*/
//  }
//
//}
