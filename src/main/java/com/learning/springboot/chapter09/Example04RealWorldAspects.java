package com.learning.springboot.chapter09;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║       EXAMPLE 04: REAL-WORLD ASPECTS — PRODUCTION PATTERNS                          ║
 * ║       LoggingAspect · PerformanceAspect · SecurityAspect · AuditAspect              ║
 * ║       RateLimitingAspect · TransactionAwareAspect                                   ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example04RealWorldAspects.java
 * Purpose:     See how Spring AOP is used in production applications.
 *              Each aspect here solves a real cross-cutting concern with
 *              patterns you can use directly in your own projects.
 * Difficulty:  ⭐⭐⭐⭐ Advanced
 * Time:        40 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * ASPECTS IN THIS FILE:
 *
 *   1. StructuredLoggingAspect   — Production logging: entry/exit/error with MDC-style context
 *   2. PerformanceMonitorAspect  — SLA monitoring, slow query detection, percentile tracking
 *   3. AuditTrailAspect          — Who did what, when, with what data
 *   4. SecurityAuditAspect       — Record all access denials for compliance
 *   5. RateLimitingAspect        — Per-user/per-method request throttling
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  ASPECT 1: STRUCTURED LOGGING — Production Request/Response Logging
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                  STRUCTURED LOGGING ASPECT (Production Pattern)              ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * Production logging has three requirements beyond basic println:
 *
 *   1. CORRELATION ID — Every related log line shares the same request ID
 *      (so you can grep all logs for request-123 in a distributed system)
 *
 *   2. STRUCTURED FORMAT — JSON or key=value pairs for log aggregation tools
 *      (Elasticsearch, Splunk, Datadog can parse and query structured logs)
 *
 *   3. SENSITIVE DATA MASKING — Never log passwords, PII, credit cards
 *
 * In production: Replace System.out with SLF4J (log.info/warn/error)
 *                and use MDC for correlation IDs.
 *
 * MDC (Mapped Diagnostic Context) is a ThreadLocal map in SLF4J/Logback:
 *   MDC.put("requestId", UUID.randomUUID().toString())
 *   log.info("method called")  → every log line automatically includes the requestId
 *   MDC.clear()                → clean up after request
 *
 */
@Aspect
@Component
@Order(1)  // ← Logging runs FIRST (outermost wrapper)
class StructuredLoggingAspect {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * Logs entry and exit for every @Loggable method.
     * Uses @Around to measure exact duration and log both entry + exit in one place.
     *
     * Sensitive parameter names are masked (password, token, secret, cvv, ssn).
     */
    @Around("@annotation(loggable) && within(com.learning.springboot.chapter09.*)")
    public Object structuredLog(ProceedingJoinPoint pjp, Loggable loggable) throws Throwable {

        MethodSignature sig   = (MethodSignature) pjp.getSignature();
        String operation      = loggable.value().isEmpty() ? sig.getName() : loggable.value();
        String requestId      = RequestContext.getRequestId();   // Simulated correlation ID
        String user           = CurrentUserContext.getUser();

        // Mask sensitive parameters
        String safeArgs = maskSensitiveArgs(sig.getParameterNames(), pjp.getArgs());

        System.out.printf("[LOG] %s | req=%s | user=%-12s | ▶ %s.%s(%s)%n",
                LocalDateTime.now().format(FMT), requestId, user,
                sig.getDeclaringType().getSimpleName(), sig.getName(), safeArgs);

        long startNanos = System.nanoTime();

        try {
            Object result = pjp.proceed();

            long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            System.out.printf("[LOG] %s | req=%s | user=%-12s | ◀ %s ✅ duration=%dms%n",
                    LocalDateTime.now().format(FMT), requestId, user, operation, millis);
            return result;

        } catch (Throwable ex) {
            long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            System.err.printf("[LOG] %s | req=%s | user=%-12s | ◀ %s ❌ error=%s duration=%dms%n",
                    LocalDateTime.now().format(FMT), requestId, user, operation,
                    ex.getClass().getSimpleName() + "(" + ex.getMessage() + ")", millis);
            throw ex;
        }
    }

