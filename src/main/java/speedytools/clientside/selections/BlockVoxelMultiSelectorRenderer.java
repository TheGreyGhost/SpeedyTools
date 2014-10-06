package speedytools.clientside.selections;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.opengl.GL11;
import speedytools.common.blocks.RegistryForBlocks;
import speedytools.common.selections.VoxelSelection;
import speedytools.common.selections.VoxelSelectionWithOrigin;
import speedytools.common.utilities.Colour;
import speedytools.common.utilities.Pair;
import speedytools.common.utilities.QuadOrientation;
import speedytools.common.utilities.UsefulConstants;

import java.util.BitSet;
import java.util.LinkedList;

/**
* Created by TheGreyGhost on 10/07/14.
* Used to render the selection.
* Typical usage:
* 1) create new
* 2) createFromSelection() to set up
* 3) render(x, y, z, renderdistance)
* 4) IMPORTANT! before discarding the instance, call release() to release the OpenGL resources
 * Internally - stores a renderlist for each chunk, aligned to the world origin chunks
*/
public class BlockVoxelMultiSelectorRenderer
{

  /**
   * releases the GL11 render lists associated with this instance
   *
   */
  public void release()
  {
    for (Pair<Integer, Integer> allocation : displayListAllocations) {
      GL11.glDeleteLists(allocation.getFirst(), allocation.getSecond());
    }
    displayListAllocations.clear();
    displayListWireFrameXY = 0;
    displayListWireFrameYZ = 0;
    displayListWireFrameXZ = 0;

    mode = OperationInProgress.INVALID;
    displayListMapping = null;
  }

  /**
   * gets the origin for the renderer in world coordinates
   *
   * @return the origin for the selection in world coordinates
   */
  public ChunkCoordinates getWorldOrigin() {
    return new ChunkCoordinates(sourceWXorigin, sourceWYorigin, sourceWZorigin);
  }

  private int displayListWireFrameXY = 0;
  private int displayListWireFrameYZ = 0;
  private int displayListWireFrameXZ = 0;

  private final int DISPLAY_LIST_XSIZE = 16;    // to align with world chunks
  private final int DISPLAY_LIST_YSIZE = 16;
  private final int DISPLAY_LIST_ZSIZE = 16;
  // display list for the blocks in the selection: each displaylist renders DISPLAY_LIST_XSIZE * YSIZE * ZSIZE blocks
//  private int displayListCubesBase = 0;
//  private int displayListCubesCount = 0;
  private int chunkCountX, chunkCountY, chunkCountZ;
  private int xSize, ySize, zSize;
  private int xOffset, yOffset, zOffset;
  private int sourceWXorigin, sourceWYorigin, sourceWZorigin;
  private BitSet unloadedChunks = new BitSet();  // unloadedChunks[cx + cz * chunkCountX] = 1 if this chunk was unloaded when trying to get textures -> don't know what the block is

  private int [] displayListMapping;  // mapping of the cx, cy, cz to displayListIndex used by OpenGL.

  // holds the allocated display lists: <start index, count>
  LinkedList<Pair<Integer, Integer>> displayListAllocations = new LinkedList<Pair<Integer, Integer>>();

  // get the display list for the given chunk
  private int getDisplayListIndex(int cx, int cy, int cz) {
    return displayListMapping[cx + cy * chunkCountX + cz * chunkCountX * chunkCountY];
//    return displayListCubesBase + cx + cy * chunkCountX + cz * chunkCountX * chunkCountY;
  }

  /** resizes the renderer to a new position and size.  Retains (repositions) the existing render lists, doesn't update
   *   any new chunks
   * @param newWXorigin  the new world origin
   * @param newWYorigin
   * @param newWZorigin
   * @param newXsize the
   * @param newYsize
   * @param newZsize
   */
  public void resize(int newWXorigin, int newWYorigin, int newWZorigin, int newXsize, int newYsize, int newZsize)
  {
    int startChunkX = newWXorigin >> 4;
    int endChunkX = (newWXorigin + newXsize - 1) >> 4;
    int startChunkY = newWYorigin >> 4;
    int endChunkY = (newWYorigin + newYsize - 1) >> 4;
    int startChunkZ = newWZorigin >> 4;
    int endChunkZ = (newWZorigin + newZsize - 1) >> 4;
    int newChunkCountX = endChunkX - startChunkX + 1;
    int newChunkCountY = endChunkY - startChunkY + 1;
    int newchunkCountZ = endChunkZ - startChunkZ + 1;

    int newDisplayListCount = resizeDisplayList(0, false, startChunkX - (sourceWXorigin >> 4), startChunkY - (sourceWYorigin >> 4), startChunkZ - (sourceWZorigin >> 4),
                                                newChunkCountX, newChunkCountY, newchunkCountZ);
    int displayListCubesBase = 0;
    if (newDisplayListCount > 0) {
      displayListCubesBase = GLAllocation.generateDisplayLists(newDisplayListCount);
      if (displayListCubesBase == 0) {
        release();
        FMLLog.warning("Unable to create a displayList in BlockVoxelMultiSelectorRenderer::resize");
        return;
      }
      displayListAllocations.add(new Pair<Integer, Integer>(displayListCubesBase, newDisplayListCount));
    }
    resizeDisplayList(displayListCubesBase, true, startChunkX - (sourceWXorigin >> 4), startChunkY - (sourceWYorigin >> 4), startChunkZ - (sourceWZorigin >> 4),
                       newChunkCountX, newChunkCountY, newchunkCountZ);

    xSize = newXsize;
    ySize = newYsize;
    zSize = newZsize;

    chunkCountX = newChunkCountX;
    chunkCountY = newChunkCountY;
    chunkCountZ = newchunkCountZ;

    xOffset = newWXorigin & 0x0f;
    yOffset = newWYorigin & 0x0f;
    zOffset = newWZorigin & 0x0f;

    sourceWXorigin = newWXorigin;
    sourceWYorigin = newWYorigin;
    sourceWZorigin = newWZorigin;
    unloadedChunks.set(0, chunkCountX * chunkCountZ);

    createMeshRenderLists(newXsize, newYsize, newZsize);
  }


