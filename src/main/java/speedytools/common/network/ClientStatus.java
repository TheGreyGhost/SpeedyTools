package speedytools.common.network;

/**
* User: The Grey Ghost
* Date: 8/03/14
*/
public enum ClientStatus
{
  IDLE(0), MONITORING_STATUS(1), WAITING_FOR_ACTION_COMPLETE(2);

  public byte getStatusID() {return statusID;}

  public static ClientStatus byteToCommand(byte value)
  {
    for (ClientStatus clientStatus : ClientStatus.values()) {
      if (value == clientStatus.getStatusID()) return clientStatus;
    }
    return null;
  }

  private ClientStatus(int i_statusID) {
    statusID = (byte)i_statusID;
  }
  private final byte statusID;
}
