package speedytools.common.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import speedytools.clientside.ClientSide;
import speedytools.clientside.selections.BlockVoxelMultiSelector;
import speedytools.clientside.rendering.SelectionBoxRenderer;
import speedytools.common.SpeedyToolsOptions;
import speedytools.common.utilities.Colour;
import speedytools.common.utilities.UsefulConstants;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static speedytools.clientside.selections.BlockMultiSelector.selectFill;

/*
three selection modes:
1) no selection field - floodfill from block clicked up
2) selection field: standing outside - all solid blocks in the field
3) selection field: standing inside - floodfill to boundary

1st rightclick = create selection
2nd right click & hold = drag selection
double right click = place
left click = undo place / undo selection
ctrl + Rclick = flip selection
ctrl + mousewheel = rotate

Selection creation and RenderList creation are both done in stages to avoid starving CPU:
(1) start / initialise
(2) each tick: continue for a max of xxx nanoseconds.

 */


public class ItemCloneCopy extends ItemCloneTool {
  public ItemCloneCopy(int id) {
    super(id);
    setMaxStackSize(1);
    setUnlocalizedName("CloneCopy");
    setFull3D();                              // setting this flag causes the staff to render vertically in 3rd person view, like a pickaxe
    checkInvariants();
    lastRightClickTime = System.nanoTime() - 10 * 1000 * 1000 * 1000;   // arbitrary valid value
  }

  @Override
  public void registerIcons(IconRegister iconRegister)
  {
    itemIcon = iconRegister.registerIcon("speedytools:copystafficon");
  }

  @Override
  public Icon getIcon(ItemStack stack, int pass)
  {
    return itemIcon;
  }

  /**
   * Selects the first Block that will be affected by the tool when the player presses right-click
   * 1) no selection field - floodfill (all non-air blocks, including diagonal fill) from block clicked up
   * 2) selection field: standing outside - all solid blocks in the field
   * 3) selection field: standing inside - floodfill to boundary field
   * So the selection algorithm is:
   * a) if the player is pointing at a block, specify that; else
   * b) check the player is pointing at a side of the boundary field (from outside)
   *
   * @param target the position of the cursor
   * @param player the player
   * @param currentItem the current item that the player is holding.  MUST be derived from ItemCloneTool.
   * @param partialTick partial tick time.
   */
  @Override
  public void highlightBlocks(MovingObjectPosition target, EntityPlayer player, ItemStack currentItem, float partialTick)
  {
    checkInvariants();
    if (currentToolState != ToolState.NO_SELECTION) return;

    final int MAX_NUMBER_OF_HIGHLIGHTED_BLOCKS = 64;
    currentlySelectedBlock = null;
    highlightedBlocks = null;
    boundaryGrabActivated = false;
    boundaryCursorSide = UsefulConstants.FACE_NONE;
    currentHighlighting = SelectionType.NONE;

    if (target != null && target.typeOfHit == EnumMovingObjectType.TILE) {
      currentlySelectedBlock = new ChunkCoordinates(target.blockX, target.blockY, target.blockZ);
      boolean playerIsInsideBoundaryField = false;

      if (boundaryCorner1 != null && boundaryCorner2 != null) {
        sortBoundaryFieldCorners();
        if (   currentlySelectedBlock.posX >= boundaryCorner1.posX && currentlySelectedBlock.posX <= boundaryCorner2.posX
            && currentlySelectedBlock.posY >= boundaryCorner1.posY && currentlySelectedBlock.posY <= boundaryCorner2.posY
            && currentlySelectedBlock.posZ >= boundaryCorner1.posZ && currentlySelectedBlock.posZ <= boundaryCorner2.posZ ) {
          playerIsInsideBoundaryField = true;
        }
      }

      if (playerIsInsideBoundaryField) {
        currentHighlighting = SelectionType.BOUND_FILL;
        highlightedBlocks = selectFill(target, player.worldObj, MAX_NUMBER_OF_HIGHLIGHTED_BLOCKS, true, true,
                                       boundaryCorner1.posX, boundaryCorner2.posX,
                                       boundaryCorner1.posY, boundaryCorner2.posY,
                                       boundaryCorner1.posZ, boundaryCorner2.posZ);
      } else {
        currentHighlighting = SelectionType.UNBOUND_FILL;
        highlightedBlocks = selectFill(target, player.worldObj, MAX_NUMBER_OF_HIGHLIGHTED_BLOCKS, true, true,
                                       Integer.MIN_VALUE, Integer.MAX_VALUE,
                                       currentlySelectedBlock.posY, 255,
                                       Integer.MIN_VALUE, Integer.MAX_VALUE);
      }
      return;
    }

    if (boundaryCorner1 == null || boundaryCorner2 == null) return;
    Vec3 playerPosition = player.getPosition(1.0F);
    if (   playerPosition.xCoord >= boundaryCorner1.posX && playerPosition.xCoord <= boundaryCorner2.posX +1
        && playerPosition.yCoord >= boundaryCorner1.posY && playerPosition.yCoord <= boundaryCorner2.posY +1
        && playerPosition.zCoord >= boundaryCorner1.posZ && playerPosition.zCoord <= boundaryCorner2.posZ +1) {
      return;
    }
    MovingObjectPosition highlightedFace = boundaryFieldFaceSelection(Minecraft.getMinecraft().renderViewEntity);
    boundaryCursorSide = (highlightedFace != null) ? UsefulConstants.FACE_ALL : UsefulConstants.FACE_NONE;
    currentHighlighting = SelectionType.FULL_BOX;
 }

