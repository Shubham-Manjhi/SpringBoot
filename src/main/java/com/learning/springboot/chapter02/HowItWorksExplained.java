package com.learning.springboot.chapter02;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║            CHAPTER 2: HOW SPRING FRAMEWORK CORE ANNOTATIONS WORK                     ║
 * ║                         DEEP DIVE — INTERNAL MECHANICS                               ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        HowItWorksExplained.java
 * Purpose:     Understand the INTERNAL workings of the Spring IoC container —
 *              how annotations are processed, how beans are wired, how proxies work.
 *              This is the "engine room" — understanding it makes you a Spring expert.
 * Difficulty:  ⭐⭐⭐⭐ Advanced
 * Time:        40 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║          HOW SPRING FRAMEWORK CORE ANNOTATIONS WORK INTERNALLY               ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
public class HowItWorksExplained {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        SECTION 1: THE SPRING IoC CONTAINER — ARCHITECTURE DEEP DIVE         ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🏛️ WHAT IS THE APPLICATION CONTEXT?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * The ApplicationContext is a REGISTRY of all Spring beans.
     * Think of it as a sophisticated Map<Type/Name, BeanDefinition + Instance>.
     *
     * Internal structure (simplified):
     *
     *   ApplicationContext
     *     │
     *     ├── BeanFactory  (bean creation + storage)
     *     │     ├── beanDefinitionMap  Map<String, BeanDefinition>
     *     │     │     "userService"  → BeanDefinition{class=UserServiceImpl, scope=singleton, ...}
     *     │     │     "orderService" → BeanDefinition{class=OrderService, scope=singleton, ...}
     *     │     │
     *     │     └── singletonObjects  Map<String, Object>    (the actual instances)
     *     │           "userService"  → UserServiceImpl@7f2b
     *     │           "orderService" → OrderService@9c3a
     *     │
     *     ├── Environment  (properties, profiles, env vars)
     *     ├── MessageSource (i18n)
     *     ├── ApplicationEventPublisher (events)
     *     └── ResourceLoader (classpath resources)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔑 BeanDefinition — The Blueprint for a Bean
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Before creating a bean instance, Spring creates a BeanDefinition — a metadata
     * object that describes how the bean should be created:
     *
     *   BeanDefinition {
     *       beanClass         = UserServiceImpl.class
     *       scope             = "singleton"
     *       primary           = false
     *       lazyInit          = false
     *       autowireMode      = AUTOWIRE_CONSTRUCTOR
     *       dependsOn         = []
     *       constructorArgs   = [UserRepository, EmailService]
     *       initMethodName    = "init"          // @PostConstruct
     *       destroyMethodName = "cleanup"       // @PreDestroy
     *       qualifiers        = []
     *   }
     *
     * BeanDefinitions are registered by:
     *   •  @ComponentScan   → reads .class files, finds @Component annotations
     *   •  @Bean methods    → explicitly declared in @Configuration classes
     *   •  XML config       → legacy (rarely used today)
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        SECTION 2: THE COMPLETE STARTUP SEQUENCE STEP BY STEP                ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     * PHASE 1: APPLICATION STARTUP — SpringApplication.run()
     * ═══════════════════════════════════════════════════════════════════════════════
     *
     *   SpringApplication.run(MyApp.class, args)
     *         ↓
     *   1. Creates SpringApplication
     *   2. Determines application type: SERVLET / REACTIVE / NONE
     *   3. Creates appropriate ApplicationContext:
     *         SERVLET   → AnnotationConfigServletWebServerApplicationContext
     *         REACTIVE  → AnnotationConfigReactiveWebServerApplicationContext
     *         NONE      → AnnotationConfigApplicationContext
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     * PHASE 2: COMPONENT SCANNING — How @Component Annotations Are Discovered
     * ═══════════════════════════════════════════════════════════════════════════════
     *
     *   ClassPathBeanDefinitionScanner is the workhorse here.
     *
     *   STEP 2.1: Determine base packages
     *     → @SpringBootApplication on class com.example.MyApp
     *     → Base package = "com.example"
     *     → All sub-packages will be scanned recursively
     *
     *   STEP 2.2: Scan .class files
     *     → Reads ALL .class files in the base package using ASM library
     *     → ASM reads bytecode WITHOUT loading classes into JVM (performance!)
     *     → Looks for specific annotations:
     *
     *         TypeFilter checks for:
     *           • @Component (and all meta-annotations: @Service, @Repository, @Controller)
     *           • @Configuration
     *           • Any custom annotation meta-annotated with @Component
     *
     *   STEP 2.3: For each matching class:
     *     → Create ScannedGenericBeanDefinition
     *     → Determine bean name:
     *           @Component                   → "myServiceImpl" (camelCase)
     *           @Component("myCustomName")   → "myCustomName"
     *     → Register in BeanDefinitionRegistry
     *
     *   STEP 2.4: Process @Configuration classes
     *     → Parse @Bean methods → register additional BeanDefinitions
     *     → Parse @Import → include other configuration classes
     *     → Parse @PropertySource → add property sources to Environment
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     * PHASE 3: BEAN INSTANTIATION — Creating the Objects
     * ═══════════════════════════════════════════════════════════════════════════════
     *
     *   After all BeanDefinitions are registered, Spring starts creating instances.
     *
     *   ORDER OF CREATION:
     *
     *   1. BeanFactoryPostProcessors first (modify BeanDefinitions before creation)
     *         e.g., PropertySourcesPlaceholderConfigurer (resolves @Value placeholders)
     *
     *   2. BeanPostProcessors (created before normal beans — they process other beans)
     *         e.g., AutowiredAnnotationBeanPostProcessor (handles @Autowired)
     *              CommonAnnotationBeanPostProcessor   (handles @PostConstruct, @PreDestroy)
     *              PersistenceAnnotationBeanPostProcessor (handles @PersistenceContext)
     *
     *   3. Singleton beans (in dependency order)
     *         Spring builds a dependency graph
     *         Beans with no dependencies are created first
     *         Then beans that depend on those, and so on
     *
     *   DEPENDENCY RESOLUTION ALGORITHM:
     *
     *     Given:
     *       A depends on B
     *       B depends on C
     *       C has no dependencies
     *
     *     Creation order: C → B → A
     *
     *     If circular: A depends on B, B depends on A
     *       → Spring uses a THREE-LEVEL CACHE to handle this:
     *
     *         Level 1: singletonObjects        (fully initialized)
     *         Level 2: earlySingletonObjects   (created but not yet fully initialized)
     *         Level 3: singletonFactories      (factory lambda to create early reference)
     *
     *       Spring creates A partially → moves to level 3
     *       Creates B → B needs A → gets A's early reference from level 3
     *       Finishes initializing A → moves to level 1
     *
     *       ⚠️ This works ONLY with singleton scope + setter/field injection
     *       Constructor injection with circular deps → ❌ BeanCurrentlyInCreationException
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     * PHASE 4: DEPENDENCY INJECTION — Wiring the Beans
     * ═══════════════════════════════════════════════════════════════════════════════
     *
     *   AutowiredAnnotationBeanPostProcessor is the key component here.
     *
     *   HOW @Autowired IS PROCESSED:
     *
     *   STEP 4.1: Inspect the class for @Autowired annotations
     *     → Scan constructors, fields, and setter methods
     *     → Build InjectionMetadata (cache for performance)
     *
     *   STEP 4.2: For each injection point, resolve the bean to inject:
     *
     *         a) FIND BY TYPE:
     *            context.getBeanNamesForType(InjectionType.class)
     *            Returns all bean names whose type matches
     *
     *         b) IF 1 MATCH → inject it
     *
     *         c) IF 0 MATCHES:
     *            @Autowired(required=true)  → throw NoSuchBeanDefinitionException
     *            @Autowired(required=false) → skip (leave null)
     *
     *         d) IF 2+ MATCHES:
     *            → Check for @Primary bean → if found, inject it
     *            → Check if one bean name matches the field/parameter name → inject it
     *            → Otherwise → throw NoUniqueBeanDefinitionException
     *
     *   STEP 4.3: @Qualifier processing
     *     → @Qualifier("beanName") narrows the candidate set to the named bean
     *     → Evaluated BEFORE @Primary check
     *
     *   STEP 4.4: Perform actual injection:
     *     Constructor injection → calls constructor with resolved args
     *     Field injection       → uses java.lang.reflect.Field.set()  (bypasses private!)
     *     Setter injection      → calls the setter method
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     * PHASE 5: @Value PROCESSING
     * ═══════════════════════════════════════════════════════════════════════════════
     *
     *   @Value is also processed by AutowiredAnnotationBeanPostProcessor.
     *
     *   HOW @Value IS RESOLVED:
     *
     *   @Value("${property.key:default}")
     *
     *   STEP 5.1: PropertySourcesPlaceholderConfigurer pre-processes BeanDefinitions
     *     → Replaces ${...} placeholders with actual values from the Environment
     *
     *   STEP 5.2: At injection time:
     *     → PropertyPlaceholderHelper resolves ${property.key}
     *     → Searches sources in order:
     *           1. Command-line args  (highest priority)
     *           2. Environment variables
     *           3. application-{profile}.yml
     *           4. application.yml
     *           5. @PropertySource files
     *           6. Default values in @Value(":default")
     *
     *   STEP 5.3: Type conversion
     *     → The raw String value is converted to the target type
     *     → Supports: String, int, long, boolean, Duration, Period, List, Set, Map
     *     → Uses ConversionService for complex types
     *
     *   HOW SpEL IS RESOLVED:
     *
     *   @Value("#{expression}")
     *
     *   → Spring's ExpressionParser evaluates the expression
     *   → Has access to:
     *       systemProperties       (OS system properties)
     *       systemEnvironment      (OS environment variables)
     *       @beanName              (reference any Spring bean: @myBean.someProperty)
     *       T(ClassName)           (reference Java classes: T(Math).PI)
     *       literal values, operators, conditional expressions
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        SECTION 3: HOW @PostConstruct AND @PreDestroy WORK INTERNALLY        ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Both annotations are processed by CommonAnnotationBeanPostProcessor.
     *
     * @PostConstruct — INTERNAL MECHANISM:
     * ═════════════════════════════════════
     *
     *   STEP 1: CommonAnnotationBeanPostProcessor.postProcessBeforeInitialization()
     *     → Invoked AFTER dependency injection, BEFORE bean is put into use
     *     → Finds all methods annotated with @PostConstruct
     *     → Calls them using reflection: method.invoke(bean)
     *
     *   STEP 2: If @PostConstruct method throws any exception:
     *     → Spring wraps it in BeanCreationException
     *     → Application startup FAILS with a clear error
     *     → This is the "fail fast" behaviour we want!
     *
     *   STEP 3: Bean is now placed in singletonObjects map (ready state)
     *
     * @PreDestroy — INTERNAL MECHANISM:
     * ════════════════════════════════════
     *
     *   STEP 1: JVM shutdown hook fires (Runtime.getRuntime().addShutdownHook(...))
     *     Spring Boot registers this hook automatically on startup.
     *
     *   STEP 2: ConfigurableApplicationContext.close() is called
     *
     *   STEP 3: DefaultSingletonBeanRegistry.destroySingletons()
     *     → Iterates over all singleton beans in REVERSE creation order
     *     → For each bean that implements DisposableBean or has @PreDestroy:
     *           Calls DisposableBeanAdapter.destroy()
     *
     *   STEP 4: CommonAnnotationBeanPostProcessor processes @PreDestroy
     *     → method.invoke(bean) — calls the @PreDestroy method via reflection
     *
     *   STEP 5: Bean is removed from singletonObjects
     *     → GC eligible
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * WHY REVERSE ORDER FOR SHUTDOWN?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * If A depends on B depends on C:
     *   Creation order:  C → B → A
     *   Destruction order: A → B → C  (reverse)
     *
     * This ensures that A is destroyed BEFORE the B it depends on.
     * A's @PreDestroy can safely use B — because B is still alive at that point.
     *
     */

    /*
     * ╔════════���══════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║             SECTION 4: HOW BEAN SCOPES WORK INTERNALLY                      ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * SINGLETON SCOPE — INTERNAL STORAGE:
     * ═════════════════════════════════════
     *
     *   DefaultSingletonBeanRegistry maintains:
     *
     *     singletonObjects: ConcurrentHashMap<String, Object>
     *       → "userService"  → UserServiceImpl@7f2b
     *       → "orderService" → OrderService@9c3a
     *
     *   Lookup:
     *     context.getBean("userService")   → returns singletonObjects.get("userService")
     *     context.getBean(UserService.class) → finds all beans of type UserService,
     *                                          returns the one from singletonObjects
     *
     * PROTOTYPE SCOPE — NO STORAGE:
     * ══════════════════════════════
     *
     *   Spring does NOT store prototype beans.
     *   Every request creates a fresh instance by calling the bean's factory method.
     *   After creation and injection, Spring forgets about it (no tracking, no @PreDestroy).
     *
     * REQUEST / SESSION / APPLICATION SCOPE — SCOPED PROXY + THREAD-LOCAL:
     * ═════════════════════════════��════════════════════════════════════════
     *
     *   These scopes use the SCOPED PROXY pattern.
     *
     *   HOW THE SCOPED PROXY WORKS:
     *
     *   1. Spring creates a CGLIB proxy class (subclass of the original bean)
     *   2. This proxy is what gets stored in the singleton bean that depends on it
     *
     *   3. When a method on the proxy is called:
     *      a) Proxy calls its ScopeManager:
     *             requestScope.get("shoppingCart", objectFactory)
     *      b) ScopeManager looks in the CURRENT REQUEST's attribute store:
     *             HttpServletRequest.getAttribute("scopedTarget.shoppingCart")
     *      c) If found → return it
     *         If not found → call objectFactory.getObject() to create new instance
     *                     → store in request attributes
     *                     → return it
     *      d) Proxy delegates the method call to the real instance
     *
     *   VISUAL:
     *
     *   Thread 1 (Request A):
     *     orderService.checkout()
     *       → shoppingCartProxy.getItems()
     *           → RequestScope.get("shoppingCart")
     *               → HttpServletRequest-A.getAttribute → ShoppingCart-A (items: [MacBook])
     *
     *   Thread 2 (Request B):
     *     orderService.checkout()
     *       → shoppingCartProxy.getItems()
     *           → RequestScope.get("shoppingCart")
     *               → HttpServletRequest-B.getAttribute → ShoppingCart-B (items: [Book])
     *
     *   SAME proxy object, but delegates to DIFFERENT real instances per thread/request!
     *   This is the power of scoped proxies.
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║         SECTION 5: HOW @Lazy WORKS — PROXY INTERNALS                        ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * @Lazy IMPLEMENTATION:
     * ══════════════════════
     *
     *   When @Lazy is used at an injection point:
     *
     *   @Autowired
     *   @Lazy
     *   private HeavyReportGenerator reportGenerator;
     *
     *   Spring does NOT inject the real HeavyReportGenerator.
     *   Instead it injects a CGLIB proxy.
     *
     *   The CGLIB proxy is a generated subclass of HeavyReportGenerator:
     *
     *     class HeavyReportGenerator$$SpringCGLIB$$0 extends HeavyReportGenerator {
     *         private volatile HeavyReportGenerator CGLIB$target; // null initially
     *
     *         @Override
     *         public String generateReport(String type, String userId) {
     *             if (CGLIB$target == null) {
     *                 synchronized(this) {
     *                     if (CGLIB$target == null) {
     *                         CGLIB$target = applicationContext.getBean(HeavyReportGenerator.class);
     *                         // ↑ This triggers real bean creation!
     *                     }
     *                 }
     *             }
     *             return CGLIB$target.generateReport(type, userId);
     *         }
     *     }
     *
     *   Double-checked locking ensures thread safety on first access.
     *
     *   TIMELINE:
     *
     *   Application startup:
     *     → ReportService constructor called
     *     → Spring injects CGLIB proxy for HeavyReportGenerator
     *     → HeavyReportGenerator REAL bean NOT created yet
     *     → Startup completes FAST
     *
     *   First call to reportService.generateReport():
     *     → Calls proxy.generateReport()
     *     → Proxy detects: CGLIB$target == null
     *     → Calls context.getBean(HeavyReportGenerator.class)
     *     → Spring creates REAL HeavyReportGenerator:
     *         → Constructor runs (500ms simulation)
     *         → @PostConstruct runs (loads templates)
     *     → Proxy stores reference in CGLIB$target
     *     → Delegates generateReport() to real bean
     *
     *   Second call:
     *     → Calls proxy.generateReport()
     *     → CGLIB$target != null → delegate immediately (no overhead)
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║         SECTION 6: BeanPostProcessor — The Plugin System                    ║
     * ║                                                                               ��
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * BeanPostProcessor is the PLUGIN SYSTEM of the Spring IoC container.
     * It intercepts bean creation and allows post-processing before/after initialization.
     *
     * KEY BeanPostProcessors in Spring:
     * ═══════════════════════════════════
     *
     *  1. AutowiredAnnotationBeanPostProcessor
     *     → Processes @Autowired, @Value, @Inject
     *     → Does the actual reflection-based dependency injection
     *
     *  2. CommonAnnotationBeanPostProcessor
     *     → Processes @PostConstruct, @PreDestroy, @Resource, @WebServiceRef
     *     → Calls @PostConstruct methods
     *     → Registers @PreDestroy methods for later cleanup
     *
     *  3. PersistenceAnnotationBeanPostProcessor
     *     → Processes @PersistenceContext, @PersistenceUnit (JPA)
     *
     *  4. AnnotationAwareAspectJAutoProxyCreator
     *     → Creates AOP proxies for @Aspect-annotated classes
     *     → Wraps beans with @Before, @After, @Around advice
     *
     *  5. AsyncAnnotationBeanPostProcessor
     *     → Wraps beans with @Async methods in an async proxy
     *
     *  6. ScheduledAnnotationBeanPostProcessor
     *     → Registers @Scheduled methods with the task scheduler
     *
     * HOW BeanPostProcessor FITS IN THE LIFECYCLE:
     * ══════════════════════════════════════════════
     *
     *   Bean created (constructor)
     *       ↓
     *   BeanPostProcessor.postProcessBeforeInitialization(bean, beanName)
     *       ↓   ← @PostConstruct is called HERE by CommonAnnotationBeanPostProcessor
     *   InitializingBean.afterPropertiesSet() (if implemented)
     *       ↓
     *   BeanPostProcessor.postProcessAfterInitialization(bean, beanName)
     *       ↓   ← AOP PROXY WRAPPING happens HERE (the bean might be replaced by a proxy!)
     *   FINAL BEAN stored in singletonObjects
     *
     * ────────────────────────────────────────────────────────────────────────���────────
     * 💡 IMPORTANT: After postProcessAfterInitialization, the FINAL stored object
     * might be a PROXY (not the original bean). This is how AOP, @Transactional,
     * @Async, and @Cacheable all work — they wrap the original bean in a proxy.
     *
     * That's why self-invocation doesn't work with proxies:
     *
     *   @Service
     *   class MyService {
     *       @Transactional
     *       public void methodA() {
     *           methodB();  // ← ❌ Calls methodB() directly on THIS (not the proxy)
     *                       //    @Transactional on methodB() is BYPASSED!
     *       }
     *
     *       @Transactional
     *       public void methodB() { ... }
     *   }
     *
     * The AOP proxy intercepts calls from OUTSIDE the bean.
     * Internal method calls (this.methodB()) go directly to the real object,
     * bypassing the proxy and all proxy-based features.
     * ───────────────────────────────────────────────────────────���─────────────────────
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║         SECTION 7: THE COMPLETE VISUAL FLOW DIAGRAM                         ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     *  SpringApplication.run()
     *        │
     *        ▼
     *  ┌─────────────────────────────────────────────────────────┐
     *  │           CREATE ApplicationContext                      │
     *  │  (AnnotationConfigServletWebServerApplicationContext)    │
     *  └───────────────────────┬─────────────────────────────────┘
     *                          │
     *                          ▼
     *  ┌─────────────────────────────────────────────────────────┐
     *  │              LOAD ENVIRONMENT                           │
     *  │  • application.yml                                      │
     *  │  • System properties (JVM -D flags)                     │
     *  │  • OS environment variables                             │
     *  │  • Command-line arguments                               │
     *  └───────────────────────┬─────────────────────────────────┘
     *                          │
     *                          ▼
     *  ┌─────────────────────────────────────────────��───────────┐
     *  │           COMPONENT SCANNING (@ComponentScan)           │
     *  │  ClassPathBeanDefinitionScanner scans classpath         │
     *  │  Finds: @Component @Service @Repository @Controller     │
     *  │  Creates BeanDefinitions (blueprints, not instances)    │
     *  └───────────────────────┬─────────────────────────────────┘
     *                          │
     *                          ▼
     *  ┌─────────────────────────────────────────────────────────┐
     *  │        BeanFactory POST-PROCESSING                      │
     *  │  PropertySourcesPlaceholderConfigurer resolves ${...}  │
     *  │  ConfigurationClassPostProcessor processes @Bean        │
     *  └───────────────────────┬─────────────────────────────────┘
     *                          │
     *                          ▼
     *  ┌─────────────────────────────────────────────────────────┐
     *  │         REGISTER BeanPostProcessors first               │
     *  │  AutowiredAnnotationBeanPostProcessor                   │
     *  │  CommonAnnotationBeanPostProcessor                      │
     *  │  AnnotationAwareAspectJAutoProxyCreator                 │
     *  └───────────────────────┬─────────────────────────────────┘
     *                          │
     *                          ▼
     *  ┌─────────────────────────────────────────────────────────┐
     *  │       INSTANTIATE SINGLETON BEANS (dependency order)    │
     *  │                                                         │
     *  │  For each bean:                                         │
     *  │    1. constructor() called                              │
     *  │    2. @Autowired / @Value injected (BeanPostProcessor)  │
     *  │    3. @PostConstruct called (BeanPostProcessor)         │
     *  │    4. AOP proxy wrapping (BeanPostProcessor)            │
     *  │    5. Stored in singletonObjects                        │
     *  │                                                         │
     *  │  @Lazy beans: SKIPPED (proxy stored instead)           │
     *  └───────────────────────┬─────────────────────────────────┘
     *                          │
     *                          ▼
     *  ┌─────────────────────────────────────────────────────────┐
     *  │          START EMBEDDED WEB SERVER (Tomcat)             │
     *  │  Map URL patterns to @Controller handler methods        │
     *  └───────────────────────┬─────────────────────────────────┘
     *                          │
     *                          ▼
     *  ┌─────────────────────────────────────────────────────────┐
     *  │            APPLICATION IS READY  🚀                      │
     *  │       Accepting HTTP requests                           │
     *  └───────────────────────┬─────────────────────────────────┘
     *                          │
     *                   (time passes)
     *                          │
     *                          ▼
     *  ┌─────────────────────────────────────────────────────────┐
     *  │              SHUTDOWN TRIGGERED                         │
     *  │  (SIGTERM / Ctrl+C / context.close())                   │
     *  └───────────────────────┬─────────────────────────────────┘
     *                          │
     *                          ▼
     *  ┌─────────────────────────────────────────────────────────┐
     *  │       DESTROY SINGLETON BEANS (REVERSE order)           │
     *  │                                                         │
     *  │  For each bean (in reverse creation order):             │
     *  │    1. @PreDestroy called (CommonAnnotationBPP)          │
     *  │    2. DisposableBean.destroy() (if implemented)         │
     *  │    3. destroyMethod (if specified in @Bean)             │
     *  │    4. Removed from singletonObjects                     │
     *  └───────────────────────┬─────────────────────────────────┘
     *                          │
     *                          ▼
     *                    APPLICATION STOPPED
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║         SECTION 8: COMMON QUESTIONS & THEIR INTERNAL ANSWERS                ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Q: "How does Spring inject into private fields? Private means inaccessible!"
     * A: Spring uses java.lang.reflect.Field.setAccessible(true) to bypass Java's
     *    access control at runtime. This is valid in Java but bypasses encapsulation,
     *    which is one reason why field injection is considered a bad practice.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Q: "Why does my @Transactional method not work when called from the same class?"
     * A: @Transactional works via an AOP proxy (CGLIB subclass). When you call
     *    this.methodB() inside the class, you are calling the REAL object directly,
     *    bypassing the proxy. The proxy only intercepts calls from OUTSIDE the bean.
     *    Fix: inject the bean into itself (not recommended), or use ApplicationContext.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Q: "Why can't I @Autowired a prototype bean into a singleton and get a new instance each time?"
     * A: @Autowired injection happens ONCE, at the singleton's creation. After that,
     *    the same prototype instance is stored in the singleton's field. Spring does not
     *    re-inject at every method call. Use ObjectProvider<T> or @Lookup method injection.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Q: "Why does Spring fail to start when I have a circular dependency with constructor injection?"
     * A: Spring's three-level singleton cache only works for singleton beans where
     *    the INSTANCE can be partially created (after constructor, before full injection).
     *    With constructor injection, Spring can't create the instance without all
     *    constructor arguments — leading to a deadlock. Use setter injection or @Lazy
     *    to break the cycle (or, better, redesign to eliminate the cycle).
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Q: "What happens if @PostConstruct throws an exception?"
     * A: The exception is caught and wrapped in BeanCreationException.
     *    The bean is NOT stored in singletonObjects.
     *    The ApplicationContext startup FAILS.
     *    The JVM exits with an error code.
     *    This is intentional — fail fast, don't run in a broken state.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Q: "Are @Component-scanned beans and @Bean-method beans treated the same way?"
     * A: Yes — both end up as BeanDefinitions in the BeanDefinitionRegistry.
     *    Once registered, Spring treats them identically: same instantiation, same
     *    BeanPostProcessor pipeline, same lifecycle (PostConstruct, PreDestroy).
     *    The difference is only in HOW the BeanDefinition is registered:
     *      @Component → via ClassPathBeanDefinitionScanner
     *      @Bean       → via ConfigurationClassBeanDefinitionReader
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║         SECTION 9: MENTAL MODEL — THE "BOOK" SUMMARY                        ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Think of Spring's IoC container as a SMART FACTORY:
     *
     *  1. CATALOG (BeanDefinitions)
     *     Like a product catalog — describes what can be built and how.
     *     @Component / @Service / @Repository / @Controller register entries.
     *
     *  2. FACTORY (Bean Instantiation)
     *     The factory builds products from the catalog, resolving dependencies
     *     (like sub-components) automatically in the right order.
     *
     *  3. ASSEMBLY LINE (BeanPostProcessors)
     *     Before a product leaves the factory, it goes through quality checks
     *     and enhancements (@Autowired injection, @PostConstruct, AOP proxying).
     *
     *  4. WAREHOUSE (singletonObjects map)
     *     Finished products are stored here. When you ask for a singleton,
     *     you always get the SAME item from the warehouse.
     *
     *  5. CUSTOM ORDERS (prototype scope)
     *     Some products are made fresh per request — not stored in the warehouse.
     *
     *  6. DEPARTMENT STORES (request/session scope)
     *     Some products are scoped to a store visit (session) or
     *     a single shopping trip (request) — separate stock per context.
     *
     *  7. CLEANING CREW (@PreDestroy)
     *     Before the factory closes, the cleaning crew runs through each product
     *     in reverse creation order — releasing resources, flushing data.
     *
     * This mental model maps 1:1 to every concept in this chapter:
     *
     *   @Component    → REGISTER in the catalog
     *   @Autowired    → RESOLVE and ASSEMBLE dependencies
     *   @Qualifier    → PICK a SPECIFIC catalog item by name
     *   @Primary      → MARK the DEFAULT catalog item
     *   @Value        → INJECT configuration into the product
     *   @Scope        → DEFINE storage policy (warehouse vs. custom order)
     *   @PostConstruct → RUN quality checks after assembly
     *   @PreDestroy   → CLEAN UP when factory closes
     *   @Lazy         → DON'T build until first customer orders it
     *
     */
}

