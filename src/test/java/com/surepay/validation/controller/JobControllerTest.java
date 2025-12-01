package com.surepay.validation.controller;

import com.surepay.validation.domain.JobEntity;
import com.surepay.validation.dto.ValidationReportDto;
import com.surepay.validation.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @Test
    void shouldGetJobStatusForPendingJob() throws Exception {
        JobEntity job = new JobEntity(
            "job123", "test.csv", JobEntity.JobStatus.PENDING,
            Instant.now(), null, null, null
        );
        
        when(jobService.getJob("job123")).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/v1/validation/jobs/job123/status"))
            .andExpect(status().isAccepted())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.jobId").value("job123"))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldGetJobStatusForProcessingJob() throws Exception {
        JobEntity job = new JobEntity(
            "job123", "test.csv", JobEntity.JobStatus.PROCESSING,
            Instant.now(), null, null, null
        );
        
        when(jobService.getJob("job123")).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/v1/validation/jobs/job123/status"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void shouldGetJobStatusForCompletedJob() throws Exception {
        ValidationReportDto report = new ValidationReportDto(true, 0, 0, 0, List.of());
        JobEntity job = new JobEntity(
            "job123", "test.csv", JobEntity.JobStatus.COMPLETED,
            Instant.now(), Instant.now(), report, null
        );
        
        when(jobService.getJob("job123")).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/v1/validation/jobs/job123/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.report").exists());
    }

    @Test
    void shouldGetJobStatusForFailedJob() throws Exception {
        JobEntity job = new JobEntity(
            "job123", "test.csv", JobEntity.JobStatus.FAILED,
            Instant.now(), Instant.now(), null, "Error message"
        );
        
        when(jobService.getJob("job123")).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/v1/validation/jobs/job123/status"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.errorMessage").value("Error message"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentJob() throws Exception {
        when(jobService.getJob("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/validation/jobs/nonexistent/status"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetJobResultForCompletedJob() throws Exception {
        ValidationReportDto report = new ValidationReportDto(true, 0, 0, 0, List.of());
        JobEntity job = new JobEntity(
            "job123", "test.csv", JobEntity.JobStatus.COMPLETED,
            Instant.now(), Instant.now(), report, null
        );
        
        when(jobService.getJob("job123")).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/v1/validation/jobs/job123/result"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void shouldReturnAcceptedForProcessingJobResult() throws Exception {
        JobEntity job = new JobEntity(
            "job123", "test.csv", JobEntity.JobStatus.PROCESSING,
            Instant.now(), null, null, null
        );
        
        when(jobService.getJob("job123")).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/v1/validation/jobs/job123/result"))
            .andExpect(status().isAccepted());
    }

    @Test
    void shouldReturnInternalServerErrorForFailedJobResult() throws Exception {
        JobEntity job = new JobEntity(
            "job123", "test.csv", JobEntity.JobStatus.FAILED,
            Instant.now(), Instant.now(), null, "Error"
        );
        
        when(jobService.getJob("job123")).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/v1/validation/jobs/job123/result"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldReturnNotFoundForNonExistentJobResult() throws Exception {
        when(jobService.getJob("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/validation/jobs/nonexistent/result"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnAcceptedForPendingJobResult() throws Exception {
        JobEntity job = new JobEntity(
            "job123", "test.csv", JobEntity.JobStatus.PENDING,
            Instant.now(), null, null, null
        );
        
        when(jobService.getJob("job123")).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/v1/validation/jobs/job123/result"))
            .andExpect(status().isAccepted());
    }

    @Test
    void shouldGetJobResultWithErrors() throws Exception {
        ValidationReportDto report = new ValidationReportDto(
            false, 2, 1, 1,
            List.of(
                new ValidationReportDto.ValidationErrorDto("ref1", "desc1", "DUPLICATE_REFERENCE", "Error"),
                new ValidationReportDto.ValidationErrorDto("ref2", "desc2", "BALANCE_MISMATCH", "Error")
            )
        );
        JobEntity job = new JobEntity(
            "job123", "test.csv", JobEntity.JobStatus.COMPLETED,
            Instant.now(), Instant.now(), report, null
        );
        
        when(jobService.getJob("job123")).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/v1/validation/jobs/job123/result"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.errorCount").value(2))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors.length()").value(2));
    }
}

