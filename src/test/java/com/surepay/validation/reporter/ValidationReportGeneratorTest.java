package com.surepay.validation.reporter;

import com.surepay.validation.domain.ValidationError;
import com.surepay.validation.domain.ValidationResult;
import com.surepay.validation.domain.Transaction;
import com.surepay.validation.dto.ValidationReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationReportGeneratorTest {

    private ReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ReportGenerator();
    }

    @Test
    void shouldGenerateReportDto() {
        ValidationResult result = new ValidationResult();
        Transaction transaction1 = new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test 1",
            new BigDecimal("100.00"),
            new BigDecimal("-50.00"),
            new BigDecimal("60.00")
        );
        Transaction transaction2 = new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test 2",
            new BigDecimal("100.00"),
            new BigDecimal("-50.00"),
            new BigDecimal("50.00")
        );

        result.addError(ValidationError.balanceMismatch(transaction1));
        result.addError(ValidationError.duplicateReference(transaction2));

        ValidationReportDto reportDto = generator.generateReportDto(result);

        assertThat(reportDto.valid()).isFalse();
        assertThat(reportDto.errorCount()).isEqualTo(2);
        assertThat(reportDto.duplicateReferenceCount()).isEqualTo(1);
        assertThat(reportDto.balanceMismatchCount()).isEqualTo(1);
        assertThat(reportDto.errors()).hasSize(2);
        assertThat(reportDto.errors().get(0).transactionReference()).isEqualTo("123456");
    }



    @Test
    void shouldGenerateValidReportForEmptyErrors() {
        ValidationResult result = new ValidationResult();

        ValidationReportDto reportDto = generator.generateReportDto(result);

        assertThat(reportDto.valid()).isTrue();
        assertThat(reportDto.errorCount()).isZero();
        assertThat(reportDto.errors()).isEmpty();
    }
}

