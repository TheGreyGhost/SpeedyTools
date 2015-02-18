package speedytools.clientside;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import speedytools.clientside.rendering.SpeedyToolRenderers;
import speedytools.clientside.sound.SoundController;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.items.RegistryForItems;

//import speedytools.clientside.network.CloneToolsNetworkClient;
//import speedytools.clientside.network.PacketSenderClient;
//import speedytools.clientside.rendering.SpeedyToolRenderers;
//import speedytools.clientside.sound.SpeedyToolSounds;

/**
 * User: The Grey Ghost
 * Date: 10/03/14
 * Contains the various objects that define the client side
 */
public class ClientSide
{
  public static void preInitialise()
  {
    tabSpeedyTools = new CreativeTabs("tabSpeedyTools") {
      @Override
      public ItemStack getIconItemStack() {
        return new ItemStack(RegistryForItems.itemSpeedySceptre, 1, 0);
      }
      @Override
      public Item getTabIconItem() {return RegistryForItems.itemSpeedySceptre;}
    };
    activeTool = new ActiveTool();
  }

  public static void load()
  {
/*  todo uncomment
    packetHandlerRegistry = new PacketHandlerRegistryClient();
    packetSenderClient = new PacketSenderClient(packetHandlerRegistry);
    cloneToolsNetworkClient = new CloneToolsNetworkClient(packetHandlerRegistry, packetSenderClient);
    speedyToolRenderers = new SpeedyToolRenderers();
    speedyToolSounds = new SoundController();
    undoManagerSimple = new UndoManagerClient(SpeedyToolsOptions.getMaxSimpleToolUndoCount());
    undoManagerComplex = new UndoManagerClient(SpeedyToolsOptions.getMaxComplexToolUndoCount());
    selectionPacketSenderComplex = new SelectionPacketSender(packetHandlerRegistry, packetSenderClient);
    clientVoxelSelection = new ClientVoxelSelection(packetHandlerRegistry, selectionPacketSenderComplex, packetSenderClient);
*/
  }

  public static void postInitialise()
  {
    userInput = new UserInput();

    String NETWORK_LOG_FILENAME_STEM = "NetworkMonitor";
//    if (SpeedyToolsOptions.getNetworkLoggingActive()) {
//      try {
//        networkTrafficMonitor = new NetworkTrafficMonitor(Side.CLIENT, SpeedyToolsOptions.getNetworkLoggingDirectory().toPath(), NETWORK_LOG_FILENAME_STEM);
//      } catch (IOException ioe) {
//        ErrorLog.defaultLog().warning("Couldn't create a NetworkTrafficMonitor because:" + ioe);
//        networkTrafficMonitor = new NetworkTrafficMonitor.NetworkTrafficMonitorNULL();
//      }
//    } else {
//      networkTrafficMonitor = new NetworkTrafficMonitor.NetworkTrafficMonitorNULL();
//    }


  }

/*
  public static void shutdown()
  {
    cloneToolsNetworkClient = null;
  }
*/

//  public static CloneToolsNetworkClient getCloneToolsNetworkClient() {  todo uncomment
//    return cloneToolsNetworkClient;
//  }
//  public static NetworkTrafficMonitor getNetworkTrafficMonitor() {
//    return networkTrafficMonitor;
//  }

//  public static CloneToolsNetworkClient cloneToolsNetworkClient;
  public static SpeedyToolRenderers speedyToolRenderers;
  public static ActiveTool activeTool;
  public static UserInput userInput;
  public static UndoManagerClient undoManagerSimple;
  public static UndoManagerClient undoManagerComplex;
  public static SoundController speedyToolSounds;
//  public static PacketSenderClient packetSenderClient;
//  public static PacketHandlerRegistryClient packetHandlerRegistry;
//  public static SelectionPacketSender selectionPacketSenderComplex;
//  public static ClientVoxelSelection clientVoxelSelection;               todo uncomment

  public static CreativeTabs tabSpeedyTools;

  public static int getGlobalTickCount() {
    return globalTickCount;
  }

  public static void tick()
  {
    ++globalTickCount;
//    getCloneToolsNetworkClient().tick();

//    if (globalTickCount % SpeedyToolsOptions.getNetworkLoggingPeriodInTicks() == 0) {
//      try {
//        ClientSide.getNetworkTrafficMonitor().log();
//      } catch (IOException ioe) {
//        ErrorLog.defaultLog().warning("Failed to log network traffic due to:" + ioe);
//      }
//    }
  }

  private static int globalTickCount = 0;


//  private static NetworkTrafficMonitor networkTrafficMonitor;
}
