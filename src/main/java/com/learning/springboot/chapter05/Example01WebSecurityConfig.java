package com.learning.springboot.chapter05;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.springframework.security.config.Customizer.withDefaults;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║            EXAMPLE 01: WEB SECURITY CONFIGURATION IN ACTION                          ║
 * ║            @EnableWebSecurity · SecurityFilterChain · HttpSecurity                   ║
 * ║            UserDetailsService · PasswordEncoder                                      ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example01WebSecurityConfig.java
 * Purpose:     Build a complete, production-pattern Spring Security configuration.
 *              Defines URL protection rules, user accounts, password hashing, and
 *              a permissive fallback so all other learning chapters keep working.
 * Difficulty:  ⭐⭐⭐ Intermediate
 * Time:        40 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * DEMO USERS (try with curl or Postman):
 *
 *   Username     Password     Roles
 *   ──────────   ──────────   ────────────────────────────────────────
 *   admin        admin123     ADMIN, USER
 *   alice        alice123     USER
 *   moderator    mod123       MODERATOR, USER
 *
 * PROTECTED ENDPOINT EXAMPLES:
 *   curl -u admin:admin123   http://localhost:8080/api/v1/secure/me
 *   curl -u alice:alice123   http://localhost:8080/api/v1/secure/users
 *   curl -u admin:admin123   http://localhost:8080/api/v1/secure/admin/dashboard
 *   curl -u alice:alice123   http://localhost:8080/api/v1/secure/admin/dashboard  → 403
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 1: @EnableWebSecurity — THE MASTER SWITCH
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                    @EnableWebSecurity  EXPLAINED                             ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @EnableWebSecurity activates Spring Security's web security support.
 * It enables the infrastructure that processes @Bean SecurityFilterChain definitions.
 *
 * In Spring Boot: @EnableWebSecurity is auto-applied when spring-boot-starter-security
 * is on the classpath. You add it explicitly when you want to CUSTOMISE security.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * @EnableMethodSecurity  —  ENABLES METHOD-LEVEL SECURITY
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @EnableMethodSecurity enables @PreAuthorize, @PostAuthorize (on by default),
 * and optionally @Secured and @RolesAllowed.
 *
 * Attributes:
 *   prePostEnabled   → enables @PreAuthorize + @PostAuthorize  (DEFAULT: true)
 *   securedEnabled   → enables @Secured                        (DEFAULT: false)
 *   jsr250Enabled    → enables @RolesAllowed, @PermitAll       (DEFAULT: false)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ REPLACEMENT FOR DEPRECATED @EnableGlobalMethodSecurity:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   OLD (deprecated in Spring Security 6):
 *     @EnableGlobalMethodSecurity(prePostEnabled=true, securedEnabled=true)
 *
 *   NEW (Spring Security 6+):
 *     @EnableMethodSecurity(securedEnabled=true, jsr250Enabled=true)
 *
 */
@Configuration
@EnableWebSecurity             // ← Enables and customises Spring Security web support
@EnableMethodSecurity(         // ← Enables method-level security for ALL beans
    prePostEnabled = true,     //   @PreAuthorize, @PostAuthorize ON (default)
    securedEnabled = true,     //   @Secured ON
    jsr250Enabled  = true      //   @RolesAllowed ON
)
class Chapter05SecurityConfig {

