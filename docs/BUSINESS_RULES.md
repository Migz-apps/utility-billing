# Business Rules

- Public registration always creates `ROLE_CUSTOMER`; public requests do not accept a role field.
- Staff users are seeded initially or created through protected admin APIs.
- Customer data access is resolved from the authenticated user's linked `Customer`.
- Meter readings are unique per meter and billing month/year.
- First reading starts from previous reading `0`.
- Current reading must be greater than previous reading.
- Bill generation is automatic after a valid reading.
- Generated bills start as `PENDING_APPROVAL`.
- Bill approval is restricted to `ROLE_ADMIN` and `ROLE_FINANCE`.
- Payments are recorded only by `ROLE_ADMIN` or `ROLE_FINANCE`.
- Zero, negative, excess, unapproved, cancelled, or already-paid bill payments are rejected.
- Overdue processing is transactional and applies penalty once.
- Bills, payments, and financial history are not physically deleted.
