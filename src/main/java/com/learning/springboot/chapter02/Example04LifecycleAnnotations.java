package com.learning.springboot.chapter02;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║            EXAMPLE 04: BEAN LIFECYCLE ANNOTATIONS IN ACTION                          ║
 * ║            @PostConstruct  ·  @PreDestroy  ·  @Lazy                                  ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example04LifecycleAnnotations.java
 * Purpose:     Master the complete Spring bean lifecycle — from creation through
 *              destruction — and learn to defer initialization with @Lazy.
 * Difficulty:  ⭐⭐ Beginner–Intermediate
 * Time:        25 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * WHAT YOU WILL LEARN:
 *   @PostConstruct → Init method after all dependencies are injected
 *   @PreDestroy    → Cleanup method just before the bean is destroyed
 *   @Lazy          → "Don't create this bean at startup; wait until it's needed"
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 1: @PostConstruct — INITIALIZATION AFTER DEPENDENCY INJECTION
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                       @PostConstruct  EXPLAINED                              ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @PostConstruct marks a method to be executed AFTER:
 *   1. The bean instance is created (constructor has run)
 *   2. ALL dependency injection is complete (@Autowired, @Value fields are set)
 *
 * Spring calls this method ONCE, before the bean is used by anything else.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🤔 WHY NOT PUT INIT LOGIC IN THE CONSTRUCTOR?
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * PROBLEM: When the constructor runs, @Autowired / @Value fields are NOT YET set!
 *
 *   @Component
 *   class BadExample {
 *       @Value("${db.url}")
 *       private String dbUrl;  ← NOT yet injected during constructor!
 *
 *       public BadExample() {
 *           connect(dbUrl);   ← ❌ dbUrl is null here!
 *       }
 *   }
 *
 * SOLUTION: Use @PostConstruct — by the time it runs, all @Value/@Autowired are set.
 *
 *   @Component
 *   class GoodExample {
 *       @Value("${db.url}")
 *       private String dbUrl;  ← Injected before @PostConstruct runs
 *
 *       @PostConstruct
 *       public void init() {
 *           connect(dbUrl);   ← ✅ dbUrl is ready!
 *       }
 *   }
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔧 RULES FOR @PostConstruct METHODS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  ✅  Must be non-static
 *  ✅  Must return void
 *  ✅  Must have no parameters
 *  ✅  Can have any access modifier (public, protected, private, package-private)
 *  ✅  Only ONE @PostConstruct method per class (technically you can have multiple
 *      but only the most specific one is called — use one for clarity)
 *  ✅  Can throw checked exceptions (Spring wraps them in BeanCreationException)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 COMMON USE CASES FOR @PostConstruct:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. Opening resource connections (database pools, file handles, sockets)
 *  2. Pre-loading / warming up caches
 *  3. Validating configuration (fail fast on bad config)
 *  4. Starting background threads or schedulers
 *  5. Registering with external services (e.g., message queues)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📝 EXAMPLE 1: DatabaseConnectionPool — Opens connection pool after config injection
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Component
class DatabaseConnectionPool {

    // Injected BEFORE @PostConstruct runs — safe to use in init()
    @Value("${spring.datasource.url:jdbc:h2:mem:testdb}")
    private String dbUrl;

    @Value("${spring.datasource.username:sa}")
    private String username;

    @Value("${db.pool.min-size:2}")
    private int minPoolSize;

    @Value("${db.pool.max-size:10}")
    private int maxPoolSize;

    private final java.util.List<String> connectionPool = new java.util.ArrayList<>();
    private boolean initialized = false;

    /**
     * Constructor runs FIRST.
     * At this point, @Value fields are NOT yet injected.
     * So we don't do any initialization here — just create the object.
     */
    public DatabaseConnectionPool() {
        System.out.println("1️⃣  [DatabaseConnectionPool] Constructor called — "
                + "fields NOT yet injected (dbUrl=" + dbUrl + ")");
        // Don't use dbUrl here — it's null at this point!
    }

