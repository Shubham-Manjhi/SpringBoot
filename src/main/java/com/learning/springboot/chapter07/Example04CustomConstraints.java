package com.learning.springboot.chapter07;

import jakarta.validation.*;
import jakarta.validation.constraints.*;
import java.lang.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║             EXAMPLE 04: CUSTOM CONSTRAINTS, CROSS-FIELD & COMPOSITION               ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example04CustomConstraints.java
 * Purpose:     Demonstrate:
 *               - Custom field-level constraint (@ValidPhone)
 *               - Custom class-level constraint (@PasswordMatch — cross-field)
 *               - Constraint composition (@StrongPassword, @ValidAge)
 *               - Programmatic validation using Validator directly
 *               - Real-world example tying all advanced concepts together
 * Difficulty:  ⭐⭐⭐⭐ Intermediate–Advanced
 * Time:        60–90 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART A — CUSTOM FIELD-LEVEL CONSTRAINT: @ValidPhone
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║     STEP 1: Define @ValidPhone — the constraint ANNOTATION                   ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 CREATING A CUSTOM CONSTRAINT — THREE MANDATORY STEPS
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Every custom constraint needs:
 *   1. An ANNOTATION (@ValidPhone) — declares the constraint, its message, groups
 *   2. A VALIDATOR (PhoneNumberValidator) — implements the actual validation logic
 *   3. APPLICATION — put @ValidPhone on fields / parameters
 *
 * MANDATORY ANNOTATION ATTRIBUTES (Bean Validation specification requires these):
 *   String message()                          — default error message
 *   Class<?>[] groups() default {}            — validation groups support
 *   Class<? extends Payload>[] payload() default {} — for extension (rarely used)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @Constraint(validatedBy = PhoneNumberValidator.class)
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT DOES:
 *   Links this annotation to its ConstraintValidator implementation(s).
 *   validatedBy can be an array for multiple types:
 *     validatedBy = {PhoneStringValidator.class, PhoneIntegerValidator.class}
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @Target — Where the annotation can be placed
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   ElementType.FIELD       → on fields/properties
 *   ElementType.METHOD      → on methods (for getter-style validation)
 *   ElementType.PARAMETER   → on method parameters
 *   ElementType.TYPE        → on classes (for class-level / cross-field constraints)
 *   ElementType.ANNOTATION_TYPE → for constraint composition (meta-annotations)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @Retention(RetentionPolicy.RUNTIME)
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   MUST be RUNTIME — Hibernate Validator reads the annotation via reflection
 *   at runtime. Without RUNTIME retention, the annotation is invisible at runtime.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @Documented
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   Includes the annotation in Javadoc output. Best practice for all custom constraints.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@interface ValidPhone {

    /*
     * Custom attributes can be added. Here we add a "format" option
     * to allow the caller to specify which phone format to accept.
     */
    PhoneFormat format() default PhoneFormat.INTERNATIONAL;

    // ── MANDATORY ATTRIBUTES (required by Bean Validation spec) ───────────────────
    String message() default "Please provide a valid phone number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    enum PhoneFormat {
        INTERNATIONAL,  // E.164: +[country code][number]
        US_FORMAT,      // (XXX) XXX-XXXX or XXX-XXX-XXXX
        ANY             // Any of the above
    }
}

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║     STEP 2: Implement PhoneNumberValidator — the validation LOGIC             ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 ConstraintValidator<A extends Annotation, T>
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * TYPE PARAMETERS:
 *   A → The annotation type this validator handles (@ValidPhone)
 *   T → The type of the element being validated (String in this case)
 *
 * TWO METHODS TO IMPLEMENT:
 *
 *   initialize(A constraintAnnotation)
 *     → Called ONCE when the validator is instantiated.
 *     → Read custom annotation attributes here (e.g., format).
 *     → Store them as instance fields for use in isValid().
 *     → Spring injects Spring beans into validators automatically!
 *
 *   isValid(T value, ConstraintValidatorContext context)
 *     → Called for EACH validation check.
 *     → Return true → value is valid.
 *     → Return false → value is invalid → constraint violation added.
 *     → ALWAYS return true if value is null — null is handled by @NotNull.
 *       This is the Bean Validation convention for null handling.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 ConstraintValidatorContext
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Allows customising the violation message at runtime:
 *
 *   context.disableDefaultConstraintViolation();
 *   context.buildConstraintViolationWithTemplate(
 *       "Phone '" + value + "' is not a valid " + format + " format"
 *   ).addConstraintViolation();
 *
 * This is powerful: you can include the actual invalid value in the error message.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
