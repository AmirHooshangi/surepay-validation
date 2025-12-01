package com.surepay.validation.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surepay.validation.domain.Transaction;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonTransactionParserTest {

    private final JsonTransactionParser parser = new JsonTransactionParser(new ObjectMapper());

    @Test
    void shouldParseValidJson() throws Exception {
        String json = """
            [
              {
                "reference": "130498",
                "accountNumber": "NL69ABNA0433647324",
                "description": "Book Jan Theuß",
                "startBalance": 26.9,
                "mutation": -18.78,
                "endBalance": 8.12
              }
            ]
            """;

        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        List<Transaction> transactions = parser.parse(inputStream).collect(Collectors.toList());

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).reference()).isEqualTo("130498");
        assertThat(transactions.get(0).startBalance()).isEqualByComparingTo(new BigDecimal("26.9"));
    }

    @Test
    void shouldSupportJsonContentType() {
        assertThat(parser.supports("application/json")).isTrue();
        assertThat(parser.supports("file.json")).isTrue();
        assertThat(parser.supports("text/csv")).isFalse();
    }

    @Test
    void shouldRejectNonArrayJson() {
        String json = """
            {
              "reference": "130498"
            }
            """;

        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        
        assertThatThrownBy(() -> parser.parse(inputStream))
            .isInstanceOf(ParseException.class)
            .hasMessageContaining("JSON root must be an array");
    }
}

