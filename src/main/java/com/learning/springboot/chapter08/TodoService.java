package com.learning.springboot.chapter08;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TodoService — the target for @MockBean / @SpyBean examples in Chapter 8.
 *
 * This service layer sits between the controller and repository.
 * In @WebMvcTest, this whole class is replaced by a Mockito mock (@MockBean).
 * In @SpyBean tests, the real instance is used but individual methods can be stubbed.
 */
@Service
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;

    // Constructor injection — makes @MockBean and @SpyBean injection straightforward
    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    // ── Read operations ────────────────────────────────────────────────────────────

    public List<Todo> getAllTodos() {
        return todoRepository.findAll();
    }

    public Optional<Todo> getById(Long id) {
        return todoRepository.findById(id);
    }

    public List<Todo> getPendingTodos() {
        return todoRepository.findByCompleted(false);
    }

    public List<Todo> getCompletedTodos() {
        return todoRepository.findByCompleted(true);
    }

    public List<Todo> searchByTitle(String keyword) {
        return todoRepository.findByTitleContainingIgnoreCase(keyword);
    }

    public Map<String, Long> getStats() {
        long total   = todoRepository.count();
        long pending = todoRepository.countByCompleted(false);
        long done    = todoRepository.countByCompleted(true);
        return Map.of("total", total, "pending", pending, "completed", done);
    }

    // ── Write operations ───────────────────────────────────────────────────────────

    @Transactional
    public Todo createTodo(String title, int priority) {
        if (todoRepository.existsByTitle(title)) {
            throw new IllegalArgumentException("A todo with title '" + title + "' already exists");
        }
        return todoRepository.save(new Todo(title, priority));
    }

    @Transactional
    public Todo completeTodo(Long id) {
        Todo todo = todoRepository.findById(id)
            .orElseThrow(() -> new TodoNotFoundException(id));
        todo.markCompleted();
        return todoRepository.save(todo);
    }

    @Transactional
    public Todo updateTitle(Long id, String newTitle) {
        Todo todo = todoRepository.findById(id)
            .orElseThrow(() -> new TodoNotFoundException(id));
        todo.setTitle(newTitle);
        return todo;   // dirty checking — no explicit save needed
    }

    @Transactional
    public void deleteTodo(Long id) {
        if (!todoRepository.existsById(id)) {
            throw new TodoNotFoundException(id);
        }
        todoRepository.deleteById(id);
    }

    // ── Custom exception (inner class for simplicity) ──────────────────────────────
    public static class TodoNotFoundException extends RuntimeException {
        public TodoNotFoundException(Long id) {
            super("Todo not found with id: " + id);
        }
    }
}

