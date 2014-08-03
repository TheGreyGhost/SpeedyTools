package speedytools.serverside.actions;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import speedytools.common.selections.VoxelSelectionWithOrigin;
import speedytools.serverside.SpeedyToolsNetworkServer;
import speedytools.serverside.worldmanipulation.AsynchronousToken;
import speedytools.serverside.worldmanipulation.WorldHistory;

/**
 * User: The Grey Ghost
 * Date: 3/08/2014
 */
public abstract class AsynchronousActionBase implements AsynchronousToken
{
  public AsynchronousActionBase(SpeedyToolsNetworkServer i_speedyToolsNetworkServer, WorldServer i_worldServer, EntityPlayerMP i_player, WorldHistory i_worldHistory, int i_sequenceNumber)
  {
    speedyToolsNetworkServer = i_speedyToolsNetworkServer;
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
  public boolean isTimeToInterrupt() {
    return (interruptTimeNS == IMMEDIATE_TIMEOUT || (interruptTimeNS != INFINITE_TIMEOUT && System.nanoTime() >= interruptTimeNS));
  }

  @Override
  public void setTimeOfInterrupt(long timeToStopNS) {
    interruptTimeNS = timeToStopNS;
  }

  @Override
  public double getFractionComplete()
  {
    return fractionComplete;
  }

  public VoxelSelectionWithOrigin getLockedRegion() {return null;}

  /**
   * Set the task to be executed next
   * @param token the next task
   * @param taskDurationWeight the estimated relative duration of this task as a fraction of the whole task duration; 0.0 - 1.0;
   */
  public void setSubTask(AsynchronousToken token, double taskDurationWeight)
  {
    cumulativeTaskDurationWeight += subTaskDurationWeight;
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

  protected boolean completed;
  protected long interruptTimeNS;
  protected double fractionComplete;
  protected SpeedyToolsNetworkServer speedyToolsNetworkServer;
  protected WorldServer worldServer;
  protected WorldHistory worldHistory;
  protected EntityPlayerMP entityPlayerMP;
  protected int sequenceNumber;
  private AsynchronousToken subTask;
  private double subTaskDurationWeight;
  private double cumulativeTaskDurationWeight;

}