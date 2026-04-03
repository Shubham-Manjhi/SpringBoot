package com.learning.springboot.chapter07;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║             EXAMPLE 01: BASIC CONSTRAINT ANNOTATIONS                                 ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example01BasicConstraints.java
 * Purpose:     Demonstrate @NotNull, @NotEmpty, @NotBlank, @Null,
 *              @Min, @Max, @DecimalMin, @DecimalMax,
 *              @Positive, @PositiveOrZero, @Negative, @NegativeOrZero,
 *              @Digits, @Size, @AssertTrue, @AssertFalse
 * Difficulty:  ⭐⭐ Beginner–Intermediate
 * Time:        45–60 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */


// ══════════════════════════════════════════════════════════════════════════════════════
//  DOMAIN CLASSES DEMONSTRATING EVERY BASIC CONSTRAINT
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║        UserRegistrationRequest — Null / Empty / String Constraints           ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * This class models a user registration form and demonstrates the most commonly
 * used null/empty constraints on String and numeric fields.
 * ─────────────────────────────────────────────────────────────────────────────────
 */
class UserRegistrationRequest {

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @NotBlank  — MOST IMPORTANT STRING CONSTRAINT
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the annotated String is:
     *     1. Not null
     *     2. Not empty ("")
     *     3. Not whitespace-only ("   ", "\t", "\n")
     *
     * PACKAGE: jakarta.validation.constraints.NotBlank
     *
     * APPLIES TO: CharSequence (String, StringBuilder, etc.)
     *
     * KEY ATTRIBUTES:
     *   message  → Custom error message shown when validation fails.
     *              Default: "must not be blank"
     *              Best practice: Provide meaningful, user-friendly messages.
     *
     * IMPORTANT: null is considered INVALID by @NotBlank.
     *            This is different from @Email, @Pattern, @Size — those treat null as VALID.
     *
     * WHY USE @NotBlank INSTEAD OF @NotNull?
     *   @NotNull("") → PASSES (empty string is not null)
     *   @NotBlank("") → FAILS (empty string is blank)
     *   For any text field that needs content → always @NotBlank
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @NotBlank(message = "First name is required and must not be blank")
    private String firstName;

    @NotBlank(message = "Last name is required and must not be blank")
    private String lastName;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @NotNull  — Value must not be null
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the annotated element is NOT null.
     *   It DOES allow empty strings, empty collections, and zero values.
     *
     * WHEN TO USE:
     *   → For non-String types (Integer, Long, Boolean, custom objects) that must
     *     be present but whose empty/zero state is valid.
     *   → When null means "not provided" and you need to force the field to be sent.
     *
     * WHEN NOT TO USE:
     *   → For String fields that must have content → use @NotBlank instead.
     *
     * EXAMPLE BELOW:
     *   dateOfBirth must be provided (not null) — the date value itself is validated
     *   separately by @Past. If null were allowed, @Past would pass (null = valid for @Past).
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @NotNull(message = "Date of birth is required")
    private java.time.LocalDate dateOfBirth;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @NotEmpty  — Value must not be null or empty
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the annotated element is:
     *     1. Not null
     *     2. Not empty  (String: length > 0; Collection: size > 0; Array: length > 0)
     *
     * CAUTION: "   " (three spaces) PASSES @NotEmpty but FAILS @NotBlank!
     *          Only use @NotEmpty for collections, arrays, and maps — not for text.
     *
     * USE CASES:
     *   → Collections that must contain at least one element
     *   → Arrays that cannot be empty
     *   → Maps that need at least one key-value pair
     *
     * HERE: Interests list must be provided and have at least one interest.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @NotEmpty(message = "Please provide at least one interest")
    private List<String> interests;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @AssertTrue  — Boolean must be true
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the boolean value is true.
     *   Null is considered VALID (use @NotNull in addition if you need to reject null).
     *
     * USE CASE:
     *   A user must accept terms and conditions to register.
     *   The UI sends termsAccepted = true or false.
     *   @AssertTrue ensures the user cannot register without accepting.
     *
     * ⚠️  Works on boolean (primitive) and Boolean (wrapper).
     *     For getter-based validation: place @AssertTrue on a method that returns boolean.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @AssertTrue(message = "You must accept the terms and conditions to register")
    private boolean termsAccepted;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @AssertFalse  — Boolean must be false
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   The opposite of @AssertTrue — the value must be false.
     *
     * USE CASE:
     *   A field that represents "account flagged as spam" — should always be false
     *   when a user registers. Prevents clients from setting this during registration.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @AssertFalse(message = "The spam flag must not be set during registration")
    private boolean markedAsSpam;

