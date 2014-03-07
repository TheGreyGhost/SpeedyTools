package speedytools.common.items;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import speedytools.clientside.selections.BlockMultiSelector;
import speedytools.clientside.rendering.SelectionBoxRenderer;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.network.Packet250SpeedyToolUse;

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
    return (   itemID == RegistryForItems.itemSpeedyWandStrong.itemID
            || itemID == RegistryForItems.itemSpeedyWandWeak.itemID
            || itemID == RegistryForItems.itemSpeedySceptre.itemID
            || itemID == RegistryForItems.itemSpeedyOrb.itemID);
  }

  /**
   * Finds the first block in the player's line of sight, including liquids
   * @param world
   * @param entityPlayer
   * @return the corresponding MovingObjectPosition
   */
  @SideOnly(Side.CLIENT)
  public MovingObjectPosition rayTraceLineOfSight(World world, EntityPlayer entityPlayer)
  {
    return this.getMovingObjectPositionFromPlayer(world, entityPlayer, true);
  }

  /**
   * Selects the Blocks that will be affected by the tool when the player presses right-click
   *   default method just selects the first block.
   * @param target the position of the cursor
   * @param player the player
   * @param currentItem the current item that the player is holding.  MUST be derived from ItemSpeedyTool.
   * @param itemStackToPlace the item that would be placed in the selection
   * @param partialTick partial tick time.
   * @return returns the list of blocks in the selection (may be zero length)
   */
  @SideOnly(Side.CLIENT)
  public List<ChunkCoordinates> selectBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, ItemStack itemStackToPlace, float partialTick)
  {
    ArrayList<ChunkCoordinates> retval = new ArrayList<ChunkCoordinates>();
    MovingObjectPosition startBlock = BlockMultiSelector.selectStartingBlock(target, player, partialTick);
    if (startBlock != null) {
      ChunkCoordinates startBlockCoordinates = new ChunkCoordinates(startBlock.blockX, startBlock.blockY, startBlock.blockZ);
      retval.add(startBlockCoordinates);
    }
    return retval;
  }

  public final int SELECTION_BOX_STYLE = 0; //0 = cube, 1 = cube with cross on each side

  public void renderSelection(EntityPlayer player, float partialTick)
  {
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
    GL11.glLineWidth(2.0F);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDepthMask(false);
    double expandDistance = 0.002F;

    double playerOriginX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTick;
    double playerOriginY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTick;
    double playerOriginZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTick;

    AxisAlignedBB boundingBox = AxisAlignedBB.getAABBPool().getAABB(0, 0, 0, 0, 0, 0);
    for (ChunkCoordinates block : currentlySelectedBlocks) {
      boundingBox.setBounds(block.posX, block.posY, block.posZ, block.posX+1, block.posY+1, block.posZ+1);
      boundingBox = boundingBox.expand(expandDistance, expandDistance, expandDistance).getOffsetBoundingBox(-playerOriginX, -playerOriginY, -playerOriginZ);
      switch (SELECTION_BOX_STYLE) {
        case 0: {
          SelectionBoxRenderer.drawCube(boundingBox);
          break;
        }
        case 1: {
          SelectionBoxRenderer.drawFilledCube(boundingBox);
          break;
        }
      }
    }

    GL11.glDepthMask(true);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
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
  @SideOnly(Side.CLIENT)
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
   * Selects the contour of Blocks that will be affected by the tool when the player presses right-click
   * Starting from the block identified by mouseTarget, the selection will attempt to follow any contours in the same plane as the side hit.
   * (for example: if there is a zigzagging wall, it will select the layer of blocks that follows the top of the wall.)
   * Depending on additiveContour, it will either select the non-solid blocks on top of the contour (to make the wall "taller"), or
   *   select the solid blocks that form the top layer of the contour (to remove the top layer of the wall).
   * @param target the position of the cursor
   * @param player the player
   * @param currentItem the current item that the player is holding.  MUST be derived from ItemSpeedyTool.
   * @param additiveContour if true, selects the layer of non-solid blocks adjacent to the contour.  if false, selects the solid blocks in the contour itself
   * @param partialTick partial tick time.
   * @return returns the list of blocks in the selection (may be zero length)
   */
  @SideOnly(Side.CLIENT)
  protected List<ChunkCoordinates> selectContourBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, boolean additiveContour, float partialTick)
  {
    MovingObjectPosition startBlock = BlockMultiSelector.selectStartingBlock(target, player, partialTick);
    if (startBlock == null) return new ArrayList<ChunkCoordinates>();

    boolean diagonalOK =  Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    int maxSelectionSize = currentItem.stackSize;
    List<ChunkCoordinates> selection = BlockMultiSelector.selectContour(target, player.worldObj, maxSelectionSize, diagonalOK, additiveContour);
    return selection;
  }

  /**
   * Selects the "blob" of blocks that will be affected by the tool when the player presses right-click
   * Starting from the block identified by mouseTarget, the selection will flood fill all matching blocks.
   * @param target  the block to start the flood fill from
   * @param player
   * @param currentItem  the current item that the player is holding.  MUST be derived from ItemSpeedyTool.
   * @param partialTick
   * @return   returns the list of blocks in the selection (may be zero length)
   */
  @SideOnly(Side.CLIENT)
  protected List<ChunkCoordinates> selectFillBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, float partialTick)
  {
    MovingObjectPosition startBlock = BlockMultiSelector.selectStartingBlock(target, player, partialTick);
    if (startBlock == null) return new ArrayList<ChunkCoordinates>();

    boolean diagonalOK =  Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    int maxSelectionSize = currentItem.stackSize;
    List<ChunkCoordinates> selection = BlockMultiSelector.selectFill(target, player.worldObj, maxSelectionSize, diagonalOK);
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
  public void attackButtonClicked(EntityClientPlayerMP thePlayer)
  {
    boolean success = buttonClicked(0);
    if (success && !undoSoundsHistory.isEmpty()) {
      Minecraft.getMinecraft().sndManager.playSound(undoSoundsHistory.getLast().getUnPlaceSound(),
              (float)(thePlayer.posX),
              (float)(thePlayer.posY),
              (float)(thePlayer.posZ),
              1.0F, 1.0F);
      undoSoundsHistory.removeLast();
    }
  }

  /**
   * called when the user presses the use button (right mouse)
   * @param thePlayer
   */
  @SideOnly(Side.CLIENT)
  public void useButtonClicked(EntityClientPlayerMP thePlayer)
  {
    boolean success = buttonClicked(1);

    if (success) {
      ItemSpeedyTool currentTool = (ItemSpeedyTool)currentlySelectedTool;
      undoSoundsHistory.addLast(currentTool);
      if (undoSoundsHistory.size() > MAXIMUM_UNDO_COUNT) undoSoundsHistory.removeFirst();
      Minecraft.getMinecraft().sndManager.playSound(undoSoundsHistory.getLast().getPlaceSound(),
              (float) (thePlayer.posX),
              (float) (thePlayer.posY),
              (float) (thePlayer.posZ),
              1.0F, 1.0F);
    }
  }

  /**
   * called when the user scrolls the mouse wheel.  Changes the stacksize of the currently held item (i.e. the number of blocks it will place)
   * @param delta the delta (see Mouse.getDWheel() )
   */
  @SideOnly(Side.CLIENT)
  public static void mouseWheelMoved(int delta)
  {
    int MOUSE_DELTA_PER_SLOT = 120;

    if (delta != 0) {
      EntityClientPlayerMP entityClientPlayerMP = Minecraft.getMinecraft().thePlayer;
      ItemStack currentItem = entityClientPlayerMP.inventory.getCurrentItem();
      int currentcount = currentItem.stackSize;
      int maxStackSize = currentItem.getMaxStackSize();
      if (currentcount >=1 && currentcount <= maxStackSize) {
        currentcount += delta / MOUSE_DELTA_PER_SLOT;
        currentcount = ((currentcount - 1) % maxStackSize);
        currentcount = ((currentcount + maxStackSize) % maxStackSize) + 1;    // take care of negative

        currentItem.stackSize = currentcount;
      }
    }
  }

  /**
   * Inform the server of the player's action with the SpeedyTool.  Checks to make sure that currentlySelectedTool is valid, and if right click then currentlySelectedBlock has at least one entry.
   * @param whichButton 0 = left (undo), 1 = right (use)
   * @return true if a packet was sent (the action is valid)
   */
  @SideOnly(Side.CLIENT)
  public static boolean buttonClicked(int whichButton)
  {
    if (currentlySelectedTool == null || (whichButton == 1 && currentlySelectedBlocks.isEmpty())) return false;

    Packet250SpeedyToolUse packet;
    try {
      packet = new Packet250SpeedyToolUse(currentlySelectedTool.itemID, whichButton, currentBlockToPlace, currentlySelectedBlocks);
    } catch (IOException e) {
      Minecraft.getMinecraft().getLogAgent().logWarning("Could not create Packet250SpeedyToolUse for itemID " + currentlySelectedTool.itemID);
      return false;
    }
    PacketDispatcher.sendPacketToServer(packet.getPacket250CustomPayload());
    return true;
  }

  protected String getPlaceSound() {return "";}

  protected String getUnPlaceSound() {return "";}

  private static final int MAXIMUM_UNDO_COUNT = 5;  // used for sound effects only

    // these keep track of the currently selected blocks, for when the tool is used
  protected static List<ChunkCoordinates> currentlySelectedBlocks = null;
  protected static Item currentlySelectedTool = null;
  protected static BlockWithMetadata currentBlockToPlace = null;
  protected static Deque<ItemSpeedyTool> undoSoundsHistory = new LinkedList<ItemSpeedyTool>();
}
