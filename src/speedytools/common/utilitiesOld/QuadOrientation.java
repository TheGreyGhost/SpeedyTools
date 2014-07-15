package speedytools.common.utilitiesOld;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

NOTE
bugs:
undo is offset when rotated 2x5 pattern is placed and undone
place/unplace highlighting is confusing
selection cancel and remake gives problems if drag mode is still on.


/**
 * User: The Grey Ghost
 * Date: 12/07/2014
 * This class is used to help keep track of the location and orientation of a quad within the world.
 * The quad is a 2D array indexed by [x, z] - its initial coordinates are [0,0] to [xsize-1, zsize-1]
 * It is mapped to a 2D location in the world by providing an [wxOrigin, wzOrigin] for the bottom left corner.
 * The quad can be flipped and rotated, and the class can be used to convert between the [x,z] and the [wx,wz]
 * The x, z axes are oriented as per the minecraft world, i.e. a 90 degrees clockwise rotation from the x axis [1,0] gives the z axis [0,1]
 * For example:
 * If the quad is set up with size [7, 6] and origin at [100, 200]:
 * An unflipped, unrotated quad:
 *   [x,z]->[wx, wz] gives [0,0] -> [100, 200], and [2,3] -> [102, 203]
 * If the quad is then flipped left-right so that xneg becomes xpos:
 *   [x,z]->[wz, wz] gives [0,0] -> [104, 200], and [2,3] -> [104, 203]
 * If the quad is rotated and the origin becomes a half a block, it is rounded down; eg
 * Rotating the quad above by 90 clockwise gives
 *  [x,z]->[wx, wz] gives [0,0] -> [100.5, 199.5] but this is rounded to [100, 199]
 */
public class QuadOrientation
{
  public QuadOrientation(int i_wxOrigin, int i_wzOrigin, int i_xSize, int i_zSize)
  {
    this(i_wxOrigin, i_wzOrigin, i_xSize, i_zSize, false, (byte)0);
  }

  public QuadOrientation(int i_wxOrigin, int i_wzOrigin, int i_xSize, int i_zSize, boolean i_flippedX, byte i_clockwiseRotationCount)
  {
    wxOrigin = i_wxOrigin;
    wzOrigin = i_wzOrigin;
    xSize = i_xSize;
    zSize = i_zSize;
    flippedX = i_flippedX;
    clockwiseRotationCount = i_clockwiseRotationCount;
    transformationIndex = (flippedX ? 4 : 0) | (clockwiseRotationCount & 3);
    calculateOriginTransformed();
  }

  public QuadOrientation(DataInputStream inputStream) throws IOException
  {
    transformationIndex = inputStream.readInt();
    wxOrigin = inputStream.readInt();
    wzOrigin = inputStream.readInt();
    xSize = inputStream.readInt();
    zSize = inputStream.readInt();
    flippedX = (transformationIndex & 4) != 0;
    clockwiseRotationCount = (byte)(transformationIndex & 3);
    calculateOriginTransformed();
  }

  /**
   * convert the quad array coordinates into world coordinates
   * @param x
   * @param z
   * @return wx - no boundary checking performed
   */
  public int calcWXfromXZ(int x, int z) {
    return wxOriginTransformed + x * WX_MULTIPLIER_FROM_X[transformationIndex] + z * WX_MULTIPLIER_FROM_Z[transformationIndex];
  }

  /**
   * convert the quad array coordinates into world coordinates
   * @param x
   * @param z
   * @return wx - no boundary checking performed
   */
  public int calcWZfromXZ(int x, int z) {
    return wzOriginTransformed + x * WZ_MULTIPLIER_FROM_X[transformationIndex] + z * WZ_MULTIPLIER_FROM_Z[transformationIndex];
  }

  /**
   * convert the world coordinates into array coordinates
   * @param wx
   * @param wz
   * @return x
   */
  public int calcXfromWXZ(int wx, int wz)  {
    return (wx - wxOriginTransformed) * X_MULTIPLIER_FROM_WX[transformationIndex] +  (wz - wzOriginTransformed) * X_MULTIPLIER_FROM_WZ[transformationIndex];
  }

