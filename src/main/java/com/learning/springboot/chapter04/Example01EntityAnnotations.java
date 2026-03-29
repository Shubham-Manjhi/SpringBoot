package com.learning.springboot.chapter04;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║             EXAMPLE 01: ENTITY ANNOTATIONS IN ACTION                                 ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example01EntityAnnotations.java
 * Purpose:     Demonstrate @Entity, @Table, @Id, @GeneratedValue, @Column,
 *              @Transient, @Enumerated, @Temporal, @Lob, @Version
 * Difficulty:  ⭐⭐ Beginner–Intermediate
 * Time:        45–60 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * 🧩 WHAT WE ARE BUILDING:
 *
 *   A complete Product entity that demonstrates EVERY core entity annotation.
 *   You'll see how each annotation translates into database table structure.
 *
 *   JAVA CLASS              →   DATABASE TABLE (tbl_products)
 *   ─────────────────────       ──────────────────────────────────────────────
 *   Product class         →    tbl_products table
 *   id field              →    product_id BIGINT PRIMARY KEY AUTO_INCREMENT
 *   name field            →    product_name VARCHAR(200) NOT NULL UNIQUE
 *   description field     →    description TEXT
 *   price field           →    price DECIMAL(10,2) NOT NULL
 *   stockQuantity field   →    stock_quantity INT DEFAULT 0
 *   status field          →    status VARCHAR(20) NOT NULL  (enum as string)
 *   createdAt field       →    created_at TIMESTAMP
 *   expiryDate field      →    expiry_date DATE
 *   version field         →    version BIGINT  (optimistic locking)
 *   discountedPrice field →    [NO COLUMN — @Transient]
 *   sku field             →    sku VARCHAR(50) NOT NULL UNIQUE
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ──────────────────────────────────────────────────────────────────────────────────────
//  BASE ENTITY  –  A reusable superclass providing id, createdAt, updatedAt
// ──────────────────────────────────────────────────────────────────────────────────────

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║               @MappedSuperclass — SHARED AUDIT FIELDS                        ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @MappedSuperclass
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT DOES:
 *   Declares that this class provides MAPPED FIELDS to its subclasses.
 *   This class itself is NOT an entity — Hibernate creates NO table for it.
 *   All subclasses inherit the fields and they appear in EACH child's table.
 *
 * WHY USE IT?
 *   Every entity in our system needs: id, createdAt, updatedAt.
 *   Without @MappedSuperclass we'd copy-paste these three fields into every entity.
 *   With it, we define once and inherit everywhere.
 *
 * COMPARISON:
 *   @MappedSuperclass   → No table; fields go into each child's table
 *   @Inheritance        → Parent entity HAS a table; used for polymorphism
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@MappedSuperclass
abstract class BaseEntity {

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Id
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Marks this field as the PRIMARY KEY of the entity's table.
     *   Every JPA entity MUST have exactly one field annotated with @Id.
     *
     * SUPPORTED TYPES:
     *   Numeric : Long (recommended), Integer, Short
     *   String  : String (UUID stored as varchar)
     *   UUID    : java.util.UUID (modern choice)
     *
     * RULE: Use Long (wrapper) not long (primitive) to allow null before persisting.
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * 📌 @GeneratedValue(strategy = GenerationType.IDENTITY)
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Tells JPA to let the database auto-generate the primary key value.
     *   IDENTITY strategy means: use the database's AUTO_INCREMENT (MySQL, H2)
     *   or SERIAL (PostgreSQL) mechanism.
     *
     * HOW IT WORKS:
     *   1. You call repository.save(entity) without setting id
     *   2. Hibernate executes: INSERT INTO tbl_products (...) VALUES (...)
     *   3. The database assigns the next auto-increment value
     *   4. Hibernate retrieves the generated id using JDBC's getGeneratedKeys()
     *   5. The id field on your Java object is now populated
     *
     * STRATEGIES COMPARISON:
     *   IDENTITY    → DB auto_increment. Simple. Can't batch INSERTs efficiently.
     *   SEQUENCE    → DB sequence object. Efficient (pre-allocates IDs). Best for PostgreSQL/Oracle.
     *   TABLE       → Special DB table for ID tracking. Slow. Portable. Rarely used.
     *   AUTO        → JPA picks based on DB dialect. Default.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Column(updatable = false)
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   updatable = false → This column is included in INSERT but NOT in UPDATE SQL.
     *   Once set on creation, createdAt can NEVER be changed by JPA.
     *
     * NOTE: LocalDateTime is a modern Java type. JPA maps it to TIMESTAMP automatically.
     *       No @Temporal annotation needed (that's only for legacy java.util.Date).
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @PrePersist  —  Entity Lifecycle Callback
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   This method is automatically called by JPA BEFORE the entity is inserted
     *   (persisted) into the database for the first time.
     *
     * USE CASES:
     *   → Set createdAt timestamp automatically (no need to do it manually)
     *   → Generate default values
     *   → Validate state before save
     *
     * HOW IT WORKS:
     *   1. You call entityManager.persist(entity) or repository.save(entity)
     *   2. JPA calls this @PrePersist method automatically BEFORE executing INSERT SQL
     *   3. createdAt and updatedAt are set to the current time
     *   4. INSERT SQL is then executed with these values
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @PreUpdate  —  Entity Lifecycle Callback
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Called by JPA BEFORE the entity is updated in the database.
     *   Perfect for auto-updating the "last modified" timestamp.
     *
     * HOW IT WORKS:
     *   1. You modify a managed entity's field (e.g., product.setPrice(newPrice))
     *   2. At transaction commit (or flush), JPA detects the change ("dirty checking")
     *   3. @PreUpdate method is called automatically
     *   4. updatedAt is set to now
     *   5. UPDATE SQL is executed
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Standard getters
    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}


// ──────────────────────────────────────────────────────────────────────────────────────
//  STATUS ENUM  –  Demonstrates @Enumerated
// ──────────────────────────────────────────────────────────────────────────────────────

/**
 * Java enum representing the possible states of a product.
 * Stored in the DB using @Enumerated(EnumType.STRING) — see below.
 */
enum ProductStatus {
    ACTIVE,       // Product is available for sale
    INACTIVE,     // Product is temporarily unavailable
    DISCONTINUED, // Product is permanently removed
    OUT_OF_STOCK  // Product is temporarily out of stock
}


// ──────────────────────────────────────────────────────────────────────────────────────
//  MAIN ENTITY  –  Product
// ──────────────────────────────────────────────────────────────────────────────────────

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                     PRODUCT ENTITY — ALL ENTITY ANNOTATIONS                  ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */

/*
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @Entity
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT DOES:
 *   Declares that this class is a JPA ENTITY — it will be mapped to a database table.
 *   Without this annotation, JPA completely ignores the class.
 *
 * RULES:
 *   1. The class must NOT be final (Hibernate creates proxies by extending the class)
 *   2. The class must have a no-argument constructor (public or protected)
 *   3. The class must have exactly one @Id field (or @EmbeddedId for composite keys)
 *   4. Persistent fields should NOT be final
 *
 * OPTIONAL: @Entity(name = "prod")
 *   Sets the JPQL entity name (used in JPQL queries like "SELECT p FROM prod p").
 *   Default is the simple class name ("Product").
 *   This is NOT the same as the database table name!
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * 📌 @Table
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT DOES:
 *   Customises the database TABLE that this entity maps to.
 *   Without @Table, the table name defaults to the class name (Product → PRODUCT).
 *
 * KEY ATTRIBUTES:
 *
 *   name = "tbl_products"
 *     → The actual table name in the database.
 *       Best practice: Use a consistent naming convention (prefix, snake_case).
 *
 *   uniqueConstraints = @UniqueConstraint(...)
 *     → Adds a UNIQUE constraint at the TABLE level (not just column level).
 *       Use this for multi-column unique constraints.
 *       Example: name = "uc_product_sku" ensures SKU is globally unique.
 *
 *   indexes = @Index(...)
 *     → Adds database indexes for faster queries.
 *       columnList can be a comma-separated list for composite indexes.
 *
 *   schema = "inventory"
 *     → Optional: places the table in a specific DB schema.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Entity
@Table(
    name = "tbl_products",
    uniqueConstraints = {
        @UniqueConstraint(name = "uc_product_sku",  columnNames = {"sku"}),
        @UniqueConstraint(name = "uc_product_name", columnNames = {"product_name"})
    },
    indexes = {
        @Index(name = "idx_product_status", columnList = "status"),
        @Index(name = "idx_product_price",  columnList = "price")
    }
)
class Product extends BaseEntity {

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Column — Full Example with All Key Attributes
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Maps this Java field to a specific database column with custom constraints.
     *
     * ATTRIBUTES EXPLAINED:
     *
     *   name = "product_name"
     *     → DB column name. If omitted, Hibernate uses the field name (camelCase
     *       converted to snake_case by default naming strategy in Spring Boot).
     *
     *   nullable = false
     *     → Adds NOT NULL constraint to this column.
     *       If you try to save a Product with name = null, DB throws an error.
     *       NOTE: For application-level validation, use @NotNull (JSR-303) too.
     *
     *   length = 200
     *     → For String fields, defines VARCHAR(200). Default is VARCHAR(255).
     *       Only applies to String type; ignored for numeric types.
     *
     *   unique = true
     *     → Adds a single-column UNIQUE constraint.
     *       For multi-column unique constraints, use @Table(uniqueConstraints = ...).
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Column(name = "product_name", nullable = false, length = 200, unique = true)
    private String name;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Column with columnDefinition — Custom SQL Type
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * columnDefinition = "TEXT"
     *   → Overrides the generated column type with a raw SQL type definition.
     *   → TEXT stores unlimited text (as opposed to VARCHAR which has a length limit).
     *   → Use with caution: makes your schema DB-vendor specific.
     *
     * WHEN TO USE:
     *   → For long text fields (descriptions, content, notes)
     *   → For special DB types (JSONB in PostgreSQL, MEDIUMTEXT in MySQL)
     *   → When you need exact control over the SQL DDL
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Column with precision & scale — Decimal Numbers
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * precision = 10  → Total number of significant digits (e.g., 12345678.99 = 10 digits)
     * scale = 2       → Digits after the decimal point (2 decimal places: cents)
     *
     * This creates: price DECIMAL(10, 2) NOT NULL
     *
     * ALWAYS use BigDecimal for money — never float or double!
     *   float/double have IEEE 754 rounding errors that cause incorrect cent values.
     *   BigDecimal provides exact arithmetic.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Column with insertable / updatable
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * insertable = true  (default) → field IS included in INSERT SQL
     * updatable  = true  (default) → field IS included in UPDATE SQL
     *
     * Setting insertable = false:  Column is never written by JPA on insert
     * Setting updatable  = false:  Column is written once (insert) then never changed
     * Setting both false:          Read-only column — value comes from DB trigger/default
     *
     * Use case here: stockQuantity has a DB DEFAULT of 0.
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Column(name = "stock_quantity", columnDefinition = "INT DEFAULT 0")
    private Integer stockQuantity = 0;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Column for SKU
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Column(name = "sku", nullable = false, length = 50)
    private String sku;  // Stock Keeping Unit — unique product code

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Enumerated(EnumType.STRING)
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Maps a Java enum field to a database column.
     *
     * ENUM TYPE — ALWAYS USE STRING:
     *
     *   EnumType.ORDINAL  (BAD ❌):
     *     Stores the INDEX (0, 1, 2...) of the enum constant.
     *     ACTIVE=0, INACTIVE=1, DISCONTINUED=2, OUT_OF_STOCK=3
     *     PROBLEM: If you INSERT a new enum constant between existing ones
     *              (e.g., add PENDING between ACTIVE and INACTIVE),
     *              ALL existing data becomes WRONG without a DB migration!
     *
     *   EnumType.STRING  (GOOD ✅):
     *     Stores the NAME ("ACTIVE", "INACTIVE") of the enum constant.
     *     Safe to add, rename, reorder enum constants.
     *     Human-readable in the database.
     *     Costs slightly more storage (string vs integer), but worth it.
     *
     * DB column: status VARCHAR(20) NOT NULL
     * Stored value: "ACTIVE", "INACTIVE", "DISCONTINUED", "OUT_OF_STOCK"
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status = ProductStatus.ACTIVE;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 LocalDate — Date Without Time
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * java.time.LocalDate maps to SQL DATE (YYYY-MM-DD) automatically.
     * No @Temporal annotation needed (that's only for legacy java.util.Date).
     *
     * LocalDate    → DATE column (year, month, day only)
     * LocalDateTime → TIMESTAMP column (date + time)
     * LocalTime    → TIME column
     * ZonedDateTime → TIMESTAMP WITH TIMEZONE
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Version — Optimistic Locking
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Enables OPTIMISTIC LOCKING — a concurrency control mechanism.
     *
     * THE PROBLEM (without optimistic locking):
     *   User A reads product (price = 100, version = 1)
     *   User B reads product (price = 100, version = 1)
     *   User A updates price to 150, saves → price = 150, version = 2
     *   User B updates price to 120, saves → OVERWRITES User A's change! (LOST UPDATE)
     *
     * THE SOLUTION (with @Version):
     *   User A reads product (price = 100, version = 1)
     *   User B reads product (price = 100, version = 1)
     *   User A updates price to 150:
     *     → UPDATE tbl_products SET price=150, version=2 WHERE id=1 AND version=1
     *     → Succeeds. Row updated, version is now 2.
     *   User B updates price to 120:
     *     → UPDATE tbl_products SET price=120, version=2 WHERE id=1 AND version=1
     *     → FAILS! No row matches (version is now 2, not 1)
     *     → Hibernate throws OptimisticLockException
     *     → You can catch this and retry or show an error to User B
     *
     * HOW HIBERNATE USES IT:
     *   On every UPDATE: Hibernate includes "AND version = currentVersion" in WHERE clause
     *   If 0 rows updated → version was changed by someone else → throw exception
     *   If update succeeds → Hibernate increments the version by 1
     *
     * SUPPORTED TYPES: Long, Integer, Short, Timestamp
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Version
    private Long version;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Transient — NOT stored in the database
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Marks this field to be COMPLETELY IGNORED by JPA.
     *   No column is created for this field. It is never read from or written to the DB.
     *
     * USE CASES:
     *   → Computed / derived values  (discounted price = price * 0.9)
     *   → Temporary session flags    (isCurrentUserFavorite)
     *   → Cached computed data       (formattedPrice = "$" + price.toPlainString())
     *
     * IMPORTANT DISTINCTION:
     *   @Transient (JPA) → not stored in database
     *   transient  (Java keyword) → not included in Java serialization
     *   You can use BOTH on the same field if needed.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Transient
    private BigDecimal discountedPrice;  // Computed at runtime, never stored

    @Transient
    private boolean isOnSale;            // Flag for current session only

    // ──────────────────────────────────────────────────────────────────────────────
    //  CONSTRUCTORS
    // ──────────────────────────────────────────────────────────────────────────────

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * ⚠️ REQUIRED: No-argument constructor
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * JPA REQUIRES every entity to have a no-arg constructor.
     * Why? Hibernate needs to instantiate the entity via reflection when loading
     * from the database (e.g., during a SELECT query), and reflection uses
     * the no-arg constructor.
     *
     * Access level: public OR protected (protected is preferred — prevents
     * accidental direct instantiation).
     * ─────────────────────────────────────────────────────────────────────────────
     */
    protected Product() {
        // Required by JPA — do NOT remove
    }

