package com.learning.springboot.chapter05;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║          EXAMPLE 03: @Secured AND @RolesAllowed IN ACTION                            ║
 * ║          @Secured · @RolesAllowed · @PermitAll · @DenyAll                           ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example03SecuredAndRolesAllowed.java
 * Purpose:     Learn two simpler alternatives to @PreAuthorize for role-based access.
 *              Understand the differences, when to use each, and their limitations.
 * Difficulty:  ⭐⭐ Beginner–Intermediate
 * Time:        20 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * SCENARIO: An inventory management system demonstrating all three security annotation
 *           styles side by side.
 *
 * PREREQUISITES: @EnableMethodSecurity(securedEnabled=true, jsr250Enabled=true)
 *   is declared in Chapter05SecurityConfig (Example01).
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  DOMAIN MODEL
// ══════════════════════════════════════════════════════════════════════════════════════

class InventoryItem {
    private Long   id;
    private String name;
    private int    quantity;
    private double price;
    private String category;

    public InventoryItem(Long id, String name, int qty, double price, String category) {
        this.id = id; this.name = name; this.quantity = qty;
        this.price = price; this.category = category;
    }

    public Long   getId()       { return id; }
    public String getName()     { return name; }
    public int    getQuantity() { return quantity; }
    public double getPrice()    { return price; }
    public String getCategory() { return category; }
    public void   setQuantity(int qty) { this.quantity = qty; }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 1: @Secured — SPRING'S SIMPLE ROLE-BASED ANNOTATION
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                         @Secured  EXPLAINED IN DEPTH                         ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @Secured is Spring Security's OWN annotation for role-based method security.
 * It predates @PreAuthorize and is simpler — but LESS POWERFUL.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔧 SYNTAX:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   @Secured("ROLE_ADMIN")                  ← single role
 *   @Secured({"ROLE_ADMIN", "ROLE_MOD"})    ← ANY of these roles (OR logic)
 *   @Secured("IS_AUTHENTICATED")            ← any authenticated user
 *   @Secured("IS_AUTHENTICATED_FULLY")      ← not via remember-me
 *   @Secured("IS_AUTHENTICATED_ANONYMOUSLY")← anonymous user
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ IMPORTANT DIFFERENCE FROM hasRole():
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   @Secured("ROLE_ADMIN")           → value must include "ROLE_" prefix
 *   @PreAuthorize("hasRole('ADMIN')") → "ROLE_" prefix added automatically
 *
 *   @Secured("ADMIN")                → ❌ WRONG — Spring looks for exact "ADMIN"
 *   @PreAuthorize("hasRole('ADMIN')") → ✅ CORRECT — resolves to "ROLE_ADMIN"
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 WHEN TO USE @Secured:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  ✅ Simple role-based access with a Spring-specific project
 *  ✅ When you need only role checks (no complex SpEL)
 *  ✅ Slightly less verbose than @PreAuthorize for simple cases
 *  ❌ When you need SpEL expressions (ownership, conditions) → use @PreAuthorize
 *  ❌ When you want portability to non-Spring frameworks → use @RolesAllowed
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🚀 ENABLE: @EnableMethodSecurity(securedEnabled = true) must be set.
 *    Without securedEnabled=true, @Secured annotations are IGNORED.
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Service
class InventorySecuredService {

