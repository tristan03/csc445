package Assignment3;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

public class GUI {
    private JFrame frame;
    private JTextArea textArea;
    private JTextField textField;
    private JButton sendButton;

    DatagramChannel channel;
    InetSocketAddress address;

    public GUI(DatagramChannel channel, InetSocketAddress address) {
        this.channel = channel;
        this.address = address;

        frame = new JFrame("Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 700);

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        textField = new JTextField();
        sendButton = new JButton("Send");
        bottomPanel.add(textField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        ActionListener sendListener = e -> {
            String message = textField.getText();
            if (!message.isEmpty()) {
                textArea.append("You: " + message + "\n");
                textArea.setText("");
                // TODO: add code to send
            }
        };
        sendButton.addActionListener(sendListener);
        textField.addActionListener(sendListener);

        frame.setVisible(true);
    }
}
