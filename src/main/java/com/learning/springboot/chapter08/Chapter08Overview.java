package com.learning.springboot.chapter08;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║                    SPRING BOOT ANNOTATIONS - COMPREHENSIVE GUIDE                     ║
 * ║                        Chapter 8: Spring Testing Annotations                         ║
 * ║                                                                                       ║
 * ║                           📚 From Zero to Expert 📚                                  ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Chapter:      8
 * Title:        Spring Testing Annotations
 * Difficulty:   ⭐⭐⭐ Intermediate
 * Estimated:    6–10 hours
 * Prerequisites: Chapters 1–4, basic understanding of unit and integration testing
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │                                                                                       │
 * │                    CHAPTER 8: OVERVIEW & LEARNING GOALS                              │
 * │                                                                                       │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📖 TABLE OF CONTENTS 📖
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Section  1 :  Chapter Introduction — Why Testing Matters
 * Section  2 :  Technology Stack   (JUnit 5, Mockito, AssertJ, Spring Test)
 * Section  3 :  @SpringBootTest    — Full integration testing
 * Section  4 :  Test Slice Annotations
 *                 @WebMvcTest      — Controller layer slice
 *                 @DataJpaTest     — Repository layer slice
 *                 @JsonTest        — JSON serialization slice
 *                 @RestClientTest  — REST client slice
 * Section  5 :  @MockBean & @SpyBean — Mocking in Spring context
 * Section  6 :  @TestConfiguration  — Test-only bean definitions
 * Section  7 :  JUnit 5 Core        (@Test, @BeforeEach, @AfterEach, @BeforeAll,
 *                                    @AfterAll, @Disabled, @DisplayName)
 * Section  8 :  Parameterized Tests  (@ParameterizedTest, @ValueSource, @CsvSource,
 *                                    @MethodSource, @EnumSource, @NullSource)
 * Section  9 :  Advanced JUnit      (@Nested, @Tag, @RepeatedTest, @Timeout,
 *                                    @TempDir, @ExtendWith, @Order)
 * Section 10 :  Advanced Spring Test (@DirtiesContext, @TestPropertySource,
 *                                    @ActiveProfiles, @Sql, @AutoConfigureMockMvc)
 * Section 11 :  Assertions           (AssertJ, JUnit 5 Assertions, Hamcrest)
 * Section 12 :  How Everything Works — Internal Mechanics
 * Section 13 :  Best Practices & Common Pitfalls
 * Section 14 :  Interview Questions & Answers
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * 📂 FILES IN THIS CHAPTER:
 *
 *  src/main/java/.../chapter08/
 *   • Chapter08Overview.java           ← YOU ARE HERE
 *   • Todo.java                         (domain entity — target of all tests)
 *   • CreateTodoRequest.java            (request DTO with validation)
 *   • TodoRepository.java               (JPA repository)
 *   • TodoService.java                  (service layer)
 *   • TodoController.java               (REST controller)
 *   • Example01SpringBootTestAnnotations.java  (@SpringBootTest, slices)
 *   • Example02MockBeanAndSpyBean.java         (@MockBean, @SpyBean, @TestConfiguration)
 *   • Example03JUnitAnnotations.java           (JUnit 5 deep dive)
 *   • Example04AdvancedTestingAnnotations.java (@DirtiesContext, @Sql, @ActiveProfiles)
 *   • HowItWorksExplained.java                (Internal mechanics)
 *
 *  src/test/java/.../chapter08/
 *   • Example01_SpringBootIntegrationTest.java  ← REAL runnable @SpringBootTest test
 *   • Example02_WebMvcControllerTest.java       ← REAL runnable @WebMvcTest test
 *   • Example03_DataJpaRepositoryTest.java      ← REAL runnable @DataJpaTest test
 *   • Example04_MockBeanSpyBeanTest.java        ← REAL runnable @MockBean test
 *   • Example05_JUnitAnnotationsTest.java       ← REAL runnable JUnit 5 showcase
 *   • Example06_AdvancedAnnotationsTest.java    ← REAL runnable advanced tests
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
public class Chapter08Overview {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                    SECTION 1: WHY TESTING MATTERS                            ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 LEARNING OBJECTIVES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * By the end of this chapter, you will be able to:
     *
     *  ✓  Write full application integration tests with @SpringBootTest
     *  ✓  Write fast, isolated controller tests with @WebMvcTest
     *  ✓  Write focused repository tests with @DataJpaTest
     *  ✓  Mock dependencies using @MockBean and @SpyBean
     *  ✓  Define test-only configurations with @TestConfiguration
     *  ✓  Use ALL JUnit 5 annotations confidently
     *  ✓  Write parameterized tests with @ParameterizedTest
     *  ✓  Organise tests using @Nested, @Tag, @DisplayName
     *  ✓  Override properties in tests with @TestPropertySource
     *  ✓  Execute SQL fixtures with @Sql
     *  ✓  Understand how Spring Test context caching works
     *  ✓  Answer tough testing interview questions
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 💥 THE PROBLEM WITHOUT TESTS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  Every change is a gamble — "I THINK this still works, let me deploy and see."
     *  Regression bugs slip through — something that worked before now silently breaks.
     *  Refactoring is terrifying — "If I touch this, will everything fall apart?"
     *  Production outages are discovered by customers, not developers.
     *  Onboarding is slow — new developers don't know what each piece does.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ✅ THE SOLUTION — AUTOMATED TESTS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  Every change is verified in seconds — CI/CD pipeline runs all tests.
     *  Regressions are caught immediately — the DEVELOPER sees it, not the customer.
     *  Refactoring is safe — tests define what the code SHOULD do.
     *  Documentation lives in the test — tests show how to USE the code.
     *  Onboarding is fast — read the tests to understand intent.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🏗️ THE TESTING PYRAMID:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *                       ╱▔▔▔▔▔▔▔▔▔▔▔▔╲
     *                      ╱  E2E / UI     ╲    ← Slow, expensive, flaky (few)
     *                     ╱   Tests         ╲
     *                    ╱▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔╲
     *                   ╱  Integration Tests  ╲  ← Medium speed, Spring Test (@SpringBootTest)
     *                  ╱  (@WebMvcTest, etc.)  ╲
     *                 ╱▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔╲
     *                ╱    Unit Tests              ╲ ← Fast, isolated, many
     *               ╱ (JUnit 5 + Mockito, no Spring)╲
     *              ╱▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔╲
     *
     *  RULE: Many unit tests + some integration tests + few end-to-end tests.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📦 WHAT spring-boot-starter-test BRINGS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  LIBRARY                  ROLE
     *  ─────────────────────    ────────────────────────────────────────────────────
     *  JUnit 5 (Jupiter)        Test framework (annotations, lifecycle, assertions)
     *  Mockito                  Mocking framework (mock, spy, verify, stub)
     *  AssertJ                  Fluent assertion library
     *  Hamcrest                 Matchers-based assertions (used by MockMvc)
     *  Spring Test              MockMvc, TestRestTemplate, @SpringBootTest, etc.
     *  Jayway JsonPath          JSON path assertions in MockMvc
     *
     *  All of these are included automatically. Zero extra configuration needed.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    SECTION 3–4: TEST SLICE ANNOTATIONS — QUICK REFERENCE                    ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 WHICH ANNOTATION TO USE — DECISION TABLE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  WHAT I WANT TO TEST          USE THIS               LOADS
     *  ──────────────────────────   ─────────────────────  ───────────────────────────
     *  Everything (integration)     @SpringBootTest         Full ApplicationContext
     *  Controller (HTTP layer)      @WebMvcTest             Only MVC beans + MockMvc
     *  Repository (JPA layer)       @DataJpaTest            Only JPA beans + H2
     *  JSON serialization           @JsonTest               Only Jackson ObjectMapper
     *  RestTemplate / WebClient     @RestClientTest         Only REST client beans
     *  Just a plain Java class      No annotation           Nothing (pure unit test)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @SpringBootTest — attributes quick reference:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   classes       → specific config classes to load (default: scan from main class)
     *   webEnvironment →
     *     MOCK (default)    → MockMvc servlet environment (no real HTTP)
     *     RANDOM_PORT       → starts real server on random port (use TestRestTemplate)
     *     DEFINED_PORT      → starts real server on port from properties
     *     NONE              → no web environment (pure service tests)
     *   properties    → override application properties inline
     *   args          → simulate command-line arguments
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @WebMvcTest — attributes quick reference:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   value / controllers → limit which controllers to load (default: all)
     *   excludeAutoConfiguration → exclude specific auto-configurations
     *   includeFilters / excludeFilters → component scan filters
     *
     *   LOADS:   @Controller, @RestController, @ControllerAdvice, @JsonComponent,
     *            Filter, WebMvcConfigurer, MockMvc (auto-configured)
     *   DOES NOT LOAD: @Service, @Repository, @Component
     *   RULE: Always @MockBean the service in a @WebMvcTest
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @DataJpaTest — attributes quick reference:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   LOADS:   @Entity classes, JPA repositories, DataSource, JPA config
     *   DOES NOT LOAD: @Service, @Controller, @Component
     *   DATABASE: replaces with embedded H2 by default
     *   TRANSACTIONS: rolls back after EACH test (no persistent state between tests)
     *
     *   replaceDataSource:
     *     NONE            → use your application's datasource (our H2 config)
     *     ANY (default)   → replace with embedded H2
     *
     *   showSql → show SQL statements (default: inherited from spring.jpa.show-sql)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @JsonTest — quick reference:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   LOADS: JacksonTester, GsonTester, JsonbTester (depending on libraries)
     *   USE: inject JacksonTester<YourDto> and call write() / parse()
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    SECTION 5–6: @MockBean, @SpyBean, @TestConfiguration — QUICK REFERENCE   ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @MockBean vs @SpyBean vs @Mock:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ANNOTATION   SPRING CONTEXT?  BEHAVIOUR                          USE WHEN
     *  ──────────   ──────────────   ───────────────────────────────    ────────────────────────────────────
     *  @Mock        NO               Pure Mockito mock (all methods      Pure unit tests with @ExtendWith(MockitoExtension.class)
     *               (unit test)      return default: null/0/false)
     *
     *  @MockBean    YES              Replaces real Spring bean with       You need a Spring context (@SpringBootTest, @WebMvcTest)
     *               (Spring test)    a Mockito mock                       and want to stub a dependency's behaviour
     *
     *  @Spy         NO               Mockito spy (real object, can        Pure unit tests — partial mocking
     *               (unit test)      stub specific methods)
     *
     *  @SpyBean     YES              Wraps real Spring bean in a spy      You need real logic PLUS ability to verify/stub some calls
     *               (Spring test)    (real methods called unless stubbed)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @TestConfiguration:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  WHAT IT DOES:
     *    Defines @Bean methods visible ONLY in the test context.
     *    Never included in the production context.
     *
     *  TWO USAGES:
     *    1. As an inner static class inside a @SpringBootTest class
     *       → Automatically picked up, adds beans to the test context
     *
     *    2. As a top-level class referenced via @Import
     *       → @SpringBootTest @Import(MyTestConfig.class) class MyTest {}
     *
     *  USE WHEN:
     *    → Replacing an external service bean with a test double
     *    → Providing an in-memory substitute (e.g., in-memory email sender)
     *    → Configuring test-specific feature flags
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    SECTION 7–9: JUNIT 5 ANNOTATIONS — COMPLETE QUICK REFERENCE              ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * LIFECYCLE ANNOTATIONS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  @Test             Marks a method as a test case
     *  @BeforeEach       Runs before EACH test method (setup per test)
     *  @AfterEach        Runs after EACH test method (teardown per test)
     *  @BeforeAll        Runs ONCE before all tests in the class (must be static)
     *  @AfterAll         Runs ONCE after all tests in the class (must be static)
     *  @Disabled         Skips the test (provide a reason string)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * DISPLAY & ORGANISATION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  @DisplayName("...")  Human-readable name for the test/class in reports
     *  @Nested              Inner class grouping related tests
     *  @Tag("tag")          Labels tests for selective execution (e.g., gradle -t "fast")
     *  @Order(n)            Controls method execution order (with @TestMethodOrder)
     *  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)  Applies ordering
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * PARAMETERIZED TESTS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  @ParameterizedTest          Marks a test as parameterized (runs multiple times)
     *  @ValueSource                Provides literals (strings, ints, longs, doubles...)
     *  @CsvSource                  Provides CSV rows (each row = one test invocation)
     *  @CsvFileSource              CSV rows from a file (resources folder)
     *  @MethodSource               Provides a Stream/List from a factory method
     *  @EnumSource                 Provides enum constants
     *  @NullSource                 Provides a single null value
     *  @EmptySource                Provides empty string / collection / array
     *  @NullAndEmptySource         Both @NullSource + @EmptySource combined
     *  @ArgumentsSource            Custom ArgumentsProvider for complex data
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ADVANCED JUnit 5:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  @RepeatedTest(n)    Run the same test n times (useful for flakiness detection)
     *  @Timeout(5)         Test fails if it takes longer than 5 seconds
     *  @TempDir            Injects a temporary directory (cleaned up after test)
     *  @ExtendWith(...)    Register extensions (SpringExtension, MockitoExtension, etc.)
     *  @RegisterExtension  Register extension instances as fields
     *  @TestInstance       Control test instance lifecycle (PER_CLASS for non-static @BeforeAll)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * @ExtendWith — THE MOST IMPORTANT EXTENSION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  @ExtendWith(SpringExtension.class)   → enables Spring TestContext Framework
     *    Already included in: @SpringBootTest, @WebMvcTest, @DataJpaTest
     *    Use directly for: @ContextConfiguration tests, Spring tests without slices
     *
     *  @ExtendWith(MockitoExtension.class)  → enables @Mock, @Spy, @InjectMocks
     *    Use for: pure unit tests without Spring (fastest tests)
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    SECTION 10: ADVANCED SPRING TEST ANNOTATIONS — QUICK REFERENCE           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @DirtiesContext:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   Signals that the test has dirtied/modified the Spring ApplicationContext.
     *   Spring will CLOSE and REBUILD the context after this test.
     *
     *   classMode:
     *     AFTER_CLASS (default)  → rebuild after all tests in this class
     *     BEFORE_CLASS           → rebuild before all tests in this class
     *     AFTER_EACH_TEST_METHOD → rebuild after EACH test method (expensive!)
     *     BEFORE_EACH_TEST_METHOD → rebuild before EACH test method
     *
     *   WHY: Spring caches the ApplicationContext by default for performance.
     *        If your test changes global state (static fields, singletons, etc.),
     *        use @DirtiesContext to force a fresh context for subsequent tests.
     *
     *   COST: Rebuilding the context is SLOW (seconds). Use sparingly.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @TestPropertySource:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   Overrides application properties for tests.
     *   Higher precedence than application.yaml — your test overrides always win.
     *
     *   locations → load from a .properties file
     *     @TestPropertySource(locations = "classpath:test-overrides.properties")
     *
     *   properties → inline key=value pairs
     *     @TestPropertySource(properties = {"spring.jpa.show-sql=false", "app.feature=off"})
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @ActiveProfiles:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   Activates Spring profiles for the test.
     *   @ActiveProfiles("test") → activates "test" profile
     *   Spring loads: application.yaml AND application-test.yaml
     *   @Component/@Bean with @Profile("test") become active
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Sql & @SqlGroup:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   Executes SQL scripts before/after a test.
     *   Useful for setting up test data that rollback cleans up.
     *
     *   @Sql(scripts = "/sql/insert-todos.sql")
     *   → Runs the SQL script before the test method executes.
     *
     *   executionPhase:
     *     BEFORE_TEST_METHOD (default) → before the test
     *     AFTER_TEST_METHOD            → after the test (for cleanup)
     *
     *   @SqlGroup → container for multiple @Sql annotations:
     *     @SqlGroup({
     *         @Sql(scripts = "/sql/cleanup.sql"),
     *         @Sql(scripts = "/sql/seed.sql")
     *     })
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @AutoConfigureMockMvc:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   Used with @SpringBootTest to auto-configure a MockMvc instance.
     *   Allows using MockMvc in a full @SpringBootTest (not just @WebMvcTest).
     *
     *   @SpringBootTest
     *   @AutoConfigureMockMvc
     *   class MyIntegrationTest {
     *       @Autowired MockMvc mockMvc;  // Available via auto-configuration
     *   }
     *
     *   DIFFERENCE FROM @WebMvcTest:
     *     @WebMvcTest      → loads ONLY MVC layer
     *     @SpringBootTest + @AutoConfigureMockMvc → loads EVERYTHING + gives you MockMvc
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    SECTION 13: BEST PRACTICES & COMMON PITFALLS                             ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ✅ BEST PRACTICES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  1. Prefer @WebMvcTest over @SpringBootTest for controller tests.
     *     @WebMvcTest is 10–50x faster. Only tests fail fast when controller logic breaks.
     *
     *  2. Use @DataJpaTest for repository tests — real SQL, real entity mapping.
     *     Don't mock repositories to test them.
     *
     *  3. Use pure JUnit 5 + Mockito (@ExtendWith(MockitoExtension.class)) for services.
     *     No Spring context needed for business logic tests.
     *
     *  4. Keep Spring context reuse high.
     *     The context is cached by its configuration key. Every @MockBean creates a
     *     NEW unique context. Fewer @MockBeans = more context reuse = faster CI.
     *
     *  5. Use @DisplayName for readable test names.
     *     "createTodo_whenTitleIsBlank_shouldReturn400" → hard to read in reports.
     *     @DisplayName("POST /todos with blank title → 400 Bad Request") → clear.
     *
     *  6. Use @Nested to group related scenarios together.
     *     Nested classes create a visible hierarchy in test reports.
     *
     *  7. Follow the AAA pattern in every test:
     *     Arrange → set up the state
     *     Act     → call the method under test
     *     Assert  → verify the result
     *
     *  8. Use AssertJ over JUnit assertions for richer, more readable failures.
     *     assertThat(todo.getTitle()).isEqualTo("Buy milk") gives better error messages.
     *
     *  9. Use @TestPropertySource to avoid polluting application.yaml with test config.
     *
     * 10. Use @DirtiesContext sparingly — it's a performance killer.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ❌ COMMON PITFALLS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PITFALL 1: Using @SpringBootTest when @WebMvcTest is sufficient.
     *    Problem: Full context startup (seconds) when only MVC layer is needed.
     *    Fix:     Use the most focused test slice for your use case.
     *
     *  PITFALL 2: @BeforeAll method is not static.
     *    Problem: JUnit throws PreconditionViolationException at runtime.
     *    Fix:     Make @BeforeAll / @AfterAll methods static.
     *             OR add @TestInstance(TestInstance.Lifecycle.PER_CLASS) to class.
     *
     *  PITFALL 3: @MockBean changes the Spring context causing cache miss.
     *    Problem: Every test class with different @MockBeans gets a new context.
     *    Fix:     Centralise @MockBeans in a shared base test class.
     *
     *  PITFALL 4: Forgetting @Transactional in @DataJpaTest custom methods.
     *    Problem: @DataJpaTest wraps each test in a transaction, but test helper
     *             methods called from tests may not join that transaction.
     *    Fix:     The whole test method is transactional — all code in it shares the tx.
     *
     *  PITFALL 5: @SpringBootTest with RANDOM_PORT and TestRestTemplate not injected.
     *    Problem: TestRestTemplate is only auto-configured for RANDOM_PORT and DEFINED_PORT.
     *    Fix:     Use RANDOM_PORT or DEFINED_PORT, then @Autowired TestRestTemplate.
     *
     *  PITFALL 6: Stateful @MockBean interactions leaking between tests.
     *    Problem: Mockito records are NOT reset between tests by default.
     *    Fix:     Use @BeforeEach reset.mockResetAll() or configure MockitoSettings.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    SECTION 14: INTERVIEW QUESTIONS & ANSWERS                                 ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Q1: What is the difference between @Mock and @MockBean?
     *
     *     @Mock (Mockito): Creates a Mockito mock with NO Spring involvement.
     *     Used with @ExtendWith(MockitoExtension.class) in pure unit tests.
     *     Injected via @InjectMocks or manually. Spring context is NOT involved.
     *
     *     @MockBean (Spring Test): Creates a Mockito mock AND REGISTERS IT IN the
     *     Spring ApplicationContext, REPLACING any existing bean of the same type.
     *     Used in @SpringBootTest, @WebMvcTest, etc. where Spring context is needed.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q2: What is @WebMvcTest and what does it load?
     *
     *     @WebMvcTest creates a "sliced" Spring context that loads ONLY the web layer:
     *     @Controller, @RestController, @ControllerAdvice, @JsonComponent, Filters,
     *     WebMvcConfigurer, MockMvc (auto-configured). It does NOT load @Service,
     *     @Repository, or @Component beans. Services must be mocked with @MockBean.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q3: How does Spring Test context caching work?
     *
     *     Spring Test caches ApplicationContexts keyed by the combination of:
     *     (configuration classes, active profiles, property sources, context loaders,
     *     @MockBean definitions). If two test classes share the same key, the SAME
     *     cached context is reused — saving startup time. Adding @DirtiesContext or
     *     different @MockBeans breaks the cache and forces a new context.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q4: What is @DirtiesContext and when should you use it?
     *
     *     @DirtiesContext tells Spring to close and discard the ApplicationContext
     *     after the annotated test (or test class). Use it when a test modifies global
     *     state that would affect other tests (e.g., changes a singleton bean's state,
     *     modifies a static field, changes the embedded server port). Use sparingly —
     *     rebuilding the context is slow (1–5 seconds per rebuild).
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q5: What is the difference between @SpringBootTest and @DataJpaTest?
     *
     *     @SpringBootTest loads the FULL ApplicationContext (all beans, all auto-configs).
     *     Suitable for integration tests that need the whole system.
     *     Slow to start (all beans created, auto-configs evaluated).
     *
     *     @DataJpaTest loads a SLICED context with ONLY JPA-related beans:
     *     @Entity classes, repositories, DataSource, EntityManagerFactory.
     *     Uses an embedded H2 by default, rolls back after each test.
     *     Much faster than @SpringBootTest.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q6: What is @ParameterizedTest and how does it work?
     *
     *     @ParameterizedTest marks a test that runs multiple times with different
     *     argument sets. Combined with argument sources:
     *     - @ValueSource: simple literal values (strings, ints, etc.)
     *     - @CsvSource: comma-separated tuples (each row = one invocation)
     *     - @MethodSource: a factory method returning a Stream of Arguments
     *     - @EnumSource: enum constants
     *     JUnit 5 invokes the test method once per argument combination,
     *     each as a separate test case with its own pass/fail result.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q7: How do you write a test for a @RestController using @WebMvcTest?
     *
     *     1. Annotate test class with @WebMvcTest(YourController.class)
     *     2. Inject MockMvc via @Autowired
     *     3. Add @MockBean for every service/repository the controller depends on
     *     4. Configure Mockito stubs: when(service.method(any())).thenReturn(...)
     *     5. Call mockMvc.perform(get("/endpoint")).andExpect(status().isOk())
     *     6. Verify assertions with .andExpect(jsonPath("$.field").value("expected"))
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q8: What is the difference between @ActiveProfiles and @TestPropertySource?
     *
     *     @ActiveProfiles("test"): Activates a Spring profile, causing Spring to
     *     load application-test.yaml and activate @Profile("test") beans.
     *     Changes WHICH beans are loaded.
     *
     *     @TestPropertySource(properties = {"key=value"}): Overrides specific
     *     property values in the test environment. Higher priority than application.yaml.
     *     Changes PROPERTY VALUES, not which beans are loaded.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                   ║");
        System.out.println("║     SPRING BOOT ANNOTATIONS — CHAPTER 8 OVERVIEW                 ║");
        System.out.println("║              Spring Testing Annotations                           ║");
        System.out.println("║                                                                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("📦 Dependency included: spring-boot-starter-test");
        System.out.println("   → JUnit 5, Mockito, AssertJ, Hamcrest, Spring Test, JsonPath");
        System.out.println();
        System.out.println("🗂️  Main files (explanations):");
        System.out.println("   1. Chapter08Overview.java         ← YOU ARE HERE");
        System.out.println("   2. Example01SpringBootTestAnnotations.java");
        System.out.println("   3. Example02MockBeanAndSpyBean.java");
        System.out.println("   4. Example03JUnitAnnotations.java");
        System.out.println("   5. Example04AdvancedTestingAnnotations.java");
        System.out.println("   6. HowItWorksExplained.java");
        System.out.println();
        System.out.println("🧪 Test files (REAL runnable tests in src/test/java):");
        System.out.println("   1. Example01_SpringBootIntegrationTest.java  → @SpringBootTest");
        System.out.println("   2. Example02_WebMvcControllerTest.java       → @WebMvcTest");
        System.out.println("   3. Example03_DataJpaRepositoryTest.java      → @DataJpaTest");
        System.out.println("   4. Example04_MockBeanSpyBeanTest.java        → @MockBean/@SpyBean");
        System.out.println("   5. Example05_JUnitAnnotationsTest.java       → JUnit 5 showcase");
        System.out.println("   6. Example06_AdvancedAnnotationsTest.java    → @Sql, @ActiveProfiles");
        System.out.println();
        System.out.println("▶  Run all tests: ./gradlew test");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

