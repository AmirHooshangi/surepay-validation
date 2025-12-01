package com.surepay.validation.validator;

import com.surepay.validation.config.ValidationProperties;
import com.surepay.validation.domain.Transaction;
import com.surepay.validation.domain.ValidationError;
import com.surepay.validation.domain.ValidationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BalanceValidator implements TransactionValidator {
    
    private final ValidationProperties validationProperties;
    
    public BalanceValidator(ValidationProperties validationProperties) {
        this.validationProperties = validationProperties;
    }

    @Override
    public void validate(Transaction transaction, ValidationResult result) {
        BigDecimal tolerance = validationProperties.getBalance().getTolerance();
        if (!transaction.isBalanceCorrect(tolerance)) {
            result.addError(ValidationError.balanceMismatch(transaction));
        }
    }
}

