package com.learning.springboot.chapter05;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║       HOW IT WORKS: SPRING SECURITY INTERNALS — COMPLETE DEEP DIVE                   ║
 * ║       DelegatingFilterProxy · FilterChainProxy · SecurityFilterChain                 ║
 * ║       Authentication Flow · Authorization Flow · AOP Method Security                 ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        HowItWorksExplained.java
 * Purpose:     Understand how EVERY piece of Spring Security works together internally.
 *              Trace the complete lifecycle of a secured HTTP request from the moment
 *              it arrives at the Servlet container to the moment a response is returned.
 * Difficulty:  ⭐⭐⭐⭐⭐ Advanced
 * Time:        45 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * CORRESPONDING OVERVIEW SECTION: Section 10 of Chapter05Overview.java
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║       HOW SPRING SECURITY WORKS INTERNALLY — THE COMPLETE GUIDE              ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
public class HowItWorksExplained {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 1: THE BIG PICTURE — Spring Security Filter Architecture        ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Spring Security is implemented as a chain of Servlet Filters.
     * Filters run BEFORE the DispatcherServlet (Spring MVC) — so security is enforced
     * before any controller code runs.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🏗️ THE ARCHITECTURE (layered):
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ┌──────────────────────────────────────────────────────────────────────────┐
     *  │                       Servlet Container (Tomcat)                          │
     *  │                                                                            │
     *  │   HTTP Request                                                             │
     *  │        ↓                                                                   │
     *  │   [DelegatingFilterProxy]  ← Single Servlet Filter registered with Tomcat │
     *  │        │                     Bridges Servlet world to Spring context       │
     *  │        ↓                                                                   │
     *  │   [FilterChainProxy]       ← Spring bean: manages all SecurityFilterChains│
     *  │        │                     Selects the right chain by securityMatcher    │
     *  │        ↓                                                                   │
     *  │   [SecurityFilterChain]    ← Your @Bean configuration                     │
     *  │        │                                                                   │
     *  │        ├─ [SecurityContextHolderFilter]   ← Load/store/clear context      │
     *  │        ├─ [HeaderWriterFilter]            ← Security headers              │
     *  │        ├─ [CsrfFilter]                    ← CSRF protection               │
     *  │        ├─ [LogoutFilter]                  ← Handle /logout                │
     *  │        ├─ [UsernamePasswordAuthFilter]    ← Form login                    │
     *  │        ├─ [BasicAuthenticationFilter]     ← HTTP Basic                   │
     *  │        ├─ [BearerTokenAuthFilter]         ← JWT/OAuth2                   │
     *  │        ├─ [AnonymousAuthenticationFilter] ← Sets anonymous user          │
     *  │        ├─ [ExceptionTranslationFilter]    ← 401/403 handling             │
     *  │        └─ [AuthorizationFilter]           ← URL authorization            │
     *  │                ↓                                                           │
     *  │   [DispatcherServlet]      ← Spring MVC                                   │
     *  │        ↓                                                                   │
     *  │   [Your @RestController]                                                   │
     *  │        ↓                                                                   │
     *  │   [Your @Service]          ← Spring AOP: @PreAuthorize checked here       │
     *  └──────────────────────────────────────────────────────────────────────────┘
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 2: DelegatingFilterProxy — THE BRIDGE                           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 WHAT IT IS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * DelegatingFilterProxy is a STANDARD SERVLET FILTER registered with Tomcat.
     * Its ONLY job is to bridge the Servlet world with the Spring ApplicationContext.
     *
     * WHY IS THIS NEEDED?
     *   The Servlet container initialises filters BEFORE the Spring ApplicationContext.
     *   So Tomcat cannot inject Spring beans into a Servlet Filter directly.
     *   DelegatingFilterProxy is a thin wrapper that:
     *     1. Gets registered with Tomcat as a Servlet Filter (no Spring beans yet)
     *     2. After the Spring context starts, it LOOKS UP the "springSecurityFilterChain"
     *        bean from the ApplicationContext
     *     3. Delegates ALL requests to that bean
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔄 HOW IT WORKS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   1. Tomcat starts → registers DelegatingFilterProxy (no Spring yet)
     *   2. Spring ApplicationContext starts → creates all beans including FilterChainProxy
     *   3. First request arrives → DelegatingFilterProxy looks up "springSecurityFilterChain"
     *   4. Delegates the request to FilterChainProxy (which IS "springSecurityFilterChain")
     *   5. All subsequent requests: lookup is cached, direct delegation
     *
     * In Spring Boot:
     *   DelegatingFilterProxy is automatically registered by SecurityFilterAutoConfiguration.
     *   You don't configure it manually — @EnableWebSecurity handles everything.
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 3: FilterChainProxy — THE DISPATCHER                            ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 WHAT IT IS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * FilterChainProxy holds ALL SecurityFilterChain beans in an ordered list.
     * For each incoming request, it:
     *   1. Iterates through SecurityFilterChain beans in @Order sequence
     *   2. Finds the FIRST chain whose securityMatcher matches the request URL
     *   3. Runs the request through that chain's filter list
     *   4. Only ONE chain processes each request (first match wins)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔄 CHAIN SELECTION LOGIC (our Chapter 5 example):
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   Request: GET /api/v1/secure/admin/dashboard
     *
     *   ✔ Chain @Order(100) — securityMatcher("/api/v1/secure/**")
     *     "/api/v1/secure/admin/dashboard" matches "/api/v1/secure/**"?
     *     YES → use secureApiFilterChain → STOP iterating
     *
     *   ─────────────────────────────────────────────────────────────────────────────
     *
     *   Request: GET /api/ch08/todos
     *
     *   ✘ Chain @Order(100) — securityMatcher("/api/v1/secure/**")
     *     "/api/ch08/todos" matches "/api/v1/secure/**"?
     *     NO → continue
     *
     *   ✔ Chain @Order(2147483642) — securityMatcher("/**")
     *     "/api/ch08/todos" matches "/**"?
     *     YES → use developmentPermissiveChain (permitAll) → STOP
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 💡 MULTIPLE FILTER CHAINS — REAL-WORLD USE CASE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @Bean @Order(1)
     *   SecurityFilterChain adminChain(HttpSecurity http) {
     *       http.securityMatcher("/admin/**")
     *           .authorizeHttpRequests(a -> a.anyRequest().hasRole("ADMIN"))
     *           .httpBasic(withDefaults());
     *       return http.build();
     *   }
     *
     *   @Bean @Order(2)
     *   SecurityFilterChain apiChain(HttpSecurity http) {
     *       http.securityMatcher("/api/**")
     *           .authorizeHttpRequests(a -> a.anyRequest().authenticated())
     *           .oauth2ResourceServer(o -> o.jwt(withDefaults()));
     *       return http.build();
     *   }
     *
     *   @Bean @Order(3)
     *   SecurityFilterChain publicChain(HttpSecurity http) {
     *       http.authorizeHttpRequests(a -> a.anyRequest().permitAll());
     *       return http.build();
     *   }
     *   // Admin URLs → chain 1 (strict), API URLs → chain 2 (JWT), others → chain 3 (open)
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 4: SECURITY FILTER CHAIN — ALL KEY FILTERS EXPLAINED           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * A SecurityFilterChain is a list of Servlet Filters executed IN ORDER.
     * Each filter has a specific responsibility:
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 1. SecurityContextHolderFilter
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PURPOSE:
     *    - LOADS the SecurityContext from a repository (session or request attribute)
     *    - Sets it in SecurityContextHolder for the current thread
     *    - CLEARS SecurityContextHolder AFTER the request completes
     *
     *  STATELESS APIs: context is empty at start; each request must authenticate fresh.
     *  SESSION-BASED: context is loaded from HttpSession (user stays logged in).
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 2. HeaderWriterFilter
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PURPOSE: Adds security headers to EVERY response:
     *    X-Content-Type-Options: nosniff          ← Prevents MIME sniffing
     *    X-Frame-Options: DENY                    ← Prevents clickjacking
     *    X-XSS-Protection: 0                      ← Modern browsers don't need this
     *    Strict-Transport-Security: max-age=...   ← HTTPS enforcement (HSTS)
     *    Cache-Control: no-cache, no-store, ...   ← Prevent caching of auth responses
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 3. CsrfFilter
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PURPOSE: Protects against Cross-Site Request Forgery (CSRF) attacks.
     *
     *  HOW CSRF ATTACKS WORK:
     *    An attacker tricks a logged-in user's browser into making a forged request.
     *    Because the browser sends cookies automatically, the server can't distinguish
     *    a legitimate request from a forged one based on cookies alone.
     *
     *  CSRF DEFENSE — SYNCHRONIZER TOKEN PATTERN:
     *    1. Server generates a unique random CSRF token per session
     *    2. Token is embedded in every HTML form (hidden input field)
     *    3. For state-changing requests (POST/PUT/DELETE), server validates the token
     *    4. Forged requests from attacker's site don't have the valid token → rejected
     *
     *  WHEN TO DISABLE CSRF:
     *    → STATELESS REST APIs (no sessions, no cookies → CSRF doesn't apply)
     *    → APIs used only by non-browser clients (mobile apps, other services)
     *    KEEP CSRF ENABLED for session-based browser applications!
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 4. LogoutFilter
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PURPOSE: Intercepts POST /logout (or configured logout URL).
     *    → Invalidates HttpSession
     *    → Clears SecurityContext
     *    → Deletes remember-me cookies
     *    → Redirects to logout success URL
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 5. UsernamePasswordAuthenticationFilter
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PURPOSE: Handles FORM LOGIN — POST /login with username and password.
     *
     *  FLOW:
     *    1. Intercepts POST to /login (default) or configured loginProcessingUrl
     *    2. Extracts username and password from request parameters
     *    3. Creates UsernamePasswordAuthenticationToken (unauthenticated)
     *    4. Passes to AuthenticationManager.authenticate()
     *    5. AuthenticationManager → DaoAuthenticationProvider
     *         → loadUserByUsername() → returns UserDetails
     *         → BCryptPasswordEncoder.matches() → verifies password
     *    6. If success: creates authenticated token, stores in SecurityContext
     *    7. If failure: throws AuthenticationException → redirect to /login?error
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 6. BasicAuthenticationFilter
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PURPOSE: Handles HTTP Basic Authentication.
     *
     *  FLOW:
     *    1. Checks for "Authorization: Basic <base64>" header
     *    2. Decodes base64 → extracts "username:password"
     *    3. Passes to AuthenticationManager (same DaoAuthenticationProvider path)
     *    4. If successful: populates SecurityContext for this request
     *    5. STATELESS by nature — no session created (perfect for REST APIs)
     *
     *  ENCODING (NOT ENCRYPTION!):
     *    "admin:admin123" → base64 → "YWRtaW46YWRtaW4xMjM="
     *    This is ENCODING, not encryption. HTTPS is REQUIRED to protect the credentials!
     *
     *  CURL EXAMPLE:
     *    curl -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" http://localhost:8080/api/...
     *    OR shorthand: curl -u admin:admin123 http://localhost:8080/api/...
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 7. BearerTokenAuthenticationFilter (OAuth2 / JWT)
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PURPOSE: Handles JWT Bearer token authentication for OAuth2 Resource Servers.
     *
     *  FLOW:
     *    1. Checks for "Authorization: Bearer <jwt_token>" header
     *    2. Validates the JWT (signature, expiry, issuer)
     *    3. Extracts claims (sub, roles, etc.)
     *    4. Creates JwtAuthenticationToken and populates SecurityContext
     *
     *  WHEN USED: .oauth2ResourceServer(o -> o.jwt(withDefaults())) in HttpSecurity
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 8. AnonymousAuthenticationFilter
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PURPOSE: Ensures SecurityContextHolder is NEVER empty.
     *
     *  FLOW:
     *    - Runs AFTER all authentication filters
     *    - If no Authentication has been set yet (no credentials provided):
     *      → Creates an AnonymousAuthenticationToken with:
     *            principal  = "anonymousUser"
     *            authority  = "ROLE_ANONYMOUS"
     *      → Sets it in SecurityContextHolder
     *
     *  WHY?
     *    Authorization filters can always assume getAuthentication() is non-null.
     *    isAnonymous() returns true if the auth is an AnonymousAuthenticationToken.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 9. ExceptionTranslationFilter — THE ERROR HANDLER
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PURPOSE: Translates Spring Security exceptions into HTTP responses.
     *
     *  INTERCEPTS TWO EXCEPTION TYPES:
     *
     *    AuthenticationException → 401 Unauthorized
     *      → User is NOT authenticated (no credentials / wrong credentials)
     *      → Calls AuthenticationEntryPoint.commence() to send the response
     *      → Default for HTTP Basic: sends "WWW-Authenticate: Basic" header
     *      → Default for form login: redirects to login page
     *
     *    AccessDeniedException → 403 Forbidden
     *      → User IS authenticated but lacks the required role/authority
     *      → For ANONYMOUS users: redirects to login (not 403)
     *      → For AUTHENTICATED users: calls AccessDeniedHandler → sends 403
     *
     *  CUSTOMISING ERROR RESPONSES:
     *    http.exceptionHandling(ex -> ex
     *        .authenticationEntryPoint((req, resp, e) -> {
     *            resp.setContentType("application/json");
     *            resp.setStatus(401);
     *            resp.getWriter().write("{\"error\":\"Authentication required\"}");
     *        })
     *        .accessDeniedHandler((req, resp, e) -> {
     *            resp.setContentType("application/json");
     *            resp.setStatus(403);
     *            resp.getWriter().write("{\"error\":\"Access denied\"}");
     *        })
     *    );
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 10. AuthorizationFilter — URL-LEVEL AUTHORIZATION (Last filter)
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  PURPOSE: Enforces URL-level authorization rules from authorizeHttpRequests().
     *
     *  FLOW:
     *    1. Gets the Authentication from SecurityContextHolder
     *    2. Gets the requested URL and HTTP method
     *    3. Iterates through requestMatchers rules IN ORDER (first match wins!)
     *    4. Evaluates the authorization expression for the matched rule
     *    5. Expression → true: allow → request continues to DispatcherServlet
     *    6. Expression → false: throw AccessDeniedException → 403 (or 401 if anonymous)
     *
     *  POSITION: LAST security filter. If it passes, the request goes to
     *  DispatcherServlet → Controller → @Service (where @PreAuthorize runs).
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 5: AUTHENTICATION FLOW — COMPLETE STEP-BY-STEP                 ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * HTTP Basic Authentication Flow (our Chapter 5 example):
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   CLIENT                        SPRING SECURITY                      YOUR CODE
     *   ──────                        ───────────────                      ─────────
     *
     *   GET /api/v1/secure/me
     *   Authorization: Basic YWRtaW46YWRtaW4xMjM=
     *        │
     *        ▼
     *   DelegatingFilterProxy                              (Servlet → Spring bridge)
     *        │
     *        ▼
     *   FilterChainProxy                                   (picks secureApiFilterChain)
     *        │
     *        ▼
     *   SecurityContextHolderFilter                        (creates empty context)
     *        │
     *        ▼
     *   HeaderWriterFilter                                 (adds security headers)
     *        │
     *        ▼
     *   CsrfFilter                                         (CSRF disabled → skip)
     *        │
     *        ▼
     *   BasicAuthenticationFilter
     *        │── decode base64 → "admin:admin123"
     *        │── create UsernamePasswordAuthenticationToken (unauthenticated)
     *        │── call AuthenticationManager.authenticate(token)
     *        │       │
     *        │       ▼
     *        │   DaoAuthenticationProvider
     *        │       │── loadUserByUsername("admin")  ←──── YOUR UserDetailsService
     *        │       │       returns UserDetails(admin, BCryptHash, [ADMIN, USER])
     *        │       │── BCryptPasswordEncoder.matches("admin123", "$2a$12$...")
     *        │       │       → TRUE ✅
     *        │       │── check isAccountNonLocked(), isEnabled() → all true
     *        │       └── return authenticated UsernamePasswordAuthenticationToken
     *        │
     *        │── store authentication in SecurityContextHolder
     *        │
     *        ▼
     *   AnonymousAuthenticationFilter                      (auth exists → skip)
     *        │
     *        ▼
     *   ExceptionTranslationFilter                         (wraps downstream in try/catch)
     *        │
     *        ▼
     *   AuthorizationFilter
     *        │── GET /api/v1/secure/me
     *        │── rule: GET /api/v1/secure/** → hasAnyRole(USER, ADMIN, MOD)
     *        │── admin has ROLE_ADMIN → ALLOW ✅
     *        │
     *        ▼
     *   DispatcherServlet → SecureUserController.getCurrentUser()  ←── YOUR CODE
     *        └── auth.getName() → "admin"
     *            auth.getAuthorities() → [ROLE_ADMIN, ROLE_USER, ...]
     *
     *   HTTP 200 OK
     *   { "username": "admin", "roles": [...], "authenticated": true }
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 6: AUTHORIZATION FAILURE FLOW — 401 and 403                    ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * CASE 1: No credentials provided → 401 Unauthorized
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   GET /api/v1/secure/me    (no Authorization header)
     *        │
     *        ▼
     *   BasicAuthenticationFilter  → no Authorization header → skip (auth not set)
     *        │
     *        ▼
     *   AnonymousAuthenticationFilter → sets AnonymousAuthenticationToken
     *        │                          (principal="anonymousUser", ROLE_ANONYMOUS)
     *        ▼
     *   AuthorizationFilter
     *        │── GET /api/v1/secure/** → hasAnyRole(USER, ADMIN, MOD)
     *        │── anonymous: ROLE_ANONYMOUS → does NOT have USER/ADMIN/MOD
     *        │── throws AccessDeniedException
     *        │
     *        ▼
     *   ExceptionTranslationFilter
     *        │── catches AccessDeniedException
     *        │── checks: is the user ANONYMOUS? → YES (AnonymousAuthenticationToken)
     *        │── → calls AuthenticationEntryPoint (not AccessDeniedHandler!)
     *        └── sends HTTP 401 Unauthorized + WWW-Authenticate: Basic realm="Realm"
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * CASE 2: Valid credentials, wrong role → 403 Forbidden
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   GET /api/v1/secure/admin/dashboard   (alice with ROLE_USER only)
     *   Authorization: Basic YWxpY2U6YWxpY2UxMjM=
     *        │
     *        ▼
     *   BasicAuthenticationFilter → authenticates alice → SecurityContext: alice (ROLE_USER)
     *        │
     *        ▼
     *   AuthorizationFilter
     *        │── /api/v1/secure/admin/** → hasRole("ADMIN")
     *        │── alice has ROLE_USER, NOT ROLE_ADMIN
     *        │── throws AccessDeniedException
     *        │
     *        ▼
     *   ExceptionTranslationFilter
     *        │── catches AccessDeniedException
     *        │── checks: is alice ANONYMOUS? → NO (she IS authenticated)
     *        │── → calls AccessDeniedHandler
     *        └── sends HTTP 403 Forbidden
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * CASE 3: Method-level denial → 403 from @PreAuthorize
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   GET /api/v1/secure/docs   (alice with ROLE_USER — passes URL check)
     *   Authorization: Basic YWxpY2U6YWxpY2UxMjM=
     *        │
     *        ▼
     *   (all filters pass — URL rule: GET /api/v1/secure/** → any authenticated user)
     *        │
     *        ▼
     *   DispatcherServlet → SecureDocumentController.getAll()
     *        │
     *        ▼
     *   docService.getAllDocuments()   ← PROXY intercepts (Spring AOP)
     *        │── @PreAuthorize("hasRole('ADMIN')")
     *        │── alice has ROLE_USER, NOT ROLE_ADMIN
     *        │── throws AccessDeniedException
     *        │
     *        ▼
     *   ExceptionTranslationFilter catches → sends HTTP 403 Forbidden
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 7: METHOD SECURITY — HOW @PreAuthorize WORKS WITH AOP          ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * @PreAuthorize is implemented using Spring AOP (Aspect-Oriented Programming).
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🏗️ HOW AOP WRAPS YOUR BEAN:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   1. Application starts, Spring detects @Service DocumentSecurityService
     *   2. Spring sees @PreAuthorize on its methods
     *   3. Spring creates a CGLIB PROXY that WRAPS DocumentSecurityService
     *   4. When you @Autowire DocumentSecurityService, you get the PROXY (not the real bean)
     *
     *   ┌──────────────────────────────────────────────────────────┐
     *   │                     CGLIB Proxy                          │
     *   │  ┌──────────────────────────────────────────────────┐   │
     *   │  │       Real DocumentSecurityService               │   │
     *   │  │  getAllDocuments() { ... }                       │   │
     *   │  └──────────────────────────────────────────────────┘   │
     *   │                                                          │
     *   │  Proxy.getAllDocuments() {                               │
     *   │    1. Get auth from SecurityContextHolder                │
     *   │    2. Evaluate @PreAuthorize("hasRole('ADMIN')")         │
     *   │    3. FALSE → throw AccessDeniedException                │
     *   │    4. TRUE  → call real.getAllDocuments()                │
     *   │  }                                                       │
     *   └──────────────────────────────────────────────────────────┘
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔄 @PreAuthorize STEP-BY-STEP EXECUTION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   Controller calls: docService.getAllDocuments()   [docService is the PROXY]
     *        │
     *        ▼
     *   CGLIB Proxy intercepts the call
     *        │
     *        ▼
     *   MethodSecurityInterceptor
     *        │── get SecurityContext from SecurityContextHolder
     *        │── get Authentication from SecurityContext
     *        │── get @PreAuthorize expression: "hasRole('ADMIN')"
     *        │── evaluate SpEL: new StandardEvaluationContext(authentication)
     *        │       hasRole('ADMIN')
     *        │       → checks auth.getAuthorities() for "ROLE_ADMIN"
     *        │
     *        ├── IF TRUE:  proceed to real getAllDocuments() → returns List<SecureDocument>
     *        │
     *        └── IF FALSE: throw AccessDeniedException
     *                         → ExceptionTranslationFilter → HTTP 403
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ⚠️ THE PROXY LIMITATION — WHY PRIVATE METHODS DON'T WORK:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   class MyService {
     *
     *       public void publicMethod() {
     *           privateMethod();   // ← DIRECT CALL (bypasses proxy!) → NOT secured!
     *       }
     *
     *       @PreAuthorize("hasRole('ADMIN')")
     *       private void privateMethod() { ... }  // ← NEVER intercepted!
     *   }
     *
     *   WHY?
     *   CGLIB creates a SUBCLASS of MyService. It can OVERRIDE public/protected methods,
     *   but NOT private methods. Calls from within the same class (this.method())
     *   go directly to the real object — the proxy is bypassed entirely.
     *
     *   ✅ SOLUTIONS:
     *   1. Make the method PUBLIC (most common)
     *   2. Move it to a SEPARATE BEAN (then called through proxy)
     *   3. Use self-injection: @Autowired MyService self; then call self.myMethod();
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 PROCESSING ORDER for all method security annotations:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @PreFilter  → filter INPUT collection
     *   @PreAuthorize → check BEFORE method runs
     *   [ method executes ]
     *   @PostAuthorize → check based on RETURN VALUE
     *   @PostFilter → filter RETURNED collection
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 8: SecurityContextHolder — ThreadLocal EXPLAINED               ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 WHAT IS ThreadLocal?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * ThreadLocal<T> provides thread-isolated storage.
     * Each THREAD has its OWN copy of the value — threads cannot see each other's values.
     *
     * Analogy: Each person (thread) has their own DESK DRAWER (ThreadLocal).
     * They can put things in and take things out, but no one else can see inside.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 WHY SecurityContextHolder USES ThreadLocal:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * A Spring Boot server handles MANY requests CONCURRENTLY.
     * Each request runs on its own THREAD (from the Tomcat thread pool).
     *
     * WITHOUT ThreadLocal: Storing authentication in a shared variable would mean
     * User A's authentication could overwrite User B's → SECURITY DISASTER.
     *
     * WITH ThreadLocal:
     *   Thread 1 (Alice's request)  → SecurityContextHolder stores ALICE's auth
     *   Thread 2 (Admin's request)  → SecurityContextHolder stores ADMIN's auth
     *   They NEVER interfere with each other ✅
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔄 LIFECYCLE PER REQUEST:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   1. Request arrives → thread assigned from Tomcat pool
     *   2. SecurityContextHolderFilter: loads/creates SecurityContext for this thread
     *   3. Authentication filter: populates the SecurityContext with Authentication
     *   4. All code on this thread can call SecurityContextHolder.getContext()
     *   5. Response sent → SecurityContextHolderFilter CLEARS the SecurityContext
     *   6. Thread returned to pool → NO leftover authentication data ✅
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 ACCESSING SecurityContextHolder:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   // In a controller or service (same thread as the request):
     *   Authentication auth = SecurityContextHolder.getContext().getAuthentication();
     *   String username  = auth.getName();
     *   boolean isAdmin  = auth.getAuthorities().stream()
     *                          .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
     *   Object principal = auth.getPrincipal();  // UserDetails if using UserDetailsService
     *
     *   // BETTER in controllers — inject Authentication directly:
     *   @GetMapping("/me")
     *   public String whoAmI(Authentication auth) {  // Spring injects from SecurityContext
     *       return auth.getName();
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ⚠️ ASYNC METHODS AND REACTIVE PROGRAMMING:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @Async methods run on a DIFFERENT THREAD!
     *   The SecurityContext is NOT automatically propagated to async threads.
     *
     *   Solution 1: Use DelegatingSecurityContextAsyncTaskExecutor:
     *     @Bean TaskExecutor secureTaskExecutor() {
     *         return new DelegatingSecurityContextAsyncTaskExecutor(
     *             new ThreadPoolTaskExecutor()
     *         );
     *     }
     *
     *   Solution 2: Change SecurityContextHolder strategy:
     *     SecurityContextHolder.setStrategyName(
     *         SecurityContextHolder.MODE_INHERITABLETHREADLOCAL  // Inherits to child threads
     *     );
     *
     *   Solution 3: Spring WebFlux (reactive) — use ReactiveSecurityContextHolder
     *     (stores SecurityContext in Reactor's context, not ThreadLocal)
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 9: AuthenticationManager AND AuthenticationProvider             ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * AuthenticationManager — THE COORDINATOR:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * AuthenticationManager has ONE method:
     *   Authentication authenticate(Authentication auth) throws AuthenticationException
     *
     * It DELEGATES to a list of AuthenticationProvider implementations.
     * The default implementation is ProviderManager.
     *
     *   ProviderManager iterates through its AuthenticationProvider list:
     *     → DaoAuthenticationProvider (username/password via UserDetailsService)
     *     → JwtAuthenticationProvider (JWT tokens)
     *     → LdapAuthenticationProvider (LDAP directory authentication)
     *     → Custom providers...
     *
     *   Each provider:
     *     1. Checks if it SUPPORTS the given Authentication token type
     *     2. If YES: attempts authentication
     *     3. If NO: skips to the next provider
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * DaoAuthenticationProvider — THE MOST COMMON PROVIDER:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Handles UsernamePasswordAuthenticationToken (form login + HTTP Basic).
     *
     * DaoAuthenticationProvider.authenticate() steps:
     *   1. userDetailsService.loadUserByUsername(username)
     *   2. passwordEncoder.matches(rawPassword, storedHash)
     *   3. check userDetails.isAccountNonLocked()
     *   4. check userDetails.isAccountNonExpired()
     *   5. check userDetails.isCredentialsNonExpired()
     *   6. check userDetails.isEnabled()
     *   7. All pass → return authenticated UsernamePasswordAuthenticationToken
     *      (username, null credentials [cleared for security], and authorities)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * BUILDING A CUSTOM AuthenticationProvider:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @Component
     *   class ApiKeyAuthenticationProvider implements AuthenticationProvider {
     *
     *       @Override
     *       public Authentication authenticate(Authentication auth) {
     *           String apiKey = auth.getCredentials().toString();
     *           if (isValidApiKey(apiKey)) {
     *               return new UsernamePasswordAuthenticationToken(
     *                   "apiClient", null,
     *                   List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
     *               );
     *           }
     *           throw new BadCredentialsException("Invalid API key");
     *       }
     *
     *       @Override
     *       public boolean supports(Class<?> authType) {
     *           return authType == ApiKeyAuthenticationToken.class;
     *       }
     *   }
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 10: COMPLETE REQUEST LIFECYCLE — FULL DIAGRAM                  ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     *  REQUEST: GET /api/v1/secure/admin/dashboard
     *  USER:    admin (ROLE_ADMIN) with HTTP Basic header
     *
     *  ┌─────────────────────────────────────────────────────────────────────────────┐
     *  │  1. NETWORK LAYER                                                            │
     *  │     HTTP request arrives at Tomcat on port 8080                              │
     *  │     Thread assigned from Tomcat thread pool                                  │
     *  ├─────────────────────────────────────────────────────────────────────────────┤
     *  │  2. DELEGATING FILTER PROXY                                                  │
     *  │     Bridges Servlet container to Spring's "springSecurityFilterChain" bean   │
     *  ├─────────────────────────────────────────────────────────────────────────────┤
     *  │  3. FILTER CHAIN PROXY — SecurityFilterChain selection                       │
     *  │     /api/v1/secure/admin/dashboard → matches secureApiFilterChain (@Order 100)│
     *  ├─────────────────────────────────────────────────────────────────────────────┤
     *  │  4. SECURITY FILTER CHAIN (secureApiFilterChain) — filters in order:        │
     *  │                                                                              │
     *  │  [a] SecurityContextHolderFilter                                             │
     *  │      → Create empty SecurityContext for this thread                          │
     *  │                                                                              │
     *  │  [b] HeaderWriterFilter                                                      │
     *  │      → Add X-Frame-Options, X-Content-Type-Options, etc.                    │
     *  │                                                                              │
     *  │  [c] CsrfFilter                                                              │
     *  │      → CSRF disabled (stateless API) → skip                                 │
     *  │                                                                              │
     *  │  [d] BasicAuthenticationFilter                                               │
     *  │      → Decode "Authorization: Basic YWRtaW46YWRtaW4xMjM="                  │
     *  │      → Call DaoAuthenticationProvider.authenticate()                        │
     *  │          → loadUserByUsername("admin") → UserDetails(admin)                 │
     *  │          → BCrypt.matches("admin123", hash) → true ✅                       │
     *  │      → Store authenticated token in SecurityContextHolder                   │
     *  │                                                                              │
     *  │  [e] AnonymousAuthenticationFilter                                           │
     *  │      → Auth already exists → skip                                           │
     *  │                                                                              │
     *  │  [f] ExceptionTranslationFilter                                              │
     *  │      → Wrap remaining filters in try/catch                                  │
     *  │                                                                              │
     *  │  [g] AuthorizationFilter                                                     │
     *  │      → Rule: /api/v1/secure/admin/** → hasRole("ADMIN")                     │
     *  │      → admin has ROLE_ADMIN → ALLOW ✅                                      │
     *  ├─────────────────────────────────────────────────────────────────────────────┤
     *  │  5. DISPATCHER SERVLET (Spring MVC)                                          │
     *  │     Routes to SecureAdminController.adminDashboard()                        │
     *  ├─────────────────────────────────────────────────────────────────────────────┤
     *  │  6. CONTROLLER METHOD — YOUR CODE                                            │
     *  │     SecureAdminController.adminDashboard(Authentication auth)               │
     *  │     auth.getName() → "admin"                                                │
     *  ├─────────────────────────────────────────────────────────────────────────────┤
     *  │  7. SERVICE CALL (if applicable)                                             │
     *  │     docService.getAllDocuments() ← CGLIB PROXY intercepts                   │
     *  │     @PreAuthorize("hasRole('ADMIN')") → ALLOW ✅                            │
     *  │     Real DocumentSecurityService.getAllDocuments() executes — YOUR CODE      │
     *  ├─────────────────────────────────────────────────────────────────────────────┤
     *  │  8. RESPONSE SENT                                                            │
     *  │     HTTP 200 OK { "panel": "Admin Dashboard", ... }                         │
     *  ├─────────────────────────────────────────────────────────────────────────────┤
     *  │  9. CLEANUP                                                                  │
     *  │     SecurityContextHolderFilter clears SecurityContext (ThreadLocal)         │
     *  │     Thread returned to Tomcat thread pool                                   │
     *  └─────────────────────────────────────────────────────────────────────────────┘
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 11: @WithMockUser — HOW IT WORKS IN TESTS                      ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * When you annotate a test with @WithMockUser, what actually happens?
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔄 STEP-BY-STEP:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   1. JUnit 5 starts the test method
     *   2. Spring Test detects @WithMockUser
     *   3. WithMockUserSecurityContextFactory.createSecurityContext() is called:
     *        a. Creates an empty SecurityContext
     *        b. Builds a UserDetails: username, password, roles/authorities
     *        c. Creates UsernamePasswordAuthenticationToken(userDetails, "password", authorities)
     *           with authenticated=true
     *        d. Sets the token in the SecurityContext
     *   4. SecurityContextHolder is populated BEFORE the test method body runs
     *   5. MockMvc performs the request → filter chain sees the pre-set authentication
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 KEY INSIGHT: @WithMockUser BYPASSES AUTHENTICATION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   The authentication filters (BasicAuthenticationFilter, etc.) run BUT they see
     *   that SecurityContextHolder ALREADY has an Authentication → they SKIP authentication.
     *
     *   The AuthorizationFilter STILL runs and checks URL rules.
     *   @PreAuthorize on service methods STILL runs.
     *
     *   @WithMockUser tests AUTHORIZATION logic, NOT AUTHENTICATION.
     *   To test authentication itself, use httpBasic() or formLogin() post-processors.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 COMPARISON: @WithMockUser vs @WithUserDetails vs httpBasic():
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @WithMockUser        → Synthetic user, bypasses auth, fast, most common
     *   @WithUserDetails     → Real UserDetailsService.loadUserByUsername() is called
     *   httpBasic("u","p")   → Full authentication flow (Basic filter authenticates)
     *   jwt()                → Populates SecurityContext with JwtAuthenticationToken
     *   user("u").roles("R") → Same as @WithMockUser but inline in a single test call
     *
     *  ⚠️  @WithMockUser does NOT test:
     *        • UsernamePasswordAuthenticationFilter
     *        • BasicAuthenticationFilter
     *        • DaoAuthenticationProvider
     *        • UserDetailsService.loadUserByUsername()
     *        • PasswordEncoder.matches()
     *      It skips all of authentication and goes straight to authorization.
     *
     *   Use httpBasic() or formLogin() when you need to test the FULL AUTH flow.
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 12: SPRING SECURITY INTERNALS SUMMARY TABLE                   ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     *  COMPONENT                   RESPONSIBILITY                           IN OUR SETUP
     *  ──────────────────────────  ───────────────────────────────────────  ──────────────────────────
     *  DelegatingFilterProxy       Bridge: Servlet container → Spring       Auto-configured by Boot
     *  FilterChainProxy            Select the right SecurityFilterChain      2 chains: secure + permissive
     *  SecurityContextHolderFilter Load/clear SecurityContext per thread    Runs for every request
     *  CsrfFilter                  Block forged state-changing requests      DISABLED (stateless REST)
     *  BasicAuthenticationFilter   Authenticate via HTTP Basic header        ENABLED (.httpBasic())
     *  AnonymousAuthenticationFilter  Ensure SecurityContext never null      Always runs
     *  ExceptionTranslationFilter  Turn 401/403 exceptions to responses     Always runs
     *  AuthorizationFilter         Enforce URL-level security rules         Enforces our requestMatchers
     *  DaoAuthenticationProvider   Verify username + password               Uses InMemoryUserDetailsManager
     *  InMemoryUserDetailsManager  Load users: admin, alice, moderator       Defined in Chapter05SecurityConfig
     *  BCryptPasswordEncoder       Hash and verify passwords                 strength=12
     *  SecurityContextHolder       Store current-thread's Authentication     ThreadLocal storage
     *  CGLIB Proxy                 Intercept @PreAuthorize on @Service       DocumentSecurityService, etc.
     *  MethodSecurityInterceptor   Evaluate SpEL in @PreAuthorize            Processes all method security
     *
     */
}

