package Assignment2_1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.Random;

public class Transfer {
    public DatagramChannel channel;
    public InetSocketAddress address;
    private SocketAddress clientAddress;
    public final int PACKET_SIZE = 520;
    public boolean drop;

    private int opc;
    private String filename;
    private String senderID;
    private int size;
    private long key;

    public Transfer(DatagramChannel channel, InetSocketAddress address) {
        this.channel = channel;
        this.address = address;
    }

    public void send(Packet packet) {
        SocketAddress clientAddress = getClientAddress();

        if (packet == null) {
            System.err.println("[ERROR] packet is null");
            return;
        }

        ByteBuffer buffer = packet.toByteBuffer();
        buffer.flip();

        try {
            if (clientAddress == null) {
                channel.send(buffer, address);
            } else {
                channel.send(buffer, clientAddress);
            }
        } catch (IOException e) {
            System.err.println("[ERROR] failed to send packet number " + packet.getSequenceNumber());
        }
    }

    public void Timeout() throws InterruptedException {
       Thread.sleep(50);
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

    public void sendOptions(DatagramChannel channel, InetSocketAddress serverAddress, Opcode opcode, long randomLong, List<Packet>packets, boolean drop) throws IOException {
        /*
                 tftp option extension

                 | opc | filename | 0 | mode | 0 |      + senderID and random long
        */
        int dropInt = 1;
        if (drop) {
            dropInt = 0;
        }

        String senderID = getRandomSenderID();

        int opc = 0;

        if (opcode == Opcode.RRQ) {
            opc = 1;
        } else if (opcode == Opcode.WRQ) {
            opc = 2;
        }

        ByteBuffer initialBuffer = ByteBuffer.allocate(PACKET_SIZE);

        String filename = "Screenshot_1.txt";
        String packetsSize = String.valueOf(packets.size());

        // put initial options
        initialBuffer.putInt(opc);       // send opcode
        initialBuffer.put(filename.getBytes());     // send filename
        initialBuffer.put((byte) 0);    // null terminator
        initialBuffer.put(senderID.getBytes()); // send senderID
        initialBuffer.put((byte) 0);
        initialBuffer.put(packetsSize.getBytes());
        initialBuffer.put((byte) 0);
        initialBuffer.putLong(randomLong);    // send randomLong for decryption
        initialBuffer.put((byte) 0);
        initialBuffer.putInt(dropInt);
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
            SocketAddress clientAddress = channel.receive(buffer);
            setClientAddress(clientAddress);
            buffer.rewind();
            ack = new Ack(buffer.getInt(4));
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
    public void setDrop(boolean drop) {
        this.drop = drop;
    }

    public String getFilename() {
        return filename;
    }
    public String getSenderID() {
        return senderID;
    }
    public int getSize() {
        return size;
    }
    public long getKey() {
        return key;
    }

    public int getOpcode() {
        return opc;
    }
    public SocketAddress getClientAddress() {
        return clientAddress;
    }
    public boolean isDrop() {
        return drop;
    }
}
