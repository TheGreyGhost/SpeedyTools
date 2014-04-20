package speedytools.clientside;

import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.common.MinecraftForge;
import speedytools.clientside.rendering.*;
import speedytools.clientside.tools.*;
import speedytools.clientside.userinput.ClientTickHandler;
import speedytools.clientside.userinput.InputEventHandler;
import speedytools.clientside.userinput.SpeedyToolControls;
import speedytools.common.CommonProxy;
import speedytools.common.items.RegistryForItems;

/**
 * CombinedClientProxy is used to set up the mod and start it running when installed on a standalone client.
 *   It should not contain any code necessary for proper operation on a DedicatedServer.  Code required for both
 *   CombinedClient and dedicated server should go into CommonProxy
 */
public class CombinedClientProxy extends CommonProxy {

  /**
   * Run before anything else. Read your config, create blocks, items, etc, and register them with the GameRegistry
   */
  @Override
  public void preInit()
  {
    super.preInit();
  }

  /**
   * Do your mod setup. Build whatever data structures you care about. Register recipes,
   * send FMLInterModComms messages to other mods.
   */
  @Override
  public void load()
  {
    super.load();
  }

  /**
   * Handle interaction with other mods, complete your setup based on this.
   */
  @Override
  public void postInit()
  {
    super.postInit();
    SpeedyToolControls.initialiseInterceptors();
    MinecraftForge.EVENT_BUS.register(new ItemEventHandler());
    MinecraftForge.EVENT_BUS.register(new SoundsRegistry());
    MinecraftForge.EVENT_BUS.register(new InputEventHandler());
    MinecraftForge.EVENT_BUS.register(new RenderEventHandler());
    TickRegistry.registerTickHandler(new ClientTickHandler(), Side.CLIENT);
    ClientSide.initialise();

    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedyWandStrong,
                                           new SpeedyToolWandStrong(RegistryForItems.itemSpeedyWandStrong,
                                                                    ClientSide.speedyToolRenderers,
                                                                    ClientSide.speedyToolSounds,
                                                                    ClientSide.undoManagerClient
                                                                   ));
    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedyWandWeak,
                                            new SpeedyToolWandWeak(RegistryForItems.itemSpeedyWandWeak,
                                                                    ClientSide.speedyToolRenderers,
                                                                    ClientSide.speedyToolSounds,
                                                                    ClientSide.undoManagerClient
                                                                  ));
    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedyOrb,
            new SpeedyToolOrb(RegistryForItems.itemSpeedyOrb,
                    ClientSide.speedyToolRenderers,
                    ClientSide.speedyToolSounds,
                    ClientSide.undoManagerClient
            ));
    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedySceptre,
            new SpeedyToolSceptre(RegistryForItems.itemSpeedySceptre,
                    ClientSide.speedyToolRenderers,
                    ClientSide.speedyToolSounds,
                    ClientSide.undoManagerClient
            ));
    ClientSide.activeTool.registerToolType(RegistryForItems.itemCloneBoundary,
            new SpeedyToolBoundary(RegistryForItems.itemCloneBoundary,
                    ClientSide.speedyToolRenderers,
                    ClientSide.speedyToolSounds,
                    ClientSide.undoManagerClient
            ));

  }
}