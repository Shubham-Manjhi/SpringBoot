package com.learning.springboot.chapter08;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   CHAPTER 8 — TEST FILE 06: Advanced Testing Annotations                            ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * WHAT IS DEMONSTRATED:
 *   → @TestPropertySource — override properties for tests
 *   → @ActiveProfiles — activate Spring profiles
 *   → @Sql / @SqlGroup — execute SQL before/after tests
 *   → @DirtiesContext — force context rebuild
 *   → Combining @DataJpaTest + @Sql for data-driven repository tests
 *
 * KEY ANNOTATIONS DEMONSTRATED:
 *   @TestPropertySource, @ActiveProfiles, @Sql, @SqlGroup
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ════════════════════════════════════════════════════════════════════════════════════════
//  TEST CLASS 1: @TestPropertySource — Override properties inline
// ════════════════════════════════════════════════════════════════════════════════════════

/**
 * Demonstrates @TestPropertySource: overrides specific properties for this test class.
 * These overrides have HIGHEST priority (win over application.yaml).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    /*
     * @TestPropertySource(properties = {...}):
     * Each string is a "key=value" pair that overrides the same key in application.yaml.
     *
     * These values are read during Spring context creation and have priority over
     * ALL other property sources (application.yaml, application-*.yaml, env variables).
     *
     * Perfect for: disabling noisy logging, turning off features, setting test-specific values.
     */
    "spring.jpa.show-sql=false",           // silence SQL output in this test
    "logging.level.root=WARN",             // minimal logging
    "spring.application.name=TestApp"      // override app name for this test
})
@DisplayName("Chapter 8 — @TestPropertySource: Property Override Tests")
class Example06a_TestPropertySourceTest {

    /*
     * @Value injects a property value from the environment.
     * The @TestPropertySource override ensures we get "false" here.
     */
    @Value("${spring.jpa.show-sql}")
    private boolean showSql;

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private TodoRepository todoRepository;

    @Test
    @DisplayName("@TestPropertySource → spring.jpa.show-sql overridden to false")
    void showSql_shouldBeFalse_dueToTestPropertySource() {
        assertThat(showSql).isFalse();
    }

    @Test
    @DisplayName("@TestPropertySource → spring.application.name overridden to 'TestApp'")
    void appName_shouldBeTestApp() {
        assertThat(appName).isEqualTo("TestApp");
    }

    @Test
    @DisplayName("application still works → repository accessible despite property overrides")
    void repository_shouldStillWork_withOverriddenProperties() {
        todoRepository.deleteAll();
        todoRepository.save(new Todo("Property override test", 1));
        assertThat(todoRepository.count()).isEqualTo(1);
        todoRepository.deleteAll();
    }
}


// ════════════════════════════════════════════════════════════════════════════════════════
//  TEST CLASS 2: @ActiveProfiles
// ════════════════════════════════════════════════════════════════════════════════════════

/**
 * Demonstrates @ActiveProfiles: activates a Spring profile for this test.
 *
 * NOTE: We activate "test" profile here.
 * If application-test.yaml doesn't exist, Spring simply skips it (no error).
 * The profile activation still affects @Profile("test") beans.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")   // Activates "test" profile → loads application-test.yaml if it exists
@DisplayName("Chapter 8 — @ActiveProfiles: Profile Activation Test")
class Example06b_ActiveProfilesTest {

    /*
     * ActiveProfiles allows environment-specific bean wiring:
     *
     * Production:
     *   @Service @Profile("!test") class RealEmailService {...}
     *
     * Test:
     *   @Service @Profile("test") class FakeEmailService {...}
     *
     * With @ActiveProfiles("test"): FakeEmailService is loaded instead of RealEmailService.
     *
     * In our project, we don't have profile-specific beans yet,
     * but we demonstrate the annotation is correctly applied.
     */
    @Autowired
    private org.springframework.core.env.Environment environment;

    @Test
    @DisplayName("@ActiveProfiles('test') → 'test' profile is active")
    void testProfile_shouldBeActive() {
        assertThat(environment.getActiveProfiles()).contains("test");
    }

    @Test
    @DisplayName("core application beans still load with 'test' profile active")
    void applicationBeans_shouldStillLoad() {
        // Context is fully loaded — our domain beans are available
        assertThat(environment).isNotNull();
    }
}


// ════════════════════════════════════════════════════════════════════════════════════════
//  TEST CLASS 3: @Sql — Execute SQL scripts in tests
// ════════════════════════════════════════════════════════════════════════════════════════

/**
 * Demonstrates @Sql: execute SQL statements/scripts before/after tests.
 * Uses @DataJpaTest for the JPA slice (fast, rolls back after each test).
 */
@DataJpaTest
@DisplayName("Chapter 8 — @Sql: Data setup via SQL statements")
class Example06c_SqlAnnotationTest {

    @Autowired
    private TodoRepository todoRepository;

    /*
     * NO @BeforeEach cleanUp here!
     *
     * WHY: Spring's SqlScriptsTestExecutionListener runs @Sql BEFORE @BeforeEach.
     * If we had deleteAll() in @BeforeEach it would delete what @Sql just inserted.
     * @DataJpaTest auto-rolls back after each test — so no cleanup is needed.
     */

