package com.learning.springboot.chapter09;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║                    SPRING BOOT ANNOTATIONS — COMPREHENSIVE GUIDE                     ║
 * ║                         Chapter 9: Spring AOP Annotations                           ║
 * ║                                                                                       ║
 * ║                           📚 From Zero to Expert 📚                                  ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Chapter:      9
 * Title:        Spring AOP Annotations
 * Difficulty:   ⭐⭐⭐⭐ Intermediate–Advanced
 * Estimated:    6–10 hours
 * Prerequisites: Chapter 2 (Spring Core), Chapter 3 (Spring MVC),
 *                Chapter 5 (Security), Basic Java & OOP
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │                                                                                       │
 * │                    CHAPTER 9: OVERVIEW & LEARNING GOALS                              │
 * │                                                                                       │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📖 TABLE OF CONTENTS 📖
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Section  1 :  Chapter Introduction & The Cross-Cutting Problem
 * Section  2 :  AOP Core Concepts — The Terminology Dictionary
 *                   → Aspect, Join Point, Pointcut, Advice, Target, Proxy, Weaving
 * Section  3 :  The Spring AOP Annotations Quick Reference
 * Section  4 :  @EnableAspectJAutoProxy  (activate AOP proxy creation)
 * Section  5 :  @Aspect                  (declare a class as an aspect)
 * Section  6 :  @Pointcut                (reusable pointcut expressions)
 *                   → execution(), within(), @annotation(), bean(), args(), target()
 * Section  7 :  Advice Annotations
 *                   → @Before            (run before the method)
 *                   → @After             (run after, always — like finally)
 *                   → @AfterReturning    (run after successful return)
 *                   → @AfterThrowing     (run when exception is thrown)
 *                   → @Around            (wrap the method — most powerful)
 * Section  8 :  JoinPoint and ProceedingJoinPoint API
 * Section  9 :  Advice Ordering with @Order
 * Section 10 :  Custom Annotation-Based Pointcuts
 * Section 11 :  Real-World Aspect Patterns
 *                   → Logging Aspect
 *                   → Performance Monitoring Aspect
 *                   → Security/Authorization Aspect
 *                   → Audit Trail Aspect
 *                   → Retry Aspect
 * Section 12 :  How Everything Works — Proxy Internals
 * Section 13 :  Best Practices & Common Pitfalls
 * Section 14 :  Interview Questions & Answers
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * 📂 FILES IN THIS CHAPTER:
 *
 *  src/main/java/.../chapter09/
 *   • Chapter09Overview.java               ← YOU ARE HERE
 *   • Example01AspectAndPointcut.java       (@EnableAspectJAutoProxy, @Aspect, @Pointcut,
 *                                            all pointcut expression types, custom annotations,
 *                                            domain services: EmployeeService, TimeTrackingService)
 *   • Example02BeforeAfterAdvice.java       (@Before, @After, @AfterReturning, @AfterThrowing,
 *                                            JoinPoint API, argument access, exception access)
 *   • Example03AroundAdvice.java            (@Around, ProceedingJoinPoint, timing, caching,
 *                                            retry, argument modification, return modification)
 *   • Example04RealWorldAspects.java        (LoggingAspect, PerformanceAspect, SecurityAspect,
 *                                            AuditAspect, RetryAspect — production patterns)
 *   • HowItWorksExplained.java              (JDK proxy vs CGLIB proxy, weaving, AnnotationAware
 *                                            AutoProxyCreator, advice chain order, self-invocation)
 *
 *  src/test/java/.../chapter09/
 *   • Chapter09AopTest.java                 (Live tests: proxy detection, aspect verification,
 *                                            @Before/@Around/@AfterThrowing in action)
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
public class Chapter09Overview {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                    SECTION 1: THE CROSS-CUTTING PROBLEM                      ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 LEARNING OBJECTIVES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * By the end of this chapter, you will be able to:
     *
     *  ✓  Understand what AOP is and why it exists (cross-cutting concerns)
     *  ✓  Declare aspects with @Aspect and activate them with @EnableAspectJAutoProxy
     *  ✓  Write pointcut expressions to select exactly the methods you want to advise
     *  ✓  Apply all five advice types (@Before, @After, @AfterReturning,
     *      @AfterThrowing, @Around)
     *  ✓  Access method name, arguments, and return values inside advice
     *  ✓  Build real-world aspects: logging, performance monitoring, security, audit
     *  ✓  Create custom annotations and use them as pointcut markers
     *  ✓  Understand JDK proxy vs CGLIB proxy — when each is used
     *  ✓  Order multiple aspects with @Order
     *  ✓  Identify the self-invocation pitfall and how to avoid it
     *  ✓  Answer AOP interview questions with confidence
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ❓ THE PROBLEM — CROSS-CUTTING CONCERNS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Imagine you have 50 service methods. You need to:
     *   1. Log every method call (name, args, return value, duration)
     *   2. Check security permissions before every admin method
     *   3. Record an audit trail for every data-modifying operation
     *   4. Measure execution time for performance SLA monitoring
     *   5. Retry transient failures (network timeouts, deadlocks)
     *
     * WITHOUT AOP — you repeat the same code in every method:
     *
     *   @Service
     *   class EmployeeService {
     *
     *       public Employee createEmployee(CreateEmployeeRequest req) {
     *           // ← LOGGING boilerplate
     *           log.info("createEmployee called: args={}", req);
     *           long start = System.currentTimeMillis();
     *           try {
     *               // ← SECURITY boilerplate
     *               if (!securityContext.hasRole("HR_MANAGER")) {
     *                   throw new AccessDeniedException("Requires HR_MANAGER");
     *               }
     *               // ← AUDIT boilerplate
     *               auditLog.record("CREATE", "Employee", req.getId());
     *
     *               // ← ACTUAL BUSINESS LOGIC (buried under boilerplate!)
     *               Employee emp = new Employee(req);
     *               return repo.save(emp);
     *
     *           } finally {
     *               // ← MORE LOGGING boilerplate
     *               log.info("createEmployee took {}ms", System.currentTimeMillis() - start);
     *           }
     *       }
     *
     *       // Repeat this EXACT pattern for every other method → DRY violation!
     *       public Employee updateEmployee(...) { /* same boilerplate again *\/ }
     *       public void deleteEmployee(...) { /* same boilerplate again *\/ }
     *       // ...50 more methods...
     *   }
     *
     * Problems:
     *   ❌ Business logic is buried under cross-cutting concerns
     *   ❌ 50 methods × 5 concerns = 250 places to update if logging format changes
     *   ❌ Easy to forget adding security check to a new method
     *   ❌ Impossible to test business logic without the cross-cutting code
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ✅ WITH AOP — Separation of Concerns:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @Service
     *   class EmployeeService {
     *       // PURE business logic — no logging, no security, no audit
     *       public Employee createEmployee(CreateEmployeeRequest req) {
     *           return repo.save(new Employee(req));
     *       }
     *       // All 50 methods are equally clean!
     *   }
     *
     *   @Aspect @Component
     *   class LoggingAspect {
     *       @Around("within(com.example.service.*)")
     *       public Object logCall(ProceedingJoinPoint pjp) throws Throwable {
     *           // ONE place for all logging logic — applies to ALL 50 methods
     *       }
     *   }
     *
     *   @Aspect @Component
     *   class SecurityAspect {
     *       @Before("@annotation(RequiresRole)")
     *       public void checkRole(JoinPoint jp, RequiresRole req) { ... }
     *   }
     *
     * Result:
     *   ✅ Business logic is clean and readable
     *   ✅ Cross-cutting code lives in ONE place
     *   ✅ Change logging format in one place → affects all 50 methods
     *   ✅ New method automatically gets logging, security, audit
     *   ✅ Each concern is independently testable
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║            SECTION 2: AOP CORE CONCEPTS — TERMINOLOGY DICTIONARY             ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 1️⃣  ASPECT
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * A MODULE that encapsulates a cross-cutting concern.
     * An Aspect contains:
     *   → One or more ADVICE methods (the "what to do")
     *   → One or more POINTCUT expressions (the "where to do it")
     *
     * Real-world analogy: A security guard.
     *   → WHAT they do: check badges, log entry/exit
     *   → WHERE: every door in the building
     *
     *   @Aspect @Component
     *   class LoggingAspect {
     *       // This is an ASPECT — it encapsulates the logging concern
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 2️⃣  JOIN POINT
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * A POINT IN PROGRAM EXECUTION where an aspect can be plugged in.
     * In Spring AOP: a Join Point is always a METHOD EXECUTION.
     *
     * Examples of join points:
     *   → The moment createEmployee() is called
     *   → The moment getEmployee(123) is called
     *   → The moment deleteEmployee() throws an exception
     *
     * Note: AspectJ supports more join point types (constructor calls, field access),
     * but Spring AOP only supports METHOD EXECUTION join points.
     *
     * In advice code, the JoinPoint object gives you info about the current join point:
     *   joinPoint.getSignature().getName()   → method name
     *   joinPoint.getArgs()                  → method arguments
     *   joinPoint.getTarget()                → the real object being called
     *   joinPoint.getThis()                  → the proxy object
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 3️⃣  POINTCUT
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * A PREDICATE (expression) that MATCHES a set of join points.
     * The pointcut answers: "WHICH methods should this advice apply to?"
     *
     * Spring supports these pointcut designators:
     *
     *   execution(...)   → match by method signature (most common)
     *   within(...)      → match all methods in a package/class
     *   @annotation(...) → match methods annotated with a specific annotation
     *   @within(...)     → match methods in classes annotated with an annotation
     *   bean(...)        → match by Spring bean name
     *   args(...)        → match by method argument types
     *   target(...)      → match by target object type
     *   this(...)        → match by proxy type
     *
     * Examples:
     *   execution(* com.example.service.*.*(..))    → all methods in service package
     *   @annotation(Loggable)                       → methods with @Loggable
     *   within(com.example.service.*)               → all beans in service package
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 4️⃣  ADVICE
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * The ACTION taken by an aspect at a join point.
     * Think of it as: "What code runs at the matched method?"
     *
     * Five advice types:
     *
     *   @Before         → runs BEFORE the method (but can't prevent execution)
     *   @After          → runs AFTER always (like finally — even on exception)
     *   @AfterReturning → runs AFTER the method returns successfully
     *   @AfterThrowing  → runs AFTER the method throws an exception
     *   @Around         → wraps the method (MOST POWERFUL — can do anything)
     *
     *   Order:
     *   @Around (before)
     *     → @Before
     *       → [METHOD EXECUTES]
     *     → @AfterReturning / @AfterThrowing
     *   → @After
     *   @Around (after)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 5️⃣  TARGET OBJECT
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * The REAL OBJECT that is being advised. In Spring, this is your @Service or
     * @Repository bean. The target object has NO knowledge that it's being advised —
     * it's completely unaware of the aspects applied to it.
     *
     *   EmployeeService (real object) ← target object
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 6️⃣  PROXY
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * An OBJECT CREATED BY AOP that wraps the target object.
     * When you @Autowire EmployeeService, you get the PROXY, not the real service.
     * The proxy intercepts method calls and runs the advice.
     *
     *   Your code → [EmployeeService PROXY] → (runs advice) → [Real EmployeeService]
     *
     * Spring creates two types of proxies:
     *   JDK Dynamic Proxy  → if target implements an interface
     *   CGLIB Proxy        → if target is a plain class (DEFAULT in Spring Boot)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 7️⃣  WEAVING
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * The process of LINKING aspects with application code.
     * Three types:
     *
     *   Compile-time weaving  → AspectJ compiler transforms bytecode during compilation
     *   Load-time weaving     → AspectJ agent transforms bytecode when loaded by JVM
     *   Runtime weaving       → Spring AOP's approach — creates proxies at runtime
     *
     * Spring uses RUNTIME WEAVING (proxy-based). It's simpler but has limitations
     * (can't advise private methods, non-Spring beans, or constructor calls).
     *
     * For full AspectJ power (compile/load-time): set proxyTargetClass=false and
     * configure the AspectJ agent. Most Spring apps use runtime weaving.
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║            SECTION 3: AOP ANNOTATIONS QUICK REFERENCE                        ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     *  ┌──────────────────────────────────┬──────────────────────────────────────────┐
     *  │ ANNOTATION                        │ ONE-LINE DESCRIPTION                     │
     *  ├──────────────────────────────────┼──────────────────────────────────────────┤
     *  │ @EnableAspectJAutoProxy          │ Activate AspectJ-powered AOP in Spring    │
     *  ├──────────────────────────────────┼──────────────────────────────────────────┤
     *  │ @Aspect                          │ Declare a class as an aspect              │
     *  │ @Pointcut                        │ Define a reusable pointcut expression     │
     *  ├──────────────────────────────────┼──────────────────────────────────────────┤
     *  │ @Before("pointcut")              │ Advice: run BEFORE the method             │
     *  │ @After("pointcut")               │ Advice: run AFTER (always — like finally) │
     *  │ @AfterReturning(...)             │ Advice: run AFTER successful return       │
     *  │ @AfterThrowing(...)              │ Advice: run AFTER exception thrown        │
     *  │ @Around("pointcut")              │ Advice: wrap the method (most powerful)   │
     *  └──────────────────────────────────┴──────────────────────────────────────────┘
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 ADVICE COMPARISON TABLE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   ADVICE         WHEN RUNS          CAN PREVENT EXECUTION?  CAN ACCESS RETURN?
     *   ─────────────  ─────────────────  ──────────────────────  ──────────────────
     *   @Before        Before method      Yes (throw exception)   No (not yet)
     *   @After         After always       No                      No (thrown away)
     *   @AfterReturning After success     No (too late)           Yes (read-only)
     *   @AfterThrowing After exception    No (too late)           N/A
     *   @Around        Before AND after   Yes (skip proceed())    Yes (can change it)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 POINTCUT DESIGNATORS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   DESIGNATOR      MATCHES                               EXAMPLE
     *   ─────────────   ─────────────────────────────────    ────────────────────────────────
     *   execution()     Method signature pattern             execution(* com.example.*.*(..))
     *   within()        All methods in package/class         within(com.example.service.*)
     *   @annotation()   Methods with a specific annotation   @annotation(Loggable)
     *   @within()       Methods in @Annotated classes        @within(org.springframework.stereotype.Service)
     *   bean()          Spring bean by name                  bean(employeeService)
     *   args()          Methods taking specific arg types    args(String, ..)
     *   target()        Target object type                   target(EmployeeService)
     *   this()          Proxy type                           this(EmployeeService)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 execution() EXPRESSION SYNTAX:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   execution([modifier] return-type [class-type.]method-name(parameter-types) [throws])
     *
     *   Wildcards:
     *     *   → matches any single segment (one part of a package, one type, etc.)
     *     ..  → matches any number of things (any package depth, any number of params)
     *     +   → match the type and its subclasses (e.g., EmployeeService+)
     *
     *   EXAMPLES:
     *     execution(* *(..))                          → ALL methods anywhere
     *     execution(public * *(..))                   → All public methods
     *     execution(* com.example.*.*(..))            → All methods in com.example.*
     *     execution(* com.example..*.*(..))           → All methods in com.example and subpackages
     *     execution(* com.example.EmployeeService.*(..)) → All methods in EmployeeService
     *     execution(* createEmployee(..))             → Any createEmployee() anywhere
     *     execution(* *(String, ..))                  → Methods taking String as first arg
     *     execution(!private * *(..))                 → All non-private methods
     *     execution(* com.example..*Service.*(..))    → All methods in *Service classes
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔗 COMBINING POINTCUTS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   &&    → AND: both expressions must match
     *   ||    → OR:  either expression must match
     *   !     → NOT: expression must NOT match
     *
     *   @Before("execution(* com.example..*(..)) && @annotation(Loggable)")
     *   @Before("inServiceLayer() || inRepositoryLayer()")
     *   @Before("within(com.example..*) && !bean(legacyService)")
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║            SECTION 4: COMMON AOP USE CASES                                   ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     *  USE CASE                  ADVICE TYPE    DESCRIPTION
     *  ─────────────────────────  ────────────   ────────────────────────────────────────
     *  Method call logging        @Around        Log entry, args, exit, return value, time
     *  Performance monitoring     @Around        Measure execution time, alert on SLA breach
     *  Security authorization     @Before        Check role/permission before method runs
     *  Audit trail                @Around        Record who did what and when
     *  Retry on failure           @Around        Retry transient failures N times
     *  Caching                    @Around        Return cached value; skip method if cached
     *  Transaction management     @Around        Spring's @Transactional uses @Around internally
     *  Input validation           @Before        Pre-validate arguments
     *  Rate limiting              @Around        Throttle requests per user
     *  Error logging/alerting     @AfterThrowing Log stack trace, send alert notification
     *  Resource cleanup           @After         Release resources whether success or failure
     *  Return value enrichment    @AfterReturning Add extra data to returned objects
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║            SECTION 5: BEST PRACTICES & COMMON PITFALLS                       ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ✅ DO's:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ✅ Scope pointcuts tightly — use within() to restrict to specific packages
     *  ✅ Use @Around for complex advice (timing, caching, retry)
     *  ✅ Extract pointcut expressions into @Pointcut methods for reuse
     *  ✅ Add @Component to @Aspect classes so Spring detects them
     *  ✅ Use @Order to control advice execution order when multiple aspects apply
     *  ✅ Keep advice methods focused — one concern per aspect
     *  ✅ Use custom annotations (@Loggable, @Auditable) as pointcut markers for clarity
     *  ✅ Always call pjp.proceed() in @Around (unless intentionally blocking)
     *  ✅ Test aspects independently — mock the JoinPoint
     *  ✅ Prefer @Around when you need both pre- and post-processing
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ❌ DON'Ts:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ❌ Don't advise private methods — CGLIB proxy can't intercept them
     *  ❌ Don't call this.method() to invoke an advised method from inside the same bean
     *     (self-invocation bypasses the proxy — the advice won't fire!)
     *  ❌ Don't mark @Aspect classes as @Transactional — causes circular proxy issues
     *  ❌ Don't use overly broad pointcuts (execution(* *(..))) — too expensive, too risky
     *  ❌ Don't forget to call pjp.proceed() in @Around — the real method won't run!
     *  ❌ Don't apply AOP to final classes/methods — CGLIB can't subclass final types
     *  ❌ Don't put expensive operations in frequently-triggered pointcuts without profiling
     *  ❌ Don't catch Throwable in @Around without re-throwing — hides real errors
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║            SECTION 6: TOP INTERVIEW QUESTIONS — CHAPTER 9                   ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Q1:  What is AOP and what problem does it solve?
     * A1:  AOP (Aspect-Oriented Programming) separates cross-cutting concerns (logging,
     *      security, caching, transactions) from business logic. Without AOP, these concerns
     *      are scattered and duplicated across many methods. With AOP, they live in one
     *      modular Aspect class and are applied automatically via pointcuts.
     *
     * Q2:  What is the difference between Aspect, Join Point, Pointcut, and Advice?
     * A2:  • Aspect = the module containing the cross-cutting logic
     *      • Join Point = a specific point in execution (always a method call in Spring AOP)
     *      • Pointcut = an expression that selects which join points to apply advice to
     *      • Advice = the actual code that runs at a matched join point
     *      Analogy: Security camera system. Aspect = the security system. Join Point =
     *      every entrance door. Pointcut = "only doors on floors 3 and 4". Advice = "take
     *      a photo and log the entry."
     *
     * Q3:  What is the difference between JDK Dynamic Proxy and CGLIB Proxy?
     * A3:  JDK Dynamic Proxy requires the target to implement an interface. It creates a
     *      proxy that implements the same interface. CGLIB Proxy creates a SUBCLASS of the
     *      target class — no interface required. Spring Boot defaults to CGLIB (proxyTargetClass=
     *      true). CGLIB can't proxy final classes or methods.
     *
     * Q4:  What is the self-invocation problem in AOP?
     * A4:  If a bean calls one of its own methods (this.myMethod()), the call bypasses
     *      the AOP proxy. The advice on myMethod() is NEVER called. Fix: (1) inject the
     *      bean into itself using @Autowired, (2) use ApplicationContext.getBean() to get
     *      the proxy, or (3) restructure to call the advised method from a different bean.
     *
     * Q5:  What is the difference between @Before and @Around?
     * A5:  @Before runs before the method but CANNOT prevent its execution (only by throwing
     *      an exception). @Around wraps the method entirely — it calls pjp.proceed() to
     *      actually run the method, can skip calling proceed() to prevent execution, can
     *      modify arguments, and can change the return value. @Around is the most powerful.
     *
     * Q6:  What is the difference between @After and @AfterReturning?
     * A6:  @After runs ALWAYS — whether the method succeeds OR throws an exception (like Java's
     *      finally block). @AfterReturning runs ONLY when the method returns successfully (no
     *      exception). @AfterReturning also gives you access to the return value.
     *
     * Q7:  How does Spring's @Transactional use AOP internally?
     * A7:  @Transactional is implemented as an @Around advice. Before the method runs, it
     *      begins a transaction (or joins an existing one based on propagation). After the
     *      method returns, it commits. If an exception is thrown, it rolls back. This is
     *      exactly what you'd write in a custom @Around advice — Spring just ships with it
     *      pre-built. This is why @Transactional also suffers from the self-invocation problem.
     *
     * Q8:  How do you order multiple aspects applied to the same method?
     * A8:  Annotate each @Aspect class with @Order(n). Lower number = higher priority =
     *      the aspect's @Before runs earlier and @After runs later (wraps the others).
     *      @Order(1) aspect's @Before runs first; @Order(1) aspect's @After runs last.
     *      Without @Order, the order is undefined/JVM-dependent.
     *
     * Q9:  Can Spring AOP advise private methods? Why not?
     * A9:  No. Spring AOP uses CGLIB to create a SUBCLASS of your bean. Subclasses in Java
     *      can override public/protected methods but NOT private methods. Private methods
     *      are called directly on the real object, bypassing the proxy entirely. To advise
     *      private-like methods, use AspectJ compile-time or load-time weaving instead.
     *
     * Q10: What is @EnableAspectJAutoProxy and when is it needed?
     * A10: @EnableAspectJAutoProxy enables Spring's AnnotationAwareAspectJAutoProxyCreator,
     *      which detects @Aspect beans and creates AOP proxies automatically. In Spring Boot,
     *      it's auto-configured when spring-boot-starter-aop is on the classpath
     *      (AopAutoConfiguration). You only need to add it explicitly if you want to
     *      change settings (e.g., proxyTargetClass=false, exposeProxy=true).
     *
     */
}

