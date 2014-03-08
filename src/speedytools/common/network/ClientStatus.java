package speedytools.common.network;

import speedytools.common.CloneToolActionStatus;

/**
* User: The Grey Ghost
* Date: 8/03/14
*/
public enum ClientStatus
{
  IDLE, MONITORING_STATUS, WAITING_FOR_ACTION_COMPLETE;

  public static final ClientStatus[] allValues = {IDLE, MONITORING_STATUS, WAITING_FOR_ACTION_COMPLETE};
}
