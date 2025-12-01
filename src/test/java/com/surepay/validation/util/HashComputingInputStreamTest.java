package com.surepay.validation.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashComputingInputStreamTest {

    @Test
    void shouldComputeHashForEmptyArray() {
        byte[] emptyData = new byte[0];
        
        String hash = HashComputingInputStream.computeHash(emptyData);
        
        assertThat(hash).isNotNull();
        assertThat(hash.length()).isEqualTo(32);
    }

    @Test
    void shouldComputeHashForSmallData() {
        byte[] data = "test data".getBytes();
        
        String hash = HashComputingInputStream.computeHash(data);
        
        assertThat(hash).isNotNull();
        assertThat(hash.length()).isEqualTo(32);
    }

    @Test
    void shouldComputeHashForLargeData() {
        byte[] largeData = new byte[100000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        
        String hash = HashComputingInputStream.computeHash(largeData);
        
        assertThat(hash).isNotNull();
        assertThat(hash.length()).isEqualTo(32);
    }

    @Test
    void shouldProduceSameHashForSameData() {
        byte[] data = "test data".getBytes();
        
        String hash1 = HashComputingInputStream.computeHash(data);
        String hash2 = HashComputingInputStream.computeHash(data);
        
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void shouldProduceDifferentHashForDifferentData() {
        byte[] data1 = "test data 1".getBytes();
        byte[] data2 = "test data 2".getBytes();
        
        String hash1 = HashComputingInputStream.computeHash(data1);
        String hash2 = HashComputingInputStream.computeHash(data2);
        
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void shouldProduceConsistentHashForRepeatedCalls() {
        byte[] data = "consistent test data".getBytes();
        
        String hash1 = HashComputingInputStream.computeHash(data);
        String hash2 = HashComputingInputStream.computeHash(data);
        String hash3 = HashComputingInputStream.computeHash(data);
        
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash2).isEqualTo(hash3);
    }

    @Test
    void shouldHandleSingleByte() {
        byte[] data = new byte[]{42};
        
        String hash = HashComputingInputStream.computeHash(data);
        
        assertThat(hash).isNotNull();
        assertThat(hash.length()).isEqualTo(32);
    }

    @Test
    void shouldHandleUnicodeData() {
        byte[] data = "测试数据 🚀".getBytes();
        
        String hash = HashComputingInputStream.computeHash(data);
        
        assertThat(hash).isNotNull();
        assertThat(hash.length()).isEqualTo(32);
    }

    @Test
    void shouldHandleBinaryData() {
        byte[] data = new byte[]{0, 1, 2, 3, -1, -2, -3, 127, -128};
        
        String hash = HashComputingInputStream.computeHash(data);
        
        assertThat(hash).isNotNull();
        assertThat(hash.length()).isEqualTo(32);
    }

    @Test
    void shouldProduceHexString() {
        byte[] data = "test".getBytes();
        
        String hash = HashComputingInputStream.computeHash(data);
        
        assertThat(hash).matches("[0-9a-f]{32}");
    }
}

