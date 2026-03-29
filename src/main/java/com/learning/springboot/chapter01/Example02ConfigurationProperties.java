package com.learning.springboot.chapter01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║               EXAMPLE 02: @ConfigurationProperties IN ACTION                         ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example02ConfigurationProperties.java
 * Purpose:     Demonstrate @ConfigurationProperties annotation
 * Difficulty:  ⭐⭐ Beginner-Intermediate
 * Time:        20 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                   @ConfigurationProperties EXPLAINED                         ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @ConfigurationProperties is used to bind external configuration properties
 * (from application.properties or application.yml) to a Java object.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 WHY USE IT?
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * PROBLEMS WITH @Value:
 *
 *     @Value("${app.name}")
 *     private String appName;
 *
 *     @Value("${app.version}")
 *     private String version;
 *
 *     @Value("${app.timeout}")
 *     private int timeout;
 *
 * Issues:
 *  ❌ Scattered across multiple classes
 *  ❌ No type safety
 *  ❌ Hard to test
 *  ❌ No IDE autocomplete
 *  ❌ No validation support
 *
 * SOLUTION WITH @ConfigurationProperties:
 *
 *     @ConfigurationProperties(prefix = "app")
 *     public class AppProperties {
 *         private String name;
 *         private String version;
 *         private int timeout;
 *         // getters and setters
 *     }
 *
 * Benefits:
 *  ✓ Centralized configuration
 *  ✓ Type-safe
 *  ✓ Easy to test
 *  ✓ IDE autocomplete
 *  ✓ Validation support
 *  ✓ Nested properties support
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔧 HOW TO ENABLE:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * METHOD 1: Use @EnableConfigurationProperties
 *
 *     @SpringBootApplication
 *     @EnableConfigurationProperties(AppProperties.class)
 *     public class Application { }
 *
 * METHOD 2: Use @Component on the properties class
 *
 *     @Component
 *     @ConfigurationProperties(prefix = "app")
 *     public class AppProperties { }
 *
 * METHOD 3: Use @ConfigurationPropertiesScan (Spring Boot 2.2+)
 *
 *     @SpringBootApplication
 *     @ConfigurationPropertiesScan
 *     public class Application { }
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */

@SpringBootApplication
@EnableConfigurationProperties({
    Example02ConfigurationProperties.AppProperties.class,
    Example02ConfigurationProperties.DatabaseProperties.class
})
public class Example02ConfigurationProperties {