    // ── Constructors ──────────────────────────────────────────────────────────────
    public UserRegistrationRequest() {}

    // ── Getters & Setters ────────────────────────────────────────────────────────
    public String           getFirstName()    { return firstName; }
    public void             setFirstName(String fn) { this.firstName = fn; }
    public String           getLastName()     { return lastName; }
    public void             setLastName(String ln)  { this.lastName = ln; }
    public java.time.LocalDate getDateOfBirth()  { return dateOfBirth; }
    public void             setDateOfBirth(java.time.LocalDate d) { this.dateOfBirth = d; }
    public List<String>     getInterests()    { return interests; }
    public void             setInterests(List<String> i) { this.interests = i; }
    public boolean          isTermsAccepted() { return termsAccepted; }
    public void             setTermsAccepted(boolean t) { this.termsAccepted = t; }
    public boolean          isMarkedAsSpam()  { return markedAsSpam; }
    public void             setMarkedAsSpam(boolean s) { this.markedAsSpam = s; }
}


/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║        ProductRequest — Numeric Constraints (Min, Max, Decimal, Digits)      ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Min(value) — Minimum integer value (inclusive)
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the number is ≥ value (inclusive by default).
     *   value is a long, so supports up to Long.MAX_VALUE.
     *
     * SUPPORTED TYPES:
     *   byte, short, int, long (and wrappers)
     *   BigInteger, BigDecimal
     *   String representations of those types (numeric strings)
     *
     * NOT SUPPORTED: float, double (use @DecimalMin/@DecimalMax for those)
     *
     * IMPORTANT: null is considered VALID. Add @NotNull to reject null.
     *
     * HERE: Stock quantity must be 0 or more (can't have -5 items).
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Max(value) — Maximum integer value (inclusive)
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the number is ≤ value (inclusive by default).
     *
     * HERE: A discount percentage must be between 0 and 100.
     *       Combined with @Min(0), the complete range is [0, 100].
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @NotNull(message = "Discount percentage is required")
    @Min(value = 0, message = "Discount cannot be negative")
    @Max(value = 100, message = "Discount cannot exceed 100%")
    private Integer discountPercent;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @DecimalMin / @DecimalMax  — Decimal-aware value range
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Like @Min/@Max but for decimal types (BigDecimal, double, float).
     *   The value is a STRING representing the boundary (can be a decimal).
     *
     * KEY ATTRIBUTE: inclusive
     *   inclusive = true  (default): value must be ≥ / ≤ the boundary (inclusive)
     *   inclusive = false:           value must be > / < the boundary (exclusive)
     *
     * SUPPORTED TYPES:
     *   BigDecimal, BigInteger, CharSequence (numeric String), byte, short,
     *   int, long (and wrappers), double, float (though precision may differ)
     *
     * HERE: Price must be between 0.01 (exclusive 0, inclusive 0.01) and 999999.99
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", inclusive = true,  message = "Price must be at least 0.01")
    @DecimalMax(value = "999999.99", inclusive = true, message = "Price cannot exceed 999,999.99")
    private BigDecimal price;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Positive  — Value must be strictly greater than 0
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the number is > 0 (strictly positive).
     *   Equivalent to @DecimalMin("0", inclusive = false) but more readable.
     *   Null is considered VALID — combine with @NotNull if needed.
     *
     * SUPPORTED TYPES: All numeric types and their wrappers.
     *
     * USE CASE:
     *   IDs, quantities that must be at least 1, prices that can't be zero.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Positive(message = "Category ID must be a positive number")
    private Long categoryId;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @PositiveOrZero  — Value must be ≥ 0
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the number is ≥ 0 (zero or positive).
     *   Equivalent to @Min(0) but works on all numeric types including BigDecimal.
     *   Preferred over @Min(0) when you need decimal support.
     *
     * USE CASE:
     *   Weight, height, measurements — can be zero (not yet measured) but not negative.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @PositiveOrZero(message = "Weight must be zero or positive (in grams)")
    private Double weightGrams;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Digits(integer, fraction)  — Number of digit constraints
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the number has at most:
     *     integer  → digits before the decimal point
     *     fraction → digits after the decimal point
     *
     * EXAMPLE:
     *   @Digits(integer = 6, fraction = 2)
     *   → Accepts:  12345.67, 999999.99, 1.00, 0.01
     *   → Rejects:  1234567.89 (7 integer digits), 1.234 (3 fraction digits)
     *
     * SUPPORTS: BigDecimal, BigInteger, String, byte, short, int, long (and wrappers)
     *
     * HERE: Tax amount — maximum 6 integer digits and 4 decimal places.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Digits(integer = 6, fraction = 4,
            message = "Tax rate must have at most 6 integer digits and 4 decimal places")
    private BigDecimal taxRate;

    // ── Getters & Setters ────────────────────────────────────────────────────────
    public String     getName()            { return name; }
    public void       setName(String n)    { this.name = n; }
    public Integer    getStockQuantity()   { return stockQuantity; }
    public void       setStockQuantity(Integer s) { this.stockQuantity = s; }
    public Integer    getDiscountPercent() { return discountPercent; }
    public void       setDiscountPercent(Integer d) { this.discountPercent = d; }
    public BigDecimal getPrice()           { return price; }
    public void       setPrice(BigDecimal p) { this.price = p; }
    public Long       getCategoryId()      { return categoryId; }
    public void       setCategoryId(Long c) { this.categoryId = c; }
    public Double     getWeightGrams()     { return weightGrams; }
    public void       setWeightGrams(Double w) { this.weightGrams = w; }
    public BigDecimal getTaxRate()         { return taxRate; }
    public void       setTaxRate(BigDecimal t) { this.taxRate = t; }
}


