package speedytools.clientside;

import cpw.mods.fml.common.network.PacketDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import speedytools.clientside.rendering.*;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.items.ItemSpeedyTool;
import speedytools.common.network.Packet250SpeedyToolUse;

import javax.swing.undo.UndoManager;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * User: The Grey Ghost
 * Date: 14/04/14
 */
public class SpeedyToolWandStrong
{
  public SpeedyToolWandStrong(Item i_parentItem, SpeedyToolRenderers i_renderers, SpeedyToolSounds i_speedyToolSounds, UndoManagerClient i_undoManagerClient)
  {
    parentItem = i_parentItem;
    rendererUpdateLink = this.new RendererUpdateLink();
    speedyToolRenderers = i_renderers;
    speedyToolSounds = i_speedyToolSounds;
    undoManagerClient = i_undoManagerClient;
    iAmActive = false;
  }

  /** The user is now holding this tool, prepare it
   * @return
   */
  public boolean activateTool()
  {
    LinkedList<RendererElement> rendererElements = new LinkedList<RendererElement>();
    rendererElements.add(new RendererWireframeSelection(rendererUpdateLink));
    speedyToolRenderers.setRenderers(rendererElements);
    iAmActive = true;
    return true;
  }

  /** The user has unequipped this tool, deactivate it, stop any effects, etc
   * @return
   */
  public boolean deactivateTool()
  {
    speedyToolRenderers.setRenderers(null);
    currentlySelectedBlocks = null;
    iAmActive = false;
    return true;
  }

  /**
   * Process user input
   * no effect if the tool is not active.
   * @param userInput
   * @return
   */
  public boolean processUserInput(EntityClientPlayerMP player, float partialTick, UserInput userInput) {
    if (!iAmActive) return false;

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
            speedyToolSounds.playSound(SpeedySoundTypes.STRONGWAND_PLACE, player.getPosition(partialTick));
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

  private boolean sendPlaceCommand()
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

  private boolean sendUndoCommand()
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
    ItemSpeedyTool itemSpeedyTool = (ItemSpeedyTool)currentItem.getItem();

    // the block to be placed is the one to the left of the tool in the hotbar
    int currentlySelectedHotbarSlot = player.inventory.currentItem;

    ItemStack itemStackToPlace = (currentlySelectedHotbarSlot == 0) ? null : player.inventory.getStackInSlot(currentlySelectedHotbarSlot-1);
    currentBlockToPlace = ItemSpeedyTool.getPlacedBlockFromItemStack(itemStackToPlace);

    MovingObjectPosition target = itemSpeedyTool.rayTraceLineOfSight(player.worldObj, player);
    currentlySelectedBlocks = itemSpeedyTool.selectBlocks(target, player, currentItem, itemStackToPlace, partialTick);
    return true;
  }

  /**
   * This class is used to provide information to the Renderer when it needs it:
   * The Renderer calls refreshRenderInfo, which copies the relevant information from the tool.
   */
  public class RendererUpdateLink implements RendererWireframeSelection.WireframeRenderInfoUpdateLink
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
      if (successfulSend) speedyToolSounds.playSound(SpeedySoundTypes.STRONGWAND_UNPLACE, playerPosition);
      return successfulSend;
    }

  }

  private boolean iAmActive;
  private RendererUpdateLink rendererUpdateLink;
  private SpeedyToolRenderers speedyToolRenderers;
  private SpeedyToolSounds speedyToolSounds;
  private UndoManagerClient undoManagerClient;

  private List<ChunkCoordinates> currentlySelectedBlocks;
  private BlockWithMetadata currentBlockToPlace;
  private Item parentItem;
}
