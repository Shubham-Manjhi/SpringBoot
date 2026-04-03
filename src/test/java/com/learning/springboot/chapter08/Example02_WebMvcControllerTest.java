package com.learning.springboot.chapter08;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   CHAPTER 8 — TEST FILE 02: @WebMvcTest Controller Slice Tests                      ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * WHAT IS TESTED:
 *   → @WebMvcTest loads ONLY the web layer (TodoController + MockMvc)
 *   → @MockBean replaces TodoService with a Mockito mock
 *   → Stubbing with given/willReturn (BDD style)
 *   → Verifying with verify()
 *   → @Captor to capture method arguments
 *   → Testing validation (@Valid on @RequestBody)
 *   → Testing exception handling (@ExceptionHandler in controller)
 *
 * KEY ANNOTATIONS DEMONSTRATED:
 *   @WebMvcTest, @MockBean, @Captor, @Nested, @DisplayName
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
@WebMvcTest(TodoController.class)   // Load ONLY TodoController and MVC infrastructure
@DisplayName("Chapter 8 — @WebMvcTest: TodoController")
class Example02_WebMvcControllerTest {

    /*
     * MockMvc: auto-configured by @WebMvcTest.
     * Use it to simulate HTTP calls without a real server.
     */
    @Autowired
    private MockMvc mockMvc;

    /*
     * NOTE: In Spring Boot 4.x @WebMvcTest slice, ObjectMapper is NOT auto-configured.
     * We write JSON request bodies as inline strings directly.
     * For full Jackson support in tests, use @SpringBootTest + @AutoConfigureMockMvc.
     */

    /*
     * @MockitoBean: Replaces the real TodoService with a Mockito mock.
     * REQUIRED in @WebMvcTest — the service is NOT loaded by the slice.
     * Without this, Spring cannot inject TodoService into TodoController → error.
     */
    @MockitoBean
    private TodoService todoService;

    // NOTE: @Captor fields need MockitoExtension to initialise — we use
    // ArgumentCaptor.forClass() inline in tests instead.

    // ─── Helper method ────────────────────────────────────────────────────────────

