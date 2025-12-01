package com.surepay.validation.validator;

import com.surepay.validation.config.ValidationProperties;
import com.surepay.validation.domain.Transaction;
import com.surepay.validation.domain.ValidationError;
import com.surepay.validation.domain.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceValidatorTest {

    private BalanceValidator validator;
    private ValidationResult result;

    @BeforeEach
    void setUp() {
        ValidationProperties properties = new ValidationProperties();
        properties.getBalance().setTolerance(new BigDecimal("0.01"));
        validator = new BalanceValidator(properties);
        result = new ValidationResult();
    }

    @Test
    void shouldAcceptCorrectBalance() {
        Transaction transaction = new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test",
            new BigDecimal("100.00"),
            new BigDecimal("-50.00"),
            new BigDecimal("50.00")
        );

        validator.validate(transaction, result);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldDetectBalanceMismatch() {
        Transaction transaction = new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test",
            new BigDecimal("100.00"),
            new BigDecimal("-50.00"),
            new BigDecimal("60.00")
        );

        validator.validate(transaction, result);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCount()).isEqualTo(1);
        assertThat(result.getErrors().get(0).errorType())
            .isEqualTo(ValidationError.ErrorType.BALANCE_MISMATCH);
    }

    @Test
    void shouldHandlePositiveMutation() {
        Transaction transaction = new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test",
            new BigDecimal("100.00"),
            new BigDecimal("50.00"),
            new BigDecimal("150.00")
        );

        validator.validate(transaction, result);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldHandleFloatingPointPrecision() {
        Transaction transaction = new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test",
            new BigDecimal("21.6"),
            new BigDecimal("-41.83"),
            new BigDecimal("-20.23")
        );

        validator.validate(transaction, result);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldHandleNegativeBalance() {
        Transaction transaction = new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test",
            new BigDecimal("10.00"),
            new BigDecimal("-20.00"),
            new BigDecimal("-10.00")
        );

        validator.validate(transaction, result);

        assertThat(result.isValid()).isTrue();
    }
}

