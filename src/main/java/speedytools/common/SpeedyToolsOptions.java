package speedytools.common;

import net.minecraft.client.Minecraft;

import java.io.File;

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

  // the maximum number of undo for the simple speedy tools
  public static int getMaxSimpleToolUndoCount() {return 5;}

  // the maximum number of undo for the complex speedy tools
  public static int getMaxComplexToolUndoCount() {return 5;}

  // if true - enabled the in-game testing tools
  public static boolean getTesterToolsEnabled() { return true;}

  // if true - logging of the network activity is active
  public static boolean getNetworkLoggingActive() { return true;}
  public static File getNetworkLoggingDirectory()
  {
    if (Minecraft.getMinecraft() == null) return null;
    return Minecraft.getMinecraft().mcDataDir;
  }
  public static int getNetworkLoggingPeriodInTicks() {return 20 * 10;}

  // the time that the in-game error message should stay on the screen
  public static long getErrorMessageDisplayDurationNS() { return 5 * 1000 * 1000 * 1000L; }

  public static int getRenderDistanceInBlocks() {
    int renderDistanceSetting = Minecraft.getMinecraft().gameSettings.renderDistanceChunks;
    int renderDistanceBlocks = 64 << 3 - renderDistanceSetting;
    return Math.min(400, renderDistanceBlocks);
  }

  // The packet size of the fragments to use when sending a Selection to the server
  public static int getSelectionPacketFragmentSize() {return 3000;}

  // The length of maximum length of time per tick we will use for asynchronous tasks on the client
  public static long getMaxClientBusyTimeMS() {return 25;}

  // The length of maximum length of time per tick we will use for asynchronous tasks on the server
  public static long getMaxServerBusyTimeMS() {return 25;}

}
