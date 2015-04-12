package speedytools.clientside.selections;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import speedytools.common.utilities.ErrorLog;

import java.awt.*;
import java.nio.IntBuffer;
import java.util.*;
import java.util.List;

/**
 * Created by TheGreyGhost on 14/03/2015.
 *
 * Used to track the textures used for each cube in a selection.
  * A limited number of block textures will be stored.  Once full a generic block texture will be substituted instead
 *
 * Usage:
 * (1) Create the SelectionBlockTextures to allocate the space and create a texture sheet
 * (2) addBlock() for each Block you want included in the texture sheet - doesn't actually capture the block's
 *     texture yet.
 * (2b) alternatively, if the autoAllocate() is set to true, calling getSBTIcon() on an unused block will automatically
 *      allocate a space for that block
 * (3) updateTextures() to capture the textures of all blocks you added using addBlock()
 * When rendering:
 * (4) bindTexture() to bind to the SelectionBlockTextures for subsequent rendering
 * (5) getSBTIcon() to get the Icon (texture coordinates) for the given block
 * Other tools:
 * (6) clear() to empty the list of blocks on the sheet
 * (7) release() to release the OpenGL texture sheet
 *
 * If you are creating and releasing a lot of SelectionBlockTextures you should release() to avoid using up
 *   the OpenGL memory.
 *
 */
public class SelectionBlockTextures {

  public SelectionBlockTextures(TextureManager i_textureManager) {
    textureManager = i_textureManager;
    int textureHeightTexels = BLOCK_COUNT_DESIRED * V_TEXELS_PER_FACE;
    int maximumTextureHeight = Minecraft.getGLMaximumTextureSize();
    if (textureHeightTexels <= maximumTextureHeight) {
      BLOCK_COUNT = BLOCK_COUNT_DESIRED;
    } else {
      BLOCK_COUNT = maximumTextureHeight / V_TEXELS_PER_FACE;
      textureHeightTexels = maximumTextureHeight;
    }
    LAST_BLOCK_INDEX_PLUS_ONE = FIRST_BLOCK_INDEX + BLOCK_COUNT - 1;

    int textureWidthTexels = NUMBER_OF_FACES_PER_BLOCK * U_TEXELS_PER_FACE;

    TEXEL_HEIGHT_PER_BLOCK = 1.0 / BLOCK_COUNT;
    TEXEL_WIDTH_PER_FACE = 1.0 / NUMBER_OF_FACES_PER_BLOCK;

    nextFreeTextureIndex = FIRST_BLOCK_INDEX;
    firstUncachedIndex = FIRST_BLOCK_INDEX;
    blockTextures = new DynamicTexture(textureWidthTexels, textureHeightTexels);
    textureAllocated = true;
    textureResourceLocation = textureManager.getDynamicTextureLocation("SelectionBlockTextures", blockTextures);

    eraseBlockTextures(NULL_BLOCK_INDEX, NULL_BLOCK_INDEX);
    eraseBlockTextures(FIRST_BLOCK_INDEX, LAST_BLOCK_INDEX_PLUS_ONE - 1);
    blockTextures.updateDynamicTexture();

    ++sbtCount;
    if (sbtCount > SBT_COUNT_WARNING) {
      System.err.println("Warning: allocated " + sbtCount + " textures without release()");
    }
  }

  /**
   * If true, a call to getSBTIcon for a block with no allocated Icon will automatically allocate an Icon
   * The default is false.
   * @param autoAllocateIcon
   */
  public void setAutoAllocateIcon(boolean autoAllocateIcon) {
    this.autoAllocateIcon = autoAllocateIcon;
  }

  /** bind the texture sheet of the SelectionBlockTextures, ready for rendering the SBTicons
   */
  public void bindTexture() {
    textureManager.bindTexture(textureResourceLocation);
  }

  /** remove all blocks from the texture sheet
   */
  public void clear()
  {
    blockTextureNumbers.clear();
    nextFreeTextureIndex = FIRST_BLOCK_INDEX;
    firstUncachedIndex = FIRST_BLOCK_INDEX;
  }

  /**
   * release the allocated OpenGL texture
   * do not use this object again after release
   */
  public void release()
  {
    blockTextures.deleteGlTexture();
    if (textureAllocated) {
      --sbtCount;
      assert sbtCount >= 0;
      textureAllocated = false;
    }
  }

