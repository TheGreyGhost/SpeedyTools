//package speedytools.clientside.userinput;
//
//import net.minecraftforge.client.event.MouseEvent;
//import net.minecraftforge.event.ForgeSubscribe;
//import speedytools.clientside.ClientSide;
//
///**
// Contains the custom Forge Event Handlers related to Input
// */
//public class InputEventHandler
//{
//
//  @ForgeSubscribe
//  public void interceptMouseInput(MouseEvent event)
//  {
//    boolean handled = ClientSide.userInput.handleMouseEvent(event);
//    if (handled) event.setCanceled(true);
//    return;
//
//    /*
//    if (event.dwheel == 0) return;
//    EntityPlayer player = Minecraft.getMinecraft().thePlayer;
//    if (player == null) return;
//    ItemStack currentItem = player.inventory.getCurrentItem();
//    boolean speedyToolHeld = currentItem != null && ItemSpeedyTool.isAspeedyTool(currentItem.itemID);
//    boolean cloneToolHeld = currentItem != null && ItemCloneTool.isAcloneTool(currentItem.itemID);
//
//    if (!speedyToolHeld && !cloneToolHeld) return;
//
//    boolean controlKeyDown =  Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
//    if (!controlKeyDown) return;
//
//    event.setCanceled(true);
//    if (speedyToolHeld) {
//      ItemSpeedyTool.mouseWheelMoved(event.dwheel);
//    } else if (cloneToolHeld) {
//      ItemCloneTool.mouseWheelMoved(event.dwheel);
//    }
//    return;
//    */
//  }
//
//}
