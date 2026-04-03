package com.learning.springboot.chapter03;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║            EXAMPLE 05: @CrossOrigin — CORS IN ACTION                                 ║
 * ║            @CrossOrigin (method) · @CrossOrigin (class) · Global WebMvcConfigurer    ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example05CrossOriginAnnotation.java
 * Purpose:     Understand CORS — what it is, why it matters, and how to configure
 *              it at every level in Spring Boot (method, class, global).
 * Difficulty:  ⭐⭐ Beginner–Intermediate
 * Time:        20 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 1: CORS FUNDAMENTALS
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                     WHAT IS CORS? — THE FULL PICTURE                         ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * CORS = Cross-Origin Resource Sharing
 *
 * A BROWSER SECURITY MECHANISM that restricts JavaScript from making HTTP requests
 * to a DIFFERENT ORIGIN (domain, port, or protocol) than the one that served the webpage.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🌐 WHAT IS AN "ORIGIN"?
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Origin = PROTOCOL + HOSTNAME + PORT
 *
 *   https://myapp.com:443   → Origin 1
 *   https://myapp.com:8080  → DIFFERENT ORIGIN (different port)
 *   http://myapp.com:443    → DIFFERENT ORIGIN (different protocol)
 *   https://api.myapp.com   → DIFFERENT ORIGIN (different subdomain)
 *   https://other.com       → DIFFERENT ORIGIN (different domain)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🚫 THE SAME-ORIGIN POLICY:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * By default, browsers enforce the Same-Origin Policy:
 *
 *   Your page:  https://myapp.com:3000  (React/Vue/Angular frontend)
 *   Your API:   https://api.myapp.com:8080  (Spring Boot backend)
 *
 *   When JavaScript on https://myapp.com:3000 calls https://api.myapp.com:8080:
 *     → Browser blocks the request! ❌ (different origin)
 *     → Error in console: "Access to fetch at ... has been blocked by CORS policy"
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ✅ HOW CORS SOLVES IT — The Preflight Dance:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   SIMPLE REQUEST (GET, POST with simple headers):
 *   ──────────────────────────────────────────────
 *   1. Browser: GET https://api.myapp.com/data
 *      + Origin: https://myapp.com
 *   2. Server:  200 OK
 *      + Access-Control-Allow-Origin: https://myapp.com  ← Server allows it
 *   3. Browser: ✅ Allows JavaScript to read the response
 *
 *
 *   COMPLEX REQUEST (PUT/DELETE/custom headers/JSON body):
 *   ──────────────────────────────────────────────────────
 *   1. Browser PREFLIGHT:
 *      OPTIONS https://api.myapp.com/data
 *      Origin: https://myapp.com
 *      Access-Control-Request-Method: DELETE
 *      Access-Control-Request-Headers: Authorization, Content-Type
 *
 *   2. Server PREFLIGHT RESPONSE:
 *      204 No Content
 *      Access-Control-Allow-Origin: https://myapp.com
 *      Access-Control-Allow-Methods: GET, POST, PUT, DELETE
 *      Access-Control-Allow-Headers: Authorization, Content-Type
 *      Access-Control-Max-Age: 3600  ← Cache preflight for 1 hour
 *
 *   3. Browser sends the ACTUAL request:
 *      DELETE https://api.myapp.com/data/1
 *
 *   4. Server responds: 204 No Content
 *
 *   5. Browser: ✅ Allows JavaScript to read the response
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 KEY CORS RESPONSE HEADERS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   Access-Control-Allow-Origin:    Which origins are allowed
 *   Access-Control-Allow-Methods:   Which HTTP methods are allowed
 *   Access-Control-Allow-Headers:   Which request headers are allowed
 *   Access-Control-Expose-Headers:  Which response headers JS can read
 *   Access-Control-Allow-Credentials: Allow cookies/auth headers
 *   Access-Control-Max-Age:         How long to cache preflight (seconds)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ IMPORTANT: CORS is a BROWSER security feature — it does NOT protect your API
 * from non-browser clients (curl, Postman, server-to-server). For API security,
 * use authentication/authorization (Spring Security + JWT/OAuth2).
 * ─────────────────────────────────────────────────────────────────────────────────
 */

// Domain model for this file
class WeatherData {
    private String city;
    private double temperatureC;
    private String condition;
    private int    humidity;

