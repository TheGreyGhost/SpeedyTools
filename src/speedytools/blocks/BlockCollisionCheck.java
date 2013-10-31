package speedytools.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/**
 * Used for collision checking - a dummy block for calling collisionRayTrace on
 * User: TheGreyGhost
 * Date: 29/10/13
 */
public class BlockCollisionCheck extends Block {
  public BlockCollisionCheck(int blockID)
  {
    super(blockID, Material.air);
  }
}
