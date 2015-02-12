//package speedytools.serverside;
//
//import net.minecraft.world.WorldServer;
//import net.minecraftforge.event.world.WorldEvent;
//import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//import speedytools.serverside.actions.SpeedyToolServerActions;
//
///**
//Contains the custom Forge Event Handlers relevant to the Server
//*/
//public class ServerEventHandler
//{
//  @SubscribeEvent
//  public void worldLoad(WorldEvent.Load event)
//  {
//    if (!(event.world instanceof WorldServer)) return;
//    SpeedyToolServerActions.worldLoadEvent(event.world);
//  }
//
//  @SubscribeEvent
//  public void worldUnload(WorldEvent.Unload event)
//  {
//    if (!(event.world instanceof WorldServer)) return;
//    SpeedyToolServerActions.worldUnloadEvent(event.world);
//  }
//}
