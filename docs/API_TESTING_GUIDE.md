# API Testing Guide

Use Swagger at:

```text
http://localhost:8080/swagger-ui/index.html
```

## Postman Import

Import these two files together:

```text
postman/Utility Billing System.postman_collection.json
postman/Utility Billing System.local.postman_environment.json
```

After import:

1. Select the `Utility Billing System Local` environment.
2. Open the `00 Session and Happy Path` folder.
3. Run `Login Admin`, `Login Operator`, `Login Finance`, or `Login Seeded Customer` depending on what you want to test.
4. The login request saves `{{accessToken}}` automatically, so protected requests inherit the token without manual copy-paste.
5. For the automated billing flow, run the requests in `00 Session and Happy Path` from top to bottom.

The collection also stores useful variables such as `customerId`, `meterId`, `readingId`, `billId`, `billReference`, `paymentId`, and `notificationId` as you go.

## Manual Test Path

1. Login as `operator@utility.rw`.
2. Get active meters with `GET /api/v1/meters/active`.
3. Capture a reading with `POST /api/v1/readings`.
4. Login as `finance@utility.rw`.
5. Confirm a pending bill exists with `GET /api/v1/bills`.
6. Approve the bill with `PATCH /api/v1/bills/{id}/approve`.
7. Record a partial payment with `POST /api/v1/payments`.
8. Record the remaining balance with another `POST /api/v1/payments`.
9. Login as `customer@utility.rw`.
10. Verify self-service endpoints under `/api/v1/customers/me`.

## Expected Security Results

- Public registration cannot create staff accounts.
- Operator can capture readings but cannot record payments.
- Finance can approve bills and record payments.
- Customer can only see data linked to their own customer profile.