    /** Returns a string of args with sensitive values replaced by "***". */
    private String maskSensitiveArgs(String[] names, Object[] values) {
        if (names == null || names.length == 0) return Arrays.toString(values);

        Set<String> sensitive = Set.of("password", "token", "secret", "cvv", "ssn", "pin");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            if (i > 0) sb.append(", ");
            if (sensitive.stream().anyMatch(names[i].toLowerCase()::contains)) {
                sb.append(names[i]).append("=***");
            } else {
                sb.append(names[i]).append("=").append(values[i]);
            }
        }
        return sb.toString();
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  ASPECT 2: PERFORMANCE MONITORING — SLA Tracking & Slow Query Detection
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║               PERFORMANCE MONITORING ASPECT (Production Pattern)             ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * Tracks performance metrics for @PerformanceMonitor annotated methods:
 *   - Execution time per call
 *   - Minimum, maximum, average execution time
 *   - SLA breach detection (warn when execution exceeds threshold)
 *   - Call count and total time
 *
 * In production: Replace printouts with Micrometer → Prometheus → Grafana.
 *
 *   Counter  → total call count, error count
 *   Timer    → execution time with histogram (p50, p95, p99 percentiles)
 *   Gauge    → current metric value
 *
 * Micrometer example:
 *   meterRegistry.timer("service.method", "method", methodName)
 *                .record(elapsed, TimeUnit.MILLISECONDS);
 *
 */
@Aspect
@Component
@Order(2)
class PerformanceMonitorAspect {

    // Stores statistics per method
    private final Map<String, MethodStats> statsMap = new ConcurrentHashMap<>();

    @Around("@annotation(monitor) && within(com.learning.springboot.chapter09.*)")
    public Object trackPerformance(ProceedingJoinPoint pjp, PerformanceMonitor monitor)
            throws Throwable {

        String key    = pjp.getSignature().getDeclaringType().getSimpleName()
                      + "." + pjp.getSignature().getName();
        long startNs  = System.nanoTime();

        try {
            Object result = pjp.proceed();
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

            // Record success metric
            MethodStats stats = statsMap.computeIfAbsent(key, k -> new MethodStats());
            stats.recordSuccess(elapsedMs);

            // Check SLA
            if (elapsedMs > monitor.slaMillis()) {
                System.err.printf("[PerfMonitor] ⚠️  SLA BREACH: '%s' — %dms > %dms SLA | calls=%d avg=%.0fms%n",
                        key, elapsedMs, monitor.slaMillis(), stats.callCount(), stats.avgMillis());
            } else {
                System.out.printf("[PerfMonitor] ✅ '%s' — %dms (SLA: %dms) | calls=%d avg=%.0fms%n",
                        key, elapsedMs, monitor.slaMillis(), stats.callCount(), stats.avgMillis());
            }

            return result;

        } catch (Throwable ex) {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
            statsMap.computeIfAbsent(key, k -> new MethodStats()).recordFailure();
            System.err.printf("[PerfMonitor] ❌ '%s' FAILED after %dms%n", key, elapsedMs);
            throw ex;
        }
    }

    /** Get statistics for a given method (useful in tests and admin endpoints). */
    public MethodStats getStats(String key) {
        return statsMap.getOrDefault(key, new MethodStats());
    }

    /** Simple statistics accumulator. In production: use Micrometer's Timer. */
    static class MethodStats {
        private final AtomicLong callCount    = new AtomicLong(0);
        private final AtomicLong totalMillis  = new AtomicLong(0);
        private final AtomicLong minMillis    = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxMillis    = new AtomicLong(0);
        private final AtomicLong errorCount   = new AtomicLong(0);

