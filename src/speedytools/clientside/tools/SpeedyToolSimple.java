package speedytools.clientside.tools;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.rendering.RendererElement;
import speedytools.clientside.rendering.RendererWireframeSelection;
import speedytools.clientside.rendering.SpeedyToolRenderers;
import speedytools.clientside.rendering.SpeedyToolSounds;
import speedytools.clientside.selections.BlockMultiSelector;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.items.ItemSpeedyTool;
import speedytools.common.network.Packet250SpeedyToolUse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * User: The Grey Ghost
 * Date: 18/04/2014
 */
public abstract class SpeedyToolSimple extends SpeedyTool
{
  public SpeedyToolSimple(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SpeedyToolSounds i_speedyToolSounds, UndoManagerClient i_undoManagerClient)
  {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient);
    wireframeRendererUpdateLink = this.new SimpleWireframeRendererLink();
  }

  /**
   * Process user input
   * no effect if the tool is not active.
   * @param userInput
   * @return
   */
  public boolean processUserInput(EntityClientPlayerMP player, float partialTick, UserInput userInput) {
    if (!iAmActive) return false;

    controlKeyIsDown = userInput.isControlKeyDown();

    UserInput.InputEvent nextEvent;
    while (null != (nextEvent = userInput.poll())) {
      switch (nextEvent.eventType) {
        case LEFT_CLICK_DOWN: {
          undoManagerClient.performUndo(player.getPosition(partialTick));
          break;
        }
        case RIGHT_CLICK_DOWN: {
          boolean successfulSend = sendPlaceCommand();
          if (successfulSend) {
            undoManagerClient.addUndoableAction(new SpeedyToolUndoCallback());
            playPlacementSound(player.getPosition(partialTick));
          }
          break;
        }
        case WHEEL_MOVE: {
          ItemStack currentItem = player.inventory.getCurrentItem();
          int currentcount = currentItem.stackSize;
          int maxStackSize = currentItem.getMaxStackSize();
          if (currentcount >= 1 && currentcount <= maxStackSize) {
            currentcount += nextEvent.count;
            currentcount = ((currentcount - 1) % maxStackSize);
            currentcount = ((currentcount + maxStackSize) % maxStackSize) + 1;    // take care of negative
            currentItem.stackSize = currentcount;
          }
          break;
        }
      }
    }
    return true;
  }

  /**
   * update the tool state based on the player selected items; where the player is looking; etc
   * No effect if not active.
   * @param world
   * @param player
   * @param partialTick
   * @return
   */
  public boolean update(World world, EntityClientPlayerMP player, float partialTick)
  {
    if (!iAmActive) return false;
    ItemStack currentItem = player.inventory.getCurrentItem();
    int maxSelectionSize = currentItem.stackSize;

    // the block to be placed is the one to the left of the tool in the hotbar
    int currentlySelectedHotbarSlot = player.inventory.currentItem;

    ItemStack itemStackToPlace = (currentlySelectedHotbarSlot == 0) ? null : player.inventory.getStackInSlot(currentlySelectedHotbarSlot-1);
    currentBlockToPlace = getPlacedBlockFromItemStack(itemStackToPlace);

    MovingObjectPosition target = parentItem.rayTraceLineOfSight(player.worldObj, player);
    currentlySelectedBlocks = selectBlocks(target, player, maxSelectionSize, itemStackToPlace, partialTick);
    return true;
  }

  /** The user is now holding this tool, prepare it
   * @return
   */
  @Override
  public boolean activateTool()
  {
    LinkedList<RendererElement> rendererElements = new LinkedList<RendererElement>();
    rendererElements.add(new RendererWireframeSelection(wireframeRendererUpdateLink));
    speedyToolRenderers.setRenderers(rendererElements);
    iAmActive = true;
    return true;
  }

  /** The user has unequipped this tool, deactivate it, stop any effects, etc
   * @return
   */
  @Override
  public boolean deactivateTool()
  {
    speedyToolRenderers.setRenderers(null);
    currentlySelectedBlocks.clear();
    iAmActive = false;
    return true;
  }

  /**
   * This class is used to provide information to the WireFrame Renderer when it needs it:
   * The Renderer calls refreshRenderInfo, which copies the relevant information from the tool.
   */
  public class SimpleWireframeRendererLink implements RendererWireframeSelection.WireframeRenderInfoUpdateLink
  {
    @Override
    public boolean refreshRenderInfo(RendererWireframeSelection.WireframeRenderInfo infoToUpdate)
    {
      infoToUpdate.currentlySelectedBlocks = currentlySelectedBlocks;
      return true;
    }
  }

  public class SpeedyToolUndoCallback implements UndoManagerClient.UndoCallback
  {
    @Override
    public boolean performUndo(Vec3 playerPosition)
    {
      boolean successfulSend = sendUndoCommand();
      if (successfulSend) playUndoSound(playerPosition);
      return successfulSend;
    }
  }

  /**
   * Selects the a straight line of Blocks that will be affected by the tool when the player presses right-click
   * @param target the position of the cursor
   * @param player the player
   * @param maxSelectionSize the maximum number of blocks in the selection
   * @param stopWhenCollide if true,  stop when a "solid" block such as stone is encountered.  "non-solid" is blocks such as air, grass, etc
   * @param partialTick partial tick time.
   * @return returns the list of blocks in the selection (may be zero length)
   */
  protected List<ChunkCoordinates> selectLineOfBlocks(MovingObjectPosition target, EntityPlayer player, int maxSelectionSize,
                                                      BlockMultiSelector.CollisionOptions stopWhenCollide, float partialTick)
  {
    MovingObjectPosition startBlock = BlockMultiSelector.selectStartingBlock(target, player, partialTick);
    if (startBlock == null) return new ArrayList<ChunkCoordinates>();

    ChunkCoordinates startBlockCoordinates = new ChunkCoordinates(startBlock.blockX, startBlock.blockY, startBlock.blockZ);
    boolean diagonalOK =  controlKeyIsDown;
    List<ChunkCoordinates> selection = BlockMultiSelector.selectLine(startBlockCoordinates, player.worldObj, startBlock.hitVec,
            maxSelectionSize, diagonalOK, stopWhenCollide);
    return selection;
  }

  protected boolean sendPlaceCommand()
  {
    if (currentlySelectedBlocks.isEmpty()) return false;

    Packet250SpeedyToolUse packet;
    try {
      final int RIGHT_BUTTON = 1;
      packet = new Packet250SpeedyToolUse(parentItem.itemID, RIGHT_BUTTON, currentBlockToPlace, currentlySelectedBlocks);
    } catch (IOException e) {
      Minecraft.getMinecraft().getLogAgent().logWarning("Could not create Packet250SpeedyToolUse for itemID " + parentItem.itemID);
      return false;
    }
    PacketDispatcher.sendPacketToServer(packet.getPacket250CustomPayload());
    return true;
  }

  protected boolean sendUndoCommand()
  {
    Packet250SpeedyToolUse packet;
    try {
      final int LEFT_BUTTON = 0;
      packet = new Packet250SpeedyToolUse(parentItem.itemID, LEFT_BUTTON, currentBlockToPlace, currentlySelectedBlocks);
    } catch (IOException e) {
      Minecraft.getMinecraft().getLogAgent().logWarning("Could not create Packet250SpeedyToolUse for itemID " + parentItem.itemID);
      return false;
    }
    PacketDispatcher.sendPacketToServer(packet.getPacket250CustomPayload());
    return true;
  }

  protected abstract void playPlacementSound(Vec3 playerPosition);

  protected abstract void playUndoSound(Vec3 playerPosition);

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
   * Selects the Blocks that will be affected by the tool when the player presses right-click
   *   default method just selects the first block.
   * @param target the position of the cursor
   * @param player the player
   * @param maxSelectionSize the maximum number of blocks in the selection
   * @param itemStackToPlace the item that would be placed in the selection
   * @param partialTick partial tick time.
   * @return returns the list of blocks in the selection (may be zero length)
   */
  protected List<ChunkCoordinates> selectBlocks(MovingObjectPosition target, EntityPlayer player, int maxSelectionSize, ItemStack itemStackToPlace, float partialTick)
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
   * Selects the contour of Blocks that will be affected by the tool when the player presses right-click
   * Starting from the block identified by mouseTarget, the selection will attempt to follow any contours in the same plane as the side hit.
   * (for example: if there is a zigzagging wall, it will select the layer of blocks that follows the top of the wall.)
   * Depending on additiveContour, it will either select the non-solid blocks on top of the contour (to make the wall "taller"), or
   *   select the solid blocks that form the top layer of the contour (to remove the top layer of the wall).
   * @param target the position of the cursor
   * @param player the player
   * @param maxSelectionSize the maximum number of blocks in the selection
   * @param additiveContour if true, selects the layer of non-solid blocks adjacent to the contour.  if false, selects the solid blocks in the contour itself
   * @param partialTick partial tick time.
   * @return returns the list of blocks in the selection (may be zero length)
   */
  protected List<ChunkCoordinates> selectContourBlocks(MovingObjectPosition target, EntityPlayer player, int maxSelectionSize, boolean additiveContour, float partialTick)
  {
    MovingObjectPosition startBlock = BlockMultiSelector.selectStartingBlock(target, player, partialTick);
    if (startBlock == null) return new ArrayList<ChunkCoordinates>();

    boolean diagonalOK =  controlKeyIsDown;
    List<ChunkCoordinates> selection = BlockMultiSelector.selectContour(target, player.worldObj, maxSelectionSize, diagonalOK, additiveContour);
    return selection;
  }

  /**
   * Selects the "blob" of blocks that will be affected by the tool when the player presses right-click
   * Starting from the block identified by mouseTarget, the selection will flood fill all matching blocks.
   * @param target  the block to start the flood fill from
   * @param player
   * @param maxSelectionSize the maximum number of blocks in the selection
   * @param partialTick
   * @return   returns the list of blocks in the selection (may be zero length)
   */
  protected List<ChunkCoordinates> selectFillBlocks(MovingObjectPosition target, EntityPlayer player, int maxSelectionSize, float partialTick)
  {
    MovingObjectPosition startBlock = BlockMultiSelector.selectStartingBlock(target, player, partialTick);
    if (startBlock == null) return new ArrayList<ChunkCoordinates>();

    boolean diagonalOK =  controlKeyIsDown;
    List<ChunkCoordinates> selection = BlockMultiSelector.selectFill(target, player.worldObj, maxSelectionSize, diagonalOK);
    return selection;
  }
  protected List<ChunkCoordinates> currentlySelectedBlocks;
  protected BlockWithMetadata currentBlockToPlace;

}
