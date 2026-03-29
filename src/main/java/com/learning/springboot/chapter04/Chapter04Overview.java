package com.learning.springboot.chapter04;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║                    SPRING BOOT ANNOTATIONS - COMPREHENSIVE GUIDE                     ║
 * ║                      Chapter 4: Spring Data JPA Annotations                          ║
 * ║                                                                                       ║
 * ║                           📚 From Zero to Expert 📚                                  ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Chapter:      4
 * Title:        Spring Data JPA Annotations
 * Difficulty:   ⭐⭐⭐ Intermediate
 * Estimated:    6–10 hours
 * Prerequisites: Chapter 1 (Core), Chapter 2 (Spring Core), Basic SQL, OOP concepts
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │                                                                                       │
 * │                    CHAPTER 4: OVERVIEW & LEARNING GOALS                              │
 * │                                                                                       │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📖 TABLE OF CONTENTS 📖
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Section 1  :  Chapter Introduction & Overview
 * Section 2  :  What is JPA? The Big Picture
 * Section 3  :  Technology Stack — JPA, Hibernate, Spring Data
 * Section 4  :  Entity Annotations   (@Entity, @Table, @Id, @GeneratedValue,
 *                                     @Column, @Transient, @Temporal, @Enumerated)
 * Section 5  :  Relationship Annotations (@OneToOne, @OneToMany, @ManyToOne,
 *                                          @ManyToMany, @JoinColumn, @JoinTable)
 * Section 6  :  Query Annotations    (@Query, @NamedQuery, @Modifying, @Param,
 *                                     @NamedNativeQuery)
 * Section 7  :  Repository Annotations (@Repository, @NoRepositoryBean)
 * Section 8  :  Entity Lifecycle Hooks (@PrePersist, @PostPersist, @PreUpdate,
 *                                        @PostUpdate, @PreRemove, @PostRemove, @PostLoad)
 * Section 9  :  Embeddable Annotations (@Embeddable, @Embedded, @EmbeddedId)
 * Section 10 :  Inheritance Annotations (@Inheritance, @DiscriminatorColumn,
 *                                         @DiscriminatorValue, @MappedSuperclass)
 * Section 11 :  How Everything Works Together — Internal Mechanics
 * Section 12 :  Best Practices & Common Pitfalls
 * Section 13 :  Interview Questions & Answers
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * 📂 FILES IN THIS CHAPTER:
 *
 *  • Chapter04Overview.java          ← YOU ARE HERE (Big picture & concepts)
 *  • Example01EntityAnnotations.java      (Entity, Column, Id, GeneratedValue, Transient...)
 *  • Example02RelationshipAnnotations.java (OneToOne, OneToMany, ManyToOne, ManyToMany...)
 *  • Example03QueryAnnotations.java       (Query, Modifying, NamedQuery, Param...)
 *  • Example04RepositoryAnnotations.java  (Repository patterns, custom repos, projections)
 *  • HowItWorksExplained.java             (Internal JPA/Hibernate mechanics deep dive)
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
public class Chapter04Overview {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                    SECTION 1: CHAPTER INTRODUCTION                           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 LEARNING OBJECTIVES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * By the end of this chapter, you will be able to:
     *
     *  ✓  Understand what JPA is and why it exists
     *  ✓  Map Java classes to database tables using @Entity, @Table, @Column
     *  ✓  Define primary keys with @Id and @GeneratedValue strategies
     *  ✓  Model complex relationships: @OneToOne, @OneToMany, @ManyToMany
     *  ✓  Write custom queries using @Query, @NamedQuery, @Modifying
     *  ✓  Build powerful repositories with Spring Data JPA
     *  ✓  Use entity lifecycle callbacks (@PrePersist, @PostLoad, etc.)
     *  ✓  Model inheritance hierarchies in the database
     *  ✓  Avoid N+1 query problems and other classic JPA pitfalls
     *  ✓  Answer tough JPA interview questions with confidence
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🌟 WHY SPRING DATA JPA?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Without Spring Data JPA (raw JDBC approach):
     *
     *     // BEFORE — 20+ lines just to fetch one user
     *     Connection conn = DriverManager.getConnection(url, user, pass);
     *     PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id=?");
     *     ps.setLong(1, userId);
     *     ResultSet rs = ps.executeQuery();
     *     User user = null;
     *     if (rs.next()) {
     *         user = new User();
     *         user.setId(rs.getLong("id"));
     *         user.setName(rs.getString("name"));
     *         user.setEmail(rs.getString("email"));
     *         // ... more fields ...
     *     }
     *     rs.close(); ps.close(); conn.close();
     *
     * With Spring Data JPA (modern approach):
     *
     *     // AFTER — 1 line to fetch one user
     *     Optional<User> user = userRepository.findById(userId);
     *
     *     // That's it! Spring Data JPA handles everything else:
     *     //  ✓  Connection management
     *     //  ✓  SQL generation
     *     //  ✓  Object mapping
     *     //  ✓  Transaction handling
     *     //  ✓  Exception translation
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📦 REQUIRED DEPENDENCY:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * In build.gradle:
     *
     *     implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
     *     runtimeOnly    'com.h2database:h2'   // or your preferred database
     *
     * This single starter brings in:
     *  •  spring-data-jpa       (Spring Data repository abstraction)
     *  •  hibernate-core        (JPA implementation / ORM engine)
     *  •  jakarta.persistence   (JPA specification / annotations)
     *  •  spring-orm            (Spring ORM integration)
     *  •  spring-tx             (Transaction management)
     *  •  HikariCP              (Connection pooling)
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                SECTION 2: WHAT IS JPA? THE BIG PICTURE                       ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔍 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * JPA = Jakarta Persistence API  (formerly Java Persistence API)
     *
     * JPA is a SPECIFICATION (not an implementation) that defines:
     *  •  How Java objects map to database tables  → OBJECT-RELATIONAL MAPPING (ORM)
     *  •  How to perform CRUD operations           → EntityManager API
     *  •  How to write queries                     → JPQL (Java Persistence Query Language)
     *  •  How to manage transactions               → @Transactional
     *
     * THINK OF IT LIKE THIS:
     *
     *   JPA Specification   ≈   JDBC Specification
     *   Hibernate           ≈   MySQL Driver
     *   Spring Data JPA     ≈   Connection Pool + Helper Layer
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🏗️ THE OBJECT-RELATIONAL MISMATCH PROBLEM:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * The core problem JPA solves is the "impedance mismatch":
     *
     *   JAVA WORLD                    DATABASE WORLD
     *   ──────────────────────────    ──────────────────────────
     *   Class                    ↔   Table
     *   Object instance          ↔   Row
     *   Field / Property         ↔   Column
     *   Object reference         ↔   Foreign key
     *   Collection / List        ↔   Join table
     *   Inheritance hierarchy    ↔   Multiple tables / discriminator column
     *   null                     ↔   NULL value
     *
     * JPA bridges this gap using ANNOTATIONS to declare the mapping rules.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔄 ENTITY LIFECYCLE STATES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ┌──────────────────────────────────────────────────────────────────────────┐
     *  │                         ENTITY LIFECYCLE                                 │
     *  │                                                                           │
     *  │   new User()          em.persist()        tx.commit() / em.flush()       │
     *  │   ──────────         ──────────────       ──────────────────────────     │
     *  │   TRANSIENT   ──────→   MANAGED    ──────→      DETACHED / DB            │
     *  │   (no ID,              (tracked by          (outside persistence          │
     *  │   not tracked)          EntityManager)        context / saved to DB)      │
     *  │       ↑                    │                        │                    │
     *  │       │                    │ em.remove()            │ em.merge()         │
     *  │       │                    ↓                        ↓                    │
     *  │       └──────────────   REMOVED    ←─────────    DETACHED               │
     *  │                         (will be deleted)        (reattached)            │
     *  └──────────────────────────────────────────────────────────────────────────┘
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║          SECTION 3: TECHNOLOGY STACK — JPA, HIBERNATE, SPRING DATA           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🏛️ LAYERED ARCHITECTURE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ┌────────────────────────────────────────────────────────────┐
     *  │              YOUR APPLICATION CODE                         │
     *  │  (uses repositories, calls findById, save, delete, etc.)  │
     *  ├────────────────────────────────────────────────────────────┤
     *  │              SPRING DATA JPA                               │
     *  │  (generates repository implementations, handles queries)   │
     *  ├────────────────────────────────────────────────────────────┤
     *  │              JPA (Jakarta Persistence API)                 │
     *  │  (specification: defines @Entity, @Query, EntityManager)   │
     *  ├────────────────────────────────────────────────────────────┤
     *  │              HIBERNATE (JPA Implementation)                │
     *  │  (generates SQL, manages sessions, caching, mapping)       │
     *  ├────────────────────────────────────────────────────────────┤
     *  │              JDBC (Java Database Connectivity)             │
     *  │  (sends actual SQL to the database driver)                 │
     *  ├────────────────────────────────────────────────────────────┤
     *  │              DATABASE (H2, MySQL, PostgreSQL, etc.)        │
     *  │  (executes SQL, stores and retrieves data)                 │
     *  └────────────────────────────────────────────────────────────┘
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎭 WHO DOES WHAT?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ROLE              TOOL              RESPONSIBILITY
     *  ──────────────    ───────────────   ────────────────────────────────────────────
     *  Specification     JPA               Defines @Entity, @Id, @Query, EntityManager
     *  Implementation    Hibernate         Generates SQL, session management, caching
     *  Abstraction       Spring Data JPA   CrudRepository, JpaRepository, query methods
     *  Container         Spring Boot       Auto-configures everything automatically
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 ANNOTATION NAMESPACES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PACKAGE                       ANNOTATIONS COME FROM
     *  ────────────────────────────  ──────────────────────────────────────
     *  jakarta.persistence.*         JPA Specification (e.g., @Entity, @Id)
     *  org.springframework.data.*    Spring Data (e.g., @Query, @Param)
     *  org.springframework.stereotype.*  Spring (e.g., @Repository)
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 4: ENTITY ANNOTATIONS — QUICK REFERENCE                         ║
     * ║     (Full examples in Example01EntityAnnotations.java)                       ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Entity
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Marks a POJO class as a JPA entity (maps to a database table)
     *  Package: jakarta.persistence.Entity
     *  Required: YES — without this, JPA ignores the class completely
     *  Rule:    Class must have a no-arg constructor (public or protected)
     *  Rule:    Class must have at least one field annotated @Id
     *
     *  Syntax:
     *      @Entity
     *      public class Product { ... }
     *
     *      @Entity(name = "prod")   // optional: JPQL entity name (not table name)
     *      public class Product { ... }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Table
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Customises the mapped table name, schema, unique constraints, indexes
     *  Package: jakarta.persistence.Table
     *  Required: NO — without it, table name = class name (by default)
     *
     *  Key attributes:
     *    name            → table name in the database
     *    schema          → database schema (e.g., "public" in PostgreSQL)
     *    catalog         → database catalog
     *    uniqueConstraints → array of @UniqueConstraint
     *    indexes         → array of @Index
     *
     *  Syntax:
     *      @Entity
     *      @Table(
     *          name = "tbl_products",
     *          schema = "inventory",
     *          uniqueConstraints = @UniqueConstraint(columnNames = {"sku"}),
     *          indexes = @Index(name = "idx_product_name", columnList = "name")
     *      )
     *      public class Product { ... }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Id
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Marks a field as the PRIMARY KEY of the entity
     *  Package: jakarta.persistence.Id
     *  Required: YES — every entity MUST have exactly one @Id field
     *  Types:   Long, Integer, String, UUID — all supported
     *
     *  Syntax:
     *      @Id
     *      private Long id;
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @GeneratedValue
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Specifies HOW the primary key value is automatically generated
     *  Package: jakarta.persistence.GeneratedValue
     *  Required: NO — but almost always used with @Id
     *
     *  Strategies (GenerationType enum):
     *
     *    AUTO        → JPA picks strategy based on the database (default)
     *                  Best for portability across databases
     *
     *    IDENTITY    → DB auto-increment column (MySQL, H2, PostgreSQL SERIAL)
     *                  Fastest for INSERT; no pre-allocation
     *                  Cannot batch inserts efficiently
     *
     *    SEQUENCE    → DB sequence object (PostgreSQL, Oracle)
     *                  Efficient: fetches multiple IDs at once (allocationSize)
     *                  Default allocationSize = 50 (fetches 50 IDs per DB call)
     *
     *    TABLE       → Uses a special table to manage IDs
     *                  Portable but slowest; rarely used in modern apps
     *
     *  Syntax:
     *      @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
     *      private Long id;
     *
     *      @Id @GeneratedValue(strategy = GenerationType.SEQUENCE,
     *                          generator = "product_seq")
     *      @SequenceGenerator(name = "product_seq", sequenceName = "seq_product", allocationSize = 1)
     *      private Long id;
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Column
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Customises the mapped column (name, length, nullable, unique, etc.)
     *  Package: jakarta.persistence.Column
     *  Required: NO — without it, column name = field name (snake_case by default)
     *
     *  Key attributes:
     *    name            → column name in the database
     *    nullable        → false adds NOT NULL constraint (default: true)
     *    unique          → true adds UNIQUE constraint (default: false)
     *    length          → for VARCHAR columns (default: 255)
     *    precision       → for DECIMAL columns (total digits)
     *    scale           → for DECIMAL columns (digits after decimal point)
     *    insertable      → whether included in INSERT SQL (default: true)
     *    updatable       → whether included in UPDATE SQL (default: true)
     *    columnDefinition → raw SQL type definition (e.g., "TEXT", "JSONB")
     *
     *  Syntax:
     *      @Column(name = "product_name", nullable = false, length = 200, unique = true)
     *      private String name;
     *
     *      @Column(name = "price", precision = 10, scale = 2, nullable = false)
     *      private BigDecimal price;
     *
     *      @Column(columnDefinition = "TEXT")   // Use TEXT type instead of VARCHAR
     *      private String description;
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Transient
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Marks a field to be IGNORED by JPA (not stored in the database)
     *  Package: jakarta.persistence.Transient
     *  Required: NO — use when a field is purely for in-memory calculation
     *
     *  Use cases:
     *   •  Computed / derived fields  (e.g., fullName = firstName + " " + lastName)
     *   •  Temporary state            (e.g., isLoggedIn flag)
     *   •  Cached values              (e.g., formatted date string)
     *
     *  Syntax:
     *      @Transient
     *      private String fullName;   // Not stored in DB, computed at runtime
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Temporal  (Legacy — for java.util.Date / java.util.Calendar)
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Specifies how java.util.Date is stored (DATE, TIME, or TIMESTAMP)
     *  Note:    Not needed for java.time.* types (LocalDate, LocalDateTime) — use those!
     *
     *  Syntax:
     *      @Temporal(TemporalType.DATE)        // Only date part (YYYY-MM-DD)
     *      private Date birthDate;
     *
     *      @Temporal(TemporalType.TIMESTAMP)   // Date + time
     *      private Date createdAt;
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Enumerated
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Maps a Java enum to a database column
     *  Type:    EnumType.ORDINAL (stores index 0,1,2...)  ← BAD (fragile!)
     *           EnumType.STRING  (stores name "ACTIVE")   ← ALWAYS prefer this
     *
     *  Syntax:
     *      @Enumerated(EnumType.STRING)
     *      @Column(nullable = false)
     *      private Status status;
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 5: RELATIONSHIP ANNOTATIONS — QUICK REFERENCE                   ║
     * ║     (Full examples in Example02RelationshipAnnotations.java)                 ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔗 THE FOUR RELATIONSHIP TYPES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ANNOTATION    EXAMPLE IN REAL WORLD              DB IMPLEMENTATION
     *  ────────────  ───────────────────────────────    ───────────────────────────────
     *  @OneToOne     Person ──── Passport               FK in either table
     *  @OneToMany    Department ──── [Employee, ...]    FK in the "many" table
     *  @ManyToOne    Employee ──── Department           FK in the "many" table
     *  @ManyToMany   Student ──── [Course, ...]         Join table (pivot table)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ⚡ FETCH STRATEGIES — CRUCIAL CONCEPT:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  FETCH TYPE        BEHAVIOUR                  DEFAULT FOR
     *  ──────────────    ──────────────────────────────────────────────────────────────
     *  FetchType.EAGER   Load related entities NOW (same SELECT)    @ManyToOne, @OneToOne
     *  FetchType.LAZY    Load related entities ON DEMAND            @OneToMany, @ManyToMany
     *
     *  RULE: ALWAYS use LAZY unless you have a specific reason for EAGER.
     *        EAGER loading causes N+1 problems and kills performance!
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🌊 CASCADE TYPES — WHAT OPERATIONS PROPAGATE?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  CASCADE TYPE      MEANING
     *  ──────────────    ───────────────────────────────────────────────────────────────
     *  PERSIST           When parent is saved, save children too
     *  MERGE             When parent is updated, update children too
     *  REMOVE            When parent is deleted, delete children too  ← DANGEROUS
     *  REFRESH           When parent is refreshed from DB, refresh children too
     *  DETACH            When parent is detached, detach children too
     *  ALL               All of the above → @OneToMany(cascade = CascadeType.ALL)
     *
     *  RULE: Be VERY careful with CascadeType.REMOVE and CascadeType.ALL.
     *        Deleting a parent can wipe out all children unintentionally!
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @JoinColumn
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Specifies the foreign key column in the current table
     *  Package: jakarta.persistence.JoinColumn
     *
     *  Key attributes:
     *    name            → foreign key column name in the current table
     *    referencedColumnName → column in the referenced table (default: PK)
     *    nullable        → whether FK can be NULL
     *    insertable / updatable → whether included in INSERT / UPDATE
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @JoinTable
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Specifies the join (pivot) table for @ManyToMany relationships
     *
     *  Key attributes:
     *    name            → name of the join table
     *    joinColumns     → FK column pointing to the owning entity
     *    inverseJoinColumns → FK column pointing to the inverse entity
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 mappedBy — The "Inverse Side" Marker
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Declares the NON-OWNING side of a bidirectional relationship.
     *           The value is the field name on the OWNING side.
     *
     *  Rule:    In every bidirectional relationship, exactly one side is the OWNER
     *           (has the FK / join table). The other side uses mappedBy.
     *
     *  Example:
     *      // OWNING SIDE (has FK column)
     *      @ManyToOne
     *      @JoinColumn(name = "department_id")
     *      private Department department;
     *
     *      // INVERSE SIDE (mappedBy = field name on owning side)
     *      @OneToMany(mappedBy = "department")
     *      private List<Employee> employees;
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║          SECTION 6: QUERY ANNOTATIONS — QUICK REFERENCE                     ║
     * ║          (Full examples in Example03QueryAnnotations.java)                   ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Query
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Defines a custom JPQL or native SQL query on a repository method
     *  Package: org.springframework.data.jpa.repository.Query
     *
     *  Key attributes:
     *    value     → the JPQL or SQL query string
     *    nativeQuery → true = native SQL; false = JPQL (default)
     *    countQuery → for Pageable queries, the COUNT query
     *
     *  JPQL vs SQL:
     *    JPQL (Java Persistence Query Language):
     *      → Queries use ENTITY names and FIELD names (not table/column names)
     *      → Portable across databases
     *      → Example: "SELECT u FROM User u WHERE u.email = :email"
     *
     *    Native SQL:
     *      → Uses actual table and column names
     *      → Database-specific syntax allowed
     *      → Example: "SELECT * FROM users WHERE email = :email"
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Modifying
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Required for @Query methods that modify data (UPDATE/DELETE)
     *  Rule:    MUST be combined with @Transactional to work correctly
     *  Package: org.springframework.data.jpa.repository.Modifying
     *
     *  Key attributes:
     *    clearAutomatically → clear the persistence context after modification
     *    flushAutomatically → flush before executing the query
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Param
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Binds a method parameter to a named parameter in the query
     *  Package: org.springframework.data.repository.query.Param
     *
     *  Syntax:
     *      @Query("SELECT u FROM User u WHERE u.email = :email")
     *      Optional<User> findByEmail(@Param("email") String email);
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 Spring Data Query Methods (Derived Queries) — No @Query needed!
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  Spring Data generates queries automatically from method NAMES:
     *
     *    findByEmail(String email)
     *     → SELECT ... WHERE email = ?
     *
     *    findByFirstNameAndLastName(String fn, String ln)
     *     → SELECT ... WHERE first_name = ? AND last_name = ?
     *
     *    findByAgeGreaterThan(int age)
     *     → SELECT ... WHERE age > ?
     *
     *    findByNameContainingIgnoreCase(String name)
     *     → SELECT ... WHERE LOWER(name) LIKE LOWER('%name%')
     *
     *    findTop10ByOrderByCreatedAtDesc()
     *     → SELECT ... ORDER BY created_at DESC LIMIT 10
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 7 & 8: REPOSITORIES & LIFECYCLE CALLBACKS — QUICK REFERENCE     ║
     * ║     (Full examples in Example04RepositoryAnnotations.java)                   ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🗂️ SPRING DATA REPOSITORY HIERARCHY:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *         Repository<T, ID>                    ← Marker interface
     *                │
     *         CrudRepository<T, ID>                ← Basic CRUD (save, findById, delete)
     *                │
     *         PagingAndSortingRepository<T, ID>    ← + pagination + sorting
     *                │
     *         JpaRepository<T, ID>                 ← + flush, saveAll, batch ops
     *
     *  RULE: Extend JpaRepository for most use cases.
     *        It provides everything you need for standard JPA operations.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 ENTITY LIFECYCLE CALLBACKS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ANNOTATION      TRIGGERED WHEN                         USE CASE
     *  ─────────────   ──────────────────────────────         ─────────────────────────
     *  @PrePersist     Before INSERT (before entity saved)    Set createdAt timestamp
     *  @PostPersist    After INSERT (after entity saved)      Send notification/log
     *  @PreUpdate      Before UPDATE                          Set updatedAt timestamp
     *  @PostUpdate     After UPDATE                           Publish change event
     *  @PreRemove      Before DELETE                          Validate deletion
     *  @PostRemove     After DELETE                           Cleanup related resources
     *  @PostLoad       After entity is loaded from DB         Decrypt a field value
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 9: EMBEDDABLE ANNOTATIONS                                        ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Embeddable & @Embedded
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  @Embeddable: Marks a class whose fields are embedded into the OWNING entity's table.
     *               The embedded class does NOT have its own table.
     *
     *  @Embedded:   Used in the owning entity to embed an @Embeddable object.
     *
     *  WHY USE IT?
     *    Model value objects (e.g., Address, Money, DateRange) without extra tables.
     *
     *  Example:
     *
     *     // ADDRESS is embeddable — no separate table!
     *     @Embeddable
     *     public class Address {
     *         private String street;
     *         private String city;
     *         private String zipCode;
     *     }
     *
     *     @Entity
     *     public class Customer {
     *         @Id @GeneratedValue
     *         private Long id;
     *
     *         private String name;
     *
     *         @Embedded   // Address fields go into the CUSTOMER table
     *         private Address address;
     *     }
     *
     *  DB Table: CUSTOMER (id, name, street, city, zip_code)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @EmbeddedId
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  Use when the PRIMARY KEY itself is a composite key represented as an object.
     *
     *  Example:
     *     @Embeddable
     *     public class OrderItemId implements Serializable {
     *         private Long orderId;
     *         private Long productId;
     *     }
     *
     *     @Entity
     *     public class OrderItem {
     *         @EmbeddedId
     *         private OrderItemId id;
     *         private int quantity;
     *     }
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 10: INHERITANCE ANNOTATIONS                                      ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🏛️ THREE STRATEGIES TO MAP INHERITANCE TO TABLES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  STRATEGY                DB STRUCTURE                  PROS / CONS
     *  ─────────────────────   ───────────────────────────   ──────────────────────────
     *  SINGLE_TABLE            One table, ALL columns        Fast queries; lots of NULLs
     *  TABLE_PER_CLASS         One table per concrete class  No NULLs; no polymorphic query
     *  JOINED                  Parent table + child tables   Normalised; complex JOINs
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Inheritance, @DiscriminatorColumn, @DiscriminatorValue
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  Example using SINGLE_TABLE:
     *
     *     @Entity
     *     @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
     *     @DiscriminatorColumn(name = "vehicle_type", discriminatorType = DiscriminatorType.STRING)
     *     public abstract class Vehicle {
     *         @Id @GeneratedValue
     *         private Long id;
     *         private String brand;
     *     }
     *
     *     @Entity
     *     @DiscriminatorValue("CAR")
     *     public class Car extends Vehicle {
     *         private int numberOfDoors;
     *     }
     *
     *     @Entity
     *     @DiscriminatorValue("TRUCK")
     *     public class Truck extends Vehicle {
     *         private double payloadTons;
     *     }
     *
     *  DB: One VEHICLE table with columns: id, brand, vehicle_type, number_of_doors, payload_tons
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @MappedSuperclass
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    The superclass is NOT an entity (no table for it).
     *           Its fields are INHERITED by child entities into their own tables.
     *           Used for shared audit fields (id, createdAt, updatedAt).
     *
     *  Example:
     *     @MappedSuperclass
     *     public abstract class BaseEntity {
     *         @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
     *         private Long id;
     *
     *         @Column(updatable = false)
     *         private LocalDateTime createdAt;
     *
     *         private LocalDateTime updatedAt;
     *
     *         @PrePersist
     *         void onCreate() { this.createdAt = LocalDateTime.now(); }
     *
     *         @PreUpdate
     *         void onUpdate() { this.updatedAt = LocalDateTime.now(); }
     *     }
     *
     *     @Entity
     *     public class Product extends BaseEntity {
     *         private String name;  // Table: id, created_at, updated_at, name
     *     }
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 12: BEST PRACTICES & COMMON PITFALLS                             ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ✅ BEST PRACTICES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  1.  ALWAYS use FetchType.LAZY for @OneToMany and @ManyToMany.
     *      Eager loading fetches ALL related records even when you don't need them.
     *
     *  2.  Use @MappedSuperclass for common audit fields (id, createdAt, updatedAt).
     *      Avoid duplicating these in every entity.
     *
     *  3.  Use EnumType.STRING for @Enumerated.
     *      ORDINAL values break if you reorder or add enum constants.
     *
     *  4.  Use java.time.LocalDate / LocalDateTime instead of java.util.Date.
     *      Modern types don't need @Temporal and are thread-safe.
     *
     *  5.  Always implement equals() and hashCode() for entities using the ID.
     *      JPA relies on these for caching and collection management.
     *
     *  6.  Use Optional<T> return type in repositories for single-result methods.
     *      Prevents NullPointerException.
     *
     *  7.  Prefer constructor injection in services over @Autowired on fields.
     *
     *  8.  Use DTOs (Data Transfer Objects) in REST controllers, NOT entities directly.
     *      Exposing entities can cause infinite recursion in JSON serialization.
     *
     *  9.  Mark many-to-many relationships as sets, not lists, to avoid duplicates.
     *
     * 10.  Use @Transactional(readOnly = true) on read-only service methods.
     *      Gives Hibernate a performance hint and prevents accidental writes.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ❌ COMMON PITFALLS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PITFALL 1: N+1 QUERY PROBLEM
     *  ────────────────────────────
     *  Problem:  Loading 100 departments, then accessing department.getEmployees()
     *            fires 1 + 100 = 101 queries!
     *  Fix:      Use JOIN FETCH in @Query:
     *            "SELECT d FROM Department d LEFT JOIN FETCH d.employees"
     *            OR use @EntityGraph annotation.
     *
     *  PITFALL 2: LazyInitializationException
     *  ────────────────────────────────────────
     *  Problem:  Accessing a LAZY collection outside an active transaction.
     *  Fix:      Ensure access happens inside a @Transactional method.
     *            OR use a DTO projection to load only what you need.
     *
     *  PITFALL 3: CascadeType.ALL on @ManyToMany
     *  ─────────────────────────────────────────
     *  Problem:  Deleting a Student also deletes the Course (unintended!).
     *  Fix:      Never use CascadeType.REMOVE or ALL on @ManyToMany relationships.
     *
     *  PITFALL 4: Bidirectional relationship without mappedBy
     *  ────────────────────────────────────────────────────────
     *  Problem:  Creates TWO separate foreign keys instead of one shared relationship.
     *  Fix:      Always declare mappedBy on the inverse (non-owning) side.
     *
     *  PITFALL 5: Missing @Modifying on UPDATE/DELETE @Query
     *  ────────────────────────────────────────────────────────
     *  Problem:  Spring Data throws an error at runtime.
     *  Fix:      Add @Modifying + @Transactional to all write @Query methods.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 13: INTERVIEW QUESTIONS & ANSWERS                                ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Q1: What is the difference between JPA and Hibernate?
     *
     *     JPA is a SPECIFICATION (a set of interfaces and rules defined by Jakarta EE).
     *     Hibernate is an IMPLEMENTATION of that specification. Other implementations
     *     include EclipseLink and OpenJPA. In Spring Boot, Hibernate is used by default.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q2: What is the difference between @OneToMany and @ManyToOne?
     *
     *     They represent TWO SIDES OF THE SAME RELATIONSHIP.
     *     @ManyToOne is on the "many" side (Employee), holds the foreign key column.
     *     @OneToMany is on the "one" side (Department), uses mappedBy to reference the
     *     field on the owning side. @ManyToOne is always the OWNING SIDE.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q3: What is the N+1 problem and how do you solve it?
     *
     *     N+1 problem: When you fetch N parent entities and then trigger N additional
     *     queries to load each parent's children lazily.
     *     Solutions:
     *      (a) JOIN FETCH in JPQL: "SELECT d FROM Department d LEFT JOIN FETCH d.employees"
     *      (b) @EntityGraph on a repository method
     *      (c) Use a DTO projection to select only needed columns
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q4: What is the difference between CrudRepository and JpaRepository?
     *
     *     CrudRepository: Basic CRUD (save, findById, findAll, delete, count)
     *     JpaRepository: Extends PagingAndSortingRepository which extends CrudRepository,
     *     plus adds: saveAll, saveAndFlush, deleteInBatch, getById, flush.
     *     JpaRepository is the most feature-rich and commonly used.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q5: What does ddl-auto = create-drop do?
     *
     *     On startup: Hibernate drops all tables (if they exist) and re-creates them
     *     from entity definitions. On application shutdown: drops all tables again.
     *     This is useful for development / testing, NEVER for production.
     *     Production should use: validate or none (managed by Flyway/Liquibase).
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q6: What is the difference between @Transient (JPA) and transient (Java keyword)?
     *
     *     Java's transient: Excludes a field from Java serialization (Serializable).
     *     JPA's @Transient:  Excludes a field from JPA persistence (database).
     *     You can use both together if you need to exclude from both.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q7: What is the purpose of @MappedSuperclass?
     *
     *     @MappedSuperclass makes a base class contribute its field mappings to
     *     child entity classes without being an entity itself (no table is created).
     *     Perfect for audit base classes with id, createdAt, updatedAt fields.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q8: What is the difference between @Embedded and @OneToOne?
     *
     *     @Embedded (with @Embeddable): No join, no separate table. The embedded
     *     class fields live directly in the owning entity's table. No separate ID.
     *     @OneToOne: A separate table with its own primary key. A JOIN is needed.
     *     Use @Embedded for value objects (Address, Money). Use @OneToOne for
     *     independent entities that just happen to have a 1-to-1 relationship.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                   ║");
        System.out.println("║     SPRING BOOT ANNOTATIONS — CHAPTER 4 OVERVIEW                 ║");
        System.out.println("║             Spring Data JPA Annotations                          ║");
        System.out.println("║                                                                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("📚 Chapter 4 covers ALL Spring Data JPA annotations");
        System.out.println();
        System.out.println("🗂️  Files in this chapter:");
        System.out.println("   1. Chapter04Overview.java          ← YOU ARE HERE");
        System.out.println("   2. Example01EntityAnnotations.java");
        System.out.println("   3. Example02RelationshipAnnotations.java");
        System.out.println("   4. Example03QueryAnnotations.java");
        System.out.println("   5. Example04RepositoryAnnotations.java");
        System.out.println("   6. HowItWorksExplained.java");
        System.out.println();
        System.out.println("🎯 Topics covered:");
        System.out.println("   @Entity         @Table          @Id           @GeneratedValue");
        System.out.println("   @Column         @Transient      @Enumerated   @Temporal");
        System.out.println("   @OneToOne       @OneToMany      @ManyToOne    @ManyToMany");
        System.out.println("   @JoinColumn     @JoinTable      mappedBy      FetchType");
        System.out.println("   @Query          @Modifying      @Param        @NamedQuery");
        System.out.println("   @Repository     @NoRepositoryBean              CrudRepository");
        System.out.println("   @PrePersist     @PostLoad       @PreUpdate    @PostUpdate");
        System.out.println("   @Embeddable     @Embedded       @EmbeddedId");
        System.out.println("   @MappedSuperclass @Inheritance  @DiscriminatorColumn");
        System.out.println();
        System.out.println("💡 Start with: Example01EntityAnnotations.java");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

