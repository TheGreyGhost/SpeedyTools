package speedytools.clientside.selections;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import scala.collection.parallel.ParIterableLike;
import speedytools.common.blocks.RegistryForBlocks;
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

//    int frameBufferID = OpenGlHelper.glGenFramebuffers();
    if (!OpenGlHelper.isFramebufferEnabled()) {  // frame buffer not available, just use blank texture
      eraseBlockTextures(firstUncachedIndex, nextFreeTextureIndex - 1);
      firstUncachedIndex = nextFreeTextureIndex;
      return;
    }

//
//    OpenGlHelper.isFramebufferEnabled()
//
//    GL11.glGenFramebuffers(1, &frameBuffer);
//    glCheckFramebufferStatus
//    glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
//
//    glFramebufferTexture2D(
//            GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texColorBuffer, 0
//                          );


    Framebuffer frameBuffer = null;
    try {
      final boolean USE_DEPTH = true;
      frameBuffer = new Framebuffer(U_TEXELS_PER_FACE, V_TEXELS_PER_FACE, USE_DEPTH);

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

      GL11.glDisable(GL11.GL_CULL_FACE);
      GL11.glDisable(GL11.GL_DEPTH_TEST);
//      this.depthBuffer = OpenGlHelper.glGenRenderbuffers();

      frameBuffer.setFramebufferColor(1.0F, 1.0F, 1.0F, 1.0F);
      frameBuffer.framebufferClear();

      frameBuffer.bindFramebufferTexture();
      IntBuffer pixelBuffer = BufferUtils.createIntBuffer(U_TEXELS_PER_FACE * V_TEXELS_PER_FACE);
      GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);

      int [] temp = new int[pixelBuffer.remaining()];
      pixelBuffer.get(temp);

      //----
      frameBuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
      frameBuffer.framebufferClear();

      pixelBuffer = BufferUtils.createIntBuffer(U_TEXELS_PER_FACE * V_TEXELS_PER_FACE);
      GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);

      temp = new int[pixelBuffer.remaining()];
      pixelBuffer.get(temp);

      //----
      frameBuffer.setFramebufferColor(0.0F, 1.0F, 0.0F, 1.0F);
      frameBuffer.framebufferClear();

      pixelBuffer = BufferUtils.createIntBuffer(U_TEXELS_PER_FACE * V_TEXELS_PER_FACE);
      GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);

      temp = new int[pixelBuffer.remaining()];
      pixelBuffer.get(temp);

      //----
      frameBuffer.setFramebufferColor(0.23F, 0.23F, 0.23F, 0.23F);
      frameBuffer.framebufferClear();

      pixelBuffer = BufferUtils.createIntBuffer(U_TEXELS_PER_FACE * V_TEXELS_PER_FACE);
      GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);

      temp = new int[pixelBuffer.remaining()];
      pixelBuffer.get(temp);

      final boolean SET_VIEWPORT_TRUE = true;
