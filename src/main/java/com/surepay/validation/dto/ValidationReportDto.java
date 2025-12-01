package com.surepay.validation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ValidationReportDto(
    @JsonProperty("valid") boolean valid,
    @JsonProperty("errorCount") int errorCount,
    @JsonProperty("duplicateReferenceCount") long duplicateReferenceCount,
    @JsonProperty("balanceMismatchCount") long balanceMismatchCount,
    @JsonProperty("errors") List<ValidationErrorDto> errors
) {
    public record ValidationErrorDto(
        @JsonProperty("transactionReference") String transactionReference,
        @JsonProperty("description") String description,
        @JsonProperty("errorType") String errorType,
        @JsonProperty("errorMessage") String errorMessage
    ) {}
}

