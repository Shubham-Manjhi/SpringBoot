package com.learning.springboot.chapter06;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TransactionLog — Chapter 6 REQUIRES_NEW propagation examples.
 */
@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    List<TransactionLog> findByFromAccountOrToAccount(String from, String to);
    List<TransactionLog> findByStatus(TransactionLog.LogStatus status);
    long countByStatus(TransactionLog.LogStatus status);
}

