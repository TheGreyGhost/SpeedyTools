package speedytools.clientside;

import net.minecraft.client.Minecraft;

/**
 * User: The Grey Ghost
 * Date: 10/03/14
 */
public class SpeedyToolsOptionsClient
{
  // the maximum duration of a "short" click.
  public static long getShortClickMaxDurationNS() {return 500 * 1000 * 1000;}

  // the length of time we have to hold down a key to perform a "long click" eg undo
  public static long getLongClickMinDurationNS() {return 2000 * 1000 * 1000;}

  // the time that the in-game error message should stay on the screen
  public static long getErrorMessageDisplayDurationNS() { return 5 * 1000 * 1000 * 1000L; }

  public static int getRenderDistanceInBlocks() {
    final int BLOCKS_PER_CHUNK = 16;
    int renderDistanceBlocks = BLOCKS_PER_CHUNK * Minecraft.getMinecraft().gameSettings.renderDistanceChunks;
    return Math.min(400, renderDistanceBlocks);
  }

  // The length of maximum length of time per tick we will use for asynchronous tasks on the client
  public static long getMaxClientBusyTimeMS() {return 25;}

}
