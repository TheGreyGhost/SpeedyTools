package speedytools.serverside.worldmanipulation;

import speedytools.common.selections.VoxelSelectionWithOrigin;

/**
 * User: The Grey Ghost
 * Date: 24/07/2014
 * AsynchronousToken provides an interface between an asynchronous task and the caller.
 * Typical usage:
 * 1) Call the asynchronous method, which returns a new token
 * 2) repeatedly call setTimeOfInterrupt() and then continueProcessing()
 * 3) The status of the task is read using isTaskComplete() and getFractionComplete()
 *
 *  isTimeToInterrupt() is intended for use by the implementation of the token / the asynchronous method
 *    It should be called periodically to see if the interrupt has occurred
 *
 */
public interface AsynchronousToken
{
  // returns true if the asynchronous task has completed
  public boolean isTaskComplete();

  // returns 0.0 .. 1.0 depending on how much of the task is complete
  public double getFractionComplete();

  // returns true if the task should interrupt processing now
  public boolean isTimeToInterrupt();

  /**
   * sets the System.nanoTime at which to interrupt processing
   * @param timeToStopNS the System.nanoTime() at which processing should stop.  INFINITE_TIMEOUT and IMMEDIATE_TIMEOUT are special values
   */
  public void setTimeOfInterrupt(long timeToStopNS);

  // continue processing until interrupt is reached
  public void continueProcessing();

  // returns a voxel selection corresponding to the world region locked by this task; null = none
  public VoxelSelectionWithOrigin getLockedRegion();

  public final long INFINITE_TIMEOUT = 0L;
  public final long IMMEDIATE_TIMEOUT = -1L;
}
