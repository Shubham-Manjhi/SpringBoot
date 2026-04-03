package com.learning.springboot.chapter08;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * TodoController — the target for @WebMvcTest examples in Chapter 8.
 *
 * Endpoint base: /api/ch08/todos  (prefixed "ch08" to avoid path conflicts)
 *
 * Endpoints:
 *   GET    /api/ch08/todos           → all todos
 *   GET    /api/ch08/todos/{id}      → single todo
 *   GET    /api/ch08/todos/pending   → all pending todos
 *   GET    /api/ch08/todos/search?q= → search by title
 *   GET    /api/ch08/todos/stats     → counts (total, pending, completed)
 *   POST   /api/ch08/todos           → create new todo
 *   PUT    /api/ch08/todos/{id}/complete → mark as done
 *   PUT    /api/ch08/todos/{id}/title   → update title
 *   DELETE /api/ch08/todos/{id}      → delete
 */
@RestController
@RequestMapping("/api/ch08/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    // ── READ ──────────────────────────────────────────────────────────────────────

    @GetMapping
    public List<Todo> getAll() {
        return todoService.getAllTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Todo> getById(@PathVariable Long id) {
        return todoService.getById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pending")
    public List<Todo> getPending() {
        return todoService.getPendingTodos();
    }

    @GetMapping("/search")
    public List<Todo> search(@RequestParam String q) {
        return todoService.searchByTitle(q);
    }

    @GetMapping("/stats")
    public Map<String, Long> getStats() {
        return todoService.getStats();
    }

    // ── WRITE ─────────────────────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Todo create(@Valid @RequestBody CreateTodoRequest request) {
        return todoService.createTodo(request.getTitle(), request.getPriority());
    }

    @PutMapping("/{id}/complete")
    public Todo complete(@PathVariable Long id) {
        return todoService.completeTodo(id);
    }

    @PutMapping("/{id}/title")
    public Todo updateTitle(
            @PathVariable Long id,
            @RequestParam String title) {
        return todoService.updateTitle(id, title);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        todoService.deleteTodo(id);
    }

    // ── Exception handling (local to this controller) ─────────────────────────────
    @ExceptionHandler(TodoService.TodoNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(TodoService.TodoNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConflict(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }
}

