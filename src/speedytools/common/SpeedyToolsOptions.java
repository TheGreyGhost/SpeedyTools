package speedytools.common;

/**
 * User: The Grey Ghost
 * Date: 10/03/14
 */
public class SpeedyToolsOptions
{
  // speed for double-clicking with the mouse
  public static int getDoubleClickSpeedMS() {
    return 200;
  }

  // the maximum duration of a "short" click.
  public static long getShortClickMaxDurationNS() {return 200 * 1000 * 1000;}

  // the length of time we have to hold down a key to perform a "long click" eg undo
  public static long getLongClickMinDurationNS() {return 2000 * 1000 * 1000;}
}
