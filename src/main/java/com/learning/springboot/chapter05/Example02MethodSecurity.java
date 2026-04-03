package com.learning.springboot.chapter05;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║          EXAMPLE 02: METHOD-LEVEL SECURITY IN ACTION                                 ║
 * ║          @EnableMethodSecurity · @PreAuthorize · @PostAuthorize                      ║
 * ║          @PreFilter · @PostFilter                                                    ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example02MethodSecurity.java
 * Purpose:     Master every form of method-level security annotation.
 *              Learn to write SpEL expressions from simple role checks to
 *              complex ownership-based and permission-based authorization.
 * Difficulty:  ⭐⭐⭐⭐ Intermediate–Advanced
 * Time:        40 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * PREREQUISITES:
 *   @EnableMethodSecurity is declared in Chapter05SecurityConfig (Example01).
 *   It must be declared ONCE for the entire application.
 *   All @PreAuthorize, @PostAuthorize, @Secured, @RolesAllowed annotations across
 *   ALL beans in the application will be processed.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  DOMAIN MODELS
// ══════════════════════════════════════════════════════════════════════════════════════

/** Represents a document with an owner. Used to demonstrate ownership-based security. */
class SecureDocument {
    private Long   id;
    private String title;
    private String content;
    private String ownerUsername;   // Owner's username
    private boolean confidential;

    public SecureDocument(Long id, String title, String content,
                          String owner, boolean confidential) {
        this.id = id; this.title = title; this.content = content;
        this.ownerUsername = owner; this.confidential = confidential;
    }

    public Long   getId()            { return id; }
    public String getTitle()         { return title; }
    public String getContent()       { return content; }
    public String getOwnerUsername() { return ownerUsername; }
    public boolean isConfidential()  { return confidential; }
    public void setContent(String c) { this.content = c; }
}

/** Represents a financial report with access control. */
class FinancialReport {
    private Long   id;
    private String reportType;
    private String department;
    private double amount;
    private boolean restricted;

    public FinancialReport(Long id, String type, String dept, double amount, boolean restricted) {
        this.id = id; this.reportType = type; this.department = dept;
        this.amount = amount; this.restricted = restricted;
    }

