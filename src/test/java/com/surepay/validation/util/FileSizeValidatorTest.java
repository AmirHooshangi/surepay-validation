package com.surepay.validation.util;

import com.surepay.validation.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

class FileSizeValidatorTest {

    @Test
    void shouldAcceptFileWithinSyncLimit() {
        byte[] content = new byte[(int) (FileSizeValidator.MAX_SYNC_FILE_SIZE - 1)];
        MultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", content
        );
        
        ErrorResponse result = FileSizeValidator.validateSyncFileSize(file);
        
        assertThat(result).isNull();
    }

    @Test
    void shouldRejectFileExceedingSyncLimit() {
        byte[] content = new byte[(int) (FileSizeValidator.MAX_SYNC_FILE_SIZE + 1)];
        MultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", content
        );
        
        ErrorResponse result = FileSizeValidator.validateSyncFileSize(file);
        
        assertThat(result).isNotNull();
        assertThat(result.error()).isEqualTo("FILE_TOO_LARGE");
        assertThat(result.message()).contains("250 MB");
    }

    @Test
    void shouldAcceptFileAtExactSyncLimit() {
        byte[] content = new byte[(int) FileSizeValidator.MAX_SYNC_FILE_SIZE];
        MultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", content
        );
        
        ErrorResponse result = FileSizeValidator.validateSyncFileSize(file);
        
        assertThat(result).isNull();
    }

    @Test
    void shouldAcceptFileWithinAsyncLimit() {
        byte[] content = new byte[1000];
        MultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", content
        );
        
        ErrorResponse result = FileSizeValidator.validateAsyncFileSize(file);
        
        assertThat(result).isNull();
    }

    @Test
    void shouldRejectFileExceedingAsyncLimit() {
        long size = (long) (FileSizeValidator.MAX_ASYNC_FILE_SIZE + 1);
        byte[] content = new byte[1000];
        MultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", content
        ) {
            @Override
            public long getSize() {
                return size;
            }
        };
        
        ErrorResponse result = FileSizeValidator.validateAsyncFileSize(file);
        
        assertThat(result).isNotNull();
        assertThat(result.error()).isEqualTo("FILE_TOO_LARGE");
        assertThat(result.message()).contains("2.5 GB");
    }

    @Test
    void shouldAcceptFileAtExactAsyncLimit() {
        long size = (long) FileSizeValidator.MAX_ASYNC_FILE_SIZE;
        byte[] content = new byte[1000];
        MultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", content
        ) {
            @Override
            public long getSize() {
                return size;
            }
        };
        
        ErrorResponse result = FileSizeValidator.validateAsyncFileSize(file);
        
        assertThat(result).isNull();
    }

    @Test
    void shouldHandleEmptyFile() {
        MultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", new byte[0]
        );
        
        ErrorResponse syncResult = FileSizeValidator.validateSyncFileSize(file);
        ErrorResponse asyncResult = FileSizeValidator.validateAsyncFileSize(file);
        
        assertThat(syncResult).isNull();
        assertThat(asyncResult).isNull();
    }

    @Test
    void shouldHaveCorrectSyncLimit() {
        assertThat(FileSizeValidator.MAX_SYNC_FILE_SIZE).isEqualTo(250L * 1024 * 1024);
    }

    @Test
    void shouldHaveCorrectAsyncLimit() {
        assertThat(FileSizeValidator.MAX_ASYNC_FILE_SIZE).isEqualTo((long) (2.5 * 1024 * 1024 * 1024));
    }
}

