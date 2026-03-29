package com.learning.springboot.chapter04;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║             EXAMPLE 02: RELATIONSHIP ANNOTATIONS IN ACTION                           ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example02RelationshipAnnotations.java
 * Purpose:     Demonstrate @OneToOne, @OneToMany, @ManyToOne, @ManyToMany,
 *              @JoinColumn, @JoinTable, FetchType, CascadeType, mappedBy,
 *              orphanRemoval, and bidirectional helper methods.
 * Difficulty:  ⭐⭐⭐ Intermediate
 * Time:        60–90 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * 🏗️ DOMAIN MODEL WE ARE BUILDING:
 *
 *   ┌─────────────────────────────────────────────────────────────────────────────┐
 *   │                     E-COMMERCE DOMAIN MODEL                                 │
 *   │                                                                             │
 *   │  Customer ─────────── @OneToOne ─────────────── CustomerProfile            │
 *   │  (has one profile)                               (belongs to one customer)  │
 *   │                                                                             │
 *   │  Department ─────── @OneToMany ──────────────── [Employee, Employee, ...]  │
 *   │  (has many employees)              @ManyToOne   (belongs to one department) │
 *   │                                                                             │
 *   │  Order ─────────── @OneToMany ───────────────── [OrderItem, OrderItem, ...] │
 *   │  (has many items)             @ManyToOne        (belongs to one order)      │
 *   │                                                                             │
 *   │  Student ─────── @ManyToMany ────────────────── [Course, Course, ...]       │
 *   │  (enrolls in many courses)        @ManyToMany   (has many students)         │
 *   └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */


// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION A — @OneToOne  (Customer ↔ CustomerProfile)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                  @OneToOne — ONE CUSTOMER : ONE PROFILE                       ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @OneToOne  EXPLAINED:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT IS:
 *   A one-to-one relationship means: one entity instance is associated with
 *   exactly one instance of another entity, and vice versa.
 *
 * REAL-WORLD EXAMPLES:
 *   Person ──────── Passport
 *   Employee ─────── Contract
 *   User ────────── UserProfile
 *   Order ────────── ShippingAddress (dedicated to that order)
 *
 * DATABASE STRUCTURE (two common approaches):
 *
 *   APPROACH 1 — FK on the OWNING side (most common):
 *     TABLE: tbl_customers     (id, name, email, profile_id ← FK)
 *     TABLE: tbl_customer_profiles  (id, bio, avatar_url, phone)
 *
 *   APPROACH 2 — Shared Primary Key:
 *     TABLE: tbl_customers     (id, name, email)
 *     TABLE: tbl_customer_profiles  (id ← SAME as customer's id, bio, avatar_url)
 *     (use @MapsId for this approach)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * KEY ATTRIBUTES:
 *
 *   fetch     → FetchType.LAZY (recommended) or FetchType.EAGER (default for @OneToOne!)
 *               WARNING: @OneToOne defaults to EAGER — override it to LAZY.
 *
 *   cascade   → Which operations propagate to the related entity.
 *               CascadeType.ALL: persist/merge/remove/refresh/detach all cascade.
 *
 *   optional  → false: the FK column is NOT NULL (profile is always required)
 *               true (default): the FK column allows NULL (profile may not exist)
 *
 *   orphanRemoval → true: if the reference is set to null, the orphaned entity
 *                   is automatically deleted from the DB.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Entity
@Table(name = "tbl_customers")
class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @OneToOne — OWNING SIDE
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * This is the OWNING SIDE of the @OneToOne relationship.
     * The Customer table holds the foreign key: profile_id.
     *
     * @JoinColumn(name = "profile_id"):
     *   Specifies the FK column name in THIS table (tbl_customers.profile_id).
     *   Without @JoinColumn, Hibernate generates a default column name.
     *
     * cascade = CascadeType.ALL:
     *   When we save a Customer, the associated CustomerProfile is also saved.
     *   When we delete a Customer, the associated CustomerProfile is also deleted.
     *   Great for "owned" relationships where the child can't exist without parent.
     *
     * fetch = FetchType.LAZY:
     *   Profile is NOT loaded until we call customer.getProfile().
     *   If we only need customer.getName(), no extra SELECT for profile.
     *   This is a PERFORMANCE BEST PRACTICE.
     *
     * orphanRemoval = true:
     *   If we do customer.setProfile(null), Hibernate DELETES the old profile row.
     *   Without this: the profile row stays in DB but is no longer referenced (orphan).
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @OneToOne(
        fetch         = FetchType.LAZY,
        cascade       = CascadeType.ALL,
        orphanRemoval = true
    )
    @JoinColumn(
        name                  = "profile_id",  // FK column in tbl_customers
        referencedColumnName  = "id",          // PK column in tbl_customer_profiles (default, explicit for clarity)
        nullable              = true           // A customer may not have a profile yet
    )
    private CustomerProfile profile;

    // ── Constructors ──────────────────────────────────────────────────────────────
    protected Customer() {}

    public Customer(String name, String email) {
        this.name  = name;
        this.email = email;
    }

    // ── Business helper: keep both sides in sync ───────────────────────────────────
    /**
     * Always use this helper method to set a profile.
     * It keeps the bidirectional reference in sync so both sides are consistent.
     *
     * WHY NEEDED?
     *   If you do only customer.setProfile(profile), then profile.getCustomer()
     *   returns null in the SAME transaction (in-memory state is inconsistent).
     *   Both sides must be set manually.
     */
    public void assignProfile(CustomerProfile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setCustomer(this);  // Set the inverse side too
        }
    }

    public Long            getId()      { return id; }
    public String          getName()    { return name; }
    public String          getEmail()   { return email; }
    public CustomerProfile getProfile() { return profile; }
    public void            setProfile(CustomerProfile profile) { this.profile = profile; }
}

