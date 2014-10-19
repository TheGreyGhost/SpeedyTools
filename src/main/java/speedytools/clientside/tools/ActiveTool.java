package speedytools.clientside.tools;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import speedytools.clientside.userinput.UserInput;

import java.util.HashMap;

/**
* User: The Grey Ghost
* Date: 14/04/14
*/
public class ActiveTool
{
  public ActiveTool()
  {
    toolTypeRegistry = new HashMap<Item, SpeedyTool>();
  }

  /**
   * sets the currently held item
   * @param heldItem
   * @return true if this is a speedy tool
   */
  public boolean setHeldItem(ItemStack heldItem)
  {
    SpeedyTool heldTool = (heldItem == null) ? null : toolTypeRegistry.get(heldItem.getItem());
    switchToTool(heldTool);
    return (heldTool != null);
  }

  /**
   * return true if a speedy tool is currently active
   * @return
   */
  public boolean toolIsActive() {return activeTool != null;}

  /** attempt to switch to the new tool
   *    (may fail if the tool is not ready to be deactivated)
   * @param newTool
   */
  private void switchToTool(SpeedyTool newTool)
  {
    if (newTool == activeTool) return;
    if (activeTool != null) {
      boolean deactivationComplete = activeTool.deactivateTool();
      if (!deactivationComplete) return;
    }
    if (newTool != null) newTool.activateTool();
    activeTool = newTool;
  }

  /**
   * update the currently selected tool's state based on the player selected items; where the player is looking; etc
   * To be called per render frame
   * No effect if not active.
   * @param world
   * @param player
   * @param partialTick
   * @return
   */
  public boolean updateForThisFrame(World world, EntityClientPlayerMP player, float partialTick)
  {
    if (activeTool != null) {
      activeTool.updateForThisFrame(world, player, partialTick);
    }
    return true;
  }

  /**
   * tick the active tool
   * @param world
   */
  public void performTick(World world)
  {
    if (activeTool != null) {
      activeTool.performTick(world);
    }
  }

  public void resetAllTools()
  {
    for (SpeedyTool tool : toolTypeRegistry.values()) {
      tool.resetTool();
    }
  }


  public boolean processUserInput(EntityClientPlayerMP player, float partialTick, UserInput userInput)
  {
    boolean inputProcessed = false;
    if (activeTool != null) {
       inputProcessed = activeTool.processUserInput(player, partialTick, userInput);
    }
    return inputProcessed;
  }

  public void registerToolType(Item item, SpeedyTool speedyTool)
  {
    if (toolTypeRegistry.containsKey(item)) throw new IllegalArgumentException("Duplicate item " + item + " in registerToolType");
    toolTypeRegistry.put(item, speedyTool);
  }

  SpeedyTool activeTool;

  private HashMap<Item, SpeedyTool> toolTypeRegistry;
}
