package test;

import cpw.mods.fml.relauncher.FMLRelaunchLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.util.Session;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;
import speedytools.clientside.CloneToolsNetworkClient;
import speedytools.common.network.ClientStatus;

import java.io.File;
import java.net.Proxy;
import java.util.logging.Level;

/**
 * User: The Grey Ghost
 * Date: 20/03/14
 * This class tests for correct communication between CloneToolsNetworkClient and CloneToolsNetworkServer.
 * The tests are based around the networkprotocols.txt specification.  It uses dummy objects to simulate network communications:
 * EntityClientPlayerMP, EntityPlayerMP, NetClientHandler, NetServerHandler
 *
 */
public class CloneToolsNetworkTest
{
   public static StubEntityClientPlayerMP stubEntityClientPlayerMP;

  @BeforeClass
  public static void setUp() throws Exception {
    Objenesis objenesis = new ObjenesisStd();
    stubEntityClientPlayerMP = (StubEntityClientPlayerMP) objenesis.newInstance(StubEntityClientPlayerMP.class);
    stubEntityClientPlayerMP.setupStub();
  }

    @Test
  public void testTick() throws Exception {
    CloneToolsNetworkClient client = new CloneToolsNetworkClient();
    client.connectedToServer(stubEntityClientPlayerMP);
    client.changeClientStatus(ClientStatus.MONITORING_STATUS);

  }

}
