package speedytools.clientside.rendering;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.sound.SoundLoadEvent;

public class SoundsRegistry
{
  static public final String BOUNDARY_GRAB = "speedytools:boundarygrab.ogg";
  static public final String BOUNDARY_UNGRAB = "speedytools:boundaryungrab.ogg";
  static public final String BOUNDARY_PLACE_1ST = "speedytools:boundaryplace1st.ogg";
  static public final String BOUNDARY_PLACE_2ND = "speedytools:boundaryplace2nd.ogg";
  static public final String BOUNDARY_UNPLACE = "speedytools:boundaryunplace.ogg";

  @SubscribeEvent
  public void onSound(SoundLoadEvent event)
  {
//    for (SpeedySoundTypes speedySound : SpeedySoundTypes.values()) {      // todo testing only
//      event.manager.addSound(speedySound.getFilename());
//    }
/*
    event.manager.addSound("speedytools:wandplace.ogg");
    event.manager.addSound("speedytools:wandunplace.ogg");
    event.manager.addSound("speedytools:orbplace.ogg");
    event.manager.addSound("speedytools:orbunplace.ogg");
    event.manager.addSound("speedytools:sceptreunplace.ogg");
    event.manager.addSound("speedytools:sceptreplace.ogg");
    event.manager.addSound(BOUNDARY_GRAB);
    event.manager.addSound(BOUNDARY_UNGRAB);
    event.manager.addSound(BOUNDARY_PLACE_1ST);
    event.manager.addSound(BOUNDARY_PLACE_2ND);
    event.manager.addSound(BOUNDARY_UNPLACE);
*/
  }

}

