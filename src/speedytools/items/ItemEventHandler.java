package speedytools.items;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import speedytools.blocks.BlockWithMetadata;

import java.util.List;

/**
 Contains the custom Forge Event Handlers
 */
public class ItemEventHandler {
/*
  @ForgeSubscribe
  public void myLivingHurt(LivingHurtEvent event)
  {
    DamageSource damageSource = event.source;
    float amount = event.ammount;
    EntityLivingBase entityLivingBase = event.entityLiving;
    float damage = amount;
    // par2 = ForgeHooks.onLivingHurt(this, par1DamageSource, par2);
    // if (par2 <= 0) return;
    // par2 = this.applyArmorCalculations(par1DamageSource, par2);
    // par2 = this.applyPotionDamageCalculations(par1DamageSource, par2);
    float f1 = damage;
    damage = Math.max(damage - this.getAbsorbtion(), 0.0F);
    this.func_110149_m(this.getAbsorbtion() - (f1 - damage));

    if (damage != 0.0F)
    {
      float f2 = this.getHealth();
      this.setEntityHealth(f2 - damage);
      this.func_110142_aN().func_94547_a(par1DamageSource, f2, damage);
      this.func_110149_m(this.getAbsorbtion() - damage);
    }

  }
*/
/*
  @ForgeSubscribe
  public void addMyCreature(WorldEvent.PotentialSpawns event) {
    World world = event.world;
    int xposition = event.x;
    int yposition = event.y;
    int zposition = event.z;
    EnumCreatureType creatureType = event.type;
    List<SpawnListEntry> listOfSpawnableCreatures = event.list;

    final int SPAWNWEIGHT = 5;  // the higher the number, the more likely this creature will spawn
    final int MINIMUMNUMBERTOSPAWN = 1;
    final int MAXIMUMNUMBERTOSPAWN = 4;

    switch (creatureType) {
      case monster: {
        SpawnListEntry myNewCreatureSpawn = new SpawnListEntry(MyCreature.class, SPAWNWEIGHT, MINIMUMNUMBERTOSPAWN, MAXIMUMNUMBERTOSPAWN);
        listOfSpawnableCreatures.add(myNewCreatureSpawn);
        break;
      }
      case creature:
      case waterCreature:
      case ambient:
      default:
    }
  }
 */
  /**
   * If a SpeedyTools item is selected, draw nothing (drawing of selection box is performed in RenderWorldLastEvent).
   * Otherwise, cancel the event so that the normal selection box is drawn.
   * @param event
   */
  @ForgeSubscribe
  public void blockHighlightDecider(DrawBlockHighlightEvent event)
  {
    EntityPlayer player = event.player;
    ItemStack currentItem = player.inventory.getCurrentItem();

    if (currentItem == null || !ItemSpeedyTool.isAspeedyTool(currentItem.getItem().itemID)) {
      return;
    }

    event.setCanceled(true);
    return;
  }

  /**
   * If a speedy tool is equipped, selects the appropriate blocks and stores the selection into SpeedyToolsMod.currentlySelectedBlocks
   *    along with the substrate used by the tool (the block to be placed) which is the block in the hotbar immediately to the left of the tool
   * Also renders the selection over the top of the existing world
   * If player is holding down Left Control or Right Control, allow "diagonal" selections, otherwise restrict to selections parallel to the
   *    coordinate axes only.
   *  The speedy tool can be stacked; the number of tools in the stack determines the number of blocks in the selection.
   *
   * @param event
   */
  @ForgeSubscribe
  public void drawSelectionBox(RenderWorldLastEvent event)
  {
    RenderGlobal context = event.context;
    assert(context.mc.renderViewEntity instanceof EntityPlayer);
    EntityPlayer player = (EntityPlayer)context.mc.renderViewEntity;
    MovingObjectPosition target = context.mc.objectMouseOver;
    ItemStack currentItem = player.inventory.getCurrentItem();
    float partialTick = event.partialTicks;

    if (currentItem == null || !ItemSpeedyTool.isAspeedyTool(currentItem.getItem().itemID)) return;
    ItemSpeedyTool itemSpeedyTool = (ItemSpeedyTool)currentItem.getItem();

    List<ChunkCoordinates> selection = itemSpeedyTool.selectBlocks(target, player, currentItem, partialTick);

    // the block to be placed is the one to the left of the tool in the hotbar
    int currentlySelectedHotbarSlot = player.inventory.currentItem;
    ItemStack itemStackToPlace = (currentlySelectedHotbarSlot == 0) ? null : player.inventory.getStackInSlot(currentlySelectedHotbarSlot-1);
    BlockWithMetadata blockToPlace = ItemSpeedyTool.getPlacedBlockFromItemStack(itemStackToPlace);

    ItemSpeedyTool.setCurrentToolSelection(currentItem.getItem(), blockToPlace, selection);
    if (selection.isEmpty()) return;

    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
    GL11.glLineWidth(2.0F);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDepthMask(false);
    double expandDistance = 0.002F;

    double playerOriginX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTick;
    double playerOriginY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTick;
    double playerOriginZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTick;

    for (ChunkCoordinates block : selection) {
      AxisAlignedBB boundingBox = AxisAlignedBB.getAABBPool().getAABB(block.posX, block.posY, block.posZ,
                                                                      block.posX+1, block.posY+1, block.posZ+1);
      boundingBox = boundingBox.expand(expandDistance, expandDistance, expandDistance).getOffsetBoundingBox(-playerOriginX, -playerOriginY, -playerOriginZ);
      SelectionBoxRenderer.drawFilledCube(boundingBox);
    }

    GL11.glDepthMask(true);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
  }




}
