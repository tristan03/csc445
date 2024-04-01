package Assignment2;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {
        int port = 3030;
        int WINDOW_SIZE = 4;

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

                        String filename = (String) inputStream.readObject();
                        File file = FileHandler.getFile(filename);

                        outputStream.writeObject(file);
                        outputStream.flush();
                    }

                } catch (Exception e) {
                    System.exit(0);
                }
            }
        }
    }

    private static long XorShift(long r) {
        r ^= r << 13;
        r ^= r >>> 7;
        r ^= r << 17;
        return r;
    }
}
