package Assignment2_1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.Random;

public class Transfer {
    public DatagramChannel channel;
    public InetSocketAddress address;
    public final int PACKET_SIZE = 514;
    public static boolean drop;

    private int opc;
    private String filename;
    private String senderID;
    private int size;
    private long key;
    private SocketAddress clientAddress;

    public Transfer(DatagramChannel channel, InetSocketAddress address) {
        this.channel = channel;
        this.address = address;
    }

    public void send(Packet packet) {
        if (packet == null) {
            System.err.println("[ERROR] packet is null");
            return;
        }

        ByteBuffer buffer = packet.toByteBuffer();
        buffer.flip();

        try {
            channel.send(buffer, address);
        } catch (IOException e) {
            System.err.println("[ERROR] failed to send packet number " + packet.getSequenceNumber());
        }
    }

    public void sendAck(Ack ack) {
        SocketAddress clientAddress = getClientAddress();

        if (ack == null) {
            System.err.println("[ERROR] ack is null");
            return;
        }

        ByteBuffer buffer = ack.toByteBuffer();
        buffer.flip();

        try {
            channel.send(buffer, clientAddress);
            System.out.println("Sent " + Opcode.ACK + " for packet " + ack.getBlockNumber());
        } catch (IOException e) {
            System.err.println("[ERROR] failed to send ack for packet number " + ack.blockNumber);
        }
    }

    public void sendOptions(DatagramChannel channel, InetSocketAddress serverAddress, long randomLong, List<Packet>packets) throws IOException {
        /*
                 tftp option extension

                 | opc | filename | 0 | mode | 0 |      + senderID and random long
        */

        String senderID = getRandomSenderID();

        int opcode = 2;

        String mode = "octet";

        ByteBuffer initialBuffer = ByteBuffer.allocate(PACKET_SIZE);

        String filename = "S8GC.png";
        String packetsSize = String.valueOf(packets.size());

        // put initial options
        initialBuffer.putInt(opcode);       // send opcode
        initialBuffer.put(filename.getBytes());     // send filename
        initialBuffer.put((byte) 0);    // null terminator
        initialBuffer.put(senderID.getBytes()); // send senderID
        initialBuffer.put((byte) 0);
        initialBuffer.put(packetsSize.getBytes());
        initialBuffer.put((byte) 0);
        initialBuffer.putLong(randomLong);    // send randomLong for decryption
        initialBuffer.put((byte) 0);
        initialBuffer.flip();

        // send initial data
        channel.send(initialBuffer, serverAddress);
    }

    public ByteBuffer receive() {
        ByteBuffer buffer = ByteBuffer.allocate(PACKET_SIZE);

        try {
            SocketAddress clientAddress = channel.receive(buffer);
            setClientAddress(clientAddress);
        } catch (IOException e) {
            System.err.println("[ERROR] could not receive packet");
        }
        return buffer;
    }

    public Ack receiveAck() {
        ByteBuffer buffer = ByteBuffer.allocate(PACKET_SIZE);
        Ack ack = null;

        try {
            channel.receive(buffer);
            ack = new Ack(buffer.get(7));
        } catch (IOException e) {
            System.err.println("[ERROR] could not receive ack");
        }
        return ack;
    }

    private static String getRandomSenderID() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            int randomIndex = random.nextInt(characters.length());
            sb.append(characters.charAt(randomIndex));
        }

        return sb.toString();
    }

    public void setOpc(int opc) {
        this.opc = opc;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setKey(long key) {
        this.key = key;
    }
    public void setClientAddress(SocketAddress clientAddress) {
        this.clientAddress = clientAddress;
    }

    public void setSenderID(String senderID) {
        this.senderID = senderID;
    }

    public int getSize() {
        return size;
    }

    public int getOpcode() {
        return opc;
    }
    public SocketAddress getClientAddress() {
        return clientAddress;
    }
}
