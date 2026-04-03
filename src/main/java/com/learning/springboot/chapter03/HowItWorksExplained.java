package com.learning.springboot.chapter03;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║          CHAPTER 3: HOW SPRING MVC ANNOTATIONS WORK INTERNALLY                       ║
 * ║                      DEEP DIVE — DISPATCHERSERVLET & THE MVC PIPELINE                ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        HowItWorksExplained.java
 * Purpose:     Understand the INTERNAL workings of Spring MVC — the DispatcherServlet,
 *              HandlerMapping, HandlerAdapter, MessageConverters, ExceptionResolvers.
 *              This is the "engine room" of every Spring REST API.
 * Difficulty:  ⭐⭐⭐⭐ Advanced
 * Time:        40 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║       HOW SPRING MVC & REST ANNOTATIONS WORK INTERNALLY                      ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
public class HowItWorksExplained {

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 1: THE BIG PICTURE — DISPATCHERSERVLET IS THE FRONT CONTROLLER  ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 📌 WHAT IS THE DISPATCHERSERVLET?
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     * DispatcherServlet is the FRONT CONTROLLER of Spring MVC — it is the SINGLE
     * entry point for ALL incoming HTTP requests.
     *
     * It's a plain Java Servlet (implements javax.servlet.Servlet / jakarta.servlet.Servlet)
     * registered with the embedded Tomcat/Jetty/Undertow at startup.
     *
     * ALL requests → Tomcat → DispatcherServlet → (routes to the right controller)
     *
     * ─────────────────────────────────────────────────────────────────────────────────
     * 🏗️ SPRING MVC ARCHITECTURE COMPONENTS:
     * ─────────────────────────────────────────────────────────────────────────────────
     *
     *                   ┌──────────────────────────────────────────────┐
     *                   │              DISPATCHERSERVLET                │
     *                   │                                              │
     *                   │  ┌──────────────────────────────────────┐   │
     *                   │  │  HandlerMapping                       │   │
     *                   │  │  Finds the handler (controller method)│   │
     *                   │  └──────────────────────────────────────┘   │
     *                   │                                              │
     *                   │  ┌──────────────────────────────────────┐   │
     *                   │  │  HandlerInterceptor(s)               │   │
     *                   │  │  Pre/post processing (auth, logging) │   │
     *                   │  └──────────────────────────────────────┘   │
     *                   │                                              │
     *                   │  ┌──────────────────────────────────────┐   │
     *                   │  │  HandlerAdapter                       │   │
     *                   │  │  Invokes the handler method          │   │
     *                   │  │  Resolves method arguments           │   │
     *                   │  └──────────────────────────────────────┘   │
     *                   │                                              │
     *                   │  ┌──────────────────────────────────────┐   │
     *                   │  │  HttpMessageConverter(s)             │   │
     *                   │  │  Deserialise request body / Serialise│   │
     *                   │  │  response body (JSON, XML, etc.)     │   │
     *                   │  └──────────────────────────────────────┘   │
     *                   │                                              │
     *                   │  ┌──────────────────────────────────────┐   │
     *                   │  │  HandlerExceptionResolver(s)          │   │
     *                   │  │  Handles exceptions from handlers    │   │
     *                   │  └──────────────────────────────────────┘   │
     *                   │                                              │
     *                   │  ┌──────────────────────────────────────┐   │
     *                   │  │  ViewResolver (for @Controller)       │   │
     *                   │  │  Resolves view names to templates    │   │
     *                   │  └──────────────────────────────────────┘   │
     *                   └──────────────────────────────────────────────┘
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 2: THE COMPLETE REQUEST LIFECYCLE — STEP BY STEP               ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Let's trace a DELETE /api/v1/orders/42 request through the entire pipeline:
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     * STEP 1: REQUEST ARRIVES AT TOMCAT
     * ═══════════════════════════════════════════════════════════════════════════════
     *
     *   Client sends:
     *     DELETE /api/v1/orders/42 HTTP/1.1
     *     Host: api.myapp.com
     *     Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
     *     Origin: https://myapp.com   (CORS preflight first if needed)
     *
     *   Tomcat:
     *     1. Accepts the TCP connection
     *     2. Parses the HTTP request into an HttpServletRequest object
     *     3. Checks the Servlet mapping → all requests go to DispatcherServlet
     *     4. Calls dispatcherServlet.service(request, response)
     *
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     * STEP 2: FILTER CHAIN (before DispatcherServlet)
     * ═══════════════════════════════════════════════════════════════════════════════
     *
     *   Servlet filters run BEFORE DispatcherServlet:
     *
     *     CorsFilter                → Handles CORS headers (if configured via Spring Security)
     *     CharacterEncodingFilter   → Sets UTF-8 encoding
     *     SecurityFilterChain       → Spring Security filter chain (if present)
     *         → JWT extraction, authentication, authorisation checks
     *     ContentNegotiationFilter  → Sets Accept/Content-Type defaults
     *
     *   Filters can:
     *     • Short-circuit the request (return 401 Unauthorized, 403 Forbidden)
     *     • Add attributes to the request: request.setAttribute("userId", userId)
     *     • Modify the request/response
     *
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     * STEP 3: DISPATCHERSERVLET.doDispatch()
     * ═══════════════════════════════════════════════════════════════════════════════
     *
     *   DispatcherServlet.doDispatch(request, response):
     *
     *   3.1 Get handler:
     *         HandlerExecutionChain handler = getHandler(request);
     *
     *         This calls each HandlerMapping in order:
     *           a. RequestMappingHandlerMapping (processes @Controller methods)
     *           b. BeanNameUrlHandlerMapping (maps /beanName to @Controller bean)
     *           c. RouterFunctionMapping (for WebFlux-style functional routing)
     *
     *         RequestMappingHandlerMapping:
     *           → Has a Map<RequestMappingInfo, HandlerMethod> built at startup
     *           → Looks up the best matching RequestMappingInfo for this request:
     *                 URL pattern:  /api/v1/orders/{id}  matches  /api/v1/orders/42
     *                 HTTP method:  DELETE matches @DeleteMapping
     *                 Headers:      no constraint
     *                 Content-Type: no constraint
     *           → Returns HandlerMethod: OrderController#cancelOrder(Long)
     *                                                          (or deleteOrder)
     *
     *   3.2 If no handler found → 404 Not Found response
     *
     *   3.3 Get HandlerAdapter:
     *         HandlerAdapter adapter = getHandlerAdapter(handler);
     *         → RequestMappingHandlerAdapter handles @RequestMapping methods
     *
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     * STEP 4: HANDLER INTERCEPTORS — preHandle()
     * ═══════════════════════════════════════════════════════════════════════════════
     *
     *   HandlerInterceptors run around the handler:
     *
     *   handlerExecutionChain.applyPreHandle(request, response):
     *
     *     → Calls preHandle() on each registered HandlerInterceptor
     *     → Example interceptors:
     *          LoggingInterceptor.preHandle()     → logs the request
     *          AuthInterceptor.preHandle()        → checks authentication token
     *          MetricsInterceptor.preHandle()     → starts timing
     *          LocaleChangeInterceptor.preHandle()→ sets locale from request
     *
     *     → If any interceptor returns false → request is aborted (response already committed)
     *
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     * STEP 5: HANDLER ADAPTER — invoke the controller method
     * ═══════════════════════════════════════════════════════════════════════════════
     *
     *   RequestMappingHandlerAdapter.handle(request, response, handler):
     *
     *   5.1 ARGUMENT RESOLUTION:
     *         InvocableHandlerMethod.invokeForRequest(request, mavContainer, args...)
     *
     *         HandlerMethodArgumentResolver resolves each parameter:
     *
     *           @PathVariable Long id:
     *             → PathVariableMethodArgumentResolver
     *             → Extracts "42" from URI template /api/v1/orders/{id}
     *             → Converts "42" (String) → Long (42L) via ConversionService
     *             → Provides Long 42L as argument
     *
     *           @RequestBody CreateOrderRequest:
     *             → RequestResponseBodyMethodProcessor
     *             → Reads request body stream
     *             → Selects HttpMessageConverter based on Content-Type: application/json
     *             → Calls Jackson's ObjectMapper.readValue(stream, CreateOrderRequest.class)
     *             → If @Valid present → runs Jakarta Bean Validation
     *             → Returns CreateOrderRequest object
     *
     *           @RequestParam String dept:
     *             → RequestParamMethodArgumentResolver
     *             → Reads request.getParameter("dept")
     *             → Applies defaultValue or throws if required and missing
     *
     *           @RequestHeader String auth:
     *             → RequestHeaderMethodArgumentResolver
     *             → Reads request.getHeader("Authorization")
     *
     *           HttpServletRequest, HttpServletResponse, HttpSession:
     *             → ServletRequestMethodArgumentResolver
     *             → Injects native Servlet objects directly
     *
     *   5.2 METHOD INVOCATION:
     *         java.lang.reflect.Method.invoke(controller, resolvedArgs...)
     *
     *         → Calls OrderController.cancelOrder(42L)
     *         → Method executes business logic
     *         → Returns an Order object (or ResponseEntity, void, etc.)
     *
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     * STEP 6: RETURN VALUE HANDLING
     * ═══════════════════════════════════════════════════════════════════════════════
     *
     *   HandlerMethodReturnValueHandler processes the return value:
     *
     *   CASE A: Return type is ResponseEntity<Order>
     *     → HttpEntityMethodProcessor handles it
     *     → Sets response status from ResponseEntity.getStatusCode()
     *     → Sets headers from ResponseEntity.getHeaders()
     *     → Serialises ResponseEntity.getBody() → response body
     *
     *   CASE B: Return type is Order (plain object with @ResponseBody / @RestController)
     *     → RequestResponseBodyMethodProcessor handles it
     *     → Status is 200 OK (default)
     *     → Serialises Order → response body
     *
     *   SERIALISATION (how Order → JSON):
     *     ContentNegotiationManager checks:
     *       1. Accept header: application/json
     *       2. URL extension: none
     *       3. Default: application/json (if configured)
     *     → Selects MappingJackson2HttpMessageConverter
     *     → Calls objectMapper.writeValueAsBytes(order)
     *     → Writes bytes to HttpServletResponse.getOutputStream()
     *     → Sets Content-Type: application/json header
     *
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     * STEP 7: HANDLER INTERCEPTORS — postHandle() and afterCompletion()
     * ═══════════════════════════════════════════════════════════════════════════════
     *
     *   After method returns successfully:
     *   handlerExecutionChain.applyPostHandle(request, response, mv):
     *     → Calls postHandle() on each interceptor (in REVERSE order)
     *     → Can modify ModelAndView (for MVC view rendering)
     *     → Cannot change response status at this point
     *
     *   After response is committed:
     *   handlerExecutionChain.triggerAfterCompletion(request, response, ex):
     *     → Calls afterCompletion() on each interceptor (in REVERSE order)
     *     → Always called, even if an exception occurred
     *     → Used for cleanup: releasing resources, recording metrics
     *
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     * STEP 8: EXCEPTION HANDLING (if an exception was thrown in STEP 5 or 6)
     * ═══════════════════════════════════════════════════════════════════════════════
     *
     *   DispatcherServlet.processHandlerException(request, response, handler, ex):
     *
     *   Iterates HandlerExceptionResolvers in order:
     *
     *   A) ExceptionHandlerExceptionResolver  (handles @ExceptionHandler methods)
     *      → Checks if there's @ExceptionHandler in the SAME controller
     *      → If yes → invokes that method and writes response
     *      → If no  → checks @ControllerAdvice / @RestControllerAdvice beans
     *      → If found → invokes global @ExceptionHandler method
     *
     *   B) ResponseStatusExceptionResolver    (handles @ResponseStatus on exceptions)
     *      → Checks if the exception class has @ResponseStatus annotation
     *      → Sets the status and writes a basic error page
     *
     *   C) DefaultHandlerExceptionResolver    (handles Spring's internal exceptions)
     *      → HttpMessageNotReadableException  → 400 Bad Request
     *      → MethodArgumentTypeMismatchException → 400 Bad Request
     *      → HttpRequestMethodNotSupportedException → 405 Method Not Allowed
     *      → HttpMediaTypeNotAcceptableException → 406 Not Acceptable
     *      → etc.
     *
     *   D) If no resolver handles it → the exception propagates to the Servlet container
     *      → Tomcat generates a generic 500 error page
     *
     *
     * ═══════════════════════════════════════════════════════════════════════════════
     * STEP 9: RESPONSE SENT TO CLIENT
     * ═══════════════════════════════════════════════════════════════════════════════
     *
     *   Tomcat flushes the response:
     *     HTTP/1.1 200 OK
     *     Content-Type: application/json
     *     Content-Length: 287
     *     Access-Control-Allow-Origin: https://myapp.com   (if CORS configured)
     *
     *     { "id": 42, "status": "CANCELLED", ... }
     *
     *   Client (JavaScript) receives and reads the response.
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 3: HOW @RequestMapping MATCHING WORKS                           ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * At STARTUP, RequestMappingHandlerMapping scans all @Controller beans
     * and builds a lookup map:
     *
     *   Map<RequestMappingInfo, HandlerMethod>
     *
     * RequestMappingInfo encodes:
     *   - URL patterns (e.g., /api/v1/orders/{id})
     *   - HTTP methods (DELETE)
     *   - Required headers
     *   - Required params
     *   - Consumed content types
     *   - Produced content types
     *
     * AT REQUEST TIME, matching algorithm:
     *
     *   STEP A: Pattern matching — find all mappings whose URL pattern matches the request URI
     *     /api/v1/orders/42 → matches /api/v1/orders/{id}  (with id=42)
     *     /api/v1/orders/42 → does NOT match /api/v1/orders/{id}/items
     *
     *   STEP B: Method matching — filter by HTTP method (DELETE)
     *     → Only @DeleteMapping or @RequestMapping(method=DELETE) survive
     *
     *   STEP C: Narrowing conditions — filter by headers, params, consumes, produces
     *     → consumes = "application/json": only if Content-Type is application/json
     *
     *   STEP D: Best match selection
     *     → If multiple mappings match, use specificity rules:
     *          /api/v1/orders/cancel  beats  /api/v1/orders/{id}  (literal > template)
     *          DELETE beats GET (when both would match otherwise)
     *
     *   STEP E: No match → 405 Method Not Allowed (URL matched but method didn't)
     *            or → 404 Not Found (URL didn't match)
     *
     * WHY 405 vs 404?
     *   404 = The URL itself doesn't exist
     *   405 = The URL exists but that HTTP method is not supported
     *         → Spring helpfully includes Allow: GET, POST in the 405 response!
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 4: HOW HttpMessageConverters WORK                               ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * HttpMessageConverter is the bridge between Java objects and HTTP message bodies.
     *
     * BUILT-IN CONVERTERS (registered by Spring Boot auto-configuration):
     *
     *   StringHttpMessageConverter
     *     → Java String  (reads/writes)  text/plain, text/...
     *
     *   ByteArrayHttpMessageConverter
     *     → byte[]  (reads/writes)  application/octet-stream
     *
     *   ResourceHttpMessageConverter
     *     → Resource  (reads/writes)  all media types (file downloads)
     *
     *   MappingJackson2HttpMessageConverter
     *     → Any POJO  (reads/writes)  application/json  (THE MAIN ONE)
     *     → Uses Jackson ObjectMapper (configurable)
     *
     *   Jaxb2RootElementHttpMessageConverter (if JAXB2 present)
     *     → @XmlRootElement classes  (reads/writes)  application/xml, text/xml
     *
     *   FormHttpMessageConverter
     *     → Map (reads/writes)  application/x-www-form-urlencoded
     *
     * HOW THE RIGHT CONVERTER IS SELECTED:
     *
     *   FOR REQUEST (@RequestBody):
     *     1. Read Content-Type from request header
     *     2. Find a converter that can read the content type → target Java type
     *     3. Call converter.read(targetType, request)
     *
     *     Content-Type: application/json + target: Order.class
     *       → MappingJackson2HttpMessageConverter.read(Order.class, request)
     *       → Jackson: objectMapper.readValue(inputStream, Order.class)
     *
     *   FOR RESPONSE (@ResponseBody / ResponseEntity):
     *     1. Determine target content type from:
     *          a. Accept header from client
     *          b. produces attribute of @GetMapping
     *          c. Default (first converter that can write the type)
     *     2. Find a converter that can write Java type → content type
     *     3. Call converter.write(order, contentType, response)
     *
     *     Accept: application/json + source: Order object
     *       → MappingJackson2HttpMessageConverter.write(order, APPLICATION_JSON, response)
     *       → Jackson: objectMapper.writeValueAsBytes(order) → response body
     *       → Sets Content-Type: application/json
     *
     * JACKSON CONFIGURATION (in Spring Boot):
     *
     *   Spring Boot auto-configures ObjectMapper with:
     *     • FAIL_ON_UNKNOWN_PROPERTIES = false (ignores extra JSON fields)
     *     • WRITE_DATES_AS_TIMESTAMPS = false (ISO-8601 date strings)
     *
     *   You can customise it:
     *     @Bean
     *     Jackson2ObjectMapperBuilderCustomizer customizer() {
     *         return builder -> builder
     *             .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
     *             .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
     *     }
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 5: HOW @PathVariable AND @RequestParam ARGUMENT RESOLUTION WORKS ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * ARGUMENT RESOLUTION PIPELINE:
     *
     *   For each method parameter, Spring iterates its HandlerMethodArgumentResolver list
     *   and calls supportsParameter() on each until one returns true. Then calls
     *   resolveArgument() to get the value.
     *
     * @PathVariable Long id  — PathVariableMethodArgumentResolver:
     *
     *   1. URL template: /api/v1/orders/{id}  + URI: /api/v1/orders/42
     *   2. AntPathMatcher extracts {id} = "42"  (stored in request attributes as Map)
     *   3. PathVariableMethodArgumentResolver reads from the map: "42" (String)
     *   4. ConversionService converts String "42" → Long 42L
     *      (TypeConverterDelegate uses registered type converters)
     *   5. Returns 42L
     *
     *   WHAT IF CONVERSION FAILS?
     *     /api/v1/orders/abc → "abc" cannot convert to Long
     *     → MethodArgumentTypeMismatchException thrown
     *     → DefaultHandlerExceptionResolver catches it → 400 Bad Request
     *
     * @RequestParam String dept:
     *
     *   1. Read request.getParameterMap()
     *   2. Find "dept" key
     *   3. If missing and required=true → MissingServletRequestParameterException → 400
     *   4. If missing and required=false → null
     *   5. If missing and defaultValue specified → use defaultValue
     *   6. If found → type-convert the value
     *
     * @RequestBody Order:
     *
     *   1. Find HttpMessageConverter for Content-Type: application/json → Order.class
     *   2. Call converter.read() → Object
     *   3. If @Valid present:
     *      a. Get SmartValidator (Hibernate Validator)
     *      b. validator.validate(order, bindingResult)
     *      c. If errors → throw MethodArgumentNotValidException
     *   4. Return validated Order object
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 6: COMPLETE VISUAL FLOW DIAGRAM                                 ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     *   [Browser]
     *     │  DELETE /api/v1/orders/42
     *     │  Authorization: Bearer eyJ...
     *     │  Origin: https://myapp.com
     *     ▼
     *   [Tomcat]
     *     │  Parses HTTP → HttpServletRequest
     *     ▼
     *   ┌──────────────────────────────────────────────────────┐
     *   │                 SERVLET FILTER CHAIN                  │
     *   │  CorsFilter → CharacterEncodingFilter → SecurityChain │
     *   └───────────────────────────┬──────────────────────────┘
     *                               ▼
     *   ┌──────────────────────────────────────────────────────┐
     *   │                 DISPATCHERSERVLET                     │
     *   │                                                       │
     *   │  1. HandlerMapping                                    │
     *   │     RequestMappingHandlerMapping                      │
     *   │     → Matches DELETE /api/v1/orders/42               │
     *   │     → Finds: OrderController#cancelOrder(Long)        │
     *   │                                                       │
     *   │  2. HandlerInterceptor.preHandle()                    │
     *   │     → LoggingInterceptor (logs request)               │
     *   │     → MetricsInterceptor (starts timer)               │
     *   │                                                       │
     *   │  3. HandlerAdapter (RequestMappingHandlerAdapter)     │
     *   │     Argument Resolution:                              │
     *   │       @PathVariable Long id = 42L                    │
     *   │     Method Invocation:                                │
     *   │       OrderController.cancelOrder(42L)               │
     *   │     Return: Order{id=42, status=CANCELLED}           │
     *   │                                                       │
     *   │  4. Return Value Handling                             │
     *   │     HttpEntityMethodProcessor (for ResponseEntity)   │
     *   │     OR RequestResponseBodyMethodProcessor (@ResponseBody)│
     *   │     Content negotiation → application/json           │
     *   │     MappingJackson2HttpMessageConverter writes JSON   │
     *   │     Response status: 200 OK                          │
     *   │                                                       │
     *   │  5. HandlerInterceptor.postHandle()                   │
     *   │  6. HandlerInterceptor.afterCompletion()              │
     *   └───────────────────────────┬──────────────────────────┘
     *                               ▼
     *   [Tomcat]
     *     │  Flushes response bytes
     *     ▼
     *   [Browser]
     *     HTTP/1.1 200 OK
     *     Content-Type: application/json
     *     { "id": 42, "status": "CANCELLED", ... }
     *
     *   ── EXCEPTION PATH ──────────────────────────────────────────────────────
     *   If OrderController.cancelOrder(42L) throws ResourceNotFoundException:
     *
     *     DispatcherServlet catches it
     *       ↓
     *     ExceptionHandlerExceptionResolver
     *       → Checks OrderController for @ExceptionHandler(ResourceNotFoundException.class)
     *       → FOUND → invokes OrderController#handleNotFound(ex, request)
     *       → Returns ErrorResponse{status:404, ...}
     *       → MappingJackson2HttpMessageConverter writes ErrorResponse to response
     *     Response: 404 Not Found + ErrorResponse JSON
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 7: SPRING MVC AUTO-CONFIGURATION IN SPRING BOOT               ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Spring Boot auto-configures all MVC components via WebMvcAutoConfiguration.
     *
     * KEY AUTO-CONFIGURED BEANS:
     *
     *   DispatcherServletAutoConfiguration
     *     → Registers DispatcherServlet at "/" (all requests)
     *     → Maps DispatcherServletRegistrationBean to Tomcat
     *
     *   WebMvcAutoConfiguration
     *     → RequestMappingHandlerMapping        (processes @RequestMapping)
     *     → RequestMappingHandlerAdapter        (invokes handler methods)
     *     → MappingJackson2HttpMessageConverter (JSON serialisation)
     *     → ContentNegotiationManager           (Accept header handling)
     *     → PathMatchConfigurer                 (URL pattern matching)
     *
     *   JacksonAutoConfiguration
     *     → ObjectMapper bean (Jackson's main serialiser)
     *     → Configured with Spring Boot defaults
     *
     *   HttpMessageConvertersAutoConfiguration
     *     → Collects all HttpMessageConverter beans
     *     → Registers them with RequestMappingHandlerAdapter
     *
     * TO CUSTOMISE MVC CONFIGURATION:
     *
     *   Implement WebMvcConfigurer (non-destructive — adds to existing config):
     *     @Configuration
     *     class MyMvcConfig implements WebMvcConfigurer {
     *         @Override void addInterceptors(InterceptorRegistry r) { ... }
     *         @Override void addCorsMappings(CorsRegistry r) { ... }
     *         @Override void configureMessageConverters(List<HttpMessageConverter<?>> c) { ... }
     *     }
     *
     *   OR use @EnableWebMvc (DESTRUCTIVE — disables auto-configuration):
     *     @EnableWebMvc replaces ALL Spring Boot MVC auto-configuration.
     *     You must configure everything manually. Avoid unless absolutely necessary.
     *
     */

    /*
     * ╔═══════════════════════════════════════════════════════════════════════════════╗
     * ║                                                                               ║
     * ║     SECTION 8: MENTAL MODEL — THE SPRING MVC "ASSEMBLY LINE"               ║
     * ║                                                                               ║
     * ╚═══════════════════════════════════════════════════════════════════════════════╝
     *
     * Think of Spring MVC as an ASSEMBLY LINE in a factory:
     *
     *  1. RECEIVING DOCK (Tomcat)
     *     Raw HTTP bytes arrive. Parsed into HttpServletRequest.
     *
     *  2. SECURITY CHECKPOINT (Filter Chain)
     *     "Do you have clearance to enter?" (JWT validation, CORS check)
     *
     *  3. ROUTER (HandlerMapping)
     *     "Which department handles DELETE /orders/42?"
     *     → "That's the Order Department, Station 3 (OrderController.cancelOrder)"
     *
     *  4. PREPARATION STATION (Argument Resolution)
     *     "Unwrap and prepare the materials:
     *       - Extract ID 42 from the URL (@PathVariable)
     *       - Parse JSON body into Java object (@RequestBody)
     *       - Look up Authorization token (@RequestHeader)"
     *
     *  5. WORKBENCH (Handler Method Execution)
     *     "OrderController.cancelOrder(42L) — do the actual work"
     *     Returns the finished product (Order object or ResponseEntity)
     *
     *  6. PACKAGING (HttpMessageConverter)
     *     "Package the order object as JSON, set Content-Type: application/json"
     *
     *  7. QUALITY CONTROL (HandlerInterceptor.postHandle)
     *     "Add metrics, log the response, verify headers"
     *
     *  8. SHIPPING DOCK (Tomcat response flush)
     *     "Ship the HTTP response back to the client"
     *
     *  EXCEPTION HANDLING = QUALITY CONTROL REWORK STATION:
     *     "Something went wrong in step 5 (OrderNotFoundException).
     *      Send to rework station (@ExceptionHandler / @RestControllerAdvice).
     *      Package a proper error response (404 Not Found + ErrorResponse JSON).
     *      Ship that instead."
     *
     * This mental model maps 1:1 to every component in this chapter:
     *
     *   @RestController   → WORKBENCH (where work gets done)
     *   @RequestMapping   → ROUTER address (which station handles which request)
     *   @PathVariable     → PREPARATION STATION: extract from URL path
     *   @RequestParam     → PREPARATION STATION: extract from query string
     *   @RequestBody      → PREPARATION STATION: unpack the JSON body
     *   @RequestHeader    → PREPARATION STATION: read the metadata labels
     *   @ResponseBody     → PACKAGING: serialise the return value
     *   @ResponseStatus   → SHIPPING LABEL: what status code to put on the package
     *   ResponseEntity    → CUSTOM PACKAGE: control every aspect of the shipment
     *   @ExceptionHandler → REWORK STATION: fix broken packages and ship error reports
     *   @CrossOrigin      → SHIPPING POLICY: who are we allowed to ship to?
     *
     */
}


