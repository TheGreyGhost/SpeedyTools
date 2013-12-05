package speedytools.clientonly.eventhandlers;

import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.ForgeSubscribe;

public class CustomSoundsHandler
{
  @ForgeSubscribe
  public void onSound(SoundLoadEvent event)
  {
    event.manager.addSound("speedytools:MultiBlockPlace.ogg");
    event.manager.addSound("speedytools:MultiBlockUnPlace.ogg");
    event.manager.addSound("speedytools:SceptreUnPlace.ogg");
    event.manager.addSound("speedytools:SceptrePlace.ogg");
  }
}

