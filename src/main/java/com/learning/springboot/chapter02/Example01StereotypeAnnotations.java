package com.learning.springboot.chapter02;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║           EXAMPLE 01: STEREOTYPE ANNOTATIONS IN ACTION                               ║
 * ║           @Component  ·  @Service  ·  @Repository  ·  @Controller                   ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example01StereotypeAnnotations.java
 * Purpose:     Demonstrate all four stereotype annotations with a real layered
 *              architecture example — a simple Product Catalog mini-application.
 * Difficulty:  ⭐ Beginner
 * Time:        25 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * WHAT YOU WILL LEARN:
 *   • @Component  — the root stereotype; registers any class as a Spring bean
 *   • @Service    — marks the business/service layer
 *   • @Repository — marks the data-access layer; adds exception translation
 *   • @Controller — marks the web/presentation layer; enables @RequestMapping
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

// ══════════════════════════════════════════════════════════════════════════════════════
//  DOMAIN MODEL (Plain Java — no Spring annotation needed here)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ─────────────────────────────────────────────────────────────────────────────────────
 * 📌 DOMAIN MODEL: Product
 * ─────────────────────────────────────────────────────────────────────────────────────
 *
 * A plain Java object (POJO) representing a product in our catalog.
 * No Spring annotations here — this is just data; Spring manages the
 * components that USE this object, not the object itself.
 */
class Product {
    private Long id;
    private String name;
    private String category;
    private double price;

