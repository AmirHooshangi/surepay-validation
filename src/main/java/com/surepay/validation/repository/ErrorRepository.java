package com.surepay.validation.repository;

import com.surepay.validation.domain.ErrorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ErrorRepository extends MongoRepository<ErrorEntity, String> {
    List<ErrorEntity> findByReportIdOrderByIndexAsc(String reportId);
    
    Page<ErrorEntity> findByReportIdOrderByIndexAsc(String reportId, Pageable pageable);
    
    long countByReportId(String reportId);
    
    void deleteByReportId(String reportId);
}

