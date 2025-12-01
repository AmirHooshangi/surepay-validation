package com.surepay.validation.controller;

import com.surepay.validation.config.ValidationProperties;
import com.surepay.validation.dto.ErrorResponse;
import com.surepay.validation.service.ReportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/validation/reports")
public class ReportController {

    private final ReportService reportService;
    private final ValidationProperties validationProperties;

    public ReportController(ReportService reportService, ValidationProperties validationProperties) {
        this.reportService = reportService;
        this.validationProperties = validationProperties;
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<?> getReport(
            @PathVariable String reportId,
            @RequestParam(value = "errors", defaultValue = "false") boolean includeErrors,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        
        if (includeErrors) {
            int pageNum = (page != null) ? page : 0;
            if (pageNum < 0) {
                ErrorResponse error = ErrorResponse.of("INVALID_INPUT", 
                    "Page number must be non-negative, got: " + pageNum);
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(error);
            }
            
            int defaultPageSize = validationProperties.getPagination().getDefaultPageSize();
            int maxPageSize = validationProperties.getPagination().getMaxPageSize();
            int pageSize = (size != null) ? size : defaultPageSize;
            
            if (pageSize < 1) {
                ErrorResponse error = ErrorResponse.of("INVALID_INPUT", 
                    "Page size must be at least 1, got: " + pageSize);
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(error);
            }
            
            if (pageSize > maxPageSize) {
                ErrorResponse error = ErrorResponse.of("INVALID_INPUT", 
                    String.format("Page size exceeds maximum of %d, got: %d", maxPageSize, pageSize));
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(error);
            }
            
            Pageable pageable = PageRequest.of(pageNum, pageSize);
            
            return reportService.getReport(reportId, includeErrors, pageable)
                .map(report -> ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(report))
                .orElse(ResponseEntity.notFound().build());
        } else {
            return reportService.getReport(reportId, includeErrors)
                .map(report -> ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(report))
                .orElse(ResponseEntity.notFound().build());
        }
    }
}

