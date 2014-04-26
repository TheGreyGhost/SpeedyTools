package speedytools.common;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.network.packet.Packet;
import net.minecraftforge.common.MinecraftForge;
import speedytools.clientside.ClientTickHandler;
import speedytools.clientside.rendering.SoundsRegistry;
import speedytools.common.items.RegistryForItems;
import speedytools.serverside.*;

/**
 * CommonProxy is used to set up the mod and start it running.  It contains all the code that should run on both the
 *   Standalone client and the dedicated server.
 */
public class CommonProxy {

  /**
   * Run before anything else. Read your config, create blocks, items, etc, and register them with the GameRegistry
   */
  public void preInit()
  {
    RegistryForItems.initialise();
  }

  /**
   * Do your mod setup. Build whatever data structures you care about. Register recipes,
   * send FMLInterModComms messages to other mods.
   */
  public void load()
  {
  }

  /**
   * Handle interaction with other mods, complete your setup based on this.
   */
  public void postInit()
  {
    ServerSide.initialise();
    MinecraftForge.EVENT_BUS.register(new ServerEventHandler());
    TickRegistry.registerTickHandler(new ServerTickHandler(), Side.SERVER);
  }
}