// ──────────────────────────────────────────────────────────────────────────────────────

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║    CustomerProfile — INVERSE SIDE of the @OneToOne relationship              ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @OneToOne(mappedBy = "profile")
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT mappedBy DOES:
 *   Declares that THIS SIDE does NOT own the relationship.
 *   The foreign key lives in the OTHER entity (Customer.profile_id).
 *   mappedBy = "profile" → refers to the field name in the OWNING entity (Customer).
 *
 * WHY IMPORTANT?
 *   Without mappedBy, JPA would think there are TWO separate relationships
 *   and try to create TWO foreign keys (one in each table). That's wrong!
 *   mappedBy tells JPA: "don't create a FK here, the other side owns this."
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Entity
@Table(name = "tbl_customer_profiles")
class CustomerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /*
     * INVERSE SIDE — mappedBy = "profile"
     * "profile" refers to the field name in Customer class.
     * No @JoinColumn here — the FK lives in Customer table.
     */
    @OneToOne(mappedBy = "profile", fetch = FetchType.LAZY)
    private Customer customer;

    // ── Constructors ──────────────────────────────────────────────────────────────
    protected CustomerProfile() {}

    public CustomerProfile(String bio, String phoneNumber) {
        this.bio         = bio;
        this.phoneNumber = phoneNumber;
    }

    public Long     getId()          { return id; }
    public String   getBio()         { return bio; }
    public String   getAvatarUrl()   { return avatarUrl; }
    public String   getPhoneNumber() { return phoneNumber; }
    public Customer getCustomer()    { return customer; }
    public void     setCustomer(Customer customer) { this.customer = customer; }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION B — @OneToMany / @ManyToOne  (Department ↔ Employee)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║           @OneToMany / @ManyToOne — DEPARTMENT : EMPLOYEES                   ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 THE RELATIONSHIP:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   One Department has MANY Employees.
 *   Each Employee belongs to ONE Department.
 *
 *   TABLES:
 *     tbl_departments  (id, name, budget)
 *     tbl_employees    (id, name, salary, department_id ← FK)
 *
 *   The FOREIGN KEY always lives on the "MANY" side table (tbl_employees).
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 WHO IS THE OWNING SIDE?
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   In a @OneToMany / @ManyToOne pair:
 *   → @ManyToOne (Employee side) is ALWAYS the OWNING SIDE (has the FK column).
 *   → @OneToMany (Department side) is the INVERSE SIDE (uses mappedBy).
 *
 *   This matters for cascades and for which side's changes Hibernate tracks.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Entity
