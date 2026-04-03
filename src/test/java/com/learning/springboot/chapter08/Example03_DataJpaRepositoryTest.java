package com.learning.springboot.chapter08;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║   CHAPTER 8 — TEST FILE 03: @DataJpaTest Repository Slice Tests                     ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * WHAT IS TESTED:
 *   → @DataJpaTest loads ONLY JPA layer (entities + repositories + H2)
 *   → Each test is wrapped in a transaction that ROLLS BACK after the test
 *   → TestEntityManager for direct entity persistence + cache control
 *   → Derived query methods (findByCompleted, findByTitleContaining...)
 *   → Custom @Query methods
 *   → Entity constraint validation at DB level
 *
 * KEY ANNOTATIONS DEMONSTRATED:
 *   @DataJpaTest, @Autowired TestEntityManager, @Nested, @DisplayName,
 *   @BeforeEach (with entity manager)
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
@DataJpaTest
@DisplayName("Chapter 8 — @DataJpaTest: TodoRepository")
class Example03_DataJpaRepositoryTest {

    /*
     * @DataJpaTest auto-wires the repository being tested.
     * TodoRepository is a real Spring Data JPA repository backed by H2.
     */
    @Autowired
    private TodoRepository todoRepository;

    /*
     * TestEntityManager: a test-friendly wrapper around EntityManager.
     * Used for: persisting test data, flushing, clearing the L1 cache.
     *
     * KEY METHODS:
     *   persistAndFlush(entity) → persist + flush in one call (writes to H2 immediately)
     *   flush() → force pending writes to DB
     *   clear() → clear Hibernate's L1 (first-level) cache — next read hits DB
     *   find(Class, id) → find by PK
     *   refresh(entity) → reload from DB
     */
    @Autowired
    private TestEntityManager entityManager;