    public WeatherData(String city, double temp, String condition, int humidity) {
        this.city = city; this.temperatureC = temp;
        this.condition = condition; this.humidity = humidity;
    }

    public String getCity()           { return city; }
    public double getTemperatureC()   { return temperatureC; }
    public String getCondition()      { return condition; }
    public int    getHumidity()       { return humidity; }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 2: @CrossOrigin — METHOD-LEVEL CORS
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                @CrossOrigin  EXPLAINED — METHOD LEVEL                        ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @CrossOrigin enables CORS for a specific endpoint or entire controller.
 * Spring automatically handles the OPTIONS preflight request for you.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔧 ALL @CrossOrigin ATTRIBUTES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   origins        → Allowed origins. Default: "*" (all). Be specific in production!
 *   originPatterns → Pattern-based matching (e.g., "https://*.myapp.com")
 *   methods        → Allowed HTTP methods. Default: GET, HEAD, POST
 *   allowedHeaders → Allowed request headers. Default: "*" (all)
 *   exposedHeaders → Response headers JS can access. Default: none
 *   allowCredentials → Allow cookies. Cannot be used with origins="*"
 *   maxAge         → Preflight cache duration in seconds. Default: 1800 (30 min)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 WHEN TO USE METHOD-LEVEL @CrossOrigin:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  You have a PUBLIC endpoint that anyone can access (e.g., a weather widget)
 *  •  You want different CORS policies for different endpoints
 *  •  Fine-grained control at the method level
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/v1/weather")
class WeatherPublicController {

    private static final Map<String, WeatherData> WEATHER_DB = Map.of(
            "London",    new WeatherData("London",    12.5, "Rainy",  80),
            "New York",  new WeatherData("New York",  22.0, "Sunny",  55),
            "Tokyo",     new WeatherData("Tokyo",     18.0, "Cloudy", 70),
            "Sydney",    new WeatherData("Sydney",    28.5, "Clear",  40)
    );

    /**
     * GET /api/v1/weather/{city}
     *
     * Public endpoint — accessible from ANY origin (e.g., a weather widget embedded
     * on any website).
     *
     * Response headers added by Spring:
     *   Access-Control-Allow-Origin: *
     */
    @GetMapping("/{city}")
    @CrossOrigin("*")   // ← Allow ALL origins — safe for PUBLIC, read-only endpoints
    public ResponseEntity<WeatherData> getWeather(@PathVariable String city) {
        System.out.println("📥 GET /api/v1/weather/" + city);
        WeatherData data = WEATHER_DB.get(city);
        return data != null ? ResponseEntity.ok(data) : ResponseEntity.notFound().build();
    }

