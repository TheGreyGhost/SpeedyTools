package speedytools.common;

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

  public static final Colour BLACK_40 = new Colour(0, 0, 0, 0.4F);
  public static final Colour WHITE_40 = new Colour(1.0F, 1.0F, 1.0F, 0.4F);
  public static final Colour GREEN_20 = new Colour(0, 1.0F, 0, 0.2F);
  public static final Colour YELLOW_20 = new Colour(1.0F, 1.0F, 0, 0.2F);
  public static final Colour GREENYELLOW_20 = new Colour(0.5F, 1.0F, 0, 0.2F);
}

