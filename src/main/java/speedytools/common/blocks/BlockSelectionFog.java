package speedytools.common.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import speedytools.SpeedyToolsMod;

/**
* User: The Grey Ghost
* Date: 21/06/2014
* Used for rendering blocks without a valid texture (eg air)
*/
public class BlockSelectionFog extends Block
{
  public static final String NAME = "selectionfog";
  public BlockSelectionFog(Material par2Material) {
    super(par2Material);
    setBlockName(NAME);
    setBlockTextureName(SpeedyToolsMod.prependModID("selectionfog"));
  }
}
