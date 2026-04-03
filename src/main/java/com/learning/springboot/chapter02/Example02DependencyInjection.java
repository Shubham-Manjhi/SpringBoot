package com.learning.springboot.chapter02;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║           EXAMPLE 02: DEPENDENCY INJECTION ANNOTATIONS IN ACTION                     ║
 * ║           @Autowired  ·  @Qualifier  ·  @Primary  ·  @Value                          ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example02DependencyInjection.java
 * Purpose:     Master every form of dependency injection in Spring, from the simplest
 *              @Autowired to complex @Qualifier + @Primary disambiguation patterns.
 * Difficulty:  ⭐⭐ Beginner–Intermediate
 * Time:        30 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * SCENARIO: A notification system that can send messages via different channels
 *           (Email, SMS, Push). We will use this real-world scenario to demonstrate
 *           every DI annotation.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 1: @Autowired  — AUTOMATIC DEPENDENCY INJECTION
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                          @Autowired  EXPLAINED                               ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @Autowired tells Spring: "Please inject (wire) the appropriate bean here."
 * Spring looks in its ApplicationContext for a bean that matches the required type,
 * and injects it automatically — no need for new() or factory calls.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔄 HOW SPRING RESOLVES THE BEAN TO INJECT:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   STEP 1: Find beans matching the REQUIRED TYPE
 *             If exactly 1 found → inject it directly ✅
 *             If 0 found        → throw NoSuchBeanDefinitionException ❌
 *             If 2+ found       → move to Step 2
 *
 *   STEP 2: Among multiple candidates, look for @Primary
 *             If one bean is @Primary → inject it ✅
 *             Otherwise              → move to Step 3
 *
 *   STEP 3: Match by FIELD NAME / PARAMETER NAME
 *             If field name matches a bean name → inject it ✅
 *             Otherwise                         → throw NoUniqueBeanDefinitionException ❌
 *
 *   BONUS: Use @Qualifier to force a specific bean at injection point.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 THREE INJECTION STYLES (All use @Autowired):
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   1. CONSTRUCTOR INJECTION  ← ✅ Recommended
 *   2. SETTER INJECTION       ← ⚠️ Okay for optional dependencies
 *   3. FIELD INJECTION        ← ❌ Avoid in production
 *
 */

// ─────────────────────────────────────────────────────────────────────────────────────
// Notification Channel Interface — The CONTRACT all channels must follow
// ─────────────────────────────────────────────────────────────────────────────────────

/**
 * Contract for sending notifications.
 * Coding to this interface allows different channels to be swapped without
 * changing any calling code — the power of Dependency Injection!
 */
interface NotificationChannel {
    void send(String recipient, String message);
    String getChannelName();
}

// ─────────────────────────────────────────────────────────────────────────────────────
// Three concrete implementations of NotificationChannel
// ─────────────────────────────────────────────────────────────────────────────────────

/**
 * Email notification channel.
 */
@Component("emailChannel")   // ← Explicit bean name "emailChannel"
class EmailNotificationChannel implements NotificationChannel {

    @Override
    public void send(String recipient, String message) {
        System.out.printf("📧 [EMAIL] To: %s | Message: %s%n", recipient, message);
    }

    @Override
    public String getChannelName() { return "EMAIL"; }
}

/**
 * SMS notification channel.
 */
@Component("smsChannel")   // ← Explicit bean name "smsChannel"
class SmsNotificationChannel implements NotificationChannel {

    @Override
    public void send(String recipient, String message) {
        System.out.printf("📱 [SMS] To: %s | Message: %s%n", recipient, message);
    }

    @Override
    public String getChannelName() { return "SMS"; }
}

/**
 * Push notification channel.
 *
 * Note: This bean is annotated with @Primary — it will be injected by default
 * whenever a NotificationChannel is required and no @Qualifier is specified.
 * (Explained in detail in Section 3.)
 */
@Component("pushChannel")   // ← Explicit bean name "pushChannel"
@Primary                    // ← "I am the DEFAULT NotificationChannel"
class PushNotificationChannel implements NotificationChannel {

