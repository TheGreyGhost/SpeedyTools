//package speedytools.common.network;
//
//import cpw.mods.fml.relauncher.Side;
//import net.minecraft.network.packet.Packet;
//
//import java.io.FileWriter;
//import java.io.IOException;
//import java.nio.file.Path;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.EnumMap;
//
///**
// * User: The Grey Ghost
// * Date: 3/08/2014
// * Used to profile the network traffic
// *
// * Need to modify the vanilla code to log to it, eg
//MemoryConnection::
//public void addToSendQueue(Packet par1Packet) {
// ServerSide.getNetworkTrafficMonitor().logPacket(NetworkTrafficMonitor.PacketLocation.OUTGOING, par1Packet);
//  // .. etc ..
//}
//public void processOrCachePacket(Packet par1Packet) {
// ServerSide.getNetworkTrafficMonitor().logPacket(NetworkTrafficMonitor.PacketLocation.INCOMING, par1Packet);
//}
//
// */
//public class NetworkTrafficMonitor
//{
//  static private final int MAX_PACKET_ID_PLUS_ONE = 256;
//  static private EnumMap<PacketLocation, int []> packetCount = new EnumMap<PacketLocation, int[]>(PacketLocation.class);
//  static private EnumMap<PacketLocation, int []> packetSize = new EnumMap<PacketLocation, int[]>(PacketLocation.class);
//  private final Side whichSide;
//  private  EnumMap<PacketLocation, FileWriter> logFiles = new EnumMap<PacketLocation, FileWriter>(PacketLocation.class);
//
//  public enum PacketLocation {INCOMING, OUTGOING}
//
//  public NetworkTrafficMonitor(Side i_whichSide, Path logFileDirectory, String logfileStem) throws IOException
//  {
//    final boolean SHOULD_APPEND = false;
//    whichSide = i_whichSide;
//    if (logFileDirectory != null) {
//      for (PacketLocation packetLocation : PacketLocation.values()) {
//        String filename = logfileStem + "-" + whichSide.toString() + "-" + packetLocation.toString() + ".log";
//        FileWriter fileWriter = new FileWriter(logFileDirectory.resolve(filename).toString(), SHOULD_APPEND);
//        logFiles.put(packetLocation, fileWriter);
//        fileWriter.write("Time, Count, Size");
//        for (int i = 0; i < MAX_PACKET_ID_PLUS_ONE; ++i) {
//          fileWriter.write("\t" + i);
//        }
//        fileWriter.write("\t");
//        for (int i = 0; i < MAX_PACKET_ID_PLUS_ONE; ++i) {
//          fileWriter.write("\t" + i);
//        }
//        fileWriter.write("\n");
//      }
//    }
//    resetTally();
//  }
//
//  public void resetTally()
//  {
//    for (PacketLocation packetLocation : PacketLocation.values()) {
//      packetCount.put(packetLocation, new int[MAX_PACKET_ID_PLUS_ONE]);
//      packetSize.put(packetLocation, new int[MAX_PACKET_ID_PLUS_ONE]);
//    }
//  }
//
//  public void logPacket(PacketLocation packetLocation, Packet packet)
//  {
//    int packetID = packet.getPacketId();
//    packetCount.get(packetLocation)[packetID]++;
//    packetSize.get(packetLocation)[packetID] += packet.getPacketSize();
//  }
//
//  public void log() throws IOException
//  {
//    Date now = new Date();
//    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yy hh:mm:ss");
//    String nowAsString = simpleDateFormat.format(now);
//
//    for (PacketLocation packetLocation : PacketLocation.values()) {
//      FileWriter fileWriter = logFiles.get(packetLocation);
//      if (fileWriter != null) {
//        fileWriter.write(nowAsString);
//        for (int i = 0; i < MAX_PACKET_ID_PLUS_ONE; ++i) {
//          fileWriter.write("\t" + packetCount.get(packetLocation)[i]);
//        }
//        fileWriter.write("\t");
//        for (int i = 0; i < MAX_PACKET_ID_PLUS_ONE; ++i) {
//          fileWriter.write("\t" + packetSize.get(packetLocation)[i]);
//        }
//        fileWriter.write("\n");
//        fileWriter.flush();
//      }
//    }
//
//  }
//
//  public void closeAll() throws IOException
//  {
//    for (PacketLocation packetLocation : PacketLocation.values()) {
//      if (logFiles.containsKey(packetLocation)) {
//        logFiles.get(packetLocation).close();
//      }
//    }
//  }
//
//  protected NetworkTrafficMonitor()  // initialise all except log files
//  {
//    whichSide = Side.CLIENT;
//    resetTally();
//  }
//
//  // logs values but doesn't write to a log file
//  public static class NetworkTrafficMonitorNULL extends NetworkTrafficMonitor
//  {
//    @Override
//    public void log() throws IOException
//    {
//    }
//    @Override
//    public void closeAll() throws IOException
//    {
//    }
//  }
//
//}
