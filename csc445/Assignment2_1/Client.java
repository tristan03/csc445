package Assignment2_1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Client {

    public static Opcode opcode;
    static int port = 3030;
    //static String host = "129.3.20.3"; // moxie's ip
    static final int WINDOW_SIZE = 4;
    static final int PACKET_SIZE = 512;
    static List<Packet> packets;

    public static void main(String[] args) throws IOException {
        //Path filepath = Path.of("C:\\Users\\trist\\IdeaProjects\\csc445\\src\\Assignment2_1\\S8GC.png");
        Path filepath = Path.of("C:\\Users\\trist\\IdeaProjects\\csc445\\src\\Assignment2_1\\test.txt");
        boolean drop = false;
        long randomLong = XorEncryption.getRandomLong();

        byte[] data = Files.readAllBytes(filepath);
        packets = Packet.makePackets(data);

        opcode = Opcode.WRQ;

        try (DatagramChannel channel = DatagramChannel.open()) {
            InetSocketAddress address = new InetSocketAddress("localhost", port);

            Transfer transfer = new Transfer(channel, address);

            if (opcode == Opcode.RRQ) {
                System.out.println();
            } else if (opcode == Opcode.WRQ) {
                transfer.sendOptions(channel, address, randomLong, packets);

                for (int i = 0; i < packets.size(); i++) {
                    transfer.send(packets.get(i));
                    Ack ack = transfer.receiveAck();
                    if (ack != null) {
                        System.out.println("Received " + Opcode.ACK + " for packet " + ack.getBlockNumber());
                    }
                }
            }
        }
    }
}
