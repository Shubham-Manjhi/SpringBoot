package com.learning.springboot.chapter06;

import org.springframework.stereotype.Service;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   EXAMPLE 02: ALL 7 TRANSACTION PROPAGATION TYPES                                   ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example02PropagationTypes.java
 * Purpose:     Deep-dive into every propagation type with:
 *               - What it means
 *               - When to use it
 *               - What happens with/without an outer transaction
 *               - Real-world use case
 *               - Code example
 * Difficulty:  ⭐⭐⭐ Intermediate
 * Time:        60–90 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * ALL 7 PROPAGATION TYPES:
 *   1. REQUIRED       (default)
 *   2. REQUIRES_NEW
 *   3. NESTED
 *   4. SUPPORTS
 *   5. NOT_SUPPORTED
 *   6. MANDATORY
 *   7. NEVER
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */


// ══════════════════════════════════════════════════════════════════════════════════════
//  AUDIT SERVICE — Used by other services to demonstrate REQUIRES_NEW propagation
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║   AuditService — Propagation.REQUIRES_NEW                                    ║
 * ║                                                                               ║
 * ║   WHY REQUIRES_NEW for audit logging?                                         ║
 * ║   The audit log MUST persist even when the main operation FAILS.              ║
 * ║   If we use REQUIRED (default): audit log is in the SAME tx.                 ║
 * ║     → Main tx fails → ROLLBACK → audit log also deleted!                     ║
 * ║   If we use REQUIRES_NEW: audit log is in a SEPARATE tx.                     ║
 * ║     → Main tx fails → only main tx rolled back → audit log SURVIVES!         ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
@Service("ch06AuditService")   // explicit name — avoids conflict with chapter02.AuditService
class AuditService {

    private final TransactionLogRepository logRepository;

