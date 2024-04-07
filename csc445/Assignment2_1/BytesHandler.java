package Assignment2_1;

/*
    Tristan Allen
    CSC445 Assignment2
    Suny Oswego

    program to handle different byte needs
 */

import java.nio.charset.StandardCharsets;

public class BytesHandler {
    public static byte[] stringToBytes(String string) {
        return string.getBytes();
    }

    public static String bytesToString(byte[] arr) {
        return new String(arr, StandardCharsets.UTF_8);
    }
}