    public Product(Long id, String name, String category, double price) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
    }

    // Getters
    public Long getId()          { return id; }
    public String getName()      { return name; }
    public String getCategory()  { return category; }
    public double getPrice()     { return price; }

    @Override
    public String toString() {
        return String.format("Product{id=%d, name='%s', category='%s', price=%.2f}",
                id, name, category, price);
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 1: @COMPONENT — THE ROOT STEREOTYPE
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                          @Component  EXPLAINED                               ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @Component is the GENERIC stereotype annotation. It tells Spring:
 * "This class is a component — create an instance and manage it in the
 *  Application Context."
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 WHEN TO USE @COMPONENT:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  •  Utility classes that don't fit service / repository / controller roles
 *  •  Infrastructure helpers (e.g., a price formatter, event publisher)
 *  •  Classes that hold cross-cutting logic but are not AOP aspects
 *  •  When none of the specialised stereotypes semantically fit
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔧 BEAN NAMING:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  @Component                 → bean name = "priceCalculator"  (camelCase class name)
 *  @Component("myCalc")       → bean name = "myCalc"           (explicit name)
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📝 EXAMPLE: PriceCalculator — A utility that doesn't fit any other stereotype
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Component   // ← Registers this class as a Spring bean
             // Spring will create exactly ONE instance (singleton by default)
             // Bean name will be "priceCalculator" (camelCase of class name)
class PriceCalculator {

    /*
     * Spring calls this constructor automatically during bean creation.
     * No manual "new PriceCalculator()" needed — Spring handles it.
     */
    public PriceCalculator() {
        System.out.println("✅ PriceCalculator bean created by Spring (via @Component)");
    }

    /**
     * Applies a discount to a product price.
     *
     * This method is PURE logic — no state stored in the bean.
     * This is why singleton scope is perfect here: stateless utilities
     * are safe to share across the entire application.
     *
     * @param price            original price
     * @param discountPercent  discount as a percentage (0–100)
     * @return                 discounted price
     */
    public double applyDiscount(double price, double discountPercent) {
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException(
                    "Discount must be between 0 and 100, but was: " + discountPercent);
        }
        return price * (1 - discountPercent / 100.0);
    }

    /**
     * Calculates the price including tax.
     *
     * @param price    net price
     * @param taxRate  tax rate as a percentage (e.g., 18 for 18%)
     * @return         price including tax
     */
    public double addTax(double price, double taxRate) {
        return price * (1 + taxRate / 100.0);
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 2: @REPOSITORY — DATA ACCESS LAYER
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                         @Repository  EXPLAINED                               ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @Repository is a specialisation of @Component for the DATA ACCESS LAYER.
 * It has ONE major extra feature beyond @Component:
 *
 *   🔑 EXCEPTION TRANSLATION
 *      Spring wraps low-level data-access exceptions (e.g., JDBC SQLExceptions,
 *      Hibernate exceptions) into Spring's own DataAccessException hierarchy.
 *
 *      WHY? So that your service layer doesn't need to know whether you're using
 *      JDBC, Hibernate, JPA, or MongoDB — it always catches the same exception
 *      type: DataAccessException.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 EXCEPTION TRANSLATION IN ACTION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *   Without @Repository:
 *       throw new java.sql.SQLException("Duplicate entry...");  ← low-level
 *
 *   With @Repository:
 *       Spring intercepts it ↓
 *       throw new DataIntegrityViolationException("...");       ← Spring's hierarchy
 *
 *   Service layer catches:
 *       catch (DataAccessException e) { ... }   ← works regardless of DB technology
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ NOTE: In a real application you would extend JpaRepository or CrudRepository
 * from Spring Data. In this example we simulate the repository with an in-memory
 * List to keep the focus on the annotation, not on JPA setup.
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Repository   // ← Marks this as the data-access layer
              // Enables Spring's exception translation mechanism
              // Bean name = "productRepository"
class ProductRepository {

    // Simulated "database" — in a real app this is replaced by JPA, JDBC, MongoDB, etc.
    private final List<Product> database = new ArrayList<>();

    public ProductRepository() {
        System.out.println("✅ ProductRepository bean created by Spring (via @Repository)");

        // Seed with sample data
        database.add(new Product(1L, "MacBook Pro",  "Electronics", 2499.99));
        database.add(new Product(2L, "Clean Code",   "Books",         35.99));
        database.add(new Product(3L, "Java Mug",     "Accessories",   14.99));
        database.add(new Product(4L, "Wireless Mouse", "Electronics", 59.99));
    }

    /**
     * Find all products.
     *
     * @return unmodifiable list of all products
     */
    public List<Product> findAll() {
        return List.copyOf(database);
    }

    /**
     * Find a single product by its ID.
     *
     * @param id the product's unique identifier
     * @return Optional containing the product, or empty if not found
     */
    public Optional<Product> findById(Long id) {
        return database.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }

    /**
     * Find all products in a given category.
     *
     * @param category the product category (case-insensitive)
     * @return list of matching products
     */
    public List<Product> findByCategory(String category) {
        return database.stream()
                .filter(p -> p.getCategory().equalsIgnoreCase(category))
                .toList();
    }

    /**
     * Save a new product.
     *
     * In a real application, if the INSERT violates a UNIQUE constraint,
     * Spring (via @Repository exception translation) would convert the low-level
     * SQLException into DataIntegrityViolationException.
     *
     * @param product the product to save
     * @return the saved product
     */
    public Product save(Product product) {
        database.add(product);
        System.out.println("💾 Saved to 'database': " + product);
        return product;
    }

    /**
     * Delete a product by ID.
     *
     * @param id the product's unique identifier
     * @return true if deleted, false if not found
     */
    public boolean deleteById(Long id) {
        return database.removeIf(p -> p.getId().equals(id));
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 3: @SERVICE — BUSINESS / SERVICE LAYER
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                           @Service  EXPLAINED                                ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @Service is a specialisation of @Component for the SERVICE LAYER.
 * It tells Spring and your team: "This class contains BUSINESS LOGIC."
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🎯 RESPONSIBILITIES OF A @Service CLASS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1.  ORCHESTRATION    — Coordinates calls to repositories and other services
 *  2.  BUSINESS RULES   — Validates business constraints (e.g., stock availability)
 *  3.  TRANSACTION MGMT — Usually the layer where @Transactional is applied
 *  4.  MAPPING          — Transforms domain objects to DTOs and back
 *  5.  ERROR HANDLING   — Catches data exceptions, throws domain exceptions
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔥 IMPORTANT: CODE TO INTERFACES!
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * Define an interface first, then implement it with @Service.
 * This allows you to:
 *   •  Swap implementations without touching calling code
 *   •  Mock the interface in tests
 *   •  Apply AOP proxies cleanly
 *
 *   interface ProductService { ... }          ← interface (contract)
 *
 *   @Service                                  ← implementation
 *   class ProductServiceImpl implements ProductService { ... }
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 */

/**
 * Contract for the product service layer.
 * Spring will auto-detect the implementation via component scanning.
 */
interface ProductService {
    List<Product> getAllProducts();
    Optional<Product> getProductById(Long id);
    List<Product> getProductsByCategory(String category);
    Product createProduct(Long id, String name, String category, double price);
    double getPriceWithTax(Long productId, double taxRate);
    double getPriceWithDiscount(Long productId, double discountPercent);
}

@Service   // ← Marks this as the service/business layer
           // Bean name = "productServiceImpl"
           //
           // Note: Spring will register this as a bean of type ProductService
           // (the interface) AND ProductServiceImpl (the concrete class).
           // Injection by the interface type is preferred!
class ProductServiceImpl implements ProductService {

    /*
     * ─────────────────────────────────────────────────────────────────────────────────
     * ✅ CONSTRUCTOR INJECTION — THE RIGHT WAY
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * We inject our dependencies through the constructor.
     *
     * WHY CONSTRUCTOR INJECTION?
     *   1. Fields can be final → immutable → thread-safe
     *   2. All dependencies visible in one place
     *   3. Object is never in an incomplete state
     *   4. Testable without Spring — just call new ProductServiceImpl(repo, calc)
     *
     * NOTE: In Spring 4.3+, @Autowired on the constructor is OPTIONAL if there
     * is only ONE constructor. Spring detects it automatically.
     * We include it here for clarity.
     */
    private final ProductRepository productRepository;   // final = immutable
    private final PriceCalculator priceCalculator;       // final = immutable

    // Spring sees this constructor and automatically injects productRepository
    // and priceCalculator beans from the ApplicationContext.
    // @Autowired is optional here (only 1 constructor) but shown for clarity.
    public ProductServiceImpl(ProductRepository productRepository,
                              PriceCalculator priceCalculator) {
        this.productRepository = productRepository;
        this.priceCalculator   = priceCalculator;
        System.out.println("✅ ProductServiceImpl bean created by Spring (via @Service)");
    }

    @Override
    public List<Product> getAllProducts() {
        System.out.println("📋 [Service] Getting all products");
        return productRepository.findAll();
    }

    @Override
    public Optional<Product> getProductById(Long id) {
        System.out.println("🔍 [Service] Looking up product with id=" + id);
        return productRepository.findById(id);
    }

    @Override
    public List<Product> getProductsByCategory(String category) {
        // Business rule: category cannot be blank
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category must not be blank");
        }
        System.out.println("📂 [Service] Getting products in category: " + category);
        return productRepository.findByCategory(category);
    }

    @Override
    public Product createProduct(Long id, String name, String category, double price) {
        // Business rule: price must be positive
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be positive, but was: " + price);
        }
        // Business rule: name cannot be blank
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }

        Product product = new Product(id, name, category, price);
        return productRepository.save(product);
    }

    @Override
    public double getPriceWithTax(Long productId, double taxRate) {
        // Delegate to PriceCalculator (@Component utility)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        double priceWithTax = priceCalculator.addTax(product.getPrice(), taxRate);
        System.out.printf("💰 [Service] Product '%s': base=%.2f, tax=%.1f%%, total=%.2f%n",
                product.getName(), product.getPrice(), taxRate, priceWithTax);
        return priceWithTax;
    }

    @Override
    public double getPriceWithDiscount(Long productId, double discountPercent) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        double discountedPrice = priceCalculator.applyDiscount(product.getPrice(), discountPercent);
        System.out.printf("🏷️  [Service] Product '%s': base=%.2f, discount=%.1f%%, price=%.2f%n",
                product.getName(), product.getPrice(), discountPercent, discountedPrice);
        return discountedPrice;
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 4: @CONTROLLER — PRESENTATION / WEB LAYER
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║                          @Controller  EXPLAINED                              ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📌 DEFINITION:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * @Controller is a specialisation of @Component for the WEB LAYER.
 * It signals Spring MVC's DispatcherServlet:
 * "This class handles HTTP requests — look for @RequestMapping methods inside."
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * @Controller  vs  @RestController:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  @Controller    → Used for traditional MVC with Thymeleaf/JSP view templates.
 *                   Handler method returns a VIEW NAME (String), not response body.
 *                   You need @ResponseBody on each method to return data directly.
 *
 *  @RestController → @Controller + @ResponseBody.
 *                    Every handler method automatically writes to the HTTP response
 *                    body (serialised to JSON/XML). Ideal for REST APIs.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 📝 EXAMPLE BELOW: @Controller with @ResponseBody (to avoid Thymeleaf dependency)
 * ─────────────────────────────────────────────────────────────────────────────────
 */
@Controller                          // ← Marks this as the web layer
@RequestMapping("/api/products")     // ← Base path for all endpoints in this controller
class ProductController {

    /*
     * ✅ CONSTRUCTOR INJECTION
     * We inject ProductService (the interface) NOT ProductServiceImpl.
     * This keeps the controller decoupled from the implementation.
     */
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
        System.out.println("✅ ProductController bean created by Spring (via @Controller)");
    }

    /**
     * GET /api/products
     * Returns all products.
     *
     * @ResponseBody tells Spring to write the return value to the HTTP response
     * body as JSON (using Jackson). Without @ResponseBody, Spring would try to
     * resolve "getAllProducts" as a Thymeleaf template name.
     */
    @GetMapping
    @ResponseBody
    public List<Product> getAllProducts() {
        System.out.println("📥 [Controller] Received GET /api/products");
        return productService.getAllProducts();
    }

    /**
     * GET /api/products/{id}
     * Returns a single product by ID.
     *
     * @PathVariable extracts the {id} segment from the URL.
     */
    @GetMapping("/{id}")
    @ResponseBody
    public Product getProductById(@PathVariable Long id) {
        System.out.println("📥 [Controller] Received GET /api/products/" + id);
        return productService.getProductById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    /**
     * GET /api/products/category/{category}
     * Returns products filtered by category.
     */
    @GetMapping("/category/{category}")
    @ResponseBody
    public List<Product> getByCategory(@PathVariable String category) {
        System.out.println("📥 [Controller] Received GET /api/products/category/" + category);
        return productService.getProductsByCategory(category);
    }
}

// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION 5: PUTTING IT ALL TOGETHER — THE COMPLETE PICTURE
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║              STEREOTYPE ANNOTATIONS — HOW THEY WORK TOGETHER                 ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * When Spring Boot starts:
 *
 *   STEP 1 — @ComponentScan (triggered by @SpringBootApplication)
 *     ↓  Scans com.learning.springboot.chapter02 recursively
 *     ↓  Finds: @Component, @Service, @Repository, @Controller
 *     ↓  Creates BeanDefinition for each
 *
 *   STEP 2 — Bean Instantiation (in dependency order)
 *     ↓  PriceCalculator          (no dependencies → created first)
 *     ↓  ProductRepository        (no Spring dependencies → created)
 *     ↓  ProductServiceImpl       (needs ProductRepository + PriceCalculator → created after)
 *     ↓  ProductController        (needs ProductService → created last)
 *
 *   STEP 3 — Dependency Injection
 *     ↓  PriceCalculator    → injected into ProductServiceImpl
 *     ↓  ProductRepository  → injected into ProductServiceImpl
 *     ↓  ProductServiceImpl → injected into ProductController (as ProductService interface!)
 *
 *   STEP 4 — Bean Registration
 *     ↓  All beans stored in ApplicationContext
 *     ↓  Ready to serve requests
 *
 *   HTTP REQUEST FLOW:
 *
 *   Client → [HTTP GET /api/products/1]
 *       ↓
 *   DispatcherServlet
 *       ↓
 *   ProductController.getProductById(1)   ← @Controller handles the HTTP layer
 *       ↓
 *   ProductService.getProductById(1)      ← @Service handles business logic
 *       ↓
 *   ProductRepository.findById(1)         ← @Repository handles data access
 *       ↓
 *   Returns Product{id=1, name='MacBook Pro', ...}
 *       ↓
 *   Jackson serializes to JSON
 *       ↓
 *   {"id":1,"name":"MacBook Pro","category":"Electronics","price":2499.99}
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🔑 KEY TAKEAWAYS:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1.  All four stereotypes are specialisations of @Component
 *  2.  @Repository adds exception translation on top of @Component
 *  3.  @Controller integrates with Spring MVC's DispatcherServlet
 *  4.  @Service adds semantic clarity; architecturally isolates business logic
 *  5.  Always code to interfaces — inject the interface, not the implementation
 *  6.  Spring DETECTS and WIRES all of this automatically via component scanning
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 💡 PRACTICAL BEAN NAMING RULES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  Class Name              Bean Name (default)
 *  ─────────────────────   ──────────────────────────────────────
 *  PriceCalculator         priceCalculator
 *  ProductRepository       productRepository
 *  ProductServiceImpl      productServiceImpl
 *  ProductController       productController
 *
 *  Custom name: @Component("myUtil")  → bean name = "myUtil"
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ⚠️ COMMON PITFALL: Stereotype annotations only work inside a component-scanned
 * package. If you move a @Service class OUTSIDE the base package, Spring will not
 * detect it and will throw NoSuchBeanDefinitionException.
 * ─────────────────────────────────────────────────────────────────────────────────
 */
class Example01StereotypeAnnotations {
    /*
     * This class is intentionally empty — it serves as the documentation entry
     * point for this file. The actual Spring beans are the annotated classes above.
     *
     * Tip: In your IDE, use the Spring bean diagram to visualise the dependency
     * graph that Spring constructs from these annotations.
     */
}

