package com.learning.springboot.chapter07;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║             EXAMPLE 03: @Valid, @Validated, GROUPS & COMPLETE REST WIRING            ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example03ValidAndValidated.java
 * Purpose:     Demonstrate:
 *               - @Valid for cascade validation (nested objects)
 *               - @Validated with validation groups (create vs update)
 *               - @Validated at class level for method-level validation in @Service
 *               - Complete REST controller with global validation error handler
 *               - MethodArgumentNotValidException handling
 *               - ConstraintViolationException handling
 * Difficulty:  ⭐⭐⭐ Intermediate
 * Time:        60–90 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART A — VALIDATION GROUPS (Marker Interfaces)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║               VALIDATION GROUPS — MARKER INTERFACES                          ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 WHAT ARE VALIDATION GROUPS?
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Groups are empty MARKER INTERFACES used as labels for constraints.
 * You assign constraints to groups via the "groups" attribute.
 * You activate specific groups via @Validated(GroupInterface.class).
 *
 * WHY USEFUL?
 *   The SAME DTO class is often used for multiple operations:
 *   - CREATE: id should be null (DB generates it), name is required
 *   - UPDATE: id is required (which record?), name may be optional
 *
 *   Without groups: you'd need two separate DTO classes (CreateUserDto, UpdateUserDto).
 *   With groups:    one DTO with group-tagged constraints.
 *
 * RULE: Groups are just empty interfaces — they act as "labels", not types.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
class ValidationGroups {

    /**
     * Applied when CREATING a new resource (POST requests).
     * Rules: id must be null, all required fields must be provided.
     */
    public interface OnCreate {}

    /**
     * Applied when UPDATING an existing resource (PUT requests).
     * Rules: id must be present, only provided fields validated.
     */
    public interface OnUpdate {}

    /**
     * Applied when partially updating (PATCH requests).
     * Rules: id is required; other fields are optional.
     */
    public interface OnPatch {}
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART B — NESTED OBJECTS WITH @Valid (cascade validation)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║        ADDRESS — Embeddable nested object (will be validated via @Valid)      ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
class Address {

    /*
     * Constraints on Address fields are only checked when a parent object
     * has @Valid on the Address field. Without @Valid on the parent, these
     * constraints are COMPLETELY IGNORED.
     */
    @NotBlank(message = "Street address is required")
    @Size(max = 200, message = "Street address cannot exceed 200 characters")
    private String street;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City name must be 2–100 characters")
    private String city;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 100, message = "Country must be 2–100 characters")
    private String country;

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "^[A-Z0-9 \\-]{3,10}$",
             flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "Please provide a valid postal code")
    private String postalCode;

    public Address() {}
    public Address(String street, String city, String country, String postalCode) {
        this.street     = street;
        this.city       = city;
        this.country    = country;
        this.postalCode = postalCode;
    }

    public String getStreet()     { return street; }
    public void   setStreet(String s) { this.street = s; }
    public String getCity()       { return city; }
    public void   setCity(String c) { this.city = c; }
    public String getCountry()    { return country; }
    public void   setCountry(String c) { this.country = c; }
    public String getPostalCode() { return postalCode; }
    public void   setPostalCode(String p) { this.postalCode = p; }
}

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║        OrderItemRequest — Another nested object                              ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
class OrderItemRequest {

    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be positive")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 999, message = "Quantity cannot exceed 999 per item")
    private Integer quantity;

    @DecimalMin(value = "0.00", inclusive = true, message = "Unit price cannot be negative")
    private BigDecimal overrideUnitPrice;  // Optional price override

    public OrderItemRequest() {}
    public OrderItemRequest(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity  = quantity;
    }

    public Long       getProductId()           { return productId; }
    public void       setProductId(Long p)     { this.productId = p; }
    public Integer    getQuantity()            { return quantity; }
    public void       setQuantity(Integer q)   { this.quantity = q; }
    public BigDecimal getOverrideUnitPrice()   { return overrideUnitPrice; }
    public void       setOverrideUnitPrice(BigDecimal p) { this.overrideUnitPrice = p; }
}