class PhoneNumberValidator implements ConstraintValidator<ValidPhone, String> {

    private ValidPhone.PhoneFormat format;

    @Override
    public void initialize(ValidPhone constraintAnnotation) {
        // Read the "format" attribute from the annotation
        this.format = constraintAnnotation.format();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        /*
         * RULE: Always return true for null values.
         * Null check is the responsibility of @NotNull, not custom constraints.
         * This follows the Bean Validation principle of orthogonal constraints.
         */
        if (value == null) {
            return true;
        }

        // Remove all whitespace for normalisation before matching
        String normalised = value.replaceAll("\\s+", "");

        boolean isValid = switch (format) {
            case INTERNATIONAL -> normalised.matches("^\\+[1-9]\\d{7,14}$");
            case US_FORMAT     -> normalised.matches("^\\(?[0-9]{3}\\)?[-.]?[0-9]{3}[-.]?[0-9]{4}$");
            case ANY           -> normalised.matches("^\\+?[1-9]\\d{7,14}$") ||
                                  normalised.matches("^\\(?[0-9]{3}\\)?[-.]?[0-9]{3}[-.]?[0-9]{4}$");
        };

        if (!isValid) {
            // Customise the violation message to include the invalid value and expected format
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "'" + value + "' is not a valid " + format.name().replace('_', ' ').toLowerCase() +
                " phone number. Expected format: " + getFormatExample(format)
            ).addConstraintViolation();
        }

