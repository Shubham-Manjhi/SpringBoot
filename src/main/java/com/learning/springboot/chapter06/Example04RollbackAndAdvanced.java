package com.learning.springboot.chapter06;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.util.List;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   EXAMPLE 04: ROLLBACK RULES, READ-ONLY, TIMEOUT & ADVANCED PATTERNS               ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example04RollbackAndAdvanced.java
 * Purpose:     Master the remaining @Transactional attributes:
 *               - rollbackFor & noRollbackFor (default vs custom rollback rules)
 *               - readOnly (performance optimization + correctness)
 *               - timeout (prevent long-running transactions)
 *               - Programmatic rollback (setRollbackOnly)
 *               - @TransactionalEventListener (bonus)
 * Difficulty:  ⭐⭐⭐ Intermediate
 * Time:        45–60 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART A: ROLLBACK RULES — rollbackFor & noRollbackFor
// ══════════════════════════════════════════════════════════════════════════════════════

@Service
class RollbackDemoService {

    private final BankAccountRepository accountRepository;
    private final TransactionLogRepository logRepository;

    RollbackDemoService(BankAccountRepository accountRepository,
                        TransactionLogRepository logRepository) {
        this.accountRepository = accountRepository;
        this.logRepository     = logRepository;
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   DEFAULT ROLLBACK BEHAVIOUR                                              ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFAULT RULES (when no rollbackFor is specified):
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * ROLLBACK TRIGGERED by: RuntimeException and Error (unchecked)
     * NO ROLLBACK for:        Checked exceptions (IOException, SQLException, etc.)
     *
     * WHY THIS DEFAULT?
     *   This follows the Java convention that RuntimeExceptions are "unexpected"
     *   programming errors (system failures), while Checked exceptions are expected
     *   application errors that the caller should handle gracefully.
     *
     *   RuntimeException examples that TRIGGER rollback by default:
     *     IllegalArgumentException, IllegalStateException, NullPointerException,
     *     ArithmeticException, DataAccessException (Spring's DB exceptions)
     *
     *   Checked exception examples that do NOT rollback by default:
     *     IOException, SQLException, ClassNotFoundException,
     *     InsufficientFundsException (our custom checked exception)
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 IMPLICATION:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * If your @Transactional method throws a checked exception and you DON'T
     * specify rollbackFor, the transaction COMMITS even though an exception was thrown!
     *
     * This is a COMMON BUG — data gets committed in an inconsistent state.
     */
    @Transactional
    public void defaultRollbackBehaviour(String accountNumber, BigDecimal amount) {

        accountRepository.findByAccountNumber(accountNumber).ifPresent(account -> {
            account.credit(amount);
            accountRepository.save(account);
        });

        // If this throws RuntimeException → ROLLBACK (everything above undone)
        // If this throws a checked exception → NO ROLLBACK (credit persists!)
        // This is the DEFAULT behaviour.
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   rollbackFor — Rollback for SPECIFIC exception types                    ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * rollbackFor specifies exception classes that SHOULD trigger a rollback,
     * EVEN if they are checked exceptions (which normally don't trigger rollback).
     *
     * Usage:
     *   rollbackFor = InsufficientFundsException.class  → rollback for this one class
     *   rollbackFor = Exception.class                   → rollback for ALL exceptions
     *   rollbackFor = {IOException.class, SQLException.class} → rollback for multiple
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 SCENARIO — Transfer with rollback on InsufficientFundsException:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   InsufficientFundsException is a CHECKED exception (extends Exception).
     *   Without rollbackFor: if debit fails, the transaction COMMITS (BUG!)
     *   With rollbackFor: if debit fails, the transaction ROLLS BACK (correct!)
     *
     *   RESULT of NOT having rollbackFor here:
     *     - debit() throws InsufficientFundsException (checked)
     *     - Spring sees: "it's a checked exception, default is NO rollback"
     *     - Any DB changes made BEFORE the exception are COMMITTED
     *     - The exception propagates to the caller
     *     - Data is in INCONSISTENT state (partially applied)
     *
     *   RESULT of HAVING rollbackFor = InsufficientFundsException.class:
     *     - debit() throws InsufficientFundsException
     *     - Spring sees: "this is in the rollbackFor list → ROLLBACK"
     *     - ALL DB changes in this tx are rolled back
     *     - Clean state maintained
     *
     */
    @Transactional(rollbackFor = BankAccount.InsufficientFundsException.class)
    public void transferWithCheckedExceptionRollback(String fromAccNr, String toAccNr,
                                                      BigDecimal amount)
            throws BankAccount.InsufficientFundsException {

        BankAccount from = accountRepository.findByAccountNumber(fromAccNr)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + fromAccNr));
        BankAccount to   = accountRepository.findByAccountNumber(toAccNr)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + toAccNr));

        // This may throw InsufficientFundsException (CHECKED!)
        // Without rollbackFor: if this throws, transaction COMMITS (wrong!)
        // With rollbackFor: if this throws, transaction ROLLS BACK (correct!)
        from.debit(amount);   // ← May throw InsufficientFundsException
        accountRepository.save(from);

        to.credit(amount);
        accountRepository.save(to);

        System.out.println("  ✅ Transfer complete — committed");
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   rollbackFor = Exception.class — Rollback for ALL exceptions            ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * RECOMMENDATION for most transactional write methods:
     * Use rollbackFor = Exception.class to ensure ANY exception causes rollback.
     *
     * This is the SAFEST option for write methods.
     * It means: "If ANYTHING goes wrong, roll back ALL changes."
     *
     * TRADE-OFF: Even expected application exceptions (like validation errors)
     * will trigger rollback. If you want to handle some exceptions without
     * rolling back, use noRollbackFor.
     */
    @Transactional(rollbackFor = Exception.class)  // ← ALL exceptions trigger rollback
    public void createAccountWithFullRollback(String accountNumber, String ownerName,
                                               BigDecimal initialBalance)
            throws Exception {
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            // AccountExistsException (checked) → ROLLBACK because rollbackFor = Exception.class
            throw new Exception("Account already exists: " + accountNumber);
        }
        BankAccount account = new BankAccount(accountNumber, ownerName,
                                              initialBalance, "CHECKING");
        accountRepository.save(account);
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   noRollbackFor — Exceptions that should NOT trigger rollback            ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * noRollbackFor specifies exception classes that should NOT trigger rollback,
     * even if they are RuntimeExceptions (which normally DO trigger rollback).
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 SCENARIO — Low balance warning should NOT rollback the audit log:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   SCENARIO: Process a batch of accounts. For each account:
     *     1. Read balance
     *     2. Log the balance check
     *     3. If balance < $10, throw LowBalanceWarning (RuntimeException)
     *        but we still WANT to keep the log entry (don't rollback!)
     *
     *   WITHOUT noRollbackFor: LowBalanceWarning → ROLLBACK → log entry deleted!
     *   WITH noRollbackFor:    LowBalanceWarning → NO ROLLBACK → log entry KEPT!
     *
     */
    @Transactional(noRollbackFor = LowBalanceWarning.class)
    public void processWithLowBalanceWarning(String accountNumber)
            throws LowBalanceWarning {

        BankAccount account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Save the processing log (want this to persist even if warning is thrown)
        logRepository.save(TransactionLog.success(
            accountNumber, null, account.getBalance(),
            TransactionLog.OperationType.INQUIRY));

        System.out.println("  [noRollbackFor] Log saved — will NOT be rolled back");

        if (account.getBalance().compareTo(new BigDecimal("10")) < 0) {
            // LowBalanceWarning is a RuntimeException — would normally ROLLBACK
            // But noRollbackFor = LowBalanceWarning.class → log entry is KEPT
            throw new LowBalanceWarning(
                "Account " + accountNumber + " has low balance: " + account.getBalance());
        }
    }

    /** Custom RuntimeException for low balance warning. */
    static class LowBalanceWarning extends RuntimeException {
        LowBalanceWarning(String message) { super(message); }
    }


    // ══════════════════════════════════════════════════════════════════════════════
    //  PART B: PROGRAMMATIC ROLLBACK
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   PROGRAMMATIC ROLLBACK — TransactionAspectSupport.setRollbackOnly()     ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 THE PROBLEM:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * SCENARIO: You're handling a batch transfer. One transfer fails.
     * You catch the exception (to log it), but you STILL want to rollback
     * the entire transaction. But catching the exception means Spring won't
     * see it → Spring won't rollback!
     *
     *   @Transactional
     *   void processTransfer() {
     *       try {
     *           doTransfer();
     *       } catch (Exception e) {
     *           log.error("Transfer failed", e);
     *           // ← Exception swallowed! Spring has no idea this failed!
     *           // → Transaction COMMITS with partial changes!
     *       }
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 THE SOLUTION: setRollbackOnly()
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * Call TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
     * to mark the current transaction for rollback without rethrowing the exception.
     *
     * Spring will attempt to commit when the method ends, but seeing rollbackOnly=true,
     * it will ROLLBACK instead.
     *
     */
    @Transactional
    public void processTransferWithProgrammaticRollback(String from, String to,
                                                          BigDecimal amount) {
        try {
            BankAccount fromAccount = accountRepository.findByAccountNumber(from)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + from));
            BankAccount toAccount   = accountRepository.findByAccountNumber(to)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + to));

            fromAccount.debit(amount);
            accountRepository.save(fromAccount);
            toAccount.credit(amount);
            accountRepository.save(toAccount);

        } catch (Exception e) {
            // Log the error but DON'T rethrow
            System.out.println("  ⚠️  Transfer failed: " + e.getMessage());
            System.out.println("  → Marking transaction for rollback (setRollbackOnly)");

            // PROGRAMMATIC ROLLBACK — marks tx for rollback without rethrowing
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

            // Method returns normally, but Spring will rollback because of setRollbackOnly()
        }
    }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART C: READ-ONLY AND TIMEOUT
