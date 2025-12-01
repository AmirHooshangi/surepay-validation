package com.surepay.validation.validator;

import com.surepay.validation.domain.Transaction;
import com.surepay.validation.domain.ValidationError;
import com.surepay.validation.domain.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.lang.ScopedValue;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class UniquenessValidatorTest {

    private UniquenessValidator validator;
    private ValidationResult result;

    @BeforeEach
    void setUp() {
        validator = new UniquenessValidator();
        result = new ValidationResult();
    }

    @Test
    void shouldAcceptUniqueReferences() throws Exception {
        Transaction transaction = createTransaction("123456", "Test");

        var scopedValue = UniquenessValidator.getScopedValue();
        ScopedValue.where(scopedValue, new HashSet<String>()).run(() -> {
            validator.validate(transaction, result);
        });

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorCount()).isZero();
    }

    @Test
    void shouldDetectDuplicateReferences() throws Exception {
        Transaction first = createTransaction("123456", "First");
        Transaction duplicate = createTransaction("123456", "Duplicate");

        var scopedValue = UniquenessValidator.getScopedValue();
        ScopedValue.where(scopedValue, new HashSet<String>()).run(() -> {
            validator.validate(first, result);
            validator.validate(duplicate, result);
        });

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCount()).isEqualTo(1);
        assertThat(result.getErrors().get(0).errorType())
            .isEqualTo(ValidationError.ErrorType.DUPLICATE_REFERENCE);
    }

    @Test
    void shouldResetState() throws Exception {
        Transaction first = createTransaction("123456", "First");
        Transaction duplicate = createTransaction("123456", "Duplicate");

        var scopedValue = UniquenessValidator.getScopedValue();
        ScopedValue.where(scopedValue, new HashSet<String>()).run(() -> {
            validator.validate(first, result);
        });
        
        ValidationResult newResult = new ValidationResult();
        ScopedValue.where(scopedValue, new HashSet<String>()).run(() -> {
            validator.validate(duplicate, newResult);
        });

        assertThat(newResult.isValid()).isTrue();
    }

    @Test
    void shouldHandleMultipleDuplicates() throws Exception {
        Transaction first = createTransaction("123456", "First");
        Transaction duplicate1 = createTransaction("123456", "Duplicate 1");
        Transaction duplicate2 = createTransaction("123456", "Duplicate 2");
        Transaction unique = createTransaction("789012", "Unique");

        var scopedValue = UniquenessValidator.getScopedValue();
        ScopedValue.where(scopedValue, new HashSet<String>()).run(() -> {
            validator.validate(first, result);
            validator.validate(duplicate1, result);
            validator.validate(duplicate2, result);
            validator.validate(unique, result);
        });

        assertThat(result.getErrorCount()).isEqualTo(2);
        assertThat(result.getDuplicateReferenceCount()).isEqualTo(2);
    }

    @Test
    void shouldIsolateStateBetweenSequentialValidationsOnSameThread() throws Exception {
        Transaction file1Transaction = createTransaction("123456", "File 1");
        Transaction file2Transaction = createTransaction("123456", "File 2");
        
        ValidationResult file1Result = new ValidationResult();
        ValidationResult file2Result = new ValidationResult();
        
        var scopedValue = UniquenessValidator.getScopedValue();
        
        ScopedValue.where(scopedValue, new HashSet<String>()).run(() -> {
            validator.validate(file1Transaction, file1Result);
        });
        
        ScopedValue.where(scopedValue, new HashSet<String>()).run(() -> {
            validator.validate(file2Transaction, file2Result);
        });
        
        assertThat(file1Result.isValid()).isTrue();
        assertThat(file2Result.isValid()).isTrue();
        assertThat(file1Result.getErrorCount()).isZero();
        assertThat(file2Result.getErrorCount()).isZero();
    }

    private Transaction createTransaction(String reference, String description) {
        return new Transaction(
            reference,
            "NL91RABO0315273637",
            description,
            new BigDecimal("100.00"),
            new BigDecimal("-50.00"),
            new BigDecimal("50.00")
        );
    }
}

