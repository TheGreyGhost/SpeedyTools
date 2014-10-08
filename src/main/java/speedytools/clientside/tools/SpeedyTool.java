package speedytools.clientside.tools;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.world.World;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.network.PacketSenderClient;
import speedytools.clientside.rendering.RendererWireframeSelection;
import speedytools.clientside.rendering.SpeedyToolRenderers;
import speedytools.clientside.sound.SoundController;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.items.ItemSpeedyTool;

/**
* User: The Grey Ghost
* Date: 18/04/2014
*/
public abstract class SpeedyTool
{
  public SpeedyTool(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SoundController i_speedyToolSounds,
                    UndoManagerClient i_undoManagerClient, PacketSenderClient i_packetSenderClient) {
    speedyToolRenderers = i_renderers;
    parentItem = i_parentItem;
    iAmActive = false;
    undoManagerClient = i_undoManagerClient;
    soundController = i_speedyToolSounds;
    packetSenderClient = i_packetSenderClient;
  }

  public abstract boolean activateTool();

  /** The user has unequipped this tool, deactivate it, stop any effects, etc
   * @return
   */
  public abstract boolean deactivateTool();

  /**
   * Process user input
   * no effect if the tool is not active.
   * @param userInput
   * @return
   */
  public abstract boolean processUserInput(EntityClientPlayerMP player, float partialTick, UserInput userInput);

  /**
   * update the tool state based on the player selected items; where the player is looking; etc
   * No effect if not active.
   * @param world
   * @param player
   * @param partialTick
   * @return
   */
  public abstract boolean updateForThisFrame(World world, EntityClientPlayerMP player, float partialTick);

  /**
   *  resets the tool state eg has a selection, moving selection, etc.
   */
  public abstract void resetTool();

  protected boolean iAmActive;
  protected SpeedyToolRenderers speedyToolRenderers;
  protected SoundController soundController;
  protected UndoManagerClient undoManagerClient;
  protected PacketSenderClient packetSenderClient;
  protected ItemSpeedyTool parentItem;
  protected boolean controlKeyIsDown;
  protected RendererWireframeSelection.WireframeRenderInfoUpdateLink wireframeRendererUpdateLink;

  public void performTick(World world) {}

}
