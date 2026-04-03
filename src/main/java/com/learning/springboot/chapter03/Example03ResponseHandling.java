package com.learning.springboot.chapter03;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║            EXAMPLE 03: RESPONSE HANDLING ANNOTATIONS IN ACTION                       ║
 * ║            @ResponseBody · @ResponseStatus · ResponseEntity<T>                       ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example03ResponseHandling.java
 * Purpose:     Master every way to control the HTTP response — status codes, headers,
 *              body format, and the powerful ResponseEntity builder.
 * Difficulty:  ⭐⭐ Beginner–Intermediate
 * Time:        25 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * SCENARIO: A Movie Catalog API that demonstrates all response annotation patterns.
 *
 * WHAT WE ARE BUILDING:
 *
 *   Every HTTP response has three parts:
 *
 *     ┌───────────────────────────────────────────────────────────────┐
 *     │ HTTP/1.1 201 Created                 ← STATUS LINE            │
 *     │ Content-Type: application/json       ← HEADERS                │
 *     │ Location: /api/v1/movies/5           ← HEADERS                │
 *     │ X-Request-ID: abc-123                ← HEADERS                │
 *     │                                                               │
 *     │ { "id": 5, "title": "Inception" }    ← BODY                   │
 *     └───────────────────────────────────────────────────────────────┘
 *
 *   This file shows how to control ALL THREE parts.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  DOMAIN MODEL
// ══════════════════════════════════════════════════════════════════════════════════════

/** Movie entity. */
class Movie {
    private Long   id;
    private String title;
    private String director;
    private int    releaseYear;
    private double rating;
    private LocalDateTime createdAt;

    public Movie() {}
    public Movie(Long id, String title, String director, int year, double rating) {
        this.id = id; this.title = title; this.director = director;
        this.releaseYear = year; this.rating = rating;
        this.createdAt = LocalDateTime.now();
    }

    public Long   getId()          { return id; }
    public String getTitle()       { return title; }
    public String getDirector()    { return director; }
    public int    getReleaseYear() { return releaseYear; }
    public double getRating()      { return rating; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setTitle(String t)       { this.title = t; }
    public void setRating(double r)      { this.rating = r; }
    public void setDirector(String d)    { this.director = d; }
    public void setReleaseYear(int y)    { this.releaseYear = y; }
}

/**
 * ─────────────────────────────────────────────────────────────────────────────────────
 * GENERIC API RESPONSE WRAPPER
 * ─────────────────────────────────────────────────────────────────────────────────────
 *
 * A common pattern in real APIs: wrap every response in a standard envelope:
 *
 *   {
 *     "success": true,
 *     "message": "Movie created successfully",
 *     "data": { "id": 5, "title": "Inception", ... },
 *     "timestamp": "2024-01-15T10:30:00"
 *   }
 *
 * Benefits:
 *   ✓ Consistent structure for all API responses
 *   ✓ Clients always know where to find the data
 *   ✓ Easy to add metadata (pagination, warnings, etc.)
 */
class ApiResponse<T> {
    private boolean success;
    private String  message;
    private T       data;
    private LocalDateTime timestamp;

    private ApiResponse(boolean success, String message, T data) {
        this.success   = success;
        this.message   = message;
        this.data      = data;
        this.timestamp = LocalDateTime.now();
    }

    /** Factory method for success responses. */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /** Factory method for failure responses. */
    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public boolean       isSuccess()   { return success; }
    public String        getMessage()  { return message; }
    public T             getData()     { return data; }
    public LocalDateTime getTimestamp() { return timestamp; }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  THE CONTROLLER — ALL RESPONSE TECHNIQUES
// ══════════════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/v1/movies")
class MovieController {

    private final Map<Long, Movie> store   = new ConcurrentHashMap<>();
    private final AtomicLong       idGen   = new AtomicLong(1);

