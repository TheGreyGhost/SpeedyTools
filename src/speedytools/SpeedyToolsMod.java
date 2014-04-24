package speedytools;

import speedytools.common.CommonProxy;
import speedytools.common.network.PacketHandlerRegistry;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;


@Mod(modid="speedytoolsmod", name="Build Faster Mod", version="2.0.0")
@NetworkMod(clientSideRequired=true, serverSideRequired=true, channels={"speedytools"}, packetHandler = PacketHandlerRegistry.class)
public class SpeedyToolsMod {

  // The instance of your mod that Forge uses.
  @Mod.Instance("speedytoolsmod")
  public static speedytools.SpeedyToolsMod instance;

  // Says where the client and server 'proxy' code is loaded.
  @SidedProxy(clientSide="speedytools.clientside.CombinedClientProxy", serverSide="speedytools.serverside.DedicatedServerProxy")
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
