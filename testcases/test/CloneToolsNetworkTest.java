package test;

import com.sun.corba.se.impl.oa.toa.TOA;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;
import junit.framework.Assert;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.NetClientHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import speedytools.clientside.ClientSide;
import speedytools.clientside.CloneToolsNetworkClient;
import speedytools.common.network.*;
import speedytools.serverside.CloneToolServerActions;
import speedytools.serverside.CloneToolsNetworkServer;
import speedytools.serverside.ServerSide;
import speedytools.serverside.SpeedyToolWorldManipulator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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
 * testSelectionMade
 * (1) Send an informSelectionMade to the server
 * (2) verify that CloneToolServerActions was called
 * (3) verify that the client receives first PERFORMING_BACKUP then IDLE
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
      ++sequenceNumber;
    }

    public void addReceivedPacket(int packetSequenceNumber, Packet packet)
    {
      receivedPackets.put(packetSequenceNumber, packet);
    }

    public boolean processPacket()
    {
      if (receivedPackets.isEmpty()) return false;
      Map.Entry<Integer, Packet> nextPacket = receivedPackets.pollFirstEntry();
      stubPacketHandler.onPacketData(myName, (Packet250CustomPayload)nextPacket.getValue(), stubEntityClientPlayerMP.get(myName));
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
      ++sequenceNumber;
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
      lastActionSequenceNumber = actionSequenceNumber;
      ++countPerformUndoOfCurrentAction;
//      System.out.println("Server: Tool Undo Current Action received: sequenceNumber " + actionSequenceNumber);
      return true;
    }

    public boolean performUndoOfLastAction(EntityPlayerMP player, int undoSequenceNumber)
    {
      cloneToolsNetworkServer.changeServerStatus(ServerStatus.UNDOING_YOUR_ACTION, player, (byte)0);
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
    public int lastToolID = 0;
    public int lastXpos = 0;
    public int lastYpos = 0;
    public int lastZpos = 0;
    public Byte lastRotationCount = null;
    public Boolean lastFlipped = null;
  }

  public static class StubPacketHandler {
    public void onPacketData(String playerName, Packet250CustomPayload packet, Player playerEntity)
    {
      if (packet.channel.equals("speedytools")) {
        Side side = (playerEntity instanceof EntityPlayerMP) ? Side.SERVER : Side.CLIENT;

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
                networkServer.handlePacket((EntityPlayerMP)playerEntity, toolUsePacket);
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
            Packet250CloneToolAcknowledge toolAcknowledgePacket = Packet250CloneToolAcknowledge.createPacket250CloneToolUse(packet);
            if (toolAcknowledgePacket != null && toolAcknowledgePacket.validForSide(side)) {
              if (side == Side.CLIENT) {
                networkClients.get(playerName).handlePacket((EntityClientPlayerMP)playerEntity, toolAcknowledgePacket);
              }
            } else {
              malformedPacketError(side, playerEntity, "PACKET250_TOOL_ACKNOWLEDGE_ID received on wrong side");
              return;
            }
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
  }

}
