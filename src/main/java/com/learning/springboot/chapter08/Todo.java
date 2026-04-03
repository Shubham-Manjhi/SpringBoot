package com.learning.springboot.chapter08;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                 CHAPTER 8 — DOMAIN MODEL: Todo Entity                                ║
 * ║  This simple entity is the TARGET of all testing examples in Chapter 8.             ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * WHY A SEPARATE DOMAIN?
 *   Chapter 8 tests need real entity / service / controller classes to test against.
 *   A self-contained "Todo" domain (simple enough to understand at a glance) lets
 *   us focus on the TESTING ANNOTATIONS rather than on the domain complexity.
 *
 * TABLE: ch08_todos  (prefixed to avoid conflicts with other chapter entities)
 */
@Entity
@Table(name = "ch08_todos")
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "priority", nullable = false)
    private int priority = 1;          // 1 = low, 2 = medium, 3 = high

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ── Lifecycle ─────────────────────────────────────────────────────────────────
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Constructors ──────────────────────────────────────────────────────────────
    protected Todo() {}

    public Todo(String title) {
        this.title = title;
    }

    public Todo(String title, int priority) {
        this.title    = title;
        this.priority = priority;
    }

    // ── Business method ───────────────────────────────────────────────────────────
    public void markCompleted() {
        this.completed   = true;
        this.completedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────
    public Long          getId()          { return id; }
    public String        getTitle()       { return title; }
    public void          setTitle(String t) { this.title = t; }
    public boolean       isCompleted()    { return completed; }
    public void          setCompleted(boolean c) { this.completed = c; }
    public int           getPriority()    { return priority; }
    public void          setPriority(int p) { this.priority = p; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    @Override
    public String toString() {
        return "Todo{id=" + id + ", title='" + title + "', completed=" + completed
               + ", priority=" + priority + '}';
    }
}

