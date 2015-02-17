package speedytools.common.items;

import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.Item;

import java.util.EnumMap;

/**
 * User: The Grey Ghost
 * Date: 13/01/2015
 * This class is used to encapsulate client-only code out of the ItemSpeedyBoundary so it doesn't crash on a dedicated server.
 */
public class ItemBoundaryModels
{
  private EnumMap<ItemSpeedyBoundary.IconNames, ModelResourceLocation> models = new EnumMap<ItemSpeedyBoundary.IconNames, ModelResourceLocation>(ItemSpeedyBoundary.IconNames.class) ;

  public ItemBoundaryModels()
  {
    for (ItemSpeedyBoundary.IconNames entry : ItemSpeedyBoundary.IconNames.values()) {
      ModelResourceLocation newIcon = new ModelResourceLocation("speedytoolsmod:" + entry.filename, "inventory");
      models.put(entry, newIcon);
    }
  }

  public ModelResourceLocation getModel(ItemSpeedyBoundary.IconNames modelName) {
    return models.get(modelName);
  }

  public void registerVariants(Item item)
  {
    // need to add the variants to the bakery so it knows what models are available for rendering.
    for (ItemSpeedyBoundary.IconNames name : ItemSpeedyBoundary.IconNames.values()) {
      ModelBakery.addVariantName(item, "speedytoolsmod:" + name.filename);
    }
  }

}
