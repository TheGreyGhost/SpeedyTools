package speedytools.common.selections;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.ChunkCoordinates;
import speedytools.common.utilities.ErrorLog;

/**
 * Created by TheGreyGhost on 5/11/14.
 */
public class FillAlgorithmSettings
{
  public enum Propagation {FLOODFILL, CONTOUR}

  /** generates a FillAlgorithmSettings from a ByteBuf serialised version
   * @param buf
   * @return  the FillAlgorithmSettings, or null for invalid
   */
  public static FillAlgorithmSettings createFromBuffer(ByteBuf buf)
  {
    FillAlgorithmSettings retval = new FillAlgorithmSettings();
    try {
      retval.propagation = Propagation.values()[buf.readInt()];
      retval.diagonalPropagationAllowed = buf.readBoolean();
      retval.automaticLowerBound = buf.readBoolean();
      retval.startPosition = new ChunkCoordinates(buf.readInt(), buf.readInt(), buf.readInt());
      retval.fillMatcher = FillMatcher.createMatcherFromBuffer(buf);
    } catch (IndexOutOfBoundsException ioe) {
      ErrorLog.defaultLog().info("Exception while createMatcherFromBuffer: " + ioe);
      return null;
    }
    return retval;
  }

  public void writeToBuffer(ByteBuf buf) {
    buf.writeInt(propagation.ordinal());
    buf.writeBoolean(diagonalPropagationAllowed);
    buf.writeBoolean(automaticLowerBound);
    buf.writeInt(startPosition.posX);
    buf.writeInt(startPosition.posY);
    buf.writeInt(startPosition.posZ);
    fillMatcher.writeToBuffer(buf);
  }

  public Propagation getPropagation() {
    return propagation;
  }

  public boolean isDiagonalPropagationAllowed() {
    return diagonalPropagationAllowed;
  }

  public ChunkCoordinates getStartPosition() {
    return startPosition;
  }

  public FillMatcher getFillMatcher() {
    return fillMatcher;
  }

  public void setPropagation(Propagation propagation) {
    this.propagation = propagation;
  }

  public void setDiagonalPropagationAllowed(boolean diagonalPropagationAllowed) {
    this.diagonalPropagationAllowed = diagonalPropagationAllowed;
  }

  public void setStartPosition(ChunkCoordinates startPosition) {
    this.startPosition = startPosition;
  }

  public void setFillMatcher(FillMatcher fillMatcher) {
    this.fillMatcher = fillMatcher;
  }

  public boolean isAutomaticLowerBound() {
    return automaticLowerBound;
  }

  public void setAutomaticLowerBound(boolean automaticLowerBound) {
    this.automaticLowerBound = automaticLowerBound;
  }

  private Propagation propagation = Propagation.FLOODFILL;
  private boolean diagonalPropagationAllowed = false;
  private ChunkCoordinates startPosition = new ChunkCoordinates();
  private FillMatcher fillMatcher = new FillMatcher.NullMatcher();
  private boolean automaticLowerBound = true;
}