    public Long   getId()          { return id; }
    public String getReportType()  { return reportType; }
    public String getDepartment()  { return department; }
    public double getAmount()      { return amount; }
    public boolean isRestricted()  { return restricted; }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 1: @PreAuthorize — CHECK BEFORE METHOD RUNS
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                     @PreAuthorize  EXPLAINED IN DEPTH                        ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @PreAuthorize evaluates a SpEL (Spring Expression Language) expression BEFORE
 * the annotated method is invoked. If the expression evaluates to FALSE:
 *   → Throws AccessDeniedException → Spring Security returns 403 Forbidden
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 HOW IT WORKS INTERNALLY:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   1. Spring wraps the @Service bean in a CGLIB proxy at startup
 *   2. When the proxy's method is called, Spring AOP intercepts it
 *   3. PreAuthorizeAuthorizationManager.check() is called
 *   4. SpEL expression is evaluated against the SecurityContextHolder
 *   5. If FALSE → throw AccessDeniedException (403)
 *   6. If TRUE  → proceed to call the real method
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 AVAILABLE SpEL VARIABLES IN @PreAuthorize:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   authentication      → The Authentication object from SecurityContextHolder
 *   principal           → The UserDetails object (authentication.getPrincipal())
 *   #paramName          → A method parameter (by name)
 *   #paramName.field    → A field of a method parameter
 *   @beanName           → Any Spring bean (e.g., @permService.hasAccess(#id))
 *   returnObject        → (PostAuthorize only) the method's return value
 *   filterObject        → (PreFilter/PostFilter only) each element in a collection
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📊 COMPLETE SpEL EXPRESSION REFERENCE:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   Expression                                  Meaning
 *   ─────────────────────────────────────────   ────────────────────────────────────
 *   hasRole('ADMIN')                            User has ROLE_ADMIN
 *   hasAuthority('READ_USERS')                  User has exact authority
 *   hasAnyRole('ADMIN','MODERATOR')             User has any of these roles
 *   hasAnyAuthority('A','B')                    User has any of these authorities
 *   isAuthenticated()                           User is logged in
 *   isAnonymous()                               User is NOT logged in
 *   isFullyAuthenticated()                      Logged in without "remember-me"
 *   permitAll()                                 Always allow
 *   denyAll()                                   Always deny
 *   #userId == authentication.name             Method param matches logged-in username
 *   authentication.name == 'admin'              Specific username check
 *   principal.username == 'admin'               Same via principal
 *   #doc.owner == authentication.name          Object field matches username
 *   authentication.authorities.size() > 3      Has more than 3 authorities
 *   @permChecker.canRead(authentication,#id)   Custom bean evaluation
 *
 */
@Service
class DocumentSecurityService {

    private final Map<Long, SecureDocument> documents = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    public DocumentSecurityService() {
        // Seed data
        documents.put(1L, new SecureDocument(1L, "Public Guide",    "Welcome!",         "admin",    false));
        documents.put(2L, new SecureDocument(2L, "Alice's Notes",   "My private notes", "alice",    false));
        documents.put(3L, new SecureDocument(3L, "Confidential Report","Top secret",    "admin",    true));
        documents.put(4L, new SecureDocument(4L, "Team Docs",       "Shared content",   "moderator",false));
        idGen.set(5);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 1: Simple role check
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Get all documents.
     *
     * @PreAuthorize("hasRole('ADMIN')")
     * → Only users with ROLE_ADMIN can call this method.
     * → If alice (ROLE_USER) calls this → AccessDeniedException → 403
     * → If admin (ROLE_ADMIN) calls this → method executes normally
     *
     * Try:
     *   curl -u alice:alice123 http://localhost:8080/api/v1/secure/docs  → 403
     *   curl -u admin:admin123 http://localhost:8080/api/v1/secure/docs  → 200
     */
    @PreAuthorize("hasRole('ADMIN')")    // ← ADMIN only — checked BEFORE method runs
    public List<SecureDocument> getAllDocuments() {
        System.out.println("📋 getAllDocuments() — ADMIN access verified");
        return List.copyOf(documents.values());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 2: Multiple roles (any of these)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Get documents for reading.
     *
     * @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
     * → Any authenticated user with USER, ADMIN, or MODERATOR role can read
     * → This is essentially all authenticated users in our system
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
    public List<SecureDocument> getReadableDocuments() {
        System.out.println("📋 getReadableDocuments() — any authenticated user");
        return documents.values().stream()
                .filter(d -> !d.isConfidential())  // Non-confidential only for regular users
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 3: Fine-grained authority check
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Delete a document.
     *
     * @PreAuthorize("hasAuthority('DELETE_USERS')")
     * → Only users with the exact 'DELETE_USERS' authority can call this.
     * → Note: hasAuthority() does NOT add the "ROLE_" prefix (unlike hasRole()).
     * → admin has DELETE_USERS authority → allowed
     * → moderator does NOT have DELETE_USERS → forbidden
     */
    @PreAuthorize("hasAuthority('DELETE_USERS')")
    public boolean deleteDocument(Long id) {
        System.out.println("🗑️  deleteDocument(" + id + ") — DELETE_USERS authority verified");
        return documents.remove(id) != null;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 4: Ownership check — method parameter against authenticated user
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Update a document — but ONLY if you own it (or are an admin).
     *
     * @PreAuthorize("#ownerUsername == authentication.name or hasRole('ADMIN')")
     *
     * This is OWNERSHIP-BASED security:
     *   alice can update her own documents (ownerUsername == "alice" == authentication.name)
     *   alice CANNOT update admin's documents (ownerUsername == "admin" != "alice")
     *   admin can update ANY document (hasRole('ADMIN'))
     *
     * #ownerUsername refers to the METHOD PARAMETER named "ownerUsername"
     * authentication.name is the logged-in user's username
     *
     * Try:
     *   alice calls updateDocument("alice", ...) → allowed (owner match)
     *   alice calls updateDocument("admin", ...) → 403 Forbidden (not owner, not admin)
     *   admin calls updateDocument("alice", ...) → allowed (is admin)
     */
    @PreAuthorize("#ownerUsername == authentication.name or hasRole('ADMIN')")
    public SecureDocument updateDocument(String ownerUsername, Long docId, String newContent) {
        System.out.printf("✏️  updateDocument(owner=%s, id=%d) — ownership verified%n",
                ownerUsername, docId);
        SecureDocument doc = documents.get(docId);
        if (doc == null) throw new NoSuchElementException("Document not found: " + docId);
        doc.setContent(newContent);
        return doc;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 5: Complex compound expression
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Access confidential documents — requires ADMIN role AND full authentication
     * (not via "remember-me" token).
     *
     * @PreAuthorize("hasRole('ADMIN') and isFullyAuthenticated()")
     * → isFullyAuthenticated() = logged in via direct credential, NOT via remember-me
     * → This prevents remember-me cookies from accessing highly sensitive data
     */
    @PreAuthorize("hasRole('ADMIN') and isFullyAuthenticated()")
    public List<SecureDocument> getConfidentialDocuments() {
        System.out.println("🔐 getConfidentialDocuments() — ADMIN + full auth verified");
        return documents.values().stream()
                .filter(SecureDocument::isConfidential)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 6: Checking a field on a METHOD PARAMETER object
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Save a document — user can only save documents they own.
     *
     * @PreAuthorize("#document.ownerUsername == authentication.name or hasRole('ADMIN')")
     *
     * #document refers to the method parameter "document"
     * #document.ownerUsername accesses the field via getter (getOwnerUsername())
     *
     * This prevents users from creating documents owned by someone else.
     */
    @PreAuthorize("#document.ownerUsername == authentication.name or hasRole('ADMIN')")
    public SecureDocument saveDocument(SecureDocument document) {
        System.out.printf("💾 saveDocument(owner=%s) — ownership on param verified%n",
                document.getOwnerUsername());
        long newId = idGen.getAndIncrement();
        SecureDocument saved = new SecureDocument(newId, document.getTitle(),
                document.getContent(), document.getOwnerUsername(), document.isConfidential());
        documents.put(newId, saved);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Pattern 7: Custom bean evaluation in SpEL
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Get a document by ID.
     *
     * @PreAuthorize("@documentAccessChecker.canRead(authentication, #docId)")
     *
     * @documentAccessChecker refers to a Spring bean named "documentAccessChecker"
     * This allows COMPLEX authorization logic in a dedicated bean —
     * keeping @PreAuthorize clean and the logic testable separately.
     *
     * (DocumentAccessChecker bean is defined at the bottom of this file.)
     */
    @PreAuthorize("@documentAccessChecker.canRead(authentication, #docId)")
    public Optional<SecureDocument> getDocumentById(Long docId) {
        System.out.println("📄 getDocumentById(" + docId + ") — custom bean check passed");
        return Optional.ofNullable(documents.get(docId));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Raw (un-secured) access — for INTERNAL use only by DocumentFilterService.
    // Security is enforced by @PostFilter on the CALLER side.
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Returns ALL documents without any @PreAuthorize check.
     *
     * ⚠️ This method is intentionally NOT secured here because DocumentFilterService
     * applies @PostFilter("filterObject.ownerUsername == authentication.name or hasRole('ADMIN')")
     * on the result AFTER this method returns.
     *
     * NEVER call this directly from controller/service code — always go through
     * DocumentFilterService.getMyDocuments() so @PostFilter is applied.
     */
    public List<SecureDocument> getAllDocumentsRaw() {
        System.out.println("📂 getAllDocumentsRaw() — raw access (PostFilter applied by caller)");
        return new java.util.ArrayList<>(documents.values());
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 2: @PostAuthorize — CHECK AFTER METHOD RETURNS
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                    @PostAuthorize  EXPLAINED IN DEPTH                        ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @PostAuthorize evaluates a SpEL expression AFTER the method has run and returned
 * a value. If the expression evaluates to FALSE → AccessDeniedException → 403.
 *
 * The return value is available as: returnObject
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 WHEN TO USE @PostAuthorize:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  When the authorization decision depends on the DATA RETURNED
 *     (you can't know at call time who owns the object — you must fetch it first)
 *
 *  •  Example: A user can view any document they own, but you don't know who owns
 *     document #42 until you fetch it from the database.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ IMPORTANT: The method STILL EXECUTES (database call happens) before
 * the check. If the check fails, the return value is withheld and 403 is returned.
 * Be careful with side effects (writes, external calls) in @PostAuthorize methods.
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @PreAuthorize vs @PostAuthorize:
 *   @PreAuthorize  → "Can this user call this method at all?" (check on input)
 *   @PostAuthorize → "Can this user see THIS specific returned object?" (check on output)
 *
 */
@Service
class SecureReportService {

    private final Map<Long, FinancialReport> reports = new ConcurrentHashMap<>();

    public SecureReportService() {
        reports.put(1L, new FinancialReport(1L, "Q1 Sales",   "Engineering", 150_000, false));
        reports.put(2L, new FinancialReport(2L, "Q2 Expenses","Marketing",   80_000,  false));
        reports.put(3L, new FinancialReport(3L, "Annual P&L", "Finance",     500_000, true)); // restricted
        reports.put(4L, new FinancialReport(4L, "Payroll",    "HR",          300_000, true)); // restricted
    }

    /**
     * Get a report by ID.
     *
     * @PostAuthorize("!returnObject.isPresent() or not returnObject.get().isRestricted()
     *                 or hasRole('ADMIN')")
     *
     * Logic:
     *   - If not found (empty Optional) → allow (no data to leak)
     *   - If the report is NOT restricted → allow (non-sensitive, anyone can see)
     *   - If restricted AND user is ADMIN → allow
     *   - If restricted AND user is NOT ADMIN → 403 Forbidden
     *
     * This prevents non-admin users from seeing restricted financial data
     * even if they know the report ID.
     *
     * Try:
     *   alice calls getReport(1) → allowed (not restricted)
     *   alice calls getReport(3) → 403 (restricted, alice is not admin)
     *   admin calls getReport(3) → allowed (admin can see all)
     */
    @PostAuthorize("!returnObject.isPresent() or " +
                   "not returnObject.get().isRestricted() or " +
                   "hasRole('ADMIN')")
    public Optional<FinancialReport> getReport(Long id) {
        System.out.println("📊 getReport(" + id + ") — fetching (PostAuthorize checks AFTER return)");
        return Optional.ofNullable(reports.get(id));
    }

    /**
     * Get all reports — only the user's department (or all if ADMIN).
     *
     * @PostAuthorize example where we check a String return value directly.
     *
     * Note: A simpler approach for list filtering is @PostFilter (see below).
     */
    @PostAuthorize("returnObject != null")   // ← Trivial example: just ensure non-null return
    public List<FinancialReport> getPublicReports() {
        return reports.values().stream()
                .filter(r -> !r.isRestricted())
                .toList();
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 3: @PreFilter and @PostFilter — FILTER COLLECTIONS
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║             @PreFilter and @PostFilter  EXPLAINED IN DEPTH                   ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @PreFilter — Filter input collection BEFORE method runs:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @PreFilter("filterObject.ownerUsername == authentication.name")
 *
 * For each element in the INPUT collection (filterObject = each element):
 *   → Keep the element if expression is TRUE
 *   → Remove the element if FALSE
 *
 * Use case: Batch operations where a user can only process their OWN items.
 *   A user submits a list of document IDs to delete.
 *   @PreFilter removes any documents they don't own BEFORE the method sees them.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @PostFilter — Filter returned collection AFTER method returns:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @PostFilter("filterObject.ownerUsername == authentication.name or hasRole('ADMIN')")
 *
 * For each element in the RETURN collection (filterObject = each element):
 *   → Keep the element if expression is TRUE
 *   → Remove the element if FALSE
 *
 * Use case: Querying all documents → Spring filters out documents not owned by the caller.
 *   User requests "all my documents."
 *   Method fetches ALL documents from DB, @PostFilter removes the ones they don't own.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ PERFORMANCE WARNING:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @PostFilter fetches ALL data first, THEN filters in memory. For large datasets,
 * this is INEFFICIENT — you should filter in the database query instead.
 * Use @PostFilter ONLY for small datasets or when DB-level filtering is not possible.
 *
 */
@Service
class DocumentFilterService {

    private final DocumentSecurityService documentService;

    public DocumentFilterService(DocumentSecurityService documentService) {
        this.documentService = documentService;
    }

    /**
     * Get documents — Spring automatically removes documents not owned by the caller
     * (unless the caller is an admin).
     *
     * @PostFilter("filterObject.ownerUsername == authentication.name or hasRole('ADMIN')")
     *
     * If alice (ROLE_USER) calls this:
     *   Method returns: [doc1(admin), doc2(alice), doc3(admin), doc4(moderator)]
     *   @PostFilter keeps only: [doc2(alice)] (where ownerUsername == "alice")
     *
     * If admin calls this:
     *   @PostFilter keeps: ALL (hasRole('ADMIN') = true for every element)
     */
    @PostFilter("filterObject.ownerUsername == authentication.name or hasRole('ADMIN')")
    public List<SecureDocument> getMyDocuments() {
        System.out.println("📋 getMyDocuments() — @PostFilter will trim results by ownership");
        return documentService.getAllDocumentsRaw();   // Returns all, @PostFilter trims
    }

    /**
     * Bulk delete documents — @PreFilter removes documents not owned by caller.
     *
     * @PreFilter("filterObject.ownerUsername == authentication.name or hasRole('ADMIN')")
     *
     * If alice submits [doc1(admin), doc2(alice)]:
     *   @PreFilter REMOVES doc1(admin) from the list (alice doesn't own it)
     *   Method receives ONLY: [doc2(alice)]
     *   alice cannot accidentally/maliciously delete admin's documents
     *
     * filterTarget specifies which parameter to filter (required if more than one collection)
     */
    @PreFilter(value = "filterObject.ownerUsername == authentication.name or hasRole('ADMIN')",
               filterTarget = "docsToDelete")
    public int bulkDeleteDocuments(List<SecureDocument> docsToDelete) {
        System.out.println("🗑️  bulkDeleteDocuments() — @PreFilter trimmed list, deleting "
                + docsToDelete.size() + " docs");
        return docsToDelete.size();  // In real code: delete each from repository
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 4: CUSTOM AUTHORIZATION BEAN
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║           CUSTOM AUTHORIZATION BEAN — @PreAuthorize("@beanName...")         ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 WHY USE A CUSTOM AUTHORIZATION BEAN?
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * SpEL expressions in @PreAuthorize can become complex and hard to read:
 *
 *   @PreAuthorize("hasRole('ADMIN') or " +
 *                 "(hasRole('MODERATOR') and #id != null and #id > 0) or " +
 *                 "(authentication.name == @userRepo.findOwner(#id))")
 *
 * A better approach: move the logic to a dedicated @Component bean:
 *
 *   @PreAuthorize("@documentAccessChecker.canRead(authentication, #docId)")
 *
 * Benefits:
 *   ✓  Logic is testable independently (unit test the bean directly)
 *   ✓  Readable annotation (self-documenting method name)
 *   ✓  Reusable across multiple @PreAuthorize annotations
 *   ✓  Can inject repositories, services, caches
 *
 */
@org.springframework.stereotype.Component("documentAccessChecker")
class DocumentAccessChecker {

    /**
     * Complex access check extracted into a dedicated, testable bean.
     * Called via: @PreAuthorize("@documentAccessChecker.canRead(authentication, #docId)")
     *
     * @param auth    the current user's Authentication object
     * @param docId   the ID of the document being accessed
     * @return true if the user is allowed to read this document
     */
    public boolean canRead(org.springframework.security.core.Authentication auth, Long docId) {
        if (auth == null || !auth.isAuthenticated()) {
            System.out.println("🔒 canRead(" + docId + ") → DENIED (not authenticated)");
            return false;
        }

        // Admins can read anything
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            System.out.println("✅ canRead(" + docId + ") → ALLOWED (admin)");
            return true;
        }

        // Users with READ_USERS authority can read non-confidential documents
        boolean hasReadAuthority = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("READ_USERS"));
        if (hasReadAuthority && docId != null && docId > 0 && docId <= 4) {
            System.out.println("✅ canRead(" + docId + ") → ALLOWED (READ_USERS authority, non-sensitive)");
            return true;
        }

        System.out.println("🔒 canRead(" + docId + ") → DENIED (insufficient permissions)");
        return false;
    }

    /**
     * Check if the user can modify a specific document.
     */
    public boolean canWrite(org.springframework.security.core.Authentication auth, Long docId) {
        if (auth == null || !auth.isAuthenticated()) return false;
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean hasWriteAuth = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("WRITE_USERS"));
        return isAdmin || hasWriteAuth;
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 5: SECURE CONTROLLER wiring all the service examples together
// ══════════════════════════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/v1/secure/docs")
class SecureDocumentController {

    private final DocumentSecurityService  docService;
    private final SecureReportService      reportService;
    private final DocumentFilterService    filterService;

    public SecureDocumentController(DocumentSecurityService docService,
                                    SecureReportService reportService,
                                    DocumentFilterService filterService) {
        this.docService    = docService;
        this.reportService = reportService;
        this.filterService = filterService;
    }

    /** GET /api/v1/secure/docs — @PreAuthorize("hasRole('ADMIN')") in service */
    @GetMapping
    public List<SecureDocument> getAll() {
        return docService.getAllDocuments();   // @PreAuthorize enforced in service
    }

    /** GET /api/v1/secure/docs/mine — @PostFilter applied to results */
    @GetMapping("/mine")
    public List<SecureDocument> getMine() {
        return filterService.getMyDocuments();  // @PostFilter removes non-owned docs
    }

    /** GET /api/v1/secure/docs/{id} — custom bean authorization check */
    @GetMapping("/{id}")
    public ResponseEntity<SecureDocument> getById(@PathVariable Long id) {
        return docService.getDocumentById(id)   // @PreAuthorize("@documentAccessChecker.canRead")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/v1/secure/reports/{id} — @PostAuthorize on return value */
    @GetMapping("/reports/{id}")
    public ResponseEntity<FinancialReport> getReport(@PathVariable Long id) {
        return reportService.getReport(id)   // @PostAuthorize checks restricted flag
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  HELPER: raw access for DocumentFilterService (bypasses security for internal use)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * Extension of DocumentSecurityService that exposes a raw (unsecured) access method
 * for INTERNAL use by DocumentFilterService.
 * The @PostFilter in DocumentFilterService then applies the security.
 */
interface RawDocumentAccess {
    List<SecureDocument> getAllDocumentsRaw();
}

// Patch DocumentSecurityService to implement the raw interface (conceptually)
// In real code, you'd have a repository that this delegates to.
// Here we add the method directly to satisfy DocumentFilterService.
// (Defined on the class above at the top of this file — this comment explains the pattern)

// ══════════════════════════════════════════════════════════════════════════════════════
//  SUMMARY: METHOD SECURITY PICTURE
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║         METHOD SECURITY — THE COMPLETE PICTURE                               ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 *  ANNOTATION       WHEN CHECKED        USE CASE
 *  ─────────────    ─────────────────   ─────────────────────────────────────────────
 *  @PreAuthorize    Before method runs  Most common — check input + caller identity
 *  @PostAuthorize   After method runs   Check if caller can SEE the returned object
 *  @PreFilter       Before method runs  Remove unauthorized items from INPUT collection
 *  @PostFilter      After method runs   Remove unauthorized items from RETURNED collection
 *  @Secured         Before method runs  Simpler role-only check (no SpEL)
 *  @RolesAllowed    Before method runs  Same as @Secured but JSR-250 portable
 *
 *  PROCESSING ORDER:
 *  @PreFilter → @PreAuthorize → [method executes] → @PostAuthorize → @PostFilter
 *
 *  ⚠️ REMEMBER:
 *  • @PreAuthorize is powered by Spring AOP → only works on PUBLIC methods
 *  • Called from OUTSIDE the bean (proxy intercepts) → works
 *  • Called from INSIDE the same bean (this.method()) → BYPASSES proxy → NOT secured!
 *  • Solution: Use @Autowired self-injection or restructure your beans
 *
 */
class Example02MethodSecurity {
    // Intentionally empty — documentation class
}

