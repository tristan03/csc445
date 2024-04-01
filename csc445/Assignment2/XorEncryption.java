package Assignment2;

import java.util.Random;

public class XorEncryption {
    public XorEncryption(long key) {
        if (key == 0) {
            throw new IllegalArgumentException("Key cannot be zero. ");
        }
    }

    public long XorShift(long r) {
        r ^= r << 13;
        r ^= r >>> 7;
        r ^= r << 17;
        return r;
    }

    public byte[] encrypt(byte[] data, long key) {
        byte[] encryptedData = new byte[data.length];

        for (int i = 0; i < data.length; i++) {
            key = XorShift(key);
            encryptedData[i] = (byte) (data[i] ^ (key & 0xFF));
        }
        return encryptedData;
    }

    public byte[] decrypt(byte[] encryptedData, long key) {
        return encrypt(encryptedData, key);
    }

    public long getRandomLong() {
        Random random = new Random();
        return XorShift(random.nextLong());
    }
}
