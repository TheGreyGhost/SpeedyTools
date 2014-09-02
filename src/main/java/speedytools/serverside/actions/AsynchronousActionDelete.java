//package speedytools.serverside.actions;
//
//import net.minecraft.entity.player.EntityPlayerMP;
//import net.minecraft.world.WorldServer;
//import speedytools.common.selections.VoxelSelectionWithOrigin;
//import speedytools.common.utilities.QuadOrientation;
//import speedytools.serverside.worldmanipulation.*;
//
///**
// * User: The Grey Ghost
// * Date: 3/08/2014
// * Deletes the selection (overwrites it with air)
// */
//public class AsynchronousActionDelete extends AsynchronousActionBase
//{
//  public AsynchronousActionDelete(WorldServer i_worldServer, EntityPlayerMP i_player, WorldHistory i_worldHistory,
//                                  VoxelSelectionWithOrigin i_voxelSelection,
//                                  int i_sequenceNumber, int i_toolID, int i_xpos, int i_ypos, int i_zpos, QuadOrientation i_quadOrientation)
//  {
//    super(i_worldServer, i_player, i_worldHistory, i_sequenceNumber);
//    sourceVoxelSelection = i_voxelSelection;
//    toolID = i_toolID;
//    xpos = i_xpos;
//    ypos = i_ypos;
//    zpos = i_zpos;
//    quadOrientation = i_quadOrientation;
//    currentStage = ActionStage.SETUP;
////    for (ActionStage actionStage : ActionStage.values()) {
////      ticksPerStage.put(actionStage, 0);
////      milliSecondsPerStage.put(actionStage, 0.0);
////    }
//  }
//
//  @Override
//  public void continueProcessing() {
//    if (aborting) {
//      continueAborting();
//      return;
//    }
//    if (rollingBack) {
//      continueRollingBack();
//      return;
//    }
//    long timeIn = System.nanoTime();
////    ActionStage entryStage = currentStage;
//    switch (currentStage) {
//      case SETUP: {
//        sourceWorldFragment = new WorldFragment(sourceVoxelSelection.getxSize(), sourceVoxelSelection.getySize(), sourceVoxelSelection.getzSize());
//        AsynchronousToken token = sourceWorldFragment.readFromWorldAsynchronous(new WorldServerReaderAllAir(worldServer),
//                                                                               sourceVoxelSelection.getWxOrigin(), sourceVoxelSelection.getWyOrigin(), sourceVoxelSelection.getWzOrigin(),
//                                                                               sourceVoxelSelection);
//        currentStage = ActionStage.READ;
//        setSubTask(token, currentStage.durationWeight, false);
//        break;
//      }
//      case READ: {
//        if (!executeSubTask()) break;
//        AsynchronousToken token = worldHistory.writeToWorldWithUndoAsynchronous(entityPlayerMP, worldServer, sourceWorldFragment, xpos, ypos, zpos, quadOrientation, getUniqueTokenID());
//        currentStage = ActionStage.WRITE;
//        if (token != null) {
//          setSubTask(token, currentStage.durationWeight, false);
//        }
//        break;
//      }
//      case WRITE: {
//        if (!executeSubTask()) break;
//        currentStage = ActionStage.COMPLETE;
////        for (ActionStage actionStage : ActionStage.values()) {
////          System.out.println(actionStage + ":" + ticksPerStage.get(actionStage) + " -> " + milliSecondsPerStage.get(actionStage));
////        }
//        break;
//      }
//      case COMPLETE: { // do nothing
//        if (!completed) {
//          sourceWorldFragment = null;
//          sourceVoxelSelection = null;
//          completed = true;
//        }
//        break;
//      }
//      default: {
//        assert false : "Invalid currentStage : " + currentStage;
//      }
//    }
//  }
//
//  private void continueAborting() {
//    switch (currentStage) {
//      case SETUP: {
//        currentStage = ActionStage.COMPLETE;
//        break;
//      }
//      case READ: {
//        if (executeAbortSubTask()) break;
//        currentStage = ActionStage.COMPLETE;
//        break;
//      }
//      case WRITE: {
//        if (executeAbortSubTask()) break;
//        currentStage = ActionStage.COMPLETE;
//
////        for (ActionStage actionStage : ActionStage.values()) {
////          System.out.println(actionStage + ":" + ticksPerStage.get(actionStage) + " -> " + milliSecondsPerStage.get(actionStage));
////        }
//        break;
//      }
//      case ROLLBACK: {
//        if (executeAbortSubTask()) break;
//        currentStage = ActionStage.COMPLETE;
//        break;
//      }
//      case COMPLETE: { // do nothing
//        if (!completed) {
//          sourceWorldFragment = null;
//          sourceVoxelSelection = null;
//          completed = true;
//        }
//        break;
//      }
//      default: {
//        assert false : "Invalid currentStage : " + currentStage;
//      }
//    }
//  }
//
//  private void continueRollingBack() {
//    if (startRollback && currentStage == ActionStage.COMPLETE) {  // force a rollback even if we have finished the task
//      currentStage = ActionStage.WRITE;
//    }
//    startRollback = false;
//    switch (currentStage) {
//      case SETUP: {
//        currentStage = ActionStage.COMPLETE;
//        break;
//      }
//      case READ: {
//        if (!executeAbortSubTask()) break;
//        currentStage = ActionStage.COMPLETE;
//        break;
//      }
//      case WRITE: {
//        if (!executeAbortSubTask()) break;
//        AsynchronousToken token = worldHistory.performComplexUndoAsynchronous(entityPlayerMP, worldServer, getUniqueTokenID());  // rollback the placement we just completed.
//        if (token == null ) {
//          currentStage = ActionStage.COMPLETE;
//        } else {
//          setSubTask(token, currentStage.durationWeight, true);
//          currentStage = ActionStage.ROLLBACK;
//        }
//        break;
//      }
//      case ROLLBACK: {
//        if (!executeSubTask()) break;
//        currentStage = ActionStage.COMPLETE;
//        break;
//      }
//      case COMPLETE: { // do nothing
//        if (!completed) {
//          sourceWorldFragment = null;
//          sourceVoxelSelection = null;
//          completed = true;
//        }
//        break;
//      }
//      default: {
//        assert false : "Invalid currentStage : " + currentStage;
//      }
//    }
//  }
//
//
//  public enum ActionStage {
//    SETUP(0.0), READ(0.3), WRITE(0.7), COMPLETE(0.0), ROLLBACK(1.0);
//    ActionStage(double i_durationWeight) {durationWeight = i_durationWeight;}
//    public double durationWeight;
//  }
//
//  private int toolID;
//  private int xpos;
//  private int ypos;
//  private int zpos;
//  private QuadOrientation quadOrientation;
//  private ActionStage currentStage;
//  WorldFragment sourceWorldFragment;
//  VoxelSelectionWithOrigin sourceVoxelSelection;
//
//
////  EnumMap<ActionStage, Integer> ticksPerStage = new EnumMap<ActionStage, Integer>(ActionStage.class);
////  EnumMap<ActionStage, Double> milliSecondsPerStage = new EnumMap<ActionStage, Double>(ActionStage.class);
//}
