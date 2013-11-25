package speedytools;

import speedytools.common.CommonProxy;
import speedytools.common.clientserversynch.PacketHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;


@Mod(modid="SpeedyToolsMod", name="Speedy Tools Mod", version="0.0.1")
@NetworkMod(clientSideRequired=true, serverSideRequired=true, channels={"SpeedyTools"}, packetHandler = PacketHandler.class)
public class SpeedyToolsMod {

  // The instance of your mod that Forge uses.
  @Mod.Instance("SpeedyToolsMod")
  public static speedytools.SpeedyToolsMod instance;

  // Says where the client and server 'proxy' code is loaded.
  @SidedProxy(clientSide="speedytools.clientonly.CombinedClientProxy", serverSide="speedytools.common.DedicatedServerProxy")
  public static CommonProxy proxy;

  @EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    proxy.preInit();
  }

  @EventHandler
  public void load(FMLInitializationEvent event) {
    proxy.load();
  }

  @EventHandler
  public void postInit(FMLPostInitializationEvent event) {
    proxy.postInit();
  }

}
