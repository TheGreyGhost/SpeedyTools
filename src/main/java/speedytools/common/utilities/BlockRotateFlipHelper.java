package speedytools.common.utilities;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;

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
  }

  private BlockRotateFlipHelper() {
  }

  /**
   * Rotate a block by 90 degrees clockwise(north->east->south->west->north);
   * Works by manipulating the block's FACING property.  Blocks without FACING are unaffected.
   * @param iBlockState the starting blockstate
   * @return the rotated blockstate
   */
  public static IBlockState rotate90(IBlockState iBlockState)
  {
    PropertyDirection propertyDirection = getPropertyDirection(iBlockState);
    if (propertyDirection == null) return iBlockState; // block doesn't have this property

    EnumFacing currentFacing = (EnumFacing)iBlockState.getValue(propertyDirection);
    int currentHorizontalIndex = currentFacing.getHorizontalIndex();
    if (currentHorizontalIndex < 0) return iBlockState;  // points up or down

    int newHorizontalIndex  = (currentHorizontalIndex + 1) & 3;
    EnumFacing newFacing = EnumFacing.getHorizontal(newHorizontalIndex);
    return iBlockState.withProperty(propertyDirection, newFacing);
  }

  /**
   * Rotate a block by 90 degrees clockwise(north->east->south->west->north);
   * Works by manipulating the block's FACING property.  Blocks without FACING are unaffected.
   * @param blockID the internal ID for this block
   * @param metadata the metadata value for this block
   * @return the rotated metadata value
   */
  public static int rotate90(int blockID, int metadata)
  {
    Boolean hasPropertyDirection = hasPropertyDirectionCache.get(blockID);
    if (hasPropertyDirection == null) {
      hasPropertyDirection = setPropertyDirectionCache(blockID, metadata);
    }
    if (!hasPropertyDirection) {
      return metadata;
    }

    Block block = Block.getBlockById(blockID);
    IBlockState iBlockState = block.getStateFromMeta(metadata);
    iBlockState = rotate90(iBlockState);
    return block.getMetaFromState(iBlockState);
  }

  /**
   * Mirror image a block
   * Can't do this perfectly because blocks only have a facing, not a left/right mirror image.
   * Make the assumption that a block typically has left/right symmetry in the direction it is facing
   * eg steps when facing EAST are north/south symmetrical but not east-west symmetrical.
   * Hence, if the steps are facing east or west, and you flip in the EAST_WEST direction, it is the same as rotating
   * the steps twice by 90 degrees.
   * If the steps are facing north or south, and you flip in the EAST_WEST direction, then nothing happens.
   * @param blockID the internal ID of this block
   * @param metadata the starting metadata of the block
   * @param flipDirection the axis of flipping - eg EAST_WEST means that the east becomes west
   * @return the flipped blockstate
   */
  public static int flip(int blockID, int metadata, FlipDirection flipDirection)
  {
    Boolean hasPropertyDirection = hasPropertyDirectionCache.get(blockID);
    if (hasPropertyDirection == null) {
      hasPropertyDirection = setPropertyDirectionCache(blockID, metadata);
    }
    if (!hasPropertyDirection) {
      return metadata;
    }

    Block block = Block.getBlockById(blockID);
    IBlockState iBlockState = block.getStateFromMeta(metadata);
    iBlockState = flip(iBlockState, flipDirection);
    return block.getMetaFromState(iBlockState);
  }

  /**
   * Mirror image a block
   * Can't do this perfectly because blocks only have a facing, not a left/right mirror image.
   * Make the assumption that a block typically has left/right symmetry in the direction it is facing
   * eg steps when facing EAST are north/south symmetrical but not east-west symmetrical.
   * Hence, if the steps are facing east or west, and you flip in the EAST_WEST direction, it is the same as rotating
   * the steps twice by 90 degrees.
   * If the steps are facing north or south, and you flip in the EAST_WEST direction, then nothing happens.
   * @param iBlockState the starting blockstate
   * @param flipDirection the axis of flipping - eg EAST_WEST means that the east becomes west
   * @return the flipped blockstate
   */
  public static IBlockState flip(IBlockState iBlockState, FlipDirection flipDirection)
  {
    PropertyDirection propertyDirection = getPropertyDirection(iBlockState);
    if (propertyDirection == null) return iBlockState; // block doesn't have this property

    EnumFacing currentFacing = (EnumFacing)iBlockState.getValue(propertyDirection);

    if (   flipDirection == FlipDirection.WEST_EAST && (currentFacing == EnumFacing.EAST || currentFacing == EnumFacing.WEST)
        || flipDirection == FlipDirection.NORTH_SOUTH && (currentFacing == EnumFacing.SOUTH || currentFacing == EnumFacing.NORTH )) {
      return  rotate90(rotate90(iBlockState));
    } else {
      return iBlockState;
    }
  }

  // retrieves the PropertyDirection for the given block, or null if it doesn't have one.
  //  uses caching
  private static PropertyDirection getPropertyDirection(IBlockState iBlockState) {
    PropertyDirection propertyDirection = null;

    if (!propertyDirectionCache.containsKey(iBlockState.getBlock())) {
      for (Object property : iBlockState.getProperties().keySet()) {
        if (property instanceof PropertyDirection) {
          propertyDirection = (PropertyDirection) property;
          break;
        }
      }
      propertyDirectionCache.put(iBlockState.getBlock(), propertyDirection);
    } else {
      propertyDirection = propertyDirectionCache.get(iBlockState.getBlock());
    }
    return propertyDirection;
  }

  // caches whether this blockID has a property direction or not
  private static boolean setPropertyDirectionCache(int blockID, int metadata)
  {
    Block block = Block.getBlockById(blockID);
    IBlockState iBlockState = block.getStateFromMeta(metadata);
    boolean hasPropertyDirection = false;
    for (Object property : iBlockState.getProperties().keySet()) {
      if (property instanceof PropertyDirection) {
        hasPropertyDirection = true;
        break;
      }
    }
    hasPropertyDirectionCache.put(blockID, hasPropertyDirection);
    return hasPropertyDirection;
  }

  private static Map<Block, PropertyDirection> propertyDirectionCache = new HashMap<Block, PropertyDirection>();
  // cache of the direction property for each block; null = block has none
  private static Map<Integer, Boolean> hasPropertyDirectionCache = new HashMap<Integer, Boolean>();

}

