package speedytools.clientside.tools;

import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.network.CloneToolsNetworkClient;
import speedytools.clientside.network.PacketSenderClient;
import speedytools.clientside.rendering.RenderCursorStatus;
import speedytools.clientside.rendering.SpeedyToolRenderers;
import speedytools.clientside.selections.BlockMultiSelector;
import speedytools.clientside.selections.ClientVoxelSelection;
import speedytools.clientside.sound.SoundController;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.items.ItemSpeedyTool;
import speedytools.common.selections.FillMatcher;
import speedytools.common.utilities.Colour;
import speedytools.common.utilities.ResultWithReason;

/**
* User: The Grey Ghost
* Date: 8/08/2014
*/
public class SpeedyToolComplexOrb extends SpeedyToolComplex
{
  public SpeedyToolComplexOrb(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SoundController i_speedyToolSounds, UndoManagerClient i_undoManagerClient, CloneToolsNetworkClient i_cloneToolsNetworkClient,
                              SpeedyToolBoundary i_speedyToolBoundary, ClientVoxelSelection i_clientVoxelSelection, CommonSelectionState i_commonSelectionState,
                              SelectionPacketSender packetSender, PacketSenderClient i_packetSenderClient) {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient, i_cloneToolsNetworkClient, i_speedyToolBoundary,
          i_clientVoxelSelection, i_commonSelectionState, packetSender, i_packetSenderClient);

    fillAlgorithmSettings.setAutomaticLowerBound(false);
  }

  @Override
  public RenderCursorStatus.CursorRenderInfo.CursorType getCursorType() {
    return RenderCursorStatus.CursorRenderInfo.CursorType.REPLACE;
  }

  @Override
  protected Colour getSelectionRenderColour() {
    return Colour.WHITE_100;
  }

  @Override
  protected boolean cancelSelectionAfterAction() {
    return true;
  }

  /**
   * if true, selections made using this tool can be dragged around
   *
   * @return
   */
  @Override
  protected boolean selectionIsMoveable() {
    return false;
  }

  /**
   * if true, CTRL + mousewheel changes the item count
   *
   * @return
   */
  @Override
  protected boolean  mouseWheelChangesCount() {
    return true;
  }

  @Override
  protected boolean isDiagonalPropagationAllowed(boolean userRequested)
  {
    return userRequested;
  }

  @Override
  protected FillMatcher getFillMatcherForSelectionCreation(World world, ChunkCoordinates blockUnderCursor)
  {
    Block block = world.getBlock(blockUnderCursor.posX, blockUnderCursor.posY, blockUnderCursor.posZ);
    int metadata = world.getBlockMetadata(blockUnderCursor.posX, blockUnderCursor.posY, blockUnderCursor.posZ);
    BlockWithMetadata blockWithMetadata = new BlockWithMetadata(block, metadata);
    FillMatcher fillMatcher = new FillMatcher.OnlySpecifiedBlock(blockWithMetadata);
    return fillMatcher;
  }

  @Override
  protected BlockWithMetadata getOverrideTexture()
  {
    return currentBlockToPlace;
  }

  @Override
  public boolean updateForThisFrame(World world, EntityClientPlayerMP player, float partialTick) {
    // the block to be placed is the one to the right of the tool in the hotbar
    int currentlySelectedHotbarSlot = player.inventory.currentItem;

    final int MAX_HOTBAR_SLOT = 8;
    ItemStack itemStackToPlace = (currentlySelectedHotbarSlot == MAX_HOTBAR_SLOT) ? null : player.inventory.getStackInSlot(currentlySelectedHotbarSlot + 1);
    currentBlockToPlace = getPlacedBlockFromItemStack(itemStackToPlace);

    boolean retval = super.updateForThisFrame(world, player, partialTick);

    return retval;
  }

  /**
   * when selecting the first block in a selection, how should it be done?
   *
   * @return
   */
  @Override
  protected BlockMultiSelector.BlockSelectionBehaviour getBlockSelectionBehaviour() {
    return BlockMultiSelector.BlockSelectionBehaviour.ORB_STYLE;
  }

  @Override
  protected ResultWithReason performComplexToolAction(Vec3 selectionPosition) {
    if (currentBlockToPlace == null) return ResultWithReason.failure("I am confused...");
    return cloneToolsNetworkClient.performComplexToolFillAction(Item.getIdFromItem(parentItem), currentBlockToPlace,
            Math.round((float) selectionPosition.xCoord),
            Math.round((float) selectionPosition.yCoord),
            Math.round((float) selectionPosition.zCoord),
            commonSelectionState.selectionOrientation);
  }
//  @Override
//  public boolean updateForThisFrame(World world, EntityClientPlayerMP player, float partialTick)
//  {
////    checkInvariants();
//    if (clientVoxelSelection.getReadinessForDisplaying() != ClientVoxelSelection.VoxelSelectionState.NO_SELECTION) return false;
//    updateBoundaryCornersFromToolBoundary();
//
//    MovingObjectPosition target = itemComplexBase.rayTraceLineOfSight(player.worldObj, player);
//

//    final int MAX_NUMBER_OF_HIGHLIGHTED_BLOCKS = 64;
//    blockUnderCursor = null;
//    highlightedBlocks = null;
//    currentHighlighting = SelectionType.NONE;
//
//    if (target != null && target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
//      blockUnderCursor = new ChunkCoordinates(target.blockX, target.blockY, target.blockZ);
//      boolean selectedBlockIsInsideBoundaryField = false;
//
//      if (boundaryCorner1 != null && boundaryCorner2 != null) {
//        if (   blockUnderCursor.posX >= boundaryCorner1.posX && blockUnderCursor.posX <= boundaryCorner2.posX
//                && blockUnderCursor.posY >= boundaryCorner1.posY && blockUnderCursor.posY <= boundaryCorner2.posY
//                && blockUnderCursor.posZ >= boundaryCorner1.posZ && blockUnderCursor.posZ <= boundaryCorner2.posZ ) {
//          selectedBlockIsInsideBoundaryField = true;
//        }
//      }
//
//      if (selectedBlockIsInsideBoundaryField) {
//        currentHighlighting = SelectionType.BOUND_FILL_STRICT;
//        highlightedBlocks = selectFill(target, player.worldObj, MAX_NUMBER_OF_HIGHLIGHTED_BLOCKS, controlKeyIsDown, false,
//                boundaryCorner1.posX, boundaryCorner2.posX,
//                boundaryCorner1.posY, boundaryCorner2.posY,
//                boundaryCorner1.posZ, boundaryCorner2.posZ);
//      } else {
//        currentHighlighting = SelectionType.UNBOUND_FILL_STRICT;
//        highlightedBlocks = selectFill(target, player.worldObj, MAX_NUMBER_OF_HIGHLIGHTED_BLOCKS, controlKeyIsDown, false,
//                Integer.MIN_VALUE, Integer.MAX_VALUE,
//                0, 255,
//                Integer.MIN_VALUE, Integer.MAX_VALUE);
//      }
//      return true;
//    }
//    return false;
//  }

   private BlockWithMetadata currentBlockToPlace;
}
