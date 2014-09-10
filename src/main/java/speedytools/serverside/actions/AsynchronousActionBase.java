package speedytools.serverside.actions;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import speedytools.common.selections.VoxelSelectionWithOrigin;
import speedytools.serverside.worldmanipulation.AsynchronousToken;
import speedytools.serverside.worldmanipulation.UniqueTokenID;
import speedytools.serverside.worldmanipulation.WorldHistory;

/**
* User: The Grey Ghost
* Date: 3/08/2014
*/
public abstract class AsynchronousActionBase implements AsynchronousToken
{
  public AsynchronousActionBase(WorldServer i_worldServer, EntityPlayerMP i_player, WorldHistory i_worldHistory, int i_sequenceNumber)
  {
    worldServer = i_worldServer;
    entityPlayerMP = i_player;
    worldHistory = i_worldHistory;
    sequenceNumber = i_sequenceNumber;
    interruptTimeNS = INFINITE_TIMEOUT;
    fractionComplete = 0;
    cumulativeTaskDurationWeight = 0.0;
    completed = false;
  }

  @Override
  public boolean isTaskComplete() {
    return completed;
  }

  @Override
  public boolean isTaskAborted() { return aborting && isTaskComplete();}

  @Override
  public boolean isTimeToInterrupt() {
    return (interruptTimeNS == IMMEDIATE_TIMEOUT || (interruptTimeNS != INFINITE_TIMEOUT && System.nanoTime() >= interruptTimeNS));
  }

  @Override
  public void setTimeOfInterrupt(long timeToStopNS) {
    interruptTimeNS = timeToStopNS;
  }

  @Override
  public void abortProcessing() {
    aborting = true;
  }

  /** attempt to rollback the changes being made by the current action.
   *
   * @param i_rollbackSequenceNumber
   */
  public void rollback(int i_rollbackSequenceNumber)
  {
    startRollback = true;
    rollingBack = true;
    rollbackSequenceNumber = i_rollbackSequenceNumber;
  }

  @Override
  public double getFractionComplete()
  {
    return fractionComplete;
  }

  public VoxelSelectionWithOrigin getLockedRegion() {return null;}

  @Override
  public UniqueTokenID getUniqueTokenID() {
    return transactionID;
  }

  /**
   * Set the task to be executed next
   * @param token the next task
   * @param taskDurationWeight the estimated relative duration of this task as a fraction of the whole task duration; 0.0 - 1.0;
   * @param resetCumulativeDuration
   */
  public void setSubTask(AsynchronousToken token, double taskDurationWeight, boolean resetCumulativeDuration)
  {
    if (resetCumulativeDuration) {
      cumulativeTaskDurationWeight = 0;
    } else {
      cumulativeTaskDurationWeight += subTaskDurationWeight;
    }
    subTaskDurationWeight = taskDurationWeight;
    subTask = token;
  }

  /** execute the current subtask
   * @return true if subTask is completed
   */
  public boolean executeSubTask()
  {
    if (subTask.isTaskComplete()) return true;
    subTask.setTimeOfInterrupt(interruptTimeNS);
    subTask.continueProcessing();
    double stageCompletion = subTask.isTaskComplete() ? 1.0 : subTask.getFractionComplete();
    fractionComplete = cumulativeTaskDurationWeight + subTaskDurationWeight * stageCompletion;
//    System.out.println("AsynchronousActionBase.executeSubTask stageCompletion " + stageCompletion + " : " + fractionComplete);
    return subTask.isTaskComplete();
  }

  /** abort the current subTask (keep executing until acknowledged)
   * @return true if subTask is completed
   */
  public boolean executeAbortSubTask()
  {
    if (subTask.isTaskComplete()) return true;
    subTask.abortProcessing();
    subTask.setTimeOfInterrupt(interruptTimeNS);
    subTask.continueProcessing();
    double stageCompletion = subTask.isTaskComplete() ? 1.0 : subTask.getFractionComplete();
    fractionComplete = cumulativeTaskDurationWeight + subTaskDurationWeight * stageCompletion;
    return subTask.isTaskComplete();
  }

  protected boolean completed;
  protected boolean aborting = false;
  protected boolean rollingBack = false;
  protected boolean startRollback = false;
  protected long interruptTimeNS;
  protected double fractionComplete;
  protected WorldServer worldServer;
  protected WorldHistory worldHistory;
  protected EntityPlayerMP entityPlayerMP;
  protected int sequenceNumber;
  protected int rollbackSequenceNumber;
  private AsynchronousToken subTask;
  private double subTaskDurationWeight;
  private double cumulativeTaskDurationWeight;
  private final UniqueTokenID transactionID = new UniqueTokenID();
}