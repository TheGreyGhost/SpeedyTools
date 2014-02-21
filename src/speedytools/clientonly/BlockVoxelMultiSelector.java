package speedytools.clientonly;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import speedytools.common.Colour;

import java.util.ArrayList;

/**
 * User: The Grey Ghost
 * Date: 17/02/14
 */
public class BlockVoxelMultiSelector
{
  private VoxelSelection selection;
  private VoxelSelection shadow;

/*
  // the coordinates of the blocks that form the
  private ArrayList<ChunkCoordinates> wireFrameXnegY;
  private ArrayList<ChunkCoordinates> wireFrameXposY;
  private ArrayList<ChunkCoordinates> wireFrameZnegY;
  private ArrayList<ChunkCoordinates> wireFrameZposY;
*/
  private int xSize;
  private int ySize;
  private int zSize;
  private int xOffset;
  private int yOffset;
  private int zOffset;

  private int xpos;
  private int ypos;
  private int zpos;

  private enum OperationInProgress {
    IDLE, ENTIREFIELD, COMPLETE
  }
  private OperationInProgress mode;

  private int displayListSelection = 0;
  private int displayListWireframeStrip = 0;

  /**
   * initialise conversion of the selected box to a VoxelSelection
   * @param world
   * @param corner1 one corner of the box
   * @param corner2 opposite corner of the box
   */
  public void selectAllInBoxStart(World world, ChunkCoordinates corner1, ChunkCoordinates corner2)
  {
    initialiseSelectionSizeFromBoundary(corner1, corner2);
    xpos = 0;
    ypos = 0;
    zpos = 0;
    mode = OperationInProgress.ENTIREFIELD;
  }

  /**
   * continue conversion of the selected box to a VoxelSelection.  Call repeatedly until conversion complete.
   * @param world
   * @param maxTimeInNS maximum elapsed duration before processing stops & function returns
   * @return true if complete, false if timeout
   */
  public boolean selectAllInBoxContinue(World world, long maxTimeInNS)
  {
    if (mode != OperationInProgress.ENTIREFIELD) {
      FMLLog.severe("Mode should be ENTIREFIELD in BlockVoxelMultiSelector::selectEntireFieldContinue");
      return true;
    }

    long startTime = System.nanoTime();

    for ( ; zpos < zSize; ++zpos, xpos = 0) {
      for ( ; xpos < xSize; ++xpos, ypos = 0) {
        for ( ; ypos < ySize; ++ypos) {
          if (System.nanoTime() - startTime >= maxTimeInNS) return false;
          if (world.getBlockId(xpos + xOffset, ypos + yOffset, zpos + zOffset) != 0) {
            selection.setVoxel(xpos, ypos, zpos);
            shadow.setVoxel(xpos, 1, zpos);
          }
        }
      }
    }
    mode = OperationInProgress.COMPLETE;
    return true;
  }

  private void initialiseSelectionSizeFromBoundary(ChunkCoordinates corner1, ChunkCoordinates corner2)
  {
    xOffset = Math.min(corner1.posX, corner2.posX);
    yOffset = Math.min(corner1.posY, corner2.posY);
    zOffset = Math.min(corner1.posZ, corner2.posZ);
    xSize = 1 + Math.max(corner1.posX, corner2.posX) - xOffset;
    ySize = 1 + Math.max(corner1.posY, corner2.posY) - yOffset;
    zSize = 1 + Math.max(corner1.posZ, corner2.posZ) - zOffset;
    if (selection == null) {
      selection = new VoxelSelection(xSize, ySize, zSize);
      shadow = new VoxelSelection(xSize, 1, zSize);
    } else {
      selection.clearAll(xSize, ySize, zSize);
      shadow.clearAll(xSize, 1, zSize);
    }
  }

