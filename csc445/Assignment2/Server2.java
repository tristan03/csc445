package Assignment2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class Server2 {
    public static void main(String[] args) {
        int port = 3030;

        try {
            DatagramChannel server = DatagramChannel.open();

            InetSocketAddress address = new InetSocketAddress(port);
            server.bind(address);

            System.out.println("Listening on port " + port);

            ByteBuffer buffer = ByteBuffer.allocate(1024);

            server.receive(buffer);

            buffer.flip();
            while (buffer.hasRemaining()) {
                System.out.println(buffer.get());
            }
            System.out.println();

            server.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