    /**
     * @PostConstruct runs AFTER constructor AND after @Value injection.
     *
     * By the time Spring calls this method:
     *   • dbUrl       is "jdbc:h2:mem:testdb" (or from application.yml)
     *   • username    is "sa"
     *   • minPoolSize is 2
     *   • maxPoolSize is 10
     *
     * This is the RIGHT place to initialize resources that depend on configuration.
     */
    @PostConstruct
    public void initializePool() {
        System.out.println("2️⃣  [DatabaseConnectionPool] @PostConstruct called — "
                + "fields now injected (dbUrl=" + dbUrl + ")");

        // Safe to use dbUrl here
        System.out.println("🔌 Initializing connection pool:");
        System.out.println("   URL      = " + dbUrl);
        System.out.println("   Username = " + username);
        System.out.println("   MinSize  = " + minPoolSize);
        System.out.println("   MaxSize  = " + maxPoolSize);

        // Simulate creating minimum connections
        for (int i = 1; i <= minPoolSize; i++) {
            String connId = "conn-" + i + "@" + dbUrl;
            connectionPool.add(connId);
            System.out.println("   ✅ Created connection: " + connId);
        }

        initialized = true;
        System.out.println("✅ Connection pool ready with " + connectionPool.size() + " connections.");
    }

    /**
     * @PreDestroy runs BEFORE the bean is destroyed.
     * We close all connections to release database resources.
     * (Detailed explanation in Section 2.)
     */
    @PreDestroy
    public void closePool() {
        System.out.println("3️⃣  [DatabaseConnectionPool] @PreDestroy called — closing pool");
        connectionPool.forEach(conn ->
                System.out.println("   ❌ Closing connection: " + conn));
        connectionPool.clear();
        initialized = false;
        System.out.println("✅ Connection pool closed gracefully.");
    }

    public boolean isInitialized() { return initialized; }
    public int getPoolSize()        { return connectionPool.size(); }
}

// ──────────────────────────────────────────────────────────────────────────────────────
//  EXAMPLE 2: CacheService — Warms up cache on startup
// ──────────────────────────────────────────────────────────────────────────────────────

/**
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📝 EXAMPLE 2: CacheWarmupService — Pre-loads cache after dependencies are injected
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Service
class CacheWarmupService {

    // Injected BEFORE @PostConstruct
    private final DatabaseConnectionPool dbPool;

    @Value("${cache.initial-load:true}")
    private boolean shouldPreload;

    public CacheWarmupService(DatabaseConnectionPool dbPool) {
        this.dbPool = dbPool;
        System.out.println("1️⃣  [CacheWarmupService] Constructor called");
    }

    /**
     * Pre-loads the cache after the database connection pool is ready.
     *
     * Notice: We depend on DatabaseConnectionPool. Spring ensures that
     * DatabaseConnectionPool is FULLY initialized (including its @PostConstruct)
     * BEFORE injecting it into CacheWarmupService.
     *
     * So when our @PostConstruct runs, dbPool is guaranteed to be initialized.
     */
    @PostConstruct
    public void warmUpCache() {
        System.out.println("2️⃣  [CacheWarmupService] @PostConstruct called");

        if (!dbPool.isInitialized()) {
            throw new IllegalStateException("DB pool must be initialized before cache warm-up!");
        }

        if (shouldPreload) {
            System.out.println("🔥 Warming up application cache...");
            System.out.println("   Loading users from DB via pool (size=" + dbPool.getPoolSize() + ")...");
            System.out.println("   Loading product catalog...");
            System.out.println("   Loading configuration flags...");
            System.out.println("✅ Cache warm-up complete.");
        } else {
            System.out.println("⏭️  Cache pre-loading disabled (cache.initial-load=false).");
        }
    }

    @PreDestroy
    public void cleanUpCache() {
        System.out.println("3️⃣  [CacheWarmupService] @PreDestroy called — clearing cache");
        System.out.println("   Cache cleared.");
    }
}

