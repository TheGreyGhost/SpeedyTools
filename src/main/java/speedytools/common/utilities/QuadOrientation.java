package speedytools.common.utilities;

import io.netty.buffer.ByteBuf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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

  public QuadOrientation(ByteBuf byteBuf)
  {
    transformationIndex = byteBuf.readInt();
    wxOrigin = byteBuf.readInt();
    wzOrigin = byteBuf.readInt();
    xSize = byteBuf.readInt();
    zSize = byteBuf.readInt();
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
  public QuadOrientation flipWX()
  {
    flipX();
//    System.out.println("flipWX:clockwiseRotationCount" + clockwiseRotationCount);
//    if ((clockwiseRotationCount & 1) == 0) {
//      System.out.println("flipX");
//      flipX();
//    } else {
//      System.out.println("flipZ");
//      flipZ();
//    }
    return this;
  }

  /**
   * Flip the quad front-back in the world coordinates  i.e wzneg becomes wzpos
   */
  public QuadOrientation flipWZ()
  {
    flipZ();
//    System.out.println("flipWZ:clockwiseRotationCount" + clockwiseRotationCount);
//    if ((clockwiseRotationCount & 1) == 0) {
//      System.out.println("flipZ");
//      flipZ();
//    } else {
//      System.out.println("flipX");
//      flipX();
//    }
    return this;
  }

  private final int [] FLIP_X_NEW_INDEX = {4, 7, 6, 5, 0, 3, 2, 1};
  /** flip the quad left-right i.e xneg becomes xpos
   */
  public QuadOrientation flipX()
  {
    transformationIndex = FLIP_X_NEW_INDEX[transformationIndex];
    flippedX = (transformationIndex & 4) != 0;
    clockwiseRotationCount = (byte)(transformationIndex & 3);
    calculateOriginTransformed();
    return this;
  }

  private final int [] FLIP_Z_NEW_INDEX = {6, 5, 4, 7, 2, 1, 0, 3};
  /** flip the quad front-back i.e zneg becomes zpos
   */
  public QuadOrientation flipZ()
  {
    transformationIndex = FLIP_Z_NEW_INDEX[transformationIndex];
    flippedX = (transformationIndex & 4) != 0;
    clockwiseRotationCount = (byte)(transformationIndex & 3);
    calculateOriginTransformed();
    return this;
  }

  /**
   * rotate the quad by 0, 90, 180, or 270 degrees
   * @param rotationCount the number of quadrants to rotate clockwise
   */
  public QuadOrientation rotateClockwise(int rotationCount)
  {
    clockwiseRotationCount = (byte)((clockwiseRotationCount + rotationCount) & 3);
    transformationIndex = (flippedX ? 4 : 0) | (clockwiseRotationCount & 3);
    calculateOriginTransformed();
    return this;
  }

  public byte getClockwiseRotationCount() {
    return clockwiseRotationCount;
  }

  public boolean isFlippedX() {
    return flippedX;
  }

  public void writeToStream(ByteBuf byteBuf)
  {
    byteBuf.writeInt(transformationIndex);
    byteBuf.writeInt(wxOrigin);
    byteBuf.writeInt(wzOrigin);
    byteBuf.writeInt(xSize);
    byteBuf.writeInt(zSize);
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

  /**
   * returns the new WXZ ranges ([min, max] inclusive) after the transformation
   * @param wxRange takes the current [xMin, xMax] inclusive and returns the new [wxMin, wxMax] inclusive
   * @param wzRange takes the current [zMin, zMax] inclusive and returns the new [wzMin, wzMax] inclusive
   */
  public void getWXZranges(Pair<Integer, Integer> wxRange, Pair<Integer, Integer> wzRange)
  {
    int wx1 = calcWXfromXZ(wxRange.getFirst(), wzRange.getFirst());
    int wz1 = calcWZfromXZ(wxRange.getFirst(), wzRange.getFirst());
    int wx2 = calcWXfromXZ(wxRange.getSecond(), wzRange.getSecond());
    int wz2 = calcWZfromXZ(wxRange.getSecond(), wzRange.getSecond());
    wxRange.setFirst(Math.min(wx1, wx2));
    wxRange.setSecond(Math.max(wx1, wx2));
    wzRange.setFirst(Math.min(wz1, wz2));
    wzRange.setSecond(Math.max(wz1, wz2));
  }

  /**
   * Make a copy of the original that has the same origin, rotation and flip but different size
   * @param newXsize the new x size
   * @param newZsize the new z size
   * @return a new QuadOrientation (this is unchanged.)
   */
  public QuadOrientation getResizedCopy(int newXsize, int newZsize)
  {
//    Pair<Integer, Integer> wxRange = new Pair<Integer, Integer>(0,0);
//    Pair<Integer, Integer> wzRange = new Pair<Integer, Integer>(0,0);
//    getWXZranges(wxRange, wzRange);
    return new QuadOrientation(wxOrigin, wzOrigin, newXsize, newZsize, flippedX, clockwiseRotationCount);
  }

  /**
   * This method calculates the placement position of the newQuadOrientation that will cause it to overlay correctly onto
   *   the oldQuadOrientation (this).
   * For example for an unflipped, unrotated quad:
   * the old QuadOrientation has size 5,7.  It maps to source world coordinates [30,40] to [34, 46]
   * the new QuadOrientation has size 10,14.  It maps to source world coordinates [26, 38] to [35, 51].  It is composed of
   *   old QuadOrientation plus a border.
   * What placement position for newQuadOrientation would cause the "internal" oldQuadOrientation to be placed in the
   *   same location as the oldQuadOrientation by itself.
   * @param newQuadOrientation the orientation of the new quad.  must match this for flipped and rotation!
   * @param xRelativeOrigin the origin of the old relative to the new; for the example above, 30 - 26 = +4
   * @param zRelativeOrigin the origin of the old relative to the new: for the example above, 40 - 38 = +2
   * @return the relative placement position <dx, dz> of the newQuadOrientation compared to the old (for example above:
   *          <-4, -5> )
   */
  public Pair<Integer, Integer> calculateDisplacementForExpandedSelection(QuadOrientation newQuadOrientation, int xRelativeOrigin, int zRelativeOrigin)
  {
    assert (flippedX == newQuadOrientation.flippedX);
    assert (clockwiseRotationCount == newQuadOrientation.clockwiseRotationCount);

//    Pair<Integer, Integer> oldWXRange = new Pair<Integer, Integer>(0, xSize-1);
//    Pair<Integer, Integer> oldWZRange = new Pair<Integer, Integer>(0, zSize-1);
//    getWXZranges(oldWXRange, oldWZRange);
//    System.out.println("oldWXZmin = [" + oldWXRange.getFirst() + ", " + oldWZRange.getFirst() + "]");
//
//    // find the world x and z range of the new using the old reference frame
////    Pair<Integer, Integer> newWXRange = new Pair<Integer, Integer>(-xRelativeOrigin, -xRelativeOrigin + newQuadOrientation.xSize - 1);
////    Pair<Integer, Integer> newWZRange = new Pair<Integer, Integer>(-zRelativeOrigin, -zRelativeOrigin + newQuadOrientation.zSize - 1);
//    Pair<Integer, Integer> newWXRange = new Pair<Integer, Integer>(xRelativeOrigin, xRelativeOrigin + xSize - 1);
//    Pair<Integer, Integer> newWZRange = new Pair<Integer, Integer>(zRelativeOrigin, zRelativeOrigin + zSize - 1);
//    System.out.println("xRange:" + newWXRange);
//    System.out.println("zRange:" + newWZRange);
//    newQuadOrientation.getWXZranges(newWXRange, newWZRange);
//    System.out.println("new WXZmin = [" + newWXRange.getFirst() + ", " + newWZRange.getFirst() + "]");
//
//    return new Pair<Integer, Integer>(newWXRange.getFirst() - oldWXRange.getFirst(),
//                                      newWZRange.getFirst() - oldWZRange.getFirst());

    int oldWX0 = calcWXfromXZ(0,0);
    int oldWZ0 = calcWZfromXZ(0,0);
    int newWX0 = newQuadOrientation.calcWXfromXZ(xRelativeOrigin, zRelativeOrigin);
    int newWZ0 = newQuadOrientation.calcWZfromXZ(xRelativeOrigin, zRelativeOrigin);
//    System.out.println("old WXZ0 = [" + oldWX0 + ", " + oldWZ0 + "]");
//    System.out.println("new WXZ0 = [" + newWX0 + ", " + newWZ0 + "]");
    int dWXa = newWX0 - oldWX0;
    int dWZa = newWZ0 - oldWZ0;

//    // we also need to make a correction for the change in minWX, minWZ when the quad is rotated by 90 or 270
//
//    Pair<Integer, Integer> oldWXrange = new Pair<Integer, Integer>(0, xSize - 1);
//    Pair<Integer, Integer> oldWZrange = new Pair<Integer, Integer>(0, zSize - 1);
//    getWXZranges(oldWXrange, oldWZrange);
//    System.out.println("old WXrange = " + oldWXrange);
//    System.out.println("old WZrange = " + oldWZrange);
//
//    Pair<Integer, Integer> newWXrange = new Pair<Integer, Integer>(0, newQuadOrientation.xSize - 1);
//    Pair<Integer, Integer> newWZrange = new Pair<Integer, Integer>(0, newQuadOrientation.zSize - 1);
//    newQuadOrientation.getWXZranges(newWXrange, newWZrange);
//    System.out.println("new WXrange = " + newWXrange);
//    System.out.println("new WZrange = " + newWZrange);
//
//    int dWXb = newWXrange.getFirst() - oldWXrange.getFirst();
//    int dWZb = newWZrange.getFirst() - oldWZrange.getFirst();
//
//    System.out.println("dx = dWXa + dWXb = " + dWXa + " + " + dWXb + " = " + (dWXa + dWXb));
//    System.out.println("dz = dWZa + dWZb = " + dWZa + " + " + dWZb + " = " + (dWZa + dWZb));
    return new Pair<Integer, Integer>(dWXa , dWZa);
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