  /**
   * resizes the render displayList; keeps the original displayLists but moves them to their new position relative to the new origin
   * fills newly created chunk slots with the indicated displaylist numbers
   * @param nextListIndexStart the index of the first displaylist to use for new chunks; n empty chunks will use indices nextListIndexStart to nextListIndexStart + n-1 inclusive
   * @param overwrite if true, create the new display list and move the old entries.  If false, just count the number of new displaylistindices required.
   * @param newCX0 the origin of the render relative to the old origin (eg newCX0 == -2 means that the new display starts 2 chunks to the left of the old)
   * @param newCY0
   * @param newCZ0
   * @param newCXsize the number of x chunks in the new render
   * @param newCYsize
   * @param newCZsize
   * @return the number of new displaylists added/required
   */
  private int resizeDisplayList(int nextListIndexStart, boolean overwrite,
                                int newCX0, int newCY0, int newCZ0, int newCXsize, int newCYsize, int newCZsize)
  {
    assert (newCXsize > 0);
    assert (newCYsize > 0);
    assert (newCZsize > 0);

    int [] newDisplayListMapping = new int[0];
    if (overwrite) {
      newDisplayListMapping = new int[newCXsize * newCYsize * newCZsize];
    }
    int newChunkCount = 0;

    for (int cx = 0; cx < newCXsize; ++cx) {
      for (int cy = 0; cy < newCYsize; ++cy) {
        for (int cz = 0; cz < newCZsize; ++cz) {
          boolean isExistingChunk = (cx + newCX0 >= 0 && cx + newCX0 < chunkCountX)
                                    && (cy + newCY0 >= 0 && cy + newCY0 < chunkCountY)
                                    && (cz + newCZ0 >= 0 && cz + newCZ0 < chunkCountZ);
          if (!isExistingChunk) {
            ++newChunkCount;
          }
          if (overwrite) {
            int newChunkIndex = cx + cy * newCXsize + cz * newCXsize * newCYsize;
            if (isExistingChunk) {
              newDisplayListMapping[newChunkIndex] = getDisplayListIndex(cx + newCX0, cy + newCY0, cz + newCZ0);
            } else {
              newDisplayListMapping[newChunkIndex] = nextListIndexStart++;
            }
          }
        }
      }
    }

    if (overwrite) {
      displayListMapping = newDisplayListMapping;
    }
    return newChunkCount;
  }

  /**
   * Refresh the renderlist for this renderer based on new information
   */
  public void refreshRenderListStart()
  {
    cxCurrent = 0;
    cyCurrent = 0;
    czCurrent = 0;

    mode = OperationInProgress.IN_PROGRESS;
  }
  /**
   * Refresh the renderlist for this renderer based on new information
   * @param world
   * @param unknownVoxels
   * @param maxTimeInNS the maximum amount of time to spend before returning, in ns
   * @return the estimated fraction complete [0 .. 1]; or -1 if complete
   */
 public float refreshRenderListContinue(World world, VoxelSelectionWithOrigin selectedVoxels, VoxelSelectionWithOrigin unknownVoxels, long maxTimeInNS)
  {
    if (mode == OperationInProgress.COMPLETE) return -1;
    if (mode != OperationInProgress.IN_PROGRESS) {
      FMLLog.severe("Mode should be IN_PROGRESS in BlockVoxelMultiSelectorRenderer::refreshRenderListContinue, but was actually " + mode);
      return -1;
    }

    long startTime = System.nanoTime();

    for (; cxCurrent < chunkCountX; ++cxCurrent, cyCurrent = 0) {
      for (; cyCurrent < chunkCountY; ++cyCurrent, czCurrent = 0) {
        for (; czCurrent < chunkCountZ; ++czCurrent) {
          if (System.nanoTime() - startTime >= maxTimeInNS) return (cxCurrent / (float)chunkCountX);
          renderThisChunk(world, selectedVoxels, unknownVoxels, sourceWXorigin, sourceWYorigin, sourceWZorigin, cxCurrent, cyCurrent, czCurrent);
          Chunk chunk = world.getChunkFromBlockCoords(sourceWXorigin + cxCurrent * DISPLAY_LIST_XSIZE, sourceWZorigin + czCurrent * DISPLAY_LIST_ZSIZE);
          if (!chunk.isEmpty()) {
            unloadedChunks.clear(cxCurrent + czCurrent * chunkCountX);
//            System.out.println("refresh clear [" + cxCurrent + ", " + czCurrent + "]");
          }
        }
      }
    }
    mode = OperationInProgress.COMPLETE;
    return -1;
  }