        void recordSuccess(long millis) {
            callCount.incrementAndGet();
            totalMillis.addAndGet(millis);
            minMillis.updateAndGet(v -> Math.min(v, millis));
            maxMillis.updateAndGet(v -> Math.max(v, millis));
        }

        void recordFailure() { errorCount.incrementAndGet(); }

        long callCount()   { return callCount.get(); }
        long errorCount()  { return errorCount.get(); }
        double avgMillis() { return callCount.get() == 0 ? 0 : (double) totalMillis.get() / callCount.get(); }
        long minMillis()   { long v = minMillis.get(); return v == Long.MAX_VALUE ? 0 : v; }
        long maxMillis()   { return maxMillis.get(); }
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  ASPECT 3: AUDIT TRAIL — Recording WHO Did WHAT and WHEN
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                   AUDIT TRAIL ASPECT (Production Pattern)                    ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * Many industries (finance, healthcare, legal) require an AUDIT TRAIL:
 * a tamper-proof record of every significant action in the system.
 *
 * What an audit record contains:
 *   → WHO performed the action (user identity)
 *   → WHAT action was performed (CREATE_EMPLOYEE, UPDATE_SALARY, etc.)
 *   → WHEN (timestamp)
 *   → WHAT resource was affected (Employee, ExpenseReport, etc.)
 *   → WHAT data was changed (before/after values)
 *   → Whether it SUCCEEDED or FAILED
 *
 * This aspect implements the @Auditable annotation from Example01.
 * It produces AuditRecord objects and "persists" them to the audit store.
 * In production: persist to a dedicated audit database/table that is
 *               WRITE-ONLY for the application (append-only log).
 *
 */
@Aspect
@Component
@Order(5)
class AuditTrailAspect {

    // In-memory audit log (in production: JPA repository, Kafka, DynamoDB, etc.)
    private final List<AuditRecord> auditLog = Collections.synchronizedList(new ArrayList<>());

