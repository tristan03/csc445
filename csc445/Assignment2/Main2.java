package Assignment2;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Random;
import java.util.Scanner;

public class Main2 {

    static String host = "rho";
    static int port = 3030;

    public static void main(String[] args) {
        System.out.print("Upload/Download: ");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();

        boolean drop = false;

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

            File currentFile = FileHandler.getFile(input);

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
        try {
            DatagramChannel client = DatagramChannel.open();

            String contents = FileHandler.readFile(file);
            assert contents != null;
            ByteBuffer buffer = ByteBuffer.wrap(contents.getBytes());

            InetSocketAddress serverAddress = new InetSocketAddress(host, port);
            client.send(buffer, serverAddress);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static File downloadWithTCP(String filename) {
        File file = new File(filename);

        return file;
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