/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║        PaymentRequest — Negative Constraints & @Size on Collections          ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
class PaymentRequest {

    @NotNull(message = "Order ID is required")
    @Positive(message = "Order ID must be positive")
    private Long orderId;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Negative / @NegativeOrZero
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * @Negative      → value must be < 0 (strictly negative)
     * @NegativeOrZero → value must be ≤ 0 (zero or negative)
     *
     * USE CASES:
     *   → Financial debits: a withdrawal amount should be negative in a ledger system
     *   → Temperature: certain readings might always be below zero
     *   → Discounts stored as negative deltas
     *
     * HERE: creditAdjustment must be negative (it's a deduction from account balance).
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Negative(message = "Credit adjustment must be a negative value (a deduction)")
    private BigDecimal creditAdjustment;

    @NegativeOrZero(message = "Balance adjustment must be zero or negative")
    private BigDecimal balanceAdjustment;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Size(min, max) on String
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the String length is within [min, max].
     *   Does NOT trim the string — "  " has length 2.
     *   Null is considered VALID — combine with @NotBlank for full protection.
     *
     * ATTRIBUTES:
     *   min     → minimum length (default: 0)
     *   max     → maximum length (default: Integer.MAX_VALUE)
     *   message → custom error message (can use {min} and {max} placeholders)
     *
     * HERE: Payment reference must be exactly 8–20 characters (e.g., "REF00012345").
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @NotBlank(message = "Payment reference is required")
    @Size(min = 8, max = 20, message = "Payment reference must be between {min} and {max} characters")
    private String paymentReference;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Size on List / Collection
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   When applied to a Collection/Array/Map, validates the SIZE (number of elements).
     *
     * HERE: itemIds must have between 1 and 50 items in a single payment request.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @NotNull(message = "Item IDs list is required")
    @Size(min = 1, max = 50, message = "A payment request must cover between 1 and 50 items")
    private List<Long> itemIds;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Null  — Value MUST be null
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   The OPPOSITE of @NotNull. The field must be null.
     *
     * USE CASE:
     *   In a CREATE operation, the client should NOT provide an ID (DB generates it).
     *   In combination with validation groups, @Null on Create and @NotNull on Update
     *   lets you use ONE DTO class for both operations.
     *
     * HERE: transactionId is assigned by the system — the client must NOT send it.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @Null(message = "Transaction ID must not be provided — it is assigned by the system")
    private String transactionId;

    // ── Getters & Setters ─────────────────────────────────────────────────────────
    public Long       getOrderId()            { return orderId; }
    public void       setOrderId(Long o)      { this.orderId = o; }
    public BigDecimal getCreditAdjustment()   { return creditAdjustment; }
    public void       setCreditAdjustment(BigDecimal c) { this.creditAdjustment = c; }
    public BigDecimal getBalanceAdjustment()  { return balanceAdjustment; }
    public void       setBalanceAdjustment(BigDecimal b) { this.balanceAdjustment = b; }
    public String     getPaymentReference()   { return paymentReference; }
    public void       setPaymentReference(String p) { this.paymentReference = p; }
    public List<Long> getItemIds()            { return itemIds; }
    public void       setItemIds(List<Long> i){ this.itemIds = i; }
    public String     getTransactionId()      { return transactionId; }
    public void       setTransactionId(String t) { this.transactionId = t; }
}


/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║        EmployeeRequest — Complete constraint stacking demonstration          ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 CONSTRAINT STACKING — Multiple constraints on one field
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * You can stack any number of constraints on a single field.
 * ALL constraints are validated independently. If multiple fail,
 * ALL violations are collected and returned together.
 *
 * This is intentional: the user gets all error messages at once instead
 * of seeing one error, fixing it, then seeing the next one.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
class EmployeeRequest {

