package com.surepay.validation.reporter;

import com.surepay.validation.domain.ValidationError;
import com.surepay.validation.domain.ValidationResult;
import com.surepay.validation.dto.ValidationReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReportGeneratorTest {

    private ReportGenerator reportGenerator;

    @BeforeEach
    void setUp() {
        reportGenerator = new ReportGenerator();
    }

    @Test
    void shouldGenerateReportForValidResult() {
        ValidationResult result = new ValidationResult();
        
        ValidationReportDto report = reportGenerator.generateReportDto(result);
        
        assertThat(report.valid()).isTrue();
        assertThat(report.errorCount()).isEqualTo(0);
        assertThat(report.duplicateReferenceCount()).isEqualTo(0);
        assertThat(report.balanceMismatchCount()).isEqualTo(0);
        assertThat(report.errors()).isEmpty();
    }

    @Test
    void shouldGenerateReportWithDuplicateReferenceError() {
        ValidationResult result = new ValidationResult();
        ValidationError error = new ValidationError(
            "123456",
            "Test transaction",
            ValidationError.ErrorType.DUPLICATE_REFERENCE
        );
        result.addError(error);
        
        ValidationReportDto report = reportGenerator.generateReportDto(result);
        
        assertThat(report.valid()).isFalse();
        assertThat(report.errorCount()).isEqualTo(1);
        assertThat(report.duplicateReferenceCount()).isEqualTo(1);
        assertThat(report.balanceMismatchCount()).isEqualTo(0);
        assertThat(report.errors()).hasSize(1);
        assertThat(report.errors().get(0).transactionReference()).isEqualTo("123456");
        assertThat(report.errors().get(0).errorType()).isEqualTo("DUPLICATE_REFERENCE");
    }

    @Test
    void shouldGenerateReportWithBalanceMismatchError() {
        ValidationResult result = new ValidationResult();
        ValidationError error = new ValidationError(
            "789012",
            "Balance test",
            ValidationError.ErrorType.BALANCE_MISMATCH
        );
        result.addError(error);
        
        ValidationReportDto report = reportGenerator.generateReportDto(result);
        
        assertThat(report.valid()).isFalse();
        assertThat(report.errorCount()).isEqualTo(1);
        assertThat(report.duplicateReferenceCount()).isEqualTo(0);
        assertThat(report.balanceMismatchCount()).isEqualTo(1);
        assertThat(report.errors()).hasSize(1);
        assertThat(report.errors().get(0).transactionReference()).isEqualTo("789012");
        assertThat(report.errors().get(0).errorType()).isEqualTo("BALANCE_MISMATCH");
    }

    @Test
    void shouldGenerateReportWithMultipleErrors() {
        ValidationResult result = new ValidationResult();
        result.addError(new ValidationError("111", "First", ValidationError.ErrorType.DUPLICATE_REFERENCE));
        result.addError(new ValidationError("222", "Second", ValidationError.ErrorType.BALANCE_MISMATCH));
        result.addError(new ValidationError("333", "Third", ValidationError.ErrorType.DUPLICATE_REFERENCE));
        
        ValidationReportDto report = reportGenerator.generateReportDto(result);
        
        assertThat(report.valid()).isFalse();
        assertThat(report.errorCount()).isEqualTo(3);
        assertThat(report.duplicateReferenceCount()).isEqualTo(2);
        assertThat(report.balanceMismatchCount()).isEqualTo(1);
        assertThat(report.errors()).hasSize(3);
    }

    @Test
    void shouldIncludeErrorMessagesInReport() {
        ValidationResult result = new ValidationResult();
        ValidationError error = new ValidationError(
            "123456",
            "Test description",
            ValidationError.ErrorType.DUPLICATE_REFERENCE
        );
        result.addError(error);
        
        ValidationReportDto report = reportGenerator.generateReportDto(result);
        
        assertThat(report.errors().get(0).errorMessage()).isNotNull();
        assertThat(report.errors().get(0).errorMessage()).isNotEmpty();
        assertThat(report.errors().get(0).description()).isEqualTo("Test description");
    }

    @Test
    void shouldPreserveErrorOrder() {
        ValidationResult result = new ValidationResult();
        result.addError(new ValidationError("111", "First", ValidationError.ErrorType.DUPLICATE_REFERENCE));
        result.addError(new ValidationError("222", "Second", ValidationError.ErrorType.BALANCE_MISMATCH));
        result.addError(new ValidationError("333", "Third", ValidationError.ErrorType.DUPLICATE_REFERENCE));
        
        ValidationReportDto report = reportGenerator.generateReportDto(result);
        
        assertThat(report.errors().get(0).transactionReference()).isEqualTo("111");
        assertThat(report.errors().get(1).transactionReference()).isEqualTo("222");
        assertThat(report.errors().get(2).transactionReference()).isEqualTo("333");
    }
}

