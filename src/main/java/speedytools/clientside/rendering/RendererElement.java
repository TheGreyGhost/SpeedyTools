//package speedytools.clientside.rendering;
//
//import net.minecraft.client.gui.ScaledResolution;
//import net.minecraft.entity.player.EntityPlayer;
//import net.minecraft.world.World;
//import net.minecraftforge.event.Event;
//
//import java.util.Collection;
//
///**
// * User: The Grey Ghost
// * Date: 14/04/14
// */
//public interface RendererElement
//{
//  /** renders an element in the world
//   * @param renderPhase
//   * @param player
//   * @param animationTickCount
//   * @param partialTick
//   */
////  public void renderWorld(RenderPhase renderPhase, EntityPlayer player, int animationTickCount, float partialTick);
//
//  /** renders an element on the overlay
//   * @param renderPhase
//   * @param scaledResolution
//   * @param animationTickCount
//   * @param partialTick
//   */
////  public void renderOverlay(RenderPhase renderPhase, ScaledResolution scaledResolution, int animationTickCount, float partialTick);
////  public boolean renderInThisPhase(RenderPhase renderPhase);
//
//  /**
//   * Which events is this RendererElement interested in?
//   * @return a collection of events that the Renderer wants to receive.
//   */
//  public Collection<Class<? extends Event>> eventsToReceive();
//
//  /**
//   * render this element in response to the given event
//   * @param partialTick
//   */
//  public void render(Event event, float partialTick);
//
////  public enum RenderPhase {
////    CROSSHAIRS, WORLD,
////  }
//
//}