    @Override
    public void send(String recipient, String message) {
        System.out.printf("🔔 [PUSH] To: %s | Message: %s%n", recipient, message);
    }

    @Override
    public String getChannelName() { return "PUSH"; }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 2: STYLE 1 — CONSTRUCTOR INJECTION (RECOMMENDED ✅)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║              CONSTRUCTOR INJECTION — THE GOLD STANDARD                       ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 HOW IT WORKS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   Spring detects the constructor, looks up beans for each parameter,
 *   and calls the constructor with the resolved beans.
 *
 *   @Autowired is OPTIONAL if there is only one constructor (Spring 4.3+).
 *   But we include it explicitly here for learning purposes.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 💡 WHY CONSTRUCTOR INJECTION IS THE GOLD STANDARD:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  ✅  Fields can be final       → immutable, thread-safe
 *  ✅  Dependencies are explicit → readable, self-documenting
 *  ✅  Testable without Spring   → call new Service(mockDep) in unit tests
 *  ✅  Fails FAST on startup     → missing bean = startup failure (not NPE at runtime)
 *  ✅  No circular dependency    → Spring detects and reports immediately
 *
 */
@Service
class OrderNotificationService {

    // ✅ CONSTRUCTOR INJECTION:
    // 1. Fields are FINAL — once set, never change
    // 2. Dependencies injected at object creation time — never null
    private final NotificationChannel notificationChannel;  // Uses @Primary → PushNotificationChannel

    /*
     * @Autowired here is optional (Spring 4.3+, only one constructor).
     * Spring automatically detects the constructor and injects `notificationChannel`.
     *
     * Which bean is injected for NotificationChannel?
     *   → There are 3 candidates: emailChannel, smsChannel, pushChannel
     *   → pushChannel is annotated @Primary → it wins!
     *   → So pushChannel (PushNotificationChannel) is injected here.
     */
    @Autowired
    public OrderNotificationService(NotificationChannel notificationChannel) {
        this.notificationChannel = notificationChannel;
        System.out.println("✅ OrderNotificationService created — using channel: "
                + notificationChannel.getChannelName());
    }

    public void notifyOrderPlaced(String userId, String orderId) {
        String message = "Your order #" + orderId + " has been placed successfully!";
        notificationChannel.send(userId, message);
    }

