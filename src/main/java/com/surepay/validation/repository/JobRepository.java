package com.surepay.validation.repository;

import com.surepay.validation.domain.JobEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends MongoRepository<JobEntity, String> {
}