/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║       CreateOrderRequest — Demonstrates @Valid cascade validation             ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @Valid on nested object fields — CASCADE VALIDATION
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT DOES:
 *   When placed on a field that is itself an object with constraints,
 *   @Valid tells the validator: "Also validate the constraints INSIDE this object."
 *
 * WITHOUT @Valid on the shippingAddress field:
 *   CreateOrderRequest is validated, but Address constraints (@NotBlank street, etc.)
 *   are COMPLETELY IGNORED. You'd get no validation errors for bad address data.
 *
 * WITH @Valid on the shippingAddress field:
 *   The validator recurses into Address and validates all its constraints too.
 *   Violations from nested objects are included in the same BindingResult.
 *   Field names in errors: "shippingAddress.street", "shippingAddress.city", etc.
 *
 * PACKAGE: jakarta.validation.Valid (NOT Spring's @Validated)
 *
 * ALSO WORKS ON:
 *   → Lists: @Valid List<OrderItemRequest> items
 *     → Validates EACH ELEMENT in the list
 *   → Optional: @Valid Optional<Address>
 *   → Map values: validates values (not keys)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
class CreateOrderRequest {

    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID must be positive")
    private Long customerId;

    /*
     * @Valid → CASCADE into Address and validate all its constraints.
     * @NotNull → The address field itself must not be null.
     * Both are needed: @NotNull stops null, @Valid validates inner fields.
     */
    @NotNull(message = "Shipping address is required")
    @Valid
    private Address shippingAddress;

    /*
     * @Valid on a List → validates EACH OrderItemRequest in the list.
     * @NotEmpty → the list must not be empty.
     * @Size → between 1 and 20 items per order.
     *
     * Error field names in violations: "items[0].productId", "items[1].quantity", etc.
     */
    @NotEmpty(message = "Order must have at least one item")
    @Size(max = 20, message = "A single order cannot exceed 20 line items")
    @Valid
    private List<OrderItemRequest> items;

    @NotBlank(message = "Order reference is required")
    @Pattern(regexp = "^ORD-[0-9]{8}$",
             message = "Order reference must be in format ORD-XXXXXXXX (8 digits)")
    private String orderReference;

    @FutureOrPresent(message = "Requested delivery date must be today or in the future")
    private java.time.LocalDate requestedDeliveryDate;

    // ── Getters & Setters ─────────────────────────────────────────────────────────
    public Long getCustomerId()              { return customerId; }
    public void setCustomerId(Long c)        { this.customerId = c; }
    public Address getShippingAddress()      { return shippingAddress; }
    public void setShippingAddress(Address a){ this.shippingAddress = a; }
    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> i) { this.items = i; }
    public String getOrderReference()        { return orderReference; }
    public void setOrderReference(String r)  { this.orderReference = r; }
    public java.time.LocalDate getRequestedDeliveryDate() { return requestedDeliveryDate; }
    public void setRequestedDeliveryDate(java.time.LocalDate d) { this.requestedDeliveryDate = d; }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART C — VALIDATION GROUPS WITH @Validated
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║       UserRequest — One DTO for Create AND Update using groups               ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 VALIDATION GROUPS IN ACTION
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Each constraint has a "groups" attribute that specifies WHEN it is active.
 * If "groups" is not specified, the constraint belongs to the DEFAULT group.
 * @Valid activates only the DEFAULT group.
 * @Validated(OnCreate.class) activates ONLY the OnCreate group.
 *
 * FLOW:
 *   POST /users  → @Validated(OnCreate.class) → checks constraints with groups = OnCreate.class
 *   PUT  /users/{id} → @Validated(OnUpdate.class) → checks constraints with groups = OnUpdate.class
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
class UserRequest {

    /*
     * id rules:
     *   OnCreate → must be null (client sends no id; DB generates it)
     *   OnUpdate → must not be null and must be positive (which record to update?)
     *
     * Without groups these would conflict. With groups, they apply in different contexts.
     */
    @Null(groups = ValidationGroups.OnCreate.class,
          message = "ID must not be provided when creating a new user — it is auto-generated")
    @NotNull(groups = ValidationGroups.OnUpdate.class,
             message = "ID is required for updating a user")
    @Positive(groups = ValidationGroups.OnUpdate.class,
              message = "ID must be positive")
    private Long id;

    /*
     * username:
     *   OnCreate → required (new users must provide a username)
     *   OnUpdate → optional (user may not want to change username)
     *   No groups specified for @Size → it's in DEFAULT group (ignored by OnCreate/OnUpdate alone)
     *
     * SOLUTION: Assign @Size to BOTH groups too
     */
    @NotBlank(groups = ValidationGroups.OnCreate.class,
              message = "Username is required when creating a new user")
    @Size(min = 3, max = 50,
          groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
          message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_\\-]{2,49}$",
             groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
             message = "Username must start with a letter and contain only letters, digits, _ or -")
    private String username;

    @NotBlank(groups = ValidationGroups.OnCreate.class,
              message = "Email is required when creating a new user")
    @Email(groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
           message = "Please provide a valid email address")
    private String email;

    /*
     * password:
     *   OnCreate → required (new users must set a password)
     *   OnUpdate → optional (only validate if provided, not null)
     */
    @NotBlank(groups = ValidationGroups.OnCreate.class,
              message = "Password is required when creating a new user")
    @Size(min = 8, max = 100,
          groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
          message = "Password must be 8–100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
             groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class},
             message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit")
    private String password;

    /*
     * Nested Address — validated with @Valid
     * @Valid always cascades into Address regardless of group.
     * @NotNull is in the DEFAULT group here — it applies when no specific group is set.
     * For group-specific cascade: declare groups in the @NotNull and @Valid field annotations.
     */
    @NotNull(groups = ValidationGroups.OnCreate.class,
             message = "Address is required when creating a new user")
    @Valid
    private Address address;

    // ── Getters & Setters ─────────────────────────────────────────────────────────
    public Long    getId()         { return id; }
    public void    setId(Long i)   { this.id = i; }
    public String  getUsername()   { return username; }
    public void    setUsername(String u) { this.username = u; }
    public String  getEmail()      { return email; }
    public void    setEmail(String e) { this.email = e; }
    public String  getPassword()   { return password; }
    public void    setPassword(String p) { this.password = p; }
    public Address getAddress()    { return address; }
    public void    setAddress(Address a) { this.address = a; }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART D — VALIDATION ERROR RESPONSE DTO
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║       ValidationErrorResponse — Structured error response for clients        ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
class ValidationErrorResponse {

    private final int    status;
    private final String message;
    private final String timestamp;
    private final Map<String, String> fieldErrors;   // field → first error message
    private final List<String>        globalErrors;  // class-level constraint violations

    public ValidationErrorResponse(int status, String message,
                                   Map<String, String> fieldErrors,
                                   List<String> globalErrors) {
        this.status       = status;
        this.message      = message;
        this.timestamp    = LocalDateTime.now().toString();
        this.fieldErrors  = fieldErrors;
        this.globalErrors = globalErrors;
    }

    public int             getStatus()       { return status; }
    public String          getMessage()      { return message; }
    public String          getTimestamp()    { return timestamp; }
    public Map<String, String> getFieldErrors()  { return fieldErrors; }
    public List<String>    getGlobalErrors() { return globalErrors; }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART E — GLOBAL VALIDATION EXCEPTION HANDLER
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║        @RestControllerAdvice — Global Validation Error Handler               ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 VALIDATION ERROR TYPES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  MethodArgumentNotValidException:
 *    → Thrown when @Valid / @Validated fails on a @RequestBody parameter
 *    → Contains BindingResult with field errors and global errors
 *    → HTTP status 400 Bad Request by default
 *
 *  ConstraintViolationException:
 *    → Thrown when @Validated fails on @RequestParam, @PathVariable, @RequestHeader
 *    → Or when method-level validation fails in a @Service/@Validated class
 *    → Contains Set<ConstraintViolation<?>> with violation details
 *    → Does NOT automatically return 400 — must be handled explicitly
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@RestControllerAdvice
class GlobalValidationExceptionHandler {

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 Handle @RequestBody validation failures (@Valid / @Validated on body param)
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT CATCHES:
     *   Any @Valid / @Validated failure on @RequestBody parameters.
     *
     * HOW TO EXTRACT ERRORS:
     *   ex.getBindingResult().getFieldErrors()  → per-field violations
     *   ex.getBindingResult().getGlobalErrors() → class-level violations (cross-field)
     *
     * RESPONSE FORMAT:
     *   HTTP 400 Bad Request with a JSON body listing all errors.
     *   Return ALL errors at once (not just the first one) — better UX.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        // Collect per-field errors: field name → first violation message
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // If multiple violations on same field, keep the first one.
            // You could also collect all messages in a List<String>.
            fieldErrors.putIfAbsent(
                fieldError.getField(),
                fieldError.getDefaultMessage()
            );
        }

        // Collect class-level (global) errors — from cross-field constraints
        List<String> globalErrors = new ArrayList<>();
        ex.getBindingResult().getGlobalErrors().forEach(error ->
            globalErrors.add(error.getDefaultMessage())
        );

        ValidationErrorResponse response = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed — please review the errors and correct your request",
            fieldErrors,
            globalErrors
        );

        return ResponseEntity.badRequest().body(response);
    }

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 Handle @RequestParam / @PathVariable / method-level validation failures
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT CATCHES:
     *   - @Validated failures on @RequestParam, @PathVariable parameters
     *   - Method-level validation failures in @Service beans
     *
     * HOW TO EXTRACT:
     *   ex.getConstraintViolations() → Set<ConstraintViolation<?>>
     *   Each violation has: propertyPath (field path), getMessage() (error message),
     *   getInvalidValue() (the value that failed), getRootBean() (the object)
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolations(
            jakarta.validation.ConstraintViolationException ex) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (jakarta.validation.ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            // propertyPath is like "createUser.request.email" — take last segment
            String path = violation.getPropertyPath().toString();
            String field = path.contains(".")
                ? path.substring(path.lastIndexOf('.') + 1)
                : path;
            fieldErrors.putIfAbsent(field, violation.getMessage());
        }

        ValidationErrorResponse response = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Request parameter validation failed",
            fieldErrors,
            Collections.emptyList()
        );

        return ResponseEntity.badRequest().body(response);
    }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART F — COMPLETE REST CONTROLLER with all validation patterns
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║        UserController — Complete REST controller with @Valid / @Validated    ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ENDPOINT REFERENCE:
 *
 *   POST   /api/v1/users                → Create user  (@Validated(OnCreate))
 *   PUT    /api/v1/users/{id}           → Update user  (@Validated(OnUpdate))
 *   GET    /api/v1/users/{id}           → Get by ID    (@PathVariable + @Positive)
 *   GET    /api/v1/users?email=...      → Find by email (@RequestParam + @Email)
 *   POST   /api/v1/orders              → Create order  (@Valid cascade validation)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/v1")
