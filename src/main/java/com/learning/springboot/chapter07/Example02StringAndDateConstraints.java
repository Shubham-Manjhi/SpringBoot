package com.learning.springboot.chapter07;

import jakarta.validation.constraints.*;
import java.time.*;

/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 * ║                                                                                       ║
 * ║             EXAMPLE 02: STRING & DATE/TIME CONSTRAINT ANNOTATIONS                    ║
 * ║                                                                                       ║
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * File:        Example02StringAndDateConstraints.java
 * Purpose:     Demonstrate @Email, @Pattern, @Past, @PastOrPresent,
 *              @Future, @FutureOrPresent — with deep explanations and real-world
 *              domain examples across multiple request objects.
 * Difficulty:  ⭐⭐ Beginner–Intermediate
 * Time:        30–45 minutes
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */


// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION A — @Email & @Pattern (String format constraints)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║        ContactRequest — @Email and @Pattern Constraints                      ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
class ContactRequest {

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Email  — Valid email address format
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the annotated String is a syntactically correct email address.
     *   Uses a built-in regular expression compliant with the email format.
     *
     * PACKAGE: jakarta.validation.constraints.Email
     *
     * APPLIES TO: CharSequence (String and subclasses)
     *
     * IMPORTANT: null is treated as VALID by @Email.
     *            If you need to require an email, combine with @NotBlank:
     *              @NotBlank @Email
     *
     * KEY ATTRIBUTES:
     *
     *   message  → Custom error message (default: "must be a well-formed email address")
     *
     *   regexp   → Optional extra regex that the email must also match.
     *              Use for stricter validation (e.g., only allow company emails).
     *
     *   flags    → Regex flags (e.g., Pattern.Flag.CASE_INSENSITIVE)
     *
     * WHAT IT VALIDATES:
     *   Passes: "user@example.com", "john.doe+tag@company.org", "a@b.co"
     *   Fails:  "notanemail", "missing@", "@nodomain", "user @example.com"
     *
     * NOTE: @Email does NOT verify that the email actually EXISTS — only the FORMAT.
     *       For real verification, send a confirmation email with a link.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email address (e.g., user@example.com)")
    private String email;

    /*
     * EXAMPLE: Strict email — only company domain
     *
     * regexp = "^[a-zA-Z0-9._%+\\-]+@company\\.com$"
     * → Only allows @company.com emails
     *
     * Used in internal tools, admin panels, HR systems.
     */
    @Email(
        regexp  = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$",
        flags   = Pattern.Flag.CASE_INSENSITIVE,
        message = "Please provide a valid email address"
    )
    private String strictEmail;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Pattern(regexp)  — Custom regular expression matching
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the String matches the given Java regular expression.
     *   null is treated as VALID — combine with @NotBlank if null must be rejected.
     *
     * PACKAGE: jakarta.validation.constraints.Pattern
     *
     * APPLIES TO: CharSequence (String and subclasses)
     *
     * KEY ATTRIBUTES:
     *
     *   regexp   → (Required) The Java regex the value must match COMPLETELY
     *              (as if you added ^ and $ anchors around it — uses Matcher.matches()).
     *
     *   flags    → Array of Pattern.Flag (CASE_INSENSITIVE, MULTILINE, etc.)
     *
     *   message  → Custom error message
     *              TIP: Use {regexp} placeholder to include the pattern in the message.
     *
     * IMPORTANT: The regex is applied to the ENTIRE string (full match), not partial.
     *            "^A" would match "A" but NOT "AB" — test your regex carefully!
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */

    /*
     * UK Phone number pattern:
     *   +44 followed by 10 digits, OR 07 followed by 9 digits
     *   Allows optional spaces between digit groups
     */
    @Pattern(
        regexp  = "^(\\+44\\s?7\\d{3}|\\(?07\\d{3}\\)?)\\s?\\d{3}\\s?\\d{3}$",
        message = "Please provide a valid UK phone number (e.g., +44 7911 123456)"
    )
    private String ukPhoneNumber;

