package com.learning.springboot.chapter08;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   EXAMPLE 04: ADVANCED TESTING ANNOTATIONS                                          ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example04AdvancedTestingAnnotations.java
 * Purpose:     Deep-dive into @DirtiesContext, @TestPropertySource, @ActiveProfiles,
 *              @Sql / @SqlGroup / @SqlConfig, @Rollback, @Commit, @AutoConfigureMockMvc,
 *              @AutoConfigureTestDatabase, and testing strategies.
 * Difficulty:  ⭐⭐⭐⭐ Intermediate–Advanced
 * Time:        45–60 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
public class Example04AdvancedTestingAnnotations {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║       @DirtiesContext — FORCE CONTEXT REBUILD                                 ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 THE PROBLEM: Context Pollution
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Spring Test CACHES the ApplicationContext between test classes for performance.
     * If Test A modifies shared state in the context, Test B (using the same cached
     * context) may fail because it inherits Test A's side effects.
     *
     * EXAMPLES OF CONTEXT POLLUTION:
     *   → Your test modifies a static field inside a Spring bean
     *   → Your test calls a shutdown method on a singleton
     *   → Your test changes Spring Security's SecurityContext permanently
     *   → Your test modifies application properties via Environment
     *   → Your test starts/stops embedded servers on specific ports
     *
     * SOLUTION: @DirtiesContext
     *   Tells Spring: "this test has modified the context — don't reuse it."
     *   Spring will CLOSE and DISCARD the context after this test.
     *   The NEXT test that needs this context will get a FRESH one.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 classMode VALUES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   AFTER_CLASS (default when on class)
     *   → Rebuild ONCE after ALL tests in the class complete.
     *   → Most common. Use when the class as a whole dirties the context.
     *
     *   BEFORE_CLASS
     *   → Rebuild BEFORE any test in this class runs.
     *   → Use when you need a fresh context specifically for this class.
     *
     *   AFTER_EACH_TEST_METHOD
     *   → Rebuild after EVERY single test method. Very expensive!
     *   → Use only if each test method independently dirties the context.
     *
     *   BEFORE_EACH_TEST_METHOD
     *   → Rebuild before every test method. Extremely expensive!
     *   → Rarely needed; use @DataJpaTest rollback instead.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 USAGE PATTERN:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @SpringBootTest
     *   @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
     *   class StatefulIntegrationTest {
     *
     *       @Autowired ApplicationContext context;
     *       @Autowired TodoService todoService;
     *
     *       @Test
     *       void test_thatModifiesSomething() {
     *           // This test modifies a singleton's state in a way that affects others
     *           // @DirtiesContext ensures next test gets a clean context
     *       }
     *   }
     *
     *   // On a METHOD (not the whole class):
     *   @Test
     *   @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
     *   void specificTest_thatDirtiesContext() {
     *       // Only this test dirties the context
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ⚠️  COST WARNING:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Rebuilding a Spring Boot application context can take 2–10 seconds.
     * If you have 20 test classes each with @DirtiesContext: 20 × 5s = 100s extra.
     *
     * ALTERNATIVES TO @DirtiesContext:
     *   → Reset state in @BeforeEach / @AfterEach instead
     *   → Use @DataJpaTest (auto-rolls back database changes)
     *   → Store state in ThreadLocal and clean up in @AfterEach
     *   → Design beans to be stateless
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║       @TestPropertySource — OVERRIDE PROPERTIES FOR TESTS                    ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @TestPropertySource overrides properties from application.yaml / application.properties
     * for the duration of a test class. Test property overrides have the HIGHEST
     * priority — they always win over any other property source.
     *
     * TWO WAYS TO USE IT:
     *
     * 1. Inline properties (most common — quick overrides):
     *    @TestPropertySource(properties = {
     *        "spring.jpa.show-sql=false",
     *        "app.feature.notifications=false",
     *        "logging.level.org.hibernate=WARN"
     *    })
     *
     * 2. From a .properties file:
     *    @TestPropertySource(locations = "classpath:test-application.properties")
     *
     *    // File: src/test/resources/test-application.properties
     *    spring.jpa.show-sql=false
     *    app.max-todos=100
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 USE CASES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  → Silence SQL logging during tests for cleaner output
     *  → Disable feature flags in tests
     *  → Override external service URLs (use fake URLs for integration tests)
     *  → Set specific limits for test scenarios
     *  → Override timeouts for faster tests
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 ALTERNATIVE: @SpringBootTest(properties = {...})
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @SpringBootTest(properties = {"spring.jpa.show-sql=false"})
     *   class MyTest { }
     *
     *   This is equivalent to @TestPropertySource(properties = {...}).
     *   Use @SpringBootTest(properties) when you're already using @SpringBootTest.
     *   Use @TestPropertySource when you want to be explicit about it, or when
     *   combining with other @ExtendWith configurations without @SpringBootTest.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 USAGE PATTERN — See real implementation in Example06_AdvancedAnnotationsTest:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @SpringBootTest
     *   @TestPropertySource(properties = {
     *       "spring.jpa.show-sql=false",
     *       "logging.level.root=WARN"       // silence almost all logging
     *   })
     *   class SilentIntegrationTest {
     *
     *       @Value("${spring.jpa.show-sql}")
     *       boolean showSql;
     *
     *       @Test
     *       void showSql_shouldBeFalse() {
     *           assertThat(showSql).isFalse();
     *       }
     *   }
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║       @ActiveProfiles — ACTIVATE SPRING PROFILES FOR TESTS                   ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @ActiveProfiles activates one or more Spring profiles for the test.
     * When a profile is active:
     *   → application-{profile}.yaml is loaded and merged with application.yaml
     *   → @Profile("{profile}") annotated beans are instantiated
     *   → @Profile("!{profile}") beans are NOT instantiated
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 COMMON PATTERNS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Pattern 1: Dedicated test profile
     *
     *   // application-test.yaml:
     *   spring:
     *     datasource:
     *       url: jdbc:h2:mem:testdb   # in-memory H2 for tests
     *     jpa:
     *       show-sql: false           # quiet for tests
     *   app:
     *     notifications:
     *       enabled: false            # don't send real emails in tests
     *
     *   @SpringBootTest
     *   @ActiveProfiles("test")
     *   class MyTest { ... }          // loads application.yaml + application-test.yaml
     *
     * Pattern 2: Multiple profiles
     *
     *   @ActiveProfiles({"test", "no-security"})
     *   // Loads test config AND disables security for this test class
     *
     * Pattern 3: Profile-specific beans
     *
     *   @Service
     *   @Profile("test")              // Only created when "test" profile is active
     *   class FakeEmailService implements EmailService {
     *       // Records emails in memory instead of sending
     *   }
     *
     *   @Service
     *   @Profile("!test")             // Created in all profiles EXCEPT "test"
     *   class RealEmailService implements EmailService { ... }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @ActiveProfiles vs @TestPropertySource:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @ActiveProfiles("test")
     *   → Activates a profile → loads a whole configuration file
     *   → Also activates profile-specific beans
     *   → Use when you have a test-specific application-test.yaml
     *
     *   @TestPropertySource(properties = {"key=value"})
     *   → Overrides specific properties inline
     *   → No profile activation, just property overrides
     *   → Use for quick one-off property changes
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║       @Sql / @SqlGroup — EXECUTE SQL SCRIPTS IN TESTS                        ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @Sql executes SQL scripts before or after a test method/class.
     * Works with @SpringBootTest and @DataJpaTest (any test with a DataSource).
     *
     * PACKAGE: org.springframework.test.context.jdbc.Sql
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 KEY ATTRIBUTES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   scripts    → array of SQL file paths (classpath: prefix supported)
     *   statements → inline SQL strings (for simple cases)
     *   executionPhase →
     *     BEFORE_TEST_METHOD (default) → before the test
     *     AFTER_TEST_METHOD            → after the test (cleanup)
     *     BEFORE_TEST_CLASS            → before any test in the class
     *     AFTER_TEST_CLASS             → after all tests in the class
     *   config     → @SqlConfig for connection mode, error handling, etc.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 USAGE PATTERNS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * PATTERN 1 — Simple inline SQL (for small setups):
     *
     *   @Test
     *   @Sql(statements = "INSERT INTO ch08_todos (title, completed, priority) VALUES ('SQL Task', false, 1)")
     *   void findById_shouldReturnInsertedTodo() {
     *       List<Todo> todos = todoRepository.findAll();
     *       assertThat(todos).hasSize(1);
     *       assertThat(todos.get(0).getTitle()).isEqualTo("SQL Task");
     *   }
     *
     * PATTERN 2 — SQL file (for larger setups):
     *
     *   // File: src/test/resources/sql/seed-todos.sql
     *   // INSERT INTO ch08_todos (title, completed, priority) VALUES ('Task 1', false, 1);
     *   // INSERT INTO ch08_todos (title, completed, priority) VALUES ('Task 2', false, 2);
     *   // INSERT INTO ch08_todos (title, completed, priority) VALUES ('Task 3', true, 3);
     *
     *   @Test
     *   @Sql(scripts = "/sql/seed-todos.sql")
     *   void findByCompleted_shouldReturnTwo() {
     *       List<Todo> pending = todoRepository.findByCompleted(false);
     *       assertThat(pending).hasSize(2);
     *   }
     *
     * PATTERN 3 — Cleanup after test (AFTER_TEST_METHOD):
     *
     *   @Test
     *   @Sql(statements = "INSERT INTO ch08_todos (title,completed,priority) VALUES ('T',false,1)")
     *   @Sql(statements = "DELETE FROM ch08_todos WHERE title='T'",
     *        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
     *   void testWithCleanup() { ... }
     *
     * PATTERN 4 — @SqlGroup for multiple @Sql annotations:
     *
     *   @Test
     *   @SqlGroup({
     *       @Sql(scripts = "/sql/cleanup.sql"),    // clean first
     *       @Sql(scripts = "/sql/seed-todos.sql")  // then seed
     *   })
     *   void testWithMultipleSqlScripts() { ... }
     *
     * PATTERN 5 — Class-level @Sql (runs before all tests in class):
     *
     *   @DataJpaTest
     *   @Sql(scripts = "/sql/seed-todos.sql",
     *        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
     *   class TodoRepositoryBulkTest { ... }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Rollback & @Commit — Control transaction behaviour:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * In @DataJpaTest, every test method is rolled back automatically.
     *
     * @Rollback(false) or @Commit:
     *   → Commit the transaction (don't roll back).
     *   → Use for integration tests where you want persistent state.
     *   → Be careful: manual cleanup needed afterwards.
     *
     *   @Test
     *   @Commit    // Override @DataJpaTest's rollback behaviour
     *   void persistTodo_andKeepInDb() {
     *       todoRepository.save(new Todo("Permanent task"));
     *       // This persists to DB and is NOT rolled back
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @AutoConfigureMockMvc — MockMvc in full integration tests:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @WebMvcTest gives you MockMvc automatically.
     * For @SpringBootTest with MOCK environment, you need @AutoConfigureMockMvc.
     *
     *   @SpringBootTest                  // Full context
     *   @AutoConfigureMockMvc            // Auto-configure MockMvc
     *   class FullIntegrationTest {
     *
     *       @Autowired MockMvc mockMvc;  // Now available
     *       @Autowired TodoRepository todoRepository;  // Real repository
     *
     *       @BeforeEach
     *       void setUp() { todoRepository.deleteAll(); }
     *
     *       @Test
     *       void createAndRetrieveTodo_fullStack() throws Exception {
     *           // Create via HTTP
     *           mockMvc.perform(post("/api/ch08/todos")
     *               .contentType(APPLICATION_JSON)
     *               .content("""{"title":"Full Stack Task","priority":2}"""))
     *               .andExpect(status().isCreated());
     *
     *           // Verify via DB
     *           assertThat(todoRepository.count()).isEqualTo(1);
     *           assertThat(todoRepository.findAll().get(0).getTitle())
     *               .isEqualTo("Full Stack Task");
     *       }
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @AutoConfigureTestDatabase — Control the test database:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @DataJpaTest
     *   @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
     *   class UseRealDatabaseTest {
     *       // Uses YOUR configured datasource (our H2 in application.yaml)
     *       // instead of the default embedded H2
     *   }
     *
     *   @DataJpaTest
     *   @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
     *   class UseEmbeddedDatabaseTest {
     *       // Default behaviour: replace with H2 embedded database
     *   }
     *
     *   USE Replace.NONE WHEN:
     *   → You need PostgreSQL-specific SQL features in tests
     *   → You want to test against Testcontainers (real Postgres in Docker)
     *   → Your queries use DB-vendor-specific syntax
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║   CHAPTER 8 — EXAMPLE 04: Advanced Testing Annotations           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  @DirtiesContext          → Force Spring context rebuild");
        System.out.println("    ClassMode: AFTER_CLASS, BEFORE_CLASS, AFTER_EACH_TEST_METHOD");
        System.out.println("    Cost: rebuilding context takes seconds — use sparingly!");
        System.out.println();
        System.out.println("  @TestPropertySource       → Override properties for tests");
        System.out.println("    properties = {\"key=value\"} or locations = \"classpath:file.properties\"");
        System.out.println();
        System.out.println("  @ActiveProfiles           → Activate Spring profiles");
        System.out.println("    Loads application-{profile}.yaml + activates @Profile beans");
        System.out.println();
        System.out.println("  @Sql                      → Execute SQL before/after tests");
        System.out.println("    scripts, statements, executionPhase (BEFORE/AFTER)");
        System.out.println("  @SqlGroup                 → Multiple @Sql annotations");
        System.out.println("  @Rollback / @Commit       → Control @DataJpaTest transaction");
        System.out.println();
        System.out.println("  @AutoConfigureMockMvc     → MockMvc in @SpringBootTest");
        System.out.println("  @AutoConfigureTestDatabase → Control test datasource");
        System.out.println();
        System.out.println("▶  See REAL tests in: Example06_AdvancedAnnotationsTest.java");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

