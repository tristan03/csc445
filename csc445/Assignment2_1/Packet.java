package Assignment2_1;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Packet {
    private final int sequenceNumber;
    private final byte[] data;
    private final Opcode opcode;

    public Packet(int sequenceNumber, byte[] data, Opcode opcode) {
        this.sequenceNumber = sequenceNumber;
        this.data = data;
        this.opcode = opcode;
    }


    public static List<Packet> makePackets(byte[] fileData) {
        List<Packet> packets = new ArrayList<>();

        int sequenceNumber = 0;
        int packetSize = 512;
        for (int i = 0; i < fileData.length; i += packetSize) {
            int end = Math.min(fileData.length, i + packetSize);
            byte[] packetData = new byte[end - i];
            System.arraycopy(fileData, i, packetData, 0, packetData.length);

            Packet packet = new Packet(sequenceNumber++, packetData, Opcode.DATA);
            packets.add(packet);
        }
        return packets;
    }

    public static void reconstructFile(Map<Integer, ByteBuffer> buffers, String outputPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            for (int i = 0; i < buffers.size(); i++) {
                ByteBuffer buffer = buffers.get(i);

                int headerSize = 8;
                buffer.position(buffer.position() + headerSize); // skip header

                while (buffer.hasRemaining()) {
//                    byte b = buffer.get();
//                    if (b == 0) {
//                        break;
//                    }
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    fos.write(data);
                }
            }
        }
        System.out.println("File saved at " + outputPath);
    }

    public ByteBuffer toByteBuffer() {
        int bufferSize = 4 + 4 + data.length; // opcode + seqNumber + data
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.putInt(opcode.opc);
        buffer.putInt(sequenceNumber);
        buffer.put(data);
        return buffer;
    }

    public byte[] getData() {
        return data;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }
}
