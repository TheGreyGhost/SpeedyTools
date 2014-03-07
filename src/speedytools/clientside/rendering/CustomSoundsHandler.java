package speedytools.clientside.rendering;

import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.ForgeSubscribe;

public class CustomSoundsHandler
{
  static public final String BOUNDARY_GRAB = "speedytools:boundarygrab.ogg";
  static public final String BOUNDARY_UNGRAB = "speedytools:boundaryungrab.ogg";
  static public final String BOUNDARY_PLACE_1ST = "speedytools:boundaryplace1st.ogg";
  static public final String BOUNDARY_PLACE_2ND = "speedytools:boundaryplace2nd.ogg";
  static public final String BOUNDARY_UNPLACE = "speedytools:boundaryunplace.ogg";

  @ForgeSubscribe
  public void onSound(SoundLoadEvent event)
  {
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
  }
}

