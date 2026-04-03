package com.learning.springboot.chapter05;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║          EXAMPLE 04: UserDetailsService & PasswordEncoder IN DEPTH                   ║
 * ║          UserDetails · UserDetailsService · BCryptPasswordEncoder                    ║
 * ║          DelegatingPasswordEncoder · Custom Implementation                           ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example04UserDetailsService.java
 * Purpose:     Deep dive into how Spring Security loads users and encodes passwords.
 *              Implement a custom UserDetailsService backed by an in-memory store
 *              (simulating a database). Understand all PasswordEncoder strategies.
 * Difficulty:  ⭐⭐⭐ Intermediate
 * Time:        30 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * NOTE: The ACTIVE UserDetailsService and PasswordEncoder beans are defined in
 * Example01WebSecurityConfig (Chapter05SecurityConfig). This file contains:
 *   1. Deep documentation of the UserDetails contract
 *   2. A CUSTOM UserDetails implementation (AppUserDetails)
 *   3. A CUSTOM UserDetailsService implementation (AppUserDetailsService)
 *   4. All PasswordEncoder strategies documented and demonstrated
 *   5. Password migration strategies (DelegatingPasswordEncoder)
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 1: UserDetails — THE USER INFORMATION CONTRACT
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                     UserDetails  EXPLAINED IN DEPTH                          ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * UserDetails is the CORE USER OBJECT in Spring Security.
 * It provides all information needed for Authentication and Authorization.
 *
 * When Spring Security needs to authenticate a user:
 *   1. Calls UserDetailsService.loadUserByUsername(username)
 *   2. Gets back a UserDetails object
 *   3. Compares the provided password with UserDetails.getPassword() (via PasswordEncoder)
 *   4. Checks isAccountNonLocked(), isAccountNonExpired(), isEnabled()
 *   5. If everything passes → creates an Authentication and stores in SecurityContext
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔧 UserDetails CONTRACT — 7 methods to implement:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   getUsername()           → The unique identifier (usually email or username)
 *   getPassword()           → The HASHED password (BCrypt, Argon2, etc.)
 *   getAuthorities()        → Collection of granted authorities (roles + permissions)
 *   isAccountNonExpired()   → Can user still log in? (false = force re-registration)
 *   isAccountNonLocked()    → Is account unlocked? (false = temporarily banned)
 *   isCredentialsNonExpired()→ Password still valid? (false = force password change)
 *   isEnabled()             → Account active? (false = soft-deleted / deactivated)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */

/** Domain model for an application user stored in the "database". */
class AppUser {
    private Long   id;
    private String username;
    private String email;
    private String hashedPassword;
    private Set<String> roles;
    private Set<String> permissions;
    private boolean enabled;
    private boolean locked;
    private boolean credentialsExpired;

    public AppUser(Long id, String username, String email, String hashedPassword,
                   Set<String> roles, Set<String> perms) {
        this.id = id; this.username = username; this.email = email;
        this.hashedPassword = hashedPassword; this.roles = roles;
        this.permissions = perms;
        this.enabled = true; this.locked = false; this.credentialsExpired = false;
    }

    public Long    getId()               { return id; }
    public String  getUsername()         { return username; }
    public String  getEmail()            { return email; }
    public String  getHashedPassword()   { return hashedPassword; }
    public Set<String> getRoles()        { return roles; }
    public Set<String> getPermissions()  { return permissions; }
    public boolean isEnabled()           { return enabled; }
    public boolean isLocked()            { return locked; }
    public boolean isCredentialsExpired(){ return credentialsExpired; }
    public void setEnabled(boolean v)    { this.enabled = v; }
    public void setLocked(boolean v)     { this.locked  = v; }
    public void setHashedPassword(String p) { this.hashedPassword = p; }
}

