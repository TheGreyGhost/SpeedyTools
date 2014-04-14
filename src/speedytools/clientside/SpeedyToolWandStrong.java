package speedytools.clientside;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import speedytools.clientside.rendering.RendererElement;
import speedytools.clientside.rendering.RendererWireframeSelection;
import speedytools.clientside.rendering.SpeedyToolRenderers;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.items.ItemSpeedyTool;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * User: The Grey Ghost
 * Date: 14/04/14
 */
public class SpeedyToolWandStrong
{
  public SpeedyToolWandStrong(SpeedyToolRenderers renderers)
  {
    rendererUpdateLink = this.new RendererUpdateLink();
    speedyToolRenderers = renderers;
  }

  public boolean activateTool()
  {
    LinkedList<RendererElement> rendererElements = new LinkedList<RendererElement>();
    rendererElements.add(new RendererWireframeSelection(rendererUpdateLink));
    speedyToolRenderers.setRenderers(rendererElements);
    return true;
  }

  public boolean deactivateTool()
  {
    speedyToolRenderers.setRenderers(null);
    currentlySelectedBlocks = null;
    return true;
  }

  public boolean update(World world, EntityClientPlayerMP player, float partialTick)
  {
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

  public class RendererUpdateLink implements RendererWireframeSelection.WireframeRenderInfoUpdateLink
  {
    @Override
    public boolean refreshRenderInfo(RendererWireframeSelection.WireframeRenderInfo infoToUpdate)
    {
      infoToUpdate.currentlySelectedBlocks = currentlySelectedBlocks;
      return true;
    }
  }

  private RendererUpdateLink rendererUpdateLink;
  private SpeedyToolRenderers speedyToolRenderers;

  private List<ChunkCoordinates> currentlySelectedBlocks;
  private BlockWithMetadata currentBlockToPlace;
}
