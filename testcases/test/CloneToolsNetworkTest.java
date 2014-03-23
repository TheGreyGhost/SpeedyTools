package test;

import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;
import junit.framework.Assert;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.NetClientHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import speedytools.clientside.CloneToolsNetworkClient;
import speedytools.common.network.*;
import speedytools.serverside.CloneToolServerActions;
import speedytools.serverside.CloneToolsNetworkServer;

import java.io.IOException;
import java.util.*;

/**
 * User: The Grey Ghost
 * Date: 20/03/14
 * This class tests for correct communication between CloneToolsNetworkClient and CloneToolsNetworkServer.  The classes tested are
 * CloneToolsNetworkClient, CloneToolsNetworkServer, Packet250CloneToolAcknowledge, Packet250CloneToolStatus, Packet250CloneToolUse,
 *   , ClientStatus, ServerStatus
 * The tests are based around the networkprotocols.txt specification.  It uses dummy objects to simulate network communications:
 * EntityClientPlayerMP, EntityPlayerMP, NetClientHandler, NetServerHandler.  PacketHandler is bypassed (not used)
 *
 * Test plan:
 * testStatus
 * (1) Set up multiple clients, add to server
 * (2) Change Client Status in various sequence and verify it is reflected in Server in correct clients
 * (3) Change the server status and verify that only the interested clients receive an update
 * (4) Change the server status and verify that the clients get the correct status (eg "busy with other player")
 * (5) tick the server many times in rapid succession to verify that it sends updates about once per second
 * (6) tick the client to verify it sends status update requests if server response not received within timeout
 *
 * testSelectionMade
 * (1) Send an informSelectionMade to the server
 * (2) verify that CloneToolServerActions was called
 * (3) verify that the client receives first PERFORMING_BACKUP then IDLE
 *
 * testPerformToolAction
 * (1) performToolAction - check that toolID etc transmitted correctly
 *     check that peekCurrentActionStatus and getCurrentActionStatus updates correctly; similarly for UndoStatus
 *     verify that server has updated client status appropriately
 *     verify that client receives acknowledgement
 *     actionCompleted() sends appropriate message
 * (2) performToolAction when server is busy (remote status, not local status)
 * (3) performToolAction when client is still waiting for response (wait ack, processing) and verify refused
 * (4) TimeOut resend if client waiting too long for an acknowledgement packet
 * (5) Client receives an old packet acknowledgement and ignores it
 * (6) Server receives duplicate packet, sends acknowledgement again
 * (7) Server receives old packet, ignores it.
 *
 * testPerformUndo
 * (1) perform action then undo before getting completed ack;
 *     check that the client status updates properly
 *     check that the server calls the undo with the correct sequence number for the right player
 *     check that the client receives the correct status back
 *     advance server to complete, check client ok
 * (2) perform action then undo after getting completed ack
 *     check that server calls the last completed undo for the right player
 *     check that the undo receives "ACKNOWLEDGE"
 * (3) verify that a busy server rejects the undo request
 * (4) send action then undo out of sequence and verify that the server replies "reject" to the action.
 *     send the same action again, verify it receives "reject" packet again but client ignores it
 * (5) send old undo request and verify ignored by the server
 * (6) verify client sends undo packet again after timeout.
 *
 */

public class CloneToolsNetworkTest
{
  // objects on client side (one per machine)
  public static Map<String, StubEntityClientPlayerMP> stubEntityClientPlayerMP = new HashMap<String, StubEntityClientPlayerMP>();
  public static Map<String, StubNetClientHandler> stubNetClientHandler = new HashMap<String, StubNetClientHandler>();
  public static Map<String, CloneToolsNetworkClient> networkClients = new HashMap<String, CloneToolsNetworkClient>();

  // objects on server side (all on the same server machine)
  public static Map<String, StubNetServerHandler> stubNetServerHandler = new HashMap<String, StubNetServerHandler>();
  public static Map<String, StubEntityPlayerMP> stubEntityPlayerMP = new HashMap<String, StubEntityPlayerMP>();
  public static StubCloneToolServerActions stubCloneToolServerActions;
  public static CloneToolsNetworkServer networkServer;
  public static StubPacketHandler stubPacketHandler;
  public static ArrayList<String> names = new ArrayList<String>();

  public static final int NUMBER_OF_CLIENTS = 5;

  @Before
  public void setUp() throws Exception {
    Objenesis objenesis = new ObjenesisStd();

    stubCloneToolServerActions = new StubCloneToolServerActions();
    networkServer = new CloneToolsNetworkServer(stubCloneToolServerActions);
    stubCloneToolServerActions.setupStub(networkServer);
    stubPacketHandler = new StubPacketHandler();

    for (int i = 0; i < NUMBER_OF_CLIENTS; ++i) {
      String name = "Player"+i;
      names.add(name);
      stubNetServerHandler.put(name, (StubNetServerHandler)objenesis.newInstance(StubNetServerHandler.class));
      stubNetClientHandler.put(name, (StubNetClientHandler)objenesis.newInstance(StubNetClientHandler.class));

      stubEntityClientPlayerMP.put(name, (StubEntityClientPlayerMP) objenesis.newInstance(StubEntityClientPlayerMP.class));
      stubEntityPlayerMP.put(name, (StubEntityPlayerMP)objenesis.newInstance(StubEntityPlayerMP.class));

      stubNetClientHandler.get(name).setupStub(name, stubEntityClientPlayerMP.get(name));
      stubNetServerHandler.get(name).setupStub(name, stubEntityPlayerMP.get(name));
      stubEntityClientPlayerMP.get(name).setupStub(name, stubNetClientHandler.get(name));
      stubEntityPlayerMP.get(name).setupStub(name, stubNetServerHandler.get(name));
    }
  }

