package com.surepay.validation.util;

import net.jpountz.xxhash.XXHashFactory;
import net.jpountz.xxhash.StreamingXXHash64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream wrapper that computes xxHash128 hash while reading data.
 * This allows hash computation and file processing to happen in a single pass.
 */
public class HashComputingInputStream extends InputStream {
    private static final XXHashFactory xxHashFactory = XXHashFactory.fastestInstance();
    private static final long XXHASH_SEED_1 = 0L;
    private static final long XXHASH_SEED_2 = 1L;
    
    private final InputStream delegate;
    private final StreamingXXHash64 hasher1;
    private final StreamingXXHash64 hasher2;
    private String computedHash;
    private boolean hashComputed = false;
    
    public HashComputingInputStream(InputStream delegate) {
        this.delegate = delegate;
        this.hasher1 = xxHashFactory.newStreamingHash64(XXHASH_SEED_1);
        this.hasher2 = xxHashFactory.newStreamingHash64(XXHASH_SEED_2);
    }
    
    @Override
    public int read() throws IOException {
        int byteValue = delegate.read();
        if (byteValue != -1) {
            byte[] singleByte = {(byte) byteValue};
            hasher1.update(singleByte, 0, 1);
            hasher2.update(singleByte, 0, 1);
        }
        return byteValue;
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = delegate.read(b, off, len);
        if (bytesRead > 0) {
            hasher1.update(b, off, bytesRead);
            hasher2.update(b, off, bytesRead);
        }
        return bytesRead;
    }
    
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
    
    @Override
    public void close() throws IOException {
        delegate.close();
    }
    
    /**
     * Computes and returns the hash. Can be called multiple times - returns cached value after first call.
     * Should be called after all data has been read.
     */
    public String getHash() {
        if (!hashComputed) {
            long hash1 = hasher1.getValue();
            long hash2 = hasher2.getValue();
            computedHash = longPairToHex(hash1, hash2);
            hashComputed = true;
        }
        return computedHash;
    }
    
    private String longPairToHex(long high, long low) {
        String highHex = Long.toUnsignedString(high, 16);
        String lowHex = Long.toUnsignedString(low, 16);
        
        String paddedHigh = String.format("%16s", highHex).replace(' ', '0');
        String paddedLow = String.format("%16s", lowHex).replace(' ', '0');
        
        return paddedHigh + paddedLow;
    }
    
    /**
     * Static utility method to compute hash from a byte array.
     * Useful for testing and cases where data is already in memory.
     */
    public static String computeHash(byte[] data) {
        try (HashComputingInputStream hashStream = new HashComputingInputStream(new ByteArrayInputStream(data))) {
            byte[] buffer = new byte[8192];
            while (hashStream.read(buffer) != -1) {
            }
            return hashStream.getHash();
        } catch (IOException e) {
            throw new RuntimeException("Failed to compute hash from byte array", e);
        }
    }
}