@Table(name = "tbl_departments")
class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(precision = 15, scale = 2)
    private BigDecimal budget;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @OneToMany — INVERSE SIDE
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Represents the "one" side of the relationship.
     *   This collection holds all Employee entities that belong to this Department.
     *
     * mappedBy = "department":
     *   → Tells JPA: "the foreign key is on the Employee side, in its 'department' field"
     *   → JPA won't try to create a join table or add a FK column here
     *
     * cascade = CascadeType.ALL:
     *   → When we save a Department, all its employees are saved
     *   → When we delete a Department, all its employees are deleted
     *   ⚠️  Be careful with CascadeType.REMOVE / ALL — it deletes children!
     *
     * orphanRemoval = true:
     *   → If we remove an Employee from the employees list (department.getEmployees().remove(emp))
     *   → Hibernate automatically deletes that Employee from the DB
     *   → Very useful for "owned" collections
     *
     * fetch = FetchType.LAZY (DEFAULT for @OneToMany):
     *   → Employees are NOT loaded when Department is loaded
     *   → Only loaded when you call department.getEmployees()
     *   → This is the CORRECT default — always keep it LAZY!
     *
     * USE List vs Set:
     *   List → maintains insertion order; can cause "MultipleBagFetchException" with multiple EAGER loads
     *   Set  → no order guarantee; safer for multiple JOIN FETCHes
     *   → Use List<Employee> for most cases; ArrayList initialisation avoids NPE.
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @OneToMany(
        mappedBy      = "department",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    private List<Employee> employees = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────────────────────────
    protected Department() {}

    public Department(String name, BigDecimal budget) {
        this.name   = name;
        this.budget = budget;
    }

    // ── Bidirectional sync helper ─────────────────────────────────────────────────
    /**
     * ALWAYS use these helpers to add/remove employees.
     *
     * Why? In JPA, if you only call employees.add(emp), the employee's
     * department field is null in memory — the DB is correct only after flush.
     * These helpers keep BOTH sides of the bidirectional relationship in sync.
     */
    public void addEmployee(Employee employee) {
        employees.add(employee);
        employee.setDepartment(this);   // Set owning side
    }

    public void removeEmployee(Employee employee) {
        employees.remove(employee);
        employee.setDepartment(null);   // Clear owning side
    }

    public Long           getId()        { return id; }
    public String         getName()      { return name; }
    public BigDecimal     getBudget()    { return budget; }
    public List<Employee> getEmployees() { return Collections.unmodifiableList(employees); }
}