  /**
   * convert the world coordinates into array coordinates
   * @param wx
   * @param wz
   * @return x
   */
  public int calcZfromWXZ(int wx, int wz)  {
    return (wx - wxOriginTransformed) * Z_MULTIPLIER_FROM_WX[transformationIndex] +  (wz - wzOriginTransformed) * Z_MULTIPLIER_FROM_WZ[transformationIndex];
  }

  /**
   * convert the world coordinates into array coordinates
   * @param wx
   * @param wz
   * @return x
   */
  public double calcXfromWXZ(double wx, double wz)  {
    double rotationPointX = wxOrigin + xSize / 2.0;
    double rotationPointZ = wzOrigin + zSize / 2.0;
    double radiusWX = wx - rotationPointX;
    double radiusWZ = wz - rotationPointZ;
    return rotationPointX + radiusWX * X_MULTIPLIER_FROM_WX[transformationIndex] + radiusWZ * X_MULTIPLIER_FROM_WZ[transformationIndex];
  }

  /**
   * convert the world coordinates into array coordinates
   * @param wx
   * @param wz
   * @return z
   */
  public double calcZfromWXZ(double wx, double wz) {
    wx -= wxNudge;
    wz -= wzNudge;
    double rotationPointX = wxOrigin + xSize / 2.0;
    double rotationPointZ = wzOrigin + zSize / 2.0;
    double radiusWX = wx - rotationPointX;
    double radiusWZ = wz - rotationPointZ;
    return rotationPointZ + radiusWX * Z_MULTIPLIER_FROM_WX[transformationIndex] + radiusWZ * Z_MULTIPLIER_FROM_WZ[transformationIndex];
  }


  /** If the quad is rotated by 90 or 270 degrees, and the xSize is odd and the zSize is even (or vica versa) then
   *    the quad is nudged to align with the grid.
   *    This function returns that "nudge" in world coordinates
    * @return [wxOffset, wzOffset] - eg if the origin has been rounded from 15.5 to 15, offset is -0.5
   */
  public Pair<Float, Float> getWXZNudge()
  {
    return new Pair<Float, Float>(wxNudge, wzNudge);
  }

  /**
   * Flip the quad left-right in the world coordinates i.e wxneg becomes wxpos
   */
  public void flipWX()
  {
    if ((clockwiseRotationCount & 1) == 0) {
      flipX();
    } else {
      flipZ();
    }
  }

  /**
   * Flip the quad front-back in the world coordinates  i.e wzneg becomes wzpos
   */
  public void flipWZ()
  {
    if ((clockwiseRotationCount & 1) == 0) {
      flipZ();
    } else {
      flipX();
    }
  }

  private final int [] FLIP_X_NEW_INDEX = {4, 7, 6, 5, 0, 3, 2, 1};
  /** flip the quad left-right i.e xneg becomes xpos
   */
  public void flipX()
  {
    transformationIndex = FLIP_X_NEW_INDEX[transformationIndex];
    flippedX = (transformationIndex & 4) != 0;
    clockwiseRotationCount = (byte)(transformationIndex & 3);
    calculateOriginTransformed();
  }

  private final int [] FLIP_Z_NEW_INDEX = {6, 5, 4, 7, 2, 1, 0, 3};
  /** flip the quad front-back i.e zneg becomes zpos
   */
  public void flipZ()
  {
    transformationIndex = FLIP_Z_NEW_INDEX[transformationIndex];
    flippedX = (transformationIndex & 4) != 0;
    clockwiseRotationCount = (byte)(transformationIndex & 3);
    calculateOriginTransformed();
  }

  /**
   * rotate the quad by 0, 90, 180, or 270 degrees
   * @param rotationCount the number of quadrants to rotate clockwise
   */
  public void rotateClockwise(int rotationCount)
  {
    clockwiseRotationCount = (byte)((clockwiseRotationCount + rotationCount) & 3);
    transformationIndex = (flippedX ? 4 : 0) | (clockwiseRotationCount & 3);
    calculateOriginTransformed();
  }

  public byte getClockwiseRotationCount() {
    return clockwiseRotationCount;
  }

  public boolean isFlippedX() {
    return flippedX;
  }

  public void writeToStream(DataOutputStream outputStream) throws IOException
  {
    outputStream.writeInt(transformationIndex);
    outputStream.writeInt(wxOrigin);
    outputStream.writeInt(wzOrigin);
    outputStream.writeInt(xSize);
    outputStream.writeInt(zSize);
  }