  @After
  public void tearDown() throws Exception {
     stubEntityClientPlayerMP.clear();
     stubNetClientHandler.clear();
     networkClients.clear();
    stubNetServerHandler.clear();
     stubEntityPlayerMP.clear();
    stubCloneToolServerActions = null;
     networkServer = null;
  }

  public void runTests() throws  Exception
  {
    testStatus();
    tearDown();
    setUp();
    testSelectionMade();
  }

  @Test
  public void testStatus() throws Exception {
    boolean result;
    for (String name : names) {
      networkClients.put(name, new CloneToolsNetworkClient());
      networkClients.get(name).connectedToServer(stubEntityClientPlayerMP.get(name));

      networkServer.addPlayer(stubEntityPlayerMP.get(name));
    }

    // all clients initialise to idle
    for (CloneToolsNetworkClient client : networkClients.values()) {
      Assert.assertTrue("Clients all say server idle", client.getServerStatus() == ServerStatus.IDLE);
    }

    // changing server has no effect for idle clients
    final byte TEST_PROGRESS = 50;
    networkServer.changeServerStatus(ServerStatus.PERFORMING_BACKUP, null, TEST_PROGRESS);
    result = processAllClients();
    Assert.assertFalse("No clients received any Packets", result);

    // client changes its status to interested and receives the correct server status back
    CloneToolsNetworkClient testClient = networkClients.get(names.get(0));
    testClient.changeClientStatus(ClientStatus.MONITORING_STATUS);
    result = processAllServers();
    Assert.assertTrue("Server received At Least One Packet", result);
    result = processAllClients();
    Assert.assertTrue("Client received At Least One Packet", result);
    for (CloneToolsNetworkClient client : networkClients.values()) {
      if (client == testClient) {
        Assert.assertTrue("Test client updated to backup", client.getServerStatus() == ServerStatus.PERFORMING_BACKUP);
        Assert.assertTrue("Test client correct progress", client.getServerPercentComplete() == TEST_PROGRESS);
      } else {
        Assert.assertTrue("All non-test clients not updated", client.getServerStatus() == ServerStatus.IDLE);
      }
    }

    // changing a second client's status to interested also receives the correct server status back
    CloneToolsNetworkClient testClient2 = networkClients.get(names.get(2));
    testClient2.changeClientStatus(ClientStatus.WAITING_FOR_ACTION_COMPLETE);
    result = processAllServers();
    Assert.assertTrue("Server received At Least One Packet", result);
    result = processAllClients();
    Assert.assertTrue("Client received At Least One Packet", result);
    for (CloneToolsNetworkClient client : networkClients.values()) {
      if (client == testClient || client == testClient2) {
        Assert.assertTrue("Test client updated to backup", client.getServerStatus() == ServerStatus.PERFORMING_BACKUP);
        Assert.assertTrue("Test client correct progress", client.getServerPercentComplete() == TEST_PROGRESS);
      } else {
        Assert.assertTrue("All non-test clients not updated", client.getServerStatus() == ServerStatus.IDLE);
      }
    }

    final byte TEST_PROGRESS2 = 85;
    // changing the server to PERFORMING ACTION now updates both clients, client 1 and 2 get the correct personalised status.
    networkServer.changeServerStatus(ServerStatus.PERFORMING_YOUR_ACTION, stubEntityPlayerMP.get(names.get(2)), TEST_PROGRESS2);
    result = processAllClients();
    Assert.assertTrue("Client received At Least One Packet", result);
    for (CloneToolsNetworkClient client : networkClients.values()) {
      if (client == testClient) {
        Assert.assertTrue("Test client updated to BUSY_WITH_OTHER_PLAYER", client.getServerStatus() == ServerStatus.BUSY_WITH_OTHER_PLAYER);
        Assert.assertTrue("Test client correct progress", client.getServerPercentComplete() == TEST_PROGRESS2);
      } else if (client == testClient2) {
          Assert.assertTrue("Test client2 updated to PERFORMING_YOUR_ACTION", client.getServerStatus() == ServerStatus.PERFORMING_YOUR_ACTION);
          Assert.assertTrue("Test client2 correct progress", client.getServerPercentComplete() == TEST_PROGRESS2);
      } else {
        Assert.assertTrue("All non-test clients not updated", client.getServerStatus() == ServerStatus.IDLE);
      }
    }

    // changing the server now updates both clients, performing the action for Client 2.
    networkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, stubEntityPlayerMP.get(names.get(0)), TEST_PROGRESS);
    result = processAllClients();
    Assert.assertTrue("Client received At Least One Packet", result);
    for (CloneToolsNetworkClient client : networkClients.values()) {
      if (client == testClient) {
        Assert.assertTrue("Test client updated to UNDOING_YOUR_ACTION", client.getServerStatus() == ServerStatus.UNDOING_YOUR_ACTION);
        Assert.assertTrue("Test client correct progress", client.getServerPercentComplete() == TEST_PROGRESS);
      } else if (client == testClient2) {
        Assert.assertTrue("Test client2 updated to BUSY_WITH_OTHER_PLAYER", client.getServerStatus() == ServerStatus.BUSY_WITH_OTHER_PLAYER);
        Assert.assertTrue("Test client2 correct progress", client.getServerPercentComplete() == TEST_PROGRESS);
      } else {
        Assert.assertTrue("All non-test clients not updated", client.getServerStatus() == ServerStatus.IDLE);
      }
    }

