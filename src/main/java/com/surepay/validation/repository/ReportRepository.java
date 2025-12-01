package com.surepay.validation.repository;

import com.surepay.validation.domain.ReportEntity;
import com.surepay.validation.dto.ValidationReportDto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends MongoRepository<ReportEntity, String> {
    default boolean existsByHash(String hash) {
        return existsById(hash);
    }
    
    default Optional<ValidationReportDto> findReportDtoById(String reportId) {
        return findById(reportId).map(ReportEntity::report);
    }
}

