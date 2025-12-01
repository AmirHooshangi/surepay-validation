package com.surepay.validation.controller;

import com.surepay.validation.dto.ValidationJobResponse;
import com.surepay.validation.dto.ValidationReportDto;
import com.surepay.validation.service.JobService;
import com.surepay.validation.domain.JobEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/validation/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/{jobId}/status")
    public ResponseEntity<ValidationJobResponse> getJobStatus(@PathVariable String jobId) {
        return jobService.getJob(jobId)
            .map(job -> {
                ValidationJobResponse response = ValidationJobResponse.from(job);
                
                HttpStatus status = switch (job.status()) {
                    case COMPLETED -> HttpStatus.OK;
                    case FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
                    case PROCESSING, PENDING -> HttpStatus.ACCEPTED;
                };
                
                return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{jobId}/result")
    public ResponseEntity<ValidationReportDto> getJobResult(@PathVariable String jobId) {
        return jobService.getJob(jobId)
            .map(this::buildResultResponse)
            .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<ValidationReportDto> buildResultResponse(JobEntity job) {
        if (job.status() == JobEntity.JobStatus.COMPLETED && job.report() != null) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(job.report());
        } else if (job.status() == JobEntity.JobStatus.FAILED) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } else {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
    }
}

