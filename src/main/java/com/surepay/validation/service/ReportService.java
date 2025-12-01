package com.surepay.validation.service;

import com.surepay.validation.dto.ValidationReportDto;
import com.surepay.validation.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    
    private final ReportRepository reportRepository;
    private final ErrorService errorService;

    public ReportService(ReportRepository reportRepository, ErrorService errorService) {
        this.reportRepository = reportRepository;
        this.errorService = errorService;
    }

    public Optional<ValidationReportDto> getReport(String reportId, boolean includeErrors) {
        logger.debug("Retrieving report {} with errors: {}", reportId, includeErrors);
        
        return reportRepository.findReportDtoById(reportId)
            .map(report -> {
                if (includeErrors) {
                    Pageable defaultPageable = PageRequest.of(0, 1000);
                    Page<ValidationReportDto.ValidationErrorDto> errorPage = errorService.getErrors(reportId, defaultPageable);
                    List<ValidationReportDto.ValidationErrorDto> errors = errorPage.getContent();
                    return new ValidationReportDto(
                        report.valid(),
                        report.errorCount(),
                        report.duplicateReferenceCount(),
                        report.balanceMismatchCount(),
                        errors
                    );
                }
                return report;
            });
    }
    
    public Optional<ValidationReportDto> getReport(String reportId, boolean includeErrors, Pageable pageable) {
        logger.debug("Retrieving report {} with errors: {}, page: {}, size: {}", 
                    reportId, includeErrors, pageable.getPageNumber(), pageable.getPageSize());
        
        return reportRepository.findReportDtoById(reportId)
            .map(report -> {
                if (includeErrors) {
                    Page<ValidationReportDto.ValidationErrorDto> errorPage = errorService.getErrors(reportId, pageable);
                    List<ValidationReportDto.ValidationErrorDto> errors = errorPage.getContent();
                    return new ValidationReportDto(
                        report.valid(),
                        report.errorCount(),
                        report.duplicateReferenceCount(),
                        report.balanceMismatchCount(),
                        errors
                    );
                }
                return report;
            });
    }
}

