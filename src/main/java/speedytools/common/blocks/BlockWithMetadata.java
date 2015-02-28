package speedytools.common.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

/**
 * User: The Grey Ghost
 * Date: 8/11/13
 */
public class BlockWithMetadata
{
  public Block block;
  public int metaData;

  public BlockWithMetadata() {}

  public BlockWithMetadata(IBlockState iBlockState)
  {
    block = iBlockState.getBlock();
    if (block != null) {
      metaData = block.getMetaFromState(iBlockState);
    }
  }

  public BlockWithMetadata(Block i_block, int i_metaData)
  {
    block = i_block;
    metaData = i_metaData;
  }
}