    // ══════════════════════════════════════════════════════════════════════════════
    //  SECTION 2: PasswordEncoder — ALWAYS DEFINE FIRST
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                       PasswordEncoder  EXPLAINED                             ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * PasswordEncoder is a Spring Security interface for HASHING and VERIFYING passwords.
     *
     * WHY HASH PASSWORDS?
     *   If your database is stolen, plain-text passwords expose your users immediately.
     *   With proper hashing, even the database owner cannot recover the original password.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 HOW BCrypt WORKS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   input:   "admin123"
     *   salt:    $2a$12$<22 random chars>     ← random per hash, stored in output
     *   output:  $2a$12$eJ...very_long_hash   ← 60 chars, includes salt + hash
     *
     *   Next login:
     *     1. Extract salt from stored hash
     *     2. Re-hash the input password with that salt
     *     3. Compare the two hashes
     *     (Never compare plain text!)
     *
     *   BCrypt strength (work factor):
     *     10 → ~100ms to hash (default)     ← comfortable for most apps
     *     12 → ~400ms to hash               ← more secure, use for sensitive data
     *     14 → ~1.6s to hash                ← very high security
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 PASSWORD ENCODING STRATEGIES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   BCryptPasswordEncoder       ← RECOMMENDED for most applications
     *   Argon2PasswordEncoder       ← Best (memory-hard), needs extra dependency
     *   Pbkdf2PasswordEncoder       ← FIPS 140-2 compliant environments
     *   SCryptPasswordEncoder       ← Memory-hard, alternative to Argon2
     *   NoOpPasswordEncoder         ← PLAIN TEXT ← NEVER use in production!
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        // BCryptPasswordEncoder(strength) — 12 rounds is a good production default
        // This bean is defined FIRST because UserDetailsService depends on it
        return new BCryptPasswordEncoder(12);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SECTION 3: UserDetailsService — HOW SPRING FINDS YOUR USERS
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                      UserDetailsService  EXPLAINED                           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * UserDetailsService is a Spring Security interface with ONE method:
     *     UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
     *
     * Spring Security calls this during AUTHENTICATION to load the user from your
     * data source (memory, database, LDAP, etc.).
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 IMPLEMENTATIONS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   InMemoryUserDetailsManager → stores users in memory (perfect for learning/dev)
     *   JdbcUserDetailsManager     → loads users from a JDBC database
     *   Custom implementation      → load from JPA entity, LDAP, Redis, API, etc.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Three demo users for our protected API endpoints.
     *
     * IMPORTANT: Password MUST be BCrypt-encoded when passed to Spring Security.
     * User.builder().password(encoder.encode("raw")) handles this correctly.
     */
    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder) {

        // ─────────────────────────────────────────────────────────────────────────
        // USER 1: admin — full access (ADMIN + USER roles + fine-grained authorities)
        // ─────────────────────────────────────────────────────────────────────────
        UserDetails admin = User.builder()
                .username("admin")
                .password(encoder.encode("admin123"))    // "admin123" hashed with BCrypt
                .roles("ADMIN", "USER")                  // → ROLE_ADMIN, ROLE_USER
                .authorities(                            // Fine-grained authorities
                        "ROLE_ADMIN", "ROLE_USER",
                        "READ_USERS", "WRITE_USERS", "DELETE_USERS",
                        "READ_ORDERS", "WRITE_ORDERS", "DELETE_ORDERS",
                        "VIEW_REPORTS", "MANAGE_SYSTEM"
                )
                .build();

        // ─────────────────────────────────────────────────────────────────────────
        // USER 2: alice — regular user (READ access only)
        // ─────────────────────────────────────────────────────────────────────────
        UserDetails alice = User.builder()
                .username("alice")
                .password(encoder.encode("alice123"))
                .roles("USER")                           // → ROLE_USER only
                .authorities("ROLE_USER", "READ_USERS", "READ_ORDERS")
                .build();

        // ─────────────────────────────────────────────────────────────────────────
        // USER 3: moderator — content manager (MODERATOR + USER roles)
        // ─────────────────────────────────────────────────────────────────────────
        UserDetails moderator = User.builder()
                .username("moderator")
                .password(encoder.encode("mod123"))
                .roles("MODERATOR", "USER")
                .authorities(
                        "ROLE_MODERATOR", "ROLE_USER",
                        "READ_USERS", "WRITE_USERS",
                        "READ_ORDERS", "MANAGE_CONTENT", "VIEW_REPORTS"
                )
                .build();

        System.out.println("✅ [Chapter05] InMemoryUserDetailsManager created with 3 demo users: " +
                "admin, alice, moderator");

        // InMemoryUserDetailsManager: lightweight, perfect for development and tests
        // In production: replace with JdbcUserDetailsManager or custom implementation
        return new InMemoryUserDetailsManager(admin, alice, moderator);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  SECTION 4: SecurityFilterChain — URL-LEVEL AUTHORIZATION
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║             SecurityFilterChain — PROTECTED API ROUTES                       ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 SecurityFilterChain is a @Bean that defines:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   1. WHICH URLs it applies to  → .securityMatcher("/api/v1/secure/**")
     *   2. WHO can access which URLs → .authorizeHttpRequests(auth -> ...)
     *   3. HOW users authenticate    → .httpBasic(), .formLogin(), .oauth2ResourceServer()
     *   4. SESSION management        → .sessionManagement(...)
     *   5. CSRF protection           → .csrf(...)
     *   6. CORS (when security active)→ .cors(...)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * @Order controls PRIORITY when multiple SecurityFilterChain beans exist:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @Order(1)    → checked FIRST (most specific, highest priority)
     *   @Order(100)  → checked second
     *   @Order(SecurityProperties.BASIC_AUTH_ORDER) → checked LAST (fallback)
     *
     *   For a given request, Spring iterates chains in order until one's
     *   securityMatcher matches. The FIRST match wins.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🏗️ THIS APPLICATION HAS TWO CHAINS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  Chain 1 (@Order 100) — Protects /api/v1/secure/**
     *    securityMatcher → /api/v1/secure/**
     *    Rules:
     *      /api/v1/secure/admin/** → ROLE_ADMIN only
     *      /api/v1/secure/reports/** → MODERATOR or ADMIN only
     *      GET /api/v1/secure/**  → any authenticated user
     *      POST/PUT /api/v1/secure/** → ADMIN or MODERATOR
     *      DELETE /api/v1/secure/** → ADMIN only
     *    Auth mechanism: HTTP Basic (for simplicity; use JWT in production)
     *    Session: STATELESS (no server-side session)
     *    CSRF: disabled (stateless REST API)
     *
     *  Chain 2 (@Order BASIC_AUTH_ORDER) — Catch-all for ALL other paths
     *    securityMatcher → /**
     *    Rules: permit everything (other learning chapters work without auth)
     *    This keeps all other chapter endpoints open for learning purposes.
     *
     */

    /**
     * PROTECTED SECURITY CHAIN — Only applies to /api/v1/secure/** paths.
     * Demonstrates all key HttpSecurity configuration options.
     */
    @Bean
    @Order(100)
    SecurityFilterChain secureApiFilterChain(HttpSecurity http) throws Exception {

        http
            // ─────────────────────────────────────────────────────────────────
            // STEP 1: securityMatcher — which URLs this chain handles
            // ─────────────────────────────────────────────────────────────────
            .securityMatcher("/api/v1/secure/**")    // ← ONLY protect these paths

            // ─────────────────────────────────────────────────────────────────
            // STEP 2: authorizeHttpRequests — URL-level authorization rules
            //
            // RULE ORDER MATTERS — more specific rules must come BEFORE less specific.
            // Spring Security checks rules top-to-bottom; FIRST match wins.
            // ─────────────────────────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // Admin panel — ADMIN role only
                .requestMatchers("/api/v1/secure/admin/**")
                    .hasRole("ADMIN")                     // hasRole("ADMIN") = hasAuthority("ROLE_ADMIN")

                // Reports — ADMIN or MODERATOR
                .requestMatchers("/api/v1/secure/reports/**")
                    .hasAnyRole("ADMIN", "MODERATOR")

                // Read operations — any authenticated user (USER, ADMIN, or MODERATOR)
                .requestMatchers(HttpMethod.GET, "/api/v1/secure/**")
                    .hasAnyRole("USER", "ADMIN", "MODERATOR")

                // Write operations — only ADMIN or MODERATOR can create/update
                .requestMatchers(HttpMethod.POST, "/api/v1/secure/**")
                    .hasAnyRole("ADMIN", "MODERATOR")
                .requestMatchers(HttpMethod.PUT, "/api/v1/secure/**")
                    .hasAnyRole("ADMIN", "MODERATOR")

                // Delete — only ADMIN
                .requestMatchers(HttpMethod.DELETE, "/api/v1/secure/**")
                    .hasRole("ADMIN")

                // Everything else under /api/v1/secure/** → must be authenticated
                .anyRequest().authenticated()
            )

            // ─────────────────────────────────────────────────────────────────
            // STEP 3: Authentication mechanism — HTTP Basic Auth
            //
            // HTTP Basic sends credentials as "Authorization: Basic <base64>"
            // e.g.: Authorization: Basic YWRtaW46YWRtaW4xMjM=
            //       (base64 of "admin:admin123")
            //
            // withDefaults() = shortcut for Customizer.withDefaults()
            // Production alternative: OAuth2 JWT Bearer tokens
            // ─────────────────────────────────────────────────────────────────
            .httpBasic(withDefaults())     // ← Enable HTTP Basic auth with defaults

            // ─────────────────────────────────────────────────────────────────
            // STEP 4: Session management — STATELESS for REST APIs
            //
            // STATELESS → Spring Security will NOT create or use HTTP sessions.
            // Each request MUST carry authentication credentials.
            // This is the correct setting for REST APIs consumed by non-browser clients.
            // ─────────────────────────────────────────────────────────────────
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ─────────────────────────────────────────────────────────────────
            // STEP 5: CSRF — disabled for stateless REST APIs
            //
            // CSRF attacks exploit browser session cookies. Since this API is
            // STATELESS (no session cookies), CSRF attacks are not applicable.
            // Always disable CSRF for stateless REST APIs!
            //
            // Keep CSRF ENABLED for traditional session-based web apps (form login).
            // ─────────────────────────────────────────────────────────────────
            .csrf(AbstractHttpConfigurer::disable);  // ← safe to disable for stateless REST

        return http.build();
    }

    /**
     * PERMISSIVE DEVELOPMENT CHAIN — Catch-all for ALL other paths (/** ).
     *
     * This ensures that all existing chapter endpoints (Chapters 1–4, 6–13)
     * remain accessible without authentication in this learning project.
     *
     * @Order(2147483642) = Integer.MAX_VALUE - 5 = very high number = LOWEST priority
     * (In older Spring Boot: SecurityProperties.BASIC_AUTH_ORDER, reorganised in Boot 4.x)
     * → Only matches if no higher-priority chain (like secureApiFilterChain) matched.
     *
     * ⚠️ IN PRODUCTION: Remove this bean or replace with a properly restricted chain.
     */
    @Bean
    @Order(2147483642)   // Integer.MAX_VALUE - 5 (= SecurityProperties.BASIC_AUTH_ORDER)
    SecurityFilterChain developmentPermissiveChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/**")   // ← Catch-all fallback (applies to all other paths)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()   // ← Open everything for learning purposes
            )
            .csrf(AbstractHttpConfigurer::disable)     // ← REST API, no browser forms
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        return http.build();
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 5: PROTECTED CONTROLLERS
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║             PROTECTED CONTROLLERS — ACCESSING SECURITY CONTEXT               ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * These controllers are under /api/v1/secure/** → protected by secureApiFilterChain.
 * Spring Security handles authentication BEFORE any controller method is invoked.
 */

