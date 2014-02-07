package speedytools.common.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import speedytools.clientonly.SelectionBoxRenderer;

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
   * @return the corresponding MovingObjectPosition, null if none
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
   *
   * @param target the position of the cursor
   * @param player the player
   * @param currentItem the current item that the player is holding.  MUST be derived from ItemSpeedyTool.
   * @param partialTick partial tick time.
   * @return returns the coordinates of the block selected, or null if none
   */
  @SideOnly(Side.CLIENT)
  public ChunkCoordinates selectBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, float partialTick)
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
   * @param thePlayer
   */
  @SideOnly(Side.CLIENT)
  public static void attackButtonClicked(EntityClientPlayerMP thePlayer)
  {
    boolean success = buttonClicked(thePlayer, 0);
    if (success) {
      ItemCloneTool currentTool = (ItemCloneTool)currentlySelectedTool;
      Minecraft.getMinecraft().sndManager.playSound(currentTool.getPlaceSound(),
              (float) (thePlayer.posX),
              (float) (thePlayer.posY),
              (float) (thePlayer.posZ),
              1.0F, 1.0F);
    }
  }

  /**
   * called when the user presses the use button (right mouse)
   * @param thePlayer
   */
  @SideOnly(Side.CLIENT)
  public static void useButtonClicked(EntityClientPlayerMP thePlayer)
  {
    boolean success = buttonClicked(thePlayer, 1);
    if (success) {
      ItemCloneTool currentTool = (ItemCloneTool)currentlySelectedTool;
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
   *
   *
   * @param thePlayer
   * @param whichButton 0 = left (undo), 1 = right (use)
   * @return true for success
   */
  @SideOnly(Side.CLIENT)
  public static boolean buttonClicked(EntityClientPlayerMP thePlayer, int whichButton)
  {
    if (currentlySelectedTool == null || !(currentlySelectedTool instanceof ItemCloneTool)) return false;
    ItemCloneTool currentTool = (ItemCloneTool)currentlySelectedTool;

    return currentTool.actOnButtonClicked(thePlayer, whichButton);
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
  public boolean actOnButtonClicked(EntityClientPlayerMP thePlayer, int whichButton)
  {
    return false;
  }

  public final int SELECTION_BOX_STYLE = 0; //0 = cube, 1 = cube with cross on each side

  /**
   * renders the selection box
   * @param player
   * @param partialTick
   */
  public void renderSelection(EntityPlayer player, float partialTick)
  {
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
    GL11.glLineWidth(2.0F);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDepthMask(false);
    double expandDistance = 0.002F;

    double playerOriginX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTick;
    double playerOriginY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTick;
    double playerOriginZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTick;

    AxisAlignedBB boundingBox = AxisAlignedBB.getAABBPool().getAABB(currentlySelectedBlock.posX, currentlySelectedBlock.posY, currentlySelectedBlock.posZ,
                                                                    currentlySelectedBlock.posX+1, currentlySelectedBlock.posY+1, currentlySelectedBlock.posZ+1);
    boundingBox = boundingBox.expand(expandDistance, expandDistance, expandDistance).getOffsetBoundingBox(-playerOriginX, -playerOriginY, -playerOriginZ);
    switch (SELECTION_BOX_STYLE) {
      case 0: {
        SelectionBoxRenderer.drawCube(boundingBox);
        break;
      }
      case 1: {
        SelectionBoxRenderer.drawFilledCube(boundingBox);
        break;
      }
    }

    ChunkCoordinates closestCorner = getClosestBoundaryCorner(currentlySelectedBlock.posX + 0.5F, currentlySelectedBlock.posY + 0.5F, currentlySelectedBlock.posZ + 0.5F);

    SelectionBoxRenderer.drawConnectingLine(closestCorner.posX + 0.5F, closestCorner.posY + 0.5F, closestCorner.posZ + 0.5F,
                                            currentlySelectedBlock.posX + 0.5F, currentlySelectedBlock.posY + 0.5F, currentlySelectedBlock.posZ + 0.5F
                                           );
    GL11.glDepthMask(true);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
  }

  /**
   * Find the corner of the selection box (defined by the boundary corners) that is closest to the designated point.
   * @return the closest x, closest y, and closest z coordinates, or null if either of the two corners are null
   */
  protected ChunkCoordinates getClosestBoundaryCorner(float pointX, float pointY, float pointZ)
  {
    if (boundaryCorner1 == null || boundaryCorner2 == null) return null;

    int wxNearest = boundaryCorner1.posX;
    if (Math.abs(wxNearest + 0.5F - pointX) > Math.abs((float)boundaryCorner2.posX + 0.5F - pointX)) {
      wxNearest = boundaryCorner2.posX;
    }
    int wyNearest = boundaryCorner1.posY;
    if (Math.abs(wyNearest + 0.5F - pointY) > Math.abs((float)boundaryCorner2.posY + 0.5F - pointY)) {
      wyNearest = boundaryCorner2.posY;
    }
    int wzNearest = boundaryCorner1.posZ;
    if (Math.abs(wzNearest + 0.5F - pointZ) < Math.abs((float)boundaryCorner2.posZ + 0.5F - pointZ)) {
      wzNearest = boundaryCorner2.posZ;
    }

    return new ChunkCoordinates(wxNearest, wyNearest, wzNearest);

  }

  /**
   * render the boundary field if there is one selected
   * @param player
   * @param partialTick
   */
  public void renderBoundaryField(EntityPlayer player, float partialTick) {
    if (boundaryCorner1 == null && boundaryCorner2 == null) return;

    ChunkCoordinates cnr1 = (boundaryCorner1 == null) ? boundaryCorner2 : boundaryCorner1;
    ChunkCoordinates cnr2 = (boundaryCorner2 == null) ? boundaryCorner1 : boundaryCorner2;

    int wxmin = Math.min(cnr1.posX, cnr2.posX);
    int wymin = Math.min(cnr1.posY, cnr2.posY);
    int wzmin = Math.min(cnr1.posZ, cnr2.posZ);
    int wxmax = Math.max(cnr1.posX, cnr2.posX);
    int wymax = Math.max(cnr1.posY, cnr2.posY);
    int wzmax = Math.max(cnr1.posZ, cnr2.posZ);

    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
    GL11.glLineWidth(2.0F);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDepthMask(false);
    double expandDistance = 0.002F;

    double playerOriginX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTick;
    double playerOriginY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTick;
    double playerOriginZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTick;

    AxisAlignedBB boundingBox = AxisAlignedBB.getAABBPool().getAABB(wxmin, wymin, wzmin, wxmax+1, wymax+1, wzmax+1);
    boundingBox = boundingBox.expand(expandDistance, expandDistance, expandDistance).getOffsetBoundingBox(-playerOriginX, -playerOriginY, -playerOriginZ);
    SelectionBoxRenderer.drawFilledCube(boundingBox);

    GL11.glDepthMask(true);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
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