    /**
     * GET /api/v1/weather
     *
     * Full @CrossOrigin with all attributes demonstrated.
     *
     * Only allow specific origins — best practice for production.
     */
    @GetMapping
    @CrossOrigin(
        origins         = {"https://myapp.com", "https://weather-widget.com"},  // ← Specific origins only
        methods         = {RequestMethod.GET, RequestMethod.HEAD},              // ← Only GET and HEAD
        allowedHeaders  = {"Content-Type", "Accept", "X-Request-ID"},          // ← Specific headers only
        exposedHeaders  = {"X-Total-Count", "X-Request-ID"},                   // ← Headers JS can read
        maxAge          = 3600                                                  // ← Cache preflight 1hr
    )
    public List<WeatherData> getAllWeather() {
        System.out.println("📥 GET /api/v1/weather");
        return List.copyOf(WEATHER_DB.values());
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 3: @CrossOrigin — CLASS-LEVEL CORS
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║              @CrossOrigin  EXPLAINED — CLASS LEVEL                           ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 CLASS-LEVEL @CrossOrigin:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * When @CrossOrigin is placed on the CLASS, it applies to ALL methods in the class.
 *
 * You can also COMBINE class-level and method-level @CrossOrigin.
 * The method-level settings OVERRIDE the class-level settings for that method.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 WHEN TO USE CLASS-LEVEL @CrossOrigin:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  ALL methods in a controller need the SAME CORS policy
 *  •  Reduces repetition (DRY principle)
 *  •  Specific controllers serve specific frontends (e.g., admin panel)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/v1/admin/weather")
@CrossOrigin(
    origins          = "https://admin.myapp.com",    // ← Only the admin frontend
    allowCredentials = "true",                        // ← Allow cookies (for admin sessions)
    maxAge           = 1800                           // ← 30 min preflight cache
)
class WeatherAdminController {

    /*
     * All methods in this controller inherit:
     *   Access-Control-Allow-Origin: https://admin.myapp.com
     *   Access-Control-Allow-Credentials: true
     *   Access-Control-Max-Age: 1800
     *
     * IMPORTANT: allowCredentials = "true" CANNOT be combined with origins = "*"
     * When credentials are allowed, the origin must be specific (not wildcard).
     */

    /**
     * GET /api/v1/admin/weather — inherits class-level CORS
     */
    @GetMapping
    public List<WeatherData> getAllWeatherAdmin() {
        System.out.println("📥 GET /api/v1/admin/weather (admin endpoint)");
        return List.of(
                new WeatherData("London",   12.5, "Rainy",  80),
                new WeatherData("New York", 22.0, "Sunny",  55)
        );
    }

    /**
     * POST /api/v1/admin/weather/refresh — inherits class CORS, adds method specifics.
     *
     * Method-level @CrossOrigin OVERRIDES / MERGES with class-level.
     * Here we allow an additional frontend origin for this specific endpoint.
     */
    @PostMapping("/refresh")
    @CrossOrigin(
        origins = {"https://admin.myapp.com", "https://monitoring.myapp.com"}  // ← Override: add more origins
    )
    public Map<String, Object> refreshWeatherCache() {
        System.out.println("📥 POST /api/v1/admin/weather/refresh");
        return Map.of("refreshed", true, "timestamp", java.time.LocalDateTime.now().toString());
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 4: WebMvcConfigurer — GLOBAL CORS CONFIGURATION (RECOMMENDED)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║           GLOBAL CORS via WebMvcConfigurer — THE PRODUCTION WAY              ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 WHY GLOBAL CORS IS RECOMMENDED:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  ONE place to manage all CORS rules — easier to maintain
 *  •  Consistent policy across all endpoints
 *  •  No risk of forgetting @CrossOrigin on a new controller
 *  •  Easier to change allowed origins when they change
 *  •  Can use different rules for different URL patterns (/api/v1/**, /public/**)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 PRIORITY ORDER (most specific wins):
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   Method-level @CrossOrigin    (most specific)
 *        ↓ overrides
 *   Class-level @CrossOrigin
 *        ↓ overrides
 *   Global WebMvcConfigurer CORS (least specific)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ SPRING SECURITY AND CORS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * When Spring Security is on the classpath, CORS must be configured in BOTH
 * Spring MVC AND Spring Security. The SecurityFilterChain must call:
 *
 *   http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
 *
 * Otherwise, the Spring Security filter blocks preflight OPTIONS requests
 * BEFORE they reach Spring MVC's CORS processing.
 *
 * Always configure CORS at the Spring Security level when security is active.
 *
 */
@Configuration
class GlobalCorsConfiguration implements WebMvcConfigurer {

    /**
     * Global CORS configuration using WebMvcConfigurer.
     *
     * This defines CORS rules for URL path patterns.
     * Multiple addMapping() calls allow different rules for different paths.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        // ─────────────────────────────────────────────────────────────────────
        // Rule 1: Public API — open to all origins, read-only
        // ─────────────────────────────────────────────────────────────────────
        registry.addMapping("/api/v1/public/**")   // ← Applies to all /public/** URLs
                .allowedOrigins("*")               // ← All origins (public data)
                .allowedMethods("GET", "HEAD")     // ← Read-only
                .maxAge(3600);                     // ← 1 hour preflight cache

        // ─────────────────────────────────────────────────────────────────────
        // Rule 2: Main API — specific origins only
        // ─────────────────────────────────────────────────────────────────────
        registry.addMapping("/api/v1/**")
                .allowedOriginPatterns(            // ← Pattern matching (Spring 5.3+)
                        "https://*.myapp.com",     //   any subdomain of myapp.com
                        "https://myapp.com",       //   root domain
                        "http://localhost:[*]"      //   any localhost port (dev only)
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders(                   // ← Allowed request headers
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "X-Request-ID",
                        "X-Idempotency-Key"
                )
                .exposedHeaders(                   // ← Response headers JS can read
                        "X-Request-ID",
                        "X-Total-Count",
                        "Location"
                )
                .allowCredentials(true)            // ← Allow cookies and auth headers
                .maxAge(1800);                     // ← 30 min preflight cache

        // ─────────────────────────────────────────────────────────────────────
        // Rule 3: Admin API — very restrictive, internal tools only
        // ─────────────────────────────────────────────────────────────────────
        registry.addMapping("/api/v1/admin/**")
                .allowedOrigins("https://admin.myapp.com")   // ← ONE specific origin
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(600);                                  // ← 10 min only
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 5: CORS COMPLETE PICTURE & DECISION GUIDE
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║             @CrossOrigin & CORS — THE COMPLETE PICTURE                       ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * CORS FLOW DIAGRAM:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   Browser (https://myapp.com:3000)
 *        │
 *        │ PREFLIGHT: OPTIONS /api/v1/orders/42
 *        │   Origin: https://myapp.com:3000
 *        │   Access-Control-Request-Method: DELETE
 *        │   Access-Control-Request-Headers: Authorization
 *        ▼
 *   Spring Boot (https://api.myapp.com:8080)
 *        │
 *        │ CorsFilter (added by Spring MVC CORS configuration)
 *        │   Checks: Is origin https://myapp.com:3000 allowed? YES
 *        │   Checks: Is DELETE method allowed? YES
 *        │   Checks: Is Authorization header allowed? YES
 *        ▼
 *   PREFLIGHT RESPONSE:
 *        Access-Control-Allow-Origin: https://myapp.com:3000
 *        Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE
 *        Access-Control-Allow-Headers: Authorization, Content-Type
 *        Access-Control-Max-Age: 1800
 *
 *   Browser (receives 200 on preflight → sends actual request)
 *        │
 *        │ ACTUAL: DELETE /api/v1/orders/42
 *        │   Origin: https://myapp.com:3000
 *        │   Authorization: Bearer eyJ...
 *        ▼
 *   Spring Boot: processes request, returns 204 No Content
 *        + Access-Control-Allow-Origin: https://myapp.com:3000
 *
 *   Browser: ✅ JavaScript can read the 204 response
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * DECISION GUIDE — Which CORS approach to use?
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  ┌──────────────────────────────────────────────────────────────────────────┐
 *  │ Is this a PRODUCTION application?                                        │
 *  │                                                                          │
 *  │  YES → Use Global WebMvcConfigurer (one place, consistent, maintainable) │
 *  │        Add Spring Security CORS if Security is on the classpath          │
 *  │                                                                          │
 *  │  NO (prototyping/dev) → @CrossOrigin("*") on individual methods is OK   │
 *  └──────────────────────────────────────────────────────────────────────────┘
 *
 *  ┌──────────────────────────────────────────────────────────────────────────┐
 *  │ Is this a PUBLIC API (weather widget, stock ticker, maps)?               │
 *  │                                                                          │
 *  │  YES → origins = "*" is acceptable (but NO credentials!)                │
 *  │  NO  → Always specify exact origins. Never use "*" with credentials.    │
 *  └──────────────────────────────────────────────────────────────────────────┘
 *
 *  ┌──────────────────────────────────────────────────────────────────────────┐
 *  │ Does your API send/receive cookies or Authorization headers?             │
 *  │                                                                          │
 *  │  YES → allowCredentials = true                                           │
 *  │        Cannot use origins = "*" — must specify exact origins            │
 *  │  NO  → allowCredentials = false (default)                               │
 *  └──────────────────────────────────────────────────────────────────────────┘
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 KEY TAKEAWAYS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. CORS is a BROWSER feature — it doesn't protect against non-browser API calls
 *  2. In production: use Global WebMvcConfigurer with explicit allowed origins
 *  3. Never use origins="*" with allowCredentials=true — it's a browser error
 *  4. Use allowedOriginPatterns for wildcard subdomain matching
 *  5. With Spring Security: configure CORS in SecurityFilterChain too
 *  6. Set maxAge to reduce preflight requests (improves performance)
 *  7. Only expose necessary headers via exposedHeaders
 *
 */
class Example05CrossOriginAnnotation {
    // Intentionally empty — documentation class
}

