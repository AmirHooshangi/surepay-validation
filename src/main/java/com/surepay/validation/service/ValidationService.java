package com.surepay.validation.service;

import com.surepay.validation.domain.ValidationResult;
import com.surepay.validation.parser.ParseException;
import com.surepay.validation.parser.ParserFactory;
import com.surepay.validation.parser.TransactionParser;
import com.surepay.validation.domain.ReportEntity;
import com.surepay.validation.dto.ValidationReportDto;
import com.surepay.validation.reporter.ReportGenerator;
import com.surepay.validation.repository.ReportRepository;
import com.surepay.validation.util.HashComputingInputStream;
import com.surepay.validation.validator.TransactionValidator;
import com.surepay.validation.validator.UniquenessValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.surepay.validation.domain.ValidationError;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.lang.ScopedValue;

@Service
public class ValidationService {
    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    private final ParserFactory parserFactory;
    private final List<TransactionValidator> validators;
    private final ReportGenerator reportGenerator;
    private final ReportRepository reportRepository;
    private final ErrorService errorService;

    public ValidationService(
            ParserFactory parserFactory,
            List<TransactionValidator> validators,
            ReportGenerator reportGenerator,
            ReportRepository reportRepository,
            ErrorService errorService) {
        this.parserFactory = parserFactory;
        this.validators = validators;
        this.reportGenerator = reportGenerator;
        this.reportRepository = reportRepository;
        this.errorService = errorService;
    }

    @Transactional
    public ValidationAndStorageResult validateAndStoreReport(
            InputStream fileInputStream,
            String contentType,
            String filename,
            long fileSize) throws ParseException{
        
        HashComputingInputStream hashStream = new HashComputingInputStream(fileInputStream);
        

        String hash = hashStream.getHash();
        logger.info("Computed hash: {}", hash);
        
        if (reportRepository.existsByHash(hash)) {
            logger.info("Report with hash {} already exists, returning existing report without re-validation", hash);
            Optional<ValidationReportDto> existingReport = reportRepository.findReportDtoById(hash);
            if (existingReport.isPresent()) {
                ValidationReportDto storedReport = existingReport.get();
                List<ValidationReportDto.ValidationErrorDto> errors = errorService.getErrors(hash);
                ValidationReportDto reportDto = new ValidationReportDto(
                    storedReport.valid(),
                    storedReport.errorCount(),
                    storedReport.duplicateReferenceCount(),
                    storedReport.balanceMismatchCount(),
                    errors
                );
                
                ValidationResult resultFromDto = createValidationResultFromDto(reportDto);
                
                return new ValidationAndStorageResult(reportDto, hash, resultFromDto);
            }
        }

        ValidationResult result = validateFile(hashStream, contentType);

        ValidationReportDto reportDto = reportGenerator.generateReportDto(result);
        
        ValidationReportDto reportForStorage = new ValidationReportDto(
            reportDto.valid(),
            reportDto.errorCount(),
            reportDto.duplicateReferenceCount(),
            reportDto.balanceMismatchCount(),
            java.util.List.of()
        );
        
        ReportEntity.ReportMetadata metadata = new ReportEntity.ReportMetadata(
            filename,
            contentType,
            Instant.now(),
            fileSize
        );
        
        ReportEntity document = ReportEntity.create(hash, reportForStorage, metadata);
        reportRepository.save(document);
        logger.debug("Saved validation report to MongoDB with ID: {} (summary only, {} errors)", 
                    hash, reportDto.errorCount());
        
        errorService.storeErrors(hash, reportDto.errors());
        logger.info("Stored validation report with hash: {} (summary: {} errors)", hash, result.getErrorCount());
        
        return new ValidationAndStorageResult(reportDto, hash, result);
    }

    private ValidationResult createValidationResultFromDto(ValidationReportDto reportDto) {
        ValidationResult result = new ValidationResult();
        for (ValidationReportDto.ValidationErrorDto errorDto : reportDto.errors()) {
            ValidationError.ErrorType errorType = ValidationError.ErrorType.valueOf(errorDto.errorType());
            ValidationError error = new ValidationError(
                errorDto.transactionReference(),
                errorDto.description(),
                errorType
            );
            result.addError(error);
        }
        return result;
    }

    private ValidationResult validateFile(InputStream fileInputStream, String contentType)
            throws ParseException {
        logger.info("Starting validation for content type: {}", contentType);

        TransactionParser parser = parserFactory.getParser(contentType);

        ValidationResult result = new ValidationResult();

        long validationStartTime = System.nanoTime();

        var scopedValue = UniquenessValidator.getScopedValue();
        try {
            return ScopedValue.where(scopedValue, new HashSet<>()).call(() -> {
                try (var transactionStream = parser.parse(fileInputStream)) {
                    transactionStream.forEach(transaction -> {
                        validators.forEach(validator ->
                                validator.validate(transaction, result)
                        );
                    });
                }

                long validationEndTime = System.nanoTime();
                long totalValidationTimeMs = (validationEndTime - validationStartTime) / 1_000_000;

                logger.info("Validation completed. Found {} errors. Total time spent on validations: {} ms",
                        result.getErrorCount(), totalValidationTimeMs);
                return result;
            });
        } catch (Exception e) {
            if (e instanceof ParseException) {
                throw (ParseException) e;
            }
            throw new RuntimeException("Validation failed", e);
        }
    }

    public record ValidationAndStorageResult(
        ValidationReportDto reportDto,
        String reportId,
        ValidationResult validationResult
    ) {}
}

