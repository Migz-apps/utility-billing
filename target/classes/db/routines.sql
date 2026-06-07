-- =============================================================================
-- PostgreSQL database routines for the Utility Billing System.
-- Provides: a STORED PROCEDURE using a CURSOR to process overdue bills.
--
-- Run manually after the schema is created (ddl-auto=update) with:
--   psql -U postgres -h localhost -d utility_billing_db -f src/main/resources/db/routines.sql
--
-- NOTE: Notification delivery is email-based in the Java application layer.
-- This script intentionally disables database-level in-app notification triggers.
-- =============================================================================


DROP TRIGGER IF EXISTS trg_notify_bill_generated ON bills;
DROP FUNCTION IF EXISTS fn_notify_bill_generated();


DROP TRIGGER IF EXISTS trg_notify_full_payment ON bills;
DROP FUNCTION IF EXISTS fn_notify_full_payment();


-- -----------------------------------------------------------------------------
-- 1) Stored procedure (with an explicit CURSOR) to process overdue bills.
--    Applies the active penalty once per bill, respecting the grace period,
--    flips the bill to OVERDUE.
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

    END LOOP;
    CLOSE bill_cursor;
END;
$$;