// ──────────────────────────────────────────────────────────────────────────────────────
//  EXAMPLE 3: Configuration Validation — Fail fast on bad configuration
// ──────────────────────────────────────────────────────────────────────────────────────

/**
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📝 EXAMPLE 3: ConfigurationValidator — Validates config values on startup
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * 💡 KEY PRINCIPLE: FAIL FAST
 *
 * It is MUCH better to crash loudly at startup with a clear error message
 * than to silently produce wrong results at 3AM in production.
 *
 * @PostConstruct is the perfect place for startup validation because:
 *   •  All @Value properties are already bound
 *   •  Throwing an exception here aborts the entire startup process
 *   •  The error message is clear and immediate
 */
@Component
class AppConfigurationValidator {

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${app.max-concurrent-requests:100}")
    private int maxConcurrentRequests;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @PostConstruct
    public void validateConfiguration() {
        System.out.println("🔍 [ConfigValidator] Validating application configuration...");

        // Validate port range
        if (serverPort < 1 || serverPort > 65535) {
            throw new IllegalStateException(
                    "Invalid server.port: " + serverPort + ". Must be between 1 and 65535.");
        }

        // Validate concurrency limits
        if (maxConcurrentRequests < 1 || maxConcurrentRequests > 10000) {
            throw new IllegalStateException(
                    "Invalid app.max-concurrent-requests: " + maxConcurrentRequests
                            + ". Must be between 1 and 10000.");
        }

        // In production, JWT secret must be set (empty = insecure)
        // We use a warning here instead of throwing to not break the demo
        if (jwtSecret.isBlank()) {
            System.out.println("⚠️  [ConfigValidator] WARNING: jwt.secret is not set! "
                    + "This is insecure in production.");
        }

        System.out.printf("✅ [ConfigValidator] Configuration valid:%n"
                + "   port=%d, maxRequests=%d%n", serverPort, maxConcurrentRequests);
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 2: @PreDestroy — CLEANUP BEFORE BEAN DESTRUCTION
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                        @PreDestroy  EXPLAINED                                ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @PreDestroy marks a method to be executed JUST BEFORE the bean is destroyed.
 * In a Spring Boot application, this happens when:
 *   •  The application context is shut down (application exits)
 *   •  Ctrl+C is pressed / SIGTERM is received (graceful shutdown)
 *   •  context.close() / context.registerShutdownHook() is called
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 COMMON USE CASES FOR @PreDestroy:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. Closing database connection pools
 *  2. Shutting down thread pools / ExecutorService
 *  3. Flushing and closing file writers/buffers
 *  4. Deregistering from external services (e.g., Eureka, consul)
 *  5. Flushing cached metrics / telemetry data
 *  6. Sending a "shutdown" notification to monitoring systems
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ IMPORTANT: @PreDestroy is NOT called for @Scope("prototype") beans.
 *    Spring does not track prototype bean lifecycle after creation.
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔧 RULES FOR @PreDestroy METHODS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  ✅  Must be non-static
 *  ✅  Must return void
 *  ✅  Must have no parameters
 *  ✅  Should NOT throw exceptions (exceptions during destroy are logged, not propagated)
 *  ✅  Best practice: wrap body in try-catch to ensure full cleanup even if one step fails
 *
 */
@Service
class BackgroundTaskExecutor {

    private final ExecutorService executorService;
    private final String poolName;

    public BackgroundTaskExecutor() {
        this.poolName       = "bg-pool";
        this.executorService = Executors.newFixedThreadPool(4,
                r -> new Thread(r, poolName + "-" + System.currentTimeMillis()));
        System.out.println("1️⃣  [BackgroundTaskExecutor] Constructor — thread pool created");
    }

    @PostConstruct
    public void onStartup() {
        System.out.println("2️⃣  [BackgroundTaskExecutor] @PostConstruct — pool is ready");
        System.out.println("   Thread pool '" + poolName + "' accepting tasks.");
    }

    /**
     * Submit a background task.
     */
    public void submitTask(Runnable task) {
        if (executorService.isShutdown()) {
            throw new IllegalStateException("Executor service is shut down — cannot accept new tasks.");
        }
        executorService.submit(task);
    }

    /**
     * @PreDestroy — graceful shutdown of the thread pool.
     *
     * BEST PRACTICE: Use a two-phase shutdown:
     *   Phase 1: Stop accepting new tasks (shutdown)
     *   Phase 2: Wait for running tasks to complete (awaitTermination)
     *   Phase 3: If still not done, force stop (shutdownNow)
     */
    @PreDestroy
    public void onShutdown() {
        System.out.println("3️⃣  [BackgroundTaskExecutor] @PreDestroy — shutting down thread pool");

        try {
            // Phase 1: No new tasks
            executorService.shutdown();
            System.out.println("   Phase 1: Stopped accepting new tasks");

            // Phase 2: Wait up to 30 seconds for running tasks
            if (executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                System.out.println("   Phase 2: All tasks completed gracefully ✅");
            } else {
                // Phase 3: Force shutdown if tasks didn't finish
                System.out.println("   Phase 3: Forcing shutdown (tasks timed out) ⚠️");
                java.util.List<Runnable> notExecuted = executorService.shutdownNow();
                System.out.println("   " + notExecuted.size() + " tasks were interrupted.");
            }
        } catch (InterruptedException e) {
            // Re-interrupt the thread to propagate the interruption signal
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
            System.out.println("   Thread interrupted during shutdown ⚠️");
        }

        System.out.println("✅ [BackgroundTaskExecutor] Thread pool shut down gracefully.");
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 3: @Lazy — DEFERRED (LAZY) BEAN INITIALIZATION
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                           @Lazy  EXPLAINED                                   ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @Lazy means: "Don't create this bean at application startup.
 *               Create it ONLY when it is first requested (injected or looked up)."
 *
 * Default behavior (no @Lazy):  bean created during ApplicationContext refresh
 * With @Lazy:                   bean created on first use (lazily)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 WHEN TO USE @Lazy:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  ✅ 1. EXPENSIVE beans you might not always need
 *        (e.g., heavy report generator loaded only if reports are actually requested)
 *
 *  ✅ 2. OPTIONAL features / plugins
 *        (e.g., PDF exporter only if the 'pdf-export' feature flag is enabled)
 *
 *  ✅ 3. STARTUP TIME optimization
 *        (many @Lazy beans → faster startup; they initialize on first actual use)
 *
 *  ✅ 4. CIRCULAR DEPENDENCY RESOLUTION (last resort)
 *        (breaking cycles by lazily initializing one of the beans)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔧 WHERE YOU CAN PUT @Lazy:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  OPTION A: On the BEAN CLASS (makes the bean lazy everywhere it is injected)
 *
 *       @Lazy
 *       @Component
 *       class HeavyBean { ... }
 *
 *  OPTION B: On the INJECTION POINT (lazy only at this specific injection)
 *
 *       @Service
 *       class SomeService {
 *           @Lazy
 *           @Autowired
 *           private HeavyBean heavyBean;  // lazy proxy here, even if HeavyBean is not @Lazy
 *       }
 *
 *  OPTION C: On a @Bean method in @Configuration (makes that bean lazy)
 *
 *       @Bean
 *       @Lazy
 *       public HeavyBean heavyBean() { return new HeavyBean(); }
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 HOW @Lazy WORKS INTERNALLY:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * When @Lazy is used at an injection point (Option B), Spring injects a CGLIB PROXY
 * (similar to how scoped proxies work). The proxy is a stand-in for the real bean.
 *
 * On the first method call on the proxy:
 *   1. Spring checks if the real bean exists in the ApplicationContext
 *   2. If not → creates it now (running constructor + @PostConstruct)
 *   3. Delegates the method call to the real bean
 *   4. On subsequent calls → real bean already exists → delegates directly
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ CAUTION WITH @Lazy:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  Configuration errors in the lazy bean are only discovered on FIRST USE
 *     (potentially at runtime in production, not at startup)
 *  •  Don't overuse — eager initialization catches errors early
 *  •  Performance: first call to a lazy bean is slower (initialization overhead)
 *  •  Thread safety: if multiple threads first-access a lazy bean simultaneously,
 *     Spring handles synchronization correctly
 *
 */

/**
 * A heavy, expensive report generator that should NOT be created at startup.
 * It takes several seconds to initialize and uses significant memory.
 * Only created when someone actually requests a report.
 */
@Component
@Lazy   // ← "Don't create me at startup; wait until I'm actually needed"
class HeavyReportGenerator {

    private static final java.time.Duration INIT_DURATION = java.time.Duration.ofMillis(500);

    public HeavyReportGenerator() {
        System.out.println("⚙️  [HeavyReportGenerator] Constructor started — "
                + "simulating heavy initialization...");

        // Simulate expensive initialization (loading templates, fonts, data, etc.)
        try {
            Thread.sleep(INIT_DURATION.toMillis());  // Simulate 500ms init time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("✅ [HeavyReportGenerator] Ready! (took " + INIT_DURATION.toMillis() + "ms)");
    }

    @PostConstruct
    public void loadReportTemplates() {
        System.out.println("📄 [HeavyReportGenerator] @PostConstruct: Loading report templates...");
        System.out.println("   ✅ 42 templates loaded.");
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("🗑️  [HeavyReportGenerator] @PreDestroy: Releasing report resources...");
    }

    public String generateReport(String reportType, String userId) {
        System.out.printf("📊 Generating %s report for user %s...%n", reportType, userId);
        return String.format("REPORT[%s] for user %s generated at %s", reportType, userId, Instant.now());
    }
}

/**
 * A PDF export service that is only needed if the 'pdf-export' module is active.
 * Using @Lazy at the INJECTION POINT means the PdfExportService itself doesn't
 * need to be @Lazy — the decision is made at the consumer level.
 */
@Service
class ReportService {

    /*
     * @Lazy on the INJECTION POINT:
     *
     * Even though HeavyReportGenerator is already @Lazy on the class,
     * we explicitly note @Lazy on the injection point here for clarity.
     *
     * If HeavyReportGenerator was NOT @Lazy, we could still add @Lazy
     * here to make this specific injection lazy.
     */
    @Lazy
    private final HeavyReportGenerator reportGenerator;

    // This injected at startup normally (not lazy) because it is lightweight.
    private final ApplicationStatisticsService statsService;

    public ReportService(
            @Lazy HeavyReportGenerator reportGenerator,  // Lazy injection
            ApplicationStatisticsService statsService) { // Eager injection (normal)
        this.reportGenerator = reportGenerator;
        this.statsService    = statsService;

        // Note: even though reportGenerator is declared here, it's a PROXY at this point.
        // The REAL HeavyReportGenerator is NOT created yet.
        System.out.println("✅ [ReportService] Created. HeavyReportGenerator is a proxy (not yet initialized).");
    }

    /**
     * When this method is called for the FIRST TIME, Spring:
     *   1. Detects that reportGenerator is a lazy proxy
     *   2. Creates the REAL HeavyReportGenerator (runs constructor + @PostConstruct)
     *   3. Delegates generateReport() to the real instance
     *
     * On the SECOND+ call, the real instance is already created — immediate delegation.
     */
    public String generateReport(String type, String userId) {
        System.out.println("📋 [ReportService] Requesting report — this will trigger lazy init on first call");
        statsService.incrementRequests();
        return reportGenerator.generateReport(type, userId);  // ← First call triggers lazy init
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 4: THE COMPLETE LIFECYCLE — FULL PICTURE
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                  THE COMPLETE SPRING BEAN LIFECYCLE                          ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔄 FULL LIFECYCLE SEQUENCE (from creation to destruction):
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   APPLICATION STARTUP:
 *   ════════════════════
 *
 *   1️⃣  Spring ApplicationContext starts
 *          ↓
 *   2️⃣  @ComponentScan discovers beans
 *          ↓
 *   3️⃣  BeanDefinitions registered
 *          ↓
 *   4️⃣  Bean INSTANTIATION
 *        → Class constructor is called
 *        → At this point: @Value and @Autowired fields are NOT yet set
 *          ↓
 *   5️⃣  DEPENDENCY INJECTION
 *        → @Autowired dependencies injected
 *        → @Value properties injected
 *          ↓
 *   6️⃣  BEAN AWARE INTERFACES (if implemented)
 *        → setBeanName(), setApplicationContext(), etc.
 *          ↓
 *   7️⃣  BeanPostProcessor.postProcessBeforeInitialization()
 *          ↓
 *   8️⃣  @PostConstruct METHOD CALLED  ← ← ← ← ← ← ← ← ← ← ← ← ← ← WE ARE HERE
 *        → All dependencies are available
 *        → Safe to initialize resources, open connections, warm caches
 *          ↓
 *   9️⃣  InitializingBean.afterPropertiesSet() (if implemented)
 *          ↓
 *  🔟  @Bean(initMethod="...") (if specified in @Configuration)
 *          ↓
 *  1️⃣1️⃣  BeanPostProcessor.postProcessAfterInitialization()
 *          ↓
 *  1️⃣2️⃣  BEAN IS READY — returned to consumers, serving requests
 *
 *   APPLICATION SHUTDOWN:
 *   ═════════════════════
 *
 *  1️⃣3️⃣  Shutdown hook triggered (SIGTERM / context.close())
 *          ↓
 *  1️⃣4️⃣  DestructionAwareBeanPostProcessor runs
 *          ↓
 *  1️⃣5️⃣  @PreDestroy METHOD CALLED  ← ← ← ← ← ← ← ← ← ← ← ← ← ← WE ARE HERE
 *        → Clean up resources, close connections, flush data
 *          ↓
 *  1️⃣6️⃣  DisposableBean.destroy() (if implemented)
 *          ↓
 *  1️⃣7️⃣  @Bean(destroyMethod="...") (if specified)
 *          ↓
 *  1️⃣8️⃣  Bean is DESTROYED (GC eligible)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 KEY TAKEAWAYS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1.  Constructor → @Value/@Autowired injection → @PostConstruct → ready → @PreDestroy
 *  2.  Never use injected fields IN the constructor — they are not set yet
 *  3.  @PostConstruct is perfect for resource opening + config validation + cache warmup
 *  4.  @PreDestroy is perfect for graceful shutdown — close pools, flush buffers
 *  5.  @Lazy defers bean creation to first use — use for heavy/optional beans
 *  6.  @PreDestroy is NOT called for @Scope("prototype") beans
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 💡 LIFECYCLE IN CODE — WHAT YOU SEE IN CONSOLE ON STARTUP:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   1️⃣  [DatabaseConnectionPool] Constructor called — fields NOT yet injected
 *   2️⃣  [DatabaseConnectionPool] @PostConstruct called — fields now injected
 *        🔌 Initializing connection pool...
 *   1️⃣  [CacheWarmupService] Constructor called
 *   2️⃣  [CacheWarmupService] @PostConstruct called
 *        🔥 Warming up application cache...
 *
 *   ON SHUTDOWN:
 *   3️⃣  [CacheWarmupService] @PreDestroy called — clearing cache
 *   3️⃣  [DatabaseConnectionPool] @PreDestroy called — closing pool
 *
 *   NOTE: @Lazy beans (HeavyReportGenerator) show NO output at startup!
 *   Only when reportService.generateReport() is first called:
 *        ⚙️  [HeavyReportGenerator] Constructor started...
 *        ✅ [HeavyReportGenerator] Ready!
 *        📄 [HeavyReportGenerator] @PostConstruct: Loading report templates...
 *
 */
class Example04LifecycleAnnotations {
    // Intentionally empty — documentation class
}

