package com.learning.springboot.chapter05;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║          EXAMPLE 05: SECURITY TESTING ANNOTATIONS — COMPLETE GUIDE                   ║
 * ║          @WithMockUser · @WithUserDetails · @WithSecurityContext                     ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example05WithMockUser.java
 * Purpose:     Learn how to write security-aware tests using Spring Security's
 *              powerful test support annotations. Understand every testing pattern
 *              from basic @WithMockUser to custom security contexts.
 * Difficulty:  ⭐⭐⭐ Intermediate
 * Time:        25 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * NOTE: The live, executable test code is in:
 *   src/test/java/com/learning/springboot/chapter05/Chapter05SecurityTest.java
 *
 * This file provides:
 *   1. Comprehensive documentation of all security testing annotations
 *   2. Code examples shown in comment blocks (requires spring-security-test dependency)
 *   3. Custom @WithSecurityContext factory implementations
 *
 * DEPENDENCY REQUIRED (already in build.gradle):
 *   testImplementation 'org.springframework.security:spring-security-test'
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║       SECURITY TESTING ANNOTATIONS — THE COMPLETE GUIDE                      ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
public class Example05WithMockUser {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        SECTION 1: @WithMockUser — MOCK AN AUTHENTICATED USER                ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @WithMockUser creates a SYNTHETIC Authentication object in the SecurityContext
     * for the duration of a test method. No real user needs to exist in the database.
     * No actual login/authentication happens — Spring Security is told "this user is
     * already authenticated."
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔧 ATTRIBUTES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   username     → Username to use (default: "user")
     *   password     → Password in context (default: "password") — rarely used
     *   roles        → Roles to grant ("ADMIN" → "ROLE_ADMIN" auto-added) (default: "USER")
     *   authorities  → Fine-grained authorities (no "ROLE_" prefix added automatically)
     *   value        → Alias for username
     *   setupBefore  → When to setup (BEFORE_TEST_METHOD default, or BEFORE_TEST_CLASS)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 CODE EXAMPLES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * EXAMPLE 1: Default mock user (username="user", role=USER)
     *
     *   @Test
     *   @WithMockUser   // ← Creates user with username="user" and ROLE_USER
     *   void testReadEndpoint() throws Exception {
     *       mockMvc.perform(get("/api/v1/secure/users"))
     *           .andExpect(status().isOk());
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * EXAMPLE 2: Mock admin user
     *
     *   @Test
     *   @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
     *   void testAdminEndpoint() throws Exception {
     *       mockMvc.perform(get("/api/v1/secure/admin/dashboard"))
     *           .andExpect(status().isOk())
     *           .andExpect(jsonPath("$.panel").value("Admin Dashboard"));
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * EXAMPLE 3: Test 403 Forbidden (user WITHOUT required role)
     *
     *   @Test
     *   @WithMockUser(username = "alice", roles = "USER")  // ← NOT admin
     *   void testAdminEndpointForbidden() throws Exception {
     *       mockMvc.perform(get("/api/v1/secure/admin/dashboard"))
     *           .andExpect(status().isForbidden());  // 403
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * EXAMPLE 4: Test 401 Unauthorized (no user at all)
     *
     *   @Test   // No @WithMockUser → anonymous request
     *   void testUnauthenticated() throws Exception {
     *       mockMvc.perform(get("/api/v1/secure/users"))
     *           .andExpect(status().isUnauthorized());  // 401
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * EXAMPLE 5: Mock user with fine-grained authorities (not just roles)
     *
     *   @Test
     *   @WithMockUser(
     *       username    = "alice",
     *       authorities = {"ROLE_USER", "READ_ORDERS", "WRITE_ORDERS"}  // exact authorities
     *   )
     *   void testWithSpecificAuthorities() throws Exception {
     *       // alice has READ_ORDERS and WRITE_ORDERS but NOT DELETE_USERS
     *       mockMvc.perform(get("/api/v1/secure/docs"))
     *           .andExpect(status().isForbidden());  // docs needs ROLE_ADMIN
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * EXAMPLE 6: @WithMockUser on the CLASS (applies to all test methods)
     *
     *   @SpringBootTest
     *   @AutoConfigureMockMvc
     *   @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})  // ← Class level
     *   class AdminControllerTests {
     *
     *       @Test  // Each method in this class runs as admin
     *       void test1() throws Exception { ... }
     *
     *       @Test
     *       @WithMockUser(username = "alice", roles = "USER")  // ← Override for this method
     *       void test2() throws Exception { ... }
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * EXAMPLE 7: Testing @PostMapping that requires MODERATOR role
     *
     *   @Test
     *   @WithMockUser(username = "mod", roles = "MODERATOR")
     *   void testCreateUserAsModerator() throws Exception {
     *       mockMvc.perform(post("/api/v1/secure/users")
     *               .contentType(MediaType.APPLICATION_JSON)
     *               .content("{\"username\":\"newUser\"}"))
     *           .andExpect(status().isCreated());
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * EXAMPLE 8: HTTP Basic auth in tests (instead of @WithMockUser)
     *
     *   import static org.springframework.security.test.web.servlet.request
     *                     .SecurityMockMvcRequestPostProcessors.*;
     *
     *   @Test
     *   void testWithHttpBasic() throws Exception {
     *       mockMvc.perform(get("/api/v1/secure/me")
     *               .with(httpBasic("admin", "admin123")))  // ← Real HTTP Basic auth
     *           .andExpect(status().isOk());
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * 🔑 HOW @WithMockUser WORKS INTERNALLY:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @WithMockUser → Spring creates a WithMockUserSecurityContextFactory
     *   Factory creates a UsernamePasswordAuthenticationToken with:
     *     - principal = UserDetails (username, password, authorities)
     *     - credentials = "password"
     *     - authorities = [ROLE_USER, ROLE_ADMIN, etc.]
     *     - authenticated = true
     *   Sets this as the SecurityContext for the current test thread
     *   Test method runs with this security context
     *   After test: security context is cleared
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║      SECTION 2: @WithUserDetails — USE REAL UserDetailsService              ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @WithUserDetails loads a real user from the ACTUAL UserDetailsService in the
     * application context. Unlike @WithMockUser which creates a synthetic user,
     * @WithUserDetails calls your loadUserByUsername() method.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * @WithMockUser vs @WithUserDetails:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @WithMockUser:
     *   ✓ Simple, fast — no UserDetailsService needed
     *   ✓ Good for most unit/slice tests
     *   ✗ User doesn't come from your real UserDetailsService
     *   ✗ Misses custom UserDetails fields (e.g., AppUserDetails.appUser)
     *
     *   @WithUserDetails:
     *   ✓ Tests against REAL user from your UserDetailsService
     *   ✓ Tests your loadUserByUsername() code path
     *   ✓ Useful when @PreAuthorize uses custom UserDetails fields
     *   ✗ Requires the user to exist in the UserDetailsService
     *   ✗ Slightly slower (calls loadUserByUsername)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 CODE EXAMPLES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * BASIC USAGE:
     *
     *   @Test
     *   @WithUserDetails("admin")    // ← Loads "admin" from the real UserDetailsService
     *   void testAdminAction() throws Exception {
     *       mockMvc.perform(get("/api/v1/secure/admin/dashboard"))
     *           .andExpect(status().isOk());
     *   }
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * WITH SPECIFIC UserDetailsService (when multiple beans exist):
     *
     *   @WithUserDetails(
     *       value = "alice",
     *       userDetailsServiceBeanName = "appUserDetailsService"  // which bean to use
     *   )
     *   @Test
     *   void testAliceAccess() { ... }
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 3: @WithSecurityContext — FULLY CUSTOM SECURITY CONTEXT         ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @WithSecurityContext allows you to create a COMPLETELY CUSTOM meta-annotation
     * for your test security context. You create:
     *   1. A custom annotation (e.g., @WithAdminUser)
     *   2. A WithSecurityContextFactory implementation
     *   3. Spring uses the factory to set up the security context
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 WHEN TO USE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  • Your Authentication object has custom fields beyond username/roles
     *  • You need a specific type of Authentication (OAuth2, SAML, JWT, custom)
     *  • You have many tests with the same complex security setup — create a reusable annotation
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 HOW TO CREATE @WithAdminUser CUSTOM ANNOTATION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Step 1: Create the annotation:
     *
     *   @Retention(RetentionPolicy.RUNTIME)
     *   @WithSecurityContext(factory = WithAdminUserSecurityContextFactory.class)
     *   public @interface WithAdminUser {
     *       String username() default "admin";
     *       String department() default "Engineering";
     *   }
     *
     * Step 2: Create the factory:
     *
     *   @Component
     *   public class WithAdminUserSecurityContextFactory
     *           implements WithSecurityContextFactory<WithAdminUser> {
     *
     *       @Override
     *       public SecurityContext createSecurityContext(WithAdminUser annotation) {
     *           SecurityContext context = SecurityContextHolder.createEmptyContext();
     *
     *           // Build custom UserDetails with extra fields
     *           AppUserDetails userDetails = new AppUserDetails(
     *               new AppUser(99L, annotation.username(), "admin@test.com",
     *                           "{noop}test", Set.of("ADMIN"), Set.of("MANAGE_ALL"))
     *           );
     *
     *           // Create the authentication token
     *           UsernamePasswordAuthenticationToken auth =
     *               new UsernamePasswordAuthenticationToken(
     *                   userDetails, "test", userDetails.getAuthorities()
     *               );
     *
     *           context.setAuthentication(auth);
     *           return context;
     *       }
     *   }
     *
     * Step 3: Use in tests:
     *
     *   @Test
     *   @WithAdminUser(username = "superadmin", department = "Security")
     *   void testSuperAdminAction() throws Exception {
     *       mockMvc.perform(get("/api/v1/secure/admin/system"))
     *           .andExpect(status().isOk());
     *   }
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 4: MockMvc REQUEST POST-PROCESSORS                              ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Spring Security Test provides REQUEST POST-PROCESSORS for MockMvc:
     *
     * import static org.springframework.security.test.web.servlet.request
     *                   .SecurityMockMvcRequestPostProcessors.*;
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * AVAILABLE POST-PROCESSORS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   httpBasic("user","pass")          → Add HTTP Basic auth header
     *   user("alice").roles("USER")       → Inline mock user (alternative to @WithMockUser)
     *   anonymous()                       → Explicitly set anonymous user
     *   csrf()                            → Add valid CSRF token to request
     *   oauth2Login()                     → Mock OAuth2 login
     *   oidcLogin()                       → Mock OIDC login
     *   jwt()                             → Mock JWT authentication
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * CODE EXAMPLES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   // HTTP Basic auth
     *   mockMvc.perform(get("/api/v1/secure/me").with(httpBasic("admin", "admin123")))
     *       .andExpect(status().isOk());
     *
     *   // Inline user mock (alternative to @WithMockUser annotation)
     *   mockMvc.perform(get("/api/v1/secure/admin/dashboard")
     *           .with(user("admin").roles("ADMIN", "USER")))
     *       .andExpect(status().isOk());
     *
     *   // Anonymous user (override any class-level @WithMockUser)
     *   mockMvc.perform(get("/api/v1/secure/users").with(anonymous()))
     *       .andExpect(status().isUnauthorized());
     *
     *   // JWT Bearer token mock (for OAuth2 resource servers)
     *   mockMvc.perform(get("/api/v1/secure/me")
     *           .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
     *       .andExpect(status().isOk());
     *
     *   // CSRF token (required for POST/PUT/DELETE with CSRF enabled)
     *   mockMvc.perform(post("/submit").with(csrf()))
     *       .andExpect(status().isOk());
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 5: COMPLETE TEST CLASS STRUCTURE                                ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * RECOMMENDED TEST CLASS STRUCTURE for a secured REST API:
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   @SpringBootTest
     *   @AutoConfigureMockMvc
     *   @DisplayName("SecureUserController Tests")
     *   class SecureUserControllerTest {
     *
     *       @Autowired
     *       MockMvc mockMvc;
     *
     *       // ─── AUTHENTICATION TESTS ──────────────────────────────────────────────
     *
     *       @Nested
     *       @DisplayName("Authentication")
     *       class Authentication {
     *
     *           @Test
     *           @DisplayName("Unauthenticated → 401")
     *           void noCredentials_returns401() throws Exception {
     *               mockMvc.perform(get("/api/v1/secure/users"))
     *                   .andExpect(status().isUnauthorized());
     *           }
     *
     *           @Test
     *           @DisplayName("Invalid password → 401")
     *           void wrongPassword_returns401() throws Exception {
     *               mockMvc.perform(get("/api/v1/secure/users")
     *                       .with(httpBasic("admin", "WRONG")))
     *                   .andExpect(status().isUnauthorized());
     *           }
     *       }
     *
     *       // ─── AUTHORIZATION TESTS ───────────────────────────────────────────────
     *
     *       @Nested
     *       @DisplayName("Authorization")
     *       @WithMockUser(username = "alice", roles = "USER")  // Default for all tests
     *       class Authorization {
     *
     *           @Test
     *           @DisplayName("USER can GET users")
     *           void userCanGetUsers() throws Exception {
     *               mockMvc.perform(get("/api/v1/secure/users"))
     *                   .andExpect(status().isOk());
     *           }
     *
     *           @Test
     *           @DisplayName("USER cannot access admin → 403")
     *           void userCannotAccessAdmin() throws Exception {
     *               mockMvc.perform(get("/api/v1/secure/admin/dashboard"))
     *                   .andExpect(status().isForbidden());  // 403
     *           }
     *
     *           @Test
     *           @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
     *           @DisplayName("ADMIN can access dashboard")
     *           void adminCanAccessDashboard() throws Exception {
     *               mockMvc.perform(get("/api/v1/secure/admin/dashboard"))
     *                   .andExpect(status().isOk())
     *                   .andExpect(jsonPath("$.panel").value("Admin Dashboard"));
     *           }
     *       }
     *
     *       // ─── METHOD SECURITY TESTS ─────────────────────────────────────────────
     *
     *       @Nested
     *       @DisplayName("Method Security (@PreAuthorize)")
     *       class MethodSecurity {
     *
     *           @Autowired
     *           DocumentSecurityService docService;
     *
     *           @Test
     *           @WithMockUser(username = "admin", roles = "ADMIN")
     *           @DisplayName("Admin can get all documents")
     *           void adminGetsAllDocs() {
     *               List<SecureDocument> docs = docService.getAllDocuments();
     *               assertThat(docs).isNotEmpty();
     *           }
     *
     *           @Test
     *           @WithMockUser(username = "alice", roles = "USER")
     *           @DisplayName("Non-admin cannot get all documents → AccessDeniedException")
     *           void nonAdminCannotGetAllDocs() {
     *               assertThatThrownBy(() -> docService.getAllDocuments())
     *                   .isInstanceOf(AccessDeniedException.class);
     *           }
     *
     *           @Test
     *           @WithMockUser(username = "alice")
     *           @DisplayName("Alice can update her own document")
     *           void aliceCanUpdateOwnDoc() {
     *               SecureDocument result = docService.updateDocument("alice", 2L, "New content");
     *               assertThat(result.getContent()).isEqualTo("New content");
     *           }
     *
     *           @Test
     *           @WithMockUser(username = "alice")
     *           @DisplayName("Alice cannot update admin's document → AccessDeniedException")
     *           void aliceCannotUpdateAdminsDoc() {
     *               assertThatThrownBy(() -> docService.updateDocument("admin", 1L, "Hack!"))
     *                   .isInstanceOf(AccessDeniedException.class);
     *           }
     *       }
     *   }
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 6: SECURITY TESTING BEST PRACTICES                             ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🏆 TESTING CHECKLIST:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ✅  Test 401 — unauthenticated access is rejected
     *  ✅  Test 403 — authenticated but unauthorized access is rejected
     *  ✅  Test 200 — authorized access succeeds
     *  ✅  Test EACH role's access to EACH endpoint (role matrix)
     *  ✅  Test ownership-based rules (@PreAuthorize with #param == auth.name)
     *  ✅  Test @PostAuthorize filtering of restricted data
     *  ✅  Test @PreFilter removes unauthorized items from collections
     *  ✅  Test invalid/malformed tokens are rejected
     *  ✅  Use @WebMvcTest for controller-layer security (fast)
     *  ✅  Use @SpringBootTest for full integration security tests
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 KEY POINTS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  @WithMockUser     → Best for most controller tests (fast, no DB needed)
     *  @WithUserDetails  → When you need to test your custom UserDetailsService
     *  httpBasic() post-processor → When testing the actual HTTP Basic auth flow
     *  jwt() post-processor → When testing OAuth2/JWT resource server
     *
     *  ⚠️  @WithMockUser bypasses authentication (UsernamePasswordFilter) and goes
     *      straight to authorization. To test the FULL authentication flow, use
     *      httpBasic() or formLogin() post-processors instead.
     *
     */
}