//      Random random = new Random();
//      boolean done = false;
//      do {
//        frameBuffer.setFramebufferColor(0.23F, 0.23F, 0.23F, 0.23F);
//        frameBuffer.framebufferClear();
//        frameBuffer.bindFramebuffer(SET_VIEWPORT_TRUE);
//        GL11.glOrtho(0.0D, 1.0, 1.0, 0.0, -10.0, 10.0);  // set up to render over [0,0,0] to [1,1,1]
//
//        float lx = 0.0F;
//        float rx = 1.0F;
//        float by = 0.0F;
//        float uy = 1.0F;
//        float fz = 0.5F;
//        int xLocationOfInterest = 0;
//        int yLocationOfInterest = 8;
//        int pixelOfInterestIndex = xLocationOfInterest + yLocationOfInterest * U_TEXELS_PER_FACE;
//        for (lx = 0.0F; lx <= 1.0F; lx += 0.25F/16.0F) {
//          GL11.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
//          GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
//
//          frameBuffer.bindFramebufferTexture();
//          pixelBuffer = BufferUtils.createIntBuffer(U_TEXELS_PER_FACE * V_TEXELS_PER_FACE);
//          GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
//          temp = new int[pixelBuffer.remaining()];
//          pixelBuffer.get(temp);
//          int firstval = temp[0];
//          for (int value : temp ) {
//            if (value != firstval ) {
//              done = true; // breakpoint here!  we have a problem
//            }
//          }
//
//          frameBuffer.bindFramebuffer(SET_VIEWPORT_TRUE);
//          GL11.glBegin(GL11.GL_QUADS);
//          GL11.glColor4f(0.0F, 0.0F, 0.0F, 1.0F);
//
//
//          GL11.glVertex4f(lx, by, fz, 1.0F);
//          GL11.glVertex4f(rx, by, fz, 1.0F);
//          GL11.glVertex4f(rx, uy, fz, 1.0F);
//          GL11.glVertex4f(lx, uy, fz, 1.0F);
//          GL11.glEnd();
//
//          frameBuffer.bindFramebufferTexture();
//          pixelBuffer = BufferUtils.createIntBuffer(U_TEXELS_PER_FACE * V_TEXELS_PER_FACE);
//          GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
//
//          temp = new int[pixelBuffer.remaining()];
//          pixelBuffer.get(temp);
//
//          String output = "LX:" + lx + " ->";
//          for (int i = 0; i < U_TEXELS_PER_FACE; ++i ) {
//            output += String.format("%X", temp[pixelOfInterestIndex + i]) + " ";
//          }
//          System.out.println(output);
//        }
//
//        final float RNG = 1000.0F; final float MID = 500.0F;
//        float x1 = random.nextFloat() * RNG - MID;
//        float y1 = random.nextFloat() * RNG - MID;
//        float z1 = random.nextFloat() * RNG - MID;
//
//        float x2 = random.nextFloat() * RNG - MID;
//        float y2 = random.nextFloat() * RNG - MID;
//        float z2 = random.nextFloat() * RNG - MID;
//
//        GL11.glBegin(GL11.GL_QUADS);
//        GL11.glColor4f(0.2F, 0.1F, 0.0F, 1.0F);
//        GL11.glVertex4f(x1, y1, z1, 1.0F);
//        GL11.glVertex4f(x1, y2, z1, 1.0F);
//        GL11.glVertex4f(x2, y2, z1, 1.0F);
//        GL11.glVertex4f(x2, y1, z1, 1.0F);
//
//        GL11.glVertex4f(x1, y1, z1, 1.0F);
//        GL11.glVertex4f(x1, y2, z1, 1.0F);
//        GL11.glVertex4f(x1, y2, z2, 1.0F);
//        GL11.glVertex4f(x1, y1, z2, 1.0F);
//
//        GL11.glVertex4f(x1, y1, z1, 1.0F);
//        GL11.glVertex4f(x2, y1, z1, 1.0F);
//        GL11.glVertex4f(x2, y1, z2, 1.0F);
//        GL11.glVertex4f(x1, y1, z2, 1.0F);
//
//        GL11.glEnd();
//
////        GL11.glBegin(GL11.GL_QUADS);
////        GL11.glVertex4f(0.125F, 0.40F, 0.019F, 1.0F);
////        GL11.glVertex4f(0.157F, -0.475F, 0.299F, 1.0F);
////        GL11.glVertex4f(0.488F, 0.042F, 0.11F, 1.0F);
////        GL11.glVertex4f(-0.02F, -0.24F, 0.195F, 1.0F);
////        GL11.glEnd();
//
//        float offsetX = random.nextFloat() - random.nextFloat();
//        float offsetY = random.nextFloat() - random.nextFloat();
//        float offsetZ = random.nextFloat() - random.nextFloat();
//        switch (random.nextInt(3)) {
//          case 0: offsetX = 0; offsetY = 0; break;
//          case 1: offsetX = 0; offsetZ = 0; break;
//          case 2: offsetY = 0; offsetZ = 0; break;
//
//        }
////        GL11.glBegin(GL11.GL_TRIANGLES);
////        GL11.glVertex4f(0.125F+offsetX, 0.40F+offsetY, 0.019F+offsetZ, 1.0F);
////        GL11.glVertex4f(0.157F+offsetX, -0.475F+offsetY, 0.299F+offsetZ, 1.0F);
////        GL11.glVertex4f(0.488F+offsetX, 0.042F+offsetY, 0.11F+offsetZ, 1.0F);
//////        GL11.glVertex4f(-0.02F, -0.24F, 0.195F, 1.0F);
////        GL11.glEnd();
//
////        GL11.glBegin(GL11.GL_TRIANGLES);
////        GL11.glVertex4f(0.125F+offsetX, 0.40F+offsetY, 0.2F, 1.0F);
////        GL11.glVertex4f(0.157F+offsetX, -0.475F+offsetY, 0.2F, 1.0F);
////        GL11.glVertex4f(0.488F+offsetX, 0.042F+offsetY, 0.2F, 1.0F);
//////        GL11.glVertex4f(-0.02F, -0.24F, 0.195F, 1.0F);
////        GL11.glEnd();
//
//        GL11.glBegin(GL11.GL_TRIANGLES);
//        GL11.glVertex4f(0.5F, 0.90F, 0.2F, 1.0F);
//        GL11.glVertex4f(0.8F, 0.90F, 0.2F, 1.0F);
//        GL11.glVertex4f(0.5F, 0.30F, 0.2F, 1.0F);
////        GL11.glVertex4f(-0.02F, -0.24F, 0.195F, 1.0F);
//        GL11.glEnd();
//
//        frameBuffer.bindFramebufferTexture();
//        pixelBuffer = BufferUtils.createIntBuffer(U_TEXELS_PER_FACE * V_TEXELS_PER_FACE);
//        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
//
//        temp = new int[pixelBuffer.remaining()];
//        pixelBuffer.get(temp);
//        int firstval = temp[0];
//        for (int value : temp ) {
//          if (value != firstval ) {
//            done = true; // breakpoint here!
//          }
//        }
//
//      } while (!done);

      frameBuffer.setFramebufferColor(1.0F, 1.0F, 1.0F, 1.0F);
      frameBuffer.framebufferClear();

      frameBuffer.bindFramebufferTexture();
      pixelBuffer = BufferUtils.createIntBuffer(U_TEXELS_PER_FACE * V_TEXELS_PER_FACE);
      GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);

      temp = new int[pixelBuffer.remaining()];
      pixelBuffer.get(temp);

      frameBuffer.bindFramebuffer(SET_VIEWPORT_TRUE);
      GL11.glOrtho(0.0D, 1.0, 1.0, 0.0, -10.0, 10.0);  // set up to render over [0,0,0] to [1,1,1]
      GL11.glEnable(GL11.GL_CULL_FACE);
      GL11.glEnable(GL11.GL_DEPTH_TEST);

      Minecraft mc = Minecraft.getMinecraft();
      BlockRendererDispatcher blockRendererDispatcher = mc.getBlockRendererDispatcher();
      BlockModelShapes blockModelShapes = blockRendererDispatcher.getBlockModelShapes();
      Block testBlock = RegistryForBlocks.blockSelectionSolidFog;
      IBlockState testState = testBlock.getDefaultState();
      IBakedModel ibakedmodel = blockModelShapes.getModelForState(testState);

      if (ibakedmodel instanceof net.minecraftforge.client.model.ISmartBlockModel) {
        ibakedmodel = ((net.minecraftforge.client.model.ISmartBlockModel)ibakedmodel).handleBlockState(testState);
      }

