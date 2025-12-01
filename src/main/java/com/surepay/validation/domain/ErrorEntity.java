package com.surepay.validation.domain;

import com.surepay.validation.dto.ValidationReportDto;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "validation_errors")
public record ErrorEntity(
    @Id String id,
    @Indexed String reportId,
    String transactionReference,
    String description,
    String errorType,
    String errorMessage,
    int index
) {
    public static ErrorEntity create(
            String reportId,
            ValidationReportDto.ValidationErrorDto errorDto,
            int index) {
        return new ErrorEntity(
            null,
            reportId,
            errorDto.transactionReference(),
            errorDto.description(),
            errorDto.errorType(),
            errorDto.errorMessage(),
            index
        );
    }
}