    /**
     * Business constructor — use this to create new products in application code.
     */
    public Product(String name, String sku, BigDecimal price) {
        this.name   = name;
        this.sku    = sku;
        this.price  = price;
        this.status = ProductStatus.ACTIVE;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    //  LIFECYCLE CALLBACKS (specific to this entity)
    // ──────────────────────────────────────────────────────────────────────────────

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @PostLoad — Called after entity is loaded from DB
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Called AFTER a SELECT query loads this entity from the database.
     *   Perfect for computing derived / transient fields from persisted data.
     *
     * HOW IT WORKS:
     *   1. repository.findById(1L) is called
     *   2. Hibernate executes SELECT SQL
     *   3. Hibernate maps the ResultSet to a Product object
     *   4. @PostLoad method is called automatically
     *   5. discountedPrice is computed from the loaded price
     *   6. The Product object (with discountedPrice set) is returned to the caller
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @PostLoad
    private void computeTransientFields() {
        // Apply a 10% discount for display purposes
        if (this.price != null) {
            this.discountedPrice = this.price.multiply(new BigDecimal("0.90"));
        }
        // Flag as on-sale if below threshold
        this.isOnSale = (this.price != null && this.price.compareTo(new BigDecimal("50.00")) < 0);
    }

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @PreRemove — Called before entity is deleted
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Called BEFORE the entity is deleted from the database.
     *   Use for validation or cleanup before deletion.
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @PreRemove
    private void beforeDelete() {
        System.out.println("⚠️  About to delete product: [" + this.sku + "] " + this.name);
        // In a real app: notify stakeholders, archive data, etc.
    }

    // ──────────────────────────────────────────────────────────────────────────────
    //  GETTERS & SETTERS
    // ──────────────────────────────────────────────────────────────────────────────
    public String  getName()             { return name; }
    public void    setName(String name)  { this.name = name; }

    public String  getDescription()                  { return description; }
    public void    setDescription(String description){ this.description = description; }

    public BigDecimal getPrice()               { return price; }
    public void       setPrice(BigDecimal price){ this.price = price; }

    public Integer getStockQuantity()                    { return stockQuantity; }
    public void    setStockQuantity(Integer stockQuantity){ this.stockQuantity = stockQuantity; }

    public String getSku()           { return sku; }
    public void   setSku(String sku) { this.sku = sku; }

    public ProductStatus getStatus()               { return status; }
    public void          setStatus(ProductStatus s){ this.status = s; }

    public LocalDate getExpiryDate()                 { return expiryDate; }
    public void      setExpiryDate(LocalDate d)      { this.expiryDate = d; }

    public Long getVersion() { return version; }

    // Transient fields — read-only, computed by @PostLoad
    public BigDecimal getDiscountedPrice() { return discountedPrice; }
    public boolean    isOnSale()           { return isOnSale; }

    @Override
    public String toString() {
        return "Product{" +
               "id="               + getId() +
               ", sku='"           + sku + '\'' +
               ", name='"          + name + '\'' +
               ", price="          + price +
               ", discountedPrice=" + discountedPrice +
               ", status="         + status +
               ", stockQuantity="  + stockQuantity +
               ", version="        + version +
               ", createdAt="      + getCreatedAt() +
               '}';
    }
}


// ──────────────────────────────────────────────────────────────────────────────────────
//  DEMONSTRATION OF ALL GENERATEDVALUE STRATEGIES
// ──────────────────────────────────────────────────────────────────────────────────────

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║              @GeneratedValue STRATEGIES — SIDE-BY-SIDE COMPARISON            ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */

/*
 * STRATEGY 1: IDENTITY — DB auto_increment
 *
 * Best for: MySQL, H2, SQL Server
 * Note: Cannot batch INSERT statements efficiently because Hibernate needs to
 *       execute each INSERT separately to get the generated ID back from the DB.
 */
@Entity
@Table(name = "demo_identity")
class EntityWithIdentityKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    protected EntityWithIdentityKey() {}
    public EntityWithIdentityKey(String name) { this.name = name; }
    public Long getId() { return id; }
    public String getName() { return name; }
}

