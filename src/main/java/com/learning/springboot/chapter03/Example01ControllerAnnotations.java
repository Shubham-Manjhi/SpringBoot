package com.learning.springboot.chapter03;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║          EXAMPLE 01: CONTROLLER ANNOTATIONS IN ACTION                                ║
 * ║          @RestController · @RequestMapping · @GetMapping · @PostMapping              ║
 * ║          @PutMapping · @DeleteMapping · @PatchMapping                                ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example01ControllerAnnotations.java
 * Purpose:     Build a complete Blog Post REST API demonstrating every controller
 *              annotation. After reading this file, you can build any CRUD REST API.
 * Difficulty:  ⭐⭐ Beginner–Intermediate
 * Time:        30 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * WHAT WE ARE BUILDING:
 *   A Blog Post Management REST API with full CRUD:
 *
 *   GET    /api/v1/posts          → List all posts
 *   GET    /api/v1/posts/{id}     → Get a single post by ID
 *   POST   /api/v1/posts          → Create a new post
 *   PUT    /api/v1/posts/{id}     → Fully replace a post
 *   PATCH  /api/v1/posts/{id}/publish  → Partially update (publish)
 *   DELETE /api/v1/posts/{id}     → Delete a post
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  DOMAIN MODEL
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * Blog Post status enum.
 */
enum PostStatus { DRAFT, PUBLISHED, ARCHIVED }

/**
 * The BlogPost domain entity.
 * Stored in an in-memory map (simulating a database).
 */
class BlogPost {
    private Long   id;
    private String title;
    private String content;
    private String author;
    private PostStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BlogPost() {}

