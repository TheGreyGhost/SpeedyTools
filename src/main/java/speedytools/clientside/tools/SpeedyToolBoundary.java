//package speedytools.clientside.tools;
//
//import net.minecraft.client.entity.EntityPlayerSP;
//import net.minecraftforge.fml.common.FMLLog;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.entity.EntityClientPlayerMP;
//import net.minecraft.item.ItemStack;
//import net.minecraft.util.*;
//import net.minecraft.world.World;
//import speedytools.clientside.UndoManagerClient;
//import speedytools.clientside.network.PacketSenderClient;
//import speedytools.clientside.rendering.*;
//import speedytools.clientside.selections.BlockMultiSelector;
//import speedytools.clientside.sound.SoundController;
//import speedytools.clientside.sound.SoundEffectBoundaryHum;
//import speedytools.clientside.sound.SoundEffectNames;
//import speedytools.clientside.sound.SoundEffectSimple;
//import speedytools.clientside.userinput.UserInput;
//import speedytools.common.items.ItemSpeedyBoundary;
//import speedytools.common.items.ItemSpeedyTool;
//import speedytools.common.utilities.UsefulConstants;
//import speedytools.common.utilities.UsefulFunctions;
//
//import java.util.ArrayList;
//import java.util.LinkedList;
//
///**
//* User: The Grey Ghost
//* Date: 18/04/2014
//*/
//public class SpeedyToolBoundary extends SpeedyToolComplexBase
//{
//  public SpeedyToolBoundary(ItemSpeedyBoundary i_parentItem, SpeedyToolRenderers i_renderers, SoundController i_speedyToolSounds,
//                            UndoManagerClient i_undoManagerClient, PacketSenderClient i_packetSenderClient) {
//    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient, i_packetSenderClient);
//    itemSpeedyBoundary = i_parentItem;
////    wireframeRendererUpdateLink = this.new BoundaryToolWireframeRendererLink();  todo uncomment
////    boundaryFieldRendererUpdateLink = this.new BoundaryFieldRendererUpdateLink();
//  }
//
//  @Override
//  public boolean activateTool(ItemStack newToolItemStack) {
//    currentToolItemStack = newToolItemStack;
//    if (soundEffectBoundaryHum == null) {
//      BoundaryHumLink boundaryHumLink = this.new BoundaryHumLink();
//      soundEffectBoundaryHum = new SoundEffectBoundaryHum(SoundEffectNames.BOUNDARY_HUM, soundController, boundaryHumLink);
//    }
//    soundEffectBoundaryHum.startPlayingLoop();
//    LinkedList<RendererElement> rendererElements = new LinkedList<RendererElement>();
////    rendererElements.add(new RendererWireframeSelection(wireframeRendererUpdateLink));    todo uncomment
////    rendererElements.add(new RendererBoundaryField(boundaryFieldRendererUpdateLink));
//    speedyToolRenderers.setRenderers(rendererElements);
//    iAmActive = true;
//    return true;
//  }
//
//  /** The user has unequipped this tool, deactivate it, stop any effects, etc
//   * @return
//   */
//  @Override
//  public boolean deactivateTool() {
//    speedyToolRenderers.setRenderers(null);
//    if (soundEffectBoundaryHum != null) {
//      soundEffectBoundaryHum.stopPlaying();
//    }
//    iAmActive = false;
//    return true;
//  }
//
//  /** called once per tick on the client side while the user is holding an ItemCloneTool
//   * used to:
//   * (1) background generation of a selection, if it has been initiated
//   * (2) start transmission of the selection, if it has just been completed
//   * (3) acknowledge (get) the action and undo statuses
//   */
//  @Override
//  public void performTick(World world) {
//    updateGrabRenderTick(boundaryGrabActivated);
//  }
//
////  /**
////   * when selecting the first block in a selection, how should it be done?
////   *
////   * @return
////   */
////  @Override
////  protected BlockMultiSelector.BlockSelectionBehaviour getBlockSelectionBehaviour() {
////    return BlockMultiSelector.BlockSelectionBehaviour.BOUNDARY_STYLE;
////  } todo uncomment
//
//  @Override
//  public boolean processUserInput(EntityPlayerSP player, float partialTick, UserInput userInput) {
//    if (!iAmActive) return false;
//
//    controlKeyIsDown = userInput.isControlKeyDown();
//
//    UserInput.InputEvent nextEvent;
//    while (null != (nextEvent = userInput.poll())) {
//      switch (nextEvent.eventType) {
//        case LEFT_CLICK_DOWN: {
//          boundaryCorner1 = null;
//          boundaryCorner2 = null;
//          SoundEffectSimple soundEffectSimple = new SoundEffectSimple(SoundEffectNames.BOUNDARY_UNPLACE, soundController);
//          soundEffectSimple.startPlaying();
//          break;
//        }
//        case RIGHT_CLICK_DOWN: {
//          doRightClick(player, partialTick);
//          break;
//        }
//      }
//    }
//    return true;
//  }
//
//  /**
//   * place corner blocks; or if already both placed - grab / ungrab one of the faces.
//   * @param player
//   * @param partialTick
//   */
//  protected void doRightClick(EntityPlayerSP player, float partialTick)
//  {
//    if (boundaryCorner1 == null) {
//      if (blockUnderCursor == null) return;
//      boundaryCorner1 = new BlockPos(blockUnderCursor);
//      SoundEffectSimple soundEffectSimple = new SoundEffectSimple(SoundEffectNames.BOUNDARY_PLACE_1ST, soundController);
//      soundEffectSimple.startPlaying();
//    } else if (boundaryCorner2 == null) {
//      if (blockUnderCursor == null) return;
//      addCornerPointWithMaxSize(blockUnderCursor);
//      SoundEffectSimple soundEffectSimple = new SoundEffectSimple(SoundEffectNames.BOUNDARY_PLACE_2ND, soundController);
//      soundEffectSimple.startPlaying();
//    } else {
//      if (boundaryGrabActivated) {  // ungrab
//        Vec3 playerPosition = player.getPosition(partialTick);
//        AxisAlignedBB newBoundaryField = getGrabDraggedBoundaryField(playerPosition);
//        boundaryCorner1.setY .posX = (int)Math.round(newBoundaryField.minX);
//        boundaryCorner1.posY = (int)Math.round(newBoundaryField.minY);
//        boundaryCorner1.posZ = (int)Math.round(newBoundaryField.minZ);
//        boundaryCorner2.posX = (int)Math.round(newBoundaryField.maxX - 1);
//        boundaryCorner2.posY = (int)Math.round(newBoundaryField.maxY - 1);
//        boundaryCorner2.posZ = (int)Math.round(newBoundaryField.maxZ - 1);
//        boundaryGrabActivated = false;
//        SoundEffectSimple soundEffectSimple = new SoundEffectSimple(SoundEffectNames.BOUNDARY_UNGRAB, soundController);
//        soundEffectSimple.startPlaying();
//      } else {
//        MovingObjectPosition highlightedFace = boundaryFieldFaceSelection(player);
//        if (highlightedFace == null) return;
//
//        boundaryGrabActivated = true;
//        boundaryGrabSide = highlightedFace.sideHit;
//        Vec3 playerPosition = player.getPosition(1.0F);
//        boundaryGrabPoint = Vec3.createVectorHelper(playerPosition.xCoord, playerPosition.yCoord, playerPosition.zCoord);
//        SoundEffectSimple soundEffectSimple = new SoundEffectSimple(SoundEffectNames.BOUNDARY_GRAB, soundController);
//        soundEffectSimple.startPlaying();
//      }
//    }
//  }
//  /**
//   * Update the selection or boundary field based on where the player is looking
//   * @param world
//   * @param player
//   * @param partialTick
//   * @return
//   */
//  @Override
//  public boolean updateForThisFrame(World world, EntityPlayerSP player, float partialTick) {
//    // update icon renderer
//
//    ItemSpeedyBoundary.IconNames itemIcon = ItemSpeedyBoundary.IconNames.BLANK;
//
//    if (boundaryCorner1 == null && boundaryCorner2 == null) {
//      itemIcon = ItemSpeedyBoundary.IconNames.NONE_PLACED;
//    } else if (boundaryCorner1 == null || boundaryCorner2 == null) {
//      itemIcon = ItemSpeedyBoundary.IconNames.ONE_PLACED;
//    } else {
//      if (boundaryGrabActivated) {
//        itemIcon = ItemSpeedyBoundary.IconNames.GRABBING;
//      } else {
//        itemIcon = ItemSpeedyBoundary.IconNames.TWO_PLACED;
//      }
//    }
//    itemSpeedyBoundary.setCurrentIcon(itemIcon);
//
//    if (boundaryCorner1 == null || boundaryCorner2 == null) {
//      boundaryGrabActivated = false;
//    }
//
//    // if boundary field active: calculate the face where the cursor is
//    if (boundaryCorner1 != null  && boundaryCorner2 != null) {
//      MovingObjectPosition highlightedFace = boundaryFieldFaceSelection(player);
//      boundaryCursorSide = (highlightedFace != null) ? highlightedFace.sideHit : UsefulConstants.FACE_NONE;
//      return true;
//    }
//
//    // choose a starting block
//    blockUnderCursor = null;
//    BlockMultiSelector.BlockSelectionBehaviour blockSelectionBehaviour = getBlockSelectionBehaviour();
//    MovingObjectPosition airSelectionIgnoringBlocks = BlockMultiSelector.selectStartingBlock(null, blockSelectionBehaviour, player, partialTick);
//    if (airSelectionIgnoringBlocks == null) return false;
//
//    ItemSpeedyTool.CollideWithLiquids collideWithLiquids= blockSelectionBehaviour.isWaterCollision() ? ItemSpeedyTool.CollideWithLiquids.COLLIDE_WITH_LIQUIDS : ItemSpeedyTool.CollideWithLiquids.DO_NOT_COLLIDE_WITH_LIQUIDS;
//    MovingObjectPosition target = itemSpeedyBoundary.rayTraceLineOfSight(player.worldObj, player, collideWithLiquids);
//
//    // we want to make sure that we only select a block at very short range.  So if we have hit a block beyond this range, shorten the target to eliminate it
//    if (target == null) {
//      target = airSelectionIgnoringBlocks;
//    } else if (target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
//      if (target.hitVec.dotProduct(target.hitVec) > airSelectionIgnoringBlocks.hitVec.dotProduct(airSelectionIgnoringBlocks.hitVec)) {
//        target = airSelectionIgnoringBlocks;
//      }
//    }
//
//    blockUnderCursor = new BlockPos(target.blockX, target.blockY, target.blockZ);
//    return true;
//  }
//
//  /**
//   * This class is used to provide information to the Boundary Field Renderer when it needs it:
//   * The Renderer calls refreshRenderInfo, which copies the relevant information from the tool.
//   */
//  public class BoundaryFieldRendererUpdateLink implements RendererBoundaryField.BoundaryFieldRenderInfoUpdateLink
//  {
//    @Override
//    public boolean refreshRenderInfo(RendererBoundaryField.BoundaryFieldRenderInfo infoToUpdate, Vec3 playerPosition)
//    {
//      if (boundaryCorner1 == null && boundaryCorner2 == null) return false;
//      infoToUpdate.boundaryCursorSide = boundaryCursorSide;
//      infoToUpdate.boundaryGrabActivated = boundaryGrabActivated;
//      infoToUpdate.boundaryGrabSide = boundaryGrabSide;
//      infoToUpdate.boundaryFieldAABB = getGrabDraggedBoundaryField(playerPosition);
//      return true;
//    }
//  }
//
//  /**
//   * This class is used to provide information to the WireFrame Renderer when it needs it:
//   * The Renderer calls refreshRenderInfo, which copies the relevant information from the tool.
//   * Draws a wireframe selection, provided that not both of the corners have been placed yet
//   */
//  public class BoundaryToolWireframeRendererLink implements RendererWireframeSelection.WireframeRenderInfoUpdateLink
//  {
//    @Override
//    public boolean refreshRenderInfo(RendererWireframeSelection.WireframeRenderInfo infoToUpdate)
//    {
//      if (boundaryCorner1 != null && boundaryCorner2 != null) return false;
//      infoToUpdate.currentlySelectedBlocks = new ArrayList<BlockPos>(1);
//      infoToUpdate.currentlySelectedBlocks.add(blockUnderCursor);
//      return true;
//    }
//  }
//
//  /** find the closest part of the boundary field (the epicentre), calculate the distance to it.
//   */
//  private class BoundaryHumLink implements SoundEffectBoundaryHum.BoundaryHumUpdateLink
//  {
//    @Override
//    public boolean refreshHumInfo(SoundEffectBoundaryHum.BoundaryHumInfo infoToUpdate) {
//      EntityClientPlayerMP entityClientPlayerMP = Minecraft.getMinecraft().thePlayer;
//
//      AxisAlignedBB boundaryFieldAABB = getBoundaryField();
//      if (boundaryFieldAABB == null) return false;
//
//      Vec3 playerPosition = entityClientPlayerMP.getPosition(0);
//      Vec3 epicentre = Vec3.createVectorHelper((boundaryFieldAABB.minX + boundaryFieldAABB.maxX) / 2.0,
//              (boundaryFieldAABB.minY + boundaryFieldAABB.maxY) / 2.0,
//              (boundaryFieldAABB.minZ + boundaryFieldAABB.maxZ) / 2.0);
//      MovingObjectPosition mop = boundaryFieldAABB.calculateIntercept(playerPosition, epicentre);
//
//      if (mop == null) {        // full volume when inside field
//        infoToUpdate.soundEpicentre = playerPosition;
//        infoToUpdate.distanceToEpicentre = 0;
//        return true;
//      }
//      infoToUpdate.soundEpicentre = mop.hitVec;
//      infoToUpdate.distanceToEpicentre = (float)playerPosition.distanceTo(infoToUpdate.soundEpicentre);
//
//      return true;
//    }
//  }
//
//
//  /**
//   * Calculate the current boundary field
//   * @return the new boundary field, null if a problem occurred.  The AABB must be used for only one tick because it
//   *         is allocated from a pool that is reused.
//   */
//  public AxisAlignedBB getBoundaryField()
//  {
//    sortBoundaryFieldCorners();
//
//    if (boundaryCorner1 == null) return null;
//    BlockPos cnrMin = boundaryCorner1;
//    BlockPos cnrMax = (boundaryCorner2 != null) ? boundaryCorner2 : boundaryCorner1;
//    double wXmin = cnrMin.posX;
//    double wYmin = cnrMin.posY;
//    double wZmin = cnrMin.posZ;
//    double wXmax = cnrMax.posX + 1;
//    double wYmax = cnrMax.posY + 1;
//    double wZmax = cnrMax.posZ + 1;
//
//    return AxisAlignedBB.getBoundingBox(wXmin, wYmin, wZmin, wXmax, wYmax, wZmax);
//  }
//
//  /**
//   * Copies the boundary field coordinates into the supplied min and max BlockPos
//   * @param minCoord the chunk coordinate to be filled with the minimum x, y, z corner
//   * @param maxCoord the chunk coordinate to be filled with the maximum x, y, z corner
//   * @return true if the boundary field is valid; false if there is no boundary field
//   */
//  public boolean copyBoundaryCorners(BlockPos minCoord, BlockPos maxCoord)
//  {
//    sortBoundaryFieldCorners();
//
//    if (boundaryCorner1 == null) {
//      return false;
//    }
//    BlockPos cnrMin = boundaryCorner1;
//    BlockPos cnrMax = (boundaryCorner2 != null) ? boundaryCorner2 : boundaryCorner1;
//    minCoord.posX = cnrMin.posX;
//    minCoord.posY = cnrMin.posY;
//    minCoord.posZ = cnrMin.posZ;
//    maxCoord.posX = cnrMax.posX;
//    maxCoord.posY = cnrMax.posY;
//    maxCoord.posZ = cnrMax.posZ;
//    return true;
//  }
//
//  /**
//   * Calculate the new boundary field after being dragged to the current player position
//   *   Drags one side depending on which one the player grabbed, and how far they have moved
//   *   Won't drag the boundary field smaller than one block
//   * @param playerPosition
//   * @return the new boundary field, null if a problem occurred
//   */
//  private AxisAlignedBB getGrabDraggedBoundaryField(Vec3 playerPosition)
//  {
//    sortBoundaryFieldCorners();
//
//    if (boundaryCorner1 == null) return null;
//    BlockPos cnrMin = boundaryCorner1;
//    BlockPos cnrMax = (boundaryCorner2 != null) ? boundaryCorner2 : boundaryCorner1;
//    double wXmin = cnrMin.posX;
//    double wYmin = cnrMin.posY;
//    double wZmin = cnrMin.posZ;
//    double wXmax = cnrMax.posX + 1;
//    double wYmax = cnrMax.posY + 1;
//    double wZmax = cnrMax.posZ + 1;
//
//    if (boundaryGrabActivated) {
//      switch (boundaryGrabSide) {
//        case UsefulConstants.FACE_YNEG: {
//          wYmin += playerPosition.yCoord - boundaryGrabPoint.yCoord;
//          wYmin = UsefulFunctions.clipToRange(wYmin, wYmax - SELECTION_MAX_YSIZE, wYmax - 1.0);
//          break;
//        }
//        case UsefulConstants.FACE_YPOS: {
//          wYmax += playerPosition.yCoord - boundaryGrabPoint.yCoord;
//          wYmax = UsefulFunctions.clipToRange(wYmax, wYmin + SELECTION_MAX_YSIZE, wYmin + 1.0);
//          break;
//        }
//        case UsefulConstants.FACE_ZNEG: {
//          wZmin += playerPosition.zCoord - boundaryGrabPoint.zCoord;
//          wZmin = UsefulFunctions.clipToRange(wZmin, wZmax - SELECTION_MAX_ZSIZE, wZmax - 1.0);
//          break;
//        }
//        case UsefulConstants.FACE_ZPOS: {
//          wZmax += playerPosition.zCoord - boundaryGrabPoint.zCoord;
//          wZmax = UsefulFunctions.clipToRange(wZmax, wZmin + SELECTION_MAX_ZSIZE, wZmin + 1.0);
//          break;
//        }
//        case UsefulConstants.FACE_XNEG: {
//          wXmin += playerPosition.xCoord - boundaryGrabPoint.xCoord;
//          wXmin = UsefulFunctions.clipToRange(wXmin, wXmax - SELECTION_MAX_XSIZE, wXmax - 1.0);
//          break;
//        }
//        case UsefulConstants.FACE_XPOS: {
//          wXmax += playerPosition.xCoord - boundaryGrabPoint.xCoord;
//          wXmax = UsefulFunctions.clipToRange(wXmax, wXmin + SELECTION_MAX_XSIZE, wXmin + 1.0);
//          break;
//        }
//        default: {
//          FMLLog.warning("Invalid boundaryGrabSide (%d", boundaryGrabSide);
//        }
//      }
//    }
//    return AxisAlignedBB.getBoundingBox(wXmin, wYmin, wZmin, wXmax, wYmax, wZmax);
//  }
//
//  /**
//   * add a new corner to the boundary (replace boundaryCorner2).  If the selection is too big, move boundaryCorner1.
//   * @param newCorner
//   */
//  private void addCornerPointWithMaxSize(BlockPos newCorner)
//  {
//    boundaryCorner2 = new BlockPos(newCorner);
//    boundaryCorner1.posX = UsefulFunctions.clipToRange(boundaryCorner1.posX, newCorner.posX - SELECTION_MAX_XSIZE + 1, newCorner.posX + SELECTION_MAX_XSIZE - 1);
//    boundaryCorner1.posY = UsefulFunctions.clipToRange(boundaryCorner1.posY, newCorner.posY - SELECTION_MAX_YSIZE + 1, newCorner.posY + SELECTION_MAX_YSIZE - 1);
//    boundaryCorner1.posZ = UsefulFunctions.clipToRange(boundaryCorner1.posZ, newCorner.posZ - SELECTION_MAX_ZSIZE + 1, newCorner.posZ + SELECTION_MAX_ZSIZE - 1);
//    sortBoundaryFieldCorners();
//  }
//
//  private boolean boundaryGrabActivated = false;
//  private int boundaryGrabSide = UsefulConstants.FACE_NONE;
//  private Vec3 boundaryGrabPoint = null;
//  protected int boundaryCursorSide = UsefulConstants.FACE_NONE;
//
//  private SoundEffectBoundaryHum soundEffectBoundaryHum;
//
//  private ItemSpeedyBoundary itemSpeedyBoundary;
//}
