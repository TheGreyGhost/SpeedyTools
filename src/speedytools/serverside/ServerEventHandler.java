package speedytools.serverside;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.WorldEvent;
import org.lwjgl.input.Keyboard;
import speedytools.common.items.ItemCloneTool;
import speedytools.common.items.ItemSpeedyTool;

/**
 Contains the custom Forge Event Handlers relevant to the Server
 */
public class ServerEventHandler
{
  @ForgeSubscribe
  public void worldLoad(WorldEvent.Load event)
  {
    assert (event.world instanceof WorldServer);
    CloneToolServerActions.worldLoadEvent(event.world);
  }

  @ForgeSubscribe
  public void worldUnload(WorldEvent.Unload event)
  {
    assert (event.world instanceof WorldServer);
    CloneToolServerActions.worldUnloadEvent(event.world);
  }
}
