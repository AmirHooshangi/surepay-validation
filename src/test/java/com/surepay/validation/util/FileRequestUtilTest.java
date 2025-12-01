package com.surepay.validation.util;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

class FileRequestUtilTest {

    @Test
    void shouldValidateValidCsvFile() {
        MultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", "test content".getBytes()
        );
        
        FileRequestUtil.FileRequestResult result = FileRequestUtil.processFileRequest(
            file, 1024 * 1024, "File too large"
        );
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.filename()).isEqualTo("test.csv");
        assertThat(result.contentType()).isEqualTo("text/csv");
        assertThat(result.fileSize()).isEqualTo(12);
    }

    @Test
    void shouldValidateValidJsonFile() {
        MultipartFile file = new MockMultipartFile(
            "file", "test.json", "application/json", "{}".getBytes()
        );
        
        FileRequestUtil.FileRequestResult result = FileRequestUtil.processFileRequest(
            file, 1024 * 1024, "File too large"
        );
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.filename()).isEqualTo("test.json");
        assertThat(result.contentType()).isEqualTo("application/json");
    }

    @Test
    void shouldDetectContentTypeFromExtensionWhenContentTypeIsGeneric() {
        MultipartFile file = new MockMultipartFile(
            "file", "test.csv", "application/octet-stream", "test".getBytes()
        );
        
        FileRequestUtil.FileRequestResult result = FileRequestUtil.processFileRequest(
            file, 1024 * 1024, "File too large"
        );
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.contentType()).isEqualTo("text/csv");
    }

    @Test
    void shouldDetectContentTypeFromExtensionWhenContentTypeIsNull() {
        MultipartFile file = new MockMultipartFile(
            "file", "test.json", null, "{}".getBytes()
        );
        
        FileRequestUtil.FileRequestResult result = FileRequestUtil.processFileRequest(
            file, 1024 * 1024, "File too large"
        );
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.contentType()).isEqualTo("application/json");
    }

    @Test
    void shouldRejectEmptyFile() {
        MultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", new byte[0]
        );
        
        FileRequestUtil.FileRequestResult result = FileRequestUtil.processFileRequest(
            file, 1024 * 1024, "File too large"
        );
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.errorResponse().error()).isEqualTo("INVALID_FILE");
        assertThat(result.errorStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectFileExceedingMaxSize() {
        byte[] largeContent = new byte[1024 * 1024 + 1];
        MultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", largeContent
        );
        
        FileRequestUtil.FileRequestResult result = FileRequestUtil.processFileRequest(
            file, 1024 * 1024, "File too large"
        );
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.errorResponse().error()).isEqualTo("FILE_TOO_LARGE");
        assertThat(result.errorStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void shouldRejectUnsupportedFileFormat() {
        MultipartFile file = new MockMultipartFile(
            "file", "test.xml", "application/octet-stream", "<xml></xml>".getBytes()
        );
        
        FileRequestUtil.FileRequestResult result = FileRequestUtil.processFileRequest(
            file, 1024 * 1024, "File too large"
        );
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.errorResponse()).isNotNull();
        assertThat(result.errorResponse().error()).isEqualTo("UNSUPPORTED_FORMAT");
        assertThat(result.errorStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldHandleCaseInsensitiveFileExtensions() {
        MultipartFile file = new MockMultipartFile(
            "file", "test.CSV", "application/octet-stream", "test".getBytes()
        );
        
        FileRequestUtil.FileRequestResult result = FileRequestUtil.processFileRequest(
            file, 1024 * 1024, "File too large"
        );
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.contentType()).isEqualTo("text/csv");
    }

    @Test
    void shouldHandleCaseInsensitiveJsonExtension() {
        MultipartFile file = new MockMultipartFile(
            "file", "test.JSON", "application/octet-stream", "{}".getBytes()
        );
        
        FileRequestUtil.FileRequestResult result = FileRequestUtil.processFileRequest(
            file, 1024 * 1024, "File too large"
        );
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.contentType()).isEqualTo("application/json");
    }

    @Test
    void shouldHandleFileWithoutExtension() {
        MultipartFile file = new MockMultipartFile(
            "file", "testfile", "text/csv", "test".getBytes()
        );
        
        FileRequestUtil.FileRequestResult result = FileRequestUtil.processFileRequest(
            file, 1024 * 1024, "File too large"
        );
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.contentType()).isEqualTo("text/csv");
    }

    @Test
    void shouldHandleFileWithNullFilename() {
        MultipartFile file = new MockMultipartFile(
            "file", null, "text/csv", "test".getBytes()
        );
        
        FileRequestUtil.FileRequestResult result = FileRequestUtil.processFileRequest(
            file, 1024 * 1024, "File too large"
        );
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.filename()).isNotNull();
        assertThat(result.contentType()).isEqualTo("text/csv");
    }

    @Test
    void shouldIdentifyGenericContentTypes() {
        assertThat(FileRequestUtil.isGenericContentType("application/octet-stream")).isTrue();
        assertThat(FileRequestUtil.isGenericContentType("binary/octet-stream")).isTrue();
        assertThat(FileRequestUtil.isGenericContentType("application/unknown")).isTrue();
        assertThat(FileRequestUtil.isGenericContentType(null)).isTrue();
        assertThat(FileRequestUtil.isGenericContentType("text/csv")).isFalse();
        assertThat(FileRequestUtil.isGenericContentType("application/json")).isFalse();
    }

    @Test
    void shouldHandleCaseInsensitiveGenericContentType() {
        assertThat(FileRequestUtil.isGenericContentType("APPLICATION/OCTET-STREAM")).isTrue();
        assertThat(FileRequestUtil.isGenericContentType("Application/Octet-Stream")).isTrue();
    }

    @Test
    void shouldAcceptFileAtExactMaxSize() {
        byte[] content = new byte[1024 * 1024];
        MultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", content
        );
        
        FileRequestUtil.FileRequestResult result = FileRequestUtil.processFileRequest(
            file, 1024 * 1024, "File too large"
        );
        
        assertThat(result.isValid()).isTrue();
    }
}

