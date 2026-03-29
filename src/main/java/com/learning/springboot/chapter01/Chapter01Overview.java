package com.learning.springboot.chapter01;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║                    SPRING BOOT ANNOTATIONS - COMPREHENSIVE GUIDE                     ║
 * ║                         Chapter 1: Core Spring Boot Annotations                      ║
 * ║                                                                                       ║
 * ║                           📚 From Zero to Expert 📚                                  ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Chapter:     1
 * Title:       Core Spring Boot Annotations
 * Difficulty:  ⭐ Beginner
 * Estimated:   3-5 hours
 * Prerequisites: Basic Java knowledge, understanding of dependency injection concepts
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │                                                                                       │
 * │                         CHAPTER 1: OVERVIEW & LEARNING GOALS                         │
 * │                                                                                       │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                                   📖 TABLE OF CONTENTS 📖
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * Section 1:  Chapter Introduction & Overview
 * Section 2:  What Are Core Spring Boot Annotations?
 * Section 3:  @SpringBootApplication - The Foundation
 * Section 4:  @EnableAutoConfiguration - Magic Behind the Scenes
 * Section 5:  @SpringBootConfiguration - Configuration Backbone
 * Section 6:  @ConfigurationProperties - Type-Safe Properties
 * Section 7:  @ConfigurationPropertiesScan - Automatic Discovery
 * Section 8:  Real-World Example - Complete Application
 * Section 9:  How Everything Works Together
 * Section 10: Best Practices & Common Pitfalls
 * Section 11: Interview Questions & Answers
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

