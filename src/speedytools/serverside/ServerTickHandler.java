package speedytools.serverside;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

import java.util.EnumSet;

/**
 * User: The Grey Ghost
 * Date: 2/11/13
 */
public class ServerTickHandler implements ITickHandler {

  public EnumSet<TickType> ticks()
  {
    return EnumSet.of(TickType.SERVER);
  }

  public void tickStart(EnumSet<TickType> type, Object... tickData)
  {
  }

  /**
   *
   * tick any objects which need it
   * @param type
   * @param tickData
   */
  public void tickEnd(EnumSet<TickType> type, Object... tickData)
  {
    ServerSide.tick();

  }


  public String getLabel()
  {
    return "ServerTickHandler";
  }

  private int tickCount;
}