@Validated  // ← Enables @RequestParam / @PathVariable validation (ConstraintViolationException)
class UserController {

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Validated(OnCreate.class) on @RequestBody
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT HAPPENS:
     *   1. Client sends POST /api/v1/users with JSON body
     *   2. Jackson deserialises JSON → UserRequest object
     *   3. @Validated(OnCreate.class) tells Spring: validate with OnCreate group
     *   4. Only constraints with groups = OnCreate.class are checked
     *   5. If violations: MethodArgumentNotValidException thrown
     *   6. GlobalValidationExceptionHandler returns 400 with error details
     *   7. If no violations: method body executes
     *
     * WHY NOT @Valid HERE?
     *   @Valid only validates the DEFAULT group.
     *   Our OnCreate-specific constraints (like @Null(groups=OnCreate.class))
     *   would NOT be activated by @Valid. We need @Validated(OnCreate.class).
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createUser(
            @Validated(ValidationGroups.OnCreate.class) @RequestBody UserRequest request) {

        // Validation passed! Focus on business logic.
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "User created successfully");
        response.put("username", request.getUsername());
        response.put("email", request.getEmail());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Validated(OnUpdate.class) on @RequestBody + @PathVariable
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT HAPPENS:
     *   1. @Validated(OnUpdate.class) → validates OnUpdate group constraints
     *   2. @PathVariable @Positive → validates path variable (class-level @Validated
     *      on the controller makes @Positive on @PathVariable work)
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(
            @PathVariable @Positive(message = "User ID must be positive") Long id,
            @Validated(ValidationGroups.OnUpdate.class) @RequestBody UserRequest request) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "User " + id + " updated successfully");
        response.put("updatedFields", List.of("username", "email"));
        return response;
    }

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Positive on @PathVariable — validate path parameters
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * REQUIRES: @Validated at class level on the controller.
     * Without class-level @Validated, constraint annotations on method parameters
     * (other than @RequestBody) are IGNORED.
     *
     * On violation: ConstraintViolationException → handled by handleConstraintViolations()
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @GetMapping("/users/{id}")
    public Map<String, Object> getUserById(
            @PathVariable @NotNull @Positive(message = "User ID must be a positive number") Long id) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", id);
        response.put("username", "john_doe");
        response.put("email", "john@example.com");
        return response;
    }

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Email on @RequestParam — validate query parameters
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * ALSO REQUIRES: class-level @Validated on the controller.
     * Constraint annotations on @RequestParam parameters work only with @Validated.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @GetMapping("/users")
    public Map<String, Object> findByEmail(
            @RequestParam
            @NotBlank(message = "Email query parameter is required")
            @Email(message = "Please provide a valid email address")
            String email) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("searchEmail", email);
        response.put("found", true);
        response.put("user", Map.of("id", 42, "username", "found_user", "email", email));
        return response;
    }

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Valid on @RequestBody — CASCADE into nested objects
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * @Valid (not @Validated) → triggers DEFAULT group + CASCADE
     * The CreateOrderRequest has:
     *   @Valid Address shippingAddress → validates Address constraints
     *   @Valid List<OrderItemRequest> items → validates each item's constraints
     *
     * All nested violations are collected and returned together.
     * Error paths: "shippingAddress.city", "items[0].quantity", etc.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Order created successfully");
        response.put("orderReference", request.getOrderReference());
        response.put("itemCount", request.getItems().size());
        response.put("shippingCity", request.getShippingAddress().getCity());
        return response;
    }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  PART G — @Validated on @Service for METHOD-LEVEL VALIDATION
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║        UserService — @Validated at class level for method validation         ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @Validated at CLASS LEVEL on @Service
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * WHAT IT DOES:
 *   Enables Spring AOP-based METHOD-LEVEL VALIDATION for ALL methods in this class.
 *   Spring creates a PROXY around the service bean.
 *   Before each method call, the proxy validates:
 *     → Input parameters (annotated with constraints like @NotNull, @Positive, @Valid)
 *     → Return values (if annotated with constraint annotations)
 *
 * ON VIOLATION:
 *   ConstraintViolationException is thrown (not MethodArgumentNotValidException).
 *   You must handle it with @ExceptionHandler(ConstraintViolationException.class).
 *
 * IMPORTANT: Works ONLY on Spring-managed beans (beans in the ApplicationContext).
 *            Does NOT work on plain Java objects created with new ServiceImpl().
 *
 * SELF-INVOCATION PROBLEM:
 *   this.findById(id) → calls the REAL object, not the proxy → validation BYPASSED!
 *   Fix: inject the service into itself via @Autowired, or restructure code.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@org.springframework.stereotype.Service
