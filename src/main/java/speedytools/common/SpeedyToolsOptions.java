package speedytools.common;

import java.io.File;

/**
 * User: The Grey Ghost
 * Date: 10/03/14
 */
public class SpeedyToolsOptions
{
  private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("speedytoolsmod.debug", "false"));

  // speed for double-clicking with the mouse
  public static int getDoubleClickSpeedMS() {
    return 200;
  }

  // the maximum number of undo for the simple speedy tools
  public static int getMaxSimpleToolUndoCount() {return 5;}

  // the maximum number of undo for the complex speedy tools
  public static int getMaxComplexToolUndoCount() {return 5;}

  // if true - enabled the in-game testing tools
  public static boolean getTesterToolsEnabled() { return DEBUG;}

  // The length of maximum length of time per tick we will use for asynchronous tasks on the server
  public static long getMaxServerBusyTimeMS() {return 25;}

  // The length of maximum length of time per tick we will use for selection generation on the server
  public static long getMaxServerSelGenTimeMS() {return 25;}

  // The packet size of the fragments to use when sending a Selection to/from the server
  public static int getSelectionPacketFragmentSize() {return 3000;}

  // if true - logging of the network activity is active
  public static boolean getNetworkLoggingActive() { return false;}  // doesn't work any more
  public static File getNetworkLoggingDirectory()
  {
    return null;
//    if (Minecraft.getMinecraft() == null) return null;
//    return Minecraft.getMinecraft().mcDataDir;
  }
  public static int getNetworkLoggingPeriodInTicks() {return 20 * 10;}

}
