package test;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.util.Session;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

/**
 * User: The Grey Ghost
 * Date: 20/03/14
 */
public class StubEntityClientPlayerMP extends EntityClientPlayerMP
{
  public StubEntityClientPlayerMP()
  {
    super(null, null, null, null);
  }

  public void setupStub()
  {
    Objenesis objenesis = new ObjenesisStd();
    StubNetClientHandler stubNetClientHandler = (StubNetClientHandler) objenesis.newInstance(StubNetClientHandler.class);
    stubNetClientHandler.setupStub();
    sendQueue = stubNetClientHandler;
  }
}
