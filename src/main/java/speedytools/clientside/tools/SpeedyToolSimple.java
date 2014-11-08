package speedytools.clientside.tools;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.network.PacketSenderClient;
import speedytools.clientside.rendering.RendererElement;
import speedytools.clientside.rendering.RendererHotbarCurrentItem;
import speedytools.clientside.rendering.RendererWireframeSelection;
import speedytools.clientside.rendering.SpeedyToolRenderers;
import speedytools.clientside.selections.BlockMultiSelector;
import speedytools.clientside.sound.SoundController;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.items.ItemSpeedyTool;
import speedytools.common.network.Packet250SpeedyToolUse;
import speedytools.common.utilities.Pair;
import speedytools.common.utilities.UsefulConstants;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
* User: The Grey Ghost
* Date: 18/04/2014
*/
public abstract class SpeedyToolSimple extends SpeedyTool
{
  public SpeedyToolSimple(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SoundController i_speedyToolSounds,
                          UndoManagerClient i_undoManagerClient, PacketSenderClient i_packetSenderClient)
  {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient, i_packetSenderClient);
    wireframeRendererUpdateLink = this.new SimpleWireframeRendererLink();
    hotbarRenderInfoUpdateLink = this.new HotbarRenderInfoUpdateLink();
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
          if (currentToolItemStack != null) {
            int newCount = parentItem.getPlacementCount(currentToolItemStack) + nextEvent.count;
            parentItem.setPlacementCount(currentToolItemStack, newCount);
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
  public boolean updateForThisFrame(World world, EntityClientPlayerMP player, float partialTick)
  {
    if (!iAmActive) return false;
//    ItemStack currentItem = player.inventory.getCurrentItem();

    if (currentToolItemStack == null) return false;                      // can be null if the user has just moved the active tool out of hotbar
    parentItem.revalidatePlacementCount(currentToolItemStack);
    int maxSelectionSize = currentToolItemStack.stackSize;

    // the block to be placed is the one to the right of the tool in the hotbar
    int currentlySelectedHotbarSlot = player.inventory.currentItem;

    final int MAX_HOTBAR_SLOT = 8;
    ItemStack itemStackToPlace = (currentlySelectedHotbarSlot == MAX_HOTBAR_SLOT) ? null : player.inventory.getStackInSlot(currentlySelectedHotbarSlot + 1);
    currentBlockToPlace = getPlacedBlockFromItemStack(itemStackToPlace);

    MovingObjectPosition target = parentItem.rayTraceLineOfSight(player.worldObj, player);
    Pair<List<ChunkCoordinates>, Integer> retval = selectBlocks(target, player, maxSelectionSize, itemStackToPlace, partialTick);
    currentlySelectedBlocks = retval.getFirst();
    currentSideToBePlaced = retval.getSecond();
    return true;
  }

  /** The user is now holding this tool, prepare it
   * @return
   * @param newToolItemStack
   */
  @Override
  public boolean activateTool(ItemStack newToolItemStack)
  {
    currentToolItemStack = newToolItemStack;
    LinkedList<RendererElement> rendererElements = new LinkedList<RendererElement>();
    rendererElements.add(new RendererWireframeSelection(wireframeRendererUpdateLink));
    rendererElements.add(new RendererHotbarCurrentItem(hotbarRenderInfoUpdateLink));
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

  @Override
  public void resetTool()
  {
    // nothing - no state information stored
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

  /**
   * This class is used to provide information to the Boundary Field Renderer when it needs it.
   * The information is taken from the reference to the SpeedyToolBoundary.
   */
  public class HotbarRenderInfoUpdateLink implements RendererHotbarCurrentItem.HotbarRenderInfoUpdateLink
  {
    @Override
    public boolean refreshRenderInfo(RendererHotbarCurrentItem.HotbarRenderInfo infoToUpdate, ItemStack currentlyHeldItem) {
      if (currentlyHeldItem == null || !(currentlyHeldItem.getItem() instanceof ItemSpeedyTool)) {
        return false;
      }
      ItemSpeedyTool itemSpeedyTool = (ItemSpeedyTool) currentlyHeldItem.getItem();
      return itemSpeedyTool.usesAdjacentBlockInHotbar();
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
  protected Pair<List<ChunkCoordinates>, Integer>  selectLineOfBlocks(MovingObjectPosition target, EntityPlayer player, int maxSelectionSize,
                                                      BlockMultiSelector.CollisionOptions stopWhenCollide, float partialTick)
  {
    MovingObjectPosition startBlock = BlockMultiSelector.selectStartingBlock(target, BlockMultiSelector.BlockTypeToSelect.NON_SOLID_OK, player, partialTick);
    if (startBlock == null) return new Pair<List<ChunkCoordinates>, Integer>(new ArrayList<ChunkCoordinates>(), UsefulConstants.FACE_YPOS);

    ChunkCoordinates startBlockCoordinates = new ChunkCoordinates(startBlock.blockX, startBlock.blockY, startBlock.blockZ);
    boolean diagonalOK =  controlKeyIsDown;
    List<ChunkCoordinates> selection = BlockMultiSelector.selectLine(startBlockCoordinates, player.worldObj, startBlock.hitVec,
            maxSelectionSize, diagonalOK, stopWhenCollide);
    return new Pair<List<ChunkCoordinates>, Integer> (selection, startBlock.sideHit);
  }

  protected boolean sendPlaceCommand()
  {
    if (currentlySelectedBlocks == null || currentlySelectedBlocks.isEmpty()) return false;

    final int RIGHT_BUTTON = 1;
    Packet250SpeedyToolUse packet = new Packet250SpeedyToolUse(RIGHT_BUTTON, currentBlockToPlace, currentSideToBePlaced, currentlySelectedBlocks);
    packetSenderClient.sendPacket(packet);
    return true;
  }

  protected boolean sendUndoCommand()
  {
    final int LEFT_BUTTON = 0;
    final int DUMMY_SIDE = 0;
    Packet250SpeedyToolUse packet = new Packet250SpeedyToolUse(LEFT_BUTTON, currentBlockToPlace, DUMMY_SIDE, currentlySelectedBlocks);
    packetSenderClient.sendPacket(packet);
    return true;
  }

  protected abstract void playPlacementSound(Vec3 playerPosition);

  protected abstract void playUndoSound(Vec3 playerPosition);

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
  protected Pair<List<ChunkCoordinates>, Integer> selectBlocks(MovingObjectPosition target, EntityPlayer player, int maxSelectionSize, ItemStack itemStackToPlace, float partialTick)
  {
    ArrayList<ChunkCoordinates> retval = new ArrayList<ChunkCoordinates>();
    MovingObjectPosition startBlock = BlockMultiSelector.selectStartingBlock(target, BlockMultiSelector.BlockTypeToSelect.SOLID_OK, player, partialTick);
    int sideToPlace = UsefulConstants.FACE_YPOS;
    if (startBlock != null) {
      ChunkCoordinates startBlockCoordinates = new ChunkCoordinates(startBlock.blockX, startBlock.blockY, startBlock.blockZ);
      retval.add(startBlockCoordinates);
      sideToPlace = startBlock.sideHit;
    }

    return new Pair<List<ChunkCoordinates>, Integer> (retval, sideToPlace);
  }

  protected List<ChunkCoordinates> currentlySelectedBlocks = new LinkedList<ChunkCoordinates>();
  protected BlockWithMetadata currentBlockToPlace;
  protected int currentSideToBePlaced;
  private RendererHotbarCurrentItem.HotbarRenderInfoUpdateLink hotbarRenderInfoUpdateLink;
}
