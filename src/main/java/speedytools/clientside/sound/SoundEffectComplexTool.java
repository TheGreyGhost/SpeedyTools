package speedytools.clientside.sound;

import net.minecraft.client.audio.PositionedSound;
import net.minecraft.util.ResourceLocation;

/**
 * Created by TheGreyGhost on 8/10/14.
 *
 * Used to create sound effects for the complex tool - powerup, sustain, and failure
 *
 * There are several basic states created from a couple of overlaid sounds
 * 1) power up - including sustain while the user holds down the button
 * 1b) failure (eg busy)
 * 2) working
 * 3) finishing
 * There are two overlaid sounds:
 * a1) powerringchargeup followed by powerringchargeloop while the ring is powering up
 * a2) powerringchargefailure when a failure message is received
 * b) powerringwhooshloop which is faded in during powering up, then looped while working and faded out when finished
 */
public class SoundEffectComplexTool
{
  public SoundEffectComplexTool(SoundEffectNames i_soundEffectName, SoundController i_soundController)
  {
    soundEffectName = i_soundEffectName;
    soundController = i_soundController;
    resourceLocation = new ResourceLocation(soundEffectName.getJsonName());
  }

  public void startPlaying()
  {
    if (nonPositionedSound != null) {
      stopPlaying();
    }
    final float VOLUME = 1.0F;
    nonPositionedSound = new NonPositionedSound(resourceLocation, VOLUME, false);
    soundController.playSound(nonPositionedSound);
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
      field_147666_i = AttenuationType.NONE;
    }
  }

}