/*
 * STRATEGY 2: SEQUENCE — DB sequence object (most efficient)
 *
 * Best for: PostgreSQL, Oracle, H2
 * Note: allocationSize = 1 means fetch IDs one by one.
 *       allocationSize = 50 (default) means fetch 50 IDs at once from the sequence
 *       and cache them in-memory — much faster for bulk inserts.
 *       BUT: if the app crashes, those 50 IDs are lost (gaps in sequence).
 *       For most apps, gaps in IDs are perfectly fine and acceptable.
 */
@Entity
@Table(name = "demo_sequence")
@SequenceGenerator(
    name           = "product_seq_gen",  // Logical name used below in @GeneratedValue
    sequenceName   = "seq_product_id",   // Actual sequence object name in the DB
    initialValue   = 1,                  // First ID value
    allocationSize = 1                   // How many IDs to fetch from DB at once
)
class EntityWithSequenceKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq_gen")
    private Long id;

    @Column
    private String name;

    protected EntityWithSequenceKey() {}
    public EntityWithSequenceKey(String name) { this.name = name; }
    public Long getId() { return id; }
    public String getName() { return name; }
}

/*
 * STRATEGY 3: UUID — Application-generated unique identifier
 *
 * Best for: Distributed systems, microservices, public-facing APIs
 * Note: UUIDs are globally unique and don't need a DB round-trip to generate.
 *       They're larger (36 chars) than Long, which impacts index size.
 *       UUID primary keys make it IMPOSSIBLE to enumerate resources by ID.
 *
 * In JPA 3.1 (Jakarta EE 10 / Spring Boot 3.x), UuidGenerator is built-in:
 */
