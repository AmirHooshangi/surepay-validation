package com.surepay.validation.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record Transaction(
    String reference,
    String accountNumber,
    String description,
    BigDecimal startBalance,
    BigDecimal mutation,
    BigDecimal endBalance
) {
    public Transaction {
        Objects.requireNonNull(reference, "Transaction reference cannot be null");
        Objects.requireNonNull(accountNumber, "Account number cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(startBalance, "Start balance cannot be null");
        Objects.requireNonNull(mutation, "Mutation cannot be null");
        Objects.requireNonNull(endBalance, "End balance cannot be null");
    }

    public BigDecimal calculateExpectedEndBalance() {
        return startBalance.add(mutation);
    }

    public boolean isBalanceCorrect(BigDecimal tolerance) {
        BigDecimal expected = calculateExpectedEndBalance();
        BigDecimal difference = endBalance.subtract(expected).abs();
        return difference.compareTo(tolerance) <= 0;
    }
}

