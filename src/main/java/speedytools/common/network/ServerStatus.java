package speedytools.common.network;

/**
* User: The Grey Ghost
* Date: 8/03/14
*/
public enum ServerStatus
{
  IDLE(10), PERFORMING_BACKUP(11), PERFORMING_YOUR_ACTION(12), UNDOING_YOUR_ACTION(13), BUSY_WITH_OTHER_PLAYER(14);

  public byte getStatusID() {return statusID;}

  public static ServerStatus byteToCommand(byte value)
  {
    for (ServerStatus serverStatus : ServerStatus.values()) {
      if (value == serverStatus.getStatusID()) return serverStatus;
    }
    return null;
  }

  private ServerStatus(int i_statusID) {
    statusID = (byte)i_statusID;
  }
  private final byte statusID;
}
