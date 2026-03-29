package com.learning.springboot.chapter01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║                  EXAMPLE 01: @SpringBootApplication IN ACTION                        ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example01SpringBootApplication.java
 * Purpose:     Demonstrate @SpringBootApplication annotation
 * Difficulty:  ⭐ Beginner
 * Time:        15 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                    @SpringBootApplication EXPLAINED                          ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @SpringBootApplication is a CONVENIENCE ANNOTATION that combines three key
 * annotations:
 *
 *  1. @Configuration      - Marks this class as a source of bean definitions
 *  2. @EnableAutoConfiguration - Enables Spring Boot's auto-configuration
 *  3. @ComponentScan      - Enables component scanning
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 WHAT HAPPENS WHEN YOU USE IT:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * STEP 1: Application Startup
 *     → SpringApplication.run() is called
 *     → Spring Boot starts initialization process
 *
 * STEP 2: Component Scanning
 *     → Scans current package and sub-packages
 *     → Finds all classes with @Component, @Service, @Repository, @Controller
 *     → Registers them as beans in the application context
 *
 * STEP 3: Auto-Configuration
 *     → Looks at classpath dependencies
 *     → Automatically configures beans (e.g., DataSource, Web MVC)
 *     → Uses sensible defaults
 *
 * STEP 4: Application Ready
 *     → All beans are created and initialized
 *     → Application context is fully loaded
 *     → Application is ready to serve requests
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 💡 KEY POINTS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  Place this annotation on your MAIN APPLICATION CLASS
 *  •  The class with main() method should have this annotation
 *  •  Component scanning starts from this package downwards
 *  •  Only ONE class per application should have this annotation
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */

