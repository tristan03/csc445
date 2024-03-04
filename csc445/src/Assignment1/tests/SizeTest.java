package Assignment1.tests;

import Assignment1.XorEncryption;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class SizeTest {
    static int key = 290398103;
    static XorEncryption xorEncryption = new XorEncryption(key);

    public static void main(String[] args) {
        long message = xorEncryption.getRandomLong();

        int size = 512;

        byte[] bytes = setSize(message, size);

        System.out.println("Expected size: " + size + " bytes ");
        System.out.println("Actual size: " + bytes.length + " bytes ");
    }

    public static byte[] longToBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }

    public static byte[] setSize(long message, int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        byte[] bytes = longToBytes(message);

        int count = size / Long.BYTES;
        for (int i = 0; i < count; i++) {
            buffer.put(bytes);
        }

        int remaining = size % Long.BYTES;
        if (remaining > 0) {
            buffer.put(Arrays.copyOfRange(bytes, 0, remaining));
        }
        return buffer.array();
    }
}