@Validated   // ← CRITICAL: enables method-level validation for this entire class
class UserValidationService {

    /*
     * Method parameter validation:
     *   @NotNull → id must not be null
     *   @Positive → id must be > 0
     *   Both are checked BEFORE the method body executes.
     *
     * Return value validation:
     *   @NotNull on the method → the return value must not be null
     */
    @NotNull(message = "findUserById must return a non-null result")
    public Map<String, Object> findUserById(
            @NotNull(message = "User ID cannot be null")
            @Positive(message = "User ID must be positive")
            Long id) {

        // At this point: id is guaranteed non-null and positive
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", id);
        user.put("username", "user_" + id);
        return user;
    }

    /*
     * @Valid on @RequestBody-style parameters in service methods:
     *   If the UserRequest passed to this method violates its constraints,
     *   ConstraintViolationException is thrown before the method executes.
     */
    public Map<String, Object> createUser(
            @NotNull(message = "User request cannot be null")
            @Valid UserRequest request) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", request.getUsername());
        result.put("created", true);
        return result;
    }

    /*
     * Demonstrating @Size on a method parameter directly
     */
    public List<Map<String, Object>> searchUsers(
            @NotBlank(message = "Search keyword cannot be blank")
            @Size(min = 2, max = 50, message = "Search keyword must be 2–50 characters")
            String keyword) {

        return List.of(
            Map.of("id", 1, "username", "found_user"),
            Map.of("id", 2, "username", "another_user")
        );
    }
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
 *  @Valid                   Jakarta standard; triggers DEFAULT group + CASCADE
 *                           Use on @RequestBody and nested object/list fields
 *                           Does NOT support validation groups
 *
 *  @Validated(Group.class)  Spring extension; triggers SPECIFIC validation group
 *                           Use when different rules apply per operation (create vs update)
 *                           On @RequestBody → MethodArgumentNotValidException
 *
 *  @Validated (class level) Enables AOP method-level validation for @Service beans
 *                           Constraint annotations work on method parameters directly
 *                           On violation → ConstraintViolationException
 *
 *  Validation Groups        Marker interfaces; assign via groups = XxxGroup.class
 *                           @Valid uses DEFAULT group; @Validated(Group) uses that group
 *                           Use @Null(groups=Create) + @NotNull(groups=Update) on same field
 *
 *  Cascade Validation       @Valid on nested object/list field recursively validates it
 *                           Without @Valid on the field: inner constraints are IGNORED!
 *                           Error field paths: "address.city", "items[0].quantity"
 *
 *  Error Handling           MethodArgumentNotValidException → @RequestBody failures → 400
 *                           ConstraintViolationException → param/service failures → 400
 *                           Use @RestControllerAdvice to centralise both
 *
 *  Class-level @Validated   Required on controller to validate @PathVariable, @RequestParam
 *                           Required on service for method-level validation
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🧪 EXERCISES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. POST /api/v1/users with an empty body {} — observe all 400 error messages.
 *  2. POST /api/v1/users with all required fields (OnCreate group) — success!
 *  3. POST /api/v1/orders with missing shippingAddress.city — see nested field error.
 *  4. GET /api/v1/users/-1 — observe @Positive violation via ConstraintViolationException.
 *  5. PUT /api/v1/users/5 with id = null in body — observe @NotNull(OnUpdate) violation.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ➡️  NEXT: Example04CustomConstraints.java
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
class Example03ValidAndValidated {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║    CHAPTER 7 — EXAMPLE 03: @Valid, @Validated, Groups            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  @Valid           → cascade + DEFAULT group (jakarta standard)");
        System.out.println("  @Validated(G)    → specific group (Spring extension)");
        System.out.println("  @Validated(cls)  → class-level: enables method validation");
        System.out.println("  ValidationGroups → OnCreate, OnUpdate, OnPatch marker interfaces");
        System.out.println("  Cascade          → @Valid on nested field/list recursively validates");
        System.out.println("  Error handling   → MethodArgumentNotValidException (body)");
        System.out.println("                     ConstraintViolationException (param/service)");
        System.out.println();
        System.out.println("Try these endpoints (start the Spring Boot app):");
        System.out.println("  POST /api/v1/users   (OnCreate group)");
        System.out.println("  PUT  /api/v1/users/1 (OnUpdate group)");
        System.out.println("  POST /api/v1/orders  (cascade @Valid)");
        System.out.println("  GET  /api/v1/users/0 (ConstraintViolationException)");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