  private void renderThisChunk(World world, VoxelSelection selectedVoxels, VoxelSelection unknownVoxels,
                               int wxOrigin, int wyOrigin, int wzOrigin,
                               int cx, int cy, int cz)
  {
    final double NUDGE_DISTANCE = 0.01;
    final double FRAME_NUDGE_DISTANCE = NUDGE_DISTANCE + 0.002;

    GL11.glNewList(getDisplayListIndex(cx, cy, cz), GL11.GL_COMPILE);
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
    GL11.glDisable(GL11.GL_CULL_FACE);
    GL11.glDisable(GL11.GL_LIGHTING);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    Tessellator tessellator = Tessellator.instance;
    tessellateSurfaceWithTexture(world, selectedVoxels, unknownVoxels, wxOrigin, wyOrigin, wzOrigin,
            cx * DISPLAY_LIST_XSIZE - xOffset, cy * DISPLAY_LIST_YSIZE - yOffset, cz * DISPLAY_LIST_ZSIZE - zOffset,
            tessellator, WhatToDraw.FACES, NUDGE_DISTANCE);

    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(Colour.BLACK_40.R, Colour.BLACK_40.G, Colour.BLACK_40.B, Colour.BLACK_40.A);
    GL11.glLineWidth(2.0F);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDepthMask(false);
    tessellateSurface(selectedVoxels, unknownVoxels,
            cx * DISPLAY_LIST_XSIZE - xOffset, cy * DISPLAY_LIST_YSIZE - yOffset, cz * DISPLAY_LIST_ZSIZE  - zOffset,
            tessellator, WhatToDraw.WIREFRAME, FRAME_NUDGE_DISTANCE);

    GL11.glDepthMask(true);
    GL11.glPopAttrib();
    GL11.glEndList();
  }



  private enum OperationInProgress {
    INVALID, IN_PROGRESS, COMPLETE
  }
  private OperationInProgress mode = OperationInProgress.INVALID;
  int cxCurrent, cyCurrent, czCurrent;

  /**
   * Look for any chunks which have missing textures and check to see if they have been loaded; if so, update the render list
  * @param world
  * @param unknownVoxels
  * @param maxTimeInNS the maximum amount of time to spend before returning, in ns
  * @return true if there are no more unknown block textures
  */
  public boolean updateWithLoadedChunks(World world, VoxelSelectionWithOrigin selectedVoxels, VoxelSelectionWithOrigin unknownVoxels, long maxTimeInNS)
  {
    if (unloadedChunks.isEmpty()) return true;
    long startTime = System.nanoTime();

    for (int cx = 0; cx < chunkCountX; ++cx) {
      for (int cz = 0; cz < chunkCountZ; ++cz) {
        if (System.nanoTime() - startTime >= maxTimeInNS) return false;
        if (unloadedChunks.get(cx + cz * chunkCountX)) {
          Chunk chunk = world.getChunkFromBlockCoords(sourceWXorigin + cx * DISPLAY_LIST_XSIZE, sourceWZorigin + cz * DISPLAY_LIST_ZSIZE);
          if (!chunk.isEmpty()) {
            for (int cy = 0; cy < chunkCountY; ++cy) {
              renderThisChunk(world, selectedVoxels, unknownVoxels, sourceWXorigin, sourceWYorigin, sourceWZorigin, cx, cy, cz);
            }
            unloadedChunks.clear(cx + cz * chunkCountX);
            System.out.println("update clear [" + cx + ", " + cz + "]");
          }
        }
      }
    }
    return unloadedChunks.isEmpty();
  }

  /**
   * create a render list for the current selection.
   * Renders all voxels in selectedVoxels with the corresponding block texture, and also renders all voxels in unknownVoxels with an "unknown" texture
   * Quads, with lines to outline them
   * Aligns to world chunk boundaries
   * @param world
   * @param selectedVoxels the current selection
   * @param unknownVoxels any unknown voxels in the current selection (voxels that might be selected - not known).  Must be the same size [x,y,z] as selectedVoxels
   */
  public void createRenderListStart(World world, int wxOrigin, int wyOrigin, int wzOrigin, VoxelSelection selectedVoxels, VoxelSelection unknownVoxels)
  {
    release();
    displayListWireFrameXY = GLAllocation.generateDisplayLists(1);
    displayListWireFrameXZ = GLAllocation.generateDisplayLists(1);
    displayListWireFrameYZ = GLAllocation.generateDisplayLists(1);
    displayListAllocations.add(new Pair<Integer, Integer>(displayListWireFrameXY, 1));
    displayListAllocations.add(new Pair<Integer, Integer>(displayListWireFrameXZ, 1));
    displayListAllocations.add(new Pair<Integer, Integer>(displayListWireFrameYZ, 1));

    int displayListCount = 1;
    xSize = selectedVoxels.getxSize();
    ySize = selectedVoxels.getySize();
    zSize = selectedVoxels.getzSize();
    assert (xSize == unknownVoxels.getxSize());
    assert (ySize == unknownVoxels.getySize());
    assert (zSize == unknownVoxels.getzSize());

    int startChunkX = wxOrigin >> 4;
    int endChunkX = (wxOrigin + xSize - 1) >> 4;
    int startChunkY = wyOrigin >> 4;
    int endChunkY = (wyOrigin + ySize - 1) >> 4;
    int startChunkZ = wzOrigin >> 4;
    int endChunkZ = (wzOrigin + zSize - 1) >> 4;

    chunkCountX = endChunkX - startChunkX + 1;
    chunkCountY = endChunkY - startChunkY + 1;
    chunkCountZ = endChunkZ - startChunkZ + 1;
    xOffset = wxOrigin & 0x0f;
    yOffset = wyOrigin & 0x0f;
    zOffset = wzOrigin & 0x0f;
    sourceWXorigin = wxOrigin;
    sourceWYorigin = wyOrigin;
    sourceWZorigin = wzOrigin;
    unloadedChunks.set(0, chunkCountX * chunkCountZ);

    if (selectedVoxels.getxSize() > 0 && selectedVoxels.getySize() > 0 && selectedVoxels.getzSize() > 0) {
      displayListCount = chunkCountX * chunkCountY * chunkCountZ;
    }

    int displayListCubesBase = GLAllocation.generateDisplayLists(displayListCount);
    if (displayListCubesBase == 0 || displayListWireFrameXY == 0 || displayListWireFrameXZ == 0 || displayListWireFrameYZ == 0) {
      release();
      FMLLog.warning("Unable to create a displayList in BlockVoxelMultiSelectorRenderer::createRenderList");
      return;
    }
    displayListAllocations.add(new Pair<Integer, Integer>(displayListCubesBase, displayListCount));
    displayListMapping = new int [chunkCountX * chunkCountY * chunkCountZ];


    for (int cx = 0; cx < chunkCountX; ++cx) {
      for (int cy = 0; cy < chunkCountY; ++cy) {
        for (int cz = 0; cz < chunkCountZ; ++cz) {
          displayListMapping[cx + cy * chunkCountX + cz * chunkCountX * chunkCountY] = displayListCubesBase + cx + cy * chunkCountX + cz * chunkCountX * chunkCountY;
        }
      }
    }

    createMeshRenderLists(selectedVoxels.getxSize(), selectedVoxels.getySize(), selectedVoxels.getzSize());

    cxCurrent = 0;
    cyCurrent = 0;
    czCurrent = 0;

    mode = OperationInProgress.IN_PROGRESS;
  }