  /** return the current x size of the quad in world coordinates
   * @return
   */
  public int getWXsize() {
    return ((clockwiseRotationCount & 1) == 0) ? xSize : zSize;
  }

  /** return the current z size of the quad in world coordinates
   * @return
   */
  public int getWZSize() {
    return ((clockwiseRotationCount & 1) == 0) ? zSize : xSize;
  }

  private void calculateOriginTransformed()
  {
    // algorithm is:
    // find the centre point, calculate the radius of the origin from the centrepoint (use the centre of the origin block as the origin)
    // then round the origin back to the bottom left corner again.
    // if the quad no longer aligns to the grid, i.e.  because
    // a) the quad is rotated by 90 or 270; and
    // b) xSize is odd and zSize is even (or vica versa);
    // then nudge down and left by half a block in world coordinates
    float xRadius = (xSize-1) / 2.0F;
    float xRotationPoint = xSize / 2.0F;
    float zRadius = (zSize-1) / 2.0F;
    float zRotationPoint = zSize / 2.0F;
    float xNewOrigin = xRotationPoint - xRadius * WX_MULTIPLIER_FROM_X[transformationIndex] - zRadius * WX_MULTIPLIER_FROM_Z[transformationIndex];
    float zNewOrigin = zRotationPoint - xRadius * WZ_MULTIPLIER_FROM_X[transformationIndex] - zRadius * WZ_MULTIPLIER_FROM_Z[transformationIndex];

    final float ROUND_DELTA = 0.1F;  // ensure correct rounding
    int xRounded = Math.round(xNewOrigin - ROUND_DELTA - 0.5F);  // add back the half block to move from centre of origin block to edge; nudge if necessary to align with grid
    int zRounded = Math.round(zNewOrigin - ROUND_DELTA - 0.5F);
    wxOriginTransformed = xRounded + wxOrigin;
    wzOriginTransformed = zRounded + wzOrigin;
    boolean parityMatches = ((xSize ^ zSize) & 1) == 0;
    wxNudge = (!parityMatches && ((clockwiseRotationCount & 1) != 0)) ? -0.5F : 0;
    wzNudge = wxNudge;
  }

  // 0 - 3 = rotations, 4 - 7 = flipX followed by rotations (& 0x03)
  private final int [] WX_MULTIPLIER_FROM_X = { 1,  0, -1,  0, -1,  0,  1,  0};
  private final int [] WX_MULTIPLIER_FROM_Z = { 0, -1,  0,  1,  0, -1,  0,  1};
  private final int [] WZ_MULTIPLIER_FROM_X = { 0,  1,  0, -1,  0, -1,  0,  1};
  private final int [] WZ_MULTIPLIER_FROM_Z = { 1,  0, -1,  0,  1,  0, -1,  0};

  private final int [] X_MULTIPLIER_FROM_WX = { 1,  0, -1,  0, -1,  0,  1,  0};
  private final int [] X_MULTIPLIER_FROM_WZ = { 0,  1,  0, -1,  0, -1,  0,  1};
  private final int [] Z_MULTIPLIER_FROM_WX = { 0, -1,  0,  1,  0, -1,  0,  1};
  private final int [] Z_MULTIPLIER_FROM_WZ = { 1,  0, -1,  0,  1,  0, -1,  0};


  // the quad is stored so that, when unrotated and unflipped, the world coordinates covered by the quad are
  // [wxOrigin, wzOrigin] - [wxOrigin + xSize - 1, wzOrigin + zSize - 1] inclusive.
  // The possible transformations of the quad are:
  // a) an optional left-right flip (xneg becomes xpos)
  // followed by
  // b) an optional number of clockwise rotations (0 - 3 quadrants)
  // Any transformations applied to the quad will be converted to this format

  private int wxOrigin;
  private int wzOrigin;

  private int xSize;
  private int zSize;

  private boolean flippedX;
  private byte clockwiseRotationCount;  // number of quadrants rotated clockwise
  private int transformationIndex; // combined index for flippedX followed by clockwiseRotationCount
  private int wxOriginTransformed;
  private int wzOriginTransformed;

  private float wxNudge;
  private float wzNudge;
}
