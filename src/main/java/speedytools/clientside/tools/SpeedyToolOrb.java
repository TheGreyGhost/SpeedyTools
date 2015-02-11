package speedytools.clientside.tools;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
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
public class SpeedyToolOrb extends SpeedyToolSimple
{
  public SpeedyToolOrb(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SoundController i_speedyToolSounds,
                       UndoManagerClient i_undoManagerClient, PacketSenderClient i_packetSenderClient)
  {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient, i_packetSenderClient);
  }

  /**
   * Selects the Blocks that will be affected by the tool when the player presses right-click
   * @param target the position of the cursor
   * @param player the player
   * @param maxSelectionSize the maximum number of blocks in the selection
   * @param partialTick partial tick time.
   * @return returns the list of blocks in the selection (may be zero length)
   */
  @Override
  protected Pair<List<BlockPos>, Integer> selectBlocks(MovingObjectPosition target, EntityPlayer player, int maxSelectionSize, float partialTick)
  {
    return selectFillBlocks(target, player, maxSelectionSize, partialTick);
  }

  @Override
  protected void playPlacementSound(Vec3 playerPosition)
  {
    SoundEffectSimple soundEffectSimple = new SoundEffectSimple(SoundEffectNames.ORB_PLACE, soundController);
    soundEffectSimple.startPlaying();
  }

  @Override
  protected void playUndoSound(Vec3 playerPosition)
  {
    SoundEffectSimple soundEffectSimple = new SoundEffectSimple(SoundEffectNames.ORB_UNPLACE, soundController);
    soundEffectSimple.startPlaying();
  }

  @Override
  protected BlockMultiSelector.BlockSelectionBehaviour getBlockSelectionBehaviour() {return BlockMultiSelector.BlockSelectionBehaviour.ORB_STYLE;}

  /**
   * Selects the "blob" of blocks that will be affected by the tool when the player presses right-click
   * Starting from the block identified by mouseTarget, the selection will flood fill all matching blocks.
   * @param blockUnderCursorMOP  the block to start the flood fill from
   * @param player
   * @param maxSelectionSize the maximum number of blocks in the selection
   * @param partialTick
   * @return   returns the list of blocks in the selection (may be zero length)
   */
  protected Pair<List<BlockPos>, Integer> selectFillBlocks(MovingObjectPosition blockUnderCursorMOP,
                                                                   EntityPlayer player, int maxSelectionSize, float partialTick)
  {
//    MovingObjectPosition startBlock = BlockMultiSelector.selectStartingBlock(blockUnderCursorMOP, BlockMultiSelector.BlockTypeToSelect.SOLID_OK, player, partialTick);
    if (blockUnderCursorMOP == null || blockUnderCursorMOP.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
      return new Pair<List<BlockPos>, Integer>(new ArrayList<BlockPos>(), UsefulConstants.FACE_YPOS);
    }
    BlockPos blockUnderCursor = new BlockPos(blockUnderCursorMOP.blockX, blockUnderCursorMOP.blockY, blockUnderCursorMOP.blockZ);

    boolean diagonalOK =  controlKeyIsDown;

    World world = player.worldObj;
    Block block = world.getBlock(blockUnderCursor.posX, blockUnderCursor.posY, blockUnderCursor.posZ);
    if (block == Blocks.air) {
      return new Pair<List<BlockPos>, Integer>(new ArrayList<BlockPos>(), UsefulConstants.FACE_YPOS);
    }

    int metadata = world.getBlockMetadata(blockUnderCursor.posX, blockUnderCursor.posY, blockUnderCursor.posZ);
    BlockWithMetadata blockWithMetadata = new BlockWithMetadata(block, metadata);
    FillMatcher fillMatcher = new FillMatcher.OnlySpecifiedBlock(blockWithMetadata);

    List<BlockPos> selection = BlockMultiSelector.selectFillUnbounded(blockUnderCursor, player.worldObj, maxSelectionSize, diagonalOK, fillMatcher);
    return new Pair<List<BlockPos>, Integer> (selection, blockUnderCursorMOP.sideHit);
  }

}
