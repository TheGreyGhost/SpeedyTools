//package speedytools.clientside.tools;
//
//import net.minecraft.entity.player.EntityPlayer;
//import net.minecraft.init.Blocks;
//import net.minecraft.util.BlockPos;
//import net.minecraft.util.EnumFacing;
//import net.minecraft.util.MovingObjectPosition;
//import net.minecraft.util.Vec3;
//import speedytools.clientside.UndoManagerClient;
//import speedytools.clientside.network.PacketSenderClient;
//import speedytools.clientside.rendering.SpeedyToolRenderers;
//import speedytools.clientside.selections.BlockMultiSelector;
//import speedytools.clientside.sound.SoundController;
//import speedytools.clientside.sound.SoundEffectNames;
//import speedytools.clientside.sound.SoundEffectSimple;
//import speedytools.common.items.ItemSpeedyTool;
//import speedytools.common.selections.FillMatcher;
//import speedytools.common.utilities.Pair;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
//* User: The Grey Ghost
//* Date: 14/04/14
//*/
//public class SpeedyToolSceptre extends SpeedyToolSimple
//{
//  public SpeedyToolSceptre(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SoundController i_speedyToolSounds,
//                           UndoManagerClient i_undoManagerClient, PacketSenderClient i_PacketSenderClient)
//  {
//    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient, i_PacketSenderClient);
//  }
//
//  /**
//   * Selects the Blocks that will be affected by the tool when the player presses right-click
//   * @param blockUnderCursor the position of the cursor
//   * @param player the player
//   * @param maxSelectionSize the maximum number of blocks in the selection
//   * @param partialTick partial tick time.
//   * @return returns the list of blocks in the selection (may be zero length)
//   */
//  @Override
//  protected Pair<List<BlockPos>, EnumFacing> selectBlocks(MovingObjectPosition blockUnderCursor, EntityPlayer player, int maxSelectionSize, float partialTick)
//  {
////    BlockWithMetadata blockWithMetadata = getPlacedBlockFromItemStack(itemStackToPlace);
//    boolean additiveContour = (currentBlockToPlace.block != Blocks.air);
//
//    return selectContourBlocks(blockUnderCursor, player, maxSelectionSize, additiveContour, partialTick);
//  }
//
//  @Override
//  protected void playPlacementSound(Vec3 playerPosition)
//  {
//    SoundEffectSimple soundEffectSimple = new SoundEffectSimple(SoundEffectNames.SCEPTRE_PLACE, soundController);
//    soundEffectSimple.startPlaying();
//  }
//
//  @Override
//  protected void playUndoSound(Vec3 playerPosition)
//  {
//    SoundEffectSimple soundEffectSimple = new SoundEffectSimple(SoundEffectNames.SCEPTRE_UNPLACE, soundController);
//    soundEffectSimple.startPlaying();
//  }
//
//  @Override
//  protected BlockMultiSelector.BlockSelectionBehaviour getBlockSelectionBehaviour()
//  {
//    boolean additiveMode = (currentBlockToPlace.block != Blocks.air);
//    return additiveMode ? BlockMultiSelector.BlockSelectionBehaviour.SCEPTRE_ADD_STYLE : BlockMultiSelector.BlockSelectionBehaviour.SCEPTRE_REPLACE_SYTLE;
//  }
//
//  /**
//   * Selects the contour of Blocks that will be affected by the tool when the player presses right-click
//   * Starting from the block identified by mouseTarget, the selection will attempt to follow any contours in the same plane as the side hit.
//   * (for example: if there is a zigzagging wall, it will select the layer of blocks that follows the top of the wall.)
//   * Depending on additiveContour, it will either select the non-solid blocks on top of the contour (to make the wall "taller"), or
//   *   select the solid blocks that form the top layer of the contour (to remove the top layer of the wall).
//   * @param target the position of the cursor
//   * @param player the player
//   * @param maxSelectionSize the maximum number of blocks in the selection
//   * @param additiveContour if true, selects the layer of non-solid blocks adjacent to the contour.  if false, selects the solid blocks in the contour itself
//   * @param partialTick partial tick time.
//   * @return returns the list of blocks in the selection (may be zero length)
//   */
//  protected Pair<List<BlockPos>, EnumFacing> selectContourBlocks(MovingObjectPosition target, EntityPlayer player, int maxSelectionSize, boolean additiveContour, float partialTick)
//  {
//    if (target == null || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
//      return new Pair<List<BlockPos>, EnumFacing>(new ArrayList<BlockPos>(), EnumFacing.UP);
//    }
//
//    BlockMultiSelector.BlockSelectionBehaviour blockSelectionBehaviour = getBlockSelectionBehaviour();
//    MovingObjectPosition startBlock = BlockMultiSelector.selectStartingBlock(target, blockSelectionBehaviour, player, partialTick);
//    if (startBlock == null) return new Pair<List<BlockPos>, EnumFacing>(new ArrayList<BlockPos>(), EnumFacing.UP);
//    BlockPos blockUnderCursor = new BlockPos(startBlock.func_178782_a());  // func_178782_a = get block hit
//    boolean diagonalOK = controlKeyIsDown;
//
//    FillMatcher fillMatcher;
//    if (additiveContour) {
//      fillMatcher = new FillMatcher.ContourFollower(true, startBlock.field_178784_b);      // face hit
//    } else {
//      fillMatcher = new FillMatcher.ContourFollower(false, startBlock.field_178784_b);
//    }
//
//    List<BlockPos> selection = BlockMultiSelector.selectContourUnbounded(blockUnderCursor, player.worldObj, maxSelectionSize, diagonalOK,
//            fillMatcher, startBlock.field_178784_b); //  field_178784_b = face hit
//    return new Pair<List<BlockPos>, EnumFacing> (selection, startBlock.field_178784_b);
//  }
//
//}