    /*
     * @Sql(statements = "..."):
     * Executes an inline SQL statement BEFORE the test method runs (default phase).
     * Useful for injecting data without going through the repository/service.
     * The data is visible within the same transaction as the test.
     *
     * IMPORTANT: The SQL runs within the test's transaction (which rolls back afterwards).
     * So the inserted data is automatically cleaned up — no manual teardown needed.
     */
    @Test
    @Sql(statements = {
        "INSERT INTO ch08_todos (title, completed, priority) VALUES ('SQL Task 1', false, 1)",
        "INSERT INTO ch08_todos (title, completed, priority) VALUES ('SQL Task 2', false, 2)",
        "INSERT INTO ch08_todos (title, completed, priority) VALUES ('SQL Done',   true,  3)"
    })
    @DisplayName("@Sql(statements) → inserts 3 rows → findAll returns 3 todos")
    void sqlStatements_shouldInsertRows_beforeTest() {
        // ASSERT: data inserted via @Sql is visible in this transaction
        List<Todo> all = todoRepository.findAll();
        assertThat(all).hasSize(3);

        List<Todo> pending = todoRepository.findByCompleted(false);
        assertThat(pending).hasSize(2);

        List<Todo> completed = todoRepository.findByCompleted(true);
        assertThat(completed).hasSize(1);
        assertThat(completed.get(0).getTitle()).isEqualTo("SQL Done");
    }

    /*
     * @SqlGroup: container for multiple @Sql annotations.
     * Runs BOTH SQL statements before the test.
     */
    @Test
    @SqlGroup({
        @Sql(statements = "INSERT INTO ch08_todos (title, completed, priority) VALUES ('Group Task 1', false, 3)"),
        @Sql(statements = "INSERT INTO ch08_todos (title, completed, priority) VALUES ('Group Task 2', false, 1)")
    })
    @DisplayName("@SqlGroup → multiple @Sql annotations → both run before test")
    void sqlGroup_shouldRunBothStatements() {
        assertThat(todoRepository.count()).isEqualTo(2);
        assertThat(todoRepository.findByPriority(3)).hasSize(1);
        assertThat(todoRepository.findByPriority(1)).hasSize(1);
    }

    /*
     * @Sql with executionPhase = AFTER_TEST_METHOD:
     * Runs the SQL AFTER the test. Useful for cleanup of data that was committed.
     *
     * In @DataJpaTest, data is rolled back automatically, so AFTER phase is rarely
     * needed. It's more relevant in @SpringBootTest with real commit semantics.
     */
    @Test
    @Sql(statements = "INSERT INTO ch08_todos (title, completed, priority) VALUES ('Before', false, 1)")
    @Sql(statements = "DELETE FROM ch08_todos WHERE title = 'Before'",
         executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @DisplayName("@Sql BEFORE + AFTER → setup and cleanup in same test")
    void sqlBeforeAndAfter_shouldSetupAndCleanup() {
        // Before: 1 row from first @Sql
        assertThat(todoRepository.count()).isEqualTo(1);
        assertThat(todoRepository.findAll().get(0).getTitle()).isEqualTo("Before");
        // After: second @Sql deletes it (demonstrates the AFTER_TEST_METHOD phase)
    }

    /*
     * Combining @Sql with repository operations:
     * Use @Sql to set up baseline data, then test queries against it.
     */
    @Test
    @Sql(statements = {
        "INSERT INTO ch08_todos (title, completed, priority) VALUES ('High 1', false, 3)",
        "INSERT INTO ch08_todos (title, completed, priority) VALUES ('High 2', false, 3)",
        "INSERT INTO ch08_todos (title, completed, priority) VALUES ('Low 1',  false, 1)"
    })
    @DisplayName("@Sql seed + custom JPQL query → returns correct filtered results")
    void jpqlQuery_withSqlSeedData_shouldFilterCorrectly() {
        List<Todo> highPriority = todoRepository.findPendingByPriority(3);

        assertThat(highPriority).hasSize(2);
        assertThat(highPriority)
            .extracting(Todo::getTitle)
            .containsExactlyInAnyOrder("High 1", "High 2");
    }
}


// ════════════════════════════════════════════════════════════════════════════════════════
//  TEST CLASS 4: @DirtiesContext (minimal demonstration)
// ════════════════════════════════════════════════════════════════════════════════════════

/**
 * Demonstrates @DirtiesContext: signals that this test class modifies the context.
 *
 * After all tests in this class complete, Spring discards the cached context
 * and will rebuild it for the next test that needs the same context.
 *
 * COST: Adds startup time for the NEXT test needing the same context.
 * USE SPARINGLY — only when a test genuinely modifies shared global state.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@org.springframework.test.annotation.DirtiesContext(
    classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS
)
@DisplayName("Chapter 8 — @DirtiesContext: Context rebuild after class")
class Example06d_DirtiesContextTest {

    @Autowired
    private TodoRepository todoRepository;

    @BeforeEach
    void cleanUp() {
        todoRepository.deleteAll();
    }

    @Test
    @DisplayName("context loaded → basic operation works → context will be dirtied")
    void contextLoads_andBasicOperationWorks() {
        /*
         * This test demonstrates the annotation is present.
         * In a real scenario, something in this test (or the class setup)
         * would modify global state — hence @DirtiesContext is needed.
         */
        todoRepository.save(new Todo("DirtiesContext test"));
        assertThat(todoRepository.count()).isEqualTo(1);
        // After this class: Spring discards and rebuilds the context.
    }
}

