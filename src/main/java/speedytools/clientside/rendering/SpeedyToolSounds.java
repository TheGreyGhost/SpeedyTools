//package speedytools.clientside.rendering;
//
//import net.minecraft.client.Minecraft;
//import net.minecraft.util.Vec3;
//
///**
// * User: The Grey Ghost
// * Date: 17/04/2014
// */
//public class SpeedyToolSounds
//{
//  public SoundControlLink playSound(SpeedySoundTypes soundType, Vec3 soundPosition)
//  {
//    final float VOLUME = 1.0F;
//    final float PITCH = 1.0F;
//    Minecraft.getMinecraft().sndManager.playSound(soundType.getFilename(),
//                                                  (float)soundPosition.xCoord, (float)soundPosition.yCoord, (float)soundPosition.zCoord,
//                                                  VOLUME, PITCH);
//    return this.new SoundControlLinkDoNothing();
//  }
//
//  public abstract class SoundControlLink
//  {
//    public abstract void startSound();
//    public abstract void stopSound();
//  }
//
//  private class SoundControlLinkDoNothing extends SoundControlLink
//  {
//    public  void startSound() {};
//    public  void stopSound() {};
//  }
//}
