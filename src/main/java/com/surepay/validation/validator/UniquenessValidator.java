package com.surepay.validation.validator;

import com.surepay.validation.domain.Transaction;
import com.surepay.validation.domain.ValidationError;
import com.surepay.validation.domain.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UniquenessValidator implements TransactionValidator {
    private static final ScopedValue<Set<String>> SEEN_REFERENCES = ScopedValue.newInstance();

    @Override
    public void validate(Transaction transaction, ValidationResult result) {
        String reference = transaction.reference();
        Set<String> references = SEEN_REFERENCES.get();
        
        if (!references.add(reference)) {
            result.addError(ValidationError.duplicateReference(transaction));
        }
    }
    
    public static ScopedValue<Set<String>> getScopedValue() {
        return SEEN_REFERENCES;
    }
}