  /**
   * Include this block in the texture sheet
   * (Doesn't actually stitch the block's texture into the sheet until you call updateTextures() )
   * @param iBlockState the block to add.  Ignores properties (uses block.getDefaultState())
   */
  public void addBlock(IBlockState iBlockState)
  {
    if (iBlockState == null) {
      return;
    }
    if (blockTextureNumbers.containsKey(iBlockState.getBlock())) {
      return;
    }
    if (nextFreeTextureIndex >= LAST_BLOCK_INDEX_PLUS_ONE) {
      return;
    }
    blockTextureNumbers.put(iBlockState.getBlock(), nextFreeTextureIndex);
    ++nextFreeTextureIndex;
  }

  /**
   * Will stitch any newly-added blocks into the texture sheet
   */
  public void updateTextures()
  {
    if (firstUncachedIndex >= nextFreeTextureIndex) return;

    if (!OpenGlHelper.isFramebufferEnabled()) {  // frame buffer not available, just use blank texture
      eraseBlockTextures(firstUncachedIndex, nextFreeTextureIndex - 1);
      firstUncachedIndex = nextFreeTextureIndex;
      return;
    }

    Framebuffer frameBuffer = null;
    try {
      GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glPushMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glPushMatrix();

      // setup modelview matrix
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glLoadIdentity();
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity();

      GL11.glOrtho(0.0D, 1.0, 1.0, 0.0, -10.0, 10.0);  // set up to render over [0,0,0] to [1,1,1]
      GL11.glEnable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_BLEND);
      final float ALPHA_TEST_THRESHOLD = 0.1F;
      GL11.glAlphaFunc(GL11.GL_GREATER, ALPHA_TEST_THRESHOLD);

      final boolean USE_DEPTH = true;
      frameBuffer = new Framebuffer(U_TEXELS_PER_FACE, V_TEXELS_PER_FACE, USE_DEPTH);

      Minecraft mc = Minecraft.getMinecraft();
      BlockRendererDispatcher blockRendererDispatcher = mc.getBlockRendererDispatcher();
      BlockModelShapes blockModelShapes = blockRendererDispatcher.getBlockModelShapes();

      for (Map.Entry<Block, Integer> entry : blockTextureNumbers.entrySet()) {
        int textureIndex = entry.getValue();
        if (textureIndex >= firstUncachedIndex) {
          Block blockToRender = entry.getKey();
          IBlockState defaultState = blockToRender.getDefaultState();

          IBakedModel ibakedmodel = blockModelShapes.getModelForState(defaultState);
          if (ibakedmodel instanceof net.minecraftforge.client.model.ISmartBlockModel) {
            ibakedmodel = ((net.minecraftforge.client.model.ISmartBlockModel)ibakedmodel).handleBlockState(defaultState);
          }

          final int BAKED_MODEL_RENDER_TYPE = 3;
          if (blockToRender.getRenderType() != BAKED_MODEL_RENDER_TYPE) {
            eraseBlockTextures(textureIndex, textureIndex);
          } else {
            stitchModelIntoTextureSheet(frameBuffer, textureIndex, blockToRender, ibakedmodel);
          }
        }
      }
    } catch (Exception e) {
      ErrorLog.defaultLog().info(e.toString());
    } finally {
      if (frameBuffer != null) {
        frameBuffer.deleteFramebuffer();
      }
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glPopMatrix();
      GL11.glPopAttrib();
    }

