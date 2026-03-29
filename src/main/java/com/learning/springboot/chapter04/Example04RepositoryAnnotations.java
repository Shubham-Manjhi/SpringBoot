package com.learning.springboot.chapter04;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║             EXAMPLE 04: REPOSITORY ANNOTATIONS IN ACTION                             ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example04RepositoryAnnotations.java
 * Purpose:     Demonstrate @Repository, @NoRepositoryBean, custom repository
 *              implementations, soft-delete pattern, complete service layer,
 *              and a fully wired REST controller — seeing everything together.
 * Difficulty:  ⭐⭐⭐⭐ Intermediate–Advanced
 * Time:        60–90 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * 🏗️ WHAT WE ARE BUILDING:
 *
 *   A complete, production-realistic layered architecture:
 *
 *   ┌─────────────────────────────────────────────────────────────────────┐
 *   │  REST Controller (@RestController)                                  │
 *   │     ↓  calls                                                        │
 *   │  Service Layer (@Service + @Transactional)                         │
 *   │     ↓  uses                                                         │
 *   │  Repository Layer (JpaRepository + custom extensions)              │
 *   │     ↓  manages                                                      │
 *   │  Entity Layer (@Entity + @MappedSuperclass)                        │
 *   │     ↓  persisted to                                                 │
 *   │  Database (H2 in-memory)                                           │
 *   └─────────────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART A — @NoRepositoryBean: CUSTOM BASE REPOSITORY (CONCEPTUAL REFERENCE)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║               @NoRepositoryBean — EXPLANATION & PATTERN                      ║
 * ║               (This is a reference pattern — not wired into the running app) ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @NoRepositoryBean
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT DOES:
 *   Marks a repository INTERFACE as a BASE/INTERMEDIATE interface.
 *   Spring Data will NOT create a bean instance for this interface itself
 *   (because it has no specific entity type bound to it).
 *
 * WHY USE IT?
 *   Define COMMON METHODS shared across ALL your repositories in one place.
 *   Pair it with a custom implementation class registered via:
 *
 *     @EnableJpaRepositories(
 *         basePackages     = "com.example",
 *         repositoryBaseClass = CustomBaseRepositoryImpl.class
 *     )
 *
 * EXAMPLE (reference pattern — not running in this app):
 *
 *   @NoRepositoryBean
 *   interface AuditableRepository<T, ID> extends JpaRepository<T, ID> {
 *       List<T> findByCreatedBy(String username);    // Common audit query
 *       List<T> findModifiedToday();                 // Common audit query
 *   }
 *
 *   // Specific repo inherits all common methods:
 *   interface ProductRepository extends AuditableRepository<Product, Long> {
 *       // product-specific methods here
 *   }
 *
 * NOTE: To keep this file self-contained and runnable, the actual BookRepository
 *       below extends JpaRepository directly and implements soft-delete via
 *       @Query methods — the most common real-world pattern.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
// @NoRepositoryBean interface shown above is for reference; see BookRepository below.


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART B — ENTITY: Book (uses soft-delete)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║    Book Entity — Demonstrates soft-delete pattern                            ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
@Entity
@Table(name = "tbl_books")

