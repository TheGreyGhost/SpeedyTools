package speedytools.clientonly.eventhandlers;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.ForgeSubscribe;
import org.lwjgl.opengl.GL11;
import speedytools.clientonly.SelectionBoxRenderer;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.items.ItemSpeedyTool;

import java.util.List;

/**
 Contains the custom Forge Event Handlers
 */
public class ItemEventHandler {

  public final int SELECTION_BOX_STYLE = 0; //0 = cube, 1 = cube with cross on each side
  /**
   * If a SpeedyTools item is selected, draw nothing (drawing of selection box is performed in RenderWorldLastEvent).
   * Otherwise, cancel the event so that the normal selection box is drawn.
   * @param event
   */
  @ForgeSubscribe
  public void blockHighlightDecider(DrawBlockHighlightEvent event)
  {
    EntityPlayer player = event.player;
    ItemStack currentItem = player.inventory.getCurrentItem();

    if (currentItem == null || !ItemSpeedyTool.isAspeedyTool(currentItem.getItem().itemID)) {
      return;
    }

    event.setCanceled(true);
    return;
  }

  /**
   * If a speedy tool is equipped, selects the appropriate blocks and stores the selection into SpeedyToolsMod.currentlySelectedBlocks
   *    along with the substrate used by the tool (the block to be placed) which is the block in the hotbar immediately to the left of the tool
   * Also renders the selection over the top of the existing world
   *
   * @param event
   */
  @ForgeSubscribe
  public void drawSelectionBox(RenderWorldLastEvent event)
  {
    RenderGlobal context = event.context;
    assert(context.mc.renderViewEntity instanceof EntityPlayer);
    EntityPlayer player = (EntityPlayer)context.mc.renderViewEntity;
//    MovingObjectPosition target = context.mc.objectMouseOver;

    ItemStack currentItem = player.inventory.getCurrentItem();
    float partialTick = event.partialTicks;

    if (currentItem == null || !ItemSpeedyTool.isAspeedyTool(currentItem.getItem().itemID)) return;
    ItemSpeedyTool itemSpeedyTool = (ItemSpeedyTool)currentItem.getItem();

    MovingObjectPosition target = itemSpeedyTool.rayTraceLineOfSight(player.worldObj, player);

    // the block to be placed is the one to the left of the tool in the hotbar
    int currentlySelectedHotbarSlot = player.inventory.currentItem;
    ItemStack itemStackToPlace = (currentlySelectedHotbarSlot == 0) ? null : player.inventory.getStackInSlot(currentlySelectedHotbarSlot-1);

    List<ChunkCoordinates> selection = itemSpeedyTool.selectBlocks(target, player, currentItem, itemStackToPlace, partialTick);

    BlockWithMetadata blockToPlace = ItemSpeedyTool.getPlacedBlockFromItemStack(itemStackToPlace);

    ItemSpeedyTool.setCurrentToolSelection(currentItem.getItem(), blockToPlace, selection);
    if (selection.isEmpty()) return;

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

    for (ChunkCoordinates block : selection) {
      AxisAlignedBB boundingBox = AxisAlignedBB.getAABBPool().getAABB(block.posX, block.posY, block.posZ,
                                                                      block.posX+1, block.posY+1, block.posZ+1);
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




}
