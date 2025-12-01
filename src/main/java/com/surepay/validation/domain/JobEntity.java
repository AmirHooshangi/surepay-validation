package com.surepay.validation.domain;

import com.surepay.validation.dto.ValidationReportDto;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "validation_jobs")
public record JobEntity(
    @Id String jobId,
    String filename,
    JobStatus status,
    Instant createdAt,
    Instant completedAt,
    ValidationReportDto report,
    String errorMessage
) {
    public enum JobStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public static JobEntity create(String jobId, String filename) {
        return new JobEntity(
            jobId,
            filename,
            JobStatus.PENDING,
            Instant.now(),
            null,
            null,
            null
        );
    }

    public JobEntity withStatus(JobStatus status) {
        return new JobEntity(
            jobId,
            filename,
            status,
            createdAt,
            status == JobStatus.COMPLETED || status == JobStatus.FAILED 
                ? Instant.now() 
                : completedAt,
            report,
            errorMessage
        );
    }

    public JobEntity withReport(ValidationReportDto report) {
        return new JobEntity(
            jobId,
            filename,
            JobStatus.COMPLETED,
            createdAt,
            Instant.now(),
            report,
            errorMessage
        );
    }

    public JobEntity withError(String errorMessage) {
        return new JobEntity(
            jobId,
            filename,
            JobStatus.FAILED,
            createdAt,
            Instant.now(),
            report,
            errorMessage
        );
    }
}