//      Tessellator tessellator = Tessellator.getInstance();
//      WorldRenderer worldrenderer = tessellator.getWorldRenderer();
//      Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
//      worldrenderer.startDrawingQuads();
//
//      worldrenderer.setColorOpaque_I(Color.YELLOW.getRGB());
//      renderModelStandard(ibakedmodel, worldrenderer);

      // from RenderFallingBlock.doRender()

      for (EnumFacing facing : EnumFacing.values()) {
        List<BakedQuad> faceQuads = ibakedmodel.getFaceQuads(facing);
        if (!faceQuads.isEmpty()) {

          frameBuffer.setFramebufferColor(0.315F, 0.315F, 0.315F, 0.315F);
          frameBuffer.framebufferClear();
          frameBuffer.bindFramebuffer(SET_VIEWPORT_TRUE);
          GL11.glOrtho(0.0D, 1.0, 1.0, 0.0, -10.0, 10.0);  // set up to render over [0,0,0] to [1,1,1]

          GlStateManager.disableLighting();
          Tessellator tessellator = Tessellator.getInstance();
          WorldRenderer worldrenderer = tessellator.getWorldRenderer();
          worldrenderer.startDrawingQuads();
          worldrenderer.setVertexFormat(DefaultVertexFormats.BLOCK);
          Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

          renderModelStandardQuads(worldrenderer, faceQuads);
//          renderModelStandard(ibakedmodel, worldrenderer);
          tessellator.draw();
          GlStateManager.enableLighting();

          frameBuffer.bindFramebufferTexture();
          pixelBuffer = BufferUtils.createIntBuffer(U_TEXELS_PER_FACE * V_TEXELS_PER_FACE);
          GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);

          temp = new int[pixelBuffer.remaining()];
          pixelBuffer.get(temp);


        }

      }


