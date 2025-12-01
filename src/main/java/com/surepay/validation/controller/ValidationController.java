package com.surepay.validation.controller;

import com.surepay.validation.dto.ValidationJobResponse;
import com.surepay.validation.service.JobService;
import com.surepay.validation.service.ValidationService;
import com.surepay.validation.util.FileSizeValidator;
import com.surepay.validation.util.FileRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/validation")
public class ValidationController {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationController.class);

    private final ValidationService validationService;
    private final JobService jobService;

    public ValidationController(
            ValidationService validationService,
            JobService jobService) {
        this.validationService = validationService;
        this.jobService = jobService;
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> validateFile(@RequestParam("file") MultipartFile file) throws IOException {
        
        logger.info("Received validation request for file: {}, size: {} bytes", 
                   file.getOriginalFilename(), file.getSize());

        var fileRequest = FileRequestUtil.processFileRequest(
            file,
            FileSizeValidator.MAX_SYNC_FILE_SIZE,
            String.format("File size (%d bytes) exceeds the maximum allowed size of 250 MB for synchronous validation." +
                    " Please use /api/v1/validation/validate/async endpoint for larger files.", file.getSize())
        );
        
        if (!fileRequest.isValid()) {
            return ResponseEntity.status(fileRequest.errorStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(fileRequest.errorResponse());
        }
        
        var result = validationService.validateAndStoreReport(
            file.getInputStream(),
            fileRequest.contentType(),
            fileRequest.filename(),
            fileRequest.fileSize()
        );

        HttpStatus status = result.validationResult().isValid() 
            ? HttpStatus.OK 
            : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
            .header("X-Report-Id", result.reportId())
            .contentType(MediaType.APPLICATION_JSON)
            .body(result.reportDto());
    }

    @PostMapping(value = "/validate/async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> validateFileAsync(
            @RequestParam("file") MultipartFile file) throws IOException {
        logger.info("Received async validation request for file: {}, size: {} bytes", 
                   file.getOriginalFilename(), file.getSize());

        var fileRequest = FileRequestUtil.processFileRequest(
            file,
            FileSizeValidator.MAX_ASYNC_FILE_SIZE,
            String.format("File size (%d bytes) exceeds the maximum allowed size of 2.5 GB for async validation.", file.getSize())
        );
        
        if (!fileRequest.isValid()) {
            return ResponseEntity.status(fileRequest.errorStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(fileRequest.errorResponse());
        }

        var job = jobService.submitJob(
            fileRequest.filename(), 
            file.getInputStream(),
            fileRequest.contentType(), 
            fileRequest.fileSize()
        );
        ValidationJobResponse response = ValidationJobResponse.from(job);

        return ResponseEntity
            .accepted()
            .location(URI.create("/api/v1/validation/jobs/" + job.jobId() + "/status"))
            .body(response);
    }

}

