package com.surepay.validation.parser;

import com.surepay.validation.domain.Transaction;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CsvTransactionParserTest {

    private final CsvTransactionParser parser = new CsvTransactionParser();

    @Test
    void shouldParseValidCsv() throws Exception {
        String csv = """
            Reference,AccountNumber,Description,Start Balance,Mutation,End Balance
            194261,NL91RABO0315273637,Book John Smith,21.6,-41.83,-20.23
            112806,NL27SNSB0917829871,Clothes Irma Steven,91.23,+15.57,106.8
            """;

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<Transaction> transactions = parser.parse(inputStream).collect(Collectors.toList());

        assertThat(transactions).hasSize(2);
        assertThat(transactions.get(0).reference()).isEqualTo("194261");
        assertThat(transactions.get(0).startBalance()).isEqualByComparingTo(new BigDecimal("21.6"));
    }

    @Test
    void shouldHandleEmptyFile() throws Exception {
        String csv = "Reference,AccountNumber,Description,Start Balance,Mutation,End Balance\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<Transaction> transactions = parser.parse(inputStream).collect(Collectors.toList());

        assertThat(transactions).isEmpty();
    }

    @Test
    void shouldSupportCsvContentType() {
        assertThat(parser.supports("text/csv")).isTrue();
        assertThat(parser.supports("application/csv")).isTrue();
        assertThat(parser.supports("file.csv")).isTrue();
        assertThat(parser.supports("application/json")).isFalse();
    }
}

