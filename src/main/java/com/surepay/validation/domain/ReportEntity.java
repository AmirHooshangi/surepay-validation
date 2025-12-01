package com.surepay.validation.domain;

import com.surepay.validation.dto.ValidationReportDto;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "validation_reports")
public record ReportEntity(
    @Id String reportId,
    ValidationReportDto report,
    String filename,
    String contentType,
    Instant timestamp,
    long fileSize
) {
    public static ReportEntity create(
            String reportId,
            ValidationReportDto report,
            ReportMetadata metadata) {
        return new ReportEntity(
            reportId,
            report,
            metadata.filename(),
            metadata.contentType(),
            metadata.timestamp(),
            metadata.fileSize()
        );
    }
    
    public record ReportMetadata(
        String filename,
        String contentType,
        Instant timestamp,
        long fileSize
    ) {}
}

