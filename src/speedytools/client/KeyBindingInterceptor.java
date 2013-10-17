package speedytools.client;

/**
 * The purpose of this class is to intercept key presses (especially left and right mouse button clicks) and allow
 *    greater flexibility in responding to them.
 *   The class replaces KeyBindings in GameSettings.  When interception is on:
 *      .isPressed() is overridden to return false so that the vanilla code never receives the clicks.
 *      .pressed is always false.
 *      The true .isPressed() and .pressed are available using .retrieveClick() and .isKeyDown()
 *   Usage:
 *    (1) replace KeyBinding with a newly generated intercept
 *        eg
 *        KeyBindingInterceptor attackButtonInterceptor(GameSettings.keyBindAttack);
 *        GameSettings.keyBindAttack = attackButtonInterceptor;
 *        This creates an interceptor linked to the existing keyBindAttack.  The existing keyBindAttack remains in the
 *          KeyBinding hashmap and keyBindArray.
 *    (2) Set the interception mode (eg OFF, ON, IN_WORLD = only when no GUI present)
 *        eg  setInterceptionMode(InterceptionMode.OFF);
 *    (3) read the underlying clicks using .retrieveClick() or .isKeyDown();
 *    (4) when Interceptor is no longer required, call .getOriginalKeyBinding();
 *        eg GameSettings.keyBindAttack = attackButtonInterceptor.getOriginalKeyBinding();
 *
 *  NOTES -
 *    (a) the interceptor does not update the .pressed field until .isPressed() is called.  The vanilla Minecraft.runTick
 *        currently always accesses .isPressed() before attempting to read .pressed.
 *    (b) In the current vanilla code, if the bindings are changed it will affect the interceptor.  The new binding will
 *        be copied to the original interceptedKeyBinding at the first call to .retrieveClick(), .isKeyDown(), or .isPressed().
 *   The clicks are only intercepted when the user is not viewing a GUI, i.e.
 *     Minecraft.getMinecraft().currentScreen == NULL
  * QN for later - what about this.currentScreen.allowUserInput?
 */
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import java.util.Iterator;

@SideOnly(Side.CLIENT)
public class KeyBindingInterceptor extends KeyBinding
{
  /**
   *  Create an Interceptor based on an existing binding.
   *  The initial interception mode is OFF.
   *  If existingKeyBinding is already a KeyBindingInterceptor, a reinitialised copy will be created but no further effect.
   * @param existingKeyBinding - the binding that will be intercepted.
   */
  public KeyBindingInterceptor(KeyBinding existingKeyBinding)
  {
    super(existingKeyBinding.keyDescription, existingKeyBinding.keyCode);
    // the base constructor automatically adds the class to the keybindArray and hash, which we don't want, so undo it
    keybindArray.remove(this);

    this.interceptionMode = InterceptionMode.OFF;
    this.pressed = false;
    this.pressTime = 0;

    if (existingKeyBinding instanceof KeyBindingInterceptor) {
      interceptedKeyBinding = ((KeyBindingInterceptor)existingKeyBinding).getOriginalKeyBinding();
    } else {
      interceptedKeyBinding = existingKeyBinding;
    }

    KeyBinding.resetKeyBindingArrayAndHash();
  }

  public enum InterceptionMode
  {
    OFF,          // don't intercept
    ON,           // always intercept
    IN_WORLD      // only intercept if Minecraft.getMinecraft().currentScreen is null
  }

  public void setInterceptionMode(InterceptionMode newMode)
  {
    interceptionMode = newMode;
  }


  public boolean isKeyDown()
  {
    copyKeyCodeToOriginal();
    return interceptedKeyBinding.pressed;
  }

  /**
   *
   * @return returns false if interception isn't active.  Otherwise, retrieves one of the clicks (true) or false if no clicks left
   */
  public boolean retrieveClick()
  {
    copyKeyCodeToOriginal();
    if (    interceptionMode == InterceptionMode.ON
        || (interceptionMode == InterceptionMode.IN_WORLD && Minecraft.getMinecraft().currentScreen == null )
       ) {
      copyClickInfoFromOriginal();

      if (this.pressTime == 0) {
        return false;
      } else {
        --this.pressTime;
        return true;
      }
    } else {
      return false;
    }
  }

  /** A better name for this method would be retrieveClick.
   * If interception is on, resets .pressed and .pressTime to zero.
   * Otherwise, copies these from the intercepted KeyBinding.
   * @return If interception is on, this will return false; Otherwise, it will pass on any clicks in the intercepted KeyBinding
   */
  @Override
  public boolean isPressed()
  {
    copyKeyCodeToOriginal();
    copyClickInfoFromOriginal();
    if (    interceptionMode == InterceptionMode.ON
            || (interceptionMode == InterceptionMode.IN_WORLD && Minecraft.getMinecraft().currentScreen == null )
            ) {
      this.pressTime = 0;
      this.pressed = false;
      return false;
    } else {

      if (this.pressTime == 0) {
        return false;
      } else {
        --this.pressTime;
        return true;
      }
    }
  }

  public KeyBinding getOriginalKeyBinding() {
    return interceptedKeyBinding;
  }

  protected KeyBinding interceptedKeyBinding;
  private InterceptionMode interceptionMode;

  protected void copyClickInfoFromOriginal()
  {
    this.pressTime += interceptedKeyBinding.pressTime;
    interceptedKeyBinding.pressTime = 0;
    this.pressed = interceptedKeyBinding.pressed;
  }

  protected void copyKeyCodeToOriginal()
  {
    // only copy if necessary
    if (interceptedKeyBinding.keyCode != this.keyCode) {
      interceptedKeyBinding.keyCode = this.keyCode;
      resetKeyBindingArrayAndHash();
    }
  }

}