/*
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @SQLRestriction (formerly @Where in older Hibernate versions)
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT DOES:
 *   Adds a WHERE clause to EVERY query on this entity automatically.
 *   This is the CORE of the soft-delete pattern.
 *
 *   @SQLRestriction("deleted = false")
 *   → All SELECT queries for Book will automatically include:
 *       WHERE deleted = false
 *   → You NEVER accidentally retrieve deleted records.
 *   → Works for findAll(), findById(), findByTitle(), @Query — EVERYTHING.
 *
 * NOTE: Spring Boot 3.x / Hibernate 6.x:  Use @SQLRestriction
 *       Spring Boot 2.x / Hibernate 5.x:  Use @Where(clause = "deleted = false")
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@SQLRestriction("deleted = false")
class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 200)
    private String author;

    @Column(name = "isbn", unique = true, length = 20)
    private String isbn;

    @Column(precision = 8, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookCategory category;

    /*
     * The soft-delete flag.
     * When deleted = true, @SQLRestriction filters this record out of all queries.
     * The row still exists in the database — it's just "invisible" to JPA queries.
     */
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    // ── Constructors ──────────────────────────────────────────────────────────────
    protected Book() {}

    public Book(String title, String author, String isbn, BigDecimal price, BookCategory category) {
        this.title    = title;
        this.author   = author;
        this.isbn     = isbn;
        this.price    = price;
        this.category = category;
    }

    // ── Soft-delete support ───────────────────────────────────────────────────────
    public void markDeleted() {
        this.deleted   = true;
        this.deletedAt = java.time.LocalDateTime.now();
    }

    public void restore() {
        this.deleted   = false;
        this.deletedAt = null;
    }

    public Long          getId()        { return id; }
    public String        getTitle()     { return title; }
    public String        getAuthor()    { return author; }
    public String        getIsbn()      { return isbn; }
    public BigDecimal    getPrice()     { return price; }
    public BookCategory  getCategory()  { return category; }
    public boolean       isDeleted()    { return deleted; }
    public java.time.LocalDateTime getDeletedAt() { return deletedAt; }

    public void setTitle(String t)        { this.title = t; }
    public void setAuthor(String a)       { this.author = a; }
    public void setPrice(BigDecimal p)    { this.price = p; }
    public void setCategory(BookCategory c) { this.category = c; }
}

enum BookCategory { FICTION, NON_FICTION, SCIENCE, TECHNOLOGY, HISTORY, BIOGRAPHY }


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART D — @Repository: BookRepository
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║        @Repository — BookRepository with all patterns demonstrated           ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @Repository (on Spring Data interfaces)
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT DOES:
 *   Marks an interface as a Spring Data repository.
 *   Spring Data automatically creates an implementation at runtime.
 *
 * TWO USES OF @Repository:
 *
 *   USE 1: On a Spring Data interface (like this one)
 *     → Optional — Spring Data creates the bean even without @Repository
 *     → But adds semantic meaning: "this is a DAO/repository component"
 *
 *   USE 2: On a manually written DAO class
 *     → Required — marks it as a Spring component (like @Service, @Component)
 *     → ALSO enables Spring's exception translation:
 *       Converts JDBC/JPA exceptions to Spring's DataAccessException hierarchy
 *       Makes exception handling DB-vendor neutral
 *
 * EXCEPTION TRANSLATION (critical benefit of @Repository on manual DAOs):
 *   Without @Repository:  org.hibernate.exception.ConstraintViolationException
 *   With @Repository:     org.springframework.dao.DataIntegrityViolationException
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * SOFT-DELETE PATTERN (implemented here with @Query):
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @SQLRestriction("deleted = false") on the entity means ALL standard queries
 * (findAll, findById, findByX...) automatically exclude deleted records.
 *
 * We implement softDelete as a bulk @Modifying UPDATE and restore similarly.
 * This is the most common real-world approach — no custom base class needed.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Repository
interface BookRepository extends JpaRepository<Book, Long> {

