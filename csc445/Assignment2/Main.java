package Assignment2;

/*
    client program
*/

import java.io.*;
import java.net.Socket;
import java.util.*;

public class Main {

    /*
        javac -d bin src/Assignment2/XorEncryption.java src/Assignment2/Main2.java src/Assignment2/Server.java
        src/Assignment2/BytesHandler.java src/Assignment2/FileHandler.java

        java -cp bin Assignment2.Main
        java -cp bin Assignment2.Server
     */

    static int port = 3030;
    static String host = "129.3.20.3";  // moxie's ip
    static final int WINDOW_SIZE = 4;

    static Map<Integer, Boolean> ackMap = new HashMap<>();  // map to keep track of what packets have been acknowledged

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

        // TODO: remove and take a command line arg
        System.out.print("Drop? ");
        Scanner dropScanner = new Scanner(System.in);
        String dropInput = dropScanner.next();
        if (dropInput.equalsIgnoreCase("y")) {
            drop = true;
        }

        if (input.equalsIgnoreCase("upload")) {
            File file = FileHandler.getFile(filename);  // get desired file to upload
            if (file != null) {
                sendWithTCP(randomNumber, senderID, drop, file);    // upload file
            } else {
                System.err.println(filename + " cannot be found. ");
            }
        } else if (input.equalsIgnoreCase("download")) {
            File file = downloadWithTCP(filename);  // download desired file

            File currentFile = null;
            try {
                currentFile = FileHandler.getFile(filename);   // get old file
            } catch (Exception e) {
                System.err.println("Cannot validate files. " + filename + " was not previously present on your machine. ");
            }

            if (currentFile != null) {
                String currentFileContents = FileHandler.readFile(currentFile); // get old file contents
                String newFileContents = FileHandler.readFile(file);    // get new file contents

                assert currentFileContents != null;
                if (currentFileContents.equals(newFileContents)) {  // validate
                    System.out.println("Valid file contents. ");
                }
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

        // fill ack map with every packet not yet acknowledged
        for (int i = 0; i < message.length; i++) {
            ackMap.put(i, false);
        }

        try (Socket socket = new Socket(host, port);
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

            outputStream.writeInt(0);   // send mode (upload)
            outputStream.writeObject(senderID); // send sender ID
            outputStream.writeObject(randomNumber);  // send long for decryption
            outputStream.writeInt(message.length);  // send message length

            // initialize variables
            int totalPackets = message.length;
            System.out.println("Total packets: " + totalPackets);
            int ackReceived = 0;
            int startPacket = -1;

            // sending file
            do {
                // fill initial window
                if (startPacket == -1) {
                    for (int i = 0; i < WINDOW_SIZE; i++) {
                        byte packet = message[i];
                        outputStream.writeObject(packet);
                        System.out.println("Sent packet " + i);
                    }
                }

                // send the rest upon receiving expecting ack of packetToSend - WINDOW_SIZE
                try {
                    Object ack = inputStream.readObject();

                    // receive ack
                    if (ack instanceof Integer) {
                        ackReceived = (Integer) ack;
                        updateAckMap(ackReceived);  // update the ack map (packet was acknowledged)
                    }

                    int remainingPackets = message.length - ackReceived;

                    if (remainingPackets != 0) {
                        System.out.println("Received ack for packet " + ackReceived);

                        if (remainingPackets > WINDOW_SIZE) {
                            startPacket = ackReceived + WINDOW_SIZE;  // slide window
                            System.out.println("Window slid, sending packet " + startPacket);
                            if (startPacket < message.length && packetWasReceived(startPacket)) {
                                byte packet = message[startPacket];

                                // if the drop flag was entered, drop 1% of packets
                                if (drop && shouldDropPacket()) {
                                    System.out.println("Dropped packet " + startPacket);
                                } else {    // upload normally, no dropped packets
                                    outputStream.writeObject(packet);
                                }
                            }
                        }
                    }

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            } while (ackReceived != message.length - 1);

            // flush and close streams
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            System.out.println("All packets acknowledged. (" + ackReceived + " packets)");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File downloadWithTCP(String filename) {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Object packet;
            int packetCount = 0;
            int packetToAck = 0;

            outputStream.writeInt(1);   // send mode (download)
            outputStream.writeObject(filename); // send filename

            String senderID = (String) inputStream.readObject();    // read senderID
            int messageLength = inputStream.readInt();  // read message length
            long randomNumber = inputStream.readLong();

            do {
                if (packetToAck == 0) {
                    for (int i = 0; i < WINDOW_SIZE; i++) {
                        packet = inputStream.readObject();
                        baos.write((byte) packet);

                        System.out.println("Received packet " + packetCount);
                        packetCount++;
                    }
                } else if (packetCount != messageLength) {
                    packet = inputStream.readObject();
                    baos.write((byte) packet );
                    System.out.println("Received packet " + packetCount);
                    packetCount++;
                }

                // send ack
                outputStream.writeObject(packetToAck);
                System.out.println("Ack sent for packet " + packetToAck);

                packetToAck++;

            } while (packetToAck != messageLength);

            byte[] message = baos.toByteArray();

            String filename1 = senderID + ".txt";

            System.out.println(filename1);
            System.out.println(randomNumber);

            long key = XorShift(randomNumber);
            XorEncryption xorEncryption = new XorEncryption(key);
            message = xorEncryption.decrypt(message, key);
            String contents = BytesHandler.bytesToString(message);

            System.out.println("File contents: " + contents + ".");

            FileHandler.writeToFile(contents, senderID);

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            return FileHandler.getFile(filename);

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

    private static boolean shouldDropPacket() {
        Random random = new Random();
        return random.nextInt(100) < 1;
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
