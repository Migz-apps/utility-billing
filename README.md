# Utility Billing System

Spring Boot backend for a production-style utility billing workflow. The project uses Maven, Java 17, PostgreSQL, Spring Security with JWT, DTO-based APIs, Swagger/OpenAPI, seeded users, and layered services.

## Stack

- Java 17
- Spring Boot 3.3.x
- Maven
- PostgreSQL
- Spring Data JPA
- Spring Security + JWT
- Swagger UI via springdoc-openapi

## Run Locally

1. Create the PostgreSQL database:

```sql
CREATE DATABASE utility_billing_db;
```

2. Configure credentials in `src/main/resources/application.properties`.

3. Start the API:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

## Seeded Users

| Role | Email | Password |
| --- | --- | --- |
| Admin | admin@utility.rw | Admin123! |
| Operator | operator@utility.rw | Operator123! |
| Finance | finance@utility.rw | Finance123! |
| Customer | customer@utility.rw | Customer123! |

## Core Flow

1. Public customer registers through `/api/v1/auth/register`; role is always `ROLE_CUSTOMER`.
2. Operator records a valid meter reading.
3. The system automatically generates a `PENDING_APPROVAL` bill.
4. Admin or Finance approves the bill.
5. Admin or Finance records payments.
6. Customer views only their linked profile, meters, approved/paid/overdue bills, payments, and notifications.

## Verification

```powershell
.\mvnw.cmd test
```

The project also includes PostgreSQL routine SQL in `src/main/resources/db/routines.sql`.
