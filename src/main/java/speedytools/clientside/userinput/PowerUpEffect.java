package speedytools.clientside.userinput;

/**
 * Manages the state of a "Power up" object (eg for animation purposes)
 * - start of charging up, progress of charge, release of charge
 * Usage:
 * (1) initiate() to begin the powerup; supply the current time and the duration of the powerup
 *     The first call to pollState() will return INITIATING, subsequent calls return POWERINGUP
 * (2) updateHolddownTime periodically, to inform as the charge builds up
 * (3) release() to stop the powerup
 *     The first call to pollState() will return RELEASING, subsequent calls return IDLE
 * The state of the powerup can be read:
 * (1) The current state is available using peekState or pollState.
 * (2) The current time in a state from getTimeSpentInThisState
 * (3) the percentage completion (0 - 100) from getPercentComplete
 */

public class PowerUpEffect
{
  public enum State {
      IDLE, INITIATING, POWERINGUP, RELEASING     // INITIATING and RELEASING are transient, to inform the caller that the state has changed.
   }

  /** start the powerup
   * @param new_initiationTime the time (in ns) when the powerup started
   * @param new_completionTime the time (in ns) when the powerup will be complete
   * @param new_initialHoldDuration the length of time (in ns) that the button had been held down, when the powerup started
   */
  public void initiate(long new_initiationTime, long new_completionTime, long new_initialHoldDuration) {
    initiationTime = new_initiationTime;
    expectedCompletionTime = new_completionTime;
    assert (expectedCompletionTime > initiationTime);
    assert (initialHoldDuration >= 0);
    initialHoldDuration = new_initialHoldDuration;
    currentHoldDuration = 0;
    state = State.INITIATING;
  }

  public void release(long new_releaseTime) {
    releaseTime = new_releaseTime;
    state = State.RELEASING;
  }
  public void abort() {
    releaseTime = initiationTime;
    state = State.RELEASING;
  }

  /**
   * update the duration of the hold
   * @param lengthOfHold total time in ns that the button has been held down
   */
  public void updateHolddownTime(long lengthOfHold) {
    if (state != State.INITIATING && state != State.POWERINGUP) return;
    if (lengthOfHold < initialHoldDuration) {     // the hold time is shorter than the start value, so presumably there was a fast click_up and then click_down again
      initialHoldDuration = lengthOfHold; //   in this case - reset the initial hold duration to be now.
    }
    currentHoldDuration = lengthOfHold;
  }

  /** returns the current state without affecting it
   * @return
   */
  public State peekState() {return state;}

  /** returns the current state and advances to the next state as appropriate
   * @return
   */
  public State pollState() {
    State retval = state;
    if (state == State.INITIATING) state = State.POWERINGUP;
    if (state == State.RELEASING) state = State.IDLE;
    return retval;
  }

  public boolean isIdle(){return (state == State.IDLE || state == State.RELEASING);}

  /** returns the time spent in this state
    * @param timeNow the current time in ns
   * @return duration in ns, or 0 if not available.
   */
  public long getTimeSpentInThisState(long timeNow) {
    switch (state) {
      case RELEASING:
      case IDLE: {
        if (releaseTime == 0) return 0;
        assert (timeNow > releaseTime);
        return (timeNow - releaseTime);
      }
      case INITIATING:
      case POWERINGUP: {
        assert (initiationTime != 0 && timeNow > initiationTime);

        return (timeNow - initiationTime);
      }
      default: assert false: "Invalid state:" + state;
    }
    return 0;
  }

  /**
   * returns the percentage of the powerup completed; either the current one (if currently powering up) or the most-recently-completed one.
   * @return
   */
  public double getPercentCompleted() {
    switch (state) {
      case RELEASING:
      case IDLE: {
        if (releaseTime == 0) return 0;
        double fraction = (releaseTime - initiationTime);
        fraction /= (expectedCompletionTime - initiationTime);
        fraction *= 100.0;
        fraction = Math.min(100.0, fraction);
        assert (fraction >= 0.0 && fraction <= 100.0);
        return fraction;
      }
      case INITIATING:
      case POWERINGUP: {
        if (currentHoldDuration == 0) return 0;
        double fraction = currentHoldDuration - initialHoldDuration;
        fraction /= (expectedCompletionTime - initiationTime);
        fraction *= 100.0;
        fraction = Math.min(100.0, fraction);
        assert (fraction >= 0.0 && fraction <= 100.0);
        return fraction;
      }
      default: assert false: "Invalid state:" + state;
    }
    return 0.0;
  }

  private State state;
  private long initiationTime = 0;
  private long initialHoldDuration = 0;    // the duration the button had been held down when powerup started
  private long currentHoldDuration = 0;    // the duration the button had currently been held down
  private long expectedCompletionTime = 0;
  private long releaseTime = 0;
}
