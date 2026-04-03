package com.learning.springboot.chapter05;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║                    SPRING BOOT ANNOTATIONS — COMPREHENSIVE GUIDE                     ║
 * ║                         Chapter 5: Spring Security Annotations                       ║
 * ║                                                                                       ║
 * ║                           📚 From Zero to Expert 📚                                  ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Chapter:      5
 * Title:        Spring Security Annotations
 * Difficulty:   ⭐⭐⭐⭐ Intermediate–Advanced
 * Estimated:    6–10 hours
 * Prerequisites: Chapters 1–3 (Core, Framework, MVC), Basic HTTP/REST, OAuth2 concepts
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │                                                                                       │
 * │                    CHAPTER 5: OVERVIEW & LEARNING GOALS                              │
 * │                                                                                       │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📖 TABLE OF CONTENTS 📖
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Section  1 :  Chapter Introduction & Overview
 * Section  2 :  The BIG IDEA — Authentication vs Authorization
 * Section  3 :  Spring Security Architecture
 * Section  4 :  Security Annotations Quick Reference
 * Section  5 :  Web Security Configuration
 *                   → @EnableWebSecurity      (enable + configure web security)
 *                   → SecurityFilterChain     (define security rules as beans)
 *                   → HttpSecurity            (fluent URL security DSL)
 * Section  6 :  Method-Level Security
 *                   → @EnableMethodSecurity   (activate method security)
 *                   → @PreAuthorize           (check BEFORE method runs)
 *                   → @PostAuthorize          (check AFTER method returns)
 *                   → @PreFilter              (filter input collections)
 *                   → @PostFilter             (filter returned collections)
 * Section  7 :  Role/Authority Annotations
 *                   → @Secured                (role-based, Spring native)
 *                   → @RolesAllowed           (JSR-250, Jakarta EE standard)
 * Section  8 :  User Identity Beans
 *                   → UserDetails             (user information contract)
 *                   → UserDetailsService      (load user by username)
 *                   → PasswordEncoder         (hash/verify passwords)
 * Section  9 :  Security Testing Annotations
 *                   → @WithMockUser           (mock authenticated user in tests)
 *                   → @WithUserDetails        (use real UserDetailsService in tests)
 *                   → @WithSecurityContext    (custom security context in tests)
 * Section 10 :  How Everything Works Together — Security Filter Chain internals
 * Section 11 :  Best Practices & Common Pitfalls
 * Section 12 :  Interview Questions & Answers
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * 📂 FILES IN THIS CHAPTER:
 *
 *  src/main/java/.../chapter05/
 *   • Chapter05Overview.java              ← YOU ARE HERE
 *   • Example01WebSecurityConfig.java     (@EnableWebSecurity, SecurityFilterChain,
 *                                          HttpSecurity, UserDetailsService, PasswordEncoder)
 *   • Example02MethodSecurity.java        (@EnableMethodSecurity, @PreAuthorize, @PostAuthorize,
 *                                          @PreFilter, @PostFilter)
 *   • Example03SecuredAndRolesAllowed.java (@Secured, @RolesAllowed, comparison)
 *   • Example04UserDetailsService.java    (UserDetails, custom UserDetailsService, passwords)
 *   • Example05WithMockUser.java          (@WithMockUser, @WithUserDetails, test patterns)
 *   • HowItWorksExplained.java            (Security filter chain, AOP, authentication flow)
 *
 *  src/test/java/.../chapter05/
 *   • Chapter05SecurityTest.java          (Live @WithMockUser / @WithUserDetails tests)
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
public class Chapter05Overview {

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
     *  ✓  Understand Authentication (who are you?) vs Authorization (what can you do?)
     *  ✓  Configure Spring Security with @EnableWebSecurity and SecurityFilterChain
     *  ✓  Protect URLs with granular access rules using HttpSecurity
     *  ✓  Secure individual methods with @PreAuthorize and @PostAuthorize
     *  ✓  Use @Secured and @RolesAllowed as simpler alternatives
     *  ✓  Implement UserDetailsService for custom user loading
     *  ✓  Hash passwords securely with PasswordEncoder (BCrypt)
     *  ✓  Write security-aware tests using @WithMockUser
     *  ✓  Understand the Spring Security filter chain internally
     *  ✓  Answer Spring Security interview questions with confidence
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        SECTION 2: AUTHENTICATION vs AUTHORIZATION — THE BIG IDEA            ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 AUTHENTICATION — "Who are you?"
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Authentication is the process of VERIFYING the identity of a user/system.
     *
     * Real-world analogy: Showing your ID at the entrance of a building.
     *
     * Methods:
     *   • Username + Password (most common)
     *   • JWT (JSON Web Token) — stateless token
     *   • OAuth2 / OpenID Connect — delegate to Google, GitHub, etc.
     *   • API Keys — long-lived tokens for services
     *   • mTLS — mutual TLS certificate authentication
     *
     * Spring Security result: an Authentication object in SecurityContextHolder
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔒 AUTHORIZATION — "What can you do?"
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Authorization is the process of determining what an AUTHENTICATED user is ALLOWED
     * to do. It happens AFTER authentication.
     *
     * Real-world analogy: Your badge says "Level 3 Clearance" — you can enter rooms
     * 1–3 but NOT room 4.
     *
     * Two common models:
     *   1. ROLE-BASED  (RBAC)   → ROLE_ADMIN, ROLE_USER, ROLE_MODERATOR
     *   2. PERMISSION-BASED     → READ_USERS, WRITE_ORDERS, DELETE_POSTS
     *
     * Spring Security approach:
     *   Roles    → use hasRole('ADMIN')       (Spring prepends "ROLE_" automatically)
     *   Authorities → use hasAuthority('READ_USERS') (exact match, no prefix added)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 SPRING SECURITY TIMELINE FOR A REQUEST:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   HTTP Request
     *        ↓
     *   [1] Spring Security Filter Chain runs
     *        ↓
     *   [2] AUTHENTICATION filters: "Is the user authenticated?"
     *        → UsernamePasswordAuthenticationFilter (form login)
     *        → BasicAuthenticationFilter (HTTP Basic)
     *        → BearerTokenAuthenticationFilter (JWT)
     *        ↓
     *   [3] SecurityContextHolder populated with Authentication object
     *        ↓
     *   [4] AUTHORIZATION (URL level): "Is authenticated user allowed here?"
     *        → FilterSecurityInterceptor / AuthorizationFilter
     *        → Checks requestMatchers rules from HttpSecurity
     *        ↓
     *   [5] Controller method is invoked
     *        ↓
     *   [6] AUTHORIZATION (Method level): "@PreAuthorize check"
     *        → Spring AOP intercepts the method call
     *        → Evaluates SpEL expression from @PreAuthorize
     *        ↓
     *   [7] Method executes
     *        ↓
     *   [8] AUTHORIZATION (Return level): "@PostAuthorize check"
     *        → Spring AOP checks return value
     *        ↓
     *   [9] Response returned
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        SECTION 3: SPRING SECURITY ARCHITECTURE                              ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🏗️ KEY COMPONENTS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ┌─────────────────────────────────────────────────────────────────────────────┐
     *  │                      SPRING SECURITY ARCHITECTURE                           │
     *  │                                                                             │
     *  │  SecurityFilterChain                                                        │
     *  │  (defines WHAT is protected and HOW)                                        │
     *  │    │                                                                        │
     *  │    ├── AuthenticationManager          ("Verify who you are")               │
     *  │    │       └── AuthenticationProvider (pluggable verification strategy)    │
     *  │    │               └── UserDetailsService (load user from DB/memory/LDAP)  │
     *  │    │               └── PasswordEncoder  (verify hashed password)           │
     *  │    │                                                                        │
     *  │    ├── SecurityContextHolder          ("Remember who you are")             │
     *  │    │       └── SecurityContext                                              │
     *  │    │               └── Authentication (principal + credentials + roles)    │
     *  │    │                                                                        │
     *  │    └── AccessDecisionManager          ("Check what you can do")            │
     *  │            └── SecurityExpressionHandler  (evaluates SpEL in @PreAuthorize)│
     *  └─────────────────────────────────────────────────────────────────────────────┘
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 MODERN SPRING SECURITY (6+/7+) vs OLD APPROACH:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  OLD (Spring Security 5.x):
     *     @Configuration
     *     class SecurityConfig extends WebSecurityConfigurerAdapter {  ← REMOVED in 6+!
     *         @Override
     *         protected void configure(HttpSecurity http) { ... }
     *     }
     *
     *  NEW (Spring Security 6+/7+) — Component-based, no inheritance:
     *     @Configuration
     *     @EnableWebSecurity
     *     class SecurityConfig {
     *         @Bean
     *         SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
     *             http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
     *             return http.build();
     *         }
     *     }
     *
     *  Key changes in Spring Security 6+/7+:
     *    ✓ WebSecurityConfigurerAdapter REMOVED (use @Bean SecurityFilterChain)
     *    ✓ @EnableGlobalMethodSecurity DEPRECATED → use @EnableMethodSecurity
     *    ✓ Lambda DSL for HttpSecurity configuration
     *    ✓ authorizeHttpRequests() replaces authorizeRequests()
     *    ✓ requestMatchers() replaces antMatchers()
     *    ✓ Default to DENY for unmatched requests
     *    ✓ SecurityFilterChain beans replace overriding configure()
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        SECTION 4: SECURITY ANNOTATIONS QUICK REFERENCE                     ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     *  ┌──────────────────────────────────┬──────────────────────────────────────────┐
     *  │ ANNOTATION                        │ ONE-LINE DESCRIPTION                     │
     *  ├──────────────────────────────────┼──────────────────────────────────────────┤
     *  │ @EnableWebSecurity               │ Enable + customize Spring Security        │
     *  │ @EnableMethodSecurity            │ Enable method-level security annotations  │
     *  ├──────────────────────────────────┼──────────────────────────────────────────┤
     *  │ @PreAuthorize("hasRole('ADMIN')")│ Authorize BEFORE the method runs          │
     *  │ @PostAuthorize("...")            │ Authorize based on RETURN VALUE            │
     *  │ @PreFilter("filterObject.active")│ Filter INPUT collection before method     │
     *  │ @PostFilter("...")               │ Filter RETURNED collection after method   │
     *  ├──────────────────────────────────┼──────────────────────────────────────────┤
     *  │ @Secured("ROLE_ADMIN")           │ Role-based, Spring native (simpler)       │
     *  │ @RolesAllowed("ADMIN")           │ Role-based, JSR-250 Jakarta standard      │
     *  ├──────────────────────────────────┼──────────────────────────────────────────┤
     *  │ @WithMockUser                    │ Mock an authenticated user in tests       │
     *  │ @WithUserDetails("alice")        │ Load real user from UserDetailsService    │
     *  │ @WithSecurityContext            │ Fully custom security context in tests    │
     *  └──────────────────────────────────┴──────────────────────────────────────────┘
     *
     * AUTHORIZATION EXPRESSION REFERENCE:
     *
     *  Expression                                    Meaning
     *  ────────────────────────────────────────────  ──────────────────────────────────
     *  hasRole('ADMIN')                              User has ROLE_ADMIN authority
     *  hasAuthority('MANAGE_USERS')                  User has exact 'MANAGE_USERS' authority
     *  hasAnyRole('ADMIN','MODERATOR')               User has any of the listed roles
     *  hasAnyAuthority('A','B')                      User has any of the listed authorities
     *  isAuthenticated()                             User is logged in (not anonymous)
     *  isAnonymous()                                 User is NOT logged in
     *  isFullyAuthenticated()                        Logged in without "remember-me"
     *  #param == authentication.name                 Method param matches logged-in username
     *  returnObject.owner == authentication.name     Return value's field matches username
     *  @permissionChecker.check(#id,'READ')          Custom bean method expression
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        SECTION 11: BEST PRACTICES & COMMON PITFALLS                        ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * DO's ✅
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ✅  Always use BCryptPasswordEncoder (or Argon2) — NEVER store plain text passwords
     *  ✅  Use @EnableMethodSecurity instead of deprecated @EnableGlobalMethodSecurity
     *  ✅  Prefer @PreAuthorize over @Secured — it supports SpEL expressions
     *  ✅  Use stateless session (STATELESS) for REST APIs
     *  ✅  Always define a PasswordEncoder bean — Spring Boot requires it
     *  ✅  Configure CORS in Spring Security (not just in WebMvcConfigurer) when Security active
     *  ✅  Test security with @WithMockUser and @WithUserDetails
     *  ✅  Return 403 (Forbidden) for authorization failures, 401 for authentication failures
     *  ✅  Use authorities ("MANAGE_USERS") over roles ("ROLE_ADMIN") for fine-grained control
     *  ✅  Disable CSRF for stateless REST APIs (CSRF is for session-based browser apps)
     *
     * DON'Ts ❌
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ❌  Don't extend WebSecurityConfigurerAdapter (removed in Spring Security 6)
     *  ❌  Don't use @EnableGlobalMethodSecurity (deprecated — use @EnableMethodSecurity)
     *  ❌  Don't use antMatchers() (replaced by requestMatchers() in Spring Security 6)
     *  ❌  Don't store passwords in plain text or MD5/SHA-1
     *  ❌  Don't call authenticated() without defining a UserDetailsService
     *  ❌  Don't skip CSRF protection in browser-facing form applications
     *  ❌  Don't expose stack traces in 401/403 error responses
     *  ❌  Don't put @PreAuthorize on private methods (Spring AOP can't intercept them)
     *  ❌  Don't use @Secured for complex expressions — use @PreAuthorize instead
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        SECTION 12: TOP INTERVIEW QUESTIONS — CHAPTER 5                     ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Q1:  What is the difference between Authentication and Authorization?
     * A1:  Authentication verifies WHO you are (username/password, token, certificate).
     *      Authorization determines WHAT you can do (roles, permissions).
     *      Authentication always happens first; Authorization happens after.
     *
     * Q2:  What is SecurityFilterChain and why does Spring Security 6+ use it?
     * A2:  SecurityFilterChain is a Bean that configures which URLs are protected and
     *      how. Spring Security 6+ removed WebSecurityConfigurerAdapter (inheritance-based)
     *      in favour of @Bean SecurityFilterChain (composition-based). Multiple
     *      SecurityFilterChain beans can coexist, each with a different securityMatcher.
     *
     * Q3:  What is the difference between hasRole() and hasAuthority()?
     * A3:  hasRole('ADMIN') automatically prepends "ROLE_" → matches "ROLE_ADMIN".
     *      hasAuthority('ROLE_ADMIN') is an exact match — no prefix is added.
     *      Use hasAuthority() when working with fine-grained permissions (not just roles).
     *
     * Q4:  What is @EnableMethodSecurity and what does it replace?
     * A4:  @EnableMethodSecurity (Spring Security 6+) replaces @EnableGlobalMethodSecurity.
     *      It enables @PreAuthorize, @PostAuthorize (prePostEnabled=true by default),
     *      @Secured (securedEnabled=true), and @RolesAllowed (jsr250Enabled=true).
     *
     * Q5:  What is the difference between @PreAuthorize, @Secured, and @RolesAllowed?
     * A5:  @PreAuthorize → most powerful, supports full SpEL expressions, recommended
     *      @Secured       → simpler, only roles, Spring-specific, no SpEL
     *      @RolesAllowed  → same as @Secured but JSR-250 (Jakarta standard, portable)
     *      Choose @PreAuthorize for complex rules; @RolesAllowed for portability.
     *
     * Q6:  What is SecurityContextHolder and why is it ThreadLocal?
     * A6:  SecurityContextHolder stores the SecurityContext (which contains the
     *      Authentication object) for the current thread. It uses ThreadLocal by
     *      default so that each request thread has its own isolated security context
     *      — preventing one user's credentials from leaking to another user's request.
     *
     * Q7:  How do you handle CSRF in a REST API?
     * A7:  CSRF (Cross-Site Request Forgery) is a browser attack. REST APIs consumed
     *      by non-browser clients (mobile apps, other services) don't need CSRF
     *      protection. For stateless REST APIs, disable CSRF:
     *          http.csrf(AbstractHttpConfigurer::disable)
     *      For browser-facing form apps, KEEP CSRF enabled (it's the default).
     *
     * Q8:  What is the purpose of UserDetailsService?
     * A8:  UserDetailsService is a contract Spring Security uses to load user information
     *      during authentication. Its single method loadUserByUsername(username) must
     *      return a UserDetails object (with username, hashed password, granted authorities).
     *      Spring Security calls this during form login, HTTP Basic, etc.
     *
     * Q9:  Why should you use BCryptPasswordEncoder?
     * A9:  BCrypt uses adaptive hashing (slow by design) and includes a random salt.
     *      This makes brute-force and rainbow table attacks impractical. The work factor
     *      (strength) can be tuned over time as hardware improves. MD5/SHA-1 are NOT
     *      suitable for passwords — they are too fast and have known vulnerabilities.
     *
     * Q10: How does @PreAuthorize work on a private method?
     * A10: It DOESN'T. @PreAuthorize relies on Spring AOP, which creates a CGLIB
     *      proxy around the bean. Proxies can only intercept PUBLIC methods called
     *      from OUTSIDE the bean. Private/package-private methods are called directly
     *      on the real object, bypassing the proxy. Always annotate PUBLIC methods.
     *
     */
}