//      int i = blockpos.getX();
//      int j = blockpos.getY();
//      int k = blockpos.getZ();
//      worldrenderer.setTranslation((double)((float)(-i) - 0.5F), (double)(-j), (double)((float)(-k) - 0.5F));
//      BlockRendererDispatcher blockrendererdispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
//      IBakedModel ibakedmodel = blockrendererdispatcher.getModelFromBlockState(iblockstate, world, (BlockPos)null);
//      blockrendererdispatcher.getBlockModelRenderer().renderModel(world, ibakedmodel, iblockstate, blockpos, worldrenderer, false);
//      worldrenderer.setTranslation(0.0D, 0.0D, 0.0D);



//      worldrenderer.setColorOpaque_I(0x0);
//      worldrenderer.addVertex(0.25, 0.25, 0.5);
//      worldrenderer.addVertex(0.75, 0.25, 0.5);
//      worldrenderer.addVertex(0.75, 0.75, 0.5);
//      worldrenderer.addVertex(0.25, 0.75, 0.5);
//      tessellator.draw();

      frameBuffer.bindFramebufferTexture();
      pixelBuffer = BufferUtils.createIntBuffer(U_TEXELS_PER_FACE * V_TEXELS_PER_FACE);
      GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);

      temp = new int[pixelBuffer.remaining()];
      pixelBuffer.get(temp);





