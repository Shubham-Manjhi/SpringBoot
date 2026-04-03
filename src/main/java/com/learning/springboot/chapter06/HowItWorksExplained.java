package com.learning.springboot.chapter06;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   HOW IT WORKS: SPRING TRANSACTION MANAGEMENT — INTERNAL MECHANICS                  ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        HowItWorksExplained.java
 * Purpose:     Deep dive into Spring's transaction infrastructure internals:
 *               - AOP proxy creation and interception
 *               - TransactionInterceptor execution flow
 *               - TransactionSynchronizationManager (the registry)
 *               - PlatformTransactionManager hierarchy
 *               - How propagation works under the hood
 *               - How rollback is triggered
 *               - Connection management
 * Difficulty:  ⭐⭐⭐⭐⭐ Advanced
 * Time:        60–90 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
public class HowItWorksExplained {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║                                                                           ║
     * ║    STAGE 1: STARTUP — PROXY CREATION                                    ║
     * ║                                                                           ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * WHEN SPRING STARTS UP:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * 1. @EnableTransactionManagement registers:
     *    → BeanFactoryTransactionAttributeSourceAdvisor (AOP advisor)
     *    → TransactionInterceptor (the advice/interceptor)
     *    → AnnotationTransactionAttributeSource (reads @Transactional metadata)
     *
     * 2. Spring's post-processing infrastructure (AbstractAutoProxyCreator) checks
     *    every bean during creation. For each bean, it checks:
     *    "Does this bean have any @Transactional annotations on it or its methods?"
     *
     * 3. If YES → Spring wraps the bean in a PROXY:
     *    → If bean implements interfaces (and proxyTargetClass=false):
     *        JDK Dynamic Proxy (implements the same interfaces)
     *    → If bean has no interfaces OR proxyTargetClass=true:
     *        CGLIB Proxy (subclass of the bean's class)
     *
     * 4. The proxy is registered in the Spring context INSTEAD of the real bean.
     *    Any other bean that @Autowired the original bean → gets the PROXY.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * PROXY STRUCTURE:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *  @Autowired BankAccountService service;
     *  // 'service' is actually a CGLIB proxy!
     *  // service.getClass() → "com.learning.springboot.chapter06.BankAccountService$$SpringCGLIB$$0"
     *
     *  WHEN YOU CALL:  service.simpleTransfer("ACC001", "ACC002", 500)
     *
     *  You're actually calling:
     *  ┌─────────────────────────────────────────────────────────────────────────┐
     *  │  CglibAopProxy.intercept()                                              │
     *  │    ↓                                                                    │
     *  │  AdvisedSupport.getInterceptorsAndDynamicInterceptionAdvice()           │
     *  │    ↓                                                                    │
     *  │  TransactionInterceptor.invoke()  ← THE CORE TRANSACTION LOGIC         │
     *  │    ↓                                                                    │
     *  │  BankAccountService.simpleTransfer() ← YOUR REAL METHOD                │
     *  └─────────────────────────────────────────────────────────────────────────┘
     *
     * ═══════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║                                                                           ║
     * ║    STAGE 2: TRANSACTIONINTERCEPTOR — THE HEART OF @Transactional         ║
     * ║                                                                           ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * COMPLETE EXECUTION FLOW of TransactionInterceptor.invoke():
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *  1. READ TRANSACTION ATTRIBUTES:
     *     transactionAttributeSource.getTransactionAttribute(method, targetClass)
     *     → Reads @Transactional annotation from method or class
     *     → Returns: TransactionAttribute {propagation, isolation, readOnly,
     *                                      timeout, rollbackRules}
     *     → Caches the result (parsed only ONCE per method, reused forever)
     *
     *  2. GET THE TRANSACTION MANAGER:
     *     determineTransactionManager(transactionAttribute)
     *     → If transactionManager="" (default): uses default bean of type
     *       PlatformTransactionManager from the context
     *     → For JPA apps: JpaTransactionManager (wraps EntityManager)
     *     → For JDBC: DataSourceTransactionManager
     *
     *  3. CREATE TRANSACTION INFO:
     *     createTransactionIfNecessary(transactionManager, txAttr, joinpointIdentification)
     *     → Based on propagation, decides what to do:
     *        REQUIRED:
     *          If no existing tx: transactionManager.getTransaction() → BEGIN
     *          If existing tx: joinExistingTransaction() (reuse the existing tx)
     *        REQUIRES_NEW:
     *          transactionManager.getTransaction(REQUIRES_NEW)
     *          → suspendTransaction(existingTx) → saves existing tx state
     *          → BEGIN new transaction
     *          → returns SuspendedResourcesHolder with old tx
     *        NESTED:
     *          Creates a SAVEPOINT: connection.setSavepoint("SAVEPOINT_X")
     *     → Result stored in TransactionInfo (thread-local)
     *
     *  4. INVOKE ACTUAL METHOD:
     *     invocation.proceedWithInvocation()
     *     → Your actual method runs here (the real BankAccountService code)
     *
     *  5a. ON SUCCESS (no exception thrown):
     *     commitTransactionAfterReturning(txInfo)
     *     → transactionManager.commit(txStatus)
     *     → For JPA: entityManager.flush() (write pending changes to DB)
     *     → Calls COMMIT on the JDBC connection
     *     → Fires TransactionSynchronization.afterCommit() callbacks
     *     → Releases DB connection back to pool
     *
     *  5b. ON EXCEPTION:
     *     completeTransactionAfterThrowing(txInfo, exception)
     *     → rollbackRules.rollbackOn(exception)?
     *        YES (RuntimeException by default): transactionManager.rollback(txStatus)
     *          → ROLLBACK sent to DB
     *          → Fires TransactionSynchronization.afterRollback() callbacks
     *        NO (Checked exception by default): transactionManager.commit(txStatus)
     *          → COMMIT (transaction committed despite exception!)
     *     → Exception propagates to caller
     *
     *  6. CLEANUP:
     *     cleanupTransactionInfo(txInfo)
     *     → Remove TransactionInfo from thread-local stack
     *     → If REQUIRES_NEW: resume suspended outer transaction
     *
     * ═══════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║                                                                           ║
     * ║    STAGE 3: TransactionSynchronizationManager — THE REGISTRY             ║
     * ║                                                                           ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * TransactionSynchronizationManager is Spring's central ThreadLocal registry
     * for the current transaction state.
     *
     * IT STORES (all in ThreadLocal — one value per thread):
     *
     *   resources:                Map<Object, Object>
     *     → Stores: DataSource → ConnectionHolder (the current DB connection)
     *     → Stores: EntityManagerFactory → EntityManagerHolder (the JPA session)
     *     → Used for connection binding — ensures same connection is used
     *       throughout the entire transaction
     *
     *   synchronizations:         Set<TransactionSynchronization>
     *     → Callbacks registered to fire on commit/rollback
     *     → Used by @TransactionalEventListener, JPA flush, cache eviction, etc.
     *
     *   currentTransactionName:   String
     *     → "com.example.MyService.myMethod" — for debugging/logging
     *
     *   currentTransactionReadOnly: boolean
     *     → Whether the current tx is read-only
     *
     *   currentTransactionIsolationLevel: Integer
     *     → The isolation level (for JdbcTemplate auto-configuration)
     *
     *   actualTransactionActive:  boolean
     *     → Is a real database transaction currently active?
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * HOW CONNECTION REUSE WORKS:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * Without transactions:
     *   Every JPA/JDBC operation: get connection from pool → execute → return connection
     *   Each operation uses a DIFFERENT connection!
     *
     * With @Transactional:
     *   Transaction START:
     *     1. Get connection from pool
     *     2. Register: resources[DataSource] = ConnectionHolder(connection)
     *     3. Set connection.setAutoCommit(false)
     *
     *   Every JPA/JDBC operation within the transaction:
     *     1. Check resources[DataSource] → finds ConnectionHolder
     *     2. Reuse the SAME connection (not a new one from pool!)
     *     3. All SQL runs on the SAME connection → part of the SAME transaction
     *
     *   Transaction END (commit/rollback):
     *     1. connection.commit() or connection.rollback()
     *     2. connection.setAutoCommit(true) (restore state)
     *     3. resources.remove(DataSource) → clean up ThreadLocal
     *     4. Return connection to pool
     *
     * THIS IS HOW ATOMICITY IS ACHIEVED:
     * Multiple SQL statements → same JDBC connection → same database transaction!
     *
     * ═══════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║                                                                           ║
     * ║    STAGE 4: PlatformTransactionManager HIERARCHY                         ║
     * ║                                                                           ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * Spring abstracts transaction management through PlatformTransactionManager.
     * This allows @Transactional to work with ANY transactional resource.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * HIERARCHY:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     *  PlatformTransactionManager (interface)
     *    │   getTransaction(TransactionDefinition) → TransactionStatus
     *    │   commit(TransactionStatus)
     *    │   rollback(TransactionStatus)
     *    │
     *    ├── AbstractPlatformTransactionManager (base class with common logic)
     *    │     │  Handles: REQUIRED joining, REQUIRES_NEW suspension, NESTED savepoints
     *    │     │  Propagation logic is HERE (not in each subclass)
     *    │     │
     *    │     ├── DataSourceTransactionManager
     *    │     │     Used for: plain JDBC, JdbcTemplate
     *    │     │     Manages: java.sql.Connection directly
     *    │     │
     *    │     ├── JpaTransactionManager ← USED IN OUR PROJECT
     *    │     │     Used for: Spring Data JPA, Hibernate
     *    │     │     Manages: EntityManager, binds to DataSource too
     *    │     │     Spring Boot auto-configures this when JPA is on classpath
     *    │     │
     *    │     └── HibernateTransactionManager
     *    │           Used for: legacy Hibernate Session (SessionFactory-based)
     *    │
     *    └── JtaTransactionManager
     *          Used for: distributed transactions (XA), multiple resources
     *          Manages: javax.transaction.UserTransaction
     *          Used with: application servers (WebLogic, JBoss)
     *          Supports: NESTED? No. REQUIRES_NEW? Yes.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     * JpaTransactionManager SPECIFICALLY:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * JpaTransactionManager.doBegin():
     *   1. Get EntityManager from EntityManagerFactory
     *   2. Get JDBC connection from EntityManager (unwrap ConnectionHolder)
     *   3. Set connection.setAutoCommit(false)
     *   4. Set isolation level on connection
     *   5. Bind EntityManager to TransactionSynchronizationManager:
     *      resources[entityManagerFactory] = EntityManagerHolder(entityManager)
     *   6. Bind DataSource connection:
     *      resources[dataSource] = ConnectionHolder(connection)
     *
     * JpaTransactionManager.doCommit():
     *   1. entityManager.flush() ← writes all pending Hibernate SQL to DB
     *   2. connection.commit()   ← COMMIT to database
     *
     * JpaTransactionManager.doRollback():
     *   1. entityManager.clear() ← discard pending changes (no flush)
     *   2. connection.rollback() ← ROLLBACK to database
     *
     * ═══════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║                                                                           ║
     * ║    STAGE 5: PROPAGATION INTERNALS                                        ║
     * ║                                                                           ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * HOW REQUIRED WORKS INTERNALLY:
     * ────────────────────────────────
     *
     *   AbstractPlatformTransactionManager.getTransaction(REQUIRED):
     *     1. Check TransactionSynchronizationManager.resources:
     *        "Is there an existing transaction?"
     *     2a. IF YES (active tx):
     *         → handleExistingTransaction(REQUIRED, txObject)
     *         → For REQUIRED: call TransactionStatus.createSavepoint? NO
     *           Just return a "participating" status (shares existing tx)
     *         → synchronizationManager.bindResource() not called again
     *         → The SAME ConnectionHolder is reused
     *     2b. IF NO (no active tx):
     *         → doBegin(): get new connection, BEGIN tx, bind to ThreadLocal
     *         → Return a new TransactionStatus (owner of the tx)
     *
     * HOW REQUIRES_NEW WORKS INTERNALLY:
     * ─────────────────────────────────────
     *
     *   AbstractPlatformTransactionManager.getTransaction(REQUIRES_NEW):
     *     1. Check for existing transaction
     *     2a. IF YES (active tx):
     *         → suspend(existingTx):
     *           a. Unbind all resources from ThreadLocal
     *              (DataSource → connection, EMF → entityManager)
     *           b. Store the unbound resources in SuspendedResourcesHolder
     *           c. Clear all synchronizations
     *         → doBegin() with NEW connection: start a fresh transaction
     *         → Return TransactionStatus with suspendedResources stored
     *     2b. IF NO: just doBegin() as normal
     *
     *     ON COMPLETION:
     *         → Commit/rollback the new inner tx
     *         → resume(suspendedResourcesHolder):
     *           a. Re-bind old resources to ThreadLocal
     *           b. Re-register old synchronizations
     *         → Outer tx is active again
     *
     * HOW NESTED WORKS INTERNALLY:
     * ──────────────────────────────
     *
     *   AbstractPlatformTransactionManager.getTransaction(NESTED):
     *     1. IF existing tx AND nestedTransactionAllowed=true:
     *        → Create a JDBC savepoint:
     *          connection.setSavepoint("SAVEPOINT_" + savepointCounter++)
     *        → Return DefaultTransactionStatus with savepoint set
     *     2. IF no existing tx: behaves like REQUIRED (new tx)
     *
     *     ON ROLLBACK:
     *        → connection.rollback(savepoint)  ← NOT a full rollback!
     *        → connection.releaseSavepoint(savepoint)
     *
     *     ON COMMIT:
     *        → connection.releaseSavepoint(savepoint)  ← savepoint released
     *        → outer tx continues; will commit when IT completes
     *
     * ═══════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║                                                                           ║
     * ║    STAGE 6: @TransactionalEventListener                                  ║
     * ║                                                                           ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * @TransactionalEventListener links event handling to TRANSACTION LIFECYCLE.
     *
     * THE PROBLEM WITHOUT IT:
     * ─────────────────────────
     *
     *   @Service class OrderService {
     *       @Transactional
     *       public Order createOrder(OrderRequest req) {
     *           Order order = orderRepository.save(req.toOrder());
     *
     *           eventPublisher.publishEvent(new OrderCreatedEvent(order));
     *           // ↑ @EventListener immediately calls EmailService.sendConfirmation()
     *           // The EMAIL is sent NOW — even though the transaction hasn't committed yet!
     *           // What if the transaction ROLLS BACK after this?
     *           // → Customer received email for an order that doesn't exist! BUG!
     *
     *           // ... more processing that might fail ...
     *           return order;
     *       }
     *   }
     *
     * THE SOLUTION:
     * ──────────────
     *
     *   @Service class EmailNotificationService {
     *
     *       @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
     *       public void onOrderCreated(OrderCreatedEvent event) {
     *           // This ONLY runs if the transaction COMMITTED successfully!
     *           // If the transaction rolled back, this method is NEVER called.
     *           emailSender.send(event.getOrder().getCustomerEmail(), "Order Confirmed!");
     *       }
     *   }
     *
     * PHASES:
     *   AFTER_COMMIT (default): runs only after successful COMMIT
     *   AFTER_ROLLBACK:         runs only after ROLLBACK
     *   AFTER_COMPLETION:       runs after both COMMIT and ROLLBACK
     *   BEFORE_COMMIT:          runs just before COMMIT
     *
     * HOW IT WORKS INTERNALLY:
     *   @TransactionalEventListener registers a TransactionSynchronization adapter.
     *   The adapter's afterCommit() / afterRollback() method calls the listener.
     *   The registration happens when publishEvent() is called (within the tx).
     *   The actual listener execution is deferred to the appropriate phase.
     *
     * ═══════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║                                                                           ║
     * ║    STAGE 7: COMPLETE REQUEST TIMELINE                                    ║
     * ║                                                                           ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     * FULL TIMELINE for: bankAccountService.simpleTransfer("ACC001","ACC002", $500)
     * (assumes @SpringBootTest / running application)
     *
     * HTTP Request arrives
     *   ↓
     * DispatcherServlet routes to controller
     *   ↓
     * Controller calls bankAccountService.simpleTransfer(...)
     *   ↓
     * CglibAopProxy intercepts the call
     *   ↓
     * TransactionInterceptor.invoke() starts
     *   ↓
     * JpaTransactionManager.getTransaction(REQUIRED):
     *   • Get EntityManager from EntityManagerFactory pool
     *   • Unwrap JDBC connection from EntityManager
     *   • connection.setAutoCommit(false)
     *   • connection.setTransactionIsolation(READ_COMMITTED)  // default
     *   • Bind to ThreadLocal: resources[emf] = emHolder, resources[ds] = connHolder
     *   ↓
     * [YOUR CODE RUNS]
     *   • accountRepository.findByAccountNumber("ACC001")
     *     → EntityManager.find() → SQL: SELECT * FROM ch06_bank_accounts WHERE account_number=?
     *     → Uses SAME connection from ThreadLocal (bound above)
     *   • accountRepository.findByAccountNumber("ACC002")
     *     → Same connection → same transaction
     *   • from.debit(500) → in-memory change (Hibernate first-level cache)
     *   • accountRepository.save(from) → EntityManager.persist/merge()
     *     → Hibernate queues: UPDATE ch06_bank_accounts SET balance=500 WHERE id=1
     *     → NOT sent to DB yet (queued in Hibernate write-behind)
     *   • to.credit(500) → in-memory change
     *   • accountRepository.save(to)
     *     → Hibernate queues: UPDATE ch06_bank_accounts SET balance=700 WHERE id=2
     *   ↓
     * [METHOD RETURNS NORMALLY — no exception]
     *   ↓
     * TransactionInterceptor.commitTransactionAfterReturning():
     *   • JpaTransactionManager.commit():
     *     1. EntityManager.flush():
     *        → Sends queued SQL to DB (on same connection, within transaction):
     *          UPDATE ch06_bank_accounts SET balance=500 WHERE id=1
     *          UPDATE ch06_bank_accounts SET balance=700 WHERE id=2
     *        → SQL executed but NOT committed to DB yet
     *     2. connection.commit():
     *        → Database COMMITS both UPDATEs atomically
     *        → Both changes become durable and visible to other transactions
     *   • connection.setAutoCommit(true)  // restore
     *   • resources.clear() // unbind from ThreadLocal
     *   • connection returned to pool
     *   ↓
     * TransactionInterceptor returns to CglibAopProxy
     *   ↓
     * Result returned to controller
     *   ↓
     * HTTP Response sent
     *
     * ═══════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║                                                                           ║
     * ║    STAGE 8: PRODUCTION TRANSACTION CHECKLIST                             ║
     * ║                                                                           ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     *
     *  ✅ USE THESE PATTERNS:
     *
     *   1.  @Transactional(readOnly = true) at service CLASS level
     *       @Transactional at specific write METHODS (overrides class-level)
     *
     *   2.  @Transactional(rollbackFor = Exception.class) on ALL write methods
     *       to ensure checked exceptions also trigger rollback
     *
     *   3.  REQUIRES_NEW for audit logging / side effects that must persist
     *       even when the main operation fails
     *
     *   4.  Set timeout on transactions that could run long
     *
     *   5.  Keep transactions SHORT — move external calls outside @Transactional
     *
     *   6.  Use @TransactionalEventListener instead of @EventListener for
     *       events that should only fire after successful commit
     *
     *  ❌ AVOID THESE ANTI-PATTERNS:
     *
     *   1.  self-invocation: this.method() where method has @Transactional
     *
     *   2.  @Transactional on private/protected methods (silently ignored)
     *
     *   3.  Catching exceptions and swallowing them inside @Transactional
     *       (use setRollbackOnly() if you must catch)
     *
     *   4.  External API calls (REST, SMTP, messaging) inside @Transactional
     *
     *   5.  Long-running transactions without timeout
     *
     *   6.  @Transactional on @Controller methods
     *       (controllers should delegate to service layer for tx management)
     *
     *   7.  Using REQUIRES_NEW in loops with many iterations
     *       (connection pool exhaustion risk)
     *
     *   8.  Forgetting that LazyInitializationException occurs when accessing
     *       lazy collections OUTSIDE of a @Transactional context
     *
     * ═══════════════════════════════════════════════════════════════════════════
     */

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║   CHAPTER 6 — HOW IT WORKS: Transaction Internal Mechanics        ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  STAGE 1: Startup → CGLIB proxy created around @Transactional beans");
        System.out.println("  STAGE 2: TransactionInterceptor.invoke() — the 6-step flow");
        System.out.println("           Read attrs → Get TM → Begin → Method → Commit/Rollback → Cleanup");
        System.out.println("  STAGE 3: TransactionSynchronizationManager ThreadLocal registry");
        System.out.println("           DataSource → ConnectionHolder (same conn = same tx)");
        System.out.println("  STAGE 4: PlatformTransactionManager hierarchy");
        System.out.println("           JpaTransactionManager → EntityManager + JDBC connection");
        System.out.println("  STAGE 5: Propagation internals — REQUIRED/REQUIRES_NEW/NESTED");
        System.out.println("  STAGE 6: @TransactionalEventListener — fire AFTER commit only");
        System.out.println("  STAGE 7: Complete timeline of simpleTransfer() call");
        System.out.println("  STAGE 8: Production checklist (8 do's, 8 don'ts)");
        System.out.println();
        System.out.println("  KEY INSIGHT: @Transactional works because ALL JPA/JDBC ops in");
        System.out.println("  the same transaction use the SAME connection (via ThreadLocal).");
        System.out.println("  Same connection = same database transaction = atomicity!");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

