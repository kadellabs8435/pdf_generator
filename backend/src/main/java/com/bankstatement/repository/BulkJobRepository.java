package com.bankstatement.repository;

import com.bankstatement.entity.BulkJob;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BulkJobRepository extends MongoRepository<BulkJob, String> {
}
