package com.learning.springboot.chapter03;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║                    SPRING BOOT ANNOTATIONS — COMPREHENSIVE GUIDE                     ║
 * ║                       Chapter 3: Spring MVC & REST Annotations                       ║
 * ║                                                                                       ║
 * ║                           📚 From Zero to Expert 📚                                  ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Chapter:      3
 * Title:        Spring MVC & REST Annotations
 * Difficulty:   ⭐⭐⭐ Intermediate
 * Estimated:    5–8 hours
 * Prerequisites: Chapter 1 (Core Spring Boot), Chapter 2 (Spring Framework Core),
 *               Basic HTTP/REST knowledge, JSON familiarity
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │                                                                                       │
 * │                    CHAPTER 3: OVERVIEW & LEARNING GOALS                              │
 * │                                                                                       │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📖 TABLE OF CONTENTS 📖
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Section  1 :  Chapter Introduction & Overview
 * Section  2 :  The BIG IDEA — HTTP, REST & Spring MVC
 * Section  3 :  Controller Annotations
 *                   → @RestController     (REST API entry point)
 *                   → @Controller         (MVC view-based entry point)
 *                   → @RequestMapping     (URL + method mapping)
 *                   → @GetMapping         (HTTP GET)
 *                   → @PostMapping        (HTTP POST)
 *                   → @PutMapping         (HTTP PUT)
 *                   → @DeleteMapping      (HTTP DELETE)
 *                   → @PatchMapping       (HTTP PATCH)
 * Section  4 :  Request Extraction Annotations
 *                   → @RequestParam       (query string parameters)
 *                   → @PathVariable       (URI template variables)
 *                   → @RequestBody        (HTTP body → Java object)
 *                   → @RequestHeader      (HTTP headers)
 *                   → @CookieValue        (HTTP cookies)
 *                   → @ModelAttribute     (form data binding)
 *                   → @SessionAttribute   (read from HTTP session)
 *                   → @RequestAttribute   (read from request attributes)
 * Section  5 :  Response Annotations
 *                   → @ResponseBody       (Java object → HTTP body)
 *                   → @ResponseStatus     (HTTP status code)
 *                   → ResponseEntity<T>   (full response control)
 * Section  6 :  Exception Handling Annotations
 *                   → @ExceptionHandler   (controller-local error handler)
 *                   → @ControllerAdvice   (global error handler)
 *                   → @RestControllerAdvice (global REST error handler)
 * Section  7 :  Cross-Origin Resource Sharing
 *                   → @CrossOrigin        (per-method / per-class CORS)
 *                   → WebMvcConfigurer    (global CORS)
 * Section  8 :  How Everything Works Together — DispatcherServlet internals
 * Section  9 :  Best Practices & Common Pitfalls
 * Section 10 :  Interview Questions & Answers
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * 📂 FILES IN THIS CHAPTER:
 *
 *  • Chapter03Overview.java                    ← YOU ARE HERE
 *  • Example01ControllerAnnotations.java        (@RestController, @RequestMapping, HTTP methods)
 *  • Example02RequestHandling.java              (@RequestParam, @PathVariable, @RequestBody,
 *                                               @RequestHeader, @CookieValue, @ModelAttribute,
 *                                               @SessionAttribute, @RequestAttribute)
 *  • Example03ResponseHandling.java             (@ResponseBody, @ResponseStatus, ResponseEntity)
 *  • Example04ExceptionHandling.java            (@ExceptionHandler, @ControllerAdvice,
 *                                               @RestControllerAdvice, ProblemDetail)
 *  • Example05CrossOriginAnnotation.java        (@CrossOrigin, global CORS config)
 *  • HowItWorksExplained.java                   (DispatcherServlet deep dive)
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
public class Chapter03Overview {

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
     *  ✓  Build complete REST APIs using Spring MVC annotations
     *  ✓  Map HTTP requests to handler methods with fine-grained control
     *  ✓  Extract data from every part of an HTTP request (URL, body, headers, cookies)
     *  ✓  Control HTTP response status codes, headers, and body format
     *  ✓  Build a centralised, production-grade exception handling strategy
     *  ✓  Enable CORS for modern front-end/back-end separation
     *  ✓  Understand the DispatcherServlet request lifecycle inside out
     *  ✓  Apply REST design best practices in Spring
     *  ✓  Answer Spring MVC interview questions with confidence
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🌟 WHY IS THIS CHAPTER CRITICAL?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Spring MVC annotations are the INTERFACE between your Java code and the HTTP world.
     * Without them, your business logic is inaccessible to the outside world.
     *
     * Every Spring Boot REST API — from a simple CRUD service to a complex microservice
     * platform — uses the annotations in this chapter.
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        SECTION 2: THE BIG IDEA — HTTP, REST & SPRING MVC                    ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 WHAT IS REST?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * REST (Representational State Transfer) is an architectural style for designing
     * networked applications. It uses HTTP as the transport layer.
     *
     * REST PRINCIPLES:
     *   1. Stateless     — Each request is independent (no server-side session per request)
     *   2. Uniform Interface — Resources identified by URLs; standard HTTP methods
     *   3. Client-Server — Separation of concerns between UI and data storage
     *   4. Cacheable     — Responses can be cached
     *   5. Layered System — Client doesn't need to know if it talks to real server or proxy
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 HTTP METHODS → CRUD OPERATIONS → SPRING ANNOTATIONS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ┌─────────────┬─────────────┬────────────────────────┬──────────────────────────┐
     *  │ HTTP METHOD │ CRUD OP     │ SPRING ANNOTATION       │ EXAMPLE URL              │
     *  ├─────────────┼─────────────┼────────────────────────┼──────────────────────────┤
     *  │ GET         │ Read        │ @GetMapping             │ GET /api/posts           │
     *  │ POST        │ Create      │ @PostMapping            │ POST /api/posts          │
     *  │ PUT         │ Replace     │ @PutMapping             │ PUT /api/posts/1         │
     *  │ PATCH       │ Partial Upd │ @PatchMapping           │ PATCH /api/posts/1       │
     *  │ DELETE      │ Delete      │ @DeleteMapping          │ DELETE /api/posts/1      │
     *  │ HEAD        │ Metadata    │ @RequestMapping(HEAD)   │ HEAD /api/posts          │
     *  │ OPTIONS     │ Capabilities│ @RequestMapping(OPTIONS)│ OPTIONS /api/posts       │
     *  └─────────────┴─────────────┴────────────────────────┴──────────────────────────┘
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 HTTP REQUEST ANATOMY — WHERE EACH ANNOTATION READS FROM:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   ┌───────────────────────────────────────────────────────────────────────────┐
     *   │  GET /api/posts/42?include=comments&page=2   HTTP/1.1                     │
     *   │  ├──────────────────────────── URL PATH ───────────────────────────────   │
     *   │  │  /api/posts/{id}  ← @PathVariable reads "42"                          │
     *   │  └──────────────────────────────────────────────────────────────────────  │
     *   │  ├──────────────────── QUERY STRING ────────────────────────────────────  │
     *   │  │  ?include=comments&page=2  ← @RequestParam reads these                │
     *   │  └──────────────────────────────────────────────────────────────────────  │
     *   │  Host: api.example.com                                                    │
     *   │  Authorization: Bearer eyJhbGc...  ← @RequestHeader reads headers        │
     *   │  Cookie: sessionId=abc123          ← @CookieValue reads cookies          │
     *   │  Content-Type: application/json                                           │
     *   │                                                                           │
     *   │  { "title": "Hello", "content": "World" }  ← @RequestBody reads body    │
     *   └───────────────────────────────────────────────────────────────────────────┘
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 HTTP STATUS CODE REFERENCE (for @ResponseStatus):
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ┌────────┬──────────────────────────────────────────────────────────────────┐
     *  │  2xx   │  SUCCESS                                                         │
     *  │  200   │  OK — Standard success for GET, PUT, PATCH                       │
     *  │  201   │  Created — Resource was created (POST)                           │
     *  │  204   │  No Content — Success but no body (DELETE, some PUT)             │
     *  ├────────┼──────────────────────────────────────────────────────────────────┤
     *  │  3xx   │  REDIRECTION                                                     │
     *  │  301   │  Moved Permanently                                               │
     *  │  302   │  Found (temporary redirect)                                      │
     *  ├────────┼──────────────────────────────────────────────────────────────────┤
     *  │  4xx   │  CLIENT ERRORS                                                   │
     *  │  400   │  Bad Request — Malformed request / validation failure            │
     *  │  401   │  Unauthorized — Authentication required                          │
     *  │  403   │  Forbidden — Authenticated but not allowed                       │
     *  │  404   │  Not Found — Resource doesn't exist                              │
     *  │  405   │  Method Not Allowed — Wrong HTTP method                          │
     *  │  409   │  Conflict — Resource already exists / state conflict             │
     *  │  422   │  Unprocessable Entity — Semantic validation failure              │
     *  │  429   │  Too Many Requests — Rate limiting                               │
     *  ├────────┼──────────────────────────────────────────────────────────────────┤
     *  │  5xx   │  SERVER ERRORS                                                   │
     *  │  500   │  Internal Server Error — Unexpected server failure               │
     *  │  502   │  Bad Gateway — Upstream service failed                           │
     *  │  503   │  Service Unavailable — Server is overloaded / maintenance        │
     *  └────────┴──────────────────────────────────────────────────────────────────┘
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🗺️ THE MVC ARCHITECTURE — HOW SPRING MVC FITS IN:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *                      CLIENT (Browser / Mobile / Other Service)
     *                                      │
     *                               HTTP Request
     *                                      │
     *                                      ▼
     *                   ┌────────────────────────────────────┐
     *                   │         DISPATCHER SERVLET          │
     *                   │   (The Front Controller)            │
     *                   │   Receives ALL HTTP requests        │
     *                   └──────────────────┬─────────────────┘
     *                                      │
     *                          Finds the right handler
     *                                      │
     *                    ┌─────────────────▼───────────────────┐
     *                    │           @RestController            │
     *                    │         Handler Method               │
     *                    │   @GetMapping / @PostMapping / etc.  │
     *                    │                                      │
     *                    │  Extracts:                           │
     *                    │    @PathVariable  ← URL path         │
     *                    │    @RequestParam  ← query string     │
     *                    │    @RequestBody   ← JSON body        │
     *                    │    @RequestHeader ← headers          │
     *                    └─────────────────┬───────────────────┘
     *                                      │
     *                              calls service layer
     *                                      │
     *                    ┌─────────────────▼───────────────────┐
     *                    │           @Service                   │
     *                    │       (Business Logic)               │
     *                    └─────────────────┬───────────────────┘
     *                                      │
     *                    ┌─────────────────▼───────────────────┐
     *                    │           @Repository                │
     *                    │          (Data Access)               │
     *                    └─────────────────┬───────────────────┘
     *                                      │
     *                              returns result
     *                                      │
     *                    ┌─────────────────▼───────────────────┐
     *                    │       @ResponseBody / ResponseEntity │
     *                    │    Jackson serialises to JSON        │
     *                    └─────────────────┬───────────────────┘
     *                                      │
     *                               HTTP Response
     *                                      │
     *                                      ▼
     *                      CLIENT receives JSON/XML response
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║              SECTION 3: CONTROLLER ANNOTATIONS — QUICK REFERENCE            ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * @Controller  vs  @RestController:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @Controller  →  Returns a VIEW NAME (for Thymeleaf / JSP / Freemarker)
     *
     *       @Controller
     *       class PageController {
     *           @GetMapping("/home")
     *           String home(Model model) {
     *               model.addAttribute("user", "Alice");
     *               return "home";              ← View name "home" → home.html
     *           }
     *       }
     *
     *   @RestController  →  Returns DATA directly (serialised to JSON/XML)
     *   = @Controller + @ResponseBody on every method
     *
     *       @RestController
     *       class ApiController {
     *           @GetMapping("/users/1")
     *           User getUser() {
     *               return new User(1, "Alice");  ← Serialised to {"id":1,"name":"Alice"}
     *           }
     *       }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * @RequestMapping attributes:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   value / path  → URL pattern(s)  e.g.  "/posts", {"/posts", "/articles"}
     *   method        → HTTP method(s)  e.g.  RequestMethod.GET
     *   params        → Required query params  e.g.  "version=2"
     *   headers       → Required headers      e.g.  "X-Custom-Header=value"
     *   consumes      → Required Content-Type e.g.  "application/json"
     *   produces      → Accepted Accept       e.g.  "application/json"
     *   name          → Symbolic name for the mapping
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * @GetMapping / @PostMapping / etc. are SHORTCUTS for @RequestMapping:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @GetMapping("/posts/{id}")
     *   // ≡
     *   @RequestMapping(value = "/posts/{id}", method = RequestMethod.GET)
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║              SECTION 4: REQUEST EXTRACTION — QUICK REFERENCE                ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     *  ┌──────────────────────┬────────────────────────────┬─────────────────────────┐
     *  │ ANNOTATION           │ READS FROM                 │ EXAMPLE                 │
     *  ├──────────────────────┼────────────────────────────┼─────────────────────────┤
     *  │ @PathVariable        │ URL path segment           │ /posts/{id}             │
     *  │ @RequestParam        │ Query string (?key=val)    │ /posts?page=2           │
     *  │ @RequestBody         │ HTTP body (JSON/XML)       │ { "title": "Hello" }    │
     *  │ @RequestHeader       │ HTTP headers               │ Authorization: Bearer.. │
     *  │ @CookieValue         │ HTTP cookies               │ Cookie: sessionId=abc   │
     *  │ @ModelAttribute      │ Form fields (URL-encoded)  │ title=Hello&body=World  │
     *  │ @SessionAttribute    │ HttpSession attribute      │ session.getAttribute()  │
     *  │ @RequestAttribute    │ Request attribute          │ request.getAttribute()  │
     *  └──────────────────────┴────────────────────────────┴─────────────────────────┘
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║              SECTION 5: RESPONSE — QUICK REFERENCE                          ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     *  ┌──────────────────────────────┬────────────────────────────────────────────┐
     *  │ TECHNIQUE                    │ WHEN TO USE                                │
     *  ├──────────────────────────────┼────────────────────────────────────────────┤
     *  │ Return POJO directly         │ Always 200 OK; let Spring serialise it     │
     *  │ @ResponseStatus(CREATED)     │ Fixed status code on method/exception      │
     *  │ ResponseEntity<T>            │ Dynamic status + custom headers + body     │
     *  │ ResponseEntity.noContent()   │ 204 No Content (DELETE, some PATCH)        │
     *  │ ResponseEntity.notFound()    │ 404 when resource missing                  │
     *  │ ProblemDetail (RFC 7807)     │ Standardised JSON error body               │
     *  └──────────────────────────────┴────────────────────────────────────────────┘
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║              SECTION 9: BEST PRACTICES QUICK REFERENCE                      ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * DO's ✅
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ✅  Use @RestController for REST APIs (not @Controller + @ResponseBody)
     *  ✅  Version your API from day one: /api/v1/... (avoid breaking clients)
     *  ✅  Use @ResponseStatus(HttpStatus.CREATED) for POST endpoints that create resources
     *  ✅  Return ResponseEntity for endpoints that might return 404 or other dynamic status
     *  ✅  Use @Valid + Jakarta Bean Validation on @RequestBody DTOs
     *  ✅  Use @RestControllerAdvice for centralised, consistent error responses
     *  ✅  Return the created resource URI in Location header for 201 responses
     *  ✅  Use specific HTTP method annotations (@GetMapping, not @RequestMapping)
     *  ✅  Use @PathVariable for resource identity, @RequestParam for filtering/sorting
     *  ✅  Configure global CORS instead of per-controller @CrossOrigin
     *
     * DON'Ts ❌
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ❌  Don't use @RequestMapping without method — it maps ALL HTTP methods
     *  ❌  Don't return null from controller methods — use Optional / 404 properly
     *  ❌  Don't expose internal exception stack traces to clients
     *  ❌  Don't mix @Controller and @RestController responsibilities in one class
     *  ❌  Don't put business logic in controllers — keep them thin
     *  ❌  Don't use @RequestBody for simple key-value form posts (use @ModelAttribute)
     *  ❌  Don't allow * CORS origins in production (be explicit)
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║              SECTION 10: TOP INTERVIEW QUESTIONS — CHAPTER 3                ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Q1:  What is the difference between @Controller and @RestController?
     * A1:  @RestController = @Controller + @ResponseBody on every method.
     *      @Controller is for MVC apps with view templates (Thymeleaf, JSP).
     *      @RestController is for REST APIs that return data (JSON/XML) directly.
     *
     * Q2:  What is the difference between @RequestParam and @PathVariable?
     * A2:  @PathVariable reads a value embedded in the URL path: /posts/{id}.
     *      @RequestParam reads a value from the query string: /posts?page=2.
     *      Use @PathVariable for resource identity; @RequestParam for filters/pagination.
     *
     * Q3:  What is ResponseEntity and when should you use it?
     * A3:  ResponseEntity gives full control over the HTTP response: status, headers, body.
     *      Use it when the status code must be dynamic (e.g., 200 vs 404), when you need
     *      to set custom headers (e.g., Location on 201 Created), or when the body can be
     *      absent (e.g., 204 No Content on DELETE).
     *
     * Q4:  What is @ControllerAdvice and how does it work?
     * A4:  @ControllerAdvice marks a class as a global exception handler (and model
     *      pre-processor). @ExceptionHandler methods inside it handle exceptions thrown
     *      by ANY controller in the application. Spring's DispatcherServlet checks
     *      ControllerAdvice beans after a controller throws an exception.
     *
     * Q5:  What is CORS and how do you enable it in Spring Boot?
     * A5:  CORS (Cross-Origin Resource Sharing) is a browser security mechanism that
     *      blocks JS from fetching data from a different origin. Spring Boot solutions:
     *      1. @CrossOrigin on a method or controller (fine-grained)
     *      2. WebMvcConfigurer.addCorsMappings() (global, recommended for production)
     *      3. Spring Security's CorsConfigurationSource (when security is enabled)
     *
     * Q6:  What is the difference between @RequestBody and @ModelAttribute?
     * A6:  @RequestBody reads the HTTP body (typically JSON) and deserialises it to a Java
     *      object using Jackson/MessageConverter. Used with Content-Type: application/json.
     *      @ModelAttribute reads HTML form fields (application/x-www-form-urlencoded) and
     *      binds them to a Java object. It also adds the object to the MVC model.
     *
     * Q7:  What is the DispatcherServlet?
     * A7:  The DispatcherServlet is the FRONT CONTROLLER of Spring MVC. All HTTP requests
     *      go through it. It delegates to HandlerMapping (find which method handles this
     *      URL), HandlerAdapter (invoke the method), and ViewResolver (find the template)
     *      or MessageConverter (serialise the return value to JSON/XML).
     *
     * Q8:  How do you return a 201 Created with a Location header from a POST endpoint?
     * A8:  Use ResponseEntity:
     *          URI location = ServletUriComponentsBuilder.fromCurrentRequest()
     *              .path("/{id}").buildAndExpand(savedResource.getId()).toUri();
     *          return ResponseEntity.created(location).body(savedResource);
     *
     * Q9:  What is the difference between @ExceptionHandler on a controller vs in
     *      a @ControllerAdvice?
     * A9:  @ExceptionHandler inside a @Controller handles exceptions thrown by THAT
     *      controller only. @ExceptionHandler inside a @ControllerAdvice handles
     *      exceptions thrown by ANY controller in the application context. Use
     *      @ControllerAdvice for centralised, consistent error handling.
     *
     * Q10: What is @RestControllerAdvice?
     * A10: @RestControllerAdvice = @ControllerAdvice + @ResponseBody.
     *      All @ExceptionHandler methods in it automatically write their return value
     *      to the HTTP response body as JSON/XML (no need for @ResponseBody on each).
     *
     */
}