    /*
     * International phone — E.164 format: +[country code][number], 8–15 digits total
     * Examples: +12025551234, +447911123456, +91 9876543210 (spaces optional)
     */
    @Pattern(
        regexp  = "^\\+[1-9]\\d{7,14}$",
        message = "Phone must be in E.164 format: +[country code][number] (e.g., +12025551234)"
    )
    private String internationalPhone;

    /*
     * Postal / ZIP code — accepts common formats:
     *   UK postcode:  SW1A 1AA
     *   US ZIP:       12345 or 12345-6789
     *   Canadian:     K1A 0B1
     */
    @Pattern(
        regexp  = "^[A-Z0-9]{2,10}([ \\-]?[A-Z0-9]{2,8})?$",
        flags   = Pattern.Flag.CASE_INSENSITIVE,
        message = "Please provide a valid postal code"
    )
    private String postalCode;

    /*
     * Username pattern:
     *   3–20 chars
     *   Only letters, digits, underscore, hyphen
     *   Must start with a letter
     */
    @NotBlank(message = "Username is required")
    @Pattern(
        regexp  = "^[a-zA-Z][a-zA-Z0-9_\\-]{2,19}$",
        message = "Username must be 3–20 characters, start with a letter, and contain only letters, digits, _ or -"
    )
    private String username;

    /*
     * Hex color code: #RRGGBB or #RGB format
     * Examples: #FF5733, #fff, #1a2b3c
     */
    @Pattern(
        regexp  = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
        message = "Color must be a valid hex code (e.g., #FF5733 or #fff)"
    )
    private String hexColor;

    // ── Getters & Setters ─────────────────────────────────────────────────────────
    public String getEmail()               { return email; }
    public void   setEmail(String e)       { this.email = e; }
    public String getStrictEmail()         { return strictEmail; }
    public void   setStrictEmail(String e) { this.strictEmail = e; }
    public String getUkPhoneNumber()       { return ukPhoneNumber; }
    public void   setUkPhoneNumber(String p) { this.ukPhoneNumber = p; }
    public String getInternationalPhone()  { return internationalPhone; }
    public void   setInternationalPhone(String p) { this.internationalPhone = p; }
    public String getPostalCode()          { return postalCode; }
    public void   setPostalCode(String p)  { this.postalCode = p; }
    public String getUsername()            { return username; }
    public void   setUsername(String u)    { this.username = u; }
    public String getHexColor()            { return hexColor; }
    public void   setHexColor(String h)    { this.hexColor = h; }
}


// ══════════════════════════════════════════════════════════════════════════════════════
//  SECTION B — Date/Time Constraints (@Past, @PastOrPresent, @Future, @FutureOrPresent)
// ══════════════════════════════════════════════════════════════════════════════════════

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║        PersonProfileRequest — All Date / Time Constraints                    ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
class PersonProfileRequest {

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Past  — Date must be in the past (strictly before "now")
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the date/time is BEFORE the current moment at validation time.
     *   "Now" is determined at validation runtime — not a fixed timestamp.
     *   null is considered VALID — combine with @NotNull.
     *
     * PACKAGE: jakarta.validation.constraints.Past
     *
     * SUPPORTED TYPES:
     *   LocalDate, LocalDateTime, LocalTime, ZonedDateTime, OffsetDateTime,
     *   OffsetTime, Instant, Date (java.util), Calendar
     *
     * USE CASES:
     *   → Date of birth (must be in the past)
     *   → Hire date (must be before today)
     *   → Historical timestamps
     *
     * NOTE: Clock can be configured via ClockProvider in the ValidatorFactory.
     *       This allows testing with fixed clocks.
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be a past date — you can't be born in the future!")
    private LocalDate dateOfBirth;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @PastOrPresent  — Date must be in the past OR exactly now
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the date/time is BEFORE OR EQUAL TO the current moment.
     *   "Now" is inclusive — a timestamp of exactly now PASSES.
     *   null is VALID — combine with @NotNull.
     *
     * DIFFERENCE FROM @Past:
     *   @Past            → strictly before now (today itself FAILS if comparing dates)
     *   @PastOrPresent   → on or before now (today itself PASSES)
     *
     * USE CASES:
     *   → Record creation timestamp: can be "now" or earlier, not future
     *   → Last login time: can be now or in the past
     *   → Document submission date: today or earlier
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @NotNull(message = "Registration date is required")
    @PastOrPresent(message = "Registration date cannot be in the future")
    private LocalDateTime registeredAt;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @Future  — Date must be in the future (strictly after "now")
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the date/time is AFTER the current moment at validation time.
     *   null is VALID — combine with @NotNull.
     *
     * USE CASES:
     *   → Event dates: must be scheduled in the future
     *   → Appointment booking: must be a future slot
     *   → Token expiry: must not have already expired
     *   → Subscription renewal dates
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @NotNull(message = "Subscription expiry date is required")
    @Future(message = "Subscription expiry date must be in the future")
    private LocalDate subscriptionExpiryDate;