/**
 * ─────────────────────────────────────────────────────────────────────────────────
 * CUSTOM UserDetails IMPLEMENTATION
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Wraps our AppUser domain object and adapts it to the UserDetails interface.
 * This is the ADAPTER PATTERN — adapting AppUser to what Spring Security expects.
 *
 * WHY SEPARATE DOMAIN MODEL FROM UserDetails?
 *   • AppUser = your domain model (stored in DB, has business logic)
 *   • UserDetails = Spring Security's view of the user (authentication/authorization)
 *   • Keeping them separate follows the Single Responsibility Principle
 *   • Lets you change AppUser without affecting Spring Security integration
 *
 * ALTERNATIVE: Make AppUser itself implement UserDetails (simpler, but tightly coupled).
 */
class AppUserDetails implements UserDetails {

    private final AppUser appUser;

    public AppUserDetails(AppUser appUser) {
        this.appUser = appUser;
    }

    /**
     * Returns ALL granted authorities.
     * Spring Security uses this for ALL authorization decisions.
     *
     * We combine:
     *   - ROLES (with "ROLE_" prefix) for hasRole() / @Secured / @RolesAllowed
     *   - PERMISSIONS (exact strings) for hasAuthority() / @PreAuthorize
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add roles with ROLE_ prefix (Spring Security convention)
        appUser.getRoles().forEach(role ->
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));

        // Add fine-grained permissions (no prefix — used with hasAuthority())
        appUser.getPermissions().forEach(perm ->
                authorities.add(new SimpleGrantedAuthority(perm)));

        return Collections.unmodifiableSet(authorities);
    }

    @Override
    public String getPassword() {
        return appUser.getHashedPassword();   // MUST be the HASHED password
    }

    @Override
    public String getUsername() {
        return appUser.getUsername();
    }

    /**
     * isAccountNonExpired — can this account still be used?
     *
     * Return false when:
     *   • Free trial period has ended
     *   • Account has been inactive too long (security policy)
     *   • Subscription has expired
     *
     * Effect: login attempt fails with "Account Expired" error.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;   // Simplified: all accounts never expire in this demo
    }

    /**
     * isAccountNonLocked — is the account unlocked?
     *
     * Return false when:
     *   • Too many failed login attempts (brute force protection)
     *   • Admin manually locked the account
     *   • Suspicious activity detected
     *
     * Effect: login fails with "Account Locked" error.
     * Typically combined with a time-based unlock mechanism.
     */
    @Override
    public boolean isAccountNonLocked() {
        return !appUser.isLocked();   // false = account is locked → login denied
    }

    /**
     * isCredentialsNonExpired — is the password still valid?
     *
     * Return false when:
     *   • Password is older than the security policy allows (e.g., > 90 days)
     *   • Admin forces a password reset
     *   • User changed password on another device
     *
     * Effect: login fails with "Credentials Expired" error → redirect to change password.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return !appUser.isCredentialsExpired();
    }

    /**
     * isEnabled — is the account active?
     *
     * Return false when:
     *   • Email not yet verified (registration flow)
     *   • Account soft-deleted
     *   • Account deactivated by admin
     *
     * Effect: login fails with "Account Disabled" error.
     */
    @Override
    public boolean isEnabled() {
        return appUser.isEnabled();
    }

    /** Access the underlying domain model (not part of UserDetails interface). */
    public AppUser getAppUser() {
        return appUser;
    }

