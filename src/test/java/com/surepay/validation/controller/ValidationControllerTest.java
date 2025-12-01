package com.surepay.validation.controller;

import com.surepay.validation.domain.JobEntity;
import com.surepay.validation.dto.ValidationReportDto;
import com.surepay.validation.service.JobService;
import com.surepay.validation.service.ValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ValidationController.class)
class ValidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ValidationService validationService;

    @MockitoBean
    private JobService jobService;

    @Test
    void shouldValidateCsvFileSynchronously() throws Exception {
        String csv = """
            Reference,AccountNumber,Description,Start Balance,Mutation,End Balance
            194261,NL91RABO0315273637,Book John Smith,21.6,-41.83,-20.23
            """;
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );
        
        ValidationReportDto reportDto = new ValidationReportDto(true, 0, 0, 0, List.of());
        com.surepay.validation.domain.ValidationResult validationResult = new com.surepay.validation.domain.ValidationResult();
        ValidationService.ValidationAndStorageResult result = 
            new ValidationService.ValidationAndStorageResult(reportDto, "hash123", validationResult);
        
        when(validationService.validateAndStoreReport(any(), any(), any(), anyLong()))
            .thenReturn(result);

        mockMvc.perform(multipart("/api/v1/validation/validate")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.errorCount").value(0))
            .andExpect(header().exists("X-Report-Id"));
    }

    @Test
    void shouldReturnBadRequestForInvalidFile() throws Exception {
        String csv = """
            Reference,AccountNumber,Description,Start Balance,Mutation,End Balance
            194261,NL91RABO0315273637,Book John Smith,21.6,-41.83,-20.23
            194261,NL91RABO0315273637,Duplicate,21.6,-41.83,-20.23
            """;
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );
        
        ValidationReportDto reportDto = new ValidationReportDto(
            false, 1, 1, 0, 
            List.of(new ValidationReportDto.ValidationErrorDto("194261", "Duplicate", "DUPLICATE_REFERENCE", "Error"))
        );
        com.surepay.validation.domain.ValidationResult validationResult = new com.surepay.validation.domain.ValidationResult();
        validationResult.addError(new com.surepay.validation.domain.ValidationError("194261", "Duplicate", com.surepay.validation.domain.ValidationError.ErrorType.DUPLICATE_REFERENCE));
        ValidationService.ValidationAndStorageResult result = 
            new ValidationService.ValidationAndStorageResult(reportDto, "hash123", validationResult);
        
        when(validationService.validateAndStoreReport(any(), any(), any(), anyLong()))
            .thenReturn(result);

        mockMvc.perform(multipart("/api/v1/validation/validate")
                .file(file))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.errorCount").value(1));
    }

    @Test
    void shouldRejectEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/validation/validate")
                .file(file))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("INVALID_FILE"));
    }

    @Test
    void shouldRejectFileExceedingSyncSizeLimit() throws Exception {
        byte[] largeContent = new byte[(int) (250L * 1024 * 1024 + 1)];
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", largeContent
        );

        mockMvc.perform(multipart("/api/v1/validation/validate")
                .file(file))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("FILE_TOO_LARGE"));
    }

    @Test
    void shouldRejectUnsupportedFileFormat() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.xml", "application/octet-stream", "<xml></xml>".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/validation/validate")
                .file(file))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("UNSUPPORTED_FORMAT"));
    }

    @Test
    void shouldSubmitAsyncValidationJob() throws Exception {
        String csv = """
            Reference,AccountNumber,Description,Start Balance,Mutation,End Balance
            194261,NL91RABO0315273637,Book John Smith,21.6,-41.83,-20.23
            """;
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );
        
        JobEntity job = JobEntity.create("hash123", "test.csv");
        
        when(jobService.submitJob(anyString(), any(), anyString(), anyLong()))
            .thenReturn(job);

        mockMvc.perform(multipart("/api/v1/validation/validate/async")
                .file(file))
            .andExpect(status().isAccepted())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.jobId").value("hash123"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(header().string("Location", "/api/v1/validation/jobs/hash123/status"));
    }

    @Test
    void shouldRejectEmptyFileForAsyncValidation() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/validation/validate/async")
                .file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_FILE"));
    }

    @Test
    void shouldRejectFileExceedingAsyncSizeLimit() throws Exception {
        long size = (long) (2.5 * 1024 * 1024 * 1024 + 1);
        byte[] content = new byte[1000];
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", content
        ) {
            @Override
            public long getSize() {
                return size;
            }
        };

        mockMvc.perform(multipart("/api/v1/validation/validate/async")
                .file(file))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.error").value("FILE_TOO_LARGE"));
    }

    @Test
    void shouldHandleJsonFile() throws Exception {
        String json = """
            [
              {
                "reference": "130498",
                "accountNumber": "NL69ABNA0433647324",
                "description": "Book Jan Theuß",
                "startBalance": 26.9,
                "mutation": -18.78,
                "endBalance": 8.12
              }
            ]
            """;
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.json", "application/json", json.getBytes(StandardCharsets.UTF_8)
        );
        
        ValidationReportDto reportDto = new ValidationReportDto(true, 0, 0, 0, List.of());
        com.surepay.validation.domain.ValidationResult validationResult = new com.surepay.validation.domain.ValidationResult();
        ValidationService.ValidationAndStorageResult result = 
            new ValidationService.ValidationAndStorageResult(reportDto, "hash123", validationResult);
        
        when(validationService.validateAndStoreReport(any(), any(), any(), anyLong()))
            .thenReturn(result);

        mockMvc.perform(multipart("/api/v1/validation/validate")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void shouldDetectContentTypeFromExtension() throws Exception {
        String csv = """
            Reference,AccountNumber,Description,Start Balance,Mutation,End Balance
            194261,NL91RABO0315273637,Book John Smith,21.6,-41.83,-20.23
            """;
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "application/octet-stream", csv.getBytes(StandardCharsets.UTF_8)
        );
        
        ValidationReportDto reportDto = new ValidationReportDto(true, 0, 0, 0, List.of());
        com.surepay.validation.domain.ValidationResult validationResult = new com.surepay.validation.domain.ValidationResult();
        ValidationService.ValidationAndStorageResult result = 
            new ValidationService.ValidationAndStorageResult(reportDto, "hash123", validationResult);
        
        when(validationService.validateAndStoreReport(any(), any(), any(), anyLong()))
            .thenReturn(result);

        mockMvc.perform(multipart("/api/v1/validation/validate")
                .file(file))
            .andExpect(status().isOk());
    }
}

