package com.learning.springboot.chapter08;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   CHAPTER 8 — TEST FILE 01: @SpringBootTest Integration Tests                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * WHAT IS TESTED:
 *   → @SpringBootTest with WebEnvironment.MOCK + @AutoConfigureMockMvc (MockMvc)
 *   → @SpringBootTest with WebEnvironment.RANDOM_PORT (TestRestTemplate)
 *   → Full stack: HTTP request → Controller → Service → Repository → H2 database
 *
 * KEY ANNOTATIONS DEMONSTRATED:
 *   @SpringBootTest, @AutoConfigureMockMvc, @LocalServerPort
 *   @BeforeEach, @Nested, @DisplayName
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ════════════════════════════════════════════════════════════════════════════════════════
//  TEST CLASS 1: @SpringBootTest + @AutoConfigureMockMvc (MOCK environment)
// ════════════════════════════════════════════════════════════════════════════════════════

/**
 * Full integration test using MockMvc (no real HTTP server).
 * FULL ApplicationContext is loaded — real DB, real service, real controller.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Chapter 8 — @SpringBootTest Integration Tests (MockMvc)")
class Example01_SpringBootIntegrationTest {

    /*
     * MockMvc: Simulates HTTP requests without starting a real server.
     * Auto-configured by @AutoConfigureMockMvc.
     */
    @Autowired
    private MockMvc mockMvc;

    /*
     * Real repository — allows verifying DB state directly.
     * This is the REAL JPA repository connected to H2.
     */
    @Autowired
    private TodoRepository todoRepository;

    /*
     * @BeforeEach: Clean the todos table before every test.
     * Ensures each test starts with a clean slate — no leftover data.
     */
    @BeforeEach
    void cleanUp() {
        todoRepository.deleteAll();
    }

    // ─── Test: GET /api/ch08/todos ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/ch08/todos → empty list → HTTP 200 with empty JSON array")
    void getAll_whenNoTodos_shouldReturnEmptyArray() throws Exception {
        /*
         * ARRANGE: database is already empty (cleaned in @BeforeEach)
         *
         * ACT + ASSERT using MockMvc:
         *   perform(get(...)) → issues an HTTP GET
         *   andExpect(status().isOk()) → asserts HTTP 200
         *   andExpect(jsonPath("$").isArray()) → asserts response is JSON array
         *   andExpect(jsonPath("$").isEmpty()) → asserts array is empty
         */
        mockMvc.perform(get("/api/ch08/todos"))
            .andDo(print())   // prints request + response to console for debugging
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/ch08/todos → 2 todos exist → HTTP 200 with 2 items")
    void getAll_whenTodosExist_shouldReturnList() throws Exception {
        // ARRANGE: save two todos to the DB
        todoRepository.save(new Todo("Buy milk"));
        todoRepository.save(new Todo("Do laundry", 2));

        // ACT + ASSERT
        mockMvc.perform(get("/api/ch08/todos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }

    // ─── Test: POST /api/ch08/todos ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/ch08/todos → valid body → HTTP 201 + todo created in DB")
    void createTodo_validRequest_shouldReturn201AndPersist() throws Exception {
        // ARRANGE: nothing in DB yet
        String requestBody = """
                {"title": "Buy groceries", "priority": 2}
                """;

        // ACT: POST the request
        mockMvc.perform(post("/api/ch08/todos")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())                         // 201
            .andExpect(jsonPath("$.title").value("Buy groceries"))   // title in response
            .andExpect(jsonPath("$.completed").value(false))         // not done yet
            .andExpect(jsonPath("$.priority").value(2));             // priority set

        // ASSERT: verify DB state independently
        assertThat(todoRepository.count()).isEqualTo(1);
        assertThat(todoRepository.findAll().get(0).getTitle()).isEqualTo("Buy groceries");
    }

    @Test
    @DisplayName("POST /api/ch08/todos → blank title → HTTP 400 Bad Request")
    void createTodo_blankTitle_shouldReturn400() throws Exception {
        String invalidBody = """
                {"title": "", "priority": 1}
                """;

        mockMvc.perform(post("/api/ch08/todos")
                .contentType(APPLICATION_JSON)
                .content(invalidBody))
            .andExpect(status().isBadRequest());

        // Nothing was saved — @Valid caught the error before the service ran
        assertThat(todoRepository.count()).isZero();
    }

    // ─── Test: PUT /api/ch08/todos/{id}/complete ─────────────────────────────────

    @Test
    @DisplayName("PUT /api/ch08/todos/{id}/complete → existing todo → marks completed")
    void completeTodo_existingId_shouldMarkCompleted() throws Exception {
        // ARRANGE
        Todo saved = todoRepository.save(new Todo("Task to complete"));
        assertThat(saved.isCompleted()).isFalse();

        // ACT
        mockMvc.perform(put("/api/ch08/todos/" + saved.getId() + "/complete"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completed").value(true));

        // ASSERT: verify DB reflects completed state
        Todo updated = todoRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.isCompleted()).isTrue();
    }

    // ─── Test: DELETE /api/ch08/todos/{id} ───────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/ch08/todos/{id} → existing → HTTP 204 + removed from DB")
    void deleteTodo_existingId_shouldReturn204AndRemoveFromDb() throws Exception {
        Todo saved = todoRepository.save(new Todo("To be deleted"));

        mockMvc.perform(delete("/api/ch08/todos/" + saved.getId()))
            .andExpect(status().isNoContent());

        assertThat(todoRepository.existsById(saved.getId())).isFalse();
    }

    @Test
    @DisplayName("DELETE /api/ch08/todos/{id} → non-existent → HTTP 404")
    void deleteTodo_nonExistentId_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/ch08/todos/999999"))
            .andExpect(status().isNotFound());
    }

    // ─── Test: GET /api/ch08/todos/stats ──────────────────────────────────────────

    @Test
    @DisplayName("GET /api/ch08/todos/stats → 2 pending 1 done → correct counts")
    void getStats_shouldReturnCorrectCounts() throws Exception {
        // ARRANGE
        todoRepository.save(new Todo("Pending 1"));
        todoRepository.save(new Todo("Pending 2"));
        Todo done = new Todo("Done");
        done.markCompleted();
        todoRepository.save(done);

        // ACT + ASSERT
        mockMvc.perform(get("/api/ch08/todos/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.pending").value(2))
            .andExpect(jsonPath("$.completed").value(1));
    }
}


// ════════════════════════════════════════════════════════════════════════════════════════
//  TEST CLASS 2: @SpringBootTest with RANDOM_PORT + TestRestTemplate
// ════════════════════════════════════════════════════════════════════════════════════════

/**
 * Integration test with a REAL embedded server started on a random port.
 * Uses TestRestTemplate to make real HTTP calls.
 *
 * USE WHEN:
 *   → You need to test actual HTTP behaviour (real headers, cookies, redirects)
 *   → Testing OAuth2 / Security filter chains that need real HTTP
 *   → E2E-style tests within the Spring Boot context
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DisplayName("Chapter 8 — @SpringBootTest RANDOM_PORT (TestRestTemplate)")
class Example01b_SpringBootRandomPortTest {

    /*
     * @LocalServerPort: injects the actual port the embedded server started on.
     * Random port prevents conflicts between parallel test runs in CI.
     */
    @LocalServerPort
    private int port;

    /*
     * TestRestTemplate: a test-friendly RestTemplate.
     * Auto-configured by Spring Boot when webEnvironment = RANDOM_PORT.
     * Advantages over raw RestTemplate in tests:
     *   → Handles HTTP errors without throwing exceptions (returns the error response)
     *   → Pre-configured with the server's base URL
     */
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TodoRepository todoRepository;

    @BeforeEach
    void cleanUp() {
        todoRepository.deleteAll();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @DisplayName("GET /api/ch08/todos/stats → real HTTP response with correct JSON")
    void getStats_shouldReturnRealHttpResponse() {
        // ARRANGE
        todoRepository.save(new Todo("A task"));

        // ACT: real HTTP call to the running server
        ResponseEntity<Map> response = restTemplate.getForEntity(
            url("/api/ch08/todos/stats"), Map.class);

        // ASSERT: real HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
            .isEqualTo(org.springframework.http.MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).containsKey("total");
        assertThat(response.getBody()).containsKey("pending");
    }

    @Test
    @DisplayName("POST → GET round trip via real HTTP")
    void createAndFetch_realHttpRoundTrip() {
        // POST to create
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(
            "{\"title\":\"HTTP Round Trip\",\"priority\":1}", headers);

        ResponseEntity<Todo> created = restTemplate.postForEntity(
            url("/api/ch08/todos"), req, Todo.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // GET to fetch
        ResponseEntity<List> fetched = restTemplate.getForEntity(
            url("/api/ch08/todos"), List.class);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).hasSize(1);
    }
}


