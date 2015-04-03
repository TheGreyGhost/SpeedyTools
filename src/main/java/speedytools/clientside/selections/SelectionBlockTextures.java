package speedytools.clientside.selections;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.HashMap;

/**
 * Created by TheGreyGhost on 14/03/2015.
 *
 * Used to track the textures used for each cube in a selection.
 * The first time the icon for a given block is requested, the block's model will be rendered by projecting onto the
 *   six cube sides, converted to greyscale, and the resulting textures stored.
 * Subsequent calls will return the cached texture.
 * A limited number of block textures will be stored, once full a generic block texture will be returned instead
 */
public class SelectionBlockTextures {

  public SelectionBlockTextures(TextureManager i_textureManager)
  {
    textureManager = i_textureManager;
    int textureWidthTexels = BLOCK_COUNT_DESIRED * U_TEXELS_PER_FACE;
    int maximumTextureWidth = Minecraft.getGLMaximumTextureSize();
    if (textureWidthTexels <= maximumTextureWidth) {
      BLOCK_COUNT = BLOCK_COUNT_DESIRED;
    } else {
      BLOCK_COUNT = maximumTextureWidth / U_TEXELS_PER_FACE;
      textureWidthTexels = maximumTextureWidth;
    }
    LAST_BLOCK_INDEX_PLUS_ONE = FIRST_BLOCK_INDEX + BLOCK_COUNT - 1;

    int textureHeightTexels = NUMBER_OF_FACES_PER_BLOCK * V_TEXELS_PER_FACE;

    TEXELHEIGHTPERFACE = 1.0 / NUMBER_OF_FACES_PER_BLOCK;
    TEXELWIDTHPERBLOCK = 1.0 / BLOCK_COUNT;

    nextFreeTextureIndex = FIRST_BLOCK_INDEX;
    blockTextures = new DynamicTexture(textureWidthTexels, textureHeightTexels);
    textureResourceLocation = textureManager.getDynamicTextureLocation("SelectionBlockTextures", blockTextures);

    // just for now, initialise texture to all white
    // todo make texturing properly
    int [] rawTexture = blockTextures.getTextureData();

    for (int i = 0; i < rawTexture.length; ++i) {
      rawTexture[i] = Color.WHITE.getRGB();
    }

    blockTextures.updateDynamicTexture();
  }

  public void bindTexture() {
    textureManager.bindTexture(textureResourceLocation);
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
    if (iBlockState == null) {
      textureIndex = NULL_BLOCK_INDEX;
    } else if (!blockTextureNumber.containsKey(iBlockState.getBlock())) {
      if (nextFreeTextureIndex < LAST_BLOCK_INDEX_PLUS_ONE) {
        textureIndex = nextFreeTextureIndex;
        addBlockTextureToMap(iBlockState);
      } else {
        textureIndex = NULL_BLOCK_INDEX;
      }
    } else {
      textureIndex = blockTextureNumber.get(iBlockState.getBlock());
      if (textureIndex == null) {
        textureIndex = NULL_BLOCK_INDEX;
      }
    }

    double umin = TEXELWIDTHPERBLOCK * textureIndex;
    double vmin = TEXELHEIGHTPERFACE * whichFace.getIndex();
    return new SBTIcon(umin, umin + TEXELWIDTHPERBLOCK, vmin, vmin + TEXELHEIGHTPERFACE);
  }

  /** insert the default state of this block into the texture map
   * @param iBlockState the block to texture; property is ignored (uses defaultstate)
   */
  public void addBlockTextureToMap(IBlockState iBlockState)
  {

  }

  private final DynamicTexture blockTextures;
  private final ResourceLocation textureResourceLocation;
  private final TextureManager textureManager;
  private final double TEXELWIDTHPERBLOCK;
  private final double TEXELHEIGHTPERFACE;
  private int nextFreeTextureIndex;

  private final int NUMBER_OF_FACES_PER_BLOCK = 6;
  private final int U_TEXELS_PER_FACE = 16;
  private final int V_TEXELS_PER_FACE = 16;

  private final int NULL_BLOCK_INDEX = 0;
  private final int FIRST_BLOCK_INDEX = NULL_BLOCK_INDEX + 1;

  private final int BLOCK_COUNT_DESIRED = 256;
  private final int BLOCK_COUNT;
  private final int LAST_BLOCK_INDEX_PLUS_ONE;

  private HashMap<Block, Integer> blockTextureNumber = new HashMap<Block, Integer>(BLOCK_COUNT_DESIRED);

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

}
