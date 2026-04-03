package com.learning.springboot.chapter09;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║       EXAMPLE 01: @Aspect, @Pointcut, @EnableAspectJAutoProxy IN DEPTH              ║
 * ║       — All Pointcut Designators · Custom Annotations · Domain Services              ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example01AspectAndPointcut.java
 * Purpose:     Master AOP setup and pointcut expressions — the WHAT and WHERE of AOP.
 *              Learn every pointcut designator with practical examples.
 *              Define custom annotations that serve as pointcut markers.
 * Difficulty:  ⭐⭐⭐⭐ Intermediate–Advanced
 * Time:        45 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * WHAT YOU'LL BUILD:
 *   ┌──────────────────────────────────────────────────────────────────────┐
 *   │  Domain Services (Targets)        Aspects (Advisors)                │
 *   │  ─────────────────────────────    ────────────────────────────────  │
 *   │  EmployeeService @Service    ←──── PointcutDemoAspect (this file)  │
 *   │  TimeTrackingService @Service ←─── (Logs ALL methods in ch09.*)    │
 *   │  ExpenseService @Service     ←────                                  │
 *   └──────────────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 1: CUSTOM ANNOTATIONS — POINTCUT MARKERS
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║            CUSTOM ANNOTATIONS AS AOP MARKERS — EXPLAINED                    ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 WHY USE CUSTOM ANNOTATIONS?
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Instead of listing every method in a pointcut expression, you can:
 *   1. Define a custom annotation (e.g., @Loggable, @Auditable)
 *   2. Put that annotation on ONLY the methods you want to advise
 *   3. Write a pointcut: @annotation(Loggable)
 *
 * Benefits:
 *   ✅ Crystal-clear intent — @Loggable on a method says "this gets logged"
 *   ✅ Selective — only the methods you explicitly mark get advised
 *   ✅ Self-documenting — no need to read the pointcut expression
 *   ✅ Type-safe — IDEs understand and autocomplete these annotations
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 REQUIRED ANNOTATION META-ANNOTATIONS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @Target(ElementType.METHOD)    ← Only valid on methods
 * @Retention(RetentionPolicy.RUNTIME) ← MUST be RUNTIME so AOP proxy can read it
 *
 * ⚠️ If you use @Retention(RetentionPolicy.CLASS) or SOURCE, Spring AOP cannot
 * read the annotation at runtime → pointcut @annotation(Loggable) won't match!
 * ALWAYS use RUNTIME for AOP marker annotations.
 *
 */

/**
 * @Loggable — marks a method for detailed entry/exit logging.
 *
 * Usage:
 *   @Loggable
 *   public Employee createEmployee(CreateEmployeeRequest req) { ... }
 *   → LoggingAspect will log the call, arguments, return value, and duration.
 */
@Target(ElementType.METHOD)            // ← Valid on methods only
@Retention(RetentionPolicy.RUNTIME)    // ← MUST be RUNTIME for AOP to detect
@Documented                            // ← Appears in Javadoc
@interface Loggable {
    /**
     * Optional description that appears in the log message.
     * Default: empty (no extra description).
     */
    String value() default "";
}

/**
 * @PerformanceMonitor — marks a method for execution time measurement.
 *
 * Usage:
 *   @PerformanceMonitor(slaMillis = 500)
 *   public List<Employee> searchEmployees(String query) { ... }
 *   → PerformanceAspect logs a WARNING if the method exceeds slaMillis.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface PerformanceMonitor {
    /** SLA threshold in milliseconds. Warn if method exceeds this. Default: 1000ms. */
    long slaMillis() default 1000L;
    /** Description of this performance-critical operation. */
    String operation() default "";
}

/**
 * @Auditable — marks a method that should generate an audit trail entry.
 *
 * Usage:
 *   @Auditable(action = "CREATE_EMPLOYEE", resourceType = "Employee")
 *   public Employee createEmployee(CreateEmployeeRequest req) { ... }
 *   → AuditAspect records: WHO called this, WHEN, with WHAT args, and WHAT returned.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface Auditable {
    /** Business action name for the audit log. */
    String action();
    /** The resource type being operated on. */
    String resourceType() default "";
    /** Whether to include the return value in the audit record. */
    boolean captureReturn() default false;
}

