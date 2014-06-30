package speedytools.common.blocks;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import net.minecraft.block.material.Material;

/**
 * creates and contains the instances of all of this mod's custom Blocks
 * the instances are created manually in order to control the creation time and order
 */
public class RegistryForBlocks
{
  // custom blocks
  public static BlockSelectionFog blockSelectionFog;

  public static void initialise()
  {
    final int START_BLOCK = 2142;
    blockSelectionFog = (BlockSelectionFog)(new BlockSelectionFog(START_BLOCK, Material.air).setUnlocalizedName("selectionfog").setTextureName("speedytoolsmod:selectionfog"));

    GameRegistry.registerBlock(blockSelectionFog, blockSelectionFog.getUnlocalizedName());

    LanguageRegistry.addName(blockSelectionFog, "Selection Fog");
  }
}
