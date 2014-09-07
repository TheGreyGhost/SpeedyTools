//package speedytools.clientside.tools;
//
//import speedytools.clientside.UndoManagerClient;
//import speedytools.clientside.network.CloneToolsNetworkClient;
//import speedytools.clientside.rendering.RenderCursorStatus;
//import speedytools.clientside.rendering.SpeedyToolRenderers;
//import speedytools.clientside.rendering.SpeedyToolSounds;
//import speedytools.common.items.ItemComplexMove;
//import speedytools.common.utilities.Colour;
//
///**
//* User: The Grey Ghost
//* Date: 8/08/2014
//*/
//public class SpeedyToolComplexMove extends SpeedyToolComplex
//{
//  public SpeedyToolComplexMove(ItemComplexMove i_parentItem, SpeedyToolRenderers i_renderers, SpeedyToolSounds i_speedyToolSounds, UndoManagerClient i_undoManagerClient, CloneToolsNetworkClient i_cloneToolsNetworkClient, SpeedyToolBoundary i_speedyToolBoundary, SelectionPacketSender packetSender) {
//    super(i_parentItem, i_renderers, i_speedyToolSounds, i_undoManagerClient, i_cloneToolsNetworkClient, i_speedyToolBoundary, packetSender);
//  }
//
//  @Override
//  protected RenderCursorStatus.CursorRenderInfo.CursorType getCursorType() {
//    return RenderCursorStatus.CursorRenderInfo.CursorType.MOVE;
//  }
//
//  @Override
//  protected Colour getSelectionRenderColour() {
//    return Colour.LIGHTBLUE_40;
//  }
//}
