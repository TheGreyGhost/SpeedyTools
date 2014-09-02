//package speedytools.serverside.ingametester;
//
//import cpw.mods.fml.relauncher.Side;
//import net.minecraft.entity.player.EntityPlayerMP;
//import net.minecraft.item.ItemStack;
//import org.objenesis.Objenesis;
//import org.objenesis.ObjenesisStd;
//import speedytools.common.items.RegistryForItems;
//import speedytools.common.network.ServerStatus;
//import speedytools.common.utilities.QuadOrientation;
//import speedytools.common.utilities.ResultWithReason;
//import speedytools.serverside.network.SpeedyToolsNetworkServer;
//
///**
// * Created by TheGreyGhost on 26/06/14.
// * used for in-game testing of the client user interface to simulate different server conditions
// * Test mode is selected by putting the test ItemSpeedyTester item into the leftmost inventory slot
// *
// * test condition:
// * (1) server state forced to PERFORMING_BACKUP
// * (2) server state forced to PERFORMING_YOUR_ACTION, playerBeingServiced = the client
// * (3) server state forced to UNDOING_YOUR_ACTION, playerBeingServiced = the client
// * (4) server state forced to PERFORMING_YOUR_ACTION, playerBeingServiced = null
// * (5) server state forced to PERFORMING_YOUR_ACTION, playerBeingServiced = someone else.
// * (6) - (10) = same as (1 - 5) except server returns to IDLE after a while, and sends the status to client
// * For states 1 - 5, the status stays on the server only, client side forced to IDLE.
// * For states 1 - 5, the percent complete slowly increases over 10 seconds from 0 to 100, stays there for 5 seconds,
// *   then starts from 0 in an endless cycle
// * For states 6 - 10, the percent complete slowly increases over 10 seconds from 0 to 100, then goes to IDLE for 5 seconds.
// *   Repeats in an endless cycle.
// * (11) = backups, placements, and undo are simulated.  Will succeed unless cancelled.  Will take 10 seconds.
// * (12) = backups, placements, and undo are simulated but will fail immediately.
// * (20 - 27) = combinations of (11) and (12): +1 = backup succeed, +2 = placement succeed, +4 = undo succeed
// *
// * Test plan:  messages to test are
// * Server-side failures:
// * - selection not transmitted yet (not tested)
// * 26 - backup failed
// * 1 - world backup in progress
// * 2 - own action already in progress
// * 3 - own undo already in progress
// * 4, 5 - someone else is busy
// * Client-side failures:
// * 6 - server backup in progress
// * - selection not transmitted yet
// * 9, 10 - someone else is busy
// */
//public class InGameStatusSimulator
//{
//  public InGameStatusSimulator()
//  {
//    Objenesis objenesis = new ObjenesisStd();
//    entityPlayerMPDummy = (EntityPlayerMPDummy) objenesis.newInstance(EntityPlayerMPDummy.class);
//    testMode = 0;
//  }
//
//  public void setTestMode(EntityPlayerMP entityPlayerMP) {
//    testMode = 0;
//    if (entityPlayerMP == null || entityPlayerMP.inventory == null) return;
//    ItemStack firstSlotItem = entityPlayerMP.inventory.getStackInSlot(0);
//    if (firstSlotItem == null) return;
//    if (firstSlotItem.itemID != RegistryForItems.itemSpeedyTester.itemID) return;
//    testMode = firstSlotItem.stackSize;
//  }
//
//  public boolean isTestModeActivated() {
//    return testMode != 0;
//  }
//
//  public ServerStatus getForcedStatus(ServerStatus unforcedStatus, Side forWhichSide)
//  {
//    int status = testMode;
//    if (testMode >= 6 && testMode <= 10) {
//      status = testMode - 5;
//      forWhichSide = Side.SERVER;
//      long nanoseconds = System.nanoTime();
//      long CYCLE_TIME_NS = 15 * 1000 * 1000L * 1000;
//      long IDLE_TIME_NS = 5 * 1000 * 1000L * 1000;
//      long cyclePosition = nanoseconds % CYCLE_TIME_NS;
//      if (cyclePosition <= IDLE_TIME_NS) return ServerStatus.IDLE;
//    }
//
//    switch (status) {
//      case 1: return forWhichSide == Side.SERVER  ? ServerStatus.PERFORMING_BACKUP : ServerStatus.IDLE;
//      case 2: return forWhichSide == Side.SERVER  ? ServerStatus.PERFORMING_YOUR_ACTION : ServerStatus.IDLE;
//      case 3: return forWhichSide == Side.SERVER  ? ServerStatus.UNDOING_YOUR_ACTION : ServerStatus.IDLE;
//      case 4: return forWhichSide == Side.SERVER  ? ServerStatus.PERFORMING_YOUR_ACTION : ServerStatus.IDLE;
//      case 5: return forWhichSide == Side.SERVER  ? ServerStatus.PERFORMING_YOUR_ACTION : ServerStatus.IDLE;
//    }
//    return unforcedStatus;
//  }
//
//  public EntityPlayerMP getForcedPlayerBeingServiced(EntityPlayerMP unforcedEntityPlayerMP, EntityPlayerMP thePlayer, Side forWhichSide)
//  {
//    int status = testMode;
//    if (testMode >= 6 && testMode <= 10) {
//      status = testMode - 5;
//      long nanoseconds = System.nanoTime();
//      long CYCLE_TIME_NS = 15 * 1000 * 1000L * 1000;
//      long IDLE_TIME_NS = 5 * 1000 * 1000L * 1000;
//      long cyclePosition = nanoseconds % CYCLE_TIME_NS;
//      if (cyclePosition <= IDLE_TIME_NS) return unforcedEntityPlayerMP;
//    }
//
//    switch (status) {
//      case 2: return thePlayer;
//      case 3: return thePlayer;
//      case 4: return null;
//      case 5: return entityPlayerMPDummy;
//    }
//    return unforcedEntityPlayerMP;
//  }
//
//  public byte getForcedPercentComplete(byte unforcedPercentComplete, Side forWhichSide)
//  {
//    if (testMode >= 6 && testMode <= 10) {
//      long nanoseconds = System.nanoTime();
//      long CYCLE_TIME_NS = 15 * 1000 * 1000L * 1000;
//      long IDLE_TIME_NS = 5 * 1000 * 1000L * 1000;
//      long cyclePosition = nanoseconds % CYCLE_TIME_NS;
//      if (cyclePosition <= IDLE_TIME_NS) return 0;
//      long percentage = (cyclePosition - IDLE_TIME_NS) / ( CYCLE_TIME_NS / 100);
//      if (percentage > 100) percentage = 100;
//      return (byte)percentage;
//    }
//
//    if (testMode >= 1 && testMode <= 5) {
//      long nanoseconds = System.nanoTime();
//      long CYCLE_TIME_NS = 15 * 1000 * 1000L * 1000;
//      long HOLD_AT_MAX_PERCENT = 50;
//      long percentage = (nanoseconds % CYCLE_TIME_NS) / (CYCLE_TIME_NS / (100 + HOLD_AT_MAX_PERCENT));
//      if (percentage > 100) percentage = 100;
//      return (byte)percentage;
//    }
//    return unforcedPercentComplete;
//  }
//
//  /** simulate this method
//   * @return null if not simulated (-> progress to real code)
//   */
//  public ResultWithReason prepareForToolAction(SpeedyToolsNetworkServer speedyToolsNetworkServer, EntityPlayerMP player)
//  {
//    final int TEST_BACKUP_FLAG = 1;
//    if (testMode >= 20 && testMode <= 27) {
//      if (((testMode - 20) & TEST_BACKUP_FLAG) == 0) {
//        return ResultWithReason.failure("Simulated Backup Failure");
//      }
//    } else {
//      if (testMode == 12) {
//        return ResultWithReason.failure("Simulated Backup Failure");
//      }
//      if (testMode != 11) return null;
//    }
//    testInProgress = TestInProgress.PREPARE_FOR_TOOL_ACTION;
//    testStartTime = System.nanoTime();
//    testPlayer = player;
//    speedyToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_BACKUP, player, (byte)0);
//    return ResultWithReason.success();
//  }
//
//  /** simulate this method
//   * @return null if not simulated (-> progress to real code)
//   */
//  public ResultWithReason performUndoOfCurrentAction(SpeedyToolsNetworkServer speedyToolsNetworkServer, EntityPlayerMP player, int undoSequenceNumber, int actionSequenceNumber)
//  {
//    final int TEST_UNDO_FLAG = 4;
//    if (testMode >= 20 && testMode <= 27) {
//      if (((testMode - 20) & TEST_UNDO_FLAG) == 0) {
//        return ResultWithReason.failure("Simulated Undo Current Failure");
//      }
//    } else {
//      if (testMode == 12) {
//        return ResultWithReason.failure("Simulated Undo Current Failure");
//      }
//      if (testMode != 11) return null;
//    }
//    testInProgress = TestInProgress.PERFORM_UNDO_OF_CURRENT_ACTION;
//    testStartTime = System.nanoTime();
//    testPlayer = player;
//    testUndoSequenceNumber = undoSequenceNumber;
//    speedyToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, player, (byte)0);
//    speedyToolsNetworkServer.actionCompleted(player, actionSequenceNumber);
//    return ResultWithReason.success();
//  }
//
//  /** simulate this method
//   * @return null if not simulated (-> progress to real code)
//   */
//  public ResultWithReason performUndoOfLastAction(SpeedyToolsNetworkServer speedyToolsNetworkServer, EntityPlayerMP player, int undoSequenceNumber)
//  {
//    final int TEST_UNDO_FLAG = 4;
//    if (testMode >= 20 && testMode <= 27) {
//      if (((testMode - 20) & TEST_UNDO_FLAG) == 0) {
//        return ResultWithReason.failure("Simulated Undo Last Failure");
//      }
//    } else {
//      if (testMode == 12) {
//        return ResultWithReason.failure("Simulated Undo Last Failure");
//      }
//      if (testMode != 11) return null;
//    }
//    testInProgress = TestInProgress.PERFORM_UNDO_OF_LAST_ACTION;
//    testStartTime = System.nanoTime();
//    testPlayer = player;
//    testUndoSequenceNumber = undoSequenceNumber;
//    speedyToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, player, (byte)0);
//    return ResultWithReason.success();
//  }
//
//  /** simulate this method
//   * @return null if not simulated (-> progress to real code)
//   */
//  public ResultWithReason performToolAction(SpeedyToolsNetworkServer speedyToolsNetworkServer, EntityPlayerMP player, int sequenceNumber, int toolID, int xpos, int ypos, int zpos, QuadOrientation quadOrientation)
//  {
//    final int TEST_ACTION_FLAG = 2;
//    if (testMode >= 20 && testMode <= 27) {
//      if (((testMode - 20) & TEST_ACTION_FLAG) == 0) {
//        return ResultWithReason.failure("Simulated Action Failure");
//      }
//    } else {
//      if (testMode == 12) {
//        return ResultWithReason.failure("Simulated Action Failure");
//      }
//      if (testMode != 11) return null;
//    }
//    testInProgress = TestInProgress.PERFORM_TOOL_ACTION;
//    testStartTime = System.nanoTime();
//    testPlayer = player;
//    testActionSequenceNumber = sequenceNumber;
//    speedyToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_YOUR_ACTION, player, (byte)0);
//    return ResultWithReason.success();
//  }
//
//  /**
//   * Update the server status according to current test (use proper code, don't force)
//   * @param speedyToolsNetworkServer
//   */
//  public void updateServerStatus(SpeedyToolsNetworkServer speedyToolsNetworkServer)
//  {
//    final long NANOSECONDS_PER_SECOND = 1000 * 1000 * 1000L;
//    if (testInProgress == TestInProgress.NONE) return;
//
//    final int TICKS_PER_STATUS_UPDATE = 20;
//    if (++tickCount % TICKS_PER_STATUS_UPDATE != 0) return;
//
//    long elapsedTime = System.nanoTime() - testStartTime;
//
//    switch (testInProgress) {
//      case PREPARE_FOR_TOOL_ACTION: {
//        final long TEST_DURATION_SECONDS = 10;
//        if (elapsedTime > TEST_DURATION_SECONDS * NANOSECONDS_PER_SECOND) {
//          speedyToolsNetworkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
//          testInProgress = TestInProgress.NONE;
//          System.out.println("Server: backup completed");
//        } else {
//          long completion = 100 * elapsedTime / (TEST_DURATION_SECONDS * NANOSECONDS_PER_SECOND);
//          speedyToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_BACKUP, testPlayer, (byte)completion);
//        }
//        break;
//      }
//      case PERFORM_TOOL_ACTION: {
//        final long TEST_DURATION_SECONDS = 10;
//        if (elapsedTime > TEST_DURATION_SECONDS * NANOSECONDS_PER_SECOND) {
//          speedyToolsNetworkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
//          speedyToolsNetworkServer.actionCompleted(testPlayer, testActionSequenceNumber);
//          System.out.println("Server: actionCompleted # " + testActionSequenceNumber);
//          testInProgress = TestInProgress.NONE;
//        } else {
//          long completion = 100 * elapsedTime / (TEST_DURATION_SECONDS * NANOSECONDS_PER_SECOND);
//          speedyToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_YOUR_ACTION, testPlayer, (byte)completion);
//        }
//        break;
//      }
//      case PERFORM_UNDO_OF_LAST_ACTION:
//      case PERFORM_UNDO_OF_CURRENT_ACTION: {
//        final long TEST_DURATION_SECONDS = 10;
//        if (elapsedTime > TEST_DURATION_SECONDS * NANOSECONDS_PER_SECOND) {
//          speedyToolsNetworkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
//          speedyToolsNetworkServer.undoCompleted(testPlayer, testUndoSequenceNumber);
//          System.out.println("Server: undoCompleted # " + testUndoSequenceNumber);
//          testInProgress = TestInProgress.NONE;
//        } else {
//          long completion = 100 * elapsedTime / (TEST_DURATION_SECONDS * NANOSECONDS_PER_SECOND);
//          speedyToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, testPlayer, (byte)completion);
//        }
//        break;
//      }
//    }
//  }
//
//  private int testMode = 0;      // 0 = none
//  private EntityPlayerMPDummy entityPlayerMPDummy;
//
//  enum TestInProgress {NONE, PREPARE_FOR_TOOL_ACTION, PERFORM_TOOL_ACTION, PERFORM_UNDO_OF_CURRENT_ACTION, PERFORM_UNDO_OF_LAST_ACTION};
//  TestInProgress testInProgress = TestInProgress.NONE;
//  long testStartTime = 0;
//  int tickCount = 0;
//  EntityPlayerMP testPlayer = null;
//  int testActionSequenceNumber;
//  int testUndoSequenceNumber;
//
//  class EntityPlayerMPDummy extends EntityPlayerMP
//  {
//    public EntityPlayerMPDummy()
//    {
//      super(null, null, null, null);
//    }
//
//    @Override
//    public String getDisplayName()
//    {
//      return "Dummy";
//    }
//
//  }
//}
