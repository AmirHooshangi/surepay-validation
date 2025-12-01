package com.surepay.validation.util;

import com.surepay.validation.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Pattern;

public class FileRequestUtil {
    
    private static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile("\\.([^.]+)$", Pattern.CASE_INSENSITIVE);

    public record FileValidationResult(
        String filename,
        String contentType,
        ErrorResponse errorResponse
    ) {}
    
    public record FileRequestResult(
        String filename,
        String contentType,
        long fileSize,
        ErrorResponse errorResponse,
        HttpStatus errorStatus
    ) {
        public boolean isValid() {
            return errorResponse == null;
        }
    }

    public static FileValidationResult validateFileInput(MultipartFile file) {
        if (file.isEmpty()) {
            return new FileValidationResult(
                null, 
                null, 
                ErrorResponse.of("INVALID_FILE", "File is empty")
            );
        }

        String filename = file.getOriginalFilename() != null 
            ? file.getOriginalFilename() 
            : "unknown";

        try {
            String contentType = determineContentType(file);
            return new FileValidationResult(filename, contentType, null);
        } catch (IllegalArgumentException e) {
            return new FileValidationResult(
                null,
                null,
                ErrorResponse.of("UNSUPPORTED_FORMAT", e.getMessage())
            );
        }
    }

    public static String determineContentType(MultipartFile file) {
        String contentType = file.getContentType();
        
        if (contentType != null && !contentType.isEmpty() && 
            !isGenericContentType(contentType)) {
            return contentType;
        }

        String filename = file.getOriginalFilename();
        if (filename != null) {
            var matcher = FILE_EXTENSION_PATTERN.matcher(filename);
            if (matcher.find()) {
                String extension = "." + matcher.group(1).toLowerCase();
                return switch (extension) {
                    case ".csv" -> "text/csv";
                    case ".json" -> "application/json";
                    default -> throw new IllegalArgumentException(
                        "Unsupported file format. Only CSV and JSON files are supported. Found: " + extension);
                };
            }
        }

        throw new IllegalArgumentException(
            "Cannot determine content type. Please provide a file with .csv or .json extension, or set the Content-Type header.");
    }

    public static boolean isGenericContentType(String contentType) {
        if (contentType == null) {
            return true;
        }
        String lower = contentType.toLowerCase();
        return lower.equals("application/octet-stream") || 
               lower.equals("binary/octet-stream") ||
               lower.equals("application/unknown");
    }
    
    public static FileRequestResult processFileRequest(
            MultipartFile file, 
            long maxFileSize, 
            String sizeErrorMessage) {
        
        FileValidationResult inputResult = validateFileInput(file);
        if (inputResult.errorResponse() != null) {
            return new FileRequestResult(
                null,
                null,
                0,
                inputResult.errorResponse(),
                HttpStatus.BAD_REQUEST
            );
        }
        
        long fileSize = file.getSize();
        if (fileSize > maxFileSize) {
            ErrorResponse sizeError = ErrorResponse.of("FILE_TOO_LARGE", sizeErrorMessage);
            return new FileRequestResult(
                inputResult.filename(),
                inputResult.contentType(),
                fileSize,
                sizeError,
                HttpStatus.PAYLOAD_TOO_LARGE
            );
        }
        
        return new FileRequestResult(
            inputResult.filename(),
            inputResult.contentType(),
            fileSize,
            null,
            null
        );
    }
}

