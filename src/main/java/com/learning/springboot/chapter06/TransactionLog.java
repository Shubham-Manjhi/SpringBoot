package com.learning.springboot.chapter06;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   CHAPTER 6 — DOMAIN ENTITY: TransactionLog                                         ║
 * ║                                                                                       ║
 * ║   Audit log for every bank operation. KEY for demonstrating REQUIRES_NEW:            ║
 * ║   Even if the main transfer FAILS and rolls back, the audit log MUST persist.        ║
 * ║   This is the classic REQUIRES_NEW use case.                                         ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
@Entity
@Table(name = "ch06_transaction_logs")
public class TransactionLog {

    public enum LogStatus { SUCCESS, FAILED, ATTEMPTED }
    public enum OperationType { DEBIT, CREDIT, TRANSFER, INQUIRY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_account", length = 20)
    private String fromAccount;

    @Column(name = "to_account", length = 20)
    private String toAccount;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private OperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LogStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "logged_at", nullable = false, updatable = false)
    private LocalDateTime loggedAt;

    // ── Lifecycle ─────────────────────────────────────────────────────────────────
    @PrePersist
    void onCreate() {
        this.loggedAt = LocalDateTime.now();
    }

    // ── Constructors ──────────────────────────────────────────────────────────────
    protected TransactionLog() {}

    public TransactionLog(String from, String to, BigDecimal amount,
                          OperationType type, LogStatus status) {
        this.fromAccount   = from;
        this.toAccount     = to;
        this.amount        = amount;
        this.operationType = type;
        this.status        = status;
    }

    // ── Factory Methods ───────────────────────────────────────────────────────────
    public static TransactionLog success(String from, String to,
                                          BigDecimal amount, OperationType type) {
        return new TransactionLog(from, to, amount, type, LogStatus.SUCCESS);
    }

    public static TransactionLog failed(String from, String to,
                                         BigDecimal amount, OperationType type,
                                         String reason) {
        TransactionLog log = new TransactionLog(from, to, amount, type, LogStatus.FAILED);
        log.failureReason = reason;
        return log;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────
    public Long          getId()             { return id; }
    public String        getFromAccount()    { return fromAccount; }
    public String        getToAccount()      { return toAccount; }
    public BigDecimal    getAmount()         { return amount; }
    public OperationType getOperationType()  { return operationType; }
    public LogStatus     getStatus()         { return status; }
    public void          setStatus(LogStatus s) { this.status = s; }
    public String        getFailureReason()  { return failureReason; }
    public void          setFailureReason(String r) { this.failureReason = r; }
    public LocalDateTime getLoggedAt()       { return loggedAt; }

    @Override
    public String toString() {
        return "TransactionLog{" + operationType + " " + fromAccount + "→" + toAccount
               + " $" + amount + " [" + status + "]}";
    }
}

