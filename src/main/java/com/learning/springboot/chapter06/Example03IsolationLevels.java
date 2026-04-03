package com.learning.springboot.chapter06;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   EXAMPLE 03: ALL 5 TRANSACTION ISOLATION LEVELS                                    ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example03IsolationLevels.java
 * Purpose:     Deep-dive into every isolation level with:
 *               - What concurrency problems it prevents
 *               - Performance vs correctness trade-offs
 *               - Real-world scenarios for each level
 *               - Code examples with @Transactional(isolation = ...)
 * Difficulty:  ⭐⭐⭐⭐ Intermediate–Advanced
 * Time:        60–90 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * PREREQUISITE KNOWLEDGE: The 3 Concurrency Problems
 * ─────────────────────────────────────────────────────────────────────────────────────
 *
 * Before understanding isolation levels, you must understand the problems they solve:
 *
 * ┌────────────────────────────────────────────────────────────────────────────────────┐
 * │  PROBLEM 1: DIRTY READ                                                             │
 * │  ───────────────────────                                                           │
 * │  Tx A reads data that Tx B has WRITTEN but NOT YET committed.                     │
 * │  If Tx B ROLLS BACK, Tx A has read data that never actually existed in the DB!    │
 * │                                                                                    │
 * │  TIMELINE:                                                                         │
 * │    Tx A: BEGIN                                                                     │
 * │    Tx B: BEGIN                                                                     │
 * │    Tx B: UPDATE account SET balance = 5000 WHERE id = 1 (not committed yet)       │
 * │    Tx A: SELECT balance FROM account WHERE id = 1 → reads 5000 ← DIRTY READ!     │
 * │    Tx B: ROLLBACK (balance stays at original value, say 1000)                     │
 * │    Tx A: thinks balance is 5000, but actual balance is 1000 → WRONG!              │
 * └────────────────────────────────────────────────────────────────────────────────────┘
 *
 * ┌────────────────────────────────────────────────────────────────────────────────────┐
 * │  PROBLEM 2: NON-REPEATABLE READ                                                    │
 * │  ──────────────────────────────                                                   │
 * │  Tx A reads the SAME row twice. Between the two reads, Tx B updates and           │
 * │  commits that row. Tx A's two reads return DIFFERENT values!                      │
 * │                                                                                    │
 * │  TIMELINE:                                                                         │
 * │    Tx A: SELECT balance FROM account WHERE id = 1 → reads 1000                   │
 * │    Tx B: UPDATE account SET balance = 2000 WHERE id = 1; COMMIT                  │
 * │    Tx A: SELECT balance FROM account WHERE id = 1 → reads 2000 ← DIFFERENT!      │
 * │    Tx A: 1000 ≠ 2000 → non-repeatable read (same row, same tx, different values) │
 * └────────────────────────────────────────────────────────────────────────────────────┘
 *
 * ┌────────────────────────────────────────────────────────────────────────────────────┐
 * │  PROBLEM 3: PHANTOM READ                                                           │
 * │  ────────────────────────                                                          │
 * │  Tx A executes a query that returns N rows. Tx B INSERTS a new row matching       │
 * │  Tx A's WHERE clause and commits. Tx A re-executes the same query → gets N+1!    │
 * │  The new row is a "phantom" that appeared between Tx A's two identical queries.   │
 * │                                                                                    │
 * │  TIMELINE:                                                                         │
 * │    Tx A: SELECT * FROM account WHERE balance > 1000 → returns 3 rows              │
 * │    Tx B: INSERT INTO account (balance=5000, ...) → COMMIT                         │
 * │    Tx A: SELECT * FROM account WHERE balance > 1000 → returns 4 rows ← PHANTOM!  │
 * └────────────────────────────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

@Service
class IsolationLevelService {

    private final BankAccountRepository accountRepository;

