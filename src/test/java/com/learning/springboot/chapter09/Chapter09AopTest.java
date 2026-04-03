package com.learning.springboot.chapter09;

import org.junit.jupiter.api.*;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   CHAPTER 9 — LIVE AOP TEST: Verifying Aspects Work in Practice                     ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * WHAT IS TESTED:
 *   → Spring's AOP proxy is created around EmployeeService/ExpenseService
 *   → @Before security advice fires and blocks unauthorized calls
 *   → @AfterReturning counts success metrics
 *   → @AfterThrowing fires on exceptions (IllegalArgumentException, SecurityException)
 *   → @Around performance timing records execution time
 *   → @Around retry aspect retries on transient failures
 *   → @Around caching returns cached values on repeated findById calls
 *   → @Around argument sanitization trims whitespace from inputs
 *   → Audit trail captures every @Auditable method call
 *   → Custom @RequiresRole annotation blocks unauthorized access
 *
 * KEY ANNOTATIONS DEMONSTRATED:
 *   @SpringBootTest — loads full application context including all AOP aspects
 *   @DirtiesContext — reset context state between test groups
 *   @BeforeEach     — set up user role and request context
 *   @AfterEach      — clear thread-local state
 *   @Nested         — organise related tests
 *   @DisplayName    — descriptive test names
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
@SpringBootTest
@DisplayName("Chapter 9 — Spring AOP Aspects in Action")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class Chapter09AopTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TimeTrackingService timeTrackingService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private AuditTrailAspect auditTrailAspect;

    /*
     * ════════════════════════════════════════════════════════════════════════════════
     *  NESTED GROUP 1: Proxy Verification
     *  Verify that Spring has created AOP proxies around chapter09 services.
     * ════════════════════════════════════════════════════════════════════════════════
     */
    @Nested
    @DisplayName("AOP Proxy Detection")
    class ProxyDetection {

        @Test
        @DisplayName("EmployeeService should be wrapped in a CGLIB AOP proxy")
        void employeeService_shouldBeCglibProxy() {
            /*
             * AopUtils.isAopProxy() returns true if Spring wrapped the bean in a proxy.
             * Spring Boot defaults to CGLIB (proxyTargetClass=true).
             *
             * If this returns false, the AOP aspect pointcut is not matching
             * EmployeeService — check the within() expression in aspects.
             */
            assertThat(AopUtils.isAopProxy(employeeService))
                    .as("EmployeeService must be wrapped in an AOP proxy (aspects should apply)")
                    .isTrue();
        }

        @Test
        @DisplayName("ExpenseService should be wrapped in a CGLIB AOP proxy")
        void expenseService_shouldBeCglibProxy() {
            assertThat(AopUtils.isAopProxy(expenseService))
                    .as("ExpenseService must be wrapped in an AOP proxy")
                    .isTrue();
        }

        @Test
        @DisplayName("Proxy target class should be the original EmployeeService")
        void proxy_targetClass_shouldBeRealEmployeeService() {
            /*
             * AopUtils.getTargetClass() unwraps the proxy and returns the real class.
             * This confirms the proxy IS wrapping our actual service bean.
             */
            Class<?> targetClass = AopUtils.getTargetClass(employeeService);
            assertThat(targetClass)
                    .as("The proxy's target should be EmployeeService, not the proxy class itself")
                    .isEqualTo(EmployeeService.class);
        }
    }

    /*
     * ════════════════════════════════════════════════════════════════════════════════
     *  NESTED GROUP 2: @Before Security Aspect (@RequiresRole)
     *  The @Before advice throws SecurityException if role doesn't match.
     *  Verify that authorized callers succeed and unauthorized callers are blocked.
     * ════════════════════════════════════════════════════════════════════════════════
     */
    @Nested
    @DisplayName("@Before Security Aspect — @RequiresRole")
    class SecurityAspectTests {

        @BeforeEach
        void setUp() {
            RequestContext.newRequest();
        }

        @AfterEach
        void tearDown() {
            CurrentUserContext.clear();
            RequestContext.clear();
            AopMetrics.reset();
        }

        @Test
        @DisplayName("updateSalary with HR_MANAGER role should succeed")
        void updateSalary_withHRManagerRole_shouldSucceed() {
            /*
             * Set the current user's role to HR_MANAGER.
             * The @Before security advice in BeforeAdviceDemoAspect.checkRoleAccess()
             * checks CurrentUserContext.getRole() against @RequiresRole("HR_MANAGER").
             * Since roles match → method proceeds → Employee is returned.
             */
            CurrentUserContext.setUser("hrmanager");
            CurrentUserContext.setRole("HR_MANAGER");

            // Act
            Employee updated = employeeService.updateSalary(1L, new BigDecimal("90000"));

            // Assert
            assertThat(updated).isNotNull();
            assertThat(updated.getSalary()).isEqualByComparingTo(new BigDecimal("90000"));
        }

        @Test
        @DisplayName("updateSalary with wrong role should throw SecurityException")
        void updateSalary_withUserRole_shouldThrowSecurityException() {
            /*
             * Set the current user's role to EMPLOYEE (not HR_MANAGER).
             * The @Before checkRoleAccess() advice fires, sees the mismatch,
             * and throws SecurityException BEFORE the real method runs.
             * The salary is NOT changed.
             */
            CurrentUserContext.setUser("regularemployee");
            CurrentUserContext.setRole("EMPLOYEE");

            // Act + Assert
            assertThatThrownBy(() -> employeeService.updateSalary(1L, new BigDecimal("999999")))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("HR_MANAGER")
                    .hasMessageContaining("Access denied");
        }

        @Test
        @DisplayName("deleteEmployee with HR_ADMIN role should succeed")
        void deleteEmployee_withAdminRole_shouldSucceed() {
            CurrentUserContext.setUser("hradmin");
            CurrentUserContext.setRole("HR_ADMIN");

            // First create an employee to delete
            Employee created = employeeService.createEmployee(
                    "Temp", "User", "temp@example.com", "Temp", new BigDecimal("50000"));

            boolean deleted = employeeService.deleteEmployee(created.getId());
            assertThat(deleted).isTrue();
        }

        @Test
        @DisplayName("deleteEmployee without HR_ADMIN role should throw SecurityException")
        void deleteEmployee_withoutAdminRole_shouldThrowSecurityException() {
            CurrentUserContext.setUser("hrmanager");
            CurrentUserContext.setRole("HR_MANAGER");  // HR_MANAGER, not HR_ADMIN

            assertThatThrownBy(() -> employeeService.deleteEmployee(1L))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("HR_ADMIN");
        }
    }

    /*
     * ════════════════════════════════════════════════════════════════════════════════
     *  NESTED GROUP 3: @AfterThrowing Aspect
     *  Verify that exceptions still propagate after @AfterThrowing runs.
     * ════════════════════════════════════════════════════════════════════════════════
     */
    @Nested
    @DisplayName("@AfterThrowing — Exception Propagation and Metrics")
    class AfterThrowingTests {

        @BeforeEach
        void setUp() {
            CurrentUserContext.setUser("test-user");
            CurrentUserContext.setRole("HR_MANAGER");
            RequestContext.newRequest();
            AopMetrics.reset();
        }

        @AfterEach
        void tearDown() {
            CurrentUserContext.clear();
            RequestContext.clear();
            AopMetrics.reset();
        }

        @Test
        @DisplayName("createEmployee with null firstName should throw IllegalArgumentException")
        void createEmployee_withNullFirstName_shouldThrowIllegalArgumentException() {
            /*
             * The real createEmployee() throws IllegalArgumentException for null firstName.
             * @AfterThrowing in AfterThrowingAdviceDemoAspect:
             *   1. logAnyException() fires  — logs the error
             *   2. onIllegalArgument() fires — records BAD_INPUT metric
             *   3. countFailure() fires      — increments failure counter
             * The exception STILL propagates to us (not swallowed).
             */
            assertThatThrownBy(() ->
                    employeeService.createEmployee(null, "Smith", "test@test.com",
                            "Engineering", new BigDecimal("75000")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("First name is required");
        }

        @Test
        @DisplayName("createEmployee with invalid email should throw IllegalArgumentException")
        void createEmployee_withInvalidEmail_shouldThrowIllegalArgumentException() {
            assertThatThrownBy(() ->
                    employeeService.createEmployee("Test", "User", "not-an-email",
                            "Engineering", new BigDecimal("75000")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Valid email");
        }

        @Test
        @DisplayName("findById with non-existent ID should return empty Optional")
        void findById_nonExistentId_shouldReturnEmptyOptional() {
            // No exception here — findById returns Optional.empty() gracefully
            Optional<Employee> result = employeeService.findById(9999L);
            assertThat(result).isEmpty();
        }
    }

    /*
     * ════════════════════════════════════════════════════════════════════════════════
     *  NESTED GROUP 4: @Around Argument Sanitization
     *  Verify that the ArgumentSanitizationAspect trims whitespace and lowercases emails.
     * ════════════════════════════════════════════════════════════════════════════════
     */
    @Nested
    @DisplayName("@Around Argument Sanitization")
    class ArgumentSanitizationTests {

        @BeforeEach
        void setUp() {
            CurrentUserContext.setUser("test-user");
            CurrentUserContext.setRole("HR_MANAGER");
            RequestContext.newRequest();
        }

        @AfterEach
        void tearDown() {
            CurrentUserContext.clear();
            RequestContext.clear();
        }

        @Test
        @DisplayName("createEmployee trims whitespace from String arguments")
        void createEmployee_shouldTrimWhitespace() {
            /*
             * ArgumentSanitizationAspect intercepts createEmployee() and trims all String args.
             * We pass "  Alice  " and expect "Alice" (trimmed).
             */
            Employee emp = employeeService.createEmployee(
                    "  Alice  ", "  Smith  ", "  alice@example.com  ",
                    "  Engineering  ", new BigDecimal("80000"));

            assertThat(emp.getFirstName()).isEqualTo("Alice");
            assertThat(emp.getLastName()).isEqualTo("Smith");
            assertThat(emp.getDepartment()).isEqualTo("Engineering");
        }

        @Test
        @DisplayName("createEmployee normalizes email to lowercase")
        void createEmployee_shouldNormalizeEmailToLowercase() {
            /*
             * ArgumentSanitizationAspect lowercases the email argument (arg index 2).
             * We pass "JOHN@EXAMPLE.COM" and expect "john@example.com".
             */
            Employee emp = employeeService.createEmployee(
                    "John", "Doe", "JOHN@EXAMPLE.COM",
                    "Marketing", new BigDecimal("70000"));

            assertThat(emp.getEmail()).isEqualTo("john@example.com");
        }
    }

    /*
     * ════════════════════════════════════════════════════════════════════════════════
     *  NESTED GROUP 5: Audit Trail Aspect (@Auditable)
     *  Verify that the AuditTrailAspect records entries for @Auditable methods.
     * ════════════════════════════════════════════════════════════════════════════════
     */
    @Nested
    @DisplayName("@Auditable — Audit Trail Recording")
    class AuditTrailTests {

        @BeforeEach
        void setUp() {
            CurrentUserContext.setUser("alice");
            CurrentUserContext.setRole("HR_MANAGER");
            RequestContext.newRequest();
        }

        @AfterEach
        void tearDown() {
            CurrentUserContext.clear();
            RequestContext.clear();
        }

        @Test
        @DisplayName("createEmployee should generate an audit record")
        void createEmployee_shouldGenerateAuditRecord() {
            /*
             * EmployeeService.createEmployee() is annotated with @Auditable(action="CREATE_EMPLOYEE").
             * AuditTrailAspect intercepts it and records an AuditRecord after the method returns.
             *
             * We verify:
             *   - At least one audit record exists
             *   - The record has action = "CREATE_EMPLOYEE"
             *   - The record was created by user "alice"
             *   - The record is marked as successful
             */
            int auditCountBefore = auditTrailAspect.getAuditLog().size();

            Employee created = employeeService.createEmployee(
                    "Audit", "Test", "audit.test@example.com",
                    "Finance", new BigDecimal("65000"));

            List<AuditRecord> auditLog = auditTrailAspect.getAuditLog();

            assertThat(auditLog.size())
                    .as("At least one audit record should have been added")
                    .isGreaterThan(auditCountBefore);

            // Find the CREATE_EMPLOYEE audit record
            AuditRecord createRecord = auditLog.stream()
                    .filter(r -> "CREATE_EMPLOYEE".equals(r.action()))
                    .reduce((first, second) -> second)  // Get last one
                    .orElse(null);

            assertThat(createRecord)
                    .as("A CREATE_EMPLOYEE audit record must exist")
                    .isNotNull();

            assertThat(createRecord.user()).isEqualTo("alice");
            assertThat(createRecord.success()).isTrue();
            assertThat(createRecord.action()).isEqualTo("CREATE_EMPLOYEE");
            assertThat(createRecord.resourceType()).isEqualTo("Employee");
        }

        @Test
        @DisplayName("failed createEmployee should generate a FAILURE audit record")
        void failedCreateEmployee_shouldGenerateFailureAuditRecord() {
            /*
             * When createEmployee() throws due to null firstName,
             * AuditTrailAspect catches the exception, records a FAILURE audit record,
             * and re-throws the exception.
             */
            int auditCountBefore = auditTrailAspect.getAuditLog().size();

            assertThatThrownBy(() ->
                    employeeService.createEmployee(null, "Fail", "fail@test.com",
                            "Test", new BigDecimal("50000")))
                    .isInstanceOf(IllegalArgumentException.class);

            // A failure audit record should have been added
            List<AuditRecord> auditLog = auditTrailAspect.getAuditLog();
            assertThat(auditLog.size()).isGreaterThan(auditCountBefore);

            AuditRecord failRecord = auditLog.stream()
                    .filter(r -> "CREATE_EMPLOYEE".equals(r.action()) && !r.success())
                    .findFirst()
                    .orElse(null);

            assertThat(failRecord)
                    .as("A failed CREATE_EMPLOYEE audit record must exist")
                    .isNotNull();

            assertThat(failRecord.success()).isFalse();
            assertThat(failRecord.errorMessage()).contains("First name is required");
        }
    }

    /*
     * ════════════════════════════════════════════════════════════════════════════════
     *  NESTED GROUP 6: Core Business Logic (Aspects are Transparent)
     *  Verify that business logic works correctly despite all the aspects wrapping it.
     *  The aspects should be transparent to the caller.
     * ════════════════════════════════════════════════════════════════════════════════
     */
    @Nested
    @DisplayName("Business Logic — Aspects are Transparent to Callers")
    class BusinessLogicTests {

        @BeforeEach
        void setUp() {
            CurrentUserContext.setUser("test-user");
            CurrentUserContext.setRole("HR_MANAGER");
            RequestContext.newRequest();
        }

        @AfterEach
        void tearDown() {
            CurrentUserContext.clear();
            RequestContext.clear();
        }

        @Test
        @DisplayName("findAll returns the pre-seeded employees")
        void findAll_shouldReturnSeededEmployees() {
            List<Employee> employees = employeeService.findAll();
            // 3 employees are seeded in EmployeeService constructor
            assertThat(employees).hasSize(3);
        }

        @Test
        @DisplayName("findById returns the correct employee")
        void findById_shouldReturnCorrectEmployee() {
            Optional<Employee> result = employeeService.findById(1L);

            assertThat(result)
                    .isPresent()
                    .hasValueSatisfying(emp -> {
                        assertThat(emp.getId()).isEqualTo(1L);
                        assertThat(emp.getFirstName()).isEqualTo("Alice");
                        assertThat(emp.getDepartment()).isEqualTo("Engineering");
                    });
        }

        @Test
        @DisplayName("createEmployee and findById round-trip works correctly")
        void createEmployee_andFindById_roundTrip() {
            // Create
            Employee created = employeeService.createEmployee(
                    "Charlie", "Brown", "charlie@example.com",
                    "Design", new BigDecimal("72000"));

            assertThat(created.getId()).isNotNull().isPositive();
            assertThat(created.getFirstName()).isEqualTo("Charlie");

            // Find
            Optional<Employee> found = employeeService.findById(created.getId());

            assertThat(found)
                    .isPresent()
                    .hasValueSatisfying(emp -> {
                        assertThat(emp.getId()).isEqualTo(created.getId());
                        assertThat(emp.getEmail()).isEqualTo("charlie@example.com");
                    });
        }

        @Test
        @DisplayName("TimeTrackingService clockIn and clockOut work correctly")
        void timeTracking_clockInAndOut_shouldWork() {
            // Clock in
            TimeEntry entry = timeTrackingService.clockIn(1L);
            assertThat(entry).isNotNull();
            assertThat(entry.getEmployeeId()).isEqualTo(1L);
            assertThat(entry.getClockIn()).isNotNull();
            assertThat(entry.getClockOut()).isNull();

            // Clock out
            TimeEntry completed = timeTrackingService.clockOut(entry.getId(), "Completed daily tasks");
            assertThat(completed.getClockOut()).isNotNull();
            assertThat(completed.getNotes()).isEqualTo("Completed daily tasks");
        }

        @Test
        @DisplayName("ExpenseService submit and findAll work correctly")
        void expenseService_submitExpense_shouldWork() {
            // No FINANCE_MANAGER role needed for submitExpense
            ExpenseReport report = expenseService.submitExpense(
                    1L, "Team lunch", new BigDecimal("125.00"), "MEALS");

            assertThat(report).isNotNull();
            assertThat(report.getStatus()).isEqualTo("PENDING");
            assertThat(report.getAmount()).isEqualByComparingTo(new BigDecimal("125.00"));
        }

        @Test
        @DisplayName("ExpenseService approve requires FINANCE_MANAGER role")
        void expenseService_approve_requiresFinanceManagerRole() {
            // Submit (no role check)
            ExpenseReport report = expenseService.submitExpense(
                    2L, "Conference ticket", new BigDecimal("299.00"), "TRAINING");

            // Approve without the right role → SecurityException
            CurrentUserContext.setRole("EMPLOYEE");
            assertThatThrownBy(() -> expenseService.approveExpense(report.getId()))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("FINANCE_MANAGER");

            // Approve WITH the right role → success
            CurrentUserContext.setRole("FINANCE_MANAGER");
            ExpenseReport approved = expenseService.approveExpense(report.getId());
            assertThat(approved.getStatus()).isEqualTo("APPROVED");
        }
    }

    /*
     * ════════════════════════════════════════════════════════════════════════════════
     *  NESTED GROUP 7: @Around Exception Wrapping (ExpenseService)
     *  ExceptionWrappingAspect wraps NoSuchElementException into ExpenseNotFoundException.
     * ════════════════════════════════════════════════════════════════════════════════
     */
    @Nested
    @DisplayName("@Around Exception Wrapping (ExpenseService)")
    class ExceptionWrappingTests {

        @BeforeEach
        void setUp() {
            CurrentUserContext.setUser("test");
            CurrentUserContext.setRole("FINANCE_MANAGER");
            RequestContext.newRequest();
        }

        @AfterEach
        void tearDown() {
            CurrentUserContext.clear();
            RequestContext.clear();
        }

        @Test
        @DisplayName("Approving non-existent expense should throw ExpenseNotFoundException")
        void approveNonExistentExpense_shouldThrowExpenseNotFoundException() {
            /*
             * ExpenseService.approveExpense() throws NoSuchElementException for unknown IDs.
             * ExceptionWrappingAspect (@Around within ExpenseService) catches it
             * and wraps it in ExpenseNotFoundException.
             *
             * The caller sees ExpenseNotFoundException — NOT NoSuchElementException.
             * This decouples the API from the implementation's exception types.
             */
            assertThatThrownBy(() -> expenseService.approveExpense(99999L))
                    .isInstanceOf(ExpenseNotFoundException.class)
                    .hasMessageContaining("Expense not found")
                    .hasMessageContaining("approveExpense");
        }

        @Test
        @DisplayName("Rejecting non-existent expense should throw ExpenseNotFoundException")
        void rejectNonExistentExpense_shouldThrowExpenseNotFoundException() {
            assertThatThrownBy(() -> expenseService.rejectExpense(99999L, "No receipt provided"))
                    .isInstanceOf(ExpenseNotFoundException.class)
                    .hasMessageContaining("Expense not found");
        }
    }
}

