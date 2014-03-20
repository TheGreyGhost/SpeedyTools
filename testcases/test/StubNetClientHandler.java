package test;

import net.minecraft.client.multiplayer.NetClientHandler;
import net.minecraft.network.packet.Packet;

import java.io.IOException;

/**
 * User: The Grey Ghost
 * Date: 21/03/14
 */
public class StubNetClientHandler extends NetClientHandler
{
  public StubNetClientHandler()  throws IOException
  {
    super(null, null, 0);
  }
  public void setupStub()
  {

  }

  public void addToSendQueue(Packet par1Packet)
  {
    System.out.print("Got a packet");
  }
}