    // ─── Derived Queries ──────────────────────────────────────────────────────────
    List<Book> findByAuthor(String author);
    List<Book> findByCategory(BookCategory category);
    List<Book> findByPriceLessThanEqual(BigDecimal maxPrice);
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);
    List<Book> findByTitleContainingIgnoreCase(String keyword);

    // ─── Sorted queries ───────────────────────────────────────────────────────────
    List<Book> findByCategoryOrderByPriceAsc(BookCategory category);
    List<Book> findTop5ByOrderByPriceDesc();

    // ─── @Query with JPQL ─────────────────────────────────────────────────────────
    @org.springframework.data.jpa.repository.Query("SELECT b FROM Book b WHERE b.price BETWEEN :min AND :max ORDER BY b.price ASC")
    List<Book> findByPriceRange(
        @Param("min") BigDecimal min,
        @Param("max") BigDecimal max
    );

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT b.author FROM Book b ORDER BY b.author")
    List<String> findAllAuthors();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(b) FROM Book b WHERE b.category = :category")
    Long countByCategory(@Param("category") BookCategory category);

    // ─── Soft-delete via @Modifying @Query ────────────────────────────────────────
    /*
     * WHY NOT USE repository.deleteById()?
     *   deleteById() fires a physical DELETE SQL statement.
     *   We want soft delete: SET deleted = true, not DELETE the row.
     *
     * @SQLRestriction("deleted = false") means findById() won't find deleted records.
     * So we use a native UPDATE query to flip the flag on a specific row.
     *
     * nativeQuery = true: used here because we need to UPDATE by id AND bypass
     * the @SQLRestriction filter (JPQL on a @SQLRestriction entity auto-adds the filter).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @org.springframework.data.jpa.repository.Query(
        value       = "UPDATE tbl_books SET deleted = true, deleted_at = NOW() WHERE id = :id",
        nativeQuery = true
    )
    void softDeleteById(@Param("id") Long id);

    /*
     * Restore: set deleted = false for a previously soft-deleted book.
     * Again uses native query to bypass @SQLRestriction filter.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @org.springframework.data.jpa.repository.Query(
        value       = "UPDATE tbl_books SET deleted = false, deleted_at = NULL WHERE id = :id",
        nativeQuery = true
    )
    void restoreById(@Param("id") Long id);

    // ─── @Modifying bulk update ───────────────────────────────────────────────────
    @Modifying(clearAutomatically = true)
    @Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE Book b SET b.price = b.price * :factor WHERE b.category = :category")
    int adjustPriceByCategory(
        @Param("category") BookCategory category,
        @Param("factor")   BigDecimal factor
    );
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART E — @Service: BookService (complete transactional service layer)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║        BookService — Complete Service Layer with @Transactional              ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 SERVICE LAYER BEST PRACTICES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. @Transactional at class level:
 *     All public methods are transactional by default.
 *     Saves you from adding @Transactional to each method manually.
 *
 *  2. @Transactional(readOnly = true) at class level + override for writes:
 *     Performance optimisation — read-only transactions are faster.
 *     Hibernate skips dirty checking for read-only transactions.
 *     Only write methods need @Transactional(readOnly = false).
 *
 *  3. Constructor injection (not @Autowired on field):
 *     - Makes dependencies explicit
 *     - Enables testing without Spring container
 *     - Prevents circular dependency issues at compile time
 *
 *  4. Return DTOs from service, not entities:
 *     - Prevents lazy loading issues outside transactions
 *     - Controls what data is exposed to the controller
 *     - Decouples the REST API from the DB schema
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Service
@Transactional(readOnly = true)   // Default: all methods are read-only transactions
class BookService {

    /*
     * Constructor injection — recommended over @Autowired field injection.
     *
     * @Autowired is optional here in Spring 4.3+ because there's only one constructor.
     * Spring automatically uses it for dependency injection.
     */
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // ─── READ OPERATIONS (inherits readOnly = true from class-level @Transactional) ──

    public List<Book> getAllBooks() {
        return bookRepository.findAll();    // @SQLRestriction auto-filters deleted=false
    }

    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id); // Returns empty if deleted
    }

    public Optional<Book> getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }

    public List<Book> getBooksByAuthor(String author) {
        return bookRepository.findByAuthor(author);
    }

    public List<Book> getBooksByCategory(BookCategory category) {
        return bookRepository.findByCategory(category);
    }

    public List<Book> searchBooks(String keyword) {
        return bookRepository.findByTitleContainingIgnoreCase(keyword);
    }

    public List<Book> getBooksInPriceRange(BigDecimal min, BigDecimal max) {
        return bookRepository.findByPriceRange(min, max);
    }

    public List<String> getAllAuthors() {
        return bookRepository.findAllAuthors();
    }

    public Long countByCategory(BookCategory category) {
        return bookRepository.countByCategory(category);
    }

    // ─── WRITE OPERATIONS (override to readOnly = false) ──────────────────────────

    @Transactional   // readOnly = false (default) — this overrides the class-level setting
    public Book createBook(String title, String author, String isbn,
                           BigDecimal price, BookCategory category) {
        // Validate ISBN uniqueness
        if (bookRepository.existsByIsbn(isbn)) {
            throw new IllegalArgumentException("A book with ISBN " + isbn + " already exists.");
        }

        Book book = new Book(title, author, isbn, price, category);
        return bookRepository.save(book);
        // save() calls: INSERT INTO tbl_books (...) VALUES (...)
        // Returns the saved entity with the generated ID populated
    }

    @Transactional
    public Book updateBook(Long id, String title, BigDecimal price) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Book not found: " + id));

        book.setTitle(title);
        book.setPrice(price);

        // No need to call save() here!
        // The entity is MANAGED — Hibernate tracks changes automatically.
        // At the end of the transaction, Hibernate detects the change ("dirty checking")
        // and generates an UPDATE SQL statement automatically.
        return book;
        // At this point, the @Transactional method returns and the transaction commits.
        // Hibernate flushes changes: UPDATE tbl_books SET title=?, price=? WHERE id=?
    }

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * Soft Delete — Physical row stays, flag set to true
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * Customer calls DELETE /books/5 → this method runs → book.deleted = true
     * Row stays in DB: id=5, deleted=true, deleted_at=<timestamp>
     * All future queries (findAll, findById, etc.) auto-filter it out via @SQLRestriction
     */
    @Transactional
    public void softDeleteBook(Long id) {
        // Verify exists first (findById respects @SQLRestriction — won't find already-deleted)
        bookRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Book not found: " + id));
        bookRepository.softDeleteById(id);
    }

    @Transactional
    public void restoreBook(Long id) {
        // restoreById uses native SQL to bypass @SQLRestriction filter
        bookRepository.restoreById(id);
    }

    @Transactional
    public int applyDiscountToCategory(BookCategory category, BigDecimal discountPercent) {
        // Convert discount percent to multiplier: 10% off → multiply by 0.90
        BigDecimal factor = BigDecimal.ONE.subtract(
            discountPercent.divide(new BigDecimal("100"))
        );
        return bookRepository.adjustPriceByCategory(category, factor);
    }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART F — REST CONTROLLER: Everything wired together
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║    BookController — Full REST API wiring Service + Repository + Entity       ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ENDPOINT REFERENCE:
 *
 *   GET    /api/books                     → All books (active only)
 *   GET    /api/books/{id}                → Single book by ID
 *   GET    /api/books/isbn/{isbn}         → Book by ISBN
 *   GET    /api/books/search?q=keyword    → Search by title keyword
 *   GET    /api/books/category/{category} → Books by category
 *   GET    /api/books/authors             → List of all unique authors
 *   POST   /api/books                     → Create a new book
 *   PUT    /api/books/{id}                → Update title and price
 *   DELETE /api/books/{id}                → Soft delete
 *   PUT    /api/books/{id}/restore        → Restore soft-deleted book
 *   POST   /api/books/discount/{category} → Apply discount to a category
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/books")
class BookController {

