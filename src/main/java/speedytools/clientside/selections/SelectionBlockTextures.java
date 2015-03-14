package speedytools.clientside.selections;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import java.awt.*;

/**
 * Created by TheGreyGhost on 14/03/2015.
 *
 * Used to track the textures used for each cube in a selection.
 *
 *
 */
public class SelectionBlockTextures {


  public SelectionBlockTextures(TextureManager i_textureManager)
  {
    textureManager = i_textureManager;
    final int BLOCK_COUNT = 1;
    final int NUMBER_OF_FACES_PER_BLOCK = 6;
    final int U_TEXELS_PER_FACE = 16;
    final int V_TEXELS_PER_FACE = 16;
    int textureWidthTexels = BLOCK_COUNT * U_TEXELS_PER_FACE;
    int textureHeightTexels = NUMBER_OF_FACES_PER_BLOCK * V_TEXELS_PER_FACE;
    TEXELHEIGHTPERFACE = 1.0 / NUMBER_OF_FACES_PER_BLOCK;
    TEXELWIDTHPERBLOCK = 1.0 / BLOCK_COUNT;

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

  public SBTIcon getSBTIcon(IBlockState iBlockState, EnumFacing whichFace)
  {
    double umin = 0.0;
    double vmin = 0.0;
    return new SBTIcon(umin, umin + TEXELWIDTHPERBLOCK, vmin, vmin + TEXELHEIGHTPERFACE);
  }

  private final DynamicTexture blockTextures;
  private final ResourceLocation textureResourceLocation;
  private final TextureManager textureManager;
  private final double TEXELWIDTHPERBLOCK;
  private final double TEXELHEIGHTPERFACE;

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
