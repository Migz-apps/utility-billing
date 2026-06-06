-- =============================================================================
-- PostgreSQL database routines for the Utility Billing System (Exam Task 6).
-- Provides: (1) a TRIGGER on bill generation, (2) a TRIGGER on full payment,
-- and (3) a STORED PROCEDURE using a CURSOR to process overdue bills.
--
-- Run manually after the schema is created (ddl-auto=update) with:
--   psql -U postgres -h localhost -d utility_billing_db -f src/main/resources/db/routines.sql
--
-- NOTE: The Java NotificationService also creates these notifications and guards
-- against duplicates (findFirstByBillAndNotificationType). These DB routines use
-- the same NOT EXISTS guard, so installing both does not double-notify.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1) On bill generation -> insert a BILL_GENERATED notification.
--    Message uses the exact exam-mandated format, including the customer name.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_notify_bill_generated()
RETURNS TRIGGER AS $$
DECLARE
    v_customer_name TEXT;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM notifications n
        WHERE n.bill_id = NEW.id AND n.notification_type = 'BILL_GENERATED'
    ) THEN
        SELECT c.full_name INTO v_customer_name
        FROM customers c WHERE c.id = NEW.customer_id;

        INSERT INTO notifications (
            customer_id, bill_id, message, notification_type, status, created_at, updated_at
        ) VALUES (
            NEW.customer_id,
            NEW.id,
            'Dear ' || v_customer_name || ',' || E'\n' ||
            'Your ' || NEW.billing_month || '/' || NEW.billing_year ||
            ' utility bill of ' || NEW.total_amount ||
            ' FRW has been successfully processed.',
            'BILL_GENERATED',
            'UNREAD',
            NOW(),
            NOW()
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_notify_bill_generated ON bills;
CREATE TRIGGER trg_notify_bill_generated
AFTER INSERT ON bills
FOR EACH ROW
EXECUTE FUNCTION fn_notify_bill_generated();


-- -----------------------------------------------------------------------------
-- 2) On full payment (status transitions to PAID) -> notify the customer.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_notify_full_payment()
RETURNS TRIGGER AS $$
DECLARE
    v_customer_name TEXT;
BEGIN
    IF NEW.status = 'PAID' AND OLD.status IS DISTINCT FROM 'PAID' THEN
        IF NOT EXISTS (
            SELECT 1 FROM notifications n
            WHERE n.bill_id = NEW.id AND n.notification_type = 'BILL_PAID'
        ) THEN
            SELECT c.full_name INTO v_customer_name
            FROM customers c WHERE c.id = NEW.customer_id;

            INSERT INTO notifications (
                customer_id, bill_id, message, notification_type, status, created_at, updated_at
            ) VALUES (
                NEW.customer_id,
                NEW.id,
                'Dear ' || v_customer_name || ',' || E'\n' ||
                'Your payment for ' || NEW.billing_month || '/' || NEW.billing_year ||
                ' utility bill has been received. Your bill is now fully paid.',
                'BILL_PAID',
                'UNREAD',
                NOW(),
                NOW()
            );
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_notify_full_payment ON bills;
CREATE TRIGGER trg_notify_full_payment
AFTER UPDATE OF status ON bills
FOR EACH ROW
EXECUTE FUNCTION fn_notify_full_payment();


-- -----------------------------------------------------------------------------
-- 3) Stored procedure (with an explicit CURSOR) to process overdue bills.
--    Applies the active penalty once per bill, respecting the grace period,
--    flips the bill to OVERDUE, and inserts a BILL_OVERDUE notification.
--    Call with:  CALL sp_process_overdue_bills();
-- -----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_process_overdue_bills()
LANGUAGE plpgsql AS $$
DECLARE
    -- Active penalty configuration (most recent effective row).
    v_penalty_type   TEXT;
    v_penalty_value  NUMERIC(19,4);
    v_grace_days     INT;
    v_cutoff         DATE;
    v_penalty        NUMERIC(19,4);
    v_customer_name  TEXT;

    -- CURSOR over all bills that are past due and not yet penalised.
    bill_cursor CURSOR FOR
        SELECT * FROM bills b
        WHERE b.status IN ('APPROVED', 'PARTIALLY_PAID')
          AND b.due_date < v_cutoff
        FOR UPDATE;
    bill_row bills%ROWTYPE;
BEGIN
    SELECT pc.penalty_type, pc.amount_or_percentage, pc.grace_period_days
      INTO v_penalty_type, v_penalty_value, v_grace_days
    FROM penalty_configs pc
    WHERE pc.active = TRUE
      AND pc.effective_from <= CURRENT_DATE
      AND (pc.effective_to IS NULL OR pc.effective_to >= CURRENT_DATE)
    ORDER BY pc.id DESC
    LIMIT 1;

    v_grace_days := COALESCE(v_grace_days, 0);
    v_cutoff := CURRENT_DATE - v_grace_days;

    OPEN bill_cursor;
    LOOP
        FETCH bill_cursor INTO bill_row;
        EXIT WHEN NOT FOUND;

        -- Apply the penalty at most once.
        IF bill_row.penalty_amount = 0 AND v_penalty_type IS NOT NULL THEN
            IF v_penalty_type = 'FIXED' THEN
                v_penalty := ROUND(v_penalty_value, 2);
            ELSE
                v_penalty := ROUND(bill_row.outstanding_balance * v_penalty_value / 100, 2);
            END IF;

            UPDATE bills
               SET penalty_amount      = v_penalty,
                   total_amount        = total_amount + v_penalty,
                   outstanding_balance = outstanding_balance + v_penalty,
                   status              = 'OVERDUE',
                   updated_at          = NOW()
             WHERE id = bill_row.id;
        ELSE
            UPDATE bills SET status = 'OVERDUE', updated_at = NOW() WHERE id = bill_row.id;
        END IF;

        SELECT c.full_name INTO v_customer_name FROM customers c WHERE c.id = bill_row.customer_id;

        INSERT INTO notifications (
            customer_id, bill_id, message, notification_type, status, created_at, updated_at
        ) VALUES (
            bill_row.customer_id,
            bill_row.id,
            'Dear ' || v_customer_name || ',' || E'\n' ||
            'Your ' || bill_row.billing_month || '/' || bill_row.billing_year ||
            ' utility bill is overdue. Please settle the outstanding balance to avoid further penalties.',
            'BILL_OVERDUE',
            'UNREAD',
            NOW(),
            NOW()
        );
    END LOOP;
    CLOSE bill_cursor;
END;
$$;