    firstUncachedIndex = nextFreeTextureIndex;
    blockTextures.updateDynamicTexture();
  }

  public void stitchModelIntoTextureSheet(Framebuffer frameBuffer, int textureIndex, Block blockToRender, IBakedModel iBakedModel) {
    // from RenderFallingBlock.doRender()

    switch (blockToRender.getBlockLayer()) {
      case SOLID: {
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        break;
      }
      case CUTOUT_MIPPED:
      case CUTOUT:
      case TRANSLUCENT: {
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        break;
      }
      default: {
        System.err.println("Unknown getBlockLayer():" + blockToRender.getBlockLayer());
        return;
      }
    }

    List<BakedQuad> generalQuads = iBakedModel.getGeneralQuads();
    for (EnumFacing facing : EnumFacing.values()) {
      List<BakedQuad> faceQuads = iBakedModel.getFaceQuads(facing);
      final float BASE_COLOUR = 0.0F;
      frameBuffer.setFramebufferColor(BASE_COLOUR, BASE_COLOUR, BASE_COLOUR, BASE_COLOUR);
      frameBuffer.framebufferClear();
      final boolean SET_VIEWPORT_TRUE = true;
      frameBuffer.bindFramebuffer(SET_VIEWPORT_TRUE);

      try {
        // various transforms are required to make the world faces render appropriately.
        // in some cases the face must be mirror imaged and the front face swapped with the back face (CW instead of CCW)
        GL11.glPushMatrix();
        GL11.glTranslatef(+0.5F, +0.5F, +0.5F);
        GL11.glFrontFace(GL11.GL_CCW);
        switch (facing) {
          case NORTH:
            GL11.glScalef(-1.0F, 1.0F, 1.0F);
            GL11.glRotatef(0.0F, 0.0F, 1.0F, 0.0F);
            GL11.glFrontFace(GL11.GL_CW);
            break;
          case SOUTH:
            GL11.glScalef(-1.0F, 1.0F, 1.0F);
            GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            GL11.glFrontFace(GL11.GL_CW);
            break;
          case EAST:
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            break;
          case WEST:
            GL11.glScalef(-1.0F, 1.0F, 1.0F);
            GL11.glRotatef(270.0F, 0.0F, 1.0F, 0.0F);
            GL11.glFrontFace(GL11.GL_CW);
            break;
          case UP:
            GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            break;
          case DOWN:
            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            break;
        }
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.startDrawingQuads();
        worldrenderer.setVertexFormat(DefaultVertexFormats.BLOCK);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        if (!faceQuads.isEmpty()) {
          renderModelStandardQuads(worldrenderer, faceQuads);
        }
        if (!generalQuads.isEmpty()) {
          renderModelStandardQuads(worldrenderer, generalQuads);
        }
        tessellator.draw();
        stitchGreyFrameBufferIntoTextureSheet(frameBuffer, textureIndex, facing);
      } finally {
        GL11.glPopMatrix();
      }

    }
  }

  /**
   * stitch the given framebuffer into the texture sheet at the appropriate location.
   * converts the framebuffer from colour to greyscale
   * @param frameBuffer
   * @param textureIndex
   * @param whichFace
   */
  private void stitchGreyFrameBufferIntoTextureSheet(Framebuffer frameBuffer, int textureIndex, EnumFacing whichFace)
  {
    frameBuffer.bindFramebufferTexture();
    IntBuffer pixelBuffer = BufferUtils.createIntBuffer(U_TEXELS_PER_FACE * V_TEXELS_PER_FACE);
    GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);

    int[] frameData = new int[pixelBuffer.remaining()];
    pixelBuffer.get(frameData);

    int[] textureSheet = blockTextures.getTextureData();
    int textureWidthTexels = NUMBER_OF_FACES_PER_BLOCK * U_TEXELS_PER_FACE;
    int textureSheetBase = textureIndex * V_TEXELS_PER_FACE * textureWidthTexels
                          + whichFace.getIndex() * U_TEXELS_PER_FACE;

    for (int v = 0; v < V_TEXELS_PER_FACE; ++v) {
      for (int u = 0; u < U_TEXELS_PER_FACE; ++u) {
        int sourceColour = frameData[u + v * U_TEXELS_PER_FACE];
        int red = sourceColour & 0xff;
        int green = (sourceColour & 0xff00) >> 8;
        int blue = (sourceColour & 0xff0000) >> 16;
        int average = (red + blue + green) / 3;
        int grey = average | (average << 8) | (average << 16) | 0xff000000;
        textureSheet[textureSheetBase + u + v * textureWidthTexels] = grey;
      }
    }
  }

  /**
   * render the given quad list at full brightness
   * @param worldRendererIn
   * @param bakedQuadList
   */
  private void renderModelStandardQuads(WorldRenderer worldRendererIn, List<BakedQuad> bakedQuadList)
  {
    final int VERTEX_BRIGHTNESS = -1;
    for (BakedQuad bakedQuad : bakedQuadList ) {
      worldRendererIn.addVertexData(bakedQuad.getVertexData());
      worldRendererIn.putBrightness4(VERTEX_BRIGHTNESS, VERTEX_BRIGHTNESS, VERTEX_BRIGHTNESS, VERTEX_BRIGHTNESS);
    }
  }

  /**
   * get the icon (position in the texture sheet) of the given block
   * If autoAllocatedIcon() has been set, a call to getSBTIcon for a block with no allocated Icon will
   *   automatically allocate an Icon, otherwise a generic null icon is returned instead.
   * The default is false, i.e. do not automatically allocate.
   * @param iBlockState block to be retrieved (properties ignored)
   * @param whichFace which faces' texture?
   * @return the blocks' icon, or a generic "null" icon if not available
   */
  public SBTIcon getSBTIcon(IBlockState iBlockState, EnumFacing whichFace)
  {
    Integer textureIndex;
    if (iBlockState == null) {
      textureIndex = NULL_BLOCK_INDEX;
    } else {
      if (!blockTextureNumbers.containsKey(iBlockState.getBlock())
            && autoAllocateIcon) {
        addBlock(iBlockState);
      }
      if (!blockTextureNumbers.containsKey(iBlockState.getBlock())) {
        textureIndex = NULL_BLOCK_INDEX;
      } else {
        textureIndex = blockTextureNumbers.get(iBlockState.getBlock());
        if (textureIndex == null) {
          textureIndex = NULL_BLOCK_INDEX;
        }
      }
    }

    double umin = TEXEL_WIDTH_PER_FACE * whichFace.getIndex();
    double vmin = TEXEL_HEIGHT_PER_BLOCK * textureIndex;
    return new SBTIcon(umin, umin + TEXEL_WIDTH_PER_FACE, vmin, vmin + TEXEL_HEIGHT_PER_BLOCK);
  }

  /**
   * Overwrite the given texture indices with blank (untextured white)
   * Must call updateDynamicTexture() afterwards to upload
   * @param firstTextureIndex first texture index to blank out
   * @param lastTextureIndex last texture index to blank out (inclusive)
   */
  private void eraseBlockTextures(int firstTextureIndex, int lastTextureIndex)
  {
    int[] rawTexture = blockTextures.getTextureData();
    int textureWidthTexels = NUMBER_OF_FACES_PER_BLOCK * U_TEXELS_PER_FACE;

    for (int i = firstTextureIndex; i <= lastTextureIndex; ++i) {
      int startRawDataIndex = textureWidthTexels * V_TEXELS_PER_FACE * firstTextureIndex;
      int endRawDataIndexPlus1 = textureWidthTexels * V_TEXELS_PER_FACE * (lastTextureIndex + 1);

      Arrays.fill(rawTexture, startRawDataIndex,  endRawDataIndexPlus1, Color.WHITE.getRGB() );
    }
  }

  // The face textures are stored as:
  //  one row per block, one column per face
  //  i.e. the texture sheet is six faces wide and BLOCK_COUNT faces high.

  private final DynamicTexture blockTextures;
  private final ResourceLocation textureResourceLocation;
  private final TextureManager textureManager;
  private final double TEXEL_WIDTH_PER_FACE;
  private final double TEXEL_HEIGHT_PER_BLOCK;
  private int nextFreeTextureIndex;
  private int firstUncachedIndex;

  private final int NUMBER_OF_FACES_PER_BLOCK = 6;
  private final int U_TEXELS_PER_FACE = 16;
  private final int V_TEXELS_PER_FACE = 16;

  private final int NULL_BLOCK_INDEX = 0;
  private final int FIRST_BLOCK_INDEX = NULL_BLOCK_INDEX + 1;

  private final int BLOCK_COUNT_DESIRED = 256;
  private final int BLOCK_COUNT;
  private final int LAST_BLOCK_INDEX_PLUS_ONE;

  private HashMap<Block, Integer> blockTextureNumbers = new HashMap<Block, Integer>(BLOCK_COUNT_DESIRED);

  private boolean autoAllocateIcon = false;

  public class SBTIcon
  {
    public SBTIcon(double i_umin, double i_umax, double i_vmin, double i_vmax)
    {
      umin = i_umin;
      umax = i_umax;
      vmin = i_vmin;
      vmax = i_vmax;
    }

    public double getMinU() {
      return umin;
    }

    public double getMaxU() {
      return umax;
    }

    public double getMinV() {
      return vmin;
    }

    public double getMaxV() {
      return vmax;
    }

    private double umin;
    private double umax;
    private double vmin;
    private double vmax;
  }

  // debug- to help detect resource leaks
  private static final int SBT_COUNT_WARNING = 5;  // if we have more than 5 opened-but-not-released objects, give a warning
  private static int sbtCount = 0;
  private boolean textureAllocated = false;
}
