package speedytools.common.network;

/**
* Created by TheGreyGhost on 3/04/14.
* wraps NetClientHandler and NetServerHandler so that we can just send a packet to it without worrying about which side
*/
public interface PacketSender
{
  /**
   * Send a packet to the recipient
   * @param packet
   * @return true if packet could be queued for sending, false if not (eg network overloaded)
   */
  public boolean sendPacket(Packet250Base packet);

  /**
   * Check if the sender is ready for another packet
   * @return true if ready, false if not (eg network is overloaded)
   */
  public boolean readyForAnotherPacket();
}
