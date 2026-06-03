package com.bankstatement.repository;

import com.bankstatement.entity.Statement;
import com.bankstatement.entity.StatementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StatementRepository extends MongoRepository<Statement, String> {
    long countByStatus(StatementStatus status);
    Page<Statement> findByCreatedByUserId(String userId, Pageable pageable);
    Page<Statement> findByStatus(StatementStatus status, Pageable pageable);
    Page<Statement> findByCreatedByUserIdAndStatus(String userId, StatementStatus status, Pageable pageable);
    Page<Statement> findByBankCode(String bankCode, Pageable pageable);
}
