package com.bankstatement.repository;

import com.bankstatement.entity.BankTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BankTemplateRepository extends MongoRepository<BankTemplate, String> {
    Optional<BankTemplate> findByCode(String code);
    List<BankTemplate> findByActiveTrue();
}
