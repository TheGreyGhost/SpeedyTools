package speedytools.clientside.tools;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.init.Blocks;
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
import speedytools.common.selections.FillAlgorithmSettings;
import speedytools.common.selections.FillMatcher;
import speedytools.common.utilities.Colour;
import speedytools.common.utilities.ResultWithReason;

/**
* User: The Grey Ghost
* Date: 8/08/2014
*/
public class SpeedyToolComplexSceptre extends SpeedyToolComplex
{
  public SpeedyToolComplexSceptre(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SoundController i_speedyToolSounds, UndoManagerClient i_undoManagerClient, CloneToolsNetworkClient i_cloneToolsNetworkClient,
                                  SpeedyToolBoundary i_speedyToolBoundary, ClientVoxelSelection i_clientVoxelSelection, CommonSelectionState i_commonSelectionState,
                                  SelectionPacketSender packetSender, PacketSenderClient i_packetSenderClient) {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient, i_cloneToolsNetworkClient, i_speedyToolBoundary,
          i_clientVoxelSelection, i_commonSelectionState, packetSender, i_packetSenderClient);

    fillAlgorithmSettings.setAutomaticLowerBound(false);
    fillAlgorithmSettings.setPropagation(FillAlgorithmSettings.Propagation.CONTOUR);

  }

  @Override
  public RenderCursorStatus.CursorRenderInfo.CursorType getCursorType() {
    return RenderCursorStatus.CursorRenderInfo.CursorType.CONTOUR;
  }

  @Override
  protected Colour getSelectionRenderColour() {
    return Colour.PURPLE_100;
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
    boolean additiveContour = (currentBlockToPlace != null && currentBlockToPlace.block != Blocks.air);
    FillMatcher fillMatcher;
    if (additiveContour) {
      fillMatcher = new FillMatcher.ContourFollower(true, blockUnderCursorSideHit);
    } else {
      fillMatcher = new FillMatcher.ContourFollower(false, blockUnderCursorSideHit);
    }
    return fillMatcher;
  }

//  @Override
//  protected MovingObjectPosition getBlockUnderCursor(EntityClientPlayerMP player, float partialTick)
//  {
//    boolean additiveContour = (currentBlockToPlace != null && currentBlockToPlace.block != Blocks.air);
//    MovingObjectPosition target = parentItem.rayTraceLineOfSight(player.worldObj, player);
//    if (target != null && target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
//      BlockMultiSelector.BlockTypeToSelect blockTypeToSelect = additiveContour ? BlockMultiSelector.BlockTypeToSelect.AIR_ONLY
//              : BlockMultiSelector.BlockTypeToSelect.SOLID_OK;
//      MovingObjectPosition startBlock = BlockMultiSelector.selectStartingBlock(target, blockTypeToSelect, player, partialTick);
//      return startBlock;
//    }
//    return target;
//  }

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
    boolean additiveMode = (currentBlockToPlace.block != Blocks.air);
    return additiveMode ? BlockMultiSelector.BlockSelectionBehaviour.SCEPTRE_ADD_STYLE : BlockMultiSelector.BlockSelectionBehaviour.SCEPTRE_REPLACE_SYTLE;
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

   private BlockWithMetadata currentBlockToPlace;

}
