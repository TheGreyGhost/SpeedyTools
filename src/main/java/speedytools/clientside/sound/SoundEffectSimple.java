package speedytools.clientside.sound;

import net.minecraft.client.audio.PositionedSound;
import net.minecraft.util.ResourceLocation;

/**
 * Created by TheGreyGhost on 8/10/14.
 */
public class SoundEffectSimple
{
  public SoundEffectSimple(SoundEffectNames i_soundEffectName, SoundController i_soundController)
  {
    soundEffectName = i_soundEffectName;
    soundController = i_soundController;
    resourceLocation = new ResourceLocation(soundEffectName.getJsonName());
  }

  public void startPlaying()
  {
    final float VOLUME = 1.0F;
    startPlaying(VOLUME);
  }

  public void startPlayingLoop()
  {
    if (nonPositionedSound != null) {
      stopPlaying();
    }
    final float VOLUME = 1.0F;
    nonPositionedSound = new NonPositionedSound(resourceLocation, VOLUME, true);
    soundController.playSound(nonPositionedSound);
  }

  public void startPlaying(float initialVolume)
  {
    if (nonPositionedSound != null) {
      stopPlaying();
    }
    nonPositionedSound = new NonPositionedSound(resourceLocation, initialVolume, false);
    soundController.playSound(nonPositionedSound);
  }

  public void stopPlaying()
  {
    if (nonPositionedSound != null) {
      soundController.stopSound(nonPositionedSound);
      nonPositionedSound = null;
    }
  }

  private SoundEffectNames soundEffectName;
  private SoundController soundController;
  private ResourceLocation resourceLocation;
  private NonPositionedSound nonPositionedSound;

  private static class NonPositionedSound extends PositionedSound
  {
    public NonPositionedSound(ResourceLocation i_resourceLocation, float i_volume, boolean i_repeat)
    {
      super(i_resourceLocation);
      repeat = i_repeat;
      volume = i_volume;
      attenuationType = AttenuationType.NONE;
    }
  }

}
