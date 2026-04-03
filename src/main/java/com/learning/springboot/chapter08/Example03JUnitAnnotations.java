package com.learning.springboot.chapter08;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   EXAMPLE 03: JUnit 5 Annotations — Complete Deep Dive                              ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example03JUnitAnnotations.java
 * Purpose:     Every JUnit 5 annotation explained with patterns and examples:
 *              @Test, lifecycle, @ParameterizedTest (all sources), @Nested,
 *              @Tag, @Timeout, @RepeatedTest, @TempDir, @ExtendWith, @TestInstance
 * Difficulty:  ⭐⭐⭐ Intermediate
 * Time:        60–90 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
public class Example03JUnitAnnotations {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║       CORE LIFECYCLE ANNOTATIONS: @Test, @BeforeEach, @AfterEach, etc.       ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 EXECUTION ORDER:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @BeforeAll      ← runs ONCE (static) — class-level setup (e.g., start DB)
     *   ┌─── test 1 ───────────────────────────────────────────┐
     *   │   @BeforeEach  ← runs before THIS test (per-test setup)  │
     *   │   @Test methodA()  ← the actual test                      │
     *   │   @AfterEach   ← runs after THIS test (per-test teardown) │
     *   └──────────────────────────────────────────────────────────┘
     *   ┌─── test 2 ───────────────────────────────────────────┐
     *   │   @BeforeEach                                             │
     *   │   @Test methodB()                                         │
     *   │   @AfterEach                                              │
     *   └──────────────────────────────────────────────────────────┘
     *   @AfterAll       ← runs ONCE (static) — class-level teardown (e.g., stop DB)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 ANNOTATION DETAILS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @Test
     *   Marks a method as a test case.
     *   JUnit 5 discovers and runs it automatically.
     *   Method visibility: CAN be package-private (no public needed in JUnit 5!)
     *   Return type: must be void
     *   Parameters: usually none (use @ParameterizedTest for parameterized)
     *
     * @BeforeEach
     *   Runs before EACH @Test method in the class.
     *   Use for: resetting state, creating fresh test data, configuring mocks.
     *   Instance method (non-static). JUnit creates a NEW test instance per method.
     *
     * @AfterEach
     *   Runs after EACH @Test method.
     *   Use for: releasing resources, clearing caches, resetting static state.
     *   Runs even if the test throws an exception (cleanup is always executed).
     *
     * @BeforeAll
     *   Runs ONCE before all tests in the class.
     *   MUST be static (JUnit creates a new instance per test method by default).
     *   Exception: use @TestInstance(PER_CLASS) to allow non-static @BeforeAll.
     *   Use for: starting expensive resources (containers, servers, drivers).
     *
     * @AfterAll
     *   Runs ONCE after all tests in the class.
     *   Same static rule as @BeforeAll.
     *   Use for: stopping resources started in @BeforeAll.
     *
     * @Disabled("reason")
     *   Skips the test/class. JUnit reports it as SKIPPED (not failed).
     *   Always provide a reason: @Disabled("Broken — JIRA-1234 tracking this")
     *   Use when a test is temporarily non-functional, not to hide failures.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 PATTERN:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   class TodoServiceUnitTest {
     *
     *       @Mock TodoRepository todoRepository;
     *       @InjectMocks TodoService todoService;
     *
     *       @BeforeEach
     *       void setUp() {
     *           MockitoAnnotations.openMocks(this);  // Or use @ExtendWith(MockitoExtension)
     *       }
     *
     *       @Test
     *       @DisplayName("createTodo → title already exists → throws IllegalArgumentException")
     *       void createTodo_whenTitleExists_shouldThrow() {
     *           when(todoRepository.existsByTitle("Existing")).thenReturn(true);
     *
     *           assertThatThrownBy(() -> todoService.createTodo("Existing", 1))
     *               .isInstanceOf(IllegalArgumentException.class);
     *       }
     *
     *       @Test
     *       @Disabled("Broken — fix after refactoring issue #789")
     *       void createTodo_whenHighPriority_shouldNotify() {
     *           // TODO: fix after notification feature is stable
     *       }
     *   }
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║       @ParameterizedTest — RUN ONCE, TEST MANY SCENARIOS                     ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 WHY PARAMETERIZED TESTS?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Without @ParameterizedTest:
     *   @Test void validateEmail_valid1() { validate("a@b.com"); }
     *   @Test void validateEmail_valid2() { validate("user@example.org"); }
     *   @Test void validateEmail_invalid1() { validate("notanemail"); }
     *   → 3 nearly identical tests. Adding more means more copy-paste.
     *
     * With @ParameterizedTest:
     *   @ParameterizedTest
     *   @ValueSource(strings = {"a@b.com", "user@example.org"})
     *   void validateEmail_valid_shouldPass(String email) { ... }
     *   → One test definition, two executions. Add more emails with one line.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @ValueSource — Simple literal values:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Provides arrays of a single type. One execution per value.
     * Supported types: strings, ints, longs, doubles, floats, chars, bytes, shorts, booleans, classes
     *
     *   @ParameterizedTest
     *   @ValueSource(strings = {"", " ", "\t", "\n"})
     *   @DisplayName("validateTitle → blank values → should return false")
     *   void validateTitle_blank_shouldReturnFalse(String blank) {
     *       assertThat(blank.isBlank()).isTrue();
     *   }
     *
     *   @ParameterizedTest
     *   @ValueSource(ints = {-1, 0, 4, 100})
     *   void validatePriority_outOfRange_shouldFail(int invalidPriority) {
     *       // Test that priorities outside [1,3] are invalid
     *       assertThat(invalidPriority).isNotBetween(1, 3);
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @CsvSource — Multiple parameters per invocation:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Each CSV row = one test invocation. Multiple columns = multiple parameters.
     *
     *   @ParameterizedTest
     *   @CsvSource({
     *       "Buy milk,     1, true",   // title, priority, expected valid
     *       "Buy eggs,     2, true",
     *       "'',           1, false",  // empty title → invalid (quotes for empty string)
     *       "Valid title,  4, false",  // priority 4 → invalid
     *   })
     *   void createTodo_validation(String title, int priority, boolean expectedValid) {
     *       // Test validation logic for different combinations
     *   }
     *
     * NOTES:
     *   → Use '' for empty strings (single quotes in CSV)
     *   → Columns are mapped to parameters by POSITION
     *   → JUnit 5 does type conversion automatically (String → int, String → boolean)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @CsvFileSource — Load test data from a CSV file:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   // File: src/test/resources/test-todos.csv
     *   // title,priority,completed
     *   // Buy milk,1,false
     *   // Do laundry,2,false
     *   // Read book,3,true
     *
     *   @ParameterizedTest
     *   @CsvFileSource(resources = "/test-todos.csv", numLinesToSkip = 1)  // skip header
     *   void fromCsvFile(String title, int priority, boolean completed) {
     *       // Use parameters from file
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @MethodSource — Complex objects from a factory method:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Points to a static factory method that returns a Stream, List, or Iterable.
     * Use for complex objects or when CSV is too limiting.
     *
     *   @ParameterizedTest
     *   @MethodSource("provideTodoArguments")
     *   void fromMethodSource(String title, int priority, boolean expectedCompleted) {
     *       // ...
     *   }
     *
     *   // Factory method — must be static and return Stream/Iterable/Collection
     *   static Stream<Arguments> provideTodoArguments() {
     *       return Stream.of(
     *           Arguments.of("Buy milk", 1, false),
     *           Arguments.of("Do laundry", 2, false),
     *           Arguments.of("Read book", 3, true)
     *       );
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @EnumSource — Test with all enum values:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   enum Priority { LOW, MEDIUM, HIGH }
     *
     *   @ParameterizedTest
     *   @EnumSource(Priority.class)   // runs for LOW, MEDIUM, HIGH
     *   void someMethod_forEachPriority(Priority priority) { ... }
     *
     *   @ParameterizedTest
     *   @EnumSource(value = Priority.class, names = {"HIGH", "MEDIUM"})  // subset
     *   void someMethod_forHighAndMedium(Priority priority) { ... }
     *
     *   @ParameterizedTest
     *   @EnumSource(value = Priority.class, mode = EXCLUDE, names = {"LOW"})
     *   void someMethod_excludingLow(Priority priority) { ... }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @NullSource / @EmptySource / @NullAndEmptySource:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @NullSource    → provides a single null value
     *   @EmptySource   → provides "" (String), [] (array), [] (collection), etc.
     *   @NullAndEmptySource → both null + empty (2 invocations)
     *
     *   // Combine with @ValueSource for comprehensive blank testing:
     *   @ParameterizedTest
     *   @NullAndEmptySource
     *   @ValueSource(strings = {"  ", "\t", "\n"})
     *   void title_nullEmptyOrBlank_shouldFailValidation(String badTitle) {
     *       // Tests: null, "", " ", "\t", "\n" → all invalid titles
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 Customising the test name in parameterized tests:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @ParameterizedTest(name = "[{index}] ''{0}'' priority={1} → valid={2}")
     *   @CsvSource({"Buy milk, 1, true", "'', 1, false"})
     *   void test(String title, int priority, boolean valid) { }
     *   // In reports:  [1] 'Buy milk' priority=1 → valid=true
     *   //              [2] '' priority=1 → valid=false
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║       ADVANCED JUnit 5: @Nested, @Tag, @RepeatedTest, @Timeout, @TempDir    ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Nested — GROUP RELATED TESTS IN A HIERARCHY:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @Nested classes create a visual and logical hierarchy in test reports.
     * Inner classes CAN access outer class fields (non-static inner classes).
     *
     *   class TodoServiceTest {
     *
     *       @Nested
     *       @DisplayName("createTodo()")
     *       class WhenCreatingTodo {
     *
     *           @Test
     *           @DisplayName("with valid title → persists and returns todo")
     *           void validTitle_shouldPersist() { ... }
     *
     *           @Test
     *           @DisplayName("with duplicate title → throws IllegalArgumentException")
     *           void duplicateTitle_shouldThrow() { ... }
     *
     *           @Nested
     *           @DisplayName("with priority")
     *           class WithPriority {
     *               @Test void highPriority_shouldSetFlag() { ... }
     *               @Test void invalidPriority_shouldThrow() { ... }
     *           }
     *       }
     *
     *       @Nested
     *       @DisplayName("completeTodo()")
     *       class WhenCompletingTodo {
     *           @Test @DisplayName("existing todo → marks completed") void ... { }
     *           @Test @DisplayName("non-existent todo → throws not found") void ... { }
     *       }
     *   }
     *
     *   // Reports show a tree:
     *   // TodoServiceTest
     *   //   createTodo()
     *   //     ✓ with valid title → persists and returns todo
     *   //     ✓ with duplicate title → throws IllegalArgumentException
     *   //     with priority
     *   //       ✓ highPriority_shouldSetFlag
     *   //       ✓ invalidPriority_shouldThrow
     *   //   completeTodo()
     *   //     ✓ existing todo → marks completed
     *   //     ✓ non-existent todo → throws not found
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Tag — CATEGORISE TESTS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @Tag("fast")        → pure unit test, no I/O
     *   @Tag("slow")        → integration test, requires DB or network
     *   @Tag("smoke")       → sanity check for basic functionality
     *   @Tag("regression")  → detects previously fixed bugs
     *
     *   // Run ONLY fast tests: ./gradlew test --tests "*.chapter08.*" -Ptags=fast
     *   // Or configure in build.gradle:
     *   test {
     *       useJUnitPlatform {
     *           includeTags 'fast'
     *           excludeTags 'slow'
     *       }
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @RepeatedTest — RUN THE SAME TEST MULTIPLE TIMES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @RepeatedTest(5)   // Runs 5 times — each is a separate test case
     *   @DisplayName("Todo creation")
     *   void createTodo_shouldAlwaysProduceUniqueId(RepetitionInfo info) {
     *       System.out.println("Running repetition " + info.getCurrentRepetition()
     *                         + " of " + info.getTotalRepetitions());
     *       Todo todo = new Todo("Task " + info.getCurrentRepetition());
     *       assertThat(todo).isNotNull();
     *   }
     *
     *   USE CASES:
     *   → Testing concurrency (run multiple times, check for race conditions)
     *   → Detecting flaky tests (run 10 times, should always pass)
     *   → Performance sampling (run 100 times, measure average time)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Timeout — TEST MUST COMPLETE WITHIN A TIME LIMIT:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @Test
     *   @Timeout(value = 5, unit = TimeUnit.SECONDS)
     *   void createManyTodos_shouldCompleteWithin5Seconds() throws Exception {
     *       for (int i = 0; i < 1000; i++) {
     *           todoService.createTodo("Task " + i, 1);
     *       }
     *       // If this takes > 5 seconds → test FAILS with TimeoutException
     *   }
     *
     *   DIFFERENCE FROM assertTimeout (AssertJ):
     *     @Timeout (annotation) → JUnit interrupts the thread after timeout
     *     assertTimeout (code)  → waits for completion, then fails if exceeded
     *     assertTimeoutPreemptively → like @Timeout but inline
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @TempDir — TEMPORARY DIRECTORY FOR FILE TESTS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @Test
     *   void exportTodos_shouldCreateCsvFile(@TempDir Path tempDir) throws Exception {
     *       Path csvFile = tempDir.resolve("todos-export.csv");
     *
     *       todoExporter.exportToCsv(todoService.getAllTodos(), csvFile);
     *
     *       assertThat(Files.exists(csvFile)).isTrue();
     *       List<String> lines = Files.readAllLines(csvFile);
     *       assertThat(lines).isNotEmpty();
     *   }
     *   // tempDir is deleted automatically after the test — no cleanup needed
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @ExtendWith — REGISTER JUnit 5 EXTENSIONS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   // Extension 1: Mockito (for @Mock, @Spy, @InjectMocks)
     *   @ExtendWith(MockitoExtension.class)
     *   class TodoServicePureUnitTest {
     *       @Mock TodoRepository repo;
     *       @InjectMocks TodoService service;
     *       // ...
     *   }
     *
     *   // Extension 2: Spring (for Spring context injection)
     *   @ExtendWith(SpringExtension.class)
     *   @ContextConfiguration(classes = {TodoService.class, TodoRepository.class})
     *   class TodoServiceSpringTest {
     *       @Autowired TodoService service;
     *       // ...
     *   }
     *   // NOTE: Already included in @SpringBootTest, @WebMvcTest, @DataJpaTest
     *
     *   // Extension 3: Multiple extensions
     *   @ExtendWith({SpringExtension.class, MockitoExtension.class})
     *   class MultiExtensionTest { ... }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @TestInstance — CONTROL TEST INSTANCE LIFECYCLE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   Default: JUnit creates a NEW instance for EACH test method.
     *   → @BeforeAll / @AfterAll must be static.
     *   → Test methods are isolated (changes in one don't affect another).
     *
     *   @TestInstance(TestInstance.Lifecycle.PER_CLASS):
     *   → ONE instance shared across ALL test methods.
     *   → @BeforeAll / @AfterAll can be non-static.
     *   → State can accumulate across test methods (be careful!).
     *   → Use when tests are ordered and depend on shared state.
     *
     *   @TestInstance(Lifecycle.PER_CLASS)
     *   class StatefulTest {
     *       private List<String> log = new ArrayList<>();  // shared state
     *
     *       @BeforeAll
     *       void initOnce() { log.add("INIT"); }  // not static!
     *
     *       @Test @Order(1) void step1() { log.add("STEP1"); }
     *       @Test @Order(2) void step2() { assertThat(log).contains("STEP1"); }
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @TestMethodOrder — CONTROL TEST EXECUTION ORDER:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @TestMethodOrder(MethodOrderer.OrderAnnotation.class)  // use @Order on methods
     *   @TestMethodOrder(MethodOrderer.DisplayName.class)      // alphabetical by display name
     *   @TestMethodOrder(MethodOrderer.Random.class)           // random (detect order dependencies)
     *   @TestMethodOrder(MethodOrderer.MethodName.class)       // alphabetical by method name
     *
     *   NOTE: Tests should be INDEPENDENT of order. If you need ordering, reconsider design.
     *         Exception: integration tests that test sequential flows.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║       ASSERTIONS — AssertJ, JUnit 5, Hamcrest COMPARISON                    ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 AssertJ (RECOMMENDED — most readable):
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   import static org.assertj.core.api.Assertions.*;
     *
     *   assertThat(value).isEqualTo(expected);
     *   assertThat(value).isNotNull();
     *   assertThat(value).isNull();
     *   assertThat(value).isTrue() / .isFalse();
     *   assertThat(str).isBlank() / .isNotBlank() / .startsWith("X");
     *   assertThat(list).hasSize(3) / .isEmpty() / .contains("a", "b");
     *   assertThat(list).containsExactly("a", "b", "c");    // exact order
     *   assertThat(list).containsExactlyInAnyOrder("b","a");
     *   assertThat(map).containsKey("key") / .containsEntry("k", "v");
     *   assertThat(num).isBetween(1, 10) / .isGreaterThan(0);
     *   assertThat(todo).extracting("title", "completed")
     *                   .containsExactly("Buy milk", false);
     *   assertThat(todos).extracting(Todo::getTitle)
     *                    .containsExactlyInAnyOrder("Task A", "Task B");
     *
     *   // Exception assertions
     *   assertThatThrownBy(() -> service.createTodo(null, 1))
     *       .isInstanceOf(IllegalArgumentException.class)
     *       .hasMessageContaining("already exists");
     *
     *   assertThatNoException().isThrownBy(() -> service.getAllTodos());
     *
     *   // Soft assertions — collect ALL failures before reporting
     *   SoftAssertions softly = new SoftAssertions();
     *   softly.assertThat(todo.getTitle()).isEqualTo("Buy milk");
     *   softly.assertThat(todo.getPriority()).isEqualTo(2);
     *   softly.assertAll();   // report all failures at once
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 JUnit 5 Assertions (simpler, less fluent):
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   import static org.junit.jupiter.api.Assertions.*;
     *
     *   assertEquals(expected, actual);
     *   assertNotNull(value);
     *   assertTrue(condition, "message if fails");
     *   assertThrows(Exception.class, () -> method());
     *   assertAll("group",
     *       () -> assertEquals(1, value1),
     *       () -> assertNotNull(value2)    // runs all even if one fails
     *   );
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 MockMvc JSONPath assertions (for HTTP tests):
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   mockMvc.perform(get("/api/ch08/todos/1"))
     *       .andExpect(status().isOk())
     *       .andExpect(content().contentType(MediaType.APPLICATION_JSON))
     *       .andExpect(jsonPath("$.id").value(1))
     *       .andExpect(jsonPath("$.title").value("Buy milk"))
     *       .andExpect(jsonPath("$.completed").value(false))
     *       .andExpect(jsonPath("$", hasSize(3)))           // list size
     *       .andExpect(jsonPath("$[*].title").value(hasItem("Buy milk")))
     *       .andDo(print());     // print request/response to console
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║   CHAPTER 8 — EXAMPLE 03: JUnit 5 Annotations Deep Dive          ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  Lifecycle:   @Test @BeforeEach @AfterEach @BeforeAll @AfterAll @Disabled");
        System.out.println("  Display:     @DisplayName @Nested @Tag @Order @TestMethodOrder");
        System.out.println("  Parameterized: @ParameterizedTest");
        System.out.println("    Sources:   @ValueSource @CsvSource @CsvFileSource @MethodSource");
        System.out.println("               @EnumSource @NullSource @EmptySource @NullAndEmptySource");
        System.out.println("  Advanced:   @RepeatedTest @Timeout @TempDir @ExtendWith @TestInstance");
        System.out.println("  Assertions: AssertJ (recommended), JUnit 5, MockMvc JSONPath");
        System.out.println();
        System.out.println("▶  See REAL tests in: Example05_JUnitAnnotationsTest.java");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

