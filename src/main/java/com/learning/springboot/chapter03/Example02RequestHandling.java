package com.learning.springboot.chapter03;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║            EXAMPLE 02: REQUEST HANDLING ANNOTATIONS IN ACTION                        ║
 * ║            @RequestParam · @PathVariable · @RequestBody · @RequestHeader             ║
 * ║            @CookieValue · @ModelAttribute · @SessionAttribute · @RequestAttribute    ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example02RequestHandling.java
 * Purpose:     Master every way to extract data from an incoming HTTP request.
 *              After this file, you'll know exactly which annotation to use for
 *              every part of the HTTP request (URL, query, body, headers, cookies).
 * Difficulty:  ⭐⭐⭐ Intermediate
 * Time:        35 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * SCENARIO: An Employee Management API that demonstrates all request extraction.
 *
 * HTTP REQUEST ANATOMY REMINDER:
 *
 *   GET /api/v1/employees/42/tasks?status=open&page=0&size=10   HTTP/1.1
 *   │   │────────────────────────│──────────────────────────────│
 *   │   @PathVariable reads {42} │   @RequestParam reads params │
 *   Host: api.company.com
 *   Authorization: Bearer eyJ...  ← @RequestHeader reads this
 *   X-Request-ID: abc-123         ← @RequestHeader reads this
 *   Cookie: sessionId=xyz789      ← @CookieValue reads this
 *   Content-Type: application/json
 *
 *   { "taskName": "Review PR" }   ← @RequestBody reads this JSON body
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  DOMAIN MODEL
// ══════════════════════════════════════════════════════════════════════════════════════

/** Employee entity. */
class Employee {
    private Long   id;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private int    salary;

    public Employee() {}
    public Employee(Long id, String firstName, String lastName,
                    String email, String department, int salary) {
        this.id = id; this.firstName = firstName; this.lastName = lastName;
        this.email = email; this.department = department; this.salary = salary;
    }

    public Long   getId()         { return id; }
    public String getFirstName()  { return firstName; }
    public String getLastName()   { return lastName; }
    public String getEmail()      { return email; }
    public String getDepartment() { return department; }
    public int    getSalary()     { return salary; }
    public void setId(Long id)                 { this.id         = id; }
    public void setFirstName(String firstName) { this.firstName  = firstName; }
    public void setLastName(String lastName)   { this.lastName   = lastName; }
    public void setEmail(String email)         { this.email      = email; }
    public void setDepartment(String dept)     { this.department = dept; }
    public void setSalary(int salary)          { this.salary     = salary; }
}

/** DTO for creating an employee. */
class CreateEmployeeRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private int    salary;

    public String getFirstName()  { return firstName; }
    public String getLastName()   { return lastName; }
    public String getEmail()      { return email; }
    public String getDepartment() { return department; }
    public int    getSalary()     { return salary; }
    public void setFirstName(String v)  { this.firstName  = v; }
    public void setLastName(String v)   { this.lastName   = v; }
    public void setEmail(String v)      { this.email      = v; }
    public void setDepartment(String v) { this.department = v; }
    public void setSalary(int v)        { this.salary     = v; }
}

/** Paginated result wrapper. */
class PageResult<T> {
    private List<T> items;
    private int     page;
    private int     size;
    private long    totalItems;
    private int     totalPages;

    public PageResult(List<T> items, int page, int size, long total) {
        this.items      = items;
        this.page       = page;
        this.size       = size;
        this.totalItems = total;
        this.totalPages = (int) Math.ceil((double) total / size);
    }

    public List<T> getItems()      { return items; }
    public int     getPage()       { return page; }
    public int     getSize()       { return size; }
    public long    getTotalItems() { return totalItems; }
    public int     getTotalPages() { return totalPages; }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  THE CONTROLLER — ALL REQUEST EXTRACTION ANNOTATIONS IN ONE PLACE
// ══════════════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/v1/employees")
class EmployeeController {

    private final Map<Long, Employee> store = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong idGen = new java.util.concurrent.atomic.AtomicLong(1);