//      OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, frameBufferID);
//      OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_COLOR_ATTACHMENT0,
//                                          GL11.GL_TEXTURE_2D, blockTextures.getGlTextureId(), 0);
//
//      GlStateManager.bindTexture(this.framebufferTexture);
//      GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, (float)p_147607_1_);
//      GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, (float)p_147607_1_);
//      GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 10496.0F);
//      GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 10496.0F);
//      GlStateManaDyger.colorMask(true, true, true, false);
//      GlStateManager.disableDepth();
//      GlStateManager.depthMask(false);
//      GlStateManager.matrixMode(5889);
//      GlStateManager.loadIdentity();
//      GlStateManager.ortho(0.0D, (double)p_178038_1_, (double)p_178038_2_, 0.0D, 1000.0D, 3000.0D);
//      GlStateManager.matrixMode(5888);
//      GlStateManager.loadIdentity();
//      GlStateManager.translate(0.0F, 0.0F, -2000.0F);
//      GlStateManager.viewport(0, 0, p_178038_1_, p_178038_2_);
//      GlStateManager.enableTexture2D();
//      GlStateManager.disableLighting();
//      GlStateManager.disableAlpha();
//
//      if (p_178038_3_)
//      {
//        GlStateManager.disableBlend();
//        GlStateManager.enableColorMaterial();
//      }
//
//      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
//      this.bindFramebufferTexture();
//      float f = (float)p_178038_1_;
//      float f1 = (float)p_178038_2_;
//      float f2 = (float)this.framebufferWidth / (float)this.framebufferTextureWidth;
//      float f3 = (float)this.framebufferHeight / (float)this.framebufferTextureHeight;
//      Tessellator tessellator = Tessellator.getInstance();
//      WorldRenderer worldrenderer = tessellator.getWorldRenderer();
//      worldrenderer.startDrawingQuads();
//      worldrenderer.setColorOpaque_I(-1);
//      worldrenderer.addVertexWithUV(0.0D, (double)f1, 0.0D, 0.0D, 0.0D);
//      worldrenderer.addVertexWithUV((double)f, (double)f1, 0.0D, (double)f2, 0.0D);
//      worldrenderer.addVertexWithUV((double)f, 0.0D, 0.0D, (double)f2, (double)f3);
//      worldrenderer.addVertexWithUV(0.0D, 0.0D, 0.0D, 0.0D, (double)f3);
//      tessellator.draw();
//      this.unbindFramebufferTexture();
//      GlStateManager.depthMask(true);
//      GlStateManager.colorMask(true, true, true, true);

      for (Map.Entry<Block, Integer> entry : blockTextureNumbers.entrySet()) {
        if (entry.getValue() >= firstUncachedIndex) {
          stitchBlockTextureIntoSheet(entry.getValue(), entry.getKey().getDefaultState());
        }
      }

    } catch (Exception e) {
      ErrorLog.defaultLog().info(e.toString());
    } finally {
      if (frameBuffer != null) {
        frameBuffer.deleteFramebuffer();
      }

//      OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
//      OpenGlHelper.glDeleteFramebuffers(frameBufferID);
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glPopMatrix();
      GL11.glPopAttrib();
    }

