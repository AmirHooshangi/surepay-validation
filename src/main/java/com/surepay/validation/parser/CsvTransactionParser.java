package com.surepay.validation.parser;

import com.opencsv.CSVReader;
import com.surepay.validation.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class CsvTransactionParser implements TransactionParser {
    private static final Logger logger = LoggerFactory.getLogger(CsvTransactionParser.class);
    private static final String[] EXPECTED_HEADERS = {
        "Reference", "AccountNumber", "Description", "Start Balance", "Mutation", "End Balance"
    };

    @Override
    public Stream<Transaction> parse(InputStream inputStream) throws ParseException {
        CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        
        try {
            String[] headers = reader.readNext();
            if (headers == null) {
                reader.close();
                return Stream.empty();
            }
            validateHeaders(headers);

            Iterator<String[]> rowIterator = new Iterator<String[]>() {
                private String[] nextRow;
                private boolean hasReadNext = false;

                @Override
                public boolean hasNext() {
                    if (!hasReadNext) {
                        try {
                            nextRow = reader.readNext();
                            hasReadNext = true;
                        } catch (Exception e) {
                            throw new RuntimeException("Error reading CSV row", e);
                        }
                    }
                    return nextRow != null;
                }

                @Override
                public String[] next() {
                    if (!hasReadNext) {
                        hasNext();
                    }
                    hasReadNext = false;
                    String[] result = nextRow;
                    nextRow = null;
                    return result;
                }
            };

            AtomicInteger rowNumber = new AtomicInteger(1);
            
            return StreamSupport.stream(
                java.util.Spliterators.spliteratorUnknownSize(rowIterator, 0),
                false
            )
            .filter(row -> row.length > 0 && !isEmptyRow(row))
            .map(row -> parseRow(row, rowNumber.getAndIncrement()))
            .filter(java.util.Objects::nonNull)
            .onClose(() -> {
                try {
                    reader.close();
                } catch (Exception e) {
                    logger.warn("Error closing CSV reader", e);
                }
            });
        } catch (ParseException e) {
            try {
                reader.close();
            } catch (Exception closeEx) {
                logger.warn("Error closing CSV reader after parse exception", closeEx);
            }
            throw e;
        } catch (Exception e) {
            try {
                reader.close();
            } catch (Exception closeEx) {
                logger.warn("Error closing CSV reader after exception", closeEx);
            }
            throw new ParseException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(String contentType) {
        return contentType != null && (
            contentType.equals("text/csv") ||
            contentType.equals("application/csv") ||
            contentType.toLowerCase().endsWith(".csv")
        );
    }

    private void validateHeaders(String[] headers) throws ParseException {
        if (headers.length < EXPECTED_HEADERS.length) {
            throw new ParseException(
                String.format("Invalid CSV format. Expected at least %d columns, found %d", 
                    EXPECTED_HEADERS.length, headers.length)
            );
        }
    }

    private boolean isEmptyRow(String[] row) {
        return row.length == 0 || (row.length == 1 && row[0].trim().isEmpty());
    }

    private Transaction parseRow(String[] row, int rowNumber) {
        try {
            if (row.length < 6) {
                logger.warn("Skipping row {}: insufficient columns (expected 6, found {})", rowNumber, row.length);
                return null;
            }

            String reference = row[0].trim();
            String accountNumber = row[1].trim();
            String description = row[2].trim();
            BigDecimal startBalance = parseDecimal(row[3].trim());
            BigDecimal mutation = parseDecimal(row[4].trim());
            BigDecimal endBalance = parseDecimal(row[5].trim());

            return new Transaction(
                reference,
                accountNumber,
                description,
                startBalance,
                mutation,
                endBalance
            );
        } catch (NumberFormatException e) {
            logger.warn("Skipping row {}: invalid number format - {}", rowNumber, e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            logger.warn("Skipping row {}: invalid data format - {}", rowNumber, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.warn("Skipping row {}: unexpected error - {}", rowNumber, e.getMessage(), e);
            return null;
        }
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot parse empty decimal value");
        }
        return new BigDecimal(value.trim());
    }
}

