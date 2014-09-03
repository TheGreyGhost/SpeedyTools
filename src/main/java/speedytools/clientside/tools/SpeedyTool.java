package speedytools.clientside.tools;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.rendering.*;
import speedytools.clientside.selections.BlockMultiSelector;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.items.ItemSpeedyTool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
* User: The Grey Ghost
* Date: 18/04/2014
*/
public abstract class SpeedyTool
{
  public SpeedyTool(ItemSpeedyTool i_parentItem, SpeedyToolRenderers i_renderers, SpeedyToolSounds i_speedyToolSounds, UndoManagerClient i_undoManagerClient) {
    speedyToolRenderers = i_renderers;
    parentItem = i_parentItem;
    iAmActive = false;
    undoManagerClient = i_undoManagerClient;
    speedyToolSounds = i_speedyToolSounds;
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
  protected SpeedyToolSounds speedyToolSounds;
  protected UndoManagerClient undoManagerClient;
  protected ItemSpeedyTool parentItem;
  protected boolean controlKeyIsDown;
  protected RendererWireframeSelection.WireframeRenderInfoUpdateLink wireframeRendererUpdateLink;

  public void performTick(World world) {}

}
