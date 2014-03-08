package speedytools.common.network;

import speedytools.common.CloneToolActionStatus;

/**
* User: The Grey Ghost
* Date: 8/03/14
*/
public enum ServerStatus
{
  IDLE, PERFORMING_BACKUP, PERFORMING_YOUR_ACTION, UNDOING_YOUR_ACTION, BUSY_WITH_OTHER_PLAYER;

  public static final ServerStatus[] allValues = {IDLE, PERFORMING_BACKUP, PERFORMING_YOUR_ACTION, UNDOING_YOUR_ACTION, BUSY_WITH_OTHER_PLAYER};
}
