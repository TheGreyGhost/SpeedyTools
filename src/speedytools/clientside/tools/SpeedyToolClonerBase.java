package speedytools.clientside.tools;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.rendering.RendererBoundaryField;
import speedytools.clientside.rendering.SpeedyToolRenderers;
import speedytools.clientside.rendering.SpeedyToolSounds;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.items.ItemSpeedyTool;

/**
 * User: The Grey Ghost
 * Date: 18/04/2014
 */
public class SpeedyToolClonerBase extends SpeedyTool
{
  public SpeedyToolClonerBase(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SpeedyToolSounds i_speedyToolSounds, UndoManagerClient i_undoManagerClient) {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient);
  }

  @Override
  public boolean activateTool() {
    return false;
  }

  @Override
  public boolean deactivateTool() {
    return false;
  }

  @Override
  public boolean processUserInput(EntityClientPlayerMP player, float partialTick, UserInput userInput) {
    return false;
  }

  @Override
  public boolean update(World world, EntityClientPlayerMP player, float partialTick) {
    return false;
  }

  /**
   * This class is used to provide information to the Boundary Field Renderer when it needs it:
   * The Renderer calls refreshRenderInfo, which copies the relevant information from the tool.
   */
  public class BoundaryFieldRendererUpdateLink implements RendererBoundaryField.BoundaryFieldRenderInfoUpdateLink
  {
    @Override
    public boolean refreshRenderInfo(RendererBoundaryField.BoundaryFieldRenderInfo infoToUpdate, Vec3 playerPosition)
    {
      infoToUpdate.currentlySelectedBlocks = currentlySelectedBlocks;
      return true;
    }
  }

  protected BoundaryFieldRendererUpdateLink boundaryFieldRendererUpdateLink;
}
