package speedytools.clientside.selections;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.Icon;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import speedytools.common.blocks.RegistryForBlocks;
import speedytools.common.selections.VoxelSelection;
import speedytools.common.utilities.Colour;
import speedytools.common.utilities.UsefulConstants;

/**
 * Created by TheGreyGhost on 10/07/14.
 * Used to render the selection.
 * Typical usage:
 * 1) create new
 * 2) createFromSelection() to set up
 * 3) render(x, y, z, renderdistance)
 * 4) IMPORTANT! before discarding the instance, call release() to release the OpenGL resources
 */
public class BlockVoxelMultiSelectorRenderer
{

  /**
   * releases the GL11 render lists associated with this instance
   *
   */
  public void release()
  {
    if (displayListWireFrameXY != 0) {
      GL11.glDeleteLists(displayListWireFrameXY, 1);
      displayListWireFrameXY = 0;
    }
    if (displayListWireFrameYZ != 0) {
      GL11.glDeleteLists(displayListWireFrameYZ, 1);
      displayListWireFrameYZ = 0;
    }
    if (displayListWireFrameXZ != 0) {
      GL11.glDeleteLists(displayListWireFrameXZ, 1);
      displayListWireFrameXZ = 0;
    }
    if (displayListCubesBase != 0 && displayListCubesCount > 0) {
      GL11.glDeleteLists(displayListCubesBase, displayListCubesCount);
      displayListCubesBase = 0;
      displayListCubesCount = 0;
    }
    mode = OperationInProgress.INVALID;
  }


  private int displayListWireFrameXY = 0;
  private int displayListWireFrameYZ = 0;
  private int displayListWireFrameXZ = 0;

  private final int DISPLAY_LIST_XSIZE = 16;
  private final int DISPLAY_LIST_YSIZE = 16;
  private final int DISPLAY_LIST_ZSIZE = 16;
  // display list for the blocks in the selection: each displaylist renders DISPLAY_LIST_XSIZE * YSIZE * ZSIZE blocks
  private int displayListCubesBase = 0;
  private int displayListCubesCount = 0;
  int chunkCountX, chunkCountY, chunkCountZ;
  int xSize, ySize, zSize;

  // get the display list for the given chunk
  private int getDisplayListIndex(int cx, int cy, int cz) {
    return displayListCubesBase + cx + cy * chunkCountX + cz * chunkCountX * chunkCountY;
  }

  private enum OperationInProgress {
    INVALID, IN_PROGRESS, COMPLETE
  }
  private OperationInProgress mode = OperationInProgress.INVALID;
  int cxCurrent, cyCurrent, czCurrent;

  /**
   * create a render list for the current selection.
   * Quads, with lines to outline them
   * @param world
   */
  public void createRenderListStart(World world, int wxOrigin, int wyOrigin, int wzOrigin, VoxelSelection voxelSelection)
  {
    release();
    displayListWireFrameXY = GLAllocation.generateDisplayLists(1);
    displayListWireFrameXZ = GLAllocation.generateDisplayLists(1);
    displayListWireFrameYZ = GLAllocation.generateDisplayLists(1);

    int displayListCount = 1;
    xSize = voxelSelection.getXsize();
    ySize = voxelSelection.getYsize();
    zSize = voxelSelection.getZsize();
    chunkCountX = ((voxelSelection.getXsize() - 1) / DISPLAY_LIST_XSIZE) + 1;
    chunkCountY = ((voxelSelection.getYsize() - 1) / DISPLAY_LIST_YSIZE) + 1;
    chunkCountZ = ((voxelSelection.getZsize() - 1) / DISPLAY_LIST_ZSIZE) + 1;

    if (voxelSelection.getXsize() > 0 && voxelSelection.getYsize() > 0 && voxelSelection.getZsize() > 0) {
      displayListCount = chunkCountX * chunkCountY * chunkCountZ;
    }

    displayListCubesBase = GLAllocation.generateDisplayLists(displayListCount);
    if (displayListCubesBase == 0 || displayListWireFrameXY == 0 || displayListWireFrameXZ == 0 || displayListWireFrameYZ == 0) {
      release();
      FMLLog.warning("Unable to create a displayList in BlockVoxelMultiSelectorRenderer::createRenderList");
      return;
    }
    displayListCubesCount = displayListCount;

    createMeshRenderLists(voxelSelection.getXsize(), voxelSelection.getYsize(), voxelSelection.getZsize());

    cxCurrent = 0;
    cyCurrent = 0;
    czCurrent = 0;

    mode = OperationInProgress.IN_PROGRESS;
  }

