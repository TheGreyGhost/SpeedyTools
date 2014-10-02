package speedytools.common.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import speedytools.SpeedyToolsMod;

/**
* User: The Grey Ghost
* Date: 21/06/2014
* Used for rendering blocks where it's known that they are in the selection, but not known what kind of block they are
*/
public class BlockSelectionSolidFog extends Block
{
  public static final String NAME = "selectionsolidfog";
  public BlockSelectionSolidFog(Material par2Material) {
    super(par2Material);
    setBlockName(NAME);
    setBlockTextureName(SpeedyToolsMod.prependModID("selectionsolidfog"));
  }
}
