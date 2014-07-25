package speedytools.serverside.worldmanipulation;

/**
 * User: The Grey Ghost
 * Date: 24/07/2014
 */
public interface AsynchronousToken
{
  // returns true if the asynchronous task has completed
  public boolean isTaskComplete();

  // returns 0.0 .. 1.0 depending on how much of the task is complete
  public double getFractionComplete();

  // returns true if the task should interrupt processing now
  public boolean isTimeToInterrupt();

  // sets the System.nanoTime at which to interrupt processing
  public void setTimeToInterrupt(long timeToStopNS);

  // continue processing until interrupt is reached
  public void continueProcessing();

  public final long INFINITE_TIMEOUT = 0L;
  public final long IMMEDIATE_TIMEOUT = -1L;
}
