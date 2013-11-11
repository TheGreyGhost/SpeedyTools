package speedytools.client;

import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;

/**
 * User: The Grey Ghost
 * Date: 10/11/13
 */
public class ConfusedMovementInput extends MovementInput
{
  public ConfusedMovementInput(MovementInput interceptedMovementInput)
  {
    underlyingMovementInput = interceptedMovementInput;
  }

  @Override
  public void updatePlayerMoveState() {
    underlyingMovementInput.updatePlayerMoveState();

    this.jump = underlyingMovementInput.jump;
    this.sneak = underlyingMovementInput.sneak;

    if (!confused) {
      this.moveStrafe = underlyingMovementInput.moveStrafe;
      this.moveForward = underlyingMovementInput.moveForward;
    } else {
      this.moveStrafe = -underlyingMovementInput.moveStrafe;
      this.moveForward = underlyingMovementInput.moveForward;
      // swap moveStrafe, moveForward, or make them negative, etc
    }

  }

  public void setConfusion(boolean newConfused) {
    confused = newConfused;
  }

  protected MovementInput underlyingMovementInput;
  private boolean confused = false;
}
