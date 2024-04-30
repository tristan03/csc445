package Assignment3;

import javax.swing.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatClient {
    private static final int PORT = 3033;
    private static final long key = 349302019;
    private static final ConcurrentHashMap<Integer, Boolean> sentMessages = new ConcurrentHashMap<>(); // maps sequence number -> ack status
    private static int lastSequenceNumber;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) throws InterruptedException {
       String connectMessage = "";

        try (Scanner scanner = new Scanner(System.in); DatagramChannel channel = DatagramChannel.open()) {
            InetSocketAddress address = new InetSocketAddress("localhost", PORT);

            Thread receiverThread = new Thread(() -> {
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    while (!Thread.currentThread().isInterrupted()) {
                        buffer.clear();
                        channel.receive(buffer);
                        buffer.flip();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        bytes = Encryption.decrypt(bytes, key);
                        String message = new String(bytes);

                        int receivedSequenceNumber = 0;
                        String actualMessage = "";
                        if (!message.equals("ACK") && !message.equals("")) {
                            receivedSequenceNumber = Integer.parseInt(message.split("-")[0]);
                            actualMessage = message.split("-")[1];
                        }

                        if (!message.equalsIgnoreCase(connectMessage)) {
                            if (message.equals("ACK")) {
                                if (receivedSequenceNumber == lastSequenceNumber) {
                                    sentMessages.replace(receivedSequenceNumber, true);
                                }
                            } else {
                                System.out.println(actualMessage);
                            }
                        }

                        System.out.print("> ");
                    }
                } catch (IOException e) {
                    System.err.println("Receiver thread I/O Error: " + e.getMessage());
                }
            });
            receiverThread.start();

            Thread senderThread = new Thread(() -> {
                try {
                    // automatically send an initial message to register with the server
                    byte[] connectMessageBytes = connectMessage.getBytes();
                    connectMessageBytes = Encryption.encrypt(connectMessageBytes, key);
                    ByteBuffer connectBuffer = ByteBuffer.wrap(connectMessageBytes);
                    channel.send(connectBuffer, address);

                    int sequenceNumber = 0;

                    while (!Thread.currentThread().isInterrupted()) {
                        System.out.print("> ");
                        String message = scanner.nextLine();
                        if ("quit".equalsIgnoreCase(message)) {
                            break;
                        }

                        String messageWithSeqNum = sequenceNumber + "-" + message;

                        byte[] encryptedBytes = messageWithSeqNum.getBytes();
                        encryptedBytes = Encryption.encrypt(encryptedBytes, key);
                        ByteBuffer buffer = ByteBuffer.wrap(encryptedBytes);
                        channel.send(buffer, address);
                        sentMessages.put(sequenceNumber, false);
                        scheduleResend(sequenceNumber, buffer, address, channel);
                        lastSequenceNumber = sequenceNumber;
                        sequenceNumber++;
                    }
                } catch (IOException e) {
                    System.err.println("Sender thread I/O Error: " + e.getMessage());
                }
            });
            senderThread.start();

            senderThread.join();
            receiverThread.interrupt();
            receiverThread.join();

        } catch (IOException e) {
            System.err.println("Main thread I/O Error: " + e.getMessage());
        }
    }

    private static void scheduleResend(int sequenceNumber, ByteBuffer message, InetSocketAddress address, DatagramChannel channel) {
        scheduler.schedule(() -> {
            if (!sentMessages.getOrDefault(sequenceNumber, true)) {
                try {
                    channel.send(message, address);
                    scheduleResend(sequenceNumber, message, address, channel);
                } catch (IOException e) {
                    System.err.println("Error resending message: " + e.getMessage());
                }
            }
        }, 3, TimeUnit.SECONDS);
    }
}
