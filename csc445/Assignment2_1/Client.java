package Assignment2_1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Client {

    static int dropCounter = 0;

    public static Opcode opcode;
    static int port = 3030;
    //static String host = "129.3.20.3"; // moxie's ip
    static final int WINDOW_SIZE = 10;
    static List<Packet> packets;
    static Map<Integer, ByteBuffer> data = new HashMap<>();
    static Map<Integer, Boolean> ackMap = new HashMap<>(); // map to keep track of what packets have been acknowledged

    public static void main(String[] args) throws IOException {
        //Path filepath = Path.of("C:\\Users\\trist\\IdeaProjects\\csc445\\src\\Assignment2_1\\S8GC.png");
        Path filepath = Path.of("C:\\Users\\trist\\IdeaProjects\\csc445\\src\\Assignment2_1\\Screenshot_1.png");
        boolean drop = true;
        long randomLong = Key.getRandomLong();

        byte[] data = Files.readAllBytes(filepath);
        packets = Packet.makePackets(data);

        long totalBytesSent = 0;
        for (Packet packet : packets) {
            totalBytesSent += packet.getData().length;
        }

        opcode = Opcode.WRQ;

        try (DatagramChannel channel = DatagramChannel.open()) {
            InetSocketAddress address = new InetSocketAddress("localhost", port);

            Transfer transfer = new Transfer(channel, address);

            double duration = 0;

            if (opcode == Opcode.RRQ) {
                transfer.sendOptions(channel, address, opcode, randomLong, packets, drop);
                duration = RRQ(transfer);
            } else if (opcode == Opcode.WRQ) {
                transfer.sendOptions(channel, address, opcode, randomLong, packets, drop);
                duration = WRQ(transfer, drop);
            }

            double durationSeconds = duration / 1_000_000_000.0;
            double mbps = (totalBytesSent * 8.0) / (durationSeconds * 1_000_000);
            System.out.println("Throughput: " + mbps + " Mbps");
            System.out.println("\nDrops: " + dropCounter);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static double RRQ(Transfer transfer) throws IOException {
        long startTime = System.nanoTime();

        int packetToAck = 0;
        int packetCount = 0;

        ByteBuffer dataSize = transfer.receive();
        int size = dataSize.getInt(8);
        Ack dataSizeAck = new Ack(0);
        transfer.sendAck(dataSizeAck);
        System.out.println(size);

        do {
            if (data.size() >= WINDOW_SIZE) {
                Ack ack = new Ack(packetToAck);
                transfer.sendAck(ack);
                packetToAck++;
            }

            if (packetCount != size) {
                ByteBuffer buffer = transfer.receive();

                buffer.rewind();
                int bufferOpcode = buffer.getInt();
                int sequenceNumber = buffer.getInt();

                System.out.println("Received packet " + sequenceNumber);
                packetCount++;

                Optional<Opcode> bufferOpc = Opcode.getOpcode(bufferOpcode);

                if (bufferOpc.isPresent()) {
                    if (bufferOpc.get().equals(Opcode.DATA)) {
                        data.put(sequenceNumber, buffer);
                        buffer.clear();
                    } else {
                        System.err.println("Received " + bufferOpc.get() + " instead of " + Opcode.DATA);

                    }
                }
            }

        } while (packetToAck != size);

        String filepath = "C:\\Users\\trist\\IdeaProjects\\csc445\\src\\Assignment2_1\\data\\temp\\Screenshot_1.png";
        Packet.reconstructFile(data, filepath);

        long endTime = System.nanoTime();
        return endTime - startTime;
    }

    private static double WRQ(Transfer transfer, boolean drop) throws IOException, InterruptedException {
        long startTime = System.nanoTime();

        int ackReceived = 0;
        int startPacket = WINDOW_SIZE;

        // fill initial window
        for (int i = 0; i < WINDOW_SIZE; i++) {
            transfer.send(packets.get(i));
            System.out.println("Sent packet " + packets.get(i).getSequenceNumber());
        }

        do {
            Ack ack = transfer.receiveAck();
            if (ack != null) {
                ackMap.put(ack.getBlockNumber(), true);
                System.out.println("Received " + Opcode.ACK + " for packet " + ack.getBlockNumber());
                ackReceived++;
            }

            if (startPacket < packets.size()) {
                if (packetWasReceived(startPacket)) {
                    if (drop && shouldDropPacket()) {
                        dropCounter++;
                        transfer.Timeout();
                        System.out.println("Dropped packet " + packets.get(startPacket).getSequenceNumber() + "\nResending...");
                        transfer.send(packets.get(startPacket));
                    } else {
                        transfer.send(packets.get(startPacket));
                    }
                    System.out.println("Sent packet " + packets.get(startPacket).getSequenceNumber());
                    startPacket++;
                } else {
                    System.out.println("Waiting on ack for " + (startPacket - WINDOW_SIZE) + "before sending packet " + startPacket);
                }
            }
        } while (ackReceived != packets.size());

        long endTime = System.nanoTime();
        return endTime - startTime;
    }

    private static boolean packetWasReceived(int currentIndex) {
        int packetToCheck = currentIndex - WINDOW_SIZE;
        return ackMap.get(packetToCheck).equals(true);
    }

    private static boolean shouldDropPacket() {
        int randomInt = ThreadLocalRandom.current().nextInt(100);
        return randomInt < 1;
    }
}
