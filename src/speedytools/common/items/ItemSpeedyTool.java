package speedytools.common.items;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import speedytools.clientside.selections.BlockMultiSelector;
import speedytools.clientside.rendering.SelectionBoxRenderer;
import speedytools.common.blocks.BlockWithMetadata;
import speedytools.common.network.Packet250SpeedyToolUse;

import java.io.IOException;
import java.util.*;

/**
 * User: The Grey Ghost
 * Date: 2/11/13
 */
public abstract class ItemSpeedyTool extends Item
{
  public ItemSpeedyTool(int id) {
    super(id);
    setCreativeTab(CreativeTabs.tabTools);
    setMaxDamage(-1);                         // not damageable
  }

  /**
   * Finds the first block in the player's line of sight, including liquids
   * Has to be in here because getMovingObjectPositionFromPlayer is protected.
   * @param world
   * @param entityPlayer
   * @return the corresponding MovingObjectPosition
   */
  public MovingObjectPosition rayTraceLineOfSight(World world, EntityPlayer entityPlayer)
  {
    return this.getMovingObjectPositionFromPlayer(world, entityPlayer, true);
  }

}
