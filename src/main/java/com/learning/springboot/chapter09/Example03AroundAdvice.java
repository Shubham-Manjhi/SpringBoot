package com.learning.springboot.chapter09;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║       EXAMPLE 03: @Around — THE MOST POWERFUL ADVICE TYPE                           ║
 * ║       ProceedingJoinPoint · Timing · Caching · Retry · Return Modification          ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example03AroundAdvice.java
 * Purpose:     Master @Around advice — the all-in-one advice that wraps a method
 *              completely. Learn every capability: timing, caching, retry, argument
 *              modification, return value modification, and exception wrapping.
 * Difficulty:  ⭐⭐⭐⭐ Intermediate–Advanced
 * Time:        40 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * WHY @Around IS THE MOST POWERFUL ADVICE:
 *
 *  @Before can only observe (and abort via exception).
 *  @AfterReturning can only observe the return value (read-only).
 *  @AfterThrowing can only observe exceptions (can't swallow them).
 *  @After can only clean up.
 *
 *  @Around can do ALL of these PLUS:
 *    ✅ PREVENT the method from running entirely
 *    ✅ MODIFY the method arguments before calling it
 *    ✅ REPLACE the return value with something different
 *    ✅ SWALLOW the exception (or rethrow a different one)
 *    ✅ Call the method MULTIPLE TIMES (e.g., retry logic)
 *    ✅ Measure EXACT execution time (before + after in the same method)
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * THE CORE REQUIREMENT: You MUST call pjp.proceed()
 *
 *   @Around("myPointcut()")
 *   public Object aroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
 *       // ← "before" logic here
 *       Object result = pjp.proceed();  // ← CALL THE REAL METHOD
 *       // ← "after" logic here
 *       return result;                  // ← RETURN THE RESULT
 *   }
 *
 *   If you forget pjp.proceed() → the real method NEVER runs (silent bug!).
 *   If you forget return result → the caller gets null (or default value).
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 1: ProceedingJoinPoint API
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║           ProceedingJoinPoint API — COMPLETE REFERENCE                       ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ProceedingJoinPoint EXTENDS JoinPoint and adds ONE critical method:
 *
 *   proceed()
 *     → Calls the REAL METHOD with its ORIGINAL arguments
 *     → Returns the method's return value as Object
 *     → Throws whatever the method throws
 *
 *   proceed(Object[] newArgs)
 *     → Calls the REAL METHOD with MODIFIED arguments
 *     → Use this to sanitize, transform, or inject arguments
 *     → The array must match the method's parameter count and types
 *
 * ALL JoinPoint methods are also available:
 *   pjp.getSignature().getName()            → method name
 *   pjp.getArgs()                           → original arguments
 *   pjp.getTarget()                         → real bean
 *   ((MethodSignature) pjp.getSignature()).getMethod() → java.lang.reflect.Method
 *
 * RETURN VALUE:
 *   proceed() returns Object. For void methods it returns null.
 *   You must return the result from the @Around advice method too.
 *   (Unless you intentionally want to return something else / block the call.)
 *
 * EXCEPTION HANDLING:
 *   proceed() throws Throwable.
 *   Your @Around method signature must declare throws Throwable.
 *   Catch it to swallow/transform; rethrow to let it propagate.
 *
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 2: @Around PATTERN 1 — PERFORMANCE TIMING (The Classic Use Case)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * Measures the exact execution time of every @PerformanceMonitor method.
 * Logs a WARNING if execution exceeds the configured SLA threshold.
 *
 * This is @Around's most common production use case — accurate timing requires
 * recording the time BEFORE and AFTER the method, which only @Around supports.
 */
@Aspect
@Component
@Order(10)  // ← Run this aspect AFTER security (Order 1) and logging (Order 2)
class PerformanceTimingAspect {