@SpringBootApplication  // ← THE MOST IMPORTANT ANNOTATION IN SPRING BOOT!
public class Example01SpringBootApplication {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                         MAIN METHOD - APPLICATION ENTRY POINT                ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * The main() method is the ENTRY POINT of your Spring Boot application.
     * It uses SpringApplication.run() to bootstrap the application.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔍 WHAT DOES SpringApplication.run() DO?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * 1. Creates an ApplicationContext
     * 2. Registers command-line arguments as beans
     * 3. Loads all beans (via component scanning & auto-configuration)
     * 4. Calls CommandLineRunner and ApplicationRunner beans
     * 5. Starts embedded web server (if web application)
     * 6. Returns the ApplicationContext
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 💡 PARAMETERS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Parameter 1: Class<?>
     *     → The primary @SpringBootApplication annotated class
     *     → Used as the source for component scanning
     *
     * Parameter 2: String[] args
     *     → Command-line arguments passed to the application
     *     → Can be accessed as application properties
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 RETURN VALUE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Returns: ConfigurableApplicationContext
     *     → The fully initialized application context
     *     → Contains all beans and configurations
     *     → Can be used to interact with the application programmatically
     */
    public static void main(String[] args) {
        // Start the Spring Boot application
        ApplicationContext context = SpringApplication.run(Example01SpringBootApplication.class, args);

        // Let's explore what was automatically configured
        printApplicationInfo(context);
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                    HELPER METHOD - DISPLAY APPLICATION INFO                  ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * This method demonstrates how to interact with the ApplicationContext
     * to see what Spring Boot has automatically configured for us.
     */
    private static void printApplicationInfo(ApplicationContext context) {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                   ║");
        System.out.println("║           🎉 SPRING BOOT APPLICATION STARTED SUCCESSFULLY 🎉     ║");
        System.out.println("║                                                                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝\n");

        // Display application name
        String appName = context.getEnvironment().getProperty("spring.application.name", "Spring Boot App");
        System.out.println("📱 Application Name: " + appName);

        // Display active profiles
        String[] profiles = context.getEnvironment().getActiveProfiles();
        System.out.println("🔧 Active Profiles: " + (profiles.length > 0 ? String.join(", ", profiles) : "default"));

        // Count total beans
        int beanCount = context.getBeanDefinitionCount();
        System.out.println("🌱 Total Beans Registered: " + beanCount);

        // Display some key auto-configured beans
        System.out.println("\n💡 Some Key Auto-Configured Beans:");
        
        if (context.containsBean("dataSource")) {
            System.out.println("   ✓ DataSource (Database connection)");
        }
        
        if (context.containsBean("entityManagerFactory")) {
            System.out.println("   ✓ EntityManagerFactory (JPA/Hibernate)");
        }
        
        if (context.containsBean("dispatcherServlet")) {
            System.out.println("   ✓ DispatcherServlet (Web MVC)");
        }

        if (context.containsBean("jacksonObjectMapper")) {
            System.out.println("   ✓ ObjectMapper (JSON processing)");
        }

        System.out.println("\n📚 What @SpringBootApplication did for us:");
        System.out.println("   1. ✓ Scanned components in: " + Example01SpringBootApplication.class.getPackage().getName());
        System.out.println("   2. ✓ Auto-configured " + beanCount + " beans");
        System.out.println("   3. ✓ Started embedded web server (if applicable)");
        System.out.println("   4. ✓ Ready to handle requests!");

        System.out.println("\n🚀 Application is running...");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                       EXAMPLE REST CONTROLLER                                ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * This inner class demonstrates how @SpringBootApplication enables
     * component scanning. This controller will be automatically detected
     * and registered as a bean.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 HOW IT WORKS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * 1. @RestController is a stereotype annotation
     * 2. Component scanning (from @SpringBootApplication) finds it
     * 3. Spring creates a bean instance
     * 4. Maps HTTP requests to handler methods
     * 5. Automatically converts return values to JSON
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🌐 TEST IT:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Once the application is running, visit:
     *     http://localhost:8080/
     *     http://localhost:8080/info
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @RestController
    public static class DemoController {

        /**
         * Root endpoint - Simple welcome message
         *
         * URL: http://localhost:8080/
         * Method: GET
         * Response: Plain text message
         */
        @GetMapping("/")
        public String home() {
            return "🎉 Welcome to Spring Boot! \n\n" +
                   "This application is powered by @SpringBootApplication annotation.\n\n" +
                   "Try these endpoints:\n" +
                   "  • GET /        - This welcome message\n" +
                   "  • GET /info    - Application information\n";
        }

        /**
         * Info endpoint - Application details
         *
         * URL: http://localhost:8080/info
         * Method: GET
         * Response: JSON object with application info
         */
        @GetMapping("/info")
        public ApplicationInfo getInfo() {
            return new ApplicationInfo(
                "Spring Boot Application Example",
                "1.0.0",
                "Demonstrating @SpringBootApplication",
                new String[]{"@Configuration", "@EnableAutoConfiguration", "@ComponentScan"}
            );
        }
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                         DATA TRANSFER OBJECT (DTO)                           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Simple POJO to demonstrate JSON serialization.
     * Spring Boot's auto-configuration provides Jackson for JSON conversion.
     */
    public static class ApplicationInfo {
        private String name;
        private String version;
        private String description;
        private String[] annotations;

        // Constructor
        public ApplicationInfo(String name, String version, String description, String[] annotations) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.annotations = annotations;
        }

        // Getters (required for JSON serialization)
        public String getName() { return name; }
        public String getVersion() { return version; }
        public String getDescription() { return description; }
        public String[] getAnnotations() { return annotations; }
    }
}

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                                  📚 LEARNING SUMMARY 📚
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * ✅ WHAT WE LEARNED:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. @SpringBootApplication is the MAIN annotation for Spring Boot applications
 *  2. It combines @Configuration + @EnableAutoConfiguration + @ComponentScan
 *  3. SpringApplication.run() bootstraps the entire application
 *  4. Component scanning automatically finds and registers beans
 *  5. Auto-configuration sets up beans based on classpath
 *  6. The ApplicationContext contains all beans and configurations
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔍 KEY TAKEAWAYS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  Always place @SpringBootApplication on your main class
 *  •  The main() method should call SpringApplication.run()
 *  •  Component scanning starts from the package of the main class
 *  •  Spring Boot automatically configures beans based on dependencies
 *  •  No XML configuration required!
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🧪 EXERCISES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. Run this application and access the endpoints
 *  2. Add more REST endpoints to the DemoController
 *  3. Try excluding auto-configuration classes
 *  4. Experiment with different scan base packages
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ➡️  NEXT: Example02ConfigurationProperties.java
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

