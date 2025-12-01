package com.surepay.validation.parser;

import com.surepay.validation.domain.Transaction;

import java.io.InputStream;
import java.util.stream.Stream;

public interface TransactionParser {
    Stream<Transaction> parse(InputStream inputStream) throws ParseException;

    boolean supports(String contentType);
}

