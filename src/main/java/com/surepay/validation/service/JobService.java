package com.surepay.validation.service;

import com.surepay.validation.domain.JobEntity;
import com.surepay.validation.domain.ValidationResult;
import com.surepay.validation.parser.ParseException;
import com.surepay.validation.dto.ValidationReportDto;
import com.surepay.validation.repository.JobRepository;
import com.surepay.validation.util.HashComputingInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;

@Service
public class JobService {
    private static final Logger logger = LoggerFactory.getLogger(JobService.class);

    private final ValidationService validationService;
    private final JobRepository jobRepository;
    private final ExecutorService virtualThreadExecutor;

    public JobService(
            ValidationService validationService,
            JobRepository jobRepository) {
        this.validationService = validationService;
        this.jobRepository = jobRepository;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public JobEntity submitJob(String filename, InputStream fileInputStream, String contentType, long fileSize) throws IOException {
        HashComputingInputStream hashStream = new HashComputingInputStream(fileInputStream);
        String hash = hashStream.getHash();


        Optional<JobEntity> existingJob = jobRepository.findById(hash);
        
        if (existingJob.isPresent()) {
            JobEntity job = existingJob.get();
            
            return switch (job.status()) {
                case COMPLETED -> {
                    logger.info("Job with hash {} already exists and is COMPLETED, returning existing job", hash);
                    yield job;
                }
                case PENDING, PROCESSING -> {
                    logger.info("Job with hash {} exists with status {}, already being processed, returning existing job", hash, job.status());
                    yield job;
                }
                case FAILED -> {
                    logger.info("Job with hash {} exists with status FAILED, retrying", hash);
                    JobEntity updatedJob = job.withStatus(JobEntity.JobStatus.PROCESSING);
                    jobRepository.save(updatedJob);
                    
                    virtualThreadExecutor.submit(() -> 
                        processValidationAsync(hash, fileInputStream, contentType, filename, fileSize)
                    );
                    
                    yield updatedJob;
                }
            };
        }
        
        JobEntity job = JobEntity.create(hash, filename);
        jobRepository.save(job);
        logger.info("Created new job with hash: {}", hash);

        virtualThreadExecutor.submit(() -> 
            processValidationAsync(hash, fileInputStream, contentType, filename, fileSize)
        );

        return job;
    }

    private void processValidationAsync(String jobId, InputStream fileInputStream, String contentType, String filename, long fileSize) {
        try {
            updateJobStatus(jobId, JobEntity.JobStatus.PROCESSING);

            long javaProcessingStartTime = System.nanoTime();

            ValidationService.ValidationAndStorageResult result = validationService.validateAndStoreReport(
                fileInputStream,
                contentType,
                filename,
                fileSize
            );
            
            String hash = result.reportId();
            ValidationReportDto reportDto = result.reportDto();
            ValidationResult validationResult = result.validationResult();

            long javaProcessingEndTime = System.nanoTime();
            long javaProcessingTimeMs = (javaProcessingEndTime - javaProcessingStartTime) / 1_000_000;
            logger.info("Java layer processing time before DB insertion: {} ms for job: {}", javaProcessingTimeMs, jobId);
            logger.info("Stored validation report with hash: {} for job: {} (summary: {} errors)", 
                       hash, jobId, validationResult.getErrorCount());

            JobEntity job = getJob(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));
            
                ValidationReportDto reportForStorage = new ValidationReportDto(
                    reportDto.valid(),
                    reportDto.errorCount(),
                    reportDto.duplicateReferenceCount(),
                    reportDto.balanceMismatchCount(),
                    java.util.List.of()
                );
                
                JobEntity completedJob = job.withReport(reportForStorage);
                jobRepository.save(completedJob);

                logger.info("Validation job {} completed successfully", jobId);
        } catch (ParseException e) {
            logger.error("Parse error for job {}: {}", jobId, e.getMessage(), e);
            updateJobWithError(jobId, "Failed to parse file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing job {}", jobId, e);
            updateJobWithError(jobId, "An error occurred during validation: " + e.getMessage());
        }
    }

    public Optional<JobEntity> getJob(String jobId) {
        return jobRepository.findById(jobId);
    }

    @Transactional
    private void updateJobStatus(String jobId, JobEntity.JobStatus status) {
        jobRepository.findById(jobId).ifPresent(job -> {
            JobEntity updatedJob = job.withStatus(status);
            jobRepository.save(updatedJob);
        });
    }

    @Transactional
    private void updateJobWithError(String jobId, String errorMessage) {
        jobRepository.findById(jobId).ifPresent(job -> {
            JobEntity updatedJob = job.withError(errorMessage);
            jobRepository.save(updatedJob);
        });
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down virtual thread executor");
        virtualThreadExecutor.shutdown();
        try {
            if (!virtualThreadExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                virtualThreadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            virtualThreadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

