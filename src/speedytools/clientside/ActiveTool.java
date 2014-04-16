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

  /**
   * sets the currently held item
   * @param heldItem
   * @return true if this is a speedy tool
   */
  public boolean setHeldItem(ItemStack heldItem)
  {
    if (heldItem == null) return false;
    SpeedyToolWandStrong heldTool = toolTypeRegistry.get(heldItem.itemID);
    switchToTool(heldTool);
    return (heldTool != null);
  }

  /**
   * return true if a speedy tool is currently active
   * @return
   */
  public boolean toolIsActive() {return activeTool != null;}

  private void switchToTool(SpeedyToolWandStrong newTool)
  {
    if (activeTool != null) activeTool.deactivateTool();
    if (newTool != null)  newTool.activateTool();
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
