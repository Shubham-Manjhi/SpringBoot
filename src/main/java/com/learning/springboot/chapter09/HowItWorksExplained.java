package com.learning.springboot.chapter09;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║       HOW SPRING AOP WORKS INTERNALLY — THE COMPLETE DEEP DIVE                      ║
 * ║       Proxy Types · Weaving · AnnotationAwareAutoProxyCreator                       ║
 * ║       Advice Chain · Self-Invocation · Order · AspectJ vs Spring AOP               ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        HowItWorksExplained.java
 * Purpose:     Understand how Spring AOP works under the hood. Trace the complete
 *              lifecycle from @EnableAspectJAutoProxy to an advice chain executing.
 * Difficulty:  ⭐⭐⭐⭐⭐ Advanced
 * Time:        40 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║       HOW SPRING AOP WORKS — THE COMPLETE INTERNALS GUIDE                   ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
public class HowItWorksExplained {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 1: THREE TYPES OF AOP WEAVING                                  ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * "Weaving" = linking aspect code with application code.
     * There are three approaches, each with different tradeoffs:
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 1. COMPILE-TIME WEAVING (AspectJ)
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * The AspectJ COMPILER (ajc) transforms bytecode at BUILD TIME.
     * Advice code is physically WOVEN INTO the .class files.
     *
     *   Employee.java → [ajc compiler] → Employee.class (contains aspect code!)
     *
     * Pros:
     *   ✅ Best performance (no proxy overhead at runtime)
     *   ✅ Can advise private methods, constructors, field access
     *   ✅ Works with any object — not just Spring beans
     *
     * Cons:
     *   ❌ Requires AspectJ compiler in build pipeline
     *   ❌ Complex build setup
     *   ❌ IDE support can be tricky
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 2. LOAD-TIME WEAVING (AspectJ Agent)
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * A Java Agent transforms bytecode as classes are loaded by the JVM.
     * Start the JVM with: -javaagent:aspectjweaver.jar
     *
     * Pros:
     *   ✅ No need to change build process
     *   ✅ Can advise private methods and constructors
     *   ✅ Works with non-Spring classes
     *
     * Cons:
     *   ❌ Requires JVM agent argument
     *   ❌ Slower startup time
     *   ❌ More complex deployment
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 3. RUNTIME WEAVING (Spring AOP — Proxy-Based) — DEFAULT IN SPRING BOOT
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Spring creates PROXY OBJECTS at runtime that wrap the real beans.
     * When you @Autowire a service, you get the PROXY, not the real object.
     * The proxy intercepts method calls and applies advice.
     *
     * Pros:
     *   ✅ No build changes required
     *   ✅ Simple setup — just add @EnableAspectJAutoProxy
     *   ✅ Integrated with Spring dependency injection
     *   ✅ Zero JVM startup overhead
     *
     * Cons:
     *   ❌ Only works for Spring-managed beans
     *   ❌ Can only intercept PUBLIC methods
     *   ❌ Self-invocation (this.method()) bypasses proxy
     *   ❌ Cannot advise constructors or field access
     *
     * → 99% of Spring applications use RUNTIME WEAVING. This is what this chapter covers.
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 2: TWO PROXY TYPES — JDK Dynamic vs CGLIB                      ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Spring uses two kinds of proxies. Understanding when each is used matters
     * for debugging and architecture decisions.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * JDK DYNAMIC PROXY
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * REQUIREMENT: Target class must implement at LEAST one interface.
     *
     * HOW IT WORKS:
     *   1. Java's java.lang.reflect.Proxy.newProxyInstance() creates a proxy class
     *      at runtime that IMPLEMENTS the same interface(s) as the target.
     *   2. Every method call on the proxy is routed through an InvocationHandler.
     *   3. InvocationHandler runs advice then calls the real method via reflection.
     *
     *   interface EmployeeRepository {        }
     *   class EmployeeRepositoryImpl implements EmployeeRepository { ... }
     *
     *   // Spring creates:
     *   class $Proxy42 implements EmployeeRepository {    // generated at runtime
     *       public Employee findById(Long id) {
     *           handler.invoke(this, findById, new Object[]{id});
     *       }
     *   }
     *
     *   When you @Autowire EmployeeRepository → you get $Proxy42 (not EmployeeRepositoryImpl).
     *
     * WHEN USED:
     *   → proxyTargetClass = false (non-default in Spring Boot)
     *   → Target implements at least one interface
     *
     * LIMITATIONS:
     *   ❌ Can only proxy through the interface — methods NOT in the interface are NOT proxied
     *   ❌ Slightly slower (uses Java reflection for every method call)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * CGLIB PROXY — DEFAULT IN SPRING BOOT
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * REQUIREMENT: Target class must NOT be final. Methods must NOT be final.
     *
     * HOW IT WORKS:
     *   1. CGLIB (Code Generation Library) generates a SUBCLASS of the target at runtime.
     *   2. The subclass OVERRIDES all non-final public/protected methods.
     *   3. Overridden methods run advice then call super.method() (the real method).
     *
     *   @Service
     *   class EmployeeService {
     *       public Employee createEmployee(...) { ... }
     *   }
     *
     *   // CGLIB generates at runtime:
     *   class EmployeeService$$SpringCGLIB$$0 extends EmployeeService {
     *       @Override
     *       public Employee createEmployee(...) {
     *           // Run @Before advice
     *           Employee result = super.createEmployee(...);  // real method
     *           // Run @AfterReturning advice
     *           return result;
     *       }
     *   }
     *
     *   When you @Autowire EmployeeService → you get EmployeeService$$SpringCGLIB$$0.
     *
     * WHEN USED:
     *   → proxyTargetClass = true (DEFAULT in Spring Boot since Spring Boot 2.x)
     *   → OR when target class doesn't implement any interface
     *
     * LIMITATIONS:
     *   ❌ Cannot proxy FINAL classes (CGLIB can't subclass them)
     *   ❌ Cannot proxy FINAL methods (CGLIB can't override them)
     *   ❌ Requires a no-arg constructor (or use objenesis to bypass this)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * WHY SPRING BOOT DEFAULTS TO CGLIB (proxyTargetClass=true):
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Spring Boot 2+ changed the default to proxyTargetClass=true because:
     *   1. Most @Service and @Repository classes don't implement interfaces.
     *   2. JDK proxy only works through the interface — methods not in the interface
     *      can't be intercepted (silently bypassed).
     *   3. CGLIB works for ALL non-final public methods, regardless of interfaces.
     *
     * Only use JDK Dynamic Proxy (proxyTargetClass=false) when:
     *   → You have strict interface-based architecture
     *   → Final classes that need to be proxied
     *   → Performance is critical and all proxied classes implement interfaces
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * HOW TO CHECK IF A BEAN IS A PROXY IN CODE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   import org.springframework.aop.support.AopUtils;
     *
     *   boolean isProxy     = AopUtils.isAopProxy(employeeService);    // true
     *   boolean isCglib     = AopUtils.isCglibProxy(employeeService);  // true (Spring Boot)
     *   boolean isJdkProxy  = AopUtils.isJdkDynamicProxy(employeeService); // false
     *
     *   // Get the real object behind the proxy:
     *   Object realBean = AopUtils.getTargetClass(employeeService);
     *   // → class com.learning.springboot.chapter09.EmployeeService (not the $$CGLIB class)
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 3: @EnableAspectJAutoProxy INTERNALS                           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * When you add @EnableAspectJAutoProxy to a @Configuration class:
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * STEP 1: Register AnnotationAwareAspectJAutoProxyCreator
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @EnableAspectJAutoProxy registers:
     *   AnnotationAwareAspectJAutoProxyCreator
     *
     * This is a BEAN POST-PROCESSOR — a special Spring hook that runs after every bean
     * is created, allowing it to REPLACE the bean with a proxy if needed.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * STEP 2: For each @Service/@Repository bean, the proxy creator runs
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   1. Spring creates the real EmployeeService instance
     *   2. BeanPostProcessor.postProcessAfterInitialization(employeeService) is called
     *   3. AnnotationAwareAspectJAutoProxyCreator checks: "Should this bean be proxied?"
     *      → Find all @Aspect beans in the context
     *      → For each aspect, check if any of its pointcuts match EmployeeService's methods
     *      → If ANY pointcut matches → create a proxy
     *   4. If proxy needed:
     *      → Create CGLIB subclass of EmployeeService (or JDK proxy if configured)
     *      → Attach the matching advice methods to the proxy
     *      → RETURN the proxy instead of the real bean
     *   5. Downstream beans that @Autowire EmployeeService get the PROXY
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * STEP 3: The Advisor Chain
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Each matching advice is wrapped in an ADVISOR:
     *
     *   Advisor = Advice + Pointcut
     *
     * The proxy holds a list of Advisors (the advice chain).
     * When a method is called on the proxy:
     *
     *   1. Proxy intercepts the call
     *   2. Proxy asks: "Which advisors match this specific method call?"
     *   3. Matching advisors are collected and sorted by @Order
     *   4. The ADVICE CHAIN is built (like a chain of filters/interceptors)
     *   5. The chain is executed:
     *      Advisor1.invoke() → [calls Advisor2.invoke() → [calls real method] → return] → return
     *
     * This is the INTERCEPTOR CHAIN pattern — same as Servlet Filters.
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 4: ADVICE EXECUTION ORDER — @Order AND CALL STACK              ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * When MULTIPLE ASPECTS apply to the SAME method, @Order controls the sequence.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * @Order RULES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   Lower number = HIGHER priority = OUTER wrapper
     *
     *   @Order(1) aspect  → OUTERMOST: its @Before runs FIRST, @After runs LAST
     *   @Order(2) aspect  → MIDDLE
     *   @Order(3) aspect  → INNERMOST: its @Before runs LAST, @After runs FIRST
     *
     * This is exactly like nested try/finally blocks:
     *
     *   try {                          // Order(1) @Before runs here
     *     try {                        // Order(2) @Before runs here
     *       try {                      // Order(3) @Before runs here
     *         realMethod();            // ← actual code
     *       } finally {
     *         // Order(3) @After runs  // innermost @After runs first
     *       }
     *     } finally {
     *       // Order(2) @After runs    // middle @After
     *     }
     *   } finally {
     *     // Order(1) @After runs      // outermost @After runs last
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * OUR CHAPTER 09 ASPECT ORDER:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @Order(1)   StructuredLoggingAspect     ← Outermost (logs everything)
     *   @Order(2)   FeatureToggleAspect         ← Gate feature before other processing
     *   @Order(2)   BeforeAdviceDemoAspect      ← Security check early
     *   @Order(3)   SecurityAuditAspect         ← Audit security decisions
     *   @Order(4)   RateLimitingAspect          ← Rate limit before business logic
     *   @Order(5)   AutoRetryAspect             ← Retry outer than caching
     *   @Order(7)   ArgumentSanitizationAspect  ← Sanitize args before business logic
     *   @Order(10)  PerformanceTimingAspect     ← Time the actual business + inner aspects
     *   @Order(15)  SimpleMethodCacheAspect     ← Cache close to business logic
     *   @Order(20)  ExceptionWrappingAspect     ← Wrap exceptions innermost
     *   @Order(30)  ComprehensiveLoggingAspect  ← Innermost logger (wraps real method)
     *
     *   [REAL METHOD]
     *
     * For a createEmployee() call with ALL aspects active, execution order is:
     *
     *   @Order(1)  StructuredLoggingAspect        → @Before (logging): "▶ EmployeeService.createEmployee"
     *   @Order(2)  BeforeAdviceDemoAspect         → @Before (security): "checkRoleAccess"
     *   @Order(3)  SecurityAuditAspect            → (AfterReturning/AfterThrowing for access)
     *   @Order(5)  AutoRetryAspect                → @Around: wraps for retry capability
     *   @Order(7)  ArgumentSanitizationAspect     → @Around: sanitizes args
     *   @Order(10) PerformanceTimingAspect        → @Around: starts timing
     *   @Order(30) ComprehensiveLoggingAspect     → @Around: "──► createEmployee(firstName=Alice, ...)"
     *
     *   ─────── REAL createEmployee() EXECUTES ───────
     *
     *   @Order(30) ComprehensiveLoggingAspect     → @Around (after): "◄── ✅ returned=Employee{id=4}"
     *   @Order(10) PerformanceTimingAspect        → @Around (after): "✅ create-employee — 12ms"
     *   @Order(7)  ArgumentSanitizationAspect     → returns
     *   @Order(5)  AutoRetryAspect                → returns (no retry needed)
     *   @Order(3)  SecurityAuditAspect            → @AfterReturning: "ALLOWED"
     *   @Order(2)  BeforeAdviceDemoAspect         → @After cleanup
     *   @Order(1)  StructuredLoggingAspect        → @Around (after): "◄── ✅ duration=15ms"
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 5: THE SELF-INVOCATION PROBLEM — AND HOW TO FIX IT            ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * THE PROBLEM:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @Service
     *   class OrderService {
     *
     *       public void placeOrder(Order order) {
     *           validateOrder(order);   // ← PROBLEM: this.validateOrder() calls the REAL object
     *           // @Loggable on validateOrder() is NEVER called — proxy bypassed!
     *       }
     *
     *       @Loggable              // ← This advice will NOT run when called from placeOrder()
     *       public void validateOrder(Order order) {
     *           ...
     *       }
     *   }
     *
     * WHY?
     *   When placeOrder() runs, it executes on the REAL EmployeeService instance.
     *   this.validateOrder() calls the real method directly — not through the proxy.
     *   The proxy was never involved, so no advice runs.
     *
     *   External code → [PROXY] → real.placeOrder() → this.validateOrder() (BYPASSES proxy)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * SOLUTION 1: Inject the bean into itself (Spring handles this gracefully)
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @Service
     *   class OrderService {
     *
     *       @Autowired
     *       private OrderService self;   // ← Gets the PROXY, not 'this'
     *
     *       public void placeOrder(Order order) {
     *           self.validateOrder(order);   // ← Goes through PROXY → advice fires!
     *       }
     *
     *       @Loggable
     *       public void validateOrder(Order order) { ... }
     *   }
     *
     *   Spring handles circular self-injection gracefully (it gives you the proxy bean).
     *   This is the CLEANEST solution.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * SOLUTION 2: Use AopContext.currentProxy() (requires exposeProxy=true)
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @Configuration
     *   @EnableAspectJAutoProxy(exposeProxy = true)  ← MUST enable this!
     *   class AopConfig { }
     *
     *   @Service
     *   class OrderService {
     *
     *       public void placeOrder(Order order) {
     *           // Get the current proxy from AopContext (ThreadLocal)
     *           ((OrderService) AopContext.currentProxy()).validateOrder(order);
     *       }
     *
     *       @Loggable
     *       public void validateOrder(Order order) { ... }
     *   }
     *
     *   This is less clean (requires a cast and import of Spring API in business code).
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * SOLUTION 3: Restructure — move the method to a different bean (BEST PRACTICE)
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @Service
     *   class OrderValidationService {
     *       @Loggable
     *       public void validateOrder(Order order) { ... }  // ← In a separate bean
     *   }
     *
     *   @Service
     *   class OrderService {
     *       @Autowired
     *       private OrderValidationService validator;   // ← Different bean → goes through proxy
     *
     *       public void placeOrder(Order order) {
     *           validator.validateOrder(order);   // ← Proxy intercepted → advice fires!
     *       }
     *   }
     *
     *   This is the BEST PRACTICE — it also improves cohesion and testability.
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 6: COMPLETE METHOD CALL LIFECYCLE THROUGH AOP                 ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Request: employeeService.createEmployee("Alice", "Smith", "alice@example.com", "Engineering", 85000)
     * where employeeService is a CGLIB proxy with Order(1) Logging + Order(2) Security aspects
     *
     *  ┌──────────────────────────────────────────────────────────────────────────────────┐
     *  │  1. CALLER CODE                                                                   │
     *  │     employeeService.createEmployee("Alice", ...)                                  │
     *  │     (employeeService is actually EmployeeService$$SpringCGLIB$$0 — the proxy)    │
     *  ├──────────────────────────────────────────────────────────────────────────────────┤
     *  │  2. CGLIB PROXY — createEmployee() override                                       │
     *  │     Proxy doesn't run business logic. Instead:                                    │
     *  │     → Gets the ReflectiveMethodInvocation (the interceptor chain)                │
     *  │     → Starts chain.proceed()                                                      │
     *  ├──────────────────────────────────────────────────────────────────────────────────┤
     *  │  3. INTERCEPTOR CHAIN (sorted by @Order)                                          │
     *  │                                                                                   │
     *  │  [Order 1] StructuredLoggingAspect (@Around — before part)                       │
     *  │     → Logs: "▶ EmployeeService.createEmployee(firstName=Alice, ...)"              │
     *  │     → calls chain.proceed()                                                       │
     *  │                                                                                   │
     *  │  [Order 2] BeforeAdviceDemoAspect (@Before — security)                           │
     *  │     → Checks: hasRole("HR_MANAGER")?                                              │
     *  │     → If NO:  throws SecurityException → chain aborted → propagates to Order 1   │
     *  │     → If YES: returns, chain continues                                            │
     *  │                                                                                   │
     *  │  [Order 30] ComprehensiveLoggingAspect (@Around — before part)                   │
     *  │     → Logs: "──► createEmployee(firstName=Alice, ...)"                           │
     *  │     → calls chain.proceed()                                                       │
     *  │                                                                                   │
     *  ├──────────────────────────────────────────────────────────────────────────────────┤
     *  │  4. REAL METHOD                                                                   │
     *  │     EmployeeService.createEmployee("Alice", ...)                                  │
     *  │     → validates args                                                              │
     *  │     → creates Employee{id=4}                                                      │
     *  │     → stores in map                                                               │
     *  │     → returns Employee{id=4}                                                      │
     *  ├──────────────────────────────────────────────────────────────────────────────────┤
     *  │  5. RETURN THROUGH CHAIN (reversed)                                               │
     *  │                                                                                   │
     *  │  [Order 30] ComprehensiveLoggingAspect (@Around — after part)                    │
     *  │     → Logs: "◄── ✅ returned=Employee{id=4} | 12ms"                              │
     *  │     → returns Employee{id=4}                                                      │
     *  │                                                                                   │
     *  │  [Order 1] StructuredLoggingAspect (@Around — after part)                        │
     *  │     → Logs: "◄── ✅ createEmployee duration=15ms"                                │
     *  │     → returns Employee{id=4}                                                      │
     *  │                                                                                   │
     *  ├──────────────────────────────────────────────────────────────────────────────────┤
     *  │  6. RESULT RETURNED TO CALLER                                                     │
     *  │     Employee{id=4, firstName="alice", lastName="smith", email="alice@example.com"}│
     *  │     (Note: email was lowercased by ArgumentSanitizationAspect if active)          │
     *  └──────────────────────────────────────────────────────────────────────────────────┘
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 7: SPRING AOP vs PURE ASPECTJ — COMPARISON                    ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     *  FEATURE                       SPRING AOP              PURE ASPECTJ
     *  ─────────────────────────────  ────────────────────    ───────────────────────────
     *  Weaving type                   Runtime (proxies)       Compile or Load-time
     *  Can advise private methods     ❌ No                    ✅ Yes
     *  Can advise constructors        ❌ No                    ✅ Yes
     *  Can advise field access        ❌ No                    ✅ Yes
     *  Works with non-Spring objects  ❌ No (only Spring beans) ✅ Yes (any Java object)
     *  Self-invocation                ❌ Bypasses proxy        ✅ Works (woven into bytecode)
     *  Setup complexity               ✅ Simple (starter-aop)  ❌ Complex (ajc/agent)
     *  Performance                    ✅ Good (minimal overhead) ✅ Best (compile-time)
     *  @Aspect annotation             Same syntax              Same syntax
     *  Pointcut expressions           Subset of AspectJ        Full AspectJ language
     *  Spring integration             ✅ Native               ✅ Supported (AJ integration)
     *  Production usage               99% of Spring apps       Rare (only for advanced needs)
     *
     *  RECOMMENDATION:
     *  Use Spring AOP (runtime) for 99% of use cases. It handles:
     *    logging, security, caching, transactions, retry, performance monitoring
     *
     *  Use AspectJ when you absolutely need:
     *    → Private method interception
     *    → Constructor interception
     *    → Non-Spring object interception
     *    → Zero proxy overhead in extremely performance-sensitive code
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 8: HOW @Transactional USES AOP (Connecting the Dots)           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Now that you understand Spring AOP, you can understand @Transactional!
     *
     * @Transactional is implemented as an @Around advice:
     *
     *   // What Spring's transaction interceptor does conceptually:
     *   @Around("@annotation(Transactional)")
     *   public Object manageTransaction(ProceedingJoinPoint pjp) throws Throwable {
     *
     *       TransactionStatus status = transactionManager.getTransaction(def);
     *       // (Propagation.REQUIRED → join existing, or begin new transaction)
     *
     *       try {
     *           Object result = pjp.proceed();  // ← run your @Service method
     *           transactionManager.commit(status);
     *           return result;
     *
     *       } catch (RuntimeException ex) {
     *           transactionManager.rollback(status);  // ← rollback on RuntimeException
     *           throw ex;
     *       } catch (CheckedException ex) {
     *           transactionManager.commit(status);    // ← COMMIT even on checked exception
     *           throw ex;                             //   (unless rollbackFor=Exception.class)
     *       }
     *   }
     *
     * THIS IS WHY:
     *   ✅ @Transactional also suffers from the SELF-INVOCATION problem
     *      (calling this.transactionalMethod() from within the same bean → no transaction!)
     *   ✅ @Transactional on private methods does NOTHING (proxy can't intercept private)
     *   ✅ @Transactional works on @Service methods but NOT on the repository layer directly
     *      (when called from a service that already has @Transactional)
     *
     * Once you deeply understand AOP internals, @Transactional makes perfect sense!
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 9: SUMMARY — THE AOP CHEAT SHEET                              ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     *  SETUP CHECKLIST:
     *    □  Add spring-boot-starter-aop to build.gradle / pom.xml
     *    □  Spring Boot auto-configures @EnableAspectJAutoProxy (no manual setup needed)
     *    □  Create @Aspect @Component class (both annotations required!)
     *    □  Define @Pointcut methods (optional but recommended for reuse)
     *    □  Write @Before/@After/@AfterReturning/@AfterThrowing/@Around advice methods
     *    □  Add @Order if you have multiple aspects affecting the same bean
     *
     *  QUICK DECISION GUIDE — Which advice type to use?
     *
     *    Do you need to...                          Use...
     *    ─────────────────────────────────────────  ──────────────
     *    Log method call + args                     @Before
     *    Block method execution (security check)    @Before (throw exception)
     *    Log return value                           @AfterReturning(returning="val")
     *    Log exceptions                             @AfterThrowing(throwing="ex")
     *    Clean up resources (always)                @After
     *    Measure execution time                     @Around
     *    Cache results                              @Around
     *    Retry on failure                           @Around
     *    Modify arguments                           @Around (pjp.proceed(newArgs))
     *    Modify return value                        @Around (change return)
     *    Prevent method from running                @Around (don't call proceed())
     *    Wrap exceptions                            @Around (catch + rethrow different)
     *    Log + Time + handle exceptions together    @Around (most powerful, use one)
     *
     *  COMMON MISTAKES:
     *    ❌ @Aspect without @Component → aspect never detected
     *    ❌ Private method with @PreAuthorize/@Transactional → silently ignored
     *    ❌ this.method() instead of self.method() → self-invocation → advice bypassed
     *    ❌ Forgetting pjp.proceed() in @Around → real method never runs!
     *    ❌ Forgetting 'return result' in @Around → caller always gets null
     *    ❌ @Around on void method: must 'return null' explicitly
     *    ❌ Too broad pointcut: execution(* *(..)) → advises everything including framework code
     *
     */
}

