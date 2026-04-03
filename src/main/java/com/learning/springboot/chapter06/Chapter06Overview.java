package com.learning.springboot.chapter06;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║                    SPRING BOOT ANNOTATIONS — COMPREHENSIVE GUIDE                     ║
 * ║                      Chapter 6: Spring Transaction Management                        ║
 * ║                                                                                       ║
 * ║                           📚 From Zero to Expert 📚                                  ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Chapter:      6
 * Title:        Spring Transaction Management Annotations
 * Difficulty:   ⭐⭐⭐⭐ Intermediate–Advanced
 * Estimated:    6–10 hours
 * Prerequisites: Chapters 1–4 (especially Chapter 4: Spring Data JPA)
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │                   CHAPTER 6: OVERVIEW & LEARNING GOALS                              │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📖 TABLE OF CONTENTS 📖
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 *  Section  1:  WHY TRANSACTIONS? — The Problem They Solve
 *  Section  2:  ACID Properties — The Foundation
 *  Section  3:  @EnableTransactionManagement — Activation
 *  Section  4:  @Transactional — The Complete Reference
 *               4.1  Where to Annotate (class vs method)
 *               4.2  All Attributes Quick Reference
 *  Section  5:  PROPAGATION TYPES — All 7 Explained
 *               REQUIRED, REQUIRES_NEW, NESTED, SUPPORTS,
 *               NOT_SUPPORTED, MANDATORY, NEVER
 *  Section  6:  ISOLATION LEVELS — All 5 Explained
 *               DEFAULT, READ_UNCOMMITTED, READ_COMMITTED,
 *               REPEATABLE_READ, SERIALIZABLE
 *               + Concurrency Problems (dirty/phantom/non-repeatable reads)
 *  Section  7:  ROLLBACK RULES — rollbackFor, noRollbackFor
 *  Section  8:  READ-ONLY TRANSACTIONS
 *  Section  9:  TIMEOUT
 *  Section 10:  COMMON PITFALLS & GOTCHAS
 *  Section 11:  How It Works Internally
 *  Section 12:  Best Practices & Production Checklist
 *  Section 13:  Interview Questions & Answers
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * 📂 FILES IN THIS CHAPTER:
 *
 *  src/main/java/.../chapter06/
 *   • Chapter06Overview.java                    ← YOU ARE HERE
 *   • BankAccount.java                           (domain entity — bank account)
 *   • TransactionLog.java                        (domain entity — audit log)
 *   • BankAccountRepository.java                 (JPA repository)
 *   • TransactionLogRepository.java              (JPA repository)
 *   • Example01TransactionalBasics.java          (@Transactional basics, @EnableTransactionManagement)
 *   • Example02PropagationTypes.java             (all 7 propagation types)
 *   • Example03IsolationLevels.java              (all 5 isolation levels)
 *   • Example04RollbackAndAdvanced.java          (rollbackFor, readOnly, timeout, savepoints)
 *   • HowItWorksExplained.java                   (internal mechanics)
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
public class Chapter06Overview {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    SECTION 1: WHY DO WE NEED TRANSACTIONS?                                   ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 💥 THE PROBLEM WITHOUT TRANSACTIONS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Imagine a bank transfer: move $500 from Account A to Account B.
     *
     *   STEP 1: Debit  Account A → balance goes from $1000 to $500
     *   STEP 2: Credit Account B → balance goes from $200  to $700
     *
     * What happens if the server CRASHES after Step 1 but before Step 2?
     *
     *   Account A: $500 (debited ✓)
     *   Account B: $200 (NOT credited ✗)
     *   $500 has VANISHED from the system!
     *
     * This is the fundamental problem transactions solve.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ✅ WITH TRANSACTIONS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Steps 1 and 2 are wrapped in ONE atomic operation.
     *
     *   BEGIN TRANSACTION
     *     Step 1: Debit  Account A
     *     Step 2: Credit Account B
     *   COMMIT TRANSACTION   → BOTH changes persisted
     *
     *   If crash between Step 1 and Step 2:
     *   ROLLBACK TRANSACTION → NEITHER change persisted
     *   Account A: $1000 (restored), Account B: $200 (unchanged)
     *   Data consistency maintained!
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    SECTION 2: ACID PROPERTIES — THE FOUNDATION                               ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Every transaction must satisfy ACID properties:
     *
     * ┌──────────────┬────────────────────────────────────────────────────────────────┐
     * │  PROPERTY    │  MEANING                                                       │
     * ├──────────────┼────────────────────────────────────────────────────────────────┤
     * │  ATOMICITY   │  All operations succeed, or NONE do. No partial results.       │
     * │              │  Bank transfer: debit AND credit both happen, or neither.       │
     * ├──────────────┼────────────────────────────────────────────────────────────────┤
     * │  CONSISTENCY │  DB moves from one valid state to another valid state.          │
     * │              │  Total money before transfer == total money after transfer.     │
     * ├──────────────┼────────────────────────────────────────────────────────────────┤
     * │  ISOLATION   │  Concurrent transactions don't see each other's partial work.  │
     * │              │  Two transfers at the same time don't corrupt each other.       │
     * ├──────────────┼────────────────────────────────────────────────────────────────┤
     * │  DURABILITY  │  Once committed, changes survive system failures.               │
     * │              │  Committed transfer persists even if server crashes next.       │
     * └──────────────┴────────────────────────────────────────────────────────────────┘
     *
     * Spring's @Transactional helps you implement all four ACID properties.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    SECTION 3: @EnableTransactionManagement                                   ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @EnableTransactionManagement activates Spring's annotation-driven transaction
     * management infrastructure. It is what makes @Transactional annotations work.
     *
     * PACKAGE: org.springframework.transaction.annotation.EnableTransactionManagement
     *
     * DO YOU NEED IT?
     *   In a Spring Boot application: NO — Spring Boot's
     *   TransactionAutoConfiguration enables it automatically when spring-tx is present.
     *
     *   In a plain Spring application (no @SpringBootApplication): YES — you must
     *   add @EnableTransactionManagement to a @Configuration class.
     *
     * KEY ATTRIBUTES:
     *
     *   mode (default = PROXY):
     *     PROXY    → Spring creates JDK dynamic proxies or CGLIB proxies.
     *                Works for calls coming FROM OUTSIDE the bean.
     *                Self-invocation (calling @Transactional from within the same class)
     *                DOES NOT trigger the transaction.
     *
     *     ASPECTJ  → Uses AspectJ compile-time or load-time weaving.
     *                Works even for self-invocation!
     *                Requires aspectjweaver on classpath + extra setup.
     *
     *   proxyTargetClass (default = false for @EnableTransactionManagement):
     *     false → Use JDK dynamic proxies (bean must implement an interface)
     *     true  → Use CGLIB proxies (subclass-based; works without interfaces)
     *     NOTE: Spring Boot defaults proxyTargetClass = true globally.
     *
     *   order (default = Ordered.LOWEST_PRECEDENCE):
     *     Controls the order of the transaction advisor relative to other AOP advisors.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 USAGE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   // In Spring Boot — not needed (auto-configured), but valid to add explicitly:
     *   @SpringBootApplication
     *   @EnableTransactionManagement   // optional in Spring Boot
     *   public class MyApplication { ... }
     *
     *   // In plain Spring — REQUIRED:
     *   @Configuration
     *   @EnableTransactionManagement
     *   public class AppConfig {
     *       @Bean
     *       public DataSourceTransactionManager transactionManager(DataSource ds) {
     *           return new DataSourceTransactionManager(ds);
     *       }
     *   }
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    SECTION 4: @Transactional — COMPLETE ATTRIBUTE REFERENCE                  ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * PACKAGE: org.springframework.transaction.annotation.Transactional
     *          (also jakarta.transaction.Transactional — avoid this one in Spring apps)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 ALL ATTRIBUTES QUICK REFERENCE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ATTRIBUTE           DEFAULT           DESCRIPTION
     *  ──────────────────  ────────────────  ──────────────────────────────────────────
     *  propagation         REQUIRED          What happens when a @Transactional method
     *                                        is called — join existing tx? create new?
     *
     *  isolation           DEFAULT           Isolation level for concurrent transactions.
     *                                        Controls dirty/phantom/non-repeatable reads.
     *
     *  readOnly            false             Hint to DB: no writes in this transaction.
     *                                        Can improve performance (no dirty checking).
     *
     *  timeout             -1 (no limit)     Transaction must complete within N seconds.
     *                                        -1 = no timeout. Throws TransactionTimedOut.
     *
     *  rollbackFor         {}                Exception TYPES that trigger rollback.
     *                                        Default: RuntimeException + Error.
     *
     *  rollbackForClassName{}                Same as rollbackFor but uses class names.
     *
     *  noRollbackFor       {}                Exception types that do NOT trigger rollback.
     *                                        Overrides the default rollback behaviour.
     *
     *  noRollbackForClassName {}             Same but uses class names.
     *
     *  transactionManager  ""                The specific transaction manager bean to use.
     *                      (default TM)      Use when multiple TMs exist (e.g., JPA + JMS).
     *
     *  label               {}                Custom labels (Spring 5.3+, metadata only).
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 WHERE TO PUT @Transactional — CLASS vs METHOD:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   CLASS LEVEL (@Transactional on the class):
     *     → Applies to ALL public methods in the class.
     *     → Each method uses the class-level attributes.
     *     → Individual methods can OVERRIDE with their own @Transactional.
     *
     *   METHOD LEVEL (@Transactional on a method):
     *     → Applies ONLY to that specific method.
     *     → More precise — preferred for methods with different transaction needs.
     *
     *   BEST PRACTICE:
     *     @Transactional(readOnly = true) at class level (covers most read methods),
     *     @Transactional at method level for write operations (overrides readOnly = true).
     *
     *   @Service
     *   @Transactional(readOnly = true)     ← all methods read-only by default
     *   class OrderService {
     *
     *       public Order findById(Long id) { ... }          // readOnly = true ✓
     *       public List<Order> findAll() { ... }            // readOnly = true ✓
     *
     *       @Transactional                                   // overrides: readOnly = false
     *       public Order createOrder(OrderRequest r) { ... } // write tx ✓
     *
     *       @Transactional(timeout = 5)                     // overrides with timeout
     *       public void processOrders() { ... }
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ⚠️  CRITICAL RULES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   RULE 1: @Transactional ONLY works on PUBLIC methods.
     *           Protected, package-private, and private methods are IGNORED.
     *
     *   RULE 2: Self-invocation bypasses the proxy.
     *           Calling a @Transactional method FROM WITHIN the same class
     *           does NOT start a new transaction!
     *
     *   RULE 3: @Transactional on interfaces vs implementations.
     *           Put it on the IMPLEMENTATION class (or its methods).
     *           Spring recommends NOT putting it on interface methods.
     *
     *   RULE 4: Checked exceptions do NOT trigger rollback by default.
     *           Only RuntimeException and Error trigger automatic rollback.
     *           Use rollbackFor = Exception.class for checked exceptions.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    SECTION 5: PROPAGATION TYPES — QUICK REFERENCE                           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ┌────────────────────┬──────────────────────────────────────────────────────────┐
     * │ PROPAGATION        │ BEHAVIOUR                                                │
     * ├────────────────────┼──────────────────────────────────────────────────────────┤
     * │ REQUIRED (default) │ Join existing tx if one exists; CREATE NEW if none.      │
     * ├────────────────────┼──────────────────────────────────────────────────────────┤
     * │ REQUIRES_NEW       │ ALWAYS create a NEW independent tx.                      │
     * │                    │ Suspend existing tx until new one completes.             │
     * ├────────────────────┼──────────────────────────────────────────────────────────┤
     * │ NESTED             │ Create a savepoint in the existing tx (partial rollback).│
     * │                    │ If nested fails, rolls back to savepoint only.           │
     * │                    │ Supported by: JDBC datasource (not always JPA).          │
     * ├────────────────────┼──────────────────────────────────────────────────────────┤
     * │ SUPPORTS           │ Join existing tx if present; run WITHOUT tx if none.     │
     * ├────────────────────┼──────────────────────────────────────────────────────────┤
     * │ NOT_SUPPORTED      │ ALWAYS run without a transaction.                        │
     * │                    │ Suspends any existing tx for the duration.               │
     * ├────────────────────┼──────────────────────────────────────────────────────────┤
     * │ MANDATORY          │ MUST join an existing tx. Throws if none exists.         │
     * ├────────────────────┼──────────────────────────────────────────────────────────┤
     * │ NEVER              │ MUST run without tx. Throws if one exists.               │
     * └────────────────────┴──────────────────────────────────────────────────────────┘
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    SECTION 6: ISOLATION LEVELS — QUICK REFERENCE                            ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * CONCURRENCY PROBLEMS (what isolation solves):
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  DIRTY READ:          Tx A reads data that Tx B has written but NOT YET committed.
     *                       If Tx B rolls back, Tx A has read non-existent data.
     *
     *  NON-REPEATABLE READ: Tx A reads row X. Tx B updates row X and commits.
     *                       Tx A reads row X again — gets DIFFERENT value!
     *
     *  PHANTOM READ:        Tx A reads rows matching WHERE clause (e.g., balance > 100).
     *                       Tx B INSERTS a new row matching the same clause.
     *                       Tx A re-reads — sees a NEW "phantom" row!
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ISOLATION LEVELS vs PROBLEMS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ┌────────────────────┬────────────┬──────────────────┬─────────────┬──────────┐
     *  │ ISOLATION LEVEL    │ Dirty Read │ Non-repeatable   │ Phantom     │ Perf     │
     *  │                    │ prevented? │ Read prevented?  │ Read prev.? │          │
     *  ├────────────────────┼────────────┼──────────────────┼─────────────┼──────────┤
     *  │ READ_UNCOMMITTED   │     ✗      │        ✗         │      ✗      │ FASTEST  │
     *  ├────────────────────┼────────────┼──────────────────┼─────────────┼──────────┤
     *  │ READ_COMMITTED     │     ✓      │        ✗         │      ✗      │ Fast     │
     *  │ (most DB default)  │            │                  │             │          │
     *  ├────────────────────┼────────────┼──────────────────┼─────────────┼──────────┤
     *  │ REPEATABLE_READ    │     ✓      │        ✓         │      ✗      │ Moderate │
     *  │ (MySQL default)    │            │                  │             │          │
     *  ├────────────────────┼────────────┼──────────────────┼─────────────┼──────────┤
     *  │ SERIALIZABLE       │     ✓      │        ✓         │      ✓      │ SLOWEST  │
     *  └────────────────────┴────────────┴──────────────────┴─────────────┴──────────┘
     *
     *  DEFAULT → uses the database's default isolation level.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    SECTION 10: COMMON PITFALLS & GOTCHAS                                    ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * PITFALL 1: SELF-INVOCATION (most common mistake)
     * ────────────────────────────────────────────────
     *
     *   @Service class OrderService {
     *
     *       public void processOrder(Order order) {
     *           this.saveOrder(order);   // ← PROXY BYPASSED! @Transactional on saveOrder IGNORED!
     *       }
     *
     *       @Transactional
     *       public void saveOrder(Order order) { ... }   // ← Never starts a transaction here!
     *   }
     *
     *   FIX OPTIONS:
     *   a) Move saveOrder() to a separate @Service class
     *   b) Inject the bean into itself: @Autowired OrderService self; self.saveOrder()
     *   c) Use @EnableTransactionManagement(mode = ASPECTJ) — loads AspectJ weaving
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * PITFALL 2: @Transactional on PRIVATE methods
     *
     *   @Transactional
     *   private void doSomething() { ... }   // ← COMPLETELY IGNORED by Spring proxy!
     *
     *   FIX: Make the method public (or protected for class-based proxies).
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * PITFALL 3: Checked exceptions DON'T rollback by default
     *
     *   @Transactional
     *   public void transfer() throws InsufficientFundsException {
     *       // If InsufficientFundsException is thrown, NO ROLLBACK happens!
     *       // Because it's a CHECKED exception.
     *   }
     *
     *   FIX: @Transactional(rollbackFor = InsufficientFundsException.class)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * PITFALL 4: Catching the exception prevents rollback
     *
     *   @Transactional
     *   public void transfer() {
     *       try {
     *           doTransfer();
     *       } catch (RuntimeException e) {
     *           log.error("Transfer failed", e);
     *           // ← Exception swallowed! Spring doesn't know about it → NO ROLLBACK!
     *       }
     *   }
     *
     *   FIX: Either don't catch, or rethrow, or call TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * PITFALL 5: LazyInitializationException — accessing lazy entities outside @Transactional
     *
     *   @Service class OrderService {
     *       Order order = repository.findById(1L).get();  // ← Transaction ends here
     *       order.getItems();  // ← LazyInitializationException! Session is closed.
     *   }
     *
     *   FIX: Access lazy collections WITHIN @Transactional, or use EAGER fetching,
     *        or use @Transactional on the calling layer, or use JOIN FETCH in query.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * PITFALL 6: REQUIRES_NEW creates truly independent transaction — rollback is SEPARATE
     *
     *   Outer tx fails → rolls back outer changes.
     *   Inner REQUIRES_NEW tx already COMMITTED → its changes PERSIST!
     *   This is often intentional (audit log must persist even if main tx fails).
     *   Make sure you WANT this behaviour before using REQUIRES_NEW.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    SECTION 13: INTERVIEW QUESTIONS & ANSWERS                                 ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Q1: What is the default propagation of @Transactional?
     *     → REQUIRED. It joins an existing transaction if one exists, or creates a new
     *       one if none exists.
     *
     * Q2: What is the difference between REQUIRED and REQUIRES_NEW?
     *     → REQUIRED: joins existing tx or creates new one. Shares the tx with caller.
     *     → REQUIRES_NEW: ALWAYS creates a completely new, independent tx. Suspends
     *       the outer tx. The new tx commits/rolls back independently.
     *
     * Q3: Which exceptions trigger rollback by default in @Transactional?
     *     → RuntimeException (and its subclasses) and Error.
     *       Checked exceptions (IOException, SQLException, etc.) do NOT trigger rollback
     *       unless explicitly specified via rollbackFor.
     *
     * Q4: What is the difference between @Transactional(readOnly=true) and no @Transactional?
     *     → Without @Transactional: no tx context, each JPA operation gets its own tx.
     *     → @Transactional(readOnly=true): opens a tx with READ-ONLY hint:
     *       - Hibernate skips dirty checking (faster)
     *       - Some DBs optimize query plans for read-only tx
     *       - Ensures all reads in the method use the SAME snapshot (consistent reads)
     *
     * Q5: What is NESTED propagation and how does it differ from REQUIRES_NEW?
     *     → REQUIRES_NEW: creates a completely separate, independent transaction.
     *       If outer fails, inner was already committed — it stays committed.
     *     → NESTED: creates a SAVEPOINT in the current transaction.
     *       If nested part fails, only rolls back to the savepoint (partial rollback).
     *       If outer fails, ENTIRE transaction (including nested part) rolls back.
     *       Nested is part of the same tx; REQUIRES_NEW is a different tx.
     *
     * Q6: Why does self-invocation bypass @Transactional?
     *     → Spring transaction management works via AOP PROXY. When you call
     *       this.someMethod(), you bypass the proxy and call the real object directly.
     *       The proxy's transaction interceptor never runs, so no transaction starts.
     *       Fix: inject the bean into itself, or move the method to another service.
     *
     * Q7: What is the difference between isolation levels READ_COMMITTED and REPEATABLE_READ?
     *     → READ_COMMITTED: you see only committed data. But re-reading the same row
     *       in the same tx may return different values (non-repeatable read allowed).
     *     → REPEATABLE_READ: once you read a row in a tx, subsequent reads of the same
     *       row within that tx always return the same value (locked for reading).
     *       But new rows can appear (phantom reads allowed, except in MySQL with InnoDB).
     *
     * Q8: What is a dirty read?
     *     → Reading data that another transaction has modified but NOT YET committed.
     *       If that other tx rolls back, you've read data that never actually existed.
     *       Prevented by: READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE.
     *
     * Q9: When would you use MANDATORY propagation?
     *     → When a method MUST always be called within an existing transaction.
     *       If it's called without a tx, it throws an exception immediately.
     *       Use case: a helper method that should never be used standalone —
     *       enforces that the caller provides the transactional context.
     *
     * Q10: What is @TransactionalEventListener?
     *     → Binds an event listener to a transaction lifecycle phase.
     *       AFTER_COMMIT (default): listener runs only when the tx commits successfully.
     *       AFTER_ROLLBACK: listener runs only when the tx rolls back.
     *       BEFORE_COMMIT: runs before the tx commits.
     *       AFTER_COMPLETION: runs after tx completes (both commit and rollback).
     *       Use case: send email ONLY when order is successfully saved to DB.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                   ║");
        System.out.println("║       SPRING BOOT ANNOTATIONS — CHAPTER 6 OVERVIEW               ║");
        System.out.println("║           Spring Transaction Management Annotations               ║");
        System.out.println("║                                                                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  SECTION  1:  Why transactions? The bank transfer problem");
        System.out.println("  SECTION  2:  ACID properties (Atomicity, Consistency, Isolation, Durability)");
        System.out.println("  SECTION  3:  @EnableTransactionManagement");
        System.out.println("  SECTION  4:  @Transactional — all 9 attributes");
        System.out.println("  SECTION  5:  7 Propagation types");
        System.out.println("  SECTION  6:  5 Isolation levels + concurrency problems");
        System.out.println("  SECTION  7:  rollbackFor / noRollbackFor");
        System.out.println("  SECTION  8:  readOnly transactions");
        System.out.println("  SECTION  9:  timeout");
        System.out.println("  SECTION 10:  6 Common pitfalls (self-invocation, private methods...)");
        System.out.println("  SECTION 13:  10 Interview Q&A");
        System.out.println();
        System.out.println("  Domain: BankAccount + TransactionLog (classic transfer use case)");
        System.out.println();
        System.out.println("  Files: Example01 (basics) → Example02 (propagation) →");
        System.out.println("         Example03 (isolation) → Example04 (rollback/advanced) →");
        System.out.println("         HowItWorksExplained");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