/**
 * SecureUserController — accessible to authenticated users (USER/ADMIN/MODERATOR).
 *
 * Demonstrates how to access the current user's identity from a controller.
 */
@RestController
@RequestMapping("/api/v1/secure")
class SecureUserController {

    /**
     * GET /api/v1/secure/me
     *
     * Returns the currently authenticated user's information.
     * Spring Security injects the Authentication object from SecurityContextHolder.
     *
     * Authentication object contains:
     *   getName()          → the username
     *   getAuthorities()   → list of roles/permissions (GrantedAuthority)
     *   getPrincipal()     → the UserDetails object (if using UserDetailsService)
     *   isAuthenticated()  → true (always — unauthenticated requests never reach here)
     *
     * Try with:
     *   curl -u admin:admin123 http://localhost:8080/api/v1/secure/me
     *   curl -u alice:alice123 http://localhost:8080/api/v1/secure/me
     */
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(Authentication authentication) {
        // Authentication is auto-injected by Spring — no @Autowired needed
        System.out.println("📥 GET /api/v1/secure/me — user: " + authentication.getName());

        return Map.of(
                "username",    authentication.getName(),
                "roles",       authentication.getAuthorities().stream()
                                   .map(GrantedAuthority::getAuthority)
                                   .toList(),
                "authenticated", authentication.isAuthenticated()
        );
    }

