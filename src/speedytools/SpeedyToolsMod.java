package speedytools;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import speedytools.blocks.BlockCollisionCheck;
import speedytools.client.KeyBindingInterceptor;
import speedytools.items.*;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.LanguageRegistry;
import net.minecraft.item.Item;

/**
 * Created with IntelliJ IDEA.
 * Date: 26/08/13
 * Time: 10:00 PM
 * To change this template use File | Settings | File Templates.
 */

@Mod(modid="SpeedyToolsMod", name="Speedy Tools Mod", version="0.0.1")
@NetworkMod(clientSideRequired=true, serverSideRequired=false)

public class SpeedyToolsMod {

  // The instance of your mod that Forge uses.
  @Mod.Instance("SpeedyToolsMod")
  public static speedytools.SpeedyToolsMod instance;

  // custom items
  private final static int STARTITEM = 5000;
  public final static Item itemSpeedyStrip = new ItemSpeedyStrip(STARTITEM);

  // custom blocks
  private final static int STARTBLOCK = 500;
  public final static Block blockCollisionCheck = new BlockCollisionCheck(STARTBLOCK);

  public static KeyBindingInterceptor attackButtonInterceptor;

  // custom itemrenderers

  // Says where the client and server 'proxy' code is loaded.
  @SidedProxy(clientSide="speedytools.client.ClientProxy", serverSide="speedytools.CommonProxy")
  public static CommonProxy proxy;

  @EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    // Stub Method
  }

  @EventHandler
  public void load(FMLInitializationEvent event) {
    addItemsToRegistries();
    addBlocksToRegistries();
    MinecraftForge.EVENT_BUS.register(new ItemEventHandler());
  }

  @EventHandler
  public void postInit(FMLPostInitializationEvent event) {
    attackButtonInterceptor = new KeyBindingInterceptor(Minecraft.getMinecraft().gameSettings.keyBindAttack);
    Minecraft.getMinecraft().gameSettings.keyBindAttack = attackButtonInterceptor;
    attackButtonInterceptor.setInterceptionActive(false);
  }

  private void addItemsToRegistries() {
    // for all items:
    // LanguageRegistry for registering the name of the item
    // MinecraftForgeClient.registerItemRenderer for custom item renderers

    LanguageRegistry.addName(itemSpeedyStrip, "Speedy Strip");

  }

  private void addBlocksToRegistries() {
    // for all blocks:
    // GameRegistry for associating an item with a block
    // LanguageRegistry for registering the name of the block
    // RenderingRegistry for custom block renderers

  }

}
