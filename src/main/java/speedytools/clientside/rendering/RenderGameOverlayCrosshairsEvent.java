package speedytools.clientside.rendering;

import net.minecraftforge.client.event.RenderGameOverlayEvent;

/**
 * Created by TheGreyGhost on 11/08/14.
 */
public class RenderGameOverlayCrosshairsEvent extends RenderGameOverlayEvent
{
  public RenderGameOverlayCrosshairsEvent(RenderGameOverlayEvent parent)
    {
      super(parent.partialTicks, parent.resolution);
    }
}
