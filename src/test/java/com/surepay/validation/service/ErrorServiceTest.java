package com.surepay.validation.service;

import com.surepay.validation.domain.ErrorEntity;
import com.surepay.validation.dto.ValidationReportDto;
import com.surepay.validation.repository.ErrorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorServiceTest {

    @Mock
    private ErrorRepository errorRepository;
    
    @Mock
    private MongoTemplate mongoTemplate;

    private ErrorService errorService;

    @BeforeEach
    void setUp() {
        com.surepay.validation.config.ValidationProperties validationProperties = new com.surepay.validation.config.ValidationProperties();
        validationProperties.getError().setBatchSize(1000);
        errorService = new ErrorService(errorRepository, mongoTemplate, validationProperties);
    }

    @Test
    void shouldStoreErrors() {
        String reportId = "testreport123";
        ValidationReportDto.ValidationErrorDto error1 = new ValidationReportDto.ValidationErrorDto(
            "ref1", "desc1", "DUPLICATE_REFERENCE", "Duplicate reference"
        );
        ValidationReportDto.ValidationErrorDto error2 = new ValidationReportDto.ValidationErrorDto(
            "ref2", "desc2", "BALANCE_MISMATCH", "Balance mismatch"
        );
        List<ValidationReportDto.ValidationErrorDto> errors = List.of(error1, error2);
        
        when(errorRepository.countByReportId(reportId)).thenReturn(0L);
        // insertAll returns void, so we don't need to mock it - just verify it's called

        errorService.storeErrors(reportId, errors);

        verify(mongoTemplate, atLeastOnce()).insertAll(any());
    }

    @Test
    void shouldNotStoreErrorsWhenAlreadyExist() {
        String reportId = "testreport123";
        ValidationReportDto.ValidationErrorDto error = new ValidationReportDto.ValidationErrorDto(
            "ref1", "desc1", "DUPLICATE_REFERENCE", "Duplicate"
        );
        List<ValidationReportDto.ValidationErrorDto> errors = List.of(error);
        
        when(errorRepository.countByReportId(reportId)).thenReturn(5L);

        errorService.storeErrors(reportId, errors);

        verify(mongoTemplate, never()).insertAll(any());
    }

    @Test
    void shouldNotStoreErrorsWhenListIsEmpty() {
        String reportId = "testreport123";
        
        errorService.storeErrors(reportId, java.util.List.of());

        verify(mongoTemplate, never()).insertAll(any());
    }

    @Test
    void shouldNotStoreErrorsWhenListIsNull() {
        String reportId = "testreport123";
        
        errorService.storeErrors(reportId, null);

        verify(mongoTemplate, never()).insertAll(any());
    }

    @Test
    void shouldGetAllErrors() {
        String reportId = "testreport123";
        ErrorEntity entity1 = ErrorEntity.create(reportId, 
            new ValidationReportDto.ValidationErrorDto("ref1", "desc1", "DUPLICATE_REFERENCE", "Error"), 0);
        ErrorEntity entity2 = ErrorEntity.create(reportId,
            new ValidationReportDto.ValidationErrorDto("ref2", "desc2", "BALANCE_MISMATCH", "Error"), 1);
        
        when(errorRepository.findByReportIdOrderByIndexAsc(reportId))
            .thenReturn(List.of(entity1, entity2));

        List<ValidationReportDto.ValidationErrorDto> errors = errorService.getErrors(reportId);

        assertThat(errors).hasSize(2);
        assertThat(errors.get(0).transactionReference()).isEqualTo("ref1");
        assertThat(errors.get(0).errorType()).isEqualTo("DUPLICATE_REFERENCE");
        assertThat(errors.get(1).transactionReference()).isEqualTo("ref2");
        assertThat(errors.get(1).errorType()).isEqualTo("BALANCE_MISMATCH");
    }

    @Test
    void shouldGetErrorsWithPagination() {
        String reportId = "testreport123";
        Pageable pageable = PageRequest.of(0, 10);
        
        ErrorEntity entity = ErrorEntity.create(reportId,
            new ValidationReportDto.ValidationErrorDto("ref1", "desc1", "DUPLICATE_REFERENCE", "Error"), 0);
        
        Page<ErrorEntity> entityPage = new PageImpl<>(List.of(entity), pageable, 1);
        
        when(errorRepository.findByReportIdOrderByIndexAsc(reportId, pageable))
            .thenReturn(entityPage);

        Page<ValidationReportDto.ValidationErrorDto> errorPage = errorService.getErrors(reportId, pageable);

        assertThat(errorPage.getContent()).hasSize(1);
        assertThat(errorPage.getTotalElements()).isEqualTo(1);
        assertThat(errorPage.getContent().get(0).transactionReference()).isEqualTo("ref1");
    }

    @Test
    void shouldReturnEmptyListWhenNoErrors() {
        String reportId = "testreport123";
        
        when(errorRepository.findByReportIdOrderByIndexAsc(reportId))
            .thenReturn(java.util.List.of());

        List<ValidationReportDto.ValidationErrorDto> errors = errorService.getErrors(reportId);

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldReturnEmptyPageWhenNoErrorsWithPagination() {
        String reportId = "testreport123";
        Pageable pageable = PageRequest.of(0, 10);
        
        Page<ErrorEntity> emptyPage = new PageImpl<>(java.util.List.of(), pageable, 0);
        
        when(errorRepository.findByReportIdOrderByIndexAsc(reportId, pageable))
            .thenReturn(emptyPage);

        Page<ValidationReportDto.ValidationErrorDto> errorPage = errorService.getErrors(reportId, pageable);

        assertThat(errorPage.getContent()).isEmpty();
        assertThat(errorPage.getTotalElements()).isEqualTo(0);
    }

    @Test
    void shouldCheckIfErrorsExist() {
        String reportId = "testreport123";
        
        when(errorRepository.countByReportId(reportId)).thenReturn(5L);

        boolean exists = errorService.hasErrors(reportId);

        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNoErrorsExist() {
        String reportId = "testreport123";
        
        when(errorRepository.countByReportId(reportId)).thenReturn(0L);

        boolean exists = errorService.hasErrors(reportId);

        assertThat(exists).isFalse();
    }
}