    /*
     * STACKING EXAMPLE:
     *   - Must not be blank (not null, not empty, not whitespace)
     *   - Must be between 2 and 100 characters long
     *
     * If null or blank → @NotBlank fires
     * If too short/long → @Size fires
     * Both can fire simultaneously if, e.g., value = null (fails @NotBlank; @Size treats null as valid)
     */
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between {min} and {max} characters")
    private String fullName;

    /*
     * STACKING EXAMPLE:
     *   - Must not be null (required field)
     *   - Must be ≥ 0 (no negative values)
     *   - Must be ≤ 60 (reasonable employee age limit)
     *   - Business rule: employees can only be registered if 18+ (min = 18 in HR context)
     */
    @NotNull(message = "Age is required")
    @Min(value = 18, message = "Employee must be at least 18 years old")
    @Max(value = 70, message = "Employee age cannot exceed 70")
    private Integer age;

    /*
     * STACKING EXAMPLE:
     *   - Must not be null
     *   - Must be positive (salary > 0)
     *   - Decimal max — salary caps (example)
     *   - Digit precision — max 8 integer digits, 2 decimal places
     */
    @NotNull(message = "Salary is required")
    @DecimalMin(value = "1000.00", message = "Salary must be at least 1,000.00")
    @DecimalMax(value = "99999999.99", message = "Salary cannot exceed 99,999,999.99")
    @Digits(integer = 8, fraction = 2,
            message = "Salary must have at most 8 integer digits and 2 decimal places")
    private BigDecimal salary;

    /*
     * STACKING EXAMPLE:
     *   - Must not be empty (department list cannot be empty)
     *   - Size between 1 and 5 (an employee can belong to up to 5 departments)
     */
    @NotEmpty(message = "Employee must be assigned to at least one department")
    @Size(max = 5, message = "An employee cannot belong to more than 5 departments")
    private List<String> departments;

