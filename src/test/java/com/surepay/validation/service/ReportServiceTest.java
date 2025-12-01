package com.surepay.validation.service;

import com.surepay.validation.dto.ValidationReportDto;
import com.surepay.validation.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ErrorService errorService;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportRepository, errorService);
    }

    @Test
    void shouldGetReportWithoutErrors() {
        String reportId = "testreport123";
        ValidationReportDto reportDto = new ValidationReportDto(
            true, 0, 0, 0, java.util.List.of()
        );
        
        when(reportRepository.findReportDtoById(reportId)).thenReturn(Optional.of(reportDto));

        Optional<ValidationReportDto> result = reportService.getReport(reportId, false);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(reportDto);
        assertThat(result.get().errors()).isEmpty();
        verify(errorService, never()).getErrors(anyString(), any(Pageable.class));
    }

    @Test
    void shouldGetReportWithErrors() {
        String reportId = "testreport123";
        ValidationReportDto reportDto = new ValidationReportDto(
            false, 2, 1, 1, java.util.List.of()
        );
        
        ValidationReportDto.ValidationErrorDto error1 = new ValidationReportDto.ValidationErrorDto(
            "ref1", "desc1", "DUPLICATE_REFERENCE", "Duplicate"
        );
        ValidationReportDto.ValidationErrorDto error2 = new ValidationReportDto.ValidationErrorDto(
            "ref2", "desc2", "BALANCE_MISMATCH", "Balance mismatch"
        );
        
        Page<ValidationReportDto.ValidationErrorDto> errorPage = new PageImpl<>(
            List.of(error1, error2)
        );
        
        when(reportRepository.findReportDtoById(reportId)).thenReturn(Optional.of(reportDto));
        when(errorService.getErrors(eq(reportId), any(Pageable.class))).thenReturn(errorPage);

        Optional<ValidationReportDto> result = reportService.getReport(reportId, true);

        assertThat(result).isPresent();
        assertThat(result.get().errors()).hasSize(2);
        assertThat(result.get().errorCount()).isEqualTo(2);
        assertThat(result.get().duplicateReferenceCount()).isEqualTo(1);
        assertThat(result.get().balanceMismatchCount()).isEqualTo(1);
        verify(errorService).getErrors(eq(reportId), any(Pageable.class));
    }

    @Test
    void shouldGetReportWithPagination() {
        String reportId = "testreport123";
        ValidationReportDto reportDto = new ValidationReportDto(
            false, 5, 2, 3, java.util.List.of()
        );
        
        Pageable pageable = PageRequest.of(1, 2);
        Page<ValidationReportDto.ValidationErrorDto> errorPage = new PageImpl<>(
            List.of(
                new ValidationReportDto.ValidationErrorDto("ref3", "desc3", "BALANCE_MISMATCH", "Error"),
                new ValidationReportDto.ValidationErrorDto("ref4", "desc4", "BALANCE_MISMATCH", "Error")
            ),
            pageable,
            5
        );
        
        when(reportRepository.findReportDtoById(reportId)).thenReturn(Optional.of(reportDto));
        when(errorService.getErrors(reportId, pageable)).thenReturn(errorPage);

        Optional<ValidationReportDto> result = reportService.getReport(reportId, true, pageable);

        assertThat(result).isPresent();
        assertThat(result.get().errors()).hasSize(2);
        assertThat(result.get().errorCount()).isEqualTo(5);
        verify(errorService).getErrors(reportId, pageable);
    }

    @Test
    void shouldReturnEmptyWhenReportNotFound() {
        String reportId = "nonexistent";
        
        when(reportRepository.findReportDtoById(reportId)).thenReturn(Optional.empty());

        Optional<ValidationReportDto> result = reportService.getReport(reportId, false);

        assertThat(result).isEmpty();
        verify(errorService, never()).getErrors(anyString(), any(Pageable.class));
    }

    @Test
    void shouldReturnEmptyWhenReportNotFoundWithErrors() {
        String reportId = "nonexistent";
        
        when(reportRepository.findReportDtoById(reportId)).thenReturn(Optional.empty());

        Optional<ValidationReportDto> result = reportService.getReport(reportId, true);

        assertThat(result).isEmpty();
        verify(errorService, never()).getErrors(anyString(), any(Pageable.class));
    }

    @Test
    void shouldUseDefaultPaginationWhenErrorsRequested() {
        String reportId = "testreport123";
        ValidationReportDto reportDto = new ValidationReportDto(
            false, 1, 0, 1, java.util.List.of()
        );
        
        Page<ValidationReportDto.ValidationErrorDto> errorPage = new PageImpl<>(
            List.of(new ValidationReportDto.ValidationErrorDto("ref1", "desc1", "BALANCE_MISMATCH", "Error"))
        );
        
        when(reportRepository.findReportDtoById(reportId)).thenReturn(Optional.of(reportDto));
        when(errorService.getErrors(eq(reportId), any(Pageable.class))).thenReturn(errorPage);

        Optional<ValidationReportDto> result = reportService.getReport(reportId, true);

        assertThat(result).isPresent();
        verify(errorService).getErrors(eq(reportId), argThat(pageable -> 
            pageable.getPageNumber() == 0 && pageable.getPageSize() == 1000
        ));
    }

    @Test
    void shouldHandleEmptyErrorList() {
        String reportId = "testreport123";
        ValidationReportDto reportDto = new ValidationReportDto(
            true, 0, 0, 0, java.util.List.of()
        );
        
        Page<ValidationReportDto.ValidationErrorDto> emptyPage = new PageImpl<>(java.util.List.of());
        
        when(reportRepository.findReportDtoById(reportId)).thenReturn(Optional.of(reportDto));
        when(errorService.getErrors(eq(reportId), any(Pageable.class))).thenReturn(emptyPage);

        Optional<ValidationReportDto> result = reportService.getReport(reportId, true);

        assertThat(result).isPresent();
        assertThat(result.get().errors()).isEmpty();
        assertThat(result.get().valid()).isTrue();
    }
}

