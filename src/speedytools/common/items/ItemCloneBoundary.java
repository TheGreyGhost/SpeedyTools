package speedytools.common.items;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import speedytools.clientside.rendering.SoundsRegistry;
import speedytools.clientside.selections.BlockMultiSelector;
import speedytools.common.utilities.UsefulConstants;
import speedytools.common.utilities.UsefulFunctions;

import java.util.HashMap;
import java.util.List;

public class ItemCloneBoundary extends ItemCloneTool {
  public ItemCloneBoundary(int id) {
    super(id);
    setMaxStackSize(1);
    setUnlocalizedName("CloneBoundary");
    setFull3D();                              // setting this flag causes the staff to render vertically in 3rd person view, like a pickaxe
    whichIcon = IconNames.NONE_PLACED;
  }

  @Override
  public void registerIcons(IconRegister iconRegister)
  {
    icons.clear();
    for (IconNames entry : IconNames.values()) {
      Icon newIcon = iconRegister.registerIcon(entry.filename);
      icons.put(entry, newIcon);
    }
    itemIcon = icons.get(IconNames.NONE_PLACED);
  }

  @Override
  public Icon getIcon(ItemStack stack, int pass)
  {
    return icons.get(whichIcon);
  }

  public enum IconNames
  {
    BLANK("speedytools:blankicon"),
    GRABBING("speedytools:cloneboundarygrab"),
    NONE_PLACED("speedytools:cloneboundarynone"),
    ONE_PLACED("speedytools:cloneboundaryone"),
    TWO_PLACED("speedytools:cloneboundarytwo");

    private IconNames(String i_filename) {filename = i_filename;}

    private final String filename;
  }

  public void setCurrentIcon(IconNames newIcon)
  {
    whichIcon = newIcon;
  }

  /**
   * allows items to add custom lines of information to the mouseover description
   */
  @Override
  public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List textList, boolean useAdvancedItemTooltips)
  {
    textList.add("Right click: place boundary");
    textList.add("             markers (x2), then");
    textList.add(" Right button hold: move around");
    textList.add("             to drag boundary");
    textList.add("Left click: remove all markers");
  }

  private IconNames whichIcon;
  private HashMap<IconNames, Icon> icons = new HashMap<IconNames, Icon>();
}