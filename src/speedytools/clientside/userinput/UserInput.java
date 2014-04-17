package speedytools.clientside.userinput;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.MouseEvent;
import org.lwjgl.input.Keyboard;

import java.util.LinkedList;
import java.util.Queue;

/**
 * User: The Grey Ghost
 * Date: 16/04/14
 * UserInput collects the various input sources and collates them into a suitable form for the tools:
 * (1) provides a queue of events for mousewheel, mouse buttons up & down
 * (2) provides information on how long the mouse button has been up or held down
 * Usage:
 * (1) Create the UserInput
 * (2) .activate() it to start collating data
 * (3) call .handleMouseEvent whenever there is a mouseevent (for the wheel)
 * (4) call .updateButtonStates() periodically (eg every tick) to update the left and right button states, which
 *     updates the queue and button hold statuses
 * (5)
 */
public class UserInput
{
  public UserInput()
  {
    active = false;
    this.reset();
  }

  /**
   * start collating input.
   */
  public void activate()
  {
    if (active) return;
    active = true;
    reset();
  }

  /**
   * stop collating input;
   */
  public void deactivate()
  {
    if (!active) return;
    active = false;
    reset();
  }

  /**
   * reset the data - empties the queue, clears the hold times
   */
  public void reset()
  {
    inputEvents.clear();
    leftButtonIsDown = false;
    rightButtonIsDown = false;
    leftButtonLastChangeTimeNS = 0;
    rightButtonLastChangeTimeNS = 0;
  }

  /**
   * handle the MouseEvent
   * @param event
   * @return true if event was handled by UserInput, false if it should be passed to vanilla
   */
  public boolean handleMouseEvent(MouseEvent event)
  {
    if (!active) return false;
    if (event.dwheel == 0) return false;
    EntityPlayer player = Minecraft.getMinecraft().thePlayer;
    if (player == null) return false;

    boolean controlKeyDown = controlKeyIsDown();
    if (!controlKeyDown) return false;

    final int MOUSE_DELTA_PER_STEP = 120;
    int stepCount = event.dwheel / MOUSE_DELTA_PER_STEP;
    if (stepCount == 0) return false;

    inputEvents.add(new InputEvent(InputEventType.WHEEL_MOVE, event.nanoseconds, controlKeyDown, stepCount));
    return true;
  }

  /**
   * update the state of the left and right buttons
   * @param newLeftButtonIsDown
   * @param newRightButtonIsDown
   * @param timeStampNS
   */
  public void updateButtonStates(boolean newLeftButtonIsDown, boolean newRightButtonIsDown, long timeStampNS)
  {
    if (!active) return;
    if (leftButtonLastChangeTimeNS == 0) {
      leftButtonLastChangeTimeNS = timeStampNS;
    } else if (newLeftButtonIsDown != this.leftButtonIsDown) {
      inputEvents.add(new InputEvent(newLeftButtonIsDown ? InputEventType.LEFT_CLICK_DOWN : InputEventType.LEFT_CLICK_UP,
                                     timeStampNS, controlKeyIsDown(), 1) );
      leftButtonLastChangeTimeNS = timeStampNS;
    }
    this.leftButtonIsDown = newLeftButtonIsDown;

    if (rightButtonLastChangeTimeNS == 0) {
      rightButtonLastChangeTimeNS = timeStampNS;
    } else if (newRightButtonIsDown && !this.rightButtonIsDown) {
      inputEvents.add(new InputEvent(newRightButtonIsDown ? InputEventType.RIGHT_CLICK_DOWN : InputEventType.RIGHT_CLICK_UP,
                                     timeStampNS, controlKeyIsDown(), 1) );
      rightButtonLastChangeTimeNS = timeStampNS;
    }
    this.rightButtonIsDown = newRightButtonIsDown;
  }

  /** returns the next event, without removing it from the queue
   * @return the next InputEvent, or null if none
   */
  public InputEvent peek()
  {
    return inputEvents.peek();
  }

  /** returns the next event, and removes it from the queue
   * @return the next InputEvent, or null if none
   */
  public InputEvent poll()
  {
    return inputEvents.poll();
  }

  /**
   * how long has the left button been in its current state?
   * @return hold time in ns.  0 = not valid, +ve = held down, -ve = up eg -1000 is "been up for 1000 ns."
   */
  public long leftButtonHoldTimeNS(long timeNow)
  {
    if (leftButtonLastChangeTimeNS == 0) return 0;
    long holdDuration = timeNow - leftButtonLastChangeTimeNS;
    assert holdDuration >= 0;
    return leftButtonIsDown ? holdDuration : -holdDuration;
  }

  /**
   * how long has the left button been in its current state?
   * @return hold time in ns.  +ve = held down, -ve = up eg -1000 is "been up for 1000 ns."
   */
  public long rightButtonHoldTimeNS(long timeNow)
  {
    if (rightButtonLastChangeTimeNS == 0) return 0;
    long holdDuration = timeNow - rightButtonLastChangeTimeNS;
    assert holdDuration >= 0;
    return rightButtonIsDown ? holdDuration : -holdDuration;
  }

  public enum InputEventType {
    LEFT_CLICK_DOWN, RIGHT_CLICK_DOWN, LEFT_CLICK_UP, RIGHT_CLICK_UP, WHEEL_MOVE;
  }

  public static class InputEvent
  {
    public InputEventType eventType;
    public long eventTimeNS;        // nanoseconds timestamp
    public boolean controlKeyDown;
    public int count;               // might be negative for some eg mousewheel

    public InputEvent(InputEventType i_inputEventType, long i_eventTimeNS, boolean i_controlKeyDown, int i_count) {
      eventType = i_inputEventType;
      eventTimeNS = i_eventTimeNS;
      controlKeyDown = i_controlKeyDown;
      count = i_count;
    }
  }

  private boolean controlKeyIsDown()
  {
    EntityPlayer player = Minecraft.getMinecraft().thePlayer;
    if (player == null) return false;

    return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
  }

  private boolean active;
  private Queue<InputEvent> inputEvents = new LinkedList<InputEvent>();
  private boolean leftButtonIsDown;
  private boolean rightButtonIsDown;
  private long leftButtonLastChangeTimeNS;       // 0 means not valid (no entry received yet)
  private long rightButtonLastChangeTimeNS;      // 0 means not valid (no entry received yet)
}
