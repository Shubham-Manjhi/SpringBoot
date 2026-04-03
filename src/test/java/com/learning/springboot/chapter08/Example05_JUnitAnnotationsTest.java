package com.learning.springboot.chapter08;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   CHAPTER 8 — TEST FILE 05: JUnit 5 Annotations Showcase                            ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * WHAT IS DEMONSTRATED:
 *   → @Test, @BeforeEach, @AfterEach, @BeforeAll, @AfterAll, @Disabled
 *   → @DisplayName, @Nested, @Tag
 *   → @ParameterizedTest with ALL source types:
 *      @ValueSource, @CsvSource, @MethodSource, @EnumSource
 *      @NullSource, @EmptySource, @NullAndEmptySource
 *   → @RepeatedTest, @Timeout, @TempDir
 *   → @TestInstance(PER_CLASS), @TestMethodOrder
 *   → @EnabledOnOs, @DisabledOnOs (conditional test execution)
 *   → AssertJ assertions + JUnit 5 assertions
 *
 * NO SPRING CONTEXT: these are pure JUnit 5 unit tests (fast, no startup time).
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
@DisplayName("Chapter 8 — JUnit 5 Annotations Showcase (Pure Unit Tests)")
@Tag("unit")     // Tag: run with: gradlew test --tests "*" -Ptags=unit
class Example05_JUnitAnnotationsTest {

    // ════════════════════════════════════════════════════════════════════════════════
    //  SECTION 1: LIFECYCLE ANNOTATIONS
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Lifecycle annotations: @BeforeAll @BeforeEach @AfterEach @AfterAll")
    class LifecycleDemo {

        private final List<String> log = new java.util.ArrayList<>();

        /*
         * @BeforeEach: runs before EACH @Test method.
         * Used here to prepare fresh state per test.
         * "this" refers to a NEW instance — JUnit creates one per test.
         */
        @BeforeEach
        void setUp() {
            log.clear();
            log.add("SETUP");
        }

        /*
         * @AfterEach: runs after EACH @Test method (even if test fails).
         * Perfect for releasing resources, resetting state.
         */
        @AfterEach
        void tearDown() {
            // Could clear resources here
        }

        @Test
        @DisplayName("@BeforeEach runs first → log has SETUP entry")
        void beforeEach_shouldRunBeforeTest() {
            assertThat(log).containsExactly("SETUP");
        }

        @Test
        @DisplayName("test 2 → also starts with clean SETUP (new instance)")
        void secondTest_alsoStartsFresh() {
            // Each test gets a NEW instance → log starts fresh each time
            assertThat(log).hasSize(1).contains("SETUP");
        }

