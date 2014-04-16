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
 */
public class UserInput
{
  public void activate()
  {
    if (active) return;
    active = true;
  }

  public void deactivate()
  {
    if (!active) return;
    active = false;
    purgeQueue();
  }

  public void purgeQueue()
  {
    inputEvents.clear();
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

    inputEvents.add(new InputEvent( (stepCount > 0 ) ? InputEventType.WHEEL_UP : InputEventType.WHEEL_DOWN,
                                    event.nanoseconds, controlKeyDown, Math.abs(stepCount)                  );
    return true;
  }

  private boolean controlKeyIsDown()
  {
    EntityPlayer player = Minecraft.getMinecraft().thePlayer;
    if (player == null) return false;

    return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
  }

  public boolean updateButtonStates(boolean newLeftButtonIsDown, boolean newRightButtonIsDown, long timeStampNS)
  {
    if (!active) return false;
    if (newLeftButtonIsDown && !this.leftButtonIsDown) {
      inputEvents.add(new InputEvent(InputEventType.LEFT_CLICK_DOWN, timeStampNS, controlKeyIsDown(), 1) );
      leftButtonHoldStartTimeNS = System.nanoTime();
    }
    this.leftButtonIsDown = newLeftButtonIsDown;

    if (newRightButtonIsDown && !this.rightButtonIsDown) {
      inputEvents.add(new InputEvent(InputEventType.RIGHT_CLICK_DOWN, timeStampNS, controlKeyIsDown(), 1) );
      rightButtonHoldStartTimeNS = System.nanoTime();
    }
    this.rightButtonIsDown = newRightButtonIsDown;
    return true;
  }

  public enum InputEventType {
    LEFT_CLICK_DOWN, RIGHT_CLICK_DOWN, WHEEL_UP, WHEEL_DOWN;
  }

  public static class InputEvent
  {
    public InputEventType eventType;
    public long eventTimeNS;        // nanoseconds timestamp
    public boolean controlKeyDown;
    public int count;

    public InputEvent(InputEventType i_inputEventType, long i_eventTimeNS, boolean i_controlKeyDown, int i_count) {
      eventType = i_inputEventType;
      eventTimeNS = i_eventTimeNS;
      controlKeyDown = i_controlKeyDown;
      count = i_count;
    }
  }

  private boolean active;
  private Queue<InputEvent> inputEvents = new LinkedList<InputEvent>();
  private boolean leftButtonIsDown;
  private boolean rightButtonIsDown;
  private long leftButtonHoldStartTimeNS;
  private long rightButtonHoldStartTimeNS;
}
