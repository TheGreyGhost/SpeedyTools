package speedytools.serverside.actions;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import speedytools.common.selections.VoxelSelectionWithOrigin;
import speedytools.common.utilities.QuadOrientation;
import speedytools.serverside.worldmanipulation.AsynchronousToken;
import speedytools.serverside.worldmanipulation.WorldFragment;
import speedytools.serverside.worldmanipulation.WorldHistory;
import speedytools.serverside.worldmanipulation.WorldServerReaderAllAir;

/**
* User: The Grey Ghost
* Date: 3/08/2014
*/
public class AsynchronousActionMove extends AsynchronousActionBase
{
  public AsynchronousActionMove(WorldServer i_worldServer, EntityPlayerMP i_player, WorldHistory i_worldHistory,
                                VoxelSelectionWithOrigin i_voxelSelection,
                                int i_sequenceNumber, int i_toolID, int i_xpos, int i_ypos, int i_zpos, QuadOrientation i_quadOrientation)
  {
    super(i_worldServer, i_player, i_worldHistory, i_sequenceNumber);
    sourceVoxelSelection = i_voxelSelection;
    toolID = i_toolID;
    xpos = i_xpos;
    ypos = i_ypos;
    zpos = i_zpos;
    quadOrientation = i_quadOrientation;
    currentStage = ActionStage.SETUP;
  }

  @Override
  public void continueProcessing() {
    if (aborting) {
      continueAborting();
      return;
    }
    if (rollingBack) {
      continueRollingBack();
      return;
    }
    switch (currentStage) {
      case SETUP: {
        sourceWorldFragment = new WorldFragment(sourceVoxelSelection.getxSize(), sourceVoxelSelection.getySize(), sourceVoxelSelection.getzSize());
        AsynchronousToken token = sourceWorldFragment.readFromWorldAsynchronous(worldServer,
                                                           sourceVoxelSelection.getWxOrigin(), sourceVoxelSelection.getWyOrigin(), sourceVoxelSelection.getWzOrigin(),
                                                           sourceVoxelSelection);
        currentStage = ActionStage.READ;
        setSubTask(token, currentStage.durationWeight, false);
        break;
      }
      case READ: {
        if (!executeSubTask()) break;
        eraseWorldFragment = new WorldFragment(sourceVoxelSelection.getxSize(), sourceVoxelSelection.getySize(), sourceVoxelSelection.getzSize());
        AsynchronousToken token = eraseWorldFragment.readFromWorldAsynchronous(new WorldServerReaderAllAir(worldServer),
                                                                                sourceVoxelSelection.getWxOrigin(), sourceVoxelSelection.getWyOrigin(), sourceVoxelSelection.getWzOrigin(),
                                                                                sourceVoxelSelection);
        currentStage = ActionStage.MAKE_BLANK;
        if (token != null) {
          setSubTask(token, currentStage.durationWeight, false);
        }
        break;
      }
      case MAKE_BLANK: {
        if (!executeSubTask()) break;
        AsynchronousToken token = worldHistory.writeToWorldWithUndoAsynchronous(entityPlayerMP, worldServer, eraseWorldFragment,
                sourceVoxelSelection.getWxOrigin(), sourceVoxelSelection.getWyOrigin(), sourceVoxelSelection.getWzOrigin(),
                quadOrientation, getUniqueTokenID());
        currentStage = ActionStage.ERASE;
        if (token != null) {
          setSubTask(token, currentStage.durationWeight, false);
        }
        break;
      }
      case ERASE: {
        if (!executeSubTask()) break;
        AsynchronousToken token = worldHistory.writeToWorldWithUndoAsynchronous(entityPlayerMP, worldServer, sourceWorldFragment, xpos, ypos, zpos, quadOrientation, getUniqueTokenID());
        currentStage = ActionStage.WRITE;
        if (token != null) {
          setSubTask(token, currentStage.durationWeight, false);
        }
        break;
      }
      case WRITE: {
        if (!executeSubTask()) break;
        currentStage = ActionStage.COMPLETE;
        break;
      }
      case COMPLETE: { // do nothing
        if (!completed) {
          sourceWorldFragment = null;
          sourceVoxelSelection = null;
          eraseWorldFragment = null;
          completed = true;
        }
        break;
      }
      default: {
        assert false : "Invalid currentStage : " + currentStage;
      }
    }
  }

  private void continueAborting() {
    switch (currentStage) {
      case SETUP: {
        currentStage = ActionStage.COMPLETE;
        break;
      }
      case READ:
      case MAKE_BLANK:
      case ERASE:
      case WRITE:
      case ROLLBACK:
      {
        if (executeAbortSubTask()) break;
        currentStage = ActionStage.COMPLETE;
        break;
      }
      case COMPLETE: { // do nothing
        if (!completed) {
          sourceWorldFragment = null;
          sourceVoxelSelection = null;
          eraseWorldFragment = null;
          completed = true;
        }
        break;
      }
      default: {
        assert false : "Invalid currentStage : " + currentStage;
      }
    }
  }

  private void continueRollingBack() {
    if (startRollback && currentStage == ActionStage.COMPLETE) {  // force a rollback even if we have finished the task
      currentStage = ActionStage.WRITE;
    }
    startRollback = false;
    switch (currentStage) {
      case SETUP: {
        currentStage = ActionStage.COMPLETE;
        break;
      }
      case READ:
      case MAKE_BLANK: {
        if (!executeAbortSubTask()) break;
        currentStage = ActionStage.COMPLETE;
        break;
      }
      case ERASE:
      case WRITE: {
        if (!executeAbortSubTask()) break;
        AsynchronousToken token = worldHistory.performComplexUndoAsynchronous(entityPlayerMP, worldServer, getUniqueTokenID());  // rollback the placement we just completed.
        if (token == null ) {
          currentStage = ActionStage.COMPLETE;
        } else {
          setSubTask(token, currentStage.durationWeight, true);
          currentStage = ActionStage.ROLLBACK;
        }
        break;
      }
      case ROLLBACK: {
        if (!executeSubTask()) break;
        AsynchronousToken token = worldHistory.performComplexUndoAsynchronous(entityPlayerMP, worldServer, getUniqueTokenID());  // might need to roll back again
        if (token == null ) {
          currentStage = ActionStage.COMPLETE;
        } else {
          setSubTask(token, currentStage.durationWeight, true);
        }
        break;
      }
      case COMPLETE: { // do nothing
        if (!completed) {
          sourceWorldFragment = null;
          sourceVoxelSelection = null;
          eraseWorldFragment = null;
          completed = true;
        }
        break;
      }
      default: {
        assert false : "Invalid currentStage : " + currentStage;
      }
    }
  }


  public enum ActionStage {
    SETUP(0.0), READ(0.3), MAKE_BLANK(0.1), ERASE(0.3),  WRITE(0.3), COMPLETE(0.0), ROLLBACK(1.0);
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
  WorldFragment eraseWorldFragment;
  VoxelSelectionWithOrigin sourceVoxelSelection;


//  EnumMap<ActionStage, Integer> ticksPerStage = new EnumMap<ActionStage, Integer>(ActionStage.class);
//  EnumMap<ActionStage, Double> milliSecondsPerStage = new EnumMap<ActionStage, Double>(ActionStage.class);
}
