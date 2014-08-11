package speedytools.clientside.rendering;

import net.minecraftforge.client.event.RenderGameOverlayEvent;

/**
 * Created by TheGreyGhost on 11/08/14.
 */
public class RenderGameOverlayHotbarEvent extends RenderGameOverlayEvent
{
  public RenderGameOverlayHotbarEvent(RenderGameOverlayEvent parent)
    {
      super(parent.partialTicks, parent.resolution, parent.mouseX, parent.mouseY);
    }
}
