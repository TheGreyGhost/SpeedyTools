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
import speedytools.common.utilities.UsefulFunctions;
import speedytools.serverside.CloneToolsNetworkServer;

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
 * (6) - (10) = same as (1 - 5) except server returns to IDLE after a while
 * For states 1 - 5, the percent complete slowly increases over 10 seconds from 0 to 100, stays there for 5 seconds,
 *   then starts from 0 in an endless cycle
 * For states 6 - 10, the percent complete slowly increases over 10 seconds from 0 to 100, then goes to IDLE for 5 seconds.
 *   Repeats in an endless cycle.
 * (11) = backups, placements, and undo are simulated.  Will succeed unless cancelled.  Will take 10 seconds.
 * (12) = backups, placements, and undo are simulated but will fail immediately.
  */
public class InGameStatusSimulator
{
  public InGameStatusSimulator()
  {
    Objenesis objenesis = new ObjenesisStd();
    entityPlayerMPDummy = (EntityPlayerMPDummy) objenesis.newInstance(EntityPlayerMPDummy.class);
  }

  public void setTestMode(EntityPlayerMP entityPlayerMP) {
    testMode = 0;
    ItemStack firstSlotItem = entityPlayerMP.inventory.getStackInSlot(0);
    if (firstSlotItem == null) return;
    if (firstSlotItem.itemID != RegistryForItems.itemSpeedyTester.itemID) return;
    testMode = firstSlotItem.stackSize;
  }

  public boolean isTestModeActivated() {
    return testMode != 0;
  }

  public ServerStatus getForcedStatus(ServerStatus unforcedStatus)
  {
    int status = testMode;
    if (testMode >= 6 && testMode <= 10) {
      status = testMode - 5;
      long nanoseconds = System.nanoTime();
      long CYCLE_TIME_NS = 15 * 1000 * 1000L * 1000;
      long IDLE_TIME_NS = 5 * 1000 * 1000L * 1000;
      long cyclePosition = nanoseconds % CYCLE_TIME_NS;
      if (cyclePosition <= IDLE_TIME_NS) return ServerStatus.IDLE;
    }

    switch (status) {
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
    int status = testMode;
    if (testMode >= 6 && testMode <= 10) {
      status = testMode - 5;
      long nanoseconds = System.nanoTime();
      long CYCLE_TIME_NS = 15 * 1000 * 1000L * 1000;
      long IDLE_TIME_NS = 5 * 1000 * 1000L * 1000;
      long cyclePosition = nanoseconds % CYCLE_TIME_NS;
      if (cyclePosition <= IDLE_TIME_NS) return unforcedEntityPlayerMP;
    }

    switch (status) {
      case 2: return thePlayer;
      case 3: return thePlayer;
      case 4: return null;
      case 5: return entityPlayerMPDummy;
    }
    return unforcedEntityPlayerMP;
  }

  public byte getForcedPercentComplete(byte unforcedPercentComplete)
  {
    if (testMode >= 6 && testMode <= 10) {
      long nanoseconds = System.nanoTime();
      long CYCLE_TIME_NS = 15 * 1000 * 1000L * 1000;
      long IDLE_TIME_NS = 5 * 1000 * 1000L * 1000;
      long cyclePosition = nanoseconds % CYCLE_TIME_NS;
      if (cyclePosition <= IDLE_TIME_NS) return 0;
      long percentage = (cyclePosition - IDLE_TIME_NS) / ( CYCLE_TIME_NS / 100);
      if (percentage > 100) percentage = 100;
      return (byte)percentage;
    }

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

  /** simulate this method
   * @return null if not simulated (-> progress to real code)
   */
  public ResultWithReason prepareForToolAction(CloneToolsNetworkServer cloneToolsNetworkServer, EntityPlayerMP player)
  {
    if (testMode == 12) {
      return ResultWithReason.failure("Simulated Backup Failure");
    }
    if (testMode != 11) return null;

    testInProgress = TestInProgress.PREPARE_FOR_TOOL_ACTION;
    testStartTime = System.nanoTime();
    testPlayer = player;
    cloneToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_BACKUP, player, (byte)0);
    return ResultWithReason.success();
  }

  /** simulate this method
   * @return null if not simulated (-> progress to real code)
   */
  public ResultWithReason performUndoOfCurrentAction(CloneToolsNetworkServer cloneToolsNetworkServer, EntityPlayerMP player, int undoSequenceNumber, int actionSequenceNumber)
  {
    if (testMode == 12) {
      return ResultWithReason.failure("Simulated Undo Current Failure");
    }
    if (testMode != 11) return null;

    testInProgress = TestInProgress.PERFORM_UNDO_OF_CURRENT_ACTION;
    testStartTime = System.nanoTime();
    testPlayer = player;
    testUndoSequenceNumber = undoSequenceNumber;
    cloneToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, player, (byte)0);
    cloneToolsNetworkServer.actionCompleted(player, actionSequenceNumber);
    return ResultWithReason.success();
  }

