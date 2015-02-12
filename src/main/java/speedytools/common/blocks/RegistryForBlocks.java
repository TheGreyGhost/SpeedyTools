package speedytools.common.blocks;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.LanguageRegistry;
import net.minecraft.block.material.Material;

import java.util.ArrayList;

/**
* creates and contains the instances of all of this mod's custom Blocks
* the instances are created manually in order to control the creation time and order
*/
public class RegistryForBlocks
{
  // custom blocks
  public static BlockSelectionFog blockSelectionFog;
  public static BlockSelectionSolidFog blockSelectionSolidFog;

  public static void initialise()
  {
    blockSelectionFog = new BlockSelectionFog(Material.rock);  // material is arbitrary; can't be air because air has no icon
    GameRegistry.registerBlock(blockSelectionFog, blockSelectionFog.NAME);
    blockSelectionSolidFog = new BlockSelectionSolidFog(Material.rock);  // material is arbitrary; can't be air because air has no icon
    GameRegistry.registerBlock(blockSelectionSolidFog, blockSelectionSolidFog.NAME);
  }

  // get a list of all the blocks which have a corresponding item
  public static String [] getAllItemBlockNames()
  {
    return new String[] {blockSelectionFog.NAME, blockSelectionSolidFog.NAME};
  }

}
