# Running the WASAC/REG Utility Billing System on a New Computer

This is a **Java 17 + Spring Boot + PostgreSQL** backend API.
No Node.js, no Docker, no Python — just Java and a database.

---

## Step 1 — Install Java 17 (JDK)

1. Go to: https://adoptium.net/temurin/releases/?version=17
2. Select **Windows**, **x64**, **JDK**, `.msi` installer
3. Download and run the installer (keep all defaults)
4. Open a new terminal and verify:
   ```
   java -version
   ```
   Expected output: `openjdk version "17.x.x"`

> Maven is already bundled in the project (`mvnw.cmd`). You do NOT need to install Maven separately.

---

## Step 2 — Install PostgreSQL

1. Go to: https://www.postgresql.org/download/windows/
2. Click **Download the installer** (EDB installer)
3. Choose the latest **PostgreSQL 16** or **17** version, Windows x86-64
4. Run the installer with these settings:
   - **Password for the postgres superuser:** `margarine`
   - **Port:** `5432` (default — do not change)
   - **Locale:** default
   - Install Stack Builder: optional, you can skip it
5. After installation, open **pgAdmin** (installed with PostgreSQL) or open the **SQL Shell (psql)** from the Start menu

---

## Step 3 — Create the database

In pgAdmin:
- Right-click **Databases** → **Create** → **Database**
- Name it: `utility_billing_db`
- Click **Save**

**Or** in the SQL Shell (psql), type:
```sql
CREATE DATABASE utility_billing_db;
```
Then press Enter.

---

## Step 4 — Clone the project

If you have Git installed:
```
git clone <your-repository-url>
cd utility-billing
```

Or copy the project folder manually to your computer.

---

## Step 5 — Verify the configuration

Open the file:
```
src\main\resources\application.properties
```

Make sure these values match your PostgreSQL setup:

```
spring.datasource.url=jdbc:postgresql://localhost:5432/utility_billing_db
spring.datasource.username=postgres
spring.datasource.password=margarine
```

Gmail SMTP (for email OTPs) is already configured. If you do not want emails, change:
```
app.mail.enabled=false
```

---

## Step 6 — Open a terminal in the project folder

Open PowerShell or Command Prompt and navigate to the project root:
```
cd C:\path\to\utility-billing
```

---

## Step 7 — Start PostgreSQL service (if not running automatically)

In PowerShell (run as Administrator): 
```
net start postgresql-x64-17
```

> Check the exact service name with:`Get-Service -Name "postgresql*"`
> Replace `postgresql-x64-17` with whatever name appears.

---


If port 8080 is still occupied after stopping, find and kill the process:
```
netstat -ano | findstr :8080
taskkill /PID <the_pid_number> /F

## Step 8 — Run the application

In the project root folder:
```
.\mvnw.cmd spring-boot:run
```

The first run will **download all Maven dependencies** (requires internet, may take 2–5 minutes).

Wait until you see this line in the output:
```
Started JavaApplication in X.XXX seconds
```

The app is now running on port **8080**.

---

## Step 9 — Open the API in your browser

```
http://localhost:8080/swagger-ui/index.html
```

You will see the full interactive API documentation.

---

## Step 10 — Log in with seeded credentials

On the very first startup, the app automatically creates these accounts:

| Role | Email | Password |
|------|-------|----------|
| `ADMIN` | `admin@utility.rw` | `Admin123!` |
| `OPERATOR` | `operator@utility.rw` | `Operator123!` |
| `FINANCE` | `finance@utility.rw` | `Finance123!` |
| `CUSTOMER` | `customer@utility.rw` | `Customer123!` |

Also seeded automatically: 1 customer profile, 2 meters (`WATER-0001` and `ELEC-0001`), tariffs, taxes, and penalty configuration.

**To log in via Swagger:**
1. Open `POST /api/v1/auth/login` → click **Try it out**
2. Enter an email and password from the table above
3. Click **Execute**
4. Copy the `token` value from the response
5. Click the **Authorize** button at the top right of the page
6. Paste the token and click **Authorize**

---

## Running the automated tests

Tests use an in-memory H2 database — PostgreSQL does NOT need to be running:
```
.\mvnw.cmd test
```

---

## Building a deployable JAR (optional)

```
.\mvnw.cmd package -DskipTests
```

Run the built JAR:
```
java -jar target\system-0.0.1-SNAPSHOT.jar
```

---

## Stopping the application

Press `Ctrl + C` in the terminal where the app is running.

If port 8080 is still occupied after stopping, find and kill the process:
```
netstat -ano | findstr :8080
taskkill /PID <the_pid_number> /F
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `Port 8080 was already in use` | Run the `netstat` and `taskkill` commands above |
| `Connection to localhost:5432 refused` | PostgreSQL is not running — run `net start postgresql-x64-17` |
| `password authentication failed for user "postgres"` | Wrong password in `application.properties` — update `spring.datasource.password` |
| `database "utility_billing_db" does not exist` | Run `CREATE DATABASE utility_billing_db;` in pgAdmin or psql |
| `java: command not found` | JDK 17 is not installed or not on the PATH — reinstall from Adoptium |
| 80 errors in the IDE Problems panel | Normal on first open — disappear once Maven downloads dependencies on first run |

---

## Project structure (at a glance)

```
utility-billing/
├── src/main/java/com/miguel/app/system/
│   ├── controller/        REST endpoints
│   ├── service/impl/      Business logic
│   ├── service/interfaces/Service contracts
│   ├── entity/            Database table models
│   ├── repository/        Database queries
│   ├── security/          JWT and authentication filter
│   ├── config/            App startup, seeding, OpenAPI, DB routines
│   ├── dto/               Request and response objects
│   ├── enums/             Role, Status, MeterType, etc.
│   └── exception/         Error handling
├── src/main/resources/
│   ├── application.properties        Main configuration
│   ├── application-test.properties   Test configuration (H2 in-memory)
│   └── db/routines.sql               PostgreSQL triggers and stored procedures
├── src/test/                         Integration tests
├── postman/                          Postman collection and environment
├── docs/                             Business rules, API guide, DB schema
├── pom.xml                           Maven dependencies
└── mvnw.cmd                          Maven Wrapper (no Maven install needed)
```