  @Override
  public void renderBoundaryField(EntityPlayer player, float partialTick)
  {
    checkInvariants();
    if (currentToolState.displayBoundaryField) {
      super.renderBoundaryField(player, partialTick);
    }
  }

    @Override
  public void renderBlockHighlight(EntityPlayer player, float partialTick)
  {
    final double THRESHOLD_SPEED_SQUARED_FOR_SNAP_GRID = 0.01;
    checkInvariants();

    Vec3 playerOrigin = player.getPosition(partialTick);
    Vec3 playerLook = player.getLook(partialTick);

    if (currentToolState.displaySelection) {
      double dragSelectionOriginX = selectionOrigin.posX;
      double dragSelectionOriginY = selectionOrigin.posY;
      double dragSelectionOriginZ = selectionOrigin.posZ;
      if (selectionGrabActivated) {
        double currentSpeedSquared = player.motionX * player.motionX + player.motionY * player.motionY + player.motionZ * player.motionZ;
        if (currentSpeedSquared >= THRESHOLD_SPEED_SQUARED_FOR_SNAP_GRID) {
          selectionMovedFastYet = true;
        }
        final boolean snapToGridWhileMoving = selectionMovedFastYet && currentSpeedSquared <= THRESHOLD_SPEED_SQUARED_FOR_SNAP_GRID;

        Vec3 distanceMoved = selectionGrabPoint.subtract(playerOrigin);
        dragSelectionOriginX += distanceMoved.xCoord;
        dragSelectionOriginY += distanceMoved.yCoord;
        dragSelectionOriginZ += distanceMoved.zCoord;
        if (snapToGridWhileMoving) {
          dragSelectionOriginX = Math.round(dragSelectionOriginX);
          dragSelectionOriginY = Math.round(dragSelectionOriginY);
          dragSelectionOriginZ = Math.round(dragSelectionOriginZ);
        }
      }
      GL11.glTranslated(dragSelectionOriginX - playerOrigin.xCoord, dragSelectionOriginY - playerOrigin.yCoord, dragSelectionOriginZ - playerOrigin.zCoord);
      Vec3 playerRelativeToSelectionOrigin = playerOrigin.addVector(-dragSelectionOriginX, -dragSelectionOriginY, -dragSelectionOriginZ);
      voxelSelectionManager.renderSelection(playerRelativeToSelectionOrigin, playerLook);
    }

    if (currentToolState.displayHighlight && highlightedBlocks != null) {
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glColor4f(Colour.BLACK_40.R, Colour.BLACK_40.G, Colour.BLACK_40.B, Colour.BLACK_40.A);
      GL11.glLineWidth(2.0F);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glDepthMask(false);
      double EXPAND_DISTANCE = 0.002F;

      AxisAlignedBB boundingBox = AxisAlignedBB.getAABBPool().getAABB(0, 0, 0, 0, 0, 0);
      for (ChunkCoordinates block : highlightedBlocks) {
        boundingBox.setBounds(block.posX, block.posY, block.posZ, block.posX+1, block.posY+1, block.posZ+1);
        boundingBox = boundingBox.expand(EXPAND_DISTANCE, EXPAND_DISTANCE, EXPAND_DISTANCE).getOffsetBoundingBox(-playerOrigin.xCoord, -playerOrigin.yCoord, -playerOrigin.zCoord);
        SelectionBoxRenderer.drawCube(boundingBox);
      }

      GL11.glDepthMask(true);
      GL11.glPopAttrib();
    }
  }

