package com.learning.springboot.chapter03;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║            EXAMPLE 04: EXCEPTION HANDLING ANNOTATIONS IN ACTION                      ║
 * ║            @ExceptionHandler · @ControllerAdvice · @RestControllerAdvice             ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example04ExceptionHandling.java
 * Purpose:     Build a production-grade, centralised exception handling strategy.
 *              Learn to produce consistent, informative error responses.
 * Difficulty:  ⭐⭐⭐ Intermediate
 * Time:        35 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * WHAT WE ARE BUILDING:
 *
 * A complete exception handling hierarchy for a REST API:
 *
 *   1. Custom exception classes (domain-specific)
 *   2. Controller-LOCAL  @ExceptionHandler (handles exceptions from one controller)
 *   3. GLOBAL @RestControllerAdvice (handles exceptions from all controllers)
 *   4. ProblemDetail (RFC 7807 — standardised JSON error format)
 *
 * WHAT A GOOD ERROR RESPONSE LOOKS LIKE:
 *
 *   HTTP/1.1 404 Not Found
 *   Content-Type: application/problem+json
 *
 *   {
 *     "type":     "https://api.example.com/errors/not-found",
 *     "title":    "Resource Not Found",
 *     "status":   404,
 *     "detail":   "Order with id 42 was not found",
 *     "instance": "/api/v1/orders/42",
 *     "timestamp": "2024-01-15T10:30:00",
 *     "errorCode": "ORDER_NOT_FOUND"
 *   }
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 1: CUSTOM EXCEPTION HIERARCHY
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║           CUSTOM EXCEPTION HIERARCHY — THE FOUNDATION                        ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 WHY CUSTOM EXCEPTIONS?
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Throwing generic RuntimeException everywhere is like having ONE fire alarm
 * that covers everything — you can't tell if it's a kitchen fire or a chemical spill.
 *
 * Custom exceptions give you:
 *   ✓ Precise control over HTTP status codes per exception type
 *   ✓ Meaningful, user-facing error messages
 *   ✓ Structured error codes for client-side handling
 *   ✓ Rich context (which resource, which user, etc.)
 *   ✓ Easy to catch specifically in @ExceptionHandler
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🏗️ EXCEPTION HIERARCHY:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   RuntimeException
 *       └── AppException (base for all app exceptions)
 *               ├── ResourceNotFoundException  (404)
 *               │       └── OrderNotFoundException
 *               │       └── ProductStockNotFoundException
 *               ├── BusinessRuleViolationException (422)
 *               │       └── InsufficientStockException
 *               │       └── OrderAlreadyCancelledException
 *               ├── ConflictException (409)
 *               │       └── DuplicateOrderException
 *               └── ServiceUnavailableException (503)
 *
 */

/** Base exception for all application exceptions. */
class AppException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    public AppException(String errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode  = errorCode;
        this.httpStatus = status;
    }

    public String     getErrorCode()  { return errorCode; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}

/** Thrown when a requested resource (entity) is not found — HTTP 404. */
class ResourceNotFoundException extends AppException {
    private final String resourceType;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(
            resourceType.toUpperCase().replace(" ", "_") + "_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            resourceType + " with id '" + resourceId + "' was not found"
        );
        this.resourceType = resourceType;
        this.resourceId   = resourceId;
    }

    public String getResourceType() { return resourceType; }
    public Object getResourceId()   { return resourceId; }
}

/** Thrown when a resource already exists — HTTP 409. */
class ConflictException extends AppException {
    public ConflictException(String message) {
        super("CONFLICT", HttpStatus.CONFLICT, message);
    }
}

/** Thrown when a business rule is violated — HTTP 422. */
class BusinessRuleViolationException extends AppException {
    public BusinessRuleViolationException(String rule, String detail) {
        super(rule, HttpStatus.UNPROCESSABLE_ENTITY, detail);
    }
}

