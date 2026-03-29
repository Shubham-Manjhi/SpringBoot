package com.learning.springboot.chapter04;

import jakarta.persistence.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║             EXAMPLE 03: QUERY ANNOTATIONS IN ACTION                                  ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example03QueryAnnotations.java
 * Purpose:     Demonstrate @Query (JPQL + native SQL), @NamedQuery,
 *              @Modifying, @Param, derived query methods, projections,
 *              pagination, and sorting.
 * Difficulty:  ⭐⭐⭐ Intermediate
 * Time:        60–90 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * 📚 QUERY APPROACHES IN SPRING DATA JPA (from simple to advanced):
 *
 *   APPROACH 1: Derived Query Methods
 *     → Spring generates SQL from the method name automatically
 *     → No @Query needed — just follow the naming convention
 *     → Best for simple queries
 *
 *   APPROACH 2: @Query with JPQL
 *     → You write JPQL (Java Persistence Query Language)
 *     → Queries use entity/field names, not table/column names
 *     → Portable across databases
 *     → Best for moderate complexity
 *
 *   APPROACH 3: @Query with native SQL (nativeQuery = true)
 *     → You write raw SQL
 *     → Uses table/column names
 *     → Can use DB-specific syntax
 *     → Best when JPQL can't express what you need
 *
 *   APPROACH 4: @NamedQuery
 *     → Query defined on the entity class
 *     → Pre-validated at startup
 *     → Good for frequently-used queries
 *
 *   APPROACH 5: Specifications & Criteria API
 *     → Programmatic, type-safe query building
 *     → Best for dynamic/conditional queries
 *     → Not covered here (advanced topic)
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */


// ══════════════════════════════════════════════════════════════════════════════════════
//  ENTITY WITH @NamedQuery and @NamedNativeQuery
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║               @NamedQuery & @NamedNativeQuery — ENTITY-LEVEL QUERIES         ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @NamedQuery
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT DOES:
 *   Defines a JPQL query ONCE on the entity class and gives it a name.
 *   The query can then be referenced by name in repositories or EntityManager.
 *
 * BENEFITS:
 *   1. VALIDATED AT STARTUP
 *      → Hibernate parses and validates the query when the application starts.
 *      → If you have a JPQL syntax error, you know immediately on startup,
 *        not when the query is called at runtime.
 *
 *   2. CENTRALIZED DEFINITION
 *      → Query lives next to the entity it belongs to.
 *      → Easy to find and maintain.
 *
 *   3. CACHED BY HIBERNATE
 *      → Named queries are pre-compiled and cached, slightly improving performance
 *        vs. dynamic queries that need to be parsed every time.
 *
 * NAMING CONVENTION:
 *   EntityName.methodName → e.g., "Article.findPublished"
 *   This naming convention allows Spring Data to automatically use @NamedQuery
 *   if a repository method has the same name.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @NamedNativeQuery
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT DOES:
 *   Same as @NamedQuery but for native SQL.
 *   Requires a @SqlResultSetMapping if you want to map results to entities.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Entity
@Table(name = "tbl_articles")

// Multiple named queries on one entity — use @NamedQueries container
@NamedQueries({
    /*
     * Query 1: Find all published articles
     *
     * JPQL notes:
     *   "Article" → entity name (class name), NOT table name
     *   "a.published" → Java field name, NOT column name
     *   "a" → alias for Article
     *   ":status" → named parameter (bound using @Param in repository)
     */
    @NamedQuery(
        name  = "Article.findByPublishedStatus",
        query = "SELECT a FROM Article a WHERE a.published = :status ORDER BY a.publishedAt DESC"
    ),

    /*
     * Query 2: Find articles by author, count total
     */
    @NamedQuery(
        name  = "Article.countByAuthor",
        query = "SELECT COUNT(a) FROM Article a WHERE a.authorName = :authorName"
    ),

    /*
     * Query 3: Find most-viewed articles (sorted by viewCount descending)
     */
    @NamedQuery(
        name  = "Article.findTopByViewCount",
        query = "SELECT a FROM Article a ORDER BY a.viewCount DESC"
    )
})

