package speedytools.clientside;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.registry.GameRegistry;
import speedytools.SpeedyToolsMod;
import speedytools.clientside.rendering.ItemEventHandler;
import speedytools.clientside.rendering.RenderEventHandlers;
import speedytools.clientside.tools.*;
import speedytools.clientside.userinput.InputEventHandler;
import speedytools.clientside.userinput.SpeedyToolControls;
import speedytools.common.CommonProxy;
import speedytools.common.SpeedyToolsOptions;
import speedytools.common.blocks.RegistryForBlocks;
import speedytools.common.items.ItemSpeedyOrb;
import speedytools.common.items.ItemSpeedyTool;
import speedytools.common.items.RegistryForItems;
import speedytools.common.utilities.ErrorLog;

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

    RegistryForItems.itemSpeedyBoundary.registerVariants();
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
//      todo uncomment
    CommonSelectionState commonSelectionState = new CommonSelectionState();
    SpeedyToolBoundary speedyToolBoundary = new SpeedyToolBoundary(RegistryForItems.itemSpeedyBoundary,
            ClientSide.speedyToolRenderers,
            ClientSide.speedyToolSounds,
            ClientSide.undoManagerSimple,
            ClientSide.packetSenderClient);
//
//    SpeedyToolOrb speedyToolOrb = new SpeedyToolOrb(RegistryForItems.itemSpeedyOrb,
//            ClientSide.speedyToolRenderers,
//            ClientSide.speedyToolSounds,
//            ClientSide.undoManagerSimple,
//            ClientSide.packetSenderClient);
//    SpeedyToolComplexOrb speedyToolComplexOrb = new SpeedyToolComplexOrb(RegistryForItems.itemSpeedyOrb,
//            ClientSide.speedyToolRenderers,
//            ClientSide.speedyToolSounds,
//            ClientSide.undoManagerComplex,
//            ClientSide.getCloneToolsNetworkClient(), speedyToolBoundary,
//            ClientSide.clientVoxelSelection, commonSelectionState,
//            ClientSide.selectionPacketSenderComplex,
//            ClientSide.packetSenderClient);
//
//    SpeedyToolSimpleAndComplex simpleComplexOrb = new SpeedyToolSimpleAndComplex(speedyToolOrb, speedyToolComplexOrb,
//            RegistryForItems.itemSpeedyOrb,
//            ClientSide.speedyToolRenderers,
//            ClientSide.speedyToolSounds,
//            ClientSide.undoManagerComplex,
//            ClientSide.packetSenderClient);
//
//    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedyOrb, simpleComplexOrb);
//
//    SpeedyToolSceptre speedyToolSceptre = new SpeedyToolSceptre(RegistryForItems.itemSpeedySceptre,
//            ClientSide.speedyToolRenderers,
//            ClientSide.speedyToolSounds,
//            ClientSide.undoManagerSimple,
//            ClientSide.packetSenderClient);
//    SpeedyToolComplexSceptre speedyToolComplexSceptre = new SpeedyToolComplexSceptre(RegistryForItems.itemSpeedySceptre,
//            ClientSide.speedyToolRenderers,
//            ClientSide.speedyToolSounds,
//            ClientSide.undoManagerComplex,
//            ClientSide.getCloneToolsNetworkClient(), speedyToolBoundary,
//            ClientSide.clientVoxelSelection, commonSelectionState,
//            ClientSide.selectionPacketSenderComplex,
//            ClientSide.packetSenderClient);
//    SpeedyToolSimpleAndComplex simpleComplexSceptre = new SpeedyToolSimpleAndComplex(speedyToolSceptre, speedyToolComplexSceptre,
//            RegistryForItems.itemSpeedySceptre,
//            ClientSide.speedyToolRenderers,
//            ClientSide.speedyToolSounds,
//            ClientSide.undoManagerComplex,
//            ClientSide.packetSenderClient);
//    ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedySceptre, simpleComplexSceptre);
//
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

