# Entity Relationship Diagram

Relational model for the Utility Billing System (PostgreSQL). Every table inherits
`id BIGSERIAL PK`, `created_at TIMESTAMP`, `updated_at TIMESTAMP` from a shared
`BaseEntity` mapped superclass — those columns are omitted from each box below for
brevity but exist on every table.

Legend: `PK` = primary key, `FK` = foreign key, `UK` = unique key.

```mermaid
---
id: ac8effe4-67bd-4769-9a2f-e95898ed9104
---
erDiagram
    USERS ||--o| CUSTOMERS : "links optional profile"
    USERS ||--o{ METER_READINGS : "captured_by"
    USERS ||--o{ BILLS : "approved_by"
    USERS ||--o{ PAYMENTS : "recorded_by"

    CUSTOMERS ||--o{ METERS : owns
    CUSTOMERS ||--o{ BILLS : receives
    CUSTOMERS ||--o{ PAYMENTS : "pays"
    CUSTOMERS ||--o{ NOTIFICATIONS : receives

    METERS ||--o{ METER_READINGS : records
    METER_READINGS ||--|| BILLS : "1:1 generates"

    BILLS ||--o{ PAYMENTS : settles
    BILLS ||--o{ NOTIFICATIONS : references

    TARIFFS ||--o{ TARIFF_TIERS : "has (TIERED only)"

    USERS {
        bigint   id                 PK
        string   full_name
        string   email              UK "lowercased; used as username"
        string   phone_number
        string   password           "BCrypt hash"
        enum     role                  "ADMIN | OPERATOR | FINANCE | CUSTOMER"
        enum     status                "ACTIVE | INACTIVE"
        boolean  email_verified
    }

    CUSTOMERS {
        bigint   id                 PK
        string   full_name
        string   national_id        UK "16 chars"
        string   email              UK
        string   phone_number
        string   address
        enum     status                "ACTIVE | INACTIVE (pending admin verify)"
        bigint   user_id            FK "→ users.id (unique, optional)"
    }

    METERS {
        bigint   id                 PK
        string   meter_number       UK
        enum     meter_type            "WATER | ELECTRICITY"
        date     installation_date
        enum     status                "ACTIVE | INACTIVE"
        bigint   customer_id        FK "→ customers.id"
    }

    METER_READINGS {
        bigint     id               PK
        bigint     meter_id         FK "→ meters.id"
        decimal    previous_reading
        decimal    current_reading       "must be > previous"
        decimal    consumption           "current − previous"
        date       reading_date          "@PastOrPresent"
        int        billing_month
        int        billing_year
        bigint     captured_by      FK "→ users.id (operator)"
        UK         meter_month_year_uk   "(meter_id, billing_month, billing_year)"
    }

    BILLS {
        bigint     id                  PK
        string     bill_reference      UK "BILL-YYYY-MM-NNNNNN"
        bigint     customer_id         FK "→ customers.id"
        bigint     meter_id            FK "→ meters.id"
        bigint     meter_reading_id    FK "→ meter_readings.id (1:1 unique)"
        int        billing_month
        int        billing_year
        decimal    consumption
        decimal    tariff_amount          "snapshot from resolved tariff"
        decimal    fixed_charge           "snapshot"
        decimal    tax_amount             "snapshot"
        decimal    penalty_amount         "applied once when overdue"
        decimal    total_amount
        decimal    amount_paid
        decimal    outstanding_balance    "= total_amount − amount_paid"
        enum       status                 "DRAFT | PENDING_APPROVAL | APPROVED | PARTIALLY_PAID | PAID | OVERDUE | CANCELLED"
        date       due_date               "reading_date + dueDays"
        bigint     approved_by         FK "→ users.id (admin/finance)"
        timestamp  approved_at
        UK         meter_month_year_uk    "(meter_id, billing_month, billing_year)"
    }

    PAYMENTS {
        bigint     id                  PK
        string     payment_reference   UK "PAY-YYYY-MM-NNNNNN"
        bigint     bill_id             FK "→ bills.id"
        bigint     customer_id         FK "→ customers.id"
        decimal    amount_paid            "> 0 and ≤ outstanding"
        enum       payment_method         "CASH | MOBILE_MONEY | BANK_TRANSFER | CARD"
        date       payment_date
        bigint     recorded_by         FK "→ users.id (admin/finance)"
    }

    NOTIFICATIONS {
        bigint   id                    PK
        bigint   customer_id           FK "→ customers.id"
        bigint   bill_id               FK "→ bills.id (nullable)"
        text     message                  "exam-mandated format"
        enum     notification_type        "BILL_GENERATED | PAYMENT_CONFIRMED | BILL_PAID | BILL_OVERDUE"
        enum     status                   "UNREAD | READ"
    }

    TARIFFS {
        bigint   id                   PK
        string   name
        enum     meter_type              "WATER | ELECTRICITY"
        enum     tariff_type             "FLAT | TIERED"
        decimal  rate_per_unit           "FLAT only; null for TIERED"
        int      version
        date     effective_from          "must be ≥ today (future cycles only)"
        date     effective_to            "nullable"
        boolean  active
    }

    TARIFF_TIERS {
        bigint   id                   PK
        bigint   tariff_id            FK "→ tariffs.id"
        decimal  min_units               "interval lower bound (exclusive)"
        decimal  max_units               "nullable = +∞"
        decimal  rate_per_unit
    }

    FIXED_CHARGES {
        bigint   id                   PK
        enum     meter_type
        decimal  amount
        int      version
        date     effective_from          "must be ≥ today"
        date     effective_to
        boolean  active
    }

    TAX_CONFIGS {
        bigint   id                   PK
        string   name                    "e.g. VAT"
        decimal  percentage
        date     effective_from          "must be ≥ today"
        date     effective_to
        boolean  active
    }

    PENALTY_CONFIGS {
        bigint   id                   PK
        string   name
        enum     penalty_type            "FIXED | PERCENTAGE"
        decimal  amount_or_percentage
        int      grace_period_days       "added to due_date before penalty fires"
        date     effective_from          "must be ≥ today"
        date     effective_to
        boolean  active
    }
```

## Notes on the model

- **Audit columns** — every table has `created_at` / `updated_at` from `BaseEntity`.
- **Money & quantities** — stored as `numeric(19,4)`; computed values use `BigDecimal` with `RoundingMode.HALF_UP` in service code.
- **Versioned configuration** — `tariffs` and `fixed_charges` carry an explicit `version` column; `tax_configs` and `penalty_configs` use time-effective versioning via `effective_from` / `effective_to`. The bill engine resolves the row whose effective window covers the reading date, picking the highest `version` on ties. `effective_from` is rejected if it is in the past.
- **Soft delete / financial integrity** — bills and payments are never physically deleted; customers with financial history are deactivated instead of deleted.
- **Identity vs profile** — `users` is the authentication identity; `customers` is the billable profile. The link is a nullable 1:1 (`customers.user_id`) created either by self-service profile completion (then admin activation) or by an admin linking an existing user.
- **Notifications source of truth** — populated both by the Java `NotificationService` and by PostgreSQL triggers (`fn_notify_bill_generated`, `fn_notify_full_payment`) and the `sp_process_overdue_bills` stored procedure. Both paths use `NOT EXISTS` guards so they never double-insert.
