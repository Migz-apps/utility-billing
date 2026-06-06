# Database Routines

The project includes a PostgreSQL routine at:

```text
src/main/resources/db/routines.sql
```

## Purpose

The routine creates a trigger function that inserts a `BILL_GENERATED` notification after a bill is inserted.

This complements service-layer logic. Java services still own validation, authorization, bill calculation, approval, payment rules, and overdue rules.

## Run Manually

```powershell
$env:PGPASSWORD='miguel'
psql -U postgres -h localhost -d utility_billing_db -f src/main/resources/db/routines.sql
```

## Why It Exists

The exam requirement asks for at least one real database routine, not only service logic. This routine is intentionally small and safe: it records a notification side effect while leaving business decisions in the Spring service layer.
