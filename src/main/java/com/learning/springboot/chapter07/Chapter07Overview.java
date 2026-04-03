package com.learning.springboot.chapter07;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║                    SPRING BOOT ANNOTATIONS - COMPREHENSIVE GUIDE                     ║
 * ║                      Chapter 7: Spring Validation Annotations                        ║
 * ║                                                                                       ║
 * ║                           📚 From Zero to Expert 📚                                  ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Chapter:      7
 * Title:        Spring Validation Annotations
 * Difficulty:   ⭐⭐⭐ Intermediate
 * Estimated:    5–8 hours
 * Prerequisites: Chapter 1 (Core), Chapter 3 (Spring MVC & REST), Basic Java
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │                                                                                       │
 * │                    CHAPTER 7: OVERVIEW & LEARNING GOALS                              │
 * │                                                                                       │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📖 TABLE OF CONTENTS 📖
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Section  1 :  Chapter Introduction & The Validation Problem
 * Section  2 :  Technology Stack  (Bean Validation, Hibernate Validator, Spring)
 * Section  3 :  Null / Empty Constraints   (@NotNull, @NotEmpty, @NotBlank, @Null)
 * Section  4 :  Numeric Constraints        (@Min, @Max, @Positive, @PositiveOrZero,
 *                                           @Negative, @NegativeOrZero, @Digits,
 *                                           @DecimalMin, @DecimalMax)
 * Section  5 :  Size Constraint            (@Size)
 * Section  6 :  Boolean Constraints        (@AssertTrue, @AssertFalse)
 * Section  7 :  String Constraints         (@Email, @Pattern)
 * Section  8 :  Date / Time Constraints    (@Past, @PastOrPresent,
 *                                           @Future, @FutureOrPresent)
 * Section  9 :  @Valid  — Cascade Validation
 * Section 10 :  @Validated — Validation Groups & Method-Level Validation
 * Section 11 :  Custom Constraints (AnnotationProcessor + ConstraintValidator)
 * Section 12 :  Cross-Field Validation (class-level constraints)
 * Section 13 :  Constraint Composition  (@Constraint + meta-annotations)
 * Section 14 :  How Everything Works Together — Internal Mechanics
 * Section 15 :  Best Practices & Common Pitfalls
 * Section 16 :  Interview Questions & Answers
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * 📂 FILES IN THIS CHAPTER:
 *
 *  • Chapter07Overview.java             ← YOU ARE HERE
 *  • Example01BasicConstraints.java      (@NotNull, @NotEmpty, @NotBlank, @Size, @Min,
 *                                         @Max, @Positive, @Negative, @Digits, @AssertTrue)
 *  • Example02StringAndDateConstraints.java  (@Email, @Pattern, @Past, @Future, etc.)
 *  • Example03ValidAndValidated.java     (@Valid, @Validated, Groups, Method validation,
 *                                         complete REST controller with error handling)
 *  • Example04CustomConstraints.java     (Custom validator, cross-field constraint,
 *                                         constraint composition)
 *  • HowItWorksExplained.java            (Bean Validation internals, Hibernate Validator,
 *                                         Spring integration mechanics)
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
public class Chapter07Overview {

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
     *  ✓  Understand the Bean Validation (JSR-380) specification
     *  ✓  Apply all built-in constraint annotations confidently
     *  ✓  Trigger validation in REST controllers with @Valid and @Validated
     *  ✓  Use validation groups to apply different rules per use case
     *  ✓  Cascade validation into nested objects
     *  ✓  Create custom constraint annotations from scratch
     *  ✓  Implement cross-field validation (e.g., password == confirmPassword)
     *  ✓  Compose reusable meta-constraints from existing ones
     *  ✓  Handle validation errors and return structured error responses
     *  ✓  Answer tough validation interview questions with confidence
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ❓ THE PROBLEM VALIDATION SOLVES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * WITHOUT validation — the classic defensive programming nightmare:
     *
     *   public User createUser(String name, String email, int age) {
     *       // Check every field manually — tedious, error-prone, scattered
     *       if (name == null || name.isBlank()) {
     *           throw new IllegalArgumentException("Name cannot be blank");
     *       }
     *       if (name.length() < 2 || name.length() > 100) {
     *           throw new IllegalArgumentException("Name must be 2–100 characters");
     *       }
     *       if (email == null || !email.matches("[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+")) {
     *           throw new IllegalArgumentException("Invalid email format");
     *       }
     *       if (age < 18 || age > 120) {
     *           throw new IllegalArgumentException("Age must be 18–120");
     *       }
     *       // ... more checks ...
     *       // Actual business logic buried under dozens of validation lines
     *   }
     *
     * WITH Bean Validation — declare rules once, apply everywhere:
     *
     *   public class CreateUserRequest {
     *       @NotBlank
     *       @Size(min = 2, max = 100)
     *       private String name;
     *
     *       @NotBlank
     *       @Email
     *       private String email;
     *
     *       @Min(18) @Max(120)
     *       private int age;
     *   }
     *
     *   // In controller — one annotation does the work:
     *   public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest req) {
     *       // Validation already done! Focus on business logic only.
     *       return ResponseEntity.ok(userService.create(req));
     *   }
     *
     * BENEFITS:
     *  ✓  Declarative — rules are self-documenting on the field
     *  ✓  Reusable — validate the same class in controllers, services, tests
     *  ✓  Centralised error handling — one @ExceptionHandler for all validation errors
     *  ✓  Composable — build powerful custom constraints from simple ones
     *  ✓  Standard — works with any Bean Validation provider (Hibernate Validator, etc.)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📦 REQUIRED DEPENDENCY:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * In build.gradle (already added for this project):
     *
     *   implementation 'org.springframework.boot:spring-boot-starter-validation'
     *
     * This brings in:
     *  •  jakarta.validation-api    (Bean Validation 3.0 specification)
     *  •  hibernate-validator       (Reference implementation)
     *  •  hibernate-validator-annotation-processor (compile-time checking)
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║          SECTION 2: TECHNOLOGY STACK                                         ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🏛️ THREE LAYERS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ┌────────────────────────────────────────────────────────────────────────────┐
     *  │               YOUR APPLICATION CODE                                        │
     *  │   (annotates classes with @NotNull, @Email, triggers with @Valid)          │
     *  ├────────────────────────────────────────────────────────────────────────────┤
     *  │               SPRING VALIDATION INTEGRATION                                │
     *  │   (intercepts @Valid/@Validated calls, integrates with MVC, AOP)           │
     *  ├────────────────────────────────────────────────────────────────────────────┤
     *  │               HIBERNATE VALIDATOR                                           │
     *  │   (implements the spec: runs constraints, collects violations)              │
     *  ├────────────────────────────────────────────────────────────────────────────┤
     *  │               BEAN VALIDATION (Jakarta Validation 3.0 / JSR-380)          │
     *  │   (specification: defines @NotNull, @Size, ConstraintValidator interface)  │
     *  └────────────────────────────────────────────────────────────────────────────┘
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📦 ANNOTATION NAMESPACES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PACKAGE                                   ANNOTATIONS COME FROM
     *  ──────────────────────────────────────    ────────────────────────────────────
     *  jakarta.validation.constraints.*          Bean Validation spec (all built-ins)
     *  org.hibernate.validator.constraints.*     Hibernate Validator extras (@URL, @ISBN...)
     *  jakarta.validation.*                      @Valid, Validator, ConstraintViolation
     *  org.springframework.validation.*          @Validated, BindingResult, Errors
     *  org.springframework.validation.annotation.* @Validated (Spring's version)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ⚡ @Valid vs @Validated — KEY DIFFERENCE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  @Valid      (jakarta.validation.Valid)
     *    → Standard Bean Validation annotation
     *    → Triggers validation on the annotated parameter or field
     *    → Supports CASCADE (validates nested objects)
     *    → Does NOT support validation groups
     *
     *  @Validated  (org.springframework.validation.annotation.Validated)
     *    → Spring's extension of @Valid
     *    → Supports VALIDATION GROUPS (validate only subset of constraints)
     *    → Enables METHOD-LEVEL validation on any Spring bean (via AOP proxy)
     *    → Can be applied at class level (enables method validation for all methods)
     *
     *  RULE: Use @Valid for simple cascade validation in controller parameters.
     *        Use @Validated when you need groups OR method-level validation in @Service.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║          SECTION 3: NULL / EMPTY CONSTRAINTS — QUICK REFERENCE              ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @NotNull
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Value must NOT be null. Can be empty string or empty collection.
     *  Applies: Any type (String, Integer, List, custom objects)
     *  Passes:  "" (empty string), [] (empty list), 0
     *  Fails:   null
     *
     *  Use when: The field MUST be provided but can be empty (e.g., a flag, an ID).
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @NotEmpty
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Value must NOT be null AND must NOT be empty.
     *  Applies: String, Collection, Map, Array
     *  Passes:  "a", [1,2], {k:v}
     *  Fails:   null, "", [], {}
     *
     *  Use when: A string or collection must have at least one element.
     *            (Does NOT trim whitespace — "  " passes @NotEmpty!)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @NotBlank  ← MOST COMMONLY USED for String fields
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Value must NOT be null AND must contain at least one non-whitespace char.
     *  Applies: String ONLY
     *  Passes:  "hello", "  hello  " (has non-space chars)
     *  Fails:   null, "", "   " (only spaces), "\t\n"
     *
     *  Use when: A text field must have meaningful content (names, titles, descriptions).
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Null
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Value MUST be null. Opposite of @NotNull.
     *  Use when: A field should NOT be provided in certain validation groups.
     *            Example: id must be null on CREATE but must not be null on UPDATE.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 COMPARISON TABLE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  VALUE       @NotNull   @NotEmpty   @NotBlank
     *  ─────────   ────────   ─────────   ─────────
     *  null        FAIL       FAIL        FAIL
     *  ""          pass       FAIL        FAIL
     *  "   "       pass       pass        FAIL
     *  "hello"     pass       pass        pass
     *
     * RULE OF THUMB: For any String field that must have meaningful text → use @NotBlank.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║          SECTION 4: NUMERIC CONSTRAINTS — QUICK REFERENCE                   ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Min(value) / @Max(value)
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Integer value must be ≥ min (or ≤ max). value is a long.
     *  Applies: byte, short, int, long, BigInteger, BigDecimal (and their wrappers)
     *  Note:    Does NOT work directly on float/double — use @DecimalMin/@DecimalMax
     *
     *  @Min(18) @Max(120)
     *  private int age;
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @DecimalMin(value) / @DecimalMax(value)
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Numeric value (decimal-aware) must be ≥ / ≤ the string value.
     *  Applies: All numeric types including BigDecimal, double, float, String (numeric)
     *  Extra:   inclusive = false makes it strictly greater-than / less-than
     *
     *  @DecimalMin(value = "0.01", inclusive = true)
     *  @DecimalMax(value = "9999999.99", inclusive = true)
     *  private BigDecimal price;
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Positive / @PositiveOrZero
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  @Positive      → value must be > 0  (strictly positive)
     *  @PositiveOrZero → value must be ≥ 0 (positive or zero)
     *
     *  Applies: All numeric types. Null is considered valid (use @NotNull too if needed).
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Negative / @NegativeOrZero
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  @Negative      → value must be < 0
     *  @NegativeOrZero → value must be ≤ 0
     *
     *  Use case: temperature adjustments, financial debits, price discounts.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Digits(integer, fraction)
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Validates maximum digit counts for a number.
     *  integer  → max digits before decimal point
     *  fraction → max digits after decimal point
     *
     *  @Digits(integer = 8, fraction = 2)
     *  private BigDecimal amount;   // Max: 99999999.99
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║          SECTION 5–8: MORE CONSTRAINTS — QUICK REFERENCE                    ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Size(min, max)
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    Size/length must be within [min, max] (inclusive).
     *  Applies: String (length), Collection, Map, Array (size)
     *  Note:    Does NOT trim. "  " has length 2. Use @NotBlank separately.
     *
     *  @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
     *  private String name;
     *
     *  @Size(min = 1, max = 10, message = "Cart must have 1–10 items")
     *  private List<CartItem> items;
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @AssertTrue / @AssertFalse
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  @AssertTrue  → boolean must be true
     *  @AssertFalse → boolean must be false
     *  Null is considered valid (combine with @NotNull if required).
     *
     *  Use case: "I accept the terms and conditions" checkbox
     *
     *  @AssertTrue(message = "You must accept the terms and conditions")
     *  private boolean termsAccepted;
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Email
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    String must be a valid email address format.
     *  Applies: String (and CharSequence)
     *  Default: Uses a loose regex (allows most valid emails)
     *  Strict:  Use regexp attribute for stricter patterns
     *
     *  @Email
     *  private String email;                              // e.g., user@example.com
     *
     *  @Email(regexp = "^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
     *  private String strictEmail;
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 @Pattern(regexp)
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  What:    String must match the given regular expression.
     *  Applies: String (and CharSequence)
     *  Null is considered valid (combine with @NotNull/@NotBlank).
     *
     *  @Pattern(regexp = "^[A-Z]{2}[0-9]{6}$", message = "Must be 2 uppercase letters + 6 digits")
     *  private String passportNumber;
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DATE / TIME CONSTRAINTS
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  @Past          → date/time must be in the PAST (strictly before now)
     *  @PastOrPresent → date/time must be in the past OR NOW
     *  @Future        → date/time must be in the FUTURE (strictly after now)
     *  @FutureOrPresent → date/time must be in the future OR NOW
     *
     *  Applies: LocalDate, LocalDateTime, LocalTime, ZonedDateTime, Instant,
     *           OffsetDateTime, Date, Calendar
     *
     *  @Past(message = "Date of birth must be a past date")
     *  private LocalDate dateOfBirth;
     *
     *  @Future(message = "Event date must be in the future")
     *  private LocalDateTime eventDate;
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║          SECTION 11–13: ADVANCED CONCEPTS — QUICK REFERENCE                 ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 CUSTOM CONSTRAINTS
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * THREE STEPS to create a custom constraint:
     *
     *  STEP 1: Define the annotation
     *    @Documented
     *    @Constraint(validatedBy = PhoneNumberValidator.class)
     *    @Target({ElementType.FIELD, ElementType.PARAMETER})
     *    @Retention(RetentionPolicy.RUNTIME)
     *    public @interface ValidPhone {
     *        String message() default "Invalid phone number";
     *        Class<?>[] groups() default {};
     *        Class<? extends Payload>[] payload() default {};
     *    }
     *
     *  STEP 2: Implement ConstraintValidator
     *    public class PhoneNumberValidator
     *        implements ConstraintValidator<ValidPhone, String> {
     *        @Override
     *        public boolean isValid(String value, ConstraintValidatorContext ctx) {
     *            if (value == null) return true;  // null handled by @NotNull
     *            return value.matches("\\+?[1-9][0-9]{7,14}");
     *        }
     *    }
     *
     *  STEP 3: Use it
     *    @ValidPhone
     *    private String phoneNumber;
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 CROSS-FIELD VALIDATION (Class-Level Constraint)
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Use when validation requires comparing MULTIPLE fields:
     *   → password == confirmPassword
     *   → startDate < endDate
     *   → if paymentType == CREDIT, then creditCardNumber must not be blank
     *
     * Apply the constraint at CLASS LEVEL:
     *   @PasswordMatch(message = "Passwords must match")
     *   public class RegisterRequest {
     *       private String password;
     *       private String confirmPassword;
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 CONSTRAINT COMPOSITION
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Combine multiple constraints into a single reusable meta-annotation:
     *
     *   @NotBlank
     *   @Size(min = 8, max = 64)
     *   @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9]).*$")
     *   @Constraint(validatedBy = {})   // No custom validator — composed only
     *   @Target({FIELD, PARAMETER})
     *   @Retention(RUNTIME)
     *   public @interface StrongPassword {
     *       String message() default "Password must be 8–64 chars with uppercase and digit";
     *       Class<?>[] groups() default {};
     *       Class<? extends Payload>[] payload() default {};
     *   }
     *
     *   // Now use it everywhere:
     *   @StrongPassword
     *   private String password;
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 VALIDATION GROUPS
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Groups let you apply DIFFERENT validation rules depending on context:
     *
     *   interface OnCreate {}
     *   interface OnUpdate {}
     *
     *   public class UserRequest {
     *       @Null(groups = OnCreate.class)     // id must be null when creating
     *       @NotNull(groups = OnUpdate.class)  // id must exist when updating
     *       private Long id;
     *
     *       @NotBlank(groups = {OnCreate.class, OnUpdate.class})
     *       private String name;
     *   }
     *
     *   // Controller: specify which group to validate
     *   public void create(@Validated(OnCreate.class) @RequestBody UserRequest req) {}
     *   public void update(@Validated(OnUpdate.class) @RequestBody UserRequest req) {}
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║          SECTION 15: BEST PRACTICES & COMMON PITFALLS                       ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ✅ BEST PRACTICES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  1.  Prefer @NotBlank over @NotNull for String fields.
     *      @NotNull allows empty strings — almost never what you want for text.
     *
     *  2.  Stack multiple constraints on one field.
     *      @NotBlank @Email @Size(max=200) → all three checked independently.
     *
     *  3.  Always provide meaningful messages.
     *      message = "Email must be a valid email address (e.g., user@example.com)"
     *      Much better than the default: "must be a valid email"
     *
     *  4.  Validate at the boundary (controller layer) not deep in services.
     *      Services receive pre-validated data and focus on business logic.
     *
     *  5.  Use @Validated at class level on @Service to enable method-level validation.
     *      Validates parameters and return values on service methods.
     *
     *  6.  Null is valid for most constraints (except @NotNull / @NotBlank / @NotEmpty).
     *      This is intentional — combine @NotNull + constraint for both checks.
     *
     *  7.  Use DTOs for validation, not JPA entities.
     *      Entities are for DB mapping; DTOs/requests are for API contracts.
     *
     *  8.  Create custom constraints for domain-specific rules.
     *      @ValidPhone, @ValidPostcode, @ValidIban — make intent clear.
     *
     *  9.  Use groups for create vs update validation differences.
     *      Don't create separate DTO classes just to handle missing id on create.
     *
     * 10.  Return structured error responses using MethodArgumentNotValidException handler.
     *      Always include field name, rejected value, and human-readable message.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ❌ COMMON PITFALLS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PITFALL 1: Using @NotEmpty instead of @NotBlank for String fields
     *    Problem:  @NotEmpty allows "   " (only spaces) to pass.
     *    Fix:      Use @NotBlank for all human-entered text fields.
     *
     *  PITFALL 2: Forgetting @Valid on nested objects
     *    Problem:  Constraints on Address fields won't fire unless User.address has @Valid.
     *    Fix:      @Valid private Address address;
     *
     *  PITFALL 3: Forgetting @Valid / @Validated on the method parameter in controller
     *    Problem:  @RequestBody has constraints, but nothing triggers validation.
     *    Fix:      public ResponseEntity<?> create(@Valid @RequestBody UserRequest req)
     *
     *  PITFALL 4: Using @Min / @Max on float / double
     *    Problem:  @Min / @Max are not defined for float/double.
     *    Fix:      Use @DecimalMin / @DecimalMax instead.
     *
     *  PITFALL 5: Self-invocation bypasses @Validated on @Service
     *    Problem:  this.someMethod() bypasses the AOP proxy — no validation fires.
     *    Fix:      Inject the service into itself, or restructure to avoid self-invocation.
     *
     *  PITFALL 6: Constraint doesn't fire because field is null (except @NotNull group)
     *    Problem:  @Email, @Pattern, @Size — all treat null as valid. Field passes.
     *    Fix:      Combine @NotNull (or @NotBlank) + @Email to catch both cases.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║          SECTION 16: INTERVIEW QUESTIONS & ANSWERS                           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Q1: What is Bean Validation? What is its relationship to Hibernate Validator?
     *
     *     Bean Validation (JSR-380, Jakarta Validation 3.0) is a SPECIFICATION
     *     that defines the standard annotations (@NotNull, @Size, etc.) and the
     *     ConstraintValidator API. Hibernate Validator is the REFERENCE IMPLEMENTATION
     *     of that specification — the engine that actually executes the validation rules.
     *     Spring Boot auto-configures Hibernate Validator when the validation starter
     *     is on the classpath.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q2: What is the difference between @Valid and @Validated?
     *
     *     @Valid (jakarta.validation.Valid):
     *       Standard Bean Validation marker. Triggers validation and supports cascade
     *       (validates nested @Valid fields). Does NOT support validation groups.
     *
     *     @Validated (org.springframework.validation.annotation.Validated):
     *       Spring's extension. Supports validation GROUPS (apply specific subset of
     *       constraints). Also enables method-level validation on Spring beans via AOP.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q3: What is the difference between @NotNull, @NotEmpty, and @NotBlank?
     *
     *     @NotNull  → value must not be null (accepts "", "   ")
     *     @NotEmpty → value must not be null or empty (accepts "   ")
     *     @NotBlank → value must not be null, empty, or whitespace-only (String only)
     *
     *     For String fields that must have meaningful text: ALWAYS use @NotBlank.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q4: How do validation groups work?
     *
     *     Groups are marker interfaces used to categorize constraints. You assign
     *     constraints to groups via the groups attribute and trigger specific groups
     *     using @Validated(GroupInterface.class). This lets you apply different
     *     validation rules for CREATE vs UPDATE vs PATCH operations using one DTO.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q5: How do you handle validation errors in a Spring Boot REST API?
     *
     *     When validation fails, Spring throws MethodArgumentNotValidException (for
     *     @RequestBody) or ConstraintViolationException (for @RequestParam / @PathVariable).
     *     Handle it with @ExceptionHandler in @ControllerAdvice:
     *       - Extract BindingResult.getFieldErrors()
     *       - Map to a structured error response {field, message, rejectedValue}
     *       - Return HTTP 400 Bad Request
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q6: How do you validate a method parameter or return value in a @Service?
     *
     *     1. Add @Validated at CLASS LEVEL on the @Service class.
     *     2. Spring creates a proxy that intercepts method calls.
     *     3. Annotate method parameters with constraint annotations directly.
     *     4. On violation, Spring throws ConstraintViolationException.
     *
     *     @Service @Validated
     *     public class UserService {
     *         public User findById(@NotNull @Positive Long id) { ... }
     *         public @NotNull User createUser(@Valid CreateUserRequest req) { ... }
     *     }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q7: How do you perform cross-field validation?
     *
     *     Use a class-level custom constraint. The validator receives the entire object
     *     and can compare any number of fields. Apply the annotation to the class:
     *
     *     @PasswordMatch  ← class-level constraint
     *     public class RegisterRequest {
     *         private String password;
     *         private String confirmPassword;
     *     }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * Q8: What is constraint composition?
     *
     *     Combining multiple built-in constraints into a single reusable annotation
     *     using meta-annotations. The composed constraint fires ALL inner constraints.
     *     Use @OverridesAttribute to customise inner constraint attributes.
     *     Use @ReportAsSingleViolation to report a single error (not one per inner
     *     constraint).
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                   ║");
        System.out.println("║     SPRING BOOT ANNOTATIONS — CHAPTER 7 OVERVIEW                 ║");
        System.out.println("║              Spring Validation Annotations                        ║");
        System.out.println("║                                                                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("📚 Chapter 7 covers ALL Spring Validation annotations");
        System.out.println();
        System.out.println("🗂️  Files in this chapter:");
        System.out.println("   1. Chapter07Overview.java              ← YOU ARE HERE");
        System.out.println("   2. Example01BasicConstraints.java");
        System.out.println("   3. Example02StringAndDateConstraints.java");
        System.out.println("   4. Example03ValidAndValidated.java");
        System.out.println("   5. Example04CustomConstraints.java");
        System.out.println("   6. HowItWorksExplained.java");
        System.out.println();
        System.out.println("🎯 Annotations covered:");
        System.out.println("   @NotNull   @NotEmpty   @NotBlank   @Null");
        System.out.println("   @Min       @Max        @Positive   @PositiveOrZero");
        System.out.println("   @Negative  @NegativeOrZero  @DecimalMin  @DecimalMax");
        System.out.println("   @Digits    @Size        @AssertTrue  @AssertFalse");
        System.out.println("   @Email     @Pattern");
        System.out.println("   @Past      @PastOrPresent  @Future  @FutureOrPresent");
        System.out.println("   @Valid     @Validated  (groups, cascade, method-level)");
        System.out.println("   Custom constraints, cross-field, constraint composition");
        System.out.println();
        System.out.println("💡 Start with: Example01BasicConstraints.java");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

