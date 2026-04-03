package com.learning.springboot.chapter06;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for BankAccount — Chapter 6 transaction examples.
 */
@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByAccountNumber(String accountNumber);
    List<BankAccount>     findByOwnerName(String ownerName);
    List<BankAccount>     findByActiveTrue();
    boolean               existsByAccountNumber(String accountNumber);

    @Query("SELECT SUM(a.balance) FROM BankAccount a WHERE a.active = true")
    BigDecimal sumAllActiveBalances();

    @Query("SELECT a FROM BankAccount a WHERE a.balance < :threshold AND a.active = true")
    List<BankAccount> findLowBalanceAccounts(@Param("threshold") BigDecimal threshold);
}

