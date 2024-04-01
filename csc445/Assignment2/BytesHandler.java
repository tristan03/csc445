package Assignment2;

import java.nio.charset.StandardCharsets;

public class BytesHandler {
    public static byte[] stringToBytes(String string) {
        return string.getBytes();
    }

    public static String bytesToString(byte[] arr) {
        return new String(arr, StandardCharsets.UTF_8);
    }
}
