package com.surepay.validation.reporter;

import com.surepay.validation.domain.ValidationError;
import com.surepay.validation.domain.ValidationResult;
import com.surepay.validation.dto.ValidationReportDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReportGenerator {

    public ReportGenerator() {

    }

    public ValidationReportDto generateReportDto(ValidationResult result) {
        List<ValidationError> errors = result.getErrors();
        
        List<ValidationReportDto.ValidationErrorDto> errorDtos = errors.stream()
            .map(error -> new ValidationReportDto.ValidationErrorDto(
                error.transactionReference(),
                error.description(),
                error.errorType().name(),
                error.errorType().getMessage()
            ))
            .collect(Collectors.toList());

        return new ValidationReportDto(
            result.isValid(),
            result.getErrorCount(),
            result.getDuplicateReferenceCount(),
            result.getBalanceMismatchCount(),
            errorDtos
        );
    }

}

