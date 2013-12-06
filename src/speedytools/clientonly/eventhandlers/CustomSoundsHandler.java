package speedytools.clientonly.eventhandlers;

import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.ForgeSubscribe;

public class CustomSoundsHandler
{
  @ForgeSubscribe
  public void onSound(SoundLoadEvent event)
  {
    event.manager.addSound("speedytools:WandPlace.ogg");
    event.manager.addSound("speedytools:WandUnPlace.ogg");
    event.manager.addSound("speedytools:OrbPlace.ogg");
    event.manager.addSound("speedytools:OrbUnPlace.ogg");
    event.manager.addSound("speedytools:SceptreUnPlace.ogg");
    event.manager.addSound("speedytools:SceptrePlace.ogg");
  }
}