        return isValid;
    }

    private String getFormatExample(ValidPhone.PhoneFormat format) {
        return switch (format) {
            case INTERNATIONAL -> "+12025551234";
            case US_FORMAT     -> "(202) 555-1234 or 202-555-1234";
            case ANY           -> "+12025551234 or (202) 555-1234";
        };
    }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART B — CROSS-FIELD CONSTRAINT: @PasswordMatch
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║     CROSS-FIELD VALIDATION — Comparing multiple fields on the same object    ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 CLASS-LEVEL CONSTRAINT
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT IS:
 *   A constraint placed on the CLASS ITSELF (not a field).
 *   The ConstraintValidator receives the ENTIRE OBJECT, allowing access to ALL fields.
 *
 * USE CASES:
 *   → password == confirmPassword
 *   → startDate < endDate
 *   → if paymentMethod == CREDIT_CARD then cardNumber is required
 *   → total == sum of all line items
 *
 * @Target({ElementType.TYPE}) — makes it applicable to classes.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 FIELD vs errorField — WHERE THE ERROR IS REPORTED
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * By default, class-level constraint violations have NO field path (they're global).
 * In BindingResult, they appear under getGlobalErrors(), not getFieldErrors().
 *
 * To make the error appear on a SPECIFIC FIELD (better UX for forms):
 *
 *   context.disableDefaultConstraintViolation();
 *   context.buildConstraintViolationWithTemplate(message)
 *          .addPropertyNode("confirmPassword")   ← points to this field
 *          .addConstraintViolation();
 *
 * This makes the error appear as a field error on "confirmPassword",
 * which the frontend can display next to the confirmPassword input.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Documented
@Constraint(validatedBy = PasswordMatchValidator.class)
@Target(ElementType.TYPE)                 // Applied to the CLASS, not a field
@Retention(RetentionPolicy.RUNTIME)
@interface PasswordMatch {
    String message() default "Passwords must match";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /*
     * These attributes let the caller specify which fields to compare.
     * Makes the constraint reusable for any pair of fields, not just "password".
     */
    String firstField()  default "password";
    String secondField() default "confirmPassword";
}

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║    PasswordMatchValidator — Reads the ENTIRE object, compares two fields     ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, Object> {

    private String firstField;
    private String secondField;
    private String message;

    @Override
    public void initialize(PasswordMatch annotation) {
        this.firstField  = annotation.firstField();
        this.secondField = annotation.secondField();
        this.message     = annotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;

        try {
            // Use reflection to read field values by name
            java.lang.reflect.Field f1 = value.getClass().getDeclaredField(firstField);
            java.lang.reflect.Field f2 = value.getClass().getDeclaredField(secondField);
            f1.setAccessible(true);
            f2.setAccessible(true);

            Object v1 = f1.get(value);
            Object v2 = f2.get(value);

            if (v1 == null && v2 == null) return true;
            if (v1 == null || !v1.equals(v2)) {
                // Report the error on the SECOND field (confirmPassword)
                // so the UI knows exactly which field to highlight
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message)
                       .addPropertyNode(secondField)
                       .addConstraintViolation();
                return false;
            }
            return true;

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ValidationException(
                "Fields '" + firstField + "' or '" + secondField +
                "' not found on class: " + value.getClass().getSimpleName(), e
            );
        }
    }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART C — CONSTRAINT COMPOSITION
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║     CONSTRAINT COMPOSITION — Build reusable meta-constraints                 ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 WHAT IS COMPOSITION?
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * A composed constraint is a new annotation that BUNDLES multiple existing constraints.
 * Instead of writing @NotBlank @Size @Pattern on every password field everywhere,
 * you write @StrongPassword once and get all three applied.
 *
 * @Constraint(validatedBy = {}) — empty validatedBy means:
 *   No custom validator class. Validation is done entirely by the COMPOSED constraints.
 *
 * @ReportAsSingleViolation (optional):
 *   Without it: ALL inner constraint violations are reported separately.
 *   With it:    If ANY inner constraint fails, only the COMPOSED constraint's
 *               message is reported (single error instead of multiple).
 *
 * @OverridesAttribute (optional):
 *   Lets you pass through an attribute to an inner constraint.
 *   Example: @OverridesAttribute(constraint = Size.class, name = "min")
 *            allows you to customise the inner @Size's min from the outer annotation.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */

/**
 * @StrongPassword — composed constraint for password validation.
 * Combines @NotBlank + @Size + @Pattern into a single reusable annotation.
 */
@NotBlank(message = "Password is required")
@Size(min = 8, max = 100,
      message = "Password must be between 8 and 100 characters")
@Pattern(
    regexp  = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
    message = "Password must contain at least: 1 uppercase letter, 1 lowercase letter, 1 digit, 1 special character (@$!%*?&)"
)
@Documented
@Constraint(validatedBy = {})             // No custom validator — composed only
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
// @ReportAsSingleViolation              // Uncomment to report only ONE error message total
@interface StrongPassword {
    String message() default "Password must be 8–100 characters with uppercase, lowercase, digit, and special character";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

/**
 * @ValidAge — composed constraint for age validation.
 * Combines @NotNull + @Min + @Max into a domain-specific annotation.
 * Makes intent clear: "this field is a person's age".
 */
@NotNull(message = "Age is required")
@Min(value = 0,   message = "Age cannot be negative")
@Max(value = 150, message = "Age must be realistic (max 150)")
@Documented
@Constraint(validatedBy = {})
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@interface ValidAge {
    String message() default "Age must be between 0 and 150";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

/**
 * @ValidPrice — composed constraint for price/monetary amount.
 * Combines @NotNull + @DecimalMin + @Digits.
 */
@NotNull(message = "Price is required")
@DecimalMin(value = "0.01", inclusive = true, message = "Price must be at least 0.01")
@DecimalMax(value = "9999999.99", inclusive = true, message = "Price cannot exceed 9,999,999.99")
@Digits(integer = 7, fraction = 2, message = "Price must have at most 7 integer digits and 2 decimal places")
@Documented
@Constraint(validatedBy = {})
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@interface ValidPrice {
    String message() default "Please provide a valid price (0.01 – 9,999,999.99)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART D — DOMAIN OBJECTS USING ALL CUSTOM CONSTRAINTS
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║   RegisterRequest — Uses @PasswordMatch (cross-field) + custom constraints   ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
@PasswordMatch(
    message      = "The passwords you entered do not match",
    firstField   = "password",
    secondField  = "confirmPassword"
)
class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,49}$",
             message = "Username must start with a letter and contain only letters, digits, or underscores")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    /*
     * @StrongPassword applies THREE inner constraints simultaneously:
     *   @NotBlank → must not be blank
     *   @Size(min=8, max=100) → length check
     *   @Pattern → complexity check (uppercase, lowercase, digit, special char)
     *
     * Much cleaner than repeating all three everywhere.
     */
    @StrongPassword
    private String password;

    @StrongPassword  // Same composed constraint on confirmPassword for standalone validation
    private String confirmPassword;
    // PLUS: @PasswordMatch on the CLASS compares password == confirmPassword

    @ValidAge
    private Integer age;

    @ValidPhone(format = ValidPhone.PhoneFormat.INTERNATIONAL,
                message = "Please provide an international phone number (e.g., +12025551234)")
    private String phoneNumber;

    @Past(message = "Date of birth must be in the past")
    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @AssertTrue(message = "You must accept the terms and conditions to register")
    private boolean termsAccepted;

    // ── Getters & Setters ─────────────────────────────────────────────────────────
    public String getUsername()       { return username; }
    public void   setUsername(String u) { this.username = u; }
    public String getEmail()          { return email; }
    public void   setEmail(String e)  { this.email = e; }
    public String getPassword()       { return password; }
    public void   setPassword(String p) { this.password = p; }
    public String getConfirmPassword(){ return confirmPassword; }
    public void   setConfirmPassword(String c) { this.confirmPassword = c; }
    public Integer getAge()           { return age; }
    public void   setAge(Integer a)   { this.age = a; }
    public String getPhoneNumber()    { return phoneNumber; }
    public void   setPhoneNumber(String p) { this.phoneNumber = p; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void   setDateOfBirth(LocalDate d) { this.dateOfBirth = d; }
    public boolean isTermsAccepted()  { return termsAccepted; }
    public void   setTermsAccepted(boolean t) { this.termsAccepted = t; }
}

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║   ProductListingRequest — Uses @ValidPrice composition                       ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
class ProductListingRequest {

    @NotBlank(message = "Product title is required")
    @Size(min = 3, max = 300, message = "Product title must be 3–300 characters")
    private String title;

    @ValidPrice   // ← One annotation does the work of three (@NotNull + @DecimalMin + @Digits)
    private BigDecimal price;

    @ValidPrice
    private BigDecimal compareAtPrice;  // Original price before discount

    @ValidPhone(format = ValidPhone.PhoneFormat.ANY,
                message = "Seller phone number is invalid")
    private String sellerPhone;

    @NotNull(message = "Listing active status is required")
    private Boolean active;

    // Getters & Setters
    public String     getTitle()             { return title; }
    public void       setTitle(String t)     { this.title = t; }
    public BigDecimal getPrice()             { return price; }
    public void       setPrice(BigDecimal p) { this.price = p; }
    public BigDecimal getCompareAtPrice()    { return compareAtPrice; }
    public void       setCompareAtPrice(BigDecimal c) { this.compareAtPrice = c; }
    public String     getSellerPhone()       { return sellerPhone; }
    public void       setSellerPhone(String s) { this.sellerPhone = s; }
    public Boolean    isActive()             { return active; }
    public void       setActive(Boolean a)   { this.active = a; }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART E — PROGRAMMATIC VALIDATION (using Validator directly)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║     PROGRAMMATIC VALIDATION — Validate objects without Spring MVC            ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 Validator  (jakarta.validation.Validator)
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT IS:
 *   The core Bean Validation API for validating objects programmatically.
 *   Useful when you need to validate outside of a controller
 *   (e.g., in batch jobs, Kafka consumers, scheduled tasks, unit tests).
 *
 * HOW TO GET A Validator:
 *   Option 1: Spring auto-configures it — inject @Autowired Validator validator
 *   Option 2: Build manually: Validation.buildDefaultValidatorFactory().getValidator()
 *
 * METHODS:
 *   validate(T object, Class<?>... groups)
 *     → Returns Set<ConstraintViolation<T>> — all violations
 *     → Empty set means VALID
 *
 *   validateProperty(T object, String propertyName, Class<?>... groups)
 *     → Validates a single property by name
 *
 *   validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups)
 *     → Validates a value BEFORE setting it on an object
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
class ProgrammaticValidationExample {

    public static void demonstrateProgrammaticValidation() {
        // Build a Validator manually (in Spring context, use @Autowired instead)
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        // ── Example 1: Valid object ───────────────────────────────────────────────
        RegisterRequest validRequest = new RegisterRequest();
        validRequest.setUsername("john_doe");
        validRequest.setEmail("john@example.com");
        validRequest.setPassword("SecretP@ss1");
        validRequest.setConfirmPassword("SecretP@ss1");
        validRequest.setAge(25);
        validRequest.setPhoneNumber("+12025551234");
        validRequest.setDateOfBirth(LocalDate.of(2000, 1, 15));
        validRequest.setTermsAccepted(true);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(validRequest);
        System.out.println("Valid request violations: " + violations.size()); // → 0

        // ── Example 2: Invalid object ─────────────────────────────────────────────
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setUsername("x");              // Too short
        invalidRequest.setEmail("not-an-email");      // Invalid email
        invalidRequest.setPassword("weak");           // Not strong
        invalidRequest.setConfirmPassword("different"); // Doesn't match
        invalidRequest.setAge(-5);                    // Negative age
        invalidRequest.setTermsAccepted(false);       // Not accepted

        Set<ConstraintViolation<RegisterRequest>> invalidViolations = validator.validate(invalidRequest);
        System.out.println("\nInvalid request violations: " + invalidViolations.size());
        for (ConstraintViolation<RegisterRequest> v : invalidViolations) {
            System.out.printf("  Field: %-25s  Message: %s%n",
                v.getPropertyPath(), v.getMessage());
        }

        // ── Example 3: Validate a single property ────────────────────────────────
        Set<ConstraintViolation<RegisterRequest>> emailViolations =
            validator.validateProperty(invalidRequest, "email");
        System.out.println("\nEmail field violations: " + emailViolations.size());

        // ── Example 4: Validate a value before setting it ────────────────────────
        Set<ConstraintViolation<RegisterRequest>> valueViolations =
            validator.validateValue(RegisterRequest.class, "username", "ab");
        System.out.println("Value 'ab' violates username constraints: " + !valueViolations.isEmpty());

        factory.close();
    }
}


/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📚 LEARNING SUMMARY 📚
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * ✅ WHAT WE LEARNED IN EXAMPLE 04:
 *
 *  CONCEPT                     RULE / KEY POINT
 *  ─────────────────────────   ──────────────────────────────────────────────────────────
 *  Custom Constraint           3 steps: annotation + ConstraintValidator + application
 *  @Constraint                 Links annotation to its ConstraintValidator implementation
 *  @Target / @Retention        MUST have RUNTIME retention; set targets to FIELD + PARAMETER
 *  ConstraintValidator         initialize() reads annotation attrs; isValid() does the check
 *  Return true for null        Custom validators should return true for null (let @NotNull handle it)
 *  ConstraintValidatorContext  Customise the violation message at runtime; point to specific field
 *
 *  Cross-field constraint      Apply @Constraint to a CLASS-LEVEL annotation (@Target TYPE)
 *                              Validator receives the ENTIRE object, can read any field
 *                              Use addPropertyNode() to report error on a specific field
 *
 *  Constraint Composition      Stack existing constraints as meta-annotations on a new annotation
 *                              @Constraint(validatedBy = {}) — no custom validator needed
 *                              @ReportAsSingleViolation → report one error total (optional)
 *                              @OverridesAttribute → expose inner constraint attributes (advanced)
 *
 *  Programmatic Validation     Use jakarta.validation.Validator directly (no Spring needed)
 *                              validate() → all violations; validateProperty() → one field
 *                              In Spring: inject Validator bean via @Autowired
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🧪 EXERCISES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. Create @ValidIBAN constraint: validates IBAN format (2 uppercase letters + 2 digits + up to 30 alphanumeric).
 *  2. Create @DateRange class-level constraint: validates startDate < endDate.
 *  3. Create @ValidCreditCard using composition: @NotBlank + @Pattern(Luhn format) + @Size(13,19).
 *  4. Extend @ValidPhone to support an "allowedCountryCodes" list (e.g., only US and UK numbers).
 *  5. Write a unit test using Validator directly to verify your custom constraints work.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ➡️  NEXT: HowItWorksExplained.java — Bean Validation internal mechanics
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
class Example04CustomConstraints {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║   CHAPTER 7 — EXAMPLE 04: Custom Constraints                     ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  @ValidPhone     → Custom field-level constraint + ConstraintValidator");
        System.out.println("  @PasswordMatch  → Cross-field (class-level) constraint");
        System.out.println("  @StrongPassword → Composed constraint (@NotBlank + @Size + @Pattern)");
        System.out.println("  @ValidAge       → Composed constraint (@NotNull + @Min + @Max)");
        System.out.println("  @ValidPrice     → Composed constraint (@NotNull + @DecimalMin + @Digits)");
        System.out.println("  Programmatic    → Validator.validate() outside of Spring MVC");
        System.out.println();
        System.out.println("Demonstrating programmatic validation...");
        System.out.println();
        ProgrammaticValidationExample.demonstrateProgrammaticValidation();
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

