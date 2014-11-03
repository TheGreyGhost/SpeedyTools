package speedytools.clientside.tools;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.world.World;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.network.PacketSenderClient;
import speedytools.clientside.rendering.RendererWireframeSelection;
import speedytools.clientside.rendering.SpeedyToolRenderers;
import speedytools.clientside.sound.SoundController;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.items.ItemSpeedyTool;

/**
* User: The Grey Ghost
* Date: 18/04/2014
*/
public abstract class SpeedyTool
{
  public SpeedyTool(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SoundController i_speedyToolSounds,
                    UndoManagerClient i_undoManagerClient, PacketSenderClient i_packetSenderClient) {
    speedyToolRenderers = i_renderers;
    parentItem = i_parentItem;
    iAmActive = false;
    undoManagerClient = i_undoManagerClient;
    soundController = i_speedyToolSounds;
    packetSenderClient = i_packetSenderClient;
  }

  public abstract boolean activateTool(ItemStack newToolItemStack);

  /** The user has unequipped this tool, deactivate it, stop any effects, etc
   * @return
   */
  public abstract boolean deactivateTool();

  /**
   * Process user input
   * no effect if the tool is not active.
   * @param userInput
   * @return
   */
  public abstract boolean processUserInput(EntityClientPlayerMP player, float partialTick, UserInput userInput);

  /**
   * update the tool state based on the player selected items; where the player is looking; etc
   * No effect if not active.
   * @param world
   * @param player
   * @param partialTick
   * @return
   */
  public abstract boolean updateForThisFrame(World world, EntityClientPlayerMP player, float partialTick);

  /**
   *  resets the tool state eg has a selection, moving selection, etc.
   */
  public abstract void resetTool();

  public void performTick(World world) {}

  protected boolean iAmActive;
  protected SpeedyToolRenderers speedyToolRenderers;
  protected SoundController soundController;
  protected UndoManagerClient undoManagerClient;
  protected PacketSenderClient packetSenderClient;
  protected ItemSpeedyTool parentItem;
  protected ItemStack currentToolItemStack;
  protected boolean controlKeyIsDown;
  protected RendererWireframeSelection.WireframeRenderInfoUpdateLink wireframeRendererUpdateLink;

  /**
   * For the given ItemStack, returns the corresponding Block that will be placed by the tool
   *   eg ItemCloth will give the Block cloth
   *   ItemBlocks are converted to the appropriate block
   *   Others:
   * @param itemToBePlaced - the Item to be placed, or null for none.
   * @return the Block (and metadata) corresponding to the item, or null for none.
   */
  protected BlockWithMetadata getPlacedBlockFromItemStack(ItemStack itemToBePlaced)
  {
    assert FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT;

    BlockWithMetadata retval = new BlockWithMetadata();
    if (itemToBePlaced == null) {
      retval.block = Blocks.air;
      retval.metaData = 0;
      return retval;
    }

    Item item = itemToBePlaced.getItem();
    if (item instanceof ItemBlock) {
      ItemBlock itemBlock = (ItemBlock)item;
      retval.block =  Block.getBlockFromItem(itemBlock);
      retval.metaData = itemBlock.getMetadata(itemToBePlaced.getItemDamage());
    } else if (item == Items.water_bucket) {
      retval.block = Blocks.water;
      retval.metaData = 0;
    } else if (item == Items.lava_bucket) {
      retval.block = Blocks.lava;
      retval.metaData = 0;
    } else if (item instanceof ItemSeeds) {
      ItemSeeds itemSeeds = (ItemSeeds)item;
      World world = Minecraft.getMinecraft().theWorld;
      retval.block = itemSeeds.getPlant(world, 0, 0, 0);      // method doesn't actually use x,y,z
      retval.metaData = itemSeeds.getPlantMetadata(world, 0, 0, 0);
    } else if (item instanceof ItemRedstone) {
      retval.block = Blocks.redstone_wire;
      retval.metaData = 0;
    } else  {
      retval.block = Blocks.air;
      retval.metaData = 0;
    }
    return retval;
  }
}
