package com.learning.springboot.chapter09;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║       EXAMPLE 02: @Before, @After, @AfterReturning, @AfterThrowing IN DEPTH         ║
 * ║       — JoinPoint API · Argument Access · Return Value · Exception Capture          ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example02BeforeAfterAdvice.java
 * Purpose:     Master the four "simple" advice types. Understand when each is appropriate,
 *              how to access method context via JoinPoint, and the execution order.
 * Difficulty:  ⭐⭐⭐ Intermediate
 * Time:        35 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * ADVICE EXECUTION ORDER FOR A SUCCESSFUL METHOD:
 *
 *   @Before     ←── runs first
 *     [method body executes]
 *   @AfterReturning ←── runs on success
 *   @After      ←── runs always (after @AfterReturning)
 *
 * ADVICE EXECUTION ORDER FOR A FAILED METHOD (exception thrown):
 *
 *   @Before     ←── runs first
 *     [method body throws exception]
 *   @AfterThrowing ←── runs on exception
 *   @After      ←── runs always (after @AfterThrowing)
 *
 * VISUAL:
 *
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │  @Before                                                     │
 *   │       ↓                                                      │
 *   │  [METHOD EXECUTES]                                           │
 *   │       ↓──── success ──→ @AfterReturning ──→ @After          │
 *   │       ↓──── exception → @AfterThrowing  ──→ @After          │
 *   └──────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 1: JoinPoint API — ACCESSING METHOD CONTEXT IN ADVICE
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║             JoinPoint API — COMPLETE REFERENCE                               ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * JoinPoint gives advice methods information about the INTERCEPTED METHOD CALL.
 * Available in: @Before, @After, @AfterReturning, @AfterThrowing
 * (For @Around, use ProceedingJoinPoint — covered in Example03)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * JoinPoint METHODS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   jp.getSignature()
 *     → Returns a Signature object describing the intercepted method.
 *     → Cast to MethodSignature for full method info.
 *
 *   jp.getSignature().getName()
 *     → The method name: "createEmployee"
 *
 *   jp.getSignature().getDeclaringTypeName()
 *     → The class name: "com.learning.springboot.chapter09.EmployeeService"
 *
 *   ((MethodSignature) jp.getSignature()).getMethod()
 *     → The java.lang.reflect.Method object — access annotations, param names, etc.
 *
 *   ((MethodSignature) jp.getSignature()).getReturnType()
 *     → The declared return type class.
 *
 *   jp.getArgs()
 *     → Object[] of the method's actual arguments at call time.
 *     → e.g., createEmployee("Alice", "Smith", ...) → ["Alice", "Smith", ...]
 *
 *   jp.getTarget()
 *     → The REAL object (not the proxy). The @Service bean instance.
 *     → Useful when you need to call methods on the real bean.
 *
 *   jp.getThis()
 *     → The PROXY object (the AOP-created wrapper).
 *
 *   jp.getKind()
 *     → "method-execution" (in Spring AOP, always this value)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * BINDING ADVICE PARAMETERS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Instead of jp.getArgs()[0], you can bind method arguments directly:
 *
 *   @Before("allEmployeeServiceMethods() && args(firstName, ..)")
 *   public void beforeCreate(JoinPoint jp, String firstName) {
 *       // firstName is automatically bound from the first String argument
 *   }
 *
 * This is type-safe and more readable than using getArgs()[0].
 *
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 2: @Before — RUNS BEFORE THE METHOD
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                       @Before  EXPLAINED IN DEPTH                            ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @Before advice runs BEFORE the matched method executes.
 * The real method STILL runs after @Before completes, UNLESS you throw an exception
 * in the @Before advice — which aborts the method call with that exception.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 USE CASES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  ✅ Security checks (check role/permission before method runs)
 *  ✅ Request logging (log "method was called with these args")
 *  ✅ Input pre-validation (validate arguments before business logic)
 *  ✅ Rate limiting (check if caller exceeded their request limit)
 *  ✅ Setting thread-local context (user ID, trace ID, etc.)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ LIMITATIONS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  ❌ Cannot change the method's arguments (read-only access to args)
 *  ❌ Cannot prevent method execution EXCEPT by throwing an exception
 *  ❌ Cannot see the return value (method hasn't run yet)
 *  ❌ Cannot change the return value
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔄 HOW IT WORKS INTERNALLY:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   Caller calls proxy.createEmployee("Alice", ...)
 *        │
 *        ▼
 *   CGLIB Proxy intercepts
 *        │
 *        ▼
 *   MethodBeforeAdviceInterceptor.invoke()
 *        │── Run all @Before advice methods
 *        │── If any @Before throws → abort → exception propagates to caller
 *        │── If all @Before pass → proceed to real method
 *        ▼
 *   EmployeeService.createEmployee("Alice", ...)  ← real method executes
 *
 */
@Aspect
@Component
@Order(1)   // ← This aspect runs first (lowest number = highest priority)
            //   @Before of Order(1) runs before @Before of Order(2)
            //   @After  of Order(1) runs AFTER  @After  of Order(2)
class BeforeAdviceDemoAspect {

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 1: Simple method call logging with JoinPoint
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Logs every public method call in chapter09 with its arguments.
     *
     * JoinPoint gives us the method name and arguments.
     * This runs BEFORE every method matched by the pointcut.
     */
    @Before("execution(public * com.learning.springboot.chapter09..*(..))")
    public void logMethodEntry(JoinPoint jp) {
        String className  = jp.getSignature().getDeclaringType().getSimpleName();
        String methodName = jp.getSignature().getName();
        Object[] args     = jp.getArgs();

        System.out.printf("[Before] %-25s.%-30s | args=%s%n",
                className, methodName, Arrays.toString(args));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 2: Security check — abort method if role missing
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Checks if the current user has the required role before the method runs.
     *
     * @annotation(requiredRole) binds the @RequiresRole annotation to the parameter.
     * This lets us access the annotation's value() attribute directly.
     *
     * If the role check FAILS, we throw an exception → method does NOT run.
     * If it PASSES, @Before completes normally → method runs.
     *
     * Real production code would check SecurityContextHolder here.
     * We simulate it with a ThreadLocal role for demonstration.
     */
    @Before("@annotation(requiredRole)")
    public void checkRoleAccess(JoinPoint jp, RequiresRole requiredRole) {
        String required = requiredRole.value();
        String current  = CurrentUserContext.getRole();  // Simulated security context

        System.out.printf("[SecurityCheck] Method '%s' requires role '%s'. Current user role: '%s'%n",
                jp.getSignature().getName(), required, current);

        if (!current.equals(required)) {
            // Throw exception → @Before aborts the method call → method does NOT run
            throw new SecurityException(
                String.format("Access denied: method '%s' requires role '%s' but user has role '%s'",
                        jp.getSignature().getName(), required, current));
        }

        System.out.printf("[SecurityCheck] ✅ Role '%s' verified — proceeding%n", required);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 3: Access individual method arguments by binding
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Demonstrates argument BINDING — binding method args to advice parameters.
     *
     * "args(id, ..)" means:
     *   → The matched method must have a Long as its FIRST argument
     *   → That Long value is bound to the 'id' parameter of this advice method
     *
     * This is safer than jp.getArgs()[0] because it's type-safe.
     */
    @Before("execution(* com.learning.springboot.chapter09.EmployeeService.findById(..)) && args(id)")
    public void beforeFindById(JoinPoint jp, Long id) {
        // 'id' is the actual Long argument passed to findById()
        System.out.printf("[Before:findById] Looking up employee id=%d%n", id);
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Employee ID must be a positive number, got: " + id);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 4: Access annotation attributes in @Before
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Reads the @PerformanceMonitor annotation and logs which operation is starting.
     * The annotation attributes (slaMillis, operation) are accessible via the bound param.
     */
    @Before("@annotation(monitor)")
    public void beforeMonitoredMethod(JoinPoint jp, PerformanceMonitor monitor) {
        String operation = monitor.operation().isEmpty()
                ? jp.getSignature().getName()
                : monitor.operation();
        System.out.printf("[PerfMonitor] Starting operation: '%s' (SLA: %dms)%n",
                operation, monitor.slaMillis());
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 3: @After — RUNS AFTER ALWAYS (LIKE FINALLY)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                       @After  EXPLAINED IN DEPTH                             ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @After runs after the method, REGARDLESS of outcome (success or exception).
 * It is the AOP equivalent of Java's finally block.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 USE CASES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  ✅ Release resources (database connections, file handles, locks)
 *  ✅ Clear thread-local context (MDC cleanup, request context removal)
 *  ✅ Record completion event (regardless of success/failure)
 *  ✅ Reset state (counters, flags set in @Before)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ LIMITATIONS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  ❌ Cannot see the return value (already returned/thrown by the time @After runs)
 *  ❌ Cannot distinguish success from failure (use @AfterReturning/@AfterThrowing instead)
 *  ❌ Runs BEFORE @AfterReturning/@AfterThrowing complete (ordering within same aspect)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * EXECUTION ORDER (within the SAME @Aspect class):
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   @Before         runs first
 *   [method runs]
 *   @AfterReturning or @AfterThrowing  runs next
 *   @After          runs last
 *
 * Note: The ordering between @After and @AfterReturning/@AfterThrowing within the
 * same aspect may vary slightly between Spring versions. Use @Around for guaranteed
 * control of execution order.
 *
 */
@Aspect
@Component
@Order(3)
class AfterAdviceDemoAspect {

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 1: Clear ThreadLocal context (like a cleanup 'finally' block)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Clears any thread-local state set during the request, ALWAYS.
     * Even if the method throws an exception, this cleanup runs.
     *
     * In production: this would clear MDC (log correlation IDs), RequestContext, etc.
     */
    @After("within(com.learning.springboot.chapter09.EmployeeService)")
    public void cleanupAfterEmployeeServiceMethod(JoinPoint jp) {
        System.out.printf("[After] ✔ Completed (or failed) — method: %s — cleanup done%n",
                jp.getSignature().getName());
        // In real code: MDC.clear(); RequestContextHolder.resetRequestAttributes(); etc.
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 2: Record method completion in a completion log
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Records that a @Loggable method has completed (whether success or failure).
     *
     * @After doesn't tell you if it succeeded — just that it finished.
     * Use @AfterReturning to distinguish success, @AfterThrowing for failure.
     */
    @After("@annotation(com.learning.springboot.chapter09.Loggable) && within(com.learning.springboot.chapter09.*)")
    public void recordMethodCompletion(JoinPoint jp) {
        String methodFullName = jp.getSignature().getDeclaringType().getSimpleName()
                + "." + jp.getSignature().getName();
        System.out.printf("[After] ← Exited: %s at %s%n", methodFullName, LocalDateTime.now());
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 4: @AfterReturning — RUNS AFTER SUCCESSFUL RETURN
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                   @AfterReturning  EXPLAINED IN DEPTH                        ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @AfterReturning runs ONLY when the method returns successfully (no exception).
 * You can optionally capture the return value using the `returning` attribute.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 SYNTAX:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   @AfterReturning("pointcut")                         ← no return value
 *   @AfterReturning(pointcut="p()", returning="result") ← capture return as 'result'
 *
 * The `returning` name MUST match a parameter name in the advice method.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 USE CASES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  ✅ Log the return value alongside the method name
 *  ✅ Post-process returned data (e.g., add extra fields, filter sensitive data)
 *  ✅ Cache the result (after it's successfully computed)
 *  ✅ Trigger notifications on successful operations
 *  ✅ Update statistics (success counter, last-success timestamp)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ IMPORTANT: Return value is READ-ONLY in @AfterReturning
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * You CAN read the return value. You CANNOT change it.
 * If you modify the object that returnObject points to (mutable object), the change
 * is visible to the caller — but you can't REPLACE the return value with a different object.
 *
 * To REPLACE a return value, use @Around advice instead.
 *
 */
@Aspect
@Component
@Order(2)
class AfterReturningAdviceDemoAspect {

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 1: Log the return value
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Logs the return value of @Auditable methods.
     *
     * returning="result" — binds the method's return value to the 'result' parameter.
     * The type Object accepts ANY return type.
     *
     * This runs ONLY on successful return. If createEmployee() throws, this doesn't run.
     */
    @AfterReturning(
        pointcut = "@annotation(com.learning.springboot.chapter09.Auditable) && within(com.learning.springboot.chapter09.*)",
        returning = "result"   // ← binds return value to the 'result' param below
    )
    public void logReturnValue(JoinPoint jp, Object result) {
        String method = jp.getSignature().getDeclaringType().getSimpleName()
                + "." + jp.getSignature().getName();

        // result can be null (for void methods), a primitive, or an object
        String resultStr = (result == null) ? "void/null" : result.toString();

        System.out.printf("[AfterReturning] ✅ %s → returned: %s%n", method, resultStr);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 2: Return type filtering — only act on specific return types
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Specifically captures Employee return values and logs their ID.
     *
     * By declaring 'returning="employee"' as type Employee (not Object),
     * this advice ONLY fires when the return value IS an Employee.
     * If the method returns null, List, or any other type → this advice is SKIPPED.
     *
     * This type filtering is very useful for type-specific post-processing.
     */
    @AfterReturning(
        pointcut = "within(com.learning.springboot.chapter09.EmployeeService)",
        returning = "employee"   // ← Only fires when return type is Employee
    )
    public void afterEmployeeReturned(JoinPoint jp, Employee employee) {
        // This advice only fires for methods returning an Employee object
        System.out.printf("[AfterReturning:Employee] ✅ Method '%s' returned Employee{id=%d, name='%s %s'}%n",
                jp.getSignature().getName(), employee.getId(),
                employee.getFirstName(), employee.getLastName());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 3: Success counter — count successful invocations
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Counts every successful method call in the chapter09 services.
     * In production: send this to Micrometer, Prometheus, etc.
     */
    @AfterReturning("within(com.learning.springboot.chapter09.*Service)")
    public void countSuccess(JoinPoint jp) {
        String key = jp.getSignature().getDeclaringType().getSimpleName()
                + "." + jp.getSignature().getName();
        AopMetrics.incrementSuccess(key);
        System.out.printf("[AfterReturning] 📊 Success count for '%s': %d%n",
                key, AopMetrics.getSuccessCount(key));
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 5: @AfterThrowing — RUNS WHEN EXCEPTION IS THROWN
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                   @AfterThrowing  EXPLAINED IN DEPTH                         ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @AfterThrowing runs ONLY when the matched method throws an exception.
 * The EXCEPTION STILL PROPAGATES to the caller — @AfterThrowing cannot swallow it.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 SYNTAX:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   @AfterThrowing("pointcut")                           ← no exception capture
 *   @AfterThrowing(pointcut="p()", throwing="ex")        ← capture exception as 'ex'
 *
 * The `throwing` name MUST match a parameter name in the advice method.
 * By declaring 'throwing' as a specific exception type, you can FILTER — only that
 * exception type (or its subclasses) will trigger this advice.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 USE CASES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  ✅ Log exception with full stack trace + method context
 *  ✅ Send error alerts (email, Slack, PagerDuty) on specific exceptions
 *  ✅ Update failure metrics/counters
 *  ✅ Record failed operations in audit log
 *  ✅ Filter: only react to specific exception types
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ IMPORTANT: Exception ALWAYS propagates — @AfterThrowing CANNOT swallow it
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * You can observe the exception. You CANNOT suppress it.
 * If you need to swallow exceptions or throw a different exception, use @Around.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * Exception Type Filtering:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   throwing = "ex" as Throwable     → fires for ALL exceptions
 *   throwing = "ex" as RuntimeException → fires ONLY for RuntimeException or subclasses
 *   throwing = "ex" as IllegalArgumentException → fires ONLY for IllegalArgumentException
 *
 * Declaring a narrower type causes Spring to FILTER — only matching exceptions run the advice.
 *
 */
@Aspect
@Component
@Order(2)
class AfterThrowingAdviceDemoAspect {

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 1: Log ALL exceptions with method context
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Catches any exception from any chapter09 service method and logs full context.
     *
     * throwing="ex" as Throwable → captures ANY exception type.
     * The method context (class, method, args) helps diagnose production errors.
     */
    @AfterThrowing(
        pointcut = "within(com.learning.springboot.chapter09.*Service)",
        throwing = "ex"    // ← binds the thrown exception to 'ex'
    )
    public void logAnyException(JoinPoint jp, Throwable ex) {
        String method = jp.getSignature().getDeclaringType().getSimpleName()
                + "." + jp.getSignature().getName();

        System.err.printf("[AfterThrowing] ❌ EXCEPTION in '%s'%n", method);
        System.err.printf("   Exception type: %s%n", ex.getClass().getSimpleName());
        System.err.printf("   Message:        %s%n", ex.getMessage());
        System.err.printf("   Method args:    %s%n", Arrays.toString(jp.getArgs()));
        // In production: log.error("...", ex);  alertingService.send(...)
        // EXCEPTION STILL PROPAGATES — we only observe it here
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 2: Exception TYPE filtering — only act on specific exception types
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Only fires for IllegalArgumentException (bad input).
     *
     * By typing the 'ex' parameter as IllegalArgumentException, Spring filters out
     * all other exception types — this advice ONLY runs for bad-argument errors.
     *
     * Use case: Track input validation errors separately from system errors.
     */
    @AfterThrowing(
        pointcut = "within(com.learning.springboot.chapter09.*Service)",
        throwing = "ex"
    )
    public void onIllegalArgument(JoinPoint jp, IllegalArgumentException ex) {
        // Only fires for IllegalArgumentException — filtered by type
        System.err.printf("[AfterThrowing:BadInput] 📋 Input validation error in '%s': %s%n",
                jp.getSignature().getName(), ex.getMessage());
        AopMetrics.incrementFailure("BAD_INPUT." + jp.getSignature().getName());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 3: Security exception handling
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Captures SecurityExceptions thrown by the @RequiresRole security advice.
     * Logs the access denial for security auditing purposes.
     *
     * Note: The SecurityException was thrown by BeforeAdviceDemoAspect.checkRoleAccess()
     * when the role check failed. This advice captures it for additional alerting.
     */
    @AfterThrowing(
        pointcut = "@annotation(com.learning.springboot.chapter09.RequiresRole) && within(com.learning.springboot.chapter09.*)",
        throwing = "ex"
    )
    public void onSecurityException(JoinPoint jp, SecurityException ex) {
        System.err.printf("[AfterThrowing:Security] 🔒 ACCESS DENIED — method: '%s', reason: %s%n",
                jp.getSignature().getName(), ex.getMessage());
        // In production: securityAuditLog.record(ex), alerting.sendSecurityAlert(...)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 4: Update failure metrics
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Counts every failed method call in chapter09 services.
     * Runs for any exception type (Throwable).
     */
    @AfterThrowing(
        pointcut = "within(com.learning.springboot.chapter09.*Service)",
        throwing = "t"
    )
    public void countFailure(JoinPoint jp, Throwable t) {
        String key = jp.getSignature().getDeclaringType().getSimpleName()
                + "." + jp.getSignature().getName();
        AopMetrics.incrementFailure(key);
        System.err.printf("[AfterThrowing] 📊 Failure count for '%s': %d%n",
                key, AopMetrics.getFailureCount(key));
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  HELPER: Simulated CurrentUserContext (Thread-Local user/role simulation)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * Simulates Spring Security's SecurityContextHolder with a simple ThreadLocal.
 * In production: use SecurityContextHolder.getContext().getAuthentication().
 *
 * This lets the security aspect (@Before with @RequiresRole) check the current user's role.
 * Tests can set the role before calling service methods to simulate different users.
 */
class CurrentUserContext {

    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<String> USER = new ThreadLocal<>();

    public static void setRole(String role) { ROLE.set(role); }
    public static String getRole() {
        String r = ROLE.get();
        return r != null ? r : "ANONYMOUS";
    }

    public static void setUser(String username) { USER.set(username); }
    public static String getUser() {
        String u = USER.get();
        return u != null ? u : "anonymous";
    }

    /** Call after each request/test to prevent memory leaks in thread pools. */
    public static void clear() {
        ROLE.remove();
        USER.remove();
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  HELPER: Simple in-memory metrics counter for AOP demonstrations
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * Simple in-memory metrics store for AOP demos.
 * Tracks success and failure counts per method.
 * In production: use Micrometer with Prometheus/Grafana.
 */
class AopMetrics {

    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong>
        SUCCESS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong>
        FAILURE = new java.util.concurrent.ConcurrentHashMap<>();

    public static void incrementSuccess(String key) {
        SUCCESS.computeIfAbsent(key, k -> new java.util.concurrent.atomic.AtomicLong(0)).incrementAndGet();
    }

    public static void incrementFailure(String key) {
        FAILURE.computeIfAbsent(key, k -> new java.util.concurrent.atomic.AtomicLong(0)).incrementAndGet();
    }

    public static long getSuccessCount(String key) {
        return SUCCESS.getOrDefault(key, new java.util.concurrent.atomic.AtomicLong(0)).get();
    }

    public static long getFailureCount(String key) {
        return FAILURE.getOrDefault(key, new java.util.concurrent.atomic.AtomicLong(0)).get();
    }

    public static void reset() {
        SUCCESS.clear();
        FAILURE.clear();
    }
}

class Example02BeforeAfterAdvice {
    // Intentionally empty — documentation class
}

