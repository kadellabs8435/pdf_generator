package com.bankstatement.repository;

import com.bankstatement.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {
    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
