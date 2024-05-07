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
    private static final int port = 3030;
    private static final long key = 349302019;
    private static final List<SocketAddress> clientAddresses = new ArrayList<>();
    private static final ConcurrentHashMap<SocketAddress, Integer> expectedSequenceNumber = new ConcurrentHashMap<>();

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
                    expectedSequenceNumber.put(clientAddress, 0);
                }

                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                bytes = Encryption.decrypt(bytes, key);

                buffer.flip();
                String message = new String(bytes);
                try {
                    int receivedSequenceNumber = Integer.parseInt(message.split("-")[0]);
                    expectedSequenceNumber.replace(clientAddress, receivedSequenceNumber);
                    if (receivedSequenceNumber == expectedSequenceNumber.get(clientAddress)) {
                        String actualMessage = message.split("-")[1];
                        String messageWithAddress = actualMessage + "-" + clientAddress;
                        System.out.println("Received message: \"" + actualMessage + "\" from " + clientAddress + " [Sequence Number: " + receivedSequenceNumber + "]");
                        ByteBuffer sendBuffer = ByteBuffer.wrap(Encryption.encrypt(messageWithAddress.getBytes(), key));
                        sendToAllExcept(sendBuffer, channel, clientAddress);
                        sendAck(channel, clientAddress, receivedSequenceNumber);

                    }
                } catch (Exception ignored) {
                    // do nothing, message is either "connect" or "disconnect"
                }

                if (message.equals("disconnect")) {
                    String disconnectMessage = clientAddress + " has disconnected";
                    System.out.println(disconnectMessage);
                    clientAddresses.remove(clientAddress);

                    ByteBuffer disconnectBuffer = ByteBuffer.wrap(Encryption.encrypt(disconnectMessage.getBytes(), key));
                    sendToAllExcept(disconnectBuffer, channel, clientAddress);
                } else if (message.equals("connect")) {
                    String connectMessage = clientAddress + " has connected";
                    System.out.println(connectMessage);
                    ByteBuffer connectBuffer = ByteBuffer.wrap(Encryption.encrypt(connectMessage.getBytes(), key));
                    sendToAllExcept(connectBuffer, channel, clientAddress);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private static void sendAck(DatagramChannel channel, SocketAddress clientAddress, int receivedSequenceNumber) throws IOException {
        String ack = receivedSequenceNumber + "-" + "ACK";
        byte[] ackBytes = ack.getBytes();
        ackBytes = Encryption.decrypt(ackBytes, key);
        ByteBuffer ackBuffer = ByteBuffer.wrap(ackBytes);
        channel.send(ackBuffer, clientAddress);
    }

    private static void sendToAllExcept(ByteBuffer buffer, DatagramChannel channel, SocketAddress clientAddress) {
        for (SocketAddress address : clientAddresses) {
            if (!address.equals(clientAddress)) {
                buffer.rewind();

                try {
                    channel.send(buffer, address);
                } catch (IOException e) {
                    System.err.println("Error sending message to: " + address);
                }
            }
        }
    }
}
