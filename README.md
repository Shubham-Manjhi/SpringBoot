# Spring Boot Annotations - A Comprehensive Guide

## 📚 Table of Contents

- [Introduction](#introduction)
- [Project Overview](#project-overview)
- [Chapter 1: Core Spring Boot Annotations](#chapter-1-core-spring-boot-annotations)
- [Chapter 2: Spring Framework Core Annotations](#chapter-2-spring-framework-core-annotations)
- [Chapter 3: Spring MVC & REST Annotations](#chapter-3-spring-mvc--rest-annotations)
- [Chapter 4: Spring Data JPA Annotations](#chapter-4-spring-data-jpa-annotations)
- [Chapter 5: Spring Security Annotations](#chapter-5-spring-security-annotations)
- [Chapter 6: Spring Transaction Management Annotations](#chapter-6-spring-transaction-management-annotations)
- [Chapter 7: Spring Validation Annotations](#chapter-7-spring-validation-annotations)
- [Chapter 8: Spring Testing Annotations](#chapter-8-spring-testing-annotations)
- [Chapter 9: Spring AOP Annotations](#chapter-9-spring-aop-annotations)
- [Chapter 10: Spring Configuration Annotations](#chapter-10-spring-configuration-annotations)
- [Chapter 11: Spring Scheduling & Async Annotations](#chapter-11-spring-scheduling--async-annotations)
- [Chapter 12: Spring Cloud Annotations](#chapter-12-spring-cloud-annotations)
- [Chapter 13: Advanced & Specialized Annotations](#chapter-13-advanced--specialized-annotations)
- [How to Use This Project](#how-to-use-this-project)
- [Prerequisites](#prerequisites)
- [Contributing](#contributing)

---

## Introduction

Welcome to the **Spring Boot Annotations Mastery Project**! This comprehensive guide is designed to provide an in-depth understanding of all Spring Boot and Spring Framework annotations. Each annotation is explained with practical examples, use cases, and best practices.

This project serves as both a learning resource and a reference guide for developers at all levels - from beginners to advanced practitioners.

---

## Project Overview

This project is structured as a hands-on learning journey through the Spring Boot ecosystem. Each chapter focuses on a specific category of annotations, providing:

- **Detailed Explanations**: Understanding what each annotation does
- **Practical Examples**: Real-world code implementations
- **Best Practices**: When and how to use each annotation
- **Common Pitfalls**: What to avoid and why
- **Interview Questions**: Common questions related to each annotation

---

## Chapter 1: Core Spring Boot Annotations

### 1.1 `@SpringBootApplication`
The most fundamental annotation that combines three key annotations:
- `@Configuration`
- `@EnableAutoConfiguration`
- `@ComponentScan`

**Topics Covered:**
- Understanding auto-configuration
- Component scanning mechanism
- Exclude specific auto-configurations
- Custom configuration classes

### 1.2 `@EnableAutoConfiguration`
Enables Spring Boot's auto-configuration mechanism.

**Topics Covered:**
- How auto-configuration works
- Conditional auto-configuration
- Creating custom auto-configurations
- Debugging auto-configuration

### 1.3 `@SpringBootConfiguration`
Indicates that a class provides Spring Boot application configuration.

**Topics Covered:**
- Difference from `@Configuration`
- Use in testing scenarios
- Application context hierarchy

### 1.4 `@ConfigurationProperties`
Binds external configuration properties to a Java object.

**Topics Covered:**
- Property binding from application.properties/yml
- Nested properties
- Validation with `@Validated`
- Type-safe configuration

### 1.5 `@ConfigurationPropertiesScan`
Scans for classes annotated with `@ConfigurationProperties`.

---

## Chapter 2: Spring Framework Core Annotations

### 2.1 Stereotype Annotations

#### 2.1.1 `@Component`
Marks a class as a Spring-managed component.

**Topics Covered:**
- Component registration
- Bean naming conventions
- Singleton vs Prototype scope

#### 2.1.2 `@Service`
Specialized `@Component` for service layer.

**Topics Covered:**
- Service layer design patterns
- Business logic organization
- Transaction boundaries

#### 2.1.3 `@Repository`
Specialized `@Component` for data access layer.

**Topics Covered:**
- Data access patterns
- Exception translation
- Integration with Spring Data

#### 2.1.4 `@Controller`
Specialized `@Component` for MVC controllers.

**Topics Covered:**
- MVC architecture
- Request handling
- View resolution

### 2.2 Dependency Injection Annotations

#### 2.2.1 `@Autowired`
Automatic dependency injection.

**Topics Covered:**
- Constructor injection (recommended)
- Field injection
- Setter injection
- Required vs optional dependencies

#### 2.2.2 `@Qualifier`
Resolves ambiguity when multiple beans of the same type exist.

**Topics Covered:**
- Bean qualification
- Custom qualifiers
- Primary beans

#### 2.2.3 `@Primary`
Indicates a bean should be given preference when multiple candidates qualify.

#### 2.2.4 `@Required`
Marks a bean property as required (deprecated in favor of constructor injection).

#### 2.2.5 `@Value`
Injects values from properties files or expressions.

**Topics Covered:**
- Property injection
- SpEL (Spring Expression Language)
- Default values
- Type conversion

### 2.3 Bean Scope Annotations

#### 2.3.1 `@Scope`
Defines the scope of a bean.

**Topics Covered:**
- Singleton scope (default)
- Prototype scope
- Request scope
- Session scope
- Application scope
- Custom scopes

#### 2.3.2 `@RequestScope`
Shortcut for request-scoped beans.

#### 2.3.3 `@SessionScope`
Shortcut for session-scoped beans.

#### 2.3.4 `@ApplicationScope`
Shortcut for application-scoped beans.

### 2.4 Lifecycle Annotations

#### 2.4.1 `@PostConstruct`
Method to be executed after dependency injection.

**Topics Covered:**
- Bean initialization
- Initialization order
- Best practices

#### 2.4.2 `@PreDestroy`
Method to be executed before bean destruction.

**Topics Covered:**
- Resource cleanup
- Graceful shutdown
- Connection closing

### 2.5 `@Lazy`
Indicates that a bean should be lazily initialized.

**Topics Covered:**
- Performance optimization
- Circular dependency resolution
- Trade-offs

---

## Chapter 3: Spring MVC & REST Annotations

### 3.1 Controller Annotations

#### 3.1.1 `@RestController`
Combines `@Controller` and `@ResponseBody`.

**Topics Covered:**
- RESTful API design
- JSON/XML serialization
- Content negotiation

#### 3.1.2 `@RequestMapping`
Maps HTTP requests to handler methods.

**Topics Covered:**
- URL patterns
- HTTP methods
- Headers and parameters
- Consumes and produces

#### 3.1.3 HTTP Method Specific Annotations
- `@GetMapping`
- `@PostMapping`
- `@PutMapping`
- `@DeleteMapping`
- `@PatchMapping`

**Topics Covered:**
- RESTful conventions
- Idempotency
- Safe methods

### 3.2 Request Handling Annotations

#### 3.2.1 `@RequestParam`
Extracts query parameters.

**Topics Covered:**
- Required vs optional parameters
- Default values
- Type conversion
- Collection parameters

#### 3.2.2 `@PathVariable`
Extracts values from URI templates.

**Topics Covered:**
- URI design
- Pattern matching
- Regular expressions

#### 3.2.3 `@RequestBody`
Binds HTTP request body to a method parameter.

**Topics Covered:**
- JSON deserialization
- Validation
- Custom converters

#### 3.2.4 `@RequestHeader`
Extracts HTTP request headers.

**Topics Covered:**
- Header validation
- Common headers
- Custom headers

#### 3.2.5 `@CookieValue`
Extracts cookie values.

#### 3.2.6 `@ModelAttribute`
Binds request parameters to a model object.

**Topics Covered:**
- Form handling
- Data binding
- Validation

#### 3.2.7 `@SessionAttribute`
Binds a method parameter to a session attribute.

#### 3.2.8 `@RequestAttribute`
Binds a method parameter to a request attribute.

### 3.3 Response Handling Annotations

#### 3.3.1 `@ResponseBody`
Binds method return value to the response body.

**Topics Covered:**
- Serialization
- Message converters
- Content types

#### 3.3.2 `@ResponseStatus`
Sets HTTP status code for a response.

**Topics Covered:**
- Status codes
- Exception handling
- Custom responses

### 3.4 Exception Handling Annotations

#### 3.4.1 `@ExceptionHandler`
Handles exceptions in controller methods.

**Topics Covered:**
- Exception hierarchy
- Global exception handling
- Custom error responses

#### 3.4.2 `@ControllerAdvice`
Global exception handling across all controllers.

**Topics Covered:**
- Centralized error handling
- Custom error messages
- Logging strategies

#### 3.4.3 `@RestControllerAdvice`
Combines `@ControllerAdvice` and `@ResponseBody`.

### 3.5 Cross-Origin Resource Sharing

#### 3.5.1 `@CrossOrigin`
Enables CORS for a controller or method.

**Topics Covered:**
- CORS configuration
- Security considerations
- Global CORS setup

---

## Chapter 4: Spring Data JPA Annotations

### 4.1 Entity Annotations

#### 4.1.1 `@Entity`
Marks a class as a JPA entity.

**Topics Covered:**
- Entity lifecycle
- Persistence context
- Entity state transitions

#### 4.1.2 `@Table`
Specifies the table name for an entity.

**Topics Covered:**
- Table naming strategies
- Schema management
- Indexes and constraints

#### 4.1.3 `@Id`
Marks a field as the primary key.

#### 4.1.4 `@GeneratedValue`
Specifies primary key generation strategy.

**Topics Covered:**
- IDENTITY strategy
- SEQUENCE strategy
- TABLE strategy
- AUTO strategy

#### 4.1.5 `@Column`
Specifies column mapping.

**Topics Covered:**
- Column properties
- Nullable fields
- Length and precision
- Unique constraints

#### 4.1.6 `@Transient`
Marks a field to be ignored by JPA.

### 4.2 Relationship Annotations

#### 4.2.1 `@OneToOne`
Defines a one-to-one relationship.

**Topics Covered:**
- Bidirectional vs unidirectional
- Cascade operations
- Fetch strategies

#### 4.2.2 `@OneToMany`
Defines a one-to-many relationship.

**Topics Covered:**
- Collection mapping
- Orphan removal
- Cascade types

#### 4.2.3 `@ManyToOne`
Defines a many-to-one relationship.

**Topics Covered:**
- Foreign key mapping
- Lazy vs eager loading
- Join columns

#### 4.2.4 `@ManyToMany`
Defines a many-to-many relationship.

**Topics Covered:**
- Join tables
- Bidirectional relationships
- Performance considerations

#### 4.2.5 `@JoinColumn`
Specifies the foreign key column.

#### 4.2.6 `@JoinTable`
Specifies the join table for many-to-many relationships.

### 4.3 Query Annotations

#### 4.3.1 `@Query`
Defines custom JPQL or native SQL queries.

**Topics Covered:**
- JPQL syntax
- Native queries
- Named parameters
- Positional parameters

#### 4.3.2 `@NamedQuery`
Defines named queries.

#### 4.3.3 `@Modifying`
Indicates a query that modifies data.

**Topics Covered:**
- UPDATE queries
- DELETE queries
- Bulk operations

#### 4.3.4 `@Param`
Binds a method parameter to a query parameter.

### 4.4 Spring Data Repository Annotations

#### 4.4.1 `@Repository` (Spring Data)
Marks an interface as a Spring Data repository.

**Topics Covered:**
- Query methods
- Custom implementations
- Repository fragments

#### 4.4.2 `@NoRepositoryBean`
Prevents creating repository instances for intermediate interfaces.

---

## Chapter 5: Spring Security Annotations

### 5.1 `@EnableWebSecurity`
Enables Spring Security configuration.

**Topics Covered:**
- Security configuration
- Filter chains
- Authentication providers

### 5.2 Method Security Annotations

#### 5.2.1 `@EnableGlobalMethodSecurity`
Enables method-level security.

**Topics Covered:**
- Pre-authorization
- Post-authorization
- Secured annotations

#### 5.2.2 `@PreAuthorize`
Checks authorization before method execution.

**Topics Covered:**
- SpEL expressions
- Role-based access
- Permission-based access

#### 5.2.3 `@PostAuthorize`
Checks authorization after method execution.

#### 5.2.4 `@Secured`
Secures methods with role-based access control.

#### 5.2.5 `@RolesAllowed`
JSR-250 annotation for role-based security.

### 5.3 `@WithMockUser`
Creates a mock user for testing.

**Topics Covered:**
- Security testing
- Mock authentication
- Custom users

---

## Chapter 6: Spring Transaction Management Annotations

### 6.1 `@Transactional`
Declares transactional behavior.

**Topics Covered:**
- Transaction propagation
- Isolation levels
- Rollback rules
- Read-only transactions
- Timeout configuration

### 6.2 `@EnableTransactionManagement`
Enables Spring's annotation-driven transaction management.

**Topics Covered:**
- Transaction managers
- Proxy-based transactions
- AspectJ mode

---

## Chapter 7: Spring Validation Annotations

### 7.1 `@Valid`
Triggers validation of a bean.

**Topics Covered:**
- Bean validation
- Nested validation
- Validation groups

### 7.2 `@Validated`
Spring's variant of JSR-303 validation.

**Topics Covered:**
- Validation groups
- Method-level validation
- Class-level validation

### 7.3 Common Validation Annotations

#### 7.3.1 Basic Constraints
- `@NotNull`
- `@NotEmpty`
- `@NotBlank`
- `@Size`
- `@Min` / `@Max`
- `@DecimalMin` / `@DecimalMax`
- `@Positive` / `@PositiveOrZero`
- `@Negative` / `@NegativeOrZero`

#### 7.3.2 String Constraints
- `@Email`
- `@Pattern`
- `@Length`

#### 7.3.3 Date/Time Constraints
- `@Past`
- `@PastOrPresent`
- `@Future`
- `@FutureOrPresent`

#### 7.3.4 Custom Constraints
**Topics Covered:**
- Creating custom validators
- Constraint composition
- Cross-field validation

---

## Chapter 8: Spring Testing Annotations

### 8.1 `@SpringBootTest`
Loads complete application context for integration tests.

**Topics Covered:**
- Test contexts
- Web environment modes
- Configuration properties

### 8.2 `@WebMvcTest`
Tests MVC controllers with sliced context.

**Topics Covered:**
- Controller testing
- MockMvc
- Auto-configured mocks

### 8.3 `@DataJpaTest`
Tests JPA repositories with sliced context.

**Topics Covered:**
- Repository testing
- Test database
- Transaction rollback

### 8.4 `@MockBean`
Adds mock beans to the application context.

**Topics Covered:**
- Mocking dependencies
- Mockito integration
- Test isolation

### 8.5 `@SpyBean`
Adds spy beans to the application context.

### 8.6 `@TestConfiguration`
Provides additional configuration for tests.

### 8.7 `@DirtiesContext`
Indicates that test modifies the application context.

**Topics Covered:**
- Context caching
- Context cleanup
- Performance implications

### 8.8 JUnit Annotations Integration
- `@Test`
- `@BeforeEach` / `@AfterEach`
- `@BeforeAll` / `@AfterAll`
- `@Disabled`
- `@ParameterizedTest`

---

## Chapter 9: Spring AOP Annotations

### 9.1 `@EnableAspectJAutoProxy`
Enables support for handling components marked with AspectJ's `@Aspect`.

### 9.2 `@Aspect`
Declares a class as an aspect.

**Topics Covered:**
- Aspect-oriented programming concepts
- Cross-cutting concerns
- Modularization

### 9.3 Advice Annotations

#### 9.3.1 `@Before`
Advice that executes before a join point.

**Topics Covered:**
- Pre-processing
- Validation
- Logging

#### 9.3.2 `@After`
Advice that executes after a join point.

#### 9.3.3 `@AfterReturning`
Advice that executes after successful method execution.

**Topics Covered:**
- Return value processing
- Success logging
- Caching

#### 9.3.4 `@AfterThrowing`
Advice that executes when a method throws an exception.

**Topics Covered:**
- Exception logging
- Error handling
- Notifications

#### 9.3.5 `@Around`
Advice that surrounds a join point.

**Topics Covered:**
- Performance monitoring
- Transaction management
- Custom behavior

### 9.4 Pointcut Annotations

#### 9.4.1 `@Pointcut`
Declares a pointcut expression.

**Topics Covered:**
- Pointcut expressions
- Execution patterns
- Reusable pointcuts

---

## Chapter 10: Spring Configuration Annotations

### 10.1 `@Configuration`
Indicates a class declares one or more `@Bean` methods.

**Topics Covered:**
- Java-based configuration
- Bean definition
- Configuration classes

### 10.2 `@Bean`
Declares a bean to be managed by Spring container.

**Topics Covered:**
- Bean lifecycle
- Bean scopes
- Initialization and destruction methods

### 10.3 `@Import`
Imports additional configuration classes.

**Topics Covered:**
- Modular configuration
- Configuration composition
- Conditional imports

### 10.4 `@PropertySource`
Adds a property source to Spring's Environment.

**Topics Covered:**
- External configuration
- Multiple property sources
- Property resolution

### 10.5 `@PropertySources`
Container annotation for multiple `@PropertySource` annotations.

### 10.6 Conditional Annotations

#### 10.6.1 `@Conditional`
Registers a bean conditionally.

**Topics Covered:**
- Custom conditions
- Condition evaluation
- Configuration flexibility

#### 10.6.2 `@ConditionalOnProperty`
Registers a bean based on property values.

#### 10.6.3 `@ConditionalOnClass`
Registers a bean if a class is present.

#### 10.6.4 `@ConditionalOnMissingClass`
Registers a bean if a class is absent.

#### 10.6.5 `@ConditionalOnBean`
Registers a bean if another bean exists.

#### 10.6.6 `@ConditionalOnMissingBean`
Registers a bean if another bean doesn't exist.

#### 10.6.7 `@ConditionalOnExpression`
Registers a bean based on SpEL expression.

#### 10.6.8 `@ConditionalOnJava`
Registers a bean based on Java version.

#### 10.6.9 `@ConditionalOnWebApplication`
Registers a bean if application is a web application.

### 10.7 `@Profile`
Activates beans for specific profiles.

**Topics Covered:**
- Environment-specific configuration
- Profile activation
- Default profile

---

## Chapter 11: Spring Scheduling & Async Annotations

### 11.1 `@EnableScheduling`
Enables Spring's scheduled task execution capability.

### 11.2 `@Scheduled`
Marks a method to be scheduled.

**Topics Covered:**
- Fixed rate execution
- Fixed delay execution
- Cron expressions
- Initial delays
- Time zones

### 11.3 `@EnableAsync`
Enables Spring's asynchronous method execution capability.

### 11.4 `@Async`
Marks a method for asynchronous execution.

**Topics Covered:**
- Thread pools
- CompletableFuture
- Exception handling
- Return types

---

## Chapter 12: Spring Cloud Annotations

### 12.1 Service Discovery

#### 12.1.1 `@EnableEurekaServer`
Enables Eureka server.

#### 12.1.2 `@EnableDiscoveryClient`
Enables service discovery client.

#### 12.1.3 `@EnableEurekaClient`
Enables Eureka client (deprecated in favor of `@EnableDiscoveryClient`).

### 12.2 Configuration

#### 12.2.1 `@EnableConfigServer`
Enables Spring Cloud Config Server.

#### 12.2.2 `@RefreshScope`
Allows beans to be refreshed dynamically.

**Topics Covered:**
- Dynamic configuration
- Configuration refresh
- Bean recreation

### 12.3 Circuit Breaker

#### 12.3.1 `@EnableCircuitBreaker`
Enables circuit breaker pattern.

#### 12.3.2 `@HystrixCommand`
Wraps a method with circuit breaker logic.

**Topics Covered:**
- Fault tolerance
- Fallback methods
- Timeout configuration

### 12.4 Load Balancing

#### 12.4.1 `@LoadBalanced`
Marks a RestTemplate as load-balanced.

**Topics Covered:**
- Client-side load balancing
- Service discovery integration
- Ribbon configuration

### 12.5 Feign Clients

#### 12.5.1 `@EnableFeignClients`
Enables Feign client scanning.

#### 12.5.2 `@FeignClient`
Declares a Feign client interface.

**Topics Covered:**
- Declarative REST clients
- Service discovery
- Custom configuration

---

## Chapter 13: Advanced & Specialized Annotations

### 13.1 `@EventListener`
Marks a method as an application event listener.

**Topics Covered:**
- Event-driven architecture
- Custom events
- Async event handling

### 13.2 `@TransactionalEventListener`
Binds event listener to transaction phases.

**Topics Covered:**
- Transaction-aware events
- Event phases
- Rollback handling

### 13.3 Caching Annotations

#### 13.3.1 `@EnableCaching`
Enables Spring's caching capabilities.

#### 13.3.2 `@Cacheable`
Marks a method's result to be cached.

**Topics Covered:**
- Cache abstraction
- Cache providers
- Cache keys
- Conditional caching

#### 13.3.3 `@CachePut`
Updates cache without interfering with method execution.

#### 13.3.4 `@CacheEvict`
Removes entries from cache.

**Topics Covered:**
- Cache invalidation
- All entries eviction
- Before/after invocation

#### 13.3.5 `@Caching`
Groups multiple cache operations.

### 13.4 JMX Annotations

#### 13.4.1 `@EnableMBeanExport`
Enables JMX export of Spring beans.

#### 13.4.2 `@ManagedResource`
Marks a class as a JMX managed resource.

#### 13.4.3 `@ManagedOperation`
Exposes a method as a JMX operation.

#### 13.4.4 `@ManagedAttribute`
Exposes a method as a JMX attribute.

### 13.5 Retry Annotations

#### 13.5.1 `@EnableRetry`
Enables Spring Retry capabilities.

#### 13.5.2 `@Retryable`
Marks a method to be retried on failure.

**Topics Covered:**
- Retry policies
- Backoff strategies
- Maximum attempts

#### 13.5.3 `@Recover`
Provides a recovery method after retry exhaustion.

### 13.6 `@DependsOn`
Ensures a bean is created after specified beans.

**Topics Covered:**
- Bean initialization order
- Dependency management
- Circular dependencies

### 13.7 `@Description`
Adds description to bean definitions.

### 13.8 `@Order`
Defines the sort order for components.

**Topics Covered:**
- Component ordering
- Filter chains
- AOP advice ordering

### 13.9 `@Priority`
JSR-250 annotation for ordering components.

---

## How to Use This Project

### Step 1: Clone the Repository
```bash
git clone <repository-url>
cd spring-boot-annotations
```

### Step 2: Explore Chapter by Chapter
Each chapter will have its own package with:
- **Example classes** demonstrating the annotations
- **Test classes** showing real-world usage
- **Documentation** explaining concepts in depth

### Step 3: Run Examples
```bash
./gradlew bootRun
```

### Step 4: Run Tests
```bash
./gradlew test
```

### Step 5: Experiment
Modify examples, create your own, and experiment with different configurations to deepen your understanding.

---

## Prerequisites

- **Java**: JDK 17 or higher
- **Build Tool**: Gradle or Maven
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions
- **Basic Knowledge**: 
  - Core Java concepts
  - Object-oriented programming
  - Understanding of dependency injection
  - Basic knowledge of web applications

---

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/learning/springboot/
│   │       ├── chapter01/          # Core Spring Boot Annotations
│   │       ├── chapter02/          # Spring Framework Core
│   │       ├── chapter03/          # Spring MVC & REST
│   │       ├── chapter04/          # Spring Data JPA
│   │       ├── chapter05/          # Spring Security
│   │       ├── chapter06/          # Transaction Management
│   │       ├── chapter07/          # Validation
│   │       ├── chapter08/          # Testing
│   │       ├── chapter09/          # AOP
│   │       ├── chapter10/          # Configuration
│   │       ├── chapter11/          # Scheduling & Async
│   │       ├── chapter12/          # Spring Cloud
│   │       └── chapter13/          # Advanced Topics
│   └── resources/
│       ├── application.yml
│       └── application-{profile}.yml
└── test/
    └── java/
        └── com/learning/springboot/
            └── [corresponding test packages]
```

---

## Learning Path

### For Beginners
1. Start with **Chapter 1** (Core Spring Boot)
2. Move to **Chapter 2** (Spring Framework Core)
3. Practice with **Chapter 3** (Spring MVC & REST)
4. Explore **Chapter 8** (Testing)

### For Intermediate Developers
1. Deep dive into **Chapter 4** (Spring Data JPA)
2. Master **Chapter 5** (Spring Security)
3. Understand **Chapter 6** (Transactions)
4. Learn **Chapter 9** (AOP)

### For Advanced Developers
1. Explore **Chapter 10** (Advanced Configuration)
2. Master **Chapter 11** (Scheduling & Async)
3. Study **Chapter 12** (Spring Cloud)
4. Practice **Chapter 13** (Advanced Topics)

---

## Best Practices

1. **Always use constructor injection** over field injection
2. **Prefer `@RestController`** for REST APIs
3. **Use `@Transactional` carefully** - understand propagation and isolation
4. **Enable method security** for fine-grained access control
5. **Write tests** for every annotation you implement
6. **Use profiles** for environment-specific configuration
7. **Keep aspects modular** and focused on single concerns
8. **Cache strategically** - not everything should be cached
9. **Document your code** - especially custom annotations
10. **Follow naming conventions** for beans and components

---

## Common Interview Questions

Each chapter includes a section on commonly asked interview questions related to the annotations covered. Practice these to prepare for technical interviews.

---

## Resources & References

- [Official Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Framework Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/)
- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [Baeldung Spring Tutorials](https://www.baeldung.com/spring-tutorial)

---

## Contributing

Contributions are welcome! If you find any issues or want to add more examples:

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## Acknowledgments

This project is created for educational purposes to help developers master Spring Boot annotations through practical examples and in-depth explanations.

---

## Contact & Support

For questions, suggestions, or support:
- Open an issue in the repository
- Reach out via discussions

---

**Happy Learning! 🚀**

*Master Spring Boot annotations one chapter at a time, and become a proficient Spring Boot developer!*