/**
 * @RequiresRole — marks a method that needs a specific role to execute.
 *
 * Usage:
 *   @RequiresRole("HR_MANAGER")
 *   public void terminateEmployee(Long id) { ... }
 *   → SecurityAspect checks the current user has the required role.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface RequiresRole {
    /** The role name required to call this method. */
    String value();
}

/**
 * @AutoRetry — marks a method that should be automatically retried on exception.
 *
 * Usage:
 *   @AutoRetry(maxAttempts = 3, delayMs = 100)
 *   public PayslipResult generatePayslip(Long employeeId) { ... }
 *   → RetryAspect retries the method up to maxAttempts times on RuntimeException.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface AutoRetry {
    /** Maximum number of attempts (including the first). Default: 3. */
    int maxAttempts() default 3;
    /** Delay between retries in milliseconds. Default: 100ms. */
    long delayMs() default 100L;
    /** Exception types to retry on. Default: RuntimeException. */
    Class<? extends Throwable>[] retryOn() default { RuntimeException.class };
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 2: DOMAIN MODEL — TARGETS FOR AOP DEMONSTRATIONS
// ══════════════════════════════════════════════════════════════════════════════════════

/** Represents an employee in the HR system. */
class Employee {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private String role;
    private BigDecimal salary;
    private LocalDateTime hiredAt;

    public Employee() {}

    public Employee(Long id, String firstName, String lastName,
                    String email, String department, BigDecimal salary) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.department = department;
        this.salary = salary;
        this.hiredAt = LocalDateTime.now();
    }

    public Long getId()              { return id; }
    public void setId(Long id)       { this.id = id; }
    public String getFirstName()     { return firstName; }
    public String getLastName()      { return lastName; }
    public String getEmail()         { return email; }
    public String getDepartment()    { return department; }
    public String getRole()          { return role; }
    public BigDecimal getSalary()    { return salary; }
    public LocalDateTime getHiredAt(){ return hiredAt; }
    public void setSalary(BigDecimal s){ this.salary = s; }
    public void setRole(String r)    { this.role = r; }

    @Override
    public String toString() {
        return String.format("Employee{id=%d, name='%s %s', dept='%s', salary=%s}",
                id, firstName, lastName, department, salary);
    }
}

/** Represents a time entry for employee attendance/work tracking. */
class TimeEntry {
    private Long id;
    private Long employeeId;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private String notes;

    public TimeEntry(Long id, Long employeeId, LocalDateTime clockIn) {
        this.id = id;
        this.employeeId = employeeId;
        this.clockIn = clockIn;
    }

    public Long getId()              { return id; }
    public Long getEmployeeId()      { return employeeId; }
    public LocalDateTime getClockIn(){ return clockIn; }
    public LocalDateTime getClockOut(){ return clockOut; }
    public void setClockOut(LocalDateTime t) { this.clockOut = t; }
    public String getNotes()         { return notes; }
    public void setNotes(String n)   { this.notes = n; }
}

/** Represents an expense report submitted by an employee. */
class ExpenseReport {
    private Long id;
    private Long employeeId;
    private String description;
    private BigDecimal amount;
    private String status; // PENDING, APPROVED, REJECTED
    private String category;

    public ExpenseReport(Long id, Long employeeId, String description,
                         BigDecimal amount, String category) {
        this.id = id;
        this.employeeId = employeeId;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.status = "PENDING";
    }