// ══════════════════════════════════════════════════════════════════════════════════════

@Service
class ReadOnlyAndTimeoutService {

    private final BankAccountRepository accountRepository;

    ReadOnlyAndTimeoutService(BankAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // READ-ONLY TRANSACTIONS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   readOnly = true — Optimizing Read Operations                           ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 WHAT readOnly = true DOES:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * 1. HIBERNATE OPTIMISATION:
     *    → Hibernate skips dirty checking for ALL loaded entities.
     *      (No need to take a snapshot of each entity at load time for comparison)
     *    → Hibernate does NOT flush the session at commit time.
     *    → Less memory used (no snapshot storage).
     *    → Faster commit (no dirty check scan).
     *
     * 2. DATABASE OPTIMISATION:
     *    → Spring sets Connection.setReadOnly(true) on the JDBC connection.
     *    → Some databases (PostgreSQL with read replicas, MySQL Group Replication)
     *      route read-only transactions to a read replica automatically.
     *    → Some databases may use optimistic locking or shared locks more aggressively.
     *
     * 3. SAFETY CHECK:
     *    → If a write operation accidentally happens in a readOnly tx, Spring
     *      may throw TransientDataAccessResourceException.
     *      (Depends on the persistence provider and DB driver)
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 WHEN readOnly = true MATTERS MOST:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   ✅ Methods that load many entities (batch reads, reports)
     *   ✅ Methods called frequently (hot paths in high-traffic APIs)
     *   ✅ When using Spring's read-replica routing (AbstractRoutingDataSource)
     *   ✅ Any method that genuinely reads and doesn't write
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 PERFORMANCE BENCHMARK (approximate):
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   Loading 1000 entities:
     *     Without readOnly: ~150ms (dirty check snapshots + flush check at commit)
     *     With readOnly:     ~95ms  (40% faster — no snapshots, no flush)
     *
     *   The larger the result set, the bigger the performance gain.
     *
     */
    @Transactional(readOnly = true)
    public List<BankAccount> generateAccountReport() {
        System.out.println("  [readOnly=true] Loading accounts for report");
        System.out.println("  → Hibernate skips dirty checking — 40% faster for large sets");
        System.out.println("  → DB may route to read replica if configured");

        List<BankAccount> accounts = accountRepository.findByActiveTrue();

        // Even if we accidentally call account.setBalance() here,
        // readOnly means Hibernate won't flush the change.
        // (In strict mode, TransientDataAccessResourceException would be thrown)

        System.out.println("  [readOnly=true] Report generated: " + accounts.size() + " accounts");
        return accounts;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalBalance() {
        System.out.println("  [readOnly=true] Calculating total balance");
        BigDecimal total = accountRepository.sumAllActiveBalances();
        return total != null ? total : BigDecimal.ZERO;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TRANSACTION TIMEOUT
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   timeout — Prevent Long-Running Transactions                            ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * timeout specifies the maximum number of SECONDS a transaction can run.
     * If the transaction is still active after timeout seconds, Spring throws
     * TransactionTimedOutException and ROLLS BACK the transaction.
     *
     * Default: -1 (no timeout — transaction can run indefinitely)
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 WHY IS TIMEOUT IMPORTANT?
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * Long-running transactions are dangerous because:
     *
     *   1. LOCK HOLDING: A transaction holds database locks.
     *      Long transactions hold locks for a long time → other transactions must WAIT.
     *      In the worst case, cascading wait → system-wide slowdown.
     *
     *   2. CONNECTION POOL EXHAUSTION: A transaction holds a DB connection.
     *      Long transactions = connections tied up for longer.
     *      Too many long transactions = connection pool exhausted → no connections available.
     *
     *   3. CASCADING FAILURES: A slow external call (email service, payment API)
     *      inside a @Transactional method can cause the DB transaction to run
     *      for seconds or minutes, causing all the above problems.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 WHAT TIMEOUT PREVENTS:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   ✅ Runaway transactions from infinite loops or hanging external calls
     *   ✅ Long-held locks that block other transactions
     *   ✅ Connection pool exhaustion
     *   ✅ Database deadlocks caused by long lock holding
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 BEST PRACTICES FOR timeout:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   SHORT transactions (< 1 second):    Don't need timeout (fast by design)
     *   Normal transactions (1-5 seconds):  timeout = 30 (safety net)
     *   Complex transactions (up to 30s):   timeout = 60
     *
     *   RULE: Keep transactions SHORT. Move external calls (API, email, file I/O)
     *         OUTSIDE of @Transactional boundaries whenever possible.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * ⚠️  IMPORTANT NOTE:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * timeout is enforced at the NEXT DB operation AFTER the timeout expires.
     * If the transaction is just sleeping (no DB activity), the timeout may not
     * be enforced until the next DB call is made.
     *
     * Behaviour depends on the JDBC driver and database engine.
     *
     */
    @Transactional(timeout = 30)   // ← transaction must complete within 30 seconds
    public void processAccountBatch(List<String> accountNumbers) {
        System.out.println("  [timeout=30s] Processing " + accountNumbers.size() + " accounts");
        System.out.println("  → TransactionTimedOutException thrown if > 30s");

        for (String accNr : accountNumbers) {
            accountRepository.findByAccountNumber(accNr).ifPresent(account -> {
                // Process each account...
                System.out.println("  Processing: " + accNr);
            });
        }

        System.out.println("  [timeout=30s] Batch completed within timeout");
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   ANTI-PATTERN: External call inside @Transactional                      ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ❌ WRONG: External API call INSIDE @Transactional
     *
     *   @Transactional
     *   public void processPayment(Payment payment) {
     *       // Save payment to DB (holds DB lock)
     *       paymentRepository.save(payment);
     *
     *       // EXTERNAL CALL — can take 0ms to 30 seconds!
     *       // During this wait, the DB transaction is OPEN and holding LOCKS
     *       externalPaymentGateway.process(payment);  ← DANGEROUS!
     *
     *       // Update status (after potentially 30s wait)
     *       paymentRepository.updateStatus(payment.getId(), "PROCESSED");
     *   }
     *
     * ✅ CORRECT: External call OUTSIDE @Transactional
     *
     *   public void processPayment(Payment payment) {
     *       // FIRST: call external API (no DB lock held)
     *       String gatewayRef = externalPaymentGateway.process(payment);
     *
     *       // THEN: save to DB in a short transaction
     *       savePaymentResult(payment, gatewayRef);
     *   }
     *
     *   @Transactional(timeout = 5)   // Short, tight timeout
     *   void savePaymentResult(Payment payment, String gatewayRef) {
     *       payment.setGatewayRef(gatewayRef);
     *       paymentRepository.save(payment);
     *   }
     */
    @Transactional(timeout = 5)   // Very tight timeout for simple DB operation
    public void updateAccountStatus(String accountNumber, boolean active) {
        accountRepository.findByAccountNumber(accountNumber).ifPresent(account -> {
            account.setActive(active);
            accountRepository.save(account);
        });
    }
}


/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📚 LEARNING SUMMARY 📚
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * ✅ WHAT WE LEARNED IN EXAMPLE 04:
 *
 *  ATTRIBUTE           KEY POINTS
 *  ──────────────────  ─────────────────────────────────────────────────────────────
 *  rollbackFor         Checked exceptions DON'T rollback by default.
 *                      Use rollbackFor = Exception.class for ALL exceptions.
 *                      Critical for data consistency with checked exceptions!
 *
 *  noRollbackFor       Prevents rollback for specific RuntimeExceptions.
 *                      Useful when a warning/notification exception shouldn't
 *                      undo committed work.
 *
 *  setRollbackOnly()   Programmatic rollback — mark tx for rollback when you
 *                      catch and swallow an exception.
 *
 *  readOnly = true     ~40% faster for bulk reads (no dirty check snapshots).
 *                      DB may route to read replica.
 *                      Hibernate doesn't flush → accidental writes not persisted.
 *
 *  timeout             Prevents long-running transactions from holding locks.
 *                      Critical: NEVER make external API calls inside @Transactional!
 *                      External calls → move OUTSIDE the transaction boundary.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ➡️  NEXT: HowItWorksExplained.java — internal transaction mechanics
 * ─────────────────────────────────────────────────────────────────────────────────
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
class Example04RollbackAndAdvanced {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║   CHAPTER 6 — EXAMPLE 04: Rollback, ReadOnly, Timeout            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  rollbackFor = Exception.class → rollback for ALL exceptions");
        System.out.println("  Default: only RuntimeException + Error trigger rollback!");
        System.out.println("  Checked exceptions DON'T rollback → common bug source");
        System.out.println();
        System.out.println("  noRollbackFor  → specific RuntimeException won't rollback");
        System.out.println("  setRollbackOnly() → programmatic rollback (swallowed exception)");
        System.out.println();
        System.out.println("  readOnly=true  → 40% faster reads, no dirty checking, read replica");
        System.out.println("  timeout=30     → rollback if transaction takes > 30 seconds");
        System.out.println("  NEVER make external API calls inside @Transactional!");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

