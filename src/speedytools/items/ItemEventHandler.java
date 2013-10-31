package speedytools.items;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.ForgeSubscribe;
import org.lwjgl.opengl.GL11;
import speedytools.SpeedyToolsMod;

/**
 Contains the custom Forge Event Handlers
 */
public class ItemEventHandler {

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

    if (currentItem == null || currentItem.getItem().itemID != SpeedyToolsMod.itemSpeedyStrip.itemID) {
      return;
    }

    event.setCanceled(true);
    return;
  }


  @ForgeSubscribe
  public void drawSelectionBox(RenderWorldLastEvent event)
  {
    RenderGlobal context = event.context;
    assert(context.mc.renderViewEntity instanceof EntityPlayer);
    EntityPlayer player = (EntityPlayer)context.mc.renderViewEntity;
    MovingObjectPosition target = context.mc.objectMouseOver;
    ItemStack currentItem = player.inventory.getCurrentItem();
    float partialTick = event.partialTicks;

    if (currentItem == null || currentItem.getItem().itemID != SpeedyToolsMod.itemSpeedyStrip.itemID) return;

    double playerOriginX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTick;
    double playerOriginY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTick;
    double playerOriginZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTick;

    Vec3 playerLook = player.getLook(partialTick);

    ChunkCoordinates startBlock = BlockMultiSelector.selectStartingBlock(target, player, partialTick);

    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
    GL11.glLineWidth(2.0F);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDepthMask(false);
    double expandDistance = 0.002F;
//    int j = context.theWorld.getBlockId(target.blockX, target.blockY, target.blockZ);


//      Block.blocksList[j].setBlockBoundsBasedOnState(context.theWorld, target.blockX, target.blockY, target.blockZ);
      AxisAlignedBB boundingBox = AxisAlignedBB.getAABBPool().getAABB(startBlock.posX, startBlock.posY, startBlock.posZ,
                                                                      startBlock.posX+1, startBlock.posY+1, startBlock.posZ+1);
      boundingBox = boundingBox.expand(expandDistance, expandDistance, expandDistance).getOffsetBoundingBox(-playerOriginX, -playerOriginY, -playerOriginZ);
      SelectionBoxRenderer.drawFilledCube(boundingBox);


    GL11.glDepthMask(true);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
  }




}