@Entity
@Table(name = "demo_uuid")
class EntityWithUuidKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column
    private String name;

    protected EntityWithUuidKey() {}
    public EntityWithUuidKey(String name) { this.name = name; }
    public String getId() { return id; }
    public String getName() { return name; }
}


// ──────────────────────────────────────────────────────────────────────────────────────
//  DEMONSTRATION RUNNER
// ──────────────────────────────────────────────────────────────────────────────────────

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📚 LEARNING SUMMARY 📚
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * ✅ WHAT WE LEARNED IN EXAMPLE 01:
 *
 *  ANNOTATION              PURPOSE                                       KEY RULE
 *  ───────────────────     ────────────────────────────────────────      ──────────────────────────────────
 *  @MappedSuperclass       Shared fields without a table                 Use for audit base class (id/timestamps)
 *  @Entity                 Maps class to DB table                        Class must NOT be final; needs no-arg constructor
 *  @Table                  Customise table name/constraints/indexes      Use @UniqueConstraint for multi-column uniqueness
 *  @Id                     Marks primary key field                       Every entity MUST have one; use Long (wrapper)
 *  @GeneratedValue         Auto-generate primary key                     IDENTITY for MySQL/H2; SEQUENCE for PostgreSQL
 *  @Column                 Customise column name/nullable/length         nullable=false adds NOT NULL constraint
 *  @Enumerated             Map Java enum to column                       ALWAYS use EnumType.STRING, never ORDINAL
 *  @Transient              Exclude field from persistence                For computed/temporary fields
 *  @Version                Optimistic locking (concurrency control)      Add to every high-concurrency entity
 *  @PrePersist             Auto-set createdAt before INSERT              Called automatically by JPA — no manual call needed
 *  @PreUpdate              Auto-set updatedAt before UPDATE              Called automatically by JPA — no manual call needed
 *  @PostLoad               Compute transient fields after SELECT         Called automatically after every load from DB
 *  @PreRemove              Validate / log before DELETE                  Called automatically before delete
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🧪 EXERCISES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. Add a Category entity. Add a @Column with EnumType.STRING for category type.
 *  2. Try saving a Product without a name and observe the constraint violation.
 *  3. Add @Version to Product and write a test that triggers OptimisticLockException.
 *  4. Add a uuid field using GenerationType.UUID and check what Hibernate generates.
 *  5. Extend BaseEntity in a new entity and verify createdAt is set automatically.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ➡️  NEXT: Example02RelationshipAnnotations.java
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
class Example01EntityAnnotations {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║          CHAPTER 4 — EXAMPLE 01: Entity Annotations              ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("This example covers:");
        System.out.println("  @MappedSuperclass  →  BaseEntity (shared id, createdAt, updatedAt)");
        System.out.println("  @Entity            →  Product class maps to tbl_products table");
        System.out.println("  @Table             →  Table name, unique constraints, indexes");
        System.out.println("  @Id                →  Primary key declaration");
        System.out.println("  @GeneratedValue    →  IDENTITY, SEQUENCE, UUID strategies");
        System.out.println("  @Column            →  Name, nullable, length, precision, scale");
        System.out.println("  @Enumerated        →  STRING (recommended) vs ORDINAL (avoid)");
        System.out.println("  @Transient         →  Exclude from persistence");
        System.out.println("  @Version           →  Optimistic locking");
        System.out.println("  @PrePersist        →  Auto-set createdAt before INSERT");
        System.out.println("  @PreUpdate         →  Auto-set updatedAt before UPDATE");
        System.out.println("  @PostLoad          →  Compute derived fields after SELECT");
        System.out.println("  @PreRemove         →  Log/validate before DELETE");
        System.out.println();
        System.out.println("See Chapter04Overview for @GeneratedValue strategy comparison.");
        System.out.println("Run the Spring Boot app and check /h2-console to see the tables!");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

