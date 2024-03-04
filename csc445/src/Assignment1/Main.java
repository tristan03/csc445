package Assignment1;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
    static long key = 301957482;
    static XorEncryption xorEncryption = new XorEncryption(key);
    static int port = 3030;

    public static void main(String[] args) {
        System.out.print("\nTCP/UDP: ");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();

        System.out.print("Host: ");
        Scanner hostScanner = new Scanner(System.in);
        String host = hostScanner.next();

        try {
            if (input.equalsIgnoreCase("TCP")) {
                TCP(host);
            } else if (input.equalsIgnoreCase("UDP")) {
                UDP(host);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void TCP(String host) throws IOException, ClassNotFoundException {
        int size8 = 8;  // 8 bytes
        int size64 = 64; // 64 bytes
        int size512 = 512; // 512 bytes

        // part 1
        long message = xorEncryption.getRandomLong();
        System.out.println("Latency (8 bytes):" + toMillis(TCPLatency(host, size8, message)) + " ms ");
        System.out.println("Latency (64 bytes):" + toMillis(TCPLatency(host, size64, message)) + " ms ");
        System.out.println("Latency (512 bytes): " + toMillis(TCPLatency(host, size512, message)) + " ms ");

        // part 3
        long message2 = xorEncryption.getRandomLong();
        // sizes
        int size256 = 256;
        int size1024 = 1024;

        // number of messages to send
        int numMessages64 = 16384;
        int numMessages256 = 4096;
        int numMessages1024 = 1024;

        System.out.println("Throughput (16384 64 bytes): " + TCPThroughput(host, size64, numMessages64, message2) + " mbps");
        System.out.println("Throughput (4096 256 bytes): " + TCPThroughput(host, size256, numMessages256, message2) + " mbps");
        System.out.println("Throughput (1024 1024 bytes): " + TCPThroughput(host, size1024, numMessages1024, message2) + " mbps");
    }

    private static long TCPLatency(String host, int size, long message) {
        long startTime;
        long endTime;

        try (Socket socket = new Socket(host, port);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

            byte[] bytes = BytesHandler.setSize(message, size);
            byte[] encryptedBytes = xorEncryption.encrypt(bytes, key);

            startTime = System.nanoTime();

            outputStream.writeObject(encryptedBytes);
            outputStream.flush();
            System.out.println("\nMessage sent! (" + size + " bytes)");

            byte[] response = (byte[]) inputStream.readObject();

            endTime = System.nanoTime();

            int responseSize = BytesHandler.getSize(response);
            System.out.println("Message received! (" + responseSize + " bytes)");

            response = xorEncryption.decrypt(response, key);

            long decryptedMessage = BytesHandler.bytesToLong(response);
            System.out.println("Decrypted message: " + decryptedMessage);

            if (isValid(message, decryptedMessage)) {
                System.out.println("Valid message ");
            } else {
                System.out.println("Invalid message ");
            }

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return endTime - startTime;
    }

    private static double TCPThroughput(String host, int size, int numMessages, long message) {
        long startTime;
        long endTime;
        double throughput;

        try (Socket socket = new Socket(host, port);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

            byte[] bytes = BytesHandler.setSize(message, size);

            startTime = System.nanoTime();

            outputStream.writeInt(numMessages);
            outputStream.flush();

            for (int i = 0; i < numMessages; i++) {
                outputStream.writeObject(bytes);
                outputStream.flush();
            }
            System.out.println("\n" + numMessages + " messages sent! (" + size + " bytes/message)");

            byte[] ack = (byte[]) inputStream.readObject();
            if (ack.length != 0) {
                int ackSize = BytesHandler.getSize(ack);
                System.out.println("Ack received (" + ackSize + " bytes)");
            }

            endTime = System.nanoTime();
            double duration = (endTime - startTime) / 1000000000.0; // seconds
            int totalBytes = 1024 * 1024;
            throughput = (totalBytes * 8) / (duration * 1024 * 1024); // Mbps

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return throughput;
    }

    private static void UDP(String host) {
        int size8 = 8;  // 8 bytes
        int size64 = 64; // 64 bytes
        int size512 = 512; // 512 bytes

        // part 2
        long message = xorEncryption.getRandomLong();

        System.out.println("Latency (8 bytes): " + toMillis(UDPLatency(host, size8, message)) + " ms ");
        System.out.println("Latency (64 bytes): " + toMillis(UDPLatency(host, size64, message)) + " ms ");
        System.out.println("Latency (512 bytes): " + toMillis(UDPLatency(host, size512, message)) + " ms ");

        // part 3
        long message2 = xorEncryption.getRandomLong();
        // sizes
        int size256 = 256;
        int size1024 = 1024;

        // number of messages to send
        int numMessages64 = 16384;
        //int numMessages64 = 2000;
        int numMessages256 = 4096;
        int numMessages1024 = 1024;

        System.out.println("Throughput (16384 64 bytes): " + UDPThroughput(host, size64, numMessages64, message2) + " mbps");
        System.out.println("Throughput (4096 256 bytes): " + UDPThroughput(host, size256, numMessages256, message2) + " mbps");
        System.out.println("Throughput (1024 1024 bytes): " + UDPThroughput(host, size1024, numMessages1024, message2) + " mbps");
    }

    private static long UDPLatency(String host, int size, long message) {
        long startTime;
        long endTime;

        try {
            // convert long message to bytes and then encrypt
            byte[] bytes = BytesHandler.setSize(message, size);
            byte[] encryptedBytes = xorEncryption.encrypt(bytes, key);

            // create UDP socket
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(host);

            // send packet
            DatagramPacket sendPacket = new DatagramPacket(encryptedBytes, encryptedBytes.length, address, port);
            startTime = System.nanoTime();
            socket.send(sendPacket);
            System.out.println("\nMessage sent! (" + encryptedBytes.length + " bytes)");

            // prepare to receive response
            byte[] buffer = new byte[size];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

            // receive response
            socket.receive(receivePacket);
            endTime = System.nanoTime();
            byte[] response = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());

            // decrypt and process the response
            response = xorEncryption.decrypt(response, key);
            long decryptedMessage = BytesHandler.bytesToLong(response);
            System.out.println("Decrypted message: " + decryptedMessage);

            if (isValid(message, decryptedMessage)) {
                System.out.println("Valid message ");
            } else {
                System.out.println("Invalid message ");
            }

            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return endTime - startTime;
    }

    private static double UDPThroughput(String host, int size, int numMessages, long message) {
        long startTime;
        long endTime;
        double throughput;

        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(host);

            // prepare the message
            byte[] bytes = BytesHandler.setSize(message, size);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(numMessages);
            dos.flush();
            byte[] numMessagesBytes = baos.toByteArray();
            DatagramPacket numMessagesPacket = new DatagramPacket(numMessagesBytes, numMessagesBytes.length, address, port);
            socket.send(numMessagesPacket); // send the number of messages as the first packet

            startTime = System.nanoTime();

            // send each message as its own packet
            System.out.println("\nSending " + numMessages + " messages (" + size + " bytes/message)...");
            for (int i = 0; i < numMessages; i++) {
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
                socket.send(packet);
                Thread.sleep(1);
            }
            System.out.println(numMessages + " messages sent! (" + size + " bytes/message)");

            // Receive ack
            byte[] ackBuffer = new byte[8];
            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
            socket.receive(ackPacket); // This will block until an ack is received
            byte[] ackData = Arrays.copyOf(ackPacket.getData(), ackPacket.getLength());
            if (ackData.length != 0) {
                int ackSize = BytesHandler.getSize(ackData);
                System.out.println("Ack received (" + ackSize + " bytes)");
            }

            endTime = System.nanoTime();
            double duration = (endTime - startTime) / 1000000000.0; // convert to seconds
            int totalBytes = size * numMessages;
            throughput = (totalBytes * 8) / (duration * 1024 * 1024); // calculate Mbps

            socket.close();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return throughput;
    }

    private static boolean isValid(long message, long receivedMessage) {
        return message == receivedMessage;
    }

    private static double toMillis(double latency) {
        return latency / 1000000.0;
    }

}