public class Chapter01Overview {

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
     *  ✓  Understand the purpose of each core Spring Boot annotation
     *  ✓  Use @SpringBootApplication to bootstrap applications
     *  ✓  Configure auto-configuration with @EnableAutoConfiguration
     *  ✓  Create type-safe configuration with @ConfigurationProperties
     *  ✓  Scan and register configuration properties automatically
     *  ✓  Build a complete Spring Boot application from scratch
     *  ✓  Debug and troubleshoot common configuration issues
     *  ✓  Answer interview questions with confidence
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🌟 WHY ARE THESE ANNOTATIONS IMPORTANT?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Core Spring Boot annotations are the FOUNDATION of every Spring Boot application.
     * They provide:
     *
     * 1. SIMPLIFIED CONFIGURATION
     *     → Replace hundreds of lines of XML with a single annotation
     *     → Automatic bean detection and registration
     *     → Convention over configuration approach
     *
     * 2. AUTO-CONFIGURATION
     *     → Automatically configure beans based on classpath
     *     → Intelligent defaults that can be overridden
     *     → Reduce boilerplate code by 80%+
     *
     * 3. TYPE-SAFE PROPERTIES
     *     → Bind external properties to Java objects
     *     → Compile-time type checking
     *     → IDE auto-completion support
     *
     * 4. RAPID DEVELOPMENT
     *     → Get applications running in minutes, not hours
     *     → Focus on business logic, not infrastructure
     *     → Production-ready features out of the box
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📚 WHAT YOU'LL BUILD:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Throughout this chapter, we'll build a complete Spring Boot application that:
     *
     *  •  Uses @SpringBootApplication for bootstrapping
     *  •  Leverages auto-configuration for database and web
     *  •  Implements type-safe configuration properties
     *  •  Demonstrates all core annotations in action
     *  •  Follows industry best practices
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║              SECTION 2: WHAT ARE CORE SPRING BOOT ANNOTATIONS?              ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔍 DEFINITION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Core Spring Boot annotations are a set of specialized annotations that provide
     * the fundamental building blocks for Spring Boot applications. They enable:
     *
     *  •  Application bootstrapping
     *  •  Automatic configuration
     *  •  Component scanning
     *  •  Property binding
     *  •  Configuration management
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📊 THE BIG FIVE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * 1. @SpringBootApplication
     *     Purpose:   Main entry point for Spring Boot applications
     *     Function:  Combines @Configuration + @EnableAutoConfiguration + @ComponentScan
     *     Usage:     Applied to the main application class
     *     Frequency: ALWAYS (every Spring Boot app has this)
     *
     * 2. @EnableAutoConfiguration
     *     Purpose:   Enables Spring Boot's auto-configuration mechanism
     *     Function:  Automatically configures beans based on classpath
     *     Usage:     Usually part of @SpringBootApplication
     *     Frequency: VERY HIGH (automatic via @SpringBootApplication)
     *
     * 3. @SpringBootConfiguration
     *     Purpose:   Indicates a configuration class for Spring Boot
     *     Function:  Specialized version of @Configuration
     *     Usage:     Usually part of @SpringBootApplication
     *     Frequency: HIGH (automatic via @SpringBootApplication)
     *
     * 4. @ConfigurationProperties
     *     Purpose:   Binds external properties to Java objects
     *     Function:  Type-safe configuration property mapping
     *     Usage:     Applied to configuration classes
     *     Frequency: HIGH (used in most production applications)
     *
     * 5. @ConfigurationPropertiesScan
     *     Purpose:   Scans for @ConfigurationProperties classes
     *     Function:  Automatic detection and registration
     *     Usage:     Applied to main application or configuration class
     *     Frequency: MEDIUM (optional but recommended)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎨 HOW THEY WORK TOGETHER:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *                     @SpringBootApplication
     *                              │
     *          ┌───────────────────┼───────────────────┐
     *          │                   │                   │
     *          ▼                   ▼                   ▼
     *   @Configuration   @EnableAutoConfiguration  @ComponentScan
     *          │                   │                   │
     *          │                   │                   └──→ Finds components
     *          │                   │
     *          │                   └──→ Auto-configures beans
     *          │
     *          └──→ @ConfigurationProperties
     *                      │
     *                      └──→ @ConfigurationPropertiesScan
     *
     * FLOW:
     * 1. Application starts with @SpringBootApplication
     * 2. Component scanning finds all @Component classes
     * 3. Auto-configuration sets up beans automatically
     * 4. Configuration properties are bound to objects
     * 5. Application context is fully initialized
     * 6. Application is ready to serve requests
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║              SECTION 3: @SpringBootApplication - THE FOUNDATION              ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 WHAT IS @SpringBootApplication?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @SpringBootApplication is a CONVENIENCE ANNOTATION that combines three essential
     * annotations into one. It's the single most important annotation in Spring Boot.
     *
     * EQUIVALENT TO:
     *     @Configuration
     *     @EnableAutoConfiguration
     *     @ComponentScan
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 KEY ATTRIBUTES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * 1. exclude (Class<?>[])
     *     → Exclude specific auto-configuration classes
     *     → Example: exclude = {DataSourceAutoConfiguration.class}
     *
     * 2. excludeName (String[])
     *     → Exclude auto-configuration by class name
     *     → Example: excludeName = "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
     *
     * 3. scanBasePackages (String[])
     *     → Specify packages to scan for components
     *     → Example: scanBasePackages = {"com.example.app", "com.example.shared"}
     *
     * 4. scanBasePackageClasses (Class<?>[])
     *     → Specify classes whose packages should be scanned
     *     → Example: scanBasePackageClasses = {MyService.class}
     *
     * 5. proxyBeanMethods (boolean)
     *     → Whether @Bean methods should be proxied
     *     → Default: true
     *     → Set to false for performance in some cases
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 💡 WHY USE IT?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * BEFORE @SpringBootApplication (Old Spring):
     *
     *     @Configuration
     *     @EnableAutoConfiguration
     *     @ComponentScan(basePackages = "com.example")
     *     public class Application {
     *         public static void main(String[] args) {
     *             SpringApplication.run(Application.class, args);
     *         }
     *     }
     *
     * AFTER @SpringBootApplication (Modern Spring Boot):
     *
     *     @SpringBootApplication
     *     public class Application {
     *         public static void main(String[] args) {
     *             SpringApplication.run(Application.class, args);
     *         }
     *     }
     *
     * BENEFITS:
     *  ✓  Less verbose - 1 annotation instead of 3
     *  ✓  Standard convention - everyone uses it
     *  ✓  Less room for error - pre-configured defaults
     *  ✓  Better readability - clear intent
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📝 USAGE PATTERNS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * PATTERN 1: Basic Usage (Most Common)
     * ────────────────────────────────────────
     *
     *     @SpringBootApplication
     *     public class MyApplication {
     *         public static void main(String[] args) {
     *             SpringApplication.run(MyApplication.class, args);
     *         }
     *     }
     *
     * PATTERN 2: With Exclusions
     * ────────────────────────────────────────
     *
     *     @SpringBootApplication(exclude = {
     *         DataSourceAutoConfiguration.class,
     *         HibernateJpaAutoConfiguration.class
     *     })
     *     public class MyApplication {
     *         // ... When you don't need database auto-configuration
     *     }
     *
     * PATTERN 3: Custom Component Scanning
     * ────────────────────────────────────────
     *
     *     @SpringBootApplication(scanBasePackages = {
     *         "com.example.app",
     *         "com.example.shared"
     *     })
     *     public class MyApplication {
     *         // ... When components are in different packages
     *     }
     *
     * PATTERN 4: Performance Optimization
     * ────────────────────────────────────────
     *
     *     @SpringBootApplication(proxyBeanMethods = false)
     *     public class MyApplication {
     *         // ... For faster startup time (Spring Boot 2.2+)
     *     }
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║         SECTION 4: @EnableAutoConfiguration - MAGIC BEHIND THE SCENES        ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔮 WHAT IS AUTO-CONFIGURATION?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * Auto-configuration is Spring Boot's INTELLIGENT CONFIGURATION mechanism that
     * automatically sets up beans based on:
     *
     *  •  JAR dependencies in the classpath
     *  •  Beans already defined in the context
     *  •  Properties in application.properties/yml
     *  •  System properties and environment variables
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎯 HOW IT WORKS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * STEP 1: Application Startup
     * ────────────────────────────
     *     → @EnableAutoConfiguration is processed
     *     → Spring Boot looks for META-INF/spring.factories files
     *     → Loads list of auto-configuration classes
     *
     * STEP 2: Conditional Evaluation
     * ────────────────────────────
     *     → Each auto-configuration class has conditions
     *     → Conditions are evaluated (@ConditionalOnClass, @ConditionalOnBean, etc.)
     *     → Only matching configurations are applied
     *
     * STEP 3: Bean Registration
     * ────────────────────────────
     *     → Matching auto-configurations register beans
     *     → Beans are created with sensible defaults
     *     → User-defined beans take precedence
     *
     * STEP 4: Application Ready
     * ────────────────────────────
     *     → All beans are initialized
     *     → Application context is ready
     *     → Application can handle requests
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 💡 REAL-WORLD EXAMPLES:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * EXAMPLE 1: Database Auto-Configuration
     * ───────────────────────────────────────
     *
     * When you add this dependency:
     *     <dependency>
     *         <groupId>org.springframework.boot</groupId>
     *         <artifactId>spring-boot-starter-data-jpa</artifactId>
     *     </dependency>
     *
     * Spring Boot automatically configures:
     *  ✓  DataSource bean
     *  ✓  EntityManagerFactory
     *  ✓  JPA repositories
     *  ✓  Transaction manager
     *  ✓  H2 embedded database (if no other DB is configured)
     *
     * EXAMPLE 2: Web Auto-Configuration
     * ───────────────────────────────────────
     *
     * When you add this dependency:
     *     <dependency>
     *         <groupId>org.springframework.boot</groupId>
     *         <artifactId>spring-boot-starter-web</artifactId>
     *     </dependency>
     *
     * Spring Boot automatically configures:
     *  ✓  Embedded Tomcat server
     *  ✓  DispatcherServlet
     *  ✓  Jackson for JSON
     *  ✓  HTTP message converters
     *  ✓  Error page handlers
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🎛️ CONTROLLING AUTO-CONFIGURATION:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * METHOD 1: Exclude Specific Auto-Configurations
     * ───────────────────────────────────────────────
     *
     *     @SpringBootApplication(exclude = {
     *         DataSourceAutoConfiguration.class
     *     })
     *
     * METHOD 2: Using Properties File
     * ───────────────────────────────────────────────
     *
     *     # application.properties
     *     spring.autoconfigure.exclude=\
     *       org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
     *
     * METHOD 3: Custom Conditional Beans
     * ───────────────────────────────────────────────
     *
     *     @Bean
     *     @ConditionalOnMissingBean
     *     public DataSource dataSource() {
     *         // Your custom DataSource
     *         // Auto-configuration won't create one
     *     }
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║        SECTION 5: @SpringBootConfiguration - CONFIGURATION BACKBONE          ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🔍 WHAT IS @SpringBootConfiguration?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * @SpringBootConfiguration is a SPECIALIZED form of @Configuration that indicates
     * a class provides Spring Boot-specific configuration.
     *
     * KEY DIFFERENCES from @Configuration:
     *
     *  1. SINGLE CONFIGURATION
     *      → Only ONE @SpringBootConfiguration allowed per application
     *      → @Configuration can be used multiple times
     *
     *  2. TESTING SUPPORT
     *      → Used by @SpringBootTest to find configuration
     *      → Helps test slicing mechanisms
     *
     *  3. SEMANTIC MEANING
     *      → Indicates this is the PRIMARY configuration
     *      → Documents the application's main entry point
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 💡 WHEN TO USE:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * TYPICAL USAGE:
     *  •  Automatically included in @SpringBootApplication
     *  •  Don't use it directly - let @SpringBootApplication handle it
     *  •  Only use directly in special testing scenarios
     *
     * CORRECT:
     *     @SpringBootApplication  // Includes @SpringBootConfiguration
     *     public class MyApplication { }
     *
     * INCORRECT (Don't do this):
     *     @SpringBootConfiguration
     *     @EnableAutoConfiguration
     *     @ComponentScan
     *     public class MyApplication { }  // Use @SpringBootApplication instead!
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     */

    // Placeholder main method for demonstration
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                   ║");
        System.out.println("║          SPRING BOOT ANNOTATIONS - CHAPTER 1 OVERVIEW            ║");
        System.out.println("║               Core Spring Boot Annotations                        ║");
        System.out.println("║                                                                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("📚 Chapter 1: Core Spring Boot Annotations");
        System.out.println();
        System.out.println("✓ This chapter covers the fundamental annotations");
        System.out.println("✓ Each annotation is explained with examples");
        System.out.println("✓ Follow the numbered examples for hands-on learning");
        System.out.println();
        System.out.println("🎯 Topics Covered:");
        System.out.println("   1. @SpringBootApplication");
        System.out.println("   2. @EnableAutoConfiguration");
        System.out.println("   3. @SpringBootConfiguration");
        System.out.println("   4. @ConfigurationProperties");
        System.out.println("   5. @ConfigurationPropertiesScan");
        System.out.println();
        System.out.println("💡 Next: Explore Example01SpringBootApplication.java");
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

