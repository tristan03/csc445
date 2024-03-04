package Assignment1;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class BytesHandler {
    public static byte[] longToBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }

    public static long bytesToLong(byte[] arr) {
        ByteBuffer buffer = ByteBuffer.wrap(arr);
        return buffer.getLong();
    }

    public static int bytesToInt(byte[] arr) {
        ByteBuffer buffer = ByteBuffer.wrap(arr);
        return buffer.getInt();
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

    public static int getSize(byte[] arr) {
        return arr.length;
    }
}
