package speedytools.common.utilities;

import com.mojang.authlib.HttpAuthenticationService;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.config.Property;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for rotating / flipping blocks
 * Authored by TheGreyGhost 17 Mar 2015
 */

public final class BlockRotateFlipHelper
{

  public enum FlipDirection
  {
    NORTH_SOUTH,
    WEST_EAST,
    UP_DOWN
  }

  private BlockRotateFlipHelper() {
  }

  /**
   * Rotate a block's data value 90 degrees (north->east->south->west->north);
   *
   * @return
   */
  public static IBlockState rotate90(IBlockState iBlockState)
  {
    PropertyDirection propertyDirection = null;

    if (!hasFacingCache.containsKey(iBlockState.getBlock())) {
      for (Object property : iBlockState.getProperties().keySet()) {
        if (property instanceof PropertyDirection) {
          propertyDirection = (PropertyDirection)property;
          break;
        }
      }
      hasFacingCache.put(iBlockState.getBlock(), propertyDirection);
    } else {
      propertyDirection = hasFacingCache.get(iBlockState.getBlock());
    }
    if (propertyDirection == null) return iBlockState; // block doesn't have this property

    EnumFacing currentFacing = (EnumFacing)iBlockState.getValue(propertyDirection);
    int currentHorizontalIndex = currentFacing.getHorizontalIndex();
    if (currentHorizontalIndex < 0) return iBlockState;  // points up or down

    int newHorizontalIndex  = (currentHorizontalIndex + 1) & 3;
    EnumFacing newFacing = EnumFacing.getHorizontal(newHorizontalIndex);
    return iBlockState.withProperty(propertyDirection, newFacing);
  }


  /**
   * Flip a block's data value.
   *
   * @return
   */
  public static IBlockState flip(IBlockState iBlockState) {
    return rotate90(rotate90(iBlockState));
  }

  private static Map<Block, PropertyDirection> hasFacingCache = new HashMap<Block, PropertyDirection>();
  // cache of the direction property for each block; null = block has none

}

