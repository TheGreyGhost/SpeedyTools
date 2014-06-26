package speedytools.serverside.ingametester;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemInWorldManager;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import speedytools.common.items.RegistryForItems;
import speedytools.common.network.ServerStatus;
import speedytools.common.utilities.ResultWithReason;

/**
 * Created by TheGreyGhost on 26/06/14.
 * used for in-game testing of the client user interface to simulate different server conditions
 * Test mode is selected by putting the test ItemSpeedyTester item into the leftmost inventory slot
 *
 * test condition:
 * (1) server state forced to PERFORMING_BACKUP
 * (2) server state forced to PERFORMING_YOUR_ACTION, playerBeingServiced = the client
 * (3) server state forced to UNDOING_YOUR_ACTION, playerBeingServiced = the client
 * (4) server state forced to PERFORMING_YOUR_ACTION, playerBeingServiced = null
 * (5) server state forced to PERFORMING_YOUR_ACTION, playerBeingServiced = someone else.
 *
 * For states 1 - 5, the percent complete slowly increases over 10 seconds from 0 to 100, stays there for 5 seconds,
 *   then starts from 0 in an endless cycle
 */
public class InGameStatusSimulator
{
  public InGameStatusSimulator()
  {
    Objenesis objenesis = new ObjenesisStd();
    entityPlayerMPDummy = (EntityPlayerMPDummy) objenesis.newInstance(EntityPlayerMPDummy.class);
  }

  public boolean testModeActivated(EntityPlayerMP entityPlayerMP) {
    testMode = 0;
    ItemStack firstSlotItem = entityPlayerMP.inventory.getStackInSlot(0);
    if (firstSlotItem == null) return false;
    if (firstSlotItem.itemID != RegistryForItems.itemSpeedyTester.itemID) return false;
    testMode = firstSlotItem.stackSize;
    return true;
  }

  public ServerStatus getForcedStatus(ServerStatus unforcedStatus)
  {
    switch (testMode) {
      case 1: return  ServerStatus.PERFORMING_BACKUP;
      case 2: return  ServerStatus.PERFORMING_YOUR_ACTION;
      case 3: return  ServerStatus.UNDOING_YOUR_ACTION;
      case 4: return  ServerStatus.PERFORMING_YOUR_ACTION;
      case 5: return  ServerStatus.PERFORMING_YOUR_ACTION;
    }
    return unforcedStatus;
  }

  public EntityPlayerMP getForcedPlayerBeingServiced(EntityPlayerMP unforcedEntityPlayerMP, EntityPlayerMP thePlayer)
  {
    switch (testMode) {
      case 2: return thePlayer;
      case 3: return thePlayer;
      case 4: return null;
      case 5: return entityPlayerMPDummy;
    }
    return unforcedEntityPlayerMP;
  }

  public byte getForcedPercentComplete(byte unforcedPercentComplete)
  {
    if (testMode >= 1 && testMode <= 5) {
      long nanoseconds = System.nanoTime();
      long CYCLE_TIME_NS = 15 * 1000 * 1000L * 1000;
      long HOLD_AT_MAX_PERCENT = 50;
      long percentage = (nanoseconds % CYCLE_TIME_NS) / (CYCLE_TIME_NS / (100 + HOLD_AT_MAX_PERCENT));
      if (percentage > 100) percentage = 100;
      return (byte)percentage;
    }
    return unforcedPercentComplete;
  }

  private int testMode = 0;
  private EntityPlayerMPDummy entityPlayerMPDummy;

  class EntityPlayerMPDummy extends EntityPlayerMP
  {
    public EntityPlayerMPDummy()
    {
      super(null, null, null, null);
    }

    @Override
    public String getDisplayName()
    {
      return "Dummy";
    }

  }
}
