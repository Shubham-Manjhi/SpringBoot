package com.learning.springboot.chapter08;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   EXAMPLE 01: @SpringBootTest & TEST SLICE ANNOTATIONS                              ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example01SpringBootTestAnnotations.java
 * Purpose:     Deep-dive into @SpringBootTest, @WebMvcTest, @DataJpaTest,
 *              @JsonTest, @AutoConfigureMockMvc — what they load, how to use
 *              them, and real code patterns.
 * Difficulty:  ⭐⭐⭐ Intermediate
 * Time:        45–60 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * 💡 All CODE PATTERNS below are shown as comments so this file lives in
 *    src/main/java (the book's explanation layer).
 *    The REAL, RUNNABLE tests are in src/test/java/chapter08/.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
public class Example01SpringBootTestAnnotations {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        @SpringBootTest — THE FULL INTEGRATION TEST                           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @SpringBootTest loads a FULL ApplicationContext — every auto-configuration,
     * every @Component, @Service, @Repository, @Controller, every @Bean.
     * It behaves exactly like starting your production application, except the
     * database is usually H2 (or Testcontainers) and the server may be a mock.
     *
     * PACKAGE: org.springframework.boot.test.context.SpringBootTest
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 webEnvironment — THE MOST IMPORTANT ATTRIBUTE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * MOCK (default):
     *   → No real HTTP server is started.
     *   → A mock Servlet environment is used.
     *   → Use MockMvc to send requests (add @AutoConfigureMockMvc).
     *   → Fastest @SpringBootTest mode.
     *   → Good for integration tests that don't need real HTTP.
     *
     * RANDOM_PORT:
     *   → Starts a real embedded server (Tomcat) on a random available port.
     *   → Use @Autowired TestRestTemplate for HTTP calls (auto-configured).
     *   → Port available via @LocalServerPort or ${local.server.port}.
     *   → Use when testing actual HTTP behaviour (redirects, filters, etc.).
     *
     * DEFINED_PORT:
     *   → Starts on the port defined in application.yaml (default 8080).
     *   → Use when you need a predictable port (e.g., browser automation).
     *   → Risk: port conflicts in CI if multiple tests run in parallel.
     *
     * NONE:
     *   → Full context but NO web environment at all.
     *   → Use for service integration tests where HTTP is not relevant.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 USAGE PATTERNS (see real implementation in Example01_SpringBootIntegrationTest):
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * PATTERN 1 — MockMvc integration test (MOCK environment)
     * ─────────────────────────────────────────────────────
     *
     *   @SpringBootTest                        // Load full context
     *   @AutoConfigureMockMvc                  // Give us MockMvc
     *   class TodoIntegrationTest {
     *
     *       @Autowired MockMvc mockMvc;
     *       @Autowired TodoRepository todoRepository;
     *
     *       @BeforeEach
     *       void setUp() { todoRepository.deleteAll(); }   // Clean slate per test
     *
     *       @Test
     *       void createTodo_shouldPersistAndReturn201() throws Exception {
     *           mockMvc.perform(post("/api/ch08/todos")
     *               .contentType(MediaType.APPLICATION_JSON)
     *               .content("""{"title":"Buy milk","priority":2}"""))
     *               .andExpect(status().isCreated())
     *               .andExpect(jsonPath("$.title").value("Buy milk"))
     *               .andExpect(jsonPath("$.completed").value(false));
     *
     *           assertThat(todoRepository.count()).isEqualTo(1);
     *       }
     *   }
     *
     * PATTERN 2 — TestRestTemplate integration test (RANDOM_PORT)
     * ─────────────────────────────────────────────────────────────
     *
     *   @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
     *   class TodoRestIntegrationTest {
     *
     *       @Autowired TestRestTemplate restTemplate;
     *       @LocalServerPort int port;
     *
     *       @Test
     *       void getStats_shouldReturn200WithCounts() {
     *           ResponseEntity<Map> resp = restTemplate.getForEntity(
     *               "http://localhost:" + port + "/api/ch08/todos/stats", Map.class);
     *           assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
     *           assertThat(resp.getBody()).containsKey("total");
     *       }
     *   }
     *
     * PATTERN 3 — Inline property overrides
     * ──────────────────────────────────────
     *
     *   @SpringBootTest(properties = {
     *       "spring.jpa.show-sql=false",
     *       "logging.level.org.hibernate=WARN"
     *   })
     *   class QuietIntegrationTest { ... }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 HOW @SpringBootTest FINDS YOUR MAIN CLASS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Spring Boot searches upward from the test class's package to find a class
     * annotated with @SpringBootApplication. In our project, that's:
     *   com.learning.springboot.SpringBootAnnotationsApplication
     *
     * It uses that class's package as the root for component scanning.
     * You can override with: @SpringBootTest(classes = MyConfig.class)
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        @WebMvcTest — THE CONTROLLER SLICE                                    ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @WebMvcTest loads ONLY the web MVC layer — everything needed to process
     * HTTP requests and produce HTTP responses, but nothing more.
     *
     * WHAT IS LOADED:
     *   @Controller, @RestController   → your controllers
     *   @ControllerAdvice              → global exception handlers
     *   @JsonComponent                 → custom JSON serialisers
     *   Filter                         → servlet filters
     *   WebMvcConfigurer               → MVC customisations
     *   MockMvc                        → auto-configured HTTP test client
     *   Jackson ObjectMapper           → JSON serialisation
     *   Validation support             → @Valid processing
     *
     * WHAT IS NOT LOADED:
     *   @Service, @Repository, @Component → must be mocked with @MockBean
     *   DataSource, EntityManagerFactory  → not started at all
     *   All auto-configurations unrelated to MVC
     *
     * WHY USE IT?
     *   5–50x faster than @SpringBootTest for controller tests.
     *   Forces you to test only the controller's behaviour.
     *   Makes the test highly focused and maintainable.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 USAGE PATTERN — See real implementation in Example02_WebMvcControllerTest:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @WebMvcTest(TodoController.class)    // Load ONLY TodoController
     *   class TodoControllerTest {
     *
     *       @Autowired MockMvc mockMvc;       // Auto-configured
     *       @Autowired ObjectMapper mapper;   // For serialising request bodies
     *
     *       @MockBean TodoService todoService; // REQUIRED — controller depends on this
     *
     *       @Test
     *       void getAll_whenNoTodos_shouldReturnEmptyArray() throws Exception {
     *           // Stub: when service is asked, return an empty list
     *           when(todoService.getAllTodos()).thenReturn(List.of());
     *
     *           mockMvc.perform(get("/api/ch08/todos"))
     *               .andExpect(status().isOk())
     *               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
     *               .andExpect(jsonPath("$").isArray())
     *               .andExpect(jsonPath("$").isEmpty());
     *
     *           verify(todoService, times(1)).getAllTodos();
     *       }
     *
     *       @Test
     *       void createTodo_whenTitleBlank_shouldReturn400() throws Exception {
     *           // Validation test — service never called for invalid input
     *           String invalidBody = """{"title":"","priority":1}""";
     *
     *           mockMvc.perform(post("/api/ch08/todos")
     *               .contentType(MediaType.APPLICATION_JSON)
     *               .content(invalidBody))
     *               .andExpect(status().isBadRequest());
     *
     *           verifyNoInteractions(todoService);
     *       }
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 MOCKITO ESSENTIALS FOR @WebMvcTest:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   STUB (define what to return):
     *     when(service.getById(1L)).thenReturn(Optional.of(todo));
     *     when(service.getAllTodos()).thenReturn(List.of(todo1, todo2));
     *     doThrow(new NotFoundException(1L)).when(service).deleteTodo(1L);
     *
     *   VERIFY (assert the method was called):
     *     verify(service).createTodo("Buy milk", 1);
     *     verify(service, times(1)).getById(anyLong());
     *     verifyNoInteractions(service);  // service was never called
     *     verifyNoMoreInteractions(service);  // only specified calls were made
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        @DataJpaTest — THE REPOSITORY SLICE                                   ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @DataJpaTest creates a sliced context with ONLY JPA-related infrastructure.
     * Perfect for testing repository queries, entity mappings, constraints.
     *
     * WHAT IS LOADED:
     *   @Entity classes                → your entities (schema auto-created)
     *   @Repository interfaces         → Spring Data repositories
     *   DataSource                     → embedded H2 by default
     *   EntityManagerFactory           → Hibernate
     *   JpaTransactionManager          → transaction management
     *
     * WHAT IS NOT LOADED:
     *   @Service, @Controller, @Component → not relevant to DB tests
     *   Web MVC configuration              → not started
     *   All non-JPA auto-configurations
     *
     * TRANSACTION BEHAVIOUR:
     *   Each test method is wrapped in a transaction that ROLLS BACK at the end.
     *   This means:
     *     → Each test starts with a clean database state (no leftover data)
     *     → No need for cleanup @BeforeEach / @AfterEach
     *     → Tests are isolated from each other
     *
     *   Override rollback: @Rollback(false) or @Commit on a specific test
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 USAGE PATTERN — See real implementation in Example03_DataJpaRepositoryTest:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @DataJpaTest
     *   class TodoRepositoryTest {
     *
     *       @Autowired TodoRepository todoRepository;
     *
     *       // Optional: inject EntityManager for direct JPA operations
     *       @Autowired TestEntityManager entityManager;
     *
     *       @Test
     *       void findByCompleted_shouldReturnOnlyPendingTodos() {
     *           // Arrange — save test data (rolled back after test!)
     *           todoRepository.save(new Todo("Pending task"));
     *           Todo done = new Todo("Done task");
     *           done.markCompleted();
     *           todoRepository.save(done);
     *           entityManager.flush();  // Force write to H2 for clean read
     *
     *           // Act
     *           List<Todo> pending = todoRepository.findByCompleted(false);
     *
     *           // Assert
     *           assertThat(pending).hasSize(1);
     *           assertThat(pending.get(0).getTitle()).isEqualTo("Pending task");
     *       }
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 TestEntityManager — JPA test utility:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Spring provides TestEntityManager as a test-friendly wrapper around EntityManager.
     *
     *   entityManager.persist(entity)    → persist (doesn't return the entity)
     *   entityManager.persistAndFlush(e) → persist + flush to DB in one call
     *   entityManager.flush()            → flush pending changes to DB
     *   entityManager.clear()            → clear first-level cache (force reload)
     *   entityManager.find(Class, id)    → find by primary key
     *   entityManager.refresh(entity)    → reload entity from DB
     *
     * WHY USE flush() IN TESTS?
     *   Without flush, Hibernate batches writes and may not have written to H2 yet
     *   when you execute the query. flush() forces the INSERT/UPDATE to run immediately,
     *   ensuring your query reads the data you just saved.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @JsonTest — JSON serialization slice:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @JsonTest loads only the Jackson auto-configuration.
     * Inject JacksonTester<T> to test serialization/deserialization:
     *
     *   @JsonTest
     *   class TodoJsonTest {
     *       @Autowired JacksonTester<Todo> json;
     *
     *       @Test
     *       void serialize_shouldIncludeAllFields() throws Exception {
     *           Todo todo = new Todo("Buy milk");
     *           assertThat(json.write(todo))
     *               .hasJsonPathStringValue("@.title", "Buy milk")
     *               .hasJsonPathBooleanValue("@.completed", false);
     *       }
     *
     *       @Test
     *       void deserialize_shouldMapJsonToTodo() throws Exception {
     *           String content = """{"title":"Buy milk","priority":2}""";
     *           assertThat(json.parse(content))
     *               .usingRecursiveComparison()
     *               .isEqualTo(new CreateTodoRequest("Buy milk", 2));
     *       }
     *   }
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║   CHAPTER 8 — EXAMPLE 01: @SpringBootTest & Test Slices          ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  @SpringBootTest    → Full ApplicationContext integration test");
        System.out.println("    WebEnvironment.MOCK        → MockMvc (no real HTTP server)");
        System.out.println("    WebEnvironment.RANDOM_PORT → Real server + TestRestTemplate");
        System.out.println("    WebEnvironment.NONE        → No web environment");
        System.out.println();
        System.out.println("  @WebMvcTest         → Controller slice (fast, MVC layer only)");
        System.out.println("    + MockMvc auto-configured");
        System.out.println("    + @MockBean for service dependencies");
        System.out.println();
        System.out.println("  @DataJpaTest        → Repository slice (JPA only, H2, rollback)");
        System.out.println("    + TestEntityManager auto-configured");
        System.out.println("    + Transactional rollback after each test");
        System.out.println();
        System.out.println("  @JsonTest           → Jackson slice (serialisation only)");
        System.out.println("    + JacksonTester<T> auto-configured");
        System.out.println();
        System.out.println("▶  See REAL tests in: src/test/java/.../chapter08/");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

