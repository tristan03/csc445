package Assignment2;

/*
    Tristan Allen
    CSC445 Assignment2
    Suny Oswego

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

        program is ran by:

            java [program] [code] [filename] [drop-flag]
                                                   ^
                                  this is optional |

            ex. java Assignment2.Main WRQ test.txt -d

        This program using TCP-style sliding windows with TFTP
     */

    static int port = 3030;
    static String host = "129.3.20.3";  // moxie's ip
    // static String host = ""; // rho's ip
    static final int WINDOW_SIZE = 5;

    static Map<Integer, Boolean> ackMap = new HashMap<>();  // map to keep track of what packets have been acknowledged
    static XorEncryption xorEncryption = new XorEncryption(1);

    public static void main(String[] args) {

//        // parse command line
//        String opcode = args[0];   // RRQ -> download (read from server), WRQ -> upload (write to server)
//        String filename = args[1];   // filename
//        boolean drop = Arrays.asList(args).contains("-d");  // "-d" flag indicates dropping packets
        String opcode = "WRQ";
        String filename = "test.txt";
        boolean drop = false;

        long randomNumber = xorEncryption.getRandomLong();   // get random long for encryption
        String senderID = getRandomSenderID();  // get unique senderID

        if (opcode.equalsIgnoreCase("WRQ")) {    // upload a file
            int opc = 2;
            File file = FileHandler.getFile(filename);  // get desired file to upload
            if (file != null) {
                upload(randomNumber, senderID, drop, file, opc);    // upload file
            } else {
                System.err.println(filename + " cannot be found. ");
            }
        } else if (opcode.equalsIgnoreCase("RRQ")) {
            int opc = 1;
            File file = download(filename, drop, opc);  // download desired file
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
                    System.out.println("\nValid file contents.\n");
                }
            }
        }
    }

    private static void upload(long randomNumber, String senderID, boolean drop, File file, int opc) {
        long key = xorEncryption.XorShift(randomNumber);

        String fileContents = FileHandler.readFile(file);
        assert fileContents != null;
        byte[] data = BytesHandler.stringToBytes(fileContents);
        byte[] message = xorEncryption.encrypt(data, key);

        // fill ack map with every packet not yet acknowledged
        for (int i = 0; i < message.length; i++) {
            ackMap.put(i, false);
        }

        // connect to server
        try (Socket socket = new Socket(host, port);
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

            String mode = "octet";

            outputStream.writeInt(opc);   // send opcode
            outputStream.writeObject(mode);    // send octet mode
            outputStream.writeObject(senderID); // send sender ID
            outputStream.writeObject(randomNumber);  // send long for decryption
            outputStream.writeInt(message.length);  // send message length
            outputStream.writeObject(file.getName()); // send the filename

            System.out.println("\n---------------------------------------------------------------\n");
            System.out.println("Transfer sender id: " + senderID);
            System.out.println("Uploading file: " + file.getName() + "\n");

            long startTime = System.currentTimeMillis();

            // initialize variables
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
                    Object oack = inputStream.readObject();

                    // receive ack
                    if (oack instanceof Integer) {
                        ackReceived = (Integer) oack;
                        updateAckMap(ackReceived);  // update the ack map (packet was acknowledged)
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

            } while (ackReceived != message.length - 1);

            long endTime = System.currentTimeMillis();

            // flush and close streams
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            // calculate throughput

            System.out.println("start time: " + startTime);
            System.out.println("end time: " + endTime);
            long duration = endTime - startTime;
            double durationSeconds = duration / 1000.0;
            double throughput = ((message.length * 8) / (durationSeconds)) / 1_000_000;


            System.out.println("Successfully uploaded " + file.getName() + "\n");
            System.out.println("Throughput: " + durationSeconds + " mbps\n");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File download(String filename, boolean drop, int opc) {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Object packet;
            int packetCount = 0;
            int packetToAck = 0;

            long startTime = System.currentTimeMillis();

            outputStream.writeInt(opc);   // send opcode
            outputStream.writeObject(filename); // send filename
            outputStream.writeObject(drop);    // send whether to drop packets or not

            int status = inputStream.readInt();  // approved or denied

            if (status == -1) {
                System.err.println("[ERROR] "+ filename + " does not exist. Request denied. ");
                System.exit(-1);
            }

            String senderID = (String) inputStream.readObject();    // read senderID
            System.out.println("\nTransfer sender id: " + senderID + "\n");
            long randomNumber = inputStream.readLong(); // read random long for decryption
            int messageLength = inputStream.readInt();  // read message length
            String filenameFromServer = (String) inputStream.readObject();

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

            long endTime = System.currentTimeMillis();

            byte[] message = baos.toByteArray();

            long key = xorEncryption.XorShift(randomNumber);
            XorEncryption xorEncryption = new XorEncryption(key);
            message = xorEncryption.decrypt(message, key);
            String contents = BytesHandler.bytesToString(message);

            System.out.println("\nSuccessfully downloaded " + filenameFromServer);
            System.out.println("File contents: " + contents + ".\n");

            FileHandler.writeToFile(contents, filenameFromServer);

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            // calculate throughput
            long duration = endTime - startTime;
            double durationSeconds = duration / 1000.0;
            double totalBits = messageLength * 8.0;
            double totalMegaBits = totalBits / 1_000_000;
            double throughput = totalMegaBits / durationSeconds;

            System.out.println("Throughput: " + throughput + " mbps\n");

            return FileHandler.getFile(filename);

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // get random string for either senderID or the file contents
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

    private static boolean packetWasReceived(int currentIndex) {
        int packetToCheck = currentIndex - WINDOW_SIZE;
        return ackMap.get(packetToCheck).equals(true);
    }

    private static boolean shouldDropPacket(int maxSize) {
        Random random = new Random();
        return random.nextInt(maxSize) < 1;
    }

    private static void updateAckMap(int index) {
        ackMap.replace(index, true);
    }
}
