package com.learning.springboot.chapter08;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TodoRepository — used as the target for @DataJpaTest examples in Chapter 8.
 *
 * All query methods here demonstrate patterns that @DataJpaTest verifies:
 *  - Derived query methods
 *  - @Query (JPQL)
 *  - Aggregate functions
 */
@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    // Derived queries — Spring Data auto-generates SQL from method name
    List<Todo> findByCompleted(boolean completed);
    List<Todo> findByPriority(int priority);
    List<Todo> findByTitleContainingIgnoreCase(String keyword);
    List<Todo> findByCompletedOrderByPriorityDesc(boolean completed);
    List<Todo> findTop3ByOrderByPriorityDesc();

    long countByCompleted(boolean completed);
    boolean existsByTitle(String title);

    // Custom JPQL query
    @Query("SELECT t FROM Todo t WHERE t.priority = :priority AND t.completed = false")
    List<Todo> findPendingByPriority(@Param("priority") int priority);

    // Native query
    @Query(value = "SELECT COUNT(*) FROM ch08_todos WHERE completed = true", nativeQuery = true)
    long countCompletedNative();
}