    @Override
    public String toString() {
        return String.format("AppUserDetails{username='%s', roles=%s, permissions=%s}",
                appUser.getUsername(), appUser.getRoles(), appUser.getPermissions());
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 2: UserDetailsService — LOADING USERS DURING AUTHENTICATION
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                    UserDetailsService  EXPLAINED IN DEPTH                    ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * UserDetailsService is an interface with ONE method:
 *
 *   UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
 *
 * Spring Security calls this method during EVERY authentication attempt.
 * You implement it to load your user from your data source.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔄 AUTHENTICATION FLOW WITH UserDetailsService:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   1. User sends: POST /login { username: "admin", password: "admin123" }
 *        ↓
 *   2. UsernamePasswordAuthenticationFilter extracts credentials
 *        ↓
 *   3. DaoAuthenticationProvider.authenticate() is called
 *        ↓
 *   4. YOUR UserDetailsService.loadUserByUsername("admin") is called
 *        ↓
 *   5. Returns AppUserDetails with BCrypt hash: $2a$12$eJ...
 *        ↓
 *   6. BCryptPasswordEncoder.matches("admin123", "$2a$12$eJ...") → TRUE
 *        ↓
 *   7. isAccountNonLocked(), isEnabled(), isAccountNonExpired() checks → all true
 *        ↓
 *   8. Authentication object created with username + authorities
 *        ↓
 *   9. SecurityContextHolder.getContext().setAuthentication(auth)
 *        ↓
 *  10. User is authenticated → can access protected resources
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ IMPORTANT: loadUserByUsername should NEVER return null.
 *   If the user is not found → throw UsernameNotFoundException.
 *   Spring Security catches this and converts it to a generic "Bad credentials" error
 *   (to prevent username enumeration attacks).
 * ─────────────────────────────────────────────────────────────────────────────────
 */

/**
 * Custom UserDetailsService implementation backed by an in-memory user store.
 * In a real application, this would query a JPA repository, LDAP, or REST API.
 *
 * NOTE: This class is a documentation demo. The ACTIVE UserDetailsService bean
 * (InMemoryUserDetailsManager) is in Example01WebSecurityConfig.
 * In a real project, you'd add @Service here and it would be the active bean.
 */
class AppUserDetailsService implements UserDetailsService {

    // Simulated user "database"
    private final Map<String, AppUser> userDatabase;
    private final PasswordEncoder passwordEncoder;

    public AppUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.userDatabase    = new HashMap<>();
        seedDemoUsers();
        System.out.println("✅ AppUserDetailsService: " + userDatabase.size() + " users loaded");
    }

    /**
     * CALLED BY SPRING SECURITY during every authentication attempt.
     *
     * @param username the username submitted in the login form / HTTP Basic header
     * @return a fully populated UserDetails object
     * @throws UsernameNotFoundException if the username is not found
     *
     * ⚠️ Security note: Do NOT reveal whether the username or password was wrong.
     * Always throw UsernameNotFoundException with a GENERIC message like
     * "Bad credentials" — Spring Security converts it to the same error message
     * for both "user not found" and "wrong password" cases.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.printf("🔍 loadUserByUsername('%s') — looking up user%n", username);

        AppUser user = userDatabase.get(username.toLowerCase());
        if (user == null) {
            // ⚠️ Don't say "username not found" — prevents username enumeration!
            throw new UsernameNotFoundException("Bad credentials");
        }

        System.out.printf("✅ User '%s' found — roles: %s%n", username, user.getRoles());
        return new AppUserDetails(user);  // Wrap in UserDetails adapter
    }

    /**
     * Seeds the in-memory user database with demo data.
     * In a real app: users come from a @Repository / JPA / JDBC.
     */
    private void seedDemoUsers() {
        // admin with ADMIN and USER roles + all permissions
        userDatabase.put("admin", new AppUser(
                1L, "admin", "admin@company.com",
                passwordEncoder.encode("admin123"),         // BCrypt hash stored
                new HashSet<>(Set.of("ADMIN", "USER")),
                new HashSet<>(Set.of("READ_USERS", "WRITE_USERS", "DELETE_USERS",
                        "READ_ORDERS", "WRITE_ORDERS", "VIEW_REPORTS"))
        ));

        // alice with USER role (read-only)
        userDatabase.put("alice", new AppUser(
                2L, "alice", "alice@company.com",
                passwordEncoder.encode("alice123"),
                new HashSet<>(Set.of("USER")),
                new HashSet<>(Set.of("READ_USERS", "READ_ORDERS"))
        ));

        // Locked account demo
        AppUser locked = new AppUser(
                3L, "lockeduser", "locked@company.com",
                passwordEncoder.encode("password123"),
                new HashSet<>(Set.of("USER")),
                Collections.emptySet()
        );
        locked.setLocked(true);    // This user's account is LOCKED
        userDatabase.put("lockeduser", locked);

        // Disabled account demo
        AppUser disabled = new AppUser(
                4L, "disabled", "disabled@company.com",
                passwordEncoder.encode("password123"),
                new HashSet<>(Set.of("USER")),
                Collections.emptySet()
        );
        disabled.setEnabled(false);   // This user's account is DISABLED
        userDatabase.put("disabled", disabled);
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 3: PasswordEncoder — ALL STRATEGIES
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║             PasswordEncoder STRATEGIES — COMPLETE REFERENCE                  ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 PasswordEncoder INTERFACE:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Two key methods:
 *
 *   String encode(CharSequence rawPassword)
 *     → Takes plain text password → returns hashed string
 *     → Called when CREATING or CHANGING a password
 *     → Each call produces a DIFFERENT hash (because of random salt)
 *
 *   boolean matches(CharSequence rawPassword, String encodedPassword)
 *     → Verifies a plain text password against the stored hash
 *     → Called during EVERY login attempt
 *     → Extracts salt from stored hash, re-hashes rawPassword, compares
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📊 PASSWORD ENCODING DEMO:
 * ─────────────────────────────────────────────────────────────────────────────────
 */
class PasswordEncoderDemo {

    static void demonstrateAllEncoders() {
        // ─────────────────────────────────────────────────────────────────────────
        // Strategy 1: BCryptPasswordEncoder — RECOMMENDED
        // ─────────────────────────────────────────────────────────────────────────
        PasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

        String rawPassword      = "MySecretPassword123";
        String bcryptHash1      = bcrypt.encode(rawPassword);
        String bcryptHash2      = bcrypt.encode(rawPassword);   // Different hash! (different salt)

        System.out.println("\n🔐 BCryptPasswordEncoder (strength=12):");
        System.out.println("  Raw:    " + rawPassword);
        System.out.println("  Hash 1: " + bcryptHash1);    // e.g., $2a$12$AB...
        System.out.println("  Hash 2: " + bcryptHash2);    // e.g., $2a$12$XY... (DIFFERENT!)
        System.out.println("  Matches: " + bcrypt.matches(rawPassword, bcryptHash1));   // → true

        // Notice: Both hashes verify correctly despite being different!
        // The salt is EMBEDDED in the hash ($2a$12$<22-char salt><31-char hash>)

        // ─────────────────────────────────────────────────────────────────────────
        // Strategy 2: DelegatingPasswordEncoder — PASSWORD MIGRATION SOLUTION
        // ─────────────────────────────────────────────────────────────────────────

        /*
         * DelegatingPasswordEncoder EXPLAINED:
         *
         * When you migrate an existing system to Spring Security, users might have
         * passwords in different formats (some MD5, some BCrypt, some NoOp).
         *
         * DelegatingPasswordEncoder:
         *   - Stores the encoding ID in the hash: {bcrypt}$2a$12$...
         *   - Can VERIFY hashes from multiple algorithms (backward compatibility)
         *   - Always ENCODES new passwords with the current algorithm (BCrypt)
         *   - Gradually migrates old passwords to the new algorithm
         *
         * Hash formats with encoding IDs:
         *   {bcrypt}$2a$12$...         ← BCrypt-hashed password
         *   {noop}password123          ← Plain text (legacy, INSECURE)
         *   {pbkdf2}...                ← PBKDF2-hashed
         *   {scrypt}...                ← SCrypt-hashed
         *   {argon2}...                ← Argon2-hashed
         *
         * Spring Security uses DelegatingPasswordEncoder BY DEFAULT when you call:
         *   PasswordEncoderFactories.createDelegatingPasswordEncoder()
         */
        PasswordEncoder delegating = PasswordEncoderFactories.createDelegatingPasswordEncoder();

        String delegatingHash = delegating.encode(rawPassword);
        System.out.println("\n🔐 DelegatingPasswordEncoder (default=bcrypt):");
        System.out.println("  Hash: " + delegatingHash);       // {bcrypt}$2a$12$...
        System.out.println("  Matches: " + delegating.matches(rawPassword, delegatingHash));

        // Can ALSO verify old-style hashed passwords:
        String noOpHash = "{noop}password123";
        System.out.println("  Matches {noop}: " + delegating.matches("password123", noOpHash));  // true

        // ─────────────────────────────────────────────────────────────────────────
        // Strategy 3: Argon2 (most secure) — requires Bouncy Castle dependency
        // ─────────────────────────────────────────────────────────────────────────

        /*
         * Argon2PasswordEncoder — requires: org.bouncycastle:bcpkix-jdk15on
         *
         * Argon2 is the winner of the Password Hashing Competition (2015).
         * It is MEMORY-HARD (requires significant RAM to compute), making
         * GPU/ASIC attacks much more expensive.
         *
         * Usage (when dependency is available):
         *
         *   PasswordEncoder argon2 = new Argon2PasswordEncoder(
         *       16,   // salt length in bytes
         *       32,   // hash length in bytes
         *       1,    // parallelism
         *       65536,// memory (KB) — 64MB
         *       10    // iterations
         *   );
         *   String hash = argon2.encode("password");
         *   // → $argon2id$v=19$m=65536,t=10,p=1$...
         */

        System.out.println("\n📋 Password Encoding Guidelines:");
        System.out.println("  ✅ BCrypt(12)  — Recommended for most apps");
        System.out.println("  ✅ Argon2      — Best security (needs BouncyCastle)");
        System.out.println("  ✅ PBKDF2      — FIPS-compliant environments");
        System.out.println("  ⚠️  BCrypt(10)  — Acceptable (default), consider upgrading");
        System.out.println("  ❌ MD5/SHA-1   — NEVER use for passwords (too fast)");
        System.out.println("  ❌ NoOp        — NEVER use in production!");
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 4: PASSWORD MIGRATION PATTERN
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║       UPGRADING PASSWORDS ON LOGIN — MIGRATION PATTERN                       ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * When migrating from an old password hashing algorithm to BCrypt,
 * you can't re-hash all passwords immediately (you don't have the originals).
 *
 * STRATEGY: Upgrade on next login
 *
 *   1. Store old hash with legacy algorithm marker: {md5}5f4dcc3b5aa...
 *   2. On login: DelegatingPasswordEncoder verifies with the right algorithm
 *   3. After successful login: if not BCrypt, re-hash with BCrypt and save
 *   4. Over time, all active users' passwords migrate to BCrypt
 *
 * IMPLEMENTATION (with Spring UserDetailsPasswordService):
 *
 *   @Service
 *   class PasswordUpgradeUserDetailsService implements UserDetailsService,
 *                                                      UserDetailsPasswordService {
 *       @Override
 *       public UserDetails updatePassword(UserDetails user, String newEncodedPassword) {
 *           // This is called automatically by DaoAuthenticationProvider
 *           // after a successful login if the password needs upgrading
 *           userRepo.updatePassword(user.getUsername(), newEncodedPassword);
 *           return User.withUserDetails(user).password(newEncodedPassword).build();
 *       }
 *
 *       @Override
 *       public UserDetails loadUserByUsername(String username) { ... }
 *   }
 *
 * Spring Security automatically calls updatePassword() when it detects that
 * the current encoder differs from the encoder used to hash the stored password.
 *
 */
class Example04UserDetailsService {
    // Intentionally empty — documentation class
}

