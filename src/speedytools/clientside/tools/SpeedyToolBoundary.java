package speedytools.clientside.tools;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.rendering.*;
import speedytools.clientside.selections.BlockMultiSelector;
import speedytools.clientside.userinput.UserInput;
import speedytools.common.items.ItemCloneBoundary;
import speedytools.common.items.ItemSpeedyTool;
import speedytools.common.utilities.UsefulConstants;
import speedytools.common.utilities.UsefulFunctions;

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
    rendererElements.add(new RendererWireframeSelection(wireframeRendererUpdateLink));
    rendererElements.add(new RendererBoundaryField(boundaryFieldRendererUpdateLink));
    speedyToolRenderers.setRenderers(rendererElements);
    iAmActive = true;
    return true;
  }

  /** The user has unequipped this tool, deactivate it, stop any effects, etc
   * @return
   */
  @Override
  public boolean deactivateTool() {
    speedyToolRenderers.setRenderers(null);
    iAmActive = false;
    return true;
  }

  @Override
  public boolean processUserInput(EntityClientPlayerMP player, float partialTick, UserInput userInput) {
    if (!iAmActive) return false;

    controlKeyIsDown = userInput.isControlKeyDown();

    UserInput.InputEvent nextEvent;
    while (null != (nextEvent = userInput.poll())) {
      switch (nextEvent.eventType) {
        case LEFT_CLICK_DOWN: {
          boundaryCorner1 = null;
          boundaryCorner2 = null;
          speedyToolSounds.playSound(SpeedySoundTypes.BOUNDARY_UNPLACE, player.getPosition(partialTick));
          break;
        }
        case RIGHT_CLICK_DOWN: {
          doRightClick(player, partialTick);
          break;
        }
      }
    }
    return true;
  }

  /**
   * place corner blocks; or if already both placed - grab / ungrab one of the faces.
   * @param player
   * @param partialTick
   */
  protected void doRightClick(EntityClientPlayerMP player, float partialTick)
  {
    if (boundaryCorner1 == null) {
      if (currentlySelectedBlock == null) return;
      boundaryCorner1 = new ChunkCoordinates(currentlySelectedBlock);
      speedyToolSounds.playSound(SpeedySoundTypes.BOUNDARY_PLACE_1ST, player.getPosition(partialTick));
    } else if (boundaryCorner2 == null) {
      if (currentlySelectedBlock == null) return;
      addCornerPointWithMaxSize(currentlySelectedBlock);
      speedyToolSounds.playSound(SpeedySoundTypes.BOUNDARY_PLACE_2ND, player.getPosition(partialTick));
    } else {
      if (boundaryGrabActivated) {  // ungrab
        Vec3 playerPosition = player.getPosition(partialTick);
        AxisAlignedBB newBoundaryField = getGrabDraggedBoundaryField(playerPosition);
        boundaryCorner1.posX = (int)Math.round(newBoundaryField.minX);
        boundaryCorner1.posY = (int)Math.round(newBoundaryField.minY);
        boundaryCorner1.posZ = (int)Math.round(newBoundaryField.minZ);
        boundaryCorner2.posX = (int)Math.round(newBoundaryField.maxX - 1);
        boundaryCorner2.posY = (int)Math.round(newBoundaryField.maxY - 1);
        boundaryCorner2.posZ = (int)Math.round(newBoundaryField.maxZ - 1);
        boundaryGrabActivated = false;
        speedyToolSounds.playSound(SpeedySoundTypes.BOUNDARY_UNGRAB, playerPosition);
      } else {
        MovingObjectPosition highlightedFace = boundaryFieldFaceSelection(player); //todo: is this needed: (Minecraft.getMinecraft().renderViewEntity);
        if (highlightedFace == null) return;

        boundaryGrabActivated = true;
        boundaryGrabSide = highlightedFace.sideHit;
        Vec3 playerPosition = player.getPosition(1.0F);
        boundaryGrabPoint = Vec3.createVectorHelper(playerPosition.xCoord, playerPosition.yCoord, playerPosition.zCoord);
        speedyToolSounds.playSound(SpeedySoundTypes.BOUNDARY_GRAB, playerPosition);
      }
    }
  }
  /**
   * Update the selection or boundary field based on where the player is looking
   * @param world
   * @param player
   * @param partialTick
   * @return
   */
  @Override
  public boolean update(World world, EntityClientPlayerMP player, float partialTick) {
    // update icon renderer

    ItemCloneBoundary.IconNames itemIcon = ItemCloneBoundary.IconNames.BLANK;

    if (boundaryCorner1 == null && boundaryCorner2 == null) {
      itemIcon = ItemCloneBoundary.IconNames.NONE_PLACED;
    } else if (boundaryCorner1 == null || boundaryCorner2 == null) {
      itemIcon = ItemCloneBoundary.IconNames.ONE_PLACED;
    } else {
      if (boundaryGrabActivated) {
        itemIcon = ItemCloneBoundary.IconNames.GRABBING;
      } else {
        itemIcon = ItemCloneBoundary.IconNames.TWO_PLACED;
      }
    }
    itemCloneBoundary.setCurrentIcon(itemIcon);

    // if boundary field active: calculate the face where the cursor is
    if (boundaryCorner1 != null  && boundaryCorner2 != null) {
      MovingObjectPosition highlightedFace = boundaryFieldFaceSelection(player);
      boundaryCursorSide = (highlightedFace != null) ? highlightedFace.sideHit : UsefulConstants.FACE_NONE;
      return true;
    }

    // choose a starting block
    currentlySelectedBlock = null;
    MovingObjectPosition airSelectionIgnoringBlocks = BlockMultiSelector.selectStartingBlock(null, player, partialTick);
    if (airSelectionIgnoringBlocks == null) return false;

    MovingObjectPosition target = itemCloneBoundary.rayTraceLineOfSight(player.worldObj, player);

    // we want to make sure that we only select a block at very short range.  So if we have hit a block beyond this range, shorten the target to eliminate it
    if (target == null) {
      target = airSelectionIgnoringBlocks;
    } else if (target.typeOfHit == EnumMovingObjectType.TILE) {
      if (target.hitVec.dotProduct(target.hitVec) > airSelectionIgnoringBlocks.hitVec.dotProduct(airSelectionIgnoringBlocks.hitVec)) {
        target = airSelectionIgnoringBlocks;
      }
    }

    currentlySelectedBlock = new ChunkCoordinates(target.blockX, target.blockY, target.blockZ);
    return true;
  }

  /**
   * add a new corner to the boundary (replace boundaryCorner2).  If the selection is too big, move boundaryCorner1.
   * @param newCorner
   */
  private void addCornerPointWithMaxSize(ChunkCoordinates newCorner)
  {
    boundaryCorner2 = new ChunkCoordinates(newCorner);
    boundaryCorner1.posX = UsefulFunctions.clipToRange(boundaryCorner1.posX, newCorner.posX - SELECTION_MAX_XSIZE + 1, newCorner.posX + SELECTION_MAX_XSIZE - 1);
    boundaryCorner1.posY = UsefulFunctions.clipToRange(boundaryCorner1.posY, newCorner.posY - SELECTION_MAX_YSIZE + 1, newCorner.posY + SELECTION_MAX_YSIZE - 1);
    boundaryCorner1.posZ = UsefulFunctions.clipToRange(boundaryCorner1.posZ, newCorner.posZ - SELECTION_MAX_ZSIZE + 1, newCorner.posZ + SELECTION_MAX_ZSIZE - 1);
    sortBoundaryFieldCorners();
  }

  private ItemCloneBoundary itemCloneBoundary;
}
