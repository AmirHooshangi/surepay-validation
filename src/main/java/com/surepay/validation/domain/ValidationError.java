package com.surepay.validation.domain;

public record ValidationError(
    String transactionReference,
    String description,
    ErrorType errorType
) {
    public enum ErrorType {
        DUPLICATE_REFERENCE("Duplicate transaction reference"),
        BALANCE_MISMATCH("End balance does not match calculated balance");

        private final String message;

        ErrorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static ValidationError duplicateReference(Transaction transaction) {
        return new ValidationError(
            transaction.reference(),
            transaction.description(),
            ErrorType.DUPLICATE_REFERENCE
        );
    }

    public static ValidationError balanceMismatch(Transaction transaction) {
        return new ValidationError(
            transaction.reference(),
            transaction.description(),
            ErrorType.BALANCE_MISMATCH
        );
    }
}

