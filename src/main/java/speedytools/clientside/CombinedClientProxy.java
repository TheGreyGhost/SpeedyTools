package speedytools.clientside;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import speedytools.clientside.rendering.ItemEventHandler;
import speedytools.clientside.rendering.RenderEventHandlers;
import speedytools.clientside.rendering.RendererInventoryItemInfinite;
import speedytools.clientside.tools.*;
import speedytools.clientside.userinput.InputEventHandler;
import speedytools.clientside.userinput.SpeedyToolControls;
import speedytools.common.CommonProxy;
import speedytools.common.SpeedyToolsOptions;
import speedytools.common.items.RegistryForItems;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

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
    ClientSide.load();
    super.load();

    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedyWandStrong,
            new SpeedyToolWandStrong(RegistryForItems.itemSpeedyWandStrong,
                    ClientSide.speedyToolRenderers,
                    ClientSide.speedyToolSounds,
                    ClientSide.undoManagerSimple,
                    ClientSide.packetSenderClient
            ));
    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedyWandWeak,
            new SpeedyToolWandWeak(RegistryForItems.itemSpeedyWandWeak,
                    ClientSide.speedyToolRenderers,
                    ClientSide.speedyToolSounds,
                    ClientSide.undoManagerSimple,
                    ClientSide.packetSenderClient
            ));

    CommonSelectionState commonSelectionState = new CommonSelectionState();
    SpeedyToolBoundary speedyToolBoundary = new SpeedyToolBoundary(RegistryForItems.itemSpeedyBoundary,
            ClientSide.speedyToolRenderers,
            ClientSide.speedyToolSounds,
            ClientSide.undoManagerSimple,
            ClientSide.packetSenderClient);

    SpeedyToolOrb speedyToolOrb = new SpeedyToolOrb(RegistryForItems.itemSpeedyOrb,
            ClientSide.speedyToolRenderers,
            ClientSide.speedyToolSounds,
            ClientSide.undoManagerSimple,
            ClientSide.packetSenderClient);
    SpeedyToolComplexOrb speedyToolComplexOrb = new SpeedyToolComplexOrb(RegistryForItems.itemSpeedyOrb,
            ClientSide.speedyToolRenderers,
            ClientSide.speedyToolSounds,
            ClientSide.undoManagerComplex,
            ClientSide.getCloneToolsNetworkClient(), speedyToolBoundary,
            ClientSide.clientVoxelSelection, commonSelectionState,
            ClientSide.selectionPacketSenderComplex,
            ClientSide.packetSenderClient);

    SpeedyToolSimpleAndComplex simpleComplexOrb = new SpeedyToolSimpleAndComplex(speedyToolOrb, speedyToolComplexOrb,
            RegistryForItems.itemSpeedyOrb,
            ClientSide.speedyToolRenderers,
            ClientSide.speedyToolSounds,
            ClientSide.undoManagerComplex,
            ClientSide.packetSenderClient);

    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedyOrb, simpleComplexOrb);

    SpeedyToolSceptre speedyToolSceptre = new SpeedyToolSceptre(RegistryForItems.itemSpeedySceptre,
            ClientSide.speedyToolRenderers,
            ClientSide.speedyToolSounds,
            ClientSide.undoManagerSimple,
            ClientSide.packetSenderClient);
    SpeedyToolComplexSceptre speedyToolComplexSceptre = new SpeedyToolComplexSceptre(RegistryForItems.itemSpeedySceptre,
            ClientSide.speedyToolRenderers,
            ClientSide.speedyToolSounds,
            ClientSide.undoManagerComplex,
            ClientSide.getCloneToolsNetworkClient(), speedyToolBoundary,
            ClientSide.clientVoxelSelection, commonSelectionState,
            ClientSide.selectionPacketSenderComplex,
            ClientSide.packetSenderClient);
    SpeedyToolSimpleAndComplex simpleComplexSceptre = new SpeedyToolSimpleAndComplex(speedyToolSceptre, speedyToolComplexSceptre,
            RegistryForItems.itemSpeedySceptre,
            ClientSide.speedyToolRenderers,
            ClientSide.speedyToolSounds,
            ClientSide.undoManagerComplex,
            ClientSide.packetSenderClient);
    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedySceptre, simpleComplexSceptre);

    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedyBoundary, speedyToolBoundary);

    ClientSide.activeTool.registerToolType(RegistryForItems.itemComplexCopy,
            new SpeedyToolComplexCopy(RegistryForItems.itemComplexCopy,
                                      ClientSide.speedyToolRenderers,
                                      ClientSide.speedyToolSounds,
                                      ClientSide.undoManagerComplex,
                                      ClientSide.getCloneToolsNetworkClient(), speedyToolBoundary,
                                      ClientSide.clientVoxelSelection, commonSelectionState,
                                      ClientSide.selectionPacketSenderComplex,
                    ClientSide.packetSenderClient
                                    )
            );

    ClientSide.activeTool.registerToolType(RegistryForItems.itemComplexDelete,
            new SpeedyToolComplexDelete(RegistryForItems.itemComplexDelete,
                    ClientSide.speedyToolRenderers,
                    ClientSide.speedyToolSounds,
                    ClientSide.undoManagerComplex,
                    ClientSide.getCloneToolsNetworkClient(), speedyToolBoundary,
                    ClientSide.clientVoxelSelection, commonSelectionState,
                    ClientSide.selectionPacketSenderComplex,
                    ClientSide.packetSenderClient
            )
    );

    ClientSide.activeTool.registerToolType(RegistryForItems.itemComplexMove,
            new SpeedyToolComplexMove(RegistryForItems.itemComplexMove,
                    ClientSide.speedyToolRenderers,
                    ClientSide.speedyToolSounds,
                    ClientSide.undoManagerComplex,
                    ClientSide.getCloneToolsNetworkClient(), speedyToolBoundary,
                    ClientSide.clientVoxelSelection, commonSelectionState,
                    ClientSide.selectionPacketSenderComplex,
                    ClientSide.packetSenderClient
            )
    );

    if (SpeedyToolsOptions.getTesterToolsEnabled()) {
      ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedyTester,
              new SpeedyToolTester(RegistryForItems.itemSpeedyTester,
                      ClientSide.speedyToolRenderers,
                      ClientSide.speedyToolSounds,
                      ClientSide.undoManagerSimple,
                      ClientSide.packetSenderClient
              ));

    }

    MinecraftForgeClient.registerItemRenderer(RegistryForItems.itemSpeedyOrb, new RendererInventoryItemInfinite(RegistryForItems.itemSpeedyOrb));
    MinecraftForgeClient.registerItemRenderer(RegistryForItems.itemSpeedySceptre, new RendererInventoryItemInfinite(RegistryForItems.itemSpeedySceptre));
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
    MinecraftForge.EVENT_BUS.register(new ItemEventHandler());
//    MinecraftForge.EVENT_BUS.register(new SoundsRegistry());
    MinecraftForge.EVENT_BUS.register(new InputEventHandler());
    MinecraftForge.EVENT_BUS.register(new RenderEventHandlers());
    FMLCommonHandler.instance().bus().register(new ClientTickHandler());

    MinecraftForge.EVENT_BUS.register(new ClientEventHandler());


  }

  /**
   * Obtains the folder that world save backups should be stored in.
   * For Integrated Server, this is the saves folder
   * For Dedicated Server, a new 'backupsaves' folder is created in the same folder that contains the world save directory
   *
   * @return the folder where backup saves should be created
   */
  @Override
  public Path getOrCreateSaveBackupsFolder() throws IOException {
    return new File(Minecraft.getMinecraft().mcDataDir, "saves").toPath();
  }
}