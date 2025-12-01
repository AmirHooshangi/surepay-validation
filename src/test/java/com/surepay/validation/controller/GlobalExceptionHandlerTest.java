package com.surepay.validation.controller;

import com.surepay.validation.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ValidationController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.surepay.validation.service.ValidationService validationService;

    @MockitoBean
    private com.surepay.validation.service.JobService jobService;

    @Test
    void shouldHandleParseException() throws Exception {
        String csv = "invalid csv content";
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
            "file", "test.csv", "text/csv", csv.getBytes()
        );
        
        when(validationService.validateAndStoreReport(any(), any(), any(), anyLong()))
            .thenThrow(new ParseException("Invalid CSV format"));

        mockMvc.perform(multipart("/api/v1/validation/validate")
                .file(file))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("PARSE_ERROR"))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldHandleIllegalArgumentException() throws Exception {
        String csv = "test content";
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
            "file", "test.csv", "text/csv", csv.getBytes()
        );
        
        when(validationService.validateAndStoreReport(any(), any(), any(), anyLong()))
            .thenThrow(new IllegalArgumentException("Unsupported format"));

        mockMvc.perform(multipart("/api/v1/validation/validate")
                .file(file))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message").value("Unsupported format"));
    }

    @Test
    void shouldHandleIOException() throws Exception {
        String csv = "test content";
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
            "file", "test.csv", "text/csv", csv.getBytes()
        );
        
        when(validationService.validateAndStoreReport(any(), any(), any(), anyLong()))
            .thenThrow(new RuntimeException(new java.io.IOException("File read error")));

        mockMvc.perform(multipart("/api/v1/validation/validate")
                .file(file))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldHandleOutOfMemoryError() throws Exception {
        String csv = "test content";
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
            "file", "test.csv", "text/csv", csv.getBytes()
        );
        
        when(validationService.validateAndStoreReport(any(), any(), any(), anyLong()))
            .thenThrow(new RuntimeException("Out of memory"));

        mockMvc.perform(multipart("/api/v1/validation/validate")
                .file(file))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldHandleGenericException() throws Exception {
        String csv = "test content";
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
            "file", "test.csv", "text/csv", csv.getBytes()
        );
        
        when(validationService.validateAndStoreReport(any(), any(), any(), anyLong()))
            .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(multipart("/api/v1/validation/validate")
                .file(file))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.message").exists());
    }
}

