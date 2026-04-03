package com.learning.springboot.chapter06;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   EXAMPLE 01: @Transactional BASICS & @EnableTransactionManagement                  ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example01TransactionalBasics.java
 * Purpose:     Master the fundamentals of @Transactional:
 *               - What it does and how it works
 *               - Class-level vs method-level placement
 *               - readOnly class-level + write method-level pattern
 *               - @EnableTransactionManagement (when and why)
 *               - Transaction boundaries
 *               - Checking active transaction programmatically
 * Difficulty:  ⭐⭐ Beginner–Intermediate
 * Time:        45–60 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART A: @EnableTransactionManagement — explained
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║    @EnableTransactionManagement — WHAT IT DOES AND WHEN YOU NEED IT          ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @EnableTransactionManagement switches on Spring's annotation-driven transaction
 * management infrastructure. It registers a BeanFactoryTransactionAttributeSourceAdvisor
 * which intercepts beans annotated with @Transactional and wraps them in a
 * transactional proxy.
 *
 * THINK OF IT AS: The "on switch" for @Transactional.
 * Without it: @Transactional annotations exist on your code but DO NOTHING.
 * With it:    @Transactional annotations are processed by Spring's AOP infrastructure.
 *
 * IN SPRING BOOT:
 *   Spring Boot's TransactionAutoConfiguration detects spring-tx on the classpath
 *   and automatically applies @EnableTransactionManagement.
 *   → You DO NOT need to add @EnableTransactionManagement manually in Spring Boot.
 *   → It's safe to add it anyway — it's idempotent.
 *
 * IN PLAIN SPRING (no Boot):
 *   You MUST add @EnableTransactionManagement to a @Configuration class.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 ATTRIBUTES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   mode = AdviceMode.PROXY (default)
 *     Spring creates a JDK or CGLIB proxy around @Transactional beans.
 *     LIMITATION: Self-invocation bypass (see pitfall 1 in Chapter06Overview).
 *
 *   mode = AdviceMode.ASPECTJ
 *     AspectJ compile-time or load-time weaving is used.
 *     Works even for self-invocation and private methods!
 *     Requires: aspectjweaver on classpath + Spring's load-time weaver configured.
 *     Rarely needed in practice.
 *
 *   proxyTargetClass = false (default)
 *     JDK dynamic proxies (requires interface implementation by the bean).
 *     NOTE: Spring Boot sets spring.aop.proxy-target-class=true globally,
 *     which overrides this to CGLIB (no interface needed).
 *
 *   proxyTargetClass = true
 *     CGLIB subclass proxies (works without interfaces).
 *
 *   order = Ordered.LOWEST_PRECEDENCE (default)
 *     AOP advisor order relative to other advisors.
 *     Reduce value (e.g., 1) to run transaction BEFORE security checks.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * The class below demonstrates it being used (though Spring Boot doesn't require it).
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@EnableTransactionManagement   // ← Explicit (Spring Boot auto-applies this already)
class TransactionManagementConfig {
    // In a real Spring Boot app, this class isn't needed.
    // Shown here for educational purposes to explain @EnableTransactionManagement.
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART B: @Transactional BASICS — the account service
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║    BankAccountService — Demonstrates @Transactional basics                   ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 CLASS-LEVEL @Transactional(readOnly = true) PATTERN:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * STRATEGY: "Default read-only, override for writes"
 *
 *   WHY readOnly at CLASS level?
 *     Most service methods are reads (findById, findAll, search...).
 *     Marking the class readOnly = true means:
 *       → Hibernate skips dirty checking for ALL objects in the session
 *         (no need to compare entity snapshots at flush time → faster)
 *       → Some databases (PostgreSQL, MySQL) can route to read replicas
 *       → Clearly communicates intent (this method won't modify data)
 *       → If a write accidentally happens in a readOnly tx, Spring throws
 *         TransientDataAccessResourceException (safety net)
 *
 *   HOW TO ADD WRITES?
 *     Add @Transactional (without readOnly) on specific write methods.
 *     Method-level @Transactional OVERRIDES the class-level one.
 *
 *   RESULT:
 *     findById()    → uses class-level: readOnly = true  ✓ (fast read)
 *     findAll()     → uses class-level: readOnly = true  ✓ (fast read)
 *     createAccount() → uses method-level: readOnly = false ✓ (write)
 *     transfer()    → uses method-level: readOnly = false ✓ (write)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Service
@Transactional(readOnly = true)  // ← ALL methods default to read-only
class BankAccountService {

    private final BankAccountRepository accountRepository;
    private final TransactionLogRepository logRepository;

    BankAccountService(BankAccountRepository accountRepository,
                       TransactionLogRepository logRepository) {
        this.accountRepository = accountRepository;
        this.logRepository     = logRepository;
    }

    // ─── READ METHODS — inherit class-level readOnly = true ───────────────────────

    /**
     * 📌 @Transactional(readOnly = true) — inherited from class level
     *
     * WHAT HAPPENS:
     *   1. Spring opens a transaction with readOnly=true hint
     *   2. Hibernate sets the connection as readOnly
     *   3. findById() executes SELECT
     *   4. Hibernate does NOT track the loaded entity (no dirty check snapshot)
     *   5. Transaction commits (no flush needed — no changes)
     *
     * BENEFIT: No entity snapshot stored → less memory, less CPU at commit time.
     */
    public Optional<BankAccount> findById(Long id) {
        // Programmatically check if we're in a transaction
        boolean inTx = TransactionSynchronizationManager.isActualTransactionActive();
        String txName = TransactionSynchronizationManager.getCurrentTransactionName();
        System.out.println("  findById() — In transaction: " + inTx + ", name: " + txName);

        return accountRepository.findById(id);
    }

    public Optional<BankAccount> findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public List<BankAccount> findAllActiveAccounts() {
        return accountRepository.findByActiveTrue();
    }

    public List<BankAccount> findByOwner(String ownerName) {
        return accountRepository.findByOwnerName(ownerName);
    }

    public BigDecimal getTotalBankBalance() {
        BigDecimal total = accountRepository.sumAllActiveBalances();
        return total != null ? total : BigDecimal.ZERO;
    }

    // ─── WRITE METHODS — override class-level with @Transactional (readOnly=false) ─

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   @Transactional on METHOD — overrides class-level readOnly=true         ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * 📌 CREATE ACCOUNT
     *
     * WHAT @Transactional does here:
     *   1. Spring's proxy intercepts the call to createAccount()
     *   2. Calls transactionManager.getTransaction() → opens a new DB connection
     *      from the connection pool and starts a transaction (BEGIN)
     *   3. Method body executes — repository.save() queues an INSERT
     *   4. Hibernate flushes: INSERT INTO ch06_bank_accounts (...)
     *   5. On success: transactionManager.commit() → COMMIT sent to database
     *   6. DB confirms commit → connection returned to pool
     *
     * If an exception is thrown:
     *   5. transactionManager.rollback() → ROLLBACK sent to database
     *   6. No data persisted → connection returned to pool
     */
    @Transactional  // readOnly = false (default) — overrides class-level
    public BankAccount createAccount(String accountNumber, String ownerName,
                                     BigDecimal initialBalance, String accountType) {
        // Validate
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new IllegalArgumentException(
                "Account number already exists: " + accountNumber);
        }
        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }

        BankAccount account = new BankAccount(accountNumber, ownerName,
                                              initialBalance, accountType);

        BankAccount saved = accountRepository.save(account);

        // Log the creation
        logRepository.save(TransactionLog.success(
            null, accountNumber, initialBalance,
            TransactionLog.OperationType.CREDIT));

        System.out.println("  ✅ Account created and logged within same transaction");
        return saved;
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   SIMPLE TRANSFER — Shows atomicity clearly                              ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * 📌 ATOMICITY DEMONSTRATION
     *
     * Both debit AND credit happen in ONE transaction.
     * Either BOTH succeed (commit) or NEITHER persists (rollback).
     *
     * Without @Transactional: two separate auto-commit operations.
     *   → Crash after debit, before credit: $500 lost!
     *
     * With @Transactional: one atomic operation.
     *   → Crash midway: ROLLBACK → both debit and credit undone.
     *
     * TRANSACTION BOUNDARY:
     *   START ──→ debit(from) ──→ credit(to) ──→ COMMIT
     *             ↕                               ↕
     *         (exception)                    (success)
     *             ↓                               ↓
     *          ROLLBACK                     DB persisted
     *
     * @param fromAccountNumber  source account
     * @param toAccountNumber    destination account
     * @param amount             amount to transfer (must be positive)
     */
    @Transactional  // One atomic transaction for the ENTIRE transfer
    public TransferResult simpleTransfer(String fromAccountNumber,
                                          String toAccountNumber,
                                          BigDecimal amount)
            throws BankAccount.InsufficientFundsException {

        System.out.println("  → Transfer started: " + fromAccountNumber
                         + " → " + toAccountNumber + " $" + amount);

        // STEP 1: Load accounts
        BankAccount from = accountRepository.findByAccountNumber(fromAccountNumber)
            .orElseThrow(() -> new IllegalArgumentException(
                "Source account not found: " + fromAccountNumber));

        BankAccount to = accountRepository.findByAccountNumber(toAccountNumber)
            .orElseThrow(() -> new IllegalArgumentException(
                "Destination account not found: " + toAccountNumber));

        // Validate accounts are active
        if (!from.isActive() || !to.isActive()) {
            throw new IllegalStateException("Cannot transfer to/from inactive accounts");
        }

        // STEP 2: Debit source — may throw InsufficientFundsException (CHECKED!)
        BigDecimal balanceBefore = from.getBalance();
        from.debit(amount);    // ← If this throws, the @Transactional method exits
        accountRepository.save(from);  //    with exception → rollback triggered

        // ★ IMPORTANT: If server crashes HERE, the @Transactional ensures
        //   both the debit AND credit are rolled back (atomicity).

        // STEP 3: Credit destination
        to.credit(amount);
        accountRepository.save(to);

        System.out.println("  ✅ Transfer complete within single transaction");

        return new TransferResult(fromAccountNumber, toAccountNumber, amount,
                                  balanceBefore, from.getBalance());
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   DEPOSIT — Simple write demonstrating transaction for a single operation ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     */
    @Transactional
    public BankAccount deposit(String accountNumber, BigDecimal amount) {
        BankAccount account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException(
                "Account not found: " + accountNumber));

        account.credit(amount);
        return accountRepository.save(account);
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   DEACTIVATE — Demonstrates @Transactional on update operation            ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     */
    @Transactional
    public void deactivateAccount(String accountNumber) {
        BankAccount account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException(
                "Account not found: " + accountNumber));

        account.setActive(false);
        // No explicit save() needed — Hibernate dirty tracking detects the change
        // and flushes automatically at transaction commit. This is the "dirty checking"
        // mechanism: Hibernate compares the current state with the snapshot taken
        // when the entity was loaded.
        System.out.println("  ℹ️  No save() called — dirty checking will flush at commit");
    }

    // ─── UTILITY — transaction inspection ────────────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   Demonstrating TransactionSynchronizationManager                        ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * TransactionSynchronizationManager is Spring's central registry for
     * transaction synchronizations and resources.
     *
     * You can use it to inspect the current transaction state programmatically:
     *   isActualTransactionActive() → is a real tx open?
     *   getCurrentTransactionName() → name of the current tx (usually class.method)
     *   isCurrentTransactionReadOnly() → is the current tx read-only?
     */
    @Transactional
    public void inspectTransaction() {
        System.out.println("  ─── Transaction Inspection ─────────────────────────────");
        System.out.println("  Active:    " + TransactionSynchronizationManager.isActualTransactionActive());
        System.out.println("  ReadOnly:  " + TransactionSynchronizationManager.isCurrentTransactionReadOnly());
        System.out.println("  TX Name:   " + TransactionSynchronizationManager.getCurrentTransactionName());
        System.out.println("  ─────────────────────────────────────────────────────────");
    }

    // ─── ANTI-PATTERN EXAMPLES (what NOT to do) ───────────────────────────────────

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║   ❌ ANTI-PATTERN: SELF-INVOCATION — @Transactional ignored!             ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * THIS METHOD HAS A BUG — FOR EDUCATIONAL PURPOSES ONLY!
     *
     * 'this.doInternalTransfer()' bypasses the Spring proxy.
     * The @Transactional on doInternalTransfer() is COMPLETELY IGNORED.
     * No transaction is started for doInternalTransfer().
     *
     * HOW TO FIX:
     *   Option 1: Move doInternalTransfer() to a separate @Service bean
     *   Option 2: Inject BankAccountService self; then self.doInternalTransfer()
     *   Option 3: Use AspectJ mode (rare)
     */
    @Transactional
    public void selfInvocationAntiPattern(String from, String to, BigDecimal amount) {
        System.out.println("  ⚠️  Calling this.doInternalTransfer() — PROXY BYPASSED!");
        // this.doInternalTransfer(from, to, amount);
        // ↑ Even if doInternalTransfer has @Transactional, it is IGNORED here
        //   because 'this' refers to the real object, not the Spring proxy.
        System.out.println("  ⚠️  doInternalTransfer() would run WITHOUT a transaction!");
    }

    // This method's @Transactional is IGNORED when called via self-invocation:
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void doInternalTransfer(String from, String to, BigDecimal amount) {
        // This would NEVER get a new transaction when called via this.doInternalTransfer()
        System.out.println("  Inside doInternalTransfer — but no transaction was started!");
    }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART C: Transfer result DTO
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * Simple result object for transfer operations.
 */
class TransferResult {
    private final String    fromAccount;
    private final String    toAccount;
    private final BigDecimal amount;
    private final BigDecimal balanceBefore;
    private final BigDecimal balanceAfter;

    TransferResult(String from, String to, BigDecimal amount,
                   BigDecimal balanceBefore, BigDecimal balanceAfter) {
        this.fromAccount   = from;
        this.toAccount     = to;
        this.amount        = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter  = balanceAfter;
    }

    public String    getFromAccount()   { return fromAccount; }
    public String    getToAccount()     { return toAccount; }
    public BigDecimal getAmount()       { return amount; }
    public BigDecimal getBalanceBefore(){ return balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }

    @Override
    public String toString() {
        return "TransferResult{" + fromAccount + " → " + toAccount
               + " $" + amount + " | balance: " + balanceBefore + " → " + balanceAfter + "}";
    }
}


/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📚 LEARNING SUMMARY 📚
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * ✅ WHAT WE LEARNED IN EXAMPLE 01:
 *
 *  CONCEPT                    KEY POINT
 *  ─────────────────────────  ────────────────────────────────────────────────────────
 *  @EnableTransactionManagement  Activates @Transactional processing. Auto-applied
 *                             by Spring Boot. Needed in plain Spring applications.
 *
 *  @Transactional (class)     Applies to ALL public methods. Default attributes.
 *                             Individual methods can OVERRIDE.
 *
 *  @Transactional (method)    Applies ONLY to that method. Overrides class-level.
 *
 *  readOnly = true            Hint for DB optimisation. Hibernate skips dirty checking.
 *  (class-level default)      Override with @Transactional on write methods.
 *
 *  Atomic operation           debit + credit in one @Transactional = either BOTH
 *                             happen or NEITHER does. Core of ACID atomicity.
 *
 *  Dirty checking             Hibernate tracks entity changes. No explicit save()
 *                             needed if entity was loaded within same transaction.
 *
 *  Self-invocation pitfall    this.method() bypasses Spring proxy → @Transactional
 *                             on called method is IGNORED. A critical gotcha!
 *
 *  TransactionSynchronizationManager  Inspect current tx state programmatically.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ➡️  NEXT: Example02PropagationTypes.java — all 7 propagation types
 * ─────────────────────────────────────────────────────────────────────────────────
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
class Example01TransactionalBasics {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║   CHAPTER 6 — EXAMPLE 01: @Transactional Basics                  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  @EnableTransactionManagement → activates @Transactional processing");
        System.out.println("  @Transactional(readOnly=true) at class level → fast reads");
        System.out.println("  @Transactional at method level → overrides for writes");
        System.out.println("  Atomicity: debit + credit in one transaction (ACID)");
        System.out.println("  Dirty checking: save() not needed for loaded entities");
        System.out.println("  Self-invocation: this.method() bypasses proxy → tx ignored!");
        System.out.println("  TransactionSynchronizationManager: inspect tx state");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

