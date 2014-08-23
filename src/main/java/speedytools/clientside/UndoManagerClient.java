package speedytools.clientside;

import net.minecraft.util.Vec3;

import java.util.Deque;
import java.util.LinkedList;

/**
 * User: The Grey Ghost
 * Date: 18/04/2014
 */
public class UndoManagerClient
{
  UndoManagerClient(int i_MAX_UNDO_COUNT) {
    MAXIMUM_UNDO_COUNT = i_MAX_UNDO_COUNT;
  }

  public void performUndo(Vec3 playerPosition)
  {
    UndoCallback undoCallback = undoHistory.peekLast();
    if (undoCallback != null) {
      boolean undoPerformed = undoCallback.performUndo(playerPosition);
      if (undoPerformed) undoHistory.removeLast();
    }
  }

  public void addUndoableAction(UndoCallback undoCallback)
  {
    undoHistory.addLast(undoCallback);
    if (undoHistory.size() > MAXIMUM_UNDO_COUNT) undoHistory.removeFirst();
  }

  public static interface UndoCallback
  {
    public boolean performUndo(Vec3 playerPosition);
  }

  private final int MAXIMUM_UNDO_COUNT;

  protected static Deque<UndoCallback> undoHistory = new LinkedList<UndoCallback>();

}