// ──────────────────────────────────────────────────────────────────────────────────────

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║    Employee — OWNING SIDE of the @ManyToOne relationship                     ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @ManyToOne — OWNING SIDE (holds the foreign key)
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT DOES:
 *   Many employees can belong to one department.
 *   This field holds the reference to the Department entity.
 *   The FK column (department_id) lives in the tbl_employees table.
 *
 * fetch = FetchType.LAZY (we override the default):
 *   Default for @ManyToOne is EAGER — but LAZY is almost always better.
 *   With EAGER: every time you load an Employee, Hibernate also loads the
 *   Department immediately (even if you only need the employee's name).
 *   With LAZY: Department is only loaded when you call employee.getDepartment().
 *
 * optional = false:
 *   This employee MUST belong to a department — the FK cannot be NULL.
 *   Maps to: NOT NULL constraint on department_id column.
 *
 * @JoinColumn(name = "department_id"):
 *   Specifies the FK column name in the tbl_employees table.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Entity
@Table(name = "tbl_employees")
class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal salary;

    @Column(name = "hire_date")
    private LocalDateTime hireDate;

    /*
     * 📌 @ManyToOne — OWNING SIDE
     *
     * fetch = FetchType.LAZY  (override the default EAGER — best practice!)
     * optional = false        → department_id is NOT NULL
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name                 = "department_id",  // FK column in tbl_employees
        referencedColumnName = "id",             // PK in tbl_departments
        nullable             = false             // Redundant with optional=false but explicit
    )
    private Department department;

    // ── Constructors ──────────────────────────────────────────────────────────────
    protected Employee() {}

    public Employee(String name, BigDecimal salary) {
        this.name     = name;
        this.salary   = salary;
        this.hireDate = LocalDateTime.now();
    }

    public Long         getId()          { return id; }
    public String       getName()        { return name; }
    public BigDecimal   getSalary()      { return salary; }
    public LocalDateTime getHireDate()   { return hireDate; }
    public Department   getDepartment()  { return department; }
    public void         setDepartment(Department dept) { this.department = dept; }

    // ──────────────────────────────────────────────────────────────────────────────
    // ⚠️  equals() and hashCode() for entities — CRITICAL for JPA
    // ──────────────────────────────────────────────────────────────────────────────
    /*
     * WHY OVERRIDE equals() and hashCode()?
     *
     *   JPA uses these to manage entities in collections and the persistence context.
     *   The default Object.equals() uses memory address — two DB rows representing
     *   the same employee will NOT be equal with the default implementation.
     *
     * RULES:
     *   1. Base equality on the BUSINESS KEY or the DATABASE ID
     *   2. Handle null ID (entity not yet persisted → id is null)
     *   3. Never use mutable fields for hashCode (they change, breaking Set behaviour)
     *   4. Use getClass() instead of instanceof for JPA proxy compatibility
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee)) return false;
        Employee other = (Employee) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode(); // Constant — safe across transient/persistent states
    }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION C — @ManyToMany  (Student ↔ Course)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║              @ManyToMany — STUDENT ENROLLS IN MANY COURSES                   ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 THE RELATIONSHIP:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   One Student can enrol in MANY Courses.
 *   One Course can have MANY Students.
 *
 *   TABLES:
 *     tbl_students              (id, name, email)
 *     tbl_courses               (id, title, credits)
 *     tbl_student_enrollments   (student_id FK, course_id FK)  ← JOIN TABLE
 *
 *   The JOIN TABLE holds two foreign keys — one to each entity.
 *   There's NO separate entity for the join table in this example.
 *   (If you need extra data on the join table, like enrollment_date,
 *    model it as a proper entity: StudentEnrollment.)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 OWNING SIDE vs INVERSE SIDE:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   OWNING SIDE (Student):
 *     → Has @JoinTable defining the join table structure
 *     → Hibernate manages the join table rows
 *
 *   INVERSE SIDE (Course):
 *     → Uses mappedBy = "courses" (field name in the owning entity)
 *     → Does NOT define the join table again
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Entity
@Table(name = "tbl_students")
class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @ManyToMany — OWNING SIDE
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * fetch = FetchType.LAZY (DEFAULT for @ManyToMany):
     *   Courses are NOT loaded when Student is loaded.
     *   Only loaded when student.getCourses() is called.
     *   Always keep this LAZY — EAGER would load all courses for all students!
     *
     * cascade = {CascadeType.PERSIST, CascadeType.MERGE}:
     *   When a Student is saved, new Courses in the collection are also saved.
     *   When a Student is merged, Courses are merged.
     *   NOTE: We deliberately exclude REMOVE here — deleting a Student should NOT
     *         delete the Courses! Courses are shared among many students.
     *
     * @JoinTable — Defines the join (pivot) table:
     *   name = "tbl_student_enrollments"
     *     → Name of the join table in the DB
     *
     *   joinColumns = @JoinColumn(name = "student_id")
     *     → FK column in the join table pointing to THIS entity (Student.id)
     *
     *   inverseJoinColumns = @JoinColumn(name = "course_id")
     *     → FK column in the join table pointing to the OTHER entity (Course.id)
     *
     * USE Set<Course> NOT List<Course>:
     *   With @ManyToMany: if you use List, Hibernate generates duplicates when
     *   fetching multiple EAGER collections simultaneously ("MultipleBagFetchException").
     *   Set avoids this and also prevents duplicate enrollments naturally.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @ManyToMany(
        fetch   = FetchType.LAZY,
        cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @JoinTable(
        name               = "tbl_student_enrollments",
        joinColumns        = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();

    // ── Constructors ──────────────────────────────────────────────────────────────
    protected Student() {}

    public Student(String name, String email) {
        this.name  = name;
        this.email = email;
    }

    // ── Bidirectional sync helpers ────────────────────────────────────────────────
    /**
     * Always use these helpers to enrol/unenrol students.
     * They keep both sides of the bidirectional relationship in sync.
     */
    public void enrol(Course course) {
        this.courses.add(course);
        course.getStudentsInternal().add(this);  // Sync inverse side
    }

    public void unenrol(Course course) {
        this.courses.remove(course);
        course.getStudentsInternal().remove(this);  // Sync inverse side
    }

    public Long       getId()      { return id; }
    public String     getName()    { return name; }
    public String     getEmail()   { return email; }
    public Set<Course> getCourses(){ return Collections.unmodifiableSet(courses); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        Student other = (Student) o;
        return id != null && id.equals(other.id);
    }

    @Override public int hashCode() { return getClass().hashCode(); }
}

