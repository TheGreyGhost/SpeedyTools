package speedytools.common.network;


import net.minecraft.network.packet.Packet250CustomPayload;
import speedytools.common.utilities.ErrorLog;

import java.io.*;

/**
 * This class is used to inform the server to perform an in-game automated test
 */
public class Packet250SpeedyIngameTester
{
  /**
   * Packet sent from client to server, to indicate when the user has used the ingame tester tool
   * @param i_whichTest the number of the test to be performed
   */
  public Packet250SpeedyIngameTester(int i_whichTest) throws IOException
  {
    super();

    whichTest = i_whichTest;

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DataOutputStream outputStream = new DataOutputStream(bos);
    outputStream.writeByte(Packet250Types.PACKET250_INGAME_TESTER.getPacketTypeID());
    outputStream.writeInt(whichTest);
    packet250 = new Packet250CustomPayload("speedytools",bos.toByteArray());
  }

  public Packet250CustomPayload getPacket250CustomPayload() {
    return packet250;
  }

  /**
   * Creates a Packet250SpeedyToolUse from Packet250CustomPayload
   * @param sourcePacket250
   * @return the converted packet, or null if failure
   */
  public static Packet250SpeedyIngameTester createPacket250SpeedyIngameTester(Packet250CustomPayload sourcePacket250)
  {
    Packet250SpeedyIngameTester newPacket = new Packet250SpeedyIngameTester();
    newPacket.packet250 = sourcePacket250;
    DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(sourcePacket250.data));

    try {
      byte packetID = inputStream.readByte();
      if (packetID != Packet250Types.PACKET250_INGAME_TESTER.getPacketTypeID()) return null;

      newPacket.whichTest = inputStream.readInt();
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Exception while reading Packet250SpeedyIngameTester: " + ioe);
      return null;
    }
    if (!newPacket.checkInvariants()) return null;
    return newPacket;
  }

  private Packet250SpeedyIngameTester()
  {
  }

  /**
   * Checks if the packet is internally consistent
   * @return true for success, false otherwise
   */
  private boolean checkInvariants()
  {
    return true;
  }

  public int getWhichTest() {
    return whichTest;
  }

  private int whichTest;
  private Packet250CustomPayload packet250 = null;
}