//    ClientSide.activeTool.registerToolType(RegistryForItems.itemComplexDelete,
//            new SpeedyToolComplexDelete(RegistryForItems.itemComplexDelete,
//                    ClientSide.speedyToolRenderers,
//                    ClientSide.speedyToolSounds,
//                    ClientSide.undoManagerComplex,
//                    ClientSide.getCloneToolsNetworkClient(), speedyToolBoundary,
//                    ClientSide.clientVoxelSelection, commonSelectionState,
//                    ClientSide.selectionPacketSenderComplex,
//                    ClientSide.packetSenderClient
//            )
//    );
//
//    ClientSide.activeTool.registerToolType(RegistryForItems.itemComplexMove,
//            new SpeedyToolComplexMove(RegistryForItems.itemComplexMove,
//                    ClientSide.speedyToolRenderers,
//                    ClientSide.speedyToolSounds,
//                    ClientSide.undoManagerComplex,
//                    ClientSide.getCloneToolsNetworkClient(), speedyToolBoundary,
//                    ClientSide.clientVoxelSelection, commonSelectionState,
//                    ClientSide.selectionPacketSenderComplex,
//                    ClientSide.packetSenderClient
//            )
//    );
//
    if (SpeedyToolsOptions.getTesterToolsEnabled()) {
      ClientSide.activeTool.registerToolType(RegistryForItems.itemSpeedyTester,
              new SpeedyToolTester(RegistryForItems.itemSpeedyTester,
                      ClientSide.speedyToolRenderers,
                      ClientSide.speedyToolSounds,
                      ClientSide.undoManagerSimple,
                      ClientSide.packetSenderClient
              ));

    }

//    MinecraftForgeClient.registerItemRenderer(RegistryForItems.itemSpeedyOrb, new RendererInventoryItemInfinite(RegistryForItems.itemSpeedyOrb));  todo uncomment
//    MinecraftForgeClient.registerItemRenderer(RegistryForItems.itemSpeedySceptre, new RendererInventoryItemInfinite(RegistryForItems.itemSpeedySceptre));
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

    // register item models for the blocks
    for (String blockName : RegistryForBlocks.getAllItemBlockNames()) {
      Item itemBlockSimple = GameRegistry.findItem("speedytoolsmod", blockName);
      ModelResourceLocation itemModelResourceLocation = new ModelResourceLocation(SpeedyToolsMod.prependModID(blockName), "inventory");
      final int DEFAULT_ITEM_SUBTYPE = 0;
      Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(itemBlockSimple, DEFAULT_ITEM_SUBTYPE, itemModelResourceLocation);
    }

    // register item models for the blocks
    for (String itemName : RegistryForItems.getAllItemNames()) {
      Item itemBlockSimple = GameRegistry.findItem("speedytoolsmod", itemName);
      ModelResourceLocation itemModelResourceLocation = new ModelResourceLocation(SpeedyToolsMod.prependModID(itemName), "inventory");
      if (itemBlockSimple instanceof ItemSpeedyTool) {
        ItemSpeedyTool itemSpeedyTool = (ItemSpeedyTool)itemBlockSimple;
        for (int metadata : itemSpeedyTool.validMetadataValues()) {
          Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(itemBlockSimple, metadata, itemModelResourceLocation);
        }
      } else {
        final int DEFAULT_ITEM_SUBTYPE = 0;
        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(itemBlockSimple, DEFAULT_ITEM_SUBTYPE, itemModelResourceLocation);
      }
    }

    MinecraftForge.EVENT_BUS.register(new ItemEventHandler());
//    MinecraftForge.EVENT_BUS.register(new SoundsRegistry());          todo uncomment
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

  @Override
  public boolean enqueueMessageOnCorrectThread(MessageContext ctx, Runnable messageProcessor) {
    switch (ctx.side) {
      case CLIENT: {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(messageProcessor);
        break;
      }
      case SERVER: {
        NetHandlerPlayServer netHandlerPlayServer = ctx.getServerHandler();
        EntityPlayerMP entityPlayerMP = netHandlerPlayServer.playerEntity;
        final WorldServer playerWorldServer = entityPlayerMP.getServerForPlayer();
        playerWorldServer.addScheduledTask(messageProcessor);
        break;
      }
      default:
        ErrorLog.defaultLog().debug("Invalid side:" + ctx.side + " in enqueueMessageOnCorrectThread");
    }
    return true;
  }


}