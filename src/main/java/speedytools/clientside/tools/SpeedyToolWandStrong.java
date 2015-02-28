//package speedytools.clientside.tools;
//
//import net.minecraft.entity.player.EntityPlayer;
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
//import speedytools.common.utilities.Pair;
//
//import java.util.List;
//
///**
//* User: The Grey Ghost
//* Date: 14/04/14
//*/
//public class SpeedyToolWandStrong extends SpeedyToolSimple
//{
//  public SpeedyToolWandStrong(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SoundController i_speedyToolSounds,
//                              UndoManagerClient i_undoManagerClient, PacketSenderClient i_packetSenderClient)
//  {
//    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient, i_packetSenderClient);
//  }
//
//  /**
//   * Selects the Blocks that will be affected by the tool when the player presses right-click
//   * @param blockUnderCursor the position of the cursor
//   * @param player the player
//   * @param  maxSelectionSize the maximum number of blocks in the selection
//   * @param partialTick partial tick time.
//   * @return returns the list of blocks in the selection (may be zero length)
//   */
//  @Override
//  protected Pair<List<BlockPos>, EnumFacing> selectBlocks(MovingObjectPosition blockUnderCursor, EntityPlayer player, int maxSelectionSize, float partialTick)
//  {
//    return selectLineOfBlocks(blockUnderCursor, player, maxSelectionSize, BlockMultiSelector.CollisionOptions.CONTINUE_THROUGH_SOLID_BLOCKS, partialTick);
//  }
//
//
//  @Override
//  protected void playPlacementSound(Vec3 playerPosition)
//  {
//    SoundEffectSimple soundEffectSimple = new SoundEffectSimple(SoundEffectNames.STRONGWAND_PLACE, soundController);
//    soundEffectSimple.startPlaying();
//  }
//
//  @Override
//  protected void playUndoSound(Vec3 playerPosition)
//  {
//    SoundEffectSimple soundEffectSimple = new SoundEffectSimple(SoundEffectNames.STRONGWAND_UNPLACE, soundController);
//    soundEffectSimple.startPlaying();
//  }
//
//  @Override
//  protected BlockMultiSelector.BlockSelectionBehaviour getBlockSelectionBehaviour() {
//    return BlockMultiSelector.BlockSelectionBehaviour.WAND_STYLE;
//  }
//}