    private final BookService bookService;

    @Autowired
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // ─── READ Endpoints ───────────────────────────────────────────────────────────

    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id) {
        return bookService.getBookById(id)
            .orElseThrow(() -> new RuntimeException("Book not found: " + id));
    }

    @GetMapping("/isbn/{isbn}")
    public Book getByIsbn(@PathVariable String isbn) {
        return bookService.getBookByIsbn(isbn)
            .orElseThrow(() -> new RuntimeException("Book not found with ISBN: " + isbn));
    }

    @GetMapping("/search")
    public List<Book> search(@RequestParam String q) {
        return bookService.searchBooks(q);
    }

    @GetMapping("/category/{category}")
    public List<Book> getByCategory(@PathVariable BookCategory category) {
        return bookService.getBooksByCategory(category);
    }

    @GetMapping("/authors")
    public List<String> getAuthors() {
        return bookService.getAllAuthors();
    }

    // ─── WRITE Endpoints ──────────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Book createBook(@RequestBody CreateBookRequest request) {
        return bookService.createBook(
            request.title(),
            request.author(),
            request.isbn(),
            request.price(),
            request.category()
        );
    }

    @PutMapping("/{id}")
    public Book updateBook(@PathVariable Long id, @RequestBody UpdateBookRequest request) {
        return bookService.updateBook(id, request.title(), request.price());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable Long id) {
        bookService.softDeleteBook(id);
    }

    @PutMapping("/{id}/restore")
    public void restoreBook(@PathVariable Long id) {
        bookService.restoreBook(id);
    }

    @PostMapping("/discount/{category}")
    public String applyDiscount(
        @PathVariable BookCategory category,
        @RequestParam BigDecimal percent
    ) {
        int updated = bookService.applyDiscountToCategory(category, percent);
        return updated + " books in category " + category + " had a " + percent + "% discount applied.";
    }
}