    public MovieController() {
        store.put(1L, new Movie(1L, "Inception",    "Christopher Nolan", 2010, 8.8));
        store.put(2L, new Movie(2L, "The Matrix",   "Wachowski Sisters", 1999, 8.7));
        store.put(3L, new Movie(3L, "Interstellar", "Christopher Nolan", 2014, 8.6));
        idGen.set(4);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SECTION 1: @ResponseBody — RETURN DATA DIRECTLY
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                        @ResponseBody  EXPLAINED                              ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @ResponseBody tells Spring MVC: "Write the METHOD'S RETURN VALUE directly to
     * the HTTP response body — do NOT try to resolve it as a view name."
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 HOW IT WORKS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @ResponseBody triggers the HttpMessageConverter pipeline:
     *
     *   Return value: Movie object
     *       ↓
     *   ContentNegotiationManager checks Accept header
     *       Accept: application/json   → MappingJackson2HttpMessageConverter
     *       Accept: application/xml    → Jaxb2RootElementHttpMessageConverter
     *       Accept: text/plain         → StringHttpMessageConverter
     *       ↓
     *   Selected converter serialises the object
     *       ↓
     *   Response body: { "id":1, "title":"Inception", ... }
     *   Content-Type:  application/json
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ⚠️ NOTE: In a @RestController, @ResponseBody is applied to ALL methods
     *    automatically. You don't need to add it explicitly.
     *    The example below shows explicit @ResponseBody for educational purposes.
     *
     *    @ResponseBody is only NECESSARY when using @Controller (not @RestController).
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Endpoint: GET /api/v1/movies/plain (returns List<Movie> directly → auto JSON)
     *
     * Since this class is @RestController, @ResponseBody is implicit on all methods.
     * The method below shows the SAME behaviour as getAllMovies() but with explicit
     * @ResponseBody to illustrate the annotation.
     */
    @GetMapping("/plain")
    @ResponseBody   // ← This is redundant in @RestController but shown for education!
    public List<Movie> getMoviesWithExplicitResponseBody() {
        System.out.println("📥 GET /api/v1/movies/plain (explicit @ResponseBody demo)");
        return List.copyOf(store.values());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SECTION 2: @ResponseStatus — SET HTTP STATUS CODE
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                       @ResponseStatus  EXPLAINED                             ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @ResponseStatus sets the HTTP status code (and optionally a reason message)
     * that will be returned with the response.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔧 TWO PLACES YOU CAN USE @ResponseStatus:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PLACE 1: On a CONTROLLER METHOD
     *
     *     @PostMapping
     *     @ResponseStatus(HttpStatus.CREATED)  ← Always responds with 201 Created
     *     Movie createMovie(@RequestBody Movie m) { ... }
     *
     *     When to use: fixed, known status code (e.g., POST always returns 201)
     *     Limitation: cannot change the status code dynamically
     *
     *  PLACE 2: On an EXCEPTION CLASS
     *
     *     @ResponseStatus(HttpStatus.NOT_FOUND)
     *     class MovieNotFoundException extends RuntimeException { ... }
     *
     *     When Spring catches this exception (uncaught from a controller), it
     *     automatically sets the response status to 404 Not Found.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * @ResponseStatus vs ResponseEntity:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @ResponseStatus(HttpStatus.CREATED)  → STATIC status code (compile-time)
     *   ResponseEntity.status(201)           → DYNAMIC status code (runtime)
     *
     *   Use @ResponseStatus when:
     *     •  The status is ALWAYS the same for this endpoint
     *     •  Simple endpoints (no 404 possibility)
     *
     *   Use ResponseEntity when:
     *     •  The status can vary (200 OR 404)
     *     •  You need to set custom headers
     *     •  You need conditional body (body vs no body)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Endpoint: POST /api/v1/movies/simple (always returns 201 Created)
     */
    @PostMapping("/simple")
    @ResponseStatus(HttpStatus.CREATED)   // ← Always 201 Created for this endpoint
    public Movie createMovieSimple(@RequestBody Movie movie) {
        System.out.println("📥 POST /api/v1/movies/simple — @ResponseStatus demo");
        long newId = idGen.getAndIncrement();
        Movie newMovie = new Movie(newId, movie.getTitle(), movie.getDirector(),
                movie.getReleaseYear(), movie.getRating());
        store.put(newId, newMovie);
        return newMovie;  // ← Serialised to JSON; Spring uses 201 from @ResponseStatus
    }

    /**
     * @ResponseStatus with reason — adds a reason phrase to the status line.
     *
     * NOTE: reason causes Spring to use response.sendError(status, reason)
     * instead of setting the status code directly. This can suppress the
     * response body in some configurations. Prefer ResponseEntity in production.
     *
     * Shown here for completeness.
     */
    @DeleteMapping("/deprecated/{id}")
    @ResponseStatus(value = HttpStatus.GONE, reason = "This endpoint is deprecated")
    public void deprecatedDelete(@PathVariable Long id) {
        // This endpoint always returns 410 Gone with reason "This endpoint is deprecated"
        System.out.println("📥 DELETE /api/v1/movies/deprecated/" + id + " (410 Gone demo)");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SECTION 3: ResponseEntity<T> — FULL RESPONSE CONTROL
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                       ResponseEntity<T>  EXPLAINED                           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * ResponseEntity<T> is a WRAPPER that gives you complete control over:
     *   1. HTTP STATUS CODE    (200, 201, 404, 409, etc.)
     *   2. HTTP HEADERS        (Location, Cache-Control, X-Custom-Header, etc.)
     *   3. RESPONSE BODY       (Any object, serialised by HttpMessageConverter)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔧 ResponseEntity BUILDER PATTERNS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   // Status only (no body)
     *   ResponseEntity.ok().build()                    // 200, no body
     *   ResponseEntity.noContent().build()             // 204, no body
     *   ResponseEntity.notFound().build()              // 404, no body
     *
     *   // Status + body
     *   ResponseEntity.ok(movie)                       // 200 + body
     *   ResponseEntity.status(HttpStatus.CREATED).body(movie)  // 201 + body
     *   ResponseEntity.status(409).body(errorMsg)     // 409 + error body
     *
     *   // Status + body + headers
     *   ResponseEntity.created(locationUri).body(movie) // 201 + Location + body
     *   ResponseEntity.ok()
     *       .header("X-Request-ID", "abc-123")
     *       .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
     *       .body(movie)
     *
     *   // From builder
     *   ResponseEntity.status(200)
     *       .headers(httpHeaders)
     *       .body(movie)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * GET /api/v1/movies/{id} — Dynamic status (200 or 404)
     */
    @GetMapping("/{id}")
    public ResponseEntity<Movie> getMovie(@PathVariable Long id) {
        System.out.println("📥 GET /api/v1/movies/" + id);
        Movie movie = store.get(id);
        if (movie == null) {
            return ResponseEntity.notFound().build();     // 404 Not Found, no body
        }
        return ResponseEntity.ok(movie);                  // 200 OK + movie JSON body
    }

    /**
     * POST /api/v1/movies — Creates a movie and returns 201 Created with Location header.
     *
     * REST best practice for POST:
     *   ✓ Return 201 Created (not 200 OK)
     *   ✓ Set Location header to the URI of the new resource
     *   ✓ Return the created resource in the body
     *
     * Response:
     *   HTTP/1.1 201 Created
     *   Location: http://localhost:8080/api/v1/movies/5
     *   Content-Type: application/json
     *   { "id": 5, "title": "Dune", ... }
     */
    @PostMapping
    public ResponseEntity<Movie> createMovie(@RequestBody Movie movieRequest) {
        System.out.println("📥 POST /api/v1/movies — creating: " + movieRequest.getTitle());

        long newId = idGen.getAndIncrement();
        Movie newMovie = new Movie(newId, movieRequest.getTitle(), movieRequest.getDirector(),
                movieRequest.getReleaseYear(), movieRequest.getRating());
        store.put(newId, newMovie);

        // Build the Location URI for the new resource
        URI location = org.springframework.web.servlet.support.ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newId)
                .toUri();

        return ResponseEntity
                .created(location)    // Sets status=201 AND Location header
                .body(newMovie);
    }

    /**
     * DELETE /api/v1/movies/{id} — Returns 204 No Content or 404.
     *
     * 204 No Content:
     *   ✓ Success
     *   ✓ No body (nothing to return after deletion)
     *   ✓ ResponseEntity<Void> — Void signals "no body"
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        System.out.println("📥 DELETE /api/v1/movies/" + id);
        Movie removed = store.remove(id);
        return removed != null
                ? ResponseEntity.noContent().build()   // 204 No Content
                : ResponseEntity.notFound().build();   // 404 Not Found
    }

    /**
     * GET /api/v1/movies/{id}/with-headers
     * Demonstrates adding CUSTOM HEADERS to the response.
     *
     * Custom headers are useful for:
     *   •  Rate limiting info: X-RateLimit-Remaining: 99
     *   •  Pagination: X-Total-Count: 150, X-Page: 2
     *   •  Caching: Cache-Control, ETag
     *   •  Correlation: X-Request-ID, X-Correlation-ID
     *   •  Deprecation: Deprecation: true, Sunset: Sat, 01 Jan 2025 00:00:00 GMT
     */
    @GetMapping("/{id}/with-headers")
    public ResponseEntity<Movie> getMovieWithCustomHeaders(@PathVariable Long id) {
        System.out.println("📥 GET /api/v1/movies/" + id + "/with-headers");
        Movie movie = store.get(id);
        if (movie == null) return ResponseEntity.notFound().build();

        // Build custom HTTP response headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-ID",          "req-" + System.currentTimeMillis());
        headers.add("X-RateLimit-Remaining", "99");
        headers.add("X-RateLimit-Reset",     "60");
        headers.add("Cache-Control",          "max-age=3600");  // Cache for 1 hour

        return ResponseEntity
                .ok()
                .headers(headers)           // ← Attach all custom headers
                .body(movie);               // ← Attach body
    }

    /**
     * GET /api/v1/movies — Returns wrapped ApiResponse with pagination metadata.
     *
     * Demonstrates the ApiResponse<T> wrapper pattern:
     * {
     *   "success": true,
     *   "message": "Movies retrieved successfully",
     *   "data": [ {...}, {...} ],
     *   "timestamp": "..."
     * }
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Movie>>> getAllMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        System.out.printf("📥 GET /api/v1/movies?page=%d&size=%d%n", page, size);

        List<Movie> all    = List.copyOf(store.values());
        int         start  = page * size;
        int         end    = Math.min(start + size, all.size());
        List<Movie> paged  = (start < all.size()) ? all.subList(start, end) : List.of();

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(all.size()));
        headers.add("X-Page",        String.valueOf(page));
        headers.add("X-Page-Size",   String.valueOf(size));

        ApiResponse<List<Movie>> body = ApiResponse.success(
                "Movies retrieved successfully", paged);

        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
    }

    /**
     * GET /api/v1/movies/{id}/conditional
     *
     * Conditional GET — Demonstrates ETag caching.
     *
     * How it works:
     *   1. First request: Returns 200 OK with ETag header
     *   2. Subsequent request with If-None-Match: ETag → Returns 304 Not Modified (no body)
     *
     * This saves bandwidth — the client already has the latest data.
     *
     * ETag = Hash of the response body (changes when data changes)
     */
    @GetMapping("/{id}/conditional")
    public ResponseEntity<Movie> getMovieConditional(
            @PathVariable Long id,
            @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {

        Movie movie = store.get(id);
        if (movie == null) return ResponseEntity.notFound().build();

        // Generate ETag as a simple hash of the movie's content
        String etag = '"' + String.valueOf(movie.hashCode()) + '"';

        // Check if client's cached version is still valid
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();  // 304 Not Modified
        }

        return ResponseEntity.ok()
                .eTag(etag)           // Sets ETag: "123456789" header
                .body(movie);         // 200 OK with body
    }

    /**
     * POST /api/v1/movies/conflict-demo
     *
     * Demonstrates 409 Conflict — when resource already exists.
     *
     * Pattern: Dynamic status code based on business logic.
     */
    @PostMapping("/conflict-demo")
    public ResponseEntity<ApiResponse<Movie>> createWithConflictCheck(
            @RequestBody Movie movieRequest) {

        // Check if a movie with the same title already exists
        boolean titleExists = store.values().stream()
                .anyMatch(m -> m.getTitle().equalsIgnoreCase(movieRequest.getTitle()));

        if (titleExists) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)    // 409 Conflict
                    .body(ApiResponse.failure(
                            "Movie with title '" + movieRequest.getTitle() + "' already exists"));
        }

        long newId = idGen.getAndIncrement();
        Movie newMovie = new Movie(newId, movieRequest.getTitle(), movieRequest.getDirector(),
                movieRequest.getReleaseYear(), movieRequest.getRating());
        store.put(newId, newMovie);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Movie created successfully", newMovie));
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 4: @ResponseStatus ON EXCEPTION CLASSES
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║            @ResponseStatus ON EXCEPTION CLASSES — EXPLAINED                  ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 HOW IT WORKS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * When you throw a @ResponseStatus-annotated exception from a controller,
 * Spring AUTOMATICALLY sends the annotated status code in the response.
 *
 * Without @ExceptionHandler:
 *   throw new MovieNotFoundException(5L);
 *   → Spring catches it → sees @ResponseStatus(NOT_FOUND) → sends 404 response
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 RECOMMENDED APPROACH (Chapter 4 - ExceptionHandling):
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * While @ResponseStatus on exceptions is convenient, the more robust approach is:
 *   1. Throw plain exceptions with meaningful messages
 *   2. Use @RestControllerAdvice to catch them and build structured error responses
 *
 * This gives you more control over the error response body.
 * See Example04ExceptionHandling.java for the full pattern.
 *
 */
@ResponseStatus(HttpStatus.NOT_FOUND)   // ← When thrown: automatically sends 404 Not Found
class MovieNotFoundException extends RuntimeException {
    private final Long movieId;

    public MovieNotFoundException(Long movieId) {
        super("Movie not found with id: " + movieId);
        this.movieId = movieId;
    }

    public Long getMovieId() { return movieId; }
}

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateMovieTitleException extends RuntimeException {
    public DuplicateMovieTitleException(String title) {
        super("Movie already exists with title: " + title);
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SUMMARY: RESPONSE HANDLING DECISION TREE
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║          RESPONSE HANDLING — THE COMPLETE DECISION TREE                      ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * Q: How should I return my response?
 *
 *   ┌──────────────────────────────────────────────────────────────────────────┐
 *   │ Is the status code ALWAYS the same for this endpoint?                    │
 *   │                                                                          │
 *   │  YES → Use @ResponseStatus(HttpStatus.XXX) on the method                │
 *   │         OR return the data object directly (defaults to 200 OK)         │
 *   │                                                                          │
 *   │  NO  → Could be 200 OR 404? Could be 201 with Location?                 │
 *   │         → Use ResponseEntity<T>                                          │
 *   └──────────────────────────────────────────────────────────────────────────┘
 *
 *   ┌──────────────────────────────────────────────────────────────────────────┐
 *   │ Do you need to set custom HEADERS?                                       │
 *   │                                                                          │
 *   │  YES → Always use ResponseEntity (it has header builders)               │
 *   │  NO  → Either approach works                                             │
 *   └──────────────────────────────────────────────────────────────────────────┘
 *
 *   ┌──────────────────────────────────────────────────────────────────────────┐
 *   │ Do you have a BODY?                                                      │
 *   │                                                                          │
 *   │  YES → ResponseEntity<YourType>.ok(yourObject)                          │
 *   │  NO  → ResponseEntity<Void>.noContent().build()                         │
 *   └──────────────────────────────────────────────────────────────────────────┘
 *
 *  QUICK REFERENCE:
 *
 *  SCENARIO                              RETURN
 *  ──────────────────────────────────    ───────────────────────────────────────────
 *  GET success                           ResponseEntity.ok(data)
 *  GET not found                         ResponseEntity.notFound().build()
 *  POST created                          ResponseEntity.created(uri).body(data)
 *  PUT success                           ResponseEntity.ok(data)
 *  PUT not found                         ResponseEntity.notFound().build()
 *  DELETE success                        ResponseEntity.noContent().build()
 *  DELETE not found                      ResponseEntity.notFound().build()
 *  Conflict (duplicate)                  ResponseEntity.status(CONFLICT).body(err)
 *  Unprocessable (bad input)             ResponseEntity.unprocessableEntity().body(err)
 *  Always 201 Created (simple POST)      @ResponseStatus(CREATED) + return T
 *  Exception → auto status              @ResponseStatus(NOT_FOUND) on exception class
 *
 */
class Example03ResponseHandling {
    // Intentionally empty — documentation class
}