    /*
     * @BeforeEach: clean the table before each test.
     *
     * WHY? @DataJpaTest rolls back AFTER each test, but we still need a clean
     * state at the START. If a previous test failed mid-way (e.g., assertion after
     * data was saved), data might still be in the transaction that we're in.
     * deleteAll() in @BeforeEach guarantees we start fresh.
     */
    @BeforeEach
    void cleanUp() {
        todoRepository.deleteAll();
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  BASIC CRUD
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Basic CRUD operations")
    class BasicCrud {

        @Test
        @DisplayName("save() → persists todo → findById returns it")
        void save_shouldPersistTodo() {
            // ARRANGE + ACT
            Todo saved = todoRepository.save(new Todo("Buy milk", 1));

            // ASSERT: flush and clear to force a real DB read
            entityManager.flush();
            entityManager.clear();

            Todo found = todoRepository.findById(saved.getId()).orElseThrow();
            assertThat(found.getTitle()).isEqualTo("Buy milk");
            assertThat(found.isCompleted()).isFalse();
            assertThat(found.getPriority()).isEqualTo(1);
            assertThat(found.getCreatedAt()).isNotNull();  // @PrePersist sets this
        }

        @Test
        @DisplayName("save() → multiple todos → findAll returns all")
        void save_multipleTodos_shouldFindAll() {
            todoRepository.save(new Todo("Task 1", 1));
            todoRepository.save(new Todo("Task 2", 2));
            todoRepository.save(new Todo("Task 3", 3));

            assertThat(todoRepository.findAll()).hasSize(3);
            assertThat(todoRepository.count()).isEqualTo(3);
        }

        @Test
        @DisplayName("deleteById() → removes todo → findById returns empty")
        void deleteById_shouldRemoveTodo() {
            Todo saved = todoRepository.save(new Todo("To delete"));
            Long id = saved.getId();

            todoRepository.deleteById(id);
            entityManager.flush();

            assertThat(todoRepository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("markCompleted() → updates completed flag → persisted correctly")
        void markCompleted_shouldPersistCompletedState() {
            Todo saved = entityManager.persistAndFlush(new Todo("Task"));
            entityManager.clear();

            // Load from DB, mark completed, save
            Todo loaded = todoRepository.findById(saved.getId()).orElseThrow();
            loaded.markCompleted();
            todoRepository.save(loaded);
            entityManager.flush();
            entityManager.clear();

            // Verify completed state is persisted
            Todo updated = todoRepository.findById(saved.getId()).orElseThrow();
            assertThat(updated.isCompleted()).isTrue();
            assertThat(updated.getCompletedAt()).isNotNull();
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  DERIVED QUERY METHODS
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Derived query methods")
    class DerivedQueries {

        @Test
        @DisplayName("findByCompleted(false) → returns only pending todos")
        void findByCompleted_false_shouldReturnPending() {
            // ARRANGE: 2 pending, 1 completed
            todoRepository.save(new Todo("Pending 1"));
            todoRepository.save(new Todo("Pending 2"));
            Todo done = new Todo("Done");
            done.markCompleted();
            todoRepository.save(done);
            entityManager.flush();

            // ACT
            List<Todo> pending = todoRepository.findByCompleted(false);

            // ASSERT
            assertThat(pending).hasSize(2);
            assertThat(pending).allMatch(t -> !t.isCompleted());
            assertThat(pending)
                .extracting(Todo::getTitle)
                .containsExactlyInAnyOrder("Pending 1", "Pending 2");
        }

        @Test
        @DisplayName("findByCompleted(true) → returns only completed todos")
        void findByCompleted_true_shouldReturnCompleted() {
            Todo done1 = new Todo("Done 1");
            done1.markCompleted();
            Todo done2 = new Todo("Done 2");
            done2.markCompleted();
            todoRepository.save(done1);
            todoRepository.save(done2);
            todoRepository.save(new Todo("Still pending"));
            entityManager.flush();

            List<Todo> completed = todoRepository.findByCompleted(true);

            assertThat(completed).hasSize(2);
            assertThat(completed).allMatch(Todo::isCompleted);
        }

        @Test
        @DisplayName("findByTitleContainingIgnoreCase() → case-insensitive search works")
        void findByTitleContaining_caseInsensitive() {
            todoRepository.save(new Todo("Buy Milk"));
            todoRepository.save(new Todo("buy eggs"));
            todoRepository.save(new Todo("Do laundry"));
            entityManager.flush();

            List<Todo> results = todoRepository.findByTitleContainingIgnoreCase("buy");

            assertThat(results).hasSize(2);
            assertThat(results)
                .extracting(Todo::getTitle)
                .containsExactlyInAnyOrder("Buy Milk", "buy eggs");
        }

        @Test
        @DisplayName("findByPriority() → returns only todos with matching priority")
        void findByPriority_shouldFilterByPriority() {
            todoRepository.save(new Todo("Low priority", 1));
            todoRepository.save(new Todo("Med priority", 2));
            todoRepository.save(new Todo("High priority", 3));
            todoRepository.save(new Todo("Also high", 3));
            entityManager.flush();

            List<Todo> highPriority = todoRepository.findByPriority(3);

            assertThat(highPriority).hasSize(2);
            assertThat(highPriority).allMatch(t -> t.getPriority() == 3);
        }

        @Test
        @DisplayName("findTop3ByOrderByPriorityDesc() → returns at most 3, highest priority first")
        void findTop3_shouldReturnTop3ByPriorityDesc() {
            todoRepository.save(new Todo("P1", 1));
            todoRepository.save(new Todo("P2", 2));
            todoRepository.save(new Todo("P3", 3));
            todoRepository.save(new Todo("P3b", 3));
            todoRepository.save(new Todo("P1b", 1));
            entityManager.flush();

            List<Todo> top3 = todoRepository.findTop3ByOrderByPriorityDesc();

            assertThat(top3).hasSize(3);
            assertThat(top3.get(0).getPriority()).isEqualTo(3);
            assertThat(top3.get(1).getPriority()).isEqualTo(3);
        }

        @Test
        @DisplayName("existsByTitle() → returns true if title exists")
        void existsByTitle_shouldReturnTrueIfExists() {
            todoRepository.save(new Todo("Unique Title"));
            entityManager.flush();

            assertThat(todoRepository.existsByTitle("Unique Title")).isTrue();
            assertThat(todoRepository.existsByTitle("Non-existent Title")).isFalse();
        }

        @Test
        @DisplayName("countByCompleted() → returns correct count")
        void countByCompleted_shouldReturnCorrectCount() {
            todoRepository.save(new Todo("P1"));
            todoRepository.save(new Todo("P2"));
            Todo done = new Todo("Done");
            done.markCompleted();
            todoRepository.save(done);
            entityManager.flush();

            assertThat(todoRepository.countByCompleted(false)).isEqualTo(2);
            assertThat(todoRepository.countByCompleted(true)).isEqualTo(1);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════
    //  CUSTOM @QUERY METHODS
    // ════════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Custom @Query methods")
    class CustomQueries {

        @Test
        @DisplayName("findPendingByPriority() → JPQL → only pending with given priority")
        void findPendingByPriority_shouldFilterCorrectly() {
            todoRepository.save(new Todo("High pending", 3));
            Todo highDone = new Todo("High done", 3);
            highDone.markCompleted();
            todoRepository.save(highDone);
            todoRepository.save(new Todo("Low pending", 1));
            entityManager.flush();

            List<Todo> result = todoRepository.findPendingByPriority(3);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("High pending");
            assertThat(result.get(0).isCompleted()).isFalse();
        }

        @Test
        @DisplayName("countCompletedNative() → native SQL → correct count")
        void countCompletedNative_shouldUseNativeSql() {
            Todo d1 = new Todo("Done 1"); d1.markCompleted();
            Todo d2 = new Todo("Done 2"); d2.markCompleted();
            todoRepository.save(d1);
            todoRepository.save(d2);
            todoRepository.save(new Todo("Pending"));
            entityManager.flush();

            long count = todoRepository.countCompletedNative();

            assertThat(count).isEqualTo(2);
        }
    }
}