    /**
     * GET /api/v1/secure/context-demo
     *
     * Demonstrates accessing security context statically via SecurityContextHolder.
     *
     * SecurityContextHolder:
     *   → Stores the SecurityContext for the CURRENT THREAD (ThreadLocal)
     *   → Can be accessed anywhere in the application (not just controllers)
     *   → Used inside services, repositories, etc.
     *
     * Note: Injecting Authentication as a method parameter (above) is cleaner.
     * Use SecurityContextHolder.getContext() inside non-controller code.
     */
    @GetMapping("/context-demo")
    public Map<String, Object> contextDemo() {
        // Access security context statically (works anywhere in the application)
        var ctx        = SecurityContextHolder.getContext();
        var auth       = ctx.getAuthentication();
        var principal  = auth.getPrincipal();

        System.out.println("🔐 SecurityContextHolder demo — thread: " + Thread.currentThread().getName());

        return Map.of(
                "username",      auth.getName(),
                "principalType", principal.getClass().getSimpleName(),
                "threadName",    Thread.currentThread().getName(),
                "authorities",   auth.getAuthorities().stream()
                                     .map(GrantedAuthority::getAuthority)
                                     .toList()
        );
    }

    /**
     * GET /api/v1/secure/users
     * Access: GET is allowed for any authenticated user
     * (from secureApiFilterChain rule: GET /api/v1/secure/** → hasAnyRole USER/ADMIN/MOD)
     *
     * Try: curl -u alice:alice123 http://localhost:8080/api/v1/secure/users → 200
     */
    @GetMapping("/users")
    public List<Map<String, String>> listUsers() {
        return List.of(
                Map.of("username", "admin",     "role", "ADMIN"),
                Map.of("username", "alice",     "role", "USER"),
                Map.of("username", "moderator", "role", "MODERATOR")
        );
    }

