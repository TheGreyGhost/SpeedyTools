package speedytools.clientside.tools;

import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import speedytools.common.utilities.QuadOrientation;

/**
 * Created by TheGreyGhost on 16/10/14.
 * Holds the selection state information shared by the copy, move, delete complex tools
 */
public class CommonSelectionState
{
  public ChunkCoordinates selectionOrigin;
  public boolean selectionGrabActivated = false;
  public Vec3 selectionGrabPoint = null;
  public boolean selectionMovedFastYet;
  public boolean hasBeenMoved;               // used to change the appearance when freshly created or placed.
  public QuadOrientation selectionOrientation;
  public ChunkCoordinates initialSelectionOrigin;
  public QuadOrientation initialSelectionOrientation;

}
