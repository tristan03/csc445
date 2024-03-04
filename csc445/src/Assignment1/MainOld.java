package Assignment1;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class MainOld {

    /*
        known servers:

        altair  129.3.20.2
        indigo  172.17.0.1
        rho     129.3.20.24
        pi      129.3.20.26
        gee     129.3.20.1
        moxie   172.19.0.1
     */

    static long key = 301957482;
    static XorEncryption xorEncryption = new XorEncryption(key);
    static int port = 3030;

    public static void main(String[] args) {
        System.out.println();
        System.out.print("TCP/UDP: ");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();

        System.out.print("Host: ");
        Scanner hostScanner = new Scanner(System.in);
        String host = hostScanner.next();

        long message = xorEncryption.getRandomLong();

        if (input.equalsIgnoreCase("TCP")) {
            TCP(message, host);
        }
    }

    private static void TCP(long message, String host) {
        // part 1

        //sizes
        int size8 = 8;
        int size64 = 64;
        int size512 = 512;

        // call method and print latency
        System.out.println("\nLatency (8 bytes): " + sendWithTCPLatency(message, host, size8) + "\n");
        System.out.println("\nLatency (64 bytes): " + sendWithTCPLatency(message, host, size64) + "\n");
        System.out.println("\nLatency (512 bytes): " + sendWithTCPLatency(message, host, size512) + "\n");

        // part 2

        System.out.println("Starting part 2... ");

        // sizes
        int size256 = 256;
        int size1024 = 1024;

        // number of messages sent
        int numMessages64 = 16384;
        int numMessages256 = 4096;
        int numMessages1024 = 1024;

//        sendWithTCPThroughput(host, size64, numMessages64);
//        sendWithTCPThroughput(host, size256, numMessages256);
//        sendWithTCPThroughput(host, size1024, numMessages1024);
    }

    private static long sendWithTCPLatency(long message, String host, int size) {

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

            int responseSize = BytesHandler.getSize(response);
            System.out.println("Message received! (" + responseSize + " bytes)");

            response = xorEncryption.decrypt(response, key);

            long decryptedMessage = BytesHandler.bytesToLong(response);
            System.out.println("Decrypted message: " + decryptedMessage);

            if (isValid(message, decryptedMessage)) {
                System.out.println("Valid message. ");
            } else {
                System.out.println("Invalid message. ");
            }

            endTime = System.nanoTime();

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return endTime - startTime;
    }

    private static void sendWithTCPThroughput(String host, int messageSize, int numMessages) {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

            byte[] message = new byte[messageSize];
            ByteBuffer buffer = ByteBuffer.wrap(message);

            long value = 0L;
            while (buffer.hasRemaining()) {
                buffer.putLong(value++);
            }

            long startTime = System.nanoTime();

            for (int i = 0; i < numMessages; i++) {
                outputStream.write(message);
                outputStream.flush();

//                byte[] ack = (byte[]) inputStream.readObject();
//                System.out.println("\rAck: " + Arrays.toString(ack));
            }

            long endTime = System.nanoTime();
            double duration = (endTime - startTime) / 1000.0; // seconds
            int totalBytes = 1024 * 1024;
            double throughput = (totalBytes * 8) / (duration * 1024 * 1024); // Mbps

            System.out.println("Throughput (" + messageSize + " bytes): " + throughput + " Mbps");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static long sendWithUDP(long message, String host) throws IOException {
        long startTime = System.nanoTime();
        long endTime = 0;

        DatagramSocket socket = new DatagramSocket(port);

        byte[] sendData = BytesHandler.longToBytes(message);

        InetAddress receiverAddress = InetAddress.getByName(host);
        int receiverPort = 3030;
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, receiverAddress, receiverPort);
        socket.send(packet);

        return endTime - startTime;
    }

    private static boolean isValid(long message, long receivedMessage) {
        return message == receivedMessage;
    }
}