    /*
     * ─────────────────────────────────────────────────────────────────────────────
     * 📌 @FutureOrPresent  — Date must be now or in the future
     * ─────────────────────────────────────────────────────────────────────────────
     *
     * WHAT IT DOES:
     *   Validates that the date/time is ON OR AFTER the current moment.
     *   "Now" is inclusive — a timestamp of exactly now PASSES.
     *   null is VALID — combine with @NotNull.
     *
     * DIFFERENCE FROM @Future:
     *   @Future          → strictly after now
     *   @FutureOrPresent → now or after now (useful for "scheduled to run now or later")
     *
     * USE CASES:
     *   → Scheduled job start time: run immediately (now) or later
     *   → Offer validity date: valid from now onwards
     *   → Embargo date: content released now or in the future
     *
     * ─────────────────────────────────────────────────────────────────────────────
     */
    @FutureOrPresent(message = "Scheduled task time must be now or in the future")
    private LocalDateTime scheduledAt;

    // ── Getters & Setters ─────────────────────────────────────────────────────────
    public LocalDate     getDateOfBirth()           { return dateOfBirth; }
    public void          setDateOfBirth(LocalDate d){ this.dateOfBirth = d; }
    public LocalDateTime getRegisteredAt()          { return registeredAt; }
    public void          setRegisteredAt(LocalDateTime r) { this.registeredAt = r; }
    public LocalDate     getSubscriptionExpiryDate()   { return subscriptionExpiryDate; }
    public void          setSubscriptionExpiryDate(LocalDate s) { this.subscriptionExpiryDate = s; }
    public LocalDateTime getScheduledAt()           { return scheduledAt; }
    public void          setScheduledAt(LocalDateTime s) { this.scheduledAt = s; }
}


/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║       EventRequest — Date constraints with all temporal types                ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
class EventRequest {

    @NotBlank(message = "Event name is required")
    @Size(min = 3, max = 200, message = "Event name must be 3–200 characters")
    private String eventName;

    /*
     * LocalDateTime — full date + time (no timezone)
     * Use for events that occur in a specific local time zone
     * and the timezone is stored separately.
     */
    @NotNull(message = "Event start time is required")
    @Future(message = "Event must be scheduled in the future")
    private LocalDateTime startTime;

    /*
     * ZonedDateTime — date + time + timezone
     * Use when events span timezones (e.g., international webinars).
     * @Future and @Past work correctly with ZonedDateTime.
     */
    @NotNull(message = "Event end time (with timezone) is required")
    @Future(message = "Event end time must be in the future")
    private ZonedDateTime endTimeZoned;

