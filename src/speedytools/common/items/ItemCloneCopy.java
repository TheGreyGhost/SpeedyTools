package speedytools.common.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import speedytools.clientonly.BlockVoxelMultiSelector;
import speedytools.clientonly.SelectionBoxRenderer;
import speedytools.clientonly.eventhandlers.CustomSoundsHandler;
import speedytools.common.Colour;
import speedytools.common.UsefulConstants;

import java.util.List;

import static speedytools.clientonly.BlockMultiSelector.selectFill;

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
    checkInvariants();

    Vec3 playerOrigin = player.getPosition(partialTick);
    Vec3 playerLook = player.getLook(partialTick);

    if (currentToolState.displaySelection) {
      double dragSelectionOriginX = selectionOrigin.posX;
      double dragSelectionOriginY = selectionOrigin.posY;
      double dragSelectionOriginZ = selectionOrigin.posZ;
      if (selectionGrabActivated) {
        Vec3 distanceMoved = selectionGrabPoint.subtract(playerOrigin);
        dragSelectionOriginX += distanceMoved.xCoord;
        dragSelectionOriginY += distanceMoved.yCoord;
        dragSelectionOriginZ += distanceMoved.zCoord;
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
    textList.add("Right click: place boundary");
    textList.add("             markers (x2), then");
    textList.add(" Right button hold: move around");
    textList.add("             to drag boundary");
    textList.add("Left click: remove all markers");
  }

   /** respond to a button click
   * @param thePlayer
   * @param whichButton 0 = left (undo), 1 = right (use)
   */
  @SideOnly(Side.CLIENT)
  @Override
  public void buttonClicked(EntityClientPlayerMP thePlayer, int whichButton)
  {
    final int DOUBLE_CLICK_SPEED_MS = 200;
    checkInvariants();

    switch (whichButton) {
      case 0: {
        undo(thePlayer);
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
              if (clickElapsedTime < DOUBLE_CLICK_SPEED_MS * 1000 * 1000) {
                placeSelection(thePlayer);
              } else {
                Vec3 playerPosition = thePlayer.getPosition(1.0F);  // beware, Vec3 is short-lived
                selectionGrabActivated = true;
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

  private void undo(EntityClientPlayerMP thePlayer)
  {
    checkInvariants();
    switch(currentToolState) {
      case NO_SELECTION: {
        break;
      }
      case GENERATING_SELECTION: {
        actionInProgress = ActionInProgress.NONE;
        currentToolState = ToolState.NO_SELECTION;
        break;
      }
      case DISPLAYING_SELECTION: {
        currentToolState = ToolState.NO_SELECTION;
        break;
      }
      default: {
        assert (currentToolState == ToolState.NO_SELECTION);
      }
    }
//        playSound(CustomSoundsHandler.BOUNDARY_UNPLACE, thePlayer);
  }

  private void placeSelection(EntityClientPlayerMP thePlayer)
  {
  }

  private void flipSelection()
  {

  }

  private void initiateSelectionCreation(EntityClientPlayerMP thePlayer)
  {
    switch (currentHighlighting) {
      case NONE: {
        break;
      }
      case FULL_BOX: {
        voxelSelectionManager = new BlockVoxelMultiSelector();
        voxelSelectionManager.selectAllInBoxStart(thePlayer.worldObj, boundaryCorner1, boundaryCorner2);
        sortBoundaryFieldCorners();
        selectionOrigin = new ChunkCoordinates(boundaryCorner1);
        currentToolState = ToolState.GENERATING_SELECTION;
        actionInProgress = ActionInProgress.VOXELCREATION;
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
  }

  /** called once per tick while the user is holding an ItemCloneTool
   * @param useKeyHeldDown
   */
  @Override
  public void tick(World world, boolean useKeyHeldDown)
  {
    checkInvariants();
    final long MAX_TIME_IN_NS = 10 * 1000 * 1000;
    switch (actionInProgress) {
      case NONE: {
        break;
      }
      case VOXELCREATION: {
        boolean finished = voxelSelectionManager.selectAllInBoxContinue(world, MAX_TIME_IN_NS);
        if (finished) {
          actionInProgress = ActionInProgress.NONE;
          voxelSelectionManager.createRenderList(world);
          currentToolState = ToolState.DISPLAYING_SELECTION;
        }
        break;
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

    checkInvariants();
  }

  private void checkInvariants()
  {
    assert (    currentToolState != ToolState.NO_SELECTION
            || (actionInProgress == ActionInProgress.NONE) );
    assert (    currentToolState != ToolState.GENERATING_SELECTION
            || (voxelSelectionManager != null && actionInProgress != ActionInProgress.NONE) );
    assert (   currentToolState != ToolState.DISPLAYING_SELECTION
            || (voxelSelectionManager != null && selectionOrigin != null && actionInProgress == ActionInProgress.NONE ) );

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
  //

  private SelectionType currentHighlighting = SelectionType.NONE;
  private List<ChunkCoordinates> highlightedBlocks;

  private ActionInProgress actionInProgress = ActionInProgress.NONE;
  private ToolState currentToolState = ToolState.NO_SELECTION;

  private BlockVoxelMultiSelector voxelSelectionManager;
  private ChunkCoordinates selectionOrigin;
  private boolean selectionGrabActivated = false;
  private Vec3    selectionGrabPoint = null;

  private long lastRightClickTime;

  private enum SelectionType {
    NONE, FULL_BOX, BOUND_FILL, UNBOUND_FILL
  }

  private enum ActionInProgress {
    NONE, VOXELCREATION
  }

  private enum ToolState {
    NO_SELECTION(true, false, true, false),
    GENERATING_SELECTION(false, false, true, true),
    DISPLAYING_SELECTION(false, true, false, false);

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

}