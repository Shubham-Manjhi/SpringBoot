package com.learning.springboot.chapter08;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   EXAMPLE 02: @MockBean, @SpyBean & @TestConfiguration                              ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example02MockBeanAndSpyBean.java
 * Purpose:     Deep-dive into @MockBean, @SpyBean, @TestConfiguration — how Spring
 *              integrates Mockito, when to use each, and real patterns.
 * Difficulty:  ⭐⭐⭐ Intermediate
 * Time:        45–60 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
public class Example02MockBeanAndSpyBean {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        @MockBean — REPLACE A SPRING BEAN WITH A MOCKITO MOCK                 ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @MockBean (org.springframework.boot.test.mock.mockito.MockBean) creates a
     * Mockito mock object AND REGISTERS it in the Spring ApplicationContext, replacing
     * any existing bean of the same type.
     *
     * WHAT IT DOES INTERNALLY:
     *   1. Creates a Mockito mock: Mockito.mock(TodoService.class)
     *   2. Registers the mock as a bean in the Spring context under the same name/type
     *   3. Any other bean that @Autowired TodoService now gets the MOCK
     *   4. By default, all mock methods return null (objects), 0 (numbers), false (booleans)
     *   5. Mockito verifications work normally on the mock
     *
     * MOCK RESET BEHAVIOUR:
     *   By default, @MockBean mocks are reset after EACH test (Mockito.RESETS_INVOCATIONS).
     *   Stubs are NOT reset — only invocation counts.
     *   Use @MockReset to control this behaviour.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 WHEN TO USE @MockBean:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ✅ In @WebMvcTest — the controller's service must be mocked
     *  ✅ In @SpringBootTest — when you want to stub an external service (email, SMS)
     *  ✅ When you need Mockito's verify() in a Spring test
     *  ✅ When you want to isolate the tested component from its dependencies
     *
     *  ❌ NOT needed in pure unit tests (@ExtendWith(MockitoExtension.class) + @Mock)
     *  ❌ NOT for repositories in @DataJpaTest (repositories are real there)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 CONTEXT CACHING IMPACT:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * IMPORTANT: Each unique combination of @MockBeans creates a DIFFERENT ApplicationContext.
     *
     * Test A: @MockBean TodoService                → Context Key 1
     * Test B: @MockBean TodoService, @MockBean X   → Context Key 2 (DIFFERENT context!)
     *
     * Minimise unique @MockBean combinations → maximise context reuse → faster CI.
     *
     * SOLUTION: Create a base test class with all common @MockBeans. All subclasses
     * reuse the same context.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 USAGE PATTERN:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @WebMvcTest(TodoController.class)
     *   class TodoControllerTest {
     *
     *       @Autowired MockMvc mockMvc;
     *
     *       // @MockBean replaces the real TodoService with a Mockito mock
     *       @MockBean TodoService todoService;
     *
     *       @Test
     *       void getById_whenFound_shouldReturn200WithTodo() throws Exception {
     *           // ARRANGE — stub the mock's behaviour
     *           Todo expected = new Todo("Buy milk");
     *           when(todoService.getById(1L)).thenReturn(Optional.of(expected));
     *
     *           // ACT — perform HTTP request
     *           mockMvc.perform(get("/api/ch08/todos/1"))
     *               // ASSERT — verify HTTP response
     *               .andExpect(status().isOk())
     *               .andExpect(jsonPath("$.title").value("Buy milk"));
     *
     *           // VERIFY — service method was called once with id=1
     *           verify(todoService, times(1)).getById(1L);
     *       }
     *
     *       @Test
     *       void getById_whenNotFound_shouldReturn404() throws Exception {
     *           // Stub: service returns empty (nothing found)
     *           when(todoService.getById(99L)).thenReturn(Optional.empty());
     *
     *           mockMvc.perform(get("/api/ch08/todos/99"))
     *               .andExpect(status().isNotFound());
     *       }
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @MockBean ON A FIELD vs AT CLASS LEVEL:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * ON FIELD (most common):
     *   @MockBean TodoService todoService;
     *   → Creates the mock AND gives you a field reference to stub/verify
     *
     * ON CLASS (when you need the mock but don't need a reference):
     *   @MockBean(TodoService.class)  // annotation on the test class
     *   → Creates the mock but no field reference
     *   → Used to prevent real bean from loading without needing to configure it
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        @SpyBean — PARTIAL MOCK OF A REAL SPRING BEAN                         ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @SpyBean wraps an EXISTING Spring bean in a Mockito SPY.
     * A spy delegates ALL method calls to the REAL object by default,
     * but you can stub specific methods to return controlled values.
     *
     * THE KEY DIFFERENCE:
     *   @MockBean → ALL methods return defaults (null / 0 / false) unless stubbed
     *   @SpyBean  → ALL methods call REAL implementation unless stubbed
     *
     * WHEN TO USE @SpyBean:
     *   → When you need most of the real logic but want to stub ONE method
     *   → When verifying that a real bean's method is called
     *   → When testing integration with mostly real components
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 IMPORTANT STUB SYNTAX DIFFERENCE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * For @SpyBean (or Mockito.spy()), you MUST use doReturn() / doThrow(),
     * NOT when().thenReturn(). Here's why:
     *
     *   // ❌ WRONG — this calls the REAL method before stubbing!
     *   when(spyBean.createTodo("milk", 1)).thenReturn(mockTodo);
     *   // The real createTodo() runs during the when() setup → side effects!
     *
     *   // ✅ CORRECT — doReturn avoids calling the real method
     *   doReturn(mockTodo).when(spyBean).createTodo("milk", 1);
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 USAGE PATTERN — See real implementation in Example04_MockBeanSpyBeanTest:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @SpringBootTest
     *   class TodoSpyBeanTest {
     *
     *       @SpyBean TodoService todoService;  // REAL service, wrapped in spy
     *       @Autowired TodoRepository todoRepository;
     *
     *       @Test
     *       void completeTodo_shouldCallRealLogicAndVerify() {
     *           // Save real data
     *           Todo saved = todoRepository.save(new Todo("Real todo"));
     *
     *           // ACT — calls REAL service method
     *           todoService.completeTodo(saved.getId());
     *
     *           // ASSERT — real database was updated
     *           Todo updated = todoRepository.findById(saved.getId()).orElseThrow();
     *           assertThat(updated.isCompleted()).isTrue();
     *
     *           // VERIFY — service method was called
     *           verify(todoService).completeTodo(saved.getId());
     *       }
     *
     *       @Test
     *       void getStats_whenNoTodos_shouldReturnZeros() {
     *           // Stub ONE specific method (skip DB call for getStats)
     *           doReturn(Map.of("total", 0L, "pending", 0L, "completed", 0L))
     *               .when(todoService).getStats();
     *
     *           Map<String, Long> stats = todoService.getStats();
     *
     *           assertThat(stats).containsEntry("total", 0L);
     *           // Other real methods still work normally (not stubbed)
     *       }
     *   }
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        @TestConfiguration — TEST-ONLY BEAN DEFINITIONS                       ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @TestConfiguration marks a configuration class (or inner static class) that
     * provides bean definitions EXCLUSIVELY for the test environment.
     * These beans NEVER appear in the production ApplicationContext.
     *
     * PACKAGE: org.springframework.boot.test.context.TestConfiguration
     *
     * HOW IT DIFFERS FROM @Configuration:
     *   @Configuration → available everywhere (production + test)
     *   @TestConfiguration → available ONLY in tests
     *
     * SCOPING RULES:
     *   When used as an INNER STATIC CLASS inside a @SpringBootTest class:
     *   → Automatically picked up, beans are added to the test context
     *   → No @Import needed
     *
     *   When used as a TOP-LEVEL class:
     *   → Must be explicitly imported: @Import(MyTestConfig.class)
     *   → Component scan does NOT pick it up (safety: prevents accidental production use)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 USAGE PATTERN 1 — Inner static class (auto-detected):
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @SpringBootTest
     *   class TodoIntegrationTest {
     *
     *       // Inner @TestConfiguration — beans available to this test class only
     *       @TestConfiguration
     *       static class TestConfig {
     *
     *           // Provide a fake email sender for tests (instead of real SMTP)
     *           @Bean
     *           @Primary  // Override the production bean
     *           EmailSender fakeEmailSender() {
     *               return (to, subject, body) -> {
     *                   System.out.println("FAKE EMAIL to " + to + ": " + subject);
     *               };
     *           }
     *       }
     *
     *       @Autowired EmailSender emailSender;  // Gets the fake one
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 USAGE PATTERN 2 — Shared top-level test config:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   // Shared config used by multiple test classes
     *   @TestConfiguration
     *   public class SharedTestConfig {
     *
     *       @Bean
     *       @Primary
     *       public NotificationService fakeNotificationService() {
     *           return Mockito.mock(NotificationService.class);
     *       }
     *   }
     *
     *   // Test class that uses it — must explicitly @Import
     *   @SpringBootTest
     *   @Import(SharedTestConfig.class)
     *   class OrderServiceTest { ... }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @TestConfiguration vs @MockBean — WHEN TO USE WHICH:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   USE @MockBean WHEN:
     *   → Simple stubbing with Mockito (when/then/verify)
     *   → The dependency is straightforward to mock
     *   → You want Mockito's out-of-the-box mock behaviour
     *
     *   USE @TestConfiguration WHEN:
     *   → You need a more realistic test double (not just a mock)
     *   → You want an in-memory implementation (e.g., in-memory message queue)
     *   → You need complex behaviour that @MockBean stubs can't express
     *   → Shared across many test classes (define once, @Import everywhere)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Captor — Capture arguments passed to mocks:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @Captor combined with @MockBean allows capturing method arguments:
     *
     *   @Captor ArgumentCaptor<Todo> todoCaptor;
     *
     *   verify(todoRepository).save(todoCaptor.capture());
     *   Todo captured = todoCaptor.getValue();
     *   assertThat(captured.getTitle()).isEqualTo("Buy milk");
     *   assertThat(captured.getPriority()).isEqualTo(2);
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @InjectMocks — Pure unit test without Spring:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * In a pure unit test (no Spring context), use @ExtendWith(MockitoExtension.class):
     *
     *   @ExtendWith(MockitoExtension.class)
     *   class TodoServiceUnitTest {
     *
     *       @Mock TodoRepository todoRepository;   // Pure Mockito mock
     *       @InjectMocks TodoService todoService;  // Real service, mocks injected
     *
     *       @Test
     *       void createTodo_whenTitleExists_shouldThrowException() {
     *           when(todoRepository.existsByTitle("Existing")).thenReturn(true);
     *
     *           assertThatThrownBy(() -> todoService.createTodo("Existing", 1))
     *               .isInstanceOf(IllegalArgumentException.class)
     *               .hasMessageContaining("already exists");
     *       }
     *   }
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║   CHAPTER 8 — EXAMPLE 02: @MockBean, @SpyBean, @TestConfiguration║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  @MockBean         → Replace Spring bean with Mockito mock");
        System.out.println("    Stub:  when(mock.method()).thenReturn(value)");
        System.out.println("    Verify: verify(mock, times(1)).method(arg)");
        System.out.println("    Impact: each unique @MockBean creates a NEW Spring context!");
        System.out.println();
        System.out.println("  @SpyBean          → Wrap real Spring bean in Mockito spy");
        System.out.println("    Real methods called unless stubbed via doReturn()");
        System.out.println("    Use doReturn().when(spy).method() NOT when(spy.method())");
        System.out.println();
        System.out.println("  @TestConfiguration → Test-only @Bean definitions");
        System.out.println("    Inner static class → auto-detected");
        System.out.println("    Top-level class    → must @Import explicitly");
        System.out.println();
        System.out.println("  @Mock / @InjectMocks → Pure unit test (no Spring context)");
        System.out.println("    Use with @ExtendWith(MockitoExtension.class)");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

