package com.surepay.validation.parser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.surepay.validation.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class JsonTransactionParser implements TransactionParser {
    private static final Logger logger = LoggerFactory.getLogger(JsonTransactionParser.class);
    private final ObjectMapper objectMapper;

    public JsonTransactionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Stream<Transaction> parse(InputStream inputStream) throws ParseException {
        JsonParser parser = null;
        MappingIterator<Transaction> iterator = null;
        try {
            parser = objectMapper.getFactory().createParser(inputStream);
            
            JsonToken token = parser.nextToken();
            if (token != JsonToken.START_ARRAY) {
                parser.close();
                throw new ParseException("JSON root must be an array, found: " + token);
            }
            
            token = parser.nextToken();
            if (token == JsonToken.END_ARRAY) {
                parser.close();
                return Stream.empty();
            }
            
            ObjectReader reader = objectMapper.readerFor(Transaction.class);
            iterator = reader.readValues(parser);
            
            MappingIterator<Transaction> finalIterator = iterator;
            JsonParser finalParser = parser;
            
            return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, 0), 
                false
            ).onClose(() -> {
                try {
                    if (finalIterator != null) {
                        finalIterator.close();
                    }
                } catch (Exception e) {
                    logger.warn("Error closing JSON iterator", e);
                }
                try {
                    if (finalParser != null) {
                        finalParser.close();
                    }
                } catch (Exception e) {
                    logger.warn("Error closing JSON parser", e);
                }
            });
        } catch (ParseException e) {
            try {
                if (iterator != null) iterator.close();
            } catch (Exception closeEx) {
                logger.warn("Error closing iterator after parse exception", closeEx);
            }
            try {
                if (parser != null) parser.close();
            } catch (Exception closeEx) {
                logger.warn("Error closing parser after parse exception", closeEx);
            }
            throw e;
        } catch (Exception e) {
            try {
                if (iterator != null) iterator.close();
            } catch (Exception closeEx) {
                logger.warn("Error closing iterator after exception", closeEx);
            }
            try {
                if (parser != null) parser.close();
            } catch (Exception closeEx) {
                logger.warn("Error closing parser after exception", closeEx);
            }
            throw new ParseException("Failed to parse JSON file: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(String contentType) {
        return contentType != null && (
            contentType.equals("application/json") ||
            contentType.toLowerCase().endsWith(".json")
        );
    }

}