    /*
     * Instant — machine timestamp (nanoseconds since Unix epoch)
     * Use for audit timestamps, API response times, precise measurements.
     * @Past and @Future work with Instant.
     */
    @PastOrPresent(message = "Created timestamp cannot be in the future")
    private Instant createdAt;

    /*
     * LocalDate — date only (no time)
     * Use for birth dates, deadlines, event dates where time doesn't matter.
     */
    @NotNull(message = "Registration deadline is required")
    @Future(message = "Registration deadline must be in the future")
    private LocalDate registrationDeadline;

    @Min(value = 1, message = "At least 1 ticket must be available")
    @Max(value = 100000, message = "Cannot offer more than 100,000 tickets")
    private int ticketsAvailable;

    @NotBlank(message = "Organizer email is required")
    @Email(message = "Organizer must have a valid email address")
    private String organizerEmail;

    /*
     * Pattern for event code:
     *   Format: EVT-YYYY-NNNN (e.g., EVT-2026-0042)
     */
    @NotBlank(message = "Event code is required")
    @Pattern(
        regexp  = "^EVT-\\d{4}-\\d{4}$",
        message = "Event code must follow the format: EVT-YYYY-NNNN (e.g., EVT-2026-0042)"
    )
    private String eventCode;

    // ── Getters & Setters ─────────────────────────────────────────────────────────
    public String        getEventName()      { return eventName; }
    public void          setEventName(String n) { this.eventName = n; }
    public LocalDateTime getStartTime()      { return startTime; }
    public void          setStartTime(LocalDateTime s) { this.startTime = s; }
    public ZonedDateTime getEndTimeZoned()   { return endTimeZoned; }
    public void          setEndTimeZoned(ZonedDateTime e) { this.endTimeZoned = e; }
    public Instant       getCreatedAt()      { return createdAt; }
    public void          setCreatedAt(Instant c) { this.createdAt = c; }
    public LocalDate     getRegistrationDeadline() { return registrationDeadline; }
    public void          setRegistrationDeadline(LocalDate r) { this.registrationDeadline = r; }
    public int           getTicketsAvailable(){ return ticketsAvailable; }
    public void          setTicketsAvailable(int t) { this.ticketsAvailable = t; }
    public String        getOrganizerEmail() { return organizerEmail; }
    public void          setOrganizerEmail(String o) { this.organizerEmail = o; }
    public String        getEventCode()      { return eventCode; }
    public void          setEventCode(String e) { this.eventCode = e; }
}


/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                               ║
 * ║       REAL-WORLD REFERENCE: Common @Pattern Examples                         ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * Collected here for quick copy-paste reference. Each is a separate field to show
 * the annotation without cluttering a single class.
 * ─────────────────────────────────────────────────────────────────────────────────
 */
class PatternExamplesReference {

    // ── Credit card number (Luhn not checked, just format) ────────────────────────
    @Pattern(regexp = "^[0-9]{13,19}$",
             message = "Credit card number must be 13–19 digits")
    private String creditCardNumber;

    // ── CVV / CVC code ─────────────────────────────────────────────────────────────
    @Pattern(regexp = "^[0-9]{3,4}$",
             message = "CVV must be 3 or 4 digits")
    private String cvv;

