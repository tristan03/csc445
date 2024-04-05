package Assignment2;

/*
    Tristan Allen
    CSC445 Assignment2
    Suny Oswego

    server program
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Server {

    static int WINDOW_SIZE = 5;
    static Map<Integer, Boolean> ackMap = new HashMap<>();  // map to keep track of what packets have been acknowledged
    static XorEncryption xorEncryption = new XorEncryption(1);

    public static void main(String[] args) throws IOException {
        int port = 3030;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("\nListening on port " + port + "\n");
            while (true) {
                try (Socket socket = serverSocket.accept();
                     ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                     ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {

                    int opcode = inputStream.readInt();

                    if (opcode == 2) {
                        System.out.println("---------------------------------------------------------------\n");
                        System.out.println("Opcode: " + opcode + " (Client is uploading a file)");

                        String mode = (String) inputStream.readObject();
                        System.out.println("Mode: " + mode + "\n");
                        String senderID = (String) inputStream.readObject();
                        System.out.println("Transfer sender id: " + senderID);
                        long randomNumber = (long) inputStream.readObject();
                        int messageLength = inputStream.readInt();
                        String filename = (String) inputStream.readObject();
                        System.out.println("Uploading file: " + filename + "\n");

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        Object packet;
                        int packetCount = 0;
                        int packetToAck = 0;

                        do {
                            boolean wasDropped = false;

                            if (packetToAck == 0) {

                                for (int i = 0; i < WINDOW_SIZE; i++) {
                                    packet = inputStream.readObject();

                                    if (packet instanceof Byte) {
                                        baos.write((byte) packet);
                                        System.out.println("Received packet " + packetCount);
                                        packetCount++;
                                    } else if (packet instanceof Integer) {
                                        wasDropped = true;
                                        System.out.println("Packet " + packetCount + " was dropped");
                                    }
                                }

                            } else if (packetCount != messageLength){
                                packet = inputStream.readObject();

                                if (packet instanceof Byte) {
                                    baos.write((byte) packet);
                                    System.out.println("Received packet " + packetCount);
                                    packetCount++;
                                } else if (packet instanceof Integer) {
                                    wasDropped = true;
                                    System.out.println("Packet " + packetCount + " was dropped");
                                }
                            }

                            if (!wasDropped) {
                                // send ack
                                outputStream.writeObject(packetToAck);
                                outputStream.flush();
                                System.out.println("Ack sent for packet " + packetToAck);

                                packetToAck++;
                            }

                        } while (packetToAck != messageLength);

                        byte[] message = baos.toByteArray();

                        long key = xorEncryption.XorShift(randomNumber);
                        XorEncryption xorEncryption = new XorEncryption(key);
                        message = xorEncryption.decrypt(message, key);
                        String fileContents = BytesHandler.bytesToString(message);

                        System.out.println("\nFile contents: " + fileContents + ".");

                        FileHandler.writeToFile(fileContents, filename);

                    } else if (opcode == 1) {
                        System.out.println("---------------------------------------------------------------\n");
                        System.out.println("Opcode " + opcode + " (Client is downloading a file) \n");

                        long randomNumber = xorEncryption.getRandomLong();
                        long key = xorEncryption.XorShift(randomNumber);
                        XorEncryption xorEncryption = new XorEncryption(key);

                        String filename = (String) inputStream.readObject();    // read filename
                        boolean drop = (boolean) inputStream.readObject();

                        File file = FileHandler.getFile(filename);
                        int status;

                        if (file == null) {
                            status = -1;
                            outputStream.writeInt(status);
                            System.out.println("File does not exist. Terminating file transfer. ");
                            break;
                        } else {
                            status = 0;
                            outputStream.writeInt(status);
                            System.out.println("File found. Approved file transfer. ");
                        }

                        String contents = FileHandler.readFile(file);   // get the file contents
                        assert contents != null;
                        byte[] data = BytesHandler.stringToBytes(contents); // convert String -> bytes
                        byte[] message = xorEncryption.encrypt(data, key);  // encrypt

                        // fill ack map with every packet not yet acknowledged
                        for (int i = 0; i < message.length; i++) {
                            ackMap.put(i, false);
                        }

                        String senderID = getRandomSenderID();

                        outputStream.writeObject(senderID); // send sender ID
                        outputStream.writeLong(randomNumber);  // send long for decryption
                        outputStream.writeInt(message.length);  // send message length
                        outputStream.writeObject(file.getName()); // send the filename

                        // initialize variables
                        int ackReceived = 0;
                        int startPacket = -1;

                        // sending file
                        do {
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

                                if (ack instanceof Integer) {
                                    ackReceived = (Integer) ack;
                                    updateAckMap(ackReceived);
                                    System.out.println("Received ack for packet " + ackReceived);
                                }

                                int remainingPackets = message.length - ackReceived;

                                if (remainingPackets != 0) {
                                    if (remainingPackets > WINDOW_SIZE) {
                                        startPacket = ackReceived + WINDOW_SIZE;  // slide window
                                        System.out.println("Window slid, sending packet " + startPacket);
                                        if (startPacket < message.length && packetWasReceived(startPacket)) {
                                            byte packet = message[startPacket];

                                            // if the drop flag was entered, drop 1% of packets
                                            boolean shouldDrop = shouldDropPacket(message.length / 5);
                                            if (drop && shouldDrop) {
                                                outputStream.writeObject(-1);   // -1 signalling dropped packet
                                                System.out.println("Dropped packet " + startPacket + ". Resending... ");

                                                // retrying to send
                                                outputStream.writeObject(packet);   // rewrite the packet
                                                System.out.println("Packet " + startPacket + " successfully sent ");
                                            } else {    // upload normally, no dropped packets
                                                outputStream.writeObject(packet);
                                            }
                                        }
                                    }
                                }

                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }

                        } while (ackReceived != message.length);

                        // flush and close streams
                        outputStream.flush();
                        outputStream.close();
                        inputStream.close();

                        System.out.println("All packets acknowledged. (" + ackReceived + " packets)");
                    }

                } catch (Exception e) {
                    System.exit(1);
                }
            }
        }
    }

    private static String getRandomSenderID() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            int randomIndex = random.nextInt(characters.length());
            sb.append(characters.charAt(randomIndex));
        }

        return sb.toString();
    }

    private static void updateAckMap(int index) {
        ackMap.replace(index, true);
    }

    private static boolean packetWasReceived(int currentIndex) {
        int packetToCheck = currentIndex - WINDOW_SIZE;
        return ackMap.get(packetToCheck).equals(true);
    }

    private static boolean shouldDropPacket(int maxSize) {
        Random random = new Random();
        return random.nextInt(maxSize) < 1;
    }
}