//    // draw the colored quad into the initially empty texture
//    glDisable(GL_CULL_FACE);
//    glDisable(GL_DEPTH_TEST);
//
//// store attibutes
//    glPushAttrib(GL_VIEWPORT_BIT | GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
//
//// reset viewport
//    glViewport(0, 0, width, height);
//
//// setup modelview matrix
//    glMatrixMode(GL_MODELVIEW);
//    glPushMatrix();
//    glLoadIdentity();
//
//// setup projection matrix
//    glMatrixMode(GL_PROJECTION);
//    glPushMatrix();
//    glLoadIdentity();
//
//// setup orthogonal projection
//    glOrtho(-width / 2, width / 2, -height / 2, height / 2, 0, 1000);
//
//// bind framebuffer object
//    glBindFramebuffer(GL_FRAMEBUFFER, frameBufferObject);
//
//// attach empty texture to framebuffer object
//    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
//
//// check framebuffer status (see above)
//
//// bind framebuffer object (IMPORTANT! bind before adding color attachments!)
//    glBindFramebuffer(GL_FRAMEBUFFER, frameBufferObject);
//
//// define render targets (empty texture is at GL_COLOR_ATTACHMENT0)
//    glDrawBuffers(1, GL_COLOR_ATTACHMENT0); // you can of course have more than only 1 attachment
//
//// activate & bind empty texture
//// I figured activating and binding must take place AFTER attaching texture as color attachment
//    glActiveTexture(GL_TEXTURE0);
//    glBindTexture(GL_TEXTURE_2D, texture);
//
//// clear color attachments
//    glClear(GL_COLOR_BUFFER_BIT);
//
//// make background yellow
//    glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
//
//// draw quad into texture attached to frame buffer object
//    glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
//    glBegin(GL_QUADS);
//    glColor4f(1.0f, 1.0f, 1.0f, 1.0f); glVertex2f(0.0f, 100.0f); // top left
//    glColor4f(1.0f, 0.0f, 0.0f, 1.0f); glVertex2f(0.0f, 0.0f); // bottom left
//    glColor4f(0.0f, 1.0f, 0.0f, 1.0f); glVertex2f(100.0f, 0.0f); // bottom right
//    glColor4f(0.0f, 0.0f, 1.0f, 1.0f); glVertex2f(100.0f, 100.0f); // top right
//    glEnd();
//
//// reset projection matrix
//    glMatrixMode(GL_PROJECTION);
//    glPopMatrix();
//
//// reset modelview matrix
//    glMatrixMode(GL_MODELVIEW);
//    glPopMatrix();
//
//// restore attributes
//    glPopAttrib();
//
//    glEnable(GL_DEPTH_TEST);
//    glEnable(GL_CULL_FACE);
//
//

    firstUncachedIndex = nextFreeTextureIndex;
  }

  public void renderModelStandard(IBakedModel modelIn, WorldRenderer worldRendererIn)
  {
    for (EnumFacing facing : EnumFacing.values()) {
      List<BakedQuad> faceQuads = modelIn.getFaceQuads(facing);
      if (!faceQuads.isEmpty()) {
        renderModelStandardQuads(worldRendererIn, faceQuads);
      }
    }

    List<BakedQuad> generalQuads = modelIn.getGeneralQuads();
    if (!generalQuads.isEmpty()) {
      renderModelStandardQuads(worldRendererIn, generalQuads);
    }
  }

  private void renderModelStandardQuads(WorldRenderer worldRendererIn, List<BakedQuad> bakedQuadList)
  {
//    double d0 = (double)blockPosIn.getX();
//    double d1 = (double)blockPosIn.getY();
//    double d2 = (double)blockPosIn.getZ();

    final int VERTEX_BRIGHTNESS = -1;
    for (BakedQuad bakedQuad : bakedQuadList ) {
      worldRendererIn.addVertexData(bakedQuad.getVertexData());
      worldRendererIn.putBrightness4(VERTEX_BRIGHTNESS, VERTEX_BRIGHTNESS, VERTEX_BRIGHTNESS, VERTEX_BRIGHTNESS);

//      worldRendererIn.putBrightness4(brightnessIn, brightnessIn, brightnessIn, brightnessIn);
    }

//    for (Iterator iterator = listQuadsIn.iterator(); iterator.hasNext(); worldRendererIn.putPosition(d0, d1, d2))
//    {
//      BakedQuad bakedquad = (BakedQuad)iterator.next();
//
//      if (ownBrightness)
//      {
//        this.fillQuadBounds(blockIn, bakedquad.getVertexData(), bakedquad.getFace(), (float[])null, boundsFlags);
//        brightnessIn = boundsFlags.get(0) ? blockIn.getMixedBrightnessForBlock(blockAccessIn, blockPosIn.offset(bakedquad.getFace())) : blockIn.getMixedBrightnessForBlock(blockAccessIn, blockPosIn);
//      }
//
//      worldRendererIn.addVertexData(bakedquad.getVertexData());
//      worldRendererIn.putBrightness4(brightnessIn, brightnessIn, brightnessIn, brightnessIn);
//
//      if (bakedquad.hasTintIndex())
//      {
//        int i1 = blockIn.colorMultiplier(blockAccessIn, blockPosIn, bakedquad.getTintIndex());
//
//        if (EntityRenderer.anaglyphEnable)
//        {
//          i1 = TextureUtil.anaglyphColor(i1);
//        }
//
//        float f = (float)(i1 >> 16 & 255) / 255.0F;
//        float f1 = (float)(i1 >> 8 & 255) / 255.0F;
//        float f2 = (float)(i1 & 255) / 255.0F;
//        worldRendererIn.putColorMultiplier(f, f1, f2, 4);
//        worldRendererIn.putColorMultiplier(f, f1, f2, 3);
//        worldRendererIn.putColorMultiplier(f, f1, f2, 2);
//        worldRendererIn.putColorMultiplier(f, f1, f2, 1);
//      }
//    }
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