    // ── UUID format ────────────────────────────────────────────────────────────────
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
             message = "Must be a valid UUID (e.g., 550e8400-e29b-41d4-a716-446655440000)")
    private String uuid;

    // ── IP address (IPv4) ─────────────────────────────────────────────────────────
    @Pattern(regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
             message = "Must be a valid IPv4 address (e.g., 192.168.1.1)")
    private String ipv4Address;

    // ── URL (basic format) ─────────────────────────────────────────────────────────
    @Pattern(regexp = "^https?://[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+$",
             message = "Must be a valid HTTP/HTTPS URL")
    private String url;

    // ── Strong password ────────────────────────────────────────────────────────────
    // At least 8 chars, one uppercase, one lowercase, one digit, one special char
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
             message = "Password must be at least 8 chars with uppercase, lowercase, digit, and special char")
    private String strongPassword;

    // ── ISO currency code (3 uppercase letters) ────────────────────────────────────
    @Pattern(regexp = "^[A-Z]{3}$",
             message = "Currency code must be 3 uppercase letters (e.g., USD, EUR, GBP)")
    private String currencyCode;

    // ── Time in HH:MM format ───────────────────────────────────────────────────────
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
             message = "Time must be in HH:MM format (e.g., 09:30, 23:59)")
    private String timeSlot;

    // Getters (for demonstration)
    public String getCreditCardNumber() { return creditCardNumber; }
    public String getCvv()              { return cvv; }
    public String getUuid()             { return uuid; }
    public String getIpv4Address()      { return ipv4Address; }
    public String getUrl()              { return url; }
    public String getStrongPassword()   { return strongPassword; }
    public String getCurrencyCode()     { return currencyCode; }
    public String getTimeSlot()         { return timeSlot; }
}


/*
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *                              📚 LEARNING SUMMARY 📚
 * ═══════════════════════════════════════════════════════════════════════════════════════
 *
 * ✅ WHAT WE LEARNED IN EXAMPLE 02:
 *
 *  ANNOTATION          NULL VALID?   APPLIES TO              KEY BEHAVIOUR
 *  ─────────────────   ──────────    ─────────────────────   ──────────────────────────────────────
 *  @Email              Yes           CharSequence            Format check (not existence check)
 *                                                            Combine with @NotBlank if required
 *  @Pattern(regexp)    Yes           CharSequence            Full string match (Matcher.matches())
 *                                                            Uses Java regex syntax
 *  @Past               Yes           All date/time types     Strictly BEFORE now
 *  @PastOrPresent      Yes           All date/time types     On or BEFORE now (inclusive)
 *  @Future             Yes           All date/time types     Strictly AFTER now
 *  @FutureOrPresent    Yes           All date/time types     On or AFTER now (inclusive)
 *
 * KEY REMINDERS:
 *   → @Email and @Pattern both treat null as VALID. Always add @NotBlank for required fields.
 *   → @Pattern uses full-string matching — no need for ^ and $ anchors (added automatically).
 *   → Date constraints use a ClockProvider for "now" — testable with fixed clocks.
 *   → ZonedDateTime, Instant, and LocalDateTime are all supported by date constraints.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * 🧪 EXERCISES:
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 *  1. Validate a string with @Pattern for IBAN format: 2 letters + 2 digits + 8–28 alphanumeric.
 *  2. Create a MeetingRequest: meetingTime (@FutureOrPresent), deadline (@Future).
 *     Test: what happens when you submit meetingTime = LocalDateTime.now()?
 *  3. Add @Email to a field and test with: "valid@email.com", "notanemail", null, "".
 *     Which ones pass and which fail? Why?
 *  4. Build a strong password validator using @Pattern with lookaheads.
 *
 * ─────────────────────────────────────────────────────────────────────────────────
 * ➡️  NEXT: Example03ValidAndValidated.java
 * ─────────────────────────────────────────────────────────────────────────────────
 *
 * ═══════════════════════════════════════════════════════════════════════════════════════
 */
class Example02StringAndDateConstraints {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║   CHAPTER 7 — EXAMPLE 02: String & Date/Time Constraints         ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  @Email             → valid email format (null = valid)");
        System.out.println("  @Pattern(regexp)   → full-string regex match (null = valid)");
        System.out.println("  @Past              → strictly before now");
        System.out.println("  @PastOrPresent     → now or before (inclusive)");
        System.out.println("  @Future            → strictly after now");
        System.out.println("  @FutureOrPresent   → now or after (inclusive)");
        System.out.println();
        System.out.println("Classes: ContactRequest, PersonProfileRequest, EventRequest,");
        System.out.println("         PatternExamplesReference");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}

