package Assignment2;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Server {

    static int WINDOW_SIZE = 4;
    static Map<Integer, Boolean> ackMap = new HashMap<>();  // map to keep track of what packets have been acknowledged

    public static void main(String[] args) throws IOException {
        int port = 3030;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening on port " + port + "\n");
            while (true) {
                try (Socket socket = serverSocket.accept();
                     ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                     ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {

                    int mode = inputStream.readInt();

                    if (mode == 0) {
                        System.out.println("Mode " + mode + " (Client is uploading a file) \n");

                        String senderID = (String) inputStream.readObject();
                        long randomNumber = (long) inputStream.readObject();
                        int messageLength = inputStream.readInt();
                        System.out.println("Message length: " + messageLength);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        Object packet;
                        int packetCount = 0;
                        int packetToAck = 0;

                        do {
                            if (packetToAck == 0) {
                                for (int i = 0; i < WINDOW_SIZE; i++) {
                                    packet = inputStream.readObject();
                                    baos.write((byte) packet);

                                    System.out.println("Received packet " + packetCount);
                                    packetCount++;
                                }
                            } else if (packetCount != messageLength){
                                packet = inputStream.readObject();
                                baos.write((byte) packet );
                                System.out.println("Received packet " + packetCount);
                                packetCount++;
                            }

                            // send ack
                            outputStream.writeObject(packetToAck);
                            outputStream.flush();
                            System.out.println("Ack sent for packet " + packetToAck);

                            packetToAck++;

                        } while (packetToAck != messageLength);

                        byte[] message = baos.toByteArray();

                        String filename = senderID + ".txt";

                        System.out.println(filename);
                        System.out.println(randomNumber);

                        long key = XorShift(randomNumber);
                        XorEncryption xorEncryption = new XorEncryption(key);
                        message = xorEncryption.decrypt(message, key);
                        String fileContents = BytesHandler.bytesToString(message);

                        System.out.println("File contents: " + fileContents + ".");

                        FileHandler.writeToFile(fileContents, senderID);

                    } else if (mode == 1) {
                        System.out.println("Mode " + mode + "(Client is downloading a file)");

                        long randomNumber = getRandomLong();
                        long key = XorShift(randomNumber);
                        XorEncryption xorEncryption = new XorEncryption(key);

                        String filename = (String) inputStream.readObject();    // read filename
                        File file = FileHandler.getFile(filename);  // get the file
                        assert file != null;

                        String contents = FileHandler.readFile(file);   // get the file contents
                        assert contents != null;
                        byte[] data = BytesHandler.stringToBytes(contents); // convert String -> bytes
                        byte[] message = xorEncryption.encrypt(data, key);  // encrypt

                        // fill ack map with every packet not yet acknowledged
                        for (int i = 0; i < message.length; i++) {
                            ackMap.put(i, false);
                        }

                        int senderIDLength = file.getName().length();
                        int endIndex = senderIDLength - 4; // subtract off the extension
                        String senderID = file.getName().substring(0, endIndex);   // get the files name without extension (senderID)

                        outputStream.writeObject(senderID); // send the senderID
                        outputStream.writeInt(message.length);   // send the message length
                        outputStream.writeLong(randomNumber);    // send the random long for decryption

                        // initialize variables
                        int totalPackets = message.length;
                        System.out.println("Total packets: " + totalPackets);
                        int ackReceived = 0;
                        int startPacket = -1;

                        // sending file
                        do {
                            if (startPacket == -1) {
                                for (int i = 0; i < WINDOW_SIZE; i++) {
                                    byte packet = message[i];
                                    outputStream.writeObject(packet);
                                    outputStream.flush();
                                    System.out.println("Sent packet " + i);
                                }
                            }

                            // send the rest upon receiving expecting ack of packetToSend - WINDOW_SIZE
                            try {
                                Object ack = inputStream.readObject();

                                if (ack instanceof Integer) {
                                    ackReceived = (Integer) ack;
                                    updateAckMap(ackReceived);
                                }

                                int remainingPackets = message.length - ackReceived;

                                if (remainingPackets != 0) {
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

                        } while (ackReceived != message.length);

                        // flush and close streams
                        outputStream.flush();
                        outputStream.close();
                        inputStream.close();

                        System.out.println("All packets acknowledged. (" + ackReceived + " packets)");
                    }

                } catch (Exception e) {
                    System.exit(0);
                }
            }
        }
    }

    private static void updateAckMap(int index) {
        ackMap.replace(index, true);
    }

    private static boolean packetWasReceived(int currentIndex) {
        int packetToCheck = currentIndex - WINDOW_SIZE;
        return ackMap.get(packetToCheck).equals(true);
    }


    private static long XorShift(long r) {
        r ^= r << 13;
        r ^= r >>> 7;
        r ^= r << 17;
        return r;
    }

    private static long getRandomLong() {
        Random random = new Random();
        return random.nextLong();
    }
}
