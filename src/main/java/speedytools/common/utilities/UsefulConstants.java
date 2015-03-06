package speedytools.common.utilities;

import net.minecraft.util.EnumFacing;

/**
 * User: The Grey Ghost
 * Date: 8/02/14
 */
public class UsefulConstants
{
  static public final int FACE_NONE = -1;
  static public final int FACE_YNEG = 0;
  static public final int FACE_YPOS = 1;
  static public final int FACE_XNEG = 4;
  static public final int FACE_XPOS = 5;
  static public final int FACE_ZNEG = 2;
  static public final int FACE_ZPOS = 3;
  static public final int FACE_ALL = 7;

  {
    if (FACE_YNEG != EnumFacing.UP.getIndex() ||
        FACE_YPOS != EnumFacing.DOWN.getIndex() ||
        FACE_XPOS != EnumFacing.EAST.getIndex() ||
        FACE_XNEG != EnumFacing.WEST.getIndex() ||
        FACE_ZNEG != EnumFacing.NORTH.getIndex() ||
        FACE_ZPOS != EnumFacing.SOUTH.getIndex() ) {
      throw new AssertionError("UsefulConstants doesn't match EnumFacing");
    }
  }

}

