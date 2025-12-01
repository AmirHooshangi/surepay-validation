package com.surepay.validation.util;

import com.surepay.validation.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

public class FileSizeValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(FileSizeValidator.class);

    public static final long MAX_SYNC_FILE_SIZE = 250L * 1024 * 1024;
    public static final long MAX_ASYNC_FILE_SIZE = (long) (2.5 * 1024 * 1024 * 1024);
    
    public static ErrorResponse validateSyncFileSize(MultipartFile file) {
        long fileSize = file.getSize();
        if (fileSize > MAX_SYNC_FILE_SIZE) {
            logger.warn("File size {} bytes exceeds maximum allowed size {} bytes for synchronous validation", 
                       fileSize, MAX_SYNC_FILE_SIZE);
            return ErrorResponse.of("FILE_TOO_LARGE", 
                String.format("File size (%d bytes) exceeds the maximum allowed size of 250 MB for synchronous validation. Please use /api/v1/validation/validate/async endpoint for larger files.", fileSize));
        }
        return null;
    }
    
    public static ErrorResponse validateAsyncFileSize(MultipartFile file) {
        long fileSize = file.getSize();
        if (fileSize > MAX_ASYNC_FILE_SIZE) {
            logger.warn("File size {} bytes exceeds maximum allowed size {} bytes for async validation", 
                       fileSize, MAX_ASYNC_FILE_SIZE);
            return ErrorResponse.of("FILE_TOO_LARGE", 
                String.format("File size (%d bytes) exceeds the maximum allowed size of 2.5 GB for async validation.", fileSize));
        }
        return null;
    }
}

