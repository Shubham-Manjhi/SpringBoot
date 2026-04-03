package com.learning.springboot.chapter02;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.ApplicationScope;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.annotation.SessionScope;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║              EXAMPLE 03: BEAN SCOPE ANNOTATIONS IN ACTION                            ║
 * ║              @Scope · @RequestScope · @SessionScope · @ApplicationScope              ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example03BeanScopes.java
 * Purpose:     Understand how Spring controls the lifecycle and number of bean instances
 *              using scope annotations. Learn when to use each scope and the pitfalls.
 * Difficulty:  ⭐⭐⭐ Intermediate
 * Time:        30 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * SCENARIO: A multi-user e-commerce session management system.
 *   We track: application-wide counters, per-session carts, and per-request contexts.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 1: SINGLETON SCOPE — ONE INSTANCE PER APPLICATION CONTEXT (DEFAULT)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║              SINGLETON SCOPE  (DEFAULT) — EXPLAINED IN DEPTH                 ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Singleton = ONE instance per ApplicationContext.
 * Every injection point that requests this bean type gets the SAME object reference.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 HOW TO DECLARE:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   @Component                          ← default is singleton — no @Scope needed
 *   @Component @Scope("singleton")      ← explicit singleton (same result)
 *   @Component @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)  ← using constant
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ✅ WHEN TO USE SINGLETON (most of the time):
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  Services, Repositories, Controllers, Configuration beans
 *  •  Any STATELESS component (doesn't hold request-specific data)
 *  •  Heavy components (database connections, thread pools) — create once, reuse always
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ SINGLETON SCOPE IS NOT THE SAME AS THE SINGLETON DESIGN PATTERN:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   Singleton Design Pattern → ONE instance per JVM (enforced by private constructor)
 *   Spring Singleton Scope   → ONE instance per ApplicationContext
 *
 *   You can have MULTIPLE ApplicationContexts in one JVM.
 *   Each would have its OWN "singleton" instance of the same bean type.
 *
 */
@Component
// @Scope("singleton") ← implicit, no annotation needed — shown here for clarity
class ApplicationStatisticsService {

    /*
     * SINGLETON = SHARED STATE across the entire application.
     * This counter is incremented by every request, from every user.
     *
     * ✅ This is SAFE because it is application-wide state we intentionally share.
     *
     * ⚠️ DANGER ZONE: Storing per-user or per-request data in a singleton is a
     * classic concurrency bug! See the prototype scope for the solution.
     */
    private long totalRequests   = 0;
    private long totalPageViews  = 0;
    private final Instant startTime = Instant.now();

    public synchronized void incrementRequests() {
        totalRequests++;
    }

    public synchronized void incrementPageViews() {
        totalPageViews++;
    }

    public long getTotalRequests()  { return totalRequests; }
    public long getTotalPageViews() { return totalPageViews; }
    public Instant getStartTime()   { return startTime; }

    /*
     * 🔑 This method can be called from ANY bean, anywhere.
     * All of them interact with the SAME instance of ApplicationStatisticsService.
     */
    public void printStats() {
        System.out.printf(
                "📊 [Singleton] Stats — requests=%d, pageViews=%d, uptime=%s%n",
                totalRequests, totalPageViews, startTime);
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 2: PROTOTYPE SCOPE — NEW INSTANCE ON EVERY INJECTION
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                    PROTOTYPE SCOPE — EXPLAINED IN DEPTH                      ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Prototype = A NEW instance is created each time this bean is requested.
 *   •  context.getBean(MyBean.class)        → new instance
 *   •  @Autowired MyBean field in BeanA     → new instance for BeanA
 *   •  @Autowired MyBean field in BeanB     → DIFFERENT new instance for BeanB
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ IMPORTANT: Spring does NOT manage prototype bean destruction.
 *    @PreDestroy is NOT called for prototype beans (Spring doesn't track them).
 *    You are responsible for cleanup.
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 WHEN TO USE PROTOTYPE:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  STATEFUL objects that hold unique data per use (e.g., a Command object)
 *  •  Objects that are NOT thread-safe and must not be shared
 *  •  Worker/task objects that carry different state per task execution
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Component
@Scope("prototype")   // ← NEW instance every time this bean is injected/requested
class AuditLogEntry {

    private final String entryId;     // Unique per instance
    private final Instant createdAt;  // Unique per instance
    private String action;
    private String userId;
    private String details;

    public AuditLogEntry() {
        this.entryId   = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        System.out.println("🆕 [Prototype] NEW AuditLogEntry created: " + entryId);
    }

    public AuditLogEntry withAction(String action)   { this.action  = action;  return this; }
    public AuditLogEntry withUserId(String userId)   { this.userId  = userId;  return this; }
    public AuditLogEntry withDetails(String details) { this.details = details; return this; }

    @Override
    public String toString() {
        return String.format("[AuditLog] id=%s | action=%s | user=%s | details=%s | at=%s",
                entryId, action, userId, details, createdAt);
    }

    public String getEntryId()  { return entryId; }
    public Instant getCreatedAt() { return createdAt; }
}

// ──────────────────────────────────────────────────────────────────────────────────────
//  INJECTING PROTOTYPE INTO SINGLETON: The Classic Pitfall & Its Solutions
// ──────────────────────────────────────────────────────────────────────────────────────

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║         THE SINGLETON + PROTOTYPE PROBLEM — AND HOW TO SOLVE IT             ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ❌ THE PROBLEM:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  @Service  // SINGLETON
 *  class AuditService {
 *
 *      @Autowired
 *      private AuditLogEntry logEntry;  // PROTOTYPE — but only injected ONCE!
 *
 *      // The same logEntry is used for every call to logAction()
 *      // Even though AuditLogEntry is @Scope("prototype"),
 *      // it is injected only ONCE when the singleton is created.
 *      // After that, the same instance is reused on every call. ❌
 *
 *      public void logAction(String userId, String action) {
 *          logEntry.withUserId(userId).withAction(action);  // mutates the same object!
 *      }
 *  }
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ✅ SOLUTION 1: ObjectProvider (Spring's recommended approach, Spring 4.3+)
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  @Service
 *  class AuditService {
 *      private final ObjectProvider<AuditLogEntry> entryProvider;
 *
 *      AuditService(ObjectProvider<AuditLogEntry> entryProvider) {
 *          this.entryProvider = entryProvider;
 *      }
 *
 *      public void logAction(String userId, String action) {
 *          AuditLogEntry entry = entryProvider.getObject();  // NEW instance each call ✅
 *          entry.withUserId(userId).withAction(action);
 *      }
 *  }
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ✅ SOLUTION 2: @Lookup method injection (Spring 4+)
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  @Service
 *  abstract class AuditService {
 *
 *      @Lookup
 *      protected abstract AuditLogEntry createEntry();  // Spring overrides this method
 *                                                        // to return a new prototype instance
 *      public void logAction(String userId, String action) {
 *          AuditLogEntry entry = createEntry();  // NEW instance each call ✅
 *          entry.withUserId(userId).withAction(action);
 *      }
 *  }
 *
 */
@Service("ch02AuditService")   // explicit name — avoids conflict with chapter06.AuditService
class AuditService {

    /*
     * ObjectProvider<T> is a lazy/optional bean provider.
     * Calling .getObject() creates a NEW prototype instance each time.
     * This is the CORRECT way to use prototype beans inside a singleton.
     */
    private final ObjectProvider<AuditLogEntry> auditLogEntryProvider;

    public AuditService(ObjectProvider<AuditLogEntry> auditLogEntryProvider) {
        this.auditLogEntryProvider = auditLogEntryProvider;
        System.out.println("✅ AuditService (singleton) created with ObjectProvider");
    }

    /**
     * Logs a user action. Each call creates a FRESH AuditLogEntry.
     *
     * @param userId  the ID of the user performing the action
     * @param action  description of the action
     * @param details additional context
     */
    public void logAction(String userId, String action, String details) {
        // .getObject() asks Spring for a NEW prototype instance each time
        AuditLogEntry entry = auditLogEntryProvider.getObject();
        entry.withUserId(userId)
             .withAction(action)
             .withDetails(details);
        System.out.println("📝 [AuditService] " + entry);
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 3: REQUEST SCOPE — ONE INSTANCE PER HTTP REQUEST
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                    @RequestScope — EXPLAINED IN DEPTH                        ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @RequestScope creates a NEW bean instance for EACH HTTP request.
 * When the request ends, the bean is destroyed (@PreDestroy IS called here!).
 *
 * Equivalent to: @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 WHEN TO USE:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  Holding data specific to the CURRENT HTTP request
 *     (e.g., request ID, authenticated user, timing data)
 *  •  Accumulating information during request processing across multiple beans
 *  •  Request-level caching (avoid re-querying the DB in the same request)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 HOW THE SCOPED PROXY WORKS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * When a @RequestScope bean is injected into a SINGLETON:
 *
 *   @Service  // singleton
 *   class OrderService {
 *       @Autowired RequestContext ctx;  // request-scoped
 *   }
 *
 * Spring CANNOT inject the actual request-scoped bean into a singleton at
 * creation time (because no HTTP request exists yet at startup time).
 *
 * Instead, Spring injects a SCOPED PROXY — a stand-in object.
 * When a real HTTP request arrives and you call ctx.getRequestId(),
 * the proxy DELEGATES to the REAL request-scoped bean for THAT request.
 *
 *   Singleton → holds Proxy
 *   Request 1 arrives → Proxy → RealBean(for request 1)
 *   Request 2 arrives → Proxy → RealBean(for request 2)  (a different instance!)
 *
 * This is the magic of ScopedProxyMode.TARGET_CLASS !
 *
 */
@Component
@RequestScope   // ← @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
class RequestContext {

    private final String requestId;
    private final Instant requestStart;
    private String authenticatedUserId;
    private String requestPath;

    public RequestContext() {
        this.requestId    = UUID.randomUUID().toString().substring(0, 8);
        this.requestStart = Instant.now();
        System.out.println("🌐 [RequestScope] NEW RequestContext created: " + requestId);
    }

    public String  getRequestId()          { return requestId; }
    public Instant getRequestStart()       { return requestStart; }
    public String  getAuthenticatedUserId() { return authenticatedUserId; }
    public String  getRequestPath()        { return requestPath; }

    public void setAuthenticatedUserId(String userId) { this.authenticatedUserId = userId; }
    public void setRequestPath(String path)           { this.requestPath = path; }

    @Override
    public String toString() {
        return String.format("[Request %s] user=%s path=%s", requestId, authenticatedUserId, requestPath);
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 4: SESSION SCOPE — ONE INSTANCE PER HTTP SESSION
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                    @SessionScope — EXPLAINED IN DEPTH                        ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @SessionScope creates a NEW bean instance for EACH HTTP session.
 * The instance lives until the session expires or is invalidated.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 WHEN TO USE:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  Shopping CART data (persists across multiple requests in the same session)
 *  •  User PREFERENCES stored for the session
 *  •  Multi-step WIZARD / form state
 *  •  Any data that should survive across multiple requests but belong to one user
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ CAUTION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  Session beans are stored in HTTP session (in memory or distributed cache)
 *  •  Large session objects waste memory — keep them lean
 *  •  Serializable is required if sessions are replicated across cluster nodes
 *  •  Always implement java.io.Serializable for session-scoped beans in production
 *
 */
@Component
@SessionScope   // ← @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
class ShoppingCart {

    private final String cartId;
    private final List<CartItem> items = new ArrayList<>();
    private String userId;

    public ShoppingCart() {
        this.cartId = "CART-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        System.out.println("🛒 [SessionScope] NEW ShoppingCart created: " + cartId);
    }

    public void setUserId(String userId) { this.userId = userId; }
    public String getUserId()            { return userId; }
    public String getCartId()            { return cartId; }

    public void addItem(String productName, int quantity, double price) {
        items.add(new CartItem(productName, quantity, price));
        System.out.printf("➕ [Cart %s] Added %dx %s @ %.2f%n", cartId, quantity, productName, price);
    }

    public void removeItem(String productName) {
        items.removeIf(item -> item.productName().equals(productName));
        System.out.printf("➖ [Cart %s] Removed %s%n", cartId, productName);
    }

    public double getTotal() {
        return items.stream().mapToDouble(CartItem::subtotal).sum();
    }

    public List<CartItem> getItems() { return List.copyOf(items); }

    public void printSummary() {
        System.out.printf("🛒 Cart [%s] for user [%s]:%n", cartId, userId);
        items.forEach(item ->
                System.out.printf("   • %-25s x%d = %.2f%n",
                        item.productName(), item.quantity(), item.subtotal()));
        System.out.printf("   ──────────────────────────────%n");
        System.out.printf("   Total: %.2f%n", getTotal());
    }

    /** Immutable line item — safe to store in a session bean. */
    public record CartItem(String productName, int quantity, double price) {
        public double subtotal() { return price * quantity; }
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 5: APPLICATION SCOPE — ONE INSTANCE PER SERVLET CONTEXT
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                   @ApplicationScope — EXPLAINED IN DEPTH                     ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @ApplicationScope creates ONE bean instance per SERVLET CONTEXT (per web application).
 * In a standard Spring Boot app this is effectively the same as singleton scope.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🤔 SINGLETON vs APPLICATION SCOPE:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   Singleton       → One per ApplicationContext (Spring's IoC container)
 *   Application     → One per ServletContext (the web application)
 *
 *   In a standard Spring Boot app these are equivalent.
 *   The difference matters in WAR deployments where:
 *   •  Multiple Spring ApplicationContexts can share ONE ServletContext
 *   •  application-scoped beans are shared across all of them
 *   •  singleton-scoped beans are NOT shared (one per ApplicationContext)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 WHEN TO USE:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  Application-wide shared caches (e.g., category list, country codes)
 *  •  Feature flags that are shared across the whole web application
 *  •  Statistics counters shared across the entire application
 *
 */
@Component
@ApplicationScope   // ← @Scope(value = "application", proxyMode = ScopedProxyMode.TARGET_CLASS)
class GlobalProductCatalogCache {

    private final List<String> cachedCategories;
    private Instant lastRefreshed;

    public GlobalProductCatalogCache() {
        this.cachedCategories = new ArrayList<>(
                List.of("Electronics", "Books", "Clothing", "Accessories", "Sports"));
        this.lastRefreshed = Instant.now();
        System.out.println("📚 [ApplicationScope] GlobalProductCatalogCache initialized");
    }

    public List<String> getCategories() { return List.copyOf(cachedCategories); }
    public Instant getLastRefreshed()   { return lastRefreshed; }

    public void refresh(List<String> newCategories) {
        cachedCategories.clear();
        cachedCategories.addAll(newCategories);
        this.lastRefreshed = Instant.now();
        System.out.println("🔄 [ApplicationScope] Cache refreshed at " + lastRefreshed);
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 6: CUSTOM SCOPE VIA @Scope
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║              @Scope WITH SCOPED PROXY — EXPLAINED IN DEPTH                   ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 @Scope annotation — full form:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   @Scope(value = "scopeName", proxyMode = ScopedProxyMode.XXX)
 *
 *   value      = the scope name: "singleton", "prototype", "request",
 *                "session", "application", or custom scope name
 *
 *   proxyMode  = how Spring creates the proxy when injecting into different scopes:
 *     ScopedProxyMode.NO              → No proxy (default for singleton/prototype)
 *     ScopedProxyMode.INTERFACES      → JDK dynamic proxy (bean must implement interface)
 *     ScopedProxyMode.TARGET_CLASS    → CGLIB proxy (works with/without interface)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 proxyMode = TARGET_CLASS is REQUIRED when injecting request/session beans
 *    into singleton beans. Without it, Spring would try to inject the actual
 *    request/session bean at startup time — but no request/session exists yet!
 *
 *    @RequestScoped  ≡ @Scope(value="request",  proxyMode=TARGET_CLASS)
 *    @SessionScoped  ≡ @Scope(value="session",  proxyMode=TARGET_CLASS)
 *    @ApplicationScoped ≡ @Scope(value="application", proxyMode=TARGET_CLASS)
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Component
@Scope(value = "prototype", proxyMode = ScopedProxyMode.NO)  // Equivalent to @Scope("prototype")
class TaskContext {

    private final String taskId;
    private String taskName;
    private final Instant createdAt;

    public TaskContext() {
        this.taskId    = "TASK-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        this.createdAt = Instant.now();
        System.out.println("⚙️  [Prototype via @Scope] NEW TaskContext: " + taskId);
    }

    public String  getTaskId()   { return taskId; }
    public Instant getCreatedAt() { return createdAt; }
    public String  getTaskName() { return taskName; }
    public void    setTaskName(String name) { this.taskName = name; }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 7: PUTTING IT ALL TOGETHER — SCOPE SUMMARY
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                BEAN SCOPES — THE COMPLETE PICTURE                            ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🗺️ VISUAL SUMMARY:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  ┌────────────────────────────────────────────────────────────────────────────┐
 *  │                         APPLICATION LIFETIME                               │
 *  │                                                                            │
 *  │  ┌─────────────────────────────────────────────────────────────────────┐  │
 *  │  │  ApplicationContext (= entire app runtime)                          │  │
 *  │  │                                                                     │  │
 *  │  │  SINGLETON BEANS: created once, live forever                        │  │
 *  │  │    ● ProductService   ● AuditService   ● OrderService               │  │
 *  │  │    ● ApplicationStatisticsService   ← lives here too                │  │
 *  │  │                                                                     │  │
 *  │  │  APPLICATION BEANS (web):    ● GlobalProductCatalogCache            │  │
 *  │  │                                                                     │  │
 *  │  │  ┌─────────────────────────────────────────────────────────────┐   │  │
 *  │  │  │  HTTP Session (one per logged-in user)                       │   │  │
 *  │  │  │                                                              │   │  │
 *  │  │  │  SESSION BEANS:                                              │   │  │
 *  │  │  │    ● ShoppingCart (unique per user session)                  │   │  │
 *  │  │  │                                                              │   │  │
 *  │  │  │  ┌──────────────────────────────────────────────────────┐   │   │  │
 *  │  │  │  │  HTTP Request (one per incoming request)              │   │   │  │
 *  │  │  │  │                                                        │   │   │  │
 *  │  │  │  │  REQUEST BEANS:                                        │   │   │  │
 *  │  │  │  │    ● RequestContext (unique per request)               │   │   │  │
 *  │  │  │  └──────────────────────────────────────────────────────┘   │   │  │
 *  │  │  └─────────────────────────────────────────────────────────────┘   │  │
 *  │  └─────────────────────────────────────────────────────────────────────┘  │
 *  │                                                                            │
 *  │  PROTOTYPE BEANS: created on demand; Spring does NOT destroy them          │
 *  │    ● AuditLogEntry (new per logAction call)                                │
 *  │    ● TaskContext (new per task)                                             │
 *  └────────────────────────────────────────────────────────────────────────────┘
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 KEY RULES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  RULE 1: Inject WIDER scope into NARROWER scope → requires a SCOPED PROXY
 *    (e.g., singleton bean holding a reference to a request-scoped bean)
 *
 *  RULE 2: Inject NARROWER scope into WIDER scope → always WRONG
 *    (e.g., request-scoped bean holding a singleton — the singleton outlives the request)
 *    But in practice, you can inject a singleton into a request-scoped bean directly.
 *
 *  RULE 3: Prototype into Singleton → use ObjectProvider or @Lookup
 *    (standard injection gives you only the first prototype instance)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ COMMON PITFALLS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. Storing mutable state in a SINGLETON bean
 *     → Race condition across concurrent requests → use ThreadLocal or narrower scope
 *
 *  2. Not implementing Serializable for SESSION-scoped beans in clustered deployments
 *     → Session replication fails → sticky sessions required as workaround (not scalable)
 *
 *  3. Forgetting proxyMode when injecting request/session beans into a singleton
 *     → Spring throws a BeanCreationException at startup
 *
 *  4. Expecting @PreDestroy on PROTOTYPE beans
 *     → Spring does NOT call @PreDestroy for prototype beans — you own the cleanup
 *
 */
class Example03BeanScopes {
    // Intentionally empty — documentation class
}