  /**
   * create a render list for the current selection.
   * Quads, with lines to outline them
   * @param world
   */
  public void createRenderList(World world)
  {
    if (displayListSelection == 0) {
      displayListSelection = GLAllocation.generateDisplayLists(1);
    }
    if (displayListSelection == 0) {
      FMLLog.warning("Unable to create a displayList in BlockVoxelMultiSelector::createRenderList");
      return;
    }

    if (selection == null) {
      GL11.glNewList(displayListSelection, GL11.GL_COMPILE);
      GL11.glEndList();
      return;
    }

    final double NUDGE_DISTANCE = 0.0001;

    Tessellator tessellator = Tessellator.instance;
    GL11.glNewList(displayListSelection, GL11.GL_COMPILE);
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
    GL11.glDisable(GL11.GL_CULL_FACE);
    tessellator.startDrawingQuads();
    tessellator.setColorOpaque_F(Colour.PINK_100.R, Colour.PINK_100.G, Colour.PINK_100.B);
    // goes outside the VoxelSelection size, which always returns zero when out of bounds
    for (int y = 0; y <= ySize; ++y) {
      for (int z = 0; z <= zSize; ++z) {
        for (int x = 0; x <= xSize; ++x) {
           if (selection.getVoxel(x, y, z) != selection.getVoxel(x-1, y, z)) {
             double xNudge = x + (selection.getVoxel(x, y, z) ? -NUDGE_DISTANCE : +NUDGE_DISTANCE);
             tessellator.addVertex(xNudge,   y,   z);
             tessellator.addVertex(xNudge, y+1,   z);
             tessellator.addVertex(xNudge, y+1, z+1);
             tessellator.addVertex(xNudge,   y, z+1);
           }
          if (selection.getVoxel(x, y, z) != selection.getVoxel(x, y-1, z)) {
            double yNudge = y + (selection.getVoxel(x, y, z) ? -NUDGE_DISTANCE : +NUDGE_DISTANCE);
            tessellator.addVertex(  x, yNudge,   z);
            tessellator.addVertex(x+1, yNudge,   z);
            tessellator.addVertex(x+1, yNudge, z+1);
            tessellator.addVertex(  x, yNudge, z+1);
          }
          if (selection.getVoxel(x, y, z) != selection.getVoxel(x, y, z-1)) {
            double zNudge = z + (selection.getVoxel(x, y, z) ? -NUDGE_DISTANCE : +NUDGE_DISTANCE);
            tessellator.addVertex(  x,   y, zNudge);
            tessellator.addVertex(  x, y+1, zNudge);
            tessellator.addVertex(x+1, y+1, zNudge);
            tessellator.addVertex(x+1,   y, zNudge);
          }
        }
      }
    }
    tessellator.draw();

    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(Colour.BLACK_40.R, Colour.BLACK_40.G, Colour.BLACK_40.B, Colour.BLACK_40.A);
    GL11.glLineWidth(2.0F);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDepthMask(false);

    // goes outside the VoxelSelection size, which always returns zero when out of bounds
    for (int y = 0; y <= ySize; ++y) {
      for (int z = 0; z <= zSize; ++z) {
        for (int x = 0; x <= xSize; ++x) {
          if (selection.getVoxel(x, y, z) != selection.getVoxel(x-1, y, z)) {
            double xNudge = x + (selection.getVoxel(x, y, z) ? -NUDGE_DISTANCE : +NUDGE_DISTANCE);
            tessellator.startDrawing(GL11.GL_LINE_LOOP);
            tessellator.addVertex(xNudge,   y,   z);
            tessellator.addVertex(xNudge, y+1,   z);
            tessellator.addVertex(xNudge, y+1, z+1);
            tessellator.addVertex(xNudge,   y, z+1);
            tessellator.draw();
          }
          if (selection.getVoxel(x, y, z) != selection.getVoxel(x, y-1, z)) {
            double yNudge = y + (selection.getVoxel(x, y, z) ? -NUDGE_DISTANCE : +NUDGE_DISTANCE);
            tessellator.startDrawing(GL11.GL_LINE_LOOP);
            tessellator.addVertex(  x, yNudge,   z);
            tessellator.addVertex(x+1, yNudge,   z);
            tessellator.addVertex(x+1, yNudge, z+1);
            tessellator.addVertex(  x, yNudge, z+1);
            tessellator.draw();
          }
          if (selection.getVoxel(x, y, z) != selection.getVoxel(x, y, z-1)) {
            double zNudge = z + (selection.getVoxel(x, y, z) ? -NUDGE_DISTANCE : +NUDGE_DISTANCE);
            tessellator.startDrawing(GL11.GL_LINE_LOOP);
            tessellator.addVertex(  x,   y, zNudge);
            tessellator.addVertex(  x, y+1, zNudge);
            tessellator.addVertex(x+1, y+1, zNudge);
            tessellator.addVertex(x+1,   y, zNudge);
            tessellator.draw();
          }
        }
      }
    }

    GL11.glDepthMask(true);

    GL11.glPopAttrib();
    GL11.glEndList();
  }

