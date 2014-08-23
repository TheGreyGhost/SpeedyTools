package speedytools.common.utilities;

/**
 * User: The Grey Ghost
 * Date: 12/02/14
 */
public class Colour
{
  public final float R;
  public final float G;
  public final float B;
  public final float A;

  public Colour(float initR, float initG, float initB, float initA)
  {
    this.R = initR;
    this.G = initG;
    this.B = initB;
    this.A = initA;
  }

  public int getColourForFontRenderer()
  {
    int red = UsefulFunctions.clipToRange((int)(this.R * 255), 0, 255);
    int green = UsefulFunctions.clipToRange((int)(this.G * 255), 0, 255);
    int blue = UsefulFunctions.clipToRange((int)(this.B * 255), 0, 255);
    int alpha = UsefulFunctions.clipToRange((int)(this.A * 255), 0, 255);
    return green | (blue << 8) | (red << 16) | (alpha << 24);
  }

  public int getColourForFontRenderer(double alphaValue)
  {
    int baseColour = getColourForFontRenderer();
    int newAlpha = UsefulFunctions.clipToRange((int)(alphaValue * 255), 0, 255);
    return (baseColour & 0xffffff) | (newAlpha << 24);
  }

  public static final Colour BLACK_40 = new Colour(0, 0, 0, 0.4F);
  public static final Colour WHITE_40 = new Colour(1.0F, 1.0F, 1.0F, 0.4F);
  public static final Colour GREEN_20 = new Colour(0, 1.0F, 0, 0.2F);
  public static final Colour YELLOW_20 = new Colour(1.0F, 1.0F, 0, 0.2F);
  public static final Colour GREENYELLOW_20 = new Colour(0.5F, 1.0F, 0, 0.2F);
  public static final Colour PINK_100 = new Colour(240.0F/255, 106.0F/255, 200.0F/255, 1.0F);
  public static final Colour RED_100 = new Colour(1.0F, 0.0F, 0.0F, 1.0F);
  public static final Colour GREEN_100 = new Colour(0.0F, 1.0F, 0.0F, 1.0F);
  public static final Colour BLUE_100 = new Colour(0.0F, 0.0F, 1.0F, 1.0F);
  public static final Colour PINK_40 = new Colour(240.0F/255, 106.0F/255, 200.0F/255, 0.4F);
  public static final Colour LIGHTGREEN_40 = new Colour(0.4F, 1.0F, 0.4F, 0.4F);
  public static final Colour LIGHTBLUE_40 = new Colour(0.4F, 0.4F, 1.0F, 0.4F);
  public static final Colour LIGHTRED_40 = new Colour(1.0F, 0.4F, 0.4F, 0.4F);
}

