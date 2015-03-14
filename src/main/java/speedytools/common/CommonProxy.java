package speedytools.common;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import speedytools.common.blocks.RegistryForBlocks;
import speedytools.common.items.RegistryForItems;
import speedytools.serverside.ServerEventHandler;
import speedytools.serverside.ServerSide;
import speedytools.serverside.ServerTickHandler;

import java.io.IOException;
import java.nio.file.Path;

/**
 * CommonProxy is used to set up the mod and start it running.  It contains all the code that should run on both the
 *   Standalone client and the dedicated server.
 */
public abstract class CommonProxy {

  /**
   * Run before anything else. Read your config, create blocks, items, etc, and register them with the GameRegistry
   */
  public void preInit()
  {
    RegistryForItems.initialise();
    RegistryForBlocks.initialise();
  }

  /**
   * Do your mod setup. Build whatever data structures you care about. Register recipes,
   * send FMLInterModComms messages to other mods.
   */
  public void load()
  {
    ServerSide.load();
  }

  /**
   * Handle interaction with other mods, complete your setup based on this.
   */
  public void postInit()
  {
    MinecraftForge.EVENT_BUS.register(new ServerEventHandler());
    FMLCommonHandler.instance().bus().register(new ServerTickHandler());
    FMLCommonHandler.instance().bus().register(ServerSide.getPlayerTrackerRegistry());
  }

  /**
   * Obtains the folder that world save backups should be stored in.
   * For Integrated Server, this is the saves folder
   * For Dedicated Server, a new 'backupsaves' folder is created in the same folder that contains the world save directory
   * @return the folder where backup saves should be created
   */
  public abstract Path getOrCreateSaveBackupsFolder() throws IOException;

  /** Places the processor for an incoming message onto the correct thread
   *
   * @param ctx message context
   * @param messageProcessor a Runnable that calls the correct message handler, for example
   *     messageProcessor = new Runnable()  {
            public void run() {
              processMessage(worldClient, message);
            }
            });
   * @return true for success, false for failure
   */
  public abstract boolean enqueueMessageOnCorrectThread(MessageContext ctx, Runnable messageProcessor);

//  /**
//   * Gets the NetworkTrafficMonitor used to monitor network traffic on the current side
//   * @return
//   */
//  public NetworkTrafficMonitor getNetworkTrafficMonitorForSide()
//  {
//    if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
//      return ClientSide.getNetworkTrafficMonitor();
//    } else {
//      return ServerSide.getNetworkTrafficMonitor();
//    }
//  }

}