    AuditService(TransactionLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║  PROPAGATION.REQUIRES_NEW — ALWAYS create a NEW independent transaction  ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * REQUIRES_NEW always starts a completely NEW, independent transaction.
     * If a transaction already exists (outer/caller), it is SUSPENDED until
     * this new transaction completes (commits or rolls back).
     *
     * TIMELINE:
     *
     *   Outer TX starts ────────────────────────────────────────────────────┐
     *       │                                                                │
     *       │→ auditService.log() called                                    │
     *              OUTER TX SUSPENDED ─────────────────────────────────────┤
     *              NEW INNER TX starts                                       │
     *              INSERT INTO transaction_logs (...)                        │
     *              NEW INNER TX COMMITS ← immediately!                      │
     *              OUTER TX RESUMED ────────────────────────────────────────┘
     *       │→ main logic continues (may succeed or fail)
     *   Outer TX COMMITS or ROLLS BACK
     *       → does NOT affect the already-committed inner TX!
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 KEY CONSEQUENCE:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * The inner REQUIRES_NEW tx COMMITS BEFORE the outer tx finishes.
     * If the outer tx rolls back, the inner tx changes REMAIN COMMITTED.
     *
     * This is INTENTIONAL for audit logging:
     *   Even if the transfer fails → audit entry says "FAILED" and persists.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 WHEN TO USE REQUIRES_NEW:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   ✅ Audit logging — must persist even when the main operation fails
     *   ✅ Sending notifications — email/SMS must be logged even on failure
     *   ✅ Sequence number generation — must not be rolled back
     *   ✅ Billing records — charge even if subsequent processing fails
     *   ✅ Any "fire and forget" sub-operation that should NOT be rolled back
     *      with the outer transaction
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * ⚠️  PERFORMANCE WARNING:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * REQUIRES_NEW suspends and resumes transactions.
     * It requires TWO database connections from the pool simultaneously
     * (one for outer tx, one for inner tx).
     *
     * If your connection pool has 10 connections and you call a REQUIRES_NEW
     * method 10 times within the same request → DEADLOCK (pool exhausted)!
     *
     * Don't use REQUIRES_NEW inside loops with many iterations.
     *
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransactionLog logOperation(String from, String to, BigDecimal amount,
                                        TransactionLog.OperationType type,
                                        TransactionLog.LogStatus status,
                                        String failureReason) {

        String txActive = TransactionSynchronizationManager.isActualTransactionActive()
                         ? "YES (NEW independent tx)" : "NO";
        System.out.println("  [AuditService] In transaction: " + txActive);
        System.out.println("  [AuditService] Saving log with status: " + status);

        TransactionLog log = status == TransactionLog.LogStatus.SUCCESS
            ? TransactionLog.success(from, to, amount, type)
            : TransactionLog.failed(from, to, amount, type, failureReason);

        return logRepository.save(log);
        // This COMMITS immediately — independent of whatever happens in the outer tx
    }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PROPAGATION DEMONSTRATION SERVICE
// ══════════════════════════════════════════════════════════════════════════════════════

@Service
class PropagationDemoService {

    private final BankAccountRepository accountRepository;
    private final AuditService auditService;
    private final TransactionLogRepository logRepository;

    PropagationDemoService(BankAccountRepository accountRepository,
                           AuditService auditService,
                           TransactionLogRepository logRepository) {
        this.accountRepository = accountRepository;
        this.auditService      = auditService;
        this.logRepository     = logRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. PROPAGATION.REQUIRED (DEFAULT)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   REQUIRED — Default propagation                                          ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * RULE:
     *   IF an existing transaction exists → JOIN it (become part of the same tx)
     *   IF no transaction exists → CREATE a new one
     *
     * DIAGRAM WITH OUTER TX:
     *   Outer TX ──────────────────────────────────────────────────────────────┐
     *       │→ requiredMethod() called                                         │
     *       │  NO new tx created — requiredMethod() joins Outer TX             │
     *       │  INSERT (part of Outer TX)                                       │
     *       │→ requiredMethod() returns                                         │
     *       │  Outer TX commits → ALL changes committed together               │
     *   └────────────────────────────────────────────────────────────────────────
     *
     * DIAGRAM WITHOUT OUTER TX:
     *   requiredMethod() called (no active tx)
     *   NEW TX created
     *   INSERT (in the new tx)
     *   TX commits
     *
     * USE CASE:
     *   The DEFAULT — use when you want standard transactional behaviour.
     *   Most service methods use REQUIRED (by default or explicitly).
     *
     * IMPORTANT ROLLBACK BEHAVIOUR:
     *   If requiredMethod() is part of an outer tx and requiredMethod() sets
     *   rollbackOnly (or throws), the ENTIRE outer tx is rolled back.
     *   There is no way to rollback only requiredMethod()'s changes while
     *   keeping the outer tx's changes. That's what NESTED is for.
     */
    @Transactional(propagation = Propagation.REQUIRED)  // same as @Transactional
    public void required_createLog(String message) {
        System.out.println("  [REQUIRED] In outer tx? "
            + TransactionSynchronizationManager.isActualTransactionActive());
        logRepository.save(TransactionLog.success(
            "SYSTEM", "SYSTEM", BigDecimal.ZERO,
            TransactionLog.OperationType.CREDIT));
        System.out.println("  [REQUIRED] Log created — joined or created tx");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. PROPAGATION.REQUIRES_NEW — Already shown in AuditService above
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   Transfer WITH audit logging — REQUIRED + REQUIRES_NEW combination       ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * SCENARIO: Transfer $200 from ACC001 to ACC002
     *   - Use REQUIRES_NEW for audit log → persists even if transfer fails
     *
     * FLOW:
     *   1. transferWithAudit() starts — OUTER TX opened (REQUIRED)
     *   2. auditService.logOperation() called — OUTER TX SUSPENDED, NEW TX started
     *   3. Audit log saved and COMMITTED (regardless of outer tx)
     *   4. OUTER TX RESUMED
     *   5a. If transfer succeeds → OUTER TX COMMITS
     *   5b. If transfer fails → OUTER TX ROLLS BACK → but audit log stays committed!
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void transferWithAudit(String fromAccNr, String toAccNr, BigDecimal amount)
            throws BankAccount.InsufficientFundsException {

        System.out.println("  [transferWithAudit] Starting outer transaction");

        // Log the ATTEMPT first (REQUIRES_NEW → commits immediately)
        auditService.logOperation(fromAccNr, toAccNr, amount,
            TransactionLog.OperationType.TRANSFER,
            TransactionLog.LogStatus.ATTEMPTED, null);
        System.out.println("  [transferWithAudit] Audit log committed (REQUIRES_NEW)");
        System.out.println("  [transferWithAudit] Outer tx resumed");

        try {
            // Main transfer logic (in OUTER tx)
            BankAccount from = accountRepository.findByAccountNumber(fromAccNr)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
            BankAccount to = accountRepository.findByAccountNumber(toAccNr)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

            from.debit(amount);
            to.credit(amount);
            accountRepository.save(from);
            accountRepository.save(to);

            // Update log to SUCCESS (also REQUIRES_NEW — separate tx)
            auditService.logOperation(fromAccNr, toAccNr, amount,
                TransactionLog.OperationType.TRANSFER,
                TransactionLog.LogStatus.SUCCESS, null);

        } catch (BankAccount.InsufficientFundsException e) {
            // Update log to FAILED (also REQUIRES_NEW — persists even though outer tx rolls back)
            auditService.logOperation(fromAccNr, toAccNr, amount,
                TransactionLog.OperationType.TRANSFER,
                TransactionLog.LogStatus.FAILED, e.getMessage());
            throw e;  // rethrow → outer tx rolls back
            // Result: debit/credit changes rolled back, but BOTH audit logs persist!
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. PROPAGATION.NESTED
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   NESTED — Savepoint within the current transaction                       ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * NESTED creates a SAVEPOINT in the EXISTING transaction.
     * It does NOT create a new, independent transaction.
     *
     * KEY DIFFERENCE FROM REQUIRES_NEW:
     *
     *   REQUIRES_NEW:  Two SEPARATE transactions. Inner commits BEFORE outer.
     *                  Inner changes survive outer rollback.
     *
     *   NESTED:        ONE transaction with a savepoint.
     *                  If nested part fails: rollback to SAVEPOINT (partial rollback).
     *                  If outer part fails: ENTIRE transaction rolls back (including nested).
     *
     * DIAGRAM:
     *   Outer TX ─────────────────────────────────────────────────────────────┐
     *       │  INSERT row A (outer tx, no savepoint yet)                      │
     *       │→ nestedMethod() called                                           │
     *       │  SAVEPOINT SP1 created ←─────────────────────────────────────┐ │
     *       │     INSERT row B (inside nested, after SP1)                   │ │
     *       │     FAILS → ROLLBACK TO SAVEPOINT SP1                         │ │
     *       │  Row B gone, Row A still intact ──────────────────────────────┘ │
     *       │  Outer tx continues with Row A only                             │
     *   Outer TX COMMITS → only Row A persisted
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 WHEN TO USE NESTED:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   ✅ Partial rollback: try optional sub-operations; if they fail, continue
     *   ✅ Batch processing: process each item; if one fails, skip it and continue
     *   ✅ Multi-step save: save main entity, try saving optional related entities
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * ⚠️  LIMITATIONS:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   NESTED uses JDBC savepoints. Supported by:
     *     ✅ DataSourceTransactionManager (plain JDBC)
     *     ✅ JpaTransactionManager (only if the JDBC connection is exposed)
     *     ❌ JTA (distributed transactions) — not supported
     *
     *   With Spring JPA (which uses JpaTransactionManager), NESTED may not work
     *   reliably unless you configure the transaction manager to expose the
     *   JDBC connection (setNestedTransactionAllowed(true)).
     *   For JPA, REQUIRES_NEW is often the safer alternative.
     *
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void processWithNestedLog(String accountNumber, BigDecimal amount) {

        System.out.println("  [NESTED demo] Starting outer transaction");

        BankAccount account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));

        account.credit(amount);
        accountRepository.save(account);
        System.out.println("  [NESTED demo] Account credited — in outer tx");

        try {
            // NESTED call — creates a savepoint
            createOptionalLog_NESTED(accountNumber, amount);
            System.out.println("  [NESTED demo] Optional log created successfully");
        } catch (Exception e) {
            // Nested operation failed → only rolled back to savepoint
            // Outer transaction (account credit) is UNAFFECTED
            System.out.println("  [NESTED demo] Optional log FAILED but outer tx continues: "
                               + e.getMessage());
        }

        System.out.println("  [NESTED demo] Outer tx completing — credit persists");
    }

    @Transactional(propagation = Propagation.NESTED)
    public void createOptionalLog_NESTED(String accountNumber, BigDecimal amount) {
        System.out.println("  [NESTED] SAVEPOINT created");
        logRepository.save(TransactionLog.success(
            accountNumber, null, amount, TransactionLog.OperationType.CREDIT));
        System.out.println("  [NESTED] Log saved (may be rolled back to savepoint if error)");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. PROPAGATION.SUPPORTS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   SUPPORTS — Use tx if available; no tx if not                           ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * SUPPORTS:
     *   If a transaction exists: JOIN it (participate in the existing tx).
     *   If no transaction exists: run WITHOUT a transaction.
     *
     * CALLED WITH outer tx   → reads are transactional (part of outer tx)
     * CALLED WITHOUT outer tx → reads are non-transactional (each query auto-commits)
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 WHEN TO USE SUPPORTS:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   ✅ Read methods that MAY or MAY NOT need to be part of a transaction
     *   ✅ When you want transactional reads if a tx happens to exist
     *   ✅ Helper/utility methods that work both in and out of transactions
     *   ✅ Reporting methods: don't need a tx, but can participate if caller has one
     *
     *   Example: getAccountBalance() — can be called standalone (no tx needed)
     *            but if called from within a transfer() tx, should see the same
     *            in-progress data.
     *
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public BigDecimal getBalanceOptionalTransaction(String accountNumber) {
        boolean inTx = TransactionSynchronizationManager.isActualTransactionActive();
        System.out.println("  [SUPPORTS] In transaction: " + inTx
            + (inTx ? " (joined caller's tx)" : " (no tx — non-transactional read)"));

        return accountRepository.findByAccountNumber(accountNumber)
            .map(BankAccount::getBalance)
            .orElse(BigDecimal.ZERO);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 5. PROPAGATION.NOT_SUPPORTED
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   NOT_SUPPORTED — Always run WITHOUT a transaction                        ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * NOT_SUPPORTED ALWAYS runs without a transaction.
     * If a transaction exists: SUSPEND it, run without tx, then RESUME it.
     *
     * WHEN TO USE:
     *   ✅ Bulk read operations where a tx would hold locks too long
     *   ✅ Long-running reports or data exports (no tx lock contention)
     *   ✅ Sending non-transactional messages (outside of the main tx scope)
     *   ✅ Operations on resources that don't support transactions (e.g., external APIs)
     *   ✅ Avoiding "transaction timeout" for long-running operations
     *
     * DIFFERENCE FROM NEVER:
     *   NOT_SUPPORTED: suspends existing tx (works both with and without outer tx)
     *   NEVER:         throws exception if a tx exists
     *
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public long countAllTransactions() {
        boolean inTx = TransactionSynchronizationManager.isActualTransactionActive();
        System.out.println("  [NOT_SUPPORTED] In transaction: " + inTx
                         + " (always false — outer tx suspended if any)");
        // Runs WITHOUT a transaction — any active outer tx was suspended
        return logRepository.count();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 6. PROPAGATION.MANDATORY
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   MANDATORY — MUST be called within an existing transaction               ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * MANDATORY: If a transaction exists, JOIN it.
     *            If NO transaction exists, throw IllegalTransactionStateException.
     *
     * WHY IS THIS USEFUL?
     *   MANDATORY is an enforcement mechanism.
     *   It says: "I am only safe to call within a transaction. If you call me
     *   without one, something is WRONG with your code — fail fast!"
     *
     * WHEN TO USE:
     *   ✅ Internal helper methods that MUST be called within a tx (safety net)
     *   ✅ Repository helper methods that assume a tx is already active
     *   ✅ Complex domain operations that must be part of a larger atomic operation
     *   ✅ Protecting against accidental non-transactional calls of critical operations
     *
     * EXAMPLE PATTERN:
     *   @Transactional(propagation = MANDATORY)
     *   private void updateLinkedAccount(Account a) {
     *       // This MUST always be called from within an outer transaction.
     *       // MANDATORY ensures it is — if someone calls it standalone, it explodes.
     *   }
     *
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void mandatoryOperation(String accountNumber) {
        // This will throw IllegalTransactionStateException if called without a tx!
        System.out.println("  [MANDATORY] In transaction: "
            + TransactionSynchronizationManager.isActualTransactionActive());
        // Safe to proceed — we KNOW a transaction is active
        accountRepository.findByAccountNumber(accountNumber).ifPresent(account -> {
            System.out.println("  [MANDATORY] Account: " + account.getAccountNumber());
        });
    }

    // Caller method that correctly calls mandatoryOperation() within a tx:
    @Transactional
    public void callerWithTransaction(String accountNumber) {
        System.out.println("  Outer tx started");
        mandatoryOperation(accountNumber);  // OK! Outer tx is active
        System.out.println("  Outer tx completing");
    }

    // This would THROW if called directly:
    // mandatoryOperation("ACC001")  → IllegalTransactionStateException!

    // ─────────────────────────────────────────────────────────────────────────────
    // 7. PROPAGATION.NEVER
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   NEVER — MUST be called WITHOUT a transaction                            ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * NEVER: If no transaction exists, run normally.
     *        If a transaction EXISTS, throw IllegalTransactionStateException.
     *
     * Opposite of MANDATORY:
     *   MANDATORY → MUST have a tx (throw if none)
     *   NEVER     → MUST NOT have a tx (throw if one exists)
     *
     * WHEN TO USE:
     *   ✅ Operations that are explicitly unsafe inside a transaction
     *      (e.g., operations that must use auto-commit for correctness)
     *   ✅ Sending messages that must NOT be part of a larger transaction
     *   ✅ Rarely used — mostly to enforce design contracts
     *
     * DIFFERENCE FROM NOT_SUPPORTED:
     *   NOT_SUPPORTED: suspends existing tx (doesn't fail)
     *   NEVER:         throws exception if tx exists (hard enforcement)
     *
     */
    @Transactional(propagation = Propagation.NEVER)
    public void neverOperation() {
        boolean inTx = TransactionSynchronizationManager.isActualTransactionActive();
        System.out.println("  [NEVER] In transaction: " + inTx
                         + " (should always be false; throws if true)");
        // If called within a tx: IllegalTransactionStateException is thrown BEFORE reaching here
        System.out.println("  [NEVER] Running non-transactionally — safe!");
        logRepository.count();  // Non-transactional DB call
    }
}


/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📚 LEARNING SUMMARY 📚
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * ✅ ALL 7 PROPAGATION TYPES:
 *
 *  TYPE           WITH ACTIVE TX             WITHOUT ACTIVE TX      USE WHEN
 *  ─────────────  ─────────────────────────  ─────────────────────  ──────────────────────────────
 *  REQUIRED       Join existing tx           Create new tx          Default; most methods
 *  REQUIRES_NEW   Suspend outer, new tx      Create new tx          Audit log, side effects
 *  NESTED         Savepoint in existing tx   Create new tx          Optional sub-ops (partial rollback)
 *  SUPPORTS       Join existing tx           Run non-transactionally Read helpers (tx optional)
 *  NOT_SUPPORTED  Suspend outer, no tx       Run non-transactionally Long reads, reports
 *  MANDATORY      Join existing tx           THROW exception        Internal helpers (must have tx)
 *  NEVER          THROW exception            Run non-transactionally Explicitly non-transactional ops
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ➡️  NEXT: Example03IsolationLevels.java — all 5 isolation levels
 * ─────────────────────────────────────────────────────────────────────────────────
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
class Example02PropagationTypes {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║   CHAPTER 6 — EXAMPLE 02: All 7 Propagation Types                ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  REQUIRED      → join existing tx OR create new one (DEFAULT)");
        System.out.println("  REQUIRES_NEW  → ALWAYS new independent tx (audit log use case)");
        System.out.println("  NESTED        → savepoint in existing tx (partial rollback)");
        System.out.println("  SUPPORTS      → join if exists; no tx otherwise");
        System.out.println("  NOT_SUPPORTED → always suspend tx; run non-transactionally");
        System.out.println("  MANDATORY     → must have tx; THROW if none exists");
        System.out.println("  NEVER         → must NOT have tx; THROW if one exists");
        System.out.println();
        System.out.println("  Key distinction: REQUIRES_NEW vs NESTED");
        System.out.println("    REQUIRES_NEW: inner commits BEFORE outer — independent");
        System.out.println("    NESTED:       savepoint — inner rollback possible without outer rollback");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

