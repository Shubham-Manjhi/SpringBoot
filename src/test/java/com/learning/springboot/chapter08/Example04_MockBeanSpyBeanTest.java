package com.learning.springboot.chapter08;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   CHAPTER 8 — TEST FILE 04: @MockBean, @SpyBean & Pure Unit Test with @Mock         ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * WHAT IS TESTED:
 *   → @MockBean replaces a Spring bean with a Mockito mock (Spring context test)
 *   → @SpyBean wraps a real Spring bean in a Mockito spy
 *   → @Mock (pure Mockito, no Spring context) — fastest unit tests
 *   → @InjectMocks — Spring-free DI for unit tests
 *   → @Captor — capture arguments passed to mocks
 *   → @TestConfiguration — inner class providing test-only beans
 *
 * KEY ANNOTATIONS DEMONSTRATED:
 *   @MockBean, @SpyBean, @Mock, @InjectMocks, @Captor
 *   @ExtendWith(MockitoExtension.class)
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ════════════════════════════════════════════════════════════════════════════════════════
//  TEST CLASS 1: @MockBean in a @SpringBootTest
// ════════════════════════════════════════════════════════════════════════════════════════

/**
 * Demonstrates @MockBean: replaces TodoRepository with a mock inside the Spring context.
 * TodoService still gets loaded as a REAL bean, but its repository dependency is mocked.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("Chapter 8 — @MockBean: Mock repository in Spring context")
class Example04a_MockBeanTest {

    /*
     * @MockBean: replaces the real TodoRepository in the Spring context with a mock.
     * TodoService is a REAL Spring bean but its todoRepository field gets the MOCK.
     * ALL repository method calls return Mockito defaults (null / 0 / false / empty)
     * unless explicitly stubbed.
     */
    @MockitoBean
    private TodoRepository todoRepository;

    /*
     * TodoService is the REAL bean (not mocked), but it uses the mocked repository.
     * We're testing the TodoService's logic in isolation from the real database.
     */
    @Autowired
    private TodoService todoService;

    @Test
    @DisplayName("getAllTodos() → mocked repo returns list → service returns same list")
    void getAllTodos_withMockedRepo_shouldReturnStubList() {
        // ARRANGE: stub what the mock repo returns
        List<Todo> expected = List.of(new Todo("Mocked Task 1"), new Todo("Mocked Task 2"));
        given(todoRepository.findAll()).willReturn(expected);

        // ACT: call real service method
        List<Todo> result = todoService.getAllTodos();

        // ASSERT: service returns what repo returned
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Todo::getTitle)
            .containsExactly("Mocked Task 1", "Mocked Task 2");

        // VERIFY: repository method was called
        then(todoRepository).should(times(1)).findAll();
    }

    @Test
    @DisplayName("createTodo() → title already exists → throws IllegalArgumentException")
    void createTodo_whenTitleExists_shouldThrow() {
        // ARRANGE: stub existsByTitle to return true (title "duplicate" already exists)
        given(todoRepository.existsByTitle("Duplicate")).willReturn(true);

        // ACT + ASSERT: service should throw
        assertThatThrownBy(() -> todoService.createTodo("Duplicate", 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        // VERIFY: save was NEVER called (exception was thrown before save)
        then(todoRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createTodo() → title is new → saves and returns todo")
    void createTodo_whenTitleIsNew_shouldSaveAndReturn() {
        // ARRANGE
        Todo saved = new Todo("New Task", 2);
        given(todoRepository.existsByTitle("New Task")).willReturn(false);
        given(todoRepository.save(any(Todo.class))).willReturn(saved);

        // ACT
        Todo result = todoService.createTodo("New Task", 2);

        // ASSERT
        assertThat(result.getTitle()).isEqualTo("New Task");
        assertThat(result.getPriority()).isEqualTo(2);

        // VERIFY with ArgumentCaptor — what exactly was passed to save()?
        ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
        then(todoRepository).should().save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("New Task");
        assertThat(captor.getValue().getPriority()).isEqualTo(2);
    }

    @Test
    @DisplayName("completeTodo() → id not found → throws TodoNotFoundException")
    void completeTodo_whenNotFound_shouldThrow() {
        // ARRANGE: stub findById to return empty (not found)
        given(todoRepository.findById(999L)).willReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> todoService.completeTodo(999L))
            .isInstanceOf(TodoService.TodoNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    @DisplayName("getStats() → mocked counts → returns correct stats map")
    void getStats_withMockedCounts_shouldReturnCorrectMap() {
        // ARRANGE: stub all aggregate methods
        given(todoRepository.count()).willReturn(10L);
        given(todoRepository.countByCompleted(false)).willReturn(7L);
        given(todoRepository.countByCompleted(true)).willReturn(3L);

        // ACT
        Map<String, Long> stats = todoService.getStats();

        // ASSERT
        assertThat(stats).containsEntry("total", 10L);
        assertThat(stats).containsEntry("pending", 7L);
        assertThat(stats).containsEntry("completed", 3L);
    }
}


// ════════════════════════════════════════════════════════════════════════════════════════
//  TEST CLASS 2: @SpyBean — Partial mock of a real Spring bean
// ════════════════════════════════════════════════════════════════════════════════════════

/**
 * Demonstrates @SpyBean: real TodoService but with ability to stub specific methods.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("Chapter 8 — @SpyBean: Spy on real service")
class Example04b_SpyBeanTest {

    /*
     * @SpyBean: wraps the REAL TodoService in a Mockito spy.
     * All methods delegate to the real implementation unless stubbed.
     * Real TodoService uses real TodoRepository which uses real H2.
     */
    @MockitoSpyBean
    private TodoService todoService;

    @Autowired
    private TodoRepository todoRepository;

    @BeforeEach
    void cleanUp() {
        todoRepository.deleteAll();
    }

    @Test
    @DisplayName("spy → real method works → verify method was called")
    void realMethod_shouldWork_andBeVerifiable() {
        // ACT: call the REAL method (spy delegates to real implementation)
        todoRepository.save(new Todo("Spy Task"));
        List<Todo> result = todoService.getAllTodos();

        // ASSERT: real result
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Spy Task");

        // VERIFY: method was actually called (spy records all invocations)
        then(todoService).should(atLeastOnce()).getAllTodos();
    }

    @Test
    @DisplayName("spy → stub ONE method → other methods still use real logic")
    void stubbedMethod_shouldReturnStubbedValue_otherMethodsStillReal() {
        // ARRANGE: stub getStats() to return fake data (avoids DB call)
        doReturn(Map.of("total", 100L, "pending", 80L, "completed", 20L))
            .when(todoService).getStats();

        // ACT: stubbed method
        Map<String, Long> stats = todoService.getStats();

        // ASSERT: stubbed value returned
        assertThat(stats).containsEntry("total", 100L);

        // Other methods still use real logic
        todoRepository.save(new Todo("Real task via spy"));
        List<Todo> todos = todoService.getAllTodos();
        assertThat(todos).hasSize(1);  // real DB call
    }
}


// ════════════════════════════════════════════════════════════════════════════════════════
//  TEST CLASS 3: Pure unit test — @Mock + @InjectMocks (NO Spring context)
// ════════════════════════════════════════════════════════════════════════════════════════

/**
 * Pure unit test: NO Spring context loaded.
 * @ExtendWith(MockitoExtension.class) enables @Mock, @Spy, @InjectMocks, @Captor.
 *
 * This is the FASTEST kind of test — typically runs in milliseconds.
 * Ideal for testing business logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Chapter 8 — Pure Unit Test: @Mock + @InjectMocks (no Spring context)")
class Example04c_PureUnitTest {

    /*
     * @Mock: pure Mockito mock — no Spring involved.
     * ALL methods return Mockito defaults unless stubbed.
     */
    @Mock
    private TodoRepository todoRepository;

    /*
     * @InjectMocks: creates a real instance of TodoService and injects
     * all @Mock fields into it (via constructor, then setter, then field injection).
     *
     * Since TodoService has a constructor: TodoService(TodoRepository repo)
     * → Mockito uses constructor injection → todoRepository mock is injected.
     */
    @InjectMocks
    private TodoService todoService;

    /*
     * @Captor: creates an ArgumentCaptor — used to capture and inspect
     * arguments passed to mock methods.
     */
    @Captor
    private ArgumentCaptor<Todo> savedTodoCaptor;

    @Test
    @DisplayName("createTodo() → valid → saves todo with correct title and priority")
    void createTodo_valid_shouldSaveWithCorrectFields() {
        // ARRANGE
        given(todoRepository.existsByTitle("Buy milk")).willReturn(false);
        given(todoRepository.save(any())).willAnswer(inv -> inv.getArgument(0));  // return arg as-is

        // ACT
        todoService.createTodo("Buy milk", 2);

        // ASSERT via ArgumentCaptor — what was passed to save()?
        then(todoRepository).should().save(savedTodoCaptor.capture());
        Todo capturedTodo = savedTodoCaptor.getValue();
        assertThat(capturedTodo.getTitle()).isEqualTo("Buy milk");
        assertThat(capturedTodo.getPriority()).isEqualTo(2);
        assertThat(capturedTodo.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("completeTodo() → found → marks completed and saves")
    void completeTodo_whenFound_shouldMarkAndSave() {
        // ARRANGE
        Todo existing = new Todo("Task to complete");
        given(todoRepository.findById(1L)).willReturn(Optional.of(existing));
        given(todoRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // ACT
        Todo result = todoService.completeTodo(1L);

        // ASSERT
        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getCompletedAt()).isNotNull();

        // Verify save() was called with the completed todo
        then(todoRepository).should().save(savedTodoCaptor.capture());
        assertThat(savedTodoCaptor.getValue().isCompleted()).isTrue();
    }

    @Test
    @DisplayName("deleteTodo() → not found → throws TodoNotFoundException without calling delete")
    void deleteTodo_whenNotFound_shouldThrowWithoutDeleting() {
        // ARRANGE
        given(todoRepository.existsById(99L)).willReturn(false);

        // ACT + ASSERT
        assertThatThrownBy(() -> todoService.deleteTodo(99L))
            .isInstanceOf(TodoService.TodoNotFoundException.class)
            .hasMessageContaining("99");

        // VERIFY: deleteById was never called
        then(todoRepository).should(never()).deleteById(any());
    }

    @Test
    @DisplayName("searchByTitle() → delegates to repository correctly")
    void searchByTitle_shouldDelegateToRepo() {
        // ARRANGE
        List<Todo> expected = List.of(new Todo("Buy milk"), new Todo("Buy eggs"));
        given(todoRepository.findByTitleContainingIgnoreCase("buy")).willReturn(expected);

        // ACT
        List<Todo> result = todoService.searchByTitle("buy");

        // ASSERT
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Todo::getTitle)
            .containsExactly("Buy milk", "Buy eggs");
    }
}

