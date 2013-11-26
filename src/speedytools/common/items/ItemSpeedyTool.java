package speedytools.common.items;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import speedytools.common.blocks.BlockMultiSelector;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.clientserversynch.Packet250SpeedyToolUse;

import java.io.IOException;
import java.util.*;

/**
 * User: The Grey Ghost
 * Date: 2/11/13
 */
public abstract class ItemSpeedyTool extends Item
{
  public ItemSpeedyTool(int id) {
    super(id);
    setCreativeTab(CreativeTabs.tabTools);
    setMaxDamage(-1);                         // not damageable
  }

  public static boolean isAspeedyTool(int itemID)
  {
    return (   itemID == RegistryForItems.itemSpeedyStripStrong.itemID
            || itemID == RegistryForItems.itemSpeedyStripWeak.itemID    );
  }

  /**
   * Selects the Blocks that will be affected by the tool when the player presses right-click
   *   default method just selects the first block.
   * @param target the position of the cursor
   * @param player the player
   * @param currentItem the current item that the player is holding.  MUST be derived from ItemSpeedyTool.
   * @param partialTick partial tick time.
   * @return returns the list of blocks in the selection (may be zero length)
   */
  public List<ChunkCoordinates> selectBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, float partialTick)
  {
    ArrayList<ChunkCoordinates> retval = new ArrayList<ChunkCoordinates>();
    MovingObjectPosition startBlock = BlockMultiSelector.selectStartingBlock(target, player, partialTick);
    if (startBlock != null) {
      ChunkCoordinates startBlockCoordinates = new ChunkCoordinates(startBlock.blockX, startBlock.blockY, startBlock.blockZ);
      retval.add(startBlockCoordinates);
    }
    return retval;
  }

  /**
  * Selects the a straight line of Blocks that will be affected by the tool when the player presses right-click
  * @param target the position of the cursor
  * @param player the player
  * @param currentItem the current item that the player is holding.  MUST be derived from ItemSpeedyTool.
  * @param stopWhenCollide if true,  stop when a "solid" block such as stone is encountered.  "non-solid" is blocks such as air, grass, etc
  * @param partialTick partial tick time.
  * @return returns the list of blocks in the selection (may be zero length)
  */
  protected List<ChunkCoordinates> selectLineOfBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, boolean stopWhenCollide, float partialTick)
  {
    MovingObjectPosition startBlock = BlockMultiSelector.selectStartingBlock(target, player, partialTick);
    if (startBlock == null) return new ArrayList<ChunkCoordinates>();

    ChunkCoordinates startBlockCoordinates = new ChunkCoordinates(startBlock.blockX, startBlock.blockY, startBlock.blockZ);
    boolean diagonalOK =  Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    int maxSelectionSize = currentItem.stackSize;
    List<ChunkCoordinates> selection = BlockMultiSelector.selectLine(startBlockCoordinates, player.worldObj, startBlock.hitVec,
                                                                     maxSelectionSize, diagonalOK, stopWhenCollide);
    return selection;
  }

  /**
   * For the given ItemStack, returns the corresponding Block that will be placed by the tool
   *   eg ItemCloth will give the Block cloth
   *   ItemBlocks are converted to the appropriate block
   *   Others:
   * @param itemToBePlaced - the Item to be placed, or null for none.
   * @return the Block (and metadata) corresponding to the item, or null for none.
   */
  @SideOnly(Side.CLIENT)
  public static BlockWithMetadata getPlacedBlockFromItemStack(ItemStack itemToBePlaced)
  {
    assert FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT;

    if (itemToBePlaced == null) return null;
    BlockWithMetadata retval = new BlockWithMetadata();

    Item item = itemToBePlaced.getItem();
    if (item instanceof ItemBlock) {
      ItemBlock itemBlock = (ItemBlock)item;
      retval.block = Block.blocksList[itemBlock.getBlockID()];
      retval.metaData = itemBlock.getMetadata(itemToBePlaced.getItemDamage());
    } else if (item.itemID == Item.bucketWater.itemID) {
      retval.block = Block.waterStill;
      retval.metaData = 0;
    } else if (item.itemID == Item.bucketLava.itemID) {
      retval.block = Block.lavaStill;
      retval.metaData = 0;
    } else if (item instanceof ItemSeeds) {
      ItemSeeds itemSeeds = (ItemSeeds)item;
      World world = Minecraft.getMinecraft().theWorld;
      retval.block = Block.blocksList[itemSeeds.getPlantID(world, 0, 0, 0)];      // method doesn't actually use x,y,z
      retval.metaData = itemSeeds.getPlantMetadata(world, 0, 0, 0);
    } else if (item instanceof ItemRedstone) {
      retval.block = Block.redstoneWire;
      retval.metaData = 0;
    } else  {
      retval = null;
    }
    return retval;

  }


  /**
   * Sets the current multiple-block selection for the currently-held speedy tool
   * @param currentTool the currently-held speedy tool
   * @param setCurrentBlockToPlace the Block to be used for filling the selection
   * @param currentSelection list of coordinates of blocks in the current selection, or null if none
   */

  @SideOnly(Side.CLIENT)
  public static void setCurrentToolSelection(Item currentTool, BlockWithMetadata setCurrentBlockToPlace, List<ChunkCoordinates> currentSelection)
  {
    currentlySelectedTool = currentTool;
    currentlySelectedBlocks = currentSelection;
    currentBlockToPlace = setCurrentBlockToPlace;
  }

  /**
   * called when the user presses the attackButton (Left Mouse)
   */
  @SideOnly(Side.CLIENT)
  public static void attackButtonClicked()
  {
    buttonClicked(0);
  }

  /**
   * called when the user presses the use button (right mouse)
   */
  @SideOnly(Side.CLIENT)
  public static void useButtonClicked()
  {
     buttonClicked(1);
  }

  @SideOnly(Side.CLIENT)
  public static void buttonClicked(int buttonClicked)
  {
    if (currentlySelectedTool == null) return;

    Packet250SpeedyToolUse packet = null;
    try {
      packet = new Packet250SpeedyToolUse(currentlySelectedTool.itemID, buttonClicked, currentBlockToPlace, currentlySelectedBlocks);
    } catch (IOException e) {
      Minecraft.getMinecraft().getLogAgent().logWarning("Could not create Packet250SpeedyToolUse for itemID " + currentlySelectedTool.itemID);
      return;
    }
    PacketDispatcher.sendPacketToServer(packet);
  }

    // these keep track of the currently selected blocks, for when the tool is used
  @SideOnly(Side.CLIENT)
  protected static List<ChunkCoordinates> currentlySelectedBlocks = null;
  @SideOnly(Side.CLIENT)
  protected static Item currentlySelectedTool = null;
  protected static BlockWithMetadata currentBlockToPlace = null;


}
