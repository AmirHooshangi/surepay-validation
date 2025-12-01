package com.surepay.validation.validator;

import com.surepay.validation.domain.Transaction;
import com.surepay.validation.domain.ValidationResult;

public interface TransactionValidator {
    void validate(Transaction transaction, ValidationResult result);
}