    public Long getId()           { return id; }
    public Long getEmployeeId()   { return employeeId; }
    public String getDescription(){ return description; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus()     { return status; }
    public String getCategory()   { return category; }
    public void setStatus(String s){ this.status = s; }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 3: TARGET SERVICES — BEANS THAT ASPECTS WILL ADVISE
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * EmployeeService — The main service that aspects will be applied to.
 *
 * Notice: This class has ZERO logging, ZERO security checks, ZERO audit code.
 * All cross-cutting concerns are handled by the Aspect classes below.
 *
 * When you @Autowire EmployeeService, you actually get a CGLIB PROXY that
 * transparently applies all the advice defined in the aspect classes.
 */
@Service
class EmployeeService {

    private final Map<Long, Employee> db = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(100);

    public EmployeeService() {
        // Seed some demo employees
        db.put(1L, new Employee(1L, "Alice", "Smith",  "alice@example.com",  "Engineering", new BigDecimal("85000")));
        db.put(2L, new Employee(2L, "Bob",   "Jones",  "bob@example.com",    "Marketing",   new BigDecimal("72000")));
        db.put(3L, new Employee(3L, "Carol", "White",  "carol@example.com",  "HR",          new BigDecimal("68000")));
        idGen.set(4);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Method with @Loggable + @Auditable + @PerformanceMonitor
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Create a new employee.
     *
     * @Loggable            → LoggingAspect will log: call start, args, return value, duration
     * @Auditable(...)      → AuditAspect will record: who created, what data, when
     * @PerformanceMonitor  → PerformanceAspect will warn if this takes > 500ms
     */
    @Loggable("Create new employee in the system")
    @Auditable(action = "CREATE_EMPLOYEE", resourceType = "Employee", captureReturn = true)
    @PerformanceMonitor(slaMillis = 500, operation = "create-employee")
    public Employee createEmployee(String firstName, String lastName, String email,
                                   String department, BigDecimal salary) {
        if (firstName == null || firstName.isBlank())
            throw new IllegalArgumentException("First name is required");
        if (email == null || !email.contains("@"))
            throw new IllegalArgumentException("Valid email is required");

        Long id = idGen.getAndIncrement();
        Employee emp = new Employee(id, firstName, lastName, email, department, salary);
        db.put(id, emp);

        System.out.println("✅ [EmployeeService] Employee created: " + emp);
        return emp;
    }

    /**
     * Get an employee by ID.
     *
     * @Loggable → logs the lookup (useful for debugging "who fetched whom")
     * @PerformanceMonitor → ensure reads are fast (SLA: 100ms)
     */
    @Loggable
    @PerformanceMonitor(slaMillis = 100, operation = "get-employee")
    public Optional<Employee> findById(Long id) {
        System.out.println("🔍 [EmployeeService] Finding employee id=" + id);
        return Optional.ofNullable(db.get(id));
    }

    /**
     * Get all employees.
     *
     * @PerformanceMonitor → large dataset could be slow
     */
    @PerformanceMonitor(slaMillis = 200, operation = "list-employees")
    public List<Employee> findAll() {
        System.out.println("📋 [EmployeeService] Listing all employees");
        return new ArrayList<>(db.values());
    }

    /**
     * Update employee salary — HR manager only.
     *
     * @RequiresRole("HR_MANAGER") → SecurityAspect blocks if caller lacks this role
     * @Auditable                  → Every pay change must be audited
     * @Loggable                   → Log for HR compliance
     */
    @RequiresRole("HR_MANAGER")
    @Auditable(action = "UPDATE_SALARY", resourceType = "Employee", captureReturn = true)
    @Loggable("Salary adjustment — HR compliance logging required")
    public Employee updateSalary(Long id, BigDecimal newSalary) {
        Employee emp = db.get(id);
        if (emp == null) throw new NoSuchElementException("Employee not found: " + id);

        BigDecimal oldSalary = emp.getSalary();
        emp.setSalary(newSalary);
        db.put(id, emp);

        System.out.printf("💰 [EmployeeService] Salary updated for %s %s: %s → %s%n",
                emp.getFirstName(), emp.getLastName(), oldSalary, newSalary);
        return emp;
    }

    /**
     * Delete an employee — Admin only.
     *
     * @RequiresRole("HR_ADMIN") → Stricter role required for deletion
     * @Auditable                → Deletion must always be audited
     */
    @RequiresRole("HR_ADMIN")
    @Auditable(action = "DELETE_EMPLOYEE", resourceType = "Employee")
    public boolean deleteEmployee(Long id) {
        System.out.println("🗑️  [EmployeeService] Deleting employee id=" + id);
        return db.remove(id) != null;
    }
}

/**
 * TimeTrackingService — another service in chapter09 (also advised by aspects).
 * Demonstrates that aspects apply to ALL services in the package — not just one.
 */
@Service
class TimeTrackingService {

    private final Map<Long, TimeEntry> entries = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    /**
     * Clock in — starts a new time entry.
     * @AutoRetry: retry if concurrent write conflict occurs
     */
    @Loggable
    @AutoRetry(maxAttempts = 3, delayMs = 50)
    public TimeEntry clockIn(Long employeeId) {
        System.out.println("⏰ [TimeTrackingService] Clock in for employee " + employeeId);
        Long id = idGen.getAndIncrement();
        TimeEntry entry = new TimeEntry(id, employeeId, LocalDateTime.now());
        entries.put(id, entry);
        return entry;
    }

    /**
     * Clock out — closes the active time entry.
     * @Auditable: work hours are payroll data — must be audited
     */
    @Loggable
    @Auditable(action = "CLOCK_OUT", resourceType = "TimeEntry")
    public TimeEntry clockOut(Long entryId, String notes) {
        TimeEntry entry = entries.get(entryId);
        if (entry == null) throw new NoSuchElementException("Time entry not found: " + entryId);

        entry.setClockOut(LocalDateTime.now());
        entry.setNotes(notes);

        System.out.printf("⏰ [TimeTrackingService] Clock out: entry=%d%n", entryId);
        return entry;
    }
}

/**
 * ExpenseService — manages employee expense reports.
 * Demonstrates @RequiresRole on approve/reject methods.
 */
@Service
class ExpenseService {

    private final Map<Long, ExpenseReport> reports = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Loggable("Submit new expense report")
    @Auditable(action = "SUBMIT_EXPENSE", resourceType = "ExpenseReport", captureReturn = true)
    public ExpenseReport submitExpense(Long employeeId, String description,
                                       BigDecimal amount, String category) {
        Long id = idGen.getAndIncrement();
        ExpenseReport report = new ExpenseReport(id, employeeId, description, amount, category);
        reports.put(id, report);
        System.out.printf("💸 [ExpenseService] Expense submitted: %s — $%s%n", description, amount);
        return report;
    }

    @RequiresRole("FINANCE_MANAGER")
    @Auditable(action = "APPROVE_EXPENSE", resourceType = "ExpenseReport", captureReturn = true)
    @Loggable
    public ExpenseReport approveExpense(Long reportId) {
        ExpenseReport report = reports.get(reportId);
        if (report == null) throw new NoSuchElementException("Expense report not found: " + reportId);
        report.setStatus("APPROVED");
        System.out.println("✅ [ExpenseService] Expense approved: " + reportId);
        return report;
    }

    @RequiresRole("FINANCE_MANAGER")
    @Auditable(action = "REJECT_EXPENSE", resourceType = "ExpenseReport")
    public ExpenseReport rejectExpense(Long reportId, String reason) {
        ExpenseReport report = reports.get(reportId);
        if (report == null) throw new NoSuchElementException("Expense report not found: " + reportId);
        report.setStatus("REJECTED");
        System.out.printf("❌ [ExpenseService] Expense rejected: %d (reason: %s)%n", reportId, reason);
        return report;
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 4: @EnableAspectJAutoProxy CONFIGURATION
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║             @EnableAspectJAutoProxy  EXPLAINED IN DEPTH                      ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @EnableAspectJAutoProxy activates Spring's AnnotationAwareAspectJAutoProxyCreator.
 * This is a BeanPostProcessor that:
 *   1. Scans all beans for @Aspect annotations
 *   2. Builds advice chains for each detected aspect
 *   3. Wraps target beans in AOP proxies (CGLIB or JDK Dynamic)
 *   4. Returns the PROXY instead of the real bean when other beans @Autowire the target
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔧 ATTRIBUTES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   proxyTargetClass (default: true in Spring Boot):
 *     true  → Always use CGLIB proxy (subclasses the target class)
 *     false → Use JDK Dynamic Proxy if target implements interface; CGLIB otherwise
 *
 *   exposeProxy (default: false):
 *     true  → Expose the current proxy via AopContext.currentProxy()
 *             (NEEDED to fix the self-invocation problem)
 *     false → Don't expose (slight performance improvement)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🚀 IN SPRING BOOT:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Spring Boot AUTO-CONFIGURES @EnableAspectJAutoProxy via AopAutoConfiguration
 * when spring-boot-starter-aop is on the classpath.
 * You do NOT need to add @EnableAspectJAutoProxy explicitly in Spring Boot.
 *
 * However, you would add it explicitly to:
 *   → Change proxyTargetClass to false (use JDK Dynamic Proxy)
 *   → Enable exposeProxy=true (fix self-invocation)
 *   → Override Spring Boot's default AOP configuration
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚙️ configuration in application.yml (Spring Boot alternative):
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   spring:
 *     aop:
 *       proxy-target-class: true   # default: true
 *       auto: true                 # enable AOP auto-configuration: default: true
 *
 */
@Configuration
@EnableAspectJAutoProxy(
    proxyTargetClass = true,   // ← Use CGLIB proxy (default in Spring Boot)
                               //   Pros: works without interfaces, more flexible
                               //   Cons: target class must NOT be final
    exposeProxy = false        // ← Don't expose proxy via AopContext (set true only
                               //   when you need to fix self-invocation issues)
)
class AopConfiguration {
    /*
     * This @Configuration class is only needed if you want to EXPLICITLY configure
     * AOP settings. In a typical Spring Boot app, you can DELETE this class entirely
     * — AopAutoConfiguration handles it automatically.
     *
     * If you ever need self-invocation support (calling this.advicedMethod() from
     * within the same bean), change exposeProxy = true and then call:
     *
     *   EmployeeService proxy = (EmployeeService) AopContext.currentProxy();
     *   proxy.createEmployee(...);   // ← goes through proxy, advice fires
     *
     * vs this.createEmployee(...);  // ← bypasses proxy, advice does NOT fire!
     */
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 5: @Aspect + @Pointcut — ALL EXPRESSION TYPES
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║             @Aspect  EXPLAINED IN DEPTH                                      ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @Aspect marks a class as an ASPECT — a module that contains advice methods.
 * By itself, @Aspect is just a marker from the AspectJ library.
 *
 * IMPORTANT: @Aspect does NOT make the class a Spring bean.
 * You MUST also add @Component (or @Service/@Repository) so Spring manages it.
 * Without @Component, Spring won't find the aspect, and no advice runs!
 *
 *   @Aspect                    ← marks as aspect (AspectJ)
 *   @Component                 ← makes it a Spring bean (Spring)
 *   class MyAspect { ... }
 *
 * Alternatively, you can declare the aspect as a @Bean in a @Configuration class.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * @Pointcut EXPLAINED:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @Pointcut declares a REUSABLE POINTCUT EXPRESSION on an empty method.
 * The method name becomes the "name" of the pointcut, callable from advice annotations.
 *
 *   @Pointcut("execution(* com.example.service.*.*(..))")
 *   private void allServiceMethods() {}   ← empty body, name = "allServiceMethods"
 *
 *   @Before("allServiceMethods()")        ← reference the pointcut by its method name
 *   public void beforeAdvice(JoinPoint jp) { ... }
 *
 * Why use @Pointcut methods?
 *   ✅ Reuse the same expression in multiple advice annotations
 *   ✅ Name the expression so it's self-documenting
 *   ✅ Combine named pointcuts with &&, ||, !
 *   ✅ Override in subclasses (if you extend the aspect class)
 *
 */
@Aspect
@Component
class PointcutExpressionsDemoAspect {

    // ─────────────────────────────────────────────────────────────────────────────
    // DESIGNATOR 1: execution() — MATCH BY METHOD SIGNATURE
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * execution() is the MOST COMMONLY USED pointcut designator.
     * Syntax: execution([modifiers] return-type [class.]method(params) [throws])
     *
     * Pattern: *  → any single segment
     *          .. → any number of parts / any params
     *
     * EXAMPLES:
     *   execution(* *(..))
     *     → ALL methods anywhere in the application (too broad!)
     *
     *   execution(public * com.learning.springboot.chapter09.*.*(..))
     *     → All PUBLIC methods in the chapter09 package (one level deep)
     *
     *   execution(* com.learning.springboot.chapter09..*.*(..))
     *     → All methods in chapter09 AND all sub-packages (recursive — note ..)
     *
     *   execution(* com.learning.springboot.chapter09.EmployeeService.*(..))
     *     → All methods in EmployeeService specifically
     *
     *   execution(* com.learning.springboot.chapter09.EmployeeService.findById(..))
     *     → Only findById() in EmployeeService
     *
     *   execution(* find*(..))
     *     → Any method whose name starts with "find" — anywhere!
     *
     *   execution(* *(String, ..))
     *     → Methods taking String as the FIRST argument (any others after)
     *
     *   execution(!private * com.learning.springboot.chapter09..*(..))
     *     → All non-private methods in chapter09
     */
    @Pointcut("execution(* com.learning.springboot.chapter09.EmployeeService.*(..))")
    public void allEmployeeServiceMethods() {}
    // ↑ Named pointcut — callable from any advice as: allEmployeeServiceMethods()

    @Pointcut("execution(* com.learning.springboot.chapter09..*Service.*(..))")
    public void allChapter09ServiceMethods() {}
    // ↑ Matches any class ending in "Service" in chapter09 (EmployeeService, TimeTrackingService, etc.)

    @Pointcut("execution(public * com.learning.springboot.chapter09..*(..))")
    public void allPublicChapter09Methods() {}
    // ↑ All PUBLIC methods in chapter09 package and sub-packages

    // ─────────────────────────────────────────────────────────────────────────────
    // DESIGNATOR 2: within() — MATCH ALL METHODS IN A PACKAGE/CLASS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * within() matches all methods WITHIN a type or package.
     * Simpler than execution() — no need to specify return type or method name.
     *
     *   within(com.example.service.*)         → all beans in com.example.service package
     *   within(com.example..*)                → all beans in com.example and sub-packages
     *   within(com.example.EmployeeService)   → all methods in EmployeeService class
     *
     * Difference from execution():
     *   execution(* com.example.service.*.*(..)) → same effect as within(com.example.service.*)
     *   within() is shorter and more readable for package-level matching.
     */
    @Pointcut("within(com.learning.springboot.chapter09.*)")
    public void withinChapter09Package() {}
    // ↑ All methods in any class directly in the chapter09 package

    @Pointcut("within(com.learning.springboot.chapter09..*)")
    public void withinChapter09Tree() {}
    // ↑ All methods in chapter09 and ALL sub-packages (recursive)

    // ─────────────────────────────────────────────────────────────────────────────
    // DESIGNATOR 3: @annotation() — MATCH METHODS WITH A SPECIFIC ANNOTATION
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * @annotation() matches methods that are annotated with the given annotation.
     * This is SELECTIVE — only methods you explicitly mark with the annotation.
     *
     *   @annotation(Loggable)         → methods with @Loggable
     *   @annotation(Auditable)        → methods with @Auditable
     *   @annotation(Transactional)    → methods with @Transactional
     *
     * WHY USE @annotation pointcuts?
     *   → Opt-in behavior: only annotated methods get the advice
     *   → Self-documenting: the annotation makes the intent clear
     *   → Fine-grained: you control exactly which methods are advised
     */
    @Pointcut("@annotation(com.learning.springboot.chapter09.Loggable)")
    public void loggableMethods() {}
    // ↑ Only methods annotated with @Loggable

    @Pointcut("@annotation(com.learning.springboot.chapter09.Auditable)")
    public void auditableMethods() {}
    // ↑ Only methods annotated with @Auditable

    @Pointcut("@annotation(com.learning.springboot.chapter09.PerformanceMonitor)")
    public void monitoredMethods() {}
    // ↑ Only methods annotated with @PerformanceMonitor

    @Pointcut("@annotation(com.learning.springboot.chapter09.RequiresRole)")
    public void securedMethods() {}
    // ↑ Only methods annotated with @RequiresRole

    // ─────────────────────────────────────────────────────────────────────────────
    // DESIGNATOR 4: @within() — MATCH ALL METHODS IN AN ANNOTATED CLASS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * @within() matches ALL methods in classes that bear a specific annotation.
     * Applies to the CLASS level — no need to annotate each method.
     *
     *   @within(org.springframework.stereotype.Service)
     *     → All methods in every @Service class
     *
     *   @within(org.springframework.stereotype.Repository)
     *     → All methods in every @Repository class
     *
     * Difference from @annotation():
     *   @annotation(Loggable)     → METHODS annotated with @Loggable
     *   @within(Loggable)         → methods in CLASSES annotated with @Loggable
     */
    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void withinAnyServiceClass() {}
    // ↑ Every method in every @Service bean in the application (very broad!)

    // ─────────────────────────────────────────────────────────────────────────────
    // DESIGNATOR 5: bean() — MATCH BY SPRING BEAN NAME
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * bean() matches methods on a specific Spring bean, identified by its name.
     * Bean names default to the class name with first letter lowercase.
     *
     *   bean(employeeService)      → the bean named "employeeService"
     *   bean(*Service)             → any bean whose name ends with "Service"
     *   bean(employee*)            → any bean whose name starts with "employee"
     *
     * NOTE: bean() is a SPRING-SPECIFIC designator (not in AspectJ).
     * It only works when using Spring AOP — not with pure AspectJ.
     */
    @Pointcut("bean(employeeService)")
    public void employeeServiceBean() {}
    // ↑ Only the bean named "employeeService"

    @Pointcut("bean(*Service)")
    public void anyServiceBean() {}
    // ↑ Any bean whose name ends with "Service"

    // ─────────────────────────────────────────────────────────────────────────────
    // DESIGNATOR 6: args() — MATCH BY METHOD ARGUMENT TYPES
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * args() matches methods with SPECIFIC ARGUMENT TYPES.
     * Also lets you BIND the arguments to advice method parameters.
     *
     *   args(String)         → methods with exactly ONE String argument
     *   args(String, ..)     → methods with String as first arg (any others after)
     *   args(.., Long)       → methods ending with a Long argument
     *   args(Long, BigDecimal) → exactly two args: Long and BigDecimal
     *
     * When binding: args(id) → the argument named "id" is bound to the advice param
     */
    @Pointcut("execution(* com.learning.springboot.chapter09..*(Long, ..)) && args(id, ..)")
    public void methodsWithLongId(Long id) {}
    // ↑ Methods taking a Long as first arg, with the Long value bound to param 'id'

    // ─────────────────────────────────────────────────────────────────────────────
    // DESIGNATOR 7: target() — MATCH BY TARGET OBJECT TYPE
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * target() matches when the TARGET object (the real bean, not the proxy)
     * is an instance of the given type.
     *
     *   target(EmployeeService)  → only when target is an EmployeeService instance
     *   target(java.io.Serializable) → only when target implements Serializable
     *
     * Useful when you have multiple implementations of an interface and want to
     * advise only a specific implementation.
     */
    @Pointcut("target(com.learning.springboot.chapter09.EmployeeService)")
    public void targetIsEmployeeService() {}

    // ─────────────────────────────────────────────────────────────────────────────
    // COMBINING POINTCUTS — OPERATORS && || !
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Combine pointcuts with &&, ||, ! to build precise expressions.
     *
     * &&  → AND: both expressions must match
     * ||  → OR:  either expression can match
     * !   → NOT: negate the expression
     */

    /**
     * Matches @Loggable methods that are ALSO inside the chapter09 package.
     * This is better than just @annotation(Loggable) because it restricts
     * to chapter09, avoiding accidental logging of other chapters' @Loggable methods.
     */
    @Pointcut("withinChapter09Package() && loggableMethods()")
    public void chapter09LoggableMethods() {}

    /**
     * Matches all chapter09 methods EXCEPT those in TimeTrackingService.
     * Useful if you want to exclude certain beans from an aspect.
     */
    @Pointcut("withinChapter09Tree() && !bean(timeTrackingService)")
    public void chapter09ExcludingTimeTracking() {}

    /**
     * Matches any method that's either @Loggable OR @Auditable in chapter09.
     * Either annotation triggers the advice.
     */
    @Pointcut("(loggableMethods() || auditableMethods()) && withinChapter09Package()")
    public void chapter09LoggableOrAuditable() {}

    // ─────────────────────────────────────────────────────────────────────────────
    // DEMO ADVICE: Uses the pointcuts defined above
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * A simple @Before advice demonstrating pointcut reuse.
     * Uses the named pointcut chapter09LoggableMethods() — which itself
     * combines withinChapter09Package() && loggableMethods().
     */
    @Before("chapter09LoggableMethods()")
    public void demoBeforeLoggable(JoinPoint jp) {
        MethodSignature sig = (MethodSignature) jp.getSignature();
        Loggable loggable = sig.getMethod().getAnnotation(Loggable.class);
        String desc = (loggable != null && !loggable.value().isEmpty())
                ? loggable.value() : sig.getName();

        System.out.printf("[PointcutDemo] @Before %-40s | args=%s%n",
                desc, Arrays.toString(jp.getArgs()));
    }
}

class Example01AspectAndPointcut {
    // Intentionally empty — documentation class
}