/** Thrown when an order cannot be placed — HTTP 422. */
class InsufficientStockException extends BusinessRuleViolationException {
    public InsufficientStockException(Long productId, int requested, int available) {
        super("INSUFFICIENT_STOCK",
              String.format("Insufficient stock for product %d: requested=%d, available=%d",
                      productId, requested, available));
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 2: ERROR RESPONSE DTOs
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * Standard error response body.
 * Used by the @RestControllerAdvice to build consistent error responses.
 *
 * Response structure (matching RFC 7807 Problem Details):
 * {
 *   "type":      "https://errors.app.com/not-found",
 *   "title":     "Resource Not Found",
 *   "status":    404,
 *   "detail":    "Order with id 42 was not found",
 *   "instance":  "/api/v1/orders/42",
 *   "timestamp": "2024-01-15T10:30:00",
 *   "errorCode": "ORDER_NOT_FOUND",
 *   "errors":    []
 * }
 */
class ErrorResponse {
    private int             status;
    private String          errorCode;
    private String          title;
    private String          detail;
    private String          instance;
    private LocalDateTime   timestamp;
    private List<FieldError> fieldErrors;

    public ErrorResponse(int status, String errorCode, String title,
                         String detail, String instance) {
        this.status      = status;
        this.errorCode   = errorCode;
        this.title       = title;
        this.detail      = detail;
        this.instance    = instance;
        this.timestamp   = LocalDateTime.now();
        this.fieldErrors = new ArrayList<>();
    }

    public int              getStatus()      { return status; }
    public String           getErrorCode()   { return errorCode; }
    public String           getTitle()       { return title; }
    public String           getDetail()      { return detail; }
    public String           getInstance()    { return instance; }
    public LocalDateTime    getTimestamp()   { return timestamp; }
    public List<FieldError> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(List<FieldError> errors) { this.fieldErrors = errors; }

    /** Represents a single field validation failure. */
    public record FieldError(String field, String message, Object rejectedValue) {}
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 3: DOMAIN MODEL (Order API)
// ══════════════════════════════════════════════════════════════════════════════════════

enum OrderStatus { PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }

class Order {
    private Long        id;
    private String      customerId;
    private List<OrderLineItem> items;
    private double      totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;

    public Order(Long id, String customerId, List<OrderLineItem> items) {
        this.id = id; this.customerId = customerId; this.items = new ArrayList<>(items);
        this.totalAmount = items.stream().mapToDouble(OrderLineItem::subtotal).sum();
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Long             getId()          { return id; }
    public String           getCustomerId()  { return customerId; }
    public List<OrderLineItem> getItems()    { return items; }
    public double           getTotalAmount() { return totalAmount; }
    public OrderStatus      getStatus()      { return status; }
    public LocalDateTime    getCreatedAt()   { return createdAt; }
    public void setStatus(OrderStatus status) { this.status = status; }
}

class OrderLineItem {
    private Long   productId;
    private String productName;
    private int    quantity;
    private double unitPrice;

    public OrderLineItem(Long productId, String productName, int quantity, double price) {
        this.productId = productId; this.productName = productName;
        this.quantity = quantity; this.unitPrice = price;
    }

    public Long   getProductId()   { return productId; }
    public String getProductName() { return productName; }
    public int    getQuantity()    { return quantity; }
    public double getUnitPrice()   { return unitPrice; }
    public double subtotal()       { return quantity * unitPrice; }
}

/** DTO with validation constraints for creating an order. */
class CreateOrderRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotNull(message = "Items list is required")
    private List<CreateOrderItemRequest> items;

    public String getCustomerId()                    { return customerId; }
    public List<CreateOrderItemRequest> getItems()   { return items; }
    public void setCustomerId(String v)              { this.customerId = v; }
    public void setItems(List<CreateOrderItemRequest> v) { this.items = v; }
}

class CreateOrderItemRequest {
    @NotNull(message = "Product ID is required")
    private Long productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    public Long getProductId() { return productId; }
    public int  getQuantity()  { return quantity; }
    public void setProductId(Long v) { this.productId = v; }
    public void setQuantity(int v)   { this.quantity  = v; }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 4: @ExceptionHandler — CONTROLLER-LOCAL EXCEPTION HANDLING
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║               @ExceptionHandler — CONTROLLER-LOCAL EXPLAINED                 ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @ExceptionHandler on a method INSIDE a @Controller / @RestController
 * handles exceptions thrown by the handler methods IN THAT SAME CONTROLLER ONLY.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ SCOPE LIMITATION — Controller-local vs Global:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   @ExceptionHandler inside @RestController:
 *     → Handles exceptions from THAT CONTROLLER ONLY
 *     → Other controllers' exceptions: NOT handled here
 *
 *   @ExceptionHandler inside @RestControllerAdvice:
 *     → Handles exceptions from ALL controllers in the application
 *     → This is the recommended approach for production
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 WHEN TO USE CONTROLLER-LOCAL @ExceptionHandler:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  A specific controller has UNIQUE error handling requirements
 *  •  You need to override the global handler for a specific controller
 *  •  The exception type is very specific to one controller's domain
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/v1/orders")
class OrderController {

    private final Map<Long, Order> orderStore = new ConcurrentHashMap<>();
    private final AtomicLong       idGen      = new AtomicLong(1);

    // Simulate an inventory (productId → stock)
    private final Map<Long, Integer> inventory = Map.of(101L, 50, 102L, 10, 103L, 0);

    public OrderController() {
        // Seed data
        List<OrderLineItem> items = List.of(new OrderLineItem(101L, "Laptop", 1, 1299.99));
        orderStore.put(1L, new Order(1L, "customer-A", items));
        idGen.set(2);
    }

    /**
     * GET /api/v1/orders/{id}
     * Throws ResourceNotFoundException if the order doesn't exist.
     */
    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        Order order = orderStore.get(id);
        if (order == null) {
            throw new ResourceNotFoundException("Order", id);   // ← Will be caught by handler below
        }
        return order;
    }

    /**
     * GET /api/v1/orders
     */
    @GetMapping
    public List<Order> getAllOrders() {
        return List.copyOf(orderStore.values());
    }

    /**
     * POST /api/v1/orders
     *
     * @Valid triggers Jakarta Bean Validation on the request body.
     * If validation fails → Spring throws MethodArgumentNotValidException → caught below.
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        System.out.println("📥 POST /api/v1/orders — customer=" + request.getCustomerId());

        // Build order items and check stock
        List<OrderLineItem> lineItems = new ArrayList<>();
        for (CreateOrderItemRequest item : request.getItems()) {
            int stock = inventory.getOrDefault(item.getProductId(), 0);
            if (stock < item.getQuantity()) {
                throw new InsufficientStockException(           // ← Business rule violation
                        item.getProductId(), item.getQuantity(), stock);
            }
            lineItems.add(new OrderLineItem(item.getProductId(), "Product-" + item.getProductId(),
                    item.getQuantity(), 99.99));
        }

        long newId = idGen.getAndIncrement();
        Order order = new Order(newId, request.getCustomerId(), lineItems);
        orderStore.put(newId, order);

        URI location = org.springframework.web.servlet.support.ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}").buildAndExpand(newId).toUri();
        return ResponseEntity.created(location).body(order);
    }

    /**
     * DELETE /api/v1/orders/{id}/cancel
     * Cancel an order. Throws if already cancelled.
     */
    @DeleteMapping("/{id}/cancel")
    public Order cancelOrder(@PathVariable Long id) {
        Order order = orderStore.get(id);
        if (order == null) throw new ResourceNotFoundException("Order", id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException("ORDER_ALREADY_CANCELLED",
                    "Order " + id + " has already been cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return order;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // CONTROLLER-LOCAL @ExceptionHandler METHODS
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Handles ResourceNotFoundException thrown by methods in THIS controller.
     *
     * Flow:
     *   getOrder(99) throws ResourceNotFoundException("Order", 99)
     *        ↓
     *   Spring checks: is there @ExceptionHandler for ResourceNotFoundException?
     *        ↓
     *   YES → calls this method
     *        ↓
     *   Returns: 404 Not Found + ErrorResponse JSON body
     *
     * NOTE: In production, move this to @RestControllerAdvice (Section 5)
     * so ALL controllers benefit. Shown here for educational purposes.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex,
                                        WebRequest request) {
        System.out.println("🚨 [OrderController] Handling ResourceNotFoundException: " + ex.getMessage());
        return new ErrorResponse(
                404,
                ex.getErrorCode(),
                "Resource Not Found",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }

    /**
     * Handles MethodArgumentNotValidException (thrown by @Valid on @RequestBody).
     *
     * When the JSON body fails bean validation:
     *   createOrder({customerId: "", items: []})
     *     → @NotBlank on customerId fails
     *     → Spring throws MethodArgumentNotValidException
     *     → This handler catches it
     *     → Returns 400 Bad Request with field-level errors
     *
     * Response example:
     * {
     *   "status": 400,
     *   "errorCode": "VALIDATION_FAILED",
     *   "title": "Validation Failed",
     *   "detail": "2 field(s) failed validation",
     *   "fieldErrors": [
     *     { "field": "customerId", "message": "Customer ID is required", "rejectedValue": "" },
     *     { "field": "items", "message": "Items list is required", "rejectedValue": null }
     *   ]
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex,
                                          WebRequest request) {
        System.out.println("🚨 [OrderController] Handling validation failure");

        // Extract field-level errors from the BindingResult
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ErrorResponse.FieldError(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getRejectedValue()
                ))
                .collect(Collectors.toList());

        ErrorResponse error = new ErrorResponse(
                400,
                "VALIDATION_FAILED",
                "Validation Failed",
                fieldErrors.size() + " field(s) failed validation",
                request.getDescription(false).replace("uri=", "")
        );
        error.setFieldErrors(fieldErrors);
        return error;
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 5: @RestControllerAdvice — GLOBAL EXCEPTION HANDLING
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║           @ControllerAdvice / @RestControllerAdvice — EXPLAINED              ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITIONS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @ControllerAdvice = A "global controller" that provides:
 *   1. @ExceptionHandler methods — handle exceptions from ALL controllers
 *   2. @ModelAttribute methods   — add data to ALL controllers' models
 *   3. @InitBinder methods       — configure WebDataBinder for ALL controllers
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 *   → All @ExceptionHandler methods automatically write to the response body
 *   → No need for @ResponseBody on each handler method
 *   → Perfect for REST API global exception handling
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 HOW SPRING FINDS THE RIGHT HANDLER:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   PRIORITY ORDER when exception is thrown:
 *
 *   1. @ExceptionHandler INSIDE the same @Controller (most specific)
 *      → Handles only this controller's exceptions
 *
 *   2. @ExceptionHandler in @ControllerAdvice (global)
 *      → Handles all controllers' exceptions
 *      → Multiple @ControllerAdvice: ordered by @Order or Ordered interface
 *
 *   3. Spring Boot's DefaultHandlerExceptionResolver (fallback)
 *      → Handles Spring's internal exceptions (e.g., HttpMessageNotReadableException)
 *
 *   4. Spring Boot's error page (/error endpoint) — last resort
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔧 @ControllerAdvice SCOPING (optionally limit which controllers it applies to):
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   @ControllerAdvice                             ← applies to ALL controllers
 *   @ControllerAdvice("com.example.api")          ← applies to base package only
 *   @ControllerAdvice(assignableTypes = MyCtrl.class) ← applies to specific class
 *   @ControllerAdvice(annotations = RestController.class) ← applies to @RestController
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@RestControllerAdvice   // = @ControllerAdvice + @ResponseBody (applies to ALL controllers)
class GlobalExceptionHandler {

    /**
     * Handles ALL AppException subclasses (ResourceNotFoundException, ConflictException, etc.)
     *
     * This ONE handler covers all custom app exceptions — because they all extend AppException
     * and carry their own httpStatus and errorCode.
     *
     * The @ExceptionHandler annotation accepts an array of exception types.
     * When any of these (or their subclasses) is thrown, this method is invoked.
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex,
                                                            WebRequest request) {
        System.out.println("🌍 [GlobalHandler] AppException: " + ex.getErrorCode() + " — " + ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                ex.getHttpStatus().value(),
                ex.getErrorCode(),
                toTitle(ex.getHttpStatus()),
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    /**
     * Handles IllegalArgumentException — typically thrown by service layer
     * when method arguments are invalid.
     *
     * Example: service.getById(-1) throws new IllegalArgumentException("ID must be positive")
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                WebRequest request) {
        System.out.println("🌍 [GlobalHandler] IllegalArgumentException: " + ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                400, "INVALID_ARGUMENT", "Invalid Request Argument",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handles ALL uncaught exceptions — the "catch-all" handler.
     *
     * This is the LAST RESORT. If no other handler matches, this one runs.
     *
     * IMPORTANT: Never expose internal details (stack traces, DB structure, etc.)
     * to clients. Log the full exception internally, but return a generic message.
     *
     * @param ex      the caught exception
     * @param request the WebRequest (for getting the request URI)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex,
                                                                 WebRequest request) {
        // Log the FULL exception internally for debugging
        System.err.println("🚨 [GlobalHandler] Unexpected exception: " + ex.getMessage());
        // In production, use: log.error("Unexpected error", ex);

        // Return a GENERIC message to the client (never expose internals)
        ErrorResponse error = new ErrorResponse(
                500, "INTERNAL_ERROR", "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.internalServerError().body(error);
    }

    /** Converts HttpStatus to a human-readable title. */
    private static String toTitle(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND            -> "Resource Not Found";
            case CONFLICT             -> "Resource Conflict";
            case UNPROCESSABLE_ENTITY -> "Business Rule Violation";
            case FORBIDDEN            -> "Access Denied";
            case UNAUTHORIZED         -> "Authentication Required";
            case BAD_REQUEST          -> "Bad Request";
            default                   -> status.getReasonPhrase();
        };
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 6: ProblemDetail — RFC 7807 STANDARD ERROR FORMAT
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║            ProblemDetail (RFC 7807) — THE MODERN STANDARD                    ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 WHAT IS RFC 7807?
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * RFC 7807 defines a standard JSON format for HTTP error responses called
 * "Problem Details". Spring 6+ / Spring Boot 3+ natively supports it via
 * the ProblemDetail class.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📊 PROBLEM DETAIL RESPONSE FORMAT:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Content-Type: application/problem+json
 *
 * {
 *   "type":     "https://api.example.com/problems/insufficient-stock",  ← URI identifying error type
 *   "title":    "Insufficient Stock",           ← human-readable summary
 *   "status":   422,                            ← HTTP status code
 *   "detail":   "Not enough stock for product", ← human-readable explanation
 *   "instance": "/api/v1/orders",               ← URI of specific occurrence
 *   "productId": 101,                           ← custom extension properties
 *   "available": 2,
 *   "requested": 10
 * }
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 ProblemDetail API (Spring 6+):
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "...");
 *   pd.setTitle("Resource Not Found");
 *   pd.setType(URI.create("https://api.example.com/problems/not-found"));
 *   pd.setProperty("resourceType", "Order");    ← custom extension
 *   pd.setProperty("resourceId", 42);           ← custom extension
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 💡 TO ENABLE RFC 7807 GLOBALLY:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Add to application.yml:
 *   spring:
 *     mvc:
 *       problemdetails:
 *         enabled: true    ← Spring automatically uses ProblemDetail for its own exceptions
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Example global handler that uses ProblemDetail (alternative to ErrorResponse):
 */
@RestControllerAdvice(basePackageClasses = OrderController.class)
// ↑ Scoped: only applies to controllers in the same package as OrderController.
//   In practice, use a global @RestControllerAdvice without scoping.
class ProblemDetailExceptionHandler {

    /**
     * Returns RFC 7807 ProblemDetail for ResourceNotFoundException.
     *
     * This is the MODERN Spring 6+ approach — using ProblemDetail directly.
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientStock(
            InsufficientStockException ex,
            WebRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        pd.setTitle("Insufficient Stock");
        pd.setType(URI.create("https://api.example.com/problems/insufficient-stock"));
        pd.setInstance(URI.create(
                request.getDescription(false).replace("uri=", "")));
        pd.setProperty("errorCode",  ex.getErrorCode());
        pd.setProperty("timestamp",  LocalDateTime.now().toString());

        return ResponseEntity.unprocessableEntity().body(pd);
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 7: THE COMPLETE EXCEPTION HANDLING PICTURE
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║        EXCEPTION HANDLING — THE COMPLETE PICTURE                             ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * EXCEPTION LIFECYCLE:
 *
 *   [HTTP Request]
 *        ↓
 *   DispatcherServlet routes to OrderController.createOrder()
 *        ↓
 *   Controller throws InsufficientStockException
 *        ↓
 *   Spring's HandlerExceptionResolver pipeline:
 *        ↓
 *   STEP 1: Check @ExceptionHandler in the SAME controller
 *           → OrderController has handlers for ResourceNotFoundException
 *             and MethodArgumentNotValidException, but NOT InsufficientStockException
 *           → NOT HANDLED HERE
 *        ↓
 *   STEP 2: Check @ExceptionHandler in @RestControllerAdvice beans
 *           → GlobalExceptionHandler has @ExceptionHandler(AppException.class)
 *           → InsufficientStockException extends BusinessRuleViolationException
 *             extends AppException → MATCH!
 *           → HANDLED HERE → returns 422 Unprocessable Entity + ErrorResponse JSON
 *        ↓
 *   Response: 422 Unprocessable Entity
 *   {
 *     "status": 422,
 *     "errorCode": "INSUFFICIENT_STOCK",
 *     "title": "Business Rule Violation",
 *     "detail": "Insufficient stock for product 103: requested=5, available=0",
 *     "instance": "/api/v1/orders",
 *     "timestamp": "..."
 *   }
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🏆 PRODUCTION BEST PRACTICES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. Create a meaningful exception hierarchy (AppException as base)
 *  2. Put ALL exception handling in @RestControllerAdvice (centralised)
 *  3. Log the FULL exception (with stack trace) internally
 *  4. Return GENERIC messages to clients (never expose internals)
 *  5. Use consistent error response format across ALL endpoints
 *  6. Include an errorCode for client-side programmatic handling
 *  7. Include a timestamp for log correlation
 *  8. Consider RFC 7807 ProblemDetail for standardised error format
 *
 */
class Example04ExceptionHandling {
    // Intentionally empty — documentation class
}

