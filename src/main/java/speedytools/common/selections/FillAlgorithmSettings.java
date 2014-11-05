package speedytools.common.selections;

import net.minecraft.util.ChunkCoordinates;

/**
 * Created by TheGreyGhost on 5/11/14.
 */
public class FillAlgorithmSettings
{
  public enum Propagation {FLOODFILL, CONTOUR}

  private Propagation propagation = Propagation.FLOODFILL;
  private boolean diagonalPropagationAllowed = false;
  private ChunkCoordinates startPosition = new ChunkCoordinates();
  private FillMatcher fillMatcher = new FillMatcher();
}
