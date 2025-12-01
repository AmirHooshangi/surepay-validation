package com.surepay.validation.controller;

import com.surepay.validation.config.ValidationProperties;
import com.surepay.validation.dto.ValidationReportDto;
import com.surepay.validation.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;
    
    @MockitoBean
    private ValidationProperties validationProperties;
    
    @BeforeEach
    void setUp() {
        ValidationProperties.Pagination pagination = new ValidationProperties.Pagination();
        pagination.setDefaultPageSize(1000);
        pagination.setMaxPageSize(10000);
        when(validationProperties.getPagination()).thenReturn(pagination);
    }

    @Test
    void shouldGetReportWithoutErrors() throws Exception {
        ValidationReportDto report = new ValidationReportDto(true, 0, 0, 0, List.of());
        
        when(reportService.getReport("report123", false)).thenReturn(Optional.of(report));

        mockMvc.perform(get("/api/v1/validation/reports/report123"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.errorCount").value(0))
            .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void shouldGetReportWithErrors() throws Exception {
        ValidationReportDto report = new ValidationReportDto(
            false, 2, 1, 1,
            List.of(
                new ValidationReportDto.ValidationErrorDto("ref1", "desc1", "DUPLICATE_REFERENCE", "Error"),
                new ValidationReportDto.ValidationErrorDto("ref2", "desc2", "BALANCE_MISMATCH", "Error")
            )
        );
        
        when(reportService.getReport(eq("report123"), eq(true), any())).thenReturn(Optional.of(report));

        mockMvc.perform(get("/api/v1/validation/reports/report123")
                .param("errors", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.errorCount").value(2))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void shouldGetReportWithPagination() throws Exception {
        ValidationReportDto report = new ValidationReportDto(
            false, 5, 2, 3,
            List.of(
                new ValidationReportDto.ValidationErrorDto("ref1", "desc1", "DUPLICATE_REFERENCE", "Error"),
                new ValidationReportDto.ValidationErrorDto("ref2", "desc2", "BALANCE_MISMATCH", "Error")
            )
        );
        
        when(reportService.getReport(eq("report123"), eq(true), any())).thenReturn(Optional.of(report));

        mockMvc.perform(get("/api/v1/validation/reports/report123")
                .param("errors", "true")
                .param("page", "1")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errorCount").value(5))
            .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void shouldUseDefaultPaginationWhenErrorsRequested() throws Exception {
        ValidationReportDto report = new ValidationReportDto(
            false, 1, 0, 1,
            List.of(new ValidationReportDto.ValidationErrorDto("ref1", "desc1", "BALANCE_MISMATCH", "Error"))
        );
        
        when(reportService.getReport(eq("report123"), eq(true), any())).thenReturn(Optional.of(report));

        mockMvc.perform(get("/api/v1/validation/reports/report123")
                .param("errors", "true"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundForNonExistentReport() throws Exception {
        when(reportService.getReport("nonexistent", false)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/validation/reports/nonexistent"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundForNonExistentReportWithErrors() throws Exception {
        when(reportService.getReport(eq("nonexistent"), eq(true), any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/validation/reports/nonexistent")
                .param("errors", "true"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldHandleReportWithNoErrorsWhenErrorsRequested() throws Exception {
        ValidationReportDto report = new ValidationReportDto(true, 0, 0, 0, List.of());
        
        when(reportService.getReport(eq("report123"), eq(true), any())).thenReturn(Optional.of(report));

        mockMvc.perform(get("/api/v1/validation/reports/report123")
                .param("errors", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void shouldHandleLargeErrorCount() throws Exception {
        ValidationReportDto report = new ValidationReportDto(
            false, 1000, 500, 500,
            List.of()
        );
        
        when(reportService.getReport("report123", false)).thenReturn(Optional.of(report));

        mockMvc.perform(get("/api/v1/validation/reports/report123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errorCount").value(1000))
            .andExpect(jsonPath("$.duplicateReferenceCount").value(500))
            .andExpect(jsonPath("$.balanceMismatchCount").value(500));
    }
}

