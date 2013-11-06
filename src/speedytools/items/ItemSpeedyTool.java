package speedytools.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.util.ChunkCoordinates;
import speedytools.SpeedyToolsMod;

import java.util.List;

/**
 * User: The Grey Ghost
 * Date: 2/11/13
 */
public abstract class ItemSpeedyTool extends Item
{
  public ItemSpeedyTool(int id) {
    super(id);
    setCreativeTab(CreativeTabs.tabTools);
    setMaxDamage(-1);                         // not damageable
  }

  public static boolean isAspeedyTool(int itemID)
  {
    return (   itemID == SpeedyToolsMod.itemSpeedyStripStrong.itemID
            || itemID == SpeedyToolsMod.itemSpeedyStripWeak.itemID    );
  }

  /**
   * if true, this tool does not destroy "solid" blocks such as stone, only "non-solid" blocks such as air, grass, etc
   * @param itemID
   * @return
   */
  public static boolean leavesSolidBlocksIntact(int itemID)
  {
    return (itemID == SpeedyToolsMod.itemSpeedyStripWeak.itemID);
  }

  /**
   * Sets the current multiple-block selection for the currently-held speedy tool
   * @param currentTool the currently-held speedy tool
   * @param currentSelection list of coordinates of blocks in the current selection
   */

  @SideOnly(Side.CLIENT)
  public static void setCurrentToolSelection(Item currentTool, List<ChunkCoordinates> currentSelection)
  {
    currentlySelectedTool = currentTool;
    currentlySelectedBlocks = currentSelection;
  }

  /**
   * called when the user presses the attackButton (Left Mouse)
   */
  @SideOnly(Side.CLIENT)
  public static void attackButtonClicked()
  {

  }

  /**
   * called when the user presses the use button (right mouse)
   */
  @SideOnly(Side.CLIENT)
  public static void useButtonClicked()
  {


  }

  @SideOnly(Side.SERVER)
  public static void performServerAction(int toolItemID, int buttonClicked, List<ChunkCoordinates> blockSelection)
  {



  }


    // these keep track of the currently selected blocks, for when the tool is used
  @SideOnly(Side.CLIENT)
  protected static List<ChunkCoordinates> currentlySelectedBlocks = null;
  @SideOnly(Side.CLIENT)
  protected static Item currentlySelectedTool = null;

  @SideOnly(Side.SERVER)
  int i;  // dummy


}
