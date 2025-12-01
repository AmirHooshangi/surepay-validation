package com.surepay.validation.parser;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParserFactory {
    private final List<TransactionParser> parsers;

    public ParserFactory(List<TransactionParser> parsers) {
        this.parsers = parsers;
    }

    public TransactionParser getParser(String contentType) throws ParseException {
        return parsers.stream()
            .filter(parser -> parser.supports(contentType))
            .findFirst()
            .orElseThrow(() -> new ParseException(
                "No parser found for content type: " + contentType
            ));
    }
}

