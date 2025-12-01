package com.surepay.validation.service;

import com.surepay.validation.domain.JobEntity;
import com.surepay.validation.dto.ValidationReportDto;
import com.surepay.validation.parser.ParseException;
import com.surepay.validation.repository.JobRepository;
import com.surepay.validation.util.HashComputingInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class JobServiceTest {

    @Mock
    private ValidationService validationService;

    @Mock
    private JobRepository jobRepository;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobService(validationService, jobRepository);
    }

    @Test
    void shouldCreateNewJobWhenNoExistingJob() throws IOException {
        byte[] fileData = "test data".getBytes();
        
        when(jobRepository.findById(anyString())).thenReturn(Optional.empty());
        when(jobRepository.save(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobEntity job = jobService.submitJob("test.csv", new ByteArrayInputStream(fileData), "text/csv", fileData.length);

        assertThat(job.jobId()).isNotNull();
        assertThat(job.filename()).isEqualTo("test.csv");
        assertThat(job.status()).isEqualTo(JobEntity.JobStatus.PENDING);
        assertThat(job.createdAt()).isNotNull();
        
        verify(jobRepository).save(any(JobEntity.class));
    }

    @Test
    void shouldCreateJobAndProcessAsync() throws IOException, InterruptedException {
        byte[] fileData = "test data".getBytes();
        // Hash will be computed during validation, not in submitJob
        String hash = HashComputingInputStream.computeHash(fileData);
        
        JobEntity job = JobEntity.create(hash, "test.csv");
        ValidationReportDto reportDto = new ValidationReportDto(true, 0, 0, 0, java.util.List.of());
        com.surepay.validation.domain.ValidationResult validationResult = new com.surepay.validation.domain.ValidationResult();
        ValidationService.ValidationAndStorageResult result = 
            new ValidationService.ValidationAndStorageResult(reportDto, hash, validationResult);
        
        // First call returns empty (new job), subsequent calls return the job
        when(jobRepository.findById(anyString())).thenReturn(Optional.empty(), Optional.of(job));
        when(jobRepository.save(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(validationService.validateAndStoreReport(any(), any(), any(), anyLong())).thenReturn(result);

        JobEntity submittedJob = jobService.submitJob("test.csv", new ByteArrayInputStream(fileData), "text/csv", fileData.length);

        assertThat(submittedJob.jobId()).isNotNull();
        assertThat(submittedJob.filename()).isEqualTo("test.csv");
        assertThat(submittedJob.status()).isEqualTo(JobEntity.JobStatus.PENDING);
        
        Thread.sleep(500);
        
        verify(jobRepository, atLeastOnce()).save(any(JobEntity.class));
        verify(validationService).validateAndStoreReport(any(), any(), any(), anyLong());
    }

    @Test
    void shouldGetJobById() {
        String jobId = "testjob123";
        JobEntity job = JobEntity.create(jobId, "test.csv");
        
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        Optional<JobEntity> result = jobService.getJob(jobId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(job);
    }

    @Test
    void shouldReturnEmptyWhenJobNotFound() {
        String jobId = "nonexistent";
        
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        Optional<JobEntity> result = jobService.getJob(jobId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleParseExceptionInAsyncProcessing() throws IOException, InterruptedException {
        byte[] fileData = "invalid data".getBytes();
        String hash = HashComputingInputStream.computeHash(fileData);
        
        JobEntity job = JobEntity.create(hash, "test.csv");
        
        when(jobRepository.findById(anyString())).thenReturn(Optional.empty(), Optional.of(job));
        when(jobRepository.save(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(validationService.validateAndStoreReport(any(), any(), any(), anyLong()))
            .thenThrow(new ParseException("Invalid format"));

        jobService.submitJob("test.csv", new ByteArrayInputStream(fileData), "text/csv", fileData.length);
        
        Thread.sleep(500);
        
        ArgumentCaptor<JobEntity> captor = ArgumentCaptor.forClass(JobEntity.class);
        verify(jobRepository, atLeast(2)).save(captor.capture());
        
        JobEntity savedJob = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(savedJob.status()).isEqualTo(JobEntity.JobStatus.FAILED);
        assertThat(savedJob.errorMessage()).contains("Failed to parse file");
    }

    @Test
    void shouldHandleOutOfMemoryErrorInAsyncProcessing() throws IOException, InterruptedException {
        byte[] fileData = "large data".getBytes();
        String hash = HashComputingInputStream.computeHash(fileData);
        
        JobEntity job = JobEntity.create(hash, "test.csv");
        
        when(jobRepository.findById(anyString())).thenReturn(Optional.empty(), Optional.of(job));
        when(jobRepository.save(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(validationService.validateAndStoreReport(any(), any(), any(), anyLong()))
            .thenThrow(new RuntimeException("Out of memory"));

        jobService.submitJob("test.csv", new ByteArrayInputStream(fileData), "text/csv", fileData.length);
        
        Thread.sleep(500);
        
        ArgumentCaptor<JobEntity> captor = ArgumentCaptor.forClass(JobEntity.class);
        verify(jobRepository, atLeast(2)).save(captor.capture());
        
        JobEntity savedJob = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(savedJob.status()).isEqualTo(JobEntity.JobStatus.FAILED);
        assertThat(savedJob.errorMessage()).contains("An error occurred during validation");
    }

    @Test
    void shouldHandleGenericExceptionInAsyncProcessing() throws IOException, InterruptedException {
        byte[] fileData = "test data".getBytes();
        String hash = HashComputingInputStream.computeHash(fileData);
        
        JobEntity job = JobEntity.create(hash, "test.csv");
        
        when(jobRepository.findById(anyString())).thenReturn(Optional.empty(), Optional.of(job));
        when(jobRepository.save(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(validationService.validateAndStoreReport(any(), any(), any(), anyLong()))
            .thenThrow(new RuntimeException("Unexpected error"));

        jobService.submitJob("test.csv", new ByteArrayInputStream(fileData), "text/csv", fileData.length);
        
        Thread.sleep(500);
        
        ArgumentCaptor<JobEntity> captor = ArgumentCaptor.forClass(JobEntity.class);
        verify(jobRepository, atLeast(2)).save(captor.capture());
        
        JobEntity savedJob = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(savedJob.status()).isEqualTo(JobEntity.JobStatus.FAILED);
        assertThat(savedJob.errorMessage()).contains("An error occurred");
    }

    @Test
    void shouldUpdateJobStatusToProcessing() throws IOException, InterruptedException {
        byte[] fileData = "test data".getBytes();
        String hash = HashComputingInputStream.computeHash(fileData);
        
        JobEntity job = JobEntity.create(hash, "test.csv");
        ValidationReportDto reportDto = new ValidationReportDto(true, 0, 0, 0, java.util.List.of());
        com.surepay.validation.domain.ValidationResult validationResult = new com.surepay.validation.domain.ValidationResult();
        ValidationService.ValidationAndStorageResult result = 
            new ValidationService.ValidationAndStorageResult(reportDto, hash, validationResult);
        
        when(jobRepository.findById(anyString())).thenReturn(Optional.empty(), Optional.of(job));
        when(jobRepository.save(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(validationService.validateAndStoreReport(any(), any(), any(), anyLong())).thenReturn(result);

        jobService.submitJob("test.csv", new ByteArrayInputStream(fileData), "text/csv", fileData.length);
        
        Thread.sleep(500);
        
        ArgumentCaptor<JobEntity> captor = ArgumentCaptor.forClass(JobEntity.class);
        verify(jobRepository, atLeast(2)).save(captor.capture());
        
        boolean foundProcessing = captor.getAllValues().stream()
            .anyMatch(j -> j.status() == JobEntity.JobStatus.PROCESSING);
        assertThat(foundProcessing).isTrue();
    }
}

