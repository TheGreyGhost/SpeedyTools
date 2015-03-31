package speedytools.clientside.tools;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.network.PacketSenderClient;
import speedytools.clientside.rendering.SpeedyToolRenderers;
import speedytools.clientside.selections.BlockMultiSelector;
import speedytools.clientside.sound.SoundController;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.items.ItemSpeedyTool;

/**
* Created by TheGreyGhost on 30/10/14.
* This tool type switches between simple and complex using the scroll wheel
*/
public class SpeedyToolSimpleAndComplex extends SpeedyTool
{
  public SpeedyToolSimpleAndComplex(SpeedyToolSimple i_simpleTool, SpeedyToolComplex i_complexTool,
          ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SoundController i_speedyToolSounds,
          UndoManagerClient i_undoManagerClient, PacketSenderClient i_packetSenderClient) {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient, i_packetSenderClient);
    speedyToolComplex = i_complexTool;
    speedyToolSimple = i_simpleTool;
    currentToolMode = ToolMode.SIMPLE;
  }

  @Override
  public boolean activateTool(ItemStack newToolItemStack) {
    currentToolItemStack = newToolItemStack;
    return (currentToolMode == ToolMode.SIMPLE) ? speedyToolSimple.activateTool(newToolItemStack) : speedyToolComplex.activateTool(newToolItemStack);
  }

  @Override
  public boolean deactivateTool() {
    return (currentToolMode == ToolMode.SIMPLE) ? speedyToolSimple.deactivateTool() : speedyToolComplex.deactivateTool();
  }

  @Override
  public boolean processUserInput(EntityPlayerSP player, float partialTick, UserInput userInput)
  {
    if (currentToolMode == ToolMode.SIMPLE) {
      boolean retval = speedyToolSimple.processUserInput(player, partialTick, userInput);
      if (!retval) return false;

//      System.out.println("simple CurrentItem:" + currentToolItemStack);
      if (parentItem.isInfiniteMode(currentToolItemStack)) {
        boolean deactivationComplete = speedyToolSimple.deactivateTool();
        if (!deactivationComplete) return true;
        speedyToolComplex.activateTool(currentToolItemStack);
        currentToolMode = ToolMode.COMPLEX;
      }
      return true;
    } else {
      boolean retval = speedyToolComplex.processUserInput(player, partialTick, userInput);
      if (!retval) return false;
//      System.out.println("complex CurrentItem:" + currentToolItemStack);
      if (!parentItem.isInfiniteMode(currentToolItemStack)) {
        boolean deactivationComplete = speedyToolComplex.deactivateTool();
        if (!deactivationComplete) return true;
        speedyToolSimple.activateTool(currentToolItemStack);
        currentToolMode = ToolMode.SIMPLE;
      }
      return true;
    }
  }

  @Override
  public boolean updateForThisFrame(World world, EntityPlayerSP player, float partialTick) {
    return (currentToolMode == ToolMode.SIMPLE) ?
            speedyToolSimple.updateForThisFrame(world, player, partialTick) :
            speedyToolComplex.updateForThisFrame(world, player, partialTick);
  }

  @Override
  public void resetTool() {
    speedyToolSimple.resetTool();
    speedyToolComplex.resetTool();
  }

  @Override
  public void performTick(World world)
  {
    if (currentToolMode == ToolMode.SIMPLE) {
      speedyToolSimple.performTick(world);
    } else {
      speedyToolComplex.performTick(world);
    }
  }

  /**
   * when selecting the first block in a selection, how should it be done?
   *
   * @return
   */
  @Override
  protected BlockMultiSelector.BlockSelectionBehaviour getBlockSelectionBehaviour() {
    return (currentToolMode == ToolMode.SIMPLE) ? speedyToolSimple.getBlockSelectionBehaviour() : speedyToolComplex.getBlockSelectionBehaviour();
  }

  private enum ToolMode {SIMPLE, COMPLEX}
  private ToolMode currentToolMode;
  private SpeedyToolSimple speedyToolSimple;
  private SpeedyToolComplex speedyToolComplex;
}