  /** simulate this method
   * @return null if not simulated (-> progress to real code)
   */
  public ResultWithReason performUndoOfLastAction(CloneToolsNetworkServer cloneToolsNetworkServer, EntityPlayerMP player, int undoSequenceNumber)
  {
    if (testMode == 12) {
      return ResultWithReason.failure("Simulated Undo Last Failure");
    }
    if (testMode != 11) return null;

    testInProgress = TestInProgress.PERFORM_UNDO_OF_LAST_ACTION;
    testStartTime = System.nanoTime();
    testPlayer = player;
    testUndoSequenceNumber = undoSequenceNumber;
    cloneToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, player, (byte)0);
    return ResultWithReason.success();
  }

  /** simulate this method
   * @return null if not simulated (-> progress to real code)
   */
  public ResultWithReason performToolAction(CloneToolsNetworkServer cloneToolsNetworkServer, EntityPlayerMP player, int sequenceNumber, int toolID, int xpos, int ypos, int zpos, byte rotationCount, boolean flipped)
  {
    if (testMode == 12) {
      return ResultWithReason.failure("Simulated Action Failure");
    }
    if (testMode != 11) return null;

    testInProgress = TestInProgress.PERFORM_TOOL_ACTION;
    testStartTime = System.nanoTime();
    testPlayer = player;
    testActionSequenceNumber = sequenceNumber;
    cloneToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_YOUR_ACTION, player, (byte)0);
    return ResultWithReason.success();
  }

  /**
   * Update the server status according to current test (use proper code, don't force)
   * @param cloneToolsNetworkServer
   */
  public void updateServerStatus(CloneToolsNetworkServer cloneToolsNetworkServer)
  {
    final long NANOSECONDS_PER_SECOND = 1000 * 1000 * 1000L;
    if (testInProgress == TestInProgress.NONE) return;

    final int TICKS_PER_STATUS_UPDATE = 20;
    if (++tickCount % TICKS_PER_STATUS_UPDATE != 0) return;

    long elapsedTime = System.nanoTime() - testStartTime;

    switch (testInProgress) {
      case PREPARE_FOR_TOOL_ACTION: {
        final long TEST_DURATION_SECONDS = 10;
        if (elapsedTime > TEST_DURATION_SECONDS * NANOSECONDS_PER_SECOND) {
          cloneToolsNetworkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
          testInProgress = TestInProgress.NONE;
          System.out.println("Server: backup completed");
        } else {
          long completion = 100 * elapsedTime / (TEST_DURATION_SECONDS * NANOSECONDS_PER_SECOND);
          cloneToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_BACKUP, testPlayer, (byte)completion);
        }
        break;
      }
      case PERFORM_TOOL_ACTION: {
        final long TEST_DURATION_SECONDS = 10;
        if (elapsedTime > TEST_DURATION_SECONDS * NANOSECONDS_PER_SECOND) {
          cloneToolsNetworkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
          cloneToolsNetworkServer.actionCompleted(testPlayer, testActionSequenceNumber);
          System.out.println("Server: actionCompleted # " + testActionSequenceNumber);
          testInProgress = TestInProgress.NONE;
        } else {
          long completion = 100 * elapsedTime / (TEST_DURATION_SECONDS * NANOSECONDS_PER_SECOND);
          cloneToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_YOUR_ACTION, testPlayer, (byte)completion);
        }
        break;
      }
      case PERFORM_UNDO_OF_LAST_ACTION:
      case PERFORM_UNDO_OF_CURRENT_ACTION: {
        final long TEST_DURATION_SECONDS = 10;
        if (elapsedTime > TEST_DURATION_SECONDS * NANOSECONDS_PER_SECOND) {
          cloneToolsNetworkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
          cloneToolsNetworkServer.undoCompleted(testPlayer, testUndoSequenceNumber);
          System.out.println("Server: undoCompleted # " + testUndoSequenceNumber);
          testInProgress = TestInProgress.NONE;
        } else {
          long completion = 100 * elapsedTime / (TEST_DURATION_SECONDS * NANOSECONDS_PER_SECOND);
          cloneToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, testPlayer, (byte)completion);
        }
        break;
      }
    }
  }

  private int testMode = 0;      // 0 = none
  private EntityPlayerMPDummy entityPlayerMPDummy;

  enum TestInProgress {NONE, PREPARE_FOR_TOOL_ACTION, PERFORM_TOOL_ACTION, PERFORM_UNDO_OF_CURRENT_ACTION, PERFORM_UNDO_OF_LAST_ACTION};
  TestInProgress testInProgress = TestInProgress.NONE;
  long testStartTime = 0;
  int tickCount = 0;
  EntityPlayerMP testPlayer = null;
  int testActionSequenceNumber;
  int testUndoSequenceNumber;

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