// ─── Request DTOs (Java Records — immutable, concise) ────────────────────────────────
record CreateBookRequest(
    String      title,
    String      author,
    String      isbn,
    BigDecimal  price,
    BookCategory category
) {}

record UpdateBookRequest(
    String     title,
    BigDecimal price
) {}


/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📚 LEARNING SUMMARY 📚
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * ✅ WHAT WE LEARNED IN EXAMPLE 04:
 *
 *  CONCEPT                   RULE / KEY POINT
 *  ───────────────────────   ──────────────────────────────────────────────────────────
 *  @NoRepositoryBean         Marks intermediate/base repository interfaces so Spring Data
 *                            does NOT create a bean for them directly.
 *                            Use for shared methods across all repositories.
 *
 *  @Repository               On Spring Data interfaces: optional, adds semantic meaning.
 *                            On manual DAO classes: required, enables exception translation.
 *
 *  Custom Repository Impl    Extend SimpleJpaRepository + implement custom interface.
 *                            Spring Data wires the implementation automatically.
 *
 *  Soft Delete Pattern       @SQLRestriction("deleted = false") filters all queries.
 *                            Set deleted=true instead of physical delete for audit trail.
 *                            Restore by bypassing the filter with EntityManager directly.
 *
 *  @Service layer            @Transactional(readOnly=true) at class level for default.
 *                            Override with @Transactional on write methods.
 *                            No need to call save() after modifying managed entities!
 *                            Use constructor injection, not @Autowired on fields.
 *
 *  Dirty Checking            Modified MANAGED entities are auto-saved at transaction end.
 *                            No explicit save() call needed after getById() + modify.
 *
 *  Request DTOs (Records)    Use Java records for immutable request/response bodies.
 *                            Never expose entities directly in REST APIs.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🧪 TRY THESE ENDPOINTS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   curl -X POST http://localhost:8080/api/books \
 *        -H "Content-Type: application/json" \
 *        -d '{"title":"Clean Code","author":"Robert Martin","isbn":"9780132350884","price":35.99,"category":"TECHNOLOGY"}'
 *
 *   curl http://localhost:8080/api/books
 *
 *   curl http://localhost:8080/api/books/search?q=clean
 *
 *   curl -X DELETE http://localhost:8080/api/books/1
 *
 *   curl http://localhost:8080/api/books        # Deleted book no longer appears!
 *
 *   curl -X PUT http://localhost:8080/api/books/1/restore
 *
 *   curl http://localhost:8080/api/books        # Restored book appears again
 *
 *   curl http://localhost:8080/h2-console       # View all tables in H2 console
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ➡️  NEXT: HowItWorksExplained.java — Deep internal mechanics of JPA/Hibernate
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
class Example04RepositoryAnnotations {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║      CHAPTER 4 — EXAMPLE 04: Repository Annotations              ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  @Repository        → On interfaces (optional) or DAO classes (required)");
        System.out.println("  @NoRepositoryBean  → Base repository interface (no direct bean creation)");
        System.out.println("  SoftDeleteRepositoryImpl → Custom base implementation");
        System.out.println("  @SQLRestriction    → Automatic WHERE filter on all queries");
        System.out.println("  @Service + @Transactional(readOnly=true) → Service layer pattern");
        System.out.println("  Dirty Checking     → No save() needed for managed entities");
        System.out.println("  REST wiring        → Controller → Service → Repository → Entity");
        System.out.println();
        System.out.println("Start the app and try the endpoints listed in the summary above!");
        System.out.println("Also visit: http://localhost:8080/h2-console");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}