    /**
     * POST /api/v1/secure/users
     * Access: POST requires ADMIN or MODERATOR role
     * (from secureApiFilterChain rule: POST /api/v1/secure/** → hasAnyRole ADMIN/MOD)
     *
     * Try: curl -u alice:alice123 -X POST ... → 403 Forbidden (alice is just USER)
     *      curl -u admin:admin123 -X POST ... → 200 OK
     */
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, String> body) {
        System.out.println("📥 POST /api/v1/secure/users — creating user: " + body.get("username"));
        return ResponseEntity.status(201).body(Map.of(
                "message", "User created successfully",
                "username", body.getOrDefault("username", "unknown")
        ));
    }

    /**
     * DELETE /api/v1/secure/users/{id}
     * Access: DELETE requires ADMIN role only
     * (from secureApiFilterChain rule: DELETE /api/v1/secure/** → hasRole ADMIN)
     *
     * Try: curl -u moderator:mod123 -X DELETE ... → 403 Forbidden
     *      curl -u admin:admin123  -X DELETE ... → 200 OK
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id,
                                                           Authentication auth) {
        System.out.println("📥 DELETE /api/v1/secure/users/" + id + " by: " + auth.getName());
        return ResponseEntity.ok(Map.of(
                "message",  "User " + id + " deleted",
                "deletedBy", auth.getName()
        ));
    }
}

/**
 * SecureAdminController — accessible to ADMIN role ONLY.
 * Protected by URL rule: /api/v1/secure/admin/** → hasRole("ADMIN")
 *
 * Try: curl -u alice:alice123 http://localhost:8080/api/v1/secure/admin/dashboard → 403
 *      curl -u admin:admin123 http://localhost:8080/api/v1/secure/admin/dashboard → 200
 */