    public BlogPost(Long id, String title, String content, String author) {
        this.id        = id;
        this.title     = title;
        this.content   = content;
        this.author    = author;
        this.status    = PostStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters & setters (omitting verbose Lombok for clarity)
    public Long          getId()        { return id; }
    public String        getTitle()     { return title; }
    public String        getContent()   { return content; }
    public String        getAuthor()    { return author; }
    public PostStatus    getStatus()    { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setTitle(String title)       { this.title   = title; }
    public void setContent(String content)   { this.content = content; }
    public void setAuthor(String author)     { this.author  = author; }
    public void setStatus(PostStatus status) { this.status  = status; }
    public void setUpdatedAt(LocalDateTime t){ this.updatedAt = t; }
}

/**
 * DTO for creating a new post.
 * DTOs keep the API contract separate from the domain model.
 */
class CreateBlogPostRequest {
    private String title;
    private String content;
    private String author;

    public String getTitle()   { return title; }
    public String getContent() { return content; }
    public String getAuthor()  { return author; }
    public void setTitle(String title)     { this.title   = title; }
    public void setContent(String content) { this.content = content; }
    public void setAuthor(String author)   { this.author  = author; }
}

/**
 * DTO for fully replacing a post (PUT).
 */
class UpdateBlogPostRequest {
    private String title;
    private String content;

    public String getTitle()   { return title; }
    public String getContent() { return content; }
    public void setTitle(String title)     { this.title   = title; }
    public void setContent(String content) { this.content = content; }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 1: @RestController — THE ENTRY POINT OF A REST API
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                        @RestController  EXPLAINED                            ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @RestController = @Controller + @ResponseBody
 *
 * It is a CONVENIENCE annotation that tells Spring:
 *   1. This class is a Spring MVC controller (@Controller)
 *   2. Every method's return value is written directly to the HTTP response
 *      body (@ResponseBody) — NOT resolved as a view template name
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 WHAT HAPPENS BEHIND THE SCENES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   Your controller method returns: BlogPost{id=1, title="Hello"}
 *        ↓
 *   Jackson's MappingJackson2HttpMessageConverter serialises it
 *        ↓
 *   HTTP response body: {"id":1,"title":"Hello","content":"...","author":"..."}
 *   Content-Type: application/json
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                      @RequestMapping  EXPLAINED (class level)                ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @RequestMapping at CLASS LEVEL sets the BASE PATH for all handler methods.
 * Every method in this class automatically gets this prefix.
 *
 * Without class-level @RequestMapping:
 *   @GetMapping("/api/v1/posts")           ← full path on every method
 *   @PostMapping("/api/v1/posts")
 *   @GetMapping("/api/v1/posts/{id}")
 *
 * With class-level @RequestMapping("/api/v1/posts"):
 *   @GetMapping            ← just "/"  relative to class path
 *   @PostMapping           ← just "/"
 *   @GetMapping("/{id}")   ← just "/{id}"  — much cleaner!
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 💡 API VERSIONING — WHY "/api/v1/":
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Version your API from day ONE. When you make breaking changes:
 *   → Create /api/v2/posts controller alongside /api/v1/posts
 *   → Existing clients stay on v1; new clients use v2
 *   → Zero downtime migration
 *
 */
@RestController                      // ← REST API entry point: @Controller + @ResponseBody
@RequestMapping("/api/v1/posts")     // ← All URLs in this class start with /api/v1/posts
class BlogPostController {

    // ─────────────────────────────────────────────────────────────────────────────
    // In-memory "database" — in production, inject a @Repository
    // ─────────────────────────────────────────────────────────────────────────────
    private final Map<Long, BlogPost> postStore    = new ConcurrentHashMap<>();
    private final AtomicLong          idGenerator  = new AtomicLong(1);

    public BlogPostController() {
        // Seed with sample data
        BlogPost p1 = new BlogPost(idGenerator.getAndIncrement(),
                "Spring Boot Basics", "Introduction to Spring Boot...", "Alice");
        BlogPost p2 = new BlogPost(idGenerator.getAndIncrement(),
                "REST API Design",    "Best practices for REST APIs...", "Bob");
        p2.setStatus(PostStatus.PUBLISHED);
        postStore.put(p1.getId(), p1);
        postStore.put(p2.getId(), p2);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  @GetMapping — HTTP GET (Read)
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                         @GetMapping  EXPLAINED                               ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @GetMapping is a SHORTCUT for @RequestMapping(method = RequestMethod.GET).
     * It maps HTTP GET requests to this handler method.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 WHEN TO USE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  •  Fetching a list of resources
     *  •  Fetching a single resource by ID
     *  •  Safe operations (GET should never modify server state)
     *  •  Idempotent operations (same result no matter how many times called)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔧 FULL SYNTAX:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @GetMapping(
     *       value   = "/path",               ← URL (can be array)
     *       params  = "version=2",           ← Only match if ?version=2 in query
     *       headers = "X-API-Version=2",     ← Only match if header present
     *       produces = "application/json"    ← Only match if Accept: application/json
     *   )
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * GET /api/v1/posts
     *
     * Returns all blog posts.
     * HTTP 200 OK with JSON array body.
     */
    @GetMapping   // ← equivalent to @RequestMapping(method = RequestMethod.GET) on the class path
    public List<BlogPost> getAllPosts() {
        System.out.println("📥 GET /api/v1/posts — returning all posts");
        return List.copyOf(postStore.values());
    }

    /**
     * GET /api/v1/posts/{id}
     *
     * Returns a single blog post by its ID.
     *
     * Return type: ResponseEntity<BlogPost>
     *   → Allows us to return 200 OK with a body OR 404 Not Found without a body.
     *   → Pure method return (BlogPost) would always force 200 OK.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 RESPONSEENTITY — when you need dynamic status codes:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   ResponseEntity.ok(body)           → 200 OK with body
     *   ResponseEntity.notFound().build() → 404 Not Found, no body
     *   ResponseEntity.status(201).body() → custom status + body
     *   ResponseEntity.noContent().build()→ 204 No Content, no body
     *
     */
    @GetMapping("/{id}")   // ← {id} is a URI template variable — see @PathVariable in Example02
    public ResponseEntity<BlogPost> getPostById(@PathVariable Long id) {
        System.out.println("📥 GET /api/v1/posts/" + id);
        return Optional.ofNullable(postStore.get(id))
                .map(ResponseEntity::ok)                         // 200 OK
                .orElse(ResponseEntity.notFound().build());      // 404 Not Found
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  @PostMapping — HTTP POST (Create)
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                         @PostMapping  EXPLAINED                              ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @PostMapping maps HTTP POST requests to this handler method.
     * POST is used to CREATE a new resource.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 CORRECT STATUS CODE FOR POST:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   201 Created   → Resource was successfully created (REST best practice)
     *   Location header → URI of the newly created resource
     *
     *   HTTP/1.1 201 Created
     *   Location: /api/v1/posts/3
     *   Content-Type: application/json
     *
     *   { "id": 3, "title": "New Post", ... }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 How to BUILD the Location header:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   ServletUriComponentsBuilder.fromCurrentRequest()  ← current request URI
     *       .path("/{id}")                                 ← append /{id}
     *       .buildAndExpand(savedPost.getId())             ← fill in {id}
     *       .toUri()                                       ← build URI object
     *   → produces: http://localhost:8080/api/v1/posts/3
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * POST /api/v1/posts
     * Content-Type: application/json
     * Body: { "title": "...", "content": "...", "author": "..." }
     *
     * Returns: 201 Created with Location header and created post body.
     */
    @PostMapping(consumes = "application/json")   // ← only accepts JSON body
    public ResponseEntity<BlogPost> createPost(@RequestBody CreateBlogPostRequest request) {
        System.out.println("📥 POST /api/v1/posts — creating: " + request.getTitle());

        // Create domain object from DTO
        BlogPost newPost = new BlogPost(
                idGenerator.getAndIncrement(),
                request.getTitle(),
                request.getContent(),
                request.getAuthor()
        );
        postStore.put(newPost.getId(), newPost);

        // Build Location header: /api/v1/posts/3
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()          // Start from current request URL
                .path("/{id}")                 // Append /{id} template
                .buildAndExpand(newPost.getId()) // Replace {id} with actual id
                .toUri();                       // Build URI object

        return ResponseEntity
                .created(location)            // 201 Created + Location header
                .body(newPost);               // Response body = created post JSON
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  @PutMapping — HTTP PUT (Full Replace)
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                         @PutMapping  EXPLAINED                               ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @PutMapping maps HTTP PUT requests. PUT means FULL REPLACEMENT of a resource.
     * You must supply ALL fields — missing fields are set to null/default.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * PUT vs PATCH:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   PUT   → Replace the entire resource. Client sends the COMPLETE representation.
     *           PUT /api/v1/posts/1  Body: { "title": "New Title", "content": "New body" }
     *           → BOTH title AND content replaced. Any missing field = gone.
     *
     *   PATCH → Partially update. Client sends ONLY the fields to change.
     *           PATCH /api/v1/posts/1  Body: { "status": "PUBLISHED" }
     *           → Only status changes; title and content stay the same.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * PUT /api/v1/posts/{id}
     * Body: { "title": "...", "content": "..." }
     *
     * Returns: 200 OK with updated post, or 404 if not found.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BlogPost> replacePost(@PathVariable Long id,
                                                @RequestBody UpdateBlogPostRequest request) {
        System.out.println("📥 PUT /api/v1/posts/" + id + " — full replace");
        BlogPost existing = postStore.get(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();  // 404 Not Found
        }

        // FULL REPLACEMENT — every field is overwritten
        existing.setTitle(request.getTitle());
        existing.setContent(request.getContent());
        existing.setUpdatedAt(LocalDateTime.now());
        postStore.put(id, existing);

        return ResponseEntity.ok(existing);  // 200 OK
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  @PatchMapping — HTTP PATCH (Partial Update)
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                        @PatchMapping  EXPLAINED                              ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @PatchMapping maps HTTP PATCH requests. PATCH means PARTIAL UPDATE of a resource.
     * Only the fields supplied in the request body are changed.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 COMMON USE CASES FOR PATCH:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  •  Changing a single field: PATCH /posts/1  { "status": "PUBLISHED" }
     *  •  Toggling a flag:         PATCH /users/1  { "active": false }
     *  •  Incrementing a counter:  PATCH /posts/1/views  (no body needed)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * PATCH /api/v1/posts/{id}/publish
     *
     * Publishes a draft post. Changes status from DRAFT → PUBLISHED.
     * No request body needed — the action is self-descriptive in the URL.
     * Returns: 200 OK with updated post.
     */
    @PatchMapping("/{id}/publish")   // ← Action-based PATCH: /publish
    public ResponseEntity<BlogPost> publishPost(@PathVariable Long id) {
        System.out.println("📥 PATCH /api/v1/posts/" + id + "/publish");
        BlogPost post = postStore.get(id);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        if (post.getStatus() != PostStatus.DRAFT) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)  // 409 — already published or archived
                    .body(post);
        }
        post.setStatus(PostStatus.PUBLISHED);
        post.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(post);
    }

    /**
     * PATCH /api/v1/posts/{id}
     *
     * General partial update — demonstrates Map<String, Object> for dynamic fields.
     * Client sends only the fields to change; others are left untouched.
     *
     * Body example: { "title": "Updated Title" }
     *               (only title changes, content stays the same)
     */
    @PatchMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<BlogPost> partialUpdatePost(@PathVariable Long id,
                                                      @RequestBody Map<String, Object> updates) {
        System.out.println("📥 PATCH /api/v1/posts/" + id + " — partial update: " + updates.keySet());
        BlogPost post = postStore.get(id);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        // Apply only the provided fields
        if (updates.containsKey("title"))   post.setTitle((String) updates.get("title"));
        if (updates.containsKey("content")) post.setContent((String) updates.get("content"));
        post.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(post);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  @DeleteMapping — HTTP DELETE (Remove)
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                       @DeleteMapping  EXPLAINED                              ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @DeleteMapping maps HTTP DELETE requests. Used to delete/remove a resource.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 CORRECT STATUS CODE FOR DELETE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   204 No Content  → Resource deleted, no body returned    (recommended)
     *   200 OK          → Resource deleted, return confirmation body (optional)
     *   404 Not Found   → Resource didn't exist in the first place
     *
     * IDEMPOTENCY: DELETE is idempotent.
     *   First call  → 204 No Content (resource deleted)
     *   Second call → 404 Not Found (already gone)
     *   Both are "correct" — the resource is gone either way.
     *   Some APIs always return 204 even if the resource didn't exist (less strict).
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * DELETE /api/v1/posts/{id}
     * Returns: 204 No Content on success, 404 if not found.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        System.out.println("📥 DELETE /api/v1/posts/" + id);
        BlogPost removed = postStore.remove(id);
        if (removed == null) {
            return ResponseEntity.notFound().build();  // 404
        }
        return ResponseEntity.noContent().build();     // 204 — success, no body
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  @RequestMapping — ADVANCED USAGE
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║              @RequestMapping ADVANCED ATTRIBUTES EXPLAINED                   ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * DEMONSTRATION: Content negotiation with consumes/produces.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 consumes — The Content-Type the endpoint ACCEPTS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   consumes = "application/json"    ← only accepts JSON request body
     *   consumes = {"application/json", "application/xml"}  ← accepts both
     *
     *   If client sends Content-Type: text/plain → 415 Unsupported Media Type
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 produces — The Content-Type the endpoint RETURNS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   produces = "application/json"    ← always returns JSON
     *   produces = {"application/json", "application/xml"}  ← negotiates with Accept header
     *
     *   If client sends Accept: text/html → 406 Not Acceptable
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 headers — Require a specific HTTP header to be present:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   headers = "X-API-Version=2"   ← only match if X-API-Version: 2 header present
     *   headers = "!X-Deprecated"     ← only match if X-Deprecated header is NOT present
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 params — Require specific query parameters:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   params = "version=2"          ← only match if ?version=2 in URL
     *   params = "!debug"             ← only match if ?debug is NOT present
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * GET /api/v1/posts/summary
     *   → only responds when Accept header is application/json
     */
    @RequestMapping(
            value    = "/summary",
            method   = RequestMethod.GET,
            produces = "application/json"     // ← Content negotiation: only return JSON
    )
    public List<Map<String, Object>> getPostsSummary() {
        System.out.println("📥 GET /api/v1/posts/summary (content negotiation demo)");
        return postStore.values().stream()
                .map(p -> Map.<String, Object>of(
                        "id",     p.getId(),
                        "title",  p.getTitle(),
                        "author", p.getAuthor(),
                        "status", p.getStatus()
                ))
                .toList();
    }

    /**
     * GET /api/v1/posts/v2 — header-based API versioning.
     *
     * This endpoint only matches when the client sends:
     *   X-API-Version: 2
     *
     * Standard API versioning strategies:
     *   1. URL versioning:    /api/v1/posts, /api/v2/posts       ← most visible
     *   2. Header versioning: X-API-Version: 2                   ← clean URLs
     *   3. Query param:       /api/posts?version=2               ← simple but ugly
     *   4. Content type:      Accept: application/vnd.api+json;v=2  ← very strict
     */
    @RequestMapping(
            value   = "/versioned",
            method  = RequestMethod.GET,
            headers = "X-API-Version=2"    // ← Only matches requests with this header
    )
    public Map<String, Object> getPostsV2() {
        return Map.of(
                "version",    2,
                "posts",      postStore.size(),
                "apiFeatures", List.of("pagination", "filtering", "sorting")
        );
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 5: @Controller — MVC VIEW-BASED CONTROLLER
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                          @Controller  EXPLAINED                              ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @Controller is the traditional Spring MVC controller for TEMPLATE-BASED views
 * (Thymeleaf, FreeMarker, JSP). Handler methods return a VIEW NAME (String) which
 * the ViewResolver resolves to a template file.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 WHEN TO USE @Controller:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  Server-side rendered HTML applications (Thymeleaf/JSP)
 *  •  When you need to populate a Model for template rendering
 *  •  Form-handling with @ModelAttribute
 *  •  Mixed applications (some endpoints return JSON, some return views)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * COMPARISON TABLE:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  @Controller                         @RestController
 *  ─────────────────────────────────   ─────────────────────────────────────────
 *  Returns VIEW NAME (String)          Returns DATA (serialised to JSON/XML)
 *  ViewResolver resolves view          HttpMessageConverter serialises data
 *  @ResponseBody needed for JSON       @ResponseBody built-in
 *  Used for HTML pages (Thymeleaf)     Used for REST APIs
 *  Works with Model / ModelMap         Works with DTOs / domain objects
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * NOTE: This controller is commented out to avoid Thymeleaf dependency issues.
 *       In a real project with spring-boot-starter-thymeleaf, this would work.
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   @Controller
 *   @RequestMapping("/blog")
 *   class BlogViewController {
 *
 *       @GetMapping("/posts")
 *       String listPosts(Model model) {
 *           model.addAttribute("posts", service.getAllPosts());
 *           return "posts/list";     // ← Resolves to: templates/posts/list.html
 *       }
 *
 *       @GetMapping("/posts/{id}")
 *       String viewPost(@PathVariable Long id, Model model) {
 *           model.addAttribute("post", service.getPost(id));
 *           return "posts/detail";   // ← Resolves to: templates/posts/detail.html
 *       }
 *
 *       @GetMapping("/posts/{id}/json")
 *       @ResponseBody                 // ← Override @Controller — return JSON, not view
 *       BlogPost getPostJson(@PathVariable Long id) {
 *           return service.getPost(id);
 *       }
 *   }
 */
class Example01ControllerAnnotations {
    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 🔑 COMPLETE PICTURE — HOW CONTROLLER ANNOTATIONS WORK TOGETHER:
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * @RestController        → Marks this class as a REST API controller
     *     │
     *     └─ @Controller     → Detected by DispatcherServlet as a request handler
     *     └─ @ResponseBody   → Applied to ALL methods; return value → response body
     *
     * @RequestMapping("/api/v1/posts")  → Base path for all methods in the class
     *
     * @GetMapping          → HTTP GET  → getAllPosts(), getPostById()
     * @PostMapping         → HTTP POST → createPost()
     * @PutMapping          → HTTP PUT  → replacePost()
     * @PatchMapping        → HTTP PATCH → publishPost(), partialUpdatePost()
     * @DeleteMapping       → HTTP DELETE → deletePost()
     *
     * COMPLETE CRUD ENDPOINT MAP:
     * ─────────────────────────────────────────────────────────────────────────────
     *   METHOD   URL                          HANDLER          RESPONSE
     *   ──────   ──────────────────────────   ───────────────  ──────────────
     *   GET      /api/v1/posts                getAllPosts       200 [...]
     *   GET      /api/v1/posts/{id}           getPostById      200 {} / 404
     *   GET      /api/v1/posts/summary        getPostsSummary  200 [...]
     *   GET      /api/v1/posts/versioned      getPostsV2       200 {}
     *   POST     /api/v1/posts                createPost       201 {}
     *   PUT      /api/v1/posts/{id}           replacePost      200 {} / 404
     *   PATCH    /api/v1/posts/{id}           partialUpdate    200 {} / 404
     *   PATCH    /api/v1/posts/{id}/publish   publishPost      200 {} / 404 / 409
     *   DELETE   /api/v1/posts/{id}           deletePost       204 / 404
     * ─────────────────────────────────────────────────────────────────────────────
     */
}