    IsolationLevelService(BankAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ISOLATION LEVEL 1: READ_UNCOMMITTED
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   READ_UNCOMMITTED — The Lowest Isolation Level                           ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * This transaction CAN READ uncommitted changes made by other transactions.
     *
     * ✅ PREVENTS: Nothing (no locking)
     * ❌ ALLOWS:   Dirty reads, Non-repeatable reads, Phantom reads
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 HOW IT WORKS:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * No read locks acquired. The transaction reads whatever data is currently
     * in the database pages, even if that data hasn't been committed yet.
     * This is the most permissive (and dangerous) level.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 PERFORMANCE:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * FASTEST — No locking overhead. Maximum concurrency.
     * Other transactions never have to wait for this transaction to read.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 WHEN TO USE:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   ✅ Approximate/statistical queries where slight inaccuracy is acceptable
     *   ✅ Real-time dashboards showing approximate counts
     *   ✅ Non-critical reporting where stale/wrong data is tolerable
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * ⚠️  DANGER:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   AVOID for financial data, critical business operations.
     *   If another transaction is updating balances and then rolls back,
     *   you might read and act on those rolled-back values!
     *   "Alice has $10,000" → we process something → Alice's tx rolled back to $100.
     *   Real-world consequence: authorising a payment based on uncommitted balance.
     *
     *   NOTE: H2 (our in-memory DB) and some other DBs may UPGRADE this to
     *   READ_COMMITTED internally. The behaviour depends on the DB engine.
     *
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public long getApproximateTotalAccountCount() {
        System.out.println("  [READ_UNCOMMITTED] Getting approximate count");
        System.out.println("  → May read uncommitted data from other transactions");
        System.out.println("  → Fastest but least safe — OK for approximate stats only");

        // This query MIGHT read rows that are being inserted/modified by other txs
        // and haven't committed yet. If those txs roll back, the count is wrong.
        return accountRepository.count();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ISOLATION LEVEL 2: READ_COMMITTED
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   READ_COMMITTED — Most Databases' Default                                ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * This transaction ONLY reads data that has been COMMITTED by other transactions.
     * Uncommitted changes from other transactions are INVISIBLE to this transaction.
     *
     * ✅ PREVENTS: Dirty reads
     * ❌ ALLOWS:   Non-repeatable reads, Phantom reads
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 HOW IT WORKS:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * Database acquires a shared lock on read data, but releases it IMMEDIATELY
     * after the read (not held until end of transaction).
     *
     * Consequence: Two reads of the same row in the SAME transaction CAN return
     * different values if another transaction commits a change between the reads.
     * This is the NON-REPEATABLE READ problem.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFAULT FOR:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   PostgreSQL, Oracle, SQL Server (and most enterprise databases)
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 WHEN TO USE:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   ✅ General purpose — the sweet spot for most applications
     *   ✅ OLTP applications with moderate concurrency
     *   ✅ When dirty reads must be prevented but non-repeatable reads are tolerable
     *   ✅ User-facing reads where showing the latest committed data is desired
     *
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, readOnly = true)
    public List<BankAccount> getCommittedAccounts() {
        System.out.println("  [READ_COMMITTED] Reading only committed account data");
        System.out.println("  → No dirty reads (uncommitted changes invisible)");
        System.out.println("  → Non-repeatable reads still possible");
        System.out.println("  → Default level for PostgreSQL and most enterprise DBs");

        // ONLY sees committed data. Safe from dirty reads.
        // But: if you call this method twice, second call might see new/updated accounts
        // if another transaction committed between the two calls.
        return accountRepository.findByActiveTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ISOLATION LEVEL 3: REPEATABLE_READ
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   REPEATABLE_READ — Consistent reads of the same row                     ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * Once a transaction reads a row, it locks that row. No other transaction can
     * UPDATE or DELETE that row until this transaction completes.
     * Re-reading the same row in the same transaction ALWAYS returns the same value.
     *
     * ✅ PREVENTS: Dirty reads, Non-repeatable reads
     * ❌ ALLOWS:   Phantom reads (new rows can appear)
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 HOW IT WORKS:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * Database holds shared locks on ALL rows read until the end of the transaction.
     * Other transactions can READ the rows (shared lock) but cannot MODIFY or DELETE them.
     *
     * In MySQL's InnoDB, REPEATABLE_READ uses MVCC (Multi-Version Concurrency Control):
     * Reads see a snapshot taken at the START of the transaction.
     * Even if another tx commits changes, this tx sees the original snapshot.
     * MySQL's REPEATABLE_READ actually ALSO prevents phantom reads!
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFAULT FOR:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   MySQL (InnoDB engine)
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 WHEN TO USE:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   ✅ Financial calculations where re-reading same data must be consistent
     *   ✅ Multi-step reports that read the same rows multiple times
     *   ✅ Scenarios where you load data, calculate, then update — and need
     *      the data to not change between load and update
     *   ✅ "Snapshot" behaviour: read-and-decide operations
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 REAL-WORLD USE CASE — Balance Verification Before Transfer:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   SCENARIO: Verify balance, show it to user, then process the transfer.
     *   PROBLEM:  Between verify() and transfer(), balance might change.
     *   SOLUTION: REPEATABLE_READ ensures the balance you read at verify() is
     *             the same balance you'll read again at transfer time.
     *
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BigDecimal verifyAndCalculateFee(String accountNumber, BigDecimal transferAmount) {
        System.out.println("  [REPEATABLE_READ] Starting balance verification");

        // FIRST READ of balance
        BankAccount account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        BigDecimal balance = account.getBalance();

        System.out.println("  [REPEATABLE_READ] First read: balance = " + balance);

        // ... some processing (potentially slow) ...
        // Even if another tx modifies and commits this account's balance NOW,
        // our next read will still see the SAME balance (our snapshot from tx start).

        BigDecimal fee = transferAmount.multiply(new BigDecimal("0.001"));

        // SECOND READ — with REPEATABLE_READ, GUARANTEED to return same balance
        // as the first read within this transaction
        BankAccount reloaded = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        System.out.println("  [REPEATABLE_READ] Second read: balance = " + reloaded.getBalance());
        System.out.println("  [REPEATABLE_READ] Both reads consistent? "
            + balance.equals(reloaded.getBalance()));

        if (reloaded.getBalance().compareTo(transferAmount.add(fee)) < 0) {
            throw new IllegalStateException("Insufficient balance for transfer + fee");
        }

        return fee;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ISOLATION LEVEL 4: SERIALIZABLE
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   SERIALIZABLE — The Highest (Strictest) Isolation Level                 ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * SERIALIZABLE provides COMPLETE isolation. It's as if transactions execute
     * ONE AT A TIME (serially), even though they may actually be concurrent.
     *
     * ✅ PREVENTS: Dirty reads, Non-repeatable reads, Phantom reads
     * ❌ ALLOWS:   Nothing — all concurrency anomalies prevented
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 HOW IT WORKS:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * Database locks the ENTIRE RANGE of data that the query touches.
     * Not just the rows that were read, but also the "gaps" between rows.
     * This prevents phantom reads: even if another tx inserts a new row in the
     * range, this tx won't see it.
     *
     * Implementation:
     *   → Range locks / predicate locks on WHERE conditions
     *   → Other transactions trying to insert/update data in the locked range
     *     must WAIT until this transaction completes
     *   → This can lead to DEADLOCKS if multiple serializable transactions
     *     are waiting on each other
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 PERFORMANCE:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * SLOWEST — Maximum locking overhead. Minimum concurrency.
     * In high-concurrency systems, can cause significant contention and deadlocks.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 WHEN TO USE:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   ✅ Critical financial operations (dividend calculation, reconciliation)
     *   ✅ Compliance scenarios where absolute data consistency is required
     *   ✅ Inventory management: "last item in stock" scenarios
     *   ✅ Unique constraint logic in application code (when DB constraint isn't enough)
     *   ✅ Low-volume, high-criticality operations
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 REAL-WORLD USE CASE — Dividend Calculation:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   Total dividend pool: $10,000
     *   Step 1: SELECT COUNT(*) FROM shareholders → 100 shareholders
     *   Step 2: dividend_per_share = 10000 / 100 = $100
     *   Step 3: UPDATE each shareholder's balance += 100
     *
     *   WITHOUT SERIALIZABLE: Between Step 1 and Step 3, 5 more shareholders
     *   could join. We'd distribute $100 each to 105 people but only had $10,000!
     *
     *   WITH SERIALIZABLE: The range lock prevents new shareholders from being
     *   inserted until our transaction completes. Calculation is accurate.
     *
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BigDecimal calculateAndDistributeDividend(BigDecimal totalPool) {
        System.out.println("  [SERIALIZABLE] Starting dividend calculation — full range lock");

        // COUNT active accounts — range lock prevents new accounts from being added
        List<BankAccount> accounts = accountRepository.findByActiveTrue();
        int count = accounts.size();

        if (count == 0) {
            System.out.println("  [SERIALIZABLE] No active accounts — nothing to distribute");
            return BigDecimal.ZERO;
        }

        // Calculate per-account dividend
        BigDecimal dividend = totalPool.divide(new BigDecimal(count), 2,
                                               java.math.RoundingMode.FLOOR);

        System.out.println("  [SERIALIZABLE] Pool: " + totalPool + ", Accounts: " + count
                         + ", Per account: " + dividend);

        // Credit each account — NO new accounts can be added during this loop
        // (range lock from SERIALIZABLE prevents phantom reads/inserts)
        for (BankAccount account : accounts) {
            account.credit(dividend);
            accountRepository.save(account);
        }

        System.out.println("  [SERIALIZABLE] Distributed " + dividend
                         + " to " + count + " accounts");

        return dividend;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ISOLATION LEVEL 5: DEFAULT
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   DEFAULT — Use the Database's Default Isolation Level                   ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * DEFAULT tells Spring: "Use whatever isolation level the database is configured
     * with by default. Don't override it."
     *
     * This is the SPRING DEFAULT when you don't specify isolation.
     *
     * TYPICAL DATABASE DEFAULTS:
     *   PostgreSQL   → READ_COMMITTED
     *   MySQL/InnoDB → REPEATABLE_READ
     *   Oracle       → READ_COMMITTED
     *   SQL Server   → READ_COMMITTED
     *   H2 (our DB)  → READ_COMMITTED
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 WHEN TO USE DEFAULT:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *   ✅ 90% of cases — the DB default is usually appropriate
     *   ✅ When you're happy with the database's default behaviour
     *   ✅ When the DBA has configured the appropriate level at DB level
     *   ✅ When you don't want to hard-code an isolation level in your code
     *      (makes it easier to switch databases)
     *
     */
    @Transactional(isolation = Isolation.DEFAULT, readOnly = true)
    public BankAccount findAccount(String accountNumber) {
        System.out.println("  [DEFAULT isolation] Using database's default isolation level");
        System.out.println("  → H2 default: READ_COMMITTED");
        return accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // CHOOSING THE RIGHT ISOLATION LEVEL — Decision Guide
    // ─────────────────────────────────────────────────────────────────────────────

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   HOW TO CHOOSE YOUR ISOLATION LEVEL                                     ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ASK THESE QUESTIONS:
     *
     * 1. Can I tolerate dirty reads (reading uncommitted data)?
     *    YES → READ_UNCOMMITTED (rare; use only for approximate reporting)
     *    NO  → READ_COMMITTED or higher
     *
     * 2. Does the same transaction re-read the same rows and need consistent results?
     *    YES → REPEATABLE_READ or higher
     *    NO  → READ_COMMITTED is sufficient
     *
     * 3. Does the same transaction re-execute range queries and need consistent results?
     *    YES (no phantom rows allowed) → SERIALIZABLE
     *    NO  → REPEATABLE_READ is sufficient
     *
     * 4. Is concurrency important? High volume of concurrent transactions?
     *    High concurrency + low isolation → use READ_COMMITTED (default for most cases)
     *    Low concurrency + critical accuracy → use SERIALIZABLE
     *
     * PRACTICAL GUIDANCE:
     *
     *   Most CRUD operations:          DEFAULT (READ_COMMITTED typically)
     *   Multi-step financial reads:    REPEATABLE_READ
     *   Critical financial operations: SERIALIZABLE (rarely needed)
     *   Approximate reporting:         READ_UNCOMMITTED (or no tx at all)
     *
     */
}


/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📚 LEARNING SUMMARY 📚
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * ✅ ALL 5 ISOLATION LEVELS:
 *
 *  LEVEL             DIRTY READ  NON-REP READ  PHANTOM READ  PERF    USE WHEN
 *  ────────────────  ──────────  ────────────  ────────────  ──────  ──────────────────────
 *  READ_UNCOMMITTED  ALLOWED     ALLOWED       ALLOWED       Fastest Approximate stats
 *  READ_COMMITTED    prevented   ALLOWED       ALLOWED       Fast    Most CRUD (PostgreSQL default)
 *  REPEATABLE_READ   prevented   prevented     ALLOWED       Mod     Multi-read in same tx
 *  SERIALIZABLE      prevented   prevented     prevented     Slowest Critical financial ops
 *  DEFAULT           Uses DB default (READ_COMMITTED for most DBs)     Standard usage
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ➡️  NEXT: Example04RollbackAndAdvanced.java — rollbackFor, readOnly, timeout
 * ─────────────────────────────────────────────────────────────────────────────────
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
class Example03IsolationLevels {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║   CHAPTER 6 — EXAMPLE 03: All 5 Isolation Levels                 ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  Concurrency Problems:");
        System.out.println("    Dirty Read       — reading uncommitted data (may disappear!)");
        System.out.println("    Non-Repeatable   — same row, different values in same tx");
        System.out.println("    Phantom Read     — extra rows appear between queries");
        System.out.println();
        System.out.println("  Isolation Levels:");
        System.out.println("    READ_UNCOMMITTED → nothing prevented (fastest, dangerous)");
        System.out.println("    READ_COMMITTED   → no dirty reads (DB default for Postgres/Oracle)");
        System.out.println("    REPEATABLE_READ  → + no non-repeatable (MySQL default)");
        System.out.println("    SERIALIZABLE     → + no phantoms (strictest, slowest)");
        System.out.println("    DEFAULT          → use whatever the DB is configured with");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

