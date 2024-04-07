package Assignment2_1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.*;

public class Server {

    private static final int BUFFER_SIZE = 512;
    private static final int WINDOW_SIZE = 4;
    private static final int port = 3030;

    static InetSocketAddress serverAddress = new InetSocketAddress("localhost", port);

    static Map<Integer, ByteBuffer> packets = new HashMap<>();

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
                    System.out.println();
                } else if (opc.get().equals(Opcode.WRQ)) {
                    int size = transfer.getSize();

                    for (int i = 0; i < size ; i++) {
                        ByteBuffer buffer = transfer.receive();

                        int bufferOpcode = buffer.get(3);
                        int sequenceNumber = buffer.get(7);

                        Optional<Opcode> bufferOpc = Opcode.getOpcode(bufferOpcode);

                        if (bufferOpc.isPresent()) {
                            if (bufferOpc.get().equals(Opcode.DATA)) {
                                packets.put(sequenceNumber, buffer);
                                buffer.clear();
                                sendAck(sequenceNumber, transfer);
                            } else {
                                System.err.println("Received " + bufferOpc.get() + " instead of " + Opcode.DATA);

                            }
                        }
                    }

                    String filepath = "C:\\Users\\trist\\IdeaProjects\\csc445\\src\\Assignment2_1\\data\\temp\\test.txt";
                    Packet.reconstructFile(packets, filepath);
                }
            }
            System.out.println();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void receiveOptions(Transfer transfer) {
        ByteBuffer buffer = transfer.receive();
        buffer.flip();

        int opc = buffer.getInt();
        String filename = extractString(buffer);
        String senderID = extractString(buffer);
        String dataLength = extractString(buffer);
        long randomLong = buffer.getLong();

        int size = Integer.parseInt(dataLength);
        long key = XorEncryption.XorShift(randomLong);

        transfer.setOpc(opc);
        transfer.setFilename(filename);
        transfer.setSize(size);
        transfer.setKey(key);
        transfer.setSenderID(senderID);
    }

    private static void sendAck(int sequenceNumber, Transfer transfer) {
        Ack ack = new Ack(sequenceNumber);
        transfer.sendAck(ack);
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
}