  /**
   * create a render list for the current selection.
   * Quads, with lines to outline them
   * @param world
   */
  public float createRenderListContinue(World world, int wxOrigin, int wyOrigin, int wzOrigin, VoxelSelection voxelSelection, long maxTimeInNS)
  {
    if (mode != OperationInProgress.IN_PROGRESS) {
      FMLLog.severe("Mode should be IN_PROGRESS in BlockVoxelMultiSelectorRenderer::createRenderListContinue, but was actually " + mode);
      return -1;
    }

    long startTime = System.nanoTime();
    final double NUDGE_DISTANCE = 0.01;
    final double FRAME_NUDGE_DISTANCE = NUDGE_DISTANCE + 0.002;

    for (; cxCurrent < chunkCountX; ++cxCurrent, cyCurrent = 0) {
      for (; cyCurrent < chunkCountY; ++cyCurrent, czCurrent = 0) {
        for (; czCurrent < chunkCountZ; ++czCurrent) {
          if (System.nanoTime() - startTime >= maxTimeInNS) return (cxCurrent / (float)chunkCountX);

          GL11.glNewList(getDisplayListIndex(cxCurrent, cyCurrent, czCurrent), GL11.GL_COMPILE);
          GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
          GL11.glDisable(GL11.GL_CULL_FACE);
          GL11.glDisable(GL11.GL_LIGHTING);
          GL11.glEnable(GL11.GL_TEXTURE_2D);
          Tessellator tessellator = Tessellator.instance;
          tessellateSurfaceWithTexture(world, voxelSelection, wxOrigin, wyOrigin, wzOrigin,
                  cxCurrent * DISPLAY_LIST_XSIZE, cyCurrent * DISPLAY_LIST_YSIZE, czCurrent * DISPLAY_LIST_ZSIZE, tessellator, WhatToDraw.FACES, NUDGE_DISTANCE);

          GL11.glEnable(GL11.GL_BLEND);
          GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
          GL11.glColor4f(Colour.BLACK_40.R, Colour.BLACK_40.G, Colour.BLACK_40.B, Colour.BLACK_40.A);
          GL11.glLineWidth(2.0F);
          GL11.glDisable(GL11.GL_TEXTURE_2D);
          GL11.glDepthMask(false);
          tessellateSurface(voxelSelection, cxCurrent * DISPLAY_LIST_XSIZE, cyCurrent * DISPLAY_LIST_YSIZE, czCurrent * DISPLAY_LIST_ZSIZE, tessellator, WhatToDraw.WIREFRAME, FRAME_NUDGE_DISTANCE);

          GL11.glDepthMask(true);
          GL11.glPopAttrib();
          GL11.glEndList();
        }
      }
    }
    mode = OperationInProgress.COMPLETE;
    return -1;
  }

//  int debugCount = 0;
  /**
   * render the current selection (must have called createRenderList previously).  Caller should set gLTranslatef so that the player's eyes are at [0,0,0]
   * playerRelativePos is position of the player relative to the minimum [x,y,z] corner of the VoxelSelection
   */
  public void renderSelection(Vec3 playerRelativePos, int blockRenderDistance, byte clockwiseRotationCount, boolean flippedX)
  {
    if (displayListCubesBase == 0) {
      return;
    }

    try {
      GL11.glPushMatrix();
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT);

      GL11.glTranslated(-playerRelativePos.xCoord, -playerRelativePos.yCoord, -playerRelativePos.zCoord);
      final int CX_MIN = Math.max(0, (int)((playerRelativePos.xCoord - blockRenderDistance)/ DISPLAY_LIST_XSIZE));
      final int CY_MIN = Math.max(0, (int)((playerRelativePos.yCoord - blockRenderDistance)/ DISPLAY_LIST_YSIZE));
      final int CZ_MIN = Math.max(0, (int)((playerRelativePos.zCoord - blockRenderDistance)/ DISPLAY_LIST_ZSIZE));
      final int CX_MAX = Math.min(chunkCountX - 1, (int)((playerRelativePos.xCoord + blockRenderDistance)/ DISPLAY_LIST_XSIZE));
      final int CY_MAX = Math.min(chunkCountY - 1, (int)((playerRelativePos.yCoord + blockRenderDistance)/ DISPLAY_LIST_YSIZE));
      final int CZ_MAX = Math.min(chunkCountZ - 1, (int)((playerRelativePos.zCoord + blockRenderDistance)/ DISPLAY_LIST_ZSIZE));

      if (flippedX) {  // flip around the midpoint
        GL11.glTranslatef(xSize / 2.0F, 0, 0);
        GL11.glScaled(-1, 1, 1);
        GL11.glTranslatef(-xSize / 2.0F, 0, 0);
      }

      if (clockwiseRotationCount > 0) { // rotate around the midpoint
        GL11.glTranslatef(xSize / 2.0F, 0, zSize / 2.0F);
        GL11.glRotatef(clockwiseRotationCount * -90, 0, 1, 0);
        GL11.glTranslatef(-xSize / 2.0F, 0, -zSize / 2.0F);
      }

      try {
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        for (int cy = CY_MIN; cy <= CY_MAX; ++cy) {
          for (int cx = CX_MIN; cx <= CX_MAX; ++cx) {
            for (int cz = CZ_MIN; cz <= CZ_MAX; ++cz) {
              GL11.glColor4f(Colour.PINK_100.R, Colour.PINK_100.G, Colour.PINK_100.B, 0.4F);
              GL11.glCallList(getDisplayListIndex(cx, cy, cz));
            }
          }
        }
      } finally {
        GL11.glPopAttrib();
        GL11.glPopMatrix();
      }

      // cull the back faces of the grid:
      // only draw a face if you can see the front of it
      //   eg for xpos face - if player is to the right, for xneg - if player is to the left.  if between don't draw either one.
      // exception: if inside the cube, render all faces.  Yneg is always drawn.
      boolean inside =     (playerRelativePos.xCoord >= 0 && playerRelativePos.xCoord <= xSize)
              && (                                 playerRelativePos.yCoord <= ySize)
              && (playerRelativePos.zCoord >= 0 && playerRelativePos.zCoord <= zSize);

      final Colour WALL_GRID_COLOUR = Colour.BLACK_40;
      final Colour FLOOR_GRID_COLOUR = Colour.WHITE_40;

      GL11.glColor4f(WALL_GRID_COLOUR.R, WALL_GRID_COLOUR.G, WALL_GRID_COLOUR.B, WALL_GRID_COLOUR.A);

      if (inside || playerRelativePos.xCoord < 0) {
        GL11.glPushMatrix();
        GL11.glTranslated(0, ySize, 0);
        GL11.glCallList(displayListWireFrameYZ);
        GL11.glPopMatrix();
      }

      if (inside || playerRelativePos.xCoord > xSize) {
        GL11.glPushMatrix();
        GL11.glTranslated(xSize, ySize, 0);
        GL11.glCallList(displayListWireFrameYZ);
        GL11.glPopMatrix();
      }

      GL11.glColor4f(FLOOR_GRID_COLOUR.R, FLOOR_GRID_COLOUR.G, FLOOR_GRID_COLOUR.B, FLOOR_GRID_COLOUR.A);

      if (true) {
        GL11.glPushMatrix();
        GL11.glTranslated(0, 0, 0);
        GL11.glCallList(displayListWireFrameXZ);
        GL11.glPopMatrix();
      }

      GL11.glColor4f(WALL_GRID_COLOUR.R, WALL_GRID_COLOUR.G, WALL_GRID_COLOUR.B, WALL_GRID_COLOUR.A);

      if (inside || playerRelativePos.yCoord > ySize) {
        GL11.glPushMatrix();
        GL11.glTranslated(0, ySize, 0);
        GL11.glCallList(displayListWireFrameXZ);
        GL11.glPopMatrix();
      }

      if (inside || playerRelativePos.zCoord < 0) {
        GL11.glPushMatrix();
        GL11.glTranslated(0, ySize, 0);
        GL11.glCallList(displayListWireFrameXY);
        GL11.glPopMatrix();
      }

      if (inside || playerRelativePos.zCoord > zSize) {
        GL11.glPushMatrix();
        GL11.glTranslated(0, ySize, zSize);
        GL11.glCallList(displayListWireFrameXY);
        GL11.glPopMatrix();
      }
    } finally {
      GL11.glPopAttrib();
      GL11.glPopMatrix();
    }
  }

  enum WhatToDraw {FACES, WIREFRAME};

  private void tessellateSurface(VoxelSelection selection, int x0, int y0, int z0,
                                 Tessellator tessellator, WhatToDraw whatToDraw, double nudgeDistance)
  {
    int xNegNudge, xPosNudge, yNegNudge, yPosNudge, zNegNudge, zPosNudge;

    if (whatToDraw == WhatToDraw.FACES) {
      tessellator.startDrawingQuads();
    }

    // goes outside the VoxelSelection size, which always returns zero when out of bounds
    // "nudges" the quad boundaries to make solid blocks slightly larger, avoids annoying visual artifacts when overlapping with world blocks
    // three cases of nudge for each edge: internal (concave) edges = nudge inwards (-1), flat = no nudge (0), outer (convex) = nudge outwards (+1)

    for (int y = y0; y <= y0 + DISPLAY_LIST_YSIZE; ++y) {
      for (int z = z0; z <= z0 + DISPLAY_LIST_ZSIZE; ++z) {
        for (int x = x0; x <= x0 + DISPLAY_LIST_XSIZE; ++x) {
          if (selection.getVoxel(x, y, z)) {
            // xneg face
            if (!selection.getVoxel(x - 1, y, z)) {
              yNegNudge = (selection.getVoxel(x-1, y-1, z+0) ? -1 : (selection.getVoxel(x+0, y-1, z+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(x-1, y+1, z+0) ? -1 : (selection.getVoxel(x+0, y+1, z+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(x-1, y+0, z-1) ? -1 : (selection.getVoxel(x+0, y+0, z-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(x-1, y+0, z+1) ? -1 : (selection.getVoxel(x+0, y+0, z+1) ? 0 : 1) );

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertex(x - nudgeDistance, y     - yNegNudge * nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x - nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x - nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              tessellator.addVertex(x - nudgeDistance, y     - yNegNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // xpos face
            if (!selection.getVoxel(x + 1, y, z)) {
              yNegNudge = (selection.getVoxel(x+1, y-1, z+0) ? -1 : (selection.getVoxel(x+0, y-1, z+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(x+1, y+1, z+0) ? -1 : (selection.getVoxel(x+0, y+1, z+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(x+1, y+0, z-1) ? -1 : (selection.getVoxel(x+0, y+0, z-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(x+1, y+0, z+1) ? -1 : (selection.getVoxel(x+0, y+0, z+1) ? 0 : 1) );

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertex(x + 1 + nudgeDistance, y     - yNegNudge * nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x + 1 + nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x + 1 + nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              tessellator.addVertex(x + 1 + nudgeDistance, y     - yNegNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // yneg face
            if (!selection.getVoxel(x, y-1, z)) {
              xNegNudge = (selection.getVoxel(x-1, y-1, z+0) ? -1 : (selection.getVoxel(x-1, y+0, z+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(x+1, y-1, z+0) ? -1 : (selection.getVoxel(x+1, y+0, z+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(x+0, y-1, z-1) ? -1 : (selection.getVoxel(x+0, y+0, z-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(x+0, y-1, z+1) ? -1 : (selection.getVoxel(x+0, y+0, z+1) ? 0 : 1) );

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertex(x     - xNegNudge * nudgeDistance, y - nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x + 1 + xPosNudge * nudgeDistance, y - nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x + 1 + xPosNudge * nudgeDistance, y - nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              tessellator.addVertex(x     - xNegNudge * nudgeDistance, y - nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // ypos face
            if (!selection.getVoxel(x, y+1, z)) {
              xNegNudge = (selection.getVoxel(x-1, y+1, z+0) ? -1 : (selection.getVoxel(x-1, y+0, z+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(x+1, y+1, z+0) ? -1 : (selection.getVoxel(x+1, y+0, z+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(x+0, y+1, z-1) ? -1 : (selection.getVoxel(x+0, y+0, z-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(x+0, y+1, z+1) ? -1 : (selection.getVoxel(x+0, y+0, z+1) ? 0 : 1) );

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertex(x     - xNegNudge * nudgeDistance, y + 1 + nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x + 1 + xPosNudge * nudgeDistance, y + 1 + nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x + 1 + xPosNudge * nudgeDistance, y + 1 + nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              tessellator.addVertex(x     - xNegNudge * nudgeDistance, y + 1 + nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // zneg face
            if (!selection.getVoxel(x, y, z-1)) {
              xNegNudge = (selection.getVoxel(x-1, y+0, z-1) ? -1 : (selection.getVoxel(x-1, y+0, z+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(x+1, y+0, z-1) ? -1 : (selection.getVoxel(x+1, y+0, z+0) ? 0 : 1) );
              yNegNudge = (selection.getVoxel(x+0, y-1, z-1) ? -1 : (selection.getVoxel(x+0, y-1, z+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(x+0, y+1, z-1) ? -1 : (selection.getVoxel(x+0, y+1, z+0) ? 0 : 1) );

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertex(x     - xNegNudge * nudgeDistance, y     - yNegNudge * nudgeDistance, z - nudgeDistance);
              tessellator.addVertex(x + 1 + xPosNudge * nudgeDistance, y     - yNegNudge * nudgeDistance, z - nudgeDistance);
              tessellator.addVertex(x + 1 + xPosNudge * nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z - nudgeDistance);
              tessellator.addVertex(x     - xNegNudge * nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z - nudgeDistance);
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // zpos face
            if (!selection.getVoxel(x, y, z+1)) {
              xNegNudge = (selection.getVoxel(x-1, y+0, z+1) ? -1 : (selection.getVoxel(x-1, y+0, z+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(x+1, y+0, z+1) ? -1 : (selection.getVoxel(x+1, y+0, z+0) ? 0 : 1) );
              yNegNudge = (selection.getVoxel(x+0, y-1, z+1) ? -1 : (selection.getVoxel(x+0, y-1, z+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(x+0, y+1, z+1) ? -1 : (selection.getVoxel(x+0, y+1, z+0) ? 0 : 1) );

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertex(x     - xNegNudge * nudgeDistance, y     - yNegNudge * nudgeDistance, z + 1 + nudgeDistance);
              tessellator.addVertex(x + 1 + xPosNudge * nudgeDistance, y     - yNegNudge * nudgeDistance, z + 1 + nudgeDistance);
              tessellator.addVertex(x + 1 + xPosNudge * nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z + 1 + nudgeDistance);
              tessellator.addVertex(x     - xNegNudge * nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z + 1 + nudgeDistance);
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
          }
        }
      }
    }

    if (whatToDraw == WhatToDraw.FACES) {
      tessellator.draw();
    }
  }

  /**
   * draw the textured surface of this chunk starting from the [x0,y0,z0] index into the selection
   * @param world
   * @param wxOrigin world x of the selection
   * @param wyOrigin world y of the selection
   * @param wzOrigin world z of the selection
   * @param x0 start x coordinate
   * @param y0 start y coordinate
   * @param z0 start z coordinated
   * @param tessellator
   * @param whatToDraw
   * @param nudgeDistance how far to nudge the face (to prevent overlap)
   */
  private void tessellateSurfaceWithTexture(World world, VoxelSelection selection, int wxOrigin, int wyOrigin, int wzOrigin, int x0, int y0, int z0,
                                            Tessellator tessellator, WhatToDraw whatToDraw, double nudgeDistance)
  {
    int xNegNudge, xPosNudge, yNegNudge, yPosNudge, zNegNudge, zPosNudge;

    if (whatToDraw == WhatToDraw.FACES) {
      tessellator.startDrawingQuads();
    }

    // goes outside the VoxelSelection size, which always returns zero when out of bounds
    // "nudges" the quad boundaries to make solid blocks slightly larger, avoids annoying visual artifacts when overlapping with world blocks
    // three cases of nudge for each edge: internal (concave) edges = nudge inwards (-1), flat = no nudge (0), outer (convex) = nudge outwards (+1)

    for (int y = y0; y <= y0 + DISPLAY_LIST_YSIZE; ++y) {
      for (int z = z0; z <= z0 + DISPLAY_LIST_ZSIZE; ++z) {
        for (int x = x0; x <= x0 + DISPLAY_LIST_XSIZE; ++x) {
          if (selection.getVoxel(x, y, z)) {
            int wx = x + wxOrigin;
            int wy = y + wyOrigin;
            int wz = z + wzOrigin;
            int blockID = world.getBlockId(wx, wy, wz);
            Block block = Block.blocksList[blockID];
            if (block == null) block = RegistryForBlocks.blockSelectionFog;

            // xneg face
            if (!selection.getVoxel(x - 1, y, z)) {
              yNegNudge = (selection.getVoxel(x-1, y-1, z+0) ? -1 : (selection.getVoxel(x+0, y-1, z+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(x-1, y+1, z+0) ? -1 : (selection.getVoxel(x+0, y+1, z+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(x-1, y+0, z-1) ? -1 : (selection.getVoxel(x+0, y+0, z-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(x-1, y+0, z+1) ? -1 : (selection.getVoxel(x+0, y+0, z+1) ? 0 : 1) );
              Icon icon = block.getBlockTexture(world, wx, wy, wz, UsefulConstants.FACE_XNEG);

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertexWithUV(x - nudgeDistance, y - yNegNudge * nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMinU(), icon.getMaxV());
              tessellator.addVertexWithUV(x - nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMinU(), icon.getMinV());
              tessellator.addVertexWithUV(x - nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMaxU(), icon.getMinV());
              tessellator.addVertexWithUV(x - nudgeDistance, y - yNegNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMaxU(), icon.getMaxV());
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // xpos face
            if (!selection.getVoxel(x + 1, y, z)) {
              yNegNudge = (selection.getVoxel(x+1, y-1, z+0) ? -1 : (selection.getVoxel(x+0, y-1, z+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(x+1, y+1, z+0) ? -1 : (selection.getVoxel(x+0, y+1, z+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(x+1, y+0, z-1) ? -1 : (selection.getVoxel(x+0, y+0, z-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(x+1, y+0, z+1) ? -1 : (selection.getVoxel(x+0, y+0, z+1) ? 0 : 1) );
              Icon icon = block.getBlockTexture(world, wx, wy, wz, UsefulConstants.FACE_XPOS);

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertexWithUV(x + 1 + nudgeDistance, y - yNegNudge * nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMinU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMinU(), icon.getMinV());
              tessellator.addVertexWithUV(x + 1 + nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMaxU(), icon.getMinV());
              tessellator.addVertexWithUV(x + 1 + nudgeDistance, y - yNegNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMaxU(), icon.getMaxV());
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // yneg face
            if (!selection.getVoxel(x, y-1, z)) {
              xNegNudge = (selection.getVoxel(x-1, y-1, z+0) ? -1 : (selection.getVoxel(x-1, y+0, z+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(x+1, y-1, z+0) ? -1 : (selection.getVoxel(x+1, y+0, z+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(x+0, y-1, z-1) ? -1 : (selection.getVoxel(x+0, y+0, z-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(x+0, y-1, z+1) ? -1 : (selection.getVoxel(x+0, y+0, z+1) ? 0 : 1) );
              Icon icon = block.getBlockTexture(world, wx, wy, wz, UsefulConstants.FACE_YNEG);
              // NB yneg face is flipped left-right in vanilla
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertexWithUV(x - xNegNudge * nudgeDistance, y - nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMaxU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + xPosNudge * nudgeDistance, y - nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMinU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + xPosNudge * nudgeDistance, y - nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMinU(), icon.getMinV());
              tessellator.addVertexWithUV(x - xNegNudge * nudgeDistance, y - nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMaxU(), icon.getMinV());
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // ypos face
            if (!selection.getVoxel(x, y+1, z)) {
              xNegNudge = (selection.getVoxel(x-1, y+1, z+0) ? -1 : (selection.getVoxel(x-1, y+0, z+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(x+1, y+1, z+0) ? -1 : (selection.getVoxel(x+1, y+0, z+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(x+0, y+1, z-1) ? -1 : (selection.getVoxel(x+0, y+0, z-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(x+0, y+1, z+1) ? -1 : (selection.getVoxel(x+0, y+0, z+1) ? 0 : 1) );
              Icon icon = block.getBlockTexture(world, wx, wy, wz, UsefulConstants.FACE_YPOS);

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertexWithUV(x - xNegNudge * nudgeDistance, y + 1 + nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMinU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + xPosNudge * nudgeDistance, y + 1 + nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMaxU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + xPosNudge * nudgeDistance, y + 1 + nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMaxU(), icon.getMinV());
              tessellator.addVertexWithUV(x - xNegNudge * nudgeDistance, y + 1 + nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMinU(), icon.getMinV());
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // zneg face
            if (!selection.getVoxel(x, y, z-1)) {
              xNegNudge = (selection.getVoxel(x-1, y+0, z-1) ? -1 : (selection.getVoxel(x-1, y+0, z+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(x+1, y+0, z-1) ? -1 : (selection.getVoxel(x+1, y+0, z+0) ? 0 : 1) );
              yNegNudge = (selection.getVoxel(x+0, y-1, z-1) ? -1 : (selection.getVoxel(x+0, y-1, z+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(x+0, y+1, z-1) ? -1 : (selection.getVoxel(x+0, y+1, z+0) ? 0 : 1) );
              Icon icon = block.getBlockTexture(world, wx, wy, wz, UsefulConstants.FACE_ZNEG);

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertexWithUV(x - xNegNudge * nudgeDistance, y - yNegNudge * nudgeDistance, z - nudgeDistance, icon.getMaxU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + xPosNudge * nudgeDistance, y - yNegNudge * nudgeDistance, z - nudgeDistance, icon.getMinU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + xPosNudge * nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z - nudgeDistance, icon.getMinU(), icon.getMinV());
              tessellator.addVertexWithUV(x - xNegNudge * nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z - nudgeDistance, icon.getMaxU(), icon.getMinV());
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // zpos face
            if (!selection.getVoxel(x, y, z+1)) {
              xNegNudge = (selection.getVoxel(x-1, y+0, z+1) ? -1 : (selection.getVoxel(x-1, y+0, z+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(x+1, y+0, z+1) ? -1 : (selection.getVoxel(x+1, y+0, z+0) ? 0 : 1) );
              yNegNudge = (selection.getVoxel(x+0, y-1, z+1) ? -1 : (selection.getVoxel(x+0, y-1, z+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(x+0, y+1, z+1) ? -1 : (selection.getVoxel(x+0, y+1, z+0) ? 0 : 1) );
              Icon icon = block.getBlockTexture(world, wx, wy, wz, UsefulConstants.FACE_ZNEG);

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertexWithUV(x - xNegNudge * nudgeDistance, y - yNegNudge * nudgeDistance, z + 1 + nudgeDistance, icon.getMinU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + xPosNudge * nudgeDistance, y - yNegNudge * nudgeDistance, z + 1 + nudgeDistance, icon.getMaxU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + xPosNudge * nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z + 1 + nudgeDistance, icon.getMaxU(), icon.getMinV());
              tessellator.addVertexWithUV(x - xNegNudge * nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z + 1 + nudgeDistance, icon.getMinU(), icon.getMinV());
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
          }
        }
      }
    }

    if (whatToDraw == WhatToDraw.FACES) {
      tessellator.draw();
    }
  }


  /**
   * create the "cage" wireframes for the selection
   * @param xSize
   * @param ySize
   * @param zSize
   */
  private void createMeshRenderLists(int xSize, int ySize, int zSize)
  {
    final int MESH_HEIGHT = 256;
    generateWireFrame2DMesh(displayListWireFrameXY, xSize, MESH_HEIGHT,     0);
    generateWireFrame2DMesh(displayListWireFrameXZ, xSize,           0, zSize);
    generateWireFrame2DMesh(displayListWireFrameYZ,     0, MESH_HEIGHT, zSize);
  }

  /**
   * a vertical wireframe mesh made up of 1x1 squares, in the xy, yz, or xz plane
   * origin is the top (ymax) corner, i.e. the topmost square is [0,0,0], the bottommost square is +xcount, -ycount, zcount
   * one of the three must be 0 to specify which plane the mesh is in.
   */
  private void generateWireFrame2DMesh(int displayListNumber, int xcount, int ycount, int zcount)
  {
    if (displayListNumber == 0) return;
    assert (xcount >= 0 && ycount >= 0 && zcount >= 0);
    assert (xcount == 0 || ycount == 0 || zcount == 0);

    Tessellator tessellator = Tessellator.instance;
    GL11.glNewList(displayListNumber, GL11.GL_COMPILE);
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glLineWidth(2.0F);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDepthMask(false);

    tessellator.startDrawing(GL11.GL_LINE_LOOP);
    tessellator.addVertex(0.0, 0.0, 0.0);
    if (xcount == 0) {
      tessellator.addVertex(   0.0, -ycount,    0.0);
      tessellator.addVertex(   0.0, -ycount, zcount);
      tessellator.addVertex(   0.0,       0, zcount);
    } else {
      tessellator.addVertex(xcount,     0.0,    0.0);
      tessellator.addVertex(xcount, -ycount, zcount);
      tessellator.addVertex(   0.0, -ycount, zcount);
    }
    tessellator.draw();

    for (int x = 1; x < xcount; ++x) {
      tessellator.startDrawing(GL11.GL_LINES);
      tessellator.addVertex(x,     0.0,    0.0);
      tessellator.addVertex(x, -ycount, zcount);
      tessellator.draw();
    }
    for (int y = 1; y < ycount; ++y) {
      tessellator.startDrawing(GL11.GL_LINES);
      tessellator.addVertex(   0.0, -y,    0.0);
      tessellator.addVertex(xcount, -y, zcount);
      tessellator.draw();
    }
    for (int z = 1; z < zcount; ++z) {
      tessellator.startDrawing(GL11.GL_LINES);
      tessellator.addVertex(   0.0,     0.0, z);
      tessellator.addVertex(xcount, -ycount, z);
      tessellator.draw();
    }

    GL11.glDepthMask(true);
    GL11.glPopAttrib();
    GL11.glEndList();
  }

}
