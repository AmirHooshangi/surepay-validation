package com.surepay.validation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surepay.validation.parser.CsvTransactionParser;
import com.surepay.validation.parser.JsonTransactionParser;
import com.surepay.validation.parser.ParserFactory;
import com.surepay.validation.parser.TransactionParser;
import com.surepay.validation.reporter.ReportGenerator;
import com.surepay.validation.repository.ReportRepository;
import com.surepay.validation.config.ValidationProperties;
import com.surepay.validation.validator.BalanceValidator;
import com.surepay.validation.validator.TransactionValidator;
import com.surepay.validation.validator.UniquenessValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ValidationServiceTest {

    private ValidationService service;
    
    @Mock
    private ReportRepository reportRepository;
    
    @Mock
    private ErrorService errorService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(reportRepository.existsByHash(anyString())).thenReturn(false);
        
        List<TransactionParser> parsers = List.of(
            new CsvTransactionParser(),
            new JsonTransactionParser(new ObjectMapper())
        );
        ParserFactory parserFactory = new ParserFactory(parsers);
        
        ValidationProperties validationProperties = new ValidationProperties();
        validationProperties.getBalance().setTolerance(new BigDecimal("0.01"));
        
        List<TransactionValidator> validators = List.of(
            new UniquenessValidator(),
            new BalanceValidator(validationProperties)
        );
        
        ReportGenerator reportGenerator = new ReportGenerator();
        
        service = new ValidationService(parserFactory, validators, reportGenerator, reportRepository, errorService);
    }

    @Test
    void shouldValidateValidCsvFile() throws Exception {
        String csv = """
            Reference,AccountNumber,Description,Start Balance,Mutation,End Balance
            194261,NL91RABO0315273637,Book John Smith,21.6,-41.83,-20.23
            112806,NL27SNSB0917829871,Clothes Irma Steven,91.23,+15.57,106.8
            """;

        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
        var result = service.validateAndStoreReport(
            new java.io.ByteArrayInputStream(csvBytes), "text/csv", "test.csv", csvBytes.length);

        assertThat(result.validationResult().isValid()).isTrue();
    }

    @Test
    void shouldDetectDuplicateReferences() throws Exception {
        String csv = """
            Reference,AccountNumber,Description,Start Balance,Mutation,End Balance
            112806,NL27SNSB0917829871,First,91.23,+15.57,106.8
            112806,NL69ABNA0433647324,Duplicate,90.83,-10.91,79.92
            """;

        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
        var result = service.validateAndStoreReport(
            new java.io.ByteArrayInputStream(csvBytes), "text/csv", "test.csv", csvBytes.length);

        assertThat(result.validationResult().isValid()).isFalse();
        assertThat(result.validationResult().getDuplicateReferenceCount()).isEqualTo(1);
    }

    @Test
    void shouldDetectBalanceMismatch() throws Exception {
        String csv = """
            Reference,AccountNumber,Description,Start Balance,Mutation,End Balance
            167875,NL93ABNA0585619023,Toy Greg Alysha,5429,-939,6368
            """;

        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
        var result = service.validateAndStoreReport(
            new java.io.ByteArrayInputStream(csvBytes), "text/csv", "test.csv", csvBytes.length);

        assertThat(result.validationResult().isValid()).isFalse();
        assertThat(result.validationResult().getBalanceMismatchCount()).isEqualTo(1);
    }

    @Test
    void shouldValidateValidJsonFile() throws Exception {
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

        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        var result = service.validateAndStoreReport(
            new java.io.ByteArrayInputStream(jsonBytes), "application/json", "test.json", jsonBytes.length);

        assertThat(result.validationResult().isValid()).isTrue();
    }

    @Test
    void shouldDetectMultipleErrors() throws Exception {
        String csv = """
            Reference,AccountNumber,Description,Start Balance,Mutation,End Balance
            112806,NL27SNSB0917829871,First,91.23,+15.57,106.8
            112806,NL69ABNA0433647324,Duplicate,90.83,-10.91,79.92
            167875,NL93ABNA0585619023,Balance Error,5429,-939,6368
            """;

        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
        var result = service.validateAndStoreReport(
            new java.io.ByteArrayInputStream(csvBytes), "text/csv", "test.csv", csvBytes.length);

        assertThat(result.validationResult().isValid()).isFalse();
        assertThat(result.validationResult().getErrorCount()).isEqualTo(2);
        assertThat(result.validationResult().getDuplicateReferenceCount()).isEqualTo(1);
        assertThat(result.validationResult().getBalanceMismatchCount()).isEqualTo(1);
    }
}