    public EmployeeController() {
        store.put(1L, new Employee(1L,"Alice","Smith","alice@co.com","Engineering",95000));
        store.put(2L, new Employee(2L,"Bob",  "Jones","bob@co.com",  "Marketing",  75000));
        store.put(3L, new Employee(3L,"Carol","Davis","carol@co.com","Engineering",88000));
        store.put(4L, new Employee(4L,"Dave", "Wilson","dave@co.com","HR",          65000));
        idGen.set(5);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SECTION 1: @RequestParam — QUERY STRING PARAMETERS
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                        @RequestParam  EXPLAINED                              ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @RequestParam extracts values from the QUERY STRING of the URL:
     *   /employees?department=Engineering&page=0&size=10
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔧 ALL @RequestParam ATTRIBUTES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @RequestParam                               ← name inferred from parameter name
     *   @RequestParam("department")                 ← explicit name
     *   @RequestParam(name="dept", required=false)  ← optional (can be null)
     *   @RequestParam(defaultValue="Engineering")   ← default if not provided
     *   @RequestParam(required=true)                ← default: required=true
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 URL EXAMPLES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   GET /api/v1/employees?department=Engineering
     *   GET /api/v1/employees?department=Engineering&page=1&size=5
     *   GET /api/v1/employees?department=HR&minSalary=50000&maxSalary=100000
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * GET /api/v1/employees
     * Query params: department (optional), page, size, sortBy, sortDir, minSalary, maxSalary
     */
    @GetMapping
    public PageResult<Employee> searchEmployees(

            // Pattern 1: Optional param — if not provided, department is null (search all)
            @RequestParam(required = false)
            String department,

            // Pattern 2: Default value — if not provided, defaults to 0
            @RequestParam(defaultValue = "0")
            int page,

            // Pattern 3: Default value — if not provided, defaults to 10
            @RequestParam(defaultValue = "10")
            int size,

            // Pattern 4: Named param different from Java variable
            @RequestParam(name = "sort", defaultValue = "firstName")
            String sortBy,

            // Pattern 5: Optional enum-like string
            @RequestParam(name = "dir", defaultValue = "asc")
            String sortDir,

            // Pattern 6: Optional numeric range
            @RequestParam(required = false)
            Integer minSalary,

            @RequestParam(required = false)
            Integer maxSalary
    ) {
        System.out.printf("📥 GET /api/v1/employees — dept=%s page=%d size=%d sort=%s %s%n",
                department, page, size, sortBy, sortDir);

        // Filter employees
        List<Employee> filtered = store.values().stream()
                .filter(e -> department == null || e.getDepartment().equalsIgnoreCase(department))
                .filter(e -> minSalary  == null || e.getSalary() >= minSalary)
                .filter(e -> maxSalary  == null || e.getSalary() <= maxSalary)
                .sorted(Comparator.comparing(Employee::getFirstName))
                .toList();

        // Paginate
        int start   = page * size;
        int end     = Math.min(start + size, filtered.size());
        List<Employee> pageItems = (start > filtered.size())
                ? List.of()
                : filtered.subList(start, end);

        return new PageResult<>(pageItems, page, size, filtered.size());
    }

    /**
     * GET /api/v1/employees/ids?id=1&id=2&id=3
     *
     * Pattern: @RequestParam with List<Long> collects repeated query params.
     *
     * URL examples:
     *   ?id=1&id=2&id=3                   ← repeated key
     *   ?ids=1,2,3  (with comma-separated) ← if using custom converter
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 HOW TO PASS A LIST AS QUERY PARAMS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   Method 1: Repeated key   → ?id=1&id=2&id=3   → List<Long> ids = [1, 2, 3]
     *   Method 2: Comma-separated → ?ids=1,2,3       → String ids; then ids.split(",")
     *
     */
    @GetMapping("/bulk")
    public List<Employee> getByIds(
            @RequestParam(name = "id")   // ← Spring collects all ?id= params into the list
            List<Long> ids) {
        System.out.println("📥 GET /api/v1/employees/bulk — ids=" + ids);
        return ids.stream()
                .map(store::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * GET /api/v1/employees/params-demo
     *
     * Collects ALL query parameters into a Map.
     * Useful for dynamic/unknown parameter names.
     *
     * URL: /params-demo?foo=1&bar=2&baz=3
     * → params = {"foo":"1", "bar":"2", "baz":"3"}
     */
    @GetMapping("/params-demo")
    public Map<String, String> allParamsDemo(
            @RequestParam Map<String, String> params) {   // ← ALL params as a Map
        System.out.println("📥 GET /api/v1/employees/params-demo — params=" + params);
        return params;
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SECTION 2: @PathVariable — URL PATH SEGMENTS
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                       @PathVariable  EXPLAINED                               ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @PathVariable extracts a value embedded INSIDE the URL path:
     *   /employees/{id}   →  /employees/42  →  id = 42
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 @PathVariable vs @RequestParam:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @PathVariable:
     *     URL:  /employees/42
     *     When: Identifies a SPECIFIC resource (the employee with id 42)
     *     Rule: Use for REQUIRED, identifying values in the URL
     *
     *   @RequestParam:
     *     URL:  /employees?page=2&size=10
     *     When: Filters, paginates, searches resources
     *     Rule: Use for OPTIONAL values that filter/modify behaviour
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔧 ALL @PathVariable SYNTAX:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @PathVariable Long id               ← name inferred from variable: {id}
     *   @PathVariable("employeeId") Long id ← explicit path variable name
     *   @PathVariable(required = false) Long id ← optional path variable (Spring 4.3.3+)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 PATH VARIABLE WITH REGEX CONSTRAINT:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @GetMapping("/{id:[0-9]+}")     ← Only match if {id} is numeric
     *   @GetMapping("/{slug:[a-z-]+}")  ← Only match if {slug} is lowercase letters/dashes
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * GET /api/v1/employees/{id}
     * Pattern 1: Simple path variable
     */
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getById(
            @PathVariable Long id) {   // ← name inferred from parameter name: {id}
        System.out.println("📥 GET /api/v1/employees/" + id);
        Employee emp = store.get(id);
        return emp != null ? ResponseEntity.ok(emp) : ResponseEntity.notFound().build();
    }

    /**
     * GET /api/v1/employees/{id}/departments/{dept}/reports
     * Pattern 2: MULTIPLE path variables in one URL.
     *
     * URL example: /api/v1/employees/42/departments/Engineering/reports
     * Spring fills in:  employeeId=42, department="Engineering"
     */
    @GetMapping("/{id}/departments/{dept}/reports")
    public Map<String, Object> getEmployeeReportsByDept(
            @PathVariable("id")   Long employeeId,      // ← explicit name "id" → {id}
            @PathVariable("dept") String department) {  // ← explicit name "dept" → {dept}
        System.out.printf("📥 GET /employees/%d/departments/%s/reports%n", employeeId, department);
        return Map.of(
                "employeeId", employeeId,
                "department", department,
                "reportsGenerated", true,
                "count", 5
        );
    }

    /**
     * GET /api/v1/employees/numeric/{id:[0-9]+}
     * Pattern 3: Regex constraint — only matches numeric IDs.
     *
     * /employees/numeric/42     ← matches  (numeric)
     * /employees/numeric/alice  ← 404      (not numeric — Spring doesn't route here)
     */
    @GetMapping("/numeric/{id:[0-9]+}")
    public ResponseEntity<Employee> getByNumericId(
            @PathVariable Long id) {
        System.out.println("📥 GET /api/v1/employees/numeric/" + id + " (regex validated)");
        return Optional.ofNullable(store.get(id))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SECTION 3: @RequestBody — HTTP BODY (JSON) DESERIALIZATION
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                        @RequestBody  EXPLAINED                               ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @RequestBody reads the HTTP request BODY and deserialises it into a Java object
     * using an HttpMessageConverter (Jackson by default).
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 HOW JACKSON DESERIALISATION WORKS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   HTTP Request Body (String):              Java Object:
     *   ─────────────────────────────────        ──────────────────────────────
     *   {                                        CreateEmployeeRequest {
     *     "firstName": "Eve",                        firstName = "Eve"
     *     "lastName":  "Turner",                     lastName  = "Turner"
     *     "email":     "eve@co.com",                 email     = "eve@co.com"
     *     "department":"Engineering",                department= "Engineering"
     *     "salary":    85000                         salary    = 85000
     *   }                                        }
     *
     * Jackson:
     *   1. Reads Content-Type: application/json
     *   2. Finds Jackson's MappingJackson2HttpMessageConverter
     *   3. Uses ObjectMapper to parse JSON → Java
     *   4. Binds field by field using setters / record components
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔧 @RequestBody WITH VALIDATION (@Valid):
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Add @Valid before @RequestBody to trigger Jakarta Bean Validation:
     *
     *   @PostMapping
     *   ResponseEntity<?> create(@Valid @RequestBody CreateEmployeeRequest request) { ... }
     *
     * If validation fails → Spring throws MethodArgumentNotValidException → 400 Bad Request
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ⚠️ REQUIRED: Content-Type MUST be application/json.
     * Without it → 415 Unsupported Media Type.
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * POST /api/v1/employees
     * Content-Type: application/json
     * Body: { "firstName": "...", "lastName": "...", ... }
     */
    @PostMapping
    public ResponseEntity<Employee> createEmployee(
            @RequestBody CreateEmployeeRequest request) {   // ← reads JSON body
        System.out.println("📥 POST /api/v1/employees — creating: " + request.getFirstName());

        long newId = idGen.getAndIncrement();
        Employee emp = new Employee(newId, request.getFirstName(), request.getLastName(),
                request.getEmail(), request.getDepartment(), request.getSalary());
        store.put(newId, emp);

        return ResponseEntity.status(201).body(emp);
    }

    /**
     * Pattern: @RequestBody with Map<String, Object>
     *
     * Sometimes you don't know the exact structure of the JSON body.
     * Map<String, Object> accepts ANY JSON object dynamically.
     *
     * Body: { "salary": 95000, "department": "Engineering" }
     * → updates = {"salary": 95000, "department": "Engineering"}
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Employee> patchEmployee(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {   // ← dynamic JSON body
        System.out.println("📥 PATCH /api/v1/employees/" + id + " — fields=" + updates.keySet());

        Employee emp = store.get(id);
        if (emp == null) return ResponseEntity.notFound().build();

        if (updates.containsKey("salary"))     emp.setSalary((int) updates.get("salary"));
        if (updates.containsKey("department")) emp.setDepartment((String) updates.get("department"));
        if (updates.containsKey("email"))      emp.setEmail((String) updates.get("email"));

        return ResponseEntity.ok(emp);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SECTION 4: @RequestHeader — HTTP HEADERS
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                      @RequestHeader  EXPLAINED                               ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @RequestHeader extracts a value from the HTTP REQUEST HEADERS.
     *
     * HTTP headers carry metadata about the request:
     *   Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
     *   Content-Type: application/json
     *   Accept: application/json
     *   X-Request-ID: 550e8400-e29b-41d4-a716-446655440000
     *   X-API-Version: 2
     *   User-Agent: Mozilla/5.0...
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔧 ALL @RequestHeader PATTERNS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @RequestHeader("Authorization") String token        ← required single header
     *   @RequestHeader(required=false) String header        ← optional header
     *   @RequestHeader(defaultValue="en") String language   ← with default
     *   @RequestHeader HttpHeaders headers                  ← ALL headers as HttpHeaders
     *   @RequestHeader Map<String,String> headers           ← ALL headers as Map
     *   @RequestHeader MultiValueMap<String,String> headers ← ALL headers (multi-value)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 COMMON USE CASES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  •  Authentication: reading Authorization / X-API-Key headers
     *  •  Request tracing: reading X-Request-ID / X-Correlation-ID
     *  •  API versioning: reading X-API-Version header
     *  •  Idempotency keys: reading X-Idempotency-Key
     *  •  Locale: reading Accept-Language for i18n
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * GET /api/v1/employees/header-demo
     * Demonstrates all @RequestHeader patterns.
     */
    @GetMapping("/header-demo")
    public Map<String, Object> headerDemo(

            // Pattern 1: Required header (missing header → 400 Bad Request)
            @RequestHeader("X-Request-ID")
            String requestId,

            // Pattern 2: Optional header (null if missing)
            @RequestHeader(name = "Authorization", required = false)
            String authHeader,

            // Pattern 3: Optional with default
            @RequestHeader(name = "Accept-Language", defaultValue = "en-US")
            String acceptLanguage,

            // Pattern 4: All headers as HttpHeaders (Spring's typed object)
            @RequestHeader HttpHeaders allHeaders
    ) {
        System.out.println("📥 GET /api/v1/employees/header-demo");
        System.out.println("   X-Request-ID     = " + requestId);
        System.out.println("   Authorization    = " + (authHeader != null ? "[PRESENT]" : "[MISSING]"));
        System.out.println("   Accept-Language  = " + acceptLanguage);
        System.out.println("   All header names = " + allHeaders.toSingleValueMap().keySet());

        return Map.of(
                "requestId",       requestId,
                "authenticated",   authHeader != null,
                "language",        acceptLanguage,
                "headerCount",     allHeaders.size()
        );
    }

    /**
     * GET /api/v1/employees/all-headers
     *
     * Collects ALL headers into a Map.
     * Useful for debugging or logging all incoming headers.
     */
    @GetMapping("/all-headers")
    public Map<String, String> allHeaders(
            @RequestHeader Map<String, String> headers) {   // ← ALL headers as Map
        System.out.println("📥 GET /api/v1/employees/all-headers — " + headers.size() + " headers received");
        return headers;
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SECTION 5: @CookieValue — HTTP COOKIES
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                       @CookieValue  EXPLAINED                                ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @CookieValue extracts a value from an HTTP COOKIE sent by the client.
     *
     * HTTP Cookie header example:
     *   Cookie: sessionId=abc123; theme=dark; locale=en-US
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔧 ALL @CookieValue PATTERNS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @CookieValue("sessionId") String sessionId         ← required cookie
     *   @CookieValue(required=false) String theme          ← optional, can be null
     *   @CookieValue(defaultValue="light") String theme    ← with default
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 COOKIES vs HEADERS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   Cookies    → Small key-value pairs stored by browser, sent automatically
     *                with every request to the same origin.
     *                Use for: session IDs, user preferences, tracking
     *
     *   Headers    → Explicitly set by client code (e.g., Authorization: Bearer ...)
     *                Cookies are actually sent AS the Cookie header by browsers.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ⚠️ IMPORTANT: @CookieValue only works in web controllers (not @SpringBootTest
     *    non-web context). Always handle required=false for cookies, as clients may
     *    block cookies.
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * GET /api/v1/employees/cookie-demo
     */
    @GetMapping("/cookie-demo")
    public Map<String, Object> cookieDemo(

            // Pattern 1: Optional session cookie
            @CookieValue(name = "sessionId", required = false)
            String sessionId,

            // Pattern 2: Optional preference cookie with default
            @CookieValue(name = "theme", defaultValue = "light")
            String theme,

            // Pattern 3: Optional locale cookie
            @CookieValue(name = "locale", required = false)
            String locale
    ) {
        System.out.println("📥 GET /api/v1/employees/cookie-demo");
        System.out.printf("   sessionId=%s | theme=%s | locale=%s%n", sessionId, theme, locale);

        return Map.of(
                "sessionPresent", sessionId != null,
                "theme",          theme,
                "locale",         locale != null ? locale : "not-set"
        );
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SECTION 6: @ModelAttribute — FORM DATA BINDING
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                      @ModelAttribute  EXPLAINED                              ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @ModelAttribute binds HTML FORM DATA (URL-encoded key-value pairs)
     * to a Java object. It works with:
     *   Content-Type: application/x-www-form-urlencoded   (default HTML form)
     *   Content-Type: multipart/form-data                 (file upload forms)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * @RequestBody vs @ModelAttribute:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @RequestBody     → Reads HTTP body as JSON/XML (Content-Type: application/json)
     *   @ModelAttribute  → Reads HTML form fields (Content-Type: application/x-www-form-urlencoded)
     *
     *   Frontend HTML form:
     *   <form action="/employees" method="POST">
     *     <input name="firstName" value="Eve">
     *     <input name="salary" value="85000">
     *   </form>
     *
     *   Browser sends:  firstName=Eve&salary=85000  (NOT JSON!)
     *   @ModelAttribute binds this to CreateEmployeeRequest automatically.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 TWO USES OF @ModelAttribute:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  USE 1: As a METHOD PARAMETER — bind form data to a Java object:
     *
     *    @PostMapping("/form")
     *    String submit(@ModelAttribute CreateEmployeeRequest form) { ... }
     *
     *  USE 2: As a METHOD ANNOTATED with @ModelAttribute — add data to every response model:
     *
     *    @ModelAttribute("companyName")
     *    String companyName() { return "AcmeCorp"; }  ← added to every controller method's model
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * POST /api/v1/employees/form
     * Content-Type: application/x-www-form-urlencoded
     * Body: firstName=Eve&lastName=Turner&email=eve@co.com&department=Engineering&salary=85000
     */
    @PostMapping(value = "/form", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<Employee> createFromForm(
            @ModelAttribute CreateEmployeeRequest formData) {   // ← binds form fields to POJO
        System.out.println("📥 POST /api/v1/employees/form (form submission) — " + formData.getFirstName());
        long newId = idGen.getAndIncrement();
        Employee emp = new Employee(newId, formData.getFirstName(), formData.getLastName(),
                formData.getEmail(), formData.getDepartment(), formData.getSalary());
        store.put(newId, emp);
        return ResponseEntity.status(201).body(emp);
    }

    /**
     * @ModelAttribute as a MODEL-POPULATING METHOD.
     *
     * This method runs BEFORE every handler method in this controller.
     * It adds "commonData" to the model — available in all Thymeleaf templates.
     *
     * NOTE: In a @RestController this has no visual effect (there's no view template).
     * It's more relevant in @Controller classes with Thymeleaf.
     * Shown here for completeness.
     */
    @ModelAttribute("requestMetadata")
    public Map<String, String> addRequestMetadata(HttpServletRequest request) {
        // This runs before every controller method and adds metadata to the model
        return Map.of(
                "requestURI",    request.getRequestURI(),
                "remoteAddr",    request.getRemoteAddr(),
                "serverName",    request.getServerName()
        );
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SECTION 7: @SessionAttribute — READ FROM HTTP SESSION
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                     @SessionAttribute  EXPLAINED                             ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @SessionAttribute (singular) reads an attribute from the CURRENT HTTP SESSION.
     * Equivalent to: httpSession.getAttribute("attributeName")
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🤔 @SessionAttribute vs @SessionAttributes:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @SessionAttribute  (singular, method parameter)
     *     → READS an existing session attribute at this injection point
     *     → @SessionAttribute("currentUser") String userId
     *
     *   @SessionAttributes (plural, class-level)
     *     → STORES model attributes in the session across requests
     *     → Typically used in multi-step wizard forms
     *     → @SessionAttributes("wizardState")  on class
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 COMMON USE CASES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  •  Reading the current authenticated user from the session
     *  •  Reading a shopping cart stored in the session
     *  •  Reading wizard state across multi-step forms
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * GET /api/v1/employees/session-demo
     * Reads the "currentUserId" attribute from the HTTP session.
     */
    @GetMapping("/session-demo")
    public Map<String, Object> sessionDemo(

            // @SessionAttribute — reads a session attribute (must exist, or required=false)
            @SessionAttribute(name = "currentUserId", required = false)
            String currentUserId,

            // We can also inject the HttpSession directly for full control
            HttpSession session
    ) {
        System.out.println("📥 GET /api/v1/employees/session-demo");

        // Set something in the session for demonstration
        if (currentUserId == null) {
            session.setAttribute("currentUserId", "demo-user-42");
            currentUserId = "demo-user-42";  // Use the newly set value
        }

        return Map.of(
                "currentUserId",  currentUserId,
                "sessionId",      session.getId(),
                "sessionCreated", session.getCreationTime()
        );
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SECTION 8: @RequestAttribute — READ FROM REQUEST ATTRIBUTES
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                     @RequestAttribute  EXPLAINED                             ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @RequestAttribute reads an attribute from the CURRENT HTTP REQUEST (not session).
     * Equivalent to: httpServletRequest.getAttribute("attributeName")
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 HOW REQUEST ATTRIBUTES ARE SET:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Request attributes are set by:
     *   1. A Servlet Filter: request.setAttribute("requestId", uuid);
     *   2. An Interceptor:   request.setAttribute("userId", authenticatedId);
     *   3. HandlerInterceptor.preHandle()
     *   4. Forward from another servlet
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 @RequestAttribute vs @SessionAttribute:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @RequestAttribute  → Lives for the DURATION OF ONE REQUEST (very short-lived)
     *                        Set by filters/interceptors before controller is invoked
     *
     *   @SessionAttribute  → Lives for the DURATION OF A SESSION (longer-lived)
     *                        Persists across multiple requests from same user
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * GET /api/v1/employees/request-attr-demo
     */
    @GetMapping("/request-attr-demo")
    public Map<String, Object> requestAttributeDemo(

            // Reads "requestId" set by a filter/interceptor earlier in the pipeline
            @RequestAttribute(name = "requestId", required = false)
            String requestId,

            // Reads "authenticatedUser" set by a security filter
            @RequestAttribute(name = "authenticatedUser", required = false)
            String authenticatedUser,

            // Direct HttpServletRequest for setting attributes in demo
            HttpServletRequest request
    ) {
        // Simulate what a filter would have done earlier
        if (requestId == null) {
            requestId = "req-" + System.currentTimeMillis();
            request.setAttribute("requestId", requestId);
        }

        System.out.println("📥 GET /api/v1/employees/request-attr-demo");
        System.out.println("   requestId = " + requestId);
        System.out.println("   authUser  = " + authenticatedUser);

        return Map.of(
                "requestId",         requestId,
                "authenticatedUser", authenticatedUser != null ? authenticatedUser : "anonymous",
                "requestUri",        request.getRequestURI()
        );
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 9: THE COMPLETE REQUEST EXTRACTION PICTURE
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║          REQUEST HANDLING ANNOTATIONS — THE COMPLETE PICTURE                 ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * COMPLETE REQUEST DECOMPOSITION:
 *
 * Given this request:
 *   PATCH /api/v1/employees/42/departments/Engineering/reports?format=pdf&locale=en
 *   Authorization: Bearer eyJhbGc...
 *   X-Request-ID: 550e8400
 *   Cookie: sessionId=abc123; theme=dark
 *   [request attr set by filter: authenticatedUser = "alice@co.com"]
 *   Content-Type: application/json
 *   Body: { "salary": 95000 }
 *
 *   public ResponseEntity<?> handler(
 *       @PathVariable("id")   Long id,                 // 42
 *       @PathVariable("dept") String dept,             // "Engineering"
 *       @RequestParam         String format,           // "pdf"
 *       @RequestParam(defaultValue="en") String locale,// "en"
 *       @RequestHeader("Authorization") String auth,   // "Bearer eyJhbGc..."
 *       @RequestHeader("X-Request-ID") String reqId,   // "550e8400"
 *       @CookieValue("sessionId") String session,      // "abc123"
 *       @CookieValue(name="theme", defaultValue="light") String theme,  // "dark"
 *       @RequestAttribute(required=false) String user, // "alice@co.com"
 *       @RequestBody Map<String,Object> body           // {"salary": 95000}
 *   ) { ... }
 *
 * DECISION GUIDE:
 * ─────────────────────────────────────────────────────────────────────────────────
 *   Data comes from...              Use...
 *   ──────────────────────────────  ──────────────────────────────────────────────
 *   URL path: /employees/{id}       @PathVariable
 *   Query string: ?page=0           @RequestParam
 *   JSON body: {...}                @RequestBody
 *   Form fields: key=val&key2=val2  @ModelAttribute
 *   HTTP headers: X-Header: val     @RequestHeader
 *   Cookie: name=value              @CookieValue
 *   HttpSession attribute           @SessionAttribute
 *   HttpRequest attribute (filter)  @RequestAttribute
 *
 */
class Example02RequestHandling {
    // Intentionally empty — documentation class
}


