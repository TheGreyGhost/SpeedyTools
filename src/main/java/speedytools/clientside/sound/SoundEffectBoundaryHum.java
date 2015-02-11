package speedytools.clientside.sound;

import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import speedytools.common.utilities.UsefulFunctions;

/**
 * Created by TheGreyGhost on 8/10/14.
 */
public class SoundEffectBoundaryHum
{
  public SoundEffectBoundaryHum(SoundEffectNames i_soundEffectName, SoundController i_soundController, BoundaryHumUpdateLink i_boundaryHumUpdateLink)
  {
    soundEffectName = i_soundEffectName;
    soundController = i_soundController;
    resourceLocation = new ResourceLocation(soundEffectName.getJsonName());
    boundaryHumUpdateLink = i_boundaryHumUpdateLink;
  }

  public void startPlayingLoop()
  {
    if (boundaryFieldSound != null) {
      stopPlaying();
    }
    final float INITIAL_VOLUME = 0.001F;
    boundaryFieldSound = new BoundaryFieldSound(resourceLocation, INITIAL_VOLUME, true);
    soundController.playSound(boundaryFieldSound);
    boundaryFieldSound.update();
  }

  public void stopPlaying()
  {
    if (boundaryFieldSound != null) {
      soundController.stopSound(boundaryFieldSound);
      boundaryFieldSound = null;
    }
  }

  private SoundEffectNames soundEffectName;
  private SoundController soundController;
  private ResourceLocation resourceLocation;
  private BoundaryFieldSound boundaryFieldSound;
  private BoundaryHumUpdateLink boundaryHumUpdateLink;

  /**
   * Used as a callback to update the sound's position and
   */
  public interface BoundaryHumUpdateLink
  {
    public boolean refreshHumInfo(BoundaryHumInfo infoToUpdate);
  }

  public static class BoundaryHumInfo
  {
    public Vec3 soundEpicentre;
    public float distanceToEpicentre;
  }

  private class BoundaryFieldSound extends PositionedSound implements ITickableSound
  {
    public BoundaryFieldSound(ResourceLocation i_resourceLocation, float i_volume, boolean i_repeat)
    {
      super(i_resourceLocation);
      repeat = i_repeat;
      volume = i_volume;
      attenuationType = AttenuationType.NONE;
    }

    private boolean donePlaying;
    @Override
    public boolean isDonePlaying() {
      return donePlaying;
    }

    /**
     * Updates the JList with a new model.
     */
    @Override
    public void update() {
      BoundaryHumInfo boundaryHumInfo = new BoundaryHumInfo();
      boolean playing = boundaryHumUpdateLink.refreshHumInfo(boundaryHumInfo);
      final float MINIMUM_VOLUME = 0.01F;
      final float MAXIMUM_VOLUME = 0.05F;
      final float INSIDE_VOLUME = 0.10F;
      final float OFF_VOLUME = 0.0F;
      if (!playing) {
//        donePlaying = true;
        this.volume = OFF_VOLUME;
      } else {
//        System.out.println(boundaryHumInfo.distanceToEpicentre);
        this.xPosF = (float)boundaryHumInfo.soundEpicentre.xCoord;
        this.yPosF = (float)boundaryHumInfo.soundEpicentre.yCoord;
        this.zPosF = (float)boundaryHumInfo.soundEpicentre.zCoord;
        if (boundaryHumInfo.distanceToEpicentre < 0.01F) {
          this.volume = INSIDE_VOLUME;
        } else {
          final float MINIMUM_VOLUME_DISTANCE = 20.0F;
          float fractionToMinimum = boundaryHumInfo.distanceToEpicentre / MINIMUM_VOLUME_DISTANCE;
          this.volume = UsefulFunctions.clipToRange(MINIMUM_VOLUME,
                                                    MAXIMUM_VOLUME - fractionToMinimum * (MAXIMUM_VOLUME - MINIMUM_VOLUME),
                                                    MAXIMUM_VOLUME);
        }
      }
    }
  }

}
