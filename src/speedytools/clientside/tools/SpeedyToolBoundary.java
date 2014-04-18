package speedytools.clientside.tools;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.world.World;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.rendering.*;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.items.ItemCloneBoundary;
import speedytools.common.items.ItemSpeedyTool;

import java.util.LinkedList;

/**
 * User: The Grey Ghost
 * Date: 18/04/2014
 */
public class SpeedyToolBoundary extends SpeedyToolClonerBase
{
  public SpeedyToolBoundary(ItemCloneBoundary i_parentItem, SpeedyToolRenderers i_renderers, SpeedyToolSounds i_speedyToolSounds, UndoManagerClient i_undoManagerClient) {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient);
    itemCloneBoundary = i_parentItem;
    boundaryFieldRendererUpdateLink = this.new BoundaryFieldRendererUpdateLink();
  }

  @Override
  public boolean activateTool() {
    LinkedList<RendererElement> rendererElements = new LinkedList<RendererElement>();
    rendererElements.add(new RendererBoundaryField(boundaryFieldRendererUpdateLink));
    speedyToolRenderers.setRenderers(rendererElements);
    iAmActive = true;
    return true;
  }

  @Override
  public boolean deactivateTool() {
    return super.deactivateTool();
  }

  @Override
  public boolean processUserInput(EntityClientPlayerMP player, float partialTick, UserInput userInput) {
    return super.processUserInput(player, partialTick, userInput);
  }

  @Override
  public boolean update(World world, EntityClientPlayerMP player, float partialTick) {
    return super.update(world, player, partialTick);
  }

  private ItemCloneBoundary itemCloneBoundary;
}
