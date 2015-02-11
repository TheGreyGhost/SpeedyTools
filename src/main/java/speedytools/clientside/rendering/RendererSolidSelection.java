//package speedytools.clientside.rendering;
//
//import net.minecraftforge.fml.common.eventhandler.Event;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.renderer.RenderGlobal;
//import net.minecraft.client.renderer.texture.TextureMap;
//import net.minecraft.entity.player.EntityPlayer;
//import net.minecraft.util.Vec3;
//import net.minecraftforge.client.event.RenderWorldLastEvent;
//import org.lwjgl.opengl.GL11;
//import speedytools.clientside.ClientSide;
//import speedytools.clientside.selections.BlockVoxelMultiSelectorRenderer;
//import speedytools.clientside.SpeedyToolsOptionsClient;
//import speedytools.common.utilities.Colour;
//import speedytools.common.utilities.QuadOrientation;
//
//import java.util.ArrayList;
//import java.util.Collection;
//
///**
//* User: The Grey Ghost
//* Date: 18/04/2014
//*  * This class is used to render the boundary field (translucent cuboid)
//* Usage:
//* (1) Call the constructor, providing a BoundaryFieldRenderInfoUpdateLink:
//*     This interface is used to fill the supplied BoundaryFieldRenderInfo with the requested information for a render.
//* (2) When ready to render, call .render.
//
//*/
//public class RendererSolidSelection implements RendererElement
//{
//  public RendererSolidSelection(SolidSelectionRenderInfoUpdateLink i_infoProvider)
//  {
//    infoProvider = i_infoProvider;
//    renderInfo = new SolidSelectionRenderInfo();
//  }
//
//  @Override
//  public Collection<Class<? extends Event>> eventsToReceive() {
//    ArrayList<Class<? extends Event>> retval = new ArrayList<Class<? extends Event>>();
//    retval.add(RenderWorldLastEvent.class);
//    return retval;
//  }
//
//  @Override
//  public void render(Event event, float partialTick) {
//    RenderWorldLastEvent fullEvent = (RenderWorldLastEvent)event;
//    RenderGlobal context = fullEvent.context;
//    EntityPlayer player = (EntityPlayer)(Minecraft.getMinecraft().getRenderViewEntity());
//    renderWorld(player, ClientSide.getGlobalTickCount(), partialTick);
//  }
//
//  /**
//   * render the boundary field if there is one selected
//   * @param player
//   * @param animationTickCount
//   * @param partialTick
//   */
//  public void renderWorld(EntityPlayer player, int animationTickCount, float partialTick)
//  {
//    boolean shouldIRender = infoProvider.refreshRenderInfo(renderInfo, player, partialTick);
//    if (!shouldIRender) return;
//
//    Vec3 playerOrigin = player.getPosition(partialTick);
//
//    try {
//      GL11.glPushMatrix();
//      GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
//      Vec3 playerRelativeToSelectionOrigin = playerOrigin.addVector(-renderInfo.draggedSelectionOriginX,
//                                                                    -renderInfo.draggedSelectionOriginY,
//                                                                    -renderInfo.draggedSelectionOriginZ);
//      if (renderInfo.opaque) {
//        GL11.glDisable(GL11.GL_BLEND);
//      } else {
//        GL11.glEnable(GL11.GL_BLEND);
//        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//      }
//
//      Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
//      GL11.glDisable(GL11.GL_ALPHA_TEST);
//
//      int renderDistanceBlocks = SpeedyToolsOptionsClient.getRenderDistanceInBlocks();
//      if (renderInfo.selectorRenderer != null) {
//        renderInfo.selectorRenderer.renderSelection(playerRelativeToSelectionOrigin, renderDistanceBlocks,
//                                                    renderInfo.selectionOrientation, renderInfo.renderColour);         // todo: later - maybe - clip by frustrum
//      }
//    } finally {
//      GL11.glPopAttrib();
//      GL11.glPopMatrix();
//    }
//  }
//
//  /**  The SolidSelectionRenderInfoUpdateLink and SolidSelectionRenderInfo are used to retrieve the necessary information for rendering from the current tool
//   *  If refreshRenderInfo returns false, no render is performed.
//   */
//  public interface SolidSelectionRenderInfoUpdateLink
//  {
//    public boolean refreshRenderInfo(SolidSelectionRenderInfo infoToUpdate, EntityPlayer player, float partialTick);
//  }
//
//  public static class SolidSelectionRenderInfo
//  {
//    public BlockVoxelMultiSelectorRenderer selectorRenderer;     // the voxel selection to be rendered
//    public double draggedSelectionOriginX;                      // the coordinates of the selection origin, after it has been dragged from its starting point
//    public double draggedSelectionOriginY;
//    public double draggedSelectionOriginZ;
//    public boolean opaque;                        // if false, make partially transparent
//    public Colour renderColour;
//    public QuadOrientation selectionOrientation;
//  }
//
//  SolidSelectionRenderInfoUpdateLink infoProvider;
//  SolidSelectionRenderInfo renderInfo;
//}
