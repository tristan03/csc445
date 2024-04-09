package Assignment2_1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Server {

    private static final int WINDOW_SIZE = 10;
    private static final int port = 3030;

    static InetSocketAddress serverAddress = new InetSocketAddress("localhost", port);

    static List<Packet> packets;
    static Map<Integer, ByteBuffer> data = new HashMap<>();
    static Map<Integer, Boolean> ackMap = new HashMap<>();

    public static void main(String[] args) {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(serverAddress);

            Transfer transfer = new Transfer(channel, serverAddress);

            System.out.println("\n-------------------------------------------------------------------\n");
            System.out.println("Listening on port " + port + "\n");

            receiveOptions(transfer);

            int opcode = transfer.getOpcode();

            Optional<Opcode> opc = Opcode.getOpcode(opcode);

            if (opc.isPresent()) {
                if (opc.get().equals(Opcode.RRQ)) {
                    RRQ(transfer);
                } else if (opc.get().equals(Opcode.WRQ)) {
                    WRQ(transfer);
                }
            }
            System.out.println();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void RRQ(Transfer transfer) throws IOException {
        Path filepath = Path.of("C:\\Users\\trist\\IdeaProjects\\csc445\\src\\Assignment2_1\\Screenshot_1.png");
        boolean drop = transfer.isDrop();

        byte[] data = Files.readAllBytes(filepath);
        packets = Packet.makePackets(data);

        // send file size
        int dataSize = packets.size();
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(dataSize);
        byte[] arr = buffer.array();
        Packet packet = new Packet(0, arr, Opcode.DATA);
        transfer.send(packet);

        while (true) {
            Ack ack = transfer.receiveAck();
            if (ack != null) {
                break;
            }
        }

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
                    transfer.send(packets.get(startPacket));
                    System.out.println("Sent packet " + packets.get(startPacket).getSequenceNumber());
                    startPacket++;
                } else {
                    System.out.println("Waiting on ack for " + (startPacket - WINDOW_SIZE) + "before sending packet " + startPacket);
                }
            }
        } while (ackReceived != packets.size());
    }

    private static void WRQ(Transfer transfer) throws IOException {
        int packetToAck = 0;
        int packetCount = 0;
        int size = transfer.getSize();
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
    }

    private static void receiveOptions(Transfer transfer) {
        ByteBuffer buffer = transfer.receive();
        buffer.flip();

        int opc = buffer.getInt();
        String filename = extractString(buffer);
        String senderID = extractString(buffer);
        String dataLength = extractString(buffer);
        long randomLong = buffer.getLong();
        int dropInt = buffer.getInt();

        boolean drop = dropInt == 0;

        int size = Integer.parseInt(dataLength);
        long key = Key.XorShift(randomLong);

        transfer.setOpc(opc);
        transfer.setFilename(filename);
        transfer.setSize(size);
        transfer.setKey(key);
        transfer.setSenderID(senderID);
        transfer.setDrop(drop);

        printOptions(transfer);
    }

    private static void printOptions(Transfer transfer) {
        int opcode = transfer.getOpcode();
        Optional<Opcode> opc = Opcode.getOpcode(opcode);

        if (opc.isPresent()) {
            System.out.println("User requested: " + opc.get());
            if (opc.get().equals(Opcode.RRQ)) {
                System.out.println("File being downloaded: " + transfer.getFilename());
            } else if (opc.get().equals(Opcode.WRQ)) {
                System.out.println("File being uploaded: " + transfer.getFilename());
            }
        }

        System.out.println("Transfer SenderID: " + transfer.getSenderID() + "\n");
    }

    private static boolean packetWasReceived(int currentIndex) {
        int packetToCheck = currentIndex - WINDOW_SIZE;
        return ackMap.get(packetToCheck).equals(true);
    }

    // method to extract a null-terminated string from the buffer
    private static String extractString(ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        byte b;
        while ((b = buffer.get()) != 0) { // read bytes until we hit the null terminator
            sb.append((char) b);
        }
        return sb.toString();
    }

    private static boolean shouldDropPacket() {
        int randomInt = ThreadLocalRandom.current().nextInt(100);
        return randomInt < 1;
    }
}

