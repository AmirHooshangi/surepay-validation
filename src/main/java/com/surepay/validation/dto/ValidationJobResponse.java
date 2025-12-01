package com.surepay.validation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.surepay.validation.domain.JobEntity;

import java.time.Instant;

public record ValidationJobResponse(
    @JsonProperty("jobId") String jobId,
    @JsonProperty("filename") String filename,
    @JsonProperty("status") String status,
    @JsonProperty("createdAt") Instant createdAt,
    @JsonProperty("completedAt") Instant completedAt,
    @JsonProperty("report") ValidationReportDto report,
    @JsonProperty("errorMessage") String errorMessage
) {
    public static ValidationJobResponse from(JobEntity job) {
        return new ValidationJobResponse(
            job.jobId(),
            job.filename(),
            job.status().name(),
            job.createdAt(),
            job.completedAt(),
            job.report(),
            job.errorMessage()
        );
    }
}