        @Test
        @DisplayName("@Disabled → this test is SKIPPED (not run, not failed)")
        @Disabled("Demonstrating @Disabled — this is intentionally skipped")
        void thisTestIsSkipped() {
            org.assertj.core.api.Assertions.fail("This should never run");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  SECTION 2: @ParameterizedTest — @ValueSource
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("@ParameterizedTest with @ValueSource")
    class ValueSourceDemo {

        /*
         * @ValueSource(strings = {...}): each string becomes one test invocation.
         * The test name shows: [1] "", [2] "   ", [3] "\t" etc.
         */
        @ParameterizedTest(name = "[{index}] blank string=''{0}'' → isBlank() returns true")
        @ValueSource(strings = {"", "  ", "\t", "\n", "   "})
        @DisplayName("blank strings → isBlank() returns true")
        void blankStrings_shouldReturnTrueForIsBlank(String blank) {
            assertThat(blank.isBlank()).isTrue();
        }

        @ParameterizedTest(name = "[{index}] priority={0} → within [1,3]")
        @ValueSource(ints = {1, 2, 3})
        @DisplayName("valid priority values → within [1,3]")
        void validPriorities_shouldBeWithinRange(int priority) {
            assertThat(priority).isBetween(1, 3);
        }

        @ParameterizedTest(name = "[{index}] n={0} → n*n is non-negative")
        @ValueSource(longs = {-100L, -1L, 0L, 1L, 100L, Long.MAX_VALUE / 2})
        @DisplayName("squaring any long → result is non-negative (for safe range)")
        void squaring_nonNegativeResult(long n) {
            // For longs within safe range, n*n can overflow, but Math.abs handles most cases
            if (n >= 0 && n <= 1_000_000L) {
                assertThat(n * n).isGreaterThanOrEqualTo(0);
            } else {
                // Just verify the test runs for each value
                assertThat(n).isNotNull();
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  SECTION 3: @ParameterizedTest — @CsvSource
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("@ParameterizedTest with @CsvSource")
    class CsvSourceDemo {

        /*
         * @CsvSource: each string is a CSV row.
         * Columns mapped to method parameters by position.
         * JUnit 5 handles type conversion: "3" → int 3, "true" → boolean true.
         */
        @ParameterizedTest(name = "[{index}] {0} + {1} = {2}")
        @CsvSource({
            "1,  2,  3",
            "0,  0,  0",
            "-1, 1,  0",
            "10, 20, 30",
            "-5, -3, -8"
        })
        @DisplayName("integer addition → a + b = expected")
        void addition_shouldProduceCorrectSum(int a, int b, int expected) {
            assertThat(a + b).isEqualTo(expected);
        }

        @ParameterizedTest(name = "[{index}] ''{0}'' has {1} chars → length check")
        @CsvSource({
            "hello, 5",
            "world, 5",
            "a,     1",
            "JUnit, 5",
            "'',   0"    // empty string → length 0
        })
        @DisplayName("string length → matches expected character count")
        void stringLength_shouldMatchExpected(String input, int expectedLength) {
            assertThat(input).hasSize(expectedLength);
        }

        @ParameterizedTest(name = "[{index}] title=''{0}'' priority={1} valid={2}")
        @CsvSource({
            "Buy milk,     1, true",
            "Do laundry,   3, true",
            "'',           1, false",   // blank title → invalid
        })
        @DisplayName("todo validation → title empty check")
        void todoValidation(String title, int priority, boolean expectedValid) {
            boolean isValid = !title.isBlank() && priority >= 1 && priority <= 3;
            assertThat(isValid).isEqualTo(expectedValid);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  SECTION 4: @ParameterizedTest — @MethodSource
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("@ParameterizedTest with @MethodSource")
    class MethodSourceDemo {

        /*
         * @MethodSource("factoryMethod"): points to a static factory method.
         * Factory must return Stream<Arguments>, Iterable, or Collection.
         * Great for complex objects that can't be expressed in CSV.
         */
        @ParameterizedTest(name = "[{index}] title=''{0}'' priority={1} → valid={2}")
        @MethodSource("provideTodoTestData")
        @DisplayName("todo creation validation via MethodSource")
        void todoCreation_validation(String title, int priority, boolean expectedValid) {
            boolean isValid = title != null && !title.isBlank() && priority >= 1 && priority <= 3;
            assertThat(isValid).isEqualTo(expectedValid);
        }

        // Factory method — must be static, same class (or specify class::method for different class)
        static Stream<Arguments> provideTodoTestData() {
            return Stream.of(
                Arguments.of("Buy milk",   1, true),    // valid
                Arguments.of("Do laundry", 3, true),    // valid, high priority
                Arguments.of("",           1, false),   // empty title → invalid
                Arguments.of("  ",         2, false),   // blank title → invalid
                Arguments.of("Valid task", 0, false),   // priority 0 → invalid
                Arguments.of("Valid task", 4, false)    // priority 4 → invalid
            );
        }

        @ParameterizedTest
        @MethodSource("provideStringsToCheck")
        @DisplayName("string trimming via MethodSource")
        void trimming_shouldRemoveLeadingAndTrailingSpaces(String input, String expected) {
            assertThat(input.strip()).isEqualTo(expected);
        }

        static Stream<Arguments> provideStringsToCheck() {
            return Stream.of(
                Arguments.of("  hello  ", "hello"),
                Arguments.of("world",     "world"),
                Arguments.of("  a  ",    "a"),
                Arguments.of("",          "")
            );
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  SECTION 5: @NullSource / @EmptySource / @NullAndEmptySource
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("@NullSource / @EmptySource / @NullAndEmptySource")
    class NullAndEmptySourceDemo {

        @ParameterizedTest
        @NullSource
        @DisplayName("@NullSource → provides null (test receives null parameter)")
        void nullSource_shouldReceiveNull(String value) {
            assertThat(value).isNull();
        }

        @ParameterizedTest
        @EmptySource
        @DisplayName("@EmptySource → provides empty string ''")
        void emptySource_shouldReceiveEmptyString(String value) {
            assertThat(value).isEmpty();
        }

        /*
         * @NullAndEmptySource = @NullSource + @EmptySource combined.
         * Runs TWICE: once with null, once with empty string.
         * Combine with @ValueSource for comprehensive "blank" testing.
         */
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        @DisplayName("null, empty, and whitespace → all should be treated as 'blank'")
        void nullEmptyWhitespace_allShouldBeConsideredBlank(String value) {
            // isNullOrEmpty() handles null, isEmpty() handles "", isBlank() handles whitespace
            boolean isBlankOrNull = value == null || value.isBlank();
            assertThat(isBlankOrNull).isTrue();
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  SECTION 6: @RepeatedTest
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("@RepeatedTest — Run same test multiple times")
    class RepeatedTestDemo {

        private int counter = 0;

        /*
         * @RepeatedTest(n): runs the method n times.
         * Each run is a separate test case in the report.
         * RepetitionInfo provides current/total repetition numbers.
         */
        @RepeatedTest(value = 3, name = "repetition {currentRepetition} of {totalRepetitions}")
        @DisplayName("counter increments on each repetition")
        void repeatedCounter_shouldIncrementEachTime(RepetitionInfo info) {
            counter++;
            // Each repetition sees the counter from this instance
            assertThat(info.getCurrentRepetition()).isBetween(1, 3);
            assertThat(info.getTotalRepetitions()).isEqualTo(3);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  SECTION 7: @TempDir
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("@TempDir — Temporary directory for file operations")
    class TempDirDemo {

        /*
         * @TempDir: JUnit 5 provides a temporary directory.
         * Deleted automatically after the test completes (cleanup handled by JUnit).
         * Works with java.nio.file.Path and java.io.File.
         */
        @Test
        @DisplayName("write to temp dir → file exists → content readable")
        void writeTempFile_shouldCreateAndRead(@TempDir Path tempDir) throws IOException {
            // ARRANGE
            Path file = tempDir.resolve("test-todos.txt");
            String content = "Buy milk\nDo laundry\nRead book";

            // ACT
            Files.writeString(file, content);

            // ASSERT
            assertThat(Files.exists(file)).isTrue();
            assertThat(Files.readString(file)).isEqualTo(content);
            assertThat(Files.readAllLines(file)).hasSize(3);
        }

        @Test
        @DisplayName("multiple files in temp dir → all accessible")
        void multipleTempFiles(@TempDir Path tempDir) throws IOException {
            Files.writeString(tempDir.resolve("a.txt"), "AAA");
            Files.writeString(tempDir.resolve("b.txt"), "BBB");

            assertThat(Files.list(tempDir).count()).isEqualTo(2);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  SECTION 8: CONDITIONAL TESTS (@EnabledOnOs, @DisabledOnOs, etc.)
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Conditional test execution (@EnabledOnOs, @EnabledIfSystemProperty)")
    class ConditionalTests {

        @Test
        @EnabledOnOs({OS.MAC, OS.LINUX, OS.WINDOWS})
        @DisplayName("enabled on Mac, Linux, Windows → always runs on common OSes")
        void enabledOnAllCommonOs_shouldAlwaysRun() {
            assertThat(System.getProperty("os.name")).isNotBlank();
        }

        @Test
        @DisabledOnOs(OS.SOLARIS)
        @DisplayName("disabled on Solaris → runs everywhere else")
        void disabledOnSolaris_shouldRunElsewhere() {
            assertThat(true).isTrue();
        }

        @Test
        @EnabledIfSystemProperty(named = "java.version", matches = ".*")
        @DisplayName("enabled if java.version system property exists → always passes")
        void enabledIfJavaVersionExists_shouldAlwaysRun() {
            assertThat(System.getProperty("java.version")).isNotBlank();
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  SECTION 9: AssertJ vs JUnit 5 Assertions
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AssertJ vs JUnit 5 Assertions comparison")
    class AssertionsComparison {

        @Test
        @DisplayName("AssertJ: fluent, readable, better error messages")
        void assertJ_fluent() {
            String actual = "Spring Boot";

            // AssertJ fluent assertions
            assertThat(actual)
                .isNotNull()
                .isNotEmpty()
                .startsWith("Spring")
                .endsWith("Boot")
                .contains("Boot")
                .hasSize(11);

            List<Integer> numbers = List.of(1, 2, 3, 4, 5);
            assertThat(numbers)
                .hasSize(5)
                .contains(3)
                .doesNotContain(6)
                .allMatch(n -> n > 0)
                .anyMatch(n -> n > 4);
        }

        @Test
        @DisplayName("JUnit 5 Assertions: simpler, fewer imports, assertAll()")
        void junit5_assertions() {
            String actual = "Spring Boot";

            // JUnit 5 assertions
            assertEquals("Spring Boot", actual);
            assertNotNull(actual);
            assertTrue(actual.startsWith("Spring"));
            assertFalse(actual.isEmpty());

            // assertAll(): runs ALL assertions even if one fails
            assertAll("Spring Boot properties",
                () -> assertEquals(11, actual.length()),
                () -> assertTrue(actual.contains("Boot")),
                () -> assertFalse(actual.isBlank())
            );
        }

        @Test
        @DisplayName("assertThatThrownBy → verify exception type and message")
        void exceptionTesting_withAssertJ() {
            // Test that a RuntimeException is thrown
            assertThatThrownBy(() -> {
                throw new IllegalArgumentException("Invalid priority: 99");
            })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99")
                .hasMessageStartingWith("Invalid");
        }

        @Test
        @DisplayName("assertThatNoException → verify no exception thrown")
        void noException_withAssertJ() {
            assertThatNoException().isThrownBy(() -> {
                new Todo("Valid task");
            });
        }
    }
}

