package com.learning.springboot.chapter06;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   CHAPTER 6 — DOMAIN ENTITY: BankAccount                                            ║
 * ║                                                                                       ║
 * ║   This simple entity is the TARGET of ALL transaction examples in Chapter 6.         ║
 * ║   A bank account with balance, owner, and status — perfect for demonstrating         ║
 * ║   ACID properties, propagation, isolation, and rollback scenarios.                   ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
@Entity
@Table(name = "ch06_bank_accounts")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;   // "CHECKING", "SAVINGS"

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    // ── Lifecycle ─────────────────────────────────────────────────────────────────
    @PrePersist
    void onCreate() {
        this.createdAt      = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.lastModifiedAt = LocalDateTime.now();
    }

    // ── Constructors ──────────────────────────────────────────────────────────────
    protected BankAccount() {}

    public BankAccount(String accountNumber, String ownerName,
                       BigDecimal initialBalance, String accountType) {
        this.accountNumber = accountNumber;
        this.ownerName     = ownerName;
        this.balance       = initialBalance;
        this.accountType   = accountType;
    }

    // ── Business Methods ──────────────────────────────────────────────────────────

    /**
     * Debit (withdraw) money from this account.
     * Throws InsufficientFundsException if balance < amount.
     */
    public void debit(BigDecimal amount) throws InsufficientFundsException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive: " + amount);
        }
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                "Account " + accountNumber + " has insufficient funds. " +
                "Balance: " + balance + ", Requested: " + amount);
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Credit (deposit) money to this account.
     */
    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive: " + amount);
        }
        this.balance = this.balance.add(amount);
    }

    // ── Custom Exception ──────────────────────────────────────────────────────────

    /**
     * Checked exception — important for rollback demonstrations.
     * By default, Spring does NOT rollback for checked exceptions.
     */
    public static class InsufficientFundsException extends Exception {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────
    public Long          getId()              { return id; }
    public String        getAccountNumber()   { return accountNumber; }
    public void          setAccountNumber(String n) { this.accountNumber = n; }
    public String        getOwnerName()       { return ownerName; }
    public void          setOwnerName(String n) { this.ownerName = n; }
    public BigDecimal    getBalance()         { return balance; }
    public void          setBalance(BigDecimal b) { this.balance = b; }
    public String        getAccountType()     { return accountType; }
    public void          setAccountType(String t) { this.accountType = t; }
    public boolean       isActive()           { return active; }
    public void          setActive(boolean a) { this.active = a; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public LocalDateTime getLastModifiedAt()  { return lastModifiedAt; }

    @Override
    public String toString() {
        return "BankAccount{accountNumber='" + accountNumber + "', owner='" + ownerName
               + "', balance=" + balance + ", type=" + accountType + '}';
    }
}


