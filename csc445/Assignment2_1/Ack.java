package Assignment2_1;

import java.nio.ByteBuffer;

public class Ack {
    public final int blockNumber;
    public Opcode opcode;

    public Ack(int blockNumber) {
        opcode = Opcode.ACK;
        this.blockNumber = blockNumber;
    }

    public ByteBuffer toByteBuffer() {
        int bufferSize = 8;     // opcode + seqNumber
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.putInt(opcode.opc);
        buffer.putInt(blockNumber);
        return buffer;
    }

    public int getBlockNumber() {
        return blockNumber;
    }
}
