package speedytools.common.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import speedytools.clientside.ClientSide;

/**
* User: The Grey Ghost
* Date: 2/11/13
*/
public abstract class ItemSpeedyTool extends Item
{
  public ItemSpeedyTool() {
    super();
    setCreativeTab(ClientSide.tabSpeedyTools);
    setMaxDamage(-1);                         // not damageable
  }

  /**
   * Finds the first block in the player's line of sight, including liquids
   * Has to be in here because getMovingObjectPositionFromPlayer is protected.
   * @param world
   * @param entityPlayer
   * @return the corresponding MovingObjectPosition
   */
  public MovingObjectPosition rayTraceLineOfSight(World world, EntityPlayer entityPlayer)
  {
    return this.getMovingObjectPositionFromPlayer(world, entityPlayer, true);
  }

  /**
   * does this tool use the adjacent block in the hotbar?
   * @return
   */
  public boolean usesAdjacentBlockInHotbar() {return true;}

}