@RestController
@RequestMapping("/api/v1/secure/admin")
class SecureAdminController {

    /**
     * GET /api/v1/secure/admin/dashboard
     *
     * Admin dashboard endpoint. Requires ROLE_ADMIN.
     * URL-level security enforced by SecurityFilterChain (before controller runs).
     */
    @GetMapping("/dashboard")
    public Map<String, Object> adminDashboard(Authentication auth) {
        System.out.println("📥 GET /api/v1/secure/admin/dashboard — admin: " + auth.getName());
        return Map.of(
                "panel",       "Admin Dashboard",
                "loggedInAs",  auth.getName(),
                "permissions", auth.getAuthorities().stream()
                                   .map(GrantedAuthority::getAuthority)
                                   .toList(),
                "totalUsers",  3,
                "systemStatus", "Healthy"
        );
    }

    /**
     * GET /api/v1/secure/admin/system
     *
     * System info — ADMIN only. URL security enforced at filter chain level.
     * Also demonstrates @PreAuthorize at method level (double-layered security).
     */
    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")    // ← Method-level security (second layer)
    public Map<String, Object> systemInfo() {
        return Map.of(
                "javaVersion",    System.getProperty("java.version"),
                "availableProcs", Runtime.getRuntime().availableProcessors(),
                "freeMemoryMB",   Runtime.getRuntime().freeMemory() / 1024 / 1024,
                "osName",         System.getProperty("os.name")
        );
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 6: HttpSecurity COMPLETE REFERENCE (documented patterns)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║         HttpSecurity CONFIGURATION PATTERNS — COMPLETE REFERENCE             ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * Below are all the major HttpSecurity patterns with detailed explanations.
 * These are shown as documented examples (not active beans) to avoid conflicts.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * PATTERN 1: FORM LOGIN (Traditional web app)
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   http.formLogin(form -> form
 *       .loginPage("/login")                 ← Custom login page URL
 *       .loginProcessingUrl("/login")         ← POST URL that Spring processes
 *       .defaultSuccessUrl("/dashboard", true)  ← Redirect after success
 *       .failureUrl("/login?error=true")     ← Redirect on failure
 *       .usernameParameter("email")          ← HTML input field name for username
 *       .passwordParameter("password")       ← HTML input field name for password
 *       .permitAll()                         ← Allow everyone to access login page
 *   );
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * PATTERN 2: LOGOUT CONFIGURATION
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   http.logout(logout -> logout
 *       .logoutUrl("/logout")                 ← POST to this URL to logout
 *       .logoutSuccessUrl("/login?logout=true") ← Redirect after logout
 *       .invalidateHttpSession(true)          ← Invalidate session on logout
 *       .deleteCookies("JSESSIONID", "remember-me") ← Delete these cookies
 *       .clearAuthentication(true)            ← Clear SecurityContext
 *       .permitAll()
 *   );
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * PATTERN 3: JWT / OAUTH2 RESOURCE SERVER
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   http
 *       .oauth2ResourceServer(oauth2 -> oauth2
 *           .jwt(jwt -> jwt
 *               .jwtAuthenticationConverter(myJwtConverter())  // ← Custom claims mapping
 *           )
 *       )
 *       .sessionManagement(s -> s.sessionCreationPolicy(STATELESS));
 *   // Requires: spring-boot-starter-oauth2-resource-server
 *   // application.yml: spring.security.oauth2.resourceserver.jwt.issuer-uri=...
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * PATTERN 4: REMEMBER-ME (Stay logged in)
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   http.rememberMe(remember -> remember
 *       .tokenValiditySeconds(86400 * 30) ← Token valid for 30 days
 *       .key("unique-secret-key")         ← Key for signing remember-me cookie
 *       .userDetailsService(userDetailsService) ← Needed for re-authentication
 *   );
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * PATTERN 5: SESSION MANAGEMENT
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   http.sessionManagement(session -> session
 *       .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Default
 *       .maximumSessions(1)            ← Allow only 1 concurrent session per user
 *       .maxSessionsPreventsLogin(true)  ← Block new login if max sessions reached
 *                                        // false = kick out existing sessions
 *   );
 *
 *   SessionCreationPolicy values:
 *     ALWAYS       → Always create a session
 *     IF_REQUIRED  → Create session if needed (default)
 *     NEVER        → Never create, but use if exists
 *     STATELESS    → Never create or use sessions (for REST APIs)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * PATTERN 6: EXCEPTION HANDLING
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   http.exceptionHandling(ex -> ex
 *       .authenticationEntryPoint((request, response, authException) -> {
 *           // Return 401 JSON instead of default HTML error page
 *           response.setContentType("application/json");
 *           response.setStatus(401);
 *           response.getWriter().write("{\"error\":\"Unauthorized\"}");
 *       })
 *       .accessDeniedHandler((request, response, accessDeniedException) -> {
 *           // Return 403 JSON instead of default HTML error page
 *           response.setContentType("application/json");
 *           response.setStatus(403);
 *           response.getWriter().write("{\"error\":\"Forbidden\"}");
 *       })
 *   );
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * PATTERN 7: CORS CONFIGURATION WITH SPRING SECURITY
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   // IMPORTANT: When Spring Security is active, configure CORS in BOTH
 *   // WebMvcConfigurer AND Spring Security. Security processes requests
 *   // BEFORE Spring MVC, so browser preflight OPTIONS requests would be
 *   // blocked without security-level CORS config.
 *
 *   http.cors(cors -> cors
 *       .configurationSource(request -> {
 *           CorsConfiguration config = new CorsConfiguration();
 *           config.setAllowedOrigins(List.of("https://myapp.com"));
 *           config.setAllowedMethods(List.of("GET","POST","PUT","DELETE"));
 *           config.setAllowedHeaders(List.of("Authorization","Content-Type"));
 *           config.setAllowCredentials(true);
 *           return config;
 *       })
 *   );
 *
 */
class Example01WebSecurityConfig {
    /*
     * 🔑 COMPLETE PICTURE — HOW @EnableWebSecurity WORKS:
     *
     * @EnableWebSecurity
     *   → Registers DelegatingFilterProxy in the Servlet container
     *   → Creates WebSecurity and registers SecurityFilterChain beans
     *   → Enables the Spring Security filter chain infrastructure
     *
     * SecurityFilterChain beans
     *   → Each is a list of Servlet Filters (BasicAuthenticationFilter,
     *     UsernamePasswordAuthenticationFilter, ExceptionTranslationFilter, etc.)
     *   → securityMatcher determines which requests enter this chain
     *   → @Order determines priority when multiple chains exist
     *
     * UserDetailsService
     *   → Spring calls loadUserByUsername() during EVERY authentication attempt
     *   → Returns UserDetails (username, hashed password, authorities, account status)
     *   → One UserDetailsService bean → used by all AuthenticationProvider beans
     *
     * PasswordEncoder
     *   → Used by DaoAuthenticationProvider to verify passwords
     *   → REQUIRED when you define a UserDetailsService
     *   → BCryptPasswordEncoder is the recommended implementation
     *
     * DEMO ENDPOINT SUMMARY:
     *   GET  /api/v1/secure/me                → any authenticated user  → returns user info
     *   GET  /api/v1/secure/users             → any authenticated user  → returns user list
     *   POST /api/v1/secure/users             → ADMIN or MODERATOR      → creates user
     *   DELETE /api/v1/secure/users/{id}      → ADMIN only              → deletes user
     *   GET  /api/v1/secure/admin/dashboard   → ADMIN only              → admin panel
     *   GET  /api/v1/secure/admin/system      → ADMIN only              → system info
     */
}

