package speedytools.common.blocks;

import net.minecraft.block.Block;

/**
 * User: The Grey Ghost
 * Date: 8/11/13
 */
public class BlockWithMetadata
{
  public Block block;
  public int metaData;

  public BlockWithMetadata() {}

  public BlockWithMetadata(Block i_block, int i_metaData)
  {
    block = i_block;
    metaData = i_metaData;
  }
}