// Named native SQL query — uses actual table/column names
@NamedNativeQuery(
    name            = "Article.findByTagNative",
    query           = "SELECT * FROM tbl_articles WHERE tags LIKE CONCAT('%', :tag, '%')",
    resultClass     = Article.class
)
class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "tags", length = 500)
    private String tags;  // Comma-separated: "java,spring,jpa"

    // ── Constructors ──────────────────────────────────────────────────────────────
    protected Article() {}

    public Article(String title, String content, String authorName) {
        this.title      = title;
        this.content    = content;
        this.authorName = authorName;
    }

    public void publish() {
        this.published   = true;
        this.publishedAt = LocalDateTime.now();
    }

    public Long          getId()          { return id; }
    public String        getTitle()       { return title; }
    public String        getContent()     { return content; }
    public String        getAuthorName()  { return authorName; }
    public boolean       isPublished()    { return published; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public Long          getViewCount()   { return viewCount; }
    public String        getTags()        { return tags; }
    public void          setTags(String t){ this.tags = t; }
    public void          setViewCount(Long v) { this.viewCount = v; }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PROJECTIONS — DTOs and Interface-Based
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║               PROJECTIONS — LOAD ONLY THE DATA YOU NEED                      ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 INTERFACE-BASED PROJECTION
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT IS:
 *   An interface that declares only the fields you want to load.
 *   Spring Data JPA generates a proxy implementation at runtime.
 *   Hibernate issues a SELECT that fetches ONLY the declared columns — not ALL columns.
 *
 * WHEN TO USE:
 *   → When you need a subset of entity fields (e.g., list view showing only title + author)
 *   → To avoid loading large TEXT/BLOB columns unnecessarily
 *   → To protect sensitive data (e.g., never expose password hash)
 *
 * PERFORMANCE:
 *   SELECT a.title, a.author_name FROM tbl_articles   ← Projection (fast)
 *   SELECT * FROM tbl_articles                         ← Full entity (slower if large)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
interface ArticleSummary {
    Long   getId();
    String getTitle();       // Must match entity field name (camelCase)
    String getAuthorName();  // Corresponds to "authorName" field
    Long   getViewCount();
}

/**
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DTO-BASED (CLASS-BASED) PROJECTION
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT IS:
 *   A regular Java class (record or POJO) that holds a subset of data.
 *   Use in JPQL with the "new" keyword:
 *     "SELECT new com.example.ArticleDto(a.id, a.title) FROM Article a"
 *
 * BENEFITS:
 *   → Type-safe, immutable (if record)
 *   → Can have custom constructor logic / computed fields
 *   → Clearly documents what data is needed
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
class ArticleDto {
    private final Long   id;
    private final String title;
    private final String authorName;
    private final Long   viewCount;

    // Constructor must match the JPQL "new" expression exactly
    public ArticleDto(Long id, String title, String authorName, Long viewCount) {
        this.id         = id;
        this.title      = title;
        this.authorName = authorName;
        this.viewCount  = viewCount;
    }

    public Long   getId()         { return id; }
    public String getTitle()      { return title; }
    public String getAuthorName() { return authorName; }
    public Long   getViewCount()  { return viewCount; }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  REPOSITORY WITH ALL QUERY ANNOTATIONS
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║        ArticleRepository — COMPREHENSIVE QUERY ANNOTATION EXAMPLES           ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
interface ArticleRepository extends JpaRepository<Article, Long> {

    // ═══════════════════════════════════════════════════════════════════════════
    //  APPROACH 1: DERIVED QUERY METHODS (No @Query needed!)
    // ═══════════════════════════════════════════════════════════════════════════

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 DERIVED QUERY METHODS
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT IS:
     *   Spring Data parses the METHOD NAME and generates the JPQL automatically.
     *   No @Query annotation needed — just follow the naming convention.
     *
     * NAMING KEYWORDS:
     *
     *   findBy...         → SELECT WHERE
     *   countBy...        → SELECT COUNT(*) WHERE
     *   existsBy...       → SELECT EXISTS(...)
     *   deleteBy...       → DELETE WHERE
     *
     *   And/Or            → logical operators
     *   Not               → NOT equal
     *   Like              → LIKE '%value%'
     *   Containing        → LIKE '%value%' (same as Like)
     *   StartingWith      → LIKE 'value%'
     *   EndingWith        → LIKE '%value'
     *   IgnoreCase        → LOWER() comparison
     *   GreaterThan       → >
     *   GreaterThanEqual  → >=
     *   LessThan          → <
     *   LessThanEqual     → <=
     *   Between           → BETWEEN ? AND ?
     *   IsNull            → IS NULL
     *   IsNotNull         → IS NOT NULL
     *   In                → IN (?, ?, ?)
     *   NotIn             → NOT IN (?, ?, ?)
     *   OrderBy...Asc/Desc → ORDER BY
     *   Top/First         → LIMIT
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */

    // → SELECT a FROM Article a WHERE a.published = ?
    List<Article> findByPublished(boolean published);

    // → SELECT a FROM Article a WHERE a.authorName = ?
    List<Article> findByAuthorName(String authorName);

    // → SELECT a FROM Article a WHERE a.title LIKE '%keyword%' (case-insensitive)
    List<Article> findByTitleContainingIgnoreCase(String keyword);

    // → SELECT a FROM Article a WHERE a.viewCount >= ? ORDER BY a.viewCount DESC
    List<Article> findByViewCountGreaterThanEqualOrderByViewCountDesc(Long minViews);

    // → SELECT a FROM Article a WHERE a.authorName = ? AND a.published = ?
    List<Article> findByAuthorNameAndPublished(String authorName, boolean published);

    // → SELECT a FROM Article a WHERE a.published = ? ORDER BY a.publishedAt DESC LIMIT 5
    List<Article> findTop5ByPublishedOrderByPublishedAtDesc(boolean published);

    // → SELECT EXISTS(SELECT a FROM Article a WHERE a.title = ?)
    boolean existsByTitle(String title);

    // → SELECT COUNT(a) FROM Article a WHERE a.authorName = ?
    Long countByAuthorName(String authorName);

    // → SELECT a FROM Article a WHERE a.publishedAt IS NOT NULL
    List<Article> findByPublishedAtIsNotNull();

    // ═══════════════════════════════════════════════════════════════════════════
    //  APPROACH 2: @Query WITH JPQL
    // ═══════════════════════════════════════════════════════════════════════════

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Query (JPQL) — Named Parameters with @Param
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IS JPQL?
     *   Java Persistence Query Language — an SQL-like language where you query
     *   ENTITY CLASSES and FIELDS, not database TABLES and COLUMNS.
     *
     *   JPQL is case-insensitive for keywords (SELECT, FROM, WHERE)
     *   but case-SENSITIVE for entity names and field names.
     *
     * JPQL vs SQL COMPARISON:
     *   SQL:   SELECT * FROM tbl_articles WHERE author_name = 'John'
     *   JPQL:  SELECT a FROM Article a WHERE a.authorName = 'John'
     *
     *   SQL:   SELECT id, title FROM tbl_articles ORDER BY view_count DESC
     *   JPQL:  SELECT a.id, a.title FROM Article a ORDER BY a.viewCount DESC
     *
     * NAMED PARAMETERS (preferred):
     *   :paramName  → bound using @Param("paramName") on the method argument
     *   Example:    WHERE a.authorName = :authorName
     *
     * POSITIONAL PARAMETERS (avoid in modern code):
     *   ?1, ?2  → bound by position
     *   Example: WHERE a.authorName = ?1 AND a.published = ?2
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */

    /**
     * Find published articles by author, with JOIN FETCH to avoid N+1 if Article
     * has lazy collections (none here, but shown for pattern awareness).
     *
     * @Param("authorName") binds the authorName method parameter to :authorName in JPQL.
     */
    @Query("SELECT a FROM Article a " +
           "WHERE a.authorName = :authorName " +
           "AND a.published = true " +
           "ORDER BY a.publishedAt DESC")
    List<Article> findPublishedByAuthor(@Param("authorName") String authorName);

    /**
     * Search articles by keyword in title OR content.
     *
     * JPQL LOWER() function for case-insensitive matching.
     * CONCAT('%', :kw, '%') builds the LIKE pattern in JPQL.
     */
    @Query("SELECT a FROM Article a " +
           "WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Article> searchByKeyword(@Param("keyword") String keyword);

    /**
     * Find articles with view count in a range.
     * Multiple @Param parameters, each bound independently.
     */
    @Query("SELECT a FROM Article a " +
           "WHERE a.viewCount BETWEEN :minViews AND :maxViews " +
           "ORDER BY a.viewCount DESC")
    List<Article> findByViewCountBetween(
        @Param("minViews") Long minViews,
        @Param("maxViews") Long maxViews
    );

    /**
     * DTO-based projection using JPQL "new" expression.
     *
     * Instead of loading the entire Article entity (including large TEXT content),
     * we load only the fields needed for a list/summary view.
     *
     * FULL CLASS NAME must be used in the JPQL query:
     *   "new com.learning.springboot.chapter04.ArticleDto(...)"
     *
     * The DTO constructor must match exactly (same types, same order).
     */
    @Query("SELECT new com.learning.springboot.chapter04.ArticleDto(" +
           "    a.id, a.title, a.authorName, a.viewCount) " +
           "FROM Article a " +
           "WHERE a.published = true " +
           "ORDER BY a.viewCount DESC")
    List<ArticleDto> findPublishedSummaries();

    /**
     * Interface-based projection — returns ArticleSummary proxy instances.
     *
     * Spring Data handles the mapping — no "new" keyword needed.
     * Hibernate generates a SELECT with only the projected columns.
     */
    @Query("SELECT a FROM Article a WHERE a.published = true")
    List<ArticleSummary> findPublishedSummariesAsInterface();

    /**
     * Pagination with @Query.
     *
     * Pageable parameter enables Spring Data to automatically add:
     *   - LIMIT / OFFSET (pagination)
     *   - ORDER BY (sorting from Sort object inside Pageable)
     *
     * Return type Page<T> includes total element count (requires a count query).
     * countQuery attribute: the query used for COUNT(*) (pagination total).
     *
     * Usage:
     *   Pageable page = PageRequest.of(0, 10, Sort.by("viewCount").descending());
     *   Page<Article> articles = repo.findPublishedPaged(page);
     *   articles.getTotalElements()  // total count
     *   articles.getContent()        // current page content
     */
    @Query(
        value      = "SELECT a FROM Article a WHERE a.published = true",
        countQuery = "SELECT COUNT(a) FROM Article a WHERE a.published = true"
    )
    Page<Article> findPublishedPaged(Pageable pageable);

    // ═══════════════════════════════════════════════════════════════════════════
    //  APPROACH 3: @Query WITH NATIVE SQL  (nativeQuery = true)
    // ═══════════════════════════════════════════════════════════════════════════

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Query(nativeQuery = true) — RAW SQL
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Executes a raw SQL query directly against the database.
     *   Uses TABLE names and COLUMN names (not entity/field names).
     *
     * WHEN TO USE:
     *   → Database-specific features (window functions, recursive CTEs, JSONB)
     *   → Complex queries that JPQL can't express
     *   → Performance-critical queries where you need exact SQL control
     *
     * DOWNSIDES:
     *   → Not portable across databases (MySQL vs PostgreSQL syntax may differ)
     *   → No compile-time / startup validation of column names
     *   → Must be updated if table/column names change
     *
     * NAMED PARAMETERS work the same way (:paramName + @Param).
     * ─────────────────────────────────────────────────────────────────────────────
     */

    /**
     * Native SQL query to find top articles using DB-specific syntax.
     * Here we use H2 SQL (compatible with most SQL databases).
     *
     * nativeQuery = true → Spring executes this as raw SQL.
     * Returns List<Article> — Hibernate maps ResultSet columns to entity fields
     * using the @Column mappings defined on the entity.
     */
    @Query(
        value       = "SELECT * FROM tbl_articles " +
                      "WHERE published = true " +
                      "ORDER BY view_count DESC " +
                      "LIMIT :limit",
        nativeQuery = true
    )
    List<Article> findTopPublishedNative(@Param("limit") int limit);

    /**
     * Native query returning a scalar value (not mapped to an entity).
     */
    @Query(
        value       = "SELECT SUM(view_count) FROM tbl_articles WHERE author_name = :author",
        nativeQuery = true
    )
    Long sumViewsByAuthorNative(@Param("author") String author);

    // ═══════════════════════════════════════════════════════════════════════════
    //  APPROACH 4: @NamedQuery (defined on the entity)
    // ═══════════════════════════════════════════════════════════════════════════

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 Using @NamedQuery in Repository
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * The @NamedQuery is defined ON THE ENTITY (see Article class above).
     * To use it in a Spring Data repository:
     *
     * OPTION 1: Method name matches the named query name exactly
     *   Named query: "Article.findByPublishedStatus"
     *   Method name: "findByPublishedStatus"
     *   Spring Data automatically uses the named query.
     *
     * OPTION 2: @Query annotation references the named query by name
     *   @Query(name = "Article.findByPublishedStatus")
     *
     * We use OPTION 1 here — the method name matches the NamedQuery name.
     * ─────────────────────────────────────────────────────────────────────────────
     */
    List<Article> findByPublishedStatus(@Param("status") boolean status);

    // ═══════════════════════════════════════════════════════════════════════════
    //  APPROACH 5: @Modifying — UPDATE and DELETE queries
    // ═══════════════════════════════════════════════════════════════════════════

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Modifying — For UPDATE and DELETE @Query methods
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Tells Spring Data that this @Query method modifies data (not a SELECT).
     *   Required for any @Query that runs an UPDATE or DELETE statement.
     *   Without @Modifying, Spring Data throws InvalidDataAccessApiUsageException.
     *
     * MUST COMBINE WITH @Transactional:
     *   Modifying queries MUST run within a transaction.
     *   If the repository method is called from a @Transactional service, it works.
     *   But it's safer and more explicit to declare @Transactional here too.
     *
     * KEY ATTRIBUTES:
     *
     *   clearAutomatically = true
     *     → After the UPDATE/DELETE, clears the first-level cache (persistence context).
     *     → IMPORTANT: If you updated rows via bulk query, the in-memory entity objects
     *       still have the OLD values. Clearing the cache forces Hibernate to reload
     *       from DB on next access.
     *     → Default: false (must explicitly enable)
     *
     *   flushAutomatically = true
     *     → Flush any pending changes to the DB BEFORE executing this query.
     *     → Ensures in-memory changes are written to DB before the bulk operation.
     *     → Default: false
     *
     * RETURN TYPE:
     *   int or void — int returns the count of rows affected.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */

    /**
     * Bulk UPDATE: publish all articles by a specific author.
     *
     * JPQL UPDATE syntax:
     *   UPDATE EntityName alias SET alias.field = value WHERE condition
     *
     * This is a BULK operation — it bypasses the persistence context!
     * Entities already loaded in memory will NOT see this change.
     * clearAutomatically = true forces Hibernate to reload from DB next time.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Article a SET a.published = true, a.publishedAt = :now " +
           "WHERE a.authorName = :authorName AND a.published = false")
    int publishAllByAuthor(
        @Param("authorName") String authorName,
        @Param("now") LocalDateTime now
    );

    /**
     * Bulk UPDATE: increment view count for a specific article.
     *
     * Note: clearAutomatically = true is important here.
     *       Without it, after incrementing via SQL, the in-memory entity
     *       (if cached) still shows the old viewCount value.
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(@Param("id") Long id);

    /**
     * Bulk DELETE: delete all unpublished drafts by an author.
     *
     * JPQL DELETE syntax:
     *   DELETE FROM EntityName alias WHERE condition
     *
     * ⚠️  This bypasses @PreRemove lifecycle callbacks!
     *     Use with caution — lifecycle callbacks (like logging, cleanup) will NOT fire.
     *     For callbacks to fire, you must load entities and call entityManager.remove()
     *     one by one, which is much slower for large datasets.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM Article a WHERE a.authorName = :authorName AND a.published = false")
    int deleteUnpublishedByAuthor(@Param("authorName") String authorName);

    /**
     * Native DELETE for demonstration.
     * Useful when you need to bypass JPA and use raw SQL (e.g., soft delete via trigger).
     */
    @Modifying
    @Transactional
    @Query(
        value       = "UPDATE tbl_articles SET view_count = 0 WHERE id = :id",
        nativeQuery = true
    )
    void resetViewCountNative(@Param("id") Long id);

    /**
     * Derived DELETE method — Spring generates DELETE SQL automatically.
     * Note: This DOES trigger lifecycle callbacks (@PreRemove) for each deleted entity.
     *       Spring Data loads the entities first, then deletes them one by one.
     *       Good for small datasets; use @Modifying @Query DELETE for bulk operations.
     */
    @Transactional
    void deleteByPublishedFalseAndAuthorName(String authorName);

    /**
     * Using Optional<T> for single-result queries.
     *
     * ALWAYS return Optional<T> (not T directly) for methods that may return null.
     * This forces the caller to handle the "not found" case explicitly:
     *
     *   repo.findByTitle("Spring JPA")
     *       .ifPresent(article -> System.out.println(article.getTitle()));
     *
     *   Article art = repo.findByTitle("Spring JPA")
     *                     .orElseThrow(() -> new RuntimeException("Not found"));
     */
    Optional<Article> findByTitle(String title);
}


/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📚 LEARNING SUMMARY 📚
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * ✅ WHAT WE LEARNED IN EXAMPLE 03:
 *
 *  CONCEPT                  RULE / KEY POINT
 *  ──────────────────────   ──────────────────────────────────────────────────────────
 *  Derived Query Methods    Spring auto-generates SQL from method name — use for simple queries
 *                           No @Query needed; follow naming keywords (findBy, countBy, etc.)
 *
 *  @Query (JPQL)            Write your own JPQL using entity/field names (not table/column names)
 *                           Use :paramName + @Param for named parameters
 *                           JPQL is validated at startup — syntax errors caught early
 *
 *  @Query (nativeQuery=true) Write raw SQL using table/column names
 *                           Less portable; use when JPQL is insufficient
 *
 *  @NamedQuery              Define on entity class; validated at startup; cached by Hibernate
 *                           Spring Data auto-links if method name matches query name
 *
 *  @Param                   Binds method parameter to :namedParameter in @Query
 *                           Required when using named parameters (:name) in JPQL or SQL
 *
 *  @Modifying               REQUIRED for @Query methods that run UPDATE or DELETE
 *                           ALWAYS combine with @Transactional
 *                           Use clearAutomatically=true to sync in-memory entities after bulk ops
 *
 *  Projections              Interface-based: Spring proxy; loads only declared columns
 *                           DTO-based: use "new FullClass.Name(...)" in JPQL for type safety
 *
 *  Pagination               Add Pageable parameter + return Page<T> for page info
 *                           Use countQuery attribute for efficient total count query
 *
 *  Optional<T>              Always return Optional<T> for single-result queries (never null)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️  CRITICAL RULES TO REMEMBER:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. @Modifying without @Transactional → Runtime exception
 *  2. Bulk @Modifying bypass @PreRemove callbacks — use entity-level removal for callbacks
 *  3. Without clearAutomatically=true, in-memory entities show stale data after bulk updates
 *  4. JPQL uses entity name ("Article") and field name ("authorName") — NEVER table/column names
 *  5. Native @Query uses table name ("tbl_articles") and column name ("author_name")
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🧪 EXERCISES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. Write a derived query: findTop3ByPublishedTrueOrderByViewCountDesc()
 *  2. Write a @Query to find articles published in the last 7 days.
 *  3. Write a @Modifying query to bulk-update tags for a specific author.
 *  4. Create an interface projection for Article with only id, title, and publishedAt.
 *  5. Use Pageable to paginate through all published articles, 5 per page.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ➡️  NEXT: Example04RepositoryAnnotations.java
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
class Example03QueryAnnotations {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║       CHAPTER 4 — EXAMPLE 03: Query Annotations                  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  Approach 1: Derived Query Methods (findBy..., countBy..., deleteBy...)");
        System.out.println("  Approach 2: @Query with JPQL (entity/field names)");
        System.out.println("  Approach 3: @Query with native SQL (nativeQuery=true)");
        System.out.println("  Approach 4: @NamedQuery (defined on entity, validated at startup)");
        System.out.println("  Approach 5: @Modifying (bulk UPDATE / DELETE) + @Transactional");
        System.out.println("  Projections: Interface-based and DTO-based (load only needed columns)");
        System.out.println("  Pagination:  Pageable + Page<T> with countQuery");
        System.out.println("  @Param:      Named parameter binding (:paramName in JPQL)");
        System.out.println("  Optional<T>: Always use for single-result queries (never return null)");
        System.out.println();
        System.out.println("See ArticleRepository interface for all examples in one place!");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

