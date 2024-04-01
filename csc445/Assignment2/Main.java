package Assignment2;

/*
    client program
*/

import java.io.*;
import java.net.Socket;
import java.util.*;

public class Main {

    static int port = 3030;
    static String host = "129.3.20.3";  // moxie's ip
    static final int WINDOW_SIZE = 4;

    static Map<Integer, Boolean> ackMap = new HashMap<>();

    public static void main(String[] args) {
        System.out.print("Upload/Download: ");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();

        boolean drop = false;
//        if (args[0] != null) {
//            if (checkPacketDrop(args[0])) {
//                drop = true;
//            }
//        }

        long randomNumber = getRandomLong();
        String senderID = getRandomString(5);

        System.out.print("Filename: ");
        Scanner fileScanner = new Scanner(System.in);
        String filename = fileScanner.next();

        if (input.equalsIgnoreCase("upload")) {
            File file = FileHandler.getFile(filename);
            if (file != null) {
                sendWithTCP(randomNumber, senderID, drop, file);
            } else {
                System.err.println(filename + " cannot be found. ");
            }
        } else if (input.equalsIgnoreCase("download")) {
            File file = downloadWithTCP(filename);

            File currentFile = FileHandler.getFile(filename);

            assert currentFile != null;
            String currentFileContents = FileHandler.readFile(currentFile);
            String newFileContents = FileHandler.readFile(file);

            assert currentFileContents != null;
            if (currentFileContents.equals(newFileContents)) {
                System.out.println("Valid file contents. ");
            }
        }
    }

    private static void sendWithTCP(long randomNumber, String senderID, boolean drop, File file) {
        long key = XorShift(randomNumber);
        XorEncryption xorEncryption = new XorEncryption(key);

        String fileContents = FileHandler.readFile(file);
        assert fileContents != null;
        byte[] data = BytesHandler.stringToBytes(fileContents);
        byte[] message = xorEncryption.encrypt(data, key);

        for (int i = 0; i < message.length; i++) {
            ackMap.put(i, false);
        }

        try (Socket socket = new Socket(host, port);
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

            outputStream.writeInt(0);
            outputStream.writeObject(senderID);
            outputStream.writeObject(randomNumber);
            outputStream.writeInt(message.length);

            int totalPackets = message.length;
            System.out.println("Total packets: " + totalPackets);
            int ackReceived = 0;
            int startPacket = -1;

            do {

                if (startPacket == -1) {
                    for (int i = 0; i < WINDOW_SIZE; i++) {
                        byte packet = message[i];
                        outputStream.writeObject(packet);
                        System.out.println("Sent packet " + i);
                    }
                }

                try {
                    Object ack = inputStream.readObject();

                    if (ack instanceof Integer) {
                        ackReceived = (Integer) ack;
                        updateAckMap(ackReceived);
                    }

                    int remainingPackets = message.length - ackReceived;

                    if (remainingPackets != 0) {
                        System.out.println("Received ack for packet " + ackReceived);

                        if (remainingPackets > WINDOW_SIZE) {
                            startPacket = ackReceived + WINDOW_SIZE;  // slide window
                            System.out.println("Window slid, sending packet " + startPacket);

                            if (startPacket < message.length && packetWasReceived(startPacket)) {
                                byte packet = message[startPacket];
                                outputStream.writeObject(packet);
                            }
                        }
                    }

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            } while (ackReceived != message.length - 1);
            System.out.println("All packets acknowledged. (" + ackReceived + " packets)");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File downloadWithTCP(String filename) {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            outputStream.writeInt(1);
            outputStream.writeObject(filename);

            File file = (File) inputStream.readObject();

            FileHandler.saveFile(file);

            return file;

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // get random string for either senderID or the file contents
    private static String getRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            sb.append(characters.charAt(randomIndex));
        }

        return sb.toString();
    }

    private static boolean checkPacketDrop(String arg) {
        return arg.equalsIgnoreCase("-d");
    }

    private static boolean packetWasReceived(int currentIndex) {
        int packetToCheck = currentIndex - WINDOW_SIZE;
        return ackMap.get(packetToCheck).equals(true);
    }

    private static void updateAckMap(int index) {
        ackMap.replace(index, true);
    }

    private static long getRandomLong() {
        Random random = new Random();
        return random.nextLong();
    }

    private static long XorShift(long r) {
        r ^= r << 13;
        r ^= r >>> 7;
        r ^= r << 17;
        return r;
    }
}