    // verify that the server.tick() sends periodic updates.
    // Test code loops for 2.5 s, so assumes that the delay is no more than approx 2 seconds.
    // If more than 10 updates are sent, it assumes there is a problem with too-frequent updates.
    final int DISABLE_SENDING = -2;
    stubNetServerHandler.get(names.get(0)).setSequenceNumber(DISABLE_SENDING);
    networkServer.changeServerStatus(ServerStatus.PERFORMING_BACKUP, stubEntityPlayerMP.get(names.get(0)), TEST_PROGRESS);
    stubNetServerHandler.get(names.get(0)).setSequenceNumber(0);
    processAllClients();
    Assert.assertTrue("Status not yet changed", testClient.getServerStatus() != ServerStatus.PERFORMING_BACKUP);
    final long MAX_TIMEOUT_MS = 2500;
    long finishTime = System.nanoTime() + MAX_TIMEOUT_MS * 1000 * 1000;

    int countReceived = 0;
    while (countReceived < 10 && System.nanoTime() < finishTime) {
      networkServer.tick();
      if (processAllClients()) ++countReceived;
      Thread.sleep(50);
    }
    Assert.assertTrue("Received at least one update", countReceived > 0);
    Assert.assertTrue("Received not too many updates", countReceived < 10);
    Assert.assertTrue("Client status changed correctly", testClient.getServerStatus() == ServerStatus.PERFORMING_BACKUP);

    // test for client automatic request for status updates if packets late
    testClient.changeClientStatus(ClientStatus.MONITORING_STATUS);
    while (processAllServers());
    finishTime = System.nanoTime() + MAX_TIMEOUT_MS * 1000 * 1000;

