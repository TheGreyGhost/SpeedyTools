package speedytools.common.items;

/**
* User: The Grey Ghost
* Date: 2/11/13
*/
public abstract class ItemComplexBase extends ItemSpeedyTool
{
  public ItemComplexBase() {
    super(PlacementCountModes.INFINITE_ONLY);
  }

  @Override
  public boolean usesAdjacentBlockInHotbar() {return false;}
}
