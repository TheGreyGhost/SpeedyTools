package speedytools.clientside.tools;

import speedytools.clientside.UndoManagerClient;
import speedytools.clientside.network.CloneToolsNetworkClient;
import speedytools.clientside.network.PacketSenderClient;
import speedytools.clientside.rendering.RenderCursorStatus;
import speedytools.clientside.rendering.SpeedyToolRenderers;
import speedytools.clientside.rendering.SpeedyToolSounds;
import speedytools.clientside.selections.ClientVoxelSelection;
import speedytools.common.items.ItemComplexCopy;
import speedytools.common.utilities.Colour;

/**
* User: The Grey Ghost
* Date: 8/08/2014
*/
public class SpeedyToolComplexCopy extends SpeedyToolComplex
{
  public SpeedyToolComplexCopy(ItemComplexCopy i_parentItem, SpeedyToolRenderers i_renderers, SpeedyToolSounds i_speedyToolSounds, UndoManagerClient i_undoManagerClient, CloneToolsNetworkClient i_cloneToolsNetworkClient,
                               SpeedyToolBoundary i_speedyToolBoundary, ClientVoxelSelection i_clientVoxelSelection,
                               SelectionPacketSender packetSender, PacketSenderClient i_packetSenderClient) {
    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient, i_cloneToolsNetworkClient, i_speedyToolBoundary,
          i_clientVoxelSelection,  packetSender, i_packetSenderClient);
  }

  @Override
  public RenderCursorStatus.CursorRenderInfo.CursorType getCursorType() {
    return RenderCursorStatus.CursorRenderInfo.CursorType.COPY;
  }

  @Override
  protected Colour getSelectionRenderColour() {
    return Colour.LIGHTGREEN_40;
  }

}
