package com.surepay.validation.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParserFactoryTest {

    private ParserFactory parserFactory;

    @BeforeEach
    void setUp() {
        List<TransactionParser> parsers = List.of(
            new CsvTransactionParser(),
            new JsonTransactionParser(new ObjectMapper())
        );
        parserFactory = new ParserFactory(parsers);
    }

    @Test
    void shouldGetCsvParserForCsvContentType() throws ParseException {
        TransactionParser parser = parserFactory.getParser("text/csv");
        
        assertThat(parser).isInstanceOf(CsvTransactionParser.class);
        assertThat(parser.supports("text/csv")).isTrue();
    }

    @Test
    void shouldGetJsonParserForJsonContentType() throws ParseException {
        TransactionParser parser = parserFactory.getParser("application/json");
        
        assertThat(parser).isInstanceOf(JsonTransactionParser.class);
        assertThat(parser.supports("application/json")).isTrue();
    }

    @Test
    void shouldThrowExceptionForUnsupportedContentType() {
        assertThatThrownBy(() -> parserFactory.getParser("application/xml"))
            .isInstanceOf(ParseException.class)
            .hasMessageContaining("No parser found for content type: application/xml");
    }

    @Test
    void shouldThrowExceptionForNullContentType() {
        assertThatThrownBy(() -> parserFactory.getParser(null))
            .isInstanceOf(ParseException.class)
            .hasMessageContaining("No parser found for content type: null");
    }

    @Test
    void shouldThrowExceptionForEmptyContentType() {
        assertThatThrownBy(() -> parserFactory.getParser(""))
            .isInstanceOf(ParseException.class)
            .hasMessageContaining("No parser found for content type:");
    }

    @Test
    void shouldHandleCaseInsensitiveContentType() throws ParseException {
        TransactionParser parser1 = parserFactory.getParser("text/csv");
        TransactionParser parser2 = parserFactory.getParser("text/csv");
        
        assertThat(parser1).isInstanceOf(com.surepay.validation.parser.CsvTransactionParser.class);
        assertThat(parser2).isInstanceOf(com.surepay.validation.parser.CsvTransactionParser.class);
        assertThat(parser1).isEqualTo(parser2);
    }
}

