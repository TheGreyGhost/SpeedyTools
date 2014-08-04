package speedytools.serverside.actions;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import speedytools.common.network.ServerStatus;
import speedytools.common.selections.VoxelSelectionWithOrigin;
import speedytools.common.utilities.QuadOrientation;
import speedytools.serverside.SpeedyToolsNetworkServer;
import speedytools.serverside.worldmanipulation.AsynchronousToken;
import speedytools.serverside.worldmanipulation.WorldFragment;
import speedytools.serverside.worldmanipulation.WorldHistory;

import java.util.EnumMap;

/**
 * User: The Grey Ghost
 * Date: 3/08/2014
 */
public class AsynchronousActionPlacement extends AsynchronousActionBase
{
  public AsynchronousActionPlacement(SpeedyToolsNetworkServer i_speedyToolsNetworkServer, WorldServer i_worldServer, EntityPlayerMP i_player, WorldHistory i_worldHistory,
                                     VoxelSelectionWithOrigin i_voxelSelection,
                                     int i_sequenceNumber, int i_toolID, int i_xpos, int i_ypos, int i_zpos, QuadOrientation i_quadOrientation)
  {
    super(i_speedyToolsNetworkServer, i_worldServer, i_player, i_worldHistory, i_sequenceNumber);
    sourceVoxelSelection = i_voxelSelection;
    toolID = i_toolID;
    xpos = i_xpos;
    ypos = i_ypos;
    zpos = i_zpos;
    quadOrientation = i_quadOrientation;
    currentStage = ActionStage.SETUP;
//    for (ActionStage actionStage : ActionStage.values()) {
//      ticksPerStage.put(actionStage, 0);
//      milliSecondsPerStage.put(actionStage, 0.0);
//    }
  }

  @Override
  public void continueProcessing() {
    long timeIn = System.nanoTime();
//    ActionStage entryStage = currentStage;
    switch (currentStage) {
      case SETUP: {
        if (aborting) {
          completed = true;sdak;jkdjgsd??
        }
        speedyToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_YOUR_ACTION, entityPlayerMP, (byte)0);
        sourceWorldFragment = new WorldFragment(sourceVoxelSelection.getxSize(), sourceVoxelSelection.getySize(), sourceVoxelSelection.getzSize());
        AsynchronousToken token = sourceWorldFragment.readFromWorldAsynchronous(worldServer,
                                                           sourceVoxelSelection.getWxOrigin(), sourceVoxelSelection.getWyOrigin(), sourceVoxelSelection.getWzOrigin(),
                                                           sourceVoxelSelection);
        currentStage = ActionStage.READ;
        setSubTask(token, currentStage.durationWeight);
        break;
      }
      case READ: {
        if (!executeSubTask()) break;
        AsynchronousToken token = worldHistory.writeToWorldWithUndoAsynchronous(entityPlayerMP, worldServer, sourceWorldFragment, xpos, ypos, zpos, quadOrientation);
        currentStage = ActionStage.WRITE;
        if (token != null) {
          setSubTask(token, currentStage.durationWeight);
        }
        break;
      }
      case WRITE: {
        if (!executeSubTask()) break;
        currentStage = ActionStage.COMPLETE;
        completed = true;
        speedyToolsNetworkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
        sourceWorldFragment = null;
        sourceVoxelSelection = null;
        speedyToolsNetworkServer.actionCompleted(entityPlayerMP, sequenceNumber);
//        for (ActionStage actionStage : ActionStage.values()) {
//          System.out.println(actionStage + ":" + ticksPerStage.get(actionStage) + " -> " + milliSecondsPerStage.get(actionStage));
//        }
        break;
      }
      case COMPLETE: { // do nothing
        break;
      }
      default: {
        assert false : "Invalid currentStage : " + currentStage;
      }
    }
    if (!completed) {
      double elapsedMS = (System.nanoTime() - timeIn) / 1.0E6;
//      ticksPerStage.put(entryStage, 1 + ticksPerStage.get(entryStage));
//      milliSecondsPerStage.put(entryStage, milliSecondsPerStage.get(entryStage) + elapsedMS);
//      System.out.println(getFractionComplete());
      speedyToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_YOUR_ACTION, entityPlayerMP,
                                                  (byte) (100 * getFractionComplete()));
    }
  }

  public enum ActionStage {
    SETUP(0.0), READ(0.3), WRITE(0.7), COMPLETE(0.0);
    ActionStage(double i_durationWeight) {durationWeight = i_durationWeight;}
    public double durationWeight;
  }

  private int toolID;
  private int xpos;
  private int ypos;
  private int zpos;
  private QuadOrientation quadOrientation;
  private ActionStage currentStage;
  WorldFragment sourceWorldFragment;
  VoxelSelectionWithOrigin sourceVoxelSelection;

//  EnumMap<ActionStage, Integer> ticksPerStage = new EnumMap<ActionStage, Integer>(ActionStage.class);
//  EnumMap<ActionStage, Double> milliSecondsPerStage = new EnumMap<ActionStage, Double>(ActionStage.class);
}
