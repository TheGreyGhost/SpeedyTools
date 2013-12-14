package speedytools.clientonly.eventhandlers;

import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.ForgeSubscribe;

public class CustomSoundsHandler
{
  @ForgeSubscribe
  public void onSound(SoundLoadEvent event)
  {
    event.manager.addSound("speedytools:wandplace.ogg");
    event.manager.addSound("speedytools:wandunplace.ogg");
    event.manager.addSound("speedytools:orbplace.ogg");
    event.manager.addSound("speedytools:orbunplace.ogg");
    event.manager.addSound("speedytools:sceptreunplace.ogg");
    event.manager.addSound("speedytools:sceptreplace.ogg");
  }
}

