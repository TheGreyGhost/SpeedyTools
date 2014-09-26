package speedytools.common.network;

/**
 * User: The Grey Ghost
 * Date: 13/04/14
 */
public enum Packet250Types
{
  PACKET250_SPEEDY_TOOL_USE_ID(0),
  PACKET250_CLONE_TOOL_USE_ID(1),
  PACKET250_TOOL_STATUS_ID(2),
  PACKET250_TOOL_ACKNOWLEDGE_ID(3),
  PACKET250_SELECTION_PACKET(4),
  PACKET250_SELECTION_PACKET_ACKNOWLEDGE(5),
  PACKET250_INGAME_TESTER(6),
  PACKET250_SERVER_SELECTION_GENERATION(7);

  public byte getPacketTypeID() {return packetTypeID;}
  public static Packet250Types byteToPacket250Type(byte value)
  {
    for (Packet250Types packetType : Packet250Types.values()) {
      if (value == packetType.getPacketTypeID()) return packetType;
    }
    return null;
  }

  /**
   * returns the paired packet corresponding to this packet (cf REQUEST and REPLY)
   */
  public Packet250Types getPairedType()
  {
    switch(this) {
      case PACKET250_SELECTION_PACKET: { return PACKET250_SELECTION_PACKET_ACKNOWLEDGE; }
      case PACKET250_SELECTION_PACKET_ACKNOWLEDGE: {return PACKET250_SELECTION_PACKET;}
      default: { return null;}
    }
  }

  private Packet250Types(int i_packetTypeID) {
    packetTypeID = (byte)i_packetTypeID;
  }
  private final byte packetTypeID;
}