    private Todo makeTodo(Long id, String title, boolean completed) {
        Todo todo = new Todo(title);
        todo.setPriority(1);
        return todo;
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  NESTED GROUP: GET All Todos
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/ch08/todos")
    class GetAll {

        @Test
        @DisplayName("→ service returns empty list → HTTP 200 with []")
        void whenNoTodos_shouldReturn200WithEmptyArray() throws Exception {
            // ARRANGE: stub service to return empty list
            given(todoService.getAllTodos()).willReturn(List.of());

            // ACT + ASSERT
            mockMvc.perform(get("/api/ch08/todos"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

            // VERIFY: service was called exactly once
            then(todoService).should(times(1)).getAllTodos();
        }

        @Test
        @DisplayName("→ service returns 2 todos → HTTP 200 with 2 items")
        void whenTodosExist_shouldReturn200WithList() throws Exception {
            // ARRANGE
            given(todoService.getAllTodos()).willReturn(List.of(
                new Todo("Task A"),
                new Todo("Task B")
            ));

            // ACT + ASSERT
            mockMvc.perform(get("/api/ch08/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Task A"))
                .andExpect(jsonPath("$[1].title").value("Task B"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  NESTED GROUP: GET By ID
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/ch08/todos/{id}")
    class GetById {

        @Test
        @DisplayName("→ todo exists → HTTP 200 with todo JSON")
        void whenFound_shouldReturn200WithTodo() throws Exception {
            // ARRANGE
            Todo todo = new Todo("Buy milk");
            given(todoService.getById(1L)).willReturn(Optional.of(todo));

            // ACT + ASSERT
            mockMvc.perform(get("/api/ch08/todos/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buy milk"))
                .andExpect(jsonPath("$.completed").value(false));
        }

        @Test
        @DisplayName("→ todo not found → HTTP 404")
        void whenNotFound_shouldReturn404() throws Exception {
            // ARRANGE: service returns empty Optional
            given(todoService.getById(999L)).willReturn(Optional.empty());

            // ACT + ASSERT
            mockMvc.perform(get("/api/ch08/todos/999"))
                .andExpect(status().isNotFound());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  NESTED GROUP: POST Create Todo
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/ch08/todos")
    class CreateTodo {

        @Test
        @DisplayName("→ valid body → HTTP 201, service called with correct arguments")
        void validBody_shouldReturn201AndCallService() throws Exception {
            // ARRANGE
            Todo created = new Todo("Buy groceries", 2);
            given(todoService.createTodo(anyString(), anyInt())).willReturn(created);

            // Inline JSON — ObjectMapper not available in WebMvcTest slice context
            String body = "{\"title\":\"Buy groceries\",\"priority\":2}";

            // ACT
            mockMvc.perform(post("/api/ch08/todos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Buy groceries"));

            // VERIFY: service called with correct args — capture them inline
            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Integer> priorityCaptor = ArgumentCaptor.forClass(Integer.class);
            then(todoService).should().createTodo(titleCaptor.capture(), priorityCaptor.capture());
            assertThat(titleCaptor.getValue()).isEqualTo("Buy groceries");
            assertThat(priorityCaptor.getValue()).isEqualTo(2);
        }

        @Test
        @DisplayName("→ blank title → HTTP 400, service NOT called")
        void blankTitle_shouldReturn400AndNotCallService() throws Exception {
            // Invalid request body
            String invalidBody = "{\"title\":\"\",\"priority\":1}";

            mockMvc.perform(post("/api/ch08/todos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidBody))
                .andExpect(status().isBadRequest());

            // CRITICAL: service should never be called for invalid input
            then(todoService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("→ missing body → HTTP 400")
        void missingBody_shouldReturn400() throws Exception {
            mockMvc.perform(post("/api/ch08/todos")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

            then(todoService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("→ duplicate title → service throws IllegalArgumentException → HTTP 409")
        void duplicateTitle_shouldReturn409() throws Exception {
            // ARRANGE: service throws when title exists
            given(todoService.createTodo(eq("Existing"), anyInt()))
                .willThrow(new IllegalArgumentException("A todo with title 'Existing' already exists"));

            String body = "{\"title\":\"Existing\",\"priority\":1}";

            mockMvc.perform(post("/api/ch08/todos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  NESTED GROUP: PUT Complete Todo
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/ch08/todos/{id}/complete")
    class CompleteTodo {

        @Test
        @DisplayName("→ existing todo → HTTP 200 with completed=true")
        void existingTodo_shouldReturn200WithCompleted() throws Exception {
            Todo completed = new Todo("Done task");
            completed.markCompleted();
            given(todoService.completeTodo(1L)).willReturn(completed);

            mockMvc.perform(put("/api/ch08/todos/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

            then(todoService).should().completeTodo(1L);
        }

        @Test
        @DisplayName("→ non-existent todo → service throws → HTTP 404")
        void nonExistentTodo_shouldReturn404() throws Exception {
            given(todoService.completeTodo(999L))
                .willThrow(new TodoService.TodoNotFoundException(999L));

            mockMvc.perform(put("/api/ch08/todos/999/complete"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Todo not found with id: 999"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  NESTED GROUP: DELETE Todo
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/ch08/todos/{id}")
    class DeleteTodo {

        @Test
        @DisplayName("→ existing id → HTTP 204 No Content")
        void existingId_shouldReturn204() throws Exception {
            willDoNothing().given(todoService).deleteTodo(1L);

            mockMvc.perform(delete("/api/ch08/todos/1"))
                .andExpect(status().isNoContent());

            then(todoService).should().deleteTodo(1L);
        }

        @Test
        @DisplayName("→ non-existent id → HTTP 404")
        void nonExistentId_shouldReturn404() throws Exception {
            willThrow(new TodoService.TodoNotFoundException(999L))
                .given(todoService).deleteTodo(999L);

            mockMvc.perform(delete("/api/ch08/todos/999"))
                .andExpect(status().isNotFound());
        }
    }
}