    // ── Getters & Setters ─────────────────────────────────────────────────────────
    public String       getFullName()   { return fullName; }
    public void         setFullName(String n) { this.fullName = n; }
    public Integer      getAge()        { return age; }
    public void         setAge(Integer a) { this.age = a; }
    public BigDecimal   getSalary()     { return salary; }
    public void         setSalary(BigDecimal s) { this.salary = s; }
    public List<String> getDepartments(){ return departments; }
    public void         setDepartments(List<String> d) { this.departments = d; }
}


/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                          📚 CONSTRAINT REFERENCE TABLE 📚
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * ANNOTATION          NULL VALID?   APPLIES TO                  CHECKS
 * ─────────────────   ──────────   ──────────────────────────   ─────────────────────────────────
 * @NotNull             No           Any type                     value != null
 * @NotEmpty            No           String, Collection, Array    not null + size/length > 0
 * @NotBlank            No           String/CharSequence          not null + has non-whitespace chars
 * @Null                N/A          Any type                     value IS null
 * @AssertTrue          Yes          boolean/Boolean              value is true (null = valid)
 * @AssertFalse         Yes          boolean/Boolean              value is false (null = valid)
 * @Min(n)              Yes          int, long, BigDecimal...     value >= n (null = valid)
 * @Max(n)              Yes          int, long, BigDecimal...     value <= n (null = valid)
 * @DecimalMin(s)       Yes          BigDecimal, double, String   value >= decimal(s) (null = valid)
 * @DecimalMax(s)       Yes          BigDecimal, double, String   value <= decimal(s) (null = valid)
 * @Positive            Yes          Any numeric                  value > 0 (null = valid)
 * @PositiveOrZero      Yes          Any numeric                  value >= 0 (null = valid)
 * @Negative            Yes          Any numeric                  value < 0 (null = valid)
 * @NegativeOrZero      Yes          Any numeric                  value <= 0 (null = valid)
 * @Digits(i,f)         Yes          BigDecimal, numeric, String  max digits (null = valid)
 * @Size(min,max)       Yes          String, Collection, Array    length/size in [min,max]
 *
 * KEY RULE: Most constraints treat NULL as VALID. This is intentional —
 *           the "is it provided?" check is separated from "is it valid?".
 *           Add @NotNull (or @NotBlank) to also reject null.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🧪 EXERCISES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. Create an OrderRequest with: orderId (@Positive), quantity (@Min(1) @Max(100)),
 *     totalAmount (@DecimalMin("0.01")), and notes (@Size(max=500)).
 *
 *  2. Try validating a ProductRequest with price = null — observe which constraints fire.
 *
 *  3. Test EmployeeRequest with salary = -500 — which constraint message appears?
 *
 *  4. Create a subscriptionTiers List<@NotBlank String> and annotate the list with
 *     @Size(min=1, max=3) — observe how both levels of validation work.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ➡️  NEXT: Example02StringAndDateConstraints.java
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
class Example01BasicConstraints {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║       CHAPTER 7 — EXAMPLE 01: Basic Constraint Annotations       ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  @NotNull       → value must not be null");
        System.out.println("  @NotEmpty      → not null + not empty (String/Collection/Array)");
        System.out.println("  @NotBlank      → not null + not empty + not whitespace (String only)");
        System.out.println("  @Null          → value MUST be null (for groups/create operations)");
        System.out.println("  @AssertTrue    → boolean must be true");
        System.out.println("  @AssertFalse   → boolean must be false");
        System.out.println("  @Min / @Max    → integer range (long, int, BigDecimal...)");
        System.out.println("  @DecimalMin/Max → decimal range (BigDecimal, double, String...)");
        System.out.println("  @Positive      → value > 0");
        System.out.println("  @PositiveOrZero → value >= 0");
        System.out.println("  @Negative      → value < 0");
        System.out.println("  @NegativeOrZero → value <= 0");
        System.out.println("  @Digits(i,f)   → max integer and fraction digits");
        System.out.println("  @Size(min,max) → String length or Collection size in [min,max]");
        System.out.println();
        System.out.println("Classes defined: UserRegistrationRequest, ProductRequest,");
        System.out.println("                 PaymentRequest, EmployeeRequest");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