    /**
     * Wraps every @PerformanceMonitor method to measure execution time.
     *
     * pjp.proceed() runs the real method and returns its result.
     * We record time before and after proceed() to get exact execution duration.
     *
     * The @PerformanceMonitor annotation is bound to the 'monitor' param
     * so we can read its slaMillis and operation attributes.
     */
    @Around("@annotation(monitor)")
    public Object measureExecutionTime(ProceedingJoinPoint pjp, PerformanceMonitor monitor)
            throws Throwable {

        String operation = monitor.operation().isEmpty()
                ? pjp.getSignature().getName()
                : monitor.operation();
        long slaMillis = monitor.slaMillis();

        // ← Record START time
        long startNanos = System.nanoTime();
        Object result;

        try {
            result = pjp.proceed();  // ← Call the real method
        } finally {
            // ← Record END time (in finally so we measure even on exception)
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

            if (elapsedMillis > slaMillis) {
                // SLA BREACH — log as warning
                System.err.printf("[PerfTiming] ⚠️  SLA BREACH: '%s' took %dms (SLA: %dms)%n",
                        operation, elapsedMillis, slaMillis);
            } else {
                System.out.printf("[PerfTiming] ✅ '%s' completed in %dms (SLA: %dms)%n",
                        operation, elapsedMillis, slaMillis);
            }

            // In production: meterRegistry.timer("method.execution", "operation", operation)
            //                             .record(elapsedMillis, TimeUnit.MILLISECONDS);
        }

        return result;   // ← ALWAYS return the result!
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 3: @Around PATTERN 2 — RETRY ON EXCEPTION
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * Implements automatic retry for @AutoRetry annotated methods.
 *
 * The retry pattern is ONLY possible with @Around because:
 *   → You call pjp.proceed() multiple times (once per attempt)
 *   → You catch the exception in the @Around method
 *   → On final failure, you re-throw the exception
 *
 * @AfterThrowing cannot do this — it can observe exceptions but not retry the method.
 */
@Aspect
@Component
@Order(5)  // ← Run retry AFTER security (Order 1) but BEFORE others
class AutoRetryAspect {

    /**
     * Retries the annotated method up to maxAttempts times on exception.
     *
     * The @AutoRetry annotation attributes control:
     *   maxAttempts → how many times to try (including the first attempt)
     *   delayMs     → how long to wait between retries (milliseconds)
     *   retryOn     → which exception types to retry on
     *
     * Example:
     *   @AutoRetry(maxAttempts = 3, delayMs = 100)
     *   public TimeEntry clockIn(Long employeeId) { ... }
     *
     *   First call fails → wait 100ms → second call fails → wait 100ms → third call
     *   If third call also fails → exception propagates to caller
     */
    @Around("@annotation(retry)")
    public Object retryOnException(ProceedingJoinPoint pjp, AutoRetry retry) throws Throwable {

        int maxAttempts  = retry.maxAttempts();
        long delayMs     = retry.delayMs();
        Class<? extends Throwable>[] retryOn = retry.retryOn();
        String methodName = pjp.getSignature().getName();

        Throwable lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    System.out.printf("[AutoRetry] 🔄 Retrying '%s' — attempt %d/%d (waited %dms)%n",
                            methodName, attempt, maxAttempts, delayMs);
                }

                Object result = pjp.proceed();  // ← Call the real method

                if (attempt > 1) {
                    System.out.printf("[AutoRetry] ✅ '%s' succeeded on attempt %d/%d%n",
                            methodName, attempt, maxAttempts);
                }
                return result;  // ← Success — return result immediately

            } catch (Throwable ex) {
                // Check if this exception type should trigger a retry
                if (!shouldRetry(ex, retryOn)) {
                    System.err.printf("[AutoRetry] ❌ Non-retryable exception in '%s': %s — not retrying%n",
                            methodName, ex.getClass().getSimpleName());
                    throw ex;  // ← Non-retryable: rethrow immediately
                }

                lastException = ex;
                System.err.printf("[AutoRetry] ⚠️  Attempt %d/%d failed for '%s': %s%n",
                        attempt, maxAttempts, methodName, ex.getMessage());

                if (attempt < maxAttempts) {
                    // Wait before next attempt (exponential backoff or fixed delay)
                    try {
                        Thread.sleep(delayMs * attempt); // ← Simple linear backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                }
            }
        }

        // All attempts exhausted — rethrow the last exception
        System.err.printf("[AutoRetry] ❌ All %d attempts failed for '%s'%n", maxAttempts, methodName);
        throw lastException;
    }

    /**
     * Checks if the thrown exception matches any of the retryable exception types.
     */
    private boolean shouldRetry(Throwable ex, Class<? extends Throwable>[] retryOn) {
        for (Class<? extends Throwable> retryType : retryOn) {
            if (retryType.isInstance(ex)) {
                return true;
            }
        }
        return false;
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 4: @Around PATTERN 3 — SIMPLE IN-MEMORY CACHING
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * A simple caching aspect powered by @Around.
 *
 * Why @Around for caching?
 *   → Check the cache BEFORE calling the method (if hit → skip method, return cached)
 *   → If cache miss → call the method AND store the result in cache
 *   → Both operations need to happen in the same advice method
 *
 * @Before + @AfterReturning cannot do this together cleanly —
 * @Before would need to signal @AfterReturning, requiring messy ThreadLocals.
 * @Around does both cleanly in a single method.
 *
 * NOTE: This is a demo implementation. In production, use Spring Cache
 *       (@Cacheable, @CacheEvict) which is more robust and configurable.
 */
@Aspect
@Component
@Order(15)
class SimpleMethodCacheAspect {

    // Simple LRU-ish cache: method-key → cached result
    private final Map<String, CachedValue> cache = new ConcurrentHashMap<>();
    private static final long DEFAULT_TTL_MILLIS = 30_000L;  // 30 seconds

    /**
     * Caches the result of any method in the chapter09 package named "find*".
     * Returns the cached result if available and not expired.
     *
     * Pattern: Only cache "find*" methods (read operations) — never write operations.
     * Read operations are safe to cache. Write operations must NEVER be cached.
     */
    @Around("execution(* com.learning.springboot.chapter09..*find*(..))")
    public Object cacheResult(ProceedingJoinPoint pjp) throws Throwable {

        // Build a unique cache key from class + method + args
        String cacheKey = buildCacheKey(pjp);

        // Step 1: Check the cache
        CachedValue cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            System.out.printf("[Cache] 🎯 HIT for '%s' — returning cached value%n",
                    pjp.getSignature().getName());
            return cached.value;  // ← Return cached result WITHOUT calling real method
        }

        if (cached != null) {
            System.out.printf("[Cache] ⏰ EXPIRED for '%s' — reloading%n",
                    pjp.getSignature().getName());
        } else {
            System.out.printf("[Cache] 💨 MISS for '%s' — calling real method%n",
                    pjp.getSignature().getName());
        }

        // Step 2: Cache miss — call the real method
        Object result = pjp.proceed();

        // Step 3: Store result in cache (only if non-null)
        if (result != null) {
            cache.put(cacheKey, new CachedValue(result, DEFAULT_TTL_MILLIS));
            System.out.printf("[Cache] 💾 Cached result for '%s'%n", pjp.getSignature().getName());
        }

        return result;
    }

    /** Evict cache entries for a given target class (call on write operations). */
    public void evictCache(Class<?> targetClass) {
        cache.keySet().removeIf(key -> key.startsWith(targetClass.getSimpleName() + "."));
        System.out.println("[Cache] 🗑️  Evicted cache for: " + targetClass.getSimpleName());
    }

    private String buildCacheKey(ProceedingJoinPoint pjp) {
        return pjp.getSignature().getDeclaringType().getSimpleName()
                + "." + pjp.getSignature().getName()
                + "(" + Arrays.toString(pjp.getArgs()) + ")";
    }

    private static class CachedValue {
        final Object value;
        final long expiresAtMillis;

        CachedValue(Object value, long ttlMillis) {
            this.value = value;
            this.expiresAtMillis = System.currentTimeMillis() + ttlMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 5: @Around PATTERN 4 — ARGUMENT MODIFICATION
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * Modifies method arguments BEFORE calling the real method.
 * Uses pjp.proceed(newArgs) instead of pjp.proceed() to pass modified args.
 *
 * Use cases:
 *   ✅ Sanitize/trim string inputs
 *   ✅ Normalize email addresses to lowercase
 *   ✅ Set default values for null arguments
 *   ✅ Encrypt sensitive data before storage
 */
@Aspect
@Component
@Order(7)
class ArgumentSanitizationAspect {

    /**
     * Sanitizes String arguments before they reach service methods.
     * Trims whitespace and normalizes email addresses to lowercase.
     *
     * pjp.proceed(newArgs) calls the method with the MODIFIED arguments,
     * not the original ones.
     */
    @Around("execution(* com.learning.springboot.chapter09.EmployeeService.createEmployee(..))")
    public Object sanitizeCreateEmployeeArgs(ProceedingJoinPoint pjp) throws Throwable {

        Object[] originalArgs = pjp.getArgs();
        Object[] sanitizedArgs = new Object[originalArgs.length];

        for (int i = 0; i < originalArgs.length; i++) {
            if (originalArgs[i] instanceof String str) {
                // Trim whitespace from all String arguments
                String trimmed = str.trim();
                // Normalize email (argument index 2 = email) to lowercase
                sanitizedArgs[i] = (i == 2) ? trimmed.toLowerCase() : trimmed;
            } else {
                sanitizedArgs[i] = originalArgs[i];  // Non-string: pass through unchanged
            }
        }

        System.out.printf("[ArgSanitize] Sanitized args: %s → %s%n",
                Arrays.toString(originalArgs), Arrays.toString(sanitizedArgs));

        // Call the method with SANITIZED arguments (not the originals!)
        return pjp.proceed(sanitizedArgs);
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 6: @Around PATTERN 5 — EXCEPTION WRAPPING
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * Catches low-level exceptions and wraps them in domain-specific exceptions.
 *
 * Why wrap exceptions?
 *   ✅ Decouple your API from implementation details (JPA exceptions, JDBC, etc.)
 *   ✅ Provide meaningful error messages to callers
 *   ✅ Add context (which service, which operation, what went wrong)
 *   ✅ Prevent implementation details from leaking through the API
 *
 * Example:
 *   DataAccessException (JPA)     → EmployeeServiceException("Data access error in...")
 *   NoSuchElementException (util) → EmployeeNotFoundException("Employee 42 not found")
 */
@Aspect
@Component
@Order(20)
class ExceptionWrappingAspect {

    @Around("within(com.learning.springboot.chapter09.ExpenseService)")
    public Object wrapExpenseServiceExceptions(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return pjp.proceed();
        } catch (NoSuchElementException ex) {
            // Wrap NoSuchElementException into a domain-specific exception
            throw new ExpenseNotFoundException(
                "Expense not found during '" + pjp.getSignature().getName() + "': " + ex.getMessage());
        } catch (SecurityException ex) {
            // Let security exceptions pass through unchanged (don't wrap them)
            throw ex;
        } catch (RuntimeException ex) {
            // Wrap unexpected runtime exceptions with operation context
            throw new ExpenseServiceException(
                "Unexpected error in '" + pjp.getSignature().getName() + "': " + ex.getMessage(), ex);
        }
    }
}

/** Domain-specific exception for expense-related errors */
class ExpenseNotFoundException extends RuntimeException {
    public ExpenseNotFoundException(String message) { super(message); }
}

/** Domain-specific exception for unexpected expense service errors */
class ExpenseServiceException extends RuntimeException {
    public ExpenseServiceException(String message, Throwable cause) { super(message, cause); }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 7: @Around PATTERN 6 — METHOD GATING / FEATURE TOGGLE
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * Prevents methods from running based on a condition (feature toggle).
 *
 * Example use cases:
 *   ✅ A/B testing — route 50% of users to new feature
 *   ✅ Feature flags — disable certain features in certain environments
 *   ✅ Maintenance mode — block all writes during DB migration
 *   ✅ Canary deployment — enable new code only for specific users
 *
 * The key: NOT calling pjp.proceed() prevents the method from running.
 * We return a default value instead.
 */
@Aspect
@Component
@Order(2)  // ← Run very early — gate before other advice runs
class FeatureToggleAspect {

    // In production: load from @ConfigurationProperties, LaunchDarkly, etc.
    private static final boolean EXPENSE_APPROVAL_ENABLED = true;

    @Around("execution(* com.learning.springboot.chapter09.ExpenseService.approveExpense(..))" +
            " || execution(* com.learning.springboot.chapter09.ExpenseService.rejectExpense(..))")
    public Object gateExpenseApproval(ProceedingJoinPoint pjp) throws Throwable {

        if (!EXPENSE_APPROVAL_ENABLED) {
            // Feature is DISABLED — don't call the real method
            System.out.println("[FeatureToggle] ⛔ Expense approval feature is DISABLED");
            // Return a default value appropriate for the return type
            // (for ExpenseReport methods, we'd throw or return null)
            throw new UnsupportedOperationException("Expense approval is currently disabled");
        }

        // Feature is ENABLED — proceed normally
        System.out.println("[FeatureToggle] ✅ Expense approval feature is ENABLED — proceeding");
        return pjp.proceed();
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 8: @Around COMPLETE PATTERN — Logging + Timing + Exception (Production)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * A complete production-grade @Around advice combining:
 *   - Structured request logging (entry + exit)
 *   - Execution time measurement
 *   - Exception logging with context
 *
 * This demonstrates how @Around combines what would require THREE separate advice methods
 * (@Before + @AfterReturning + @AfterThrowing) into ONE clean, cohesive method.
 *
 * @Order(30) — runs last so it wraps ALL the other advice methods.
 * Its "before" section runs after all lower-@Order "before" sections.
 * Its "after" section runs before all lower-@Order "after" sections.
 *
 *   Timeline: [Order30.before] [Order10.before] [Order5.before] [METHOD] [Order5.after] [Order10.after] [Order30.after]
 */
@Aspect
@Component
@Order(30)
class ComprehensiveLoggingAspect {

    @Pointcut("within(com.learning.springboot.chapter09.*Service) && execution(public * *(..))")
    private void publicServiceMethods() {}

    /**
     * Wraps ALL public service methods with structured entry/exit logging + timing.
     *
     * This single @Around advice replaces what would otherwise be:
     *   @Before     → log entry + args
     *   @AfterReturning → log success + return value + time
     *   @AfterThrowing  → log failure + exception + time
     */
    @Around("publicServiceMethods()")
    public Object comprehensiveLog(ProceedingJoinPoint pjp) throws Throwable {

        // Gather method context
        MethodSignature sig    = (MethodSignature) pjp.getSignature();
        String className       = sig.getDeclaringType().getSimpleName();
        String methodName      = sig.getName();
        String[] paramNames    = sig.getParameterNames();
        Object[] paramValues   = pjp.getArgs();

        // Build readable parameter string: "firstName=Alice, salary=85000"
        StringBuilder params = new StringBuilder();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if (i > 0) params.append(", ");
                params.append(paramNames[i]).append("=").append(paramValues[i]);
            }
        } else {
            params.append(Arrays.toString(paramValues));
        }

        // ──── BEFORE PROCEED: Log method entry ────
        System.out.printf("[ComprehensiveLog] ──► %s.%s(%s)%n", className, methodName, params);
        long startNanos = System.nanoTime();

        try {
            // ──── CALL THE REAL METHOD ────
            Object result = pjp.proceed();

            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

            // ──── AFTER PROCEED (SUCCESS): Log exit ────
            String returnStr = (result == null) ? "void" : result.toString();
            // Truncate very long return values (e.g., large lists)
            if (returnStr.length() > 100) returnStr = returnStr.substring(0, 97) + "...";

            System.out.printf("[ComprehensiveLog] ◄── %s.%s ✅ returned=%s | %dms%n",
                    className, methodName, returnStr, elapsed);

            return result;

        } catch (Throwable ex) {
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

            // ──── AFTER PROCEED (FAILURE): Log exception ────
            System.err.printf("[ComprehensiveLog] ◄── %s.%s ❌ threw=%s('%s') | %dms%n",
                    className, methodName, ex.getClass().getSimpleName(), ex.getMessage(), elapsed);

            throw ex;  // ← ALWAYS rethrow (unless you intentionally want to swallow)
        }
    }
}

class Example03AroundAdvice {
    // Intentionally empty — documentation class
}