// ──────────────────────────────────────────────────────────────────────────────────────

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║    Course — INVERSE SIDE of the @ManyToMany relationship                     ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @ManyToMany(mappedBy = "courses")
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * mappedBy = "courses":
 *   → "courses" is the field name in the OWNING entity (Student.courses)
 *   → Hibernate knows: the join table is defined there, don't create another one
 *   → This side does NOT define @JoinTable
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Entity
@Table(name = "tbl_courses")
class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private Integer credits;

    @Column(columnDefinition = "TEXT")
    private String description;

    /*
     * INVERSE SIDE — mappedBy = "courses"
     * "courses" is the field name in Student (the owning side).
     * No @JoinTable needed here — already defined in Student.
     */
    @ManyToMany(mappedBy = "courses", fetch = FetchType.LAZY)
    private Set<Student> students = new HashSet<>();

    // ── Constructors ──────────────────────────────────────────────────────────────
    protected Course() {}

    public Course(String title, Integer credits) {
        this.title   = title;
        this.credits = credits;
    }

    public Long         getId()              { return id; }
    public String       getTitle()           { return title; }
    public Integer      getCredits()         { return credits; }
    public String       getDescription()     { return description; }
    public void         setDescription(String d) { this.description = d; }

    // Package-private: only used by Student's sync helper
    Set<Student> getStudentsInternal()       { return students; }
    public Set<Student> getStudents()        { return Collections.unmodifiableSet(students); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course)) return false;
        Course other = (Course) o;
        return id != null && id.equals(other.id);
    }

    @Override public int hashCode() { return getClass().hashCode(); }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION D — Composite Pattern: Order + OrderItem (@OneToMany with extra data)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║     ORDER + ORDER ITEM — @OneToMany with Rich Relationship Data               ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🧩 WHY A SEPARATE OrderItem ENTITY?
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * In the Student-Course example, the join table had only two FK columns.
 * But what if you need extra data on the relationship?
 *   → quantity (how many of a product in an order)
 *   → unit_price at time of order (price may change later)
 *   → discount applied
 *
 * When the join table needs extra columns, model it as a full ENTITY:
 *
 *   Order   ─── @OneToMany ──→   OrderItem   ←── @ManyToOne ─── (Product is referenced by OrderItem)
 *
 * This is the standard pattern for rich many-to-many relationships.
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Entity
@Table(name = "tbl_orders")
class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /*
     * Order has many OrderItems.
     * cascade = ALL: saving an Order saves all its items.
     * orphanRemoval = true: removing an item from the list deletes it from DB.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────────────────────────
    protected Order() {}

    public Order(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    // ── Business Methods ──────────────────────────────────────────────────────────
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);    // Sync owning side
        recalculateTotal();
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
        recalculateTotal();
    }

    private void recalculateTotal() {
        this.totalAmount = items.stream()
            .map(OrderItem::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long            getId()          { return id; }
    public LocalDateTime   getOrderDate()   { return orderDate; }
    public OrderStatus     getOrderStatus() { return orderStatus; }
    public BigDecimal      getTotalAmount() { return totalAmount; }
    public List<OrderItem> getItems()       { return Collections.unmodifiableList(items); }
    public void            setOrderStatus(OrderStatus s) { this.orderStatus = s; }
}

enum OrderStatus { PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }

// ──────────────────────────────────────────────────────────────────────────────────────

@Entity
@Table(name = "tbl_order_items")
class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;     // Snapshot of product name at order time

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;   // Snapshot of price at order time (price may change later)

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    /*
     * @ManyToOne — owning side (holds FK: order_id)
     * An OrderItem belongs to one Order.
     * fetch = LAZY: we don't need the full Order when loading items.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // ── Constructors ──────────────────────────────────────────────────────────────
    protected OrderItem() {}

    public OrderItem(String productName, BigDecimal unitPrice, Integer quantity) {
        this.productName = productName;
        this.unitPrice   = unitPrice;
        this.quantity    = quantity;
    }

    // ── Computed Field ────────────────────────────────────────────────────────────
    @Transient
    public BigDecimal getLineTotal() {
        // price * qty * (1 - discount%)
        BigDecimal discount  = discountPercent.divide(new BigDecimal("100"));
        BigDecimal netPrice  = unitPrice.multiply(BigDecimal.ONE.subtract(discount));
        return netPrice.multiply(new BigDecimal(quantity));
    }

    public Long       getId()              { return id; }
    public String     getProductName()     { return productName; }
    public BigDecimal getUnitPrice()       { return unitPrice; }
    public Integer    getQuantity()        { return quantity; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public Order      getOrder()           { return order; }
    public void       setOrder(Order o)    { this.order = o; }
}


/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                               📚 LEARNING SUMMARY 📚
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * ✅ WHAT WE LEARNED IN EXAMPLE 02:
 *
 *  CONCEPT                 RULE / BEST PRACTICE
 *  ─────────────────────   ──────────────────────────────────────────────────────────
 *  @OneToOne               Default fetch is EAGER — ALWAYS override to LAZY
 *                          Use optional=false + @JoinColumn(nullable=false) for required
 *                          Use orphanRemoval=true for "owned" dependent entities
 *
 *  @OneToMany              Default fetch is LAZY — keep it that way!
 *                          Always on the "one" side; use mappedBy (it's the inverse side)
 *                          Use helper methods (addEmployee/removeEmployee) to sync both sides
 *
 *  @ManyToOne              Default fetch is EAGER — ALWAYS override to LAZY
 *                          Always the OWNING SIDE — holds the FK column (@JoinColumn)
 *                          Use optional=false when the FK is NOT NULL
 *
 *  @ManyToMany             Default fetch is LAZY — keep it that way!
 *                          Use @JoinTable on the owning side, mappedBy on inverse side
 *                          NEVER use CascadeType.REMOVE — you'll delete shared entities!
 *                          Use Set<T> not List<T> to avoid duplicate issues
 *                          Use bidirectional sync helpers for in-memory consistency
 *
 *  mappedBy                Points to the FIELD NAME (not column name) on the owning side
 *                          The side WITHOUT mappedBy holds the FK column
 *
 *  equals()/hashCode()     Override in entities — base on ID or business key
 *                          Use constant hashCode() to be safe across persist states
 *
 *  Rich @ManyToMany        When join table needs extra columns, model as a full entity
 *                          (Order → @OneToMany → OrderItem → @ManyToOne → Product)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🧪 EXERCISES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. Create a Manager entity with @OneToMany employees and @ManyToOne department.
 *  2. Try FetchType.EAGER on employees and print how many SQL queries fire.
 *  3. Add enrollment_date to the Student-Course relationship (rich join entity).
 *  4. Test orphanRemoval by removing an OrderItem from an Order.
 *  5. Write a JPQL query with JOIN FETCH to avoid N+1 when loading orders with items.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ➡️  NEXT: Example03QueryAnnotations.java
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
class Example02RelationshipAnnotations {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║       CHAPTER 4 — EXAMPLE 02: Relationship Annotations           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  @OneToOne   → Customer ↔ CustomerProfile  (FK in Customer table)");
        System.out.println("  @OneToMany  → Department → [Employee...]   (FK in Employee table)");
        System.out.println("  @ManyToOne  → Employee → Department        (OWNING SIDE, has FK)");
        System.out.println("  @ManyToMany → Student ↔ Course             (join table)");
        System.out.println("  @JoinColumn → defines FK column name & constraints");
        System.out.println("  @JoinTable  → defines join table name & FK columns for @ManyToMany");
        System.out.println("  mappedBy    → marks the NON-OWNING (inverse) side");
        System.out.println("  FetchType   → LAZY (recommended) vs EAGER");
        System.out.println("  CascadeType → controls what operations propagate to children");
        System.out.println("  orphanRemoval → auto-delete children removed from collection");
        System.out.println();
        System.out.println("Run the Spring Boot app — check /h2-console for all generated tables!");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