    private final List<InventoryItem> inventory = List.of(
            new InventoryItem(1L, "Laptop",   100, 1200.00, "Electronics"),
            new InventoryItem(2L, "Keyboard",  50,   99.99, "Electronics"),
            new InventoryItem(3L, "Desk",      20,  450.00, "Furniture"),
            new InventoryItem(4L, "Chair",     30,  250.00, "Furniture")
    );

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 1: Single role requirement
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * List all inventory items.
     * Requires ROLE_ADMIN.
     *
     * @Secured("ROLE_ADMIN")  ← ⚠️ Note: full "ROLE_ADMIN", NOT "ADMIN"
     *
     * Try:
     *   alice (ROLE_USER) → 403 Forbidden
     *   admin (ROLE_ADMIN) → 200 OK
     */
    @Secured("ROLE_ADMIN")
    public List<InventoryItem> getAllInventory() {
        System.out.println("📦 getAllInventory() — @Secured('ROLE_ADMIN') passed");
        return inventory;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 2: Multiple roles (OR logic — user needs ANY ONE of these)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * View stock levels.
     * Accessible to ADMIN or MODERATOR.
     *
     * @Secured({"ROLE_ADMIN", "ROLE_MODERATOR"})
     * → User needs ROLE_ADMIN OR ROLE_MODERATOR (OR logic)
     * → alice (ROLE_USER only) → 403
     * → moderator (ROLE_MODERATOR) → allowed
     * → admin (ROLE_ADMIN) → allowed
     */
    @Secured({"ROLE_ADMIN", "ROLE_MODERATOR"})
    public Map<String, Integer> getStockLevels() {
        System.out.println("📊 getStockLevels() — @Secured(['ROLE_ADMIN','ROLE_MODERATOR']) passed");
        return Map.of(
                "Laptop",   100,
                "Keyboard", 50,
                "Desk",     20,
                "Chair",    30
        );
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 3: Any authenticated user
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Read public catalog.
     * Accessible to any authenticated user.
     *
     * @Secured("IS_AUTHENTICATED")
     * → Any user who has authenticated (regardless of role)
     */
    @Secured("IS_AUTHENTICATED")
    public List<String> getPublicCatalog() {
        System.out.println("📋 getPublicCatalog() — @Secured('IS_AUTHENTICATED') passed");
        return inventory.stream().map(InventoryItem::getName).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // LIMITATION OF @Secured: No SpEL — this is IMPOSSIBLE with @Secured
    // ─────────────────────────────────────────────────────────────────────────────

    /*
     * ❌ CAN'T DO THIS WITH @Secured:
     *
     * @Secured("hasRole('ADMIN') and #itemId > 0")  ← WRONG: @Secured doesn't support SpEL
     * @Secured("#username == authentication.name")  ← WRONG: no parameter binding
     *
     * ✅ USE @PreAuthorize INSTEAD:
     *
     * @PreAuthorize("hasRole('ADMIN') and #itemId > 0")
     * @PreAuthorize("#username == authentication.name")
     */
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 2: @RolesAllowed — JSR-250 (Jakarta) STANDARD
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                       @RolesAllowed  EXPLAINED IN DEPTH                      ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @RolesAllowed is a JSR-250 annotation defined in the Jakarta EE specification
 * (jakarta.annotation.security.RolesAllowed).
 * It is functionally equivalent to @Secured but follows the Jakarta EE standard.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 @RolesAllowed vs @Secured:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   FEATURE                    @Secured           @RolesAllowed
 *   ─────────────────────────  ─────────────────  ─────────────────────────────
 *   Namespace                  Spring             Jakarta EE (JSR-250)
 *   Portability                Spring-only        Works in Java EE/Jakarta apps too
 *   Role prefix ("ROLE_")      Required           Required
 *   SpEL support               No                 No
 *   Multiple roles (OR)        Yes (array)        Yes (array)
 *   Companion annotations      None               @PermitAll, @DenyAll
 *   Enable flag                securedEnabled     jsr250Enabled
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 WHEN TO USE @RolesAllowed OVER @Secured:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  ✅ When your application might run in a Jakarta EE container too (WildFly, Payara)
 *  ✅ When your team has a standard of using Jakarta annotations
 *  ✅ When you use the companion @PermitAll / @DenyAll annotations
 *  ❌ If you're Spring-only and don't need portability → @Secured or @PreAuthorize
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🚀 ENABLE: @EnableMethodSecurity(jsr250Enabled = true) must be set.
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Service
class InventoryRolesAllowedService {

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 1: Single role with @RolesAllowed
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Get full inventory detail.
     * Requires ADMIN role.
     *
     * @RolesAllowed("ROLE_ADMIN")
     * ← Functionally identical to @Secured("ROLE_ADMIN")
     *
     * Note: "ROLE_" prefix is still required with @RolesAllowed in Spring Security.
     * (In a pure Jakarta EE container, roles are typically without "ROLE_" prefix)
     */
    @RolesAllowed("ROLE_ADMIN")
    public List<InventoryItem> getFullInventoryDetails() {
        System.out.println("📦 getFullInventoryDetails() — @RolesAllowed('ROLE_ADMIN') passed");
        return List.of(
                new InventoryItem(1L, "Laptop",   100, 1200.00, "Electronics"),
                new InventoryItem(2L, "Keyboard",  50,   99.99, "Electronics")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 2: Multiple roles with @RolesAllowed
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Process inventory reorder.
     * Requires ADMIN or MODERATOR role.
     *
     * @RolesAllowed({"ROLE_ADMIN", "ROLE_MODERATOR"})
     */
    @RolesAllowed({"ROLE_ADMIN", "ROLE_MODERATOR"})
    public Map<String, Object> processReorder(Long itemId, int quantity) {
        System.out.println("🔄 processReorder() — @RolesAllowed(['ROLE_ADMIN','ROLE_MODERATOR']) passed");
        return Map.of(
                "itemId",      itemId,
                "reorderQty",  quantity,
                "status",      "REORDER_PLACED"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 3: @PermitAll — Allow everyone (even anonymous)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Get public product information.
     * @PermitAll allows ANYONE — even unauthenticated users.
     *
     * @PermitAll is equivalent to @PreAuthorize("permitAll()")
     * It's a JSR-250 annotation (jakarta.annotation.security.PermitAll).
     *
     * Use case: A product catalog visible to the public (no login required).
     */
    @PermitAll    // ← JSR-250: Allow ALL users including anonymous
    public List<String> getPublicProductNames() {
        System.out.println("📋 getPublicProductNames() — @PermitAll (no auth required)");
        return List.of("Laptop", "Keyboard", "Desk", "Chair");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 4: @DenyAll — Block everyone unconditionally
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Deprecated system method — NOBODY can call this, not even admins.
     *
     * @DenyAll is equivalent to @PreAuthorize("denyAll()")
     * Use case: Marking methods as disabled/deprecated in the security layer.
     *
     * ⚠️ Important: @DenyAll is checked by Spring Security — it's different from
     * just removing the method. It gives a controlled 403 instead of 404.
     */
    @DenyAll    // ← JSR-250: Deny ALL users, no exceptions
    public void deprecatedSystemReset() {
        // This code never executes — @DenyAll prevents it at the security layer
        throw new UnsupportedOperationException("This method is permanently disabled");
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 3: COMPARISON TABLE — All Three Annotation Styles
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║     @PreAuthorize vs @Secured vs @RolesAllowed — COMPLETE COMPARISON         ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📊 COMPARISON TABLE:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  FEATURE                   @PreAuthorize          @Secured         @RolesAllowed
 *  ────────────────────────  ───────────────────    ──────────────   ─────────────
 *  Package                   spring-security-core   spring-security  jakarta.annotation
 *  Portability                Spring only           Spring only      Jakarta EE ✅
 *  Enable flag               prePostEnabled=true    securedEnabled   jsr250Enabled
 *  SpEL support              ✅ Full SpEL            ❌ None          ❌ None
 *  Role prefix (ROLE_)       Auto-added by          Must add manually Must add manually
 *                            hasRole()              @Secured("ROLE_X") @RolesAllowed("ROLE_X")
 *  OR logic (any role)       hasAnyRole('A','B')    @Secured({"A","B"}) @RolesAllowed({"A","B"})
 *  AND logic                 hasRole('A') and ...   ❌ Impossible    ❌ Impossible
 *  Parameter access          ✅ #paramName           ❌ None          ❌ None
 *  Return value access       ✅ returnObject          ❌ None          ❌ None
 *  Bean method call          ✅ @beanName.method()   ❌ None          ❌ None
 *  Companion annotations     None                   None             @PermitAll, @DenyAll
 *  Default in Spring Boot    ✅ Enabled              ❌ Off           ❌ Off
 *  Recommended for           Everything             Simple roles     Portability
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🏆 RECOMMENDATION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  DEFAULT CHOICE → @PreAuthorize
 *    Most powerful, most flexible, recommended for all new Spring applications.
 *    Works for simple cases too: @PreAuthorize("hasRole('ADMIN')")
 *
 *  USE @RolesAllowed WHEN:
 *    → You want Jakarta EE portability
 *    → Your team standardises on Jakarta security annotations
 *    → You also use @PermitAll / @DenyAll
 *
 *  USE @Secured WHEN:
 *    → You have a legacy Spring project already using it
 *    → You want to avoid SpEL entirely (simpler mental model)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📝 EQUIVALENT EXAMPLES — Three ways to say the same thing:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   // "Only ADMIN can call this method"
 *   @PreAuthorize("hasRole('ADMIN')")                  ← recommended
 *   @Secured("ROLE_ADMIN")                             ← simpler, Spring-specific
 *   @RolesAllowed("ROLE_ADMIN")                        ← Jakarta portable
 *
 *   // "ADMIN or MODERATOR can call this method"
 *   @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
 *   @Secured({"ROLE_ADMIN", "ROLE_MODERATOR"})
 *   @RolesAllowed({"ROLE_ADMIN", "ROLE_MODERATOR"})
 *
 *   // "Only caller with username matching #userId" — ONLY possible with @PreAuthorize
 *   @PreAuthorize("#userId == authentication.name")    ← @Secured / @RolesAllowed can't do this!
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 STACKING ANNOTATIONS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * You CAN stack multiple method security annotations:
 *
 *   @PreAuthorize("hasRole('ADMIN')")
 *   @Secured("ROLE_MODERATOR")    // User must satisfy BOTH annotations!
 *   public void strictMethod() { ... }
 *
 * ⚠️ This creates AND logic (user must satisfy ALL of them).
 * This is rarely what you want — prefer a single @PreAuthorize with AND/OR.
 *
 */
class Example03SecuredAndRolesAllowed {
    // Intentionally empty — documentation class
}

