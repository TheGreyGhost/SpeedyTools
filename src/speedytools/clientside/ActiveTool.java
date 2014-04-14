package speedytools.clientside;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.HashMap;

/**
 * User: The Grey Ghost
 * Date: 14/04/14
 */
public class ActiveTool
{
  public ActiveTool()
  {
    toolTypeRegistry = new HashMap<Integer, SpeedyToolWandStrong>();
  }

  public void setHeldItem(ItemStack heldItem)
  {
    if (heldItem == null) return;
    SpeedyToolWandStrong heldTool = toolTypeRegistry.get(heldItem.itemID);
    if (heldTool == null) return;
    switchToTool(heldTool);
  }

  private void switchToTool(SpeedyToolWandStrong newTool)
  {
    if (activeTool != null) activeTool.deactivateTool();
    newTool.activateTool();
    activeTool = newTool;
  }

  public boolean update(World world, EntityClientPlayerMP player, float partialTick)
  {
    if (activeTool != null) {
      activeTool.update(world, player, partialTick);
    }
    return true;
  }

  public void registerToolType(Item item, SpeedyToolWandStrong speedyToolWandStrong)
  {
    if (toolTypeRegistry.containsKey(item.itemID)) throw new IllegalArgumentException("Duplicate itemID " + item.itemID + " in registerToolType");
    toolTypeRegistry.put(item.itemID, speedyToolWandStrong);
  }

  SpeedyToolWandStrong activeTool;

  private HashMap<Integer, SpeedyToolWandStrong> toolTypeRegistry;
}
