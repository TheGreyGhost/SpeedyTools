package speedytools.clientside.tools;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Facing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.network.PacketSenderClient;
import speedytools.clientside.rendering.SpeedyToolRenderers;
import speedytools.clientside.selections.BlockMultiSelector;
import speedytools.clientside.sound.SoundController;
import speedytools.clientside.sound.SoundEffectNames;
import speedytools.clientside.sound.SoundEffectSimple;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.items.ItemSpeedyTool;
import speedytools.common.selections.FillMatcher;
import speedytools.common.utilities.Pair;
import speedytools.common.utilities.UsefulConstants;

import java.util.ArrayList;
import java.util.List;

/**
* User: The Grey Ghost
* Date: 14/04/14
*/
public class SpeedyToolSceptre extends SpeedyToolSimple
{
  public SpeedyToolSceptre(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SoundController i_speedyToolSounds,
                           UndoManagerClient i_undoManagerClient, PacketSenderClient i_PacketSenderClient)
  {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient, i_PacketSenderClient);
  }

  /**
   * Selects the Blocks that will be affected by the tool when the player presses right-click
   * @param target the position of the cursor
   * @param player the player
   * @param maxSelectionSize the maximum number of blocks in the selection
   * @param itemStackToPlace the item that would be placed in the selection
   * @param partialTick partial tick time.
   * @return returns the list of blocks in the selection (may be zero length)
   */
  @Override
  protected Pair<List<ChunkCoordinates>, Integer> selectBlocks(MovingObjectPosition target, EntityPlayer player, int maxSelectionSize, ItemStack itemStackToPlace, float partialTick)
  {
    BlockWithMetadata blockWithMetadata = getPlacedBlockFromItemStack(itemStackToPlace);
    boolean additiveContour = (blockWithMetadata.block != Blocks.air);

    return selectContourBlocks(target, player, maxSelectionSize, additiveContour, partialTick);
  }

  @Override
  protected void playPlacementSound(Vec3 playerPosition)
  {
    SoundEffectSimple soundEffectSimple = new SoundEffectSimple(SoundEffectNames.SCEPTRE_PLACE, soundController);
    soundEffectSimple.startPlaying();
  }

  @Override
  protected void playUndoSound(Vec3 playerPosition)
  {
    SoundEffectSimple soundEffectSimple = new SoundEffectSimple(SoundEffectNames.SCEPTRE_UNPLACE, soundController);
    soundEffectSimple.startPlaying();
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
  protected Pair<List<ChunkCoordinates>, Integer> selectContourBlocks(MovingObjectPosition target, EntityPlayer player, int maxSelectionSize, boolean additiveContour, float partialTick)
  {
    if (target == null || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
      return new Pair<List<ChunkCoordinates>, Integer>(new ArrayList<ChunkCoordinates>(), UsefulConstants.FACE_YPOS);
    }

    BlockMultiSelector.BlockTypeToSelect blockTypeToSelect = additiveContour ? BlockMultiSelector.BlockTypeToSelect.AIR_ONLY
                                                                             : BlockMultiSelector.BlockTypeToSelect.SOLID_OK;
    MovingObjectPosition startBlock = BlockMultiSelector.selectStartingBlock(target, blockTypeToSelect, player, partialTick);
    if (startBlock == null) return new Pair<List<ChunkCoordinates>, Integer>(new ArrayList<ChunkCoordinates>(), UsefulConstants.FACE_YPOS);
    ChunkCoordinates blockUnderCursor = new ChunkCoordinates(startBlock.blockX, startBlock.blockY, startBlock.blockZ);
    boolean diagonalOK = controlKeyIsDown;

    FillMatcher fillMatcher;
    if (additiveContour) {
      fillMatcher = new FillMatcher.ContourFollower(Facing.oppositeSide[startBlock.sideHit]);
    } else {
      fillMatcher = new FillMatcher.AnySolid();
    }

    List<ChunkCoordinates> selection = BlockMultiSelector.selectContourUnbounded(blockUnderCursor, player.worldObj, maxSelectionSize, diagonalOK,
            fillMatcher, startBlock.sideHit);
    return new Pair<List<ChunkCoordinates>, Integer> (selection, startBlock.sideHit);
  }

}
