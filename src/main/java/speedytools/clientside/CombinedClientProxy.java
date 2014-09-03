package speedytools.clientside;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraftforge.common.MinecraftForge;
import speedytools.clientside.userinput.InputEventHandler;
import speedytools.clientside.userinput.SpeedyToolControls;
import speedytools.common.CommonProxy;

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
    ClientSide.preInitialise();
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
    ClientSide.postInitialise();
    super.postInit();
    SpeedyToolControls.initialiseInterceptors();
//    MinecraftForge.EVENT_BUS.register(new ItemEventHandler());
//    MinecraftForge.EVENT_BUS.register(new SoundsRegistry());
    MinecraftForge.EVENT_BUS.register(new InputEventHandler());
//    MinecraftForge.EVENT_BUS.register(new RenderEventHandlers());
    FMLCommonHandler.instance().bus().register(new ClientTickHandler());


//
//    MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
//
//    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedyWandStrong,
//                                           new SpeedyToolWandStrong(RegistryForItems.itemSpeedyWandStrong,
//                                                                    ClientSide.speedyToolRenderers,
//                                                                    ClientSide.speedyToolSounds,
//                                                                    ClientSide.undoManagerSimple
//                                                                   ));
//    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedyWandWeak,
//                                            new SpeedyToolWandWeak(RegistryForItems.itemSpeedyWandWeak,
//                                                                    ClientSide.speedyToolRenderers,
//                                                                    ClientSide.speedyToolSounds,
//                                                                    ClientSide.undoManagerSimple
//                                                                  ));
//    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedyOrb,
//            new SpeedyToolOrb(RegistryForItems.itemSpeedyOrb,
//                    ClientSide.speedyToolRenderers,
//                    ClientSide.speedyToolSounds,
//                    ClientSide.undoManagerSimple
//            ));
//    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedySceptre,
//            new SpeedyToolSceptre(RegistryForItems.itemSpeedySceptre,
//                    ClientSide.speedyToolRenderers,
//                    ClientSide.speedyToolSounds,
//                    ClientSide.undoManagerSimple
//            ));
//    SpeedyToolBoundary speedyToolBoundary = new SpeedyToolBoundary(RegistryForItems.itemSpeedyBoundary,
//            ClientSide.speedyToolRenderers,
//            ClientSide.speedyToolSounds,
//            ClientSide.undoManagerSimple);
//
//    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedyBoundary, speedyToolBoundary);
//
//    SelectionPacketSender selectionPacketSenderComplex = new SelectionPacketSender(ClientSide.packetHandlerRegistry, ClientSide.packetSenderClient);
//
//    ClientSide.activeTool.registerToolType(RegistryForItems.itemComplexCopy,
//            new SpeedyToolComplexCopy(RegistryForItems.itemComplexCopy,
//                                      ClientSide.speedyToolRenderers,
//                                      ClientSide.speedyToolSounds,
//                                      ClientSide.undoManagerComplex,
//                                      ClientSide.getCloneToolsNetworkClient(), speedyToolBoundary,
//                                      selectionPacketSenderComplex
//                                    )
//            );
//
//    ClientSide.activeTool.registerToolType(RegistryForItems.itemComplexDelete,
//            new SpeedyToolComplexDelete(RegistryForItems.itemComplexDelete,
//                    ClientSide.speedyToolRenderers,
//                    ClientSide.speedyToolSounds,
//                    ClientSide.undoManagerComplex,
//                    ClientSide.getCloneToolsNetworkClient(), speedyToolBoundary,
//                    selectionPacketSenderComplex
//            )
//    );
//
//    ClientSide.activeTool.registerToolType(RegistryForItems.itemComplexMove,
//            new SpeedyToolComplexMove(RegistryForItems.itemComplexMove,
//                    ClientSide.speedyToolRenderers,
//                    ClientSide.speedyToolSounds,
//                    ClientSide.undoManagerComplex,
//                    ClientSide.getCloneToolsNetworkClient(), speedyToolBoundary,
//                    selectionPacketSenderComplex
//            )
//    );
//
//    if (SpeedyToolsOptions.getTesterToolsEnabled()) {
//      ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedyTester,
//              new SpeedyToolTester(RegistryForItems.itemSpeedyTester,
//                                  ClientSide.speedyToolRenderers,
//                                  ClientSide.speedyToolSounds,
//                                  ClientSide.undoManagerSimple
//              ));
//
//    }

  }
}