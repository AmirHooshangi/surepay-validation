package com.surepay.validation.service;

import com.surepay.validation.config.ValidationProperties;
import com.surepay.validation.domain.ErrorEntity;
import com.surepay.validation.dto.ValidationReportDto;
import com.surepay.validation.repository.ErrorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ErrorService {
    private static final Logger logger = LoggerFactory.getLogger(ErrorService.class);
    
    private final ErrorRepository errorRepository;
    private final MongoTemplate mongoTemplate;
    private final ValidationProperties validationProperties;
    
    public ErrorService(ErrorRepository errorRepository, MongoTemplate mongoTemplate, ValidationProperties validationProperties) {
        this.errorRepository = errorRepository;
        this.mongoTemplate = mongoTemplate;
        this.validationProperties = validationProperties;
    }
    
    @Transactional
    public void storeErrors(String reportId, List<ValidationReportDto.ValidationErrorDto> errors) {
        if (errors == null || errors.isEmpty()) {
            logger.debug("No errors to store for reportId: {}", reportId);
            return;
        }
        
        if (hasErrors(reportId)) {
            logger.debug("Errors already exist for reportId: {}, skipping storage", reportId);
            return;
        }
        
        saveAllInBatches(reportId, errors);
    }
    
    private void saveAllInBatches(String reportId, List<ValidationReportDto.ValidationErrorDto> errors) {
        int BATCH_SIZE = validationProperties.getError().getBatchSize();
        int totalErrors = errors.size();
        logger.info("Storing {} errors for reportId: {} in batches of {}", totalErrors, reportId, BATCH_SIZE);
        
        IntStream.range(0, totalErrors)
            .boxed()
            .collect(Collectors.groupingBy(i -> i / BATCH_SIZE))
            .values()
            .forEach(batchIndices -> {
                List<ErrorEntity> batch = batchIndices.stream()
                    .map(i -> ErrorEntity.create(reportId, errors.get(i), i))
                    .collect(Collectors.toList());
                
                mongoTemplate.insertAll(batch);
                int startIdx = batchIndices.get(0) + 1;
                int endIdx = batchIndices.get(batchIndices.size() - 1) + 1;
                logger.debug("Stored batch {}-{} of {} errors for reportId: {}", 
                            startIdx, endIdx, totalErrors, reportId);
            });
        
        logger.info("Stored {} errors for reportId: {}", totalErrors, reportId);
    }
    
    public List<ValidationReportDto.ValidationErrorDto> getErrors(String reportId) {
        List<ErrorEntity> documents = errorRepository.findByReportIdOrderByIndexAsc(reportId);
        
        return documents.stream()
            .map(doc -> new ValidationReportDto.ValidationErrorDto(
                doc.transactionReference(),
                doc.description(),
                doc.errorType(),
                doc.errorMessage()
            ))
            .collect(Collectors.toList());
    }
    
    public Page<ValidationReportDto.ValidationErrorDto> getErrors(String reportId, Pageable pageable) {
        Page<ErrorEntity> documentPage = errorRepository.findByReportIdOrderByIndexAsc(reportId, pageable);
        
        return documentPage.map(doc -> new ValidationReportDto.ValidationErrorDto(
            doc.transactionReference(),
            doc.description(),
            doc.errorType(),
            doc.errorMessage()
        ));
    }
    
    public boolean hasErrors(String reportId) {
        return errorRepository.countByReportId(reportId) > 0;
    }
}