    public void notifyOrderShipped(String userId, String orderId, String trackingNumber) {
        String message = "Order #" + orderId + " shipped! Track: " + trackingNumber;
        notificationChannel.send(userId, message);
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 3: STYLE 2 — SETTER INJECTION
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║              SETTER INJECTION — OPTIONAL DEPENDENCIES                        ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 WHEN TO USE SETTER INJECTION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  Optional dependencies (required = false allows bean to be null)
 *  •  Frameworks that require a no-arg constructor (e.g., some legacy frameworks)
 *  •  When you need to replace a dependency after construction (rare)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ CAUTION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  Fields cannot be final → object is mutable → use with caution
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Service
class AlertService {

    // Not final — can be null if @Autowired(required = false) doesn't find a bean
    private NotificationChannel alertChannel;

    /**
     * SETTER INJECTION — Spring calls this setter after creating the bean.
     *
     * required = false means:
     *   If no NotificationChannel bean exists → alertChannel stays null
     *   (the application still starts instead of throwing an exception)
     *
     * In this example, it WILL be injected because pushChannel (@Primary) exists.
     */
    @Autowired(required = false)
    public void setAlertChannel(NotificationChannel alertChannel) {
        this.alertChannel = alertChannel;
        System.out.println("✅ AlertService: alertChannel set via setter injection → "
                + (alertChannel != null ? alertChannel.getChannelName() : "null (optional)"));
    }

    public void sendAlert(String recipient, String alert) {
        if (alertChannel != null) {
            alertChannel.send(recipient, "[ALERT] " + alert);
        } else {
            System.out.println("⚠️ AlertService: No alert channel configured — alert dropped.");
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 4: STYLE 3 — FIELD INJECTION (FOR ILLUSTRATION — AVOID IN PRODUCTION ❌)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║              FIELD INJECTION — WHY TO AVOID IT                               ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 WHAT IS FIELD INJECTION?
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Spring uses REFLECTION to inject directly into private fields.
 * This bypasses normal Java access control — which is why many consider it a code smell.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ❌ PROBLEMS WITH FIELD INJECTION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. Cannot make fields final → no immutability guarantee
 *  2. Hidden dependencies — not visible without looking at the field
 *  3. Requires Spring to test — can't instantiate without the Spring container
 *  4. Fails silently — NullPointerException at runtime, not startup
 *  5. Harder to detect circular dependencies early
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ✅ THIS STYLE IS SHOWN HERE FOR EDUCATIONAL PURPOSES ONLY
 *    Use constructor injection in production code!
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Service
class MarketingService {

    /*
     * Field injection — Spring uses reflection to set this field
     * after instantiating the bean with the no-arg constructor.
     * @Primary annotation on PushNotificationChannel means it's injected here.
     */
    @Autowired
    private NotificationChannel notificationChannel;  // ← ❌ Bad practice in production

    public void sendPromotion(String userId, String promotion) {
        // ⚠️ If Spring context is not running, this.notificationChannel = null!
        // That's the hidden danger of field injection.
        notificationChannel.send(userId, "🎁 Promotion: " + promotion);
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 5: @Qualifier — INJECT A SPECIFIC BEAN BY NAME
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                          @Qualifier  EXPLAINED                               ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @Qualifier resolves ambiguity when multiple beans of the same type exist.
 * It specifies EXACTLY which bean to inject at a particular injection point.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔧 SYNTAX:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   @Autowired
 *   @Qualifier("beanName")     ← must match the bean's registered name
 *   private NotificationChannel channel;
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 WHEN TO USE:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  Multiple beans of the same type exist
 *  •  You need a specific bean at ONE injection point (not the default @Primary)
 *  •  Different features need different implementations of the same interface
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 💡 @Qualifier vs @Primary:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   @Primary  → "Use me by DEFAULT everywhere no qualifier is specified"
 *   @Qualifier → "At THIS specific injection point, use THIS specific bean"
 *
 *   They complement each other:
 *     @Primary  sets the default
 *     @Qualifier overrides the default at specific points
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Service
class MultiChannelNotificationService {

    private final NotificationChannel defaultChannel;  // @Primary → push
    private final NotificationChannel emailChannel;    // @Qualifier → explicitly email
    private final NotificationChannel smsChannel;      // @Qualifier → explicitly sms

    /*
     * CONSTRUCTOR INJECTION WITH @Qualifier:
     *
     * Parameter 1 (defaultChannel):
     *   → No @Qualifier → Spring picks the @Primary bean → pushChannel
     *
     * Parameter 2 (emailChannel):
     *   → @Qualifier("emailChannel") → Spring picks the bean named "emailChannel"
     *   → EmailNotificationChannel
     *
     * Parameter 3 (smsChannel):
     *   → @Qualifier("smsChannel") → Spring picks the bean named "smsChannel"
     *   → SmsNotificationChannel
     */
    @Autowired
    public MultiChannelNotificationService(
            NotificationChannel defaultChannel,                        // → @Primary = push

            @Qualifier("emailChannel")
            NotificationChannel emailChannel,                          // → explicitly email

            @Qualifier("smsChannel")
            NotificationChannel smsChannel) {                         // → explicitly sms

        this.defaultChannel = defaultChannel;
        this.emailChannel   = emailChannel;
        this.smsChannel     = smsChannel;

        System.out.println("✅ MultiChannelNotificationService created:");
        System.out.println("   default = " + defaultChannel.getChannelName());
        System.out.println("   email   = " + emailChannel.getChannelName());
        System.out.println("   sms     = " + smsChannel.getChannelName());
    }

    /**
     * Send via the default channel (pushChannel due to @Primary).
     */
    public void sendDefault(String recipient, String message) {
        System.out.println("📤 Sending via DEFAULT channel...");
        defaultChannel.send(recipient, message);
    }

    /**
     * Send a formal notification via email (regardless of @Primary).
     */
    public void sendEmail(String recipient, String message) {
        System.out.println("📤 Sending via EMAIL channel (forced by @Qualifier)...");
        emailChannel.send(recipient, message);
    }

    /**
     * Send an urgent notification via SMS.
     */
    public void sendSms(String recipient, String message) {
        System.out.println("📤 Sending via SMS channel (forced by @Qualifier)...");
        smsChannel.send(recipient, message);
    }

    /**
     * Broadcast to ALL channels.
     */
    public void broadcast(String recipient, String message) {
        System.out.println("📡 Broadcasting to ALL channels...");
        defaultChannel.send(recipient, message);  // push
        emailChannel.send(recipient, message);     // email
        smsChannel.send(recipient, message);       // sms
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 6: @Primary — SET THE DEFAULT BEAN
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                           @Primary  EXPLAINED                                ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * When multiple beans of the same type exist, @Primary designates one as the
 * DEFAULT — the one Spring should prefer in the absence of an explicit @Qualifier.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔧 HOW IT IS USED:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Already demonstrated on PushNotificationChannel above:
 *
 *     @Component("pushChannel")
 *     @Primary                     ← "I am the default NotificationChannel"
 *     class PushNotificationChannel implements NotificationChannel { ... }
 *
 * And without @Qualifier → pushChannel is always selected:
 *
 *     @Autowired
 *     private NotificationChannel channel;  // Gets PushNotificationChannel
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 💡 @Primary CAN ALSO BE ON @Bean METHODS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *     @Configuration
 *     class AppConfig {
 *
 *         @Bean
 *         @Primary                          ← production datasource is default
 *         DataSource prodDataSource() { ... }
 *
 *         @Bean
 *         DataSource testDataSource() { ... }
 *     }
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ ONLY ONE @Primary per type is allowed.
 * Having two @Primary beans of the same type → startup exception.
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @Primary in @Configuration / @Bean context:
 */
@Configuration
class NotificationConfig {

    /*
     * Example: defining beans manually with @Bean.
     * @Primary makes the fast logger the default.
     *
     * This pattern is used when:
     *   • The bean class is in a third-party library (you can't add @Primary to it)
     *   • You want to conditionally change the primary bean without editing the class
     */
    @Bean("consoleLogger")
    @Primary                          // ← Default logger is the console logger
    public NotificationLogger consoleNotificationLogger() {
        return message -> System.out.println("🖥️  [CONSOLE LOG] " + message);
    }

    @Bean("fileLogger")
    public NotificationLogger fileNotificationLogger() {
        return message -> System.out.println("📄 [FILE LOG] " + message);
    }
}

/** Functional interface for logging notifications. */
@FunctionalInterface
interface NotificationLogger {
    void log(String message);
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 7: @Value — INJECT PROPERTIES AND SpEL EXPRESSIONS
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                            @Value  EXPLAINED                                 ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @Value injects a VALUE into a field, constructor parameter, or method parameter.
 * It can inject:
 *   1. PROPERTY VALUES  from application.properties / application.yml
 *   2. SpEL EXPRESSIONS (Spring Expression Language) — computed at startup
 *   3. LITERALS         (constant values, for documentation/default purposes)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📝 SYNTAX REFERENCE:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   @Value("${property.key}")               ← Property placeholder
 *   @Value("${property.key:defaultValue}")  ← Property with default
 *   @Value("#{SpEL expression}")            ← Spring Expression Language
 *   @Value("literal string")               ← Hard-coded literal
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 PROPERTIES IN application.yml:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * notification:
 *   default-sender: noreply@example.com
 *   max-retries: 3
 *   enabled: true
 *   supported-channels:
 *     - EMAIL
 *     - SMS
 *     - PUSH
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Component
class NotificationSettings {

    // ─────────────────────────────────────────────────────────────────────────────
    // PATTERN 1: Simple property injection
    //
    //  Syntax: @Value("${property.key}")
    //  Reads the value of "notification.default-sender" from application.yml/properties.
    //  If the property is missing → BeanCreationException at startup.
    // ─────────────────────────────────────────────────────────────────────────────
    @Value("${notification.default-sender:noreply@example.com}")
    private String defaultSender;  // Injected from application.yml, or "noreply@example.com" if missing

    // ─────────────────────────────────────────────────────────────────────────────
    // PATTERN 2: Property with DEFAULT VALUE
    //
    //  Syntax: @Value("${property.key:defaultValue}")
    //  The colon (:) separates the property key from its default.
    //  If "notification.max-retries" is not in application.yml, uses 3.
    // ─────────────────────────────────────────────────────────────────────────────
    @Value("${notification.max-retries:3}")
    private int maxRetries;        // e.g., 3

    // ─────────────────────────────────────────────────────────────────────────────
    // PATTERN 3: Boolean property
    //
    //  Spring automatically converts the String "true" / "false" to boolean.
    // ─────────────────────────────────────────────────────────────────────────────
    @Value("${notification.enabled:true}")
    private boolean enabled;

    // ─────────────────────────────────────────────────────────────────────────────
    // PATTERN 4: SpEL — Mathematical / logical expressions
    //
    //  Syntax: @Value("#{expression}")
    //  Spring evaluates the expression at runtime.
    //  T(class) is SpEL syntax to reference a Java class.
    // ─────────────────────────────────────────────────────────────────────────────
    @Value("#{3 * 1000}")          // Evaluated: 3000 ms = 3 seconds timeout
    private long timeoutMs;

    // ─────────────────────────────────────────────────────────────────────────────
    // PATTERN 5: SpEL — Access system properties
    // ─────────────────────────────────────────────────────────────────────────────
    @Value("#{systemProperties['user.name']}")
    private String osUsername;     // Injected from OS user.name system property

    // ─────────────────────────────────────────────────────────────────────────────
    // PATTERN 6: SpEL — Access environment variables
    // ─────────────────────────────────────────────────────────────────────────────
    @Value("#{systemEnvironment['HOME'] ?: '/home/unknown'}")
    private String homeDirectory;  // OS $HOME variable with fallback

    // ─────────────────────────────────────────────────────────────────────────────
    // PATTERN 7: List injection from comma-separated property
    //
    //  application.yml: notification.channels: EMAIL,SMS,PUSH
    //
    //  @Value with SpEL splits the comma-separated string into a List<String>
    // ─────────────────────────────────────────────────────────────────────────────
    @Value("${notification.channels:EMAIL,SMS,PUSH}")
    private String rawChannels;    // "EMAIL,SMS,PUSH"  (raw String)

    // ─────────────────────────────────────────────────────────────────────────────
    // PATTERN 8: @Value ON CONSTRUCTOR PARAMETER (constructor injection + value)
    // ─────────────────────────────────────────────────────────────────────────────

    // appVersion is set via the constructor (Pattern 8 demo below in method)

    private final String appVersion;

    /**
     * Constructor injection with @Value.
     *
     * @Value on a constructor parameter injects a property value into
     * the constructor argument. Combines the benefits of constructor injection
     * with property injection.
     *
     * @param appVersion the application version from application.yml
     */
    @Autowired
    public NotificationSettings(
            @Value("${spring.application.name:SpringBootApp}")
            String appVersion) {
        this.appVersion = appVersion;
        System.out.println("✅ NotificationSettings created — app=" + appVersion);
    }

    /**
     * Display all injected values for demonstration.
     */
    public void printSettings() {
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║         NOTIFICATION SETTINGS (@Value Injection)     ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.printf ("║  defaultSender  : %-34s║%n", defaultSender);
        System.out.printf ("║  maxRetries     : %-34d║%n", maxRetries);
        System.out.printf ("║  enabled        : %-34b║%n", enabled);
        System.out.printf ("║  timeoutMs      : %-34d║%n", timeoutMs);
        System.out.printf ("║  osUsername     : %-34s║%n", osUsername);
        System.out.printf ("║  homeDirectory  : %-34s║%n", homeDirectory);
        System.out.printf ("║  rawChannels    : %-34s║%n", rawChannels);
        System.out.printf ("║  appVersion     : %-34s║%n", appVersion);
        System.out.println("╚══════════════════════════════════════════════════════╝\n");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ⚠️ COMMON @Value PITFALLS:
    // ─────────────────────────────────────────────────────────────────────────────
    //
    // PITFALL 1: Missing property without default → startup exception
    //   @Value("${property.that.does.not.exist}")  ← ❌ throws at startup
    //   @Value("${property.that.does.not.exist:}")  ← ✅ injects empty string as default
    //
    // PITFALL 2: Wrong placeholder syntax
    //   @Value("property.key")          ← ❌ injects the literal string "property.key"
    //   @Value("${property.key}")       ← ✅ injects the PROPERTY VALUE
    //
    // PITFALL 3: SpEL vs Property placeholder confusion
    //   @Value("${1 + 2}")              ← ❌ looks for property named "1 + 2"
    //   @Value("#{1 + 2}")              ← ✅ evaluates SpEL expression → 3
    //
    // PITFALL 4: @Value on @Configuration classes with CGLIB proxy
    //   If a @Configuration class uses @Value on @Bean method parameters, prefer
    //   @ConfigurationProperties for type-safe binding (see Chapter 1 Example 02).
    // ─────────────────────────────────────────────────────────────────────────────

    // Getters for use in other components
    public String getDefaultSender() { return defaultSender; }
    public int getMaxRetries()        { return maxRetries; }
    public boolean isEnabled()        { return enabled; }
    public long getTimeoutMs()        { return timeoutMs; }
    public List<String> getChannels() {
        return List.of(rawChannels.split(","));
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 8: COMPLETE WIRING PICTURE
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║            DEPENDENCY INJECTION — THE COMPLETE PICTURE                       ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 *
 * ─── BEANS REGISTERED IN APPLICATION CONTEXT ───────────────────────────────────
 *
 *  emailChannel        (EmailNotificationChannel)
 *  smsChannel          (SmsNotificationChannel)
 *  pushChannel         (PushNotificationChannel)  ← @Primary
 *  orderNotificationService  ← injects NotificationChannel → pushChannel (@Primary)
 *  alertService              ← setter injects NotificationChannel → pushChannel (@Primary)
 *  marketingService          ← field injects NotificationChannel → pushChannel (@Primary)
 *  multiChannelService       ← constructor injects ALL THREE via @Qualifier
 *  notificationSettings      ← @Value injects properties
 *  consoleLogger             ← @Primary from @Configuration class
 *  fileLogger                ← secondary logger
 *
 * ─── @Autowired RESOLUTION FLOW ────────────────────────────────────────────────
 *
 *   @Autowired NotificationChannel (no @Qualifier)
 *       Step 1: Find by type → 3 candidates found
 *       Step 2: Check @Primary → pushChannel is @Primary → INJECT pushChannel ✅
 *
 *   @Autowired @Qualifier("emailChannel") NotificationChannel
 *       Step 1: Find by type → 3 candidates found
 *       Step 2: Check @Primary → but @Qualifier overrides @Primary
 *       Step 3: Find bean named "emailChannel" → INJECT emailChannel ✅
 *
 * ─── KEY TAKEAWAYS ──────────────────────────────────────────────────────────────
 *
 *  ✓  @Autowired = "Spring, please inject the right bean here"
 *  ✓  @Qualifier = "No, not the default — give me THIS specific one"
 *  ✓  @Primary   = "I am the go-to bean when no qualifier is specified"
 *  ✓  @Value     = "Read this from properties or evaluate this expression"
 *  ✓  Always prefer CONSTRUCTOR injection
 *  ✓  Code to INTERFACES, not implementations
 *
 */
class Example02DependencyInjection {
    // Intentionally empty — documentation class
}

