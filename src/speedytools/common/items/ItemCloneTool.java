package speedytools.common.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.util.*;

/**
 * User: The Grey Ghost
 * Date: 2/11/13
 */
public abstract class ItemCloneTool extends Item
{
  public ItemCloneTool(int id) {
    super(id);
    setCreativeTab(CreativeTabs.tabTools);
    setMaxDamage(-1);                         // not damageable
  }

  public static boolean isAcloneTool(int itemID)
  {
    return (   itemID == RegistryForItems.itemCloneBoundary.itemID);
  }

  /**
   * Finds the first block in the player's line of sight, including liquids
   * @param world
   * @param entityPlayer
   * @return the corresponding MovingObjectPosition
   */
  @SideOnly(Side.CLIENT)
  public MovingObjectPosition rayTraceLineOfSight(World world, EntityPlayer entityPlayer)
  {
    return this.getMovingObjectPositionFromPlayer(world, entityPlayer, true);
  }

  /**
   * Selects the Block that will be affected by the tool when the player presses right-click
   *   default method just selects the first block as per vanilla
   *
   * @param target the position of the cursor
   * @param player the player
   * @param currentItem the current item that the player is holding.  MUST be derived from ItemSpeedyTool.
   * @param itemStackToPlace the item that would be placed in the selection
   * @param partialTick partial tick time.
   * @return returns the coordinates of the block selected, or null if none
   */
  @SideOnly(Side.CLIENT)
  public ChunkCoordinates selectBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, ItemStack itemStackToPlace, float partialTick)
  {
    if (target == null) return null;
    ChunkCoordinates startBlockCoordinates = new ChunkCoordinates(target.blockX, target.blockY, target.blockZ);
    return startBlockCoordinates;
  }

  /**
   * Sets the current selection for the currently-held clone tool
   * @param currentTool the currently-held clone tool
   * @param currentSelection the currently selected block, or null if none
   */
  @SideOnly(Side.CLIENT)
  public static void setCurrentToolSelection(Item currentTool, ChunkCoordinates currentSelection)
  {
    currentlySelectedTool = currentTool;
    currentlySelectedBlock = currentSelection;
  }

  /**
   * called when the user presses the attackButton (Left Mouse)
   */
  @SideOnly(Side.CLIENT)
  public static void attackButtonClicked()
  {
    boolean success = buttonClicked(0);
    if (success) {
      ItemCloneTool currentTool = (ItemCloneTool)currentlySelectedTool;
      EntityClientPlayerMP thePlayer = Minecraft.getMinecraft().thePlayer;
      Minecraft.getMinecraft().sndManager.playSound(currentTool.getPlaceSound(),
              (float) (thePlayer.posX),
              (float) (thePlayer.posY),
              (float) (thePlayer.posZ),
              1.0F, 1.0F);
    }
  }

  /**
   * called when the user presses the use button (right mouse)
   */
  @SideOnly(Side.CLIENT)
  public static void useButtonClicked()
  {
    boolean success = buttonClicked(1);
    if (success) {
      ItemCloneTool currentTool = (ItemCloneTool)currentlySelectedTool;
      EntityClientPlayerMP thePlayer = Minecraft.getMinecraft().thePlayer;
      Minecraft.getMinecraft().sndManager.playSound(currentTool.getUnPlaceSound(),
              (float) (thePlayer.posX),
              (float) (thePlayer.posY),
              (float) (thePlayer.posZ),
              1.0F, 1.0F);
    }
  }

  /**
   * called when the user scrolls the mouse wheel.
   * @param delta the delta (see Mouse.getDWheel() )
   */
  @SideOnly(Side.CLIENT)
  public static void mouseWheelMoved(int delta)
  {
    int MOUSE_DELTA_PER_SLOT = 120;

    // by default - do nothing
  }

  /**
   * Inform the server of the player's action with the SpeedyTool.  Checks to make sure that currentlySelectedTool is valid, and if right click then currentlySelectedBlock has at least one entry.
   * @param whichButton 0 = left (undo), 1 = right (use)
   * @return true for success
   */
  @SideOnly(Side.CLIENT)
  public static boolean buttonClicked(int whichButton)
  {
    if (currentlySelectedTool == null || !(currentlySelectedTool instanceof ItemCloneTool)) return false;
    ItemCloneTool currentTool = (ItemCloneTool)currentlySelectedTool;

    return currentTool.actOnButtonClicked(whichButton);
    /*
    Packet250SpeedyToolUse packet;
    try {
      packet = new Packet250SpeedyToolUse(currentlySelectedTool.itemID, whichButton, currentBlockToPlace, currentlySelectedBlock);
    } catch (IOException e) {
      Minecraft.getMinecraft().getLogAgent().logWarning("Could not create Packet250SpeedyToolUse for itemID " + currentlySelectedTool.itemID);
      return false;
    }
    PacketDispatcher.sendPacketToServer(packet.getPacket250CustomPayload());
    return true;
    */
  }

  @SideOnly(Side.CLIENT)
  public boolean actOnButtonClicked(int whichButton)
  {
    return false;
  }

  protected String getPlaceSound() {return "";}

  protected String getUnPlaceSound() {return "";}

    // these keep track of the currently selected block, for when the tool is used
  protected static ChunkCoordinates currentlySelectedBlock = null;
  protected static Item currentlySelectedTool = null;
  protected static Deque<ItemCloneTool> undoSoundsHistory = new LinkedList<ItemCloneTool>();

  protected static ChunkCoordinates boundaryCorner1 = null;
  protected static ChunkCoordinates boundaryCorner2 = null;
}
