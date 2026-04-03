package com.learning.springboot.chapter07;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║       HOW IT WORKS: SPRING VALIDATION — INTERNAL MECHANICS                           ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        HowItWorksExplained.java
 * Purpose:     Deep dive into what happens under the hood when Spring Boot
 *              processes validation annotations — from annotation scanning to
 *              violation reporting, including Spring MVC integration, AOP proxy
 *              mechanics, Hibernate Validator internals, and message interpolation.
 * Difficulty:  ⭐⭐⭐⭐⭐ Advanced
 * Time:        60–120 minutes (read + reflect)
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │                                                                                       │
 * │          HOW SPRING VALIDATION WORKS — FROM ANNOTATION TO HTTP 400 ERROR             │
 * │                                                                                       │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 */
public class HowItWorksExplained {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 1: APPLICATION STARTUP — HOW SPRING AUTO-CONFIGURES VALIDATION      ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * When you add 'spring-boot-starter-validation' to build.gradle and start the app:
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * STEP 1: Auto-Configuration Detects Hibernate Validator
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * ValidationAutoConfiguration activates because hibernate-validator is on classpath.
     * Spring Boot creates and registers these beans automatically:
     *
     *   ┌──────────────────────────────────────────────────────────────────────────┐
     *   │  ValidatorFactory  (jakarta.validation.ValidatorFactory)                 │
     *   │    → Built from Validation.buildDefaultValidatorFactory()                │
     *   │    → Uses HibernateValidatorConfiguration                               │
     *   │    → Configured with ClockProvider (for date constraints)               │
     *   │    → Configured with TraversableResolver (for cascade)                  │
     *   │                                                                          │
     *   │  Validator  (jakarta.validation.Validator)                               │
     *   │    → The main validation engine                                          │
     *   │    → Thread-safe, reused across requests                                │
     *   │    → Inject with @Autowired Validator validator in any Spring bean       │
     *   │                                                                          │
     *   │  LocalValidatorFactoryBean  (Spring's adapter)                          │
     *   │    → Wraps Hibernate Validator                                           │
     *   │    → Integrates with Spring's MessageSource for message interpolation   │
     *   │    → Registers as both ValidatorFactory and Validator                   │
     *   │                                                                          │
     *   │  MethodValidationPostProcessor                                           │
     *   │    → Creates AOP proxies for @Validated classes                         │
     *   │    → Enables method-level validation (@Validated on @Service)           │
     *   └──────────────────────────────────────────────────────────────────────────┘
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * STEP 2: Hibernate Validator Scans Your Classes at First Use
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * The FIRST TIME a class is validated, Hibernate Validator:
     *
     *   1. Reads all fields via reflection
     *   2. Finds all constraint annotations (@NotBlank, @Email, @ValidPhone, etc.)
     *   3. For each constraint annotation:
     *      a. Reads @Constraint(validatedBy = ...) to find the validator class
     *      b. Instantiates the ConstraintValidator
     *      c. Calls validator.initialize(annotationInstance) — reads annotation attrs
     *   4. Builds a BeanDescriptor — a metadata model of the class's constraints
     *   5. CACHES the BeanDescriptor — subsequent validations of the same class
     *      reuse the cached model (no repeated reflection overhead)
     *
     * This means the SECOND CALL to validate() on the same class type is FASTER
     * than the first call, because the metadata is cached.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 2: AN HTTP REQUEST ARRIVES — COMPLETE @Valid TRACE                  ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Request: POST /api/v1/users  {"username":"","email":"bad","password":"weak"}
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * CALL SEQUENCE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  HTTP Request arrives
     *       ↓
     *  Embedded Tomcat accepts on port 8080
     *       ↓
     *  DispatcherServlet.doDispatch() is called
     *       ↓
     *  HandlerMapping finds: UserController.createUser() matches POST /api/v1/users
     *       ↓
     *  RequestResponseBodyMethodProcessor.resolveArgument() is called
     *    → Jackson: reads request body JSON → UserRequest object
     *    → {"username":"","email":"bad","password":"weak"} deserialized to:
     *         userRequest.username = ""
     *         userRequest.email    = "bad"
     *         userRequest.password = "weak"
     *       ↓
     *  Spring checks: does the parameter have @Valid or @Validated?
     *    → YES: @Validated(OnCreate.class) is present
     *       ↓
     *  Spring calls SmartValidator.validate(userRequest, bindingResult, OnCreate.class)
     *       ↓
     *  [Hibernate Validator executes]
     *  1. Retrieve BeanDescriptor for UserRequest (from cache or build first time)
     *  2. For each constraint in OnCreate group (group = OnCreate.class):
     *
     *     CONSTRAINT: @NotBlank(groups=OnCreate) on username
     *       → Username is "" (empty) → @NotBlank FAILS
     *       → ConstraintViolation added: {field="username", message="Username is required..."}
     *
     *     CONSTRAINT: @Email(groups={OnCreate,OnUpdate}) on email
     *       → "bad" is not a valid email → @Email FAILS
     *       → ConstraintViolation added: {field="email", message="Please provide a valid..."}
     *
     *     CONSTRAINT: @NotBlank(groups=OnCreate) on password
     *       → "weak" is not blank → @NotBlank PASSES
     *
     *     CONSTRAINT: @Size(min=8, groups={OnCreate,OnUpdate}) on password
     *       → "weak" has length 4, min is 8 → @Size FAILS
     *       → ConstraintViolation added: {field="password", message="Password must be..."}
     *
     *     CONSTRAINT: @Pattern(...) on password
     *       → "weak" doesn't match the regex → @Pattern FAILS
     *       → ConstraintViolation added: {field="password", message="Password must contain..."}
     *
     *     CONSTRAINT: @NotNull(groups=OnCreate) on address
     *       → address is null → @NotNull FAILS
     *       → ConstraintViolation added: {field="address", message="Address is required..."}
     *
     *  3. Total: 5 violations
     *       ↓
     *  Spring adds violations to BindingResult:
     *    bindingResult.hasErrors() → true
     *       ↓
     *  Spring throws MethodArgumentNotValidException(bindingResult)
     *       ↓
     *  [Spring AOP intercepts — @ExceptionHandler in @RestControllerAdvice]
     *  GlobalValidationExceptionHandler.handleValidationErrors(ex) is called
     *       ↓
     *  Extract errors from ex.getBindingResult().getFieldErrors()
     *  Build ValidationErrorResponse {status=400, fieldErrors={...}}
     *       ↓
     *  ResponseEntity<ValidationErrorResponse> with HTTP 400 is returned
     *       ↓
     *  Jackson serializes to JSON:
     *    {
     *      "status": 400,
     *      "message": "Validation failed — please review the errors",
     *      "fieldErrors": {
     *        "username": "Username is required when creating a new user",
     *        "email":    "Please provide a valid email address",
     *        "password": "Password must be 8–100 characters",
     *        "address":  "Address is required when creating a new user"
     *      },
     *      "globalErrors": []
     *    }
     *       ↓
     *  HTTP 400 response sent to client — createUser() method body NEVER executes
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 3: HOW CASCADE VALIDATION (@Valid on nested fields) WORKS           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Request: POST /api/v1/orders  {shippingAddress: {city: "", street: "Main St"}}
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * CASCADE VALIDATION TRACE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  validate(CreateOrderRequest, {})  [DEFAULT group, no specific group]
     *       ↓
     *  Validate top-level constraints on CreateOrderRequest:
     *    @NotNull customerId → null → FAIL: {field="customerId", msg="Customer ID is required"}
     *    @NotNull shippingAddress → not null → PASS
     *    @Valid shippingAddress → TRIGGER CASCADE ↓
     *       ↓
     *  [Hibernate detects @Valid on shippingAddress field]
     *  Recursively validate the Address object:
     *    @NotBlank street → "Main St" → PASS
     *    @NotBlank city   → "" (empty) → FAIL:
     *      {field="shippingAddress.city", msg="City is required"}
     *    @NotBlank country → null → FAIL:
     *      {field="shippingAddress.country", msg="Country is required"}
     *    @NotBlank postalCode → null → FAIL:
     *      {field="shippingAddress.postalCode", msg="Postal code is required"}
     *       ↓
     *  @NotEmpty items → null → FAIL: {field="items", msg="Order must have..."}
     *       ↓
     *  Total violations: 5 (1 top-level + 1 cascade from address + 2 null cascades)
     *
     * IMPORTANT: Field paths in violations reflect the NESTING:
     *   "shippingAddress.city" → nested field path (not just "city")
     *   "items[0].quantity"    → indexed collection element
     *
     * In BindingResult:
     *   fieldError.getField() → "shippingAddress.city"
     *   This is how the frontend knows EXACTLY which input field to highlight.
     *
     * HOW HIBERNATE KNOWS TO CASCADE:
     *   When building the BeanDescriptor, Hibernate Validator looks for
     *   @Valid on each field. If found, it marks that field for cascaded validation.
     *   Only fields marked @Valid are recursively validated.
     *   Fields without @Valid are treated as opaque (not validated internally).
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 4: HOW @Validated CLASS-LEVEL + AOP PROXY WORKS (@Service)          ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * WHAT METHODVALIDATIONPOSTPROCESSOR DOES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * When Spring scans the ApplicationContext:
     *   → Finds @Validated on UserValidationService class
     *   → Creates a CGLIB PROXY wrapping the real UserValidationService
     *   → The proxy intercepts ALL method calls
     *
     * The proxy for UserValidationService looks like (conceptually):
     *
     *   class UserValidationServiceProxy extends UserValidationService {
     *
     *       @Override
     *       public Map<String, Object> findUserById(Long id) {
     *
     *           // BEFORE the method:
     *           // Validate all parameters with their constraints
     *           Set<ConstraintViolation<Object>> violations =
     *               executableValidator.validateParameters(
     *                   realService,
     *                   findUserByIdMethod,
     *                   new Object[]{id}
     *               );
     *
     *           if (!violations.isEmpty()) {
     *               throw new ConstraintViolationException(violations);
     *           }
     *
     *           // YOUR ACTUAL METHOD EXECUTES:
     *           Map<String, Object> result = super.findUserById(id);
     *
     *           // AFTER the method:
     *           // Validate return value (if annotated with constraints)
     *           violations = executableValidator.validateReturnValue(
     *               realService, findUserByIdMethod, result
     *           );
     *
     *           if (!violations.isEmpty()) {
     *               throw new ConstraintViolationException(violations);
     *           }
     *
     *           return result;
     *       }
     *   }
     *
     * IMPORTANT DIFFERENCE:
     *   Controller @Valid → MethodArgumentNotValidException (Spring MVC specific)
     *   Service @Validated → ConstraintViolationException (standard Bean Validation)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * THE SELF-INVOCATION PROBLEM:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @Service @Validated
     *   class UserValidationService {
     *       public void outerMethod() {
     *           this.findUserById(null);  // DIRECTLY calls real object, BYPASSES PROXY!
     *                                     // @NotNull on id is IGNORED!
     *       }
     *
     *       public Map<String, Object> findUserById(@NotNull Long id) {
     *           // ...
     *       }
     *   }
     *
     * WHY: "this" refers to the REAL object, not the CGLIB proxy.
     *      The proxy only intercepts calls coming FROM OUTSIDE the object.
     *      Internal calls go directly to the real object, bypassing the proxy.
     *
     * FIX OPTIONS:
     *   1. Inject the service into itself: @Autowired UserValidationService self;
     *      then: self.findUserById(null); → goes through proxy → validation fires
     *
     *   2. Move findUserById() to a separate service class → inject it.
     *
     *   3. Use AspectJ load-time weaving (more complex — not typical in Spring Boot).
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 5: MESSAGE INTERPOLATION — HOW ERROR MESSAGES ARE RESOLVED          ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * DEFAULT MESSAGES: ValidationMessages.properties
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Hibernate Validator bundles a default message file. Each constraint has a key:
     *
     *   jakarta.validation.constraints.NotBlank.message  = must not be blank
     *   jakarta.validation.constraints.Email.message     = must be a well-formed email address
     *   jakarta.validation.constraints.Size.message      = size must be between {min} and {max}
     *   jakarta.validation.constraints.Min.message       = must be greater than or equal to {value}
     *   jakarta.validation.constraints.NotNull.message   = must not be null
     *
     * PLACEHOLDER RESOLUTION:
     *   {min}, {max}, {value} → Replaced with annotation attribute values at runtime.
     *
     *   @Size(min = 3, max = 50)
     *   → "size must be between {min} and {max}" → "size must be between 3 and 50"
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * CUSTOM MESSAGES: Override in ValidationMessages.properties
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Create src/main/resources/ValidationMessages.properties:
     *
     *   jakarta.validation.constraints.NotBlank.message  = This field cannot be empty
     *   jakarta.validation.constraints.Email.message     = Enter a valid email address
     *   com.example.ValidPhone.message                   = Invalid phone number format
     *
     * OR override per-field in the annotation:
     *   @NotBlank(message = "Your custom message here")
     *
     * OR reference a message key:
     *   @NotBlank(message = "{user.name.required}")
     *   Then define: user.name.required=The name field is required (from messages file)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * SPRING INTEGRATION: MessageSource
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * LocalValidatorFactoryBean integrates with Spring's MessageSource.
     * If a key like "{user.name.required}" is not found in ValidationMessages.properties,
     * Spring falls back to the MessageSource (messages.properties).
     * This allows validation messages to be:
     *   → Internationalised (i18n) for different locales
     *   → Centralised in the application's message files
     *   → Translated for different languages (messages_fr.properties, messages_de.properties)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * INTERPOLATION ORDER:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   1. Inline message literal:   @NotBlank(message = "Name is required")
     *      → Used directly, no lookup.
     *
     *   2. Message key reference:    @NotBlank(message = "{name.required}")
     *      → Look up in ValidationMessages.properties
     *      → If not found: look in Spring's MessageSource
     *      → If not found: use key itself as message
     *
     *   3. Default:                  @NotBlank  (no message attribute)
     *      → Use the constraint's default message key:
     *        {jakarta.validation.constraints.NotBlank.message}
     *      → Resolved from Hibernate Validator's bundled properties
     *
     *   4. Placeholder replacement:  {min}, {max}, {value}, {regexp}
     *      → Replaced with annotation attribute values
     *      → Applied AFTER the key is resolved to a string
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 6: HOW VALIDATION GROUPS WORK INTERNALLY                            ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * GROUP PROCESSING ORDER:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * When @Validated(OnCreate.class) triggers validation:
     *
     *   Hibernate validator processes constraints in this ORDER:
     *     1. DEFAULT group constraints (if OnCreate.class is the DEFAULT group)
     *     2. Constraints in the specified group (OnCreate.class)
     *
     * IMPORTANT: Ungroup constraints (no groups specified) belong to the DEFAULT group.
     *
     * If you specify: @Validated(OnCreate.class)
     *   → ONLY constraints with groups = OnCreate.class are evaluated
     *   → Constraints with NO groups specified (DEFAULT) are NOT evaluated
     *     UNLESS you also include Default.class in the call
     *
     * Example:
     *   @Validated({ValidationGroups.OnCreate.class, Default.class})
     *   → Evaluates both OnCreate AND DEFAULT group constraints
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * GROUP SEQUENCES (@GroupSequence):
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @GroupSequence defines the ORDER in which groups are validated.
     * If one group fails, subsequent groups are NOT validated.
     * Use when early constraints are preconditions for later ones.
     *
     *   @GroupSequence({BasicValidation.class, AdvancedValidation.class, CrossFieldValidation.class})
     *   public interface CompleteValidation {}
     *
     *   // Use in controller:
     *   @Validated(CompleteValidation.class)
     *
     *   Result: if BasicValidation constraints fail, AdvancedValidation is skipped entirely.
     *   This prevents "already invalid" data from triggering expensive validations.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * CLASS-LEVEL CONSTRAINTS & GROUP SEQUENCES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Class-level constraints (like @PasswordMatch) should typically be in a LATER group
     * than field-level constraints. Here's why:
     *
     *   Step 1: Validate @NotBlank on password → might fail (password is blank)
     *   Step 2: Validate @PasswordMatch → compares password and confirmPassword
     *            If password is null/blank, comparison might throw NPE!
     *
     * Solution: put @PasswordMatch in a later group (CrossFieldChecks) that only
     * runs if the basic field validations in earlier groups PASS.
     *
     *   @GroupSequence({Default.class, CrossFieldChecks.class})
     *   public interface RegistrationSequence {}
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 7: HOW CONSTRAINT COMPOSITION WORKS INTERNALLY                      ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Given:
     *
     *   @NotBlank
     *   @Size(min = 8, max = 100)
     *   @Pattern(regexp = "^(?=.*[A-Z]).*$")
     *   @Constraint(validatedBy = {})
     *   public @interface StrongPassword { ... }
     *
     * Applied to: @StrongPassword private String password;
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * WHAT HIBERNATE VALIDATOR DOES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   1. Reads @StrongPassword annotation on the field
     *   2. Detects it's a composed constraint (has meta-annotations @NotBlank, @Size, @Pattern)
     *   3. "Unwraps" it: treats the field as if it had all three annotations directly
     *   4. Creates validators for each inner constraint:
     *      → NotBlankValidator
     *      → SizeValidatorForCharSequence
     *      → PatternValidator
     *   5. Runs each validator independently
     *   6. Collects violations from ALL inner validators
     *
     * DEFAULT (without @ReportAsSingleViolation):
     *   If password = "weak":
     *     @NotBlank PASSES ("weak" is not blank)
     *     @Size(min=8) FAILS → violation: "Password must be 8–100 characters"
     *     @Pattern FAILS → violation: "Password must contain at least one uppercase..."
     *   Result: 2 violations reported (one per failed inner constraint)
     *
     * WITH @ReportAsSingleViolation:
     *   If ANY inner constraint fails → report ONLY the composed constraint's message
     *   If password = "weak":
     *     @Size FAILS → but instead of @Size's message, reports:
     *     "Password must be 8–100 chars with uppercase, lowercase, digit, and special char"
     *   Result: 1 violation total (the composed annotation's message)
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 8: PERFORMANCE CONSIDERATIONS                                        ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * WHAT'S FAST:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  1. Metadata caching:
     *     First validate() call builds BeanDescriptor (slow).
     *     All subsequent calls reuse cached descriptor (fast).
     *
     *  2. Simple constraints:
     *     @NotNull = null check (nanoseconds)
     *     @NotBlank = null + isEmpty + trimming (microseconds)
     *     @Min = comparison (nanoseconds)
     *
     *  3. Short-circuit on null:
     *     Most constraints return true immediately for null values.
     *     No expensive logic runs for optional null fields.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * WHAT'S POTENTIALLY SLOW:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  1. Complex @Pattern with expensive regex:
     *     Regex compilation is cached, but matching can be expensive.
     *     Avoid catastrophic backtracking (exponential regex).
     *     Test regex performance with large inputs.
     *
     *  2. Deep cascade chains:
     *     @Valid on @Valid on @Valid (three levels deep with large collections)
     *     = many reflection calls + many validator invocations.
     *     Keep nesting shallow; use projections for validation where possible.
     *
     *  3. Custom validators with DB calls:
     *     @Constraint validatedBy = EmailExistsInDbValidator.class
     *     = a DB query per validation call!
     *     Acceptable in service layer; AVOID in high-throughput REST endpoints.
     *
     *  4. Very large collections with @Valid:
     *     @Valid List<OrderItemRequest> with 10,000 items
     *     = 10,000 individual validation calls
     *     Add @Size(max = ...) BEFORE the list to limit maximum collection size.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * PERFORMANCE BEST PRACTICES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  → Limit collection sizes with @Size before using @Valid on collections
     *  → Avoid DB calls in ConstraintValidators for REST endpoints
     *  → Use @Validated(group) to validate only the constraints you need
     *  → Keep custom validators stateless and thread-safe (they're singletons)
     *  → Prefer @Validated(readOnly=true) service calls for single-field lookups
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 9: COMPLETE VALIDATION FLOW DIAGRAM                                  ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     *  ┌──────────────────────────────────────────────────────────────────────────────┐
     *  │  HTTP Request                                                                │
     *  │      ↓                                                                      │
     *  │  DispatcherServlet                                                          │
     *  │      ↓                                                                      │
     *  │  Jackson Deserialises JSON → Request DTO Object                            │
     *  │      ↓                                                                      │
     *  │  @Valid / @Validated detected on parameter?                                │
     *  │      YES ───────────────────────────────────────────────────────────────┐  │
     *  │      NO → Method executes (no validation)                               │  │
     *  │                                                                          ↓  │
     *  │                              SmartValidator.validate(object, groups)       │
     *  │                                        ↓                                  │
     *  │                              Hibernate Validator:                         │
     *  │                              ┌─────────────────────────────────────┐     │
     *  │                              │ 1. Get BeanDescriptor (from cache)  │     │
     *  │                              │ 2. For each constraint in group:    │     │
     *  │                              │    a. Get ConstraintValidator        │     │
     *  │                              │    b. Call isValid(value, context)   │     │
     *  │                              │    c. If false → add violation       │     │
     *  │                              │ 3. For @Valid fields → recurse       │     │
     *  │                              └─────────────────────────────────────┘     │
     *  │                                        ↓                                  │
     *  │                              violations.isEmpty()?                        │
     *  │                              YES ──────────────────────────────────────┐ │
     *  │                              NO ↓                                      │ │
     *  │                                                                         │ │
     *  │                    MethodArgumentNotValidException thrown               │ │
     *  │                              ↓                                          │ │
     *  │                    @ExceptionHandler in @ControllerAdvice              │ │
     *  │                              ↓                                          │ │
     *  │                    HTTP 400 + ValidationErrorResponse JSON              │ │
     *  │                                                                         │ │
     *  │                                          ↓                              │ │
     *  │                              Controller method body executes ←──────────┘ │
     *  │                                          ↓                                │
     *  │                              Return ResponseEntity / object               │
     *  │                                          ↓                                │
     *  │                              HTTP 200/201 response to client             │
     *  └──────────────────────────────────────────────────────────────────────────────┘
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║    STAGE 10: PRODUCTION CHECKLIST FOR VALIDATION                             ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     *  ✅  Use @NotBlank (not @NotNull) for all String fields requiring content.
     *
     *  ✅  Add @NotNull before other constraints on required non-String fields.
     *      (@Min, @Email etc. treat null as valid — @NotNull rejects null separately)
     *
     *  ✅  Always provide friendly messages with context.
     *      "must not be blank" → "Product name is required and cannot be empty"
     *
     *  ✅  Use a global @ExceptionHandler for MethodArgumentNotValidException and
     *      ConstraintViolationException. Never let raw stack traces reach the client.
     *
     *  ✅  Return ALL violations at once (not just the first).
     *      Users should see all their errors in one response.
     *
     *  ✅  Use validation groups for create vs update operations.
     *      Avoid duplicate DTO classes just to handle null id on create.
     *
     *  ✅  Mark nested objects with @Valid for cascade validation.
     *      Without @Valid: inner constraints are silently ignored.
     *
     *  ✅  Add @Validated at class level on controllers for path/query param validation.
     *      Without it: constraint annotations on @PathVariable / @RequestParam are ignored.
     *
     *  ✅  Keep custom ConstraintValidators stateless and thread-safe.
     *      They're singleton beans — shared across all threads.
     *
     *  ✅  Validate at the BOUNDARY (controller layer) — services receive valid data.
     *      Use service-level validation (@Validated @Service) sparingly and only
     *      where business rules (not format rules) need enforcement.
     *
     *  ✅  Add @Size(max=...) BEFORE @Valid on collections to prevent DoS attacks
     *      via huge arrays. Without it, a client can send 1M items and trigger
     *      1M validation calls.
     *
     *  ✅  Don't expose raw ConstraintViolation messages in API responses in production.
     *      They may contain internal class names. Format them cleanly.
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║   CHAPTER 7 — HOW IT WORKS: Spring Validation Internal Mechanics  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  STAGE 1 :  App startup — ValidatorFactory, LocalValidatorFactoryBean");
        System.out.println("  STAGE 2 :  @Valid on @RequestBody — complete trace to HTTP 400");
        System.out.println("  STAGE 3 :  Cascade validation — how @Valid on nested fields works");
        System.out.println("  STAGE 4 :  @Validated @Service — AOP proxy mechanics");
        System.out.println("  STAGE 5 :  Message interpolation — {min}, {max}, MessageSource");
        System.out.println("  STAGE 6 :  Validation groups — group sequences, ordering");
        System.out.println("  STAGE 7 :  Constraint composition — unwrapping, @ReportAsSingleViolation");
        System.out.println("  STAGE 8 :  Performance — caching, expensive validators, collection limits");
        System.out.println("  STAGE 9 :  Complete flow diagram — request to response");
        System.out.println("  STAGE 10:  Production checklist — 12 rules for production validation");
        System.out.println();
        System.out.println("📚 Chapter 7 — Complete! You now know Spring Validation from Zero to Expert.");
        System.out.println();
        System.out.println("  Next Chapter: Chapter 8 — Spring Testing Annotations");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

