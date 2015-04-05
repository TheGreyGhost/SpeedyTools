package speedytools.clientside.selections;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by TheGreyGhost on 14/03/2015.
 *
 * Used to track the textures used for each cube in a selection.
  * A limited number of block textures will be stored, once full a generic block texture will be returned instead
 *
 * Usage:
 * (1) Create the SelectionBlockTextures to allocate the space and create a texture sheet
 * (2) addBlock() for each Block you want included in the texture sheet - doesn't actually capture the block's
 *     texture yet.
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

    GL11.glGenFramebuffers(1, &frameBuffer);
    glCheckFramebufferStatus
    glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);

    glFramebufferTexture2D(
            GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texColorBuffer, 0
                          );

    int frameBufferID = OpenGlHelper.glGenFramebuffers();
    if (frameBufferID < 0) {

      firstUncachedIndex = nextFreeTextureIndex;
    }
    for (Map.Entry<Block, Integer> entry : blockTextureNumbers.entrySet()) {
      if (entry.getValue() >= firstUncachedIndex) {
        stitchBlockTextureIntoSheet(entry.getKey().getDefaultState());
      }
    }

    glBindFramebuffer(GL_FRAMEBUFFER, 0);


    glDeleteFramebuffers(1, &frameBuffer);

    // draw the colored quad into the initially empty texture
    glDisable(GL_CULL_FACE);
    glDisable(GL_DEPTH_TEST);

// store attibutes
    glPushAttrib(GL_VIEWPORT_BIT | GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

// reset viewport
    glViewport(0, 0, width, height);

// setup modelview matrix
    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

// setup projection matrix
    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();

// setup orthogonal projection
    glOrtho(-width / 2, width / 2, -height / 2, height / 2, 0, 1000);

// bind framebuffer object
    glBindFramebuffer(GL_FRAMEBUFFER, frameBufferObject);

// attach empty texture to framebuffer object
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);

// check framebuffer status (see above)

// bind framebuffer object (IMPORTANT! bind before adding color attachments!)
    glBindFramebuffer(GL_FRAMEBUFFER, frameBufferObject);

// define render targets (empty texture is at GL_COLOR_ATTACHMENT0)
    glDrawBuffers(1, GL_COLOR_ATTACHMENT0); // you can of course have more than only 1 attachment

// activate & bind empty texture
// I figured activating and binding must take place AFTER attaching texture as color attachment
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, texture);

// clear color attachments
    glClear(GL_COLOR_BUFFER_BIT);

// make background yellow
    glClearColor(1.0f, 1.0f, 0.0f, 1.0f);

// draw quad into texture attached to frame buffer object
    glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    glBegin(GL_QUADS);
    glColor4f(1.0f, 1.0f, 1.0f, 1.0f); glVertex2f(0.0f, 100.0f); // top left
    glColor4f(1.0f, 0.0f, 0.0f, 1.0f); glVertex2f(0.0f, 0.0f); // bottom left
    glColor4f(0.0f, 1.0f, 0.0f, 1.0f); glVertex2f(100.0f, 0.0f); // bottom right
    glColor4f(0.0f, 0.0f, 1.0f, 1.0f); glVertex2f(100.0f, 100.0f); // top right
    glEnd();

// reset projection matrix
    glMatrixMode(GL_PROJECTION);
    glPopMatrix();

// reset modelview matrix
    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();

// restore attributes
    glPopAttrib();

    glEnable(GL_DEPTH_TEST);
    glEnable(GL_CULL_FACE);



    firstUncachedIndex = nextFreeTextureIndex;
  }

  /**
   * get the icon (position in the texture sheet) of the given block
   * @param iBlockState block to be retrieved (properties ignored)
   * @param whichFace which faces' texture?
   * @return the blocks' icon, or a generic "null" icon if not available
   */
  public SBTIcon getSBTIcon(IBlockState iBlockState, EnumFacing whichFace)
  {
    Integer textureIndex;
    if (iBlockState == null
        || !blockTextureNumbers.containsKey(iBlockState.getBlock())) {
      textureIndex = NULL_BLOCK_INDEX;
    } else {
      textureIndex = blockTextureNumbers.get(iBlockState.getBlock());
      if (textureIndex == null) {
        textureIndex = NULL_BLOCK_INDEX;
      }
    }

    double umin = TEXEL_WIDTH_PER_FACE * whichFace.getIndex();
    double vmin = TEXEL_HEIGHT_PER_BLOCK * textureIndex;
    return new SBTIcon(umin, umin + TEXEL_WIDTH_PER_FACE, vmin, vmin + TEXEL_HEIGHT_PER_BLOCK);
  }

  /** insert the default state of this block into the texture map
   * @param frameBufferID the frame buffer
   * @param iBlockState the block to texture; property is ignored (uses defaultstate)
   */
  private void stitchBlockTextureIntoSheet(int frameBufferID,  IBlockState iBlockState)
  {

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