    countReceived = 0;
    while (countReceived < 10 && System.nanoTime() < finishTime) {
      testClient.tick();
      if (processAllServers()) ++countReceived;
      Thread.sleep(50);
    }
    Assert.assertTrue("Received at least one update", countReceived > 0);
    Assert.assertTrue("Received not too many updates", countReceived < 10);
    Assert.assertTrue("Client status changed correctly", testClient.getServerStatus() == ServerStatus.PERFORMING_BACKUP);
  }

  @Test
  public void testSelectionMade() throws Exception {
    boolean result;
    for (String name : names) {
      networkClients.put(name, new CloneToolsNetworkClient());
      networkClients.get(name).connectedToServer(stubEntityClientPlayerMP.get(name));

      networkServer.addPlayer(stubEntityPlayerMP.get(name));
    }

    // client changes its status to interested, performs an inform and gets the correct statuses back:
    // first PERFORMING_BACKUP then IDLE
    CloneToolsNetworkClient testClient = networkClients.get(names.get(0));
    testClient.changeClientStatus(ClientStatus.MONITORING_STATUS);
    result = processAllServers();
    result = processAllClients();
    result = testClient.informSelectionMade();
    Assert.assertTrue("informSelectionMade ok", result);
    result = processAllServers();
    Assert.assertTrue("Server received at least one packet", result);
    result = processAllClients();
    Assert.assertTrue("Client received first packet", result);
    for (CloneToolsNetworkClient client : networkClients.values()) {
      if (client == testClient) {
        Assert.assertTrue("Test client updated to backup", client.getServerStatus() == ServerStatus.PERFORMING_BACKUP);
      } else {
        Assert.assertTrue("All non-test clients not updated", client.getServerStatus() == ServerStatus.IDLE);
      }
    }
    result = processAllClients();
    Assert.assertTrue("Client received second packet", result);
    for (CloneToolsNetworkClient client : networkClients.values()) {
      Assert.assertTrue("All clients IDLE", client.getServerStatus() == ServerStatus.IDLE);
    }

  }

  @Test
  public void testPerformToolActions() throws Exception {
    boolean result;
    for (String name : names) {
      networkClients.put(name, new CloneToolsNetworkClient());
      networkClients.get(name).connectedToServer(stubEntityClientPlayerMP.get(name));

      networkServer.addPlayer(stubEntityPlayerMP.get(name));
    }
    CloneToolsNetworkClient testClient0 = networkClients.get(names.get(0));

    // test (1)
    result = testClient0.performToolAction(1361, -12345, 35135, 0, (byte)3, false);
    Assert.assertTrue(result);
    result = processAllServers();
    Assert.assertTrue(result);
    result = (stubCloneToolServerActions.lastToolID == 1361) && (stubCloneToolServerActions.lastXpos == -12345) && (stubCloneToolServerActions.lastYpos == 35135) && (stubCloneToolServerActions.lastZpos == 0)
             && (stubCloneToolServerActions.lastRotationCount == 3) && (stubCloneToolServerActions.lastFlipped == false);
    Assert.assertTrue("Action Data correct", result);
    Assert.assertTrue(testClient0.peekCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);
    Assert.assertTrue(testClient0.peekCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    result = processAllClients();
    Assert.assertTrue(result);
    Assert.assertTrue(testClient0.peekCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.PROCESSING);
    Assert.assertTrue(testClient0.peekCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.PROCESSING);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.PROCESSING);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);

    networkServer.actionCompleted(stubEntityPlayerMP.get(names.get(0)), stubCloneToolServerActions.lastActionSequenceNumber);
    result = processAllClients();
    Assert.assertTrue(result);
    Assert.assertTrue(testClient0.peekCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.COMPLETED);
    Assert.assertTrue(testClient0.peekCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.COMPLETED);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);

    // send packet to a different player, verify received and other player not affected
    CloneToolsNetworkClient testClient3 = networkClients.get(names.get(3));
    networkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
    result = processAllClients();

    result = testClient3.performToolAction(161, 12345, -35135, 230, (byte)0, true);
    Assert.assertTrue(result);
    result = processAllServers();
    Assert.assertTrue(result);
    result = (stubCloneToolServerActions.lastToolID == 161) && (stubCloneToolServerActions.lastXpos == 12345) && (stubCloneToolServerActions.lastYpos == -35135) && (stubCloneToolServerActions.lastZpos == 230)
            && (stubCloneToolServerActions.lastRotationCount == 0) && (stubCloneToolServerActions.lastFlipped == true);
    Assert.assertTrue("Action Data correct", result);
    Assert.assertTrue(testClient3.peekCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);
    Assert.assertTrue(testClient0.peekCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    result = processAllClients();
    Assert.assertTrue(result);
    Assert.assertTrue(testClient3.peekCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.PROCESSING);
    Assert.assertTrue(testClient0.peekCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);

    networkServer.actionCompleted(stubEntityPlayerMP.get(names.get(3)), stubCloneToolServerActions.lastActionSequenceNumber);
    result = processAllClients();
    Assert.assertTrue(result);
    Assert.assertTrue(testClient3.peekCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.COMPLETED);
    Assert.assertTrue(testClient0.peekCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Assert.assertTrue(testClient3.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.COMPLETED);
    Assert.assertTrue(testClient3.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);

    // test (2)
    CloneToolsNetworkClient testClient2 = networkClients.get(names.get(2));
    networkServer.changeServerStatus(ServerStatus.PERFORMING_BACKUP, null, (byte)0);  // not communicated to client2
    result = processAllClients();
    result = testClient2.performToolAction(1361, -12345, 35135, 0, (byte) 3, false);
    Assert.assertTrue(result);
    Assert.assertTrue(processAllServers());
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient2.peekCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.REJECTED);
    Assert.assertTrue(testClient2.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.REJECTED);
    Assert.assertTrue(testClient2.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);

    networkServer.changeServerStatus(ServerStatus.PERFORMING_YOUR_ACTION, stubEntityPlayerMP.get(names.get(0)), (byte)0);
    result = processAllClients();
    result = testClient2.performToolAction(1361, -12345, 35135, 0, (byte) 3, false);
    Assert.assertTrue(result);
    result = processAllServers();
    Assert.assertTrue(result);
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient2.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.REJECTED);

    networkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, stubEntityPlayerMP.get(names.get(0)), (byte)0);
    result = processAllClients();
    result = testClient2.performToolAction(1361, -12345, 35135, 0, (byte) 3, false);
    Assert.assertTrue(result);
    result = processAllServers();
    Assert.assertTrue(result);
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient2.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.REJECTED);

    networkServer.changeServerStatus(ServerStatus.PERFORMING_YOUR_ACTION, stubEntityPlayerMP.get(names.get(2)), (byte)0);
    result = processAllClients();
    result = testClient2.performToolAction(1361, -12345, 35135, 0, (byte) 3, false);
    Assert.assertTrue(result);
    result = processAllServers();
    Assert.assertTrue(result);
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient2.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.REJECTED);

    networkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, stubEntityPlayerMP.get(names.get(2)), (byte)0);
    result = processAllClients();
    result = testClient2.performToolAction(1361, -12345, 35135, 0, (byte) 3, false);
    Assert.assertTrue(result);
    result = processAllServers();
    Assert.assertTrue(result);
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient2.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.REJECTED);

    // test (3)
    networkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
    result = testClient0.performToolAction(1361, -12345, 35135, 0, (byte)3, false);
    Assert.assertTrue(result);
    result = processAllServers();
    Assert.assertTrue(result);
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);

    result = testClient0.performToolAction(136, -12345, 3513, 0, (byte)2, false);
    Assert.assertFalse(result);
    result = processAllServers();
    Assert.assertFalse(result);
    result = processAllClients();
    Assert.assertTrue(result);
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.PROCESSING);

    result = testClient0.performToolAction(136, -12345, 3513, 0, (byte)2, false);
    Assert.assertFalse(result);
    Assert.assertFalse(processAllServers());

    networkServer.actionCompleted(stubEntityPlayerMP.get(names.get(0)), stubCloneToolServerActions.lastActionSequenceNumber);
    result = processAllClients();
    Assert.assertTrue(result);
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.COMPLETED);

    // test (4)
    // verify that the client.tick() sends periodic repeats of the packet
    // Test code loops for 2.5 s, so assumes that the delay is no more than approx 2 seconds.
    // If more than 10 updates are sent, it assumes there is a problem with too-frequent updates.
    networkServer.changeServerStatus(ServerStatus.IDLE, stubEntityPlayerMP.get(names.get(0)), (byte)0);
    while (processAllClients());
    final int DISABLE_SENDING = -2;
    stubNetServerHandler.get(names.get(0)).setSequenceNumber(DISABLE_SENDING);
    while (processAllServers());
    result = testClient0.performToolAction(13, -145, 355, 5, (byte)1, true);
    Assert.assertTrue(result);
    Assert.assertTrue(testClient0.peekCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);
    Assert.assertTrue(processAllServers());
    Packet250CustomPayload initialPacket = stubPacketHandler.lastpacketreceived;

    final long MAX_TIMEOUT_MS = 2500;
    long finishTime = System.nanoTime() + MAX_TIMEOUT_MS * 1000 * 1000;

    int countReceived = 0;
    while (countReceived < 10 && System.nanoTime() < finishTime) {
      testClient0.tick();
      if (processAllServers()) {
        ++countReceived;
        Assert.assertTrue(Arrays.equals(initialPacket.data, stubPacketHandler.lastpacketreceived.data));
      }
      Thread.sleep(50);
    }
    Assert.assertTrue("Received at least one update", countReceived > 0);
    Assert.assertTrue("Received not too many updates", countReceived < 10);
    while (processAllClients());
    stubNetServerHandler.get(names.get(0)).setSequenceNumber(0);

    // now advance to PROCESSING and try again
    finishTime = System.nanoTime() + MAX_TIMEOUT_MS * 1000 * 1000;
    do {
      testClient0.tick();
      processAllServers();
      processAllClients();
      Thread.sleep(50);
    } while (System.nanoTime() < finishTime && testClient0.peekCurrentActionStatus() != CloneToolsNetworkClient.ActionStatus.PROCESSING);
    Assert.assertTrue(testClient0.peekCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.PROCESSING);

    stubNetServerHandler.get(names.get(0)).setSequenceNumber(DISABLE_SENDING);
    while (processAllServers());
    while (processAllClients());

    finishTime = System.nanoTime() + MAX_TIMEOUT_MS * 1000 * 1000;

    countReceived = 0;
    while (countReceived < 10 && System.nanoTime() < finishTime) {
      testClient0.tick();
      if (processAllServers()) {
        ++countReceived;
        Assert.assertTrue(Arrays.equals(initialPacket.data, stubPacketHandler.lastpacketreceived.data));
      }
      Thread.sleep(50);
    }
    Assert.assertTrue("Received at least one update", countReceived > 0);
    Assert.assertTrue("Received not too many updates", countReceived < 10);
  }

  @Test
  public void testPerformToolActions5to7() throws Exception {
// test (5)

    boolean result;
    for (String name : names) {
      networkClients.put(name, new CloneToolsNetworkClient());
      networkClients.get(name).connectedToServer(stubEntityClientPlayerMP.get(name));

      networkServer.addPlayer(stubEntityPlayerMP.get(name));
    }
    CloneToolsNetworkClient testClient0 = networkClients.get(names.get(0));

    networkServer.changeServerStatus(ServerStatus.PERFORMING_BACKUP, null, (byte)0);
    while (processAllClients());
    Assert.assertTrue(testClient0.performToolAction(0, 1, 2, 3, (byte)2, false));
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);
    Assert.assertTrue(processAllServers());
    Packet250CustomPayload test7OldClientPacket = stubPacketHandler.lastpacketreceived;
    Assert.assertTrue(processAllClients());
    Packet250CustomPayload oldPacket = stubPacketHandler.lastpacketreceived;
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.REJECTED);

    Assert.assertTrue(testClient0.performToolAction(0, 1, 2, 3, (byte)2, false));
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);
    Assert.assertTrue(processAllServers());
    Packet250CustomPayload test6clientAction = stubPacketHandler.lastpacketreceived;
    stubPacketHandler.onPacketData(names.get(0), oldPacket, stubEntityClientPlayerMP.get(names.get(0)));   // inject old packet to client
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.REJECTED);

    // test (6) - check server sends same ack packet again
    Packet250CustomPayload rejectAcknowledgement = stubPacketHandler.lastpacketreceived;
    stubPacketHandler.onPacketData(names.get(0), test6clientAction, stubEntityPlayerMP.get(names.get(0)));  // inject same packet again to server
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(Arrays.equals(rejectAcknowledgement.data, stubPacketHandler.lastpacketreceived.data));

    // test (7) - server ignores an old packet
    stubPacketHandler.onPacketData(names.get(0), test7OldClientPacket, stubEntityPlayerMP.get(names.get(0)));  // inject same packet again to server
    Assert.assertFalse(processAllClients());
  }

  @Test
  public void testPerformUndo() throws Exception {
    boolean result;
    for (String name : names) {
      networkClients.put(name, new CloneToolsNetworkClient());
      networkClients.get(name).connectedToServer(stubEntityClientPlayerMP.get(name));

      networkServer.addPlayer(stubEntityPlayerMP.get(name));
    }
    CloneToolsNetworkClient testClient0 = networkClients.get(names.get(0));
    StubNetClientHandler testNCH0 = stubNetClientHandler.get(names.get(0));

    // test (1)
    result = testClient0.performToolAction(1361, -12345, 35135, 0, (byte)3, false);
    Assert.assertTrue(result);
    Assert.assertTrue(processAllServers());
    Assert.assertTrue(testClient0.peekCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);
    Assert.assertTrue(testClient0.peekCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    int savedActionSequenceNumber = stubCloneToolServerActions.lastActionSequenceNumber;
    Assert.assertTrue(stubCloneToolServerActions.countPerformToolAction == 1);
    stubCloneToolServerActions.lastActionSequenceNumber = 0;

    Assert.assertFalse(processAllServers());
    Assert.assertTrue(testClient0.performToolUndo());
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);
    Assert.assertTrue(processAllServers());
    Assert.assertTrue(stubCloneToolServerActions.countPerformUndoOfCurrentAction == 1);
    Assert.assertTrue(stubCloneToolServerActions.lastActionSequenceNumber == savedActionSequenceNumber);
    Assert.assertTrue(stubCloneToolServerActions.lastPlayer == stubEntityPlayerMP.get(names.get(0)));
    int savedUndoSeqNumber = stubCloneToolServerActions.lastUndoSequenceNumber;
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.COMPLETED);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.PROCESSING);

    networkServer.undoCompleted(stubEntityPlayerMP.get(names.get(0)), savedUndoSeqNumber);
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.COMPLETED);

    // test (2)
    networkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
    result = testClient0.performToolAction(1361, -12345, 35135, 0, (byte)3, false);
    Assert.assertTrue(result);
    Assert.assertTrue(processAllServers());
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient0.peekCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.PROCESSING);
    Assert.assertTrue(testClient0.peekCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    networkServer.actionCompleted(stubEntityPlayerMP.get(names.get(0)), stubCloneToolServerActions.lastActionSequenceNumber);
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.COMPLETED);

    networkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
    Assert.assertTrue(testClient0.performToolUndo());
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);
    Assert.assertTrue(processAllServers());
    Assert.assertTrue(stubCloneToolServerActions.countPerformUndoOfLastAction == 1);
    savedUndoSeqNumber = stubCloneToolServerActions.lastUndoSequenceNumber;
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.PROCESSING);

    networkServer.undoCompleted(stubEntityPlayerMP.get(names.get(0)), savedUndoSeqNumber);
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.COMPLETED);
    Assert.assertTrue(stubCloneToolServerActions.lastPlayer == stubEntityPlayerMP.get(names.get(0)));

    // test (3)
    networkServer.changeServerStatus(ServerStatus.PERFORMING_BACKUP, null, (byte)0);
    while (processAllClients());
    Assert.assertTrue(testClient0.performToolUndo());
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);
    Assert.assertTrue(processAllServers());
    Packet250CustomPayload test5OldUndo = stubPacketHandler.lastpacketreceived;
    Assert.assertTrue(stubCloneToolServerActions.countPerformUndoOfLastAction == 1);
    Assert.assertTrue(stubCloneToolServerActions.countPerformUndoOfCurrentAction == 1);
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.REJECTED);

    // test (4)  - action received after the corresponding undo
    networkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
    testNCH0.setSequenceNumber(1000);
    result = testClient0.performToolAction(1361, -12345, 35135, 0, (byte)3, false);
    Assert.assertTrue(result);
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);
    testNCH0.setSequenceNumber(100);
    Assert.assertTrue(testClient0.performToolUndo());

    Assert.assertTrue(processAllServers());
    Packet250CustomPayload actionPacket = stubPacketHandler.lastpacketreceived;
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.REJECTED);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.COMPLETED);

    Assert.assertTrue(processAllServers());
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Packet250CustomPayload rejectPacket = stubPacketHandler.lastpacketreceived;

    //     send the same action again, verify it receives "reject" packet again but client ignores it
    Assert.assertFalse(processAllClients());
    stubPacketHandler.onPacketData(names.get(0), actionPacket, stubEntityPlayerMP.get(names.get(0)));  // inject same packet again to server
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(Arrays.equals(rejectPacket.data, stubPacketHandler.lastpacketreceived.data));
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);

    // test (5) - old undo request
    networkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
    result = testClient0.performToolAction(1361, -12345, 35135, 0, (byte)3, false);
    Assert.assertTrue(result);
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);
    Assert.assertTrue(processAllServers());
    Assert.assertTrue(processAllClients());
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.PROCESSING);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    int snap = stubCloneToolServerActions.countPerformUndoOfCurrentAction + stubCloneToolServerActions.countPerformUndoOfLastAction;
    stubPacketHandler.onPacketData(names.get(0), test5OldUndo, stubEntityPlayerMP.get(names.get(0)));  // inject same packet again to server
    Assert.assertTrue(snap == stubCloneToolServerActions.countPerformUndoOfCurrentAction + stubCloneToolServerActions.countPerformUndoOfLastAction);
    Assert.assertFalse(processAllClients());
    Assert.assertTrue(testClient0.getCurrentActionStatus() == CloneToolsNetworkClient.ActionStatus.PROCESSING);
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.NONE_PENDING);
    networkServer.actionCompleted(stubEntityPlayerMP.get(names.get(0)), stubCloneToolServerActions.lastActionSequenceNumber);
    Assert.assertTrue(processAllClients());

    // test (6) - client timeout on response to undo packet - sends request again
    networkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
    final long MAX_TIMEOUT_MS = 2500;
    long finishTime = System.nanoTime() + MAX_TIMEOUT_MS * 1000 * 1000;
    Assert.assertFalse(processAllServers());
    Assert.assertTrue(testClient0.performToolUndo());
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.WAITING_FOR_ACKNOWLEDGEMENT);
    Assert.assertTrue(processAllServers());
    Packet250CustomPayload initialPacket = stubPacketHandler.lastpacketreceived;
    int countReceived = 0;
    while (countReceived < 10 && System.nanoTime() < finishTime) {
      testClient0.tick();
      if (processAllServers()) {
        ++countReceived;
        Assert.assertTrue(Arrays.equals(initialPacket.data, stubPacketHandler.lastpacketreceived.data));
      }
      Thread.sleep(50);
    }
    Assert.assertTrue("Received at least one update", countReceived > 0);
    Assert.assertTrue("Received not too many updates", countReceived < 10);
    // advance to PROCESSING, try again

    Assert.assertTrue(processAllClients());
    while(processAllClients());
    finishTime = System.nanoTime() + MAX_TIMEOUT_MS * 1000 * 1000;
    Assert.assertTrue(testClient0.getCurrentUndoStatus() == CloneToolsNetworkClient.ActionStatus.PROCESSING);
    countReceived = 0;
    while (countReceived < 10 && System.nanoTime() < finishTime) {
      testClient0.tick();
      if (processAllServers()) {
        ++countReceived;
        Assert.assertTrue(Arrays.equals(initialPacket.data, stubPacketHandler.lastpacketreceived.data));
      }
      Thread.sleep(50);
    }
    Assert.assertTrue("Received at least one update", countReceived > 0);
    Assert.assertTrue("Received not too many updates", countReceived < 10);

  }

  public boolean processAllServers()
  {
    boolean atLeastOne = false;
    for (StubNetServerHandler handler : stubNetServerHandler.values()) {
      if (handler.processPacket())
        atLeastOne = true;
    }
    return atLeastOne;
  }

  public boolean processAllClients()
  {
    boolean atLeastOne = false;
    for (StubNetClientHandler handler : stubNetClientHandler.values()) {
      if (handler.processPacket())
        atLeastOne = true;
    }
    return atLeastOne;
  }

  public static class StubEntityClientPlayerMP extends EntityClientPlayerMP implements Player
  {
    public StubEntityClientPlayerMP()
    {
      super(null, null, null, null);
    }

    public void setupStub(String init_name, StubNetClientHandler newStubNetClientHandler)
    {
      myName = init_name;
      sendQueue = newStubNetClientHandler;
      entityId =  ++myNextID;
    }
    String myName;
    static int myNextID = 0;
  }

  public static class StubEntityPlayerMP extends EntityPlayerMP implements Player
  {
    public StubEntityPlayerMP()
    {
      super(null, null, null, null);
    }

    public void setupStub(String init_name, StubNetServerHandler newStubNewServerHandler)
    {
      myName = init_name;
      playerNetServerHandler = newStubNewServerHandler;
      entityId =  ++myNextID;
    }
    String myName;
    static int myNextID = 0;
  }

  /**
   * The client and server stubs send packets to each other directly, with a sequence number to allow for packets
   *   to be received out of order.
   *   Usage:
   *   (1) Create class.
   *   (2) .setupStub
   *   (3) optionally: setSequenceNumber
   *   (4)   addToSendQueue gets called by the test code, optionally multiple times, setSequenceNumber can be changed between calls
   *   (5) call processPacket on the receiving class, multiple times until return false (no more packets received).
   */
  public static class StubNetClientHandler extends NetClientHandler
  {
    public StubNetClientHandler()  throws IOException
    {
      super(null, null, 0);
    }
    public void setupStub(String init_name, StubEntityClientPlayerMP newStubEntityClientPlayerMP)
    {
      myName = init_name;
      myPlayer = newStubEntityClientPlayerMP;
      receivedPackets = new TreeMap<Integer, Packet>();
    }

    public void addToSendQueue(Packet par1Packet)
    {
      if (sequenceNumber < 0) return;

      stubNetServerHandler.get(myName).addReceivedPacket(sequenceNumber, par1Packet);
      sequenceNumber += 100;
    }

    public void addReceivedPacket(int packetSequenceNumber, Packet packet)
    {
      receivedPackets.put(packetSequenceNumber, packet);
    }

    // duplicates the packet with the given sequence number;
    public boolean duplicatePacket(int packetSequenceNumber, int increment)
    {
      if (!receivedPackets.containsKey(packetSequenceNumber)) return false;
      receivedPackets.put(packetSequenceNumber + increment, receivedPackets.get(packetSequenceNumber));
      return true;
    }

    public boolean processPacket()
    {
      if (receivedPackets.isEmpty()) return false;
      Map.Entry<Integer, Packet> nextPacket = receivedPackets.pollFirstEntry();
      stubPacketHandler.onPacketData(myName, (Packet250CustomPayload)nextPacket.getValue(), stubEntityClientPlayerMP.get(myName));
      return true;
    }

    // set the sequence number for the following packets
    //    sequence increases by 100 for every packet sent
    //  negative number causes the packets to be dropped, sequence number doesn't increase
    public void setSequenceNumber(int sequence) {
      this.sequenceNumber = sequence;
    }

    private int sequenceNumber = 0;
    private TreeMap<Integer, Packet> receivedPackets;
    StubEntityClientPlayerMP myPlayer;
    String myName;
  }

  public static class StubNetServerHandler extends NetServerHandler
  {
    public StubNetServerHandler()  throws IOException
    {
      super(null, null, null);
    }
    public void setupStub(String init_name, StubEntityPlayerMP newStubEntityPlayerMP)
    {
      myName = init_name;
      myPlayer = newStubEntityPlayerMP;
      receivedPackets = new TreeMap<Integer, Packet>();
    }

    public void sendPacketToPlayer(Packet par1Packet)
    {
      if (sequenceNumber < 0) return;
      stubNetClientHandler.get(myName).addReceivedPacket(sequenceNumber, par1Packet);
      sequenceNumber += 100;
    }

    // duplicates the packet with the given sequence number;
    public boolean duplicatePacket(int packetSequenceNumber, int increment)
    {
      if (!receivedPackets.containsKey(packetSequenceNumber)) return false;
      receivedPackets.put(packetSequenceNumber + increment, receivedPackets.get(packetSequenceNumber));
      return true;
    }

    public void addReceivedPacket(int packetSequenceNumber, Packet packet)
    {
      receivedPackets.put(packetSequenceNumber, packet);
    }

    public boolean processPacket()
    {
      if (receivedPackets.isEmpty()) return false;
      Map.Entry<Integer, Packet> nextPacket = receivedPackets.pollFirstEntry();
      stubPacketHandler.onPacketData(myName, (Packet250CustomPayload)nextPacket.getValue(), stubEntityPlayerMP.get(myName));
      return true;
    }

    // set the sequence number for the following packets
    //    sequence increases by 1 for every packet sent
    //  negative number causes the packets to be dropped, sequence number doesn't increase
    public void setSequenceNumber(int sequence) {
      this.sequenceNumber = sequence;
    }

    private int sequenceNumber = 0;
    private TreeMap<Integer, Packet> receivedPackets;
    StubEntityPlayerMP myPlayer;
    String myName;
  }

  public static class StubCloneToolServerActions extends CloneToolServerActions
  {
    public void setupStub(CloneToolsNetworkServer newNetworkServer)
    {
      cloneToolsNetworkServer = newNetworkServer;
    }

    public boolean prepareForToolAction(EntityPlayerMP player)
    {
      cloneToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_BACKUP, null, (byte) 0);
      cloneToolsNetworkServer.changeServerStatus(ServerStatus.IDLE, null, (byte)0);
      ++countPrepareForToolAction;
      return true;
    }

    public boolean performToolAction(EntityPlayerMP player, int sequenceNumber, int toolID, int xpos, int ypos, int zpos, byte rotationCount, boolean flipped)
    {
      cloneToolsNetworkServer.changeServerStatus(ServerStatus.PERFORMING_YOUR_ACTION, player, (byte)0);
      lastActionSequenceNumber = sequenceNumber;
      lastToolID = toolID;
      lastXpos = xpos;
      lastYpos = ypos;
      lastZpos = zpos;
      lastRotationCount = rotationCount;
      lastFlipped = flipped;
      ++countPerformToolAction;
//      System.out.println("Server: Tool Action received sequence #" + sequenceNumber + ": tool " + toolID + " at [" + xpos + ", " + ypos + ", " + zpos + "], rotated:" + rotationCount + ", flipped:" + flipped);
      return true;
    }
    public boolean performUndoOfCurrentAction(EntityPlayerMP player, int undoSequenceNumber, int actionSequenceNumber)
    {
      cloneToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, player, (byte)0);
      cloneToolsNetworkServer.actionCompleted(player, actionSequenceNumber);

      lastActionSequenceNumber = actionSequenceNumber;
      lastUndoSequenceNumber = undoSequenceNumber;
      lastPlayer = player;
      ++countPerformUndoOfCurrentAction;
