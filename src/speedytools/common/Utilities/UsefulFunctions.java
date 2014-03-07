package speedytools.common.utilities;

/**
 * User: The Grey Ghost
 * Date: 22/02/14
 */
public class UsefulFunctions
{
  /**
   * constrain value to the range given by limit1 and limit2.  If outside the range, clip to the nearest end
   * @param value the value to be clipped
   * @param limit1 one end of the clipping range
   * @param limit2 one end of the clipping range
   * @return the clipped value
   */
  public static int clipToRange(int value, int limit1, int limit2) {
    if (limit1 < limit2) {
      return Math.min(limit2, Math.max(limit1, value));
    } else {
      return Math.max(limit2, Math.min(limit1, value));
    }
  }

  /**
   * constrain value to the range given by limit1 and limit2.  If outside the range, clip to the nearest end
   * @param value the value to be clipped
   * @param limit1 one end of the clipping range
   * @param limit2 one end of the clipping range
   * @return the clipped value
   */
  public static float clipToRange(float value, float limit1, float limit2) {
    if (limit1 < limit2) {
      return Math.min(limit2, Math.max(limit1, value));
    } else {
      return Math.max(limit2, Math.min(limit1, value));
    }
  }

  /**
   * constrain value to the range given by limit1 and limit2.  If outside the range, clip to the nearest end
   * @param value the value to be clipped
   * @param limit1 one end of the clipping range
   * @param limit2 one end of the clipping range
   * @return the clipped value
   */
  public static double clipToRange(double value, double limit1, double limit2) {
    if (limit1 < limit2) {
      return Math.min(limit2, Math.max(limit1, value));
    } else {
      return Math.max(limit2, Math.min(limit1, value));
    }
  }
}