  /**
   * a vertical wireframe strip made up of 1x1 squares, in the xy plane
   * origin is the top (ymax) corner, i.e. the topmost square is [0,0,0] to [1, -1, 0]
   */
  private void generateWireFrameStrip()
  {
    if (displayListWireframeStrip == 0) {
      displayListWireframeStrip = GLAllocation.generateDisplayLists(1);
    }

    if (displayListSelection == 0) {
      FMLLog.warning("Unable to create a displayList in BlockVoxelMultiSelector::createShadowRenderList");
      return;
    }

    Tessellator tessellator = Tessellator.instance;
    GL11.glNewList(displayListSelection, GL11.GL_COMPILE);
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(Colour.BLACK_40.R, Colour.BLACK_40.G, Colour.BLACK_40.B, Colour.BLACK_40.A);
    GL11.glLineWidth(2.0F);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDepthMask(false);

    final int FRAME_HEIGHT = 256;
    tessellator.startDrawing(GL11.GL_LINE_LOOP);
    tessellator.addVertex(0.0, 0.0, 0.0);
    tessellator.addVertex(1.0, 0.0, 0.0);
    tessellator.addVertex(1.0, -FRAME_HEIGHT, 0.0);
    tessellator.addVertex(0.0, -FRAME_HEIGHT, 0.0);
    tessellator.draw();

    for (int y = -1; y > -FRAME_HEIGHT; --y) {
      tessellator.startDrawing(GL11.GL_LINE);
      tessellator.addVertex(0.0, y, 0.0);
      tessellator.addVertex(1.0, y, 0.0);
      tessellator.draw();
    }

    GL11.glDepthMask(true);
    GL11.glPopAttrib();
    GL11.glEndList();
  }

  private void createShadowRenderList(World world)
  {
    generateWireFrameStrip();



/*
    int [] convexHullXneg = new int[zSize];
    int [] convexHullXpos = new int[zSize];
    int [] convexHullZneg = new int[xSize];
    int [] convexHullZpos = new int[xSize];

    // eliminate all pixels which are not directly illuminated from both x and from z (i.e. are outermost in both x and z)

    int x, y, z;
    for (x = 0; x < xSize; ++x) {
      for (z = 0; z < zSize && !shadow.getVoxel(x, 0, z); ++z) {} ;
      convexHullZneg[x] = z;
      for (z = zSize - 1; z >= 0 && !shadow.getVoxel(x, 0, z); --z) {} ;
      convexHullZpos[x] = z;
    }

    for (z = 0; z < zSize; ++z) {
      for (x = 0; x < xSize && !shadow.getVoxel(x, 0, z); ++x) {} ;
      convexHullXneg[z] = x;

      for (x = xSize-1; x >= 0 & !shadow.getVoxel(x, 0, z); --x) {};
      convexHullXpos[z] = x;
    }

    wireFrameXnegY = new ArrayList<ChunkCoordinates>();
    wireFrameXposY = new ArrayList<ChunkCoordinates>();
    wireFrameZnegY = new ArrayList<ChunkCoordinates>();
    wireFrameZposY = new ArrayList<ChunkCoordinates>();

    for (x = 0; x < xSize; ++x) {
      z = convexHullZneg[x];
      if (z < zSize
          && (convexHullXneg[z] == x || convexHullXpos[z] == x)) {
        wireFrameZnegY.add(new ChunkCoordinates(x, 0, z));
      }
      z = convexHullZpos[x];
      if (z >= 0
          && (convexHullXneg[z] == x || convexHullXpos[z] == x)) {
        wireFrameZposY.add(new ChunkCoordinates(x, 0, z));
      }
    }

    for (z = 0; z < zSize; ++z) {
      x = convexHullXneg[z];
      if (x < xSize
              && (convexHullZneg[x] == z || convexHullZpos[x] == z)) {
        wireFrameXnegY.add(new ChunkCoordinates(x, 0, z));
      }
      x = convexHullXpos[z];
      if (x >= 0
              && (convexHullZneg[x] == z || convexHullZpos[x] == z)) {
        wireFrameXposY.add(new ChunkCoordinates(x, 0, z));
      }
    }

    for (ChunkCoordinates coordinates : wireFrameXnegY) {
      x = coordinates.posX;
      z = coordinates.posZ;
      for (y = 0; y < ySize && !selection.getVoxel(x, y, z); ++y) {};
      coordinates.posY = y;
    }

    for (ChunkCoordinates coordinates : wireFrameZY) {
      x = coordinates.posX;
      z = coordinates.posZ;
      for (y = 0; y < ySize && !selection.getVoxel(x, y, z); ++y) {};
      coordinates.posY = y;
    }
*/
  }