  /**
   * create a render list for the current selection.
   * Quads, with lines to outline them
   * @param world
   * @param unknownVoxels
   * @return the estimated fraction complete [0 .. 1]; or -1 if complete
   */
  public float createRenderListContinue(World world, int wxOrigin, int wyOrigin, int wzOrigin, VoxelSelection selectedVoxels, VoxelSelection unknownVoxels, long maxTimeInNS)
  {
    if (mode != OperationInProgress.IN_PROGRESS) {
      FMLLog.severe("Mode should be IN_PROGRESS in BlockVoxelMultiSelectorRenderer::createRenderListContinue, but was actually " + mode);
      return -1;
    }

    long startTime = System.nanoTime();
//    final double NUDGE_DISTANCE = 0.01;
//    final double FRAME_NUDGE_DISTANCE = NUDGE_DISTANCE + 0.002;

    for (; cxCurrent < chunkCountX; ++cxCurrent, cyCurrent = 0) {
      for (; cyCurrent < chunkCountY; ++cyCurrent, czCurrent = 0) {
        for (; czCurrent < chunkCountZ; ++czCurrent) {
          if (System.nanoTime() - startTime >= maxTimeInNS) return (cxCurrent / (float)chunkCountX);
          renderThisChunk(world, selectedVoxels, unknownVoxels, wxOrigin, wyOrigin, wzOrigin, cxCurrent, cyCurrent, czCurrent);
          Chunk chunk = world.getChunkFromBlockCoords(wxOrigin + cxCurrent * DISPLAY_LIST_XSIZE, wzOrigin + DISPLAY_LIST_ZSIZE);
          if (!chunk.isEmpty()) {
            unloadedChunks.clear(cxCurrent + czCurrent * chunkCountX);
          }
//
//          GL11.glNewList(getDisplayListIndex(cxCurrent, cyCurrent, czCurrent), GL11.GL_COMPILE);
//          GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
//          GL11.glDisable(GL11.GL_CULL_FACE);
//          GL11.glDisable(GL11.GL_LIGHTING);
//          GL11.glEnable(GL11.GL_TEXTURE_2D);
//          Tessellator tessellator = Tessellator.instance;
//          tessellateSurfaceWithTexture(world, selectedVoxels, unknownVoxels, wxOrigin, wyOrigin, wzOrigin,
//                                        cxCurrent * DISPLAY_LIST_XSIZE - xOffset, cyCurrent * DISPLAY_LIST_YSIZE - yOffset, czCurrent * DISPLAY_LIST_ZSIZE - zOffset,
//                                        tessellator, WhatToDraw.FACES, NUDGE_DISTANCE);
//
//          GL11.glEnable(GL11.GL_BLEND);
//          GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//          GL11.glColor4f(Colour.BLACK_40.R, Colour.BLACK_40.G, Colour.BLACK_40.B, Colour.BLACK_40.A);
//          GL11.glLineWidth(2.0F);
//          GL11.glDisable(GL11.GL_TEXTURE_2D);
//          GL11.glDepthMask(false);
//          tessellateSurface(selectedVoxels, unknownVoxels,
//                            cxCurrent * DISPLAY_LIST_XSIZE - xOffset, cyCurrent * DISPLAY_LIST_YSIZE - yOffset, czCurrent * DISPLAY_LIST_ZSIZE  - zOffset,
//                            tessellator, WhatToDraw.WIREFRAME, FRAME_NUDGE_DISTANCE);
//
//          GL11.glDepthMask(true);
//          GL11.glPopAttrib();
//          GL11.glEndList();
        }
      }
    }
    mode = OperationInProgress.COMPLETE;
    return -1;
  }

