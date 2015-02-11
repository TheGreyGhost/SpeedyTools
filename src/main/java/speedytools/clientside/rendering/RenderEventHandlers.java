//package speedytools.clientside.rendering;
//
//import net.minecraft.client.entity.EntityPlayerSP;
//import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.entity.EntityClientPlayerMP;
//import net.minecraft.client.gui.ScaledResolution;
//import net.minecraft.client.renderer.RenderGlobal;
//import net.minecraft.entity.player.EntityPlayer;
//import net.minecraftforge.client.event.DrawBlockHighlightEvent;
//import net.minecraftforge.client.event.RenderGameOverlayEvent;
//import net.minecraftforge.client.event.RenderWorldLastEvent;
//import speedytools.clientside.ClientSide;
//
///**
//Contains the custom Forge Event Handlers related to Rendering
//*/
//public class RenderEventHandlers
//{
//
//  /**
//   * Draw the custom crosshairs if reqd
//   * Otherwise, cancel the event so that the normal selection box is drawn.
//   * @param event
//   */
//  @SubscribeEvent
//  public void renderOverlayPre(RenderGameOverlayEvent.Pre event) {
//    if (!ClientSide.activeTool.toolIsActive()) {
//      return;
//    }
//
//    if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
//      RenderGameOverlayCrosshairsEvent renderGameOverlayCrosshairsEvent = new RenderGameOverlayCrosshairsEvent(event);
//      ClientSide.speedyToolRenderers.render(renderGameOverlayCrosshairsEvent, event.partialTicks);
//      event.setCanceled(renderGameOverlayCrosshairsEvent.isCanceled());
//    } else if (event.type == RenderGameOverlayEvent.ElementType.HOTBAR) {
//      RenderGameOverlayHotbarEvent renderGameOverlayHotbarEvent = new RenderGameOverlayHotbarEvent(event);
//      ClientSide.speedyToolRenderers.render(renderGameOverlayHotbarEvent, event.partialTicks);
//      event.setCanceled(renderGameOverlayHotbarEvent.isCanceled());
//    }
//
//
//    return;
//  }
//
//  @SubscribeEvent
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
//  @SubscribeEvent
//  public void worldRender(RenderWorldLastEvent event)
//  {
//    RenderGlobal context = event.context;
//    EntityPlayer player = (EntityPlayer)(Minecraft.getMinecraft().getRenderViewEntity());
//    EntityPlayerSP entityPlayerSP = (EntityPlayerSP)player;
//
//    float partialTick = event.partialTicks;
//
//    ClientSide.activeTool.updateForThisFrame(player.getEntityWorld(), entityPlayerSP, partialTick);
//    ClientSide.speedyToolRenderers.render(event, partialTick);
//  }
//
//}