//      System.out.println("Server: Tool Undo Current Action received: sequenceNumber " + actionSequenceNumber);
      return true;
    }

    public boolean performUndoOfLastAction(EntityPlayerMP player, int undoSequenceNumber)
    {
      cloneToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, player, (byte)0);
      lastPlayer = player;
      lastUndoSequenceNumber = undoSequenceNumber;
      ++countPerformUndoOfLastAction;
//      System.out.println("Server: Tool Undo Last Completed Action received ");
      return true;
    }
    private CloneToolsNetworkServer cloneToolsNetworkServer;
    public int countPerformUndoOfLastAction = 0;
    public int countPerformUndoOfCurrentAction = 0;
    public int countPerformToolAction = 0;
    public int countPrepareForToolAction = 0;
    public int lastActionSequenceNumber = 0;
    public int lastUndoSequenceNumber = 0;
    public int lastToolID = 0;
    public int lastXpos = 0;
    public int lastYpos = 0;
    public int lastZpos = 0;
    public Byte lastRotationCount = null;
    public Boolean lastFlipped = null;
    public EntityPlayer lastPlayer = null;
  }

  public static class StubPacketHandler {
    public void onPacketData(String playerName, Packet250CustomPayload packet, Player playerEntity)
    {
      if (packet.channel.equals("speedytools")) {
        Side side = (playerEntity instanceof EntityPlayerMP) ? Side.SERVER : Side.CLIENT;

        lastpacketreceived = packet;
        switch (packet.data[0]) {
/*
          case PacketHandler.PACKET250_SPEEDY_TOOL_USE_ID: {
            if (side != Side.SERVER) {
              malformedPacketError(side, playerEntity, "PACKET250_SPEEDY_TOOL_USE_ID received on wrong side");
              return;
            }
            Packet250SpeedyToolUse toolUsePacket = Packet250SpeedyToolUse.createPacket250SpeedyToolUse(packet);
            if (toolUsePacket == null) return;
            SpeedyToolWorldManipulator manipulator = ServerSide.getSpeedyToolWorldManipulator();
            manipulator.performServerAction(playerEntity, toolUsePacket.getToolItemID(), toolUsePacket.getButton(),
                    toolUsePacket.getBlockToPlace(), toolUsePacket.getCurrentlySelectedBlocks());
            break;
          }
          */
          case PacketHandler.PACKET250_CLONE_TOOL_USE_ID: {
            Packet250CloneToolUse toolUsePacket = Packet250CloneToolUse.createPacket250CloneToolUse(packet);
            if (toolUsePacket != null && toolUsePacket.validForSide(side)) {
              if (side == Side.SERVER) {
                networkServer.handlePacket((EntityPlayerMP) playerEntity, toolUsePacket);
              }
            } else {
              malformedPacketError(side, playerEntity, "PACKET250_CLONE_TOOL_USE_ID received on wrong side");
              return;
            }
            break;

          }
          case PacketHandler.PACKET250_TOOL_STATUS_ID: {
            Packet250CloneToolStatus toolStatusPacket = Packet250CloneToolStatus.createPacket250ToolStatus(packet);
            if (toolStatusPacket != null && toolStatusPacket.validForSide(side)) {
              if (side == Side.SERVER) {
                networkServer.handlePacket((EntityPlayerMP) playerEntity, toolStatusPacket);
              } else {
                networkClients.get(playerName).handlePacket((EntityClientPlayerMP) playerEntity, toolStatusPacket);
              }
            } else {
              malformedPacketError(side, playerEntity, "PACKET250_TOOL_STATUS_ID received on wrong side");
              return;
            }
            break;
          }
          case PacketHandler.PACKET250_TOOL_ACKNOWLEDGE_ID: {
            Packet250CloneToolAcknowledge toolAcknowledgePacket = Packet250CloneToolAcknowledge.createPacket250CloneToolAcknowledge(packet);
            if (toolAcknowledgePacket != null && toolAcknowledgePacket.validForSide(side)) {
              if (side == Side.CLIENT) {
                networkClients.get(playerName).handlePacket((EntityClientPlayerMP)playerEntity, toolAcknowledgePacket);
              }
            } else {
              malformedPacketError(side, playerEntity, "PACKET250_TOOL_ACKNOWLEDGE_ID received on wrong side");
              return;
            }
            break;
          }
          default: {
            malformedPacketError(side, playerEntity, "Malformed Packet250SpeedyTools:Invalid packet type ID");
            return;
          }

        }
      }
    }

    private void malformedPacketError(Side side, Player player, String message) {
      switch (side) {
        case CLIENT: {
          Minecraft.getMinecraft().getLogAgent().logWarning(message);
          break;
        }
        case SERVER: {
          MinecraftServer.getServer().getLogAgent().logWarning(message);
          break;
        }
        default:
          assert false: "invalid Side";
      }
    }
    public Packet250CustomPayload lastpacketreceived = null;
  }

}
