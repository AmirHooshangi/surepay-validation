package com.surepay.validation.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Test
    void shouldCreateValidTransaction() {
        Transaction transaction = new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test transaction",
            new BigDecimal("100.00"),
            new BigDecimal("-50.00"),
            new BigDecimal("50.00")
        );
        
        assertThat(transaction.reference()).isEqualTo("123456");
        assertThat(transaction.accountNumber()).isEqualTo("NL91RABO0315273637");
        assertThat(transaction.description()).isEqualTo("Test transaction");
    }

    @Test
    void shouldCalculateExpectedEndBalance() {
        Transaction transaction = new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test",
            new BigDecimal("100.00"),
            new BigDecimal("-50.00"),
            new BigDecimal("50.00")
        );
        
        BigDecimal expected = transaction.calculateExpectedEndBalance();
        
        assertThat(expected).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void shouldDetectCorrectBalance() {
        Transaction transaction = new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test",
            new BigDecimal("100.00"),
            new BigDecimal("-50.00"),
            new BigDecimal("50.00")
        );
        
        assertThat(transaction.isBalanceCorrect(new BigDecimal("0.01"))).isTrue();
    }

    @Test
    void shouldDetectIncorrectBalance() {
        Transaction transaction = new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test",
            new BigDecimal("100.00"),
            new BigDecimal("-50.00"),
            new BigDecimal("60.00")
        );
        
        assertThat(transaction.isBalanceCorrect(new BigDecimal("0.01"))).isFalse();
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
        
        assertThat(transaction.isBalanceCorrect(new BigDecimal("0.01"))).isTrue();
    }

    @Test
    void shouldHandleToleranceWithinLimit() {
        Transaction transaction = new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test",
            new BigDecimal("100.00"),
            new BigDecimal("-50.00"),
            new BigDecimal("50.01")
        );
        
        assertThat(transaction.isBalanceCorrect(new BigDecimal("0.01"))).isTrue();
    }

    @Test
    void shouldHandleToleranceExceedingLimit() {
        Transaction transaction = new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test",
            new BigDecimal("100.00"),
            new BigDecimal("-50.00"),
            new BigDecimal("50.02")
        );
        
        assertThat(transaction.isBalanceCorrect(new BigDecimal("0.01"))).isFalse();
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
        
        assertThat(transaction.isBalanceCorrect(new BigDecimal("0.01"))).isTrue();
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
        
        assertThat(transaction.isBalanceCorrect(new BigDecimal("0.01"))).isTrue();
    }

    @Test
    void shouldHandleZeroBalance() {
        Transaction transaction = new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test",
            new BigDecimal("50.00"),
            new BigDecimal("-50.00"),
            new BigDecimal("0.00")
        );
        
        assertThat(transaction.isBalanceCorrect(new BigDecimal("0.01"))).isTrue();
    }

    @Test
    void shouldRejectNullReference() {
        assertThatThrownBy(() -> new Transaction(
            null,
            "NL91RABO0315273637",
            "Test",
            new BigDecimal("100.00"),
            new BigDecimal("-50.00"),
            new BigDecimal("50.00")
        )).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Transaction reference cannot be null");
    }

    @Test
    void shouldRejectNullAccountNumber() {
        assertThatThrownBy(() -> new Transaction(
            "123456",
            null,
            "Test",
            new BigDecimal("100.00"),
            new BigDecimal("-50.00"),
            new BigDecimal("50.00")
        )).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Account number cannot be null");
    }

    @Test
    void shouldRejectNullDescription() {
        assertThatThrownBy(() -> new Transaction(
            "123456",
            "NL91RABO0315273637",
            null,
            new BigDecimal("100.00"),
            new BigDecimal("-50.00"),
            new BigDecimal("50.00")
        )).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Description cannot be null");
    }

    @Test
    void shouldRejectNullStartBalance() {
        assertThatThrownBy(() -> new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test",
            null,
            new BigDecimal("-50.00"),
            new BigDecimal("50.00")
        )).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Start balance cannot be null");
    }

    @Test
    void shouldRejectNullMutation() {
        assertThatThrownBy(() -> new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test",
            new BigDecimal("100.00"),
            null,
            new BigDecimal("50.00")
        )).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Mutation cannot be null");
    }

    @Test
    void shouldRejectNullEndBalance() {
        assertThatThrownBy(() -> new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test",
            new BigDecimal("100.00"),
            new BigDecimal("-50.00"),
            null
        )).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("End balance cannot be null");
    }

    @Test
    void shouldHandleLargeNumbers() {
        Transaction transaction = new Transaction(
            "123456",
            "NL91RABO0315273637",
            "Test",
            new BigDecimal("999999999.99"),
            new BigDecimal("0.01"),
            new BigDecimal("1000000000.00")
        );
        
        assertThat(transaction.isBalanceCorrect(new BigDecimal("0.01"))).isTrue();
    }
}
