package Assignment1;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class UDPHandler {
    static long key = 301957482;
    static XorEncryption xorEncryption = new XorEncryption(key);

    public static void main(String[] args) throws IOException {
        int port = 3030;

        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("\nListening on port " + port + "\n");

            while (true) {
                try {
                    // prepare buffer for receiving data
                    byte[] buffer = new byte[65507]; // max UDP packet size
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    try {
                        // receive packet
                        socket.receive(packet);

                        // process received message
                        byte[] byteMessage = Arrays.copyOf(packet.getData(), packet.getLength());
                        byteMessage = xorEncryption.decrypt(byteMessage, key);

                        long decryptedMessage = BytesHandler.bytesToLong(byteMessage);

                        InetAddress clientHost = packet.getAddress();
                        int clientPort = packet.getPort();

                        // respond
                        sendBackWithUDP(decryptedMessage, byteMessage.length, socket, clientHost, clientPort);

                    } catch (Exception e) {
                        byte[] message = Arrays.copyOf(packet.getData(), packet.getLength());
                        int numMessages = BytesHandler.bytesToInt(message);

                        for (int i = 0; i < numMessages; i++) {
                            InetAddress clientHost = packet.getAddress();
                            int clientPort = packet.getPort();
                            socket.setSoTimeout(5);

                            try {
                                packet.setLength(buffer.length);
                                socket.receive(packet);
                            } catch (SocketTimeoutException ex) {
                                continue;
                            }

                            byte[] bytes = Arrays.copyOf(packet.getData(), packet.getLength());
                            if (i == numMessages - 1) {
                                System.out.println(numMessages + " messages received" );
                                System.out.println("Last message: " + BytesHandler.bytesToLong(bytes));
                                sendAck(socket, clientHost, clientPort);
                                System.out.println("Ack sent successfully\n");
                            }
                        }
                    }

                } catch (IOException e) {
                    System.out.println("IOException: " + e.getMessage());
                    break;
                }
            }
        }
    }

    private static void sendBackWithUDP(long message, int size, DatagramSocket socket, InetAddress address, int port) {
        try {
            // prepare the message to send back
            byte[] messageToSend = BytesHandler.setSize(message, size);
            messageToSend = xorEncryption.encrypt(messageToSend, key);

            // send message
            DatagramPacket sendPacket = new DatagramPacket(messageToSend, messageToSend.length, address, port);
            socket.send(sendPacket);

            System.out.println("Message sent back! (" + messageToSend.length + " bytes)");
            System.out.println("Message: " + message + "\n");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void sendAck(DatagramSocket socket, InetAddress host, int port) {
        int size = 8;   // 8 byte acknowledgement

        try {
            long value = xorEncryption.getRandomLong();
            byte[] ack = BytesHandler.setSize(value, size);

            DatagramPacket sendPacket = new DatagramPacket(ack, ack.length, host, port);
            socket.send(sendPacket);


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
