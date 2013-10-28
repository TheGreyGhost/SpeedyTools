package speedytools.items;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.event.ForgeSubscribe;
import org.lwjgl.opengl.GL11;
import speedytools.SpeedyToolsMod;

/**
 Contains the custom Forge Event Handlers
 */
public class ItemEventHandler {

  @ForgeSubscribe
  public void drawSelectionBox(DrawBlockHighlightEvent event)
  {
    RenderGlobal context = event.context;
    EntityPlayer player = event.player;
    MovingObjectPosition target = event.target;
    int subID = event.subID;
    ItemStack currentItem = event.currentItem;
    float partialTick = event.partialTicks;

    if (currentItem == null || currentItem.getItem().itemID != SpeedyToolsMod.itemSpeedyStrip.itemID) return;
    if (target.typeOfHit != EnumMovingObjectType.TILE) return;

    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
    GL11.glLineWidth(2.0F);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDepthMask(false);
    double expandDistance = 0.002F;
    int j = context.theWorld.getBlockId(target.blockX, target.blockY, target.blockZ);

    if (j > 0)
    {
      Block.blocksList[j].setBlockBoundsBasedOnState(context.theWorld, target.blockX, target.blockY, target.blockZ);
      double playerOriginX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTick;
      double playerOriginY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTick;
      double playerOriginZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTick;
      AxisAlignedBB boundingBox = AxisAlignedBB.getAABBPool().getAABB(target.blockX, target.blockY, target.blockZ, target.blockX+1.0, target.blockY+1.0, target.blockZ+1.0);
      boundingBox = boundingBox.expand(expandDistance, expandDistance, expandDistance).getOffsetBoundingBox(-playerOriginX, -playerOriginY, -playerOriginZ);
      SelectionBoxRenderer.drawFilledCube(boundingBox);
    }

    GL11.glDepthMask(true);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);

    event.setCanceled(true);
  }




}