    public static void main(String[] args) {
        SpringApplication.run(Example02ConfigurationProperties.class, args);
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                  EXAMPLE 1: SIMPLE CONFIGURATION PROPERTIES                  ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * This class binds properties with prefix "app" from application.properties.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 APPLICATION.PROPERTIES EXAMPLE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *     # Application Properties
     *     app.name=My Spring Boot App
     *     app.version=1.0.0
     *     app.description=Learning @ConfigurationProperties
     *     app.timeout=5000
     *     app.max-retries=3
     *     app.enabled=true
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 KEY FEATURES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *  •  Prefix: "app" - all properties start with app.
     *  •  Automatic type conversion (String, int, boolean, etc.)
     *  •  Kebab-case to camelCase conversion (max-retries → maxRetries)
     *  •  IDE autocomplete support (with spring-boot-configuration-processor)
     */
    @ConfigurationProperties(prefix = "app")
    public static class AppProperties {

        // Simple string property
        private String name;

        // Version property
        private String version;

        // Description property
        private String description;

        // Integer property (milliseconds)
        private int timeout = 3000; // Default value

        // Integer property with kebab-case in properties file
        private int maxRetries = 5; // Default value

        // Boolean property
        private boolean enabled = true; // Default value

        // ─────────────────────────────────────────────────────────────────────────
        // GETTERS AND SETTERS (Required for property binding!)
        // ─────────────────────────────────────────────────────────────────────────

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public String toString() {
            return "AppProperties{" +
                   "name='" + name + '\'' +
                   ", version='" + version + '\'' +
                   ", description='" + description + '\'' +
                   ", timeout=" + timeout +
                   ", maxRetries=" + maxRetries +
                   ", enabled=" + enabled +
                   '}';
        }
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                 EXAMPLE 2: NESTED CONFIGURATION PROPERTIES                   ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * This class demonstrates NESTED properties and COLLECTIONS.
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 APPLICATION.PROPERTIES EXAMPLE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *     # Database Configuration
     *     database.url=jdbc:mysql://localhost:3306/mydb
     *     database.username=admin
     *     database.password=secret123
     *     database.driver-class-name=com.mysql.cj.jdbc.Driver
     *
     *     # Connection Pool Settings (Nested Object)
     *     database.pool.min-size=5
     *     database.pool.max-size=20
     *     database.pool.timeout=30000
     *
     *     # List of allowed hosts
     *     database.allowed-hosts[0]=192.168.1.1
     *     database.allowed-hosts[1]=192.168.1.2
     *     database.allowed-hosts[2]=localhost
     *
     *     # Map of properties
     *     database.properties.useSSL=true
     *     database.properties.serverTimezone=UTC
     *     database.properties.cachePrepStmts=true
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 OR USE YAML (application.yml):
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *     database:
     *       url: jdbc:mysql://localhost:3306/mydb
     *       username: admin
     *       password: secret123
     *       driver-class-name: com.mysql.cj.jdbc.Driver
     *       pool:
     *         min-size: 5
     *         max-size: 20
     *         timeout: 30000
     *       allowed-hosts:
     *         - 192.168.1.1
     *         - 192.168.1.2
     *         - localhost
     *       properties:
     *         useSSL: true
     *         serverTimezone: UTC
     *         cachePrepStmts: true
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @ConfigurationProperties(prefix = "database")
    public static class DatabaseProperties {

        // Simple properties
        private String url;
        private String username;
        private String password;
        private String driverClassName;

        // NESTED OBJECT - Pool configuration
        private PoolConfig pool = new PoolConfig();

        // LIST - Allowed hosts
        private List<String> allowedHosts;

        // MAP - Additional properties
        private Map<String, String> properties;

        // ─────────────────────────────────────────────────────────────────────────
        // NESTED CLASS FOR POOL CONFIGURATION
        // ─────────────────────────────────────────────────────────────────────────

        public static class PoolConfig {
            private int minSize = 5;
            private int maxSize = 10;
            private int timeout = 30000;

            // Getters and Setters
            public int getMinSize() {
                return minSize;
            }

            public void setMinSize(int minSize) {
                this.minSize = minSize;
            }

            public int getMaxSize() {
                return maxSize;
            }

            public void setMaxSize(int maxSize) {
                this.maxSize = maxSize;
            }

            public int getTimeout() {
                return timeout;
            }

            public void setTimeout(int timeout) {
                this.timeout = timeout;
            }

            @Override
            public String toString() {
                return "PoolConfig{" +
                       "minSize=" + minSize +
                       ", maxSize=" + maxSize +
                       ", timeout=" + timeout +
                       '}';
            }
        }

        // ─────────────────────────────────────────────────────────────────────────
        // GETTERS AND SETTERS
        // ─────────────────────────────────────────────────────────────────────────

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public PoolConfig getPool() {
            return pool;
        }

        public void setPool(PoolConfig pool) {
            this.pool = pool;
        }

        public List<String> getAllowedHosts() {
            return allowedHosts;
        }

        public void setAllowedHosts(List<String> allowedHosts) {
            this.allowedHosts = allowedHosts;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

        @Override
        public String toString() {
            return "DatabaseProperties{" +
                   "url='" + url + '\'' +
                   ", username='" + username + '\'' +
                   ", password='***'" +
                   ", driverClassName='" + driverClassName + '\'' +
                   ", pool=" + pool +
                   ", allowedHosts=" + allowedHosts +
                   ", properties=" + properties +
                   '}';
        }
    }

    /**
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║                    REST CONTROLLER TO DISPLAY PROPERTIES                     ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * This controller demonstrates how to use @ConfigurationProperties
     * in your application code.
     */
    @RestController
    public static class PropertiesController {

        // Inject configuration properties via constructor
        private final AppProperties appProperties;
        private final DatabaseProperties databaseProperties;

        public PropertiesController(AppProperties appProperties, 
                                   DatabaseProperties databaseProperties) {
            this.appProperties = appProperties;
            this.databaseProperties = databaseProperties;
        }

        /**
         * Display application properties
         * URL: http://localhost:8080/properties/app
         */
        @GetMapping("/properties/app")
        public AppProperties getAppProperties() {
            return appProperties;
        }

        /**
         * Display database properties
         * URL: http://localhost:8080/properties/database
         */
        @GetMapping("/properties/database")
        public DatabaseProperties getDatabaseProperties() {
            return databaseProperties;
        }

        /**
         * Display all properties as formatted text
         * URL: http://localhost:8080/properties/all
         */
        @GetMapping("/properties/all")
        public String getAllProperties() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== APPLICATION PROPERTIES ===\n\n");
            sb.append(appProperties.toString()).append("\n\n");
            sb.append("=== DATABASE PROPERTIES ===\n\n");
            sb.append(databaseProperties.toString()).append("\n");
            return sb.toString();
        }
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
 *  1. @ConfigurationProperties binds external properties to Java objects
 *  2. It provides TYPE-SAFE configuration management
 *  3. Supports simple properties, nested objects, lists, and maps
 *  4. Kebab-case in properties converts to camelCase in Java
 *  5. Default values can be set in the Java class
 *  6. Must use @EnableConfigurationProperties or @Component to enable
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔍 KEY BENEFITS OVER @Value:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  ✓  CENTRALIZED: All related properties in one class
 *  ✓  TYPE-SAFE: Compile-time type checking
 *  ✓  TESTABLE: Easy to mock and test
 *  ✓  ORGANIZED: Nested structure for complex configuration
 *  ✓  VALIDATED: Can use JSR-303 validation annotations
 *  ✓  DOCUMENTED: Properties documented in the class
 *  ✓  IDE SUPPORT: Autocomplete with configuration processor
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 💡 BEST PRACTICES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. Use meaningful prefix names (e.g., "app", "database", "mail")
 *  2. Provide default values for optional properties
 *  3. Use kebab-case in properties files (my-property)
 *  4. Group related properties in nested objects
 *  5. Add spring-boot-configuration-processor dependency for IDE support
 *  6. Use @Validated for property validation
 *  7. Don't inject @ConfigurationProperties in constructors of @Configuration
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🧪 EXERCISES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. Create application.properties and add the properties
 *  2. Run the application and access the endpoints
 *  3. Try changing values and see live updates (with DevTools)
 *  4. Create your own ConfigurationProperties class
 *  5. Add validation annotations (@NotNull, @Min, @Max)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ➡️  NEXT: Example03ConfigurationPropertiesScan.java
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

