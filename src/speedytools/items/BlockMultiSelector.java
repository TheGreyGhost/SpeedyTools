package speedytools.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: TheGreyGhost
 * Date: 28/10/13
 * Time: 9:47 PM
 * BlockMultiSelector is a group of methods used to select multiple blocks based on where the mouse is pointing.
 */
public class BlockMultiSelector
{
  /**
   * selectLine is used to select a straight line of blocks, and return a list of their coordinates.
   * There are three distinct cases for the starting block:
   * (1) the mouse is not on any target: the first block selected will be the one corresponding to the position of the player's hand
   * (2) the mouse is on a tile target: the first block selected will be the one adjacent to the tile in mouseTarget, on the face in mouseTarget.
   * (3) the mouse is on an entity: no selection.
   * After selecting the inital block, the selection will continue in a line parallel to the player's look direction, snapped to the six cardinal directions or
   *   alternatively to one of the twenty 45 degree directions (if diagonalOK == true)
   * @param mouseTarget  where the cursor is currently pointed
   * @param player       the player (used for position and look information)
   * @param partialTick
   * @param maxLineLength the maximum number of blocks to select
   * @param diagonalOK    if true, diagonal 45 degree lines are allowed
   * @param stopWhenCollide if true, stops when a solid block is encountered (canCollide == true).  Otherwise, continues for maxLineLength
   * @return a list of the coordinates of all blocks in the selection
   */
  public static List<Vec3> selectLine(MovingObjectPosition mouseTarget, EntityPlayer player, float partialTick,
                                      int maxLineLength, boolean diagonalOK, boolean stopWhenCollide)
  {
    List<Vec3> selection = new ArrayList<Vec3>();

    switch (mouseTarget.typeOfHit) {
 //     case null:

      case TILE: {
        break;
      }
      case ENTITY:{
        return selection;
      }
    }


    return selection;
  }

}
