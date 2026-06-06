# Spring Boot Practical Exam Agent Constitution

Use this file as the mandatory checklist when building any Spring Boot backend practical exam project. The project topic may change, but the technologies and real-life backend expectations remain the same.

## 1. Core Technology Rules

The project must use:

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- JWT authentication and authorization
- Validation using Jakarta Bean Validation
- MySQL or PostgreSQL database
- Swagger/OpenAPI documentation
- Lombok if allowed
- Maven

Do not build a weak demo project. Build a clean RESTful backend that looks like a real production system.

## 2. Required Spring Initializr Dependencies

When creating the project from Spring Initializr, include:

- Spring Web
- Spring Data JPA
- Spring Security
- Validation
- MySQL Driver or PostgreSQL Driver
- Lombok
- Spring Boot DevTools
- Java Mail Sender if the scenario needs email notifications

Add Swagger manually in `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

Add JWT dependencies manually:

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
```

## 3. Required Package Structure

Use this structure:

```txt
com.exam.project
 ├── config
 ├── controller
 ├── dto
 │   ├── request
 │   └── response
 ├── entity
 ├── enums
 ├── exception
 ├── repository
 ├── security
 ├── service
 │   └── impl
 └── util
```

Never put all logic inside controllers. Controllers should only receive requests, validate, call services, and return responses.

## 4. Mandatory Architecture Flow

Every feature must follow this flow:

```txt
Client / Postman / Swagger
        ↓
Controller
        ↓
DTO validation
        ↓
Service interface
        ↓
Service implementation
        ↓
Repository
        ↓
Database
```

Return clean API responses, not raw internal errors.

## 5. Entity Design Rules

Every entity should include:

- `id`
- unique `code` where useful
- meaningful fields from the scenario
- `status` enum instead of random strings
- `createdAt`
- `updatedAt`
- relationships where needed

Example base fields:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Column(unique = true, nullable = false)
private String code;

private LocalDateTime createdAt;
private LocalDateTime updatedAt;
```

Use lifecycle methods:

```java
@PrePersist
public void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
}

@PreUpdate
public void onUpdate() {
    updatedAt = LocalDateTime.now();
}
```

## 6. DTO Rules

Never expose entity objects directly in controllers.

Create request DTOs and response DTOs:

```txt
CreateEmployeeRequest
UpdateEmployeeRequest
EmployeeResponse
LoginRequest
RegisterRequest
AuthResponse
```

Request DTOs must include validation annotations:

```java
@NotBlank(message = "First name is required")
private String firstName;

@Email(message = "Email must be valid")
@NotBlank(message = "Email is required")
private String email;

@NotNull(message = "Amount is required")
@Positive(message = "Amount must be greater than zero")
private BigDecimal amount;
```

## 7. Input Validation Rules

Validate every user input.

Required validation examples:

- Names must not be empty
- Email must be valid and unique
- Phone number must not be empty where required
- Password must be encrypted
- Amounts must be positive
- Percentages must be between 0 and 100
- Dates must be logical
- Status values must come from enums
- IDs must exist before processing
- Duplicate records must be prevented

Examples of real-life checks:

- Do not create two users with the same email
- Do not create two records with the same code
- Do not process payment twice
- Do not generate duplicate payroll for the same employee/month/year
- Do not assign inactive records
- Do not approve an already approved transaction
- Do not delete important records physically if they are used elsewhere

## 8. Error Handling Rules

Create a global exception handler using `@RestControllerAdvice`.

Required custom exceptions:

```txt
ResourceNotFoundException
BadRequestException
DuplicateResourceException
UnauthorizedException
ForbiddenException
BusinessRuleException
```

All errors must return a clean structure:

```json
{
  "success": false,
  "message": "Employee not found",
  "timestamp": "2026-06-04T15:00:00",
  "path": "/api/employees/10"
}
```

Never expose stack traces to the user.

## 9. Standard API Response Format

All success responses should be consistent:

```json
{
  "success": true,
  "message": "Employee created successfully",
  "data": {}
}
```

For lists:

```json
{
  "success": true,
  "message": "Records retrieved successfully",
  "data": [],
  "total": 10
}
```

## 10. Authentication Rules

Implement:

- Register
- Login
- JWT generation
- Password encryption using `BCryptPasswordEncoder`
- Role-based access control
- Authenticated user profile endpoint

Required endpoints:

```txt
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

Never store plain passwords.

## 11. Authorization Rules

Use roles depending on the scenario.

Example roles:

```txt
ROLE_ADMIN
ROLE_MANAGER
ROLE_EMPLOYEE
ROLE_USER
ROLE_INSPECTOR
```

Protect endpoints with role rules:

```java
@PreAuthorize("hasRole('ADMIN')")
```

or configure them in `SecurityConfig`.

General rule:

- Admin manages everything
- Manager handles business operations
- Normal user views only their own records
- Special role performs scenario-specific actions

## 12. Business Logic Rules

The service layer must enforce real business rules.

Always ask:

- Who is allowed to do this?
- Is the record active?
- Does the record already exist?
- Is the date valid?
- Is the amount valid?
- Can this action be repeated?
- Should this action change status?
- Should a notification be sent?
- Should this be logged?

Never allow invalid state changes.

Example status transition:

```txt
PENDING → APPROVED → COMPLETED
PENDING → REJECTED
COMPLETED cannot go back to PENDING
```

## 13. Database Rules

Use constraints where possible:

```java
@Column(nullable = false)
@Column(unique = true)
```

Use repository checks:

```java
boolean existsByEmail(String email);
boolean existsByCode(String code);
boolean existsByEmployeeAndMonthAndYear(Employee employee, int month, int year);
```

Use transactions for important operations:

```java
@Transactional
public PayrollResponse generatePayroll(...) {
    // important operation
}
```

## 14. CRUD Endpoint Rules

Every main module should have:

```txt
POST   /api/module
GET    /api/module
GET    /api/module/{id}
PUT    /api/module/{id}
DELETE /api/module/{id}
```

Prefer soft delete for real-life systems:

```txt
ACTIVE → INACTIVE
ENABLED → DISABLED
```

Do not physically delete records that may be needed for history.

## 15. Pagination, Searching, and Filtering

For list endpoints, support at least basic retrieval.

If time allows, add:

```txt
?page=0&size=10
?status=ACTIVE
?keyword=john
?fromDate=2026-01-01&toDate=2026-01-31
```

This makes the project look more professional.

## 16. Notification Rules

If the scenario includes notifications, support:

- Email notification
- Notification log table
- Status of sent or failed
- Clear message template

Do not just send email without saving proof.

Example notification entity:

```txt
NotificationLog
- id
- recipientEmail
- subject
- message
- status: SENT / FAILED
- createdAt
```

## 17. Swagger Documentation Rules

Enable Swagger UI and document all APIs.

Swagger URL:

```txt
http://localhost:8080/swagger-ui/index.html
```

Each controller should have:

```java
@Tag(name = "Employees", description = "Employee management APIs")
```

Important endpoints should have:

```java
@Operation(summary = "Create employee")
```

## 18. Security Configuration Rules

Security must allow public access only to:

```txt
/api/auth/**
/swagger-ui/**
/v3/api-docs/**
```

All other endpoints must require authentication.

Use stateless sessions:

```java
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

## 19. Real-Life Small Details Checklist

The AI agent must include these details when possible:

- Clear success and error messages
- Unique codes generated automatically
- Status fields for important records
- Timestamps for created and updated records
- Password encryption
- Duplicate prevention
- Role-based access
- Global exception handling
- DTO validation
- Soft delete instead of hard delete
- Transactional service methods
- Swagger documentation
- Clean package structure
- No business logic inside controllers
- No plain password in responses
- No stack trace exposed to users
- No repeated processing of the same transaction
- Clear status transitions
- Proper HTTP status codes

## 20. Proper HTTP Status Codes

Use correct status codes:

```txt
200 OK - successful retrieval or update
201 CREATED - successful creation
400 BAD REQUEST - invalid input or business rule failure
401 UNAUTHORIZED - not logged in or invalid token
403 FORBIDDEN - logged in but no permission
404 NOT FOUND - record does not exist
409 CONFLICT - duplicate record
500 INTERNAL SERVER ERROR - unexpected server error
```

## 21. Recommended Exam Build Order

Because practical exams are timed, build in this order:

### Step 1: Understand scenario

Identify:

- Main actors
- Roles
- Main entities
- Required calculations
- Required statuses
- Required endpoints
- Business rules

### Step 2: Create project

Use Spring Initializr with required dependencies.

### Step 3: Configure database

Create `application.properties`.

### Step 4: Create entities and enums

Build clean database structure first.

### Step 5: Create repositories

Add useful finder methods and duplicate checks.

### Step 6: Create DTOs

Add validation annotations.

### Step 7: Create exception handling

Add global error handler before many controllers.

### Step 8: Create services

Put all business logic in service implementation.

### Step 9: Create controllers

Expose RESTful APIs.

### Step 10: Add security

Implement JWT login, filters, and role protection.

### Step 11: Add Swagger

Make APIs testable and documented.

### Step 12: Test with Postman or Swagger

Test success cases and error cases.

## 22. Minimum Must-Have Features Before Submission

If time is short, make sure these work:

- Database connects successfully
- At least all main entities are created
- CRUD works for main modules
- Register and login work
- Password is encrypted
- JWT is returned on login
- Protected endpoints require token
- Role-based access works for important endpoints
- Main business calculation works
- Duplicate prevention works
- Global error handling works
- Swagger opens successfully

## 23. Final Agent Instruction

When generating the project, do not only code what is written literally. Think like a real backend engineer. Add missing small details that make the system safe, testable, and realistic, but do not overcomplicate the project beyond exam time.

The final backend must be clean, secure, validated, documented, and able to run locally with Postman or Swagger.