  /**
   * allows items to add custom lines of information to the mouseover description
   */
  @Override
  public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List textList, boolean useAdvancedItemTooltips)
  {
    textList.add("Right click: create selection");
    textList.add("Right hold: drag selection");
    textList.add("Left click: undo selection");
    textList.add("Right 2x click: copy selection");
    textList.add("Left 2x click: undo copy");
  }

   /** respond to a button click
   * @param thePlayer
   * @param whichButton 0 = left (undo), 1 = right (use)
   */
  @SideOnly(Side.CLIENT)
  @Override
  public void buttonClicked(EntityClientPlayerMP thePlayer, int whichButton)
  {
    final long DOUBLE_CLICK_SPEED_NS = SpeedyToolsOptions.getDoubleClickSpeedMS() * 1000 * 1000;
    checkInvariants();

    switch (whichButton) {
      case 0: {
        if (lastLeftClickTime == null) {
          lastLeftClickTime = System.nanoTime();  // .tick() will act on this after the double click time has elapsed
        } else {
          long clickElapsedTime = System.nanoTime() - lastLeftClickTime;
          if (clickElapsedTime < DOUBLE_CLICK_SPEED_NS) {
            undoAction(thePlayer);
          } else {
            undoSelection(thePlayer);    // in case the tick didn't occur yet; discard second click
          }
        }
        break;
      }
      case 1: {
        if (currentToolState.performingAction) return;
        switch (currentToolState) {
          case NO_SELECTION: {
            initiateSelectionCreation(thePlayer);
            break;
          }
          case DISPLAYING_SELECTION: {
            long clickElapsedTime = System.nanoTime() - lastRightClickTime;
            lastRightClickTime = System.nanoTime();

            boolean controlKeyDown =  Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
            if (controlKeyDown) {
              flipSelection();
            } else {
              if (clickElapsedTime < DOUBLE_CLICK_SPEED_NS) {
                placeSelection(thePlayer);
              } else {
                Vec3 playerPosition = thePlayer.getPosition(1.0F);  // beware, Vec3 is short-lived
                selectionGrabActivated = true;
                selectionMovedFastYet = false;
                selectionGrabPoint = Vec3.createVectorHelper(playerPosition.xCoord, playerPosition.yCoord, playerPosition.zCoord);
              }
            }
          }
        }
        break;
      }
      default: {     // should never happen- if it does, ignore it
        break;
      }
    }

    checkInvariants();
    return;
  }

  /**
   * If there is a current selection, destroy it.  No effect if waiting for the server to do something.
   * @param thePlayer
   */
  private void undoSelection(EntityClientPlayerMP thePlayer)
  {
    switch (currentToolState) {
      case NO_SELECTION: {
        break;
      }
      case GENERATING_SELECTION:
      case DISPLAYING_SELECTION: {
        currentToolState = ToolState.NO_SELECTION;
        break;
      }
      case PERFORMING_ACTION:
      case PERFORMING_UNDO: {
        break;   // do nothing
      }
      default:
        assert false : "invalid currentToolState";

    }
  }

  /**
   * if the player has previously performed an action, undo it.
   * @param thePlayer
   */
  private void undoAction(EntityClientPlayerMP thePlayer)
  {
    checkInvariants();
    // if we're currently undoing an action, do nothing
    // if we're in the middle of performing an action, cancel it
    // if we have previously performed an action, undo it.
    switch(currentToolState) {
      case NO_SELECTION:
      case PERFORMING_UNDO:
      case GENERATING_SELECTION: {
        break;
      }
      case DISPLAYING_SELECTION: {
        if (lastCompletedActionSequenceNumber != null) {
          int undoSequenceNumber = ClientSide.getCloneToolsNetworkClient().toolUndoPerformed(lastCompletedActionSequenceNumber);
          pendingUndoSequenceNumber = undoSequenceNumber;

        }
        break;
      }
      case PERFORMING_ACTION: {
        if (pendingActionSequenceNumber != null) {
          int undoSequenceNumber = ClientSide.getCloneToolsNetworkClient().toolUndoPerformed(pendingActionSequenceNumber);
          pendingUndoSequenceNumber = undoSequenceNumber;
        }
        break;
      }

      default: {
        assert false : "Illegal currentToolState in undoAction";
      }
    }
//        playSound(CustomSoundsHandler.BOUNDARY_UNPLACE, thePlayer);
  }

  /**
   * don't call if an action is already pending
   * @param thePlayer
   */
  private void placeSelection(EntityClientPlayerMP thePlayer)
  {
    assert pendingActionSequenceNumber == null && pendingUndoSequenceNumber == null;
    int actionSequenceNumber = ClientSide.getCloneToolsNetworkClient().toolActionPerformed(itemID,
                                                selectionOrigin.posX, selectionOrigin.posY, selectionOrigin.posZ, (byte)0, false); // todo: implement rotation and flipped
    pendingActionSequenceNumber = actionSequenceNumber;
  }

  private void flipSelection()
  {

  }

  private void initiateSelectionCreation(EntityClientPlayerMP thePlayer)
  {
    switch (currentHighlighting) {
      case NONE: {
        return;
      }
      case FULL_BOX: {
        voxelSelectionManager = new BlockVoxelMultiSelector();
        voxelSelectionManager.selectAllInBoxStart(thePlayer.worldObj, boundaryCorner1, boundaryCorner2);
        sortBoundaryFieldCorners();
        selectionOrigin = new ChunkCoordinates(boundaryCorner1);
        currentToolState = ToolState.GENERATING_SELECTION;
//            playSound(CustomSoundsHandler.BOUNDARY_PLACE_1ST, thePlayer);
        break;
      }
      case UNBOUND_FILL: {
        break;
      }
      case BOUND_FILL: {
        break;
      }
    }
    ClientSide.getCloneToolsNetworkClient().toolSelectionPerformed();
  }

  @Override
  public void updateAction(int sequenceNumber, boolean successfullyStarted)
  {

    switch ()

    private Integer pendingActionSequenceNumber = null;              // if not null, an action is pending that hasn't been confirmed started by the server
    private Integer lastCompletedActionSequenceNumber = null;        // if not null, there is a completed action which can be undone
    private Integer pendingUndoSequenceNumber = null;                // if not null, an undo action is pending that hasn't been confirmed by the server

  }

  /** called once per tick on the client side while the user is holding an ItemCloneTool
   * used to:
   * (1) background generation of a selection, if it has been initiated
   * (2) drag the selection around (ungrabbing it if the user had released the button)
   * (3) perform an undo selection, once enough time has elapsed to it is clear that this is a single left click not a double left click
   * @param useKeyHeldDown
   */
  @Override
  public void tick(World world, boolean useKeyHeldDown)
  {
    super.tick(world, useKeyHeldDown);
    checkInvariants();
    final long MAX_TIME_IN_NS = 20 * 1000 * 1000;
    if (currentToolState == ToolState.GENERATING_SELECTION) {
      System.out.print("Vox start nano(ms) : " + System.nanoTime()/ 1000000);                                     //todo: remove
      actionPercentComplete = 100.0F * voxelSelectionManager.selectAllInBoxContinue(world, MAX_TIME_IN_NS);
      System.out.println(": end (ms) : " + System.nanoTime()/ 1000000);                                           //todo: remove

      if (actionPercentComplete < 0.0F) {
        if (voxelSelectionManager.isEmpty()) {
          currentToolState = ToolState.NO_SELECTION;
        } else {
          voxelSelectionManager.createRenderList(world);
          currentToolState = ToolState.DISPLAYING_SELECTION;
        }
      }
    }

    if (currentToolState == ToolState.DISPLAYING_SELECTION) {
      if (selectionGrabActivated & !useKeyHeldDown) {
        Vec3 playerPosition = Minecraft.getMinecraft().renderViewEntity.getPosition(1.0F);
        Vec3 distanceMoved = selectionGrabPoint.subtract(playerPosition);
        selectionOrigin.posX += (int)Math.round(distanceMoved.xCoord);
        selectionOrigin.posY += (int)Math.round(distanceMoved.yCoord);
        selectionOrigin.posZ += (int)Math.round(distanceMoved.zCoord);
        selectionGrabActivated = false;
//        playSound(CustomSoundsHandler.BOUNDARY_UNGRAB,
//                (float)playerPosition.xCoord, (float)playerPosition.yCoord, (float)playerPosition.zCoord);
      }
    }

    final long DOUBLE_CLICK_SPEED_NS = SpeedyToolsOptions.getDoubleClickSpeedMS() * 1000 * 1000;
    if (lastLeftClickTime != null) {
      long clickElapsedTime = System.nanoTime() - lastLeftClickTime;
      if (clickElapsedTime > DOUBLE_CLICK_SPEED_NS) { // the double-click time has elapsed without a second click
        undoSelection(Minecraft.getMinecraft().thePlayer);
      }
    }

    checkInvariants();
  }

  private void checkInvariants()
  {
    assert (    currentToolState != ToolState.GENERATING_SELECTION || voxelSelectionManager != null);
    assert (   currentToolState != ToolState.DISPLAYING_SELECTION
            || (voxelSelectionManager != null && selectionOrigin != null) );

    assert (    selectionGrabActivated == false || selectionGrabPoint != null);
  }

  // the Item can be in several states as given by currentToolState:
  // 1) NO_SELECTION
  //    highlightBlocks() is used to update some variables, based on what the player is looking at
  //      a) highlightedBlocks (block wire outline)
  //      b) currentHighlighting (what type of highlighting depending on whether there is a boundary field, whether
  //         the player is looking at a block or at a side of the boundary field
  //      c) currentlySelectedBlock (if looking at a block)
  //      d) boundaryCursorSide (if looking at the boundary field)
  //    voxelSelectionManager is not valid
  // 2) GENERATING_SELECTION - user has clicked to generate a selection
  //    a) actionInProgress gives the action being performed
  //    b) voxelSelectionManager has been created and initialised
  //    c) every tick, the voxelSelectionManager is updated further until complete
  // 3) DISPLAYING_SELECTION - selection is being displayed and/or moved
  //    voxelSelectionManager is valid and has a renderlist
  // 4) PERFORMING_ACTION - we've sent the action command to the server and are waiting for it to finish
  // 5) PERFORMING_UNDO - we've send an undo command to the server and are waiting for it to finish

  private SelectionType currentHighlighting = SelectionType.NONE;
  private List<ChunkCoordinates> highlightedBlocks;

  private float actionPercentComplete;
  private ToolState currentToolState = ToolState.NO_SELECTION;

  private BlockVoxelMultiSelector voxelSelectionManager;
  private ChunkCoordinates selectionOrigin;
  private boolean selectionGrabActivated = false;
  private Vec3    selectionGrabPoint = null;
  private boolean selectionMovedFastYet;

  private Long lastRightClickTime = null;
  private Long lastLeftClickTime = null;

  private Integer pendingActionSequenceNumber = null;              // if not null, an action is pending that hasn't been confirmed started by the server
  private Integer lastCompletedActionSequenceNumber = null;        // if not null, there is a completed action which can be undone
  private Integer pendingUndoSequenceNumber = null;                // if not null, an undo action is pending that hasn't been confirmed by the server

  private enum SelectionType {
    NONE, FULL_BOX, BOUND_FILL, UNBOUND_FILL
  }

  private enum ToolState {
    NO_SELECTION(true, false, true, false),
    GENERATING_SELECTION(false, false, true, true),
    DISPLAYING_SELECTION(false, true, false, false),
    PERFORMING_ACTION(false, false, false, true),
    PERFORMING_UNDO(false, false, false, true);

    public final boolean displayHighlight;
    public final boolean displaySelection;
    public final boolean displayBoundaryField;
    public final boolean performingAction;

    private ToolState(boolean init_displayHighlight, boolean init_displaySelection, boolean init_displayBoundaryField, boolean init_performingAction)
    {
      displayHighlight = init_displayHighlight;
      displaySelection = init_displaySelection;
      displayBoundaryField = init_displayBoundaryField;
      performingAction = init_performingAction;
    }
  }

  @Override
  public boolean renderCrossHairs(ScaledResolution scaledResolution, float partialTick)
  {
    if (!currentToolState.performingAction) return false;

    final float Z_LEVEL_FROM_GUI_IN_GAME_FORGE = -90.0F;            // taken from GuiInGameForge.renderCrossHairs
    final double CROSSHAIR_SPIN_DEGREES_PER_TICK = 360.0 / 20;
    final int CROSSHAIR_ICON_WIDTH = 16;
    final int CROSSHAIR_ICON_HEIGHT = 16;
    final int CROSSHAIR_X_OFFSET = -7;      // taken from GuiInGameForge.renderCrossHairs
    final int CROSSHAIR_Y_OFFSET = -7;
    final float ARC_LINE_WIDTH = 4.0F;

    Minecraft mc = Minecraft.getMinecraft();
    int width = scaledResolution.getScaledWidth();
    int height = scaledResolution.getScaledHeight();

    GL11.glPushAttrib(GL11.GL_ENABLE_BIT);

    mc.getTextureManager().bindTexture(Gui.icons);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR);

    GL11.glPushMatrix();
    GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
    GL11.glRotated(((tickCount + (double) partialTick) * CROSSHAIR_SPIN_DEGREES_PER_TICK) % 360.0, 0.0, 0.0, 1.0);
    drawTexturedModalRect(CROSSHAIR_X_OFFSET, CROSSHAIR_Y_OFFSET, Z_LEVEL_FROM_GUI_IN_GAME_FORGE,
                          0, 0, CROSSHAIR_ICON_WIDTH, CROSSHAIR_ICON_HEIGHT);
    GL11.glPopMatrix();

    GL11.glPushMatrix();
    GL11.glTranslatef(width / 2, height / 2, Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
    GL11.glColor4d(1.0, 1.0, 1.0, 1.0);
    GL11.glLineWidth(ARC_LINE_WIDTH);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    drawArc(12.0, 0.0, actionPercentComplete * 360.0 / 100.0, (double) Z_LEVEL_FROM_GUI_IN_GAME_FORGE);
    GL11.glPopMatrix();
    
    GL11.glPopAttrib();
    return true;
  }

  /**
   * Draws a textured rectangle at the given z-value. Args: x, y, u, v, width, height
   */
  public void drawTexturedModalRect(int x, int y, float z, int u, int v, int width, int height)
  {
    double ICON_SCALE_FACTOR_X = 1/256.0F;
    double ICON_SCALE_FACTOR_Y =  1/256.0F;
    Tessellator tessellator = Tessellator.instance;
    tessellator.startDrawingQuads();
    tessellator.addVertexWithUV(    x + 0, y + height, z,           u * ICON_SCALE_FACTOR_X, (v + height) * ICON_SCALE_FACTOR_Y);
    tessellator.addVertexWithUV(x + width, y + height, z, (u + width) * ICON_SCALE_FACTOR_X, (v + height) * ICON_SCALE_FACTOR_Y);
    tessellator.addVertexWithUV(x + width,      y + 0, z, (u + width) * ICON_SCALE_FACTOR_X,            v * ICON_SCALE_FACTOR_Y);
    tessellator.addVertexWithUV(    x + 0,      y + 0, z,           u * ICON_SCALE_FACTOR_X,            v * ICON_SCALE_FACTOR_Y);
    tessellator.draw();
  }


  /**
   * Draw an arc centred around the zero point.  Setup translatef, colour and line width etc before calling.
   * @param radius
   * @param startAngle clockwise starting from 12 O'clock
   * @param endAngle
   */
   void drawArc(double radius, double startAngle, double endAngle, double zLevel)
  {
    final double angleIncrement = Math.toRadians(10.0);
    float direction = (endAngle >= startAngle) ? 1.0F : -1.0F;
    double deltaAngle = Math.abs(endAngle - startAngle);
    deltaAngle %= 360.0;

    startAngle -= Math.floor(startAngle/360.0);
    startAngle = Math.toRadians(startAngle);
    deltaAngle = Math.toRadians(deltaAngle);

    GL11.glBegin(GL11.GL_LINE_STRIP);

    double x, y;
    double arcPos = 0;
    boolean arcFinished = false;
    do {
      double truncAngle = Math.min(arcPos, deltaAngle);
      x = radius * Math.sin(startAngle + direction * truncAngle);
      y = -radius * Math.cos(startAngle + direction * truncAngle);
      GL11.glVertex3d(x, y, zLevel);

      arcFinished = (arcPos >= deltaAngle);
      arcPos += angleIncrement;
    } while (!arcFinished);
    GL11.glEnd();
  }
}

