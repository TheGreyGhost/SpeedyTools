package speedytools.clientside.tools;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.network.PacketSenderClient;
import speedytools.clientside.rendering.*;
import speedytools.clientside.selections.BlockMultiSelector;
import speedytools.clientside.sound.SpeedySoundTypes;
import speedytools.clientside.sound.SpeedyToolSounds;
import speedytools.common.items.ItemSpeedyTool;

import java.util.List;

/**
* User: The Grey Ghost
* Date: 14/04/14
*/
public class SpeedyToolWandStrong extends SpeedyToolSimple
{
  public SpeedyToolWandStrong(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SpeedyToolSounds i_speedyToolSounds,
                              UndoManagerClient i_undoManagerClient, PacketSenderClient i_packetSenderClient)
  {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient, i_packetSenderClient);
  }

  /**
   * Selects the Blocks that will be affected by the tool when the player presses right-click
   * @param target the position of the cursor
   * @param player the player
   * @param  maxSelectionSize the maximum number of blocks in the selection
   * @param itemStackToPlace the item that would be placed in the selection
   * @param partialTick partial tick time.
   * @return returns the list of blocks in the selection (may be zero length)
   */
  @Override
  protected List<ChunkCoordinates> selectBlocks(MovingObjectPosition target, EntityPlayer player, int maxSelectionSize, ItemStack itemStackToPlace, float partialTick)
  {
    return selectLineOfBlocks(target, player, maxSelectionSize, BlockMultiSelector.CollisionOptions.CONTINUE_THROUGH_SOLID_BLOCKS, partialTick);
  }


  @Override
  protected void playPlacementSound(Vec3 playerPosition)
  {
    speedyToolSounds.playSound(SpeedySoundTypes.STRONGWAND_PLACE, playerPosition);
  }

  @Override
  protected void playUndoSound(Vec3 playerPosition)
  {
    speedyToolSounds.playSound(SpeedySoundTypes.STRONGWAND_UNPLACE, playerPosition);
  }
}
