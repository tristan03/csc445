import Assignment1.BytesHandler;
import Assignment1.XorEncryption;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Test {

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

        try (Socket socket = new Socket(host, port);
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

            if (input.equalsIgnoreCase("TCP")) {
                TCP(inputStream, outputStream);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void TCP(ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        int size8 = 8;  // 8 bytes
        int size64 = 64; // 64 bytes
        int size512 = 512; // 512 bytes

        System.out.println("Latency (8 bytes):" + TCPLatency(inputStream, outputStream, size8) + " ns ");
        System.out.println("Latency (64 bytes): " + TCPLatency(inputStream, outputStream, size64) + " ns ");
        System.out.println("Latency (512 bytes): " + TCPLatency(inputStream, outputStream, size512) + " ns ");
    }

    private static long TCPLatency(ObjectInputStream inputStream, ObjectOutputStream outputStream, int size) {
        long startTime = 0;
        long endTime = 0;

        try {
            long message = xorEncryption.getRandomLong();

            byte[] bytes = BytesHandler.setSize(message, size);
            byte[] encryptedBytes = xorEncryption.encrypt(bytes, key);

            startTime = System.nanoTime();

            outputStream.writeObject(encryptedBytes);
            outputStream.flush();
            System.out.println("\nMessage sent! (" + size + " bytes)");

            byte[] response = (byte[]) inputStream.readObject();

            int responseSize = BytesHandler.getSize(response);
            System.out.println("\nMessage received! (" + responseSize + " bytes)");

            response = xorEncryption.decrypt(response, key);

            long decryptedMessage = BytesHandler.bytesToLong(response);
            System.out.println("\nDecrypted message: " + decryptedMessage);

            if (isValid(message, decryptedMessage)) {
                System.out.println("Valid message. ");
            } else {
                System.out.println("Invalid message. ");
            }

            endTime = System.nanoTime();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return endTime - startTime;
    }

    private static boolean isValid(long message, long receivedMessage) {
        return message == receivedMessage;
    }

}
