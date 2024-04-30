package Assignment3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int port = 3033;
    private static final long key = 349302019;
    private static final List<SocketAddress> clientAddresses = new ArrayList<>();
    private static final ConcurrentHashMap<SocketAddress, Integer> clientMessageSequenceMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(new InetSocketAddress(port));

            ByteBuffer buffer = ByteBuffer.allocate(1024);

            System.out.println("Chat server is running on port " + port);

            while (true) {
                buffer.clear();
                SocketAddress clientAddress = channel.receive(buffer);
                buffer.flip();

                if (!clientAddresses.contains(clientAddress)) {
                    clientAddresses.add(clientAddress);
                    clientMessageSequenceMap.put(clientAddress, 0);
                }

                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                bytes = Encryption.decrypt(bytes, key);

                String message = new String(bytes);
                try {
                    int receivedSequenceNumber = Integer.parseInt(message.split("-")[0]);
                    String actualMessage = message.split("-")[1];
                    System.out.println("Received message: \"" + actualMessage + "\" from " + clientAddress + " [Sequence Number: " + receivedSequenceNumber + "]");
                } catch (Exception e) {
                    System.out.println("Client connected from " + clientAddress);
                }

                sendAck(channel, clientAddress);

                if (message.equalsIgnoreCase("quit") || Thread.interrupted()) {
                    for (int i = 0; i < clientAddresses.size(); i++) {
                        if (clientAddresses.get(i).equals(clientAddress)) {
                            clientAddresses.remove(clientAddresses.get(i));
                            System.out.println("Client " + clientAddress + " disconnected ");
                        }
                    }
                }

                buffer.flip();
                for (SocketAddress address : clientAddresses) {
                    if (!address.equals(clientAddress)) {
                        buffer.rewind();
                        channel.send(buffer, address);
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private static void sendAck(DatagramChannel channel, SocketAddress clientAddress) throws IOException {
        String ack = "ACK";
        byte[] ackBytes = ack.getBytes();
        ackBytes = Encryption.decrypt(ackBytes, key);
        ByteBuffer ackBuffer = ByteBuffer.wrap(ackBytes);
        channel.send(ackBuffer, clientAddress);
    }
}
