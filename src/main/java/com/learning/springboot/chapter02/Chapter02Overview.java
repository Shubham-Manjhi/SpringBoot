package com.learning.springboot.chapter02;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║                    SPRING BOOT ANNOTATIONS - COMPREHENSIVE GUIDE                     ║
 * ║                    Chapter 2: Spring Framework Core Annotations                      ║
 * ║                                                                                       ║
 * ║                           📚 From Zero to Expert 📚                                  ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Chapter:      2
 * Title:        Spring Framework Core Annotations
 * Difficulty:   ⭐⭐ Beginner–Intermediate
 * Estimated:    4–6 hours
 * Prerequisites: Chapter 1 (Core Spring Boot), Basic Java OOP, Interfaces & Inheritance
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │                                                                                       │
 * │                    CHAPTER 2: OVERVIEW & LEARNING GOALS                              │
 * │                                                                                       │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📖 TABLE OF CONTENTS 📖
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Section 1  :  Chapter Introduction & Overview
 * Section 2  :  The BIG IDEA — Spring IoC Container
 * Section 3  :  Stereotype Annotations
 *                   → @Component  (The Root)
 *                   → @Service    (Business Layer)
 *                   → @Repository (Data Access Layer)
 *                   → @Controller (Web Layer)
 * Section 4  :  Dependency Injection Annotations
 *                   → @Autowired  (Auto-wiring)
 *                   → @Qualifier  (Disambiguation)
 *                   → @Primary    (Default Bean)
 *                   → @Value      (Property Injection)
 * Section 5  :  Bean Scope Annotations
 *                   → @Scope           (Generic scope)
 *                   → @RequestScope    (Per HTTP request)
 *                   → @SessionScope    (Per HTTP session)
 *                   → @ApplicationScope (Per application)
 * Section 6  :  Bean Lifecycle Annotations
 *                   → @PostConstruct   (Initialization callback)
 *                   → @PreDestroy      (Destruction callback)
 *                   → @Lazy            (Deferred initialization)
 * Section 7  :  How Everything Works Together — Internal Mechanics
 * Section 8  :  Best Practices & Common Pitfalls
 * Section 9  :  Interview Questions & Answers
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * 📂 FILES IN THIS CHAPTER:
 *
 *  • Chapter02Overview.java                   ← YOU ARE HERE (Big picture & concepts)
 *  • Example01StereotypeAnnotations.java       (@Component, @Service, @Repository, @Controller)
 *  • Example02DependencyInjection.java         (@Autowired, @Qualifier, @Primary, @Value)
 *  • Example03BeanScopes.java                  (@Scope, @RequestScope, @SessionScope, @ApplicationScope)
 *  • Example04LifecycleAnnotations.java        (@PostConstruct, @PreDestroy, @Lazy)
 *  • HowItWorksExplained.java                  (Internal Spring IoC Container deep dive)
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
public class Chapter02Overview {

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
     *  ✓  Understand the Spring IoC (Inversion of Control) container
     *  ✓  Use stereotype annotations to register beans automatically
     *  ✓  Inject dependencies using @Autowired, @Qualifier, @Primary
     *  ✓  Inject configuration values using @Value
     *  ✓  Control bean scopes with @Scope, @RequestScope, @SessionScope
     *  ✓  Use @PostConstruct and @PreDestroy for lifecycle management
     *  ✓  Optimize startup performance with @Lazy
     *  ✓  Write clean, maintainable Spring beans following best practices
     *  ✓  Answer core Spring interview questions with confidence
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🌟 WHY IS THIS CHAPTER CRITICAL?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Chapter 2 is the BACKBONE of the entire Spring ecosystem. Every other chapter
     * (MVC, Data JPA, Security, AOP, Scheduling) builds directly upon these annotations.
     *
     * If Chapter 1 was the IGNITION KEY, Chapter 2 is the ENGINE.
     *
     *  •  Without stereotype annotations → Spring can't find your beans
     *  •  Without dependency injection   → Beans can't talk to each other
     *  •  Without scope control          → Shared state causes bugs
     *  •  Without lifecycle hooks        → Resources leak
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║               SECTION 2: THE BIG IDEA — SPRING IoC CONTAINER                ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 WHAT IS IoC (INVERSION OF CONTROL)?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * In traditional programming:
     *
     *     class OrderService {
     *         private PaymentService paymentService = new PaymentService(); // YOU create it
     *         private ShippingService shippingService = new ShippingService(); // YOU create it
     *     }
     *
     * Problems:
     *   ❌ Tight coupling — changing PaymentService breaks OrderService
     *   ❌ Hard to test — can't swap PaymentService with a mock
     *   ❌ You manage lifecycle — creating, destroying objects manually
     *   ❌ No reuse — each class creates its own instances
     *
     * With IoC (Spring):
     *
     *     @Service
     *     class OrderService {
     *         private final PaymentService paymentService;   // Spring creates & injects it
     *         private final ShippingService shippingService; // Spring creates & injects it
     *
     *         @Autowired
     *         OrderService(PaymentService ps, ShippingService ss) {
     *             this.paymentService = ps;
     *             this.shippingService = ss;
     *         }
     *     }
     *
     * Benefits:
     *   ✓ Loose coupling — easily swap implementations
     *   ✓ Testable — inject mocks in tests
     *   ✓ Spring manages lifecycle — creation, injection, destruction
     *   ✓ Singleton by default — beans are reused
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🏗️ THE APPLICATION CONTEXT (Spring's Bean Factory)
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * The ApplicationContext IS the IoC container. Think of it as a WAREHOUSE:
     *
     *   ┌──────────────────────────────────────────────────────────────────┐
     *   │                    APPLICATION CONTEXT                           │
     *   │  (The IoC Container / Bean Factory)                              │
     *   │                                                                  │
     *   │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
     *   │  │ OrderService │  │PaymentService│  │ShippingService│          │
     *   │  │   (Bean)     │  │   (Bean)     │  │   (Bean)     │           │
     *   │  └──────┬───────┘  └──────────────┘  └──────────────┘           │
     *   │         │  depends on ↑                 ↑                        │
     *   │         └──────────────────────────────┘                         │
     *   └──────────────────────────────────────────────────────────────────┘
     *
     * Spring:
     *  1. SCANS packages for @Component, @Service, etc.
     *  2. CREATES instances (respecting scopes)
     *  3. INJECTS dependencies (wires beans together)
     *  4. MANAGES lifecycle (@PostConstruct, @PreDestroy)
     *  5. DESTROYS beans gracefully on shutdown
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 THE THREE PILLARS OF SPRING CORE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *        ┌─────────────────────────────────────────────────┐
     *        │                                                   │
     *        │   PILLAR 1       PILLAR 2         PILLAR 3        │
     *        │   ─────────      ─────────         ─────────      │
     *        │   BEAN           DEPENDENCY        BEAN           │
     *        │   REGISTRATION   INJECTION         LIFECYCLE      │
     *        │                                                   │
     *        │   @Component     @Autowired         @PostConstruct │
     *        │   @Service       @Qualifier         @PreDestroy    │
     *        │   @Repository    @Primary           @Lazy          │
     *        │   @Controller    @Value             @Scope         │
     *        │                                                   │
     *        └─────────────────────────────────────────────────┘
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║               SECTION 3: STEREOTYPE ANNOTATIONS — OVERVIEW                  ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 WHAT ARE STEREOTYPE ANNOTATIONS?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Stereotype annotations are CLASS-LEVEL annotations that tell Spring:
     *  "Hey Spring! This class is a bean — please manage it for me."
     *
     * They are all specializations of @Component:
     *
     *         @Component
     *         (The Root)
     *             │
     *    ┌────────┼────────┬────────────┐
     *    │        │        │            │
     * @Service @Repository @Controller @RestController
     * (Business) (Data)   (Web/MVC)  (Web/REST)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🗺️ WHERE EACH STEREOTYPE BELONGS (Layered Architecture):
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ┌────────────────────────────────────────────────┐
     *  │         PRESENTATION LAYER (Web / API)          │
     *  │     @Controller  /  @RestController             │
     *  │   Handles HTTP requests, returns responses      │
     *  └──────────────────┬─────────────────────────────┘
     *                     │ calls
     *  ┌──────────────────▼─────────────────────────────┐
     *  │              SERVICE LAYER                      │
     *  │            @Service                             │
     *  │   Contains business logic, orchestrates work   │
     *  └──────────────────┬─────────────────────────────┘
     *                     │ calls
     *  ┌──────────────────▼─────────────────────────────┐
     *  │         PERSISTENCE LAYER (Data Access)         │
     *  │           @Repository                           │
     *  │  Talks to database, translates exceptions       │
     *  └──────────────────┬─────────────────────────────┘
     *                     │ uses
     *  ┌──────────────────▼─────────────────────────────┐
     *  │                DATABASE                         │
     *  │           (H2, MySQL, PostgreSQL...)            │
     *  └────────────────────────────────────────────────┘
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 💡 WHY DIFFERENT STEREOTYPES INSTEAD OF JUST @COMPONENT?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Functional difference:
     *   @Component    → General purpose; no extra behaviour
     *   @Service      → Semantic clarity; future AOP interceptors on service layer
     *   @Repository   → Exception translation (DataAccessException wrapping)
     *   @Controller   → Signals Spring MVC to treat it as a request handler
     *
     * Developer readability:
     *   A new developer reading your code IMMEDIATELY understands the role of each class
     *   just by its annotation — no need to open the file!
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║           SECTION 4: DEPENDENCY INJECTION ANNOTATIONS — OVERVIEW            ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 THREE TYPES OF DEPENDENCY INJECTION IN SPRING:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * TYPE 1: CONSTRUCTOR INJECTION  ← ✅ RECOMMENDED
     *
     *     @Service
     *     class OrderService {
     *         private final PaymentService paymentService;
     *
     *         @Autowired                    // Optional in Spring 4.3+ if only 1 constructor
     *         OrderService(PaymentService ps) {
     *             this.paymentService = ps;
     *         }
     *     }
     *
     *   ✓ Immutable dependencies (final fields)
     *   ✓ All dependencies visible at a glance
     *   ✓ Easy to test without Spring context
     *   ✓ Fails fast on missing dependencies
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * TYPE 2: SETTER INJECTION  ← ⚠️ USE SPARINGLY
     *
     *     @Service
     *     class NotificationService {
     *         private EmailService emailService;
     *
     *         @Autowired
     *         public void setEmailService(EmailService emailService) {
     *             this.emailService = emailService;
     *         }
     *     }
     *
     *   ✓ Useful for optional dependencies
     *   ✓ Allows changing dependencies after construction
     *   ❌ Mutable (can accidentally change at runtime)
     *   ❌ Object can exist in incomplete state
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * TYPE 3: FIELD INJECTION  ← ❌ AVOID IN PRODUCTION
     *
     *     @Service
     *     class UserService {
     *         @Autowired
     *         private UserRepository userRepository;  // Direct field injection
     *     }
     *
     *   ✓ Concise (less boilerplate)
     *   ❌ Can't make field final → not immutable
     *   ❌ Hidden dependencies (not visible in constructor)
     *   ❌ Cannot be unit tested without Spring
     *   ❌ Fails silently (NullPointerException at runtime)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🏆 THE GOLDEN RULE: ALWAYS USE CONSTRUCTOR INJECTION
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Modern Spring + Lombok makes it elegant:
     *
     *     @Service
     *     @RequiredArgsConstructor          // Lombok generates constructor
     *     class OrderService {
     *         private final PaymentService paymentService;   // Lombok picks these up
     *         private final ShippingService shippingService;
     *         // Constructor is auto-generated by Lombok — zero boilerplate!
     *     }
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║               SECTION 5: BEAN SCOPE ANNOTATIONS — OVERVIEW                  ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 WHAT IS A BEAN SCOPE?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Scope = "How many instances of this bean does Spring create?"
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 SCOPE COMPARISON TABLE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ┌────────────────┬───────────────────────────────────────┬──────────────────┐
     *  │     SCOPE      │           INSTANCE CREATED            │  USE CASE        │
     *  ├────────────────┼───────────────────────────────────────┼──────────────────┤
     *  │  singleton     │ ONE per ApplicationContext (DEFAULT)   │ Services, Repos  │
     *  │  prototype     │ NEW for EVERY injection/getBean()      │ Stateful objects │
     *  │  request       │ ONE per HTTP request                   │ Request data     │
     *  │  session       │ ONE per HTTP session                   │ User session     │
     *  │  application   │ ONE per ServletContext                 │ App-wide state   │
     *  │  websocket     │ ONE per WebSocket session              │ Chat, live data  │
     *  └────────────────┴───────────────────────────────────────┴──────────────────┘
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * ⚠️ SINGLETON SCOPE (DEFAULT) — THE MOST IMPORTANT:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * By default ALL Spring beans are SINGLETONS. This means:
     *
     *   userService.getUserById(1)
     *      ↓
     *   UserService@2f5a6b4c  ← always the SAME object!
     *
     * This is fine as long as your beans are STATELESS.
     *
     * ⚠️ DANGER: Never store mutable state in singleton beans!
     *
     *     @Service // SINGLETON!
     *     class BadService {
     *         private List<String> requestData; // ← SHARED across ALL requests!
     *                                            // Race condition waiting to happen!
     *     }
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║           SECTION 6: BEAN LIFECYCLE ANNOTATIONS — OVERVIEW                  ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 THE COMPLETE BEAN LIFECYCLE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *   1. Spring scans and finds the class (@Component etc.)
     *        ↓
     *   2. BeanDefinition is registered in BeanFactory
     *        ↓
     *   3. Bean instance is CREATED (constructor called)
     *        ↓
     *   4. Dependencies are INJECTED (@Autowired, @Value)
     *        ↓
     *   5. ← @PostConstruct method is called here ←
     *        ↓
     *   6. Bean is READY — used by the application
     *        ↓
     *   7. Application is shutting down
     *        ↓
     *   8. ← @PreDestroy method is called here ←
     *        ↓
     *   9. Bean is DESTROYED and GC'd
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 QUICK REFERENCE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  @PostConstruct  → Run initialization AFTER injection (e.g., open connection pool)
     *  @PreDestroy     → Cleanup BEFORE destruction (e.g., close resources)
     *  @Lazy           → Don't create bean at startup; create it on first use
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                  SECTION 7: QUICK ANNOTATION REFERENCE CARD                 ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ┌────────────────────────────┬────────────────────────────────────────────────┐
     * │         ANNOTATION         │              ONE-LINE DESCRIPTION              │
     * ├────────────────────────────┼────────────────────────────────────────────────┤
     * │ @Component                 │ Generic Spring-managed bean                    │
     * │ @Service                   │ Bean in the business/service layer             │
     * │ @Repository                │ Bean in the data-access layer                  │
     * │ @Controller                │ Bean in the web/presentation layer             │
     * ├────────────────────────────┼────────────────────────────────────────────────┤
     * │ @Autowired                 │ Auto-inject a dependency by type               │
     * │ @Qualifier("name")         │ Inject the bean with this specific name        │
     * │ @Primary                   │ Prefer this bean when multiple match           │
     * │ @Value("${prop}")          │ Inject a value from properties / SpEL          │
     * ├────────────────────────────┼────────────────────────────────────────────────┤
     * │ @Scope("prototype")        │ New bean instance per injection                │
     * │ @RequestScope              │ New bean instance per HTTP request             │
     * │ @SessionScope              │ New bean instance per HTTP session             │
     * │ @ApplicationScope          │ New bean instance per application              │
     * ├────────────────────────────┼────────────────────────────────────────────────┤
     * │ @PostConstruct             │ Init method called after dependency injection   │
     * │ @PreDestroy                │ Cleanup method called before bean destruction  │
     * │ @Lazy                      │ Delay bean creation until first use            │
     * └────────────────────────────┴────────────────────────────────────────────────┘
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║             SECTION 8: BEST PRACTICES QUICK REFERENCE                       ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * DO's ✅
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ✅  Always prefer CONSTRUCTOR INJECTION over field injection
     *  ✅  Make injected fields FINAL for immutability
     *  ✅  Code to INTERFACES, not implementations
     *      (e.g., PaymentService interface, not StripePaymentService)
     *  ✅  Use the correct STEREOTYPE for semantic clarity
     *  ✅  Keep beans STATELESS for singleton scope
     *  ✅  Use @Lazy for expensive beans not needed at startup
     *  ✅  Always close resources in @PreDestroy
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * DON'Ts ❌
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  ❌  Don't use field injection in production code
     *  ❌  Don't store mutable request-specific data in singleton beans
     *  ❌  Don't call @PostConstruct method directly (it's called by Spring)
     *  ❌  Don't mix @Primary and @Qualifier — use one or the other
     *  ❌  Don't use new keyword to instantiate Spring beans
     *  ❌  Don't forget @Transactional on @Repository / @Service when needed
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║              SECTION 9: TOP INTERVIEW QUESTIONS — CHAPTER 2                 ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Q1: What is the difference between @Component, @Service, @Repository, @Controller?
     * A1: All are specializations of @Component. @Repository adds exception translation.
     *     @Controller enables MVC request mapping. @Service adds semantic meaning for the
     *     service layer but has no special behaviour beyond @Component currently.
     *
     * Q2: What are the different types of dependency injection in Spring?
     * A2: Constructor injection, setter injection, and field injection.
     *     Constructor injection is recommended because it ensures immutability and
     *     makes dependencies explicit.
     *
     * Q3: What is the default scope of a Spring bean?
     * A3: Singleton — one instance per ApplicationContext.
     *
     * Q4: When would you use prototype scope?
     * A4: When each injection site needs a fresh, independent instance — e.g., a
     *     stateful command object or a bean that holds per-request mutable state.
     *
     * Q5: What happens if two beans of the same type exist and @Autowired is used?
     * A5: Spring throws NoUniqueBeanDefinitionException. You must use @Qualifier or
     *     @Primary to resolve the ambiguity.
     *
     * Q6: What is @PostConstruct and when is it called?
     * A6: @PostConstruct marks an initialization method. It is called after Spring has
     *     created the bean and injected all dependencies, but before the bean is
     *     returned to the caller.
     *
     * Q7: What is the difference between @Primary and @Qualifier?
     * A7: @Primary sets a default bean to use when no explicit qualifier is given.
     *     @Qualifier explicitly names the bean to inject at a specific injection point.
     *     Both solve the same problem (ambiguity), but @Qualifier gives finer control.
     *
     * Q8: What is @Lazy and when should you use it?
     * A8: @Lazy defers bean creation until first use. Useful for expensive beans or to
     *     break circular dependency problems. Avoid overusing it — it can hide startup
     *     configuration errors.
     *
     * Q9: Can you inject a prototype-scoped bean into a singleton bean?
     * A9: Yes, but naively it won't work as expected — the prototype bean will only be
     *     created ONCE (when the singleton is created). Solutions: use @Lookup methods,
     *     ObjectFactory/ObjectProvider injection, or ApplicationContext.getBean().
     *
     * Q10: What is the difference between BeanFactory and ApplicationContext?
     * A10: BeanFactory is the basic IoC container. ApplicationContext extends it with:
     *      event publication, internationalization, AOP, annotation processing, and more.
     *      In Spring Boot you always use ApplicationContext (via SpringApplication.run).
     *
     */
}

