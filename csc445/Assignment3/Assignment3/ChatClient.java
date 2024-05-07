package Assignment3;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.*;

public class ChatClient {
    /*
     TODO:
      Fix disconnecting. I want it to send to everyone "[clientAddress] has disconnected"
      also add it to say "[clientAddress] has connected".
      add it to make gui pop ups of any errors. a separate window for ex. "error sending/receiving message"
      then possibly add username + passwords. this would add 2 things:
      1. more secure to only allow registered users to join and talk
      2. makes for an easy way to distinguish between messages. right now there is no way to tell the difference
        between 2 different clients
        for example,
            You: yo
            Eric: yoo
            Steve: whats up guys
        instead of
            You: yo
            yoo
            whats up guys
        and this could also replace [clientAddress] above ^
        but if this ends up being too complicated, i'm sure just showing client addresses would be fine, considering
        this is a fake chat network anyway
      might want to also consider looking into consensus a bit and see if there is any other ways of maintaining
      order that i could add other than ACK ing messages + sending messages with sequence numbers
      after this, i can't really think of much else i could add
      maybe add some more error handling with resending messages
     */


    private static final int PORT = 3030;
    private static final long key = 349302019;
    private static ConcurrentHashMap<Integer, Boolean> sentMessages = new ConcurrentHashMap<>();
    private static int sequenceNumber = 0;
    private static DatagramChannel channel;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static JTextArea textArea;
    private static JTextField textField;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        textField = new JTextField();
        JButton sendButton = new JButton("Send");
        bottomPanel.add(textField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        try {
            channel = DatagramChannel.open();
            InetSocketAddress address = new InetSocketAddress("localhost", PORT);

            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    String disconnectMessage = "disconnect";
                    ByteBuffer disconnectBuffer = ByteBuffer.wrap(Encryption.encrypt(disconnectMessage.getBytes(), key));
                    try {
                        channel.send(disconnectBuffer, address);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    System.exit(0);
                }
            });

            connectWithServer(address);

            ActionListener sendListener = e -> {
                String message = textField.getText();
                if (!message.isEmpty()) {
                    textArea.append("You: " + message + "\n");
                    sendMessage(message, address);
                    textField.setText("");
                }
            };
            sendButton.addActionListener(sendListener);
            textField.addActionListener(sendListener);

            frame.setVisible(true);

            receiveMessages();

        } catch (IOException e) {
            showErrorPopup("Error connecting with the server. ");
        }
    }

    private static void connectWithServer(InetSocketAddress address) {
        try {
            String connectMessage = "connect";
            byte[] messageBytes = Encryption.encrypt(connectMessage.getBytes(), key);
            ByteBuffer buffer = ByteBuffer.wrap(messageBytes);
            channel.socket().setSoTimeout(10000);
            channel.send(buffer, address);
        } catch (IOException e) {
            showErrorPopup("Error connecting with server. Attempting to retry... ");
            connectWithServer(address);
        }
    }

    private static void sendMessage(String message, InetSocketAddress address) {
        new Thread(() -> {
            try {
                String messageWithSeqNum = sequenceNumber + "-" + message;
                byte[] messageBytes = Encryption.encrypt(messageWithSeqNum.getBytes(), key);
                ByteBuffer buffer = ByteBuffer.wrap(messageBytes);
                channel.send(buffer, address);
                sentMessages.put(sequenceNumber, false);
                scheduleResend(sequenceNumber, buffer, address, channel);
                sequenceNumber++;
            } catch (IOException e) {
                showErrorPopup("Error sending message. Please try again. ");
            }
        }).start();
    }

    private static void receiveMessages() {
        new Thread(() -> {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                while (true) {
                    buffer.clear();
                    channel.receive(buffer);
                    buffer.flip();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    String message = new String(Encryption.decrypt(bytes, key));

                    if (!message.equals("disconnect")) {
                        String receivedClientAddress = "";
                        String actualMessage;

                        try {
                            int ackSequenceNumber = Integer.parseInt(message.split("-")[0]);
                            sentMessages.replace(ackSequenceNumber, true);
                            continue;
                        } catch (Exception ignored) {
                            // do nothing, message is not an ACK
                        }

                        try {
                            actualMessage = message.split("-")[0];
                            receivedClientAddress = message.split("-")[1];
                        } catch (Exception ignored) {
                            actualMessage = message;
                        }
                        String finalActualMessage = actualMessage;
                        String finalReceivedClientAddress = receivedClientAddress;
                        SwingUtilities.invokeLater(() -> textArea.append(finalReceivedClientAddress + ": " + finalActualMessage + "\n"));
                    }
                }
            } catch (IOException e) {
                System.err.println("Error receiving messages: " + e.getMessage());
            }
        }).start();
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

    private static void showErrorPopup(String error) {
        JOptionPane.showMessageDialog(null, error, "Error", JOptionPane.ERROR_MESSAGE);
    }
}