  int debugCount = 0;
  /**
   * render the current selection (must have called createRenderList previously).  Caller should set gLTranslatef so that the player's eyes are at [0,0,0]
   * playerRelativePos is position of the player relative to the minimum [x,y,z] corner of the VoxelSelection
   */
  public void renderSelection(Vec3 playerRelativePos, int blockRenderDistance, QuadOrientation quadOrientation, Colour colour)
  {
    if (displayListMapping == null) {
      return;
    }

    double relativeXpos = playerRelativePos.xCoord + xOffset;      // align to chunk boundary
    double relativeYpos = playerRelativePos.yCoord + yOffset;
    double relativeZpos = playerRelativePos.zCoord + zOffset;

    try {
      GL11.glPushMatrix();
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT);

      GL11.glTranslated(-relativeXpos, -relativeYpos, -relativeZpos);

      // transform the player world [x,z] into the coordinates of the selection
      int playerRelativePositionX = quadOrientation.calcXfromWXZ((int)(relativeXpos), (int)(relativeZpos));
      int playerRelativePositionZ = quadOrientation.calcZfromWXZ((int)(relativeXpos), (int)(relativeZpos));

      final int CX_MIN = Math.max(0, (int)((playerRelativePositionX - blockRenderDistance)/ DISPLAY_LIST_XSIZE));
      final int CY_MIN = Math.max(0, (int)((relativeYpos - blockRenderDistance)/ DISPLAY_LIST_YSIZE));
      final int CZ_MIN = Math.max(0, (int)((playerRelativePositionZ - blockRenderDistance)/ DISPLAY_LIST_ZSIZE));
      final int CX_MAX = Math.min(chunkCountX - 1, (int)((playerRelativePositionX + blockRenderDistance)/ DISPLAY_LIST_XSIZE));
      final int CY_MAX = Math.min(chunkCountY - 1, (int)((relativeYpos + blockRenderDistance)/ DISPLAY_LIST_YSIZE));
      final int CZ_MAX = Math.min(chunkCountZ - 1, (int)((playerRelativePositionZ + blockRenderDistance)/ DISPLAY_LIST_ZSIZE));

      Pair<Float, Float> renderNudge = quadOrientation.getWXZNudge();
      GL11.glTranslatef(renderNudge.getFirst(), 0.0F, renderNudge.getSecond());

      if (quadOrientation.getClockwiseRotationCount() > 0) { // rotate around the midpoint
        GL11.glTranslatef(xSize / 2.0F + xOffset, 0, zSize / 2.0F + zOffset);
        GL11.glRotatef(quadOrientation.getClockwiseRotationCount() * -90, 0, 1, 0);
        GL11.glTranslatef(-xSize / 2.0F - xOffset, 0, -zSize / 2.0F - zOffset);
      }
      if (quadOrientation.isFlippedX()) {  // flip around the midpoint
        GL11.glTranslatef(xSize / 2.0F + xOffset, 0, 0);
        GL11.glScaled(-1, 1, 1);
        GL11.glTranslatef(-xSize / 2.0F - xOffset, 0, 0);
      }

      try {
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        for (int cy = CY_MIN; cy <= CY_MAX; ++cy) {
          for (int cx = CX_MIN; cx <= CX_MAX; ++cx) {
            for (int cz = CZ_MIN; cz <= CZ_MAX; ++cz) {
              GL11.glTranslated(cx*16, cy*16, cz*16);
              GL11.glColor4f(colour.R, colour.G, colour.B, colour.A);
              GL11.glCallList(getDisplayListIndex(cx, cy, cz));
              GL11.glTranslated(-cx*16, -cy*16, -cz*16);
            }
          }
        }
      } finally {
        GL11.glPopAttrib();
        GL11.glPopMatrix();
      }

      GL11.glTranslated(xOffset, yOffset, zOffset);       // the grid doesn't need to be offset for the chunk alignment

      // cull the back faces of the grid:
      // only draw a face if you can see the front of it
      //   eg for xpos face - if player is to the right, for xneg - if player is to the left.  if between don't draw either one.
      // exception: if inside the cube, render all faces.  Yneg is always drawn.

      double playerRelativePositionVx = quadOrientation.calcXfromWXZ(playerRelativePos.xCoord, playerRelativePos.zCoord);
      double playerRelativePositionVz = quadOrientation.calcZfromWXZ(playerRelativePos.xCoord, playerRelativePos.zCoord);
      boolean inside =     (playerRelativePositionVx >= 0 && playerRelativePositionVx <= xSize)
                && (                                         playerRelativePos.yCoord <= ySize)
                        && (playerRelativePositionVz >= 0 && playerRelativePositionVz <= zSize);


      final Colour WALL_GRID_COLOUR = Colour.BLACK_40;
      final Colour FLOOR_GRID_COLOUR = Colour.WHITE_40;

      GL11.glColor4f(WALL_GRID_COLOUR.R, WALL_GRID_COLOUR.G, WALL_GRID_COLOUR.B, WALL_GRID_COLOUR.A);

      if (inside || playerRelativePositionVx < 0) {
        GL11.glPushMatrix();
        GL11.glTranslated(0, ySize, 0);
        GL11.glCallList(displayListWireFrameYZ);
        GL11.glPopMatrix();
      }

      if (inside || playerRelativePositionVx > xSize) {
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

      if (inside || playerRelativePositionVz < 0) {
        GL11.glPushMatrix();
        GL11.glTranslated(0, ySize, 0);
        GL11.glCallList(displayListWireFrameXY);
        GL11.glPopMatrix();
      }

      if (inside || playerRelativePositionVz > zSize) {
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

  enum WhatToDraw {FACES, WIREFRAME}

  private void tessellateSurface(VoxelSelection selection, VoxelSelection unknownVoxels, int sx0, int sy0, int sz0,
                                 Tessellator tessellator, WhatToDraw whatToDraw, double nudgeDistance)
  {
    int xNegNudge, xPosNudge, yNegNudge, yPosNudge, zNegNudge, zPosNudge;

    if (whatToDraw == WhatToDraw.FACES) {
      tessellator.startDrawingQuads();
    }

    // goes outside the VoxelSelection size, which always returns zero when out of bounds
    // "nudges" the quad boundaries to make solid blocks slightly larger, avoids annoying visual artifacts when overlapping with world blocks
    // three cases of nudge for each edge: internal (concave) edges = nudge inwards (-1), flat = no nudge (0), outer (convex) = nudge outwards (+1)

    for (int y = 0; y < DISPLAY_LIST_YSIZE; ++y) {
      for (int z = 0; z < DISPLAY_LIST_ZSIZE; ++z) {
        for (int x = 0; x < DISPLAY_LIST_XSIZE; ++x) {
          int sx = x + sx0;
          int sy = y + sy0;
          int sz = z + sz0;
          if (selection.getVoxel(sx, sy, sz) || unknownVoxels.getVoxel(sx, sy, sz)) {
            // xneg face
            if (!selection.getVoxel(sx - 1, sy, sz) && !unknownVoxels.getVoxel(sx - 1, sy, sz)) {
              yNegNudge = (selection.getVoxel(sx-1, sy-1, sz+0) ? -1 : (selection.getVoxel(sx+0, sy-1, sz+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(sx-1, sy+1, sz+0) ? -1 : (selection.getVoxel(sx+0, sy+1, sz+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(sx-1, sy+0, sz-1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(sx-1, sy+0, sz+1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz+1) ? 0 : 1) );

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertex(x - nudgeDistance, y     - yNegNudge * nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x - nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x - nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              tessellator.addVertex(x - nudgeDistance, y     - yNegNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // xpos face
            if (!selection.getVoxel(sx + 1, sy, sz) && !unknownVoxels.getVoxel(sx + 1, sy, sz)) {
              yNegNudge = (selection.getVoxel(sx+1, sy-1, sz+0) ? -1 : (selection.getVoxel(sx+0, sy-1, sz+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(sx+1, sy+1, sz+0) ? -1 : (selection.getVoxel(sx+0, sy+1, sz+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(sx+1, sy+0, sz-1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(sx+1, sy+0, sz+1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz+1) ? 0 : 1) );

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertex(x + 1 + nudgeDistance, y     - yNegNudge * nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x + 1 + nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x + 1 + nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              tessellator.addVertex(x + 1 + nudgeDistance, y     - yNegNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // yneg face
            if (!selection.getVoxel(sx, sy-1, sz) && !unknownVoxels.getVoxel(sx, sy-1, sz)) {
              xNegNudge = (selection.getVoxel(sx-1, sy-1, sz+0) ? -1 : (selection.getVoxel(sx-1, sy+0, sz+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(sx+1, sy-1, sz+0) ? -1 : (selection.getVoxel(sx+1, sy+0, sz+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(sx+0, sy-1, sz-1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(sx+0, sy-1, sz+1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz+1) ? 0 : 1) );

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertex(x     - xNegNudge * nudgeDistance, y - nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x + 1 + xPosNudge * nudgeDistance, y - nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x + 1 + xPosNudge * nudgeDistance, y - nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              tessellator.addVertex(x     - xNegNudge * nudgeDistance, y - nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // ypos face
            if (!selection.getVoxel(sx, sy+1, sz) && !unknownVoxels.getVoxel(sx, sy+1, sz)) {
              xNegNudge = (selection.getVoxel(sx-1, sy+1, sz+0) ? -1 : (selection.getVoxel(sx-1, sy+0, sz+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(sx+1, sy+1, sz+0) ? -1 : (selection.getVoxel(sx+1, sy+0, sz+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(sx+0, sy+1, sz-1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(sx+0, sy+1, sz+1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz+1) ? 0 : 1) );

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertex(x     - xNegNudge * nudgeDistance, y + 1 + nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x + 1 + xPosNudge * nudgeDistance, y + 1 + nudgeDistance, z     - zNegNudge * nudgeDistance);
              tessellator.addVertex(x + 1 + xPosNudge * nudgeDistance, y + 1 + nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              tessellator.addVertex(x     - xNegNudge * nudgeDistance, y + 1 + nudgeDistance, z + 1 + zPosNudge * nudgeDistance);
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // zneg face
            if (!selection.getVoxel(sx, sy, sz-1) && !unknownVoxels.getVoxel(sx, sy, sz-1)) {
              xNegNudge = (selection.getVoxel(sx-1, sy+0, sz-1) ? -1 : (selection.getVoxel(sx-1, sy+0, sz+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(sx+1, sy+0, sz-1) ? -1 : (selection.getVoxel(sx+1, sy+0, sz+0) ? 0 : 1) );
              yNegNudge = (selection.getVoxel(sx+0, sy-1, sz-1) ? -1 : (selection.getVoxel(sx+0, sy-1, sz+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(sx+0, sy+1, sz-1) ? -1 : (selection.getVoxel(sx+0, sy+1, sz+0) ? 0 : 1) );

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertex(x     - xNegNudge * nudgeDistance, y     - yNegNudge * nudgeDistance, z - nudgeDistance);
              tessellator.addVertex(x + 1 + xPosNudge * nudgeDistance, y     - yNegNudge * nudgeDistance, z - nudgeDistance);
              tessellator.addVertex(x + 1 + xPosNudge * nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z - nudgeDistance);
              tessellator.addVertex(x     - xNegNudge * nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z - nudgeDistance);
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // zpos face
            if (!selection.getVoxel(sx, sy, sz+1) && !unknownVoxels.getVoxel(sx, sy, sz+1)) {
              xNegNudge = (selection.getVoxel(sx-1, sy+0, sz+1) ? -1 : (selection.getVoxel(sx-1, sy+0, sz+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(sx+1, sy+0, sz+1) ? -1 : (selection.getVoxel(sx+1, sy+0, sz+0) ? 0 : 1) );
              yNegNudge = (selection.getVoxel(sx+0, sy-1, sz+1) ? -1 : (selection.getVoxel(sx+0, sy-1, sz+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(sx+0, sy+1, sz+1) ? -1 : (selection.getVoxel(sx+0, sy+1, sz+0) ? 0 : 1) );

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
   * @param unknownVoxels
   * @param wxOrigin world x of the selection
   * @param wyOrigin world y of the selection
   * @param wzOrigin world z of the selection
   * @param sx0 start x coordinate in the selection
   * @param sy0 start y coordinate in the selection
   * @param sz0 start z coordinated in the selection
   * @param tessellator
   * @param whatToDraw
   * @param nudgeDistance how far to nudge the face (to prevent overlap)
   */
  private void tessellateSurfaceWithTexture(World world, VoxelSelection selection, VoxelSelection unknownVoxels, int wxOrigin, int wyOrigin, int wzOrigin,
                                               int sx0, int sy0, int sz0,
                                               Tessellator tessellator, WhatToDraw whatToDraw, double nudgeDistance)
  {
    int xNegNudge, xPosNudge, yNegNudge, yPosNudge, zNegNudge, zPosNudge;

    if (whatToDraw == WhatToDraw.FACES) {
      tessellator.startDrawingQuads();
    }

    // goes outside the VoxelSelection size, which always returns zero when out of bounds
    // "nudges" the quad boundaries to make solid blocks slightly larger, avoids annoying visual artifacts when overlapping with world blocks
    // three cases of nudge for each edge: internal (concave) edges = nudge inwards (-1), flat = no nudge (0), outer (convex) = nudge outwards (+1)

    for (int y = 0; y < DISPLAY_LIST_YSIZE; ++y) {
      for (int z = 0; z <  DISPLAY_LIST_ZSIZE; ++z) {
        for (int x = 0; x < DISPLAY_LIST_XSIZE; ++x) {
          int sx = x + sx0;
          int sy = y + sy0;
          int sz = z + sz0;
          boolean selected = selection.getVoxel(sx, sy, sz);
          if (selected || unknownVoxels.getVoxel(sx, sy, sz)) {
            int wx = sx + wxOrigin;
            int wy = sy + wyOrigin;
            int wz = sz + wzOrigin;

            // unknown blocks get SelectionFog
            // selected blocks which are air (either because the block is air, or because the chunk is not loaded), get SelectionSolidFog
            Block block = RegistryForBlocks.blockSelectionFog;
            if (selected) {
              block = world.getBlock(wx, wy, wz);
              if (block == Blocks.air) {
                block = RegistryForBlocks.blockSelectionSolidFog;
              }
            }

            // xneg face
            if (!selection.getVoxel(sx - 1, sy, sz) && !unknownVoxels.getVoxel(sx - 1, sy, sz)) {
              yNegNudge = (selection.getVoxel(sx-1, sy-1, sz+0) ? -1 : (selection.getVoxel(sx+0, sy-1, sz+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(sx-1, sy+1, sz+0) ? -1 : (selection.getVoxel(sx+0, sy+1, sz+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(sx-1, sy+0, sz-1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(sx-1, sy+0, sz+1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz+1) ? 0 : 1) );
              IIcon icon = block.getBlockTextureFromSide(UsefulConstants.FACE_XNEG);

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertexWithUV(x - nudgeDistance, y - yNegNudge * nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMinU(), icon.getMaxV());
              tessellator.addVertexWithUV(x - nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMinU(), icon.getMinV());
              tessellator.addVertexWithUV(x - nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMaxU(), icon.getMinV());
              tessellator.addVertexWithUV(x - nudgeDistance, y - yNegNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMaxU(), icon.getMaxV());
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // xpos face
            if (!selection.getVoxel(sx + 1, sy, sz) && !unknownVoxels.getVoxel(sx + 1, sy, sz)) {
              yNegNudge = (selection.getVoxel(sx+1, sy-1, sz+0) ? -1 : (selection.getVoxel(sx+0, sy-1, sz+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(sx+1, sy+1, sz+0) ? -1 : (selection.getVoxel(sx+0, sy+1, sz+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(sx+1, sy+0, sz-1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(sx+1, sy+0, sz+1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz+1) ? 0 : 1) );
              IIcon icon = block.getBlockTextureFromSide(UsefulConstants.FACE_XPOS);

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertexWithUV(x + 1 + nudgeDistance, y - yNegNudge * nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMinU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMinU(), icon.getMinV());
              tessellator.addVertexWithUV(x + 1 + nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMaxU(), icon.getMinV());
              tessellator.addVertexWithUV(x + 1 + nudgeDistance, y - yNegNudge * nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMaxU(), icon.getMaxV());
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // yneg face
            if (!selection.getVoxel(sx, sy-1, sz) && !unknownVoxels.getVoxel(sx, sy-1, sz)) {
              xNegNudge = (selection.getVoxel(sx-1, sy-1, sz+0) ? -1 : (selection.getVoxel(sx-1, sy+0, sz+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(sx+1, sy-1, sz+0) ? -1 : (selection.getVoxel(sx+1, sy+0, sz+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(sx+0, sy-1, sz-1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(sx+0, sy-1, sz+1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz+1) ? 0 : 1) );
              IIcon icon = block.getBlockTextureFromSide(UsefulConstants.FACE_YNEG);
              // NB yneg face is flipped left-right in vanilla
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertexWithUV(x - xNegNudge * nudgeDistance, y - nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMaxU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + xPosNudge * nudgeDistance, y - nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMinU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + xPosNudge * nudgeDistance, y - nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMinU(), icon.getMinV());
              tessellator.addVertexWithUV(x - xNegNudge * nudgeDistance, y - nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMaxU(), icon.getMinV());
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // ypos face
            if (!selection.getVoxel(sx, sy+1, sz) && !unknownVoxels.getVoxel(sx, sy+1, sz)) {
              xNegNudge = (selection.getVoxel(sx-1, sy+1, sz+0) ? -1 : (selection.getVoxel(sx-1, sy+0, sz+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(sx+1, sy+1, sz+0) ? -1 : (selection.getVoxel(sx+1, sy+0, sz+0) ? 0 : 1) );
              zNegNudge = (selection.getVoxel(sx+0, sy+1, sz-1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz-1) ? 0 : 1) );
              zPosNudge = (selection.getVoxel(sx+0, sy+1, sz+1) ? -1 : (selection.getVoxel(sx+0, sy+0, sz+1) ? 0 : 1) );
              IIcon icon = block.getBlockTextureFromSide(UsefulConstants.FACE_YPOS);

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertexWithUV(x - xNegNudge * nudgeDistance, y + 1 + nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMinU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + xPosNudge * nudgeDistance, y + 1 + nudgeDistance, z - zNegNudge * nudgeDistance, icon.getMaxU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + xPosNudge * nudgeDistance, y + 1 + nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMaxU(), icon.getMinV());
              tessellator.addVertexWithUV(x - xNegNudge * nudgeDistance, y + 1 + nudgeDistance, z + 1 + zPosNudge * nudgeDistance, icon.getMinU(), icon.getMinV());
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // zneg face
            if (!selection.getVoxel(sx, sy, sz-1) && !unknownVoxels.getVoxel(sx, sy, sz-1)) {
              xNegNudge = (selection.getVoxel(sx-1, sy+0, sz-1) ? -1 : (selection.getVoxel(sx-1, sy+0, sz+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(sx+1, sy+0, sz-1) ? -1 : (selection.getVoxel(sx+1, sy+0, sz+0) ? 0 : 1) );
              yNegNudge = (selection.getVoxel(sx+0, sy-1, sz-1) ? -1 : (selection.getVoxel(sx+0, sy-1, sz+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(sx+0, sy+1, sz-1) ? -1 : (selection.getVoxel(sx+0, sy+1, sz+0) ? 0 : 1) );
              IIcon icon = block.getBlockTextureFromSide(UsefulConstants.FACE_ZNEG);

              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.startDrawing(GL11.GL_LINE_LOOP);
              tessellator.addVertexWithUV(x - xNegNudge * nudgeDistance, y - yNegNudge * nudgeDistance, z - nudgeDistance, icon.getMaxU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + xPosNudge * nudgeDistance, y - yNegNudge * nudgeDistance, z - nudgeDistance, icon.getMinU(), icon.getMaxV());
              tessellator.addVertexWithUV(x + 1 + xPosNudge * nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z - nudgeDistance, icon.getMinU(), icon.getMinV());
              tessellator.addVertexWithUV(x - xNegNudge * nudgeDistance, y + 1 + yPosNudge * nudgeDistance, z - nudgeDistance, icon.getMaxU(), icon.getMinV());
              if (whatToDraw == WhatToDraw.WIREFRAME) tessellator.draw();
            }
            // zpos face
            if (!selection.getVoxel(sx, sy, sz+1) && !unknownVoxels.getVoxel(sx, sy, sz+1)) {
              xNegNudge = (selection.getVoxel(sx-1, sy+0, sz+1) ? -1 : (selection.getVoxel(sx-1, sy+0, sz+0) ? 0 : 1) );
              xPosNudge = (selection.getVoxel(sx+1, sy+0, sz+1) ? -1 : (selection.getVoxel(sx+1, sy+0, sz+0) ? 0 : 1) );
              yNegNudge = (selection.getVoxel(sx+0, sy-1, sz+1) ? -1 : (selection.getVoxel(sx+0, sy-1, sz+0) ? 0 : 1) );
              yPosNudge = (selection.getVoxel(sx+0, sy+1, sz+1) ? -1 : (selection.getVoxel(sx+0, sy+1, sz+0) ? 0 : 1) );
              IIcon icon = block.getBlockTextureFromSide(UsefulConstants.FACE_ZNEG);

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
   * @param meshXSize
   * @param ySize
   * @param meshZSize
   */
  private void createMeshRenderLists(int meshXSize, int ySize, int meshZSize)
  {
    final int MESH_HEIGHT = 256;
    generateWireFrame2DMesh(displayListWireFrameXY, meshXSize, MESH_HEIGHT,     0);
    generateWireFrame2DMesh(displayListWireFrameXZ, meshXSize,           0, meshZSize);
    generateWireFrame2DMesh(displayListWireFrameYZ,     0, MESH_HEIGHT, meshZSize);
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