    @Around("@annotation(auditable) && within(com.learning.springboot.chapter09.*)")
    public Object recordAuditTrail(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {

        // Gather context BEFORE the method runs
        String user         = CurrentUserContext.getUser();
        String role         = CurrentUserContext.getRole();
        String requestId    = RequestContext.getRequestId();
        String action       = auditable.action();
        String resourceType = auditable.resourceType();
        LocalDateTime when  = LocalDateTime.now();
        Object[] args       = pjp.getArgs();

        try {
            Object result = pjp.proceed();  // ← Run the real method

            // Gather the return value for audit (if configured)
            String returnSummary = null;
            if (auditable.captureReturn() && result != null) {
                returnSummary = result.toString();
                if (returnSummary.length() > 200) {
                    returnSummary = returnSummary.substring(0, 197) + "...";
                }
            }

            // Create SUCCESS audit record
            AuditRecord record = AuditRecord.success(
                requestId, when, user, role, action, resourceType,
                Arrays.toString(args), returnSummary
            );
            auditLog.add(record);

            System.out.printf("[Audit] ✅ %s | user=%-10s | action=%-25s | resource=%s%n",
                    when.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    user, action, resourceType);

            return result;

        } catch (Throwable ex) {
            // Create FAILURE audit record
            AuditRecord record = AuditRecord.failure(
                requestId, when, user, role, action, resourceType,
                Arrays.toString(args), ex.getClass().getSimpleName() + ": " + ex.getMessage()
            );
            auditLog.add(record);

            System.err.printf("[Audit] ❌ %s | user=%-10s | action=%-25s | error=%s%n",
                    when.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    user, action, ex.getMessage());

            throw ex;  // ← Always rethrow — the audit records the failure but doesn't hide it
        }
    }

    /** Returns all audit records (use for compliance reports, security investigations). */
    public List<AuditRecord> getAuditLog() {
        return Collections.unmodifiableList(auditLog);
    }

    /** Returns audit records for a specific user. */
    public List<AuditRecord> getAuditLogForUser(String user) {
        return auditLog.stream()
                .filter(r -> r.user().equals(user))
                .toList();
    }

    /** Returns audit records for a specific action type. */
    public List<AuditRecord> getAuditLogByAction(String action) {
        return auditLog.stream()
                .filter(r -> r.action().equals(action))
                .toList();
    }
}

/**
 * Represents a single audit event.
 * In production: @Entity persisted to audit_records table.
 */
record AuditRecord(
        String requestId,
        LocalDateTime timestamp,
        String user,
        String role,
        String action,
        String resourceType,
        String inputData,
        String outputData,   // null if captureReturn=false or void method
        String errorMessage, // null if success
        boolean success
) {
    static AuditRecord success(String requestId, LocalDateTime ts, String user, String role,
                                String action, String resourceType, String input, String output) {
        return new AuditRecord(requestId, ts, user, role, action, resourceType, input, output, null, true);
    }

    static AuditRecord failure(String requestId, LocalDateTime ts, String user, String role,
                                String action, String resourceType, String input, String error) {
        return new AuditRecord(requestId, ts, user, role, action, resourceType, input, null, error, false);
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  ASPECT 4: SECURITY AUDIT — Record All Access Denials
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║               SECURITY AUDIT ASPECT (Compliance Pattern)                     ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * Records every security-related event for compliance and intrusion detection.
 * This complements the @Before security check in BeforeAdviceDemoAspect.
 *
 * Events captured:
 *   → Successful access to role-protected methods (INFO)
 *   → Access denial attempts (WARN — potential unauthorized access)
 *   → Repeated denials from same user (ALERT — potential attack)
 *
 */
@Aspect
@Component
@Order(3)
class SecurityAuditAspect {

    // Track denial counts per user for intrusion detection
    private final Map<String, AtomicInteger> denialCounts = new ConcurrentHashMap<>();
    private static final int ALERT_THRESHOLD = 5;

    /**
     * Records both successful access and access denials for @RequiresRole methods.
     *
     * @AfterReturning → method ran successfully (role check passed)
     * @AfterThrowing  → SecurityException was thrown (role check failed)
     */
    @AfterReturning("@annotation(com.learning.springboot.chapter09.RequiresRole) && within(com.learning.springboot.chapter09.*)")
    public void recordSuccessfulAccess(JoinPoint jp) {
        MethodSignature sig = (MethodSignature) jp.getSignature();
        RequiresRole req    = sig.getMethod().getAnnotation(RequiresRole.class);
        String user         = CurrentUserContext.getUser();

        System.out.printf("[SecurityAudit] ✅ ALLOWED: user=%-12s method=%s.%s (role=%s)%n",
                user,
                jp.getSignature().getDeclaringType().getSimpleName(),
                jp.getSignature().getName(),
                req != null ? req.value() : "?");

        // Reset denial counter on successful access (legitimate user)
        denialCounts.remove(user);
    }

    @AfterThrowing(
        pointcut = "@annotation(com.learning.springboot.chapter09.RequiresRole) && within(com.learning.springboot.chapter09.*)",
        throwing = "ex"
    )
    public void recordAccessDenial(JoinPoint jp, SecurityException ex) {
        String user   = CurrentUserContext.getUser();
        String method = jp.getSignature().getDeclaringType().getSimpleName()
                      + "." + jp.getSignature().getName();

        // Increment denial count for this user
        int count = denialCounts
                .computeIfAbsent(user, u -> new AtomicInteger(0))
                .incrementAndGet();

        System.err.printf("[SecurityAudit] 🔒 DENIED: user=%-12s method=%-40s denials=%d%n",
                user, method, count);

        // Alert if denial threshold exceeded (potential brute force / privilege escalation)
        if (count >= ALERT_THRESHOLD) {
            System.err.printf("[SecurityAudit] 🚨 ALERT: user='%s' has %d consecutive denials " +
                    "— potential unauthorized access attempt!%n", user, count);
            // In production: send to SIEM, trigger incident management, temporarily ban user
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  ASPECT 5: RATE LIMITING — Per-User Request Throttling
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║               RATE LIMITING ASPECT (API Protection Pattern)                  ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * Limits how many times a user can call specific methods per time window.
 * Protects against:
 *   → API abuse (a user calling search 1000 times/second)
 *   → Brute force attacks (trying many passwords)
 *   → Resource exhaustion DoS attacks
 *   → Fair usage enforcement in multi-tenant systems
 *
 * Algorithm: Sliding Window Counter
 *   - Track call timestamps for each user+method combination
 *   - Count calls in the last 60 seconds
 *   - If count >= limit → reject the request
 *
 * In production: Use Redis for distributed rate limiting (works across multiple instances).
 * Bucket4j library provides production-grade rate limiting with Spring AOP.
 *
 * This demo uses simple in-memory counters (single-instance only).
 *
 */
@Aspect
@Component
@Order(4)
class RateLimitingAspect {

    // Stores call timestamps per "user:method" key
    private final Map<String, List<Long>> callHistory = new ConcurrentHashMap<>();

    // Rate limits: max calls per 60-second window
    private static final int DEFAULT_LIMIT  = 100;
    private static final int SEARCH_LIMIT   = 20;   // Searches are expensive
    private static final long WINDOW_MILLIS = 60_000L; // 60 second window

    /**
     * Rate-limits all expense service methods (financial operations need throttling).
     */
    @Around("within(com.learning.springboot.chapter09.ExpenseService)")
    public Object rateLimitExpenseOperations(ProceedingJoinPoint pjp) throws Throwable {

        String user      = CurrentUserContext.getUser();
        String method    = pjp.getSignature().getName();
        String key       = user + ":" + method;
        int limit        = DEFAULT_LIMIT;
        long now         = System.currentTimeMillis();

        // Get or create call history for this user+method
        List<Long> history = callHistory.computeIfAbsent(key, k ->
                Collections.synchronizedList(new ArrayList<>()));

        // Remove calls outside the time window (sliding window)
        synchronized (history) {
            history.removeIf(callTime -> (now - callTime) > WINDOW_MILLIS);

            if (history.size() >= limit) {
                long oldestCall = history.get(0);
                long resetInMs  = WINDOW_MILLIS - (now - oldestCall);
                System.err.printf("[RateLimit] ⛔ Rate limit exceeded for user='%s' method='%s' " +
                        "(%d/%d calls in 60s). Reset in %dms%n",
                        user, method, history.size(), limit, resetInMs);
                throw new RateLimitExceededException(
                    String.format("Rate limit exceeded: %d/%d calls allowed per minute. " +
                            "Try again in %d seconds.", history.size(), limit, resetInMs / 1000));
            }

            history.add(now);
        }

        System.out.printf("[RateLimit] ✅ user='%s' method='%s' (%d/%d calls in 60s)%n",
                user, method, history.size(), limit);

        return pjp.proceed();
    }
}

/** Thrown when a user exceeds the rate limit. */
class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) { super(message); }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  HELPER: Simulated RequestContext (Correlation ID per request)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * Simulates MDC (Mapped Diagnostic Context) with a simple ThreadLocal correlation ID.
 * In production: use MDC.put("requestId", ...) from SLF4J/Logback.
 *
 * Usage:
 *   RequestContext.newRequest();   // at the start of each request (e.g., in a Filter)
 *   RequestContext.getRequestId(); // in any advice or service method on the same thread
 *   RequestContext.clear();        // at the end of the request
 */
class RequestContext {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();
    private static long requestCounter = 0;

    public static void newRequest() {
        REQUEST_ID.set("req-" + String.format("%04d", ++requestCounter));
    }

    public static String getRequestId() {
        String id = REQUEST_ID.get();
        return id != null ? id : "req-0000";
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}

class Example04RealWorldAspects {
    // Intentionally empty — documentation class
}

