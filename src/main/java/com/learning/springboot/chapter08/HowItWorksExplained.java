package com.learning.springboot.chapter08;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   HOW IT WORKS: SPRING TESTING — INTERNAL MECHANICS                                  ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        HowItWorksExplained.java
 * Purpose:     Deep dive into what happens internally when Spring Boot tests run:
 *              ApplicationContext caching, test slice mechanics, Mockito integration,
 *              MockMvc processing, @DataJpaTest transaction lifecycle, and more.
 * Difficulty:  ⭐⭐⭐⭐⭐ Advanced
 * Time:        60–90 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
public class HowItWorksExplained {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 1: HOW SPRING TEST CONTEXT CACHING WORKS                           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * THE PROBLEM: Context startup is SLOW:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Starting a Spring Boot ApplicationContext involves:
     *   → Scanning all packages for components
     *   → Evaluating all @Conditional annotations
     *   → Creating and wiring ALL beans
     *   → Running all auto-configurations
     *   → Starting the datasource, validating schema, etc.
     *
     * For our project, this takes 2–5 seconds. If every test class rebuilt the
     * context, a suite of 50 test classes would take 50 × 4s = 200 seconds just
     * on context startup — before a single test method runs!
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * THE SOLUTION: ApplicationContext CACHE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Spring Test maintains a static cache:
     *   Map<ContextKey, ApplicationContext> contextCache;
     *
     * The ContextKey is computed from:
     *   → The set of configuration classes (@SpringBootApplication class, etc.)
     *   → Active profiles (@ActiveProfiles)
     *   → Property sources (@TestPropertySource)
     *   → Context loaders
     *   → @MockBean definitions (each unique set of MockBeans = unique key!)
     *   → Web environment type (MOCK, RANDOM_PORT, etc.)
     *
     * When a test class starts:
     *   1. Compute the ContextKey for this test class
     *   2. Look up the cache: contextCache.get(key)
     *   3. If FOUND → reuse the cached context (near-instant!)
     *   4. If NOT FOUND → create a new context (slow), store in cache
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * MAXIMISING CACHE REUSE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * For our Chapter 8 tests:
     *   Example01_SpringBootIntegrationTest       → @SpringBootTest MOCK + @AutoConfigureMockMvc
     *   Example01b_SpringBootRandomPortTest        → @SpringBootTest RANDOM_PORT
     *   Example04a_MockBeanTest                    → @SpringBootTest NONE + @MockBean TodoRepository
     *   Example04b_SpyBeanTest                     → @SpringBootTest NONE + @SpyBean TodoService
     *   Example06a_TestPropertySourceTest          → @SpringBootTest NONE + different properties
     *   Example06b_ActiveProfilesTest              → @SpringBootTest NONE + "test" profile
     *
     * Each of these has a DIFFERENT ContextKey → each creates its own context.
     * Example04a and Example04b cannot share because their mock configurations differ.
     *
     * Best practice for real projects:
     *   Create a SHARED BASE TEST CLASS with all common @MockBeans:
     *
     *   @SpringBootTest
     *   @MockBean(TodoRepository.class)   // all tests need this
     *   abstract class BaseIntegrationTest {}
     *
     *   class TestA extends BaseIntegrationTest {}
     *   class TestB extends BaseIntegrationTest {}
     *   // TestA and TestB SHARE the same cached context!
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * @DirtiesContext AND CACHE INVALIDATION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * When a test class has @DirtiesContext, after it finishes:
     *   → Spring calls context.close() → destroys all beans, releases connections
     *   → Removes the entry from contextCache
     *   → Next test that needs the same key: cache miss → rebuild from scratch
     *
     * This is why @DirtiesContext is expensive: it forces a full rebuild.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 2: HOW @WebMvcTest SLICE WORKS INTERNALLY                           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * When @WebMvcTest is applied, Spring Boot:
     *
     *  1. Identifies all registered AUTO-CONFIGURATIONS
     *
     *  2. FILTERS them — keeps ONLY:
     *     → MockMvcAutoConfiguration → sets up MockMvc
     *     → MockMvcSecurityAutoConfiguration → security for MockMvc
     *     → WebMvcAutoConfiguration → Spring MVC setup
     *     → Jackson/Gson/Jsonb auto-configs → JSON handling
     *     → ValidationAutoConfiguration → @Valid support
     *
     *  3. EXCLUDES:
     *     → DataSourceAutoConfiguration → no DB
     *     → HibernateJpaAutoConfiguration → no JPA
     *     → TaskExecutionAutoConfiguration → no async
     *     → All non-MVC auto-configs
     *
     *  4. Scans for ONLY @Controller / @RestController / @ControllerAdvice
     *     If @WebMvcTest(TodoController.class): only scans TodoController
     *
     *  5. Creates the ApplicationContext with this stripped configuration
     *
     *  6. For any bean the controller needs that wasn't loaded:
     *     → Must be provided as @MockBean → added to context as a Mockito mock
     *
     *  RESULT:
     *  → Context starts in ~200–500ms vs 2–5s for full @SpringBootTest
     *  → Only MVC-related components are loaded
     *  → No database, no JPA, no background tasks
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * HOW MockMvc PROCESSES A REQUEST:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  mockMvc.perform(get("/api/ch08/todos/1"))
     *       ↓
     *  MockMvc creates a MockHttpServletRequest
     *       ↓
     *  Passes through registered Filters (authentication, CORS, etc.)
     *       ↓
     *  DispatcherServlet.service() is called
     *       ↓
     *  HandlerMapping finds: TodoController.getById() matches GET /api/ch08/todos/{id}
     *       ↓
     *  HandlerAdapter (RequestMappingHandlerAdapter) invokes the method:
     *    → @PathVariable id extracted from URL
     *    → todoService.getById(1L) called (returns mocked value)
     *    → Return value: ResponseEntity<Todo>
     *       ↓
     *  HttpMessageConverter (Jackson MappingJackson2HttpMessageConverter):
     *    → Todo object → JSON bytes
     *       ↓
     *  MockHttpServletResponse populated with:
     *    → Status: 200
     *    → Content-Type: application/json
     *    → Body: {"title":"Buy milk","completed":false,...}
     *       ↓
     *  MvcResult returned to test
     *       ↓
     *  .andExpect(status().isOk()) → checks status == 200
     *  .andExpect(jsonPath("$.title").value("Buy milk")) → evaluates JSONPath
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 3: HOW @DataJpaTest TRANSACTION ROLLBACK WORKS                      ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * TRANSACTION TIMELINE IN @DataJpaTest:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ┌── Test method starts ────────────────────────────────────────────────────────┐
     *  │  TransactionSynchronizationManager.bindResource(...)                        │
     *  │  BEGIN TRANSACTION  ← Spring opens a transaction                           │
     *  │      ↓                                                                      │
     *  │  @BeforeEach runs (inside the transaction)                                 │
     *  │      ↓                                                                      │
     *  │  @Test method runs (inside the transaction)                                │
     *  │    → todoRepository.save(new Todo("Test"))                                  │
     *  │      → Hibernate: INSERT INTO ch08_todos (...)                              │
     *  │      → But NOT committed to H2 yet (pending in transaction)                │
     *  │    → entityManager.flush()                                                  │
     *  │      → Hibernate: flushes pending SQL to H2                               │
     *  │      → H2 has the row NOW within this transaction                          │
     *  │    → todoRepository.findByCompleted(false)                                  │
     *  │      → SELECT FROM ch08_todos WHERE completed=false                        │
     *  │      → Returns the row we just flushed (within same tx)                   │
     *  │      ↓                                                                      │
     *  │  @AfterEach runs (inside the transaction)                                  │
     *  │      ↓                                                                      │
     *  │  ROLLBACK  ← Spring rolls back the transaction                            │
     *  │    → All INSERTs, UPDATEs, DELETEs are UNDONE                             │
     *  │    → H2 is back to its pre-test state                                     │
     *  └── Test method ends ─────────────────────────────────────────────────────────┘
     *
     *  ┌── Next test starts with CLEAN database state ────────────────────────────────┐
     *
     * WHY flush() IS IMPORTANT IN TESTS:
     *   Hibernate's write-behind mechanism: by default, writes are batched.
     *   Without flush(), Hibernate may not have written to H2 yet.
     *   Your SELECT query runs before the INSERT → returns stale data!
     *
     *   Solution:
     *     entityManager.flush() → force pending writes
     *     entityManager.clear() → clear L1 cache → next read hits DB
     *     This ensures you read back what you actually wrote.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * HOW @Sql INTERACTS WITH TRANSACTIONS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  @Sql runs in the SAME transaction as the test (by default).
     *  → SQL data is visible within the test
     *  → SQL data is rolled back along with the test
     *  → No manual cleanup needed
     *
     *  Override with @SqlConfig(transactionMode = ISOLATED):
     *  → @Sql runs in its OWN transaction (committed separately)
     *  → Data persists even after test transaction rollback
     *  → Manual cleanup needed
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 4: HOW @MockBean INTEGRATES WITH SPRING CONTEXT                     ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * What happens when Spring Boot sees @MockBean TodoRepository:
     *
     *  1. MockitoPostProcessor scans for @MockBean in the test class
     *
     *  2. Before the ApplicationContext is fully initialised:
     *     → TodoRepository's bean definition is found in the context
     *     → It is REPLACED with a new bean definition that creates a Mockito mock:
     *        BeanDefinition(beanClass=Mockito.mock(TodoRepository.class))
     *
     *  3. Context creates the mock: Mockito.mock(TodoRepository.class)
     *     → All methods return defaults: null for objects, 0 for numbers, false for booleans
     *     → Empty Optional for methods returning Optional (NOT automatically — it returns null)
     *
     *  4. The mock is registered in the context as a singleton bean
     *
     *  5. Any other bean that @Autowired TodoRepository gets the MOCK
     *     → TodoService's constructor: new TodoService(todoRepositoryMock)
     *
     *  6. In the test: @MockBean TodoRepository todoRepository
     *     → This field reference points to the SAME mock that's in the context
     *     → Stubbing via `given(todoRepository.findAll()).willReturn(...)` affects the
     *        mock that TodoService is using
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * MOCK RESET BETWEEN TESTS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * By default, @MockBean mocks are reset AFTER EACH test using:
     *   Mockito.RESETS_INVOCATIONS (default MockReset.AFTER)
     *   → Invocation records cleared (verify() sees clean state)
     *   → Stub configurations are NOT cleared (stubs persist between tests)
     *
     * To clear stubs too: use MockReset.BEFORE or call Mockito.reset(mock) in @BeforeEach
     *
     * @MockBean(reset = MockReset.NONE)   → no automatic reset (stubs + invocations persist)
     * @MockBean(reset = MockReset.BEFORE) → reset before each test
     * @MockBean(reset = MockReset.AFTER)  → reset after each test (default)
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 5: HOW @ParameterizedTest WORKS INTERNALLY                          ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Given:
     *
     *   @ParameterizedTest
     *   @ValueSource(strings = {"a", "b", "c"})
     *   void myTest(String value) { assertThat(value).isNotBlank(); }
     *
     * JUnit 5 INTERNALS:
     *
     *  1. JUnit 5 discovers myTest as a @ParameterizedTest (not a regular @Test)
     *
     *  2. @ValueSource is an ArgumentsSource — it's an AnnotationConsumerInitializer
     *     that produces a Stream<Arguments>: [("a"), ("b"), ("c")]
     *
     *  3. JUnit 5 creates a ParameterizedTestExtension which:
     *     a. Calls argumentsSource.provideArguments(ctx) → Stream<Arguments>
     *     b. Iterates over the stream: for each Arguments row:
     *        → Creates a new test method invocation
     *        → Uses TypeConvertingArgumentConverter to convert strings → target types
     *        → Calls myTest("a"), myTest("b"), myTest("c")
     *
     *  4. Each invocation is reported as a SEPARATE test in the report:
     *     ✓ [1] a
     *     ✓ [2] b
     *     ✓ [3] c
     *
     *  5. If any invocation fails, only THAT invocation is marked as failed.
     *     Others continue running (unlike one test with 3 assertions where first
     *     failure stops the test).
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * TYPE CONVERSION IN PARAMETERIZED TESTS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @CsvSource({"1, hello, true"}) → String → {int, String, boolean}
     * JUnit 5 uses:
     *   1. DefaultArgumentConverter
     *   2. Tries: Integer.parseInt("1") → 1
     *              "hello" → "hello" (already String)
     *              Boolean.parseBoolean("true") → true
     *
     * Supports: all Java primitives, Number, Date, LocalDate, etc.
     * For complex objects: use @MethodSource with custom Arguments.of(...)
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 6: COMPLETE EXECUTION FLOW — FROM TEST RUN TO RESULT               ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     *  ./gradlew test
     *       ↓
     *  Gradle configures JUnit Platform
     *       ↓
     *  JUnit Platform ClassSelector scans build/classes for @Test methods
     *       ↓
     *  Test classes are discovered:
     *    Example01_SpringBootIntegrationTest,
     *    Example02_WebMvcControllerTest,
     *    Example03_DataJpaRepositoryTest, ...
     *       ↓
     *  For each test class:
     *    1. Compute ContextKey
     *    2. Get/create ApplicationContext (from cache or build new)
     *    3. Create test instance (new instance per method by default)
     *    4. Inject @Autowired fields from context
     *    5. Wire @MockBean fields from context (they're already registered)
     *       ↓
     *  For each @Test method:
     *    1. Run @BeforeAll (once if not already done)
     *    2. Run @BeforeEach
     *    3. Run @Test method
     *    4. Run @AfterEach
     *    5. [After all methods] Run @AfterAll
     *       ↓
     *  JUnit reports pass/fail per test
     *       ↓
     *  Gradle generates HTML report: build/reports/tests/test/index.html
     *  Gradle generates XML report: build/test-results/test/TEST-*.xml
     *       ↓
     *  Build SUCCEEDS if all tests pass, FAILS if any test fails
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 7: PRODUCTION TESTING CHECKLIST                                     ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     *  TEST PYRAMID IMPLEMENTATION:
     *  ─────────────────────────────
     *  ✅  Unit tests (no Spring): 60–70% of tests
     *      @ExtendWith(MockitoExtension.class) + @Mock + @InjectMocks
     *      Fast (milliseconds), test single class, maximum isolation
     *
     *  ✅  Integration tests (Spring slices): 20–30% of tests
     *      @WebMvcTest for controllers, @DataJpaTest for repositories
     *      Medium speed (seconds), test a layer, realistic infrastructure
     *
     *  ✅  Full integration tests: 5–10% of tests
     *      @SpringBootTest for critical end-to-end flows
     *      Slower, but verify the whole system works together
     *
     *  CONTEXT REUSE:
     *  ──────────────
     *  ✅  Use a shared base test class for @MockBean combinations
     *  ✅  Avoid unnecessary @DirtiesContext
     *  ✅  Group tests by their context configuration
     *
     *  TEST ISOLATION:
     *  ───────────────
     *  ✅  @BeforeEach: clean database state before each test
     *  ✅  @DataJpaTest: auto-rollback (no manual cleanup needed)
     *  ✅  Pure unit tests: no shared state between test instances
     *
     *  ASSERTIONS:
     *  ───────────
     *  ✅  Use AssertJ for all assertions (fluent, readable error messages)
     *  ✅  Use @DisplayName for human-readable test names
     *  ✅  Use @Nested to group related test scenarios
     *  ✅  Follow AAA: Arrange → Act → Assert
     *
     *  RELIABILITY:
     *  ────────────
     *  ✅  Never rely on execution order between test classes
     *  ✅  @RepeatedTest(10) flaky tests to detect timing issues
     *  ✅  @Timeout on long-running tests to catch infinite loops
     *  ✅  Separate @Tag("slow") tests for nightly runs
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║   CHAPTER 8 — HOW IT WORKS: Spring Testing Internal Mechanics     ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  STAGE 1:  ApplicationContext caching (ContextKey, cache misses)");
        System.out.println("  STAGE 2:  @WebMvcTest slice + MockMvc request processing");
        System.out.println("  STAGE 3:  @DataJpaTest transaction timeline + rollback mechanics");
        System.out.println("  STAGE 4:  @MockBean Spring integration (MockitoPostProcessor)");
        System.out.println("  STAGE 5:  @ParameterizedTest internals (ArgumentsSource, conversion)");
        System.out.println("  STAGE 6:  Complete execution flow (gradlew test → HTML report)");
        System.out.println("  STAGE 7:  Production testing checklist (pyramid, isolation, reliability)");
        System.out.println();
        System.out.println("📚 Chapter 8 — Complete! Run all tests with: ./gradlew test");
        System.out.println("   View report: build/reports/tests/test/index.html");
        System.out.println("   Next: Chapter 9 — Spring AOP Annotations");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

