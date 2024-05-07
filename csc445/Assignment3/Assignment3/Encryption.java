package Assignment3;

import java.util.Random;

public class Encryption {
    public static long XorShift(long r) {
        r ^= r << 13;
        r ^= r >>> 7;
        r ^= r << 17;
        return r;
    }

    public static byte[] encrypt(byte[] data, long key) {
        byte[] encryptedData = new byte[data.length];

        for (int i = 0; i < data.length; i++) {
            key = XorShift(key);
            encryptedData[i] = (byte) (data[i] ^ (key & 0xFF));
        }
        return encryptedData;
    }

    public static byte[] decrypt(byte[] encryptedData, long key) {
        return encrypt(encryptedData, key);
    }

    public static long getRandomLong() {
        Random random = new Random();
        return XorShift(random.nextLong());
    }
}
