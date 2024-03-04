package Assignment1;

import java.io.*;
import java.net.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

public class Assignment1 {

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

    public static void main(String[] args) throws IOException {
        System.out.println();
        System.out.print("TCP/UDP: ");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();

        System.out.print("Host: ");
        Scanner hostScanner = new Scanner(System.in);
        String host = hostScanner.next();

        long message = xorEncryption.getRandomLong();

        long latency;
        if (input.equalsIgnoreCase("TCP")) {
            latency = sendWithTCP(message, host);
        } else if (input.equalsIgnoreCase("UDP")) {
            latency = sendWithUDP(message, host);
        } else {
            throw new RuntimeException(input + " is not a command. ");
        }
        System.out.println("\nLatency: " + latency + "\n");
    }

    private static long sendWithTCP(long message, String host) {
        System.out.print("Size: ");
        Scanner sizeScanner = new Scanner(System.in);
        int size = sizeScanner.nextInt();

        long startTime = System.nanoTime();
        long endTime;

        try (Socket socket = new Socket(host, 3030);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

            byte[] bytes = setSize(message, size);
            byte[] encryptedBytes = xorEncryption.encrypt(bytes, key);

            outputStream.writeObject(encryptedBytes);
            outputStream.flush();
            System.out.println("\nMessage sent! ");

            byte[] response = (byte[]) inputStream.readObject();

            System.out.println("\nMessage received! ");

            response = xorEncryption.decrypt(response, key);

            long decryptedMessage = bytesToLong(response);
            System.out.println("\nDecrypted message: " + decryptedMessage);

            endTime = System.nanoTime();

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return endTime - startTime;
    }

    private static long sendWithUDP(long message, String host) throws IOException {
        long startTime = System.nanoTime();
        long endTime = 0;

        int port = 3030;
        DatagramSocket socket = new DatagramSocket(port);

        byte[] sendData = longToBytes(message);

        InetAddress receiverAddress = InetAddress.getByName(host);
        int receiverPort = 3030;
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, receiverAddress, receiverPort);
        socket.send(packet);

        return endTime - startTime;
    }

    public static byte[] longToBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }

    public static long bytesToLong(byte[] arr) {
        ByteBuffer buffer = ByteBuffer.wrap(arr);
        return buffer.getLong();
    }

    public static byte[] setSize(long message, int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        byte[] bytes = longToBytes(message);

        int count = size / Long.BYTES;
        for (int i = 0; i < count; i++) {
            buffer.put(bytes);
        }

        int remaining = size % Long.BYTES;
        if (remaining > 0) {
            buffer.put(Arrays.copyOfRange(bytes, 0, remaining));
        }
        return buffer.array();
    }
}
