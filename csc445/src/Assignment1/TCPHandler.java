package Assignment1;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

/*
        javac -d bin src/Assignment1/Main.java src/Assignment1/XorEncryption.java src/Assignment1/BytesHandler.java
        java -cp bin Assignment1.Main

        javac -d bin src/Assignment1/TCPHandler.java src/Assignment1/XorEncryption.java src/Assignment1/BytesHandler.java
        java -cp bin Assignment1.TCPHandler
 */

public class TCPHandler {

    static long key = 301957482;
    static XorEncryption xorEncryption = new XorEncryption(key);

    public static void main(String[] args) throws IOException {
        int port = 3030;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("\nListening on port " + port + "\n");
            while (true) {
                try (Socket socket = serverSocket.accept();
                     ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                     ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {

                    // receive message
                    byte[] byteMessage;
                    int numMessages;
                    try {
                        byteMessage = (byte[]) inputStream.readObject();
                        int messageSize = BytesHandler.getSize(byteMessage);
                        System.out.println("Message received! (" + messageSize + " bytes)");

                        // decrypt byteMessage
                        byteMessage = xorEncryption.decrypt(byteMessage, key);
                        long decryptedMessage = BytesHandler.bytesToLong(byteMessage);
                        System.out.println("Message: " + decryptedMessage + "\n");

                        sendBackWithTCP(decryptedMessage, messageSize, outputStream);
                    } catch (Exception e) {
                        numMessages = inputStream.readInt();

                        for (int i = 0; i < numMessages; i++) {
                            byteMessage = (byte[]) inputStream.readObject();
                            if (i == numMessages - 1) {
                                int messageSize = BytesHandler.getSize(byteMessage);
                                System.out.println(numMessages + " messages received (" + messageSize + " bytes)");
                                System.out.println("Last message: " + BytesHandler.bytesToLong(byteMessage));
                                sendAck(outputStream);
                                System.out.println("Ack sent successfully\n");
                            }
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("IOException: " + e.getMessage());
                    break;
                }
            }
        }
    }

    private static void sendBackWithTCP(long message, int size, ObjectOutputStream outputStream) {
        try {
            // set size of new message
            byte[] messageToSend = BytesHandler.setSize(message, size);

            // encrypt new message
            messageToSend = xorEncryption.encrypt(messageToSend, key);

            // send message
            outputStream.writeObject(messageToSend);
            outputStream.flush();

            // print specifics
            int newMessageSize = BytesHandler.getSize(messageToSend);
            System.out.println("Message sent! (" + newMessageSize + " bytes)");
            System.out.println("Message: " + message + "\n");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void sendAck(ObjectOutputStream outputStream) {
        try {
            int size = 8;   // 8 byte acknowledgement

            long value = xorEncryption.getRandomLong();
            byte[] ack = BytesHandler.setSize(value, size);

            outputStream.writeObject(ack);
            outputStream.flush();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
