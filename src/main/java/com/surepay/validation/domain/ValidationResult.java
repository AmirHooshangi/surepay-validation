package com.surepay.validation.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationResult {
    private final List<ValidationError> errors = Collections.synchronizedList(new ArrayList<>());

    public void addError(ValidationError error) {
        errors.add(error);
    }

    public void merge(ValidationResult other) {
        this.errors.addAll(other.errors);
    }

    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public int getErrorCount() {
        return errors.size();
    }

    public long getDuplicateReferenceCount() {
        return errors.stream()
            .filter(error -> error.errorType() == ValidationError.ErrorType.DUPLICATE_REFERENCE)
            .count();
    }

    public long getBalanceMismatchCount() {
        return errors.stream()
            .filter(error -> error.errorType() == ValidationError.ErrorType.BALANCE_MISMATCH)
            .count();
    }
}

