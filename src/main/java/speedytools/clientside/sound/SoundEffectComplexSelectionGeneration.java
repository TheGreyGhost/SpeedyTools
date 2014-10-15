package speedytools.clientside.sound;

import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.util.ResourceLocation;
import speedytools.common.utilities.ErrorLog;
import speedytools.common.utilities.UsefulFunctions;

import java.lang.ref.WeakReference;
import java.util.Deque;
import java.util.Random;

/**
 * Created by TheGreyGhost on 8/10/14.
 *
 * Used to create sound effects for the complex tool when it creates a selection
 * Random layering of sounds at random intervals
 * performTick() should be called every tick by the client
 */
public class SoundEffectComplexSelectionGeneration
{
  public SoundEffectComplexSelectionGeneration(SoundController i_soundController)
  {
    soundController = i_soundController;
    generationResource = new ResourceLocation(SoundEffectNames.CREATE_SELECTION.getJsonName());
  }

  public void startPlaying()
  {
    tickCountOfCurrentSounds = 0;
    soundActive = true;
//    lastGenerationInProgress = null;
  }

  public void stopPlaying()
  {
    soundActive = false;
  }

  /** call every tick to update the sounds of selection generation
   * @param generationInProgress  true if the selection is currently being generated
   */
  public void performTick(boolean generationInProgress)
  {
    if (!soundActive) return;

//    if (lastGenerationInProgress == null || lastGenerationInProgress != generationInProgress) {
//      lastGenerationInProgress = generationInProgress;
//    }

    final int MAX_CURRENT_SOUND_TICK_COUNT = 30;
    final int AVERAGE_SOUND_SPACING_TICKS = 5;
    final int TICKS_PER_SOUND = 10;

    --tickCountOfCurrentSounds;
    tickCountOfCurrentSounds = Math.max(0, tickCountOfCurrentSounds);
    if (tickCountOfCurrentSounds > MAX_CURRENT_SOUND_TICK_COUNT) {
      return;
    }

    if (!generationInProgress) return;

    if (random.nextFloat() * AVERAGE_SOUND_SPACING_TICKS <= 1.0) {
      final float MAX_VOLUME = 1.0F;
      final float MIN_VOLUME = 0.4F;
      float volume = MIN_VOLUME + (MAX_VOLUME - MIN_VOLUME) * random.nextFloat();
      NonPositionedSound nonPositionedSound = new NonPositionedSound(generationResource, volume, false);
      soundController.playSound(nonPositionedSound);
      tickCountOfCurrentSounds += TICKS_PER_SOUND;
    }
  }

  private int tickCountOfCurrentSounds;
  private boolean soundActive = false;
//  private Boolean lastGenerationInProgress;
  private Random random = new Random();
  ResourceLocation generationResource;

  private SoundController soundController;

  private static class NonPositionedSound extends PositionedSound
  {
    public NonPositionedSound(ResourceLocation i_resourceLocation, float i_volume, boolean i_repeat)
    {
      super(i_resourceLocation);
      repeat = i_repeat;
      volume = i_volume;
      field_147666_i = AttenuationType.NONE;
    }
  }

}