  /**
   * render the current selection (must have called createRenderList previously).  Caller should set gLTranslatef so that the render starts in the
   *   correct spot  (the min[x,y,z] corner of the VoxelSelection will be drawn at [0,0,0])
   */
  public void renderSelection(ChunkCoordinates worldZeroPoint)
  {
    if (displayListSelection == 0) {
      return;
    }

    GL11.glPushMatrix();
    GL11.glCallList(displayListSelection);
    GL11.glPopMatrix();
  }

  /**
   * selectFill is used to select a flood fill of blocks which match the starting block, and return a list of their coordinates.
   * Starting from the block identified by mouseTarget, the selection will flood fill out in three directions
   * depending on diagonalOK it will follow diagonals or only the cardinal directions.
   * Keeps going until it reaches maxBlockCount, y goes outside the valid range.  The search algorithm is to look for closest blocks first
   *   ("closest" meaning the shortest distance travelled along the blob being created)
   *
   * @param mouseTarget the block under the player's cursor.  Uses [x,y,z]
   * @param world       the world
   * @param maxBlockCount the maximum number of blocks to select
   * @param diagonalOK    if true, diagonal 45 degree lines are allowed
   * @param matchAnyNonAir
   * @param xMin  the fill will not extend below xMin.  Likewise it will not extend above xMax.  Similar for y, z.
   */
/*
  public static List<ChunkCoordinates> selectFill(MovingObjectPosition mouseTarget, World world,
                                                  int maxBlockCount, boolean diagonalOK, boolean matchAnyNonAir,
                                                  int xMin, int xMax, int yMin, int yMax, int zMin, int zMax)
  {

    // lookup table to give the possible search directions for non-diagonal and diagonal respectively
    final int NON_DIAGONAL_DIRECTIONS = 6;
    final int ALL_DIRECTIONS = 26;
    final int searchDirectionsX[] = {+0, +0, +0, +0, -1, +1,   // non-diagonal
            +1, +0, -1, +0,   +1, +1, -1, -1,   +1, +0, -1, +0,  // top, middle, bottom "edge" blocks
            +1, +1, -1, -1,   +1, +1, -1, -1                   // top, bottom "corner" blocks
    };
    final int searchDirectionsY[] = {-1, +1, +0, +0, +0, +0,   // non-diagonal
            +1, +1, +1, +1,   +0, +0, +0, +0,   -1, -1, -1, -1,   // top, middle, bottom "edge" blocks
            +1, +1, +1, +1,   -1, -1, -1, -1                      // top, bottom "corner" blocks
    };

    final int searchDirectionsZ[] = {+0, +0, -1, +1, +0, +0,   // non-diagonal
            +0, -1, +0, +1,   +1, -1, -1, +1,   +0, -1, +0, +1,   // top, middle, bottom "edge" blocks
            +1, -1, -1, +1,   +1, -1, -1, +1
    };

    List<ChunkCoordinates> selection = new ArrayList<ChunkCoordinates>();

    if (mouseTarget == null || mouseTarget.typeOfHit != EnumMovingObjectType.TILE) return selection;

    ChunkCoordinates startingBlock = new ChunkCoordinates();
    startingBlock.posX = mouseTarget.blockX;
    startingBlock.posY = mouseTarget.blockY;
    startingBlock.posZ = mouseTarget.blockZ;
    selection.add(startingBlock);

    int blockToReplaceID = world.getBlockId(startingBlock.posX, startingBlock.posY, startingBlock.posZ);
    int blockToReplaceMetadata = world.getBlockMetadata(startingBlock.posX, startingBlock.posY, startingBlock.posZ);

    final int INITIAL_CAPACITY = 128;
    Set<ChunkCoordinates> locationsFilled = new HashSet<ChunkCoordinates>(INITIAL_CAPACITY);                                   // locations which have been selected during the fill
    Deque<SearchPosition> currentSearchPositions = new LinkedList<SearchPosition>();
    Deque<SearchPosition> nextDepthSearchPositions = new LinkedList<SearchPosition>();
    ChunkCoordinates checkPosition = new ChunkCoordinates(0,0,0);
    ChunkCoordinates checkPositionSupport = new ChunkCoordinates(0,0,0);

    locationsFilled.add(startingBlock);
    currentSearchPositions.add(new SearchPosition(startingBlock));

    // algorithm is:
    //   for each block in the list of search positions, iterate through each adjacent block to see whether it meets the criteria for expansion:
    //     a) matches the block-to-be-replaced (if matchAnyNonAir: non-air, otherwise if blockID and metaData match.  For lava or water metadata doesn't need to match).
    //     b) hasn't been filled already during this contour search
    //   if the criteria are met, select the block and add it to the list of blocks to be search next round.
    //   if the criteria aren't met, keep trying other directions from the same position until all positions are searched.  Then delete the search position and move onto the next.
    //   This will ensure that the fill spreads evenly out from the starting point.   Check the boundary to stop fill spreading outside it.

    while (!currentSearchPositions.isEmpty() && selection.size() < maxBlockCount) {
      SearchPosition currentSearchPosition = currentSearchPositions.getFirst();
      checkPosition.set(currentSearchPosition.chunkCoordinates.posX + searchDirectionsX[currentSearchPosition.nextSearchDirection],
              currentSearchPosition.chunkCoordinates.posY + searchDirectionsY[currentSearchPosition.nextSearchDirection],
              currentSearchPosition.chunkCoordinates.posZ + searchDirectionsZ[currentSearchPosition.nextSearchDirection]
      );
      if (    checkPosition.posX >= xMin && checkPosition.posX <= xMax
              &&  checkPosition.posY >= yMin && checkPosition.posY <= yMax
              &&  checkPosition.posZ >= zMin && checkPosition.posZ <= zMax
              && !locationsFilled.contains(checkPosition)) {
        boolean blockIsSuitable = false;

        int blockToCheckID = world.getBlockId(checkPosition.posX, checkPosition.posY, checkPosition.posZ);

        if (matchAnyNonAir && blockToCheckID != 0) {
          blockIsSuitable = true;
        } else if (blockToCheckID == blockToReplaceID) {
          if (world.getBlockMetadata(checkPosition.posX, checkPosition.posY, checkPosition.posZ) == blockToReplaceMetadata) {
            blockIsSuitable = true;
          } else {
            if (Block.blocksList[blockToCheckID].blockMaterial == Material.lava
                    || Block.blocksList[blockToCheckID].blockMaterial == Material.water) {
              blockIsSuitable = true;
            }
          }
        }

        if (blockIsSuitable) {
          ChunkCoordinates newChunkCoordinate = new ChunkCoordinates(checkPosition);
          SearchPosition nextSearchPosition = new SearchPosition(newChunkCoordinate);
          nextDepthSearchPositions.addLast(nextSearchPosition);
          locationsFilled.add(newChunkCoordinate);
          selection.add(newChunkCoordinate);
        }
      }
      ++currentSearchPosition.nextSearchDirection;
      if (currentSearchPosition.nextSearchDirection >= (diagonalOK ? ALL_DIRECTIONS : NON_DIAGONAL_DIRECTIONS)) {
        currentSearchPositions.removeFirst();
        if (currentSearchPositions.isEmpty()) {
          Deque<SearchPosition> temp = currentSearchPositions;
          currentSearchPositions = nextDepthSearchPositions;
          nextDepthSearchPositions = temp;
        }
      }
    }

    return selection;
  }
*/
  /**
   * see selectFill above
   * @param mouseTarget
   * @param world
   * @param maxBlockCount
   * @param diagonalOK
   * @return
   */
/*
  public static List<ChunkCoordinates> selectFill(MovingObjectPosition mouseTarget, World world,
                                                  int maxBlockCount, boolean diagonalOK)
  {
    return selectFill(mouseTarget, world, maxBlockCount, diagonalOK, false,
            Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 255, Integer.MIN_VALUE, Integer.MAX_VALUE);
  }
*/

